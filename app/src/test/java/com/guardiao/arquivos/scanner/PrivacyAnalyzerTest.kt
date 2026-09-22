package com.guardiao.arquivos.scanner

import java.io.File
import java.nio.file.Files
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyAnalyzerTest {

  private val analyzer = PrivacyAnalyzer()

  private fun tempDir(): File =
    Files.createTempDirectory("guardiao").toFile().also { it.deleteOnExit() }

  @Test
  fun `categoriza por extensao`() {
    assertEquals(FileCategory.IMAGE, FileCategory.fromFileName("foto.JPG"))
    assertEquals(FileCategory.PDF, FileCategory.fromFileName("contrato.pdf"))
    assertEquals(FileCategory.AUDIO, FileCategory.fromFileName("PTT-20240101-WA0001.opus"))
    assertEquals(null, FileCategory.fromFileName("app.apk"))
    assertEquals("", FileCategory.extensionOf("semextensao"))
  }

  @Test
  fun `nome de arquivo com palavra chave forte gera risco`() {
    val findings = FilenameHeuristics.analyze("Comprovante_Residencia_2024.pdf", "/storage/emulated/0/Download", FileCategory.PDF)
    val analysis = PrivacyAnalysis(findings)
    assertTrue(analysis.findings.any { it.type == FindingType.FILENAME_HINT })
    assertTrue(analysis.level >= RiskLevel.MEDIUM)
  }

  @Test
  fun `mensagem de voz do whatsapp e reconhecida`() {
    val findings =
      FilenameHeuristics.analyze(
        "PTT-20240501-WA0003.opus",
        "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Voice Notes/202418",
        FileCategory.AUDIO,
      )
    assertTrue(findings.any { it.type == FindingType.VOICE_RECORDING })
  }

  @Test
  fun `captura de tela recebe risco medio`() {
    val findings = FilenameHeuristics.analyze("Screenshot_20240101-101010.png", "/storage/emulated/0/Pictures/Screenshots", FileCategory.IMAGE)
    val analysis = PrivacyAnalysis(findings)
    assertEquals(RiskLevel.LOW, analysis.level)
    assertTrue(findings.any { it.type == FindingType.SCREENSHOT })
  }

  @Test
  fun `arquivo neutro nao gera indicios`() {
    val findings = FilenameHeuristics.analyze("wallpaper.png", "/storage/emulated/0/Pictures/Wallpapers", FileCategory.IMAGE)
    assertEquals(RiskLevel.NONE, PrivacyAnalysis(findings).level)
  }

  @Test
  fun `analisa conteudo de arquivo txt`() {
    val file = File(tempDir(), "notas.txt")
    file.writeText("Meu CPF é 529.982.247-25 e o telefone (21) 98765-4321")
    val analysis = analyzer.analyze(file, FileCategory.DOCUMENT)
    assertTrue(analysis.findings.any { it.type == FindingType.CPF })
    assertTrue(analysis.findings.any { it.type == FindingType.PHONE })
    assertEquals(RiskLevel.HIGH, analysis.level)
  }

  @Test
  fun `extrai texto de docx`() {
    val file = File(tempDir(), "curriculo.docx")
    ZipOutputStream(file.outputStream()).use { zip ->
      zip.putNextEntry(ZipEntry("word/document.xml"))
      zip.write(
        "<w:document><w:body><w:p><w:r><w:t>Contato: pessoa@example.com</w:t></w:r></w:p></w:body></w:document>"
          .toByteArray()
      )
      zip.closeEntry()
    }
    val analysis = analyzer.analyze(file, FileCategory.DOCUMENT)
    assertTrue(analysis.findings.any { it.type == FindingType.EMAIL })
    assertTrue(analysis.findings.any { it.type == FindingType.FILENAME_HINT })
  }

  @Test
  fun `extrai texto de pdf com stream comprimido`() {
    val content = "BT /F1 12 Tf (CPF 529.982.247-25) Tj ET"
    val deflater = Deflater()
    deflater.setInput(content.toByteArray(Charsets.ISO_8859_1))
    deflater.finish()
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(1024)
    while (!deflater.finished()) {
      val n = deflater.deflate(buffer)
      out.write(buffer, 0, n)
    }
    val compressed = out.toByteArray()
    val file = File(tempDir(), "arquivo.pdf")
    file.outputStream().use { fos ->
      fos.write("%PDF-1.4\n1 0 obj\n<< /Length ${compressed.size} /Filter /FlateDecode >>\nstream\n".toByteArray(Charsets.ISO_8859_1))
      fos.write(compressed)
      fos.write("\nendstream\nendobj\n%%EOF".toByteArray(Charsets.ISO_8859_1))
    }
    val analysis = analyzer.analyze(file, FileCategory.PDF)
    assertTrue(analysis.findings.any { it.type == FindingType.CPF })
  }

  @Test
  fun `extrai texto de pdf sem compressao`() {
    val file = File(tempDir(), "simples.pdf")
    file.writeText("%PDF-1.4\n1 0 obj\n<< /Length 40 >>\nstream\nBT [(E-mail: ) (teste@example.com)] TJ ET\nendstream\nendobj", Charsets.ISO_8859_1)
    val analysis = analyzer.analyze(file, FileCategory.PDF)
    assertTrue(analysis.findings.any { it.type == FindingType.EMAIL })
  }

  @Test
  fun `metadados gps geram risco`() {
    val metadata = MediaMetadata(hasGpsLocation = true, deviceModel = "Pixel 8")
    val findings = analyzer.analyzeMetadata(metadata, FileCategory.IMAGE)
    assertTrue(findings.any { it.type == FindingType.GPS_LOCATION })
    assertTrue(PrivacyAnalysis(findings).level >= RiskLevel.MEDIUM)
  }

  @Test
  fun `musica com tags nao e tratada como gravacao`() {
    val metadata = MediaMetadata(hasMusicTags = true, author = "Banda", durationMillis = 200_000)
    val findings = analyzer.analyzeMetadata(metadata, FileCategory.AUDIO)
    assertFalse(findings.any { it.type == FindingType.VOICE_RECORDING })
    assertFalse(findings.any { it.type == FindingType.AUTHOR_METADATA })
  }

  @Test
  fun `strip rtf e tags`() {
    assertEquals("Olá mundo", TextExtractor.stripRtf("{\\rtf1\\ansi Ol\\'e1 mundo}"))
    assertEquals("a & b", TextExtractor.stripTags("<p>a &amp; b</p>"))
  }
}

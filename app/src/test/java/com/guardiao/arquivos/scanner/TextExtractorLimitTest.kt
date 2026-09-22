package com.guardiao.arquivos.scanner

import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TextExtractorLimitTest {

  private fun tempDir(): File =
    Files.createTempDirectory("guardiao").toFile().also { it.deleteOnExit() }

  @Test
  fun `limite corta o texto de arquivo simples`() {
    val file = File(tempDir(), "longo.txt")
    file.writeText("abcdefghij".repeat(500))

    val prévia = TextExtractor.extract(file, "txt", maxChars = 40)

    assertNotNull(prévia)
    assertEquals(40, prévia!!.length)
    assertTrue(prévia.startsWith("abcdefghij"))
  }

  @Test
  fun `sem limite explicito o texto inteiro volta`() {
    val file = File(tempDir(), "curto.txt")
    file.writeText("Contato: pessoa@example.com")

    assertEquals("Contato: pessoa@example.com", TextExtractor.extract(file, "txt"))
  }

  @Test
  fun `limite tambem vale para docx`() {
    val file = File(tempDir(), "doc.docx")
    ZipOutputStream(file.outputStream()).use { zip ->
      zip.putNextEntry(ZipEntry("word/document.xml"))
      zip.write(
        ("<w:document><w:body><w:p><w:r><w:t>" + "palavra ".repeat(400) + "</w:t></w:r></w:p></w:body></w:document>")
          .toByteArray()
      )
      zip.closeEntry()
    }

    val prévia = TextExtractor.extract(file, "docx", maxChars = 60)

    assertNotNull(prévia)
    assertTrue(prévia!!.length <= 60)
    assertTrue(prévia.contains("palavra"))
  }

  @Test
  fun `limite fora da faixa e ajustado, nao quebra`() {
    val file = File(tempDir(), "x.txt")
    file.writeText("texto de teste")

    assertEquals("t", TextExtractor.extract(file, "txt", maxChars = 1))
    // Abaixo de 1 vira 1, em vez de devolver vazio ou estourar.
    assertEquals("t", TextExtractor.extract(file, "txt", maxChars = 0))
    assertEquals("t", TextExtractor.extract(file, "txt", maxChars = -5))
    // Acima do teto o texto inteiro volta, limitado ao máximo do extrator.
    assertEquals("texto de teste", TextExtractor.extract(file, "txt", maxChars = Int.MAX_VALUE))
  }

  @Test
  fun `formatos com previa possivel estao listados`() {
    listOf("pdf", "docx", "doc", "odt", "rtf", "txt", "md").forEach {
      assertTrue("faltou $it", TextExtractor.supportedExtensions.contains(it))
    }
    assertTrue(!TextExtractor.supportedExtensions.contains("jpg"))
  }
}

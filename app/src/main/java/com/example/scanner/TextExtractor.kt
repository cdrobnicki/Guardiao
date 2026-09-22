package com.example.scanner

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset
import java.util.zip.DataFormatException
import java.util.zip.Inflater
import java.util.zip.ZipInputStream

/**
 * Extrai texto de documentos usando apenas a biblioteca padrão, sem enviar nada para fora do
 * aparelho.
 *
 * A extração é propositalmente simples: o objetivo é obter texto suficiente para as heurísticas
 * de [PatternDetectors], não reproduzir o documento com fidelidade.
 */
object TextExtractor {

  /** Limite de bytes lidos de um arquivo para análise de conteúdo. */
  const val MAX_BYTES = 12L * 1024 * 1024

  /** Limite de caracteres de texto devolvidos. */
  const val MAX_CHARS = 300_000

  private val plainTextExtensions = setOf("txt", "md", "log", "json", "xml", "csv", "tsv", "html", "htm")
  private val ooxmlExtensions = setOf("docx", "xlsx", "xlsm", "pptx")
  private val odfExtensions = setOf("odt", "ods", "odp")
  private val legacyOfficeExtensions = setOf("doc", "xls", "ppt")

  /** Devolve o texto extraído ou null se o formato não é suportado ou a leitura falhou. */
  fun extract(file: File, extension: String): String? {
    if (!file.isFile || file.length() == 0L) return null
    return try {
      when (extension.lowercase()) {
        in plainTextExtensions -> readPlainText(file)
        "rtf" -> stripRtf(readPlainText(file))
        in ooxmlExtensions -> extractFromZip(file) { name ->
          name.startsWith("word/") || name.startsWith("xl/sharedStrings") || name.startsWith("xl/worksheets/") ||
            name.startsWith("ppt/slides/") || name == "docProps/core.xml"
        }
        in odfExtensions -> extractFromZip(file) { name -> name == "content.xml" || name == "meta.xml" }
        "epub" -> extractFromZip(file) { name -> name.endsWith(".xhtml") || name.endsWith(".html") || name.endsWith(".opf") }
        "pdf" -> extractFromPdf(file)
        in legacyOfficeExtensions -> extractPrintableStrings(readBytes(file))
        else -> null
      }?.take(MAX_CHARS)
    } catch (_: Exception) {
      null
    } catch (_: OutOfMemoryError) {
      null
    }
  }

  private fun readBytes(file: File): ByteArray {
    val size = minOf(file.length(), MAX_BYTES).toInt()
    val buffer = ByteArray(size)
    file.inputStream().use { input ->
      var read = 0
      while (read < size) {
        val n = input.read(buffer, read, size - read)
        if (n < 0) break
        read += n
      }
      return if (read == size) buffer else buffer.copyOf(read)
    }
  }

  private fun readPlainText(file: File): String = decode(readBytes(file))

  private fun decode(bytes: ByteArray): String {
    if (bytes.size >= 2) {
      val b0 = bytes[0].toInt() and 0xFF
      val b1 = bytes[1].toInt() and 0xFF
      if (b0 == 0xFF && b1 == 0xFE) return String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
      if (b0 == 0xFE && b1 == 0xFF) return String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
    }
    val utf8 = String(bytes, Charsets.UTF_8)
    // Se houve muitas substituições, provavelmente é Latin-1 (comum em CSV antigos).
    val replacements = utf8.count { it == '\uFFFD' }
    return if (replacements > utf8.length / 100) String(bytes, Charset.forName("ISO-8859-1")) else utf8
  }

  private fun extractFromZip(file: File, accept: (String) -> Boolean): String {
    val builder = StringBuilder()
    ZipInputStream(file.inputStream().buffered()).use { zip ->
      var entry = zip.nextEntry
      while (entry != null && builder.length < MAX_CHARS) {
        if (!entry.isDirectory && accept(entry.name)) {
          val bytes = readEntry(zip)
          builder.append(stripTags(String(bytes, Charsets.UTF_8))).append('\n')
        }
        zip.closeEntry()
        entry = zip.nextEntry
      }
    }
    return builder.toString()
  }

  private fun readEntry(input: InputStream): ByteArray {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    var total = 0L
    while (true) {
      val n = input.read(buffer)
      if (n < 0) break
      out.write(buffer, 0, n)
      total += n
      if (total > MAX_BYTES) break
    }
    return out.toByteArray()
  }

  /** Remove marcações XML/HTML e converte entidades básicas. */
  fun stripTags(markup: String): String =
    markup
      .replace(Regex("<[^>]+>"), " ")
      .replace("&amp;", "&")
      .replace("&lt;", "<")
      .replace("&gt;", ">")
      .replace("&quot;", "\"")
      .replace("&apos;", "'")
      .replace("&nbsp;", " ")
      .replace(Regex("&#(\\d+);")) { m -> m.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: " " }
      .replace(Regex("\\s+"), " ")
      .trim()

  /** Remove palavras de controle do RTF, deixando o texto. */
  fun stripRtf(rtf: String): String =
    rtf
      .replace(Regex("""\\'([0-9a-fA-F]{2})""")) { m -> m.groupValues[1].toInt(16).toChar().toString() }
      .replace(Regex("""\\[a-zA-Z]+-?\d* ?"""), " ")
      .replace(Regex("""[{}]"""), " ")
      .replace(Regex("\\s+"), " ")
      .trim()

  // ---------------------------------------------------------------------------------------------
  // PDF
  // ---------------------------------------------------------------------------------------------

  private val pdfLiteralRegex = Regex("""\((?:\\.|[^\\)])*\)""")
  private val pdfHexRegex = Regex("""<([0-9A-Fa-f\s]{4,})>\s*Tj""")
  private val pdfTextOperatorRegex = Regex("""\((?:\\.|[^\\)])*\)\s*Tj|\[[^\]]*\]\s*TJ|\((?:\\.|[^\\)])*\)\s*'""")
  private val pdfInfoRegex = Regex("""/(Title|Author|Subject|Keywords|Creator)\s*\((?:\\.|[^\\)])*\)""")

  private fun extractFromPdf(file: File): String {
    val bytes = readBytes(file)
    val raw = String(bytes, Charsets.ISO_8859_1)
    val builder = StringBuilder()

    pdfInfoRegex.findAll(raw).forEach { m -> builder.append(decodePdfString(m.value.substringAfter('('))).append('\n') }

    // Texto em streams não comprimidos.
    builder.append(collectPdfText(raw))

    // Streams comprimidos com FlateDecode.
    var searchFrom = 0
    var streamsRead = 0
    while (builder.length < MAX_CHARS && streamsRead < 2000) {
      val start = raw.indexOf("stream", searchFrom)
      if (start < 0) break
      var dataStart = start + "stream".length
      if (dataStart < raw.length && raw[dataStart] == '\r') dataStart++
      if (dataStart < raw.length && raw[dataStart] == '\n') dataStart++
      val end = raw.indexOf("endstream", dataStart)
      if (end < 0) break
      searchFrom = end + "endstream".length
      streamsRead++
      val header = raw.substring((start - 300).coerceAtLeast(0), start)
      if (!header.contains("FlateDecode")) continue
      val inflated = inflate(bytes, dataStart, end - dataStart) ?: continue
      val content = String(inflated, Charsets.ISO_8859_1)
      builder.append(collectPdfText(content))
    }
    return builder.toString()
  }

  private fun collectPdfText(content: String): String {
    if (!content.contains("Tj") && !content.contains("TJ") && !content.contains("'")) return ""
    val builder = StringBuilder()
    pdfTextOperatorRegex.findAll(content).forEach { m ->
      val chunk = m.value
      if (chunk.trimEnd().endsWith("TJ")) {
        pdfLiteralRegex.findAll(chunk).forEach { lit -> builder.append(decodePdfString(lit.value.substring(1))) }
        builder.append(' ')
      } else {
        builder.append(decodePdfString(chunk.substringAfter('('))).append(' ')
      }
      if (builder.length > MAX_CHARS) return builder.toString()
    }
    pdfHexRegex.findAll(content).forEach { m ->
      val hex = m.groupValues[1].replace(Regex("\\s"), "")
      val text = hex.chunked(2).mapNotNull { it.toIntOrNull(16)?.toChar() }.joinToString("")
      builder.append(text).append(' ')
    }
    return builder.toString()
  }

  /** Decodifica uma string literal PDF (sem o parêntese inicial; termina no parêntese de fechamento). */
  private fun decodePdfString(literalTail: String): String {
    val builder = StringBuilder()
    var i = 0
    var depth = 1
    while (i < literalTail.length) {
      val c = literalTail[i]
      when {
        c == '\\' && i + 1 < literalTail.length -> {
          val next = literalTail[i + 1]
          when (next) {
            'n' -> builder.append('\n')
            'r' -> builder.append('\r')
            't' -> builder.append('\t')
            '(' , ')', '\\' -> builder.append(next)
            in '0'..'7' -> {
              val octal = literalTail.substring(i + 1).takeWhile { it in '0'..'7' }.take(3)
              builder.append(octal.toInt(8).toChar())
              i += octal.length - 1
            }
            else -> builder.append(next)
          }
          i += 2
          continue
        }
        c == '(' -> depth++
        c == ')' -> {
          depth--
          if (depth == 0) break
        }
      }
      builder.append(c)
      i++
    }
    return builder.toString()
  }

  private fun inflate(bytes: ByteArray, offset: Int, length: Int): ByteArray? {
    if (length <= 0 || offset + length > bytes.size) return null
    val inflater = Inflater()
    return try {
      inflater.setInput(bytes, offset, length)
      val out = ByteArrayOutputStream()
      val buffer = ByteArray(16 * 1024)
      while (!inflater.finished() && out.size() < 4 * 1024 * 1024) {
        val n = inflater.inflate(buffer)
        if (n == 0) {
          if (inflater.needsInput() || inflater.needsDictionary()) break
        } else out.write(buffer, 0, n)
      }
      out.toByteArray().takeIf { it.isNotEmpty() }
    } catch (_: DataFormatException) {
      null
    } finally {
      inflater.end()
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Formatos binários antigos (DOC/XLS/PPT)
  // ---------------------------------------------------------------------------------------------

  /** Extrai sequências de caracteres imprimíveis (ASCII/Latin-1 e UTF-16LE) de dados binários. */
  fun extractPrintableStrings(bytes: ByteArray, minLength: Int = 4): String {
    val builder = StringBuilder()
    var run = StringBuilder()

    fun flush() {
      if (run.length >= minLength) builder.append(run).append('\n')
      run = StringBuilder()
    }

    // Latin-1
    for (b in bytes) {
      val c = (b.toInt() and 0xFF).toChar()
      if (isPrintable(c)) run.append(c) else flush()
      if (builder.length > MAX_CHARS) return builder.toString()
    }
    flush()

    // UTF-16LE (usado pelo Word 97+ para texto)
    var i = 0
    while (i + 1 < bytes.size) {
      val lo = bytes[i].toInt() and 0xFF
      val hi = bytes[i + 1].toInt() and 0xFF
      val c = ((hi shl 8) or lo).toChar()
      if (hi == 0 && isPrintable(c)) run.append(c) else flush()
      i += 2
      if (builder.length > MAX_CHARS) return builder.toString()
    }
    flush()
    return builder.toString()
  }

  private fun isPrintable(c: Char): Boolean =
    c == ' ' || c == '\t' || (c.code in 0x21..0x7E) || (c.code in 0xA0..0xFF)
}

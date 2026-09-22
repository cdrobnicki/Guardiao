package com.guardiao.arquivos.scanner

/**
 * Categorias de arquivos suportadas pela varredura.
 *
 * Cada categoria conhece as extensões que a compõem. Arquivos com extensões fora dessas listas
 * são ignorados na varredura (executáveis, caches, bancos de dados de apps etc.).
 */
enum class FileCategory(val label: String, val extensions: Set<String>) {
  IMAGE(
    "Fotos",
    setOf("jpg", "jpeg", "png", "gif", "webp", "heic", "heif", "bmp", "tif", "tiff", "dng", "raw", "avif"),
  ),
  VIDEO(
    "Vídeos",
    setOf("mp4", "mkv", "mov", "avi", "3gp", "3g2", "webm", "m4v", "wmv", "flv", "mpg", "mpeg", "ts"),
  ),
  AUDIO(
    "Áudio",
    setOf("mp3", "wav", "ogg", "oga", "opus", "m4a", "aac", "flac", "amr", "wma", "mid", "midi", "3ga", "aiff"),
  ),
  PDF("PDF", setOf("pdf")),
  DOCUMENT(
    "Documentos",
    setOf("doc", "docx", "odt", "rtf", "txt", "md", "epub", "pages", "log", "json", "xml", "html", "htm"),
  ),
  SPREADSHEET("Planilhas", setOf("xls", "xlsx", "xlsm", "ods", "csv", "tsv", "numbers")),
  PRESENTATION("Apresentações", setOf("ppt", "pptx", "odp", "key"));

  companion object {
    private val byExtension: Map<String, FileCategory> =
      entries.flatMap { category -> category.extensions.map { it to category } }.toMap()

    /** Retorna a categoria para a extensão informada (sem ponto, qualquer caixa) ou null. */
    fun fromExtension(extension: String): FileCategory? = byExtension[extension.lowercase()]

    /** Retorna a categoria a partir do nome completo do arquivo ou null. */
    fun fromFileName(fileName: String): FileCategory? = fromExtension(extensionOf(fileName))

    /** Extrai a extensão (sem o ponto, em minúsculas) de um nome de arquivo. */
    fun extensionOf(fileName: String): String {
      val dot = fileName.lastIndexOf('.')
      if (dot <= 0 || dot == fileName.length - 1) return ""
      return fileName.substring(dot + 1).lowercase()
    }
  }
}

/** Tabela de tipos MIME usada quando o sistema não conhece a extensão. */
object MimeTypes {
  private val table =
    mapOf(
      "jpg" to "image/jpeg",
      "jpeg" to "image/jpeg",
      "png" to "image/png",
      "gif" to "image/gif",
      "webp" to "image/webp",
      "heic" to "image/heic",
      "heif" to "image/heif",
      "bmp" to "image/bmp",
      "tif" to "image/tiff",
      "tiff" to "image/tiff",
      "dng" to "image/x-adobe-dng",
      "avif" to "image/avif",
      "mp4" to "video/mp4",
      "m4v" to "video/mp4",
      "mkv" to "video/x-matroska",
      "mov" to "video/quicktime",
      "avi" to "video/x-msvideo",
      "3gp" to "video/3gpp",
      "3g2" to "video/3gpp2",
      "webm" to "video/webm",
      "wmv" to "video/x-ms-wmv",
      "flv" to "video/x-flv",
      "mpg" to "video/mpeg",
      "mpeg" to "video/mpeg",
      "ts" to "video/mp2t",
      "mp3" to "audio/mpeg",
      "wav" to "audio/wav",
      "ogg" to "audio/ogg",
      "oga" to "audio/ogg",
      "opus" to "audio/opus",
      "m4a" to "audio/mp4",
      "aac" to "audio/aac",
      "flac" to "audio/flac",
      "amr" to "audio/amr",
      "wma" to "audio/x-ms-wma",
      "mid" to "audio/midi",
      "midi" to "audio/midi",
      "3ga" to "audio/3gpp",
      "aiff" to "audio/aiff",
      "pdf" to "application/pdf",
      "doc" to "application/msword",
      "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      "odt" to "application/vnd.oasis.opendocument.text",
      "rtf" to "application/rtf",
      "txt" to "text/plain",
      "md" to "text/markdown",
      "log" to "text/plain",
      "json" to "application/json",
      "xml" to "text/xml",
      "html" to "text/html",
      "htm" to "text/html",
      "epub" to "application/epub+zip",
      "xls" to "application/vnd.ms-excel",
      "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      "xlsm" to "application/vnd.ms-excel.sheet.macroEnabled.12",
      "ods" to "application/vnd.oasis.opendocument.spreadsheet",
      "csv" to "text/csv",
      "tsv" to "text/tab-separated-values",
      "ppt" to "application/vnd.ms-powerpoint",
      "pptx" to "application/vnd.openxmlformats-officedocument.presentationml.presentation",
      "odp" to "application/vnd.oasis.opendocument.presentation",
    )

  fun forExtension(extension: String): String =
    table[extension.lowercase()]
      ?: when (FileCategory.fromExtension(extension)) {
        FileCategory.IMAGE -> "image/*"
        FileCategory.VIDEO -> "video/*"
        FileCategory.AUDIO -> "audio/*"
        else -> "application/octet-stream"
      }
}

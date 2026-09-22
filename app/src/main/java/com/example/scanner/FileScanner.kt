package com.example.scanner

import android.content.Context
import android.os.Environment
import android.webkit.MimeTypeMap
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/** Evento emitido durante a varredura. */
sealed class ScanEvent {
  data class Progress(val progress: ScanProgress) : ScanEvent()

  data class Found(val file: ScannedFile) : ScanEvent()

  data class Finished(val total: Int, val matched: Int) : ScanEvent()
}

/**
 * Percorre o armazenamento do aparelho e analisa cada arquivo de interesse.
 *
 * A varredura ignora pastas ocultas, `Android/data` e `Android/obb` (inacessíveis) e a pasta de
 * quarentena escolhida pelo usuário.
 */
class FileScanner(
  private val context: Context,
  private val analyzer: PrivacyAnalyzer = PrivacyAnalyzer(AndroidMediaInspector()),
) {

  private val skippedDirectoryNames = setOf(".thumbnails", ".trashed", ".cache", "cache", "LOST.DIR", ".Trash")

  /** Inicia a varredura. [excludedPaths] são pastas que não devem ser percorridas (ex.: quarentena). */
  fun scan(excludedPaths: List<String> = emptyList()): Flow<ScanEvent> =
    flow {
      val roots = StorageAccess.storageRoots(context)
      val excluded = excludedPaths.map { it.trimEnd('/') }.filter { it.isNotBlank() }
      var scanned = 0
      var matched = 0
      var lastEmitted = 0L

      suspend fun emitProgress(current: String, force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (force || now - lastEmitted > 150) {
          lastEmitted = now
          emit(ScanEvent.Progress(ScanProgress(scanned, matched, current)))
        }
      }

      val stack = ArrayDeque<File>()
      roots.forEach { stack.addLast(it) }
      val visited = HashSet<String>()

      while (stack.isNotEmpty()) {
        currentCoroutineContext().ensureActive()
        val dir = stack.removeLast()
        val canonical = runCatching { dir.canonicalPath }.getOrDefault(dir.absolutePath)
        if (!visited.add(canonical)) continue
        if (shouldSkipDirectory(dir, roots, excluded)) continue

        val children = dir.listFiles() ?: continue
        emitProgress(dir.absolutePath)

        for (child in children) {
          currentCoroutineContext().ensureActive()
          if (child.isDirectory) {
            stack.addLast(child)
            continue
          }
          if (!child.isFile) continue
          scanned++
          val category = FileCategory.fromFileName(child.name) ?: continue
          val scannedFile = analyzeFile(child, category)
          matched++
          emit(ScanEvent.Found(scannedFile))
          emitProgress(child.absolutePath)
        }
      }
      emit(ScanEvent.Progress(ScanProgress(scanned, matched, "", finished = true)))
      emit(ScanEvent.Finished(scanned, matched))
    }
      .flowOn(Dispatchers.IO)

  /** Reanalisa um único arquivo (usado após restaurar da quarentena). */
  fun analyzeFile(file: File, category: FileCategory = FileCategory.fromFileName(file.name) ?: FileCategory.DOCUMENT): ScannedFile {
    val extension = FileCategory.extensionOf(file.name)
    val analysis = runCatching { analyzer.analyze(file, category) }.getOrDefault(PrivacyAnalysis.EMPTY)
    return ScannedFile(
      path = file.absolutePath,
      name = file.name,
      extension = extension,
      category = category,
      mimeType = mimeTypeFor(extension),
      sizeBytes = file.length(),
      lastModified = file.lastModified(),
      analysis = analysis,
    )
  }

  private fun shouldSkipDirectory(dir: File, roots: List<File>, excluded: List<String>): Boolean {
    val name = dir.name
    val path = dir.absolutePath
    if (excluded.any { path == it || path.startsWith("$it/") }) return true
    if (dir in roots) return false
    if (name.startsWith(".") || name in skippedDirectoryNames) return true
    // Android/data e Android/obb não são legíveis; Android/media (WhatsApp etc.) é.
    val parent = dir.parentFile
    if (parent != null && parent.name == "Android" && (name == "data" || name == "obb")) return true
    return false
  }

  companion object {
    fun mimeTypeFor(extension: String): String {
      val fromSystem = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
      return fromSystem ?: MimeTypes.forExtension(extension)
    }
  }
}

/** Utilidades de acesso ao armazenamento e permissões. */
object StorageAccess {

  /** Raízes de armazenamento a serem varridas (interno compartilhado + cartões SD). */
  fun storageRoots(context: Context): List<File> {
    val roots = linkedSetOf<File>()
    val primary = Environment.getExternalStorageDirectory()
    if (primary != null && primary.exists()) roots += primary

    // Cada volume aparece como .../<volume>/Android/data/<pacote>/files; subimos até o volume.
    context.getExternalFilesDirs(null).filterNotNull().forEach { appDir ->
      val marker = "/Android/data/"
      val path = appDir.absolutePath
      val index = path.indexOf(marker)
      if (index > 0) {
        val root = File(path.substring(0, index))
        if (root.exists() && root.canRead()) roots += root
      }
    }
    return roots.toList()
  }
}

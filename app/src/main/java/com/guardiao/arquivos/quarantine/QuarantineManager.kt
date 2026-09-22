package com.guardiao.arquivos.quarantine

import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.core.content.edit
import androidx.documentfile.provider.DocumentFile
import com.guardiao.arquivos.scanner.FileScanner
import com.guardiao.arquivos.scanner.ScannedFile
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Move arquivos para a pasta de quarentena escolhida pelo usuário e os restaura.
 *
 * A pasta é escolhida pelo seletor de pastas do sistema (Storage Access Framework). Quando ela
 * está no armazenamento compartilhado, o arquivo é movido diretamente pelo caminho; caso
 * contrário, é copiado por meio do provedor de documentos e o original é apagado.
 */
class QuarantineManager(private val context: Context, private val dao: QuarantineDao) {

  private val prefs = context.getSharedPreferences("guardiao_prefs", Context.MODE_PRIVATE)

  /** URI da pasta de quarentena escolhida ou null. */
  val folderUri: Uri?
    get() = prefs.getString(KEY_FOLDER_URI, null)?.let(Uri::parse)

  /** Caminho local da pasta de quarentena quando ela pode ser resolvida em um caminho. */
  val folderPath: String?
    get() = folderUri?.let { resolveTreeUriToFile(it)?.absolutePath }

  /** Nome amigável da pasta para exibição. */
  val folderDisplayName: String?
    get() {
      val uri = folderUri ?: return null
      folderPath?.let { return it }
      return runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull() ?: uri.toString()
    }

  /** Guarda a pasta escolhida e mantém a permissão de acesso após reiniciar o app. */
  fun setFolder(uri: Uri) {
    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
    prefs.edit { putString(KEY_FOLDER_URI, uri.toString()) }
  }

  /** Move [file] para a quarentena e registra a operação. */
  suspend fun quarantine(file: ScannedFile): Result<QuarantineRecord> =
    withContext(Dispatchers.IO) {
      runCatching {
        val treeUri = folderUri ?: throw IOException("Nenhuma pasta de quarentena foi escolhida.")
        val source = File(file.path)
        if (!source.isFile) throw IOException("O arquivo não existe mais.")

        val destinationPath = moveToTree(source, treeUri, file.mimeType)
        val record =
          QuarantineRecord(
            fileName = file.name,
            originalPath = file.path,
            quarantinePath = destinationPath,
            category = file.category.name,
            riskLevel = file.riskLevel.name,
            sizeBytes = file.sizeBytes,
          )
        val id = dao.insert(record)
        notifyMediaScanner(listOf(file.path, destinationPath))
        record.copy(id = id)
      }
    }

  /** Devolve o arquivo para o local original e remove o registro. */
  suspend fun restore(record: QuarantineRecord): Result<File> =
    withContext(Dispatchers.IO) {
      runCatching {
        val target = uniqueFile(File(record.originalPath))
        target.parentFile?.mkdirs()
        if (record.quarantinePath.startsWith("content://")) {
          val doc =
            DocumentFile.fromSingleUri(context, Uri.parse(record.quarantinePath))
              ?: throw IOException("Arquivo em quarentena não encontrado.")
          if (!doc.exists()) throw IOException("Arquivo em quarentena não encontrado.")
          context.contentResolver.openInputStream(doc.uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
          } ?: throw IOException("Não foi possível ler o arquivo em quarentena.")
          if (!doc.delete()) throw IOException("Arquivo restaurado, mas a cópia na quarentena não pôde ser removida.")
        } else {
          val source = File(record.quarantinePath)
          if (!source.isFile) throw IOException("Arquivo em quarentena não encontrado.")
          moveFile(source, target)
        }
        dao.delete(record)
        notifyMediaScanner(listOf(record.quarantinePath, target.absolutePath))
        target
      }
    }

  /** Remove apenas o registro (quando o arquivo foi apagado manualmente). */
  suspend fun forget(record: QuarantineRecord) = dao.delete(record)

  /** Abre um arquivo em quarentena em outro app. */
  fun uriFor(record: QuarantineRecord): Uri? =
    if (record.quarantinePath.startsWith("content://")) {
      Uri.parse(record.quarantinePath)
    } else {
      FileOpener.contentUri(context, File(record.quarantinePath))
    }

  private fun moveToTree(source: File, treeUri: Uri, mimeType: String): String {
    val localDir = resolveTreeUriToFile(treeUri)
    if (localDir != null && (localDir.isDirectory || localDir.mkdirs()) && localDir.canWrite()) {
      val target = uniqueFile(File(localDir, source.name))
      moveFile(source, target)
      return target.absolutePath
    }

    val tree = DocumentFile.fromTreeUri(context, treeUri) ?: throw IOException("Pasta de quarentena inacessível.")
    if (!tree.canWrite()) throw IOException("Sem permissão de escrita na pasta de quarentena.")
    val created =
      tree.createFile(mimeType, source.name)
        ?: throw IOException("Não foi possível criar o arquivo na pasta de quarentena.")
    try {
      context.contentResolver.openOutputStream(created.uri)?.use { output ->
        source.inputStream().use { input -> input.copyTo(output) }
      } ?: throw IOException("Não foi possível escrever na pasta de quarentena.")
    } catch (e: IOException) {
      created.delete()
      throw e
    }
    if (!source.delete()) {
      created.delete()
      throw IOException("Não foi possível remover o arquivo original.")
    }
    return created.uri.toString()
  }

  private fun moveFile(source: File, target: File) {
    if (source.renameTo(target)) return
    source.inputStream().use { input -> target.outputStream().use { output -> input.copyTo(output) } }
    if (target.length() != source.length()) {
      target.delete()
      throw IOException("Falha ao copiar o arquivo.")
    }
    if (!source.delete()) {
      target.delete()
      throw IOException("Não foi possível remover o arquivo original.")
    }
  }

  private fun uniqueFile(desired: File): File {
    if (!desired.exists()) return desired
    val base = desired.nameWithoutExtension
    val ext = desired.extension
    var index = 1
    while (true) {
      val candidate = File(desired.parentFile, if (ext.isEmpty()) "$base ($index)" else "$base ($index).$ext")
      if (!candidate.exists()) return candidate
      index++
    }
  }

  private fun notifyMediaScanner(paths: List<String>) {
    val real = paths.filter { !it.startsWith("content://") }.toTypedArray()
    if (real.isEmpty()) return
    runCatching { MediaScannerConnection.scanFile(context, real, null, null) }
  }

  /**
   * Converte uma URI de árvore do SAF (`primary:Quarentena`, `1234-ABCD:Pasta`) em um caminho
   * local. Devolve null quando o provedor não expõe um caminho.
   */
  fun resolveTreeUriToFile(treeUri: Uri): File? {
    if (treeUri.authority != "com.android.externalstorage.documents") return null
    val docId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull() ?: return null
    val separator = docId.indexOf(':')
    if (separator < 0) return null
    val volume = docId.substring(0, separator)
    val relative = docId.substring(separator + 1)
    val root =
      when (volume) {
        "primary" -> Environment.getExternalStorageDirectory()
        "home" -> File(Environment.getExternalStorageDirectory(), "Documents")
        else -> File("/storage/$volume")
      }
    return if (relative.isEmpty()) root else File(root, relative)
  }

  companion object {
    private const val KEY_FOLDER_URI = "quarantine_folder_uri"
  }
}

/**
 * Gera as URIs usadas para abrir arquivos em outros apps.
 *
 * A montagem e o disparo do Intent ficam em
 * [com.guardiao.arquivos.openwith.FileLauncher], que também lembra o app escolhido.
 */
object FileOpener {
  fun contentUri(context: Context, file: File): Uri? =
    runCatching {
      androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
      .getOrNull()

  fun mimeTypeFor(extension: String): String = FileScanner.mimeTypeFor(extension)
}

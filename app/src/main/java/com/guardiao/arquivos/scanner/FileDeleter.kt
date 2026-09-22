package com.guardiao.arquivos.scanner

import android.content.Context
import android.media.MediaScannerConnection
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Apaga um arquivo do armazenamento, em definitivo.
 *
 * Diferente da quarentena, aqui não há volta: o arquivo não vai para lixeira nenhuma. Por isso a
 * confirmação é responsabilidade de quem chama.
 */
class FileDeleter(private val context: Context) {

  suspend fun delete(file: ScannedFile): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        val alvo = File(file.path)
        // Já não existe: o objetivo do usuário está cumprido, não é erro.
        if (!alvo.exists()) return@runCatching
        if (!alvo.delete()) {
          throw IOException("O sistema não permitiu apagar este arquivo.")
        }
        // Sem isto, o arquivo continuaria aparecendo na galeria e em outros apps.
        runCatching { MediaScannerConnection.scanFile(context, arrayOf(file.path), null, null) }
        Unit
      }
    }
}

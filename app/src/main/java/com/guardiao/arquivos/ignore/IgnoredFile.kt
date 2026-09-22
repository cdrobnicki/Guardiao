package com.guardiao.arquivos.ignore

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Arquivo que o usuário mandou ignorar nas próximas varreduras.
 *
 * O caminho é a chave: é ele que a varredura consulta. Nome e data ficam guardados para a tela de
 * revisão poder mostrar o que foi ignorado e quando, sem depender do arquivo ainda existir.
 */
@Entity(tableName = "ignored_files")
data class IgnoredFile(
  @PrimaryKey val path: String,
  val fileName: String,
  val ignoredAt: Long = System.currentTimeMillis(),
)

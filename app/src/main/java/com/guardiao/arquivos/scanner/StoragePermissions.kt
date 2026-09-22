package com.guardiao.arquivos.scanner

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

/**
 * Permissões necessárias para ler todos os arquivos e movê-los para a quarentena.
 *
 * - Android 11+ (API 30): acesso a todos os arquivos (`MANAGE_EXTERNAL_STORAGE`), concedido em uma
 *   tela do sistema.
 * - Android 7 a 10: permissões de leitura e escrita do armazenamento em tempo de execução.
 */
object StoragePermissions {

  val legacyPermissions: Array<String> =
    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)

  val usesAllFilesAccess: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

  fun hasFullAccess(context: Context): Boolean =
    if (usesAllFilesAccess) {
      Environment.isExternalStorageManager()
    } else {
      legacyPermissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
      }
    }

  /** Intent que abre a tela do sistema para conceder acesso a todos os arquivos (API 30+). */
  @RequiresApi(Build.VERSION_CODES.R)
  fun allFilesAccessIntent(context: Context): Intent {
    val specific =
      Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${context.packageName}"))
    return if (specific.resolveActivity(context.packageManager) != null) {
      specific
    } else {
      Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
    }
  }
}

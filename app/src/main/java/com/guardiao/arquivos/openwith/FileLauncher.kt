package com.guardiao.arquivos.openwith

import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Build

/** Resultado da tentativa de abrir um arquivo, para a tela dar o retorno certo ao usuário. */
enum class OpenOutcome {
  /** Abriu direto no app já lembrado para este tipo. */
  OPENED_WITH_REMEMBERED,
  /** Mostrou o seletor: é a primeira vez com este tipo, ou o app lembrado sumiu. */
  ASKED,
  /** Nenhum app instalado abre este tipo de arquivo. */
  NO_APP,
}

/**
 * Abre arquivos em outros apps, lembrando a escolha por tipo.
 *
 * Na primeira vez que um tipo é aberto, mostra o seletor do sistema e pede que ele avise qual app
 * foi escolhido. Da vez seguinte, abre direto nesse app. Se o app tiver sido desinstalado ou
 * deixar de abrir o tipo, a escolha é esquecida e o seletor volta a aparecer.
 *
 * Precisa do contexto da Activity: iniciar uma tela a partir do contexto do aplicativo exigiria
 * uma nova tarefa e tiraria o app da pilha de volta.
 */
class FileLauncher(private val context: Context, private val preferences: OpenWithPreferences) {

  fun open(uri: Uri, mimeType: String): OpenOutcome {
    val base =
      Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }

    preferences.componentFor(mimeType)?.let { remembered ->
      val direct = Intent(base).setComponent(remembered)
      try {
        context.startActivity(direct)
        return OpenOutcome.OPENED_WITH_REMEMBERED
      } catch (_: ActivityNotFoundException) {
        // App desinstalado ou já não declara este tipo: esquece e volta a perguntar.
        preferences.forget(mimeType)
      } catch (_: SecurityException) {
        preferences.forget(mimeType)
      }
    }

    val chooser =
      Intent.createChooser(base, "Abrir com").apply {
        putExtra(Intent.EXTRA_CHOSEN_COMPONENT_INTENT_SENDER, chosenComponentSender(mimeType))
      }
    return try {
      context.startActivity(chooser)
      OpenOutcome.ASKED
    } catch (_: ActivityNotFoundException) {
      OpenOutcome.NO_APP
    }
  }

  /** IntentSender que o seletor dispara informando o app escolhido. */
  private fun chosenComponentSender(mimeType: String): IntentSender {
    val callback =
      Intent(context, ChosenComponentReceiver::class.java)
        .putExtra(ChosenComponentReceiver.EXTRA_MIME_TYPE, mimeType)
    // O sistema preenche EXTRA_CHOSEN_COMPONENT, então o PendingIntent precisa ser mutável.
    val mutability = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
    return PendingIntent.getBroadcast(
        context,
        mimeType.hashCode(),
        callback,
        PendingIntent.FLAG_UPDATE_CURRENT or mutability,
      )
      .intentSender
  }
}

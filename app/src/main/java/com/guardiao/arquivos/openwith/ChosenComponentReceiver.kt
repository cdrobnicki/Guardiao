package com.guardiao.arquivos.openwith

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.IntentCompat

/**
 * Recebe do sistema qual app o usuário escolheu no seletor "Abrir com" e guarda a escolha.
 *
 * O seletor do Android não devolve a escolha para quem o abriu; o único jeito de saber é pedir ao
 * sistema que avise, passando um IntentSender em `EXTRA_CHOSEN_COMPONENT_INTENT_SENDER`. É esse
 * aviso que chega aqui.
 */
class ChosenComponentReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    val mimeType = intent.getStringExtra(EXTRA_MIME_TYPE) ?: return
    val chosen =
      IntentCompat.getParcelableExtra(intent, Intent.EXTRA_CHOSEN_COMPONENT, ComponentName::class.java)
        ?: return
    OpenWithPreferences(context).remember(mimeType, chosen)
  }

  companion object {
    const val EXTRA_MIME_TYPE = "com.guardiao.arquivos.EXTRA_MIME_TYPE"
  }
}

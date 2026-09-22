package com.guardiao.arquivos.openwith

import android.content.ComponentName
import android.content.Context
import androidx.core.content.edit

/**
 * Guarda qual app o usuário escolheu para abrir cada tipo de arquivo.
 *
 * A chave é o tipo MIME — o mesmo critério que o Android usa para "abrir por padrão" — então
 * escolher um leitor para `application/pdf` vale para todos os PDFs, e não para imagens.
 *
 * Fica em SharedPreferences porque o receptor da escolha ([ChosenComponentReceiver]) roda fora da
 * tela e precisa gravar sem depender do ViewModel.
 */
class OpenWithPreferences(context: Context) {

  private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

  /** Quantos tipos de arquivo já têm um app lembrado. */
  val rememberedCount: Int
    get() = prefs.all.size

  /** Lista legível dos tipos lembrados, para a tela de ajustes. */
  fun rememberedTypes(): List<String> = prefs.all.keys.sorted()

  fun componentFor(mimeType: String): ComponentName? =
    prefs.getString(key(mimeType), null)?.let(ComponentName::unflattenFromString)

  fun remember(mimeType: String, component: ComponentName) {
    prefs.edit { putString(key(mimeType), component.flattenToString()) }
  }

  /** Esquece um tipo — usado quando o app lembrado não abre mais o arquivo. */
  fun forget(mimeType: String) {
    prefs.edit { remove(key(mimeType)) }
  }

  fun forgetAll() {
    prefs.edit { clear() }
  }

  private fun key(mimeType: String) = mimeType.lowercase()

  private companion object {
    const val FILE = "guardiao_abrir_com"
  }
}

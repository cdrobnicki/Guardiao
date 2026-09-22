package com.guardiao.arquivos

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.memory.MemoryCache
import coil.request.CachePolicy

/**
 * Configura o carregador de miniaturas usado nas listas de fotos e vídeos.
 *
 * O cache em disco fica **desligado** de propósito: gravar miniaturas das fotos do usuário no
 * diretório de cache do app criaria uma segunda cópia de conteúdo pessoal no armazenamento, o que
 * contraria o objetivo do app. As miniaturas ficam apenas em memória e desaparecem ao fechar.
 */
class GuardiaoApp : Application(), ImageLoaderFactory {

  override fun newImageLoader(): ImageLoader =
    ImageLoader.Builder(this)
      // Permite extrair um quadro de vídeos, além de decodificar imagens.
      .components { add(VideoFrameDecoder.Factory()) }
      .memoryCache { MemoryCache.Builder(this@GuardiaoApp).maxSizePercent(0.15).build() }
      .diskCachePolicy(CachePolicy.DISABLED)
      .crossfade(true)
      .build()
}

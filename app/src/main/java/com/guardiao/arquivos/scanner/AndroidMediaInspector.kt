package com.guardiao.arquivos.scanner

import android.media.MediaMetadataRetriever
import androidx.exifinterface.media.ExifInterface
import java.io.File

/**
 * Lê metadados de fotos (EXIF), vídeos e áudios usando as APIs do Android.
 *
 * Fotos: coordenadas GPS, modelo do aparelho e artista/autor.
 * Vídeos: localização e modelo do aparelho.
 * Áudios: artista/álbum (indica música) e duração.
 */
class AndroidMediaInspector : MediaInspector {

  override fun inspect(file: File, category: FileCategory): MediaMetadata? =
    when (category) {
      FileCategory.IMAGE -> inspectImage(file)
      FileCategory.VIDEO, FileCategory.AUDIO -> inspectAv(file, category)
      else -> null
    }

  private fun inspectImage(file: File): MediaMetadata? {
    val extension = FileCategory.extensionOf(file.name)
    // Formatos sem EXIF não precisam ser abertos.
    if (extension in setOf("gif", "bmp", "svg")) return null
    return try {
      val exif = ExifInterface(file)
      val latLong = exif.latLong
      val make = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim()
      val model = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim()
      val artist = exif.getAttribute(ExifInterface.TAG_ARTIST)?.trim()
      val device =
        listOfNotNull(make, model)
          .filter { it.isNotBlank() }
          .distinct()
          .joinToString(" ")
          .ifBlank { null }
      MediaMetadata(
        hasGpsLocation = latLong != null,
        deviceModel = device,
        author = artist?.ifBlank { null },
      )
    } catch (_: Exception) {
      null
    }
  }

  private fun inspectAv(file: File, category: FileCategory): MediaMetadata? {
    val retriever = MediaMetadataRetriever()
    return try {
      retriever.setDataSource(file.absolutePath)
      val location = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION)
      val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.trim()
      val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.trim()
      val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.trim()
      val author = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR)?.trim()
      val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
      val hasMusicTags =
        category == FileCategory.AUDIO && (!artist.isNullOrBlank() || !album.isNullOrBlank()) && !title.isNullOrBlank()
      MediaMetadata(
        hasGpsLocation = !location.isNullOrBlank(),
        deviceModel = null,
        author = (artist ?: author)?.ifBlank { null },
        hasMusicTags = hasMusicTags,
        durationMillis = duration,
      )
    } catch (_: Exception) {
      null
    } finally {
      try {
        retriever.release()
      } catch (_: Exception) {
        // ignorado
      }
    }
  }
}

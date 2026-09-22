package com.guardiao.arquivos.scanner

import java.io.File

/** Metadados relevantes extraídos de fotos, vídeos e áudios. */
data class MediaMetadata(
  val hasGpsLocation: Boolean = false,
  val deviceModel: String? = null,
  val author: String? = null,
  val hasMusicTags: Boolean = false,
  val durationMillis: Long? = null,
)

/** Abstração para leitura de metadados de mídia (implementada com APIs Android). */
fun interface MediaInspector {
  fun inspect(file: File, category: FileCategory): MediaMetadata?
}

/**
 * Combina todas as heurísticas (nome, pasta, conteúdo e metadados) em uma [PrivacyAnalysis].
 *
 * Toda a análise é feita localmente. Nenhum conteúdo sai do aparelho.
 */
class PrivacyAnalyzer(private val mediaInspector: MediaInspector? = null) {

  fun analyze(file: File, category: FileCategory): PrivacyAnalysis {
    val extension = FileCategory.extensionOf(file.name)
    val findings = mutableListOf<Finding>()

    findings += FilenameHeuristics.analyze(file.name, file.parent ?: "", category)

    when (category) {
      FileCategory.PDF,
      FileCategory.DOCUMENT,
      FileCategory.SPREADSHEET,
      FileCategory.PRESENTATION -> {
        val text = TextExtractor.extract(file, extension)
        if (text != null) findings += PatternDetectors.detect(text)
      }
      FileCategory.IMAGE,
      FileCategory.VIDEO,
      FileCategory.AUDIO -> {
        val metadata = runCatching { mediaInspector?.inspect(file, category) }.getOrNull()
        if (metadata != null) findings += analyzeMetadata(metadata, category)
      }
    }

    return PrivacyAnalysis(mergeFindings(findings))
  }

  /** Converte metadados de mídia em indícios. */
  fun analyzeMetadata(metadata: MediaMetadata, category: FileCategory): List<Finding> {
    val findings = mutableListOf<Finding>()
    if (metadata.hasGpsLocation) {
      findings += Finding(FindingType.GPS_LOCATION, "Contém coordenadas GPS de onde foi registrado", 35)
    }
    if (!metadata.deviceModel.isNullOrBlank()) {
      findings += Finding(FindingType.DEVICE_METADATA, "Registrado com o aparelho ${metadata.deviceModel}", 10)
    }
    if (!metadata.author.isNullOrBlank() && !metadata.hasMusicTags) {
      findings += Finding(FindingType.AUTHOR_METADATA, "Autor/artista nos metadados: ${metadata.author}", 10)
    }
    if (category == FileCategory.AUDIO && !metadata.hasMusicTags) {
      val duration = metadata.durationMillis
      // Músicas costumam ter tags de álbum/artista; áudios curtos sem tags tendem a ser gravações.
      if (duration != null && duration in 1_000..(20 * 60 * 1_000L)) {
        findings += Finding(FindingType.VOICE_RECORDING, "Áudio sem tags de música (provável gravação de voz)", 15)
      }
    }
    return findings
  }

  /** Mantém apenas o indício de maior peso para cada tipo, evitando somas infladas. */
  private fun mergeFindings(findings: List<Finding>): List<Finding> =
    findings
      .groupBy { it.type }
      .map { (_, group) -> group.maxBy { it.weight } }
      .sortedByDescending { it.weight }
}

package com.guardiao.arquivos.scanner

/** Nível de risco de um arquivo conter informações pessoais. */
enum class RiskLevel(val label: String, val minScore: Int) {
  NONE("Sem indícios", 0),
  LOW("Baixo", 10),
  MEDIUM("Médio", 30),
  HIGH("Alto", 60);

  companion object {
    fun fromScore(score: Int): RiskLevel = entries.last { score >= it.minScore }
  }
}

/** Tipo de indício de informação pessoal encontrado. */
enum class FindingType(val label: String) {
  CPF("CPF"),
  CNPJ("CNPJ"),
  RG("RG"),
  CREDIT_CARD("Número de cartão"),
  EMAIL("E-mail"),
  PHONE("Telefone"),
  CEP("CEP"),
  PASSWORD("Senha ou credencial"),
  BANK("Dados bancários"),
  HEALTH("Dados de saúde"),
  BIRTH_DATE("Data de nascimento"),
  DOCUMENT_KEYWORD("Palavra-chave de documento"),
  FILENAME_HINT("Nome do arquivo"),
  PATH_HINT("Pasta de origem"),
  GPS_LOCATION("Localização GPS"),
  DEVICE_METADATA("Metadados do aparelho"),
  AUTHOR_METADATA("Autor nos metadados"),
  VOICE_RECORDING("Gravação de voz"),
  SCREENSHOT("Captura de tela"),
  CAMERA_PHOTO("Foto da câmera"),
  MESSAGING_MEDIA("Mídia de mensageiro"),
}

/** Um indício concreto encontrado em um arquivo. */
data class Finding(val type: FindingType, val description: String, val weight: Int)

/** Resultado da análise de privacidade de um arquivo. */
data class PrivacyAnalysis(val findings: List<Finding>) {
  val score: Int = findings.sumOf { it.weight }.coerceIn(0, 100)
  val level: RiskLevel = RiskLevel.fromScore(score)
  val hasPersonalInfo: Boolean
    get() = level != RiskLevel.NONE

  companion object {
    val EMPTY = PrivacyAnalysis(emptyList())
  }
}

/** Um arquivo encontrado na varredura. */
data class ScannedFile(
  val path: String,
  val name: String,
  val extension: String,
  val category: FileCategory,
  val mimeType: String,
  val sizeBytes: Long,
  val lastModified: Long,
  val analysis: PrivacyAnalysis,
) {
  val parentPath: String
    get() = path.substringBeforeLast('/', "")

  val riskLevel: RiskLevel
    get() = analysis.level
}

/** Progresso da varredura em andamento. */
data class ScanProgress(
  val scannedFiles: Int,
  val matchedFiles: Int,
  val currentPath: String,
  val finished: Boolean = false,
)

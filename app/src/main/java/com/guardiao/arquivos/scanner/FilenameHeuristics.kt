package com.guardiao.arquivos.scanner

import java.text.Normalizer

/**
 * Heurísticas baseadas no nome do arquivo e na pasta onde ele está.
 *
 * Não abre o arquivo: serve para todas as categorias, inclusive fotos e vídeos cujo conteúdo não
 * pode ser lido como texto.
 */
object FilenameHeuristics {

  private class Keyword(val term: String, val weight: Int, val description: String)

  private val strongKeywords =
    listOf(
      Keyword("cpf", 40, "documento de CPF"),
      Keyword("rg", 40, "documento de identidade (RG)"),
      Keyword("cnh", 40, "carteira de motorista (CNH)"),
      Keyword("habilitacao", 40, "carteira de motorista"),
      Keyword("passaporte", 40, "passaporte"),
      Keyword("passport", 40, "passaporte"),
      Keyword("identidade", 40, "documento de identidade"),
      Keyword("titulo de eleitor", 35, "título de eleitor"),
      Keyword("titulo eleitor", 35, "título de eleitor"),
      Keyword("certidao", 35, "certidão"),
      Keyword("ctps", 35, "carteira de trabalho"),
      Keyword("carteira de trabalho", 35, "carteira de trabalho"),
      Keyword("senha", 45, "senha"),
      Keyword("senhas", 45, "senhas"),
      Keyword("password", 45, "senha"),
      Keyword("credencia", 40, "credenciais"),
      Keyword("contracheque", 35, "contracheque"),
      Keyword("holerite", 35, "holerite"),
      Keyword("imposto de renda", 40, "declaração de imposto de renda"),
      Keyword("irpf", 40, "declaração de imposto de renda"),
      Keyword("declaracao", 25, "declaração"),
      Keyword("extrato", 35, "extrato bancário"),
      Keyword("comprovante", 35, "comprovante"),
      Keyword("boleto", 30, "boleto"),
      Keyword("fatura", 30, "fatura"),
      Keyword("contrato", 30, "contrato"),
      Keyword("procuracao", 35, "procuração"),
      Keyword("laudo", 35, "laudo médico"),
      Keyword("exame", 30, "exame médico"),
      Keyword("receita", 25, "receita"),
      Keyword("atestado", 35, "atestado"),
      Keyword("prontuario", 40, "prontuário"),
      Keyword("curriculo", 35, "currículo"),
      Keyword("curriculum", 35, "currículo"),
      Keyword("cv", 25, "currículo"),
      Keyword("residencia", 30, "comprovante de residência"),
      Keyword("nota fiscal", 25, "nota fiscal"),
      Keyword("nfe", 20, "nota fiscal"),
      Keyword("assinatura", 30, "assinatura"),
      Keyword("biometria", 40, "biometria"),
      Keyword("selfie", 30, "selfie"),
      Keyword("cartao", 30, "cartão"),
      Keyword("card", 20, "cartão"),
      Keyword("certificado", 25, "certificado"),
      Keyword("diploma", 25, "diploma"),
      Keyword("historico escolar", 30, "histórico escolar"),
      Keyword("carteirinha", 30, "carteirinha"),
      Keyword("plano de saude", 30, "plano de saúde"),
      Keyword("sus", 20, "cartão SUS"),
      Keyword("backup", 20, "backup"),
      Keyword("chaves", 25, "chaves"),
      Keyword("keys", 25, "chaves"),
      Keyword("wallet", 30, "carteira digital"),
      Keyword("seed", 25, "frase de recuperação"),
    )

  private val mediumKeywords =
    listOf(
      Keyword("documento", 20, "documento"),
      Keyword("documentos", 20, "documentos"),
      Keyword("doc", 10, "documento"),
      Keyword("digitalizado", 20, "documento digitalizado"),
      Keyword("scan", 20, "documento digitalizado"),
      Keyword("scanner", 20, "documento digitalizado"),
      Keyword("camscanner", 25, "documento digitalizado"),
      Keyword("adobe scan", 25, "documento digitalizado"),
      Keyword("conversa", 20, "conversa"),
      Keyword("chat", 15, "conversa"),
      Keyword("endereco", 20, "endereço"),
      Keyword("telefone", 15, "telefone"),
      Keyword("contatos", 20, "contatos"),
      Keyword("contacts", 20, "contatos"),
      Keyword("agenda", 15, "agenda"),
      Keyword("financeiro", 20, "financeiro"),
      Keyword("financas", 20, "finanças"),
      Keyword("pagamento", 20, "pagamento"),
      Keyword("recibo", 20, "recibo"),
      Keyword("pedido", 10, "pedido"),
      Keyword("cadastro", 20, "cadastro"),
      Keyword("formulario", 15, "formulário"),
      Keyword("ficha", 15, "ficha"),
      Keyword("matricula", 20, "matrícula"),
      Keyword("medico", 20, "médico"),
      Keyword("hospital", 20, "hospital"),
      Keyword("banco", 20, "banco"),
      Keyword("nubank", 25, "banco"),
      Keyword("itau", 25, "banco"),
      Keyword("bradesco", 25, "banco"),
      Keyword("santander", 25, "banco"),
      Keyword("caixa", 15, "banco"),
      Keyword("sicoob", 25, "banco"),
      Keyword("inter", 10, "banco"),
      Keyword("picpay", 25, "banco"),
      Keyword("pix", 25, "Pix"),
      Keyword("transferencia", 20, "transferência"),
      Keyword("salario", 25, "salário"),
      Keyword("folha", 10, "folha de pagamento"),
      Keyword("familia", 15, "família"),
      Keyword("pessoal", 15, "pessoal"),
      Keyword("privado", 20, "privado"),
      Keyword("private", 20, "privado"),
      Keyword("confidencial", 30, "confidencial"),
      Keyword("sigiloso", 30, "sigiloso"),
      Keyword("intimo", 30, "íntimo"),
      Keyword("nude", 30, "íntimo"),
    )

  /** Analisa nome e caminho e devolve os indícios encontrados. */
  fun analyze(fileName: String, parentPath: String, category: FileCategory): List<Finding> {
    val findings = mutableListOf<Finding>()
    val normalizedName = normalize(fileName.substringBeforeLast('.'))
    val nameTokens = tokenize(normalizedName)
    val normalizedPath = normalize(parentPath)

    findings += keywordFindings(normalizedName, nameTokens, FindingType.FILENAME_HINT, "Nome do arquivo sugere")
    findings += pathFindings(normalizedPath)
    findings += patternFindings(fileName, category, normalizedPath)

    return findings
  }

  private fun keywordFindings(
    normalized: String,
    tokens: Set<String>,
    type: FindingType,
    prefix: String,
  ): List<Finding> {
    val hits = mutableListOf<Keyword>()
    for (keyword in strongKeywords + mediumKeywords) {
      val matches =
        if (keyword.term.contains(' ')) normalized.contains(keyword.term) else tokens.contains(keyword.term)
      if (matches) hits += keyword
    }
    if (hits.isEmpty()) return emptyList()
    val best = hits.maxBy { it.weight }
    val extra = (hits.size - 1).coerceAtMost(3) * 5
    val descriptions = hits.sortedByDescending { it.weight }.map { it.description }.distinct().take(3)
    return listOf(Finding(type, "$prefix ${descriptions.joinToString(", ")}", best.weight + extra))
  }

  private fun pathFindings(path: String): List<Finding> {
    val findings = mutableListOf<Finding>()
    val segments = path.split('/').filter { it.isNotBlank() }.toSet()

    when {
      path.contains("/whatsapp") && (path.contains("voice notes") || path.contains("ptt")) ->
        findings += Finding(FindingType.VOICE_RECORDING, "Mensagem de voz do WhatsApp", 35)
      path.contains("/whatsapp") && path.contains("documents") ->
        findings += Finding(FindingType.MESSAGING_MEDIA, "Documento recebido ou enviado pelo WhatsApp", 25)
      path.contains("/whatsapp") && path.contains("profile") ->
        findings += Finding(FindingType.MESSAGING_MEDIA, "Foto de perfil do WhatsApp", 30)
      path.contains("/whatsapp") ->
        findings += Finding(FindingType.MESSAGING_MEDIA, "Mídia trocada pelo WhatsApp", 10)
      path.contains("/telegram") ->
        findings += Finding(FindingType.MESSAGING_MEDIA, "Mídia trocada pelo Telegram", 10)
      path.contains("/signal") ->
        findings += Finding(FindingType.MESSAGING_MEDIA, "Mídia trocada pelo Signal", 10)
    }

    if (segments.any { it == "screenshots" || it == "capturas de tela" || it == "screenshot" }) {
      findings += Finding(FindingType.SCREENSHOT, "Capturas de tela costumam mostrar conversas e dados de apps", 20)
    }

    if (path.contains("/dcim/camera") || segments.contains("camera") || path.contains("/dcim/100")) {
      findings += Finding(FindingType.CAMERA_PHOTO, "Registro feito pela câmera do aparelho", 10)
    }

    val recordingFolders =
      listOf("recordings", "recorder", "voice recorder", "gravador", "gravacoes", "sound recorder", "call", "callrecord", "call recordings", "record")
    if (segments.any { seg -> recordingFolders.any { seg == it || seg.contains("record") || seg.contains("gravad") } }) {
      findings += Finding(FindingType.VOICE_RECORDING, "Pasta de gravações de voz ou chamadas", 35)
    }

    if (segments.contains("documents") || segments.contains("documentos")) {
      findings += Finding(FindingType.PATH_HINT, "Arquivo na pasta de documentos", 10)
    }

    val keywordHits = keywordFindings(path, segments.flatMap { tokenize(it) }.toSet(), FindingType.PATH_HINT, "Pasta sugere")
    keywordHits.forEach { hit -> findings += hit.copy(weight = (hit.weight / 2).coerceAtLeast(5)) }

    return findings
  }

  private fun patternFindings(fileName: String, category: FileCategory, normalizedPath: String): List<Finding> {
    val findings = mutableListOf<Finding>()
    val upper = fileName.uppercase()
    when (category) {
      FileCategory.AUDIO -> {
        when {
          upper.startsWith("PTT-") -> findings += Finding(FindingType.VOICE_RECORDING, "Mensagem de voz (padrão PTT do WhatsApp)", 35)
          upper.startsWith("AUD-") -> findings += Finding(FindingType.MESSAGING_MEDIA, "Áudio recebido por mensageiro", 20)
          upper.contains("REC") || upper.contains("GRAV") || upper.contains("VOICE") || upper.contains("CALL") || upper.contains("CHAMADA") ->
            findings += Finding(FindingType.VOICE_RECORDING, "Nome indica gravação de voz ou chamada", 30)
        }
      }
      FileCategory.IMAGE -> {
        when {
          upper.startsWith("SCREENSHOT") || upper.contains("CAPTURA") ->
            if (!normalizedPath.contains("screenshot")) {
              findings += Finding(FindingType.SCREENSHOT, "Captura de tela", 20)
            }
          upper.matches(Regex("IMG-\\d{8}-WA\\d+.*")) ->
            if (!normalizedPath.contains("whatsapp")) {
              findings += Finding(FindingType.MESSAGING_MEDIA, "Imagem recebida pelo WhatsApp", 10)
            }
          upper.startsWith("IMG_") || upper.startsWith("PXL_") || upper.startsWith("DSC") ->
            if (!normalizedPath.contains("camera")) {
              findings += Finding(FindingType.CAMERA_PHOTO, "Foto tirada com a câmera", 10)
            }
        }
      }
      FileCategory.VIDEO -> {
        if (upper.matches(Regex("VID-\\d{8}-WA\\d+.*")) && !normalizedPath.contains("whatsapp")) {
          findings += Finding(FindingType.MESSAGING_MEDIA, "Vídeo recebido pelo WhatsApp", 10)
        } else if ((upper.startsWith("VID_") || upper.startsWith("PXL_")) && !normalizedPath.contains("camera")) {
          findings += Finding(FindingType.CAMERA_PHOTO, "Vídeo gravado com a câmera", 10)
        }
      }
      else -> Unit
    }
    return findings
  }

  /** Remove acentos, converte para minúsculas e troca separadores por espaço. */
  fun normalize(value: String): String {
    val decomposed = Normalizer.normalize(value, Normalizer.Form.NFD)
    val stripped = decomposed.replace(Regex("\\p{M}+"), "")
    return stripped.lowercase().replace(Regex("[_\\-.,()\\[\\]]+"), " ").replace(Regex("\\s+"), " ").trim()
  }

  private fun tokenize(normalized: String): Set<String> =
    normalized.split(' ', '/').filter { it.isNotBlank() }.toSet()
}

package com.example.scanner

/**
 * Detecção de padrões de informações pessoais em texto livre.
 *
 * Todas as verificações rodam localmente no aparelho e são heurísticas: elas apontam indícios,
 * não certezas. Números de CPF, CNPJ e cartão passam por validação de dígitos verificadores para
 * reduzir falsos positivos.
 */
object PatternDetectors {

  private val cpfRegex = Regex("""(?<!\d)\d{3}\.?\d{3}\.?\d{3}-?\d{2}(?!\d)""")
  private val cnpjRegex = Regex("""(?<!\d)\d{2}\.?\d{3}\.?\d{3}/?\d{4}-?\d{2}(?!\d)""")
  private val cardRegex = Regex("""(?<![\d-])(?:\d[ -]?){12,18}\d(?![\d-])""")
  private val emailRegex = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""")
  private val phoneRegex =
    Regex(
      """(?<!\d)(?:\+55[\s-]?\(?\d{2}\)?[\s-]?9?\d{4}[\s-]?\d{4}|\(\d{2}\)[\s-]?9?\d{4}[\s-]?\d{4}|\d{2}[\s-]9?\d{4}[\s-]\d{4}|9\d{4}-\d{4})(?!\d)"""
    )
  private val cepRegex = Regex("""(?<!\d)\d{5}-\d{3}(?!\d)""")
  private val rgRegex = Regex("""(?<!\d)\d{1,2}\.\d{3}\.\d{3}-?[\dXx](?![\d])""")
  private val passwordRegex =
    Regex("""(?i)\b(senha|password|passwd|pwd|pin|token|api[_ -]?key|secret)\b\s*[:=]\s*\S+""")
  private val birthDateRegex =
    Regex("""(?i)(data\s+de\s+nascimento|nascid[oa]\s+em|date\s+of\s+birth|\bdob\b)""")

  private val bankKeywords =
    listOf(
      "agência",
      "agencia",
      "conta corrente",
      "conta poupança",
      "conta poupanca",
      "chave pix",
      "pix",
      "extrato",
      "saldo",
      "fatura",
      "boleto",
      "iban",
      "swift",
      "cartão de crédito",
      "cartao de credito",
      "validade",
      "cvv",
    )

  private val healthKeywords =
    listOf(
      "prontuário",
      "prontuario",
      "diagnóstico",
      "diagnostico",
      "receita médica",
      "receita medica",
      "laudo",
      "exame",
      "atestado",
      "cid-",
      "cid ",
      "medicamento",
      "paciente",
      "plano de saúde",
      "plano de saude",
      "convênio",
      "convenio",
    )

  private val documentKeywords =
    listOf(
      "cpf",
      "rg",
      "cnh",
      "carteira de identidade",
      "passaporte",
      "título de eleitor",
      "titulo de eleitor",
      "certidão",
      "certidao",
      "contrato",
      "procuração",
      "procuracao",
      "comprovante",
      "holerite",
      "contracheque",
      "imposto de renda",
      "declaração",
      "declaracao",
      "endereço",
      "endereco",
      "filiação",
      "filiacao",
      "nome da mãe",
      "nome da mae",
      "estado civil",
      "nacionalidade",
      "naturalidade",
      "assinatura",
      "currículo",
      "curriculo",
      "curriculum",
      "matrícula",
      "matricula",
      "nis",
      "pis",
      "pasep",
    )

  /** Analisa [text] e devolve a lista de indícios encontrados. */
  fun detect(text: String): List<Finding> {
    if (text.isBlank()) return emptyList()
    val findings = mutableListOf<Finding>()

    val cpfs = cpfRegex.findAll(text).map { it.value }.filter { isValidCpf(it) }.toList()
    if (cpfs.isNotEmpty()) {
      findings += Finding(FindingType.CPF, occurrences("CPF válido encontrado", cpfs.size, maskDigits(cpfs.first())), 45)
    }

    val cnpjs = cnpjRegex.findAll(text).map { it.value }.filter { isValidCnpj(it) }.toList()
    if (cnpjs.isNotEmpty()) {
      findings += Finding(FindingType.CNPJ, occurrences("CNPJ válido encontrado", cnpjs.size, maskDigits(cnpjs.first())), 20)
    }

    val cards =
      cardRegex
        .findAll(text)
        .map { it.value }
        .filter { candidate ->
          val digits = candidate.filter(Char::isDigit)
          digits.length in 13..19 && digits[0] in "3456" && luhnValid(digits) && !cpfs.contains(candidate)
        }
        .toList()
    if (cards.isNotEmpty()) {
      findings +=
        Finding(
          FindingType.CREDIT_CARD,
          occurrences("Número com formato de cartão (Luhn válido)", cards.size, maskDigits(cards.first())),
          50,
        )
    }

    val emails = emailRegex.findAll(text).map { it.value }.distinct().toList()
    if (emails.isNotEmpty()) {
      findings += Finding(FindingType.EMAIL, occurrences("Endereço de e-mail", emails.size, maskEmail(emails.first())), 15)
    }

    val phones = phoneRegex.findAll(text).map { it.value }.distinct().toList()
    if (phones.isNotEmpty()) {
      findings += Finding(FindingType.PHONE, occurrences("Número de telefone", phones.size, maskDigits(phones.first())), 15)
    }

    val ceps = cepRegex.findAll(text).map { it.value }.distinct().toList()
    if (ceps.isNotEmpty()) {
      findings += Finding(FindingType.CEP, occurrences("CEP (endereço)", ceps.size, null), 10)
    }

    val rgs = rgRegex.findAll(text).map { it.value }.distinct().toList()
    if (rgs.isNotEmpty()) {
      findings += Finding(FindingType.RG, occurrences("Número com formato de RG", rgs.size, maskDigits(rgs.first())), 25)
    }

    if (passwordRegex.containsMatchIn(text)) {
      findings += Finding(FindingType.PASSWORD, "Senha, token ou credencial escrita no conteúdo", 45)
    }

    if (birthDateRegex.containsMatchIn(text)) {
      findings += Finding(FindingType.BIRTH_DATE, "Menção a data de nascimento", 15)
    }

    val lower = text.lowercase()

    val bankHits = bankKeywords.filter { lower.contains(it) }
    if (bankHits.isNotEmpty()) {
      findings += Finding(FindingType.BANK, "Termos bancários: ${bankHits.take(3).joinToString(", ")}", 15)
    }

    val healthHits = healthKeywords.filter { lower.contains(it) }
    if (healthHits.isNotEmpty()) {
      findings += Finding(FindingType.HEALTH, "Termos de saúde: ${healthHits.take(3).joinToString(", ")}", 20)
    }

    val docHits = documentKeywords.filter { containsWord(lower, it) }
    if (docHits.isNotEmpty()) {
      findings +=
        Finding(
          FindingType.DOCUMENT_KEYWORD,
          "Termos de documento pessoal: ${docHits.take(4).joinToString(", ")}",
          (10 + 5 * docHits.size).coerceAtMost(30),
        )
    }

    return findings
  }

  /** Valida os dígitos verificadores de um CPF (aceita com ou sem pontuação). */
  fun isValidCpf(value: String): Boolean {
    val digits = value.filter(Char::isDigit)
    if (digits.length != 11) return false
    if (digits.all { it == digits[0] }) return false
    val numbers = digits.map { it - '0' }
    val d1 = checkDigit(numbers.subList(0, 9), 10)
    val d2 = checkDigit(numbers.subList(0, 10), 11)
    return numbers[9] == d1 && numbers[10] == d2
  }

  private fun checkDigit(numbers: List<Int>, startWeight: Int): Int {
    var weight = startWeight
    val sum = numbers.sumOf { n -> (n * weight).also { weight-- } }
    val remainder = (sum * 10) % 11
    return if (remainder == 10) 0 else remainder
  }

  /** Valida os dígitos verificadores de um CNPJ (aceita com ou sem pontuação). */
  fun isValidCnpj(value: String): Boolean {
    val digits = value.filter(Char::isDigit)
    if (digits.length != 14) return false
    if (digits.all { it == digits[0] }) return false
    val numbers = digits.map { it - '0' }
    val weights1 = listOf(5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)
    val weights2 = listOf(6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)
    val d1 = cnpjDigit(numbers.subList(0, 12), weights1)
    val d2 = cnpjDigit(numbers.subList(0, 13), weights2)
    return numbers[12] == d1 && numbers[13] == d2
  }

  private fun cnpjDigit(numbers: List<Int>, weights: List<Int>): Int {
    val sum = numbers.indices.sumOf { numbers[it] * weights[it] }
    val remainder = sum % 11
    return if (remainder < 2) 0 else 11 - remainder
  }

  /** Algoritmo de Luhn usado por cartões de crédito. */
  fun luhnValid(digits: String): Boolean {
    if (digits.isEmpty() || !digits.all(Char::isDigit)) return false
    var sum = 0
    var alternate = false
    for (i in digits.indices.reversed()) {
      var n = digits[i] - '0'
      if (alternate) {
        n *= 2
        if (n > 9) n -= 9
      }
      sum += n
      alternate = !alternate
    }
    return sum % 10 == 0
  }

  private fun containsWord(haystack: String, word: String): Boolean {
    var index = haystack.indexOf(word)
    while (index >= 0) {
      val before = if (index == 0) ' ' else haystack[index - 1]
      val afterIndex = index + word.length
      val after = if (afterIndex >= haystack.length) ' ' else haystack[afterIndex]
      if (!before.isLetterOrDigit() && !after.isLetterOrDigit()) return true
      index = haystack.indexOf(word, index + 1)
    }
    return false
  }

  private fun occurrences(label: String, count: Int, sample: String?): String {
    val suffix = if (count > 1) " ($count ocorrências)" else ""
    val sampleText = if (sample != null) " · ex.: $sample" else ""
    return "$label$suffix$sampleText"
  }

  /** Mascara todos os dígitos exceto os 2 últimos, preservando pontuação. */
  fun maskDigits(value: String): String {
    val total = value.count(Char::isDigit)
    var seen = 0
    return buildString {
      for (c in value) {
        if (c.isDigit()) {
          seen++
          append(if (seen > total - 2) c else '*')
        } else append(c)
      }
    }
  }

  private fun maskEmail(email: String): String {
    val at = email.indexOf('@')
    if (at <= 1) return "***${email.substring(at.coerceAtLeast(0))}"
    return email.substring(0, 1) + "***" + email.substring(at)
  }
}

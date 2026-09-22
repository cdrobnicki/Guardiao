package com.guardiao.arquivos.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternDetectorsTest {

  @Test
  fun `valida cpf com digitos verificadores corretos`() {
    assertTrue(PatternDetectors.isValidCpf("529.982.247-25"))
    assertTrue(PatternDetectors.isValidCpf("52998224725"))
    assertFalse(PatternDetectors.isValidCpf("529.982.247-26"))
    assertFalse(PatternDetectors.isValidCpf("111.111.111-11"))
    assertFalse(PatternDetectors.isValidCpf("123"))
  }

  @Test
  fun `valida cnpj`() {
    assertTrue(PatternDetectors.isValidCnpj("11.222.333/0001-81"))
    assertFalse(PatternDetectors.isValidCnpj("11.222.333/0001-80"))
  }

  @Test
  fun `valida numero de cartao com luhn`() {
    assertTrue(PatternDetectors.luhnValid("4111111111111111"))
    assertFalse(PatternDetectors.luhnValid("4111111111111112"))
  }

  @Test
  fun `detecta cpf email telefone e senha em texto`() {
    val text =
      """
      Nome: Fulano de Tal
      CPF: 529.982.247-25
      E-mail: fulano@example.com
      Telefone: (11) 99999-8888
      senha: abc123
      """
        .trimIndent()
    val findings = PatternDetectors.detect(text)
    val types = findings.map { it.type }.toSet()
    assertTrue(types.contains(FindingType.CPF))
    assertTrue(types.contains(FindingType.EMAIL))
    assertTrue(types.contains(FindingType.PHONE))
    assertTrue(types.contains(FindingType.PASSWORD))
    val analysis = PrivacyAnalysis(findings)
    assertEquals(RiskLevel.HIGH, analysis.level)
  }

  @Test
  fun `ignora cpf invalido e numeros aleatorios`() {
    val text = "Pedido 123.456.789-00 total 1234 5678"
    val findings = PatternDetectors.detect(text)
    assertFalse(findings.any { it.type == FindingType.CPF })
    assertFalse(findings.any { it.type == FindingType.CREDIT_CARD })
  }

  @Test
  fun `detecta cartao de credito`() {
    val findings = PatternDetectors.detect("Cartão 4111 1111 1111 1111 validade 12/30")
    assertTrue(findings.any { it.type == FindingType.CREDIT_CARD })
    val card = findings.first { it.type == FindingType.CREDIT_CARD }
    assertFalse(card.description.contains("4111 1111 1111 1111"))
  }

  @Test
  fun `texto comum nao gera indicios`() {
    val findings = PatternDetectors.detect("Lista de compras: arroz, feijão, leite e pão.")
    assertTrue(findings.isEmpty())
  }

  @Test
  fun `mascara digitos preservando os dois ultimos`() {
    assertEquals("***.***.***-25", PatternDetectors.maskDigits("529.982.247-25"))
  }
}

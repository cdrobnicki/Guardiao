package com.guardiao.arquivos.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskOrderingTest {

  private fun file(name: String, findings: List<Finding>, modified: Long = 0L) =
    ScannedFile(
      path = "/storage/emulated/0/$name",
      name = name,
      extension = FileCategory.extensionOf(name),
      category = FileCategory.fromFileName(name) ?: FileCategory.DOCUMENT,
      mimeType = "application/octet-stream",
      sizeBytes = 100,
      lastModified = modified,
      analysis = PrivacyAnalysis(findings),
    )

  private fun finding(weight: Int) = Finding(FindingType.EMAIL, "teste", weight)

  @Test
  fun `maior risco vem primeiro`() {
    val baixo = file("a.pdf", listOf(finding(12)))
    val alto = file("b.pdf", listOf(finding(70)))
    val medio = file("c.pdf", listOf(finding(35)))
    val nenhum = file("d.pdf", emptyList())

    val ordenado = listOf(baixo, nenhum, alto, medio).sortedByRisk()

    assertEquals(listOf("b.pdf", "c.pdf", "a.pdf", "d.pdf"), ordenado.map { it.name })
    assertTrue(ordenado.first().analysis.score >= ordenado.last().analysis.score)
  }

  @Test
  fun `empate de pontuacao usa o mais recente`() {
    val antigo = file("antigo.pdf", listOf(finding(40)), modified = 1_000L)
    val recente = file("recente.pdf", listOf(finding(40)), modified = 9_000L)

    val ordenado = listOf(antigo, recente).sortedByRisk()

    assertEquals(listOf("recente.pdf", "antigo.pdf"), ordenado.map { it.name })
  }

  @Test
  fun `niveis derivam da pontuacao`() {
    assertEquals(RiskLevel.NONE, RiskLevel.fromScore(0))
    assertEquals(RiskLevel.LOW, RiskLevel.fromScore(10))
    assertEquals(RiskLevel.MEDIUM, RiskLevel.fromScore(30))
    assertEquals(RiskLevel.HIGH, RiskLevel.fromScore(60))
    assertEquals(RiskLevel.HIGH, RiskLevel.fromScore(100))
  }

  @Test
  fun `pontuacao nao passa de 100`() {
    val exagerado = file("x.pdf", List(6) { finding(40) })
    assertEquals(100, exagerado.analysis.score)
    assertEquals(RiskLevel.HIGH, exagerado.riskLevel)
  }

  @Test
  fun `lista vazia nao quebra`() {
    assertTrue(emptyList<ScannedFile>().sortedByRisk().isEmpty())
  }
}

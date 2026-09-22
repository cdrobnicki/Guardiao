package com.guardiao.arquivos.quarantine

import com.guardiao.arquivos.scanner.FileCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuarantineSortTest {

  private fun record(
    name: String,
    size: Long = 100,
    movedAt: Long = 0,
    category: FileCategory = FileCategory.DOCUMENT,
  ) =
    QuarantineRecord(
      fileName = name,
      originalPath = "/storage/emulated/0/$name",
      quarantinePath = "/storage/emulated/0/Quarentena/$name",
      category = category.name,
      riskLevel = "HIGH",
      sizeBytes = size,
      movedAt = movedAt,
    )

  @Test
  fun `ordem padrao traz o mais recente primeiro`() {
    val antigo = record("antigo.pdf", movedAt = 100)
    val recente = record("recente.pdf", movedAt = 900)

    assertEquals(
      listOf("recente.pdf", "antigo.pdf"),
      listOf(antigo, recente).sortedBy(QuarantineSort.RECENT).map { it.fileName },
    )
  }

  @Test
  fun `por tamanho traz o maior primeiro`() {
    val pequeno = record("pequeno.pdf", size = 10)
    val grande = record("grande.mp4", size = 9_000_000)
    val medio = record("medio.docx", size = 5_000)

    assertEquals(
      listOf("grande.mp4", "medio.docx", "pequeno.pdf"),
      listOf(pequeno, grande, medio).sortedBy(QuarantineSort.SIZE).map { it.fileName },
    )
  }

  @Test
  fun `por tipo agrupa na ordem das categorias`() {
    val doc = record("c.docx", category = FileCategory.DOCUMENT)
    val foto = record("a.jpg", category = FileCategory.IMAGE)
    val audio = record("b.mp3", category = FileCategory.AUDIO)

    val ordenado = listOf(doc, foto, audio).sortedBy(QuarantineSort.TYPE).map { it.fileName }

    // IMAGE vem antes de AUDIO, que vem antes de DOCUMENT, como em FileCategory.
    assertEquals(listOf("a.jpg", "b.mp3", "c.docx"), ordenado)
  }

  @Test
  fun `por nome ignora maiusculas`() {
    val registros = listOf(record("Zebra.pdf"), record("abacaxi.pdf"), record("Maçã.pdf"))

    assertEquals(
      listOf("abacaxi.pdf", "Maçã.pdf", "Zebra.pdf"),
      registros.sortedBy(QuarantineSort.NAME).map { it.fileName },
    )
  }

  @Test
  fun `empate desempata pelo nome, mantendo a ordem estavel`() {
    val b = record("b.pdf", size = 500)
    val a = record("a.pdf", size = 500)

    assertEquals(listOf("a.pdf", "b.pdf"), listOf(b, a).sortedBy(QuarantineSort.SIZE).map { it.fileName })
    assertEquals(listOf("a.pdf", "b.pdf"), listOf(b, a).sortedBy(QuarantineSort.RECENT).map { it.fileName })
  }

  @Test
  fun `categoria desconhecida vai para o fim e nao quebra`() {
    val valido = record("ok.jpg", category = FileCategory.IMAGE)
    val invalido = valido.copy(fileName = "estranho.xyz", category = "CATEGORIA_QUE_NAO_EXISTE")

    val ordenado = listOf(invalido, valido).sortedBy(QuarantineSort.TYPE).map { it.fileName }

    assertEquals(listOf("ok.jpg", "estranho.xyz"), ordenado)
  }

  @Test
  fun `lista vazia nao quebra em nenhum criterio`() {
    QuarantineSort.entries.forEach { assertTrue(emptyList<QuarantineRecord>().sortedBy(it).isEmpty()) }
  }
}

package com.guardiao.arquivos.quarantine

import com.guardiao.arquivos.scanner.FileCategory

/** Critérios de ordenação da lista de quarentena. */
enum class QuarantineSort(val label: String) {
  /** Padrão: o que foi movido por último aparece primeiro. */
  RECENT("Mais recentes"),

  /** Maior primeiro — é a ordem útil para quem quer liberar espaço. */
  SIZE("Tamanho"),

  /** Agrupa por categoria, na mesma ordem usada no resto do app. */
  TYPE("Tipo"),

  NAME("Nome"),
}

/**
 * Ordena os registros da quarentena.
 *
 * Todos os critérios desempatam pelo nome, para a lista não trocar de ordem entre aberturas
 * quando dois arquivos têm o mesmo tamanho, tipo ou instante.
 */
fun List<QuarantineRecord>.sortedBy(order: QuarantineSort): List<QuarantineRecord> {
  val byName = compareBy<QuarantineRecord> { it.fileName.lowercase() }
  return when (order) {
    QuarantineSort.RECENT -> sortedWith(compareByDescending<QuarantineRecord> { it.movedAt }.then(byName))
    QuarantineSort.SIZE -> sortedWith(compareByDescending<QuarantineRecord> { it.sizeBytes }.then(byName))
    QuarantineSort.TYPE -> sortedWith(compareBy<QuarantineRecord> { it.categoryOrder }.then(byName))
    QuarantineSort.NAME -> sortedWith(byName)
  }
}

/**
 * Posição da categoria na ordem declarada em [FileCategory]. Categorias desconhecidas — de um
 * registro gravado por uma versão anterior — vão para o fim, em vez de quebrar a ordenação.
 */
private val QuarantineRecord.categoryOrder: Int
  get() = runCatching { FileCategory.valueOf(category).ordinal }.getOrDefault(Int.MAX_VALUE)

package com.example.quarantine

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Registro de um arquivo movido para a quarentena, usado para listar e restaurar. */
@Entity(tableName = "quarantine")
data class QuarantineRecord(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val fileName: String,
  val originalPath: String,
  val quarantinePath: String,
  val category: String,
  val riskLevel: String,
  val sizeBytes: Long,
  val movedAt: Long = System.currentTimeMillis(),
)

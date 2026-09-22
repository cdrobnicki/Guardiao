package com.guardiao.arquivos.quarantine

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QuarantineDao {
  @Query("SELECT * FROM quarantine ORDER BY movedAt DESC") fun observeAll(): Flow<List<QuarantineRecord>>

  @Query("SELECT * FROM quarantine WHERE id = :id LIMIT 1") suspend fun findById(id: Long): QuarantineRecord?

  @Insert suspend fun insert(record: QuarantineRecord): Long

  @Delete suspend fun delete(record: QuarantineRecord)
}

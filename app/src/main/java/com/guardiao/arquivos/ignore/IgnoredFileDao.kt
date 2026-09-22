package com.guardiao.arquivos.ignore

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IgnoredFileDao {
  @Query("SELECT * FROM ignored_files ORDER BY ignoredAt DESC")
  fun observeAll(): Flow<List<IgnoredFile>>

  /** Consultado no início de cada varredura, para saber o que pular. */
  @Query("SELECT path FROM ignored_files")
  suspend fun paths(): List<String>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(files: List<IgnoredFile>)

  @Query("DELETE FROM ignored_files WHERE path = :path")
  suspend fun remove(path: String)

  @Query("DELETE FROM ignored_files")
  suspend fun clear()
}

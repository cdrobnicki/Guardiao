package com.guardiao.arquivos.quarantine

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [QuarantineRecord::class], version = 1, exportSchema = false)
abstract class QuarantineDatabase : RoomDatabase() {
  abstract fun quarantineDao(): QuarantineDao

  companion object {
    @Volatile private var instance: QuarantineDatabase? = null

    fun get(context: Context): QuarantineDatabase =
      instance
        ?: synchronized(this) {
          instance
            ?: Room.databaseBuilder(context.applicationContext, QuarantineDatabase::class.java, "guardiao_quarentena.db")
              .fallbackToDestructiveMigration(dropAllTables = true)
              .build()
              .also { instance = it }
        }
  }
}

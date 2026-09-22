package com.guardiao.arquivos.ignore

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Banco separado, só para a lista de ignorados.
 *
 * Poderia ser mais uma tabela no banco da quarentena, mas isso exigiria uma migração de esquema —
 * e migração é justamente o tipo de código que não dá para validar sem rodar em um aparelho que já
 * tenha a versão anterior instalada. Um banco novo começa na versão 1 e não corre o risco de
 * apagar o histórico de quarentena de quem já usa o app. Os dois assuntos também são
 * independentes: um guarda o que foi movido, o outro o que não deve mais aparecer.
 */
@Database(entities = [IgnoredFile::class], version = 1, exportSchema = false)
abstract class IgnoreDatabase : RoomDatabase() {
  abstract fun ignoredFileDao(): IgnoredFileDao

  companion object {
    @Volatile private var instance: IgnoreDatabase? = null

    fun get(context: Context): IgnoreDatabase =
      instance
        ?: synchronized(this) {
          instance
            ?: Room.databaseBuilder(
                context.applicationContext,
                IgnoreDatabase::class.java,
                "guardiao_ignorados.db",
              )
              .build()
              .also { instance = it }
        }
  }
}

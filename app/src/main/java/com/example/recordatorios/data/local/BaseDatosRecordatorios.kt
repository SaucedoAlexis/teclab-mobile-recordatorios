package com.example.recordatorios.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// Reúne las entidades, la versión del esquema y los DAO.
// version 1: Actividad 3. version 2: Actividad 4 (columna uid, ver MIGRACION_1_2).
@Database(
    entities = [RecordatorioEntity::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Conversores::class)
abstract class BaseDatosRecordatorios : RoomDatabase() {

    abstract fun recordatorioDao(): RecordatorioDao

    companion object {
        // Se llama una sola vez desde RecordatoriosApplication: hay una única instancia en toda la app
        fun crear(context: Context): BaseDatosRecordatorios =
            Room.databaseBuilder(
                context.applicationContext,
                BaseDatosRecordatorios::class.java,
                "recordatorios.db"
            )
                .addMigrations(MIGRACION_1_2)
                .build()
    }
}

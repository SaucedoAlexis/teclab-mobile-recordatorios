package com.example.recordatorios.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// Reúne las entidades, la versión del esquema y los DAO.
// version = 1 porque es la primera base de la app. Si en el Módulo 4 se agregan columnas
// (ubicación, aviso) hay que subir la versión y escribir una migración que conserve los datos.
@Database(
    entities = [RecordatorioEntity::class],
    version = 1,
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
            ).build()
    }
}

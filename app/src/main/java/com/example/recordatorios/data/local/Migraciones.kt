package com.example.recordatorios.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Versión 1 (Actividad 3) → versión 2 (Actividad 4): se agrega la cuenta dueña de cada recordatorio.
// Una migración conserva los datos existentes en lugar de borrar y recrear la base.
val MIGRACION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Las filas que ya existían no tienen dueño: quedan con uid vacío. Ninguna cuenta las ve
        // (las consultas filtran por uid) y se eliminan la primera vez que alguien inicia sesión,
        // porque no se puede saber a quién pertenecían.
        db.execSQL("ALTER TABLE recordatorios ADD COLUMN uid TEXT NOT NULL DEFAULT ''")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_recordatorios_uid ON recordatorios(uid)")
    }
}

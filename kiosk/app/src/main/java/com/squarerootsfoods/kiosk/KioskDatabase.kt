package com.squarerootsfoods.kiosk

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [InstallVisit::class, LaneMark::class, InstallEvent::class],
    version = 1,
    exportSchema = false,
)
abstract class KioskDatabase : RoomDatabase() {
    abstract fun installDao(): InstallDao

    companion object {
        @Volatile
        private var instance: KioskDatabase? = null

        fun get(context: Context): KioskDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    KioskDatabase::class.java,
                    "kiosk.db",
                ).build().also { instance = it }
            }
        }
    }
}

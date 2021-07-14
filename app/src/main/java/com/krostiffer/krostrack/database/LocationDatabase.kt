package com.krostiffer.krostrack.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [LocationExt::class], version = 5)
abstract class LocationDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    val exportSchema =false

    companion object {
        @Volatile
        private var INSTANCE: LocationDatabase? = null

        fun getDatabase(
            context: Context,
        ): LocationDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LocationDatabase::class.java,
                    "location_Database"
                )
                    // Wipes and rebuilds instead of migrating if no Migration object.
                    // Migration is not part of this codelab.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}
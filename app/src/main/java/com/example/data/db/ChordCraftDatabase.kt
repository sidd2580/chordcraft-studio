package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.Setlist
import com.example.data.model.Song

@Database(entities = [Song::class, Setlist::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class ChordCraftDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun setlistDao(): SetlistDao

    companion object {
        @Volatile
        private var INSTANCE: ChordCraftDatabase? = null

        fun getDatabase(context: Context): ChordCraftDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChordCraftDatabase::class.java,
                    "chordcraft_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

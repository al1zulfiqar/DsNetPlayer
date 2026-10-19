package com.digitalsignage.dsnetcoreplayer.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.digitalsignage.dsnetcoreplayer.data.Converters
import com.digitalsignage.dsnetcoreplayer.data.dao.PlayerDao
import com.digitalsignage.dsnetcoreplayer.data.player.Player

@Database(entities = [Player::class], version = 3)
@TypeConverters(Converters::class)
abstract class DsNetDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao

    companion object {
        @Volatile
        private var INSTANCE: DsNetDatabase? = null

        fun getDatabase(context: Context): DsNetDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DsNetDatabase::class.java,
                    "app_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

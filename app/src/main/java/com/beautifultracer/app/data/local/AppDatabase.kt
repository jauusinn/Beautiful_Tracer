package com.beautifultracer.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.beautifultracer.app.data.local.dao.SearchHistoryDao
import com.beautifultracer.app.data.local.dao.TracerouteHopDao
import com.beautifultracer.app.data.local.dao.WhoisCacheDao
import com.beautifultracer.app.data.local.entity.SearchHistoryEntity
import com.beautifultracer.app.data.local.entity.TracerouteHopEntity
import com.beautifultracer.app.data.local.entity.WhoisCacheEntity

@Database(
    entities = [
        TracerouteHopEntity::class,
        WhoisCacheEntity::class,
        SearchHistoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun tracerouteHopDao(): TracerouteHopDao
    abstract fun whoisCacheDao(): WhoisCacheDao
    abstract fun searchHistoryDao(): SearchHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "beautiful_tracer.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

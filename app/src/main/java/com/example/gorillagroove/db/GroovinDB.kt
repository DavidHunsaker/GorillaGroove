package com.example.gorillagroove.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.gorillagroove.db.dao.PlaylistDao
import com.example.gorillagroove.db.dao.UserDao
import com.example.gorillagroove.db.model.Playlist
import com.example.gorillagroove.db.model.User

@Database(entities = [User::class, Playlist::class], version = 1, exportSchema = false)
abstract class GroovinDB : RoomDatabase() {

    abstract fun userRepository(): UserDao
    abstract fun playlistRepository(): PlaylistDao

    companion object {
        @Volatile
        private var INSTANCE: GroovinDB? = null

        fun getDatabase(context: Context): GroovinDB {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GroovinDB::class.java,
                    "GroovinDB"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}
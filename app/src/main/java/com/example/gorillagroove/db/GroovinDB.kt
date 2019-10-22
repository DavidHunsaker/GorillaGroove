package com.example.gorillagroove.db

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
import com.example.gorillagroove.db.dao.UserDao
import com.example.gorillagroove.db.model.User

@Database(entities = [User::class], version = 1, exportSchema = false)
abstract class GroovinDB : RoomDatabase() {

    abstract fun userRepository(): UserDao

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
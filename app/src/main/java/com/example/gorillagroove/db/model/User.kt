package com.example.gorillagroove.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.annotation.Nullable

@Entity
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo val userName: String,
    @ColumnInfo val email: String,
    @Nullable @ColumnInfo val token: String?,
    @ColumnInfo val loggedIn: Int,
    @ColumnInfo val deviceId: String?
)
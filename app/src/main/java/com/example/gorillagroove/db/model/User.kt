package com.example.gorillagroove.db.model

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.support.annotation.Nullable

@Entity
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo val userName: String,
    @ColumnInfo val email: String,
    @Nullable @ColumnInfo val token: String?
)
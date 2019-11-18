package com.example.gorillagroove.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Playlist(
    @PrimaryKey val id: Long,
    @ColumnInfo val name: String,
    @ColumnInfo val createdAt: String
)
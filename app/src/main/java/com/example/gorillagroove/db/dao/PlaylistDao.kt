package com.example.gorillagroove.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.gorillagroove.db.model.Playlist

@Dao
interface PlaylistDao {
    @Insert
    fun createPlaylist(playlist: Playlist)

    @Query(value = "SELECT * FROM Playlist WHERE name = :name")
    fun getPlaylistByName(name: String): Playlist?

    @Query(value = "SELECT * FROM Playlist")
    fun getAllPlaylists(): List<Playlist>
}
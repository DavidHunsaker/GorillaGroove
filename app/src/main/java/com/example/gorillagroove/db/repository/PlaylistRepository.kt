package com.example.gorillagroove.db.repository

import android.util.Log
import com.example.gorillagroove.db.dao.PlaylistDao
import com.example.gorillagroove.db.model.Playlist

const val playlistRepositoryTag = "Playlist Repository"

class PlaylistRepository(private val playlistDao: PlaylistDao) {

    fun getPlaylist(name: String): Playlist? {
        Log.i(playlistRepositoryTag, "Retrieving playlist with name=$name")
        return playlistDao.getPlaylistByName(name)
    }

    fun getAllPlaylists(): List<Playlist> {
        return playlistDao.getAllPlaylists()
    }

    fun createPlaylist(id: Long, name: String, createdAt: String) {
        val newPlaylist = Playlist(id, name, createdAt)
        playlistDao.createPlaylist(newPlaylist)
    }

    fun createPlaylist(playlist: Playlist) {
        if (playlistDao.getPlaylistByName(playlist.name) == null)
            playlistDao.createPlaylist(playlist)
    }
}
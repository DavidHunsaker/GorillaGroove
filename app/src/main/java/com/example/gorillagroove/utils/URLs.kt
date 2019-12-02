package com.example.gorillagroove.utils

object URLs {
    const val LOGIN = "https://gorillagroove.net/api/authentication/login"
    const val PLAYLISTS = "https://gorillagroove.net/api/playlist"
    const val PLAYLIST_TEMPLATE =
        "https://gorillagroove.net/api/playlist/track?playlistId=49&size=200"
    const val LIBRARY =
        "https://gorillagroove.net/api/track?sort=artist,asc&sort=album,asc&sort=trackNumber,asc&size=600&page=0"
    const val MARK_LISTENED = "https://gorillagroove.net/api/track/mark-listened"
    const val TRACK = "https://gorillagroove.net/api/file/link/"
}
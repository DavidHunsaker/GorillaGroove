package com.example.gorillagroove.utils

object URLs {
    private const val BASE_URL = "https://gorillagroove.net"
    const val LOGIN = "$BASE_URL/api/authentication/login"
    const val PLAYLISTS = "$BASE_URL/api/playlist"
    const val PLAYLIST_BASE =
        "$BASE_URL/api/playlist/track?"
    const val LIBRARY =
        "$BASE_URL/api/track?sort=artist,asc&sort=album,asc&sort=trackNumber,asc&size=600&page=0"
    const val MARK_LISTENED = "$BASE_URL/api/track/mark-listened"
    const val TRACK = "$BASE_URL/api/file/link/"
    const val USER = "$BASE_URL/api/user"
    const val DEVICE = "$BASE_URL/api/device"
}
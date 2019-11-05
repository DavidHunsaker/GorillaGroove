package com.example.gorillagroove

import org.json.JSONObject

interface IVolley {
    fun onResponse(response: JSONObject)
    fun onResponse(response: String)
    fun onLoginResponse(response: JSONObject)
    fun onPlaylistRequestResponse(response: JSONObject)
//    fun onSongResponse(response: JSONObject)
}
package com.example.gorillagroove.client

import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import kotlin.concurrent.thread

private val client = OkHttpClient()

fun loginRequest(url: String, email: String, password: String): JSONObject {
    val body = """{ "email": "$email", "password": "$password" }""".trimIndent()

    val request = Request.Builder()
        .url(url)
        .post(RequestBody.create("application/json".toMediaTypeOrNull(), body))
        .build()

    var responseVal = JSONObject()

    Log.i("Login Request", "Making request with credentials email=$email, password=$password")

    thread {
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                responseVal = (JSONObject(response.body!!.string()))
            } else {
                Log.i("Login Request", "Unsuccessful response with code ${response.code}")
            }
        }
    }.join()
    return responseVal
}


fun authenticatedGetRequest(url: String, token: String): JSONObject {
    val request = Request.Builder()
        .url(url)
        .get()
        .addHeader("Authorization", "Bearer $token")
        .build()

    var responseVal = JSONObject()

    thread {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            responseVal = JSONObject(response.body!!.string())
        }
    }.join()
    return responseVal
}

fun playlistGetRequest(url: String, token: String): JSONArray {
    val request = Request.Builder()
        .url(url)
        .get()
        .addHeader("Authorization", "Bearer $token")
        .build()

    var responseVal = JSONArray()

    thread {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            responseVal = JSONArray(response.body!!.string())
        }
    }.join()
    return responseVal
}

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
private const val VERSION = "1.1.1"

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

fun updateDevice(url: String, token: String, deviceId: String) {
    val body = """{
        |"deviceId": "$deviceId",
        |"version":"$VERSION",
        |"deviceType":"ANDROID"
        |}
    """.trimMargin()
    val request = Request.Builder()
        .url(url)
        .put(RequestBody.create("application/json".toMediaTypeOrNull(), body))
        .header("Authorization", "Bearer $token")
        .build()

    thread { client.newCall(request).execute() }.join()
}

fun markListenedRequest(url: String, trackId: Long, token: String, deviceId: String) {
    val body = """{ "trackId": $trackId, "deviceId": "$deviceId" }""".trimIndent()
    val request = Request.Builder()
        .url(url)
        .post(RequestBody.create("application/json".toMediaTypeOrNull(), body))
        .header("Authorization", "Bearer $token")
        .build()

    thread { client.newCall(request).execute() }.join()
}

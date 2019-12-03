package com.example.gorillagroove.client

import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

private const val HTTPCLIENT_TAG = "HTTP_CLIENT"
private val client = OkHttpClient().newBuilder().connectTimeout(15, TimeUnit.SECONDS).build()

fun loginRequest(url: String, email: String, password: String): JSONObject {
    val body = """{ "email": "$email", "password": "$password" }""".trimIndent()

    val request = Request.Builder()
        .url(url)
        .post(RequestBody.create("application/json".toMediaTypeOrNull(), body))
        .build()

    var responseVal = JSONObject()

    Log.i("Login Request", "Making request with credentials email=$email, password=$password")

    thread {
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    responseVal = (JSONObject(response.body!!.string()))
                } else {
                    Log.e("Login Request", "Unsuccessful response with code ${response.code}")
                }
            }
        } catch(e: SocketTimeoutException) {
            Log.e(HTTPCLIENT_TAG, "Socket Timeout Exception occured when making login request")
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
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful)
                    Log.e(HTTPCLIENT_TAG, "Unexpected Response Code ${response.code}")
                responseVal = JSONObject(response.body!!.string())
            }
        } catch(e: SocketTimeoutException) {
            Log.e(HTTPCLIENT_TAG, "Socket Timeout Occurred When Making GET Request To $url")
        }
    }.join()

    return responseVal
}

fun markListenedRequest(url: String, trackId: Long, token: String) {
    val body = """{ "trackId": $trackId }""".trimIndent()
    val request = Request.Builder()
        .url(url)
        .post(RequestBody.create("application/json".toMediaTypeOrNull(), body))
        .header("Authorization", "Bearer $token")
        .build()

    thread {
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Log.i(
                    HTTPCLIENT_TAG,
                    "Track $trackId successfully marked listened"
                )
                else Log.e(HTTPCLIENT_TAG, "Track $trackId failed to update play count")
            }
        } catch (e: SocketTimeoutException) {
            Log.e(
                HTTPCLIENT_TAG,
                "There was a network timeout when updating play count for track=$trackId"
            )
        }
    }.join()
}

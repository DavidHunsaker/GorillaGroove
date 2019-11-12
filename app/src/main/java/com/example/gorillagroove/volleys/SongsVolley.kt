package com.example.gorillagroove.volleys

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.util.LruCache
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyLog
import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

interface SongsVolley {
    fun onGetSongResponse(response: JSONObject)
}

class SongsRequest private constructor(context: Context, songsVolley: SongsVolley) {

    private var mRequestQueue: RequestQueue? = null
    private var context: Context? = context
    private var songsVolley: SongsVolley? = songsVolley
    var imageLoader: ImageLoader? = null

    private val requestQueue: RequestQueue
        get() {
            if (mRequestQueue == null)
                mRequestQueue = Volley.newRequestQueue(context!!.applicationContext)
            return mRequestQueue!!
        }

    init {
        mRequestQueue = requestQueue
        this.imageLoader = ImageLoader(mRequestQueue, object : ImageLoader.ImageCache {
            private val mCache = LruCache<String, Bitmap>(10)
            override fun getBitmap(url: String?): Bitmap {
                return mCache.get(url)
            }

            override fun putBitmap(url: String?, bitmap: Bitmap?) {
                mCache.put(url, bitmap)
            }

        })
    }

    private fun <T> addToRequestQueue(req: Request<T>) {
        requestQueue.add(req)
    }

    fun getSongRequest(url: String, token: String) {
        val postRequest = object :
            JsonObjectRequest(
                Method.GET, url, null, Response.Listener { response ->
                    Log.d(ContentValues.TAG, response.toString())
                    songsVolley!!.onGetSongResponse(response)
                },
                Response.ErrorListener { error -> VolleyLog.d(ContentValues.TAG, "Error: ${error.message}") }
            ) {

            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json; charset=utf-8"
                headers["Authorization"] = "Bearer $token"
                headers["Accept-Encoding"] = "gzip, deflate"
                return headers
            }
        }
        addToRequestQueue(postRequest)
    }

    companion object {
        private var mInstance: SongsRequest? = null

        @Synchronized
        fun getInstance(context: Context, songsVolley: SongsVolley): SongsRequest {
            if (mInstance == null) {
                mInstance = SongsRequest(context, songsVolley)
            }
            return mInstance!!
        }
    }
}
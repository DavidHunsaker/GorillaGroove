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

interface AuthenticationVolley {
    fun onLoginResponse(response: JSONObject)
    fun onLogoutResponse(response: JSONObject)
}

class AuthenticationResponses {
    private var mRequestQueue: RequestQueue? = null
    private var context: Context? = null
    private var authenticationVolley: AuthenticationVolley? = null
    var imageLoader: ImageLoader? = null

    private val requestQueue: RequestQueue
        get() {
            if (mRequestQueue == null)
                mRequestQueue = Volley.newRequestQueue(context!!.applicationContext)
            return mRequestQueue!!
        }

    private constructor(context: Context, authenticationVolley: AuthenticationVolley) {
        this.context = context
        this.authenticationVolley = authenticationVolley
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

    fun <T> addToRequestQueue(req: Request<T>) {
        requestQueue.add(req)
    }

    fun loginRequest(url: String, params: Map<String, String>) {
        val postRequest = object :
            JsonObjectRequest(
                Method.POST, url, JSONObject(params), Response.Listener { response ->
                    Log.d(ContentValues.TAG, response.toString())
                    authenticationVolley!!.onLoginResponse(response)
                },
                Response.ErrorListener { error -> VolleyLog.d(ContentValues.TAG, "Error: " + error.message) }
            ) {
            //Ctrl+O
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json; charset=utf-8"
                return headers
            }
        }
        addToRequestQueue(postRequest)
    }

    fun logoutRequest(url: String) {
        val postRequest = object :
            JsonObjectRequest(
                Method.POST, url, null, Response.Listener { response ->
                    Log.d(ContentValues.TAG, response.toString())
                    authenticationVolley!!.onLogoutResponse(response)
                },
                Response.ErrorListener { error -> VolleyLog.d(ContentValues.TAG, "Error: " + error.message) }
            ) {
            //Ctrl+O
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json; charset=utf-8"
                return headers
            }
        }
        addToRequestQueue(postRequest)
    }

    companion object {
        private var mInstance: AuthenticationResponses? = null

        @Synchronized
        fun getInstance(context: Context, authenticationVolley: AuthenticationVolley): AuthenticationResponses {
            if (mInstance == null) {
                mInstance = AuthenticationResponses(context, authenticationVolley)
            }
            return mInstance!!
        }
    }

}
package com.example.gorillagroove

import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.util.LruCache
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import com.android.volley.AuthFailureError
import com.android.volley.Request.Method.*
import com.android.volley.VolleyLog


class MyVolleyRequest {
    private var mRequestQueue: RequestQueue? = null
    private var context: Context? = null
    private var iVolley: IVolley? = null
    var imageLoader: ImageLoader? = null

    private val requestQueue: RequestQueue
        get() {
            if (mRequestQueue == null)
                mRequestQueue = Volley.newRequestQueue(context!!.applicationContext)
            return mRequestQueue!!
        }

    private constructor(context: Context, iVolley: IVolley) {
        this.context = context
        this.iVolley = iVolley
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

    private constructor(context: Context) {
        this.context = context
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

    //GET METHOD
    fun getRequest(url: String) {
        val getRequest = JsonObjectRequest(GET, url, null, Response.Listener { response ->
            iVolley!!.onResponse(response.toString())
        }, Response.ErrorListener { error ->
            iVolley!!.onResponse(error.message!!)
        })

        addToRequestQueue(getRequest)
    }

    fun getPlaylistRequest(url: String, token: String){
        val postRequest = object :
            JsonObjectRequest(GET, url, null, Response.Listener { response ->
                Log.d(TAG, response.toString())
                iVolley!!.onPlaylistRequestResponse(response)
            },
                Response.ErrorListener { error -> VolleyLog.d(TAG, "Error: " + error.message) }
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


    fun makeJsonObjRequest(url: String) {

        val params = HashMap<String, String>()
        params["email"] = "dude@dude.dude"
        params["password"] = "dude"

        val jsonObjReq = object :
            JsonObjectRequest(POST, url, JSONObject(params), Response.Listener { response ->
                Log.d(TAG, response.toString())
                iVolley!!.onLoginResponse(response)
            },
                Response.ErrorListener { error -> VolleyLog.d(TAG, "Error: " + error.message) }
            ) {
            /**
             * Passing some request headers
             */
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json; charset=utf-8"
                return headers
            }
        }
        addToRequestQueue(jsonObjReq)
    }

    //POST METHOD with params
    fun postRequest(url: String, params: Map<String, String>) {
        val postRequest = object :
            JsonObjectRequest(POST, url, JSONObject(params), Response.Listener { response ->
                Log.d(TAG, response.toString())
                iVolley!!.onLoginResponse(response)
            },
                Response.ErrorListener { error -> VolleyLog.d(TAG, "Error: " + error.message) }
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

    //PUT METHOD with params
    fun putRequest(url: String, params: Map<String, String>) {
        val putRequest = object : JsonObjectRequest(
            PUT,
            url,
            JSONObject(params),
            Response.Listener { response ->
                iVolley!!.onResponse(response.toString())
            }, Response.ErrorListener { error ->
                VolleyLog.d(TAG)
            }
        ) {
            //Ctrl+O
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "aplication/json; charset=utf-8"
                return headers
            }
        }
        addToRequestQueue(putRequest)
    }

    //PATCH METHOD with params
    fun patchRequest(url: String, params: Map<String, String>) {
        val patchRequest = object : JsonObjectRequest(
            PATCH,
            url,
            JSONObject(params),
            Response.Listener { response ->
                iVolley!!.onResponse(response.toString())
            }, Response.ErrorListener { error ->
                VolleyLog.d(TAG)
            }
        ) {
            //Ctrl+O
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "aplication/json; charset=utf-8"
                return headers
            }
        }
        addToRequestQueue(patchRequest)
    }

    //DELETE METHOD
    fun deleteRequest(url: String, params: Map<String, String>) {
        val deleteRequest = object : JsonObjectRequest(
            DELETE,
            url,
            JSONObject(params),
            Response.Listener { response ->
                iVolley!!.onResponse(response.toString())
            }, Response.ErrorListener { error ->
                VolleyLog.d(TAG)
            }
        ) {
            //Ctrl+O
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "aplication/json; charset=utf-8"
                return headers
            }
        }
        addToRequestQueue(deleteRequest)
    }

    companion object {
        private var mInstance: MyVolleyRequest? = null

        @Synchronized
        fun getInstance(context: Context): MyVolleyRequest {
            if (mInstance == null) {
                mInstance = MyVolleyRequest(context)
            }
            return mInstance!!
        }

        @Synchronized
        fun getInstance(context: Context, iVolley: IVolley): MyVolleyRequest {
            if (mInstance == null) {
                mInstance = MyVolleyRequest(context, iVolley)
            }
            return mInstance!!
        }
    }
}
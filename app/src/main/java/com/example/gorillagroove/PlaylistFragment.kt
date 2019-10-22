package com.example.gorillagroove

import android.os.AsyncTask
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class FuckAndroid: AsyncTask<String, Void, String>() {
    override fun doInBackground(vararg params: String?): String? {
       return asFuck().toString()
    }

    fun asFuck(){
        val url = URL("http://gorillagroove.net/api/authentication/login")
        val params: HashMap<String, String> = HashMap()
        params["email"] = "david.e.hunsaker@gmail.com"
        params["password"] = "july21haPpy!"

//        var reqParam = URLEncoder.encode("userName", "UTF-8") + "=" + URLEncoder.encode("david.e.hunsaker@gmail.com", "UTF-8")
//        reqParam += "$" + URLEncoder.encode("password", "UTF-8") + "=" + URLEncoder.encode("july21haPpy!", "UTF-8")

        var reqParam = URLEncoder.encode("userName=david.e.hunsaker@gmail.com\$password=july21haPpy!", "UTF-8")

        with(url.openConnection() as HttpURLConnection){
            requestMethod = "POST"

            val wr = OutputStreamWriter(outputStream)
            wr.write(reqParam)
            wr.flush()

            println("URL: $url")
            println("Response Code: $responseCode")

            BufferedReader(InputStreamReader(inputStream)).use {
                val response = StringBuffer()

                var inputLine = it.readLine()
                while (inputLine != null) {
                    response.append(inputLine)
                    inputLine = it.readLine()
                }
                it.close()
                println("Response: $response")
            }


        }
    }

}
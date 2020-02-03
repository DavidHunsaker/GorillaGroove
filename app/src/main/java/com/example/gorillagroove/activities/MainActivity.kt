package com.example.gorillagroove.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.gorillagroove.R
import com.example.gorillagroove.client.loginRequest
import com.example.gorillagroove.client.updateDevice
import com.example.gorillagroove.db.GroovinDB
import com.example.gorillagroove.db.model.User
import com.example.gorillagroove.db.repository.UserRepository
import com.example.gorillagroove.utils.URLs
import kotlinx.android.synthetic.main.app_bar_main.toolbar
import kotlinx.android.synthetic.main.new_content_main.button_login
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.UUID

private const val MainActivityTag = "MainActivity"

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    private lateinit var userRepository: UserRepository

    private lateinit var passwordField: EditText
    private lateinit var emailField: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userRepository =
            UserRepository(GroovinDB.getDatabase(this@MainActivity).userRepository())

        runBlocking {
            withContext(Dispatchers.IO) {
                val user = userRepository.lastLoggedInUser()
                Log.i(MainActivityTag, "Last logged in user=$user")
                if (user != null) {
                    if (user.deviceId == null) {
                        val deviceId = UUID.randomUUID().toString()
                        userRepository.updateDeviceId(user.id, deviceId)

                        val anotherOne = userRepository.lastLoggedInUser()
                        if (anotherOne != null) startActivity(createPlaylistIntent(anotherOne))
                    } else {
                        startActivity(createPlaylistIntent(user))
                    }
                }
            }
        }

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        emailField = findViewById(R.id.edit_text_email)
        passwordField = findViewById(R.id.edit_text_password)

        emailField.requestFocus()

        button_login.setOnClickListener {

            val emailFieldText = emailField.text.toString()
            val passwordFieldText = passwordField.text.toString()

            val response =
                loginRequest(URLs.LOGIN, emailFieldText, passwordFieldText)

            if (!response.has("token")) {
                Toast.makeText(
                    this,
                    "Incorrect login credentials, please try again",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                val token = response["token"].toString()
                val userName = response["username"].toString()
                val email = response["email"].toString()

                val user = runBlocking {
                    withContext(Dispatchers.IO) {
                        val innerUser: User? = userRepository.findUser(email)

                        if (innerUser != null) {
                            if (innerUser.deviceId == null) {
                                val deviceId = UUID.randomUUID().toString()
                                userRepository.updateDeviceId(innerUser.id, deviceId)
                            }
                            userRepository.updateToken(innerUser.id, token)
                        } else {
                            val deviceId = UUID.randomUUID().toString()
                            userRepository.createUser(userName, email, token, deviceId)
                        }

                        return@withContext userRepository.findUser(email)
                    }
                }

                emailField.text.clear()
                passwordField.text.clear()
                emailField.requestFocus()
                startActivity(createPlaylistIntent(user!!))
            }
        }
    }

    private fun createPlaylistIntent(user: User): Intent {
        runBlocking { updateDevice(URLs.DEVICE, user.token!!, user.deviceId!!) }

        val intent = Intent(applicationContext, PlaylistActivity::class.java)
        intent.putExtra("token", user.token)
        intent.putExtra("username", user.userName)
        intent.putExtra("email", user.email)
        intent.putExtra("deviceId", user.deviceId)
        return intent
    }
}

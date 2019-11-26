package com.example.gorillagroove.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.example.gorillagroove.R
import com.example.gorillagroove.client.loginRequest
import com.example.gorillagroove.client.playlistGetRequest
import com.example.gorillagroove.db.GroovinDB
import com.example.gorillagroove.db.model.Playlist
import com.example.gorillagroove.db.model.User
import com.example.gorillagroove.db.repository.PlaylistRepository
import com.example.gorillagroove.db.repository.UserRepository
import com.example.gorillagroove.dto.PlaylistDTO
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.activity_main.drawer_layout
import kotlinx.android.synthetic.main.activity_main.nav_view
import kotlinx.android.synthetic.main.app_bar_main.toolbar
import kotlinx.android.synthetic.main.content_main.btn_login
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener, CoroutineScope by MainScope() {
    var user: User? = null
    var token: String = ""
    var userName: String = ""
    var email: String = ""


    private val om = ObjectMapper()
    private val loginUrl = "https://gorillagroove.net/api/authentication/login"
    private val playlistsUrl = "https://gorillagroove.net/api/playlist"

    private lateinit var userRepository: UserRepository
    private lateinit var playlistRepository: PlaylistRepository

    private lateinit var passwordField: EditText
    private lateinit var emailField: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        userRepository =
            UserRepository(GroovinDB.getDatabase(this@MainActivity).userRepository())
        playlistRepository =
            PlaylistRepository(GroovinDB.getDatabase(this@MainActivity).playlistRepository())

        emailField = findViewById(R.id.editText2)
        passwordField = findViewById(R.id.editText)

        emailField.requestFocus()

        if (intent.hasExtra("token")) token = intent.getStringExtra("token")
        if (intent.hasExtra("username")) {
            userName = intent.getStringExtra("username")
            findViewById<TextView>(R.id.tv_nav_header).text = userName
        }

        val toggle = ActionBarDrawerToggle(
            this,
            drawer_layout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()


        btn_login.setOnClickListener {

            val emailFieldText = emailField.text.toString()
            val passwordFieldText = passwordField.text.toString()

            val response = runBlocking { loginRequest(loginUrl, emailFieldText, passwordFieldText) }

            if (!response.has("token")) {
                Toast.makeText(
                    this,
                    "Incorrect login credentials, please try again",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                token = response["token"].toString()
                userName = response["username"].toString()
                email = response["email"].toString()

                findViewById<TextView>(R.id.tv_nav_header).text = userName

                launch {
                    withContext(Dispatchers.IO) {
                        user = userRepository.findUser(emailFieldText)

                        if (user != null) {
                            userRepository.updateToken(user!!.id, token)
                        } else userRepository.createUser(userName, emailFieldText, token)
                    }
                }

                emailField.text.clear()
                passwordField.text.clear()
                emailField.requestFocus()
            }
        }

        nav_view.setNavigationItemSelectedListener(this)
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (item.itemId == R.id.action_settings_logout) {
            Toast.makeText(this, "In the future this will actually log out", Toast.LENGTH_SHORT)
                .show()
        }
        return when (item.itemId) {
            R.id.action_settings_logout -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_library -> {
                if (token.isNotBlank()) {
                    val intent = Intent(applicationContext, PlaylistActivity::class.java)
                    intent.putExtra("token", token)
                    intent.putExtra("username", userName)
                    intent.putExtra("email", email)
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "You must be logged in to go here!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }
}

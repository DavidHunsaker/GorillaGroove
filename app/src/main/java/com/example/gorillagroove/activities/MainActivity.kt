package com.example.gorillagroove.activities

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.gorillagroove.R
import com.example.gorillagroove.db.GroovinDB
import com.example.gorillagroove.db.model.User
import com.example.gorillagroove.db.repository.UserRepository
import com.example.gorillagroove.volleys.AuthenticationRequests
import com.example.gorillagroove.volleys.AuthenticationVolley
import kotlinx.android.synthetic.main.activity_main.drawer_layout
import kotlinx.android.synthetic.main.activity_main.nav_view
import kotlinx.android.synthetic.main.app_bar_main.toolbar
import kotlinx.android.synthetic.main.content_main.btn_login
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject


class MainActivity : AppCompatActivity(), AuthenticationVolley,
    NavigationView.OnNavigationItemSelectedListener, CoroutineScope by MainScope() {
    var user: User? = null
    var token: String = ""
    var userName: String = ""
    var email: String = ""

    private lateinit var repository: UserRepository

    private lateinit var passwordField: EditText
    private lateinit var emailField: EditText

    override fun onLoginResponse(response: JSONObject) {
        token = response["token"].toString()
        userName = response["username"].toString()
        email = response["email"].toString()

        findViewById<TextView>(R.id.tv_nav_header).text = userName

        launch {
            withContext(Dispatchers.IO) {
                user = repository.findUser(email)

                if (user != null) {
                    repository.updateToken(user!!.id, token)
                } else repository.createUser(userName, email, token)
            }
        }
        Log.i(
            "Main Activity",
            "What's up dude, we just snagged ourselves some token: $token and userName: $userName"
        )

        emailField.text.clear()
        passwordField.text.clear()
        emailField.requestFocus()
    }

    override fun onLogoutResponse(response: JSONObject) {
        Toast.makeText(this@MainActivity, "Successfully Logged out", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        repository = UserRepository(GroovinDB.getDatabase(this@MainActivity).userRepository())

        emailField = findViewById(R.id.editText2)
        passwordField = findViewById(R.id.editText)

        emailField.requestFocus()

        if (intent.hasExtra("token")) token = intent.getStringExtra("token")
        if (intent.hasExtra("username")) token = intent.getStringExtra("username")

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

            val credentials =
                credentialsToMap(emailField.text.toString(), passwordField.text.toString())

            AuthenticationRequests.getInstance(this@MainActivity, this@MainActivity)
                .loginRequest("http://gorillagroove.net/api/authentication/login", credentials)
        }

        nav_view.setNavigationItemSelectedListener(this)
    }

    private fun credentialsToMap(email: String, password: String): HashMap<String, String> {
        val credentials = HashMap<String, String>()
        credentials["email"] = email
        credentials["password"] = password

        return credentials
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
            launch {
                withContext(Dispatchers.IO) {
                    AuthenticationRequests.getInstance(this@MainActivity, this@MainActivity)
                        .logoutRequest("http://gorillagroove.net/api/authentication/logout")
                }
            }
        }
        return when (item.itemId) {
            R.id.action_settings_logout -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_login -> {
                val intent = Intent(applicationContext, MainActivity::class.java)
                intent.putExtra("token", token)
                intent.putExtra("username", userName)
                intent.putExtra("email", email)
                startActivity(intent)
            }
            R.id.nav_playlists -> {
                if (token.isNotBlank()) {
                    val intent = Intent(applicationContext, PlaylistActivity::class.java)
                    intent.putExtra("token", token)
                    intent.putExtra("username", userName)
                    intent.putExtra("email", email)
                    startActivity(intent)
                } else {
                    Toast.makeText(this@MainActivity, "You must be logged in to go here!", Toast.LENGTH_LONG).show()
                }
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }
}

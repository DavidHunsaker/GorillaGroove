package com.example.gorillagroove

import android.os.AsyncTask
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Adapter
import android.widget.EditText
import android.widget.Toast
import com.example.gorillagroove.db.GroovinDB
import com.example.gorillagroove.db.repository.UserRepository
import com.example.gorillagroove.dto.SongDTO
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.json.JSONObject


class MainActivity : AppCompatActivity(), IVolley, NavigationView.OnNavigationItemSelectedListener {

    var token: String = ""
    var userName: String = ""
    var email: String = ""

    private lateinit var repository: UserRepository

    private lateinit var passwordField: EditText
    private lateinit var emailField: EditText

    private lateinit var playlistAdapter: PlaylistAdapter


    override fun onResponse(response: String) {
        Toast.makeText(this@MainActivity, response, Toast.LENGTH_LONG).show()
    }

    override fun onPlaylistRequestResponse(response: JSONObject) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onLoginResponse(response: JSONObject) {
        token = response["token"].toString()
        userName = response["username"].toString()
        email = response["email"].toString()

        AsyncTask.execute {
            val user = repository.findUser(email)

            if (user == null) {
                repository.createUser(userName, email, token)
            } else repository.updateToken(user.id, token)

        }
        Log.i(
            "Main Activity",
            "What's up dude, we just snagged ourselves some token: $token and userName: $userName"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        repository = UserRepository(GroovinDB.getDatabase(this@MainActivity).userRepository())

        emailField = findViewById(R.id.editText2)
        passwordField = findViewById(R.id.editText)

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

            MyVolleyRequest.getInstance(this@MainActivity, this@MainActivity)
                .postRequest("http://gorillagroove.net/api/authentication/login", credentials)
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
        when (item.itemId) {
            R.id.action_settings -> return true
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_camera -> {
                setContentView(R.layout.activity_main)
            }
            R.id.nav_gallery -> {
                setContentView(R.layout.fragment_user_playlist)
            }
            R.id.nav_slideshow -> {

            }
            R.id.nav_manage -> {

            }

        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }
}

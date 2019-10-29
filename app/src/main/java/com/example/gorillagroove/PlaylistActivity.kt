package com.example.gorillagroove

// TODO: Make this a fragment you lazy ho

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import com.example.gorillagroove.adapters.PlaylistAdapter
import com.example.gorillagroove.db.repository.UserRepository
import com.example.gorillagroove.dto.SongDTO
import kotlinx.android.synthetic.main.activity_main.drawer_layout
import kotlinx.android.synthetic.main.activity_main.nav_view
import kotlinx.android.synthetic.main.app_bar_main.toolbar
import org.json.JSONObject


class PlaylistActivity : AppCompatActivity(), IVolley,
    NavigationView.OnNavigationItemSelectedListener {

    var token: String = ""
    var userName: String = ""
    var email: String = ""

    private lateinit var repository: UserRepository

    private lateinit var passwordField: EditText
    private lateinit var emailField: EditText

    private lateinit var playlistAdapter: PlaylistAdapter


    override fun onResponse(response: String) {
        Toast.makeText(this@PlaylistActivity, response, Toast.LENGTH_LONG).show()
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
        setContentView(R.layout.fragment_user_playlist)
        setSupportActionBar(toolbar)

        val songs = listOf(
            SongDTO(
                "Wet Sand",
                "Red Hot Chili Peppers",
                "",
                "https://external-content.duckduckgo.com/iu/?u=https%3A%2F%2Fstudiosol-a.akamaihd.net%2Fuploadfile%2Fletras%2Falbuns%2Fe%2Fe%2Ff%2Fe%2F229761441219734-tb_180.jpg&f=1&nofb=1"
            ),
            SongDTO(
                "Fuck Gravity",
                "Virtual Riot",
                "",
                "https://external-content.duckduckgo.com/iu/?u=https%3A%2F%2Fi.ytimg.com%2Fvi%2FToIyuChbHK4%2Fhqdefault.jpg%3Fsqp%3D-oaymwEiCMQBEG5IWvKriqkDFQgBFQAAAAAYASUAAMhCPQCAokN4AQ%3D%3D%26rs%3DAOn4CLC2Guth45n6F_Wbu03yIOePggx-Ew&f=1&nofb=1"
            )
        )

        val recyclerView: RecyclerView = findViewById(R.id.rv_playlist)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = PlaylistAdapter(songs)

//        nav_view.setNavigationItemSelectedListener(this)
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
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_login -> {
                val intent = Intent(applicationContext, MainActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_playlists -> {
                val intent = Intent(applicationContext, PlaylistActivity::class.java)
                startActivity(intent)
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }
}
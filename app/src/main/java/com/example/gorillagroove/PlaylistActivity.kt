package com.example.gorillagroove

// TODO: Make this a fragment you lazy ho

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import com.example.gorillagroove.adapters.PlaylistAdapter
import com.example.gorillagroove.db.GroovinDB
import com.example.gorillagroove.db.repository.UserRepository
import com.example.gorillagroove.dto.SongRequest
import kotlinx.android.synthetic.main.activity_main.drawer_layout
import kotlinx.android.synthetic.main.activity_main.nav_view
import kotlinx.android.synthetic.main.app_bar_main.toolbar
import org.json.JSONObject


class PlaylistActivity : AppCompatActivity(), IVolley,
    NavigationView.OnNavigationItemSelectedListener {

    var token: String = ""
    var userName: String = ""

    private lateinit var repository: UserRepository

    private lateinit var passwordField: EditText
    private lateinit var emailField: EditText

    private lateinit var playlistAdapter: PlaylistAdapter


    override fun onResponse(response: JSONObject) {
//        Toast.makeText(this@PlaylistActivity, response.toString(), Toast.LENGTH_LONG).show()
        println(response.toString())
    }

    override fun onResponse(response: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onLoginResponse(response: JSONObject) {
        TODO("not implemented")
    }

    override fun onPlaylistRequestResponse(response: JSONObject) {
        println(response.toString())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist)
        setSupportActionBar(toolbar)

        repository = UserRepository(GroovinDB.getDatabase(this@PlaylistActivity).userRepository())

        val toggle = ActionBarDrawerToggle(
            this,
            drawer_layout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        // TODO: this needs to be a blocking call. Need to figure this out
        AsyncTask.execute {
            token = repository.findUser("test@gorilla.groove")!!.token!!
        }

        // TODO: Fix this janky mess!!
        while(this.token == "") { Thread.sleep(150) }

        MyVolleyRequest.getInstance(this@PlaylistActivity, this@PlaylistActivity)
            .getPlaylistRequest("http://gorillagroove.net/api/playlist/track?playlistId=49&size=75&page=0", token)

        val songs = listOf(
            SongRequest(
                "Wet Sand",
                "Red Hot Chili Peppers",
                null,
                null
            ),
            SongRequest(
                "Fuck Gravity",
                "Virtual Riot",
                null,
                null
            )
        )

        val recyclerView: RecyclerView = findViewById(R.id.rv_playlist)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = PlaylistAdapter(songs)

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
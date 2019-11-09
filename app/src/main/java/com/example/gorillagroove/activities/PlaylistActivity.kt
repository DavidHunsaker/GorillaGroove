package com.example.gorillagroove.activities

// TODO: Make this a fragment

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import com.example.gorillagroove.R
import com.example.gorillagroove.adapters.PlaylistAdapter
import com.example.gorillagroove.db.GroovinDB
import com.example.gorillagroove.db.repository.UserRepository
import com.example.gorillagroove.dto.PlaylistSongDTO
import com.example.gorillagroove.volleys.PlaylistRequests
import com.example.gorillagroove.volleys.PlaylistVolley
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.android.synthetic.main.activity_main.drawer_layout
import kotlinx.android.synthetic.main.activity_main.nav_view
import kotlinx.android.synthetic.main.app_bar_main.toolbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject


class PlaylistActivity : AppCompatActivity(), PlaylistVolley,
    NavigationView.OnNavigationItemSelectedListener, CoroutineScope by MainScope() {

    private var token: String = ""
    private var userName: String = ""
    private var activePlaylist: List<PlaylistSongDTO> = emptyList()
    private val om = ObjectMapper()

    private lateinit var recyclerView: RecyclerView

    private lateinit var repository: UserRepository

    override fun onPlaylistRequestResponse(response: JSONObject) {
        val content: String = response.get("content").toString()

        activePlaylist = om.readValue(content, arrayOf(PlaylistSongDTO())::class.java).toList()
        recyclerView.adapter = PlaylistAdapter(activePlaylist)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist)
        setSupportActionBar(toolbar)

        repository = UserRepository(GroovinDB.getDatabase(this@PlaylistActivity).userRepository())
        token = intent.getStringExtra("token")
        userName = intent.getStringExtra("username")

        val toggle = ActionBarDrawerToggle(
            this,
            drawer_layout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        launch {
            withContext(Dispatchers.IO) {
                PlaylistRequests.getInstance(this@PlaylistActivity, this@PlaylistActivity)
                    .getPlaylistRequest(
                        "http://gorillagroove.net/api/playlist/track?playlistId=49",
                        token
                    )
            }
        }

        recyclerView = findViewById(R.id.rv_playlist)
        recyclerView.layoutManager = LinearLayoutManager(this)

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
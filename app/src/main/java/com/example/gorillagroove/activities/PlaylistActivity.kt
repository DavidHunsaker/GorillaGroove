package com.example.gorillagroove.activities

// TODO: Make this a fragment

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import com.google.android.material.navigation.NavigationView
import androidx.core.view.GravityCompat
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.MediaController.MediaPlayerControl
import com.example.gorillagroove.R
import com.example.gorillagroove.adapters.OnItemClickListener
import com.example.gorillagroove.adapters.PlaylistAdapter
import com.example.gorillagroove.controller.MusicController
import com.example.gorillagroove.db.GroovinDB
import com.example.gorillagroove.db.repository.UserRepository
import com.example.gorillagroove.dto.PlaylistSongDTO
import com.example.gorillagroove.service.MusicPlayerService
import com.example.gorillagroove.service.MusicPlayerService.MusicBinder
import com.example.gorillagroove.volleys.PlaylistRequests
import com.example.gorillagroove.volleys.PlaylistVolley
import com.example.gorillagroove.volleys.authenticatedGetRequest
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.android.synthetic.main.activity_main.drawer_layout
import kotlinx.android.synthetic.main.activity_main.nav_view
import kotlinx.android.synthetic.main.app_bar_main.toolbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.system.exitProcess


class PlaylistActivity : AppCompatActivity(), PlaylistVolley,
    NavigationView.OnNavigationItemSelectedListener, CoroutineScope by MainScope(),
    MediaPlayerControl, OnItemClickListener {

    private val om = ObjectMapper()

    private var paused = false
    private var musicBound = false
    private var token: String = ""
    private var userName: String = ""
    private var playbackPaused = false
    private var playIntent: Intent? = null
    private var musicPlayerService: MusicPlayerService? = null
    private var activePlaylist: List<PlaylistSongDTO> = emptyList()

    private lateinit var recyclerView: RecyclerView
    private lateinit var repository: UserRepository
    private lateinit var controller: MusicController

    override fun onPlaylistRequestResponse(response: JSONObject) {
        val content: String = response.get("content").toString()

        activePlaylist = om.readValue(content, arrayOf(PlaylistSongDTO())::class.java).toList()
        musicPlayerService!!.setSongList(activePlaylist)
        val playlistAdapter = PlaylistAdapter(activePlaylist)
        recyclerView.adapter = playlistAdapter
        playlistAdapter.setClickListener(this)
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

        val playlistUrl = "http://gorillagroove.net/api/playlist/track?playlistId=49&size=200"

        val response = runBlocking { authenticatedGetRequest(playlistUrl, token) }

        val content: String = response.get("content").toString()

        activePlaylist = om.readValue(content, arrayOf(PlaylistSongDTO())::class.java).toList()
        recyclerView = findViewById(R.id.rv_playlist)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val playlistAdapter = PlaylistAdapter(activePlaylist)
        recyclerView.adapter = playlistAdapter
        playlistAdapter.setClickListener(this)

//        launch {
//            withContext(Dispatchers.IO) {
//                Log.d("PlaylistActivity", "User=$userName is making a playlist request")
//                PlaylistRequests.getInstance(this@PlaylistActivity, this@PlaylistActivity)
//                    .getPlaylistRequest(
//                        "http://gorillagroove.net/api/playlist/track?playlistId=49&size=200",
//                        token
//                    )
//            }
//        }


        setController()

        nav_view.setNavigationItemSelectedListener(this)
    }

    //connect to the service
    private val musicConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as MusicBinder
            //get service
            musicPlayerService = binder.getService()
            //pass list
            musicPlayerService!!.setSongList(activePlaylist)
            musicBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            musicBound = false
        }
    }

    override fun onStart() {
        Log.i("PlaylistActivity", "onStart Called!")
        super.onStart()
        if (playIntent == null) {
            playIntent = Intent(this@PlaylistActivity, MusicPlayerService::class.java)
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE)
            startService(playIntent)
        }
//        musicPlayerService!!.setSongList(activePlaylist)
    }

    override fun onClick(view: View, position: Int) {
        Log.i("PlaylistActivity", "onClick called!")
        musicPlayerService!!.setSong(position)
        if(playbackPaused){
            setController()
            playbackPaused = false
        }
        musicPlayerService!!.playSong()
        controller.show(0) // Passing 0 so controller always shows
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
        when (item.itemId) {
            R.id.action_shuffle -> {
                musicPlayerService!!.setShuffle()
            }
            R.id.action_end -> {
                stopService(playIntent)
                musicPlayerService = null
                exitProcess(0)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        stopService(playIntent)
        musicPlayerService = null
        super.onDestroy()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_login -> {
                val intent = Intent(applicationContext, MainActivity::class.java)
                intent.putExtra("token", token)
                intent.putExtra("username", userName)
                startActivity(intent)
            }

            R.id.nav_playlists -> {
                val intent = Intent(applicationContext, PlaylistActivity::class.java)
                intent.putExtra("token", token)
                intent.putExtra("username", userName)
                startActivity(intent)
            }
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun setController() {
        controller = MusicController(this@PlaylistActivity)
        controller.setPrevNextListeners({ playNext() }, { playPrevious() })
        controller.setMediaPlayer(this)
        controller.setAnchorView(findViewById(R.id.rv_playlist))
        controller.isEnabled = true
    }

    private fun playNext() {
        musicPlayerService!!.playNext()
        if (playbackPaused) {
            setController()
            playbackPaused = false
        }
        controller.show(0)
    }

    private fun playPrevious() {
        musicPlayerService!!.playPrevious()
        if (playbackPaused) {
            setController()
            playbackPaused = false
        }
        controller.show(0)
    }

    override fun isPlaying(): Boolean {
        return if (musicPlayerService != null && musicBound) musicPlayerService!!.isPlaying()
        else false
    }

    override fun canSeekForward(): Boolean {
        return true
    }

    override fun getDuration(): Int {
        Log.i("PlaylistActivity", "Call to getDuration!")
        return if (musicPlayerService != null && musicBound && musicPlayerService!!.isPlaying()) musicPlayerService!!.getDuration()
        else 0
    }

    override fun pause() {
        playbackPaused = true
        musicPlayerService!!.pausePlayer()
    }

    override fun seekTo(pos: Int) {
        musicPlayerService!!.seek(pos)
    }

    override fun getCurrentPosition(): Int {
        return if (musicPlayerService != null && musicBound && musicPlayerService!!.isPlaying()) musicPlayerService!!.getPosition()
        else 0
    }

    override fun canSeekBackward(): Boolean {
        return true
    }

    override fun start() {
        musicPlayerService!!.start()
    }

    override fun canPause(): Boolean {
        return true
    }

    override fun onPause() {
        super.onPause()
        paused = true
    }

    override fun onResume() {
        super.onResume()
        if (paused) {
            setController()
            paused = false
        }
    }

    override fun onStop() {
        controller.hide()
        super.onStop()
    }

    override fun getBufferPercentage(): Int {
        return musicPlayerService!!.getBufferPercentage()
    }

    override fun getAudioSessionId(): Int {
        return musicPlayerService!!.getAudioSessionId()
    }
}
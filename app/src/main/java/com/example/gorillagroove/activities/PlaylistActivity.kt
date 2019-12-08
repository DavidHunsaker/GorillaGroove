package com.example.gorillagroove.activities

// TODO: Make this a fragment

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.MediaController.MediaPlayerControl
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gorillagroove.R
import com.example.gorillagroove.adapters.OnItemClickListener
import com.example.gorillagroove.adapters.PlaylistAdapter
import com.example.gorillagroove.client.authenticatedGetRequest
import com.example.gorillagroove.client.playlistGetRequest
import com.example.gorillagroove.controller.MusicController
import com.example.gorillagroove.db.GroovinDB
import com.example.gorillagroove.db.repository.UserRepository
import com.example.gorillagroove.dto.PlaylistDTO
import com.example.gorillagroove.dto.PlaylistSongDTO
import com.example.gorillagroove.dto.Track
import com.example.gorillagroove.dto.Users
import com.example.gorillagroove.service.MusicPlayerService
import com.example.gorillagroove.service.MusicPlayerService.MusicBinder
import com.example.gorillagroove.utils.URLs
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.activity_main.drawer_layout
import kotlinx.android.synthetic.main.activity_playlist.nav_view
import kotlinx.android.synthetic.main.app_bar_main.toolbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import kotlin.system.exitProcess

class PlaylistActivity : AppCompatActivity(),
    CoroutineScope by MainScope(), MediaPlayerControl, OnItemClickListener,
    NavigationView.OnNavigationItemSelectedListener {

    private val om =
        ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private var musicBound = false
    private var token: String = ""
    private var email: String = ""
    private var userName: String = ""
    private var playbackPaused = false
    private var playIntent: Intent? = null
    private var users: List<Users> = emptyList()
    private var playlists: List<PlaylistDTO> = emptyList()
    private var musicPlayerService: MusicPlayerService? = null
    private var activeSongsList: List<PlaylistSongDTO> = emptyList()

    private lateinit var recyclerView: RecyclerView
    private lateinit var repository: UserRepository
    private lateinit var controller: MusicController
    private var songCurrentPosition = 0
    private var songCurrentDuration = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist)
        setSupportActionBar(toolbar)

        repository = UserRepository(GroovinDB.getDatabase(this@PlaylistActivity).userRepository())

        if (EventBus.getDefault().isRegistered(this@PlaylistActivity)) {
            EventBus.getDefault().register(this@PlaylistActivity)
        }

        val toggle = ActionBarDrawerToggle(
            this@PlaylistActivity,
            drawer_layout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        token = intent.getStringExtra("token")
        userName = intent.getStringExtra("username")
        email = intent.getStringExtra("email")

        loadLibrarySongs()
        requestUsers()
        requestPlaylists()
        setController()

        nav_view.setNavigationItemSelectedListener(this@PlaylistActivity)
    }

    private fun loadLibrarySongs() {
        val response = runBlocking { authenticatedGetRequest(URLs.LIBRARY, token) }

        val content: String = response.get("content").toString()

        activeSongsList =
            om.readValue(content, arrayOf(Track())::class.java).map { PlaylistSongDTO(0, it) }
                .toList()

        attachSongsListToAdapter()
        if(musicBound) musicPlayerService!!.setSongList(activeSongsList)
    }

    private fun loadPlaylistSongs(playlistId: Long) {
        val response = runBlocking {
            authenticatedGetRequest(
                "${URLs.PLAYLIST_TEMPLATE}playlistId=$playlistId&size=200",
                token
            )
        }
        val content: String = response.get("content").toString()
        activeSongsList = om.readValue(content, arrayOf(PlaylistSongDTO())::class.java).toList()
        attachSongsListToAdapter()
        if(musicBound) musicPlayerService!!.setSongList(activeSongsList)
    }

    private fun loadUserLibraries(userId: Long) {
        val response =
            runBlocking { authenticatedGetRequest("${URLs.LIBRARY}&userId=$userId", token) }
        val content: String = response.get("content").toString()
        activeSongsList =
            om.readValue(content, arrayOf(Track())::class.java).map { PlaylistSongDTO(0, it) }
                .toList()
        attachSongsListToAdapter()
        if(musicBound) musicPlayerService!!.setSongList(activeSongsList)
    }

    private fun attachSongsListToAdapter() {
        recyclerView = findViewById(R.id.rv_playlist)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val playlistAdapter = PlaylistAdapter(activeSongsList)
        recyclerView.adapter = playlistAdapter
        playlistAdapter.setClickListener(this)
    }

    private fun requestUsers() {
        val response = runBlocking { playlistGetRequest(URLs.USER, token) }
        if (response.length() > 0) {
            users = om.readValue(response.toString(), arrayOf(Users())::class.java).toList()
            val menu = nav_view.menu
            val subMenu = menu.addSubMenu("User Libraries")
            users.forEach { subMenu.add(2, it.id.toInt(), 0, it.username) }
        }
    }

    private fun requestPlaylists() {
        val response = runBlocking { playlistGetRequest(URLs.PLAYLISTS, token) }
        if (response.length() > 0) {
            playlists =
                om.readValue(response.toString(), arrayOf(PlaylistDTO())::class.java).toList()
            val menu = nav_view.menu
            val subMenu = menu.addSubMenu("Playlists")
            playlists.forEach { subMenu.add(1, it.id.toInt(), 1, it.name) }
        }
    }

    //connect to the service
    private val musicConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as MusicBinder
            //get service
            musicPlayerService = binder.getService()
            //pass list
            musicPlayerService!!.setSongList(activeSongsList)
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
                .putExtra("email", email)

            bindService(playIntent, musicConnection, Context.BIND_IMPORTANT)
            startService(playIntent)
        }
        if (!EventBus.getDefault().isRegistered(this@PlaylistActivity)) {
            EventBus.getDefault().register(this@PlaylistActivity)
        }
    }

    override fun onClick(view: View, position: Int) {
        Log.i("PlaylistActivity", "onClick called!")
        musicPlayerService!!.setSong(position)
        setController()
        playbackPaused = false
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
                val message = if (musicPlayerService!!.setShuffle()) "Shuffle On" else "Shuffle Off"
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
            R.id.action_end -> {
                stopService(playIntent)
                musicPlayerService = null
                exitProcess(0)
            }
            R.id.action_settings_logout -> {
                val user =
                    runBlocking { withContext(Dispatchers.IO) { repository.findUser(email) } }
                logout(user!!.id)
                exitProcess(0)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        if (EventBus.getDefault().isRegistered(this@PlaylistActivity)) {
            EventBus.getDefault().unregister(this@PlaylistActivity)
        }
        stopService(playIntent)
        musicPlayerService = null
        super.onDestroy()
    }

    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    fun onEndOfSongEvent(event: EndOfSongEvent) {
        Log.i("EventBus", "Message received ${event.message}")
        playNext()
    }

    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    fun onMediaPlayerLoadedEvent(event: MediaPlayerLoadedEvent) {
        Log.i("EventBus", "Message received ${event.message}")
        controller.show(0)
    }

    private fun setController() {
        controller = MusicController(this@PlaylistActivity)
        controller.setPrevNextListeners({ playNext() }, { playPrevious() })
        controller.setMediaPlayer(this)
        controller.setAnchorView(findViewById(R.id.rv_playlist))
        controller.isEnabled = true
        playbackPaused = false
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
        if (musicPlayerService != null && musicBound && musicPlayerService!!.isPlaying()) songCurrentDuration =
            musicPlayerService!!.getDuration()
        return songCurrentDuration
    }

    override fun pause() {
        playbackPaused = true
        musicPlayerService!!.pausePlayer()
    }

    override fun seekTo(pos: Int) {
        musicPlayerService!!.seek(pos)
    }

    override fun getCurrentPosition(): Int {
        if (musicPlayerService != null && musicBound && musicPlayerService!!.isPlaying()) songCurrentPosition =
            musicPlayerService!!.getPosition()
        return songCurrentPosition
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
        playbackPaused = true
    }

    override fun onResume() {
        super.onResume()
        if (playbackPaused) {
            setController()
            playbackPaused = false
        }
    }

    override fun getBufferPercentage(): Int {
        return musicPlayerService!!.getBufferPercentage()
    }

    override fun getAudioSessionId(): Int {
        return musicPlayerService!!.getAudioSessionId()
    }

    private fun logout(userId: Long) {
        runBlocking {
            withContext(Dispatchers.IO) {
                repository.logout(userId)
                stopService(playIntent)
                musicPlayerService = null
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_library -> {
                loadLibrarySongs()
            }
        }
        when (item.groupId) {
            1 -> {
                loadPlaylistSongs(item.itemId.toLong())
            }
            2 -> {
                loadUserLibraries(item.itemId.toLong())
            }
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }
}

class EndOfSongEvent(message: String) {
    val message = message
}

class MediaPlayerLoadedEvent(message: String) {
    val message = message
}
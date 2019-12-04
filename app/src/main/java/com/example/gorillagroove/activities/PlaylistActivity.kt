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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.gorillagroove.R
import com.example.gorillagroove.adapters.OnItemClickListener
import com.example.gorillagroove.adapters.PlaylistAdapter
import com.example.gorillagroove.client.authenticatedGetRequest
import com.example.gorillagroove.controller.MusicController
import com.example.gorillagroove.db.GroovinDB
import com.example.gorillagroove.db.repository.UserRepository
import com.example.gorillagroove.dto.PlaylistSongDTO
import com.example.gorillagroove.dto.Track
import com.example.gorillagroove.service.MusicPlayerService
import com.example.gorillagroove.service.MusicPlayerService.MusicBinder
import com.example.gorillagroove.utils.URLs
import com.fasterxml.jackson.databind.DeserializationConfig
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.android.synthetic.main.activity_main.drawer_layout
import kotlinx.android.synthetic.main.app_bar_main.toolbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject
import kotlin.system.exitProcess

private const val PLAYLIST_ACTIVITY_TAG = "PlaylistActivity"

class PlaylistActivity : AppCompatActivity(),
    CoroutineScope by MainScope(), MediaPlayerControl, OnItemClickListener,
    SwipeRefreshLayout.OnRefreshListener {

    private val om = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private var musicBound = false
    private var token: String = ""
    private var email: String = ""
    private var userName: String = ""
    private var playbackPaused = false
    private var playIntent: Intent? = null
    private var musicPlayerService: MusicPlayerService? = null
    private var activePlaylist: List<PlaylistSongDTO> = emptyList()

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var repository: UserRepository
    private lateinit var controller: MusicController
    private var songCurrentPosition = 0
    private var songCurrentDuration = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist)
        setSupportActionBar(toolbar)

        swipeRefreshLayout = findViewById(R.id.swipe_view)
        swipeRefreshLayout.setOnRefreshListener {
            Log.i(PLAYLIST_ACTIVITY_TAG, "Refreshing Songs!")
            onRefresh()
        }
        repository = UserRepository(GroovinDB.getDatabase(this@PlaylistActivity).userRepository())

        if (EventBus.getDefault().isRegistered(this@PlaylistActivity)) {
            EventBus.getDefault().register(this@PlaylistActivity)
        }

        token = intent.getStringExtra("token")
        userName = intent.getStringExtra("username")
        email = intent.getStringExtra("email")

//        val response = runBlocking { authenticatedGetRequest(URLs.LIBRARY, token) }

        val response = JSONObject()
        if (response.length() > 0) {
            val content: String = response.get("content").toString()

            activePlaylist =
                om.readValue(content, arrayOf(Track())::class.java).map { PlaylistSongDTO(0, it) }
                    .toList()
        }
        recyclerView = findViewById(R.id.rv_playlist)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val playlistAdapter = PlaylistAdapter(activePlaylist)
        recyclerView.adapter = playlistAdapter
        playlistAdapter.setClickListener(this)

        setController()
    }

    override fun onRefresh() {

        val response = runBlocking { authenticatedGetRequest(URLs.LIBRARY, token) }

        if(response.length() > 0) {
            val content: String = response.get("content").toString()

            activePlaylist =
                om.readValue(content, arrayOf(Track())::class.java)
                    .map { PlaylistSongDTO(0, it) }
                    .toList()

            val playlistAdapter = PlaylistAdapter(activePlaylist)
            recyclerView.adapter = playlistAdapter
            playlistAdapter.setClickListener(this)
        }

        swipeRefreshLayout.isRefreshing = false
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
                .putExtra("email", email)

            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE)
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
                return true
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
            R.id.action_reload_songs -> {
                Log.i(PLAYLIST_ACTIVITY_TAG, "Reload Songs Menu Item Selected")
                swipeRefreshLayout.isRefreshing = true
                onRefresh()
                return true
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
}

class EndOfSongEvent(message: String) {
    val message = message
}

class MediaPlayerLoadedEvent(message: String) {
    val message = message
}
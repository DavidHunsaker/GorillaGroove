package com.example.gorillagroove.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import com.example.gorillagroove.R
import com.example.gorillagroove.activities.PlaylistActivity
import com.example.gorillagroove.db.GroovinDB
import com.example.gorillagroove.db.repository.UserRepository
import com.example.gorillagroove.dto.PlaylistSongDTO
import com.example.gorillagroove.dto.TrackResponse
import com.example.gorillagroove.volleys.authenticatedGetRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext


class MusicPlayerService : Service(), MediaPlayer.OnPreparedListener,
    MediaPlayer.OnErrorListener,
    MediaPlayer.OnCompletionListener, CoroutineScope by MainScope() {

    private val player = MediaPlayer()
    private lateinit var songs: List<PlaylistSongDTO>
    private lateinit var userRepository: UserRepository
    private val NOTIFY_ID = 1

    private val musicBind: IBinder = MusicBinder()
    private var songPosition = 0

    private var shuffle = false
    private var songTitle = ""
    private var token = ""

    override fun onCreate() {
        Log.i("MSP", "onCreate is called")
        super.onCreate()
        songPosition = 0
        initMusicPlayer()
        userRepository =
            UserRepository(GroovinDB.getDatabase(this@MusicPlayerService).userRepository())
        launch {
            withContext(Dispatchers.IO) {
                userToken()
            }
        }
    }

    private fun userToken() {
        try {
            token = userRepository.findUser("test@gorilla.groove")!!.token!!
        } catch (e: Exception) {
            Log.e("MusicPlayerService", "something bad happened: $e")
        }
    }

    private fun initMusicPlayer() {
        Log.i("MSP", "Initializing Media Player")
        player.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
        player.setAudioStreamType(AudioManager.STREAM_MUSIC) // We are supported API 19, and thus need this deprecated method
        player.setOnPreparedListener(this)
        player.setOnCompletionListener(this)
        player.setOnErrorListener(this)
    }

    fun setSongList(songList: List<PlaylistSongDTO>) {
        songs = songList
    }

    fun setSong(songIndex: Int) {
        songPosition = songIndex
    }

    fun getPosition(): Int {
        return player.currentPosition
    }

    fun getDuration(): Int {
        return player.duration
    }

    fun isPlaying(): Boolean {
        return player.isPlaying
    }

    fun pausePlayer() {
        player.pause()
    }

    fun start() {
        player.start()
    }

    fun seek(position: Int) {
        player.seekTo(position)
    }

    fun getBufferPercentage(): Int {
        return (player.currentPosition * 100) / player.duration
    }

    fun getAudioSessionId(): Int {
        return player.audioSessionId
    }

    fun playPrevious() {
        songPosition -= 1
        if (songPosition < 0) songPosition = songs.size - 1
        playSong()
    }

    fun playNext() {
        songPosition += 1
        if (songPosition > songs.size) songPosition = 0
        Log.i("MusicPlayerService", "Current songPosition is now=$songPosition")
        playSong()
    }

    inner class MusicBinder : Binder() {

        fun getService(): MusicPlayerService {
            return this@MusicPlayerService
        }
    }

    override fun onPrepared(mp: MediaPlayer) {
        mp.start()
        val notIntent = Intent(applicationContext, PlaylistActivity::class.java)
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            notIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = Notification.Builder(applicationContext)

        builder.setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.play)
            .setTicker(songTitle)
            .setOngoing(true)
            .setContentTitle("Playing")
            .setContentText(songTitle)
        val not = builder.build()

        startForeground(NOTIFY_ID, not)
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        mp!!.reset()
        return false
    }

    override fun onCompletion(mp: MediaPlayer?) {
        if (player.currentPosition > 0) mp!!.reset()

        // FIXME Song play count update here?
        playNext()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return musicBind
    }

    override fun onUnbind(intent: Intent?): Boolean {
        player.stop()
        player.release()
        return false
    }

    override fun onDestroy() {
        stopForeground(true)
    }

    fun setShuffle() {
        Log.i("Shuffle Alert!", "Shuffle is now set to $shuffle")
        shuffle = !shuffle
    }

    fun playSong() {
        player.reset()
        val song = songs[songPosition]
        songTitle = song.track.name.toString()

        val trackResponse = getSongStreamInfo(song.track.id)

        try {
            player.setDataSource(trackResponse.songLink)
            player.prepare()
        } catch (e: Exception) {
            Log.e("MusicPlayerService", "Error setting data source: $e")
        }
    }

    private fun getSongStreamInfo(trackId: Long): TrackResponse {
        Log.d("MusicPlayerService", "Geting song info for track=$trackId with token=$token")

        val url = "http://gorillagroove.net/api/file/link/$trackId"
        val response = runBlocking { authenticatedGetRequest(url, token) }

        return TrackResponse(response["songLink"].toString(), response["albumArtLink"].toString())
    }
}
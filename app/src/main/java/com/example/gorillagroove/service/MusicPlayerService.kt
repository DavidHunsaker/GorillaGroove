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
import com.example.gorillagroove.activities.MainActivity
import com.example.gorillagroove.db.GroovinDB
import com.example.gorillagroove.db.repository.UserRepository
import com.example.gorillagroove.dto.PlaylistSongDTO
import com.example.gorillagroove.dto.TrackResponse
import com.example.gorillagroove.volleys.SongsRequest
import com.example.gorillagroove.volleys.SongsVolley
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Random


class MusicPlayerService() : Service(), MediaPlayer.OnPreparedListener,
    MediaPlayer.OnErrorListener,
    MediaPlayer.OnCompletionListener, CoroutineScope by MainScope(), SongsVolley {

    private lateinit var player: MediaPlayer
    private lateinit var songs: List<PlaylistSongDTO>
    private lateinit var random: Random
    private lateinit var userRepository: UserRepository

    private val NOTIFY_ID = 1
    private val musicBind: IBinder = MusicBinder()

    private var songPosition = 1
    private var shuffle = false
    private var songTitle = ""
    private var token = ""
    private var currentTrackResponse: TrackResponse? = null

    override fun onCreate() {
        super.onCreate()
        songPosition = 0
        player = MediaPlayer()
        initMusicPlayer()
        userRepository =
            UserRepository(GroovinDB.getDatabase(this@MusicPlayerService).userRepository())
        launch {
            withContext(Dispatchers.IO) {
                userToken()
            }
        }
    }

    private suspend fun userToken() {
        Log.i("Yeah Boi!!", "We're fucking grabbing a beautiful token!")
        try {
            token = userRepository.findUser("david.e.hunsaker@gmail.com")!!.token!!
        } catch (e: Exception) {
            Log.e("MusicPlayerService", "oh fuck: $e")
        }
    }

    private fun initMusicPlayer() {
        player.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
        player.setAudioStreamType(AudioManager.STREAM_MUSIC) // We are targeting API 19, and thus need this deprecated method
        player.setOnPreparedListener(this@MusicPlayerService)
        player.setOnCompletionListener(this@MusicPlayerService)
        player.setOnErrorListener(this@MusicPlayerService)
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
        if (shuffle) {
            var newSong = songPosition
            while (newSong == songPosition) {
                newSong = random.nextInt(songs.size)
            }
            songPosition = newSong
        } else {
            songPosition += 1
            if (songPosition > songs.size) songPosition = 0
        }
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
        val notIntent = Intent(this@MusicPlayerService, MainActivity::class.java)
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this@MusicPlayerService,
            0,
            notIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = Notification.Builder(this@MusicPlayerService)

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
        if (player.currentPosition > 0) {
            mp!!.reset()
            // FIXME Song play count update here?
            playNext()
        }
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
        val playSong = songs[songPosition]
        songTitle = playSong.track.name.toString()

        // FIXME This is likely where I need to make a request to grab the song
        launch {
            getSongStreamInfo(playSong.track.id)


            try {
                player.setDataSource(currentTrackResponse!!.trackLink)
                player.prepareAsync()
            } catch (e: Exception) {
                Log.e("MusicPlayerService", "Error setting data source: $e")
            }
        }

    }

    private suspend fun getSongStreamInfo(trackId: Long) {
        withContext(Dispatchers.IO) {
            Log.d("MusicPlayerService", "Geting song info for track=$trackId with token=$token")
            SongsRequest.getInstance(this@MusicPlayerService, this@MusicPlayerService)
                .getSongRequest(
                    "http://gorillagroove.net/api/file/link/$trackId",
                    token
                )
        }
    }

    override fun onGetSongResponse(response: JSONObject) {
        currentTrackResponse =
            TrackResponse(response["songLink"].toString(), response["albumArtLink"].toString())
    }

}
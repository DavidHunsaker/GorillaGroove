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
import com.example.gorillagroove.client.authenticatedGetRequest
import com.example.gorillagroove.client.markListenedRequest
import com.example.gorillagroove.db.GroovinDB
import com.example.gorillagroove.db.repository.UserRepository
import com.example.gorillagroove.dto.PlaylistSongDTO
import com.example.gorillagroove.dto.TrackResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.Random

private const val markListenedUrl = "https://gorillagroove.net/api/track/mark-listened"
private const val trackUrl = "https://gorillagroove.net/api/file/link/"

class MusicPlayerService : Service(), MediaPlayer.OnPreparedListener,
    MediaPlayer.OnErrorListener,
    MediaPlayer.OnCompletionListener, CoroutineScope by MainScope() {
    private val player = MediaPlayer()
    private lateinit var songs: List<PlaylistSongDTO>
    private lateinit var userRepository: UserRepository
    private val NOTIFY_ID = 1

    private val musicBind: IBinder = MusicBinder()
    private var songPosition = 0
    private val random = Random()

    private var lastRecordedTime = 0
    private var playCountPosition = 0
    private var playCountDuration = 0
    private var markedListened = false
    private var previousShuffle = 0

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
        val currentTime = System.currentTimeMillis().toInt()
        val elapsedPosition = currentTime - lastRecordedTime
        lastRecordedTime = currentTime
        playCountPosition += elapsedPosition // Milliseconds
        if (!markedListened && playCountDuration > 0 && playCountPosition >= playCountDuration) {
            markListened(playCountPosition, player.currentPosition, playCountDuration)
            markedListened = true
        }
        return player.currentPosition
    }

    fun getDuration(): Int {
        if (playCountDuration == 0) playCountDuration = (player.duration * 0.6).toInt()
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
        lastRecordedTime = System.currentTimeMillis().toInt()
    }

    fun seek(position: Int) {
        player.seekTo(position)
        lastRecordedTime = System.currentTimeMillis().toInt()
    }

    fun getBufferPercentage(): Int {
        return (player.currentPosition * 100) / player.duration
    }

    fun getAudioSessionId(): Int {
        return player.audioSessionId
    }

    fun playPrevious() {
        if (shuffle) {
            songPosition = previousShuffle
        } else {
            songPosition -= 1
            if (songPosition < 0) songPosition = songs.size - 1
        }
        playSong()
    }

    fun playNext() {
        if (shuffle) {
            previousShuffle = songPosition
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
        lastRecordedTime = System.currentTimeMillis().toInt()
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
        clearPlayCountInfo()
        mp!!.reset()
        return false
    }

    override fun onCompletion(mp: MediaPlayer?) {
        clearPlayCountInfo()
        if (player.currentPosition > 0) mp!!.reset()
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
        shuffle = !shuffle
        Log.i("Shuffle Alert!", "Shuffle is now set to $shuffle")
    }

    fun playSong() {
        player.reset()
        clearPlayCountInfo()
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

    private fun clearPlayCountInfo() {
        playCountPosition = 0
        playCountDuration = 0
        markedListened = false
    }

    private fun getSongStreamInfo(trackId: Long): TrackResponse {
        Log.d("MusicPlayerService", "Geting song info for track=$trackId with token=$token")

        val response = runBlocking { authenticatedGetRequest(trackUrl+"$trackId", token) }

        return TrackResponse(response["songLink"].toString(), response["albumArtLink"].toString())
    }

    private fun markListened(
        playCountPosition: Int,
        playerCurrentPosition: Int,
        playCountDuration: Int
    ) {
        val trackId = songs[songPosition].track.id
        Log.d(
            "MusicPlayerService",
            "Marking track=$trackId as listened with:\nplayCountPosition=$playCountPosition\nplayerCurrentPosition=$playerCurrentPosition\nplayCountDuration=$playCountDuration"
        )
        launch { withContext(Dispatchers.IO) { markListenedRequest(markListenedUrl, trackId, token) } }
    }
}
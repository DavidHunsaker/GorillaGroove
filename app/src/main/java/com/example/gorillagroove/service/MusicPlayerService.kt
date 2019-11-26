package com.example.gorillagroove.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.gorillagroove.R
import com.example.gorillagroove.activities.EndOfSongEvent
import com.example.gorillagroove.activities.MediaPlayerLoadedEvent
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
import org.greenrobot.eventbus.EventBus

private const val markListenedUrl = "https://gorillagroove.net/api/track/mark-listened"
private const val trackUrl = "https://gorillagroove.net/api/file/link/"

class MusicPlayerService : Service(), MediaPlayer.OnPreparedListener,
    MediaPlayer.OnErrorListener,
    MediaPlayer.OnCompletionListener, CoroutineScope by MainScope() {
    private val NOTIFY_ID = 1
    private val player = MediaPlayer()
    private val musicBind: IBinder = MusicBinder()

    private var token = ""
    private var email = ""
    private var songTitle = ""
    private var artist = ""
    private var shuffle = false
    private var songPosition = 0
    private var lastRecordedTime = 0
    private var playCountPosition = 0
    private var playCountDuration = 0
    private var markedListened = false
    private var currentSongPosition = 0
    private var shuffledSongs: List<Int> = emptyList()

    private lateinit var userRepository: UserRepository
    private lateinit var songs: List<PlaylistSongDTO>

    override fun onCreate() {
        Log.i("MSP", "onCreate is called")
        super.onCreate()
        songPosition = 0
        initMusicPlayer()

        userRepository =
            UserRepository(GroovinDB.getDatabase(this@MusicPlayerService).userRepository())
        if (token.isBlank()) {
            launch {
                withContext(Dispatchers.IO) {
                    userToken()
                }
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    private fun userToken() {
        try {
            token = userRepository.findUser(email)!!.token!!
        } catch (e: Exception) {
            Log.e("MusicPlayerService", "User not found!: $e")
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
        currentSongPosition = songIndex
        if (shuffle) {
            songPosition = shuffledSongs.indexOf(songIndex)
        }
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
        songPosition -= 1
        if (songPosition < 0) songPosition = songs.size - 1

        playSong()
    }

    fun playNext() {
        songPosition += 1
        if (songPosition >= songs.size) songPosition = 0

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val NOTIFICATION_CHANNEL_ID = "channel_id"
            val CHANNEL_NAME = "Notification Channel"
            val importance = NotificationManager.IMPORTANCE_LOW
            val notificationChannel =
                NotificationChannel(NOTIFICATION_CHANNEL_ID, CHANNEL_NAME, importance)
            notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)

            val notificationCompat = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Gorilla Groove")
                .setContentText("$songTitle - $artist")
                .setSmallIcon(R.drawable.logo)
                .setTicker("$songTitle - $artist")
                .setOngoing(true)

            notificationManager.notify(NOTIFY_ID, notificationCompat.build())
        } else {

            val builder = Notification.Builder(applicationContext)

            builder.setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.logo)
                .setTicker("$songTitle - $artist")
                .setOngoing(true)
                .setContentTitle("Gorilla Groove")
                .setContentText("$songTitle - $artist")


            val not = builder.build()

            startForeground(NOTIFY_ID, not)
        }
        EventBus.getDefault().post(MediaPlayerLoadedEvent("Media Player Loaded, now Showing"))
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        clearPlayCountInfo()
        mp!!.reset()
        return false
    }

    override fun onCompletion(mp: MediaPlayer?) {
        clearPlayCountInfo()
        if (player.currentPosition > 0) mp!!.reset()
        EventBus.getDefault().post(EndOfSongEvent("Resetting Music Player"))
    }

    override fun onBind(intent: Intent?): IBinder? {
        if (intent != null) {
            if (intent.hasExtra("email")) email = intent.getStringExtra("email")
            if (intent.hasExtra("token")) token = intent.getStringExtra("token")
        }
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
        if (shuffle) {
            shuffledSongs = songs.indices.toList().shuffled()
            Log.i(
                "Shuffling",
                "Shuffled list is: $shuffledSongs\n Songs list is ${songs.indices.toList()}"
            )
        } else {
            songPosition = currentSongPosition
        }
        Log.i("Shuffle Alert!", "Shuffle is now set to $shuffle")
    }

    fun playSong() {
        player.reset()
        clearPlayCountInfo()
        val song = if (shuffle) {
            currentSongPosition = shuffledSongs[songPosition]
            songs[currentSongPosition]
        } else {
            songs[songPosition]
        }
        songTitle = song.track.name.toString()
        artist = song.track.artist.toString()

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
        Log.d("MusicPlayerService", "Getting song info for track=$trackId with token=$token")

        val response = runBlocking { authenticatedGetRequest(trackUrl + "$trackId", token) }

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
        launch {
            withContext(Dispatchers.IO) {
                markListenedRequest(
                    markListenedUrl,
                    trackId,
                    token
                )
            }
        }
    }


    fun higherSDKNotifications(): NotificationChannel? {

        return null
    }
}
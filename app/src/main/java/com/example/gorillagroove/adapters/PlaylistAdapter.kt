package com.example.gorillagroove.adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.gorillagroove.R
import com.example.gorillagroove.dto.SongRequest

class PlaylistAdapter(private val values: List<SongRequest>) : RecyclerView.Adapter<PlaylistAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.playlist_song_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun getItemCount() = values.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.songTitle?.text = values[position].title
        holder.artist?.text = values[position].artist
    }

    class ViewHolder(itemView: View?): RecyclerView.ViewHolder(itemView!!) {
        var songTitle: TextView? = null
        var artist: TextView? = null
        init {
            songTitle = itemView?.findViewById(R.id.tv_song_title)
            artist = itemView?.findViewById(R.id.tv_artist_name)
        }
    }
}


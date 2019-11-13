package com.example.gorillagroove.adapters

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.gorillagroove.R
import com.example.gorillagroove.dto.PlaylistSongDTO

class PlaylistAdapter(private val values: List<PlaylistSongDTO>) :
    androidx.recyclerview.widget.RecyclerView.Adapter<PlaylistAdapter.ViewHolder>() {

    private lateinit var clickListener: OnItemClickListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.playlist_song_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun getItemCount() = values.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.songTitle.text = values[position].track.name
        holder.artist.text = values[position].track.artist
        holder.itemView.tag = position
    }

    fun setClickListener(itemClickListener: OnItemClickListener){
        this.clickListener = itemClickListener
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        // each data item is just a string in this case
        val songTitle: TextView = itemView.findViewById(R.id.tv_song_title)
        val artist: TextView = itemView.findViewById(R.id.tv_artist_name)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            var currentPosition = adapterPosition
            if(currentPosition <= 0){
                currentPosition = 0
            } else if(currentPosition >= itemCount) {
                currentPosition = (itemCount - 1)
            }
            clickListener.onClick(view, currentPosition)
        }
    }
}

interface OnItemClickListener {
    fun onClick(view: View, position: Int)
}


package com.example.gorillagroove.dto

import java.time.ZonedDateTime

data class PlaylistDTO(
    val id: Long,
    val name: String,
    val createdOn: ZonedDateTime
)
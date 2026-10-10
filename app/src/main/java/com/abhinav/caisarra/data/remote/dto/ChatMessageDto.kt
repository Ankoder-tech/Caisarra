package com.abhinav.caisarra.data.remote.dto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ChatMessageDto(
    val id: String,

    @SerialName("game_id")
    val gameId: String,

    @SerialName("sender_id")
    @JsonNames("user_id")
    val senderId: Long,

    val username: String? = null,

    val message: String,

    @SerialName("created_at")
    val createdAt: String
)

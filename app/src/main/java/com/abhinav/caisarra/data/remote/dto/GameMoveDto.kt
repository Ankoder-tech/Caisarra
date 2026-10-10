
package com.abhinav.caisarra.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GameMoveDto(
    val id: String,

    @SerialName("game_id")
    val gameId: String,

    @SerialName("move_number")
    val moveNumber: Int,

    @SerialName("player_id")
    val playerId: String,
    val move: String,

    @SerialName("position_after")
    val positionAfter: String,

    @SerialName("created_at")
    val createdAt: String
)

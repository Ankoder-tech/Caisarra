
package com.abhinav.caisarra.data.repository

import com.abhinav.caisarra.data.local.entity.GameEntity
import com.abhinav.caisarra.data.local.entity.RecordStatus
import com.abhinav.caisarra.data.remote.api.GamesService
import com.abhinav.caisarra.data.remote.dto.GameHistoryDto
import com.abhinav.caisarra.data.remote.dto.GameMoveDto
import java.time.Instant

class RemoteGameRepository(
    private val gamesService: GamesService
) {

    suspend fun getGameHistory(
        username: String
    ): List<GameEntity> {
        return gamesService.getGameHistory().map { remote ->
            remote.toGameEntity(username)
        }
    }
    suspend fun getGameMoves(
        gameId: String
    ): List<GameMoveDto> {
        return gamesService.getGameMoves(gameId)
            .sortedBy { it.moveNumber }
    }

    private fun GameHistoryDto.toGameEntity(
        username: String
    ): GameEntity {
        val isFinished = status.equals(
            "finished",
            ignoreCase = true
        )

        return GameEntity(
            id = id,
            ownerId = username,
            whiteName = "White #$whitePlayerId",
            blackName = "Black #$blackPlayerId",
            timeControlMinutes = timeControlMinutes,
            incrementSeconds = (incrementMs / 1000L).toInt(),
            moves = "",
            moveTimes = "",
            whiteTimeMs = whiteTimeMs,
            blackTimeMs = blackTimeMs,

            result = when (result?.lowercase()) {
                "white_win" -> "WHITE_WINS"
                "black_win" -> "BLACK_WINS"
                "draw" -> "DRAW"
                else -> null
            },

            endReason = endReason,

            startedAt = parseTimestamp(
                startedAt ?: createdAt
            ),

            endedAt = endedAt?.let(::parseTimestamp),

            status = if (isFinished) {
                RecordStatus.Synced
            } else {
                RecordStatus.InProgress
            }
        )
    }

    private fun parseTimestamp(value: String): Long {
        return runCatching {
            Instant.parse(value).toEpochMilli()
        }.getOrElse {
            System.currentTimeMillis()
        }
    }
}

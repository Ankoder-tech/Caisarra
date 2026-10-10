
package com.abhinav.caisarra.data.remote.api

import com.abhinav.caisarra.data.remote.dto.GameHistoryDto
import com.abhinav.caisarra.data.remote.dto.GameMoveDto
import retrofit2.http.GET
import retrofit2.http.Path

interface GamesService {

    @GET("api/games/history")
    suspend fun getGameHistory(): List<GameHistoryDto>

    @GET("api/games/{gameID}/moves")
    suspend fun getGameMoves(
        @Path("gameID") gameId: String
    ): List<GameMoveDto>
}

package com.abhinav.caisarra.presentation.game.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhinav.caisarra.data.local.entity.GameEntity
import com.abhinav.caisarra.data.remote.dto.ChatMessageDto
import com.abhinav.caisarra.data.remote.dto.GameMoveDto
import com.abhinav.caisarra.data.repository.AuthRepository
import com.abhinav.caisarra.data.repository.ChatRepository
import com.abhinav.caisarra.data.repository.GameRepository
import com.abhinav.caisarra.data.repository.RemoteGameRepository
import com.abhinav.caisarra.domain.chess.engine.ChesslibEngine
import com.abhinav.caisarra.domain.chess.game.PassAndPlayGame
import com.abhinav.caisarra.domain.chess.game.mapper.toUi
import com.abhinav.caisarra.domain.chess.game.mapper.toUiBoard
import com.abhinav.caisarra.domain.chess.model.PieceColor
import com.abhinav.caisarra.domain.chess.model.toMove
import com.abhinav.caisarra.presentation.game.model.MoveUi
import com.abhinav.caisarra.presentation.game.model.Piece
import com.abhinav.caisarra.presentation.game.model.Square
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val STARTING_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

data class GameHistoryReviewState(
    val isLoading: Boolean = true,
    val game: GameEntity? = null,
    val currentPly: Int = 0,
    val totalPlies: Int = 0,
    val board: Map<Square, Piece> = emptyMap(),
    val whiteTimeMillis: Long = 0L,
    val blackTimeMillis: Long = 0L,
    val whitePlayerName: String = "White",
    val blackPlayerName: String = "Black",
    val moves: List<MoveUi> = emptyList(),
    val chatMessages: List<ChatMessageDto> = emptyList(),
    val chatError: String? = null,
    val isRegisteredGame: Boolean = false,
    val lastMoveFrom: Square? = null,
    val lastMoveTo: Square? = null,
    val isWhiteInCheck: Boolean = false,
    val isBlackInCheck: Boolean = false,
    val isWhiteTurn: Boolean = true,
    val error: String? = null
)

class GameHistoryReviewViewModel(
    private val repository: GameRepository,
    private val gameId: String,
    private val authRepository: AuthRepository,
    private val remoteGameRepository: RemoteGameRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _state = MutableStateFlow(
        GameHistoryReviewState()
    )
    val state = _state.asStateFlow()

    private var gameEntity: GameEntity? = null
    private var localMoves: List<String> = emptyList()
    private var moveTimes: List<Pair<Long, Long>> = emptyList()
    private var backendMoves: List<GameMoveDto>? = null

    init {
        loadGame()
    }


    private fun loadGame() {
        viewModelScope.launch {
            try {
                val username = authRepository.getUsername()
                    ?.takeIf { it.isNotBlank() }

                val isRegistered = username != null &&
                        authRepository.isLoggedIn()

                if (isRegistered) {
                    loadRegisteredGame(username!!)
                } else {
                    loadLocalGame()
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unable to load game history."
                    )
                }
            }
        }
    }


    private suspend fun loadRegisteredGame(username: String) {
        val game = remoteGameRepository
            .getGameHistory(username)
            .firstOrNull { it.id == gameId }

        if (game == null) {
            _state.update {
                it.copy(
                    isLoading = false,
                    error = "This game was not found in your server history."
                )
            }
            return
        }

        gameEntity = game

        _state.update {
            it.copy(
                game = game,
                whitePlayerName = game.whiteName,
                blackPlayerName = game.blackName,
                isRegisteredGame = true
            )
        }
        try {
            backendMoves = remoteGameRepository.getGameMoves(gameId)
            val remote = backendMoves.orEmpty()
            localMoves = emptyList()
            moveTimes = emptyList()

            _state.update {
                it.copy(
                    isLoading = false,
                    totalPlies = remote.size,
                    currentPly = remote.size,
                    moves = createBackendMoveList(remote),
                    error = null
                )
            }

            renderPosition(remote.size)
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    isLoading = false,
                    error = e.message
                        ?: "Unable to load moves from the server."
                )
            }
            return
        }

        val chatResult = chatRepository.getMessages(gameId)
        chatResult.fold(
            onSuccess = { messages ->
                _state.update {
                    it.copy(
                        chatMessages = messages.sortedBy { message ->
                            message.createdAt
                        },
                        chatError = null
                    )
                }
            },
            onFailure = { error ->
                _state.update {
                    it.copy(
                        chatError = error.message
                            ?: "Unable to load chat history."
                    )
                }
            }
        )
    }

    private suspend fun loadLocalGame() {
        val savedGame = repository.getById(gameId)

        if (savedGame == null) {
            _state.update {
                it.copy(
                    isLoading = false,
                    error = "Game history could not be found."
                )
            }
            return
        }

        gameEntity = savedGame
        backendMoves = null
        localMoves = repository.movesOf(savedGame)
        moveTimes = repository.moveTimesOf(savedGame)

        _state.update {
            it.copy(
                isLoading = false,
                game = savedGame,
                currentPly = localMoves.size,
                totalPlies = localMoves.size,
                whitePlayerName = savedGame.whiteName,
                blackPlayerName = savedGame.blackName,
                moves = createLocalMoveList(localMoves),
                chatMessages = emptyList(),
                chatError = null,
                isRegisteredGame = false
            )
        }

        renderPosition(localMoves.size)
    }

    fun previousMove() {
        val current = _state.value.currentPly
        if (current > 0) renderPosition(current - 1)
    }

    fun nextMove() {
        val current = _state.value.currentPly
        if (current < _state.value.totalPlies) {
            renderPosition(current + 1)
        }
    }

    fun jumpToMove(ply: Int) {
        renderPosition(
            ply.coerceIn(0, _state.value.totalPlies)
        )
    }

    private fun renderPosition(ply: Int) {
        val savedGame = gameEntity ?: return
        val remote = backendMoves

        if (remote != null) {
            renderBackendPosition(savedGame, remote, ply)
        } else {
            renderLocalPosition(savedGame, ply)
        }
    }

    private fun renderBackendPosition(
        game: GameEntity,
        remoteMoves: List<GameMoveDto>,
        ply: Int
    ) {
        val fen = if (ply == 0) {
            STARTING_FEN
        } else {
            remoteMoves.getOrNull(ply - 1)?.positionAfter
                ?: STARTING_FEN
        }

        try {
            val engine = ChesslibEngine()
            engine.loadFen(fen)
            val domainPieces = engine.pieces()
            val checkSquare = engine.checkSquare()
            val turn = engine.turn()

            _state.update {
                it.copy(
                    currentPly = ply,
                    totalPlies = remoteMoves.size,
                    board = domainPieces.toUiBoard(),
                    whiteTimeMillis = game.whiteTimeMs,
                    blackTimeMillis = game.blackTimeMs,
                    lastMoveFrom = null,
                    lastMoveTo = null,
                    isWhiteInCheck = checkSquare != null && turn == PieceColor.White,
                    isBlackInCheck = checkSquare != null && turn == PieceColor.Black,
                    isWhiteTurn = turn == PieceColor.White,
                    error = null
                )
            }
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    error = "The server returned an invalid board position."
                )
            }
        }
    }

    private fun renderLocalPosition(
        game: GameEntity,
        ply: Int
    ) {
        val replay = PassAndPlayGame()

        localMoves.take(ply).forEach { uci ->
            runCatching {
                replay.play(uci.toMove())
            }
        }

        val lastMove = replay.lastMove
        val checkSquare = replay.checkSquare

        val time = when {
            ply == 0 -> {
                val initial =
                    (game.timeControlMinutes ?: 0) * 60_000L
                initial to initial
            }

            ply == localMoves.size ->
                game.whiteTimeMs to game.blackTimeMs

            ply <= moveTimes.size ->
                moveTimes[ply - 1]

            else ->
                game.whiteTimeMs to game.blackTimeMs
        }

        _state.update {
            it.copy(
                currentPly = ply,
                totalPlies = localMoves.size,
                board = replay.pieces.toUiBoard(),
                whiteTimeMillis = time.first,
                blackTimeMillis = time.second,
                lastMoveFrom = lastMove?.from?.toUi(),
                lastMoveTo = lastMove?.to?.toUi(),
                isWhiteInCheck = checkSquare != null && replay.turn == PieceColor.White,
                isBlackInCheck = checkSquare != null && replay.turn == PieceColor.Black,
                isWhiteTurn = replay.turn == PieceColor.White,
                error = null
            )
        }
    }

    private fun createBackendMoveList(
        remote: List<GameMoveDto>
    ): List<MoveUi> {
        return remote
            .withIndex()
            .groupBy { it.index / 2 }
            .map { (rowIndex, pair) ->
                MoveUi(
                    moveNumber = rowIndex + 1,
                    whiteMove = pair.getOrNull(0)?.value?.move,
                    blackMove = pair.getOrNull(1)?.value?.move
                )
            }
    }

    private fun createLocalMoveList(
        rawMoves: List<String>
    ): List<MoveUi> {
        return rawMoves.chunked(2).mapIndexed { index, pair ->
            MoveUi(
                moveNumber = index + 1,
                whiteMove = pair.getOrNull(0)?.let(::formatUciMove),
                blackMove = pair.getOrNull(1)?.let(::formatUciMove)
            )
        }
    }

    private fun formatUciMove(uci: String): String {
        if (uci.length < 4) return uci
        return buildString {
            append(uci.substring(0, 2))
            append("-")
            append(uci.substring(2, 4))
            if (uci.length > 4) {
                append("=")
                append(
                    when (uci[4].lowercaseChar()) {
                        'q' -> "Q"
                        'r' -> "R"
                        'b' -> "B"
                        'n' -> "N"
                        else -> uci[4]
                    }
                )
            }
        }
    }
}

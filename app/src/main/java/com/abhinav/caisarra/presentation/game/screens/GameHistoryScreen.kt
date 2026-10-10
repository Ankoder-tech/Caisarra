package com.abhinav.caisarra.presentation.game.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhinav.caisarra.presentation.game.components.ChessBoard
import com.abhinav.caisarra.presentation.game.components.ClockBar
import com.abhinav.caisarra.presentation.game.model.MoveUi
import com.abhinav.caisarra.presentation.game.viewmodel.GameHistoryReviewViewModel
import com.abhinav.caisarra.presentation.theme.JetBrainsMono

private val ScreenBg = Color(0xFF080D13)
private val CardBg = Color(0xFF151F2B)
private val BorderColor = Color(0xFF304154)
private val White = Color(0xFFF5F5F5)
private val Secondary = Color(0xFF8C98A5)
private val Accent = Color(0xFF10B981)

@Composable
fun GameHistoryScreen(
    viewModel: GameHistoryReviewViewModel,
    onBack: () -> Unit
) {

    val state by viewModel.state.collectAsState()
    var isFlipped by
    remember {
        mutableStateOf(false)
    }

    if (state.isLoading) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ScreenBg)
                .safeDrawingPadding(),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = Accent
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text =
                    "Loading game history...",
                color = White
            )
        }
        return
    }
    if (state.game == null) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ScreenBg)
                .safeDrawingPadding()
                .padding(24.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center
        ) {

            Text(text = state.error ?: "Game not found",
                color = White,
                fontFamily = JetBrainsMono
            )

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onBack
            ) {
                Text(
                    "BACK",
                    color = White
                )
            }
        }
        return
    }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
            .safeDrawingPadding()
    ) {
        val boardSize =
            minOf(
                maxWidth - 24.dp,
                430.dp
            )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 12.dp,
                    vertical = 12.dp
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            HistoryTopBar(
                onBack = onBack,
                onFlip = {
                    isFlipped = !isFlipped
                }
            )
            Spacer(modifier = Modifier.height(10.dp))

            Text(text =
                    "${state.whitePlayerName}  vs  " +
                            state.blackPlayerName,
                color = White,
                fontFamily =
                    JetBrainsMono,
                fontSize = 14.sp,
                fontWeight =
                    FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${state.currentPly} / " + "${state.totalPlies} moves",
                color = Secondary,
                fontFamily = JetBrainsMono,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            ClockBar(
                playerName = state.blackPlayerName,
                timeMillis = state.blackTimeMillis,
                isActive = state.currentPly <state.totalPlies &&!state.isWhiteTurn,
                modifier =Modifier.width(boardSize)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier.size(boardSize)
            ) {
                ChessBoard(
                    board = state.board,
                    selectedSquare = null,
                    legalMoves = emptyList(),
                    lastMoveFrom = state.lastMoveFrom,
                    lastMoveTo = state.lastMoveTo,
                    isWhiteInCheck = state.isWhiteInCheck,
                    isBlackInCheck = state.isBlackInCheck,
                    isFlipped = isFlipped,
                    onSquareClick = {}
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            ClockBar(playerName = state.whitePlayerName,
                timeMillis = state.whiteTimeMillis,
                isActive = state.currentPly < state.totalPlies && state.isWhiteTurn,

                modifier = Modifier.width(boardSize)
            )

            Spacer(modifier = Modifier.height(12.dp))

            HistoryNavigationButtons(
                canGoBack =state.currentPly > 0,
                canGoForward = state.currentPly < state.totalPlies,
                onBack = viewModel::previousMove,
                onForward = viewModel::nextMove,
                modifier = Modifier.width(boardSize)
            )

            Spacer(modifier = Modifier.height(12.dp))
            HistoryMoves(
                moves = state.moves,
                currentPly = state.currentPly,
                modifier = Modifier.width(boardSize),
                onMoveClick = { moveNumber, isBlackMove ->
                    val ply =
                        if (isBlackMove) {
                            moveNumber * 2
                        } else {
                            moveNumber * 2 - 1
                        }
                    viewModel.jumpToMove(ply)
                }
            )
            if (state.isRegisteredGame) {
                Spacer(modifier = Modifier.height(16.dp))

                ChatHistorySection(
                    messages = state.chatMessages,
                    error = state.chatError,
                    modifier = Modifier.width(boardSize)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HistoryTopBar(
    onBack: () -> Unit,
    onFlip: () -> Unit
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(
            onClick = onBack
        ) {
            Text(
                "Home",
                color = White
            )
        }

        Text(
            text = "MOSAIC HISTORY",
            color = White,
            fontFamily = JetBrainsMono,
            fontSize = 16.sp,
            fontWeight =
                FontWeight.Bold,
            letterSpacing = 1.sp
        )

        TextButton(
            onClick = onFlip
        ) {
            Text(
                "Flip",
                color = White
            )
        }
    }
}

@Composable
private fun HistoryNavigationButtons(
    canGoBack: Boolean,
    canGoForward: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    modifier: Modifier = Modifier
) {

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        OutlinedButton(
            onClick = onBack,
            enabled = canGoBack,
            modifier =
                Modifier.weight(1f),
            shape =
                RoundedCornerShape(12.dp)
        ) {

            Text(
                text = "‹  BACK",
                color =
                    if (canGoBack)
                        White
                    else
                        Secondary,
                fontFamily =
                    JetBrainsMono,
                fontSize = 12.sp
            )
        }

        OutlinedButton(
            onClick = onForward,
            enabled = canGoForward,
            modifier =
                Modifier.weight(1f),
            shape =
                RoundedCornerShape(12.dp)
        ) {

            Text(
                text = "FORWARD  ›",
                color = if (canGoForward)
                        White
                    else
                        Secondary,
                fontFamily =
                    JetBrainsMono,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun HistoryMoves(
    moves: List<MoveUi>,
    currentPly: Int,
    modifier: Modifier = Modifier,
    onMoveClick:
        (Int, Boolean) -> Unit
) {

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                CardBg,
                RoundedCornerShape(14.dp)
            )
            .border(
                1.dp,
                BorderColor,
                RoundedCornerShape(14.dp)
            )
            .padding(12.dp)
    ) {

        Text(
            text = "MOVES",
            color = White,
            fontFamily = JetBrainsMono,
            fontSize = 13.sp,
            fontWeight =
                FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (moves.isEmpty()) {

            Text(
                text = "No moves recorded.",
                color = Secondary,
                fontFamily = JetBrainsMono,
                fontSize = 11.sp
            )

        } else {

            moves.forEach { move ->

                val whitePly =
                    move.moveNumber * 2 - 1

                val blackPly =
                    move.moveNumber * 2

                Row(
                    modifier = Modifier.fillMaxWidth()
                            .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {

                    Text(
                        text = "${move.moveNumber}.",
                        color = Secondary,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        modifier = Modifier.width(28.dp)
                    )

                    MoveCell(
                        text = move.whiteMove ?: "-",

                        selected = currentPly == whitePly,

                        onClick =
                            if (
                                move.whiteMove != null
                            ) {
                                { onMoveClick(
                                        move.moveNumber,
                                        false
                                    )
                                }
                            } else {
                                null
                            },

                        modifier = Modifier.weight(1f)
                    )

                    MoveCell(
                        text = move.blackMove ?: "-",
                        selected = currentPly == blackPly,
                        onClick =
                            if (
                                move.blackMove != null
                            ) {
                                {
                                    onMoveClick(
                                        move.moveNumber,
                                        true
                                    )
                                }
                            } else {
                                null
                            },

                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MoveCell(
    text: String,
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {

    Box(
        modifier = modifier
            .background(
                if (selected)
                    Color(0xFF263B30)
                else
                    Color.Transparent,
                RoundedCornerShape(6.dp)
            )
            .clickable(
                enabled = onClick != null
            ) {
                onClick?.invoke()
            }
            .padding(horizontal = 6.dp,
                vertical = 5.dp
            )
    ) {

        Text(
            text = text,
            color = if (selected)
                    Accent
                else
                    White,
            fontFamily = JetBrainsMono,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun ChatHistorySection(
    messages: List<com.abhinav.caisarra.data.remote.dto.ChatMessageDto>,
    error: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                CardBg,
                RoundedCornerShape(14.dp)
            )
            .border(
                1.dp,
                BorderColor,
                RoundedCornerShape(14.dp)
            )
            .padding(12.dp)
    ) {
        Text(
            text = "GAME CHAT",
            color = White,
            fontFamily = JetBrainsMono,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        when {
            error != null && messages.isEmpty() -> {
                Text(
                    text = error,
                    color = Secondary,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp
                )
            }

            messages.isEmpty() -> {
                Text(
                    text = "No chat messages recorded.",
                    color = Secondary,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp
                )
            }

            else -> {
                messages.forEach { message ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Text(
                            text = message.username
                                ?.takeIf { it.isNotBlank() }
                                ?: "Player ${message.senderId}",
                            color = Accent,
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(3.dp)
                        )

                        Text(
                            text = message.message,
                            color = White,
                            fontSize = 13.sp
                        )

                        Text(
                            text = message.createdAt,
                            color = Secondary,
                            fontFamily = JetBrainsMono,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}

package com.hexmerge.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hexmerge.game.GameState
import com.hexmerge.model.Direction
import com.hexmerge.model.HexBoard
import com.hexmerge.model.HexCoord
import com.hexmerge.viewmodel.GameViewModel
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private val BG_COLOR = Color(0xFF1A1A2E)
private val GRID_COLOR = Color(0xFF16213E)
private val GRID_STROKE = Color(0xFF0F3460)
private val TEXT_COLOR = Color(0xFFE8E8E8)
private val SCORE_BG = Color(0xFF16213E)

private fun tileColor(value: Int): Color = when (value) {
    2 -> Color(0xFF2ECC71)
    4 -> Color(0xFF27AE60)
    8 -> Color(0xFF1ABC9C)
    16 -> Color(0xFF16A085)
    32 -> Color(0xFF3498DB)
    64 -> Color(0xFF2980B9)
    128 -> Color(0xFF9B59B6)
    256 -> Color(0xFF8E44AD)
    512 -> Color(0xFFE74C3C)
    1024 -> Color(0xFFC0392B)
    2048 -> Color(0xFFF39C12)
    else -> Color(0xFFE67E22)
}

@Composable
fun GameScreen(viewModel: GameViewModel) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BG_COLOR)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "HexMerge",
            color = TEXT_COLOR,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(16.dp))
        ScoreBar(score = state.score, bestScore = state.bestScore)
        Spacer(modifier = Modifier.height(16.dp))
        HexGridWithSwipe(
            state = state,
            onSwipe = { direction -> viewModel.swipe(direction) },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { viewModel.restart() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F3460)),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("New Game", color = TEXT_COLOR)
        }

        if (state.isGameOver) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Game Over!",
                color = Color(0xFFE74C3C),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Score: ${state.score}",
                color = TEXT_COLOR,
                fontSize = 18.sp,
            )
        }
    }
}

@Composable
private fun ScoreBar(score: Int, bestScore: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ScoreBox(label = "SCORE", value = score)
        ScoreBox(label = "BEST", value = bestScore)
    }
}

@Composable
private fun ScoreBox(label: String, value: Int) {
    Column(
        modifier = Modifier
            .background(SCORE_BG, RoundedCornerShape(8.dp))
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = label, color = Color(0xFF888888), fontSize = 12.sp)
        Text(
            text = value.toString(),
            color = TEXT_COLOR,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun HexGridWithSwipe(
    state: GameState,
    onSwipe: (Direction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val minDragPx = with(density) { 20.dp.toPx() }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        val dx = dragAmount.x
                        val dy = dragAmount.y
                        val dist = sqrt(dx * dx + dy * dy)
                        if (dist < minDragPx) return@detectDragGestures

                        val angle = atan2(dy.toDouble(), dx.toDouble())
                        val direction = angleToDirection(angle)
                        if (direction != null) {
                            onSwipe(direction)
                        }
                    }
                },
        ) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val hexSize = min(size.width, size.height) / (state.radius * 2 + 1) / 1.8f

            val allCells = HexBoard.allCells(state.radius)
            for (coord in allCells) {
                val pixel = hexToPixel(coord, hexSize, centerX, centerY)
                drawHexagon(pixel, hexSize, GRID_COLOR, GRID_STROKE)
            }

            for ((coord, value) in state.board) {
                val pixel = hexToPixel(coord, hexSize, centerX, centerY)
                drawHexagon(pixel, hexSize * 0.92f, tileColor(value), Color.Transparent)
                drawTileText(pixel, value)
            }
        }
    }
}

private fun angleToDirection(angle: Double): Direction? {
    val degrees = Math.toDegrees(angle)
    val normalized = if (degrees < 0) degrees + 360 else degrees

    return when {
        normalized < 30 || normalized >= 330 -> Direction.EAST
        normalized < 90 -> Direction.SOUTHEAST
        normalized < 150 -> Direction.SOUTHWEST
        normalized < 210 -> Direction.WEST
        normalized < 270 -> Direction.NORTHWEST
        else -> Direction.NORTHEAST
    }
}

private fun hexToPixel(coord: HexCoord, size: Float, cx: Float, cy: Float): Offset {
    val x = size * (sqrt(3f) * coord.q + sqrt(3f) / 2 * coord.r)
    val y = size * (3f / 2 * coord.r)
    return Offset(cx + x, cy + y)
}

private fun DrawScope.drawHexagon(
    center: Offset,
    size: Float,
    fillColor: Color,
    strokeColor: Color,
) {
    val path = Path()
    for (i in 0..5) {
        val angleDeg = 60.0 * i - 30.0
        val angleRad = PI / 180.0 * angleDeg
        val x = center.x + size * cos(angleRad).toFloat()
        val y = center.y + size * sin(angleRad).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, fillColor, style = Fill)
    if (strokeColor != Color.Transparent) {
        drawPath(path, strokeColor, style = Stroke(width = 2f))
    }
}

private val tilePaint = android.graphics.Paint().apply {
    color = android.graphics.Color.WHITE
    textAlign = android.graphics.Paint.Align.CENTER
    isFakeBoldText = true
    isAntiAlias = true
}

private fun DrawScope.drawTileText(center: Offset, value: Int) {
    tilePaint.textSize = when {
        value < 10 -> 40f
        value < 100 -> 36f
        value < 1000 -> 30f
        else -> 24f
    }
    drawContext.canvas.nativeCanvas.drawText(
        value.toString(),
        center.x,
        center.y + tilePaint.textSize / 3,
        tilePaint,
    )
}

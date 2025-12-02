package com.ray.trarailwaysalaryapp.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.ray.trarailwaysalaryapp.R
import com.ray.trarailwaysalaryapp.data.StopTime

@Composable
fun TrainRouteLine(
    stops: List<StopTime>,
    currentStationName: String?,
    modifier: Modifier = Modifier
) {
    if (stops.isEmpty()) return

    val context = LocalContext.current
    val trainIconBitmap = ContextCompat.getDrawable(context, R.drawable.ic_train)?.toBitmap()?.asImageBitmap()

    val lineColor = MaterialTheme.colorScheme.primary
    val stationColor = MaterialTheme.colorScheme.secondary
    val textColor = Color.White // Use bright white for text color
    val timeColor = Color.LightGray // Dimmer color for time

    // Increase vertical spacing and calculate the required height
    val stopSpacing = 80.dp
    val topPadding = 30.dp
    val bottomPadding = 30.dp
    val totalHeight =
        stopSpacing * (stops.size - 1).coerceAtLeast(0).toFloat() +
                topPadding +
                bottomPadding
    Canvas(modifier = modifier.fillMaxWidth().height(totalHeight)) {
        val lineX = 60.dp.toPx() // X position for the vertical line

        // Draw the vertical line
        drawLine(
            color = lineColor,
            start = Offset(lineX, topPadding.toPx()),
            end = Offset(lineX, size.height - bottomPadding.toPx()),
            strokeWidth = 4.dp.toPx()
        )

        // Prepare paint for station names
        val textPaint = Paint().apply {
            color = textColor.toArgb()
            textAlign = Paint.Align.LEFT
            textSize = 16.sp.toPx()
        }

        // Prepare paint for time
        val timeTextPaint = Paint().apply {
            color = timeColor.toArgb()
            textAlign = Paint.Align.LEFT
            textSize = 12.sp.toPx()
        }

        // Helper to vertically center the text with the station dot
        val textVerticalOffset = (textPaint.descent() + textPaint.ascent()) / 2

        // Draw station points and names
        stops.forEachIndexed { index, stop ->
            val stationY = topPadding.toPx() + index * stopSpacing.toPx()

            // Draw station circle
            drawCircle(
                color = stationColor,
                radius = 8.dp.toPx(),
                center = Offset(lineX, stationY)
            )

            // Draw station name
            drawContext.canvas.nativeCanvas.drawText(
                stop.StationName.Zh_tw,
                lineX + 25.dp.toPx(), // Position text to the right of the line
                stationY - textVerticalOffset, // Vertically center the text
                textPaint
            )

            // Draw arrival and departure times below the station name
            val timeText = "到站: ${stop.ArrivalTime ?: "--:--"} | 開車: ${stop.DepartureTime ?: "--:--"}"
            drawContext.canvas.nativeCanvas.drawText(
                timeText,
                lineX + 25.dp.toPx(),
                stationY - textVerticalOffset + 20.dp.toPx(), // Position below the station name
                timeTextPaint
            )
        }

        // Find the current station's index and draw the train icon
        val currentStopIndex = stops.indexOfFirst { it.StationName.Zh_tw == currentStationName }
        if (trainIconBitmap != null && currentStopIndex != -1) {
            val trainY = topPadding.toPx() + currentStopIndex * stopSpacing.toPx()
            val iconSize = 36.dp.toPx()

            // Draw the train icon, centered over the current station's Y coordinate
            drawImage(
                image = trainIconBitmap,
                topLeft = Offset(lineX - iconSize / 2, trainY - iconSize / 2),
                alpha = 1f
            )
        }
    }
}

package com.iith.attendanceapp

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb

/**
 * Draws a semi-transparent dark mask with an oval "window" cut out in the center.
 * Standard liveness UI pattern used by banking and KYC apps.
 */
@Composable
fun FaceOvalOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                color = Color(0xA0000000)  // semi-transparent black
            }
            // Draw full dark mask
            canvas.drawRect(
                left = 0f, top = 0f, right = size.width, bottom = size.height, paint = paint
            )
            // Cut out oval using BlendMode.Clear
            val ovalWidth  = size.width  * 0.72f
            val ovalHeight = size.height * 0.42f
            val cx = size.width  / 2f
            val cy = size.height * 0.42f  // slightly above center

            val clearPaint = Paint().apply {
                blendMode = BlendMode.Clear
            }
            canvas.drawOval(
                Rect(
                    offset = Offset(cx - ovalWidth / 2f, cy - ovalHeight / 2f),
                    size   = Size(ovalWidth, ovalHeight)
                ),
                clearPaint
            )
        }

        // Draw white oval border on top
        val ovalWidth  = size.width  * 0.72f
        val ovalHeight = size.height * 0.42f
        val cx = size.width  / 2f
        val cy = size.height * 0.42f

        drawOval(
            color   = Color.White,
            topLeft = Offset(cx - ovalWidth / 2f, cy - ovalHeight / 2f),
            size    = Size(ovalWidth, ovalHeight),
            style   = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
        )
    }
}

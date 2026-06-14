package com.vitasleep.android.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vitasleep.android.ui.theme.Dimens
import com.vitasleep.android.ui.theme.GlassBorder
import com.vitasleep.android.ui.theme.IceBlue

@Composable
fun BatteryRing(
    percentage: Int,
    modifier: Modifier = Modifier,
    size: Dp = Dimens.ringSize,
    strokeWidth: Dp = 3.dp,
    label: String? = null
) {
    val animatedProgress by animateFloatAsState(
        targetValue = percentage / 100f,
        animationSpec = tween(durationMillis = 800),
        label = "ring"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(size)) {
                val stroke = Stroke(
                    width = strokeWidth.toPx(),
                    cap = StrokeCap.Round
                )
                val diameter = size.toPx() - strokeWidth.toPx()
                val topLeft = Offset(
                    x = (size.toPx() - diameter) / 2f,
                    y = (size.toPx() - diameter) / 2f
                )
                val arcSize = Size(diameter, diameter)

                drawArc(
                    color = GlassBorder,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke
                )
                drawArc(
                    color = IceBlue,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke
                )
            }
            Text(
                text = "$percentage",
                style = if (size >= Dimens.ringSizeLg)
                    MaterialTheme.typography.titleLarge
                else
                    MaterialTheme.typography.titleMedium
            )
        }
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

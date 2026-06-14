package com.vitasleep.android.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.vitasleep.android.ui.theme.IceBlue
import kotlinx.coroutines.delay

@Composable
fun TypewriterText(
    fullText: String,
    modifier: Modifier = Modifier,
    charDelayMs: Long = 50L,
    onTypingComplete: (() -> Unit)? = null
) {
    var displayedText by remember { mutableStateOf("") }
    var isTyping by remember(fullText) { mutableStateOf(true) }

    LaunchedEffect(fullText) {
        displayedText = ""
        isTyping = true
        fullText.forEachIndexed { index, _ ->
            displayedText = fullText.substring(0, index + 1)
            delay(charDelayMs)
        }
        isTyping = false
        onTypingComplete?.invoke()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    Row(modifier = modifier) {
        Text(
            text = displayedText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (isTyping) {
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "▌",
                style = MaterialTheme.typography.bodyMedium,
                color = IceBlue,
                modifier = Modifier.alpha(cursorAlpha)
            )
        }
    }
}

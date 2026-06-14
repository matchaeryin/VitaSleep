package com.vitasleep.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.vitasleep.android.ui.theme.Amber
import com.vitasleep.android.ui.theme.Dimens
import com.vitasleep.android.ui.theme.IceBlue
import com.vitasleep.android.ui.theme.MintGreen
import com.vitasleep.android.ui.theme.RoseRed
import com.vitasleep.android.ui.theme.ShapeChip
import com.vitasleep.android.ui.theme.SkyBlue
import com.vitasleep.android.ui.theme.TextPrimary
import com.vitasleep.android.ui.theme.TextTertiary

data class EventTypeConfig(
    val color: Color,
    val label: String
)

fun getEventTypeConfig(type: String): EventTypeConfig = when (type.lowercase()) {
    "fixed", "meeting" -> EventTypeConfig(SkyBlue, "固定")
    "work" -> EventTypeConfig(MintGreen, "工作")
    "rest", "sleep" -> EventTypeConfig(Amber, "休息")
    "health" -> EventTypeConfig(RoseRed, "健康")
    "health_intervention" -> EventTypeConfig(IceBlue, "干预")
    "flexible" -> EventTypeConfig(MintGreen, "灵活")
    else -> EventTypeConfig(Amber, "日程")
}

@Composable
fun EventCard(
    title: String,
    timeRange: String,
    description: String,
    type: String,
    hasConflict: Boolean = false,
    conflictSuggestion: String? = null,
    isAccepted: Boolean = false,
    originalTitle: String? = null,
    acceptedAt: String? = null,
    onAccept: (() -> Unit)? = null,
    onIgnore: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val config = getEventTypeConfig(type)
    val accent = if (hasConflict) RoseRed else config.color

    Box(modifier = modifier.fillMaxWidth()) {
        if (hasConflict) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .matchParentSize()
                    .background(RoseRed)
            )
        }
        GlassCard(
            modifier = Modifier.padding(start = if (hasConflict) 3.dp else 0.dp),
            accent = accent
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(accent.copy(alpha = 0.12f), ShapeChip)
                            .padding(horizontal = Dimens.spaceSm, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (hasConflict) "⚠ ${config.label}" else config.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = accent
                        )
                    }
                    Text(text = timeRange, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                }
                if (hasConflict) {
                    Text(text = "冲突", style = MaterialTheme.typography.labelSmall, color = RoseRed)
                } else if (isAccepted) {
                    Text(text = "✓ 已采纳", style = MaterialTheme.typography.labelSmall, color = MintGreen)
                }
            }

            if (isAccepted && originalTitle != null) {
                Text(
                    text = originalTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextTertiary,
                    textDecoration = TextDecoration.LineThrough
                )
                Text(
                    text = "→ $title",
                    style = MaterialTheme.typography.titleMedium,
                    color = MintGreen
                )
                if (acceptedAt != null) {
                    Text(text = "采纳于 $acceptedAt", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                }
            } else {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                if (description.isNotEmpty()) {
                    Text(text = description, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                }
            }

            if (hasConflict && conflictSuggestion != null) {
                ConflictBanner(
                    suggestion = conflictSuggestion,
                    onAccept = onAccept,
                    onIgnore = onIgnore
                )
            }
        }
    }
}

@Composable
private fun ConflictBanner(
    suggestion: String,
    onAccept: (() -> Unit)?,
    onIgnore: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Dimens.spaceMd)
            .background(IceBlue.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
            .border(1.dp, IceBlue.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .padding(Dimens.spaceMd)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm)) {
            Text("AI 调整建议", style = MaterialTheme.typography.labelSmall, color = IceBlue)
        }
        Text(
            text = suggestion,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            modifier = Modifier.padding(vertical = Dimens.spaceSm)
        )
        if (onAccept != null && onIgnore != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm)) {
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(containerColor = IceBlue),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("采纳建议", color = Color(0xFF0A0D13))
                }
                OutlinedButton(
                    onClick = onIgnore,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("忽略")
                }
            }
        }
    }
}

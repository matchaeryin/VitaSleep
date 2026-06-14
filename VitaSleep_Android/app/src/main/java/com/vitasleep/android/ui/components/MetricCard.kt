package com.vitasleep.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vitasleep.android.ui.theme.Dimens
import com.vitasleep.android.ui.theme.ShapeNode
import com.vitasleep.android.ui.theme.TextDisabled
import com.vitasleep.android.ui.theme.TextPrimary
import com.vitasleep.android.ui.theme.TextTertiary

fun formatMetricValue(value: Int?): String {
    return value?.toString() ?: "—"
}

fun formatBloodPressure(systolic: Int?, diastolic: Int?): String {
    return if (systolic != null && diastolic != null) "$systolic/$diastolic" else "—"
}

@Composable
fun MetricCard(
    name: String,
    value: String,
    unit: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val hasData = value != "—"
    val valueColor = if (hasData) TextPrimary else TextDisabled

    GlassCard(modifier = modifier, accent = if (hasData) accentColor else null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.iconNodeSize)
                    .background(accentColor.copy(alpha = 0.15f), ShapeNode),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = name,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Column(modifier = Modifier.padding(top = Dimens.spaceMd)) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (hasData) "$name · $unit" else "$name · 暂无数据",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary
            )
        }
    }
}

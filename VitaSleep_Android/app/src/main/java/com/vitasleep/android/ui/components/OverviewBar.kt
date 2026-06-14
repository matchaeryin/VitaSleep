package com.vitasleep.android.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.vitasleep.android.ui.theme.Dimens
import com.vitasleep.android.ui.theme.TextTertiary

@Composable
fun OverviewBar(
    batteryPercentage: Int,
    statusText: String,
    metricsSummary: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = Dimens.spaceLg, vertical = Dimens.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd)
    ) {
        BatteryRing(
            percentage = batteryPercentage,
            size = Dimens.ringSize
        )
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = metricsSummary,
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "查看详情",
            tint = TextTertiary
        )
    }
}

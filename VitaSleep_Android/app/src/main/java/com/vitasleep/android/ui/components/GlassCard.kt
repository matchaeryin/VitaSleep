package com.vitasleep.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vitasleep.android.ui.theme.Dimens
import com.vitasleep.android.ui.theme.GlassBg
import com.vitasleep.android.ui.theme.GlassBorder
import com.vitasleep.android.ui.theme.ShapeCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val backgroundColor = if (accent != null) accent.copy(alpha = 0.05f) else GlassBg
    val borderColor = if (accent != null) accent.copy(alpha = 0.15f) else GlassBorder

    val colors = CardDefaults.cardColors(containerColor = backgroundColor)
    val border = BorderStroke(1.dp, borderColor)

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = ShapeCard,
            colors = colors,
            border = border,
            content = { Column(modifier = Modifier.padding(Dimens.spaceLg), content = content) }
        )
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = ShapeCard,
            colors = colors,
            border = border,
            content = { Column(modifier = Modifier.padding(Dimens.spaceLg), content = content) }
        )
    }
}

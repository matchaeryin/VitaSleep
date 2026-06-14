package com.vitasleep.android.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val ShapeCard = RoundedCornerShape(16.dp)
val ShapeCardLarge = RoundedCornerShape(18.dp)
val ShapePill = RoundedCornerShape(999.dp)
val ShapeChip = RoundedCornerShape(6.dp)
val ShapeNode = RoundedCornerShape(10.dp)

val AppShapes = Shapes(
    small = ShapeChip,
    medium = ShapeCard,
    large = ShapeCardLarge
)

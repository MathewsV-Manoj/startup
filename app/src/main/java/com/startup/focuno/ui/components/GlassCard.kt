package com.startup.focuno.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Glassmorphism surface: 24dp radius, translucent gradient fill and a 1px gradient border. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    Column(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0x26FFFFFF), Color(0x0DFFFFFF))))
            .border(1.dp, Brush.linearGradient(listOf(Color(0x66FFFFFF), Color(0x14FFFFFF))), shape)
            .padding(contentPadding),
        content = content,
    )
}

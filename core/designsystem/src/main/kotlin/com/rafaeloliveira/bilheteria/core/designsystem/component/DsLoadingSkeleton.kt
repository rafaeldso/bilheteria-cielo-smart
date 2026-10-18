/*
 * Copyright 2026 Rafael Oliveira
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.rafaeloliveira.bilheteria.core.designsystem.component

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rafaeloliveira.bilheteria.core.designsystem.theme.BilheteriaTheme

private const val DEFAULT_ROWS = 6
private const val PULSE_HALF_CYCLE_MILLIS = 700
private const val PULSE_MIN_ALPHA = 0.45f
private const val STATIC_ALPHA = 1f
private const val TEXT_LINE_FRACTION = 0.6f
private const val SHORT_TEXT_LINE_FRACTION = 0.4f
private const val CARD_TITLE_FRACTION = 0.5f
private const val CARD_BODY_FRACTION = 0.9f
private const val CARD_LAST_LINE_FRACTION = 0.7f
private val TrailingWidth = 64.dp
private val TrailingHeight = 20.dp
private val LineHeight = 16.dp
private val ShortLineHeight = 12.dp

/**
 * Placeholder shown while content loads, with the same geometry as the content to avoid layout shift.
 * Uses `surfaceContainerHighest` shapes that pulse (1.0 to 0.45 alpha, 1.4 s per cycle). The pulse
 * stays still when the user removed animations in the system settings.
 *
 * TalkBack announces [contentDescription] politely when the skeleton appears; the shapes themselves
 * are invisible to accessibility services.
 *
 * @param contentDescription what is loading, already localized (e.g. "Carregando eventos").
 * @param modifier applied to the root.
 * @param variant [DsLoadingSkeletonVariant.List] (default) or [DsLoadingSkeletonVariant.Card].
 * @param rows number of rows drawn by the [DsLoadingSkeletonVariant.List] variant; ignored by the card.
 */
@Composable
fun DsLoadingSkeleton(
    contentDescription: String,
    modifier: Modifier = Modifier,
    variant: DsLoadingSkeletonVariant = DsLoadingSkeletonVariant.List,
    rows: Int = DEFAULT_ROWS,
) {
    val pulseAlpha = rememberPulseAlpha()
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    this.contentDescription = contentDescription
                    liveRegion = LiveRegionMode.Polite
                }.graphicsLayer { alpha = pulseAlpha.value },
    ) {
        when (variant) {
            DsLoadingSkeletonVariant.List -> repeat(rows.coerceAtLeast(1)) { SkeletonRow() }
            DsLoadingSkeletonVariant.Card -> SkeletonCard()
        }
    }
}

@Composable
private fun rememberPulseAlpha(): State<Float> {
    if (rememberReducedMotion()) return remember { mutableFloatStateOf(STATIC_ALPHA) }
    return rememberInfiniteTransition(label = "skeletonPulse").animateFloat(
        initialValue = STATIC_ALPHA,
        targetValue = PULSE_MIN_ALPHA,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = PULSE_HALF_CYCLE_MILLIS, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "skeletonPulseAlpha",
    )
}

@Composable
private fun SkeletonRow() {
    val spacing = BilheteriaTheme.spacing
    Row(
        modifier = Modifier.fillMaxWidth().padding(spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkeletonShape(shape = CircleShape, modifier = Modifier.size(BilheteriaTheme.sizes.iconLarge))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            SkeletonShape(modifier = Modifier.fillMaxWidth(TEXT_LINE_FRACTION).height(LineHeight))
            SkeletonShape(modifier = Modifier.fillMaxWidth(SHORT_TEXT_LINE_FRACTION).height(ShortLineHeight))
        }
        SkeletonShape(modifier = Modifier.size(width = TrailingWidth, height = TrailingHeight))
    }
}

@Composable
private fun SkeletonCard() {
    val spacing = BilheteriaTheme.spacing
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(spacing.lg)
                .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.medium)
                .padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        SkeletonShape(modifier = Modifier.fillMaxWidth(CARD_TITLE_FRACTION).height(LineHeight))
        SkeletonShape(modifier = Modifier.fillMaxWidth(CARD_BODY_FRACTION).height(ShortLineHeight))
        SkeletonShape(modifier = Modifier.fillMaxWidth(CARD_BODY_FRACTION).height(ShortLineHeight))
        SkeletonShape(modifier = Modifier.fillMaxWidth(CARD_LAST_LINE_FRACTION).height(ShortLineHeight))
    }
}

@Composable
private fun SkeletonShape(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small,
) {
    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest, shape))
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsLoadingSkeletonListPreview() {
    DsPreviewSurface(modifier = Modifier.width(PREVIEW_WIDTH), padded = false) {
        DsLoadingSkeleton(contentDescription = "Carregando eventos", rows = 4)
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DsLoadingSkeletonCardPreview() {
    DsPreviewSurface(modifier = Modifier.width(PREVIEW_WIDTH), padded = false) {
        DsLoadingSkeleton(contentDescription = "Carregando compra", variant = DsLoadingSkeletonVariant.Card)
    }
}

private val PREVIEW_WIDTH = 360.dp

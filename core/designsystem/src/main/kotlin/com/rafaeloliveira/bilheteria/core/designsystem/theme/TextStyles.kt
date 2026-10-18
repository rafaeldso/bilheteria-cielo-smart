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
package com.rafaeloliveira.bilheteria.core.designsystem.theme

import androidx.compose.ui.text.TextStyle

private const val TABULAR_FIGURES = "tnum"

/**
 * This style with tabular (fixed width) digits, so amounts and quantities line up and do not jump
 * when a digit changes. Use it on every text that shows money, counts or other figures.
 */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = TABULAR_FIGURES)

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

import androidx.compose.runtime.Immutable

/**
 * One row of a [DsKeyValueList].
 *
 * @property label name of the field, already localized (e.g. "Nº da compra").
 * @property value value of the field, already formatted (e.g. "7F3K-9Q2L").
 * @property spokenValue text TalkBack reads instead of [value] when the visible text is abbreviated or
 * formatted for the eye (e.g. "100 reais" for "R$ 100,00"); null reads [value].
 */
@Immutable
data class DsKeyValue(
    val label: String,
    val value: String,
    val spokenValue: String? = null,
)

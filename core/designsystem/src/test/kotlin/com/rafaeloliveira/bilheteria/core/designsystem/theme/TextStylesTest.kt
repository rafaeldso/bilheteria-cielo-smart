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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TextStylesTest {
    @Test
    fun `dado um estilo de tipografia quando aplicar tabular entao liga digitos tabulares e mantem o resto`() {
        val base = BilheteriaTypography.displaySmall

        val tabular = base.tabular()

        assertEquals("tnum", tabular.fontFeatureSettings)
        assertEquals(base.copy(fontFeatureSettings = "tnum"), tabular)
    }
}

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

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** The skeleton pulse and the progress bar stay still exactly when this says motion is reduced. */
class ReducedMotionTest {
    @Test
    fun `dado escala de animacao zero quando avaliar entao o movimento e reduzido`() {
        assertTrue(isMotionReduced(animatorDurationScale = 0f))
    }

    @Test
    fun `dado escala de animacao abaixo de zero quando avaliar entao o movimento e reduzido`() {
        assertTrue(isMotionReduced(animatorDurationScale = -1f))
    }

    @Test
    fun `dado escala de animacao logo acima de zero quando avaliar entao o movimento nao e reduzido`() {
        assertFalse(isMotionReduced(animatorDurationScale = 0.5f))
    }

    @Test
    fun `dado escala de animacao padrao quando avaliar entao o movimento nao e reduzido`() {
        assertFalse(isMotionReduced(animatorDurationScale = 1f))
    }
}

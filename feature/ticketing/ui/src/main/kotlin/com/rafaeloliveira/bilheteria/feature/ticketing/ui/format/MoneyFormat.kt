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
package com.rafaeloliveira.bilheteria.feature.ticketing.ui.format

import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Money
import java.math.BigDecimal
import java.text.NumberFormat

private const val CENTS_SCALE = 2
private const val NO_BREAK_SPACE = '\u00A0'
private const val NARROW_NO_BREAK_SPACE = '\u202F'
private const val SPACE = ' '

/**
 * "R$ 1.250,00": the currency format of Brazil, from the exact cents (never a floating point; BR-06).
 *
 * The separator between the symbol and the figures is always a no-break space, whichever space the platform's
 * locale data uses, so the symbol never wraps away from the figures and the text is the same on the terminal, on a
 * phone and in the JVM tests.
 */
internal fun Money.formatBrl(): String =
    NumberFormat
        .getCurrencyInstance(PtBr)
        .format(BigDecimal.valueOf(cents, CENTS_SCALE))
        .replace(SPACE, NO_BREAK_SPACE)
        .replace(NARROW_NO_BREAK_SPACE, NO_BREAK_SPACE)

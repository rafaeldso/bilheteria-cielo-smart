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
package com.rafaeloliveira.bilheteria.feature.ticketing.domain.usecase

import kotlin.math.min

/** Ticket quantity rules for one purchase (BR-05, BR-06, BR-07). The initial quantity is [MIN]. */
object QuantityLimits {
    const val MIN: Int = 1
    const val MAX_PER_PURCHASE: Int = 10

    fun max(available: Int): Int = min(MAX_PER_PURCHASE, available)

    fun canDecrement(quantity: Int): Boolean = quantity > MIN

    fun canIncrement(
        quantity: Int,
        available: Int,
    ): Boolean = quantity < max(available)
}

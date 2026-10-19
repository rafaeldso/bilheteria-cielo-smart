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

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Serializes the read-then-update sequences on the purchase table that react to the Cielo callback and to the
 * recovery (BR-15, BR-29). `PurchaseRepository.update` rewrites the whole row from a snapshot read earlier, so two
 * of those sequences running at once could overwrite each other (e.g. an `Approved` from the callback replaced by
 * `Recovering` from the recovery).
 *
 * Not reentrant: code that already runs inside [withLock] must call the unlocked variants.
 */
@Singleton
class PurchaseWriteLock
    @Inject
    constructor() {
        private val mutex = Mutex()

        suspend fun <T> withLock(block: suspend () -> T): T = mutex.withLock { block() }
    }

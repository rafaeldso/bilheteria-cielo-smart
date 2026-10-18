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

import com.rafaeloliveira.bilheteria.core.common.DataResult
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Purchase
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.repository.PurchaseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Observes the open purchase, `Pending` or `Recovering` (BR-10, BR-15). Emits `null` when there is none, which is
 * also what the observer sees once the open purchase reaches a final status. A repository failure is propagated as is.
 */
class ObserveOpenPurchaseUseCase
    @Inject
    constructor(
        private val repository: PurchaseRepository,
    ) {
        operator fun invoke(): Flow<DataResult<Purchase?>> = repository.observeOpenPurchase()
    }

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

/** Observes one purchase by id, for the receipt and the payment summary (BR-16, BR-19). */
class ObservePurchaseUseCase
    @Inject
    constructor(
        private val repository: PurchaseRepository,
    ) {
        operator fun invoke(id: String): Flow<DataResult<Purchase>> = repository.observePurchase(id)
    }

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
package com.rafaeloliveira.bilheteria.feature.ticketing.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/**
 * System clock for the ticketing use cases (`HandlePaymentOutcomeUseCase`, `RecoverPendingPurchaseUseCase`); tests
 * inject a fixed one. Lives here because the Cielo callback is the first entry point that needs the use cases.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object ClockModule {
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemUTC()
}

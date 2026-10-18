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

import com.rafaeloliveira.bilheteria.core.common.IoDispatcher
import com.rafaeloliveira.bilheteria.feature.ticketing.data.BuildConfig
import com.rafaeloliveira.bilheteria.feature.ticketing.data.cielo.AndroidTerminalAppLauncher
import com.rafaeloliveira.bilheteria.feature.ticketing.data.cielo.CieloCredentials
import com.rafaeloliveira.bilheteria.feature.ticketing.data.cielo.CieloDeepLinkGateway
import com.rafaeloliveira.bilheteria.feature.ticketing.data.cielo.ForegroundServicePaymentSession
import com.rafaeloliveira.bilheteria.feature.ticketing.data.cielo.PaymentSession
import com.rafaeloliveira.bilheteria.feature.ticketing.data.cielo.TerminalAppLauncher
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.payment.PaymentGateway
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/** Scope that outlives `CieloCallbackActivity`, so the stored outcome is not cancelled when the activity finishes. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class CallbackScope

@Module
@InstallIn(SingletonComponent::class)
internal interface CieloDataModule {
    @Binds
    @Singleton
    fun bindPaymentGateway(impl: CieloDeepLinkGateway): PaymentGateway

    @Binds
    fun bindTerminalAppLauncher(impl: AndroidTerminalAppLauncher): TerminalAppLauncher

    @Binds
    fun bindPaymentSession(impl: ForegroundServicePaymentSession): PaymentSession

    companion object {
        @Provides
        @Singleton
        fun provideCieloCredentials(): CieloCredentials =
            CieloCredentials(clientId = BuildConfig.CIELO_CLIENT_ID, accessToken = BuildConfig.CIELO_ACCESS_TOKEN)

        @Provides
        @Singleton
        @CallbackScope
        fun provideCallbackScope(
            @IoDispatcher ioDispatcher: CoroutineDispatcher,
        ): CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher)
    }
}

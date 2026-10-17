// Module: feature:balance:data (Android library)
// Packages: ...data.remote (DTO, API, data source), ...data.mapper, ...data.repository, ...data.di
package com.example.app.feature.balance.data

import com.example.app.core.common.AppError
import com.example.app.core.common.DataResult
import com.example.app.core.common.IoDispatcher
import com.example.app.core.common.suspendRunCatching
import com.example.app.feature.balance.domain.Balance
import com.example.app.feature.balance.domain.BalanceError
import com.example.app.feature.balance.domain.BalanceRepository
import com.example.app.feature.balance.domain.Money
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import retrofit2.HttpException
import retrofit2.http.GET
import java.io.IOException
import java.math.BigDecimal
import java.net.SocketTimeoutException
import java.time.Instant
import javax.inject.Inject

// --- remote -----------------------------------------------------------------

/** DTOs never leave the data layer (AP-06). Money arrives as string to avoid precision loss. */
@Serializable
internal data class BalanceDto(
    val available: String,
    val blocked: String,
    val updatedAt: String,
)

internal interface BalanceApi {
    @GET("v1/balance")
    suspend fun getBalance(): BalanceDto
}

internal class BalanceRemoteDataSource @Inject constructor(
    private val api: BalanceApi,
) {
    suspend fun fetch(): BalanceDto = api.getBalance()
}

// --- mapper -----------------------------------------------------------------

internal fun BalanceDto.toDomain(): Balance = Balance(
    available = Money(BigDecimal(available)),
    blocked = Money(BigDecimal(blocked)),
    updatedAt = Instant.parse(updatedAt),
)

// --- repository: maps technical exceptions using the plan's error matrix ----

internal class DefaultBalanceRepository @Inject constructor(
    private val remote: BalanceRemoteDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : BalanceRepository {

    override suspend fun getBalance(): DataResult<Balance> = withContext(ioDispatcher) {
        suspendRunCatching { remote.fetch().toDomain() }.fold(
            onSuccess = { DataResult.Success(it) },
            onFailure = { DataResult.Failure(it.toBalanceError()) },
        )
    }

    private fun Throwable.toBalanceError(): AppError = when (this) {
        is SocketTimeoutException -> AppError.Network.Timeout
        is IOException -> AppError.Network.Offline
        is HttpException -> when (code()) {
            HTTP_UNAUTHORIZED -> AppError.Auth.SessionExpired
            HTTP_LOCKED -> BalanceError.AccountBlocked
            else -> AppError.Unknown
        }
        else -> AppError.Unknown
    }

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_LOCKED = 423
    }
}

// --- di ---------------------------------------------------------------------

@Module
@InstallIn(SingletonComponent::class)
internal interface BalanceDataModule {
    @Binds
    fun bindBalanceRepository(impl: DefaultBalanceRepository): BalanceRepository
}

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

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.Money
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.R

private const val CENTS_PER_REAL = 100L

/**
 * An amount split to be read aloud: TalkBack must say "100 reais", not "R$ 100,00". Only the numbers are decided
 * here; the words (real/reais, centavo/centavos, "e") come from the plural resources of `strings_common.xml`.
 */
internal data class SpokenMoney(
    val reais: Long,
    val centavos: Int,
) {
    /** Plural quantity for the reais, clamped because plural resources take an `Int`. */
    val reaisQuantity: Int get() = reais.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()

    /** True for `R$ 0,00`, which is read "zero reais" (the plural rules of pt would say "0 real"). */
    val isZero: Boolean get() = reais == 0L && centavos == 0

    /** True when both parts are read, joined by "e" ("10 reais e 50 centavos"). */
    val hasReaisAndCentavos: Boolean get() = reais > 0L && centavos > 0

    /** True when only the cents are read ("50 centavos"). */
    val onlyCentavos: Boolean get() = reais == 0L && centavos > 0
}

internal fun Money.toSpokenMoney(): SpokenMoney =
    SpokenMoney(reais = cents / CENTS_PER_REAL, centavos = (cents % CENTS_PER_REAL).toInt())

/** An amount as TalkBack should say it: "zero reais", "50 centavos", "100 reais", "100 reais e 50 centavos". */
@Composable
internal fun spokenMoney(money: Money): String = spokenMoney(money.toSpokenMoney())

/** Same as [spokenMoney] for an amount already split (some screen models keep it that way). */
@Composable
internal fun spokenMoney(spoken: SpokenMoney): String = LocalResources.current.spokenMoneyText(spoken)

/** The resource lookup behind [spokenMoney]; usable outside composition, e.g. inside a `remember`. */
internal fun Resources.spokenMoneyText(spoken: SpokenMoney): String =
    when {
        spoken.isZero -> {
            getString(R.string.ticketing_common_spoken_zero)
        }

        spoken.onlyCentavos -> {
            spokenCentavos(spoken)
        }

        spoken.hasReaisAndCentavos -> {
            getString(
                R.string.ticketing_common_spoken_reais_and_centavos,
                spokenReais(spoken),
                spokenCentavos(spoken),
            )
        }

        else -> {
            spokenReais(spoken)
        }
    }

private fun Resources.spokenReais(spoken: SpokenMoney): String =
    getQuantityString(R.plurals.ticketing_common_spoken_reais, spoken.reaisQuantity, spoken.reais)

private fun Resources.spokenCentavos(spoken: SpokenMoney): String =
    getQuantityString(R.plurals.ticketing_common_spoken_centavos, spoken.centavos, spoken.centavos)

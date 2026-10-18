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
package com.rafaeloliveira.bilheteria.feature.ticketing.ui.receipt

/**
 * Where the operator came from. It only changes the navigation of the receipt (ui-spec SCR-03 4.2): after a sale
 * the way out is "Nova venda"; from the history it is the "voltar ao histórico" arrow. The receipt itself is the
 * same. The caller (the navigation graph) knows the origin; the receipt never guesses it.
 */
internal enum class ReceiptOrigin {
    /** Right after the payment result: shows "Nova venda", no back arrow. */
    Sale,

    /** Opened from the purchase history: shows the back arrow, no "Nova venda". */
    History,
}

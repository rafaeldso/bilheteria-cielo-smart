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
package com.rafaeloliveira.bilheteria.feature.ticketing.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.rafaeloliveira.bilheteria.feature.ticketing.api.navigation.EventListRoute
import com.rafaeloliveira.bilheteria.feature.ticketing.api.navigation.PurchaseHistoryRoute
import com.rafaeloliveira.bilheteria.feature.ticketing.api.navigation.PurchaseRoute
import com.rafaeloliveira.bilheteria.feature.ticketing.api.navigation.ReceiptRoute
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.receipt.ReceiptOrigin
import java.time.Clock
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.eventlist.EventListRoute as EventListScreenRoute
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.history.PurchaseHistoryRoute as PurchaseHistoryScreenRoute
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.purchase.PurchaseRoute as PurchaseScreenRoute
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.receipt.ReceiptRoute as ReceiptScreenRoute

/**
 * The ticketing flow as destinations of the host `NavHost` (plan §4): SCR-01 to SCR-04, typed by the routes of
 * `:feature:ticketing:api`. The start destination is [EventListRoute]. Click navigation lives here, as callbacks of
 * the screens' Routes; the one result-driven navigation, the end of a purchase, is a state of the purchase screen
 * (`Completed`) that replaces it with the receipt, so back from the receipt does not reopen the purchase.
 *
 * @param clock the clock of the app, so every screen shows dates in the zone the use cases group them in (BR-22).
 */
public fun NavGraphBuilder.ticketingGraph(
    navController: NavController,
    clock: Clock,
) {
    composable<EventListRoute> {
        EventListScreenRoute(
            onEventSelected = { eventId -> navController.navigateSingleTop(PurchaseRoute(eventId)) },
            onOpenHistory = { navController.navigateSingleTop(PurchaseHistoryRoute) },
            clock = clock,
        )
    }
    composable<PurchaseRoute> {
        PurchaseScreenRoute(
            onCompleted = { purchaseId ->
                navController.navigate(ReceiptRoute(purchaseId)) {
                    popUpTo<PurchaseRoute> { inclusive = true }
                    launchSingleTop = true
                }
            },
            onBackToEvents = { navController.backToEvents() },
            onNavigateUp = { navController.popBackStack() },
            clock = clock,
        )
    }
    composable<ReceiptRoute> { entry ->
        // The origin is read from the back stack, which survives process death: after a sale the receipt sits on the
        // event list (the purchase was popped); from the history the history is right below it.
        val origin = remember(entry) { navController.receiptOrigin() }
        ReceiptScreenRoute(
            origin = origin,
            onNewSale = { navController.backToEvents() },
            onNavigateUp = { navController.popBackStack() },
            clock = clock,
        )
    }
    composable<PurchaseHistoryRoute> {
        PurchaseHistoryScreenRoute(
            onPurchaseSelected = { purchaseId -> navController.navigateSingleTop(ReceiptRoute(purchaseId)) },
            onNavigateUp = { navController.popBackStack() },
            clock = clock,
        )
    }
}

/**
 * Takes the user back to the purchase left open when the app starts (BR-15, ui-spec flow). Call it once, next to the
 * `NavHost`: it reads the open purchase when the activity starts and, if the user is still on the event list, opens
 * SCR-02 on top of it. After a process death the restored back stack already holds the right screen, so nothing is
 * pushed again.
 */
@Composable
public fun OpenPurchaseRedirect(navController: NavHostController) {
    val viewModel: OpenPurchaseRedirectViewModel = hiltViewModel()
    val redirect by viewModel.uiState.collectAsStateWithLifecycle()
    val entry by navController.currentBackStackEntryAsState()
    val current = entry
    val target = redirect as? OpenPurchaseRedirectUiState.ToPurchase

    // Waits for the graph: the redirect only makes sense once the current destination is known.
    LaunchedEffect(target, current) {
        if (target != null && current != null) {
            if (current.destination.hasRoute<EventListRoute>()) {
                navController.navigateSingleTop(PurchaseRoute(target.eventId))
            }
            viewModel.onEvent(OpenPurchaseRedirectUiEvent.OnRedirectHandled)
        }
    }
}

private fun NavController.navigateSingleTop(route: Any) = navigate(route) { launchSingleTop = true }

/** Back to SCR-01 keeping it (the start of the flow); "Nova venda" and "Voltar para eventos" end here. */
private fun NavController.backToEvents() {
    if (!popBackStack<EventListRoute>(inclusive = false)) navigate(EventListRoute) { launchSingleTop = true }
}

/** History when the entry below the receipt is the history; otherwise the receipt came from a sale. */
private fun NavController.receiptOrigin(): ReceiptOrigin =
    if (previousBackStackEntry?.destination?.hasRoute<PurchaseHistoryRoute>() == true) {
        ReceiptOrigin.History
    } else {
        ReceiptOrigin.Sale
    }

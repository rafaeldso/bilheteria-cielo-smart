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
package com.rafaeloliveira.bilheteria.feature.ticketing.ui.tickets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsButton
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsButtonVariant
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsErrorState
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsIconButton
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsLoadingSkeleton
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsLoadingSkeletonVariant
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsQrCode
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsQrMatrix
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsSnackbar
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsTopAppBar
import com.rafaeloliveira.bilheteria.core.designsystem.icon.DsIcons
import com.rafaeloliveira.bilheteria.core.designsystem.theme.BilheteriaTheme
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.PrintFailureReason
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.QrMatrix
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.R
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.common.ActionBar
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.common.actionsScrollWithContent
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.format.shortEventDate
import java.time.ZoneId

/**
 * SCR-05 Ingressos (spec 002), stateless: every [TicketsUiState] has its own branch. In [TicketsUiState.Content] the
 * actions live in a bottom bar, or at the end of the content when the font is large; failures and the "Ingressos
 * impressos" notice are a snackbar on top of the content (never a dialog, and it does not move the list).
 *
 * @param zoneId zone of the event date shown above the tickets.
 * @param onNavigateUp the back arrow to the receipt (navigation, not a [TicketsUiEvent]).
 */
@Composable
internal fun TicketsScreen(
    uiState: TicketsUiState,
    zoneId: ZoneId,
    onEvent: (TicketsUiEvent) -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val content = uiState as? TicketsUiState.Content
    val actionsInline = actionsScrollWithContent()
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            DsTopAppBar(
                title = stringResource(R.string.ticketing_tickets_title),
                navigationIcon = {
                    DsIconButton(
                        icon = DsIcons.ArrowBack,
                        contentDescription = stringResource(R.string.ticketing_tickets_back),
                        onClick = onNavigateUp,
                    )
                },
            )
        },
        bottomBar = { if (content != null && !actionsInline) ActionBar { TicketActions(content.action, onEvent) } },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.TopCenter) {
            TicketsBody(uiState = uiState, zoneId = zoneId, onEvent = onEvent, actionsInline = actionsInline)
            if (content != null && !actionsInline) {
                ActionNotice(
                    action = content.action,
                    onEvent = onEvent,
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .widthIn(max = BilheteriaTheme.sizes.contentMaxWidth)
                            .padding(BilheteriaTheme.spacing.md),
                )
            }
        }
    }
}

@Composable
private fun TicketsBody(
    uiState: TicketsUiState,
    zoneId: ZoneId,
    onEvent: (TicketsUiEvent) -> Unit,
    actionsInline: Boolean,
) {
    when (uiState) {
        TicketsUiState.Loading -> {
            DsLoadingSkeleton(
                contentDescription = stringResource(R.string.ticketing_tickets_loading),
                variant = DsLoadingSkeletonVariant.Card,
            )
        }

        TicketsUiState.Error -> {
            DsErrorState(
                title = stringResource(R.string.ticketing_tickets_error_title),
                body = stringResource(R.string.ticketing_tickets_error_body),
                retryLabel = stringResource(R.string.ticketing_tickets_retry),
                onRetryClick = { onEvent(TicketsUiEvent.OnRetry) },
                icon = DsIcons.ConfirmationNumber,
            )
        }

        is TicketsUiState.Content -> {
            Column(
                modifier =
                    Modifier
                        .widthIn(max = BilheteriaTheme.sizes.contentMaxWidth)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
            ) {
                TicketsContent(content = uiState, zoneId = zoneId)
                if (actionsInline) {
                    InlineActions(action = uiState.action, onEvent = onEvent)
                } else {
                    // Room for the snackbar above the bar so it never hides the last ticket.
                    Box(modifier = Modifier.padding(bottom = BilheteriaTheme.spacing.xxxl))
                }
            }
        }
    }
}

/** Large font: the notice joins the list above the buttons, so it never hides them. */
@Composable
private fun InlineActions(
    action: TicketActionState,
    onEvent: (TicketsUiEvent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(BilheteriaTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(BilheteriaTheme.spacing.sm),
    ) {
        ActionNotice(action, onEvent)
        TicketActions(action, onEvent)
    }
}

/** Event header and one card per ticket (BR-35). */
@Composable
private fun TicketsContent(
    content: TicketsUiState.Content,
    zoneId: ZoneId,
) {
    val date = remember(content.event.startsAt, zoneId) { content.event.startsAt.shortEventDate(zoneId) }
    val eventWhen =
        stringResource(
            R.string.ticketing_purchase_event_when,
            date.weekday,
            date.day,
            date.month,
            date.time,
            content.event.venue,
        )
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = BilheteriaTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(BilheteriaTheme.spacing.md),
    ) {
        TicketsHeader(content = content, eventWhen = eventWhen)
        content.tickets.forEach { TicketCard(ticket = it, eventName = content.event.name, eventWhen = eventWhen) }
    }
}

@Composable
private fun TicketsHeader(
    content: TicketsUiState.Content,
    eventWhen: String,
) {
    val colors = MaterialTheme.colorScheme
    val count = content.tickets.size
    Column(verticalArrangement = Arrangement.spacedBy(BilheteriaTheme.spacing.xs)) {
        Text(text = content.event.name, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
        Text(text = eventWhen, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        Text(
            text = pluralStringResource(R.plurals.ticketing_tickets_count, count, count),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
    }
}

/**
 * One ticket (BR-35): "Ingresso n de q", the QR Code and the event. Composed here, not in the design system (UI
 * Spec 002 §5). No `mergeDescendants`: it would swallow the heading and repeat the QR Code label.
 */
@Composable
private fun TicketCard(
    ticket: TicketItem,
    eventName: String,
    eventWhen: String,
) {
    val matrix = remember(ticket.qr) { ticket.qr.toDsMatrix() }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(BilheteriaTheme.spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(BilheteriaTheme.spacing.md),
        ) {
            Text(
                text = stringResource(R.string.ticketing_tickets_item_title, ticket.ordinal, ticket.total),
                modifier = Modifier.fillMaxWidth().semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            DsQrCode(
                matrix = matrix,
                contentDescription =
                    stringResource(R.string.ticketing_tickets_qr_description, ticket.ordinal, ticket.total, eventName),
            )
            Text(
                text = stringResource(R.string.ticketing_tickets_event_lines, eventName, eventWhen),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** "Imprimir ingressos" (main) and "Compartilhar ingressos" (tonal, unavailable while printing: D3). */
@Composable
private fun TicketActions(
    action: TicketActionState,
    onEvent: (TicketsUiEvent) -> Unit,
) {
    val printing = action is TicketActionState.Printing
    DsButton(
        text = stringResource(if (printing) R.string.ticketing_tickets_printing else R.string.ticketing_tickets_print),
        onClick = { onEvent(TicketsUiEvent.OnPrint) },
        modifier = Modifier.semantics { if (printing) liveRegion = LiveRegionMode.Polite },
        fillMaxWidth = true,
        leadingIcon = DsIcons.Print,
        busyDescription = if (printing) stringResource(R.string.ticketing_tickets_printing_a11y) else null,
    )
    DsButton(
        text = stringResource(R.string.ticketing_tickets_share),
        onClick = { onEvent(TicketsUiEvent.OnShare) },
        fillMaxWidth = true,
        variant = DsButtonVariant.Tonal,
        enabled = !printing,
        leadingIcon = DsIcons.Share,
    )
}

/**
 * Failure or confirmation of the last action, over the content. "Ingressos impressos" ends by itself: the Route
 * times it.
 */
@Composable
private fun ActionNotice(
    action: TicketActionState,
    onEvent: (TicketsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (action) {
        is TicketActionState.PrintFailed -> {
            DsSnackbar(
                message = printFailedMessage(action),
                modifier = modifier,
                actionLabel = stringResource(R.string.ticketing_tickets_action_retry),
                onAction = { onEvent(TicketsUiEvent.OnRetryAction) },
            )
        }

        TicketActionState.ShareFailed -> {
            DsSnackbar(
                message = stringResource(R.string.ticketing_tickets_share_error),
                modifier = modifier,
                actionLabel = stringResource(R.string.ticketing_tickets_action_retry),
                onAction = { onEvent(TicketsUiEvent.OnRetryAction) },
            )
        }

        TicketActionState.Printed -> {
            DsSnackbar(message = stringResource(R.string.ticketing_tickets_printed), modifier = modifier)
        }

        TicketActionState.Idle, is TicketActionState.Printing, is TicketActionState.ShareReady -> {
            Unit
        }
    }
}

/** The reason in words plus how many tickets already came out, so a retry (which resumes) is not a surprise (BR-40). */
@Composable
private fun printFailedMessage(action: TicketActionState.PrintFailed): String {
    val reason =
        stringResource(
            when (action.reason) {
                PrintFailureReason.TerminalAppUnavailable -> R.string.ticketing_tickets_print_error_terminal
                PrintFailureReason.NoPaper -> R.string.ticketing_tickets_print_error_paper
                PrintFailureReason.PrinterError -> R.string.ticketing_tickets_print_error_printer
                PrintFailureReason.Timeout -> R.string.ticketing_tickets_print_error_timeout
                PrintFailureReason.Unknown -> R.string.ticketing_tickets_print_error_unknown
            },
        )
    if (action.reason == PrintFailureReason.TerminalAppUnavailable || action.printed == 0) return reason
    val progress =
        pluralStringResource(R.plurals.ticketing_tickets_print_progress, action.total, action.printed, action.total)
    return stringResource(R.string.ticketing_tickets_print_failed_format, reason, progress)
}

/** The design system draws its own matrix type, so it does not depend on the domain. */
private fun QrMatrix.toDsMatrix(): DsQrMatrix =
    DsQrMatrix(side = side, modules = BooleanArray(side * side) { isDark(it % side, it / side) })

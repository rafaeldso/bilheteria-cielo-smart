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

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.R
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.format.toReceiptDateText
import kotlinx.coroutines.delay
import java.time.Clock

/** How long "Ingressos impressos" stays on screen before the action state goes back to idle. */
private const val PRINTED_NOTICE_MILLIS = 4_000L

/**
 * Stateful entry of SCR-05 (`TicketsRoute(purchaseId)`): connects the [TicketsViewModel] to the stateless
 * [TicketsScreen] and opens the system share sheet when the ViewModel signals that the images are ready
 * ([TicketActionState.ShareReady]), answering with [TicketsUiEvent.OnShareLaunched] to consume the signal.
 *
 * @param onNavigateUp back to the receipt.
 * @param clock the app clock, for the zone of the event date.
 */
@Composable
internal fun TicketsRoute(
    onNavigateUp: () -> Unit,
    clock: Clock,
    modifier: Modifier = Modifier,
    viewModel: TicketsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val content = uiState as? TicketsUiState.Content
    val ready = content?.action as? TicketActionState.ShareReady

    // "Ingressos impressos" is a notice of the moment: it ends by itself (BR-44).
    LaunchedEffect(content?.action == TicketActionState.Printed) {
        if (content?.action == TicketActionState.Printed) {
            delay(PRINTED_NOTICE_MILLIS)
            viewModel.onEvent(TicketsUiEvent.OnActionDismissed)
        }
    }
    LaunchedEffect(ready) {
        if (content != null && ready != null) {
            // Consume the signal first, so a rotation cannot open the share sheet twice.
            viewModel.onEvent(TicketsUiEvent.OnShareLaunched)
            try {
                context.startActivity(shareChooser(context, ready, content, clock))
            } catch (expected: ActivityNotFoundException) {
                viewModel.onEvent(TicketsUiEvent.OnShareLaunchFailed)
            } catch (expected: SecurityException) {
                viewModel.onEvent(TicketsUiEvent.OnShareLaunchFailed)
            }
        }
    }
    TicketsScreen(
        uiState = uiState,
        zoneId = clock.zone,
        onEvent = viewModel::onEvent,
        onNavigateUp = onNavigateUp,
        modifier = modifier,
    )
}

/**
 * The system share sheet for the ticket images (BR-41, BR-42): `ACTION_SEND_MULTIPLE` with PNG `content://` URIs of the
 * private `FileProvider`, read access granted for this share only, and a short text with the event, date and venue of
 * each ticket. It carries no `reference`, no transaction data and no destination: the operator picks the app there.
 */
private fun shareChooser(
    context: Context,
    ready: TicketActionState.ShareReady,
    content: TicketsUiState.Content,
    clock: Clock,
): Intent {
    val uris = ready.bundle.uris.map(String::toUri)
    val intent =
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/png"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            putExtra(Intent.EXTRA_TEXT, shareText(context, content, clock))
            clipData = clipOf(uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    return Intent.createChooser(intent, context.getString(R.string.ticketing_tickets_share_chooser))
}

private fun clipOf(uris: List<Uri>): ClipData =
    ClipData.newRawUri("tickets", uris.first()).also { clip ->
        uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
    }

private fun shareText(
    context: Context,
    content: TicketsUiState.Content,
    clock: Clock,
): String {
    val dateTime = content.event.startsAt.toReceiptDateText(clock.zone, context.resources)
    return content.tickets.joinToString(separator = "\n") { ticket ->
        context.getString(
            R.string.ticketing_tickets_share_text,
            ticket.ordinal,
            ticket.total,
            content.event.name,
            dateTime,
            content.event.venue,
        )
    }
}

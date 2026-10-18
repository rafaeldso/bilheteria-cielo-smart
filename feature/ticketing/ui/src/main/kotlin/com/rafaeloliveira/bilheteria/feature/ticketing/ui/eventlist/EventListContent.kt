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
package com.rafaeloliveira.bilheteria.feature.ticketing.ui.eventlist

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsFilterChip
import com.rafaeloliveira.bilheteria.core.designsystem.component.DsListItem
import com.rafaeloliveira.bilheteria.core.designsystem.icon.DsIcons
import com.rafaeloliveira.bilheteria.core.designsystem.theme.BilheteriaTheme
import com.rafaeloliveira.bilheteria.core.designsystem.theme.tabular
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.AvailableEvent
import com.rafaeloliveira.bilheteria.feature.ticketing.domain.model.EventMonth
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.R
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.format.formatBrl
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.format.spokenEventDate
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.format.spokenMoney
import kotlinx.collections.immutable.ImmutableList

/**
 * `Content` of SCR-01: month filter above a list grouped by month, with a sticky header per month (BR-22, BR-23).
 * The filter stays outside the list so it is always visible; the headers stick to the top of the list.
 */
@Composable
internal fun EventListContent(
    content: EventListUiState.Content,
    formats: EventListFormats,
    onEvent: (EventListUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        MonthFilter(
            months = content.months,
            selectedMonth = content.selectedMonth,
            formats = formats,
            onMonthSelected = { onEvent(EventListUiEvent.OnMonthSelected(it)) },
        )
        // A new filter starts at the top of its list instead of keeping the previous scroll position.
        key(content.selectedMonth) {
            EventGroups(
                groups = content.groups,
                formats = formats,
                onEventClick = { onEvent(EventListUiEvent.OnEventSelected(it)) },
            )
        }
    }
}

@Composable
private fun ColumnScope.EventGroups(
    groups: ImmutableList<MonthGroup>,
    formats: EventListFormats,
    onEventClick: (id: String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.weight(1f),
        contentPadding = WindowInsets.navigationBars.asPaddingValues(),
    ) {
        groups.forEach { group ->
            stickyHeader(key = "month-${group.month.year}-${group.month.month}", contentType = HEADER_TYPE) {
                MonthHeader(month = group.month, formats = formats)
            }
            itemsIndexed(
                items = group.events,
                key = { _, item -> "event-${item.event.id}" },
                contentType = { _, _ -> EVENT_TYPE },
            ) { index, item ->
                EventItem(
                    item = item,
                    formats = formats,
                    showTopDivider = index > 0,
                    onClick = { onEventClick(item.event.id) },
                )
            }
        }
    }
}

@Composable
private fun MonthFilter(
    months: ImmutableList<MonthOption>,
    selectedMonth: EventMonth?,
    formats: EventListFormats,
    onMonthSelected: (EventMonth?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = BilheteriaTheme.spacing
    val groupLabel = stringResource(R.string.ticketing_events_filter_group)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics { contentDescription = groupLabel }
                .selectableGroup()
                .horizontalScroll(rememberScrollState())
                .padding(start = spacing.lg, top = spacing.xs, end = spacing.lg, bottom = spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        DsFilterChip(
            label = stringResource(R.string.ticketing_events_filter_all),
            selected = selectedMonth == null,
            onClick = { onMonthSelected(null) },
        )
        months.forEach { option ->
            DsFilterChip(
                label = monthText(formats.monthChip(option.month), R.string.ticketing_events_month_chip_format),
                selected = option.month == selectedMonth,
                onClick = { onMonthSelected(option.month) },
                spokenLabel =
                    monthText(formats.monthHeader(option.month), R.string.ticketing_events_month_header_format),
            )
        }
    }
}

@Composable
private fun MonthHeader(
    month: EventMonth,
    formats: EventListFormats,
    modifier: Modifier = Modifier,
) {
    val spacing = BilheteriaTheme.spacing
    Text(
        text = monthText(formats.monthHeader(month), R.string.ticketing_events_month_header_format),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(start = spacing.lg, top = spacing.md, end = spacing.lg, bottom = spacing.xs)
                .semantics { heading() },
    )
}

@Composable
private fun EventItem(
    item: AvailableEvent,
    formats: EventListFormats,
    showTopDivider: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val event = item.event
    val shortDate = remember(event.startsAt, formats) { formats.shortDate(event.startsAt) }
    DsListItem(
        headline = event.name,
        modifier = modifier,
        supportingText =
            stringResource(
                R.string.ticketing_events_item_supporting_format,
                shortDate.weekday,
                shortDate.day,
                shortDate.month,
                shortDate.time,
                event.venue,
            ),
        leadingIcon = DsIcons.ConfirmationNumber,
        onClick = onClick,
        spokenLabel = spokenLabel(item = item, formats = formats),
        showTopDivider = showTopDivider,
    ) {
        Text(
            text = remember(event.unitPrice) { event.unitPrice.formatBrl() },
            style = MaterialTheme.typography.labelLarge.tabular(),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = pluralStringResource(R.plurals.ticketing_events_available, item.available, item.available),
            style = MaterialTheme.typography.labelMedium.tabular(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** "Noite de Jazz, sábado, 15 de novembro, às 20 horas, Concha, 50 reais por ingresso, 48 ingressos disponíveis". */
@Composable
private fun spokenLabel(
    item: AvailableEvent,
    formats: EventListFormats,
): String {
    val event = item.event
    val date = remember(event.startsAt, formats) { formats.fullDate(event.startsAt) }
    return stringResource(
        R.string.ticketing_events_item_a11y,
        event.name,
        spokenEventDate(date),
        event.venue,
        spokenMoney(event.unitPrice),
        pluralStringResource(R.plurals.ticketing_events_available_spoken, item.available, item.available),
    )
}

@Composable
private fun monthText(
    label: MonthLabel,
    formatRes: Int,
): String = if (label.year == null) label.name else stringResource(formatRes, label.name, label.year)

private const val HEADER_TYPE = "month-header"
private const val EVENT_TYPE = "event"

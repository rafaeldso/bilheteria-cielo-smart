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
package com.rafaeloliveira.bilheteria.feature.ticketing.data.ticket

import androidx.core.content.FileProvider

/**
 * `FileProvider` of the ticket images shared through the system share sheet (spec 002, BR-41, BR-42). It is declared
 * as a subclass so it never clashes with another `FileProvider` of the app in the manifest merge. It is not exported
 * and only serves `files/shared_tickets/` (see `res/xml/ticket_file_paths.xml`); access is granted per share, to the
 * app the operator picks, with `FLAG_GRANT_READ_URI_PERMISSION`.
 */
internal class TicketFileProvider : FileProvider()

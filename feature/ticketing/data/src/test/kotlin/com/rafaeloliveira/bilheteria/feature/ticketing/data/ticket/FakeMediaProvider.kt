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

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import java.io.File
import java.io.FileNotFoundException

/**
 * A tiny `MediaStore.Images` stand-in for Robolectric: rows keep the values the app inserted and the bytes it wrote
 * to a file, so the print image store runs its real code (insert, write, publish, query path, delete, sweep).
 */
internal class FakeMediaProvider : ContentProvider() {
    class Row(
        val id: Long,
        val values: ContentValues,
        val file: File,
    )

    override fun onCreate(): Boolean = true

    override fun insert(
        uri: Uri,
        values: ContentValues?,
    ): Uri? {
        if (failInsert) return null
        val id = nextId++
        val file = File.createTempFile("media-$id-", ".png", checkNotNull(context).cacheDir)
        rows += Row(id, ContentValues(values), file)
        return ContentUris.withAppendedId(uri, id)
    }

    override fun openFile(
        uri: Uri,
        mode: String,
    ): ParcelFileDescriptor {
        if (failOpen) throw FileNotFoundException("simulated")
        val row = rows.firstOrNull { it.id == ContentUris.parseId(uri) } ?: throw FileNotFoundException("no row")
        return ParcelFileDescriptor.open(row.file, ParcelFileDescriptor.MODE_READ_WRITE)
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int {
        val row = rows.firstOrNull { it.id == ContentUris.parseId(uri) } ?: return 0
        row.values.putAll(values ?: ContentValues())
        return 1
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val cursor = MatrixCursor(arrayOf(MediaStore.MediaColumns.DATA))
        rows.firstOrNull { it.id == ContentUris.parseId(uri) }?.let { cursor.addRow(arrayOf(it.file.path)) }
        return cursor
    }

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int {
        val doomed =
            when {
                selection == null -> {
                    rows.filter { it.id == ContentUris.parseId(uri) }
                }

                selection.startsWith(MediaStore.MediaColumns.DATA) -> {
                    rows.filter {
                        it.file.path ==
                            selectionArgs?.first()
                    }
                }

                else -> {
                    rows.filter {
                        it.values
                            .getAsString(
                                MediaStore.MediaColumns.RELATIVE_PATH,
                            ).orEmpty()
                            .startsWith(prefix(selectionArgs))
                    }
                }
            }
        doomed.forEach { it.file.delete() }
        rows.removeAll(doomed.toSet())
        return doomed.size
    }

    override fun getType(uri: Uri): String = "image/png"

    private fun prefix(args: Array<out String>?): String = args?.first().orEmpty().removeSuffix("%")

    companion object {
        val rows = mutableListOf<Row>()
        var failInsert = false
        var failOpen = false
        private var nextId = 1L

        fun reset() {
            rows.forEach { it.file.delete() }
            rows.clear()
            failInsert = false
            failOpen = false
            nextId = 1L
        }
    }
}

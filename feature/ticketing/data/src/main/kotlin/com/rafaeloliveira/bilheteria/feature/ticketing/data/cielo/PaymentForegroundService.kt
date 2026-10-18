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
package com.rafaeloliveira.bilheteria.feature.ticketing.data.cielo

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.rafaeloliveira.bilheteria.feature.ticketing.data.R

/**
 * Foreground service (`dataSync`) that keeps the process alive while the Cielo app handles a payment; without it
 * Android may kill the app in the background and the transaction would not return (Cielo FAQ, research.md §12).
 * Started by [ForegroundServicePaymentSession] before the deep link and stopped by `PaymentGateway.finish()`.
 *
 * The notification shows no amount and no buyer data (BR-25, plan §7). It does nothing else: the answer is handled
 * by [CieloCallbackActivity].
 */
internal class PaymentForegroundService : Service() {
    override fun onCreate() {
        super.onCreate()
        NotificationManagerCompat.from(this).createNotificationChannel(
            NotificationChannelCompat
                .Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(getString(R.string.payment_channel_name))
                .build(),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification())
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int = START_NOT_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    /** Android 15+ ends `dataSync` services after a few hours; stop instead of crashing with ANR. */
    override fun onTimeout(
        startId: Int,
        fgsType: Int,
    ) {
        stopSelf(startId)
    }

    private fun notification(): Notification =
        NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_payment_notification)
            .setContentTitle(getString(R.string.payment_notification_title))
            .setContentText(getString(R.string.payment_notification_text))
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

    private companion object {
        const val CHANNEL_ID = "payment_in_progress"
        const val NOTIFICATION_ID = 1001
    }
}

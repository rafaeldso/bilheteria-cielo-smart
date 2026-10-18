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
package com.rafaeloliveira.bilheteria

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.rafaeloliveira.bilheteria.core.designsystem.theme.BilheteriaTheme
import com.rafaeloliveira.bilheteria.feature.ticketing.api.navigation.EventListRoute
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.navigation.OpenPurchaseRedirect
import com.rafaeloliveira.bilheteria.feature.ticketing.ui.navigation.ticketingGraph
import dagger.hilt.android.AndroidEntryPoint
import java.time.Clock
import javax.inject.Inject

/**
 * Single activity: hosts the `NavHost` with the ticketing flow, which starts on the event list. When the app opens
 * with a purchase still open, [OpenPurchaseRedirect] takes the user back to it (BR-15).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var clock: Clock

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            BilheteriaTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = EventListRoute) {
                    ticketingGraph(navController = navController, clock = clock)
                }
                OpenPurchaseRedirect(navController)
            }
        }
    }
}

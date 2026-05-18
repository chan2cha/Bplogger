package com.pulselog

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.pulselog.ui.BloodPressureScreen
import com.pulselog.ui.BpViewModel
import com.pulselog.ui.PulseLogTheme

class MainActivity : ComponentActivity() {

    private lateinit var vm: BpViewModel
    private var notificationsAllowed by mutableStateOf(false)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsAllowed = granted || canPostNotifications()
        if (notificationsAllowed) {
            vm.showNotificationPermissionGranted()
        } else {
            vm.showNotificationPermissionRequired()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appGraph = AppGraph(applicationContext)
        vm = appGraph.createViewModel()
        notificationsAllowed = canPostNotifications()

        setContent {
            PulseLogTheme {
                BloodPressureScreen(
                    vm = vm,
                    notificationsAllowed = notificationsAllowed,
                    onRequestNotificationPermission = ::requestNotificationPermissionIfNeeded
                )
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !canPostNotifications()
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }
}

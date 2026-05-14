package com.pulselog

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.pulselog.ui.BloodPressureScreen
import com.pulselog.ui.BpViewModel
import com.pulselog.ui.PulseLogTheme

class MainActivity : ComponentActivity() {

    private lateinit var vm: BpViewModel
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // The receiver checks permission before posting, so no immediate follow-up is required.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appGraph = AppGraph(applicationContext)
        vm = appGraph.createViewModel()

        setContent {
            PulseLogTheme {
                BloodPressureScreen(
                    vm = vm,
                    onRequestNotificationPermission = ::requestNotificationPermissionIfNeeded
                )
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

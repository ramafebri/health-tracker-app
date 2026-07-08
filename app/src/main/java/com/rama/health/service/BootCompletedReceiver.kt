package com.rama.health.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.rama.health.data.local.datastore.StepPreferencesDataSource
import com.rama.health.util.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Restarts step tracking after a device reboot, but only if the user had previously opted in
 * ([StepPreferencesDataSource.trackingEnabled]) and the required runtime permission is still
 * granted. Does nothing otherwise -- no crash, no service start.
 */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject lateinit var stepPreferencesDataSource: StepPreferencesDataSource

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val wasTrackingEnabled = stepPreferencesDataSource.trackingEnabled.first()
                val hasPermission = PermissionUtils.hasActivityRecognitionPermission(context)
                if (wasTrackingEnabled && hasPermission) {
                    val serviceIntent = Intent(context, StepCounterService::class.java)
                    ContextCompat.startForegroundService(context, serviceIntent)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

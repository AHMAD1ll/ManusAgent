package com.manus.agent

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.manus.agent.ManusAccessibilityService.Companion.ACTION_SERVICE_STATE_CHANGED
import com.manus.agent.ManusAccessibilityService.Companion.EXTRA_STATE
import com.manus.agent.ManusAccessibilityService.Companion.STATE_MODEL_LOAD_FAIL
import com.manus.agent.ManusAccessibilityService.Companion.STATE_MODEL_LOAD_SUCCESS
import com.manus.agent.ui.theme.ManusAgentTheme

class MainActivity : ComponentActivity() {

    private var serviceStatus by mutableStateOf("Initializing...")

    private val serviceStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // التحقق من أن intent ليس null
            intent?.let {
                val state = it.getStringExtra(EXTRA_STATE)
                serviceStatus = when (state) {
                    STATE_MODEL_LOAD_SUCCESS -> "Ready"
                    STATE_MODEL_LOAD_FAIL -> "Error: Model failed to load"
                    else -> state ?: "Unknown State"
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ManusAgentTheme {
                Greeting(name = serviceStatus)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // تسجيل الـ receiver
        registerReceiver(serviceStateReceiver, IntentFilter(ACTION_SERVICE_STATE_CHANGED), RECEIVER_NOT_EXPORTED)
    }

    override fun onPause() {
        super.onPause()
        // إلغاء تسجيل الـ receiver
        unregisterReceiver(serviceStateReceiver)
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Service Status: $name")
}

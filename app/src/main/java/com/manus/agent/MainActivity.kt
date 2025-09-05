package com.manus.agent

import android.Manifest
import android.content.*
import android.content.pm.PackageManager  // أضفت هذا السطر
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.manus.agent.ui.theme.ManusAgentTheme
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    private var uiState by mutableStateOf(UiState())

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) copyModelFiles()
            else uiState = uiState.copy(statusMessage = "Permission denied. Cannot copy models.")
        }

    private val openSettingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { checkPermissionsAndServiceStatus() }

    private val serviceStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val state = it.getStringExtra(ManusAccessibilityService.EXTRA_STATE)
                val message = it.getStringExtra(ManusAccessibilityService.EXTRA_MESSAGE)
                Log.d("MainActivity", "Received state: $state, Message: $message")
                uiState = uiState.copy(statusMessage = message ?: state ?: "Unknown State")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ManusAgentTheme {
                MainScreen(
                    uiState = uiState,
                    onGrantPermissionClick = ::requestStoragePermission,
                    onEnableServiceClick = ::openAccessibilitySettings
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(serviceStateReceiver, IntentFilter(ManusAccessibilityService.ACTION_SERVICE_STATE_CHANGED))
        checkPermissionsAndServiceStatus()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(serviceStateReceiver)
    }

    private fun checkPermissionsAndServiceStatus() {
        val hasPermission = hasStoragePermission()
        val isServiceEnabled = isAccessibilityServiceEnabled()
        val modelsCopied = areModelsCopied()

        uiState = uiState.copy(
            hasStoragePermission = hasPermission,
            isAccessibilityServiceEnabled = isServiceEnabled,
            areModelsCopied = modelsCopied,
            statusMessage = when {
                !hasPermission -> "Storage permission needed to copy AI models."
                !modelsCopied -> "Copying AI models..."
                !isServiceEnabled -> "Accessibility Service is not enabled."
                else -> "Ready to receive commands."
            }
        )

        if (hasPermission && !modelsCopied) copyModelFiles()
    }

    private fun hasStoragePermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager()
        else ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                openSettingsLauncher.launch(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                openSettingsLauncher.launch(intent)
            }
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val service = "$packageName/${ManusAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabledServices?.contains(service) == true
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    private fun areModelsCopied(): Boolean = File(filesDir, "phi3.onnx").exists()

    private fun copyModelFiles() {
        val modelNames = listOf("phi3.onnx", "phi3.onnx.data", "tokenizer.json")
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        CoroutineScope(Dispatchers.IO).launch {
            var allFilesFound = true
            for (modelName in modelNames) {
                val sourceFile = File(downloadDir, modelName)
                if (!sourceFile.exists()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Error: $modelName not found in Download folder.", Toast.LENGTH_LONG).show()
                        uiState = uiState.copy(statusMessage = "Please place model files in Download folder.")
                    }
                    allFilesFound = false
                    break
                }
            }

            if (allFilesFound) {
                for (modelName in modelNames) {
                    val sourceFile = File(downloadDir, modelName)
                    val destFile = File(filesDir, modelName)
                    if (!destFile.exists() || destFile.length() != sourceFile.length()) {
                        sourceFile.copyTo(destFile, overwrite = true)
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "AI Models copied successfully!", Toast.LENGTH_SHORT).show()
                    uiState = uiState.copy(areModelsCopied = true)
                    checkPermissionsAndServiceStatus()
                }
            }
        }
    }
}

data class UiState(
    val hasStoragePermission: Boolean = false,
    val isAccessibilityServiceEnabled: Boolean = false,
    val areModelsCopied: Boolean = false,
    val statusMessage: String = "Initializing..."
)

@Composable
fun MainScreen(uiState: UiState, onGrantPermissionClick: () -> Unit, onEnableServiceClick: () -> Unit) {
    val context = LocalContext.current
    var commandText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Manus Agent", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(uiState.statusMessage, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(32.dp))

        when {
            !uiState.hasStoragePermission -> Button(onClick = onGrantPermissionClick) { Text("1. Grant File Permission") }
            !uiState.areModelsCopied -> {
                CircularProgressIndicator()
                Spacer(Modifier.height(8.dp))
                Text("Copying models...")
            }
            !uiState.isAccessibilityServiceEnabled -> Button(onClick = onEnableServiceClick) { Text("2. Enable Accessibility Service") }
            else -> {
                OutlinedTextField(
                    value = commandText,
                    onValueChange = { commandText = it },
                    label = { Text("Enter your command here") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    val intent = Intent(context, ManusAccessibilityService::class.java).apply {
                        action = ManusAccessibilityService.ACTION_COMMAND
                        putExtra(ManusAccessibilityService.EXTRA_COMMAND_TEXT, commandText)
                    }
                    context.startService(intent)
                    Toast.makeText(context, "Command sent!", Toast.LENGTH_SHORT).show()
                }) { Text("Start Task") }
            }
        }
    }
}

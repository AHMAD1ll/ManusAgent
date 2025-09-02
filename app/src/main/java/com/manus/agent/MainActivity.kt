package com.manus.agent

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    private var uiState by mutableStateOf(UiState())

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                copyModelFiles()
            } else {
                uiState = uiState.copy(statusMessage = "Permission denied. Cannot copy models.")
            }
        }

    private val openSettingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            checkPermissionsAndServiceStatus()
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
        checkPermissionsAndServiceStatus()
    }

    private fun checkPermissionsAndServiceStatus() {
        val hasPermission = hasStoragePermission()
        val isServiceEnabled = isAccessibilityServiceEnabled()
        val modelsCopied = areModelsCopied()

        uiState = if (!hasPermission) {
            uiState.copy(statusMessage = "Storage permission needed to copy AI models.")
        } else if (!modelsCopied) {
            uiState.copy(statusMessage = "Copying AI models...")
            copyModelFiles()
        } else if (!isServiceEnabled) {
            uiState.copy(statusMessage = "Accessibility Service is not enabled.")
        } else {
            uiState.copy(statusMessage = "Ready to receive commands.")
        }
    }

    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
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
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(service) == true
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    private fun areModelsCopied(): Boolean {
        val modelFile = File(filesDir, "phi3.onnx")
        return modelFile.exists()
    }

    private fun copyModelFiles() {
        val modelNames = listOf("phi3.onnx", "phi3.onnx.data", "tokenizer.json")
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        var allFilesFound = true
        for (modelName in modelNames) {
            val sourceFile = File(downloadDir, modelName)
            if (!sourceFile.exists()) {
                Log.e("FileCopy", "Source file not found: ${sourceFile.absolutePath}")
                Toast.makeText(this, "Error: $modelName not found in Download folder.", Toast.LENGTH_LONG).show()
                allFilesFound = false
                break
            }
        }

        if (allFilesFound) {
            Thread {
                var success = true
                try {
                    for (modelName in modelNames) {
                        val sourceFile = File(downloadDir, modelName)
                        val destFile = File(filesDir, modelName)
                        if (!destFile.exists() || destFile.length() != sourceFile.length()) {
                            sourceFile.inputStream().use { input ->
                                FileOutputStream(destFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("FileCopy", "Error copying files", e)
                    success = false
                }
                runOnUiThread {
                    if (success) {
                        Toast.makeText(this, "AI Models copied successfully!", Toast.LENGTH_SHORT).show()
                        uiState = uiState.copy(areModelsCopied = true)
                        checkPermissionsAndServiceStatus()
                    } else {
                        Toast.makeText(this, "Failed to copy AI models.", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        } else {
            uiState = uiState.copy(statusMessage = "Please place model files in Download folder.")
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
fun MainScreen(
    uiState: UiState,
    onGrantPermissionClick: () -> Unit,
    onEnableServiceClick: () -> Unit
) {
    val context = LocalContext.current
    var commandText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Manus Agent", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = uiState.statusMessage, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(32.dp))

        if (!uiState.hasStoragePermission) {
            Button(onClick = onGrantPermissionClick) {
                Text("1. Grant File Permission")
            }
        } else if (!uiState.areModelsCopied) {
            CircularProgressIndicator()
            Text("Copying models...")
        } else if (!uiState.isAccessibilityServiceEnabled) {
            Button(onClick = onEnableServiceClick) {
                Text("2. Enable Accessibility Service")
            }
        } else {
            OutlinedTextField(
                value = commandText,
                onValueChange = { commandText = it },
                label = { Text("Enter your command here") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                val intent = Intent(context, ManusAccessibilityService::class.java).apply {
                    action = ManusAccessibilityService.ACTION_COMMAND
                    putExtra(ManusAccessibilityService.EXTRA_COMMAND_TEXT, commandText)
                }
                context.startService(intent)
                Toast.makeText(context, "Command sent!", Toast.LENGTH_SHORT).show()
            }) {
                Text("Start Task")
            }
        }
    }
}

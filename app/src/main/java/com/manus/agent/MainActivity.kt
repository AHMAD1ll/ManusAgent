package com.manus.agent

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.manus.agent.ui.theme.ManusAgentTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.*

/**
 * MainActivity:
 * - Chat-like UI (text + mic)
 * - Permissions & copy model files from Downloads -> app filesDir
 * - Auto-transition to chat when ready
 * - Simple local "memory" = list of messages (last 50)
 * - Sends commands to ManusAccessibilityService via Intent (ACTION_COMMAND + EXTRA_COMMAND_TEXT)
 * - Receives async service updates via broadcast (ACTION_SERVICE_STATE_CHANGED)
 */

data class ChatMessage(val id: String = UUID.randomUUID().toString(), val text: String, val fromUser: Boolean, val timestamp: Long = System.currentTimeMillis())

class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"

    private val modelFiles = listOf("phi3.onnx", "phi3.onnx.data", "tokenizer.json")

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening by mutableStateOf(false)

    // BroadcastReceiver to receive status/messages from ManusAccessibilityService
    private val serviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            val state = intent.getStringExtra(ManusAccessibilityService.EXTRA_STATE)
            val message = intent.getStringExtra(ManusAccessibilityService.EXTRA_MESSAGE)
            Log.d(TAG, "Service update: state=$state message=$message")
            if (!message.isNullOrBlank()) {
                addAssistantMessage(message)
            } else if (!state.isNullOrBlank()) {
                addAssistantMessage("Service: $state")
            }
            checkAllReadyAndNotify()
        }
    }

    // Activity results
    private val requestRecordAudioLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(this, "Microphone permission denied.", Toast.LENGTH_SHORT).show()
            }
        }

    private val openAllFilesSettingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // user returned from MANAGE_ALL_FILES_ACCESS_PERMISSION
            checkPermissionsAndState()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ManusAgentTheme {
                AppContent()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(serviceReceiver, IntentFilter(ManusAccessibilityService.ACTION_SERVICE_STATE_CHANGED))
        checkPermissionsAndState()
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(serviceReceiver)
        } catch (e: Exception) {
            // ignore
        }
        stopListening()
    }

    // ---------------------------
    // UI state & helpers
    // ---------------------------
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val _messages = mutableStateListOf<ChatMessage>()
    private val messages: List<ChatMessage> get() = _messages

    private var hasStoragePermissionState by mutableStateOf(false)
    private var modelsCopiedState by mutableStateOf(false)
    private var accessibilityEnabledState by mutableStateOf(false)
    private var statusMessageState by mutableStateOf("Initializing...")

    private fun addUserMessage(text: String) {
        coroutineScope.launch(Dispatchers.Main) {
            _messages.add(0, ChatMessage(text = text, fromUser = true))
            trimMessages()
        }
    }

    private fun addAssistantMessage(text: String) {
        coroutineScope.launch(Dispatchers.Main) {
            _messages.add(0, ChatMessage(text = text, fromUser = false))
            trimMessages()
        }
    }

    private fun trimMessages() {
        while (_messages.size > 50) _messages.removeLast()
    }

    // ---------------------------
    // Permissions & checks
    // ---------------------------
    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = android.net.Uri.parse("package:$packageName")
                openAllFilesSettingsLauncher.launch(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                openAllFilesSettingsLauncher.launch(intent)
            }
        } else {
            // READ_EXTERNAL_STORAGE for older devices
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) {
                    copyModelFilesIfNeeded()
                } else {
                    Toast.makeText(this, "Storage permission denied.", Toast.LENGTH_SHORT).show()
                }
            }.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun requestRecordAudioPermission() {
        requestRecordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    private fun checkPermissionsAndState() {
        hasStoragePermissionState = hasStoragePermission()
        modelsCopiedState = areModelsCopied()
        accessibilityEnabledState = isAccessibilityServiceEnabled()
        statusMessageState = when {
            !hasStoragePermissionState -> "Storage permission required to copy AI models."
            !modelsCopiedState -> "Model files missing — copying..."
            !accessibilityEnabledState -> "Accessibility Service is not enabled."
            else -> "Ready"
        }
        if (hasStoragePermissionState && !modelsCopiedState) {
            copyModelFilesIfNeeded()
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val service = "$packageName/${ManusAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabledServices?.contains(service) == true
    }

    // ---------------------------
    // File copy logic
    // ---------------------------
    private fun areModelsCopied(): Boolean {
        return modelFiles.all { File(filesDir, it).exists() }
    }

    private fun copyModelFilesIfNeeded() {
        coroutineScope.launch {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            var anyMissing = false
            for (name in modelFiles) {
                val src = File(downloadDir, name)
                if (!src.exists()) {
                    anyMissing = true
                    break
                }
            }
            if (anyMissing) {
                // notify user on main thread
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Put model files (phi3.onnx, .data, tokenizer.json) into Downloads folder.", Toast.LENGTH_LONG).show()
                    statusMessageState = "Place model files in Downloads."
                }
                return@launch
            }

            var success = true
            try {
                for (name in modelFiles) {
                    val src = File(downloadDir, name)
                    val dst = File(filesDir, name)
                    if (!dst.exists() || dst.length() != src.length()) {
                        src.inputStream().use { input ->
                            FileOutputStream(dst).use { out ->
                                input.copyTo(out)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "copy failed", e)
                success = false
            }

            runOnUiThread {
                if (success) {
                    modelsCopiedState = true
                    statusMessageState = "Models copied."
                    Toast.makeText(this@MainActivity, "Models copied successfully.", Toast.LENGTH_SHORT).show()
                    addAssistantMessage("AI models ready.")
                    checkAllReadyAndNotify()
                } else {
                    statusMessageState = "Failed copying models."
                    Toast.makeText(this@MainActivity, "Failed to copy models.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // ---------------------------
    // Send command to service
    // ---------------------------
    private fun sendCommandToService(command: String) {
        if (command.isBlank()) return
        addUserMessage(command)
        // optimistic assistant placeholder
        addAssistantMessage("Processing...")
        try {
            val intent = Intent(this, ManusAccessibilityService::class.java).apply {
                action = ManusAccessibilityService.ACTION_COMMAND
                putExtra(ManusAccessibilityService.EXTRA_COMMAND_TEXT, command)
            }
            startService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send command", e)
            addAssistantMessage("Failed to send command to service: ${e.message}")
        }
    }

    // ---------------------------
    // Speech recognition
    // ---------------------------
    private fun ensureSpeechRecognizer() {
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
                override fun onError(error: Int) {
                    isListening = false
                    runOnUiThread { Toast.makeText(this@MainActivity, "Speech recognition error: $error", Toast.LENGTH_SHORT).show() }
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    if (text.isNotBlank()) {
                        sendCommandToService(text)
                    }
                }
            })
        }
    }

    private fun startListening() {
        if (!hasRecordAudioPermission()) {
            requestRecordAudioPermission()
            return
        }
        ensureSpeechRecognizer()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        try {
            speechRecognizer?.startListening(intent)
            isListening = true
        } catch (e: Exception) {
            isListening = false
            Toast.makeText(this, "Cannot start speech recognition: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
        } catch (e: Exception) { /* ignore */ }
        isListening = false
    }

    // ---------------------------
    // Helpers
    // ---------------------------
    private fun checkAllReadyAndNotify() {
        hasStoragePermissionState = hasStoragePermission()
        modelsCopiedState = areModelsCopied()
        accessibilityEnabledState = isAccessibilityServiceEnabled()
        statusMessageState = when {
            !hasStoragePermissionState -> "Storage permission required"
            !modelsCopiedState -> "Models missing"
            !accessibilityEnabledState -> "Enable Accessibility Service"
            else -> "Ready"
        }
    }

    // ---------------------------
    // Composable UI
    // ---------------------------
    @Composable
    fun AppContent() {
        val context = LocalContext.current
        var inputText by remember { mutableStateOf("") }
        val msgs by remember { derivedStateOf { messages } }

        LaunchedEffect(Unit) {
            checkPermissionsAndState()
        }

        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Manus Agent", textAlign = TextAlign.Center) }
                )
                when {
                    !hasStoragePermissionState -> {
                        PermissionCard(
                            title = "Storage permission required",
                            description = "Grant file permission so the app can copy AI models from Downloads.",
                            buttonText = "Grant",
                            onClick = { requestStoragePermission() }
                        )
                    }
                    !modelsCopiedState -> {
                        StatusCard(title = "Copying models", description = "Ensure phi3.onnx, .data and tokenizer.json are in Downloads.")
                    }
                    !accessibilityEnabledState -> {
                        StatusCard(
                            title = "Enable Accessibility",
                            description = "Enable Manus Agent in Accessibility settings.",
                            buttonText = "Open Settings",
                            onClick = { openAccessibilitySettings() }
                        )
                    }
                    else -> {
                        // Chat UI
                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                ChatMessagesList(messages = msgs)
                                Divider()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = inputText,
                                        onValueChange = { inputText = it },
                                        modifier = Modifier.weight(1f),
                                        placeholder = { Text("Type a command...") }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(onClick = {
                                        if (inputText.isNotBlank()) {
                                            sendCommandToService(inputText.trim())
                                            inputText = ""
                                        }
                                    }) {
                                        Icon(Icons.Default.Send, contentDescription = "Send")
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(onClick = {
                                        if (isListening) stopListening() else startListening()
                                    }) {
                                        Icon(
                                            imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
                                            contentDescription = "Mic"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Footer(status = statusMessageState)
            }
        }
    }

    @Composable
    fun PermissionCard(title: String, description: String, buttonText: String = "Grant", onClick: (() -> Unit)? = null) {
        Card(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(description)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { onClick?.invoke() }) {
                    Text(buttonText)
                }
            }
        }
    }

    @Composable
    fun StatusCard(title: String, description: String, buttonText: String? = null, onClick: (() -> Unit)? = null) {
        Card(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(description)
                Spacer(modifier = Modifier.height(12.dp))
                if (buttonText != null) {
                    Button(onClick = { onClick?.invoke() }) {
                        Text(buttonText)
                    }
                }
            }
        }
    }

    @Composable
    fun ChatMessagesList(messages: List<ChatMessage>) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp),
            reverseLayout = true
        ) {
            itemsIndexed(messages) { index, msg ->
                ChatRow(msg)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    @Composable
    fun ChatRow(message: ChatMessage) {
        val align = if (message.fromUser) Alignment.End else Alignment.Start
        val bg = if (message.fromUser) Color(0xFFDCF8C6) else Color(0xFFECEFF1)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start) {
            Box(modifier = Modifier
                .widthIn(max = 280.dp)
                .background(bg, RoundedCornerShape(12.dp))
                .padding(10.dp)
            ) {
                Text(text = message.text)
            }
        }
    }

    @Composable
    fun Footer(status: String) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFAFAFA))
            .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = status, style = MaterialTheme.typography.bodySmall)
        }
    }
}
```0

package com.manus.agent

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class ManusAccessibilityService : AccessibilityService() {

    private var ortEnv: OrtEnvironment? = null
    private var session: OrtSession? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        const val ACTION_COMMAND = "com.manus.agent.ACTION_COMMAND"
        const val EXTRA_COMMAND_TEXT = "command_text"
        const val ACTION_SERVICE_STATE_CHANGED = "com.manus.agent.SERVICE_STATE_CHANGED"
        const val EXTRA_STATE = "state"
        const val EXTRA_MESSAGE = "message"
        private const val TAG = "ManusAccessibilityService"
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Service connected. Initializing AI model...")
        broadcastState("initializing", "Initializing AI model...")
        
        scope.launch {
            initializeOrt()
        }
    }

    private fun initializeOrt() {
        try {
            val modelPath = File(filesDir, "phi3.onnx").absolutePath
            val modelFile = File(modelPath)

            if (!modelFile.exists() || modelFile.length() == 0L) {
                Log.e(TAG, "Model file is missing or empty at path: $modelPath")
                broadcastState("error", "Model file missing or empty. Check app storage.")
                return
            }
            Log.d(TAG, "Model file found at: $modelPath, size: ${modelFile.length()} bytes.")

            Log.d(TAG, "Creating ONNX Runtime environment...")
            ortEnv = OrtEnvironment.getEnvironment()
            
            // --- هذا هو الحل الصحيح والوحيد المطلوب ---
            val sessionOptions = OrtSession.SessionOptions().apply {
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                setMemoryPatternOptimization(true)
            }
            // ------------------------------------------
            
            Log.d(TAG, "Creating ONNX session with memory optimization...")
            session = ortEnv?.createSession(modelPath, sessionOptions)

            if (session != null) {
                Log.d(TAG, "ONNX session created successfully. Model is ready.")
                broadcastState("ready", "AI Model loaded. Ready for commands.")
            } else {
                Log.e(TAG, "ONNX session creation returned null.")
                broadcastState("error", "Failed to create ONNX session (returned null).")
            }

        } catch (e: Exception) {
            Log.e(TAG, "CRITICAL: Error initializing ONNX Runtime", e)
            broadcastState("error", "Model Load Error: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            if (it.action == ACTION_COMMAND) {
                val command = it.getStringExtra(EXTRA_COMMAND_TEXT) ?: ""
                if (command.isNotEmpty()) {
                    handleCommand(command)
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun handleCommand(command: String) {
        Log.d(TAG, "Command received: $command")
        
        if (session == null) {
            Toast.makeText(this, "AI Model not ready yet.", Toast.LENGTH_SHORT).show()
            Log.w(TAG, "Session is null, cannot process command.")
            return
        }

        Toast.makeText(this, "Command received: $command. AI processing not implemented yet.", Toast.LENGTH_LONG).show()
    }

    private fun findNodeByText(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        if (node.text?.toString()?.contains(text, ignoreCase = true) == true) return node
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let {
                val found = findNodeByText(it, text)
                if (found != null) return found
            }
        }
        return null
    }

    private fun broadcastState(state: String, message: String) {
        val intent = Intent(ACTION_SERVICE_STATE_CHANGED).apply {
            putExtra(EXTRA_STATE, state)
            putExtra(EXTRA_MESSAGE, message)
            `package` = packageName
        }
        sendBroadcast(intent)
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) { }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            session?.close()
            ortEnv?.close()
            Log.d(TAG, "Service destroyed, ONNX session closed.")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing ONNX session on destroy", e)
        }
    }
}

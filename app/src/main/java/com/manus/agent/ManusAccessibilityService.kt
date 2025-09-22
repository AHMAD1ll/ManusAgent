package com.manus.agent

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.Collections

class ManusAccessibilityService : AccessibilityService() {

    // --- متغيرات جديدة للذكاء الاصطناعي ---
    private var ortEnv: OrtEnvironment? = null
    private var session: OrtSession? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    // ------------------------------------

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
        Log.d(TAG, "Service connected.")
        broadcastState("connected", "Accessibility Service connected")
        
        // بدء تهيئة النموذج في الخلفية
        scope.launch {
            initializeOrt()
        }
    }

    private fun initializeOrt() {
        try {
            Log.d(TAG, "Initializing ONNX Runtime...")
            val modelFile = File(filesDir, "phi3.onnx")
            if (!modelFile.exists()) {
                Log.e(TAG, "Model file not found at: ${modelFile.absolutePath}")
                broadcastState("error", "Model file not found.")
                return
            }

            ortEnv = OrtEnvironment.getEnvironment()
            val sessionOptions = OrtSession.SessionOptions()
            session = ortEnv?.createSession(modelFile.absolutePath, sessionOptions)

            Log.d(TAG, "ONNX session created successfully.")
            broadcastState("ready", "AI Model loaded. Ready for commands.")

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing ONNX Runtime", e)
            broadcastState("error", "Failed to load AI model.")
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) { }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        session?.close()
        ortEnv?.close()
        Log.d(TAG, "Service destroyed, ONNX session closed.")
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

        // TODO: لاحقاً سنقوم بتحليل الشاشة هنا وإرسالها للنموذج
        Toast.makeText(this, "Command received: $command. AI processing not implemented yet.", Toast.LENGTH_LONG).show()

        // المنطق القديم للبحث بالنص (كمثال مؤقت)
        val rootNode = rootInActiveWindow ?: return
        val targetNode = findNodeByText(rootNode, command)
        targetNode?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
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
        }
        sendBroadcast(intent)
    }
}

package com.manus.agent

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import ai.onnxruntime.OnnxRuntime
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OrtSession.SessionOptions
import java.io.File

class ManusAccessibilityService : AccessibilityService() {

    private val TAG = "ManusAccessibilityService"

    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var modelInitialized = false

    companion object {
        const val ACTION_COMMAND = "com.manus.agent.ACTION_COMMAND"
        const val EXTRA_COMMAND_TEXT = "EXTRA_COMMAND_TEXT"

        const val ACTION_SERVICE_STATE_CHANGED = "com.manus.agent.ACTION_SERVICE_STATE_CHANGED"
        const val EXTRA_STATE = "EXTRA_STATE"
        const val EXTRA_MESSAGE = "EXTRA_MESSAGE"

        private const val MODEL_FILE = "phi3.onnx" // expected filename copied into filesDir
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Service connected")
        sendStateBroadcast("connected", "Accessibility service connected.")
        initializeOrtIfPossible()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // not used for now; could listen for window changes if needed
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            if (intent?.action == ACTION_COMMAND) {
                val cmd = intent.getStringExtra(EXTRA_COMMAND_TEXT) ?: ""
                Log.d(TAG, "Received command: $cmd")
                // handle command asynchronously to avoid blocking; here we keep it simple
                handleCommand(cmd)
            }
        } catch (e: Exception) {
            Log.e(TAG, "onStartCommand error", e)
            sendStateBroadcast("error", "Failed to handle command: ${e.message}")
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun initializeOrtIfPossible() {
        if (modelInitialized) return
        try {
            val modelFile = File(filesDir, MODEL_FILE)
            if (!modelFile.exists()) {
                Log.w(TAG, "ONNX model not found at ${modelFile.absolutePath}")
                sendStateBroadcast("model_missing", "Model file not found.")
                return
            }

            ortEnv = OrtEnvironment.getEnvironment()
            val options = SessionOptions()
            ortSession = ortEnv?.createSession(modelFile.absolutePath, options)
            modelInitialized = ortSession != null
            sendStateBroadcast("model_ready", if (modelInitialized) "ONNX model loaded." else "Failed to init model.")
            Log.d(TAG, "ONNX init success: $modelInitialized")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to init ONNX", t)
            sendStateBroadcast("model_error", "ONNX init error: ${t.message}")
        }
    }

    private fun handleCommand(command: String) {
        // Simple flow:
        // 1. try to interpret simple actions without model (heuristic)
        // 2. if model available, (future) run prompt->model to get structured action
        // For now implement robust heuristic click-by-text + broadcast status

        // quick heuristics
        val lc = command.trim().lowercase()
        when {
            lc.startsWith("click ") -> {
                val target = command.removePrefix("click ").trim()
                val success = clickNodeByText(target)
                sendStateBroadcast("action", if (success) "Clicked: $target" else "Could not find: $target")
            }
            lc.startsWith("press ") -> {
                val target = command.removePrefix("press ").trim()
                val success = clickNodeByText(target)
                sendStateBroadcast("action", if (success) "Pressed: $target" else "Could not find: $target")
            }
            lc.startsWith("back") -> {
                performGlobalAction(GLOBAL_ACTION_BACK)
                sendStateBroadcast("action", "Performed BACK")
            }
            else -> {
                // If ONNX is initialized in future, call it here.
                if (!modelInitialized) {
                    sendStateBroadcast("unhandled", "No model: could not interpret command.")
                } else {
                    // placeholder for model-based parsing (not implemented fully)
                    sendStateBroadcast("unhandled", "Model available but model-inference path not implemented.")
                }
            }
        }
    }

    private fun clickNodeByText(text: String): Boolean {
        try {
            val root = rootInActiveWindow ?: return false
            val found = findNodeByText(root, text)
            if (found != null) {
                // prefer performAction on clickable ancestor
                var node: AccessibilityNodeInfo? = found
                while (node != null) {
                    if (node.isClickable) {
                        val performed = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        Log.d(TAG, "performAction click result: $performed for text=$text")
                        return performed
                    }
                    node = node.parent
                }
                // fallback: try to click found node
                return found.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "clickNodeByText error", t)
        }
        return false
    }

    private fun findNodeByText(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val q = text.trim()
        // breadth-first search
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(node)
        while (queue.isNotEmpty()) {
            val n = queue.removeFirst()
            try {
                val nodeText = n.text?.toString()
                val desc = n.contentDescription?.toString()
                if (!nodeText.isNullOrBlank() && nodeText.equals(q, ignoreCase = true)) return n
                if (!desc.isNullOrBlank() && desc.equals(q, ignoreCase = true)) return n
                // also contains match
                if (!nodeText.isNullOrBlank() && nodeText.contains(q, ignoreCase = true)) return n
                if (!desc.isNullOrBlank() && desc.contains(q, ignoreCase = true)) return n
                for (i in 0 until n.childCount) {
                    val child = n.getChild(i)
                    if (child != null) {
                        queue.add(child)
                    }
                }
            } catch (e: Exception) {
                // ignore per-node errors
            }
        }
        return null
    }

    private fun sendStateBroadcast(state: String, message: String) {
        try {
            val intent = Intent(ACTION_SERVICE_STATE_CHANGED)
            intent.putExtra(EXTRA_STATE, state)
            intent.putExtra(EXTRA_MESSAGE, message)
            sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e(TAG, "sendStateBroadcast failed", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            ortSession?.close()
        } catch (_: Exception) { }
        try {
            ortEnv?.close()
        } catch (_: Exception) { }
        ortSession = null
        ortEnv = null
        modelInitialized = false
    }
}

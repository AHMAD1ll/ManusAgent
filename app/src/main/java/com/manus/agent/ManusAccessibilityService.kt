package com.manus.agent

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import java.io.File

class ManusAccessibilityService : AccessibilityService() {

    companion object {
        const val ACTION_COMMAND = "com.manus.agent.ACTION_COMMAND"
        const val EXTRA_COMMAND_TEXT = "command_text"
        const val ACTION_SERVICE_STATE_CHANGED = "com.manus.agent.SERVICE_STATE_CHANGED"
        const val EXTRA_STATE = "state"
        const val EXTRA_MESSAGE = "message"
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        copyModelIfExists() // نسخ الملفات إذا وجدت
        broadcastState("connected", "Accessibility Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) { }

    override fun onInterrupt() { }

    private fun copyModelIfExists() {
        val filesToCopy = listOf("phi3.onnx", "phi3.onnx.data", "tokenizer.json")
        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_DOWNLOADS
        )

        for (fileName in filesToCopy) {
            val sourceFile = File(downloadsDir, fileName)
            val destFile = File(filesDir, fileName)
            
            if (destFile.exists()) continue // إذا الملف موجود، skip
            
            if (sourceFile.exists()) {
                sourceFile.copyTo(destFile) // انسخ إذا المصدر موجود
                Log.d("Manus", "تم نسخ: $fileName")
            } else {
                Log.w("Manus", "ملف غير موجود: $fileName")
            }
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
        Log.d("ManusAccessibilityService", "Command received: $command")
        Toast.makeText(this, "Command received: $command", Toast.LENGTH_SHORT).show()
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

    private fun performClick(x: Float, y: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = android.graphics.Path()
            path.moveTo(x, y)
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
                .build()
            dispatchGesture(gesture, null, null)
        }
    }
}

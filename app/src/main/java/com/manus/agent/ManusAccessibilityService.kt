package com.manus.agent

import android.view.accessibility.AccessibilityNodeInfo
import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

class ManusAccessibilityService : AccessibilityService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private var currentTask: String? = null

    companion object {
        // إعادة تعريف كل الثوابت (Constants) هنا
        const val ACTION_SERVICE_STATE_CHANGED = "com.manus.agent.SERVICE_STATE_CHANGED"
        const val EXTRA_STATE = "EXTRA_STATE"
        const val EXTRA_MESSAGE = "EXTRA_MESSAGE"
        const val STATE_CONNECTED = "CONNECTED"
        const val STATE_DISCONNECTED = "DISCONNECTED"
        const val STATE_MODEL_LOAD_FAIL = "MODEL_LOAD_FAIL"
        const val STATE_MODEL_LOAD_SUCCESS = "STATE_MODEL_LOAD_SUCCESS"
        const val ACTION_COMMAND = "com.manus.agent.COMMAND"
        const val EXTRA_COMMAND_TEXT = "EXTRA_COMMAND_TEXT"
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        broadcastState(STATE_CONNECTED)
        Toast.makeText(this, "Manus Agent Service: CONNECTED", Toast.LENGTH_SHORT).show()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_COMMAND) {
            currentTask = intent.getStringExtra(EXTRA_COMMAND_TEXT)
            Toast.makeText(this, "New task: $currentTask", Toast.LENGTH_SHORT).show()
            val rootNode = rootInActiveWindow
            if (rootNode != null) {
                val screenContent = captureScreenContent(rootNode)
                Log.d("ManusService", "Current screen:\n$screenContent")
                rootNode.recycle()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Logic for handling accessibility events
    }

    private fun captureScreenContent(node: AccessibilityNodeInfo?): String {
        if (node == null) return ""
        val builder = StringBuilder()
        traverseNode(node, builder)
        return builder.toString()
    }

    private fun traverseNode(node: AccessibilityNodeInfo, builder: StringBuilder) {
        val text: String? = node.text?.toString()?.trim()
        val contentDesc: String? = node.contentDescription?.toString()?.trim()

        if (!text.isNullOrEmpty()) {
            builder.append(text).append("\n")
        } else if (!contentDesc.isNullOrEmpty()) {
            builder.append(contentDesc).append("\n")
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                traverseNode(child, builder)
                child.recycle()
            }
        }
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        broadcastState(STATE_DISCONNECTED)
        Toast.makeText(this, "Manus Agent Service: DISCONNECTED", Toast.LENGTH_SHORT).show()
        job.cancel()
        return super.onUnbind(intent)
    }

    private fun broadcastState(state: String, message: String? = null) {
        val intent = Intent(ACTION_SERVICE_STATE_CHANGED).apply {
            putExtra(EXTRA_STATE, state)
            message?.let { putExtra(EXTRA_MESSAGE, it) }
        }
        sendBroadcast(intent)
    }
}

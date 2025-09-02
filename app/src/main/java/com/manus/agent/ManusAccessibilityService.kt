package com.manus.agent

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.microsoft.onnxruntime.*

class ManusAccessibilityService : AccessibilityService() {

    companion object {
        const val ACTION_COMMAND = "com.manus.agent.ACTION_COMMAND"
        const val EXTRA_COMMAND_TEXT = "EXTRA_COMMAND_TEXT"
        const val ACTION_SERVICE_STATE_CHANGED = "com.manus.agent.ACTION_SERVICE_STATE_CHANGED"
        const val EXTRA_STATE = "EXTRA_STATE"
        const val EXTRA_MESSAGE = "EXTRA_MESSAGE"
    }

    private lateinit var ortEnvironment: OrtEnvironment
    private var ortSession: OrtSession? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or AccessibilityEvent.TYPE_VIEW_CLICKED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
        }
        serviceInfo = info

        initializeOrt()
        sendServiceState("CONNECTED", "Service connected and ONNX ready.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // يمكن تطوير المعالجة المستقبلية للأحداث هنا
    }

    override fun onInterrupt() {
        Log.d("ManusService", "AccessibilityService interrupted")
    }

    private fun initializeOrt() {
        try {
            ortEnvironment = OrtEnvironment.getEnvironment()
            val modelFile = filesDir.resolve("phi3.onnx")
            ortSession = ortEnvironment.createSession(modelFile.absolutePath)
            Log.d("ManusService", "ONNX Runtime session initialized")
        } catch (e: Exception) {
            Log.e("ManusService", "Failed to initialize ONNX", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            if (it.action == ACTION_COMMAND) {
                val commandText = it.getStringExtra(EXTRA_COMMAND_TEXT)
                commandText?.let { cmd ->
                    handleCommand(cmd)
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun handleCommand(command: String) {
        // هنا يتم تمرير الأمر إلى النموذج وتنفيذ الفعل
        Log.d("ManusService", "Received command: $command")
        performClickOnButton("example_button_id") // مثال
    }

    private fun performClickOnButton(buttonId: String) {
        val rootNode = rootInActiveWindow
        rootNode?.let {
            val nodes = it.findAccessibilityNodeInfosByViewId(buttonId)
            if (nodes.isNotEmpty()) {
                nodes[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
                sendServiceState("ACTION", "Clicked button $buttonId")
            }
        }
    }

    private fun sendServiceState(state: String, message: String) {
        val intent = Intent(ACTION_SERVICE_STATE_CHANGED)
        intent.putExtra(EXTRA_STATE, state)
        intent.putExtra(EXTRA_MESSAGE, message)
        sendBroadcast(intent)
    }
}

package com.manus.agent

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.microsoft.onnxruntime.*

class ManusAccessibilityService : AccessibilityService() {

    companion object {
        const val ACTION_COMMAND = "com.manus.agent.ACTION_COMMAND"
        const val EXTRA_COMMAND_TEXT = "command_text"
        const val ACTION_SERVICE_STATE_CHANGED = "com.manus.agent.SERVICE_STATE_CHANGED"
        const val EXTRA_STATE = "state"
        const val EXTRA_MESSAGE = "message"
    }

    private var ortEnvironment: OrtEnvironment? = null
    private var ortSession: OrtSession? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        initializeOrt()
        broadcastState("connected", "Accessibility Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // يمكن إضافة مراقبة الأحداث لاحقًا
    }

    override fun onInterrupt() {
        // يمكن إضافة منطق التوقف عند الانقطاع
    }

    private fun initializeOrt() {
        try {
            ortEnvironment = OrtEnvironment.getEnvironment()
            val modelFile = filesDir.resolve("phi3.onnx")

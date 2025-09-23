package com.manus.agent

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.knuddels.jtokkit.Encodings
import com.knuddels.jtokkit.api.Encoding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.LongBuffer

class ManusAccessibilityService : AccessibilityService() {

    private var ortEnv: OrtEnvironment? = null
    private var session: OrtSession? = null
    private var tokenizer: Encoding? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        // ... نفس الـ companion object ...
        const val ACTION_COMMAND = "com.manus.agent.ACTION_COMMAND"
        const val EXTRA_COMMAND_TEXT = "command_text"
        const val ACTION_SERVICE_STATE_CHANGED = "com.manus.agent.SERVICE_STATE_CHANGED"
        const val EXTRA_STATE = "state"
        const val EXTRA_MESSAGE = "message"
        private const val TAG = "ManusAccessibilityService"
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Service connected. Initializing AI components...")
        broadcastState("initializing", "Initializing AI components...")
        
        scope.launch {
            initializeComponents()
        }
    }

    private fun initializeComponents() {
        try {
            // 1. تحميل الـ Tokenizer
            Log.d(TAG, "Loading tokenizer...")
            val tokenizerFile = File(filesDir, "tokenizer.json")
            if (!tokenizerFile.exists()) {
                broadcastState("error", "tokenizer.json not found.")
                return
            }
            val tokenizerJson = tokenizerFile.readText()
            val registry = Encodings.newDefaultEncodingRegistry()
            tokenizer = registry.getEncoding(com.knuddels.jtokkit.api.EncodingType.O200K_BASE) // Phi-3 uses this
            Log.d(TAG, "Tokenizer loaded.")
            
            // 2. تحميل نموذج ONNX
            Log.d(TAG, "Loading ONNX model...")
            val modelPath = File(filesDir, "phi3.onnx").absolutePath
            if (!File(modelPath).exists()) {
                broadcastState("error", "phi3.onnx not found.")
                return
            }
            
            ortEnv = OrtEnvironment.getEnvironment()
            val sessionOptions = OrtSession.SessionOptions().apply {
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                setMemoryPatternOptimization(true)
            }
            
            session = ortEnv?.createSession(modelPath, sessionOptions)

            if (session != null && tokenizer != null) {
                Log.d(TAG, "AI components ready.")
                broadcastState("ready", "AI Ready. Give me a prompt to complete.")
            } else {
                broadcastState("error", "Failed to initialize AI components.")
            }

        } catch (e: Exception) {
            Log.e(TAG, "CRITICAL: Error during initialization", e)
            broadcastState("error", "Init Error: ${e.message}")
        }
    }

    private fun handleCommand(command: String) {
        scope.launch {
            if (session == null || tokenizer == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ManusAccessibilityService, "AI not ready.", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            try {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ManusAccessibilityService, "Thinking...", Toast.LENGTH_SHORT).show()
                }

                // 1. تحويل النص إلى Tokens
                val prompt = "<|user|>\n$command<|end|>\n<|assistant|>"
                val tokens = tokenizer!!.encode(prompt)
                val tokenIds = tokens.map { it.toLong() }.toLongArray()
                
                // 2. تجهيز المدخلات للنموذج
                val tensor = OnnxTensor.createTensor(ortEnv, LongBuffer.wrap(tokenIds), longArrayOf(1, tokenIds.size.toLong()))
                val inputs = mapOf("input_ids" to tensor)

                // 3. تشغيل الاستدلال
                val results = session!!.run(inputs)
                val outputTensor = results[0] as OnnxTensor
                val outputIds = (outputTensor.value as Array<LongArray>)[0]

                // 4. تحويل الـ Tokens الناتجة إلى نص
                val generatedText = tokenizer!!.decode(outputIds.toList())

                withContext(Dispatchers.Main) {
                    // عرض النتيجة
                    Toast.makeText(this@ManusAccessibilityService, "Result: $generatedText", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error during inference", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ManusAccessibilityService, "Inference Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    // ... باقي الدوال تبقى كما هي ...
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

    private fun broadcastState(state: String, message: String) {
        val intent = Intent(ACTION_SERVICE_STATE_CHANGED).apply {
            putExtra(EXTRA_STATE, state)
            putExtra(EXTRA_MESSAGE, message)
            `package` = packageName
        }
        sendBroadcast(intent)
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) { }
    override fun onInterrupt() { }
    override fun onDestroy() {
        super.onDestroy()
        session?.close()
        ortEnv?.close()
    }
}

package com.sotech.chameleon.execution

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.chaquo.python.Python
import com.chaquo.python.PyObject
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ExecutionResult(
    val success: Boolean,
    val output: String,
    val error: String,
    val graphData: GraphData? = null,
    val imageBitmap: Bitmap? = null
)

class CodeExecutor @Inject constructor() {

    // Maintain dictionary state per session ID to allow persistent variables like Jupyter/Colab
    private val sessionGlobals = mutableMapOf<String, PyObject>()

    fun resetSession(sessionId: String) {
        sessionGlobals.remove(sessionId)
    }

    suspend fun execute(code: String, language: String, context: Context, sessionId: String = "default"): ExecutionResult = withContext(Dispatchers.IO) {
        if (language.lowercase() != "python") {
            return@withContext ExecutionResult(false, "", "Only Python is supported via Chaquopy.")
        }

        try {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(context))
            }
            val py = Python.getInstance()

            // Fetch or create persistent globals dict for this session
            val globals = sessionGlobals.getOrPut(sessionId) {
                py.getModule("builtins").callAttr("dict")
            }

            val wrapperCode = """
import sys
import io
import base64
import traceback

old_stdout = sys.stdout
old_stderr = sys.stderr
sys.stdout = stdout_buffer = io.StringIO()
sys.stderr = stderr_buffer = io.StringIO()

image_data = None

try:
    import matplotlib
    matplotlib.use('Agg')
    import matplotlib.pyplot as plt
    
    def custom_show(*args, **kwargs):
        global image_data
        buf = io.BytesIO()
        plt.savefig(buf, format='png', bbox_inches='tight')
        buf.seek(0)
        image_data = base64.b64encode(buf.read()).decode('utf-8')
        plt.clf()
        
    plt.show = custom_show
except Exception:
    pass

error_msg = ""
try:
    exec(user_code, globals())
except Exception as e:
    error_msg = traceback.format_exc()

sys.stdout = old_stdout
sys.stderr = old_stderr

output = stdout_buffer.getvalue()

result = {
    "output": output.strip(),
    "error": error_msg.strip(),
    "image": image_data
}
            """.trimIndent()

            // Inject the user code into the persistent globals namespace
            globals.callAttr("__setitem__", "user_code", code)

            // Execute the wrapper script in the context of our persistent globals
            py.getModule("builtins").callAttr("exec", wrapperCode, globals)

            val resultDict = globals.callAttr("get", "result")
            val outputStr = resultDict?.callAttr("get", "output")?.toString() ?: ""
            val errorStr = resultDict?.callAttr("get", "error")?.toString() ?: ""
            val imageObj = resultDict?.callAttr("get", "image")

            var bitmap: Bitmap? = null
            if (imageObj != null && imageObj.toString() != "None") {
                val base64Str = imageObj.toString()
                val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
                bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            }

            val success = errorStr.isEmpty()
            val finalOutput = if (success) outputStr else outputStr + "\n" + errorStr

            return@withContext ExecutionResult(success, finalOutput, errorStr, null, bitmap)
        } catch (e: Exception) {
            return@withContext ExecutionResult(false, "", e.message ?: "Unknown Chaquopy Execution Error")
        }
    }

    suspend fun getInstalledPackages(context: Context): List<String> = withContext(Dispatchers.IO) {
        try {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(context))
            }
            val py = Python.getInstance()
            val code = """
import pkgutil
modules = []
for module_info in pkgutil.iter_modules():
    modules.append(module_info.name)
result = "\n".join(sorted(modules))
            """.trimIndent()

            val tempGlobals = py.getModule("builtins").callAttr("dict")
            tempGlobals.callAttr("__setitem__", "user_code", code)
            py.getModule("builtins").callAttr("exec", "exec(user_code, globals())", tempGlobals)

            val resultStr = tempGlobals.callAttr("get", "result")?.toString() ?: ""
            return@withContext resultStr.split("\n").filter { it.isNotBlank() }
        } catch (e: Exception) {
            return@withContext listOf("Failed to fetch packages: ${e.message}")
        }
    }
}
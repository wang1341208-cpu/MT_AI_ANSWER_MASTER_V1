package com.mtaimaster.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private val Bg = Color(0xFF070B11)
private val Panel = Color(0xFF111B27)
private val Cyan = Color(0xFF35C8F4)
private val Green = Color(0xFF38DDA5)
private val Muted = Color(0xFF91A3B5)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = darkColorScheme(primary = Cyan, background = Bg, surface = Panel)) { AnswerMasterApp(this) } }
    }
}

@Composable
private fun AnswerMasterApp(activity: MainActivity) {
    val prefs = remember { activity.getSharedPreferences("master", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    var endpoint by remember { mutableStateOf(prefs.getString("endpoint", "https://api.openai.com/v1/chat/completions") ?: "") }
    var model by remember { mutableStateOf(prefs.getString("model", "gpt-4o-mini") ?: "gpt-4o-mini") }
    var apiKey by remember { mutableStateOf(prefs.getString("api_key", "") ?: "") }
    var screenText by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("等待识别屏幕题目。请先配置兼容 OpenAI Chat Completions 的 AI 服务。") }
    var targetText by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var autoEnabled by remember { mutableStateOf(prefs.getBoolean("auto_enabled", false)) }
    var showRootConfirm by remember { mutableStateOf(false) }
    var rootOutput by remember { mutableStateOf("Root 未执行。Root 仅提供固定只读诊断命令。") }

    fun refreshScreen() {
        screenText = prefs.getString("last_screen_text", "") ?: ""
        if (screenText.isBlank()) answer = "暂时没有读取到屏幕文字。请开启无障碍服务，并切换到目标应用后重试。"
    }

    if (showRootConfirm) {
        AlertDialog(
            onDismissRequest = { showRootConfirm = false },
            containerColor = Panel,
            title = { Text("确认临时 Root 诊断", color = Green, fontWeight = FontWeight.Bold) },
            text = { Text("将请求 su 权限，只运行固定只读命令：id、getprop ro.build.version.release、getenforce。不会执行任意命令。是否继续？", color = Color.White) },
            confirmButton = {
                TextButton(onClick = {
                    showRootConfirm = false
                    scope.launch {
                        rootOutput = withContext(Dispatchers.IO) {
                            try {
                                val p = ProcessBuilder("su", "-c", "id; getprop ro.build.version.release; getenforce").redirectErrorStream(true).start()
                                val result = p.inputStream.bufferedReader().use { it.readText() }
                                "退出状态：${p.waitFor()}\n$result"
                            } catch (e: Exception) { "Root 请求失败：${e.message}" }
                        }
                    }
                }) { Text("确认执行", color = Green) }
            },
            dismissButton = { TextButton(onClick = { showRootConfirm = false }) { Text("取消") } }
        )
    }

    Column(Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("MT AI ANSWER MASTER", color = Cyan, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                Text("SCREEN AI  •  FULL AUTO  •  ROOT CONFIRM", color = Muted, fontSize = 9.sp)
            }
            Surface(color = Color(0xFF12352D), shape = RoundedCornerShape(20.dp)) { Text("V1.0", color = Green, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 11.sp) }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("① 屏幕控制中心", color = Cyan, fontWeight = FontWeight.Bold)
                Text("通过 Android 无障碍服务读取当前应用可访问的文字；不需要 Root。", color = Muted, fontSize = 12.sp)
                Button(onClick = { activity.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }, modifier = Modifier.fillMaxWidth()) { Text("打开无障碍设置") }
                OutlinedButton(onClick = { refreshScreen() }, modifier = Modifier.fillMaxWidth()) { Text("读取当前屏幕文字") }
                Text(if (prefs.getBoolean("accessibility_connected", false)) "无障碍服务：已连接" else "无障碍服务：尚未确认连接", color = if (prefs.getBoolean("accessibility_connected", false)) Green else Muted, fontSize = 11.sp)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("② AI 服务配置", color = Cyan, fontWeight = FontWeight.Bold)
                OutlinedTextField(endpoint, { endpoint = it }, label = { Text("API 地址") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(model, { model = it }, label = { Text("模型名称") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(apiKey, { apiKey = it }, label = { Text("API Key") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation())
                Button(onClick = { prefs.edit().putString("endpoint", endpoint.trim()).putString("model", model.trim()).putString("api_key", apiKey.trim()).apply(); answer = "AI 配置已保存在本机应用私有空间。" }, modifier = Modifier.fillMaxWidth()) { Text("保存 AI 配置") }
                Text("使用兼容 OpenAI Chat Completions 的服务。API Key 仅保存在本机应用私有数据中；请勿分享 APK 或密钥。", color = Muted, fontSize = 10.sp)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("③ AI 识题与自动操作", color = Cyan, fontWeight = FontWeight.Bold)
                Text("屏幕文字预览", color = Muted, fontSize = 11.sp)
                Text(screenText.ifBlank { "尚未读取屏幕内容" }, color = Color.White, fontSize = 11.sp, modifier = Modifier.fillMaxWidth().heightIn(max = 150.dp).background(Color(0xFF080E15), RoundedCornerShape(8.dp)).padding(10.dp))
                Button(enabled = !busy, onClick = {
                    refreshScreen()
                    val captured = prefs.getString("last_screen_text", "") ?: ""
                    if (captured.isBlank()) { answer = "没有屏幕文字可分析，请先开启无障碍服务。"; return@Button }
                    if (apiKey.isBlank()) { answer = "请先填写并保存 API Key。"; return@Button }
                    busy = true
                    scope.launch {
                        val result = withContext(Dispatchers.IO) { askAi(endpoint, model, apiKey, captured) }
                        answer = result
                        busy = false
                        try {
                            val json = JSONObject(result.substringAfter("```json", result).substringBefore("```").trim())
                            val action = json.optString("action", "none")
                            val target = json.optString("target_text", "").trim()
                            val command = Intent(ScreenAgentService.ACTION_CLICK_TEXT).setPackage(activity.packageName)
                            when (action) {
                                "click" -> if (target.isNotBlank()) {
                                    targetText = target
                                    if (autoEnabled) {
                                        command.putExtra(ScreenAgentService.EXTRA_ACTION, "click").putExtra(ScreenAgentService.EXTRA_TARGET, target)
                                        activity.sendBroadcast(command)
                                        answer += "\n\n自动操作已发送：点击可见控件「$target」。"
                                    } else answer += "\n\nAI 建议点击「$target」，但自动模式未开启。"
                                }
                                "input" -> {
                                    val value = json.optString("value", "")
                                    if (autoEnabled && value.isNotEmpty() && value.length <= 2000) {
                                        command.putExtra(ScreenAgentService.EXTRA_ACTION, "input").putExtra(ScreenAgentService.EXTRA_VALUE, value)
                                        activity.sendBroadcast(command)
                                        answer += "\n\n自动操作已发送：填入当前已聚焦的输入框。"
                                    } else answer += "\n\nAI 建议输入内容；请先聚焦输入框并开启自动模式。"
                                }
                                "scroll_down", "scroll_up" -> if (autoEnabled) {
                                    command.putExtra(ScreenAgentService.EXTRA_ACTION, action)
                                    activity.sendBroadcast(command)
                                    answer += "\n\n自动操作已发送：${if (action == "scroll_down") "向下" else "向上"}滚动。"
                                } else answer += "\n\nAI 建议滚动屏幕，但自动模式未开启。"
                            }
                        } catch (_: Exception) { /* Plain-text AI response is also displayed; no action is taken. */ }
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "AI 分析中…" else "分析屏幕并生成操作建议") }
                Text(answer, color = Color(0xFFB8F3D9), fontSize = 12.sp, modifier = Modifier.fillMaxWidth().background(Color(0xFF080E15), RoundedCornerShape(8.dp)).padding(10.dp))
                OutlinedTextField(targetText, { targetText = it }, label = { Text("精确点击文字（可选）") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("FULL 自动点击模式", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("仅点击屏幕上文字完全匹配的可见控件", color = Muted, fontSize = 10.sp)
                    }
                    Switch(checked = autoEnabled, onCheckedChange = { autoEnabled = it; prefs.edit().putBoolean("auto_enabled", it).apply() })
                }
                Button(enabled = autoEnabled && targetText.isNotBlank(), onClick = {
                    activity.sendBroadcast(Intent(ScreenAgentService.ACTION_CLICK_TEXT).setPackage(activity.packageName).putExtra(ScreenAgentService.EXTRA_ACTION, "click").putExtra(ScreenAgentService.EXTRA_TARGET, targetText.trim()))
                }, modifier = Modifier.fillMaxWidth()) { Text("执行指定文字点击") }
                OutlinedButton(onClick = { autoEnabled = false; prefs.edit().putBoolean("auto_enabled", false).apply(); answer = "自动操作已停止。" }, modifier = Modifier.fillMaxWidth()) { Text("紧急停止自动操作") }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("④ 临时 Root（逐次确认）", color = Green, fontWeight = FontWeight.Bold)
                Text("屏幕读取和点击不需要 Root。此处只提供固定只读诊断，执行前必须确认。", color = Muted, fontSize = 11.sp)
                Button(onClick = { showRootConfirm = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF174235))) { Text("请求 Root 并运行只读诊断") }
                Text(rootOutput, color = Color(0xFFB8F3D9), fontSize = 11.sp)
            }
        }
        Text("安全说明：仅在你自己的设备和获准使用的应用中操作。服务不会绕过登录、验证码、付费或应用安全限制。屏幕内容可能含隐私信息，发送给 AI 前请先检查。", color = Muted, fontSize = 10.sp)
        Text("MT AI ANSWER MASTER • ANDROID • V1.0", color = Muted, fontSize = 9.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

private fun askAi(endpoint: String, model: String, apiKey: String, screen: String): String {
    var connection: HttpURLConnection? = null
    return try {
        connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 30000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")
        }
        val system = "你是屏幕答题辅助 AI。分析屏幕文字并帮助理解题目。只返回一个 JSON 对象。可用 action：click（仅当屏幕上存在明确选项，target_text 必须与屏幕文字完全一致）、input（value 仅填入当前已聚焦输入框）、scroll_down、scroll_up、none。格式示例：{\"action\":\"click\",\"target_text\":\"与屏幕完全一致的选项\",\"answer\":\"答案\",\"reason\":\"理由\"}。输入格式：{\"action\":\"input\",\"value\":\"要输入的文字\",\"answer\":\"答案\"}。如果不确定，必须使用 action=none。绝不猜测不存在的控件文字，不执行购买、转账、删除、发送消息、提交支付等有后果的操作。"
        val body = JSONObject().put("model", model).put("messages", org.json.JSONArray()
            .put(JSONObject().put("role", "system").put("content", system))
            .put(JSONObject().put("role", "user").put("content", "请分析当前屏幕：\n$screen")))
            .put("temperature", 0.2)
        connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val raw = stream.bufferedReader().use { it.readText() }
        if (code !in 200..299) "AI 服务错误 HTTP $code：$raw"
        else JSONObject(raw).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
    } catch (e: Exception) { "请求失败：${e.message}\n请检查网络、API 地址、模型名称和 API Key。" }
    finally { connection?.disconnect() }
}

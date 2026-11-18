package com.mtaimaster.app

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ScreenAgentService : AccessibilityService() {
    private val prefs by lazy { getSharedPreferences("master", Context.MODE_PRIVATE) }

    private val commandReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != ACTION_CLICK_TEXT) return
            if (!prefs.getBoolean("auto_enabled", false)) return
            when (intent.getStringExtra(EXTRA_ACTION)) {
                "click" -> {
                    val target = intent.getStringExtra(EXTRA_TARGET)?.trim().orEmpty()
                    if (target.isNotBlank() && target.length <= 100) clickVisibleText(target)
                }
                "input" -> {
                    val value = intent.getStringExtra(EXTRA_VALUE).orEmpty()
                    if (value.length <= 2000) inputFocusedField(value)
                }
                "scroll_down" -> scrollScreen(forward = true)
                "scroll_up" -> scrollScreen(forward = false)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs.edit().putBoolean("accessibility_connected", true).apply()
        val filter = IntentFilter(ACTION_CLICK_TEXT)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(commandReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(commandReceiver, filter)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val root = rootInActiveWindow ?: return
        val lines = mutableListOf<String>()
        collectText(root, lines, 0)
        val screen = lines.distinct().take(120).joinToString("\n").take(12000)
        prefs.edit().putString("last_screen_text", screen).putLong("screen_updated", System.currentTimeMillis()).apply()
        root.recycle()
    }

    private fun collectText(node: AccessibilityNodeInfo?, out: MutableList<String>, depth: Int) {
        if (node == null || depth > 20 || out.size >= 250) return
        val text = node.text?.toString()?.trim().orEmpty()
        val desc = node.contentDescription?.toString()?.trim().orEmpty()
        if (text.isNotBlank()) out.add(text)
        if (desc.isNotBlank() && desc != text) out.add(desc)
        for (i in 0 until node.childCount) collectText(node.getChild(i), out, depth + 1)
    }

    private fun inputFocusedField(value: String) {
        val root = rootInActiveWindow ?: return
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focused != null && focused.isEditable) {
            val args = android.os.Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
            }
            focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }
        root.recycle()
    }

    private fun scrollScreen(forward: Boolean) {
        val root = rootInActiveWindow ?: return
        fun visit(node: AccessibilityNodeInfo?): Boolean {
            if (node == null) return false
            if (node.isScrollable) {
                val action = if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                if (node.performAction(action)) return true
            }
            for (i in 0 until node.childCount) if (visit(node.getChild(i))) return true
            return false
        }
        visit(root)
        root.recycle()
    }

    private fun clickVisibleText(target: String) {
        val root = rootInActiveWindow ?: return
        val matches = root.findAccessibilityNodeInfosByText(target)
        for (match in matches) {
            val label = match.text?.toString()?.trim().orEmpty()
            if (!label.equals(target, ignoreCase = true)) continue
            var candidate: AccessibilityNodeInfo? = match
            var depth = 0
            while (candidate != null && depth < 6 && !candidate.isClickable) {
                candidate = candidate.parent
                depth++
            }
            if (candidate?.isClickable == true) {
                candidate.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                break
            }
        }
        root.recycle()
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        prefs.edit().putBoolean("accessibility_connected", false).apply()
        try { unregisterReceiver(commandReceiver) } catch (_: Exception) {}
        super.onDestroy()
    }

    companion object {
        const val ACTION_CLICK_TEXT = "com.mtaimaster.ACTION_CLICK_TEXT"
        const val EXTRA_TARGET = "target_text"
        const val EXTRA_ACTION = "action"
        const val EXTRA_VALUE = "value"
    }
}

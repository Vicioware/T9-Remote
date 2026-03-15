package com.example.t9remote

import android.accessibilityservice.AccessibilityService
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class KeyCaptureAccessibilityService : AccessibilityService() {

    companion object {
        var captureCallback: ((Int) -> Unit)? = null
        var isCapturing = false
        private var lastCapturedKeyCode = -1

        private val IGNORED_KEYS = setOf(
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_MUTE,
            KeyEvent.KEYCODE_POWER,
            KeyEvent.KEYCODE_HOME
        )

        fun startCapture(callback: (Int) -> Unit) {
            captureCallback = callback
            isCapturing = true
            lastCapturedKeyCode = -1
        }

        fun stopCapture() {
            isCapturing = false
            captureCallback = null
            lastCapturedKeyCode = -1
        }
    }

    private lateinit var mappingManager: KeyMappingManager

    override fun onCreate() {
        super.onCreate()
        mappingManager = KeyMappingManager(this)
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        // Consume the UP of a key we just captured during mapping
        if (event.action == KeyEvent.ACTION_UP
            && event.keyCode == lastCapturedKeyCode) {
            lastCapturedKeyCode = -1
            return true
        }

        // Essential keys always pass through
        if (event.keyCode in IGNORED_KEYS) {
            return super.onKeyEvent(event)
        }

        val normalized = KeyMappingManager.normalizeKeyCode(event.keyCode)

        // ── Capture mode (key mapping screen) ───────────
        if (isCapturing) {
            if (event.action != KeyEvent.ACTION_DOWN) return true
            lastCapturedKeyCode = event.keyCode
            captureCallback?.invoke(normalized)
            isCapturing = false
            return true
        }

        // ── Normal mode: block mapped keys while typing ─
        val ime = T9InputMethodService.activeInstance
        if (ime != null && T9InputMethodService.isInputActive) {
            val action = mappingManager.getActionForKey(normalized)
            if (action != null) {
                // Forward only the initial DOWN to the IME
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    ime.onKeyDown(normalized, event)
                }
                // Consume DOWN, UP, and repeats to prevent system action
                return true
            }
        }

        return super.onKeyEvent(event)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}
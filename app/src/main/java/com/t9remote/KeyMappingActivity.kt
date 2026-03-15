package com.example.t9remote

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class KeyMappingActivity : AppCompatActivity() {

    companion object {
        private const val GITHUB_URL = "https://github.com/Vicioware/T9-Remote?tab=readme-ov-file#advanced-button-mapping"

        private val PASSTHROUGH_KEYS = setOf(
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_BACK
        )
    }

    private lateinit var mappingManager: KeyMappingManager

    private lateinit var containerToggleCase: LinearLayout
    private lateinit var containerDelete: LinearLayout
    private lateinit var containerChangeLang: LinearLayout

    private lateinit var btnAddToggleCase: Button
    private lateinit var btnAddDelete: Button
    private lateinit var btnAddChangeLang: Button

    private var captureDialog: AlertDialog? = null
    private var captureAction: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_key_mapping)

        mappingManager = KeyMappingManager(this)

        containerToggleCase = findViewById(R.id.container_toggle_case)
        containerDelete = findViewById(R.id.container_delete)
        containerChangeLang = findViewById(R.id.container_change_lang)

        btnAddToggleCase = findViewById(R.id.btn_add_toggle_case)
        btnAddDelete = findViewById(R.id.btn_add_delete)
        btnAddChangeLang = findViewById(R.id.btn_add_change_lang)

        btnAddToggleCase.setOnClickListener {
            startCapture(KeyMappingManager.ACTION_TOGGLE_CASE)
        }
        btnAddDelete.setOnClickListener {
            startCapture(KeyMappingManager.ACTION_DELETE)
        }
        btnAddChangeLang.setOnClickListener {
            startCapture(KeyMappingManager.ACTION_CHANGE_LANG)
        }

        setupAddButtonFocus(btnAddToggleCase)
        setupAddButtonFocus(btnAddDelete)
        setupAddButtonFocus(btnAddChangeLang)

        findViewById<View>(R.id.btn_reset_defaults).setOnClickListener {
            mappingManager.resetToDefaults()
            refreshAll()
            Toast.makeText(this, getString(R.string.defaults_restored),
                Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btn_back).setOnClickListener {
            finish()
        }

        refreshAll()
    }

    override fun onDestroy() {
        KeyCaptureAccessibilityService.stopCapture()
        captureDialog?.dismiss()
        super.onDestroy()
    }

    // ── Accessibility check ─────────────────────────────────

    private fun isAccessibilityServiceEnabled(): Boolean {
        val serviceName = "$packageName/${KeyCaptureAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.split(':').any {
            it.equals(serviceName, ignoreCase = true)
        }
    }

    // ── Capture start ───────────────────────────────────────

    private fun startCapture(action: String) {
        captureAction = action

        if (isAccessibilityServiceEnabled()) {
            startCaptureWithAccessibility(action)
        } else {
            startCaptureWithFallback(action)
        }
    }

    // ── Mode 1: Accessibility Service (full capture) ────────

    private fun startCaptureWithAccessibility(action: String) {
        val actionName = getActionName(action)

        captureDialog = AlertDialog.Builder(this, R.style.Theme_T9Remote_Dialog)
            .setTitle(actionName)
            .setMessage(getString(R.string.capture_prompt))
            .setNegativeButton(getString(R.string.btn_cancel)) { d, _ ->
                KeyCaptureAccessibilityService.stopCapture()
                d.dismiss()
            }
            .setCancelable(false)
            .create()

        KeyCaptureAccessibilityService.startCapture { keyCode ->
            Handler(Looper.getMainLooper()).post {
                onKeyCaptured(keyCode)
            }
        }

        captureDialog?.show()
    }

    // ── Mode 2: Fallback (dispatchKeyEvent) ─────────────────

    private fun startCaptureWithFallback(action: String) {
        val actionName = getActionName(action)

        captureDialog = object : AlertDialog(
            this@KeyMappingActivity, R.style.Theme_T9Remote_Dialog
        ) {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                val keyCode = event.keyCode

                if (keyCode in PASSTHROUGH_KEYS) {
                    return super.dispatchKeyEvent(event)
                }

                if (event.action != KeyEvent.ACTION_DOWN) return true

                val normalized = KeyMappingManager.normalizeKeyCode(keyCode)
                onKeyCaptured(normalized)
                return true
            }
        }.apply {
            setTitle(actionName)
            setMessage(getString(R.string.capture_prompt_fallback))
            setButton(AlertDialog.BUTTON_NEGATIVE,
                getString(R.string.btn_cancel)) { d, _ -> d.dismiss() }
            setButton(AlertDialog.BUTTON_NEUTRAL,
                getString(R.string.btn_visit_guide)) { _, _ ->
                try {
                    startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse(GITHUB_URL)))
                } catch (_: Exception) { }
            }
            setCancelable(false)
        }

        captureDialog?.show()
    }

    // ── Key captured callback ───────────────────────────────

    private fun onKeyCaptured(keyCode: Int) {
        val action = captureAction ?: return

        if (mappingManager.isReservedKey(keyCode)) {
            Toast.makeText(this, getString(R.string.key_reserved),
                Toast.LENGTH_SHORT).show()

            if (isAccessibilityServiceEnabled()) {
                KeyCaptureAccessibilityService.startCapture { newCode ->
                    Handler(Looper.getMainLooper()).post {
                        onKeyCaptured(newCode)
                    }
                }
            }
            return
        }

        mappingManager.addKeyForAction(action, keyCode)
        captureDialog?.dismiss()
        captureDialog = null
        captureAction = null
        refreshAll()
        getAddButtonForAction(action).requestFocus()
    }

    // ── UI helpers ──────────────────────────────────────────

    private fun getActionName(action: String): String = when (action) {
        KeyMappingManager.ACTION_TOGGLE_CASE ->
            getString(R.string.action_toggle_case)
        KeyMappingManager.ACTION_DELETE -> getString(R.string.action_delete)
        KeyMappingManager.ACTION_CHANGE_LANG ->
            getString(R.string.action_change_lang)
        else -> ""
    }

    private fun setupAddButtonFocus(btn: Button) {
        btn.setOnFocusChangeListener { v, hasFocus ->
            val button = v as Button
            val color = if (hasFocus) Color.WHITE else
                Color.parseColor("#BBBBDD")
            button.setTextColor(color)
            button.compoundDrawablesRelative.forEach { drawable ->
                drawable?.mutate()?.setTint(color)
            }
        }
    }

    private fun refreshAll() {
        refreshSection(containerToggleCase,
            KeyMappingManager.ACTION_TOGGLE_CASE)
        refreshSection(containerDelete, KeyMappingManager.ACTION_DELETE)
        refreshSection(containerChangeLang,
            KeyMappingManager.ACTION_CHANGE_LANG)
    }

    private fun refreshSection(container: LinearLayout, action: String) {
        container.removeAllViews()
        val keys = mappingManager.getKeysForAction(action).sorted()

        if (keys.isEmpty()) {
            val tv = TextView(this).apply {
                text = getString(R.string.no_keys_mapped)
                setTextColor(Color.parseColor("#444466"))
                textSize = 13f
                setPadding(0, dpToPx(4), 0, dpToPx(4))
            }
            container.addView(tv)
        } else {
            for ((index, keyCode) in keys.withIndex()) {
                val keyName = formatKeyName(keyCode)
                val chip = createChip(keyName, keyCode, action, container, index)
                container.addView(chip)
            }
        }
    }

    private fun createChip(
        label: String,
        keyCode: Int,
        action: String,
        container: LinearLayout,
        index: Int
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            background = resources.getDrawable(R.drawable.selector_key_chip, theme)
            isFocusable = true
            setPadding(dpToPx(12), dpToPx(7), dpToPx(10), dpToPx(7))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(6)
            }

            setOnClickListener {
                mappingManager.removeKeyForAction(action, keyCode)
                val remainingKeys =
                    mappingManager.getKeysForAction(action).sorted()
                refreshAll()

                container.post {
                    if (remainingKeys.isEmpty()) {
                        getAddButtonForAction(action).requestFocus()
                    } else {
                        val focusIndex = minOf(index, container.childCount - 1)
                        if (focusIndex in 0 until container.childCount) {
                            container.getChildAt(focusIndex).requestFocus()
                        } else {
                            getAddButtonForAction(action).requestFocus()
                        }
                    }
                }
            }

            val tvLabel = TextView(this@KeyMappingActivity).apply {
                text = label
                textSize = 12f
                setTextColor(Color.parseColor("#CCCCDD"))
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val ivClose = ImageView(this@KeyMappingActivity).apply {
                setImageResource(R.drawable.ic_close)
                layoutParams = LinearLayout.LayoutParams(
                    dpToPx(14), dpToPx(14)
                ).apply {
                    marginStart = dpToPx(6)
                }
            }

            addView(tvLabel)
            addView(ivClose)
        }
    }

    private fun getAddButtonForAction(action: String): View {
        return when (action) {
            KeyMappingManager.ACTION_TOGGLE_CASE -> btnAddToggleCase
            KeyMappingManager.ACTION_DELETE -> btnAddDelete
            KeyMappingManager.ACTION_CHANGE_LANG -> btnAddChangeLang
            else -> btnAddToggleCase
        }
    }

    private fun formatKeyName(keyCode: Int): String {
        return KeyEvent.keyCodeToString(keyCode)
            .removePrefix("KEYCODE_")
            .replace("_", " ")
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()
}
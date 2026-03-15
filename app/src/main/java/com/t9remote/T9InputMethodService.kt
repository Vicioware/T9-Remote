package com.example.t9remote

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

class T9InputMethodService : InputMethodService() {

    companion object {
        private const val AUTO_COMMIT_MS = 1300L
        private const val LONG_PRESS_REPEAT = 1

        var activeInstance: T9InputMethodService? = null
        var isInputActive = false

        private val T9_MAP_EN = mapOf(
            KeyEvent.KEYCODE_1 to charArrayOf('.', ',', '!', '?', '-', '\'', '1'),
            KeyEvent.KEYCODE_2 to charArrayOf('a', 'b', 'c', '2'),
            KeyEvent.KEYCODE_3 to charArrayOf('d', 'e', 'f', '3'),
            KeyEvent.KEYCODE_4 to charArrayOf('g', 'h', 'i', '4'),
            KeyEvent.KEYCODE_5 to charArrayOf('j', 'k', 'l', '5'),
            KeyEvent.KEYCODE_6 to charArrayOf('m', 'n', 'o', '6'),
            KeyEvent.KEYCODE_7 to charArrayOf('p', 'q', 'r', 's', '7'),
            KeyEvent.KEYCODE_8 to charArrayOf('t', 'u', 'v', '8'),
            KeyEvent.KEYCODE_9 to charArrayOf('w', 'x', 'y', 'z', '9'),
            KeyEvent.KEYCODE_0 to charArrayOf(' ', '0'),
        )

        private val T9_MAP_ES = mapOf(
            KeyEvent.KEYCODE_1 to charArrayOf('.', ',', '!', '?', '-', '\'', '¿', '¡', '1'),
            KeyEvent.KEYCODE_2 to charArrayOf('a', 'b', 'c', '2'),
            KeyEvent.KEYCODE_3 to charArrayOf('d', 'e', 'f', '3'),
            KeyEvent.KEYCODE_4 to charArrayOf('g', 'h', 'i', '4'),
            KeyEvent.KEYCODE_5 to charArrayOf('j', 'k', 'l', '5'),
            KeyEvent.KEYCODE_6 to charArrayOf('m', 'n', 'ñ', 'o', '6'),
            KeyEvent.KEYCODE_7 to charArrayOf('p', 'q', 'r', 's', '7'),
            KeyEvent.KEYCODE_8 to charArrayOf('t', 'u', 'v', '8'),
            KeyEvent.KEYCODE_9 to charArrayOf('w', 'x', 'y', 'z', '9'),
            KeyEvent.KEYCODE_0 to charArrayOf(' ', '0'),
        )

        private val ACCENT_MAP = mapOf(
            'a' to 'á', 'e' to 'é', 'i' to 'í', 'o' to 'ó', 'u' to 'ú',
            'A' to 'Á', 'E' to 'É', 'I' to 'Í', 'O' to 'Ó', 'U' to 'Ú'
        )
    }

    private var activeKey = -1
    private var charIndex = 0
    private var hasPending = false
    private var upperCase = false
    private var isSpanish = false
    private var accentApplied = false

    private lateinit var mappingManager: KeyMappingManager
    private lateinit var themeManager: CandidateThemeManager

    private val handler = Handler(Looper.getMainLooper())
    private val autoCommit = Runnable { commitPending() }

    private var candidatesBarView: View? = null
    private var tvMode: TextView? = null
    private var charContainer: LinearLayout? = null

    private fun currentT9Map() = if (isSpanish) T9_MAP_ES else T9_MAP_EN

    override fun onCreate() {
        super.onCreate()
        activeInstance = this
        isSpanish = Locale.getDefault().language == "es"
        mappingManager = KeyMappingManager(this)
        themeManager = CandidateThemeManager(this)
    }

    override fun onStartInput(info: EditorInfo?, restarting: Boolean) {
        super.onStartInput(info, restarting)
        isInputActive = true
        resetState()
        setCandidatesViewShown(true)
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        applyThemeToBar()
        setCandidatesViewShown(true)
        updateModeLabel()
        showHint()
    }

    override fun onBindInput() {
        super.onBindInput()
        setCandidatesViewShown(true)
    }

    override fun onWindowShown() {
        super.onWindowShown()
        setCandidatesViewShown(true)
        applyThemeToBar()
        updateModeLabel()
    }

    override fun onStartCandidatesView(info: EditorInfo?, restarting: Boolean) {
        super.onStartCandidatesView(info, restarting)
        setCandidatesViewShown(true)
    }

    override fun onFinishInput() {
        isInputActive = false
        commitPending()
        resetState()
        super.onFinishInput()
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onEvaluateInputViewShown(): Boolean {
        super.onEvaluateInputViewShown()
        return true
    }

    override fun onCreateCandidatesView(): View {
        val view = LayoutInflater.from(this).inflate(R.layout.view_candidates, null)
        candidatesBarView = view
        tvMode = view.findViewById(R.id.tv_mode)
        charContainer = view.findViewById(R.id.char_container)
        applyThemeToBar()
        updateModeLabel()
        return view
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val key = normalizeNumpad(keyCode)
        val t9Map = currentT9Map()

        if (t9Map.containsKey(key)) {
            val repeatCount = event?.repeatCount ?: 0
            if (repeatCount == 0) {
                accentApplied = false
                onNumberPressed(key)
            } else if (repeatCount >= LONG_PRESS_REPEAT && isSpanish &&
                !accentApplied && hasPending) {
                applyAccent()
            }
            return true
        }

        val mappedAction = mappingManager.getActionForKey(key)
        if (mappedAction != null) {
            when (mappedAction) {
                KeyMappingManager.ACTION_DELETE -> { onDelete(); return true }
                KeyMappingManager.ACTION_TOGGLE_CASE -> { toggleCase(); return true }
                KeyMappingManager.ACTION_CHANGE_LANG -> { toggleLanguage(); return true }
            }
        }

        return when (key) {
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {
                if (hasPending) {
                    commitPending(); true
                } else {
                    val imeAction = currentInputEditorInfo?.imeOptions
                        ?.and(EditorInfo.IME_MASK_ACTION)
                        ?: EditorInfo.IME_ACTION_UNSPECIFIED
                    currentInputConnection?.performEditorAction(imeAction)
                    true
                }
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    // ── T9 logic ────────────────────────────────────────────

    private fun onNumberPressed(keyCode: Int) {
        val chars = currentT9Map()[keyCode] ?: return
        handler.removeCallbacks(autoCommit)

        val ic = currentInputConnection ?: return
        ic.beginBatchEdit()

        if (keyCode == activeKey && hasPending) {
            charIndex = (charIndex + 1) % chars.size
        } else {
            if (hasPending) ic.finishComposingText()
            activeKey = keyCode
            charIndex = 0
        }

        hasPending = true
        ic.setComposingText(applyCase(chars[charIndex]).toString(), 1)
        ic.endBatchEdit()

        updateCandidatesBar(chars)
        handler.postDelayed(autoCommit, AUTO_COMMIT_MS)
    }

    private fun applyAccent() {
        val chars = currentT9Map()[activeKey] ?: return
        for (i in chars.indices) {
            val c = applyCase(chars[i])
            val accented = ACCENT_MAP[c] ?: continue

            accentApplied = true
            charIndex = i
            handler.removeCallbacks(autoCommit)

            val ic = currentInputConnection ?: return
            ic.beginBatchEdit()
            ic.setComposingText(accented.toString(), 1)
            ic.endBatchEdit()

            updateCandidatesBarAccent(accented)
            handler.postDelayed(autoCommit, AUTO_COMMIT_MS)
            return
        }
    }

    private fun commitPending() {
        handler.removeCallbacks(autoCommit)
        if (hasPending) {
            currentInputConnection?.finishComposingText()
            hasPending = false
            activeKey = -1
            charIndex = 0
            accentApplied = false
            showHint()
        }
    }

    private fun onDelete() {
        handler.removeCallbacks(autoCommit)
        if (hasPending) {
            val ic = currentInputConnection ?: return
            ic.beginBatchEdit()
            ic.setComposingText("", 0)
            ic.finishComposingText()
            ic.endBatchEdit()
            hasPending = false
            activeKey = -1
            charIndex = 0
            accentApplied = false
            showHint()
        } else {
            currentInputConnection?.deleteSurroundingText(1, 0)
        }
    }

    private fun toggleCase() {
        if (hasPending) commitPending()
        upperCase = !upperCase
        updateModeLabel()
    }

    private fun toggleLanguage() {
        if (hasPending) commitPending()
        isSpanish = !isSpanish
        updateModeLabel()
        showHint()
    }

    // ── Theme ───────────────────────────────────────────────

    private fun getCurrentTheme(): CandidateTheme = themeManager.getSelectedTheme()

    private fun applyThemeToBar() {
        val theme = getCurrentTheme()
        candidatesBarView?.setBackgroundColor(Color.parseColor(theme.barBg))
        tvMode?.setTextColor(Color.parseColor(theme.modeLabelText))
    }

    // ── UI updates ──────────────────────────────────────────

    private fun updateCandidatesBar(chars: CharArray) {
        val container = charContainer ?: return
        container.removeAllViews()
        applyThemeToBar()

        val theme = getCurrentTheme()
        val cornerPx = dpToPx(6).toFloat()
        val selCorner = dpToPx(4).toFloat()
        val cellSize = dpToPx(28)

        container.background = GradientDrawable().apply {
            setColor(Color.parseColor(theme.containerBg))
            cornerRadius = cornerPx
            setStroke(dpToPx(1), Color.parseColor(theme.containerBorder))
        }
        container.setPadding(dpToPx(6), dpToPx(4), dpToPx(6), dpToPx(4))

        for (i in chars.indices) {
            val c = applyCase(chars[i])
            val display = if (c == ' ') "␣" else c.toString()
            val sel = (i == charIndex)

            val tv = TextView(this).apply {
                text = display
                textSize = 17f
                gravity = Gravity.CENTER
                includeFontPadding = false
                layoutParams = LinearLayout.LayoutParams(cellSize, cellSize).apply {
                    marginEnd = if (i < chars.size - 1) dpToPx(1) else 0
                }
                if (sel) {
                    setTextColor(Color.parseColor(theme.selectedText))
                    setTypeface(null, Typeface.BOLD)
                    background = GradientDrawable().apply {
                        setColor(Color.parseColor(theme.selectedBg))
                        cornerRadius = selCorner
                    }
                } else {
                    setTextColor(Color.parseColor(theme.normalText))
                    setTypeface(null, Typeface.NORMAL)
                }
            }
            container.addView(tv)
        }
    }

    private fun updateCandidatesBarAccent(accentedChar: Char) {
        val container = charContainer ?: return
        container.removeAllViews()
        applyThemeToBar()

        val theme = getCurrentTheme()
        val cornerPx = dpToPx(6).toFloat()
        val selCorner = dpToPx(4).toFloat()
        val cellSize = dpToPx(28)

        container.background = GradientDrawable().apply {
            setColor(Color.parseColor(theme.containerBg))
            cornerRadius = cornerPx
            setStroke(dpToPx(1), Color.parseColor(theme.containerBorder))
        }
        container.setPadding(dpToPx(6), dpToPx(4), dpToPx(6), dpToPx(4))

        val tv = TextView(this).apply {
            text = accentedChar.toString()
            textSize = 17f
            gravity = Gravity.CENTER
            includeFontPadding = false
            layoutParams = LinearLayout.LayoutParams(cellSize, cellSize)
            setTextColor(Color.parseColor(theme.selectedText))
            setTypeface(null, Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(Color.parseColor(theme.selectedBg))
                cornerRadius = selCorner
            }
        }
        container.addView(tv)
    }

    private fun showHint() {
        charContainer?.removeAllViews()
        charContainer?.background = null
        charContainer?.setPadding(0, 0, 0, 0)
    }

    private fun updateModeLabel() {
        val lang = if (isSpanish) "ES" else "EN"
        val caseText = if (upperCase) "ABC" else "abc"
        tvMode?.text = "$lang · $caseText"
    }

    // ── Helpers ─────────────────────────────────────────────

    private fun applyCase(c: Char): Char =
        if (upperCase && c.isLetter()) c.uppercaseChar() else c

    private fun dpToPx(dp: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(),
            resources.displayMetrics
        ).toInt()

    private fun normalizeNumpad(keyCode: Int): Int =
        KeyMappingManager.normalizeKeyCode(keyCode)

    private fun resetState() {
        handler.removeCallbacks(autoCommit)
        hasPending = false
        activeKey = -1
        charIndex = 0
        upperCase = false
        accentApplied = false
        showHint()
        updateModeLabel()
    }

    override fun onDestroy() {
        activeInstance = null
        isInputActive = false
        super.onDestroy()
    }
}
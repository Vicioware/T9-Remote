package com.example.t9remote

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class ThemePickerActivity : AppCompatActivity() {

    private lateinit var themeManager: CandidateThemeManager
    private lateinit var previewBar: FrameLayout
    private lateinit var previewModeLabel: TextView
    private lateinit var previewContainer: LinearLayout
    private lateinit var themeListContainer: LinearLayout

    private val sampleChars = charArrayOf('a', 'b', 'c', '2')
    private val sampleSelected = 1

    private var selectedThemeId = ""
    private val themeItems = mutableMapOf<String, View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_theme_picker)

        themeManager = CandidateThemeManager(this)
        selectedThemeId = themeManager.getSelectedThemeId()

        previewBar = findViewById(R.id.preview_bar)
        previewModeLabel = findViewById(R.id.preview_mode_label)
        previewContainer = findViewById(R.id.preview_container)
        themeListContainer = findViewById(R.id.theme_list)

        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }

        buildThemeList()
        updatePreview(CandidateThemeManager.getThemeById(selectedThemeId))

        themeListContainer.post {
            themeItems[selectedThemeId]?.requestFocus()
        }
    }

    private fun buildThemeList() {
        for (theme in CandidateThemeManager.THEMES) {
            val item = createThemeItem(theme)
            themeItems[theme.id] = item
            themeListContainer.addView(item)
        }
    }

    private fun createThemeItem(theme: CandidateTheme): View {
        val isSpanish = Locale.getDefault().language == "es"
        val displayName = if (isSpanish) theme.nameEs else theme.name

        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isFocusable = true
            isClickable = true
            background = resources.getDrawable(
                R.drawable.selector_theme_item, this@ThemePickerActivity.theme)
            setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dpToPx(6) }

            // Color swatch
            val swatch = View(this@ThemePickerActivity).apply {
                layoutParams = LinearLayout.LayoutParams(
                    dpToPx(32), dpToPx(20)
                ).apply { marginEnd = dpToPx(14) }
                background = GradientDrawable().apply {
                    cornerRadius = dpToPx(4).toFloat()
                    setColor(Color.parseColor(theme.swatchColor))
                    if (theme.id == "light") {
                        setStroke(dpToPx(1), Color.parseColor("#AAAAAA"))
                    }
                }
            }

            // Theme name
            val tvName = TextView(this@ThemePickerActivity).apply {
                text = displayName
                textSize = 15f
                setTextColor(Color.parseColor("#DDDDEE"))
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            // Checkmark
            val ivCheck = ImageView(this@ThemePickerActivity).apply {
                setImageResource(R.drawable.ic_check_input)
                layoutParams = LinearLayout.LayoutParams(dpToPx(20), dpToPx(20))
                visibility = if (theme.id == selectedThemeId)
                    View.VISIBLE else View.INVISIBLE
            }
            tag = ivCheck

            addView(swatch)
            addView(tvName)
            addView(ivCheck)

            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) updatePreview(theme)
            }

            setOnClickListener { selectTheme(theme.id) }
        }
    }

    private fun selectTheme(themeId: String) {
        selectedThemeId = themeId
        themeManager.setSelectedTheme(themeId)
        for ((id, item) in themeItems) {
            (item.tag as? ImageView)?.visibility =
                if (id == themeId) View.VISIBLE else View.INVISIBLE
        }
    }

    private fun updatePreview(theme: CandidateTheme) {
        previewBar.background = GradientDrawable().apply {
            setColor(Color.parseColor(theme.barBg))
            cornerRadius = dpToPx(10).toFloat()
        }
        previewModeLabel.setTextColor(Color.parseColor(theme.modeLabelText))

        previewContainer.removeAllViews()
        val corner = dpToPx(6).toFloat()
        val selCorner = dpToPx(4).toFloat()
        val cell = dpToPx(28)

        previewContainer.background = GradientDrawable().apply {
            setColor(Color.parseColor(theme.containerBg))
            cornerRadius = corner
            setStroke(dpToPx(1), Color.parseColor(theme.containerBorder))
        }
        previewContainer.setPadding(dpToPx(6), dpToPx(4), dpToPx(6), dpToPx(4))

        for (i in sampleChars.indices) {
            val sel = (i == sampleSelected)
            val tv = TextView(this).apply {
                text = sampleChars[i].toString()
                textSize = 17f
                gravity = Gravity.CENTER
                includeFontPadding = false
                layoutParams = LinearLayout.LayoutParams(cell, cell).apply {
                    marginEnd = if (i < sampleChars.size - 1) dpToPx(1) else 0
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
            previewContainer.addView(tv)
        }
    }

    private fun dpToPx(dp: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(),
            resources.displayMetrics
        ).toInt()
}
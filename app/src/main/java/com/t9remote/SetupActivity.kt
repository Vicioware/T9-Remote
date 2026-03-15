package com.example.t9remote

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class SetupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        findViewById<View>(R.id.btn_enable_select).setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }

        findViewById<View>(R.id.btn_key_mapping).setOnClickListener {
            startActivity(Intent(this, KeyMappingActivity::class.java))
        }

        findViewById<View>(R.id.btn_theme_picker).setOnClickListener {
            startActivity(Intent(this, ThemePickerActivity::class.java))
        }

        val etTest = findViewById<EditText>(R.id.et_test)
        etTest.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                v.post {
                    imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT)
                }
            }
        }

        findViewById<View>(R.id.btn_enable_select).requestFocus()
    }
}
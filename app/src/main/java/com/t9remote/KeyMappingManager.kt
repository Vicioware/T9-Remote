package com.example.t9remote

import android.content.Context
import android.view.KeyEvent

class KeyMappingManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "t9_key_mappings"

        const val ACTION_TOGGLE_CASE = "action_toggle_case"
        const val ACTION_DELETE = "action_delete"
        const val ACTION_CHANGE_LANG = "action_change_language"

        val ALL_ACTIONS = listOf(ACTION_TOGGLE_CASE, ACTION_DELETE, ACTION_CHANGE_LANG)

        val DEFAULT_MAPPINGS = mapOf(
            ACTION_TOGGLE_CASE to setOf(
                KeyEvent.KEYCODE_CHANNEL_UP
            ),
            ACTION_DELETE to setOf(
                KeyEvent.KEYCODE_CHANNEL_DOWN
            ),
            ACTION_CHANGE_LANG to setOf(
                KeyEvent.KEYCODE_MENU,
                KeyEvent.KEYCODE_PROG_RED
            )
        )

        private val RESERVED_KEYS = setOf(
            KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2,
            KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_5,
            KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_8,
            KeyEvent.KEYCODE_9,
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_BACK
        )

        fun normalizeKeyCode(keyCode: Int): Int = when (keyCode) {
            KeyEvent.KEYCODE_NUMPAD_0 -> KeyEvent.KEYCODE_0
            KeyEvent.KEYCODE_NUMPAD_1 -> KeyEvent.KEYCODE_1
            KeyEvent.KEYCODE_NUMPAD_2 -> KeyEvent.KEYCODE_2
            KeyEvent.KEYCODE_NUMPAD_3 -> KeyEvent.KEYCODE_3
            KeyEvent.KEYCODE_NUMPAD_4 -> KeyEvent.KEYCODE_4
            KeyEvent.KEYCODE_NUMPAD_5 -> KeyEvent.KEYCODE_5
            KeyEvent.KEYCODE_NUMPAD_6 -> KeyEvent.KEYCODE_6
            KeyEvent.KEYCODE_NUMPAD_7 -> KeyEvent.KEYCODE_7
            KeyEvent.KEYCODE_NUMPAD_8 -> KeyEvent.KEYCODE_8
            KeyEvent.KEYCODE_NUMPAD_9 -> KeyEvent.KEYCODE_9
            KeyEvent.KEYCODE_NUMPAD_MULTIPLY -> KeyEvent.KEYCODE_STAR
            else -> keyCode
        }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getKeysForAction(action: String): Set<Int> {
        val stored = prefs.getStringSet(action, null)
        if (stored != null) {
            return stored.mapNotNull { it.toIntOrNull() }.toSet()
        }
        return DEFAULT_MAPPINGS[action] ?: emptySet()
    }

    fun getActionForKey(keyCode: Int): String? {
        for (action in ALL_ACTIONS) {
            if (keyCode in getKeysForAction(action)) {
                return action
            }
        }
        return null
    }

    fun addKeyForAction(action: String, keyCode: Int) {
        for (other in ALL_ACTIONS) {
            if (other != action && keyCode in getKeysForAction(other)) {
                removeKeyForAction(other, keyCode)
            }
        }
        val current = getKeysForAction(action).toMutableSet()
        current.add(keyCode)
        saveKeys(action, current)
    }

    fun removeKeyForAction(action: String, keyCode: Int) {
        val current = getKeysForAction(action).toMutableSet()
        current.remove(keyCode)
        saveKeys(action, current)
    }

    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }

    fun isReservedKey(keyCode: Int): Boolean = keyCode in RESERVED_KEYS

    private fun saveKeys(action: String, keys: Set<Int>) {
        prefs.edit().putStringSet(action, keys.map { it.toString() }.toSet()).apply()
    }
}
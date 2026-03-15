package com.example.t9remote

import android.content.Context

data class CandidateTheme(
    val id: String,
    val name: String,
    val nameEs: String,
    val swatchColor: String,
    val barBg: String,
    val modeLabelText: String,
    val containerBg: String,
    val containerBorder: String,
    val selectedBg: String,
    val selectedText: String,
    val normalText: String
)

class CandidateThemeManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "t9_theme_prefs"
        private const val KEY_SELECTED = "selected_theme"
        private const val DEFAULT_THEME = "midnight"

        val THEMES = listOf(
            // Midnight — azul frío oscuro
            CandidateTheme("midnight", "Midnight", "Medianoche",
                "#0078D4",
                "#F01C1C30", "#60CDFF", "#252540", "#353555",
                "#0078D4", "#FFFFFF", "#7878A0"),

            // Light — claro tipo Windows 11
            CandidateTheme("light", "Light", "Claro",
                "#FFFFFF",
                "#F0E8ECF4", "#005A9E", "#FFFFFF", "#BFC8D4",
                "#0078D4", "#FFFFFF", "#5A6070"),

            // Ocean — teal + coral (complementarios)
            CandidateTheme("ocean", "Ocean", "Océano",
                "#FF6E40",
                "#F00A2233", "#FF8A65", "#0E3040", "#1A4A58",
                "#FF6E40", "#FFFFFF", "#4D99A8"),

            // Sunset — púrpura + ámbar (split complementario)
            CandidateTheme("sunset", "Sunset", "Atardecer",
                "#FFB300",
                "#F02A1528", "#FFD54F", "#352030", "#504050",
                "#FFB300", "#1A1018", "#A07888"),

            // Forest — verde + cobre (compuestos)
            CandidateTheme("forest", "Forest", "Bosque",
                "#66BB6A",
                "#F0121F14", "#FFB74D", "#1A3020", "#2A4A2E",
                "#43A047", "#FFFFFF", "#5E8A62"),

            // Violet — púrpura + chartreuse (complementarios)
            CandidateTheme("violet", "Violet", "Violeta",
                "#B388FF",
                "#F01A1035", "#EEFF41", "#251A45", "#382A58",
                "#AA00FF", "#FFFFFF", "#8070A8")
        )

        fun getThemeById(id: String): CandidateTheme =
            THEMES.find { it.id == id } ?: THEMES[0]
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSelectedThemeId(): String =
        prefs.getString(KEY_SELECTED, DEFAULT_THEME) ?: DEFAULT_THEME

    fun getSelectedTheme(): CandidateTheme = getThemeById(getSelectedThemeId())

    fun setSelectedTheme(id: String) {
        prefs.edit().putString(KEY_SELECTED, id).apply()
    }
}
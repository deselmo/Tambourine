package com.deselmo.android.tambourine

import android.content.Context
import android.preference.PreferenceManager

class ThemeChange {
    companion object {
        fun apply(context: Context) {
            when(PreferenceManager.getDefaultSharedPreferences(context)
                    .getString("theme", "light")) {
                "light" -> context.setTheme(R.style.AppThemeLight)
                "dark" -> context.setTheme(R.style.AppThemeDark)
            }
        }
    }
}
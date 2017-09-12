package com.deselmo.android.tambourine

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceFragment
import android.preference.EditTextPreference
import android.preference.MultiSelectListPreference
import android.preference.ListPreference
import android.preference.Preference
import android.preference.PreferenceGroup
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.content.Intent
import android.support.v4.app.TaskStackBuilder

import kotlinx.android.synthetic.main.activity_settings.*


class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeChange.apply(this)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }


    class SettingsFragment : PreferenceFragment(),
            SharedPreferences.OnSharedPreferenceChangeListener {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.preferences)
            PreferenceManager.setDefaultValues(activity.applicationContext, R.xml.preferences,
                    false)
            initSummary(preferenceScreen)
        }

        override fun onResume() {
            super.onResume()

            preferenceScreen.sharedPreferences
                    .registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            super.onPause()

            preferenceScreen.sharedPreferences
                    .unregisterOnSharedPreferenceChangeListener(this)
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
            updatePrefSummary(findPreference(key))

            when(key) {
                "theme" -> {
                    ThemeChange.apply(activity.applicationContext)
                    TaskStackBuilder.create(activity)
                            .addNextIntent(Intent(activity, TambourineActivity::class.java))
                            .addNextIntent(activity.intent)
                            .startActivities()
                }
                else -> initSummary(preferenceScreen)
            }
        }

        private fun initSummary(preference: Preference) {
            if (preference is PreferenceGroup) {
                for (i in 0 until preference.preferenceCount) {
                    initSummary(preference.getPreference(i))
                }
            } else {
                updatePrefSummary(preference)
            }
        }

        private fun updatePrefSummary(preference: Preference) {
            (preference as? ListPreference)?.summary = (preference as ListPreference).entry

            when(preference) {
                is EditTextPreference ->
                    preference.setSummary(preference.text)

                is MultiSelectListPreference ->
                    preference.setSummary((preference as EditTextPreference).text)
            }
        }
    }
}
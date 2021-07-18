package com.krostiffer.krostrack.ui

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.LocaleList
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.*
import com.krostiffer.krostrack.MainActivity
import com.krostiffer.krostrack.R
import java.util.*

class SettingsActivity : AppCompatActivity() {

    //Standard Settings Activity onCreate
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
    }


    //Einstellungsfenster
    class SettingsFragment : PreferenceFragmentCompat() {

        private val PREFS_FILENAME = "com.krostiffer.krostrack.prefs"
        var mSharedPreference: SharedPreferences? = null
        //Einstellungsänderungen werden in die sharedPreferences übernommen
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            mSharedPreference = activity?.getSharedPreferences(PREFS_FILENAME, 0)
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            val timeBehind: EditTextPreference? = findPreference("TimeBehind")
            timeBehind!!.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, newValue ->
                    changePreference(preference, newValue)
                    true
                }
            val distBehind: EditTextPreference? = findPreference("DistBehind")
            distBehind!!.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, newValue ->
                    changePreference(preference, newValue)
                    true
                }
            val language: ListPreference? = findPreference("language")
            language!!.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, newValue ->
                    changePref(preference.key, newValue.toString())
                    ChangeCurrentLanguage(newValue.toString())
                    true
                }
        }

        //Ändert die Sprache, die verwendet wird
        private fun ChangeCurrentLanguage(lang: String) {
            var locale: Locale = Locale.getDefault()
            if (lang != "phone") {
                locale = Locale(lang)
            }

            val res = resources
            val conf = res.configuration
            conf.setLocales(LocaleList(locale))
            res.updateConfiguration(conf, resources.displayMetrics)
            val refresh = Intent(
                this.context,
                MainActivity::class.java
            )
            startActivity(refresh)
        }

        //Ändert die sharedPreferences der Float Werte, gibt eine Nachricht aus, wenn ein Falscher Wert eingegeben wurde
        private fun changePreference(
            preference: Preference,
            newValue: Any,
        ) {
            try {
                changePref(preference.key, newValue.toString().replace(",", ".").toFloat())
            } catch (e: Exception) {
                Toast.makeText(this.context,
                    getString(R.string.choose_valid_number),
                    Toast.LENGTH_LONG).show()
            }
        }

        //Ändert die sharedPreference side auf den Float Wert value. Wird nur mit vorigem Check verwendet, dass value ein valider float ist
        fun changePref(side:String, value: Float) {
            val editor = mSharedPreference?.edit()
            if (editor != null) {
                editor.putFloat(side, value)
                editor.apply()
            }

        }

        //Ändert die sharedPreference side auf den String Wert value.
        fun changePref(side:String, value: String) {
            val editor = mSharedPreference?.edit()
            if (editor != null) {
                editor.putString(side, value)
                editor.apply()
            }

        }

    }
}
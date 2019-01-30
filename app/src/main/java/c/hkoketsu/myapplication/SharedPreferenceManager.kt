package c.hkoketsu.myapplication

import android.content.Context
import android.preference.PreferenceManager

/**
 * Created by Hiro on 2019/01/24.
 */
class SharedPreferenceManager(c: Context) {
    private val sp = PreferenceManager.getDefaultSharedPreferences(c)
    private val editor = sp.edit()

    private val appPreferenceKey = "defaultApp"
    private val intervalPreferenceKey = "interval"

    fun commit() {
        editor.commit()
    }

    fun putAppPreference(appName : String) {
        editor.putString(appPreferenceKey, appName)
        commit()
    }

    fun putIntervalPreference(interval : Int) {
        editor.putInt(intervalPreferenceKey, interval)
        commit()
    }

    fun getAppPreference() : String {
        return sp.getString(appPreferenceKey, "twitter")
    }

    fun getIntervalPreference() : Int {
        return sp.getInt(intervalPreferenceKey, 1)
    }
}
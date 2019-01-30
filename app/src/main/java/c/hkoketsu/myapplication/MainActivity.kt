package c.hkoketsu.myapplication

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


class MainActivity : AppCompatActivity() {

    private var permissionEnabled = false
    private var app = ""
    private var interval = 0

    private lateinit var appButtons : Array<ImageButton>

    private lateinit var usageStatsManager : UsageStatsManager
    private lateinit var spManager : SharedPreferenceManager

    private enum class Day(val v:Int) {
        Today(0),
        Yesterday(1)
    }

    private enum class App(val v:String) {
        Twitter("twitter"),
        Facebook("facebook"),
        Instagram("instagram"),
        YouTube("youtube")
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeSetting()

        if (checkForPermission()) {
            permissionEnabled = true
        } else {
            requestPermission()
        }
    }

    private fun initializeSetting() {
        appButtons = arrayOf(buttonTwitter, buttonFacebook, buttonInstagram, buttonYoutube)
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        spManager = SharedPreferenceManager(this@MainActivity)

        app  = spManager.getAppPreference()
        interval = spManager.getIntervalPreference()

        grayOutAppButtons()
    }

    override fun onStart() {
        super.onStart()
        if (permissionEnabled) changeDisplay()
    }

    fun switchApp(view : View) {
        app = when (view.id) {
            R.id.buttonTwitter   -> App.Twitter.v
            R.id.buttonFacebook  -> App.Facebook.v
            R.id.buttonInstagram -> App.Instagram.v
            R.id.buttonYoutube   -> App.YouTube.v
            else                 -> App.Twitter.v
        }

        grayOutAppButtons()
        spManager.putAppPreference(app)
        changeDisplay()
    }

    private fun grayOutAppButtons() {
        val notGray = when (app) {
            "twitter"   -> 0
            "facebook"  -> 1
            "instagram" -> 2
            "youtube"   -> 3
            else        -> 0
        }

        for (i in 0..appButtons.size - 1) {
            if   (i != notGray) appButtons[i].setColorFilter(R.color.gray)
            else                appButtons[i].setColorFilter(null)
        }
    }

    fun switchInterval(view : View) {
        if (view.id == R.id.buttonToday) {
            interval = Day.Today.v
        }
        else if (view.id == R.id.buttonYesterday) {
            interval = Day.Yesterday.v
        }

        spManager.putIntervalPreference(interval)
        changeDisplay()
    }

    private fun changeDisplay() {
        setUnitText()
        setStat()
    }


    private fun setUnitText()
    {
        var text:String
        val capAppName = app.substring(0,1).toUpperCase() + app.substring(1)

        if (Locale.getDefault().equals(Locale.JAPAN)) {
            text = if (interval == Day.Today.v) "本日の" else "昨日の"
            text += capAppName + "使用時間"
        }
        else {
            text =  "Time spent on $capAppName"
            text += if (interval== Day.Today.v) " today" else " yesterday"
        }
        unitText.setText(text)
    }


    private fun setStat()
    {
        val stats = getUsageStats()
        var usageInSeconds = 0L
        var found = false

        for (stat in stats)
        {
            if (stat.packageName.contains(app)) {
                found = true
                usageInSeconds = stat.totalTimeInForeground / 1000
                val sec:String = if (usageInSeconds % 60 < 10) "0" + (usageInSeconds % 60) else (usageInSeconds % 60).toString()
                val min:String = if (usageInSeconds / 60 % 60 < 10) "0" + (usageInSeconds / 60 % 60) else (usageInSeconds / 60 % 60).toString()
                val hour:String = if (usageInSeconds / 3600 < 10) "0" + (usageInSeconds / 3600) else (usageInSeconds / 3600).toString()
                val displayText = "$hour : $min : $sec"
                usageText.text = displayText
                break
            }
        }

        val comment =
                if (interval == Day.Today.v) when (usageInSeconds / 60) {
                    in 0..60    -> resources.getString(R.string.comment1)
                    in 61..180  -> resources.getString(R.string.comment2)
                    in 181..300 -> resources.getString(R.string.comment3)
                    else        -> resources.getString(R.string.comment4)
                } else ""

        if (found) commentText.text = comment
        else
        {
            usageText.text = ""
            var text = "You may not have this app installed or have not used it "
            text += if (interval == 1) "today" else "this week"
            commentText.setText(text)
        }
    }


    private fun getUsageStats() : List<UsageStats> {
        val calendar = Calendar.getInstance()
        calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE), 0, 0, 0)

        var start = calendar.timeInMillis
        var end = System.currentTimeMillis()

        if (interval == Day.Yesterday.v) {
            end = calendar.timeInMillis
            calendar.add(Calendar.DATE, -1)
            start = calendar.timeInMillis
        }

        var stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, start, end)
        return stats
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        when (requestCode) {
            100 -> setStat()
        }
    }

    private fun requestPermission() {
        Toast.makeText(this, "Need to request permission", Toast.LENGTH_SHORT).show()
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    private fun checkForPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), getPackageName())
        return mode == AppOpsManager.MODE_ALLOWED
    }

}

package com.lance.steadysteps

import android.webkit.JavascriptInterface
import org.json.JSONArray
import org.json.JSONObject

class Bridge(private val a: MainActivity) {
    private fun p() = Alarms.prefs(a)

    @JavascriptInterface
    fun getConfig(): String {
        val o = JSONObject()
        o.put("alarmsOn", p().getBoolean("alarmsOn", false))
        o.put("textOn", p().getBoolean("textOn", false))
        o.put("textMin", p().getInt("textMin", 480))
        o.put("wife", p().getString("wife", ""))
        o.put("days", p().getString("days", "1100111"))
        o.put("last", p().getString("lastSms", ""))
        o.put("next", Alarms.nextAlarmText(a))
        o.put("build", BuildConfig.VERSION_CODE)
        o.put("version", BuildConfig.VERSION_NAME)
        o.put("smsOk", a.checkSelfPermission(android.Manifest.permission.SEND_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED)
        return o.toString()
    }

    @JavascriptInterface
    fun setConfig(json: String) {
        val o = JSONObject(json)
        val e = p().edit()
        if (o.has("alarmsOn")) e.putBoolean("alarmsOn", o.getBoolean("alarmsOn"))
        if (o.has("textOn")) e.putBoolean("textOn", o.getBoolean("textOn"))
        if (o.has("textMin")) e.putInt("textMin", o.getInt("textMin"))
        if (o.has("wife")) e.putString("wife", o.getString("wife").trim())
        if (o.has("days")) e.putString("days", o.getString("days"))
        e.apply()
        Alarms.scheduleAll(a)
    }

    @JavascriptInterface
    fun askPermissions() { a.runOnUiThread { a.askPermissions() } }

    @JavascriptInterface
    fun batterySettings() { a.runOnUiThread { a.batterySettings() } }

    @JavascriptInterface
    fun testAlarm() { Alarms.testRingIn1Min(a) }

    @JavascriptInterface
    fun testText(): String = Alarms.sendDinnerText(a)

    @JavascriptInterface
    fun getAte(): String = JSONArray(Alarms.ateToday(a)).toString()

    @JavascriptInterface
    fun setAte(id: String, value: Boolean) { Alarms.setAte(a, id, value) }

    @JavascriptInterface
    fun checkUpdate() {
        Thread {
            val r = Updater.check(a)
            a.runOnUiThread { a.web.evaluateJavascript("window.onUpdate&&window.onUpdate(" + r + ")", null) }
        }.start()
    }

    @JavascriptInterface
    fun installUpdate(url: String) {
        Thread {
            val r = Updater.download(a, url)
            a.runOnUiThread { a.web.evaluateJavascript("window.onUpdateStatus&&window.onUpdateStatus(" + JSONObject.quote(r) + ")", null) }
        }.start()
    }
}

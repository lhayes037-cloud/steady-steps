package com.lance.steadysteps

import android.content.Intent
import android.net.Uri
import android.provider.Settings

import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object Updater {
    private const val API = "https://api.github.com/repos/lhayes037-cloud/steady-steps/releases/latest"

    fun check(c: MainActivity): String {
        return try {
            val con = URL(API).openConnection() as HttpURLConnection
            con.setRequestProperty("Accept", "application/vnd.github+json")
            con.connectTimeout = 10000; con.readTimeout = 15000
            val body = con.inputStream.bufferedReader().readText()
            val j = JSONObject(body)
            val tag = j.getString("tag_name")
            val n = tag.removePrefix("build-").toIntOrNull() ?: 0
            val assets = j.getJSONArray("assets")
            var url = ""
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.getString("name").endsWith(".apk")) url = a.getString("browser_download_url")
            }
            val o = JSONObject()
            o.put("ok", true); o.put("latest", n); o.put("have", BuildConfig.VERSION_CODE)
            o.put("newer", n > BuildConfig.VERSION_CODE && url.isNotEmpty()); o.put("url", url)
            o.toString()
        } catch (e: Exception) {
            JSONObject().put("ok", false).put("error", e.message ?: "no connection").toString()
        }
    }

    fun download(c: MainActivity, url: String): String {
        if (!url.startsWith("https://github.com/lhayes037-cloud/steady-steps/")) return "Update link not trusted"
        if (!c.packageManager.canRequestPackageInstalls()) {
            c.runOnUiThread {
                c.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + c.packageName)))
            }
            return "Allow installs from this app, then tap Update again"
        }
        return try {
            val dir = File(c.cacheDir, "updates"); dir.mkdirs()
            val f = File(dir, "steady-steps.apk")
            val con = URL(url).openConnection() as HttpURLConnection
            con.connectTimeout = 15000; con.readTimeout = 60000
            con.inputStream.use { i -> f.outputStream().use { o -> i.copyTo(o) } }
            val uri = androidx.core.content.FileProvider.getUriForFile(c, "com.lance.steadysteps.files", f)
            val it = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            c.runOnUiThread { c.startActivity(it) }
            "Downloaded. Tap Install on the next screen."
        } catch (e: Exception) {
            "Download failed: " + (e.message ?: "unknown")
        }
    }
}

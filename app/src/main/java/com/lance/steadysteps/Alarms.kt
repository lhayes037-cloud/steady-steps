package com.lance.steadysteps

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.telephony.SmsManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class Slot(val id: String, val title: String, val body: String, val startMin: Int, val endMin: Int, val every: Int)

object Alarms {
    val SLOTS = listOf(
        Slot("eat1", "Eat 1", "Egg white scramble and a cup of fruit", 390, 450, 30),
        Slot("eat2", "Eat 2", "Healthy Turkey and a cup of Market Vegetable soup", 720, 870, 30),
        Slot("grab", "Fruit", "Apple or pear", 870, 870, 30)
    )
    const val TEXT_CODE = 100
    const val CHANNEL = "eat_alarm"

    fun prefs(c: Context) = c.getSharedPreferences("ss", Context.MODE_PRIVATE)

    fun dayKey(cal: Calendar = Calendar.getInstance()): String =
        SimpleDateFormat("yyyyMMdd", Locale.US).format(cal.time)

    private fun calFor(offset: Int, minOfDay: Int): Calendar {
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_YEAR, offset)
        c.set(Calendar.HOUR_OF_DAY, minOfDay / 60)
        c.set(Calendar.MINUTE, minOfDay % 60)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c
    }

    private fun workDay(c: Context, cal: Calendar): Boolean {
        val s = prefs(c).getString("days", "1111111")!!
        return s[Dinners.todayIndex(cal)] == '1'
    }

    fun isAte(c: Context, id: String, offset: Int = 0): Boolean {
        val cal = Calendar.getInstance(); cal.add(Calendar.DAY_OF_YEAR, offset)
        return prefs(c).getBoolean("ate_" + id + "_" + dayKey(cal), false)
    }

    fun setAte(c: Context, id: String, value: Boolean) {
        prefs(c).edit().putBoolean("ate_" + id + "_" + dayKey(), value).apply()
        val idx = SLOTS.indexOfFirst { it.id == id }
        if (idx >= 0) {
            (c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(1000 + idx)
            scheduleOne(c, idx)
        }
    }

    fun ateToday(c: Context): List<String> = SLOTS.filter { isAte(c, it.id) }.map { it.id }

    private fun showPi(c: Context): PendingIntent =
        PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)

    private fun ringPi(c: Context, idx: Int, slotMin: Int): PendingIntent {
        val i = Intent(c, AlarmReceiver::class.java).setAction("RING")
            .putExtra("idx", idx).putExtra("slot", slotMin)
        return PendingIntent.getBroadcast(c, idx + 1, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun textPi(c: Context): PendingIntent {
        val i = Intent(c, AlarmReceiver::class.java).setAction("TEXT")
        return PendingIntent.getBroadcast(c, TEXT_CODE, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun am(c: Context) = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun setClock(c: Context, at: Long, pi: PendingIntent) {
        am(c).setAlarmClock(AlarmManager.AlarmClockInfo(at, showPi(c)), pi)
    }

    private fun nextTrigger(c: Context, s: Slot, after: Long): Pair<Int, Long>? {
        for (off in 0..7) {
            val day = calFor(off, 0)
            if (!workDay(c, day)) continue
            if (off == 0 && isAte(c, s.id)) continue
            var m = s.startMin
            while (m <= s.endMin) {
                val t = calFor(off, m).timeInMillis
                if (t > after) return Pair(m, t)
                m += s.every
            }
        }
        return null
    }

    fun scheduleOne(c: Context, idx: Int) {
        val s = SLOTS[idx]
        val pi = ringPi(c, idx, s.startMin)
        if (!prefs(c).getBoolean("alarmsOn", false)) { am(c).cancel(pi); return }
        val nt = nextTrigger(c, s, System.currentTimeMillis() + 2000)
        if (nt == null) { am(c).cancel(pi); return }
        setClock(c, nt.second, ringPi(c, idx, nt.first))
    }

    fun scheduleText(c: Context) {
        val pi = textPi(c)
        val p = prefs(c)
        if (!p.getBoolean("textOn", false)) { am(c).cancel(pi); return }
        val min = p.getInt("textMin", 480)
        var t = calFor(0, min).timeInMillis
        if (t <= System.currentTimeMillis() + 2000) t = calFor(1, min).timeInMillis
        setClock(c, t, pi)
    }

    fun scheduleAll(c: Context) {
        for (i in SLOTS.indices) scheduleOne(c, i)
        scheduleText(c)
    }

    fun testRingIn1Min(c: Context) {
        val i = Intent(c, AlarmReceiver::class.java).setAction("TEST").putExtra("idx", 1)
        val pi = PendingIntent.getBroadcast(c, 200, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        setClock(c, System.currentTimeMillis() + 60 * 1000L, pi)
    }

    fun nextAlarmText(c: Context): String {
        if (!prefs(c).getBoolean("alarmsOn", false)) return "Next alarm: none (alarms are off)"
        var best: Long = Long.MAX_VALUE
        var label = ""
        for (s in SLOTS) {
            val nt = nextTrigger(c, s, System.currentTimeMillis()) ?: continue
            if (nt.second < best) { best = nt.second; label = s.title }
        }
        if (best == Long.MAX_VALUE) return "Next alarm: none (no work days picked)"
        return "Next alarm: " + SimpleDateFormat("EEE h:mm a", Locale.US).format(Date(best)) + " (" + label + ")"
    }

    fun snooze(c: Context, idx: Int) {
        (c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(1000 + idx)
        val s = SLOTS[idx]
        setClock(c, System.currentTimeMillis() + 15 * 60 * 1000L, ringPi(c, idx, s.startMin))
    }

    private fun act(c: Context, action: String, idx: Int, req: Int): PendingIntent {
        val i = Intent(c, AlarmReceiver::class.java).setAction(action).putExtra("idx", idx)
        return PendingIntent.getBroadcast(c, req, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun ring(c: Context, idx: Int) {
        val s = SLOTS[idx]
        val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val ch = NotificationChannel(CHANNEL, "Eating alarms", NotificationManager.IMPORTANCE_HIGH)
        ch.setSound(sound, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        ch.enableVibration(true)
        ch.vibrationPattern = longArrayOf(0, 600, 400, 600)
        nm.createNotificationChannel(ch)

        val full = PendingIntent.getActivity(
            c, 2000 + idx,
            Intent(c, AlarmActivity::class.java).putExtra("idx", idx).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val ate = Notification.Action.Builder(Icon.createWithResource(c, R.drawable.ic_launcher), "I ate", act(c, "ATE", idx, 3000 + idx)).build()
        val snz = Notification.Action.Builder(Icon.createWithResource(c, R.drawable.ic_launcher), "Snooze 15 min", act(c, "SNOOZE", idx, 4000 + idx)).build()
        val n = Notification.Builder(c, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("Time to eat")
            .setContentText(s.body)
            .setCategory(Notification.CATEGORY_ALARM)
            .setOngoing(true)
            .setContentIntent(full)
            .setFullScreenIntent(full, true)
            .addAction(ate)
            .addAction(snz)
            .build()
        n.flags = n.flags or Notification.FLAG_INSISTENT
        nm.notify(1000 + idx, n)
    }

    fun sendDinnerText(c: Context): String {
        val num = prefs(c).getString("wife", "")!!.trim()
        if (num.isEmpty()) return "No number saved yet"
        if (c.checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) return "Text permission is off"
        return try {
            val sm = c.getSystemService(SmsManager::class.java)
            val parts = sm.divideMessage(Dinners.message(Dinners.todayIndex()))
            sm.sendMultipartTextMessage(num, null, parts, null, null)
            val msg = "Sent " + SimpleDateFormat("EEE h:mm a", Locale.US).format(Date())
            prefs(c).edit().putString("lastSms", msg).apply()
            msg
        } catch (e: Exception) {
            val msg = "Could not send: " + (e.message ?: "unknown")
            prefs(c).edit().putString("lastSms", msg).apply()
            msg
        }
    }
}

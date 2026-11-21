package com.lance.steadysteps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val idx = i.getIntExtra("idx", 0).coerceIn(0, Alarms.SLOTS.size - 1)
        when (i.action) {
            "RING" -> {
                if (!Alarms.isAte(c, Alarms.SLOTS[idx].id)) Alarms.ring(c, idx)
                Alarms.scheduleOne(c, idx)
            }
            "ATE" -> Alarms.setAte(c, Alarms.SLOTS[idx].id, true)
            "SNOOZE" -> Alarms.snooze(c, idx)
            "TEXT" -> {
                Alarms.sendDinnerText(c)
                Alarms.scheduleText(c)
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        Alarms.scheduleAll(c)
    }
}

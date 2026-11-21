package com.lance.steadysteps

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class AlarmActivity : Activity() {
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun label(t: String, size: Float, color: Int, bold: Boolean = false): TextView {
        val v = TextView(this)
        v.text = t; v.textSize = size; v.setTextColor(color)
        if (bold) v.setTypeface(null, Typeface.BOLD)
        v.setPadding(0, dp(6), 0, dp(6))
        return v
    }

    private fun button(t: String, bg: Int, fg: Int, border: Boolean): Button {
        val b = Button(this)
        b.text = t; b.textSize = 20f; b.setTextColor(fg); b.isAllCaps = false
        val g = GradientDrawable(); g.cornerRadius = dp(16).toFloat(); g.setColor(bg)
        if (border) g.setStroke(dp(2), Color.parseColor("#8FA0C2"))
        b.background = g
        b.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(64)).apply { topMargin = dp(12) }
        return b
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        val idx = intent.getIntExtra("idx", 0).coerceIn(0, Alarms.SLOTS.size - 1)
        val slot = Alarms.SLOTS[idx]
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#14213D"))
        root.setPadding(dp(28), dp(72), dp(28), dp(40))
        root.gravity = Gravity.CENTER_VERTICAL

        val time = java.text.SimpleDateFormat("h:mm", java.util.Locale.US).format(java.util.Date())
        root.addView(label(slot.title.uppercase() + " ALARM", 13f, Color.parseColor("#B9C3D6"), true))
        root.addView(label(time, 72f, Color.WHITE, true))
        root.addView(label("Time to eat", 26f, Color.WHITE, true))
        root.addView(label(slot.body, 18f, Color.parseColor("#DCE3F0")))
        root.addView(label("Rings again every 30 minutes until you tap I ate.", 14f, Color.parseColor("#B9C3D6")))

        val ate = button("I ate", Color.parseColor("#FF7A1A"), Color.parseColor("#14213D"), false)
        ate.setOnClickListener { Alarms.setAte(this, slot.id, true); finish() }
        val snz = button("Snooze 15 minutes", Color.TRANSPARENT, Color.WHITE, true)
        snz.setOnClickListener { Alarms.snooze(this, idx); finish() }
        root.addView(ate); root.addView(snz)
        setContentView(root)
    }
}

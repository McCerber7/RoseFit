package com.pinkfitness.tracker

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setMargins
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.time.LocalDate

class OnboardingActivity : AppCompatActivity() {

    private val presets = listOf(30, 60, 75, 90, 100)
    private var startOffsetDays = 0L
    private lateinit var daysInput: TextInputEditText
    private val chips = mutableListOf<TextView>()
    private lateinit var btnToday: TextView
    private lateinit var btnYesterday: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        setContentView(R.layout.activity_onboarding)

        daysInput = findViewById(R.id.daysInput)
        val chipsRow = findViewById<LinearLayout>(R.id.chipsRow)
        btnToday = findViewById(R.id.btnStartToday)
        btnYesterday = findViewById(R.id.btnStartYesterday)

        daysInput.setText("75")

        for (p in presets) {
            val chip = TextView(this).apply {
                text = "$p"
                textSize = 14f
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#E84E8A"))
                background = androidx.core.content.ContextCompat.getDrawable(this@OnboardingActivity, R.drawable.bg_pill)
                val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                lp.setMargins((4 * resources.displayMetrics.density).toInt())
                layoutParams = lp
                setPadding(0, (11 * resources.displayMetrics.density).toInt(), 0, (11 * resources.displayMetrics.density).toInt())
                setOnClickListener {
                    daysInput.setText("$p")
                    daysInput.setSelection(daysInput.text?.length ?: 0)
                    highlightChip(this)
                }
            }
            chips.add(chip)
            chipsRow.addView(chip)
        }
        highlightChip(chips[2]) // 75 по умолчанию

        btnToday.setOnClickListener {
            startOffsetDays = 0
            highlightDateButton()
        }
        btnYesterday.setOnClickListener {
            startOffsetDays = 1
            highlightDateButton()
        }
        highlightDateButton()

        findViewById<MaterialButton>(R.id.btnStart).setOnClickListener { start() }
    }

    private fun highlightChip(selected: TextView) {
        for (c in chips) {
            if (c == selected) {
                c.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FF6FA5"))
                c.setTextColor(Color.WHITE)
            } else {
                c.backgroundTintList = null
                c.setTextColor(Color.parseColor("#E84E8A"))
            }
        }
    }

    private fun highlightDateButton() {
        val pink = Color.parseColor("#FF6FA5")
        val dark = Color.parseColor("#E84E8A")
        if (startOffsetDays == 0L) {
            btnToday.backgroundTintList = ColorStateList.valueOf(pink)
            btnToday.setTextColor(Color.WHITE)
            btnYesterday.backgroundTintList = null
            btnYesterday.setTextColor(dark)
        } else {
            btnYesterday.backgroundTintList = ColorStateList.valueOf(pink)
            btnYesterday.setTextColor(Color.WHITE)
            btnToday.backgroundTintList = null
            btnToday.setTextColor(dark)
        }
    }

    private fun start() {
        val days = daysInput.text.toString().toIntOrNull()
        if (days == null || days < 1 || days > 999) {
            Toast.makeText(this, "Введи число дней от 1 до 999 🌸", Toast.LENGTH_LONG).show()
            return
        }
        Store.totalDays = days
        Store.startDate = LocalDate.now().minusDays(startOffsetDays)
        finish()
    }
}

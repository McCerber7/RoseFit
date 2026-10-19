package com.pinkfitness.tracker

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.time.LocalDate
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var circle: DayCircleView
    private lateinit var statDays: TextView
    private lateinit var statWeight: TextView
    private lateinit var statSteps: TextView

    private val onboardingLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (!Store.isConfigured) finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        setContentView(R.layout.activity_main)

        circle = findViewById(R.id.dayCircle)
        statDays = findViewById(R.id.statDays)
        statWeight = findViewById(R.id.statWeight)
        statSteps = findViewById(R.id.statSteps)

        circle.setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
        }
        findViewById<ImageView>(R.id.btnSettings).setOnClickListener { showSettings() }
    }

    override fun onResume() {
        super.onResume()
        if (!Store.isConfigured) {
            onboardingLauncher.launch(Intent(this, OnboardingActivity::class.java))
            return
        }
        refresh()
    }

    private fun refresh() {
        val today = Store.currentDay()
        val todayRaw = Store.todayRaw()
        circle.dayNumber = today
        circle.totalDays = Store.totalDays
        circle.finished = todayRaw > Store.totalDays
        circle.doneToday = Store.load(today).done

        var doneCount = 0
        for (d in 1..Store.totalDays) {
            if (Store.load(d).done) doneCount++
        }
        statDays.text = "$doneCount / ${Store.totalDays}"

        var latestWeight: Double? = null
        var day = 5
        while (day <= Store.totalDays && day <= todayRaw) {
            Store.load(day).weightKg?.let { latestWeight = it }
            day += 5
        }
        statWeight.text = latestWeight?.let { fmtKg(it) } ?: "—"

        statSteps.text = Store.load(today).steps.toString()
    }

    private fun fmtKg(w: Double): String {
        val s = String.format("%.1f", w)
        return s.replace('.', ',')
    }

    private fun showSettings() {
        val items = arrayOf(
            "🔢 Изменить длину челленджа",
            "🗓 Изменить дату старта",
            "🗑 Сбросить весь прогресс"
        )
        AlertDialog.Builder(this)
            .setTitle("Настройки 🎀")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> editDays()
                    1 -> editStartDate()
                    2 -> confirmReset()
                }
            }
            .setNegativeButton("Закрыть", null)
            .show()
    }

    private fun editDays() {
        val input = EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        input.setText(Store.totalDays.toString())
        val container = FrameLayout(this)
        val pad = (22 * resources.displayMetrics.density).toInt()
        container.setPadding(pad, pad / 2, pad, 0)
        container.addView(input)
        AlertDialog.Builder(this)
            .setTitle("Сколько дней?")
            .setView(container)
            .setPositiveButton("Сохранить 💾") { _, _ ->
                val v = input.text.toString().toIntOrNull()
                if (v != null && v in 1..999) {
                    Store.totalDays = v
                    refresh()
                } else {
                    toast("Введи число от 1 до 999 🌸")
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun editStartDate() {
        val cal = Calendar.getInstance()
        Store.startDate?.let {
            cal.set(it.year, it.monthValue - 1, it.dayOfMonth)
        }
        DatePickerDialog(this, { _, y, m, d ->
            Store.startDate = LocalDate.of(y, m + 1, d)
            refresh()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun confirmReset() {
        AlertDialog.Builder(this)
            .setTitle("Сбросить прогресс? 🥺")
            .setMessage("Все отметки, фото и записи будут удалены. Это действие нельзя отменить.")
            .setPositiveButton("Да, сбросить") { _, _ ->
                Store.resetAll(this)
                recreate()
            }
            .setNegativeButton("Нет, оставить 💕", null)
            .show()
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

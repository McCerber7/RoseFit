package com.pinkfitness.tracker

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CalendarActivity : AppCompatActivity() {

    private lateinit var grid: GridLayout
    private lateinit var progressText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        setContentView(R.layout.activity_calendar)

        grid = findViewById(R.id.calendarGrid)
        progressText = findViewById(R.id.progressText)
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        buildGrid()
    }

    private fun buildGrid() {
        grid.removeAllViews()
        val total = Store.totalDays
        val today = Store.todayRaw()

        var doneCount = 0
        for (d in 1..total) if (Store.load(d).done) doneCount++
        progressText.text = "Отмечено $doneCount из $total 💕"

        val inflater = LayoutInflater.from(this)
        for (d in 1..total) {
            val cell = inflater.inflate(R.layout.item_cell, grid, false)
            val lp = GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED, 1f),
                GridLayout.spec(GridLayout.UNDEFINED, 1f)
            )
            lp.width = 0
            lp.height = (66 * resources.displayMetrics.density).toInt()
            val m = (4 * resources.displayMetrics.density).toInt()
            lp.setMargins(m, m, m, m)
            cell.layoutParams = lp

            val root = cell.findViewById<View>(R.id.cellRoot)
            val number = cell.findViewById<TextView>(R.id.cellNumber)
            val check = cell.findViewById<TextView>(R.id.cellCheck)
            val weigh = cell.findViewById<TextView>(R.id.cellWeigh)
            val lock = cell.findViewById<TextView>(R.id.cellLock)
            val todayMark = cell.findViewById<TextView>(R.id.cellToday)

            number.text = d.toString()
            val data = Store.load(d)
            val isFuture = d > today

            if (Store.isWeighDay(d)) weigh.visibility = View.VISIBLE

            when {
                isFuture -> {
                    root.setBackgroundResource(R.drawable.bg_cell_locked)
                    number.visibility = View.GONE
                    lock.visibility = View.VISIBLE
                }
                data.done -> {
                    root.setBackgroundResource(R.drawable.bg_cell_done)
                    number.setTextColor(0xFFFFFFFF.toInt())
                    check.visibility = View.VISIBLE
                }
            }
            if (d == today && !isFuture) {
                todayMark.visibility = View.VISIBLE
                if (data.done) {
                    root.setBackgroundResource(R.drawable.bg_cell_done_today)
                } else {
                    root.isSelected = true
                }
            }

            root.setOnClickListener {
                if (isFuture) {
                    Toast.makeText(this, "Этот день ещё впереди 🌸", Toast.LENGTH_SHORT).show()
                } else {
                    startActivity(
                        Intent(this, DayActivity::class.java).putExtra("day", d)
                    )
                }
            }

            grid.addView(cell)
        }
    }
}

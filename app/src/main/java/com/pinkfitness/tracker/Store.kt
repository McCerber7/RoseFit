package com.pinkfitness.tracker

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class Meal(
    val name: String,
    val kcal: Int,
    val photoPath: String? = null,
    val time: String = ""
)

data class DayData(
    var done: Boolean = false,
    var steps: Int = 0,
    var weightKg: Double? = null,
    var bodyPhoto: String? = null,
    val meals: MutableList<Meal> = mutableListOf()
) {
    val totalKcal: Int get() = meals.sumOf { it.kcal }

    fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("done", done)
        o.put("steps", steps)
        weightKg?.let { o.put("weight", it) }
        bodyPhoto?.let { o.put("body", it) }
        val arr = JSONArray()
        for (m in meals) {
            val mo = JSONObject()
            mo.put("name", m.name)
            mo.put("kcal", m.kcal)
            m.photoPath?.let { mo.put("photo", it) }
            mo.put("time", m.time)
            arr.put(mo)
        }
        o.put("meals", arr)
        return o
    }

    companion object {
        fun fromJson(o: JSONObject): DayData {
            val d = DayData()
            d.done = o.optBoolean("done", false)
            d.steps = o.optInt("steps", 0)
            if (o.has("weight") && !o.isNull("weight")) d.weightKg = o.optDouble("weight")
            d.bodyPhoto = o.optString("body", "").ifBlank { null }
            val arr = o.optJSONArray("meals") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val m = arr.optJSONObject(i) ?: continue
                d.meals.add(
                    Meal(
                        m.optString("name", "Еда").ifBlank { "Еда" },
                        m.optInt("kcal", 0),
                        m.optString("photo", "").ifBlank { null },
                        m.optString("time", "")
                    )
                )
            }
            return d
        }
    }
}

object Store {
    private const val PREFS_NAME = "pinkfit_prefs"
    private lateinit var sp: SharedPreferences

    fun init(context: Context) {
        if (!::sp.isInitialized) {
            sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    var totalDays: Int
        get() = sp.getInt("totalDays", 0)
        set(v) { sp.edit().putInt("totalDays", v).apply() }

    var startDate: LocalDate?
        get() {
            val s = sp.getString("startDate", null) ?: return null
            return try { LocalDate.parse(s) } catch (e: Exception) { null }
        }
        set(v) { sp.edit().putString("startDate", v?.toString()).apply() }

    val isConfigured: Boolean get() = totalDays > 0 && startDate != null

    /** Номер сегодняшнего дня без ограничения (может быть < 1 до старта или > totalDays после). */
    fun todayRaw(): Int {
        val s = startDate ?: return 1
        return ChronoUnit.DAYS.between(s, LocalDate.now()).toInt() + 1
    }

    fun currentDay(): Int = todayRaw().coerceIn(1, if (totalDays > 0) totalDays else 1)

    fun isWeighDay(day: Int): Boolean = day % 5 == 0

    fun dateForDay(day: Int): LocalDate = (startDate ?: LocalDate.now()).plusDays((day - 1).toLong())

    fun load(day: Int): DayData {
        val s = sp.getString("day_$day", null) ?: return DayData()
        return try { DayData.fromJson(JSONObject(s)) } catch (e: Exception) { DayData() }
    }

    fun saveDay(day: Int, d: DayData) {
        sp.edit().putString("day_$day", d.toJson().toString()).apply()
    }

    fun resetAll(context: Context) {
        sp.edit().clear().apply()
        runCatching { File(context.filesDir, "photos").deleteRecursively() }
    }
}

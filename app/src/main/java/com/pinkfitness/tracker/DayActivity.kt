package com.pinkfitness.tracker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class DayActivity : AppCompatActivity() {

    private var day = 1
    private lateinit var data: DayData
    private var loading = true

    // Диалог приёма пищи
    private var mealDialog: AlertDialog? = null
    private var mealDialogPhotoPath: String? = null
    private var mealPhotoPreview: ImageView? = null
    private var mealPhotoPlaceholder: View? = null
    private var editingMealIndex = -1

    // Фото (камера / галерея)
    private enum class PhotoTarget { NONE, MEAL, BODY }
    private var photoTarget = PhotoTarget.NONE
    private var cameraFile: File? = null
    private var pendingBodyPhoto: String? = null

    private lateinit var bodyPhotoImage: ImageView
    private lateinit var bodyPhotoPlaceholder: View
    private lateinit var bodyPhotoDelete: TextView
    private lateinit var mealsContainer: LinearLayout
    private lateinit var mealsEmpty: TextView
    private lateinit var kcalTotal: TextView

    private val cameraContract = object : ActivityResultContract<android.net.Uri, Boolean>() {
        override fun createIntent(context: Context, input: android.net.Uri): Intent =
            Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                .putExtra(MediaStore.EXTRA_OUTPUT, input)
                .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)

        override fun parseResult(resultCode: Int, intent: Intent?): Boolean =
            resultCode == Activity.RESULT_OK
    }

    private val cameraLauncher = registerForActivityResult(cameraContract) { ok ->
        val f = cameraFile
        if (f != null && f.exists() && f.length() > 0 && (ok || f.length() > 10_000)) {
            PhotoUtil.shrinkInPlace(f.absolutePath)
            onPhotoReceived(f.absolutePath)
        } else {
            f?.let { runCatching { it.delete() } }
        }
        cameraFile = null
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val path = PhotoUtil.importFromUri(this, uri, day)
            if (path != null) onPhotoReceived(path)
            else toast("Не удалось загрузить фото 🥺")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        setContentView(R.layout.activity_day)

        day = intent.getIntExtra("day", 1).coerceIn(1, Store.totalDays.coerceAtLeast(1))
        data = Store.load(day)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.dayTitle).text = "День $day"
        findViewById<TextView>(R.id.dayDate).text = formatDate(Store.dateForDay(day))

        bodyPhotoImage = findViewById(R.id.bodyPhotoImage)
        bodyPhotoPlaceholder = findViewById(R.id.bodyPhotoPlaceholder)
        bodyPhotoDelete = findViewById(R.id.bodyPhotoDelete)
        mealsContainer = findViewById(R.id.mealsContainer)
        mealsEmpty = findViewById(R.id.mealsEmpty)
        kcalTotal = findViewById(R.id.kcalTotal)

        // День выполнен
        val doneSwitch = findViewById<MaterialSwitch>(R.id.doneSwitch)
        val motivation = findViewById<TextView>(R.id.motivationText)
        doneSwitch.isChecked = data.done
        motivation.text = if (data.done) "Ты умничка! Так держать 💖" else "Отметь, когда всё готово!"
        doneSwitch.setOnCheckedChangeListener { _, checked ->
            if (loading) return@setOnCheckedChangeListener
            data.done = checked
            motivation.text = if (checked) "Ты умничка! Так держать 💖" else "Отметь, когда всё готово!"
            Store.saveDay(day, data)
        }

        // Шаги
        val stepsInput = findViewById<TextInputEditText>(R.id.stepsInput)
        if (data.steps > 0) stepsInput.setText(data.steps.toString())
        stepsInput.addTextChangedListener(simpleWatcher {
            data.steps = stepsInput.text.toString().toIntOrNull() ?: 0
            Store.saveDay(day, data)
        })

        // Взвешивание (каждые 5 дней)
        val weighCard = findViewById<MaterialCardView>(R.id.weighCard)
        if (Store.isWeighDay(day)) {
            weighCard.visibility = View.VISIBLE
            findViewById<TextView>(R.id.weighBadge).visibility = View.VISIBLE

            val weightInput = findViewById<TextInputEditText>(R.id.weightInput)
            data.weightKg?.let { weightInput.setText(fmtKg(it)) }
            weightInput.addTextChangedListener(simpleWatcher {
                data.weightKg = parseKg(weightInput.text.toString())
                Store.saveDay(day, data)
            })

            findViewById<MaterialCardView>(R.id.bodyPhotoCard).setOnClickListener {
                askPhotoSource(PhotoTarget.BODY)
            }
            bodyPhotoDelete.setOnClickListener { confirmDeleteBodyPhoto() }
            showBodyPhoto()
        }

        // Еда
        findViewById<MaterialButton>(R.id.btnAddMeal).setOnClickListener { openMealDialog(-1) }
        renderMeals()

        loading = false
    }

    // ---------- Форматирование ----------

    private val months = arrayOf(
        "января", "февраля", "марта", "апреля", "мая", "июня",
        "июля", "августа", "сентября", "октября", "ноября", "декабря"
    )

    private fun formatDate(d: LocalDate): String = "${d.dayOfMonth} ${months[d.monthValue - 1]} ${d.year}"

    private fun fmtKg(w: Double): String =
        String.format("%.1f", w).replace('.', ',')

    private fun parseKg(s: String): Double? {
        val v = s.replace(',', '.').toDoubleOrNull()
        return if (v != null && v in 20.0..400.0) v else null
    }

    // ---------- Еда ----------

    private fun renderMeals() {
        mealsContainer.removeAllViews()
        mealsEmpty.visibility = if (data.meals.isEmpty()) View.VISIBLE else View.GONE
        kcalTotal.text = "${data.totalKcal} ккал 🔥"

        val inflater = LayoutInflater.from(this)
        data.meals.forEachIndexed { index, meal ->
            val v = inflater.inflate(R.layout.item_meal, mealsContainer, false)
            val photo = v.findViewById<ImageView>(R.id.mealPhoto)
            val emoji = v.findViewById<TextView>(R.id.mealEmoji)
            if (meal.photoPath != null) {
                val bmp = PhotoUtil.decode(meal.photoPath, 160)
                if (bmp != null) {
                    photo.setImageBitmap(bmp)
                    photo.visibility = View.VISIBLE
                    emoji.visibility = View.GONE
                }
            }
            v.findViewById<TextView>(R.id.mealNameText).text = meal.name
            val timePart = if (meal.time.isNotBlank()) " • ${meal.time}" else ""
            v.findViewById<TextView>(R.id.mealKcalText).text = "${meal.kcal} ккал$timePart"
            v.findViewById<TextView>(R.id.mealEdit).setOnClickListener { openMealDialog(index) }
            v.findViewById<TextView>(R.id.mealDelete).setOnClickListener { confirmDeleteMeal(index) }
            mealsContainer.addView(v)
        }
    }

    private fun defaultMealName(): String {
        val h = LocalTime.now().hour
        return when {
            h < 11 -> "Завтрак"
            h < 16 -> "Обед"
            h < 20 -> "Ужин"
            else -> "Перекус"
        }
    }

    private fun openMealDialog(index: Int) {
        editingMealIndex = index
        val existing = if (index >= 0) data.meals[index] else null
        mealDialogPhotoPath = existing?.photoPath

        val view = LayoutInflater.from(this).inflate(R.layout.dialog_meal, null)
        val nameInput = view.findViewById<TextInputEditText>(R.id.mealName)
        val kcalInput = view.findViewById<TextInputEditText>(R.id.mealKcal)
        mealPhotoPreview = view.findViewById(R.id.photoPreview)
        mealPhotoPlaceholder = view.findViewById(R.id.photoPlaceholder)

        nameInput.setText(existing?.name ?: defaultMealName())
        existing?.let { kcalInput.setText(it.kcal.toString()) }
        refreshMealDialogPhoto()

        view.findViewById<MaterialCardView>(R.id.photoSlot).setOnClickListener {
            askPhotoSource(PhotoTarget.MEAL)
        }

        val title = if (index >= 0) "Изменить приём пищи ✏️" else "Новый приём пищи 🍽"
        mealDialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setView(view)
            .setPositiveButton("Сохранить 💾", null)
            .setNegativeButton("Отмена", null)
            .create()
        mealDialog?.show()
        mealDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
            val kcal = kcalInput.text.toString().toIntOrNull()
            if (kcal == null) {
                toast("Введи калории числом 🌸")
                return@setOnClickListener
            }
            val name = nameInput.text.toString().trim().ifBlank { defaultMealName() }
            if (index >= 0) {
                val old = data.meals[index]
                if (mealDialogPhotoPath != old.photoPath) PhotoUtil.deleteFile(old.photoPath)
                data.meals[index] = Meal(name, kcal, mealDialogPhotoPath, old.time)
            } else {
                val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                data.meals.add(Meal(name, kcal, mealDialogPhotoPath, time))
            }
            Store.saveDay(day, data)
            renderMeals()
            mealDialog?.dismiss()
            mealDialog = null
        }
    }

    private fun refreshMealDialogPhoto() {
        val preview = mealPhotoPreview ?: return
        val placeholder = mealPhotoPlaceholder ?: return
        val path = mealDialogPhotoPath
        if (path != null) {
            val bmp = PhotoUtil.decode(path, 520)
            if (bmp != null) {
                preview.setImageBitmap(bmp)
                preview.visibility = View.VISIBLE
                placeholder.visibility = View.GONE
                return
            }
        }
        preview.visibility = View.GONE
        placeholder.visibility = View.VISIBLE
    }

    private fun confirmDeleteMeal(index: Int) {
        AlertDialog.Builder(this)
            .setTitle("Удалить приём пищи?")
            .setMessage("«${data.meals[index].name}» исчезнет из этого дня 🥺")
            .setPositiveButton("Удалить") { _, _ ->
                PhotoUtil.deleteFile(data.meals[index].photoPath)
                data.meals.removeAt(index)
                Store.saveDay(day, data)
                renderMeals()
            }
            .setNegativeButton("Оставить", null)
            .show()
    }

    // ---------- Фото прогресса ----------

    private fun showBodyPhoto() {
        val path = pendingBodyPhoto ?: data.bodyPhoto
        if (path != null) {
            val bmp = PhotoUtil.decode(path, 640)
            if (bmp != null) {
                bodyPhotoImage.setImageBitmap(bmp)
                bodyPhotoImage.visibility = View.VISIBLE
                bodyPhotoPlaceholder.visibility = View.GONE
                bodyPhotoDelete.visibility = View.VISIBLE
                return
            }
        }
        bodyPhotoImage.visibility = View.GONE
        bodyPhotoPlaceholder.visibility = View.VISIBLE
        bodyPhotoDelete.visibility = View.GONE
    }

    private fun confirmDeleteBodyPhoto() {
        AlertDialog.Builder(this)
            .setTitle("Удалить фото прогресса?")
            .setMessage("Фото изменений за этот день будет удалено 🥺")
            .setPositiveButton("Удалить") { _, _ ->
                PhotoUtil.deleteFile(pendingBodyPhoto ?: data.bodyPhoto)
                pendingBodyPhoto = null
                data.bodyPhoto = null
                Store.saveDay(day, data)
                showBodyPhoto()
            }
            .setNegativeButton("Оставить", null)
            .show()
    }

    private fun askPhotoSource(target: PhotoTarget) {
        photoTarget = target
        AlertDialog.Builder(this)
            .setTitle("Добавить фото 📸")
            .setItems(arrayOf("📷 Камера", "🖼 Галерея")) { _, which ->
                if (which == 0) launchCamera() else launchGallery()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun launchCamera() {
        try {
            val f = PhotoUtil.newCameraFile(this, day)
            cameraFile = f
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", f)
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            toast("Камера недоступна 🥺")
        }
    }

    private fun launchGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun onPhotoReceived(path: String) {
        when (photoTarget) {
            PhotoTarget.MEAL -> {
                mealDialogPhotoPath = path
                refreshMealDialogPhoto()
            }
            PhotoTarget.BODY -> {
                if (data.bodyPhoto != null && data.bodyPhoto != path) {
                    PhotoUtil.deleteFile(data.bodyPhoto)
                }
                pendingBodyPhoto = path
                data.bodyPhoto = path
                Store.saveDay(day, data)
                showBodyPhoto()
                toast("Фото прогресса сохранено 🌷")
            }
            PhotoTarget.NONE -> {}
        }
    }

    // ---------- Утилиты ----------

    private fun simpleWatcher(onChange: () -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            if (!loading) onChange()
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

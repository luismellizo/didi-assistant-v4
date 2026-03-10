package com.kepler.didioracle

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var projectionManager: MediaProjectionManager
    private lateinit var prefs: SharedPreferences

    private val captureResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val serviceIntent = Intent(this, ScreenReaderService::class.java).apply {
                putExtra(ScreenReaderService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenReaderService.EXTRA_RESULT_DATA, result.data)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        prefs = getSharedPreferences("DidiOraclePrefs", Context.MODE_PRIVATE)

        val mainLayout = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0A0A0A"))
            isFillViewport = true
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 80, 60, 80)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // --- LOGO HACKER ---
        val logoView = ImageView(this).apply {
            val resId = resources.getIdentifier("logo_hacker", "drawable", packageName)
            if (resId != 0) setImageResource(resId)
            layoutParams = LinearLayout.LayoutParams(450, 450)
            setPadding(0, 0, 0, 40)
        }

        val titleText = TextView(this).apply {
            text = "DIDI ORACLE v4.0"
            textSize = 26f
            setTextColor(Color.parseColor("#00FF41"))
            typeface = Typeface.MONOSPACE
            setPadding(0, 0, 0, 10)
        }

        val subTitle = TextView(this).apply {
            text = "[ STATUS: SYSTEM_READY ]"
            textSize = 14f
            setTextColor(Color.parseColor("#003B00"))
            typeface = Typeface.MONOSPACE
            setPadding(0, 0, 0, 60)
        }

        // --- INPUTS ESTILO TERMINAL ---
        fun createHackTitle(txt: String) = TextView(this).apply {
            text = "> $txt"
            setTextColor(Color.parseColor("#00FF41"))
            typeface = Typeface.MONOSPACE
            textSize = 14f
            setPadding(0, 20, 0, 10)
        }

        fun createHackEdit(hintText: String, valKey: String, defaultVal: Any): EditText {
            return EditText(this).apply {
                hint = hintText
                setHintTextColor(Color.parseColor("#003B00"))
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#1A1A1A"))
                    setStroke(2, Color.parseColor("#003B00"))
                    cornerRadius = 10f
                }
                setPadding(30,30,30,30)
                typeface = Typeface.MONOSPACE
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                
                if (defaultVal is Float) setText(prefs.getFloat(valKey, defaultVal).toString())
                else setText(prefs.getInt(valKey, defaultVal as Int).toString())
            }
        }

        val etTarifa = createHackEdit("TARIFA_TARGET", "META_KM", 1350f)
        val etOrigen = createHackEdit("MAX_ORIGIN_KM", "MAX_ORIGIN", 1.0f)

        val cbxNuevos = CheckBox(this).apply {
            text = "REJECT_NEW_USERS"
            setTextColor(Color.parseColor("#00FF41"))
            buttonTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#00FF41"))
            typeface = Typeface.MONOSPACE
            isChecked = prefs.getBoolean("RECHAZAR_NUEVOS", true)
            setPadding(20, 40, 0, 40)
        }

        // --- BOTÓN GUARDAR ---
        val btnSave = createHackButton("SAVE_CONFIG", "#003B00") {
            val t = etTarifa.text.toString().toFloatOrNull() ?: 1350f
            val o = etOrigen.text.toString().toFloatOrNull() ?: 1.0f
            prefs.edit().putFloat("META_KM", t).putFloat("MAX_ORIGIN", o).putBoolean("RECHAZAR_NUEVOS", cbxNuevos.isChecked).apply()
            Toast.makeText(context, "CONFIG_SAVED", Toast.LENGTH_SHORT).show()
        }

        // --- BOTONES DE ACCIÓN ---
        val btnOverlay = createHackButton("GRANT_OVERLAY_PERMISSION", "#1A1A1A") {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        }

        val btnStart = createHackButton("INITIALIZE_ORACLE", "#00FF41", Color.BLACK) {
            if (Settings.canDrawOverlays(this@MainActivity)) {
                captureResultLauncher.launch(projectionManager.createScreenCaptureIntent())
            } else {
                Toast.makeText(context, "ACCESS_DENIED: OVERLAY_PERMISSION", Toast.LENGTH_LONG).show()
            }
        }

        // --- FIRMA ---
        val credits = TextView(this).apply {
            text = "\n\nDesarrollada por Luis Mellizo\n[ Premium Edition ]"
            textSize = 12f
            alpha = 0.5f
            setTextColor(Color.parseColor("#00FF41"))
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            setPadding(0, 100, 0, 40)
        }

        content.addView(logoView)
        content.addView(titleText)
        content.addView(subTitle)
        
        content.addView(createHackTitle("PARAM_GANANCIA_KM"))
        content.addView(etTarifa)
        content.addView(createHackTitle("PARAM_DISTANCIA_RECOGIDA"))
        content.addView(etOrigen)
        content.addView(cbxNuevos)
        content.addView(btnSave)
        
        content.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(1, 80) })
        content.addView(btnOverlay)
        content.addView(btnStart)
        content.addView(credits)

        mainLayout.addView(content)
        setContentView(mainLayout)
    }

    private fun createHackButton(txt: String, bgColor: String, txtColor: Int = Color.parseColor("#00FF41"), onClick: Button.() -> Unit): Button {
        return Button(this).apply {
            text = txt
            setTextColor(txtColor)
            typeface = Typeface.MONOSPACE
            background = GradientDrawable().apply {
                setColor(Color.parseColor(bgColor))
                setStroke(3, Color.parseColor("#00FF41"))
                cornerRadius = 15f
            }
            setOnClickListener { onClick() }
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins(0, 20, 0, 20)
            layoutParams = params
        }
    }
}

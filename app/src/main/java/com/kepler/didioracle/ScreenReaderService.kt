package com.kepler.didioracle

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.hardware.display.DisplayManager
import java.util.Locale
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class ScreenReaderService : Service() {

    companion object {
        const val EXTRA_RESULT_CODE = "RESULT_CODE"
        const val EXTRA_RESULT_DATA = "RESULT_DATA"
        private const val FOREGROUND_ID = 1111
        private const val CHANNEL_ID = "DidiOracleChannel"
    }

    private lateinit var windowManager: WindowManager
    private lateinit var projectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    // UI Overlay
    private var overlayLayout: FrameLayout? = null
    private var isOverlayInflated = false

    private val handler = Handler(Looper.getMainLooper())
    private var isProcessingImage = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(FOREGROUND_ID, createNotification())
        
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val resultData: Intent? = intent?.getParcelableExtra(EXTRA_RESULT_DATA)
        
        if (resultCode == Activity.RESULT_OK && resultData != null) {
            startScreenCapture(resultCode, resultData)
        } else {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startScreenCapture(resultCode: Int, intent: Intent) {
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        val density = metrics.densityDpi
        
        // Reducimos la resolución a la mitad para que MLKit procese 10 veces más rápido 
        // y el ImageReader no se sature de memoria
        val width = metrics.widthPixels / 2
        val height = metrics.heightPixels / 2

        mediaProjection = projectionManager.getMediaProjection(resultCode, intent)
        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                super.onStop()
                stopSelf()
            }
        }, null)

        // IMPORTANTE: PixelFormat.RGBA_8888 y maxImages=2
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )

        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            
            if (isProcessingImage) {
                // Buffer lleno o lenteado, droppeamos frame manual para no trabar el servicio
                image.close()
                return@setOnImageAvailableListener
            }
            
            isProcessingImage = true
            processImageWithMLKit(image, width, height)
            
        }, handler)
    }

    private fun processImageWithMLKit(image: Image, width: Int, height: Int) {
        try {
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * width
            
            // Recrear bitmap compensando el Memory Padding del Hardware
            val bitmap = Bitmap.createBitmap(
                width + rowPadding / pixelStride,
                height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            
            // Encerramos el close rápido para liberar la RAM de la GPU
            val finalBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height)
            image.close() 

            val inputImage = InputImage.fromBitmap(finalBitmap, 0)
            textRecognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    // Agrupar texto
                    val allTexts = visionText.textBlocks.map { it.text }
                    analyzeTexts(allTexts)
                    isProcessingImage = false
                }
                .addOnFailureListener { e ->
                    Log.e("DidiOracle", "Error procesando OCR", e)
                    isProcessingImage = false
                }
        } catch (e: Exception) {
            Log.e("DidiOracle", "Error general OCR", e)
            image.close()
            isProcessingImage = false
        }
    }

    private fun analyzeTexts(texts: List<String>) {
        val wholeText = texts.joinToString(" ")
        Log.d("DidiOracle", "OCR TEXTO: $wholeText")
        
        // Verifica si estamos en pantalla de Oferta 
        val lowerText = wholeText.lowercase()
        if (lowerText.contains("aceptar") || lowerText.contains("rechazo permitido") || lowerText.contains("incluye la tarifa")) {
            // Pasamos los textos sin procesar al nuevo Evaluador
            val currentTripData = OracleLogic.evaluate(this, texts)
            showOverlay(currentTripData)
        } else {
            hideOverlay()
        }
    }

    private fun showOverlay(trip: OracleLogic.TripData) {
        if (!isOverlayInflated) {
             val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            params.y = 120 // Más abajo para no tapar los headers de DiDi

            overlayLayout = FrameLayout(this)
            
            // Fondo Hacker: Negro profundo con borde neón
            val bgDrawable = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 30f
                setColor(Color.parseColor("#0A0A0A"))
                setStroke(6, Color.parseColor("#00FF41")) // Verde neón por defecto
            }
            overlayLayout?.background = bgDrawable
            overlayLayout?.setPadding(45, 35, 45, 35)

            val container = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                gravity = Gravity.CENTER
            }

            val titleView = TextView(this).apply {
                textSize = 28f
                typeface = Typeface.MONOSPACE
                setTypeface(null, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
                id = android.R.id.text1
            }
            
            val subView = TextView(this).apply {
                textSize = 18f
                typeface = Typeface.MONOSPACE
                setTextColor(Color.parseColor("#003B00")) // Verde Matrix apagado
                gravity = Gravity.CENTER
                id = android.R.id.text2
                setPadding(0, 10, 0, 0)
            }
            
            container.addView(titleView)
            container.addView(subView)
            overlayLayout?.addView(container)

            windowManager.addView(overlayLayout, params)
            isOverlayInflated = true
        }

        val titleView = overlayLayout?.findViewById<TextView>(android.R.id.text1)
        val subView = overlayLayout?.findViewById<TextView>(android.R.id.text2)
        
        val bgDrawable = overlayLayout?.background as? android.graphics.drawable.GradientDrawable

        val pagoIcon = when (trip.tipoPago) {
            "NEQUI" -> "🟣 NEQUI"
            "TARJETA" -> "💳 TARJETA"
            else -> "💵 EFECTIVO" 
        }

        if (trip.isRentable) {
            val valorStr = String.format(Locale.US, "%,.0f", trip.valorPorkm)
            val netaStr = String.format(Locale.US, "%,.0f", trip.gananciaNeta)
            titleView?.text = "¡OK! \$$valorStr/km"
            titleView?.setTextColor(Color.parseColor("#00FF41"))
            val distStr = String.format(Locale.US, "%.1f", trip.origenKm + trip.destinoKm)
            val gasStr = String.format(Locale.US, "%,.0f", trip.costoGasolina)
            subView?.text = "NETA: \$$netaStr | GAS: \$$gasStr\nDIST: ${distStr}km | $pagoIcon"
            subView?.setTextColor(Color.parseColor("#80FF80"))
            bgDrawable?.setStroke(6, Color.parseColor("#00FF41"))
        } else {
            titleView?.text = "[ RECHAZAR ]"
            titleView?.setTextColor(Color.parseColor("#FF0000"))
            val extraVal = if (trip.valorPorkm > 0) "\$${String.format(Locale.US, "%,.0f", trip.valorPorkm)}/km neto" else ""
            val gasStr = if (trip.costoGasolina > 0) "GAS: \$${String.format(Locale.US, "%,.0f", trip.costoGasolina)}" else ""
            subView?.text = "${trip.msgRechazo}\n$extraVal $gasStr\n$pagoIcon"
            subView?.setTextColor(Color.parseColor("#FF6666"))
            bgDrawable?.setStroke(6, Color.parseColor("#FF0000"))
        }
    }

    private fun hideOverlay() {
        if (isOverlayInflated) {
            windowManager.removeView(overlayLayout)
            overlayLayout = null
            isOverlayInflated = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        virtualDisplay?.release()
        mediaProjection?.stop()
        imageReader?.close()
        hideOverlay()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Oracle Screen Reader",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder.setContentTitle("DiDi Oráculo")
            .setContentText("Leyendo pantalla en busca de servicios rentables...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }
}

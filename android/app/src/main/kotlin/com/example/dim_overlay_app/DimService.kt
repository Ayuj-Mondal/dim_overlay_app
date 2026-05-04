package com.example.dim_overlay_app

import android.app.*
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.View
import android.view.WindowManager
import android.app.PendingIntent

import io.flutter.plugin.common.MethodChannel

class DimService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var alpha: Int = 128

    companion object {
        var methodChannel: MethodChannel? = null
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForeground(1, createNotification())

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        when (intent?.action) {

            "START" -> {
                val level = intent.getDoubleExtra("level", 0.5)
                alpha = ((level * 255).toInt()).coerceIn(0, 255)

                if (overlayView == null) {
                    showOverlay() 
                } else {
                    updateOverlay() // This forces the change if service was already alive
                }
            }

            "STOP" -> {
                notifyFlutter("onStop", 0.0) // Notify UI to toggle off
                removeOverlay()
                stopSelf()
            }

            "UPDATE" -> {
                val level = intent.getDoubleExtra("level", 0.5)
                alpha = ((level * 255).toInt()).coerceIn(0, 255)
                updateOverlay()
            }

            "INC" -> {
                alpha = (alpha + 20).coerceAtMost(255)
                updateOverlay()
                notifyFlutter("onUpdate", alpha / 255.0)
            }

            "DEC" -> {
                alpha = (alpha - 20).coerceAtLeast(0)
                updateOverlay()
                notifyFlutter("onUpdate", alpha / 255.0)
            }
        }

        return START_STICKY
    }

    private fun notifyFlutter(method: String, level: Double) {
        // Run on main thread to talk to Flutter
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            methodChannel?.invokeMethod(method, mapOf("level" to level))
        }
    }

    private fun showOverlay() {
        if (overlayView != null) return

        overlayView = View(this).apply {
            setBackgroundColor(Color.argb(alpha.toInt(), 0, 0, 0))
        
            // This forces the view's content to ignore the navigation bar's reserved space
            systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                                 View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                                 View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }

        val params = WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
        
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

            flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

            // Handles the top "notch" area on your Realme C21
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }

            format = PixelFormat.TRANSLUCENT
            // FILL ensures there are no gaps at the bottom edge
            gravity = android.view.Gravity.FILL 
        }

        try {
            windowManager.addView(overlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateOverlay() {
       overlayView?.setBackgroundColor(Color.argb(alpha.toInt(), 0, 0, 0))

       try {
            val params = overlayView?.layoutParams as? WindowManager.LayoutParams
            if (params != null && overlayView != null) {
                windowManager.updateViewLayout(overlayView, params)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeOverlay() {
        try {
            overlayView?.let {
                windowManager.removeView(it)
                overlayView = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotification(): Notification {

        val channelId = "dim_channel_id"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Dim Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val incIntent = Intent(this, DimService::class.java).apply { action = "INC" }
        val decIntent = Intent(this, DimService::class.java).apply { action = "DEC" }
        val stopIntent = Intent(this, DimService::class.java).apply { action = "STOP" }

        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

        val incPending = PendingIntent.getService(this, 1, incIntent, flags)
        val decPending = PendingIntent.getService(this, 2, decIntent, flags)
        val stopPending = PendingIntent.getService(this, 3, stopIntent, flags)

        return Notification.Builder(this, channelId)
            .setContentTitle("Dim Active")
            .setContentText("Screen dim overlay running")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .addAction(0, "+", incPending)
            .addAction(0, "-", decPending)
            .addAction(0, "OFF", stopPending)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
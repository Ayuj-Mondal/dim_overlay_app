package com.example.dim_overlay_app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity: FlutterActivity() {

    private val CHANNEL = "dim_channel"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        val channel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
        // Pass the channel reference to the service class
        DimService.methodChannel = channel

        
            channel.setMethodCallHandler { call, result ->

                when (call.method) {

                    "startDim" -> {
                        val level = call.argument<Double>("level") ?: 0.5
                        val intent = Intent(this, DimService::class.java)
                        intent.action = "START"
                        intent.putExtra("level", level) // Ensure this line exists
                        startService(intent)
                        result.success(null)
                    }

                    "stopDim" -> {
                        val intent = Intent(this, DimService::class.java)
                        intent.action = "STOP"
                        startService(intent)
                        result.success(null)
                    }

                    "updateDim" -> {
                        val level = call.argument<Double>("level") ?: 0.5
                        val intent = Intent(this, DimService::class.java)
                        intent.action = "UPDATE"
                        intent.putExtra("level", level)
                        startService(intent)
                        result.success(null)
                    }

                    "requestOverlay" -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                            !Settings.canDrawOverlays(this)) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName")
                            )
                            startActivity(intent)
                        }
                        result.success(null)
                    }

                    else -> result.notImplemented()
                }
            }
    }
}
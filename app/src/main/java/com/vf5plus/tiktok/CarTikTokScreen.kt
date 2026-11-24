package com.vf5plus.tiktok

import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ContextThemeWrapper
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

private const val TAG = "CarTikTokScreen"

class CarTikTokScreen(carContext: CarContext) : Screen(carContext), SurfaceCallback {

    private var virtualDisplay: VirtualDisplay? = null
    private var presentation: TikTokCarPresentation? = null

    init {
        try {
            carContext.getCarService(AppManager::class.java).setSurfaceCallback(this)
        } catch (e: Exception) {
            Log.e(TAG, "setSurfaceCallback error: ${e.message}")
        }

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                releaseDisplay()
            }
        })
    }

    override fun onGetTemplate(): Template {
        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle("🔲 Bố cục")
                    .setOnClickListener {
                        Handler(Looper.getMainLooper()).post {
                            presentation?.toggleMode()
                        }
                    }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("▼ Tiếp")
                    .setOnClickListener {
                        Handler(Looper.getMainLooper()).post {
                            TikTokEngine.getInstance(carContext).skipNext()
                        }
                    }
                    .build()
            )
            .build()

        return NavigationTemplate.Builder()
            .setActionStrip(actionStrip)
            .build()
    }

    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        val surface = surfaceContainer.surface ?: return
        val width = if (surfaceContainer.width > 0) surfaceContainer.width else 1280
        val height = if (surfaceContainer.height > 0) surfaceContainer.height else 720
        val dpi = if (surfaceContainer.dpi > 0) surfaceContainer.dpi else 213

        Handler(Looper.getMainLooper()).post {
            try {
                releaseDisplay()

                val themedContext = ContextThemeWrapper(carContext, android.R.style.Theme_DeviceDefault_NoActionBar)
                val displayManager = carContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

                val flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION or
                            DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY

                virtualDisplay = displayManager.createVirtualDisplay(
                    "VF5TikTokDisplay",
                    width, height, dpi,
                    surface,
                    flags
                )

                virtualDisplay?.display?.let { display ->
                    presentation = TikTokCarPresentation(themedContext, display).apply {
                        show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "onSurfaceAvailable error: ${e.message}")
            }
        }
    }

    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        Handler(Looper.getMainLooper()).post {
            releaseDisplay()
        }
    }

    private fun releaseDisplay() {
        try {
            presentation?.dismiss()
            presentation = null
            virtualDisplay?.release()
            virtualDisplay = null
        } catch (e: Exception) {
            Log.e(TAG, "releaseDisplay error: ${e.message}")
        }
    }
}

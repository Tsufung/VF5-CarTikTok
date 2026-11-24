package com.vf5plus.tiktok

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var engine: TikTokEngine
    private lateinit var phoneWebViewContainer: FrameLayout
    private lateinit var tvCarStatus: TextView

    private val handler = Handler(Looper.getMainLooper())
    private val statusChecker = object : Runnable {
        override fun run() {
            updateCarStatus()
            handler.postDelayed(this, 2000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Xin quyền thông báo trên Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // Khởi động Playback Service nhận phím vô-lăng
        val serviceIntent = Intent(this, TikTokPlaybackService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            startService(serviceIntent)
        }

        engine = TikTokEngine.getInstance(this)

        tvCarStatus = findViewById(R.id.tvCarStatus)
        phoneWebViewContainer = findViewById(R.id.phoneWebViewContainer)

        val btnTestNext: Button = findViewById(R.id.btnTestNext)
        val btnToggleLayout: Button = findViewById(R.id.btnToggleLayout)
        val btnStopAll: Button = findViewById(R.id.btnStopAll)

        btnTestNext.setOnClickListener {
            engine.skipNext()
            Toast.makeText(this, "Đã chuyển sang video tiếp theo!", Toast.LENGTH_SHORT).show()
        }

        btnToggleLayout.setOnClickListener {
            if (TikTokCarPresentation.activeInstance != null) {
                TikTokCarPresentation.activeInstance?.toggleMode()
                Toast.makeText(this, "Đã chuyển bố cục màn hình xe!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Chưa kết nối màn hình xe. Hãy cắm USB vào xe VF5!", Toast.LENGTH_SHORT).show()
            }
        }

        btnStopAll.setOnClickListener {
            engine.stopAndRelease()
            val stopIntent = Intent(this, TikTokPlaybackService::class.java)
            stopService(stopIntent)
            finishAffinity()
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

    override fun onStart() {
        super.onStart()
        // Nếu màn hình xe chưa mở thì gắn vào điện thoại để người dùng đăng nhập
        if (TikTokCarPresentation.activeInstance == null) {
            engine.attachToContainer(phoneWebViewContainer)
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(statusChecker)
    }

    override fun onPause() {
        handler.removeCallbacks(statusChecker)
        super.onPause()
    }

    override fun onDestroy() {
        if (isFinishing) {
            engine.stopAndRelease()
            val stopIntent = Intent(this, TikTokPlaybackService::class.java)
            stopService(stopIntent)
        }
        super.onDestroy()
    }

    private fun updateCarStatus() {
        if (TikTokCarPresentation.activeInstance != null) {
            tvCarStatus.text = "🟢 ĐÃ KẾT NỐI XE VF5 (Video TikTok đang phát trực tiếp trên xe)"
            tvCarStatus.setTextColor(Color.parseColor("#34D399"))
        } else {
            tvCarStatus.text = "⚪ Đang chờ cắm USB kết nối Android Auto trên xe VF5"
            tvCarStatus.setTextColor(Color.parseColor("#94A3B8"))
        }
    }
}

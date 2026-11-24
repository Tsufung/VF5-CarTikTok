package com.vf5plus.tiktok

import android.app.Presentation
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class TikTokCarPresentation(
    private val themedContext: Context,
    display: Display
) : Presentation(themedContext, display, android.R.style.Theme_DeviceDefault_NoActionBar) {

    companion object {
        var activeInstance: TikTokCarPresentation? = null
            private set

        const val MODE_SPLIT = 0 // Maps Trái (65%) + TikTok Điện Thoại Đứng Phải (35% - Y hệt ảnh)
        const val MODE_FULL_PHONE = 1 // TikTok Điện Thoại Đứng ở giữa màn hình xe
    }

    private var currentMode = MODE_SPLIT
    private val engine = TikTokEngine.getInstance(themedContext)
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var rootLayout: FrameLayout
    private lateinit var splitLayout: LinearLayout
    private lateinit var mapsContainer: FrameLayout
    private lateinit var phoneWrapper: FrameLayout
    private lateinit var phoneInnerContainer: FrameLayout
    private lateinit var webViewMaps: WebView

    init {
        activeInstance = this
        buildPresentationUi()
    }

    private fun buildPresentationUi() {
        rootLayout = FrameLayout(themedContext).apply {
            setBackgroundColor(Color.parseColor("#090D16"))
        }

        // ─── 1. Khung Chia Đôi (Split Layout) ──────────────────────────────────
        splitLayout = LinearLayout(themedContext).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Khung Trái: Google Maps (65%)
        mapsContainer = FrameLayout(themedContext).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 65f)
            setBackgroundColor(Color.parseColor("#121212"))
        }
        setupMaps()
        splitLayout.addView(mapsContainer)

        // Divider phân cách mỏng
        val divider = View(themedContext).apply {
            layoutParams = LinearLayout.LayoutParams(dp(3), ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.parseColor("#1E293B"))
        }
        splitLayout.addView(divider)

        // Khung Phải: Khung Điện Thoại Đứng TikTok (35% - Đúng chỗ người dùng khoanh đỏ!)
        phoneWrapper = FrameLayout(themedContext).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 35f)
            setBackgroundColor(Color.parseColor("#0F172A"))
        }
        setupPhoneContainer()
        splitLayout.addView(phoneWrapper)

        rootLayout.addView(splitLayout)
        setContentView(rootLayout)

        // Gắn TikTok WebView vào khung điện thoại
        engine.attachToContainer(phoneInnerContainer)
    }

    private fun setupMaps() {
        webViewMaps = WebView(themedContext).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                cacheMode = WebSettings.LOAD_DEFAULT
                userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
            }
            webViewClient = WebViewClient()
            loadUrl("https://www.google.com/maps")
        }
        mapsContainer.addView(webViewMaps)
    }

    private fun setupPhoneContainer() {
        // Hộp bao khung điện thoại
        val phoneFrame = FrameLayout(themedContext).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                setMargins(dp(6), dp(6), dp(6), dp(6))
            }
            // Viền bo tròn giống điện thoại
            background = GradientDrawable().apply {
                setColor(Color.BLACK)
                cornerRadius = dp(14).toFloat()
                setStroke(dp(2), Color.parseColor("#334155"))
            }
            clipToOutline = true
        }

        // Vùng chứa WebView TikTok
        phoneInnerContainer = FrameLayout(themedContext).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        phoneFrame.addView(phoneInnerContainer)

        // Thanh điều khiển nhanh nổi trên góc khung điện thoại
        val floatingControls = LinearLayout(themedContext).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
                marginEnd = dp(8)
            }
        }

        // Nút chuyển video trước
        val btnPrev = createTouchCircleButton("▲") {
            engine.skipPrevious()
        }
        // Nút chuyển video tiếp theo
        val btnNext = createTouchCircleButton("▼") {
            engine.skipNext()
        }
        // Nút đổi chế độ toàn màn / chia đôi
        val btnToggleMode = createTouchCircleButton("🔲") {
            toggleMode()
        }

        floatingControls.addView(btnPrev)
        floatingControls.addView(btnNext)
        floatingControls.addView(btnToggleMode)

        phoneFrame.addView(floatingControls)
        phoneWrapper.addView(phoneFrame)
    }

    private fun createTouchCircleButton(symbol: String, onClick: () -> Unit): TextView {
        return TextView(themedContext).apply {
            text = symbol
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply {
                bottomMargin = dp(8)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#CC1E293B"))
                setStroke(dp(1), Color.parseColor("#475569"))
            }
            setOnClickListener { onClick() }
        }
    }

    fun toggleMode() {
        handler.post {
            currentMode = if (currentMode == MODE_SPLIT) MODE_FULL_PHONE else MODE_SPLIT
            if (currentMode == MODE_FULL_PHONE) {
                // Ẩn bản đồ, đưa khung điện thoại ra giữa màn xe
                mapsContainer.visibility = View.GONE
                (phoneWrapper.layoutParams as LinearLayout.LayoutParams).apply {
                    weight = 100f
                    width = ViewGroup.LayoutParams.MATCH_PARENT
                }
            } else {
                // Quay lại chia đôi (65% Maps - 35% TikTok đúng ảnh khoanh đỏ)
                mapsContainer.visibility = View.VISIBLE
                (phoneWrapper.layoutParams as LinearLayout.LayoutParams).apply {
                    weight = 35f
                    width = 0
                }
            }
            splitLayout.requestLayout()
        }
    }

    private fun dp(value: Int): Int {
        val density = themedContext.resources.displayMetrics.density
        return (value * density + 0.5f).toInt()
    }

    override fun onDetachedFromWindow() {
        activeInstance = null
        super.onDetachedFromWindow()
    }
}

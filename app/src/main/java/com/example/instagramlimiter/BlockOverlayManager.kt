package com.example.instagramlimiter

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.CountDownTimer
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class BlockOverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private var timer: CountDownTimer? = null

    val isShowing: Boolean
        get() = overlayView != null

    /**
     * Afișează modalul interactiv "Are u doomscrolling?" cu opțiunile Yes / No.
     */
    fun showIntentPrompt(
        onYesClicked: () -> Unit,
        onNoClicked: () -> Unit
    ) {
        if (!Settings.canDrawOverlays(context) || isShowing) return

        val rootLayout = createBaseContainer().apply {
            setBackgroundColor(Color.parseColor("#E6090D16")) // Semi-transparent dark
        }

        val questionView = TextView(context).apply {
            text = "Are u doomscrolling?"
            textSize = 24f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 48)
        }

        val buttonsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val yesButton = Button(context).apply {
            text = "Yes"
            setBackgroundColor(Color.parseColor("#E11D48"))
            setTextColor(Color.WHITE)
            setPadding(64, 28, 64, 28)
            setOnClickListener {
                dismiss()
                onYesClicked()
            }
        }

        val spacer = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(40, 1)
        }

        val noButton = Button(context).apply {
            text = "No"
            setBackgroundColor(Color.parseColor("#1E293B"))
            setTextColor(Color.WHITE)
            setPadding(64, 28, 64, 28)
            setOnClickListener {
                dismiss()
                onNoClicked()
            }
        }

        buttonsRow.addView(yesButton)
        buttonsRow.addView(spacer)
        buttonsRow.addView(noButton)

        rootLayout.addView(questionView)
        rootLayout.addView(buttonsRow)

        attachOverlay(rootLayout)
    }

    /**
     * Afișează modalul final de blocare cu mesajul "Enough doomscroll".
     */
    fun showBlockModal(
        message: String = "Enough doomscroll",
        remainingCooldownMs: Long = 0L,
        onDismissed: () -> Unit
    ) {
        if (!Settings.canDrawOverlays(context)) return
        dismiss() // Înlocuiește orice alt overlay activ

        val rootLayout = createBaseContainer().apply {
            setBackgroundColor(Color.parseColor("#F50F172A"))
        }

        val titleView = TextView(context).apply {
            text = "SESSION TERMINATED"
            textSize = 13f
            letterSpacing = 0.15f
            setTextColor(Color.parseColor("#F43F5E"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }

        val messageView = TextView(context).apply {
            text = message
            textSize = 24f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }

        val countdownView = TextView(context).apply {
            textSize = 15f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 48)
        }

        val dismissButton = Button(context).apply {
            text = "Damn it, okay"
            setBackgroundColor(Color.parseColor("#2563EB"))
            setTextColor(Color.WHITE)
            setPadding(56, 28, 56, 28)
            setOnClickListener {
                dismiss()
                onDismissed()
            }
        }

        rootLayout.addView(titleView)
        rootLayout.addView(messageView)
        rootLayout.addView(countdownView)
        rootLayout.addView(dismissButton)

        attachOverlay(rootLayout)
        startCountdown(remainingCooldownMs, countdownView)
    }

    private fun createBaseContainer(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(64, 64, 64, 64)
        }
    }

    private fun attachOverlay(view: View) {
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        overlayView = view
        try {
            windowManager.addView(overlayView, layoutParams)
        } catch (_: Exception) {
            overlayView = null
        }
    }

    private fun startCountdown(durationMs: Long, targetView: TextView) {
        timer?.cancel()
        if (durationMs <= 0L) {
            targetView.visibility = View.GONE
            return
        }

        targetView.visibility = View.VISIBLE
        timer = object : CountDownTimer(durationMs, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = kotlin.math.ceil(millisUntilFinished / 1000.0).toInt()
                targetView.text = "Cooldown: ${secondsRemaining}s"
            }

            override fun onFinish() {
                targetView.text = "Cooldown expired."
            }
        }.start()
    }

    fun dismiss() {
        timer?.cancel()
        timer = null
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: IllegalArgumentException) {
                // View deja detașat
            }
            overlayView = null
        }
    }
}
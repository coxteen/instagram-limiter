package com.example.instagramlimiter

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class ReelsBlockerService : AccessibilityService() {

    private val maxLimit = 3

    private var reelsCount = 0
    private var storiesCount = 0

    private var lastScrollTimestamp = 0L
    private val scrollDebounceMs = 1200L

    // Pentru a ști când se schimbă utilizatorul/story-ul vizualizat
    private var lastStoryAuthor: String = ""

    private var overlayView: View? = null
    private var windowManager: WindowManager? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return

        // Dacă ieșim din Instagram, resetăm contoarele de sesiune
        if (packageName != "com.instagram.android") {
            if (reelsCount > 0 || storiesCount > 0) {
                reelsCount = 0
                storiesCount = 0
                lastStoryAuthor = ""
            }
            return
        }

        val rootNode = rootInActiveWindow ?: return

        checkStories(rootNode)
        checkReels(event, rootNode)
    }

    private fun checkStories(rootNode: AccessibilityNodeInfo) {
        // În interfața de Stories există un container cu elementul de progres și numele autorului
        // Căutăm nodurile tipice ecranului de Story (ex: bara de progres sau ID-urile specifice de header)
        val reelViewerMatches = rootNode.findAccessibilityNodeInfosByViewId("com.instagram.android:id/reel_viewer_title")
        val storyAuthorNode = if (reelViewerMatches.isNotEmpty()) reelViewerMatches[0] else null

        if (storyAuthorNode != null && storyAuthorNode.text != null) {
            val currentAuthor = storyAuthorNode.text.toString()

            // Dacă s-a trecut la o altă persoană sau la primul story
            if (currentAuthor.isNotBlank() && currentAuthor != lastStoryAuthor) {
                lastStoryAuthor = currentAuthor
                storiesCount++

                showToast("Stories vizualizate: $storiesCount/$maxLimit")

                if (storiesCount >= maxLimit) {
                    blockAndExitToHome("Fă puțină mișcare! Ieși la aer!")
                }
            }
        }
    }

    private fun checkReels(event: AccessibilityEvent, rootNode: AccessibilityNodeInfo) {
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED) return

        // Verificăm dacă suntem în viewer-ul de Reels (buton de audio, remix sau video container)
        val isReelsTab = rootNode.findAccessibilityNodeInfosByViewId("com.instagram.android:id/clips_video_container").isNotEmpty() 
                || rootNode.findAccessibilityNodeInfosByViewId("com.instagram.android:id/clips_viewer_view_pager").isNotEmpty()

        val currentTime = System.currentTimeMillis()
        if (isReelsTab && (currentTime - lastScrollTimestamp > scrollDebounceMs)) {
            lastScrollTimestamp = currentTime
            reelsCount++

            showToast("Reels vizualizate: $reelsCount/$maxLimit")

            if (reelsCount >= maxLimit) {
                blockAndExitToHome("Fă puțină mișcare! Ieși la aer!")
            }
        }
    }

    private fun blockAndExitToHome(message: String) {
        performGlobalAction(GLOBAL_ACTION_HOME)

        Handler(Looper.getMainLooper()).postDelayed({
            showOverlay(message)
        }, 300L)
    }

    private fun showOverlay(message: String) {
        if (!Settings.canDrawOverlays(this)) {
            showToast("Lipsește permisiunea de Overlay!")
            return
        }

        if (overlayView != null) return

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

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#F2121212"))
            setPadding(60, 60, 60, 60)
        }

        val textView = TextView(this).apply {
            text = message
            textSize = 21f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 50)
        }

        val dismissButton = Button(this).apply {
            text = "Închide"
            setBackgroundColor(Color.parseColor("#E11D48")) // Roșu/accent modern
            setTextColor(Color.WHITE)
            setOnClickListener {
                removeOverlay()
            }
        }

        rootLayout.addView(textView)
        rootLayout.addView(dismissButton)

        overlayView = rootLayout
        windowManager?.addView(overlayView, layoutParams)
    }

    private fun removeOverlay() {
        overlayView?.let {
            windowManager?.removeView(it)
            overlayView = null
        }
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
    }

    override fun onInterrupt() {}
}
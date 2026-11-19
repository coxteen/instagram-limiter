package com.example.instagramlimiter

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

class ReelsBlockerService : AccessibilityService() {

    private val maxItemsLimit = 3
    private val scrollDebounceMs = 1100L
    private val secondPromptDelayMs = 10_000L
    private val cooldownDurationMs = 60_000L // 60 secunde

    private var isInInstagramSession = false
    private var promptsCompleted = 0
    private var scrollItemsCount = 0 // Contor comun pentru Reels + Home Feed scrolls
    private var storiesCount = 0
    private var lastScrollTimestamp = 0L
    private var lastStoryAuthor = ""

    private var cooldownUntilTimestamp = 0L
    private var isExiting = false

    private val analyzer = InstagramScreenAnalyzer()
    private lateinit var overlayManager: BlockOverlayManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private val secondPromptRunnable = Runnable {
        if (isInInstagramSession && promptsCompleted == 1 && System.currentTimeMillis() >= cooldownUntilTimestamp) {
            triggerIntentPrompt(promptNumber = 2)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlayManager = BlockOverlayManager(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || isExiting) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName == applicationContext.packageName) return

        if (packageName != "com.instagram.android") {
            resetSession()
            return
        }

        val now = System.currentTimeMillis()

        if (now < cooldownUntilTimestamp) {
            val remaining = cooldownUntilTimestamp - now
            enforceExitWithBlockModal(remainingCooldownMs = remaining)
            return
        }

        if (!isInInstagramSession) {
            isInInstagramSession = true
            promptsCompleted = 0
            triggerIntentPrompt(promptNumber = 1)
            return
        }

        if (overlayManager.isShowing) return

        val rootNode = rootInActiveWindow ?: return
        val currentContext = analyzer.detectContext(rootNode)

        when (currentContext) {
            InstagramContext.SAFE_DM -> {
                // Suntem în mesaje: nu contorizăm nimic și nu blocăm
                return
            }
            InstagramContext.STORIES -> handleStoryContext(rootNode)
            InstagramContext.REELS, InstagramContext.HOME_FEED -> handleScrollContext(event, now, currentContext)
            InstagramContext.OTHER -> Unit
        }
    }

    private fun triggerIntentPrompt(promptNumber: Int) {
        mainHandler.post {
            overlayManager.showIntentPrompt(
                onYesClicked = {
                    cooldownUntilTimestamp = System.currentTimeMillis() + cooldownDurationMs
                    exitInstagramToHome(showModalAfter = false, remainingCooldownMs = 0L)
                },
                onNoClicked = {
                    if (promptNumber == 1) {
                        promptsCompleted = 1
                        mainHandler.removeCallbacks(secondPromptRunnable)
                        mainHandler.postDelayed(secondPromptRunnable, secondPromptDelayMs)
                    } else {
                        promptsCompleted = 2
                    }
                }
            )
        }
    }

    private fun handleStoryContext(rootNode: AccessibilityNodeInfo) {
        val author = analyzer.extractActiveAuthor(rootNode)
        if (author.isNotBlank() && author != lastStoryAuthor) {
            lastStoryAuthor = author
            storiesCount++

            showToast("Stories: $storiesCount/$maxItemsLimit")

            if (storiesCount >= maxItemsLimit) {
                cooldownUntilTimestamp = System.currentTimeMillis() + cooldownDurationMs
                enforceExitWithBlockModal(cooldownDurationMs)
            }
        }
    }

    private fun handleScrollContext(event: AccessibilityEvent, now: Long, context: InstagramContext) {
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED) return

        if (now - lastScrollTimestamp > scrollDebounceMs) {
            lastScrollTimestamp = now
            scrollItemsCount++

            val label = if (context == InstagramContext.REELS) "Reels" else "Feed"
            showToast("$label scroll: $scrollItemsCount/$maxItemsLimit")

            if (scrollItemsCount >= maxItemsLimit) {
                cooldownUntilTimestamp = System.currentTimeMillis() + cooldownDurationMs
                enforceExitWithBlockModal(cooldownDurationMs)
            }
        }
    }

    private fun enforceExitWithBlockModal(remainingCooldownMs: Long) {
        exitInstagramToHome(showModalAfter = true, remainingCooldownMs = remainingCooldownMs)
    }

    private fun exitInstagramToHome(showModalAfter: Boolean, remainingCooldownMs: Long) {
        if (isExiting) return
        isExiting = true
        mainHandler.removeCallbacks(secondPromptRunnable)

        performGlobalAction(GLOBAL_ACTION_BACK)

        mainHandler.postDelayed({
            performGlobalAction(GLOBAL_ACTION_HOME)

            if (showModalAfter) {
                mainHandler.postDelayed({
                    overlayManager.showBlockModal(
                        message = "Enough doomscroll",
                        remainingCooldownMs = remainingCooldownMs,
                        onDismissed = {
                            isExiting = false
                        }
                    )
                    isExiting = false
                }, 150L)
            } else {
                isExiting = false
            }
        }, 120L)
    }

    private fun resetSession() {
        if (!isInInstagramSession && scrollItemsCount == 0 && storiesCount == 0) return

        mainHandler.removeCallbacks(secondPromptRunnable)
        isInInstagramSession = false
        promptsCompleted = 0
        scrollItemsCount = 0
        storiesCount = 0
        lastStoryAuthor = ""

        if (overlayManager.isShowing && System.currentTimeMillis() >= cooldownUntilTimestamp) {
            mainHandler.post { overlayManager.dismiss() }
        }
    }

    private fun showToast(message: String) {
        mainHandler.post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacks(secondPromptRunnable)
        if (::overlayManager.isInitialized) {
            overlayManager.dismiss()
        }
    }

    override fun onInterrupt() {}
}
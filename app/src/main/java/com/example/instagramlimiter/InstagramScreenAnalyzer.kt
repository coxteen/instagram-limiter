package com.example.instagramlimiter

import android.view.accessibility.AccessibilityNodeInfo

enum class InstagramContext {
    SAFE_DM,     // Mesaje / Inbox -> complet permis, fără limite
    STORIES,     // Story viewer activ
    REELS,       // Tab-ul Reels sau Reel deschis full-screen
    HOME_FEED,   // Feed-ul principal (Home)
    OTHER
}

class InstagramScreenAnalyzer {

    fun detectContext(rootNode: AccessibilityNodeInfo?): InstagramContext {
        if (rootNode == null) return InstagramContext.OTHER

        // 1. Prioritate maximă: Verificăm dacă suntem în Direct Messages (Inbox sau Conversație)
        if (isDirectMessagesScreen(rootNode)) {
            return InstagramContext.SAFE_DM
        }

        // 2. Scannăm arborele pentru ecranele de doomscrolling (Stories, Reels, Home Feed)
        return scanDoomscrollContext(rootNode, depth = 0, maxDepth = 6)
    }

    private fun isDirectMessagesScreen(rootNode: AccessibilityNodeInfo): Boolean {
        // ID-uri specifice conversațiilor și inbox-ului DM
        val dmIndicators = listOf(
            "com.instagram.android:id/row_thread_composer_edittext", // Câmpul de scris mesaj
            "com.instagram.android:id/direct_thread_header",         // Header-ul conversației
            "com.instagram.android:id/direct_inbox_recycler_view",   // Lista de conversații (Inbox)
            "com.instagram.android:id/message_list"                  // Lista de mesaje din chat
        )

        for (id in dmIndicators) {
            if (rootNode.findAccessibilityNodeInfosByViewId(id).isNotEmpty()) {
                return true
            }
        }

        // Fallback semantic pentru bara de scriere mesaj (indiferent de versiunea aplicației)
        val editTexts = rootNode.findAccessibilityNodeInfosByText("Message...")
            .ifEmpty { rootNode.findAccessibilityNodeInfosByText("Mesaj...") }
        if (editTexts.isNotEmpty()) {
            return true
        }

        return false
    }

    private fun scanDoomscrollContext(node: AccessibilityNodeInfo, depth: Int, maxDepth: Int): InstagramContext {
        if (depth > maxDepth) return InstagramContext.OTHER

        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val viewId = node.viewIdResourceName?.lowercase() ?: ""

        // A. Stories full-screen (excludem bara de sus din feed 'tray')
        if (!viewId.contains("tray") && (
                viewId.contains("reel_viewer_progress_bar") ||
                viewId.contains("story_viewer") ||
                desc.contains("story progress")
            )
        ) {
            return InstagramContext.STORIES
        }

        // B. Reels full-screen
        if (viewId.contains("clips_video_container") ||
            viewId.contains("clips_viewer_view_pager") ||
            desc.contains("audio original") ||
            desc.contains("original audio")
        ) {
            return InstagramContext.REELS
        }

        // C. Home Feed (bara de navigație jos este vizibilă + suntem pe tab-ul Home selectat)
        if (viewId.contains("feed_tab") && node.isSelected) {
            return InstagramContext.HOME_FEED
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = scanDoomscrollContext(child, depth + 1, maxDepth)
            if (result != InstagramContext.OTHER) {
                return result
            }
        }

        return InstagramContext.OTHER
    }

    fun extractActiveAuthor(rootNode: AccessibilityNodeInfo?): String {
        if (rootNode == null) return ""
        val titleNodes = rootNode.findAccessibilityNodeInfosByViewId("com.instagram.android:id/reel_viewer_title")
        if (titleNodes.isNotEmpty()) {
            return titleNodes[0].text?.toString()?.trim() ?: ""
        }
        return ""
    }
}
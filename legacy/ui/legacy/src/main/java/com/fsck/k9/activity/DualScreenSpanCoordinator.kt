package com.fsck.k9.activity

import android.app.Presentation
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Canvas
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.os.SystemClock
import android.view.Display
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.Window
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.core.view.WindowInsetsControllerCompat
import java.lang.ref.WeakReference

/**
 * Presents one authoritative Activity view as a continuous vertical canvas across the two KEMI displays.
 *
 * Display 2 renders logical Y=0..1279. The Activity stays on Display 0 and its root view is translated so the
 * physical lower display renders logical Y=1280..2559. Touches from Display 2 are dispatched back to the same root
 * view; Display 0 touches are mapped by Android through the root view's translation.
 */
@Suppress("TooManyFunctions")
internal class DualScreenSpanCoordinator(
    private val activity: MessageHomeActivity,
    private val sourceView: View,
    private val geometry: DualScreenSpanGeometry = DualScreenSpanGeometry(),
) : DisplayManager.DisplayListener {
    private val displayManager = activity.getSystemService(DisplayManager::class.java)
    private val originalRequestedOrientation = activity.requestedOrientation
    private val originalLayoutWidth = sourceView.layoutParams.width
    private val originalLayoutHeight = sourceView.layoutParams.height
    private val originalTranslationY = sourceView.translationY

    private var started = false
    private var orientationLocked = false
    private var secondaryPresentation: SpanPresentation? = null

    fun start() {
        if (started) return

        started = true
        displayManager.registerDisplayListener(this, null)
        reconcileDisplays()
    }

    fun stop() {
        if (!started) return

        started = false
        displayManager.unregisterDisplayListener(this)
        deactivateSpanning()
    }

    fun destroy() {
        stop()
        restoreSourceLayout()
        restoreOrientation()
    }

    override fun onDisplayAdded(displayId: Int) = reconcileDisplays()

    override fun onDisplayRemoved(displayId: Int) = reconcileDisplays()

    override fun onDisplayChanged(displayId: Int) = reconcileDisplays()

    @Suppress("ReturnCount")
    private fun reconcileDisplays() {
        if (!started) return

        val secondaryDisplay = findEligibleSecondaryDisplay()
        if (secondaryDisplay == null || activity.display?.displayId != Display.DEFAULT_DISPLAY) {
            deactivateSpanning()
            return
        }

        val currentPresentation = secondaryPresentation
        if (currentPresentation?.display?.displayId == secondaryDisplay.displayId && currentPresentation.isShowing) {
            return
        }

        dismissSecondaryPresentation()
        lockOrientation()

        val candidate = SpanPresentation(
            context = activity,
            display = secondaryDisplay,
            sourceView = sourceView,
            geometry = geometry,
            handleBackPressed = { activity.onBackPressedDispatcher.onBackPressed() },
        )
        candidate.setOnDismissListener {
            if (secondaryPresentation === candidate) {
                secondaryPresentation = null
                restoreSourceLayout()
                restoreOrientation()
                showSystemBars(activity.window)
            }
        }

        try {
            candidate.show()
            secondaryPresentation = candidate
            applySpanningLayout()
            hideSystemBars(activity.window)
            candidate.requestFrame()
        } catch (_: WindowManager.InvalidDisplayException) {
            candidate.setOnDismissListener(null)
            candidate.dismiss()
            restoreSourceLayout()
            restoreOrientation()
            showSystemBars(activity.window)
        }
    }

    private fun findEligibleSecondaryDisplay(): Display? {
        val eligibleDisplays = displayManager.getDisplays(
            DisplayManager.DISPLAY_CATEGORY_PRESENTATION,
        ).filter { display ->
            display.state == Display.STATE_ON &&
                display.displayId != Display.DEFAULT_DISPLAY &&
                geometry.matches(display.mode.physicalWidth, display.mode.physicalHeight)
        }

        return eligibleDisplays.firstOrNull { it.displayId == PREFERRED_SECONDARY_DISPLAY_ID }
            ?: eligibleDisplays.firstOrNull()
    }

    private fun applySpanningLayout() {
        val layoutParams = sourceView.layoutParams ?: return
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        layoutParams.height = geometry.logicalHeight
        sourceView.layoutParams = layoutParams
        sourceView.translationY = geometry.primaryTranslationY
        sourceView.requestLayout()
    }

    private fun deactivateSpanning() {
        dismissSecondaryPresentation()
        restoreSourceLayout()
        restoreOrientation()
        showSystemBars(activity.window)
    }

    private fun dismissSecondaryPresentation() {
        val presentation = secondaryPresentation ?: return
        secondaryPresentation = null
        presentation.setOnDismissListener(null)
        presentation.dismiss()
    }

    private fun restoreSourceLayout() {
        val layoutParams = sourceView.layoutParams ?: return
        layoutParams.width = originalLayoutWidth
        layoutParams.height = originalLayoutHeight
        sourceView.layoutParams = layoutParams
        sourceView.translationY = originalTranslationY
        sourceView.requestLayout()
    }

    private fun lockOrientation() {
        if (orientationLocked) return

        orientationLocked = true
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    private fun restoreOrientation() {
        if (!orientationLocked) return

        orientationLocked = false
        activity.requestedOrientation = originalRequestedOrientation
    }

    private fun hideSystemBars(window: Window) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun showSystemBars(window: Window) {
        WindowCompat.getInsetsController(window, window.decorView).show(systemBars())
    }

    private class SpanPresentation(
        context: Context,
        display: Display,
        private val sourceView: View,
        private val geometry: DualScreenSpanGeometry,
        private val handleBackPressed: () -> Unit,
    ) : Presentation(context, display) {
        private var secondaryViewport: SecondaryViewport? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, false)
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    hide(systemBars())
                    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }

            secondaryViewport = SecondaryViewport(context, sourceView, geometry).also(::setContentView)
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    handleBackPressed.invoke()
                    true
                } else {
                    false
                }
            }
        }

        fun requestFrame() {
            secondaryViewport?.requestCoalescedFrame()
        }
    }

    private class SecondaryViewport(
        context: Context,
        sourceView: View,
        private val geometry: DualScreenSpanGeometry,
    ) : View(context) {
        private val sourceView = WeakReference(sourceView)
        private val bootstrapUntil = SystemClock.uptimeMillis() + BOOTSTRAP_FRAME_WINDOW_MILLIS
        private val sourceDrawListener = ViewTreeObserver.OnDrawListener {
            if (!drawingSource) requestCoalescedFrame()
        }

        private var drawingSource = false
        private var frameScheduled = false

        init {
            setBackgroundColor(Color.WHITE)
            isClickable = true
            isFocusableInTouchMode = true
            sourceView.getViewTreeObserver().addOnDrawListener(sourceDrawListener)
            requestCoalescedFrame()
        }

        fun requestCoalescedFrame() {
            if (frameScheduled) return

            frameScheduled = true
            postInvalidateOnAnimation()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            frameScheduled = false

            val source = sourceView.get() ?: return
            if (source.width == 0 || source.height < geometry.logicalHeight) {
                if (SystemClock.uptimeMillis() < bootstrapUntil) requestCoalescedFrame()
                return
            }

            drawingSource = true
            try {
                source.draw(canvas)
            } finally {
                drawingSource = false
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            val source = sourceView.get() ?: return false
            val logicalEvent = MotionEvent.obtain(event)
            logicalEvent.setLocation(event.x, geometry.secondaryLogicalY(event.y))

            try {
                source.dispatchTouchEvent(logicalEvent)
            } finally {
                logicalEvent.recycle()
            }

            return true
        }

        override fun onDetachedFromWindow() {
            val source = sourceView.get()
            if (source != null && source.viewTreeObserver.isAlive) {
                source.viewTreeObserver.removeOnDrawListener(sourceDrawListener)
            }
            super.onDetachedFromWindow()
        }
    }

    private companion object {
        const val PREFERRED_SECONDARY_DISPLAY_ID = 2
        const val BOOTSTRAP_FRAME_WINDOW_MILLIS = 1_500L
    }
}

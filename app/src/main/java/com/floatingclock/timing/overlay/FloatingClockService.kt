package com.floatingclock.timing.overlay

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.ViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.ViewTreeSavedStateRegistryOwner
import com.floatingclock.timing.data.AppDependencies
import com.floatingclock.timing.ui.theme.FloatingClockTheme

class FloatingClockService : Service(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

    private val controller: FloatingClockController by lazy { AppDependencies.floatingClockController }
    private val windowManager by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }
    private var composeView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    private val viewModelStore = ViewModelStore()

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    override fun getViewModelStore(): ViewModelStore = viewModelStore

    override fun onCreate() {
        super.onCreate()
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        createOverlay()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
        controller.onServiceDestroyed()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        savedStateController.performDetach()
        viewModelStore.clear()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createOverlay() {
        if (composeView != null) return
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 64
            y = 128
        }
        layoutParams = params

        val composeView = ComposeView(this).apply {
            ViewTreeLifecycleOwner.set(this, this@FloatingClockService)
            ViewTreeSavedStateRegistryOwner.set(this, this@FloatingClockService)
            ViewTreeViewModelStoreOwner.set(this, this@FloatingClockService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                FloatingClockTheme {
                    val state by controller.overlayState.collectAsState()
                    FloatingOverlaySurface(
                        state = state,
                        onClose = { controller.hideOverlay(this@FloatingClockService) },
                        onDrag = { dx, dy -> updatePosition(dx, dy) }
                    )
                }
            }
        }
        windowManager.addView(composeView, params)
        this.composeView = composeView
    }

    private fun removeOverlay() {
        composeView?.let { view ->
            windowManager.removeView(view)
            composeView = null
        }
    }

    private fun updatePosition(deltaX: Float, deltaY: Float) {
        val params = layoutParams ?: return
        params.x += deltaX.toInt()
        params.y += deltaY.toInt()
        windowManager.updateViewLayout(composeView, params)
    }

}

package com.floatingclock.timing.overlay

import android.app.Service
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.floatingclock.timing.ui.theme.FloatingClockTheme
import com.floatingclock.timing.data.PreferencesRepository
import com.floatingclock.timing.data.TimeSyncManager

/**
 * A context wrapper that provides lifecycle support for ComposeView in Service
 */
class LifecycleServiceContext(
    base: Context
) : ContextWrapper(base), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    
    override val viewModelStore: ViewModelStore = ViewModelStore()
    
    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    
    private val savedStateController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry
    
    fun onCreate() {
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }
    
    fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()
    }
    
    /**
     * Sets ViewTree owners using reflection to avoid import issues
     */
    fun setViewTreeOwners(view: View) {
        try {
            // Set ViewTreeLifecycleOwner
            val lifecycleOwnerClass = Class.forName("androidx.lifecycle.ViewTreeLifecycleOwner")
            val setLifecycleOwnerMethod = lifecycleOwnerClass.getDeclaredMethod("set", View::class.java, LifecycleOwner::class.java)
            setLifecycleOwnerMethod.invoke(null, view, this)
            
            // Set ViewTreeViewModelStoreOwner
            val viewModelStoreOwnerClass = Class.forName("androidx.lifecycle.ViewTreeViewModelStoreOwner")
            val setViewModelStoreOwnerMethod = viewModelStoreOwnerClass.getDeclaredMethod("set", View::class.java, ViewModelStoreOwner::class.java)
            setViewModelStoreOwnerMethod.invoke(null, view, this)
            
            // Set ViewTreeSavedStateRegistryOwner
            val savedStateRegistryOwnerClass = Class.forName("androidx.savedstate.ViewTreeSavedStateRegistryOwner")
            val setSavedStateRegistryOwnerMethod = savedStateRegistryOwnerClass.getDeclaredMethod("set", View::class.java, SavedStateRegistryOwner::class.java)
            setSavedStateRegistryOwnerMethod.invoke(null, view, this)
        } catch (e: Exception) {
            // If reflection fails, log the error but continue
            android.util.Log.w("FloatingClockService", "Failed to set ViewTree owners via reflection", e)
        }
    }
}

class FloatingClockService : Service() {

    companion object {
        private var isServiceRunning = false
        private var serviceInstance: FloatingClockService? = null
        
        fun isRunning(): Boolean = isServiceRunning
    }

    private val windowManager by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }
    private var overlayView: ViewGroup? = null
    private lateinit var controller: FloatingClockController
    private var layoutParams: WindowManager.LayoutParams? = null
    private lateinit var lifecycleContext: LifecycleServiceContext

    override fun onCreate() {
        super.onCreate()
        
        // Prevent multiple instances
        if (isServiceRunning) {
            stopSelf()
            return
        }
        
        isServiceRunning = true
        serviceInstance = this
        
        // Create lifecycle context
        lifecycleContext = LifecycleServiceContext(this)
        lifecycleContext.onCreate()
        
        // Initialize dependencies
        val preferencesRepository = PreferencesRepository(this)
        val ntpClient = com.floatingclock.timing.data.NtpClient()
        val eventRepository = com.floatingclock.timing.data.EventRepository(applicationContext)
        val timeSyncManager = TimeSyncManager(
            preferencesRepository = preferencesRepository,
            ntpClient = ntpClient,
            eventRepository = eventRepository
        )
        
        controller = FloatingClockController(
            appContext = applicationContext,
            timeSyncManager = timeSyncManager,
            eventRepository = eventRepository,
            preferencesRepository = preferencesRepository
        )
        createOverlay()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
        controller.onServiceDestroyed()
        lifecycleContext.onDestroy()
        
        // Reset service state
        isServiceRunning = false
        serviceInstance = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createOverlay() {
        if (overlayView != null) return
        
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
            // Picture-in-Picture style flags
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16 // Small margin from edge
            y = 100 // Below status bar
            // Set window level to appear above other apps but not system UI
            windowAnimations = android.R.style.Animation_Toast
        }
        layoutParams = params

        // Create a simple FrameLayout container
        val container = FrameLayout(lifecycleContext)
        
        // Set ViewTree owners using reflection
        lifecycleContext.setViewTreeOwners(container)
        
        // Add ComposeView to the container
        val composeView = ComposeView(lifecycleContext).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                FloatingClockContent()
            }
        }
        
        // Also set ViewTree owners for ComposeView
        lifecycleContext.setViewTreeOwners(composeView)
        
        container.addView(
            composeView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        
        try {
            windowManager.addView(container, params)
            overlayView = container
        } catch (e: Exception) {
            android.util.Log.e("FloatingClockService", "Failed to add overlay view", e)
            stopSelf()
        }
    }
    
    @Composable
    private fun FloatingClockContent() {
        FloatingClockTheme {
            val state by controller.overlayState.collectAsState()
            FloatingOverlaySurface(
                state = state,
                onClose = { controller.hideOverlay(this@FloatingClockService) },
                onDrag = { dx, dy -> updatePosition(dx, dy) }
            )
        }
    }

    private fun removeOverlay() {
        overlayView?.let { view ->
            windowManager.removeView(view)
            overlayView = null
        }
    }

    private fun updatePosition(deltaX: Float, deltaY: Float) {
        val params = layoutParams ?: return
        params.x += deltaX.toInt()
        params.y += deltaY.toInt()
        windowManager.updateViewLayout(overlayView, params)
    }
}
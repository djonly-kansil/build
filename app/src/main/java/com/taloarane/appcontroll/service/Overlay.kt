package com.taloarane.appcontroll.service

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Full-screen dim curtain shown while the automation drives the Settings UI,
 * so the user never sees the screens flickering by.
 */
object Overlay {
    private var wm: WindowManager? = null
    private var view: View? = null
    private var title: TextView? = null
    private var subtitle: TextView? = null

    suspend fun show(context: Context) = withContext(Dispatchers.Main) {
        if (view != null) return@withContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            return@withContext
        }
        val manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#F2070A12"))
        }
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                cornerRadius = dp(context, 20f)
                setColor(Color.parseColor("#111A2E"))
                setStroke(dp(context, 1.5f).toInt(), Color.parseColor("#7C4DFF"))
            }
            val p = dp(context, 24f).toInt()
            setPadding(p, p, p, p)
        }
        title = TextView(context).apply {
            setTextColor(Color.parseColor("#E8ECFF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            text = "App Controll"
        }
        subtitle = TextView(context).apply {
            setTextColor(Color.parseColor("#8FA0C8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            text = "..."
        }
        card.addView(title)
        card.addView(subtitle)
        root.addView(card)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_SYSTEM_ALERT

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        runCatching { manager.addView(root, lp) }
        wm = manager
        view = root
    }

    suspend fun update(label: String, index: Int, total: Int) = withContext(Dispatchers.Main) {
        subtitle?.text = if (total > 0) "$index/$total · $label" else label
    }

    suspend fun hide() = withContext(Dispatchers.Main) {
        val v = view ?: return@withContext
        runCatching { wm?.removeView(v) }
        view = null
        title = null
        subtitle = null
    }

    private fun dp(context: Context, value: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, context.resources.displayMetrics)
}

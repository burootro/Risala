package ro.buroot.risala.hook.features

import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.View
import android.view.ViewGroup
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Applies a glass (glassmorphism) look to Google Messages: translucent,
 * blurred backgrounds on the top app bar and conversation cards, using the
 * platform RenderEffect blur on Android 12+.
 *
 * This is purely cosmetic — it re-skins existing views and adds no behaviour.
 */
object GlassUiHook {

    fun install(lpparam: XC_LoadPackage.LoadPackageParam, prefs: XSharedPreferences) {
        val blur = prefs.getInt("glass_blur_strength", 60).coerceIn(0, 100)
        if (blur == 0) return

        // Tint the window background translucent and blur the decor content so
        // conversation surfaces read as frosted glass over the wallpaper.
        val activityClass = XposedHelpers.findClassIfExists(
            "android.app.Activity", lpparam.classLoader
        ) ?: return

        XposedBridge.hookAllMethods(activityClass, "onResume", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val activity = param.thisObject as? android.app.Activity ?: return
                if (activity.javaClass.name.contains("messaging", ignoreCase = true).not() &&
                    activity.packageName != "com.google.android.apps.messaging"
                ) return
                runCatching { applyGlass(activity, blur) }
                    .onFailure { XposedBridge.log("[Risala] Glass apply: ${it.message}") }
            }
        })
    }

    private fun applyGlass(activity: android.app.Activity, blur: Int) {
        val window = activity.window ?: return
        // Let the wallpaper show through behind frosted surfaces.
        window.setBackgroundDrawableResource(android.R.color.transparent)

        val root = window.decorView as? ViewGroup ?: return
        val radius = (blur / 100f) * 40f  // up to 40px blur radius

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && radius > 0f) {
            frostContainers(root, radius)
        } else {
            // Pre-12 fallback: translucent overlay tint only.
            tintContainers(root)
        }
    }

    private fun frostContainers(view: View, radius: Float) {
        if (view is ViewGroup && looksLikeSurface(view)) {
            val effect = RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
            view.setRenderEffect(effect)
            view.setBackgroundColor(Color.argb(90, 255, 255, 255))
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) frostContainers(view.getChildAt(i), radius)
        }
    }

    private fun tintContainers(view: View) {
        if (view is ViewGroup && looksLikeSurface(view)) {
            view.setBackgroundColor(Color.argb(110, 255, 255, 255))
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) tintContainers(view.getChildAt(i))
        }
    }

    private fun looksLikeSurface(vg: ViewGroup): Boolean {
        val id = runCatching { vg.resources.getResourceEntryName(vg.id) }.getOrNull() ?: return false
        return id.contains("toolbar", true) || id.contains("app_bar", true) ||
            id.contains("card", true) || id.contains("conversation", true)
    }
}

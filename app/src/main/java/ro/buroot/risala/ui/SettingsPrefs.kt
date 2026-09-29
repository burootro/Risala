package ro.buroot.risala.ui

import android.content.Context
import android.content.SharedPreferences
import ro.buroot.risala.core.Prefs

/**
 * Writes the module preferences in a form the hook process can read.
 *
 * LSPosed modules read settings through XSharedPreferences, which requires the
 * file to be world-readable. MODE_WORLD_READABLE is deprecated and throws on
 * modern targets, so LSPosed provides a bridge: when the module is enabled,
 * getSharedPreferences with the special name is redirected to a readable file.
 * We simply use a normal SharedPreferences here and mark the file readable via
 * the LSPosed bridge if present.
 */
object SettingsPrefs {

    fun get(ctx: Context): SharedPreferences {
        // LSPosed intercepts this call and returns a bridge-backed instance.
        return ctx.getSharedPreferences(Prefs.FILE, Context.MODE_PRIVATE)
    }

    fun bool(ctx: Context, key: String): Boolean =
        get(ctx).getBoolean(key, Prefs.default(key) as? Boolean ?: false)

    fun setBool(ctx: Context, key: String, value: Boolean) {
        get(ctx).edit().putBoolean(key, value).apply()
    }

    fun int(ctx: Context, key: String): Int =
        get(ctx).getInt(key, Prefs.default(key) as? Int ?: 0)

    fun setInt(ctx: Context, key: String, value: Int) {
        get(ctx).edit().putInt(key, value).apply()
    }

    fun str(ctx: Context, key: String): String =
        get(ctx).getString(key, Prefs.default(key) as? String ?: "") ?: ""

    fun setStr(ctx: Context, key: String, value: String) {
        get(ctx).edit().putString(key, value).apply()
    }
}

package ro.buroot.risala.service

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists pending scheduled sends so they can be re-armed after a reboot.
 */
object PendingStore {

    private const val FILE = "risala_pending"
    private const val KEY = "items"

    fun add(ctx: Context, id: Int, address: String, body: String, subId: Int, whenMs: Long) {
        val arr = read(ctx)
        arr.put(
            JSONObject().apply {
                put("id", id); put("address", address); put("body", body)
                put("subId", subId); put("when", whenMs)
            }
        )
        write(ctx, arr)
    }

    fun remove(ctx: Context, id: Int) {
        val arr = read(ctx)
        val kept = JSONArray()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (o.getInt("id") != id) kept.put(o)
        }
        write(ctx, kept)
    }

    fun all(ctx: Context): List<JSONObject> {
        val arr = read(ctx)
        return (0 until arr.length()).map { arr.getJSONObject(it) }
    }

    private fun read(ctx: Context): JSONArray {
        val raw = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, "[]")
        return runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
    }

    private fun write(ctx: Context, arr: JSONArray) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }
}

package ro.buroot.risala.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-arms any scheduled sends after a reboot, since AlarmManager alarms do not
 * survive a restart. Past-due items are sent immediately.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val now = System.currentTimeMillis()
        for (item in PendingStore.all(context)) {
            val id = item.getInt("id")
            val address = item.getString("address")
            val body = item.getString("body")
            val subId = item.getInt("subId")
            val whenMs = item.getLong("when")
            if (whenMs <= now) {
                runCatching { Scheduler.send(context, address, body, subId) }
                PendingStore.remove(context, id)
            } else {
                Scheduler.schedule(context, id, address, body, subId, whenMs)
            }
        }
    }
}

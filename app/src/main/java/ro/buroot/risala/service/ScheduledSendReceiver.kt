package ro.buroot.risala.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Sends a scheduled message when its alarm fires, then clears it from the
 * pending store.
 */
class ScheduledSendReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val address = intent.getStringExtra("address") ?: return
        val body = intent.getStringExtra("body") ?: return
        val subId = intent.getIntExtra("subId", -1)
        val id = intent.getIntExtra("id", -1)
        runCatching { Scheduler.send(context, address, body, subId) }
        if (id != -1) PendingStore.remove(context, id)
    }
}

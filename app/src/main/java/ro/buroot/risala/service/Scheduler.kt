package ro.buroot.risala.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager

/**
 * Reliable scheduled send. A message is queued with an exact alarm; when it
 * fires, [ScheduledSendReceiver] sends it via SmsManager. Because it rides the
 * system AlarmManager rather than an in-app timer, it survives the app being
 * killed by battery optimisation, and pending sends are re-armed on boot by
 * [BootReceiver].
 */
object Scheduler {

    fun schedule(ctx: Context, id: Int, address: String, body: String, subId: Int, whenMs: Long) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ctx, ScheduledSendReceiver::class.java).apply {
            putExtra("address", address)
            putExtra("body", body)
            putExtra("subId", subId)
            putExtra("id", id)
        }
        val pi = PendingIntent.getBroadcast(
            ctx, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // Exact alarm so the message goes out on time even in Doze.
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, pi)
        PendingStore.add(ctx, id, address, body, subId, whenMs)
    }

    fun cancel(ctx: Context, id: Int) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ctx, ScheduledSendReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            ctx, id, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) am.cancel(pi)
        PendingStore.remove(ctx, id)
    }

    fun send(ctx: Context, address: String, body: String, subId: Int) {
        val sm = if (subId >= 0 && android.os.Build.VERSION.SDK_INT >= 31) {
            ctx.getSystemService(SmsManager::class.java).createForSubscriptionId(subId)
        } else {
            @Suppress("DEPRECATION") SmsManager.getDefault()
        }
        val parts = sm.divideMessage(body)
        sm.sendMultipartTextMessage(address, null, parts, null, null)
    }
}

package ro.buroot.risala.data

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import com.google.gson.GsonBuilder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Reads the system SMS provider and produces backups/exports. Runs in the
 * settings app, which the user grants SMS permission. Everything here is
 * read-only against the provider except [restore], which the user triggers
 * explicitly.
 */
object TelephonyRepo {

    data class Message(
        val id: Long,
        val address: String,
        val body: String,
        val date: Long,
        val type: Int,        // 1 = inbox, 2 = sent
        val read: Boolean,
        val subId: Int
    )

    fun readAll(ctx: Context): List<Message> {
        val out = ArrayList<Message>()
        val cursor = ctx.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE,
                Telephony.Sms.READ,
                Telephony.Sms.SUBSCRIPTION_ID
            ),
            null, null, "${Telephony.Sms.DATE} DESC"
        ) ?: return out

        cursor.use { c ->
            while (c.moveToNext()) {
                out.add(
                    Message(
                        id = c.getLong(0),
                        address = c.getString(1) ?: "",
                        body = c.getString(2) ?: "",
                        date = c.getLong(3),
                        type = c.getInt(4),
                        read = c.getInt(5) == 1,
                        subId = c.getInt(6)
                    )
                )
            }
        }
        return out
    }

    // ---------- Backup (JSON, complete + restorable) ----------

    fun backupToJson(ctx: Context, dest: File): File {
        val gson = GsonBuilder().setPrettyPrinting().create()
        dest.writeText(gson.toJson(readAll(ctx)))
        return dest
    }

    fun restoreFromJson(ctx: Context, src: File): Int {
        val gson = GsonBuilder().create()
        val messages = gson.fromJson(src.readText(), Array<Message>::class.java)
        var inserted = 0
        for (m in messages) {
            val values = android.content.ContentValues().apply {
                put(Telephony.Sms.ADDRESS, m.address)
                put(Telephony.Sms.BODY, m.body)
                put(Telephony.Sms.DATE, m.date)
                put(Telephony.Sms.TYPE, m.type)
                put(Telephony.Sms.READ, if (m.read) 1 else 0)
            }
            val uri: Uri = if (m.type == 2) Telephony.Sms.Sent.CONTENT_URI
            else Telephony.Sms.Inbox.CONTENT_URI
            if (ctx.contentResolver.insert(uri, values) != null) inserted++
        }
        return inserted
    }

    // ---------- Export (human-readable) ----------

    fun exportToHtml(ctx: Context, dest: File): File {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val byThread = readAll(ctx).groupBy { it.address }
        val sb = StringBuilder()
        sb.append(
            """
            <!doctype html><html><head><meta charset="utf-8">
            <title>Risala export</title>
            <style>
              body{font-family:sans-serif;background:#0d0d12;color:#eee;margin:0;padding:16px}
              h2{color:#d4af37;border-bottom:1px solid #333;padding-bottom:6px}
              .msg{max-width:70%;margin:6px 0;padding:8px 12px;border-radius:14px}
              .in{background:#1e1e28}
              .out{background:#2a2320;margin-left:auto;text-align:right}
              .t{font-size:11px;color:#888;margin-top:2px}
            </style></head><body>
            """.trimIndent()
        )
        for ((address, msgs) in byThread) {
            sb.append("<h2>${escape(address)}</h2>")
            for (m in msgs.sortedBy { it.date }) {
                val cls = if (m.type == 2) "out" else "in"
                sb.append("<div class='msg $cls'>${escape(m.body)}")
                sb.append("<div class='t'>${fmt.format(Date(m.date))}</div></div>")
            }
        }
        sb.append("</body></html>")
        dest.writeText(sb.toString())
        return dest
    }

    private fun escape(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    // ---------- Best-effort recovery of deleted rows ----------

    /**
     * Deleted SMS rows are removed from the provider, but on many devices the
     * underlying mmssms.db retains free pages that still contain the text until
     * the database is vacuumed. With root, the settings app can copy the raw DB
     * and scan its free pages for recoverable strings. This is best-effort and
     * makes no guarantee; it only ever reads the user's own database.
     */
    fun recoverDeleted(dbCopy: File): List<String> {
        if (!dbCopy.exists()) return emptyList()
        val bytes = dbCopy.readBytes()
        val text = String(bytes, Charsets.ISO_8859_1)
        // Extract printable runs that look like message bodies.
        val runs = Regex("[\\p{L}\\p{N} .,:;!?@#/\\-]{12,}")
            .findAll(text)
            .map { it.value.trim() }
            .filter { it.length in 12..1000 }
            .distinct()
            .toList()
        return runs
    }
}

package com.hisabh.app.domain.helper

import android.content.Context
import android.provider.Telephony
import com.hisabh.app.domain.parser.HdfcSmsParser
import com.hisabh.app.domain.parser.ParsedTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SmsReaderHelper {

    suspend fun readHdfcSmsFromInbox(
        context: Context,
        sinceTimestamp: Long = 0L
    ): List<ParsedTransaction> = withContext(Dispatchers.IO) {
        val parsedList = mutableListOf<ParsedTransaction>()
        val contentResolver = context.contentResolver

        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        val selection = if (sinceTimestamp > 0) {
            "${Telephony.Sms.DATE} >= ?"
        } else null

        val selectionArgs = if (sinceTimestamp > 0) {
            arrayOf(sinceTimestamp.toString())
        } else null

        val sortOrder = "${Telephony.Sms.DATE} DESC"

        try {
            contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)

                while (cursor.moveToNext()) {
                    val address = cursor.getString(addressIndex)
                    val body = cursor.getString(bodyIndex)
                    val date = cursor.getLong(dateIndex)

                    if (HdfcSmsParser.isHdfcSender(address)) {
                        val parsed = HdfcSmsParser.parse(body, date)
                        if (parsed != null) {
                            parsedList.add(parsed)
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            // READ_SMS permission not granted
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext parsedList
    }
}


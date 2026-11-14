package com.hisabh.app.domain.export

import com.hisabh.app.data.local.entity.TransactionEntity
import com.hisabh.app.util.DateUtils
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.Locale

object CsvExporter {

    fun exportToCsv(
        file: File,
        transactions: List<TransactionEntity>
    ) {
        OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8).use { writer ->
            // RFC 4180 Header
            writer.write("Date,Time,Type,Merchant,Category,Amount,UTR,Note\n")

            for (tx in transactions) {
                val date = DateUtils.formatDate(tx.timestamp)
                val time = DateUtils.formatTime(tx.timestamp)
                val type = tx.txType
                val merchant = escapeCsv(tx.displayName)
                val category = escapeCsv(tx.category)
                val amount = String.format(Locale.US, "%.2f", tx.amount)
                val utr = escapeCsv(tx.utrNumber)
                val note = escapeCsv(tx.note ?: "")

                writer.write("$date,$time,$type,$merchant,$category,$amount,$utr,$note\n")
            }
            writer.flush()
        }
    }

    private fun escapeCsv(value: String): String {
        var str = value.trim()
        if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
            str = str.replace("\"", "\"\"")
            return "\"$str\""
        }
        return str
    }
}


package com.hisabh.app.domain.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.hisabh.app.data.local.entity.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ExportManager {

    suspend fun exportAndShareCsv(
        context: Context,
        monthName: String,
        transactions: List<TransactionEntity>
    ) = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val fileName = "Hisabh_${monthName.replace(" ", "_")}_Transactions.csv"
        val file = File(exportDir, fileName)

        CsvExporter.exportToCsv(file, transactions)

        shareFile(
            context = context,
            file = file,
            mimeType = "text/csv",
            subject = "Hisabh Expense Statement - $monthName"
        )
    }

    suspend fun exportAndSharePdf(
        context: Context,
        monthName: String,
        transactions: List<TransactionEntity>
    ) = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val fileName = "Hisabh_Statement_${monthName.replace(" ", "_")}.pdf"
        val file = File(exportDir, fileName)

        PdfStatementExporter.exportToPdf(file, monthName, transactions)

        shareFile(
            context = context,
            file = file,
            mimeType = "application/pdf",
            subject = "Hisabh Expense Statement - $monthName"
        )
    }

    private fun shareFile(
        context: Context,
        file: File,
        mimeType: String,
        subject: String
    ) {
        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, file)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserIntent = Intent.createChooser(shareIntent, "Share Hisabh Statement").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooserIntent)
    }
}


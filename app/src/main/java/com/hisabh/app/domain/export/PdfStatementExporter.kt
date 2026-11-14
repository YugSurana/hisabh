package com.hisabh.app.domain.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.hisabh.app.data.local.entity.TransactionEntity
import com.hisabh.app.data.model.TransactionType
import com.hisabh.app.util.CurrencyFormatter
import com.hisabh.app.util.DateUtils
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object PdfStatementExporter {

    private const val PAGE_WIDTH = 595 // Standard A4 width in PostScript points (72 dpi)
    private const val PAGE_HEIGHT = 842 // Standard A4 height
    private const val MARGIN = 40f

    fun exportToPdf(
        file: File,
        monthName: String,
        transactions: List<TransactionEntity>
    ) {
        val document = PdfDocument()

        // Pre-calculate statistics
        val totalSpent = transactions.filter { it.txType == TransactionType.DEBIT.name }.sumOf { it.amount }
        val totalReceived = transactions.filter { it.txType == TransactionType.CREDIT.name }.sumOf { it.amount }
        val netBalance = totalReceived - totalSpent

        val categoryTotals = transactions
            .filter { it.txType == TransactionType.DEBIT.name }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }

        // Setup paints
        val primaryColor = Color.parseColor("#163827")
        val secondaryColor = Color.parseColor("#5A6E62")
        val darkTextColor = Color.parseColor("#122118")
        val creamCardColor = Color.parseColor("#F2EFE8")
        val borderColor = Color.parseColor("#EAE5DC")
        val terracottaColor = Color.parseColor("#B43B33")
        val pineColor = Color.parseColor("#1E6845")

        val titlePaint = Paint().apply {
            color = primaryColor
            textSize = 18f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = secondaryColor
            textSize = 9f
            isAntiAlias = true
        }

        val sectionPaint = Paint().apply {
            color = primaryColor
            textSize = 11f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val boldBodyPaint = Paint().apply {
            color = darkTextColor
            textSize = 8.5f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val regularBodyPaint = Paint().apply {
            color = darkTextColor
            textSize = 8.5f
            isAntiAlias = true
        }

        val debitPaint = Paint().apply {
            color = terracottaColor
            textSize = 8.5f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }

        val creditPaint = Paint().apply {
            color = pineColor
            textSize = 8.5f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }

        val linePaint = Paint().apply {
            color = borderColor
            strokeWidth = 0.8f
            isAntiAlias = true
        }

        val fillPaint = Paint().apply {
            isAntiAlias = true
        }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        var y = MARGIN

        // Draw Document Header
        canvas.drawText("HISABH", MARGIN, y + 14f, titlePaint)
        y += 24f

        val subtitleText = "Monthly Expense Statement • $monthName • Generated on ${DateUtils.formatDate(System.currentTimeMillis())}"
        canvas.drawText(subtitleText, MARGIN, y, subtitlePaint)
        y += 16f

        // Top separator
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 14f

        // 1. Draw Summary Cards
        val cardWidth = (PAGE_WIDTH - (MARGIN * 2) - 20f) / 3f
        val cardHeight = 44f

        // Spent Card
        drawMetricCard(
            canvas = canvas,
            x = MARGIN,
            y = y,
            w = cardWidth,
            h = cardHeight,
            label = "TOTAL SPENT",
            value = CurrencyFormatter.formatRupees(totalSpent),
            valColor = terracottaColor,
            bgColor = creamCardColor,
            fillPaint = fillPaint,
            linePaint = linePaint,
            subtitlePaint = subtitlePaint,
            valuePaint = boldBodyPaint
        )

        // Received Card
        drawMetricCard(
            canvas = canvas,
            x = MARGIN + cardWidth + 10f,
            y = y,
            w = cardWidth,
            h = cardHeight,
            label = "TOTAL RECEIVED",
            value = CurrencyFormatter.formatRupees(totalReceived),
            valColor = pineColor,
            bgColor = creamCardColor,
            fillPaint = fillPaint,
            linePaint = linePaint,
            subtitlePaint = subtitlePaint,
            valuePaint = boldBodyPaint
        )

        // Net Balance Card
        drawMetricCard(
            canvas = canvas,
            x = MARGIN + (cardWidth * 2) + 20f,
            y = y,
            w = cardWidth,
            h = cardHeight,
            label = "NET SAVINGS",
            value = CurrencyFormatter.formatRupees(netBalance),
            valColor = if (netBalance >= 0) primaryColor else terracottaColor,
            bgColor = creamCardColor,
            fillPaint = fillPaint,
            linePaint = linePaint,
            subtitlePaint = subtitlePaint,
            valuePaint = boldBodyPaint
        )

        y += cardHeight + 20f

        // 2. Category Summary (Top categories if any)
        if (categoryTotals.isNotEmpty()) {
            canvas.drawText("Category Spending Breakdown", MARGIN, y, sectionPaint)
            y += 12f

            val catColWidth = (PAGE_WIDTH - (MARGIN * 2)) / 2f
            for (i in 0 until minOf(4, categoryTotals.size)) {
                val (catName, catAmount) = categoryTotals[i]
                val pct = if (totalSpent > 0) (catAmount / totalSpent) * 100 else 0.0
                val text = "• $catName: ${CurrencyFormatter.formatRupees(catAmount)} (${String.format(Locale.US, "%.1f", pct)}%)"
                val colX = if (i % 2 == 0) MARGIN else MARGIN + catColWidth
                canvas.drawText(text, colX, y, regularBodyPaint)
                if (i % 2 != 0 || i == categoryTotals.size - 1) {
                    y += 12f
                }
            }
            y += 8f
        }

        // 3. Transactions Table Header
        canvas.drawText("Itemized Transaction Ledger", MARGIN, y, sectionPaint)
        y += 14f

        fun drawTableHeader(c: Canvas, curY: Float) {
            fillPaint.color = primaryColor
            c.drawRect(MARGIN, curY - 11f, PAGE_WIDTH - MARGIN, curY + 6f, fillPaint)

            val headerTextPaint = Paint().apply {
                color = Color.WHITE
                textSize = 8.5f
                isFakeBoldText = true
                isAntiAlias = true
            }

            c.drawText("Date & Time", MARGIN + 6f, curY, headerTextPaint)
            c.drawText("Merchant / Description", MARGIN + 105f, curY, headerTextPaint)
            c.drawText("Category", MARGIN + 265f, curY, headerTextPaint)
            c.drawText("Type", MARGIN + 375f, curY, headerTextPaint)

            headerTextPaint.textAlign = Paint.Align.RIGHT
            c.drawText("Amount (INR)", PAGE_WIDTH - MARGIN - 6f, curY, headerTextPaint)
        }

        drawTableHeader(canvas, y)
        y += 14f

        // 4. Itemized Transaction Rows
        val rowHeight = 16f
        var rowIndex = 0

        for (tx in transactions) {
            // Check if page overflow
            if (y + rowHeight > PAGE_HEIGHT - MARGIN) {
                // Draw footer for current page
                drawPageFooter(canvas, pageNumber, subtitlePaint)
                document.finishPage(page)

                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas

                y = MARGIN + 15f
                drawTableHeader(canvas, y)
                y += 14f
            }

            // Alternating row striping
            if (rowIndex % 2 == 1) {
                fillPaint.color = Color.parseColor("#F9F8F5")
                canvas.drawRect(MARGIN, y - 10f, PAGE_WIDTH - MARGIN, y + 4f, fillPaint)
            }

            // Date & Time
            val dateStr = "${DateUtils.formatDate(tx.timestamp)} ${DateUtils.formatTime(tx.timestamp)}"
            canvas.drawText(dateStr, MARGIN + 6f, y, regularBodyPaint)

            // Merchant (truncate if too long)
            val merchantStr = if (tx.displayName.length > 28) tx.displayName.take(26) + "…" else tx.displayName
            canvas.drawText(merchantStr, MARGIN + 105f, y, boldBodyPaint)

            // Category
            val catStr = if (tx.category.length > 20) tx.category.take(18) + "…" else tx.category
            canvas.drawText(catStr, MARGIN + 265f, y, regularBodyPaint)

            // Type
            canvas.drawText(tx.txType, MARGIN + 375f, y, regularBodyPaint)

            // Amount
            val isDebit = tx.txType == TransactionType.DEBIT.name
            val amtStr = (if (isDebit) "-" else "+") + CurrencyFormatter.formatRupees(tx.amount)
            val amtPaint = if (isDebit) debitPaint else creditPaint
            canvas.drawText(amtStr, PAGE_WIDTH - MARGIN - 6f, y, amtPaint)

            y += rowHeight
            rowIndex++
        }

        // Draw footer on the last page
        drawPageFooter(canvas, pageNumber, subtitlePaint)
        document.finishPage(page)

        // Write to file
        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()
    }

    private fun drawMetricCard(
        canvas: Canvas,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        label: String,
        value: String,
        valColor: Int,
        bgColor: Int,
        fillPaint: Paint,
        linePaint: Paint,
        subtitlePaint: Paint,
        valuePaint: Paint
    ) {
        fillPaint.color = bgColor
        canvas.drawRoundRect(x, y, x + w, y + h, 6f, 6f, fillPaint)
        canvas.drawRoundRect(x, y, x + w, y + h, 6f, 6f, linePaint)

        canvas.drawText(label, x + 8f, y + 14f, subtitlePaint)

        valuePaint.color = valColor
        canvas.drawText(value, x + 8f, y + 32f, valuePaint)
    }

    private fun drawPageFooter(canvas: Canvas, pageNumber: Int, paint: Paint) {
        val footerY = PAGE_HEIGHT - (MARGIN / 2f)
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Hisabh Personal Expense Tracker • Offline & Private", MARGIN, footerY, paint)

        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Page $pageNumber", PAGE_WIDTH - MARGIN, footerY, paint)
    }
}


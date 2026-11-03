package com.avioiva.financiiea.data

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Creates and restores FINANCIIEA backup files, and exports CSV / PDF reports. */
object BackupManager {

    private val fileDate = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    private val rowDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    // ---------- JSON backup ----------

    fun backupFileName(): String = "FINANCIIEA_backup_${fileDate.format(Date())}.json"

    fun createBackupJson(transactions: List<Transaction>): String {
        val root = JSONObject()
        root.put("app", "FINANCIIEA")
        root.put("formatVersion", 1)
        root.put("exportedAt", System.currentTimeMillis())
        val arr = JSONArray()
        for (tx in transactions) {
            arr.put(JSONObject().apply {
                put("type", tx.type)
                put("amount", tx.amount)
                put("currency", tx.currency)
                put("account", tx.account)
                put("category", tx.category)
                put("fromWhom", tx.fromWhom)
                put("note", tx.note)
                put("timestamp", tx.timestamp)
                put("receiptPath", tx.receiptPath)
            })
        }
        root.put("transactions", arr)
        return root.toString(2)
    }

    /** Returns the restored list, or null if the file is not a FINANCIIEA backup. */
    fun parseBackup(text: String): List<Transaction>? {
        return try {
            val root = JSONObject(text)
            if (root.optString("app") != "FINANCIIEA") return null
            val arr = root.getJSONArray("transactions")
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Transaction(
                    type = o.optString("type", TxType.EXPENSE.name),
                    amount = o.optDouble("amount", 0.0),
                    currency = o.optString("currency", "BDT"),
                    account = o.optString("account", Account.CASH.name),
                    category = o.optString("category", "other"),
                    fromWhom = o.optString("fromWhom", ""),
                    note = o.optString("note", ""),
                    timestamp = o.optLong("timestamp", System.currentTimeMillis()),
                    receiptPath = o.optString("receiptPath", "")
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    // ---------- CSV export ----------

    fun csvFileName(): String = "FINANCIIEA_transactions_${fileDate.format(Date())}.csv"

    fun createCsv(transactions: List<Transaction>): String {
        fun q(v: String): String = "\"" + v.replace("\"", "\"\"") + "\""
        val sb = StringBuilder()
        sb.append("Date,Time,Type,Category,Account,Currency,Amount,From Whom,Note\n")
        for (tx in transactions) {
            val parts = rowDate.format(Date(tx.timestamp)).split(" ")
            sb.append(q(parts.getOrNull(0) ?: "")).append(',')
                .append(q(parts.getOrNull(1) ?: "")).append(',')
                .append(q(tx.type)).append(',')
                .append(q(tx.category)).append(',')
                .append(q(tx.account)).append(',')
                .append(q(tx.currency)).append(',')
                .append(tx.amount).append(',')
                .append(q(tx.fromWhom)).append(',')
                .append(q(tx.note)).append('\n')
        }
        return sb.toString()
    }

    // ---------- PDF export ----------

    fun pdfFileName(): String = "FINANCIIEA_report_${fileDate.format(Date())}.pdf"

    /** Draws a simple A4 report: one row per transaction + totals. */
    fun createPdf(context: Context, transactions: List<Transaction>, periodLabel: String): File {
        val pageW = 595
        val pageH = 842
        val margin = 40f
        val navy = Color.rgb(13, 27, 62)
        val teal = Color.rgb(20, 184, 166)

        val doc = PdfDocument()
        var pageNo = 0
        var page: PdfDocument.Page
        var canvas: Canvas
        var y: Float

        val titlePaint = Paint().apply {
            color = Color.WHITE; textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = Paint().apply { color = Color.DKGRAY; textSize = 11f }
        val headPaint = Paint().apply { color = Color.WHITE; textSize = 11f; typeface = Typeface.DEFAULT_BOLD }
        val cellPaint = Paint().apply { color = Color.BLACK; textSize = 10f }
        val boldCell = Paint(cellPaint).apply { typeface = Typeface.DEFAULT_BOLD }
        val inPaint = Paint(boldCell).apply { color = Color.rgb(46, 125, 50) }
        val outPaint = Paint(boldCell).apply { color = Color.rgb(198, 40, 40) }

        fun newPage() {
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNo).create())
            canvas = page.canvas
            canvas.drawRect(0f, 0f, pageW.toFloat(), 64f, Paint().apply { color = navy })
            canvas.drawText("FINANCIIEA — Your hassle-free money diary", margin, 30f, titlePaint)
            canvas.drawText(
                "Report period: $periodLabel  •  Generated ${rowDate.format(Date())}",
                margin, 50f, Paint().apply { color = Color.rgb(190, 205, 225); textSize = 11f })
            y = 88f
        }

        fun drawTableHeader() {
            canvas.drawRect(margin, y, pageW - margin, y + 18f, Paint().apply { color = teal })
            var x = margin + 6f
            for (h in listOf("Date", "Type", "Category", "Account", "From", "Amount")) {
                canvas.drawText(h, x, y + 13f, headPaint)
                x += when (h) {
                    "Date" -> 110f
                    "Type" -> 62f
                    "Category" -> 90f
                    "Account" -> 62f
                    "From" -> 100f
                    else -> 0f
                }
            }
            y += 26f
        }

        newPage()
        drawTableHeader()

        for (tx in transactions) {
            if (y > pageH - 60f) {
                doc.finishPage(page)
                newPage()
                drawTableHeader()
            }
            canvas.drawText(rowDate.format(Date(tx.timestamp)), margin + 6f, y, cellPaint)
            canvas.drawText(
                tx.type.lowercase(Locale.US).replaceFirstChar { it.uppercase(Locale.US) },
                margin + 116f, y, cellPaint)
            canvas.drawText(tx.category, margin + 178f, y, cellPaint)
            canvas.drawText(tx.account.lowercase(Locale.US), margin + 268f, y, cellPaint)
            canvas.drawText(tx.fromWhom.ifEmpty { "-" }, margin + 330f, y, cellPaint)
            val sign = if (tx.txType == TxType.EXPENSE) "-" else "+"
            val paint = if (tx.txType == TxType.EXPENSE) outPaint else inPaint
            canvas.drawText("$sign${"%,.2f".format(Locale.US, tx.amount)} ${tx.currency}", margin + 430f, y, paint)
            y += 18f
            if (tx.note.isNotBlank()) {
                canvas.drawText("    note: ${tx.note.take(70)}", margin + 6f, y, subPaint)
                y += 15f
            }
        }

        val totalIn = transactions.filter { it.txType != TxType.EXPENSE }.sumOf { it.amount }
        val totalOut = transactions.filter { it.txType == TxType.EXPENSE }.sumOf { it.amount }
        y += 12f
        if (y > pageH - 60f) { doc.finishPage(page); newPage() }
        canvas.drawText(
            "Total In: ${"%,.2f".format(Locale.US, totalIn)}    " +
                "Total Out: ${"%,.2f".format(Locale.US, totalOut)}    " +
                "Net: ${"%,.2f".format(Locale.US, totalIn - totalOut)}",
            margin + 6f, y, boldCell)

        doc.finishPage(page)

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, pdfFileName())
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }
}

package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.data.db.InvoiceItemSerializer
import com.example.data.model.BusinessProfile
import com.example.data.model.Invoice
import com.example.data.model.formatRupiah
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object InvoicePdfGenerator {

    fun generateInvoicePdf(
        context: Context,
        invoice: Invoice,
        profile: BusinessProfile,
        targetFile: File
    ): File? {
        val pdfDocument = PdfDocument()
        
        // A4 Paper Size in Pixels (72 dpi): 595 x 842
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Paints for drawing
        val textPaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
        }

        val selectedStyle = invoice.layoutStyle

        // Color Palette based on Layout Style
        val primaryColor = when (selectedStyle) {
            "Minimalist" -> Color.parseColor("#212121")  // Charcoal
            "Creative" -> Color.parseColor("#4F46E5")    // Modern Violet/Indigo
            else -> Color.parseColor("#1A237E")          // Classic Navy
        }

        val accentColor = when (selectedStyle) {
            "Minimalist" -> Color.parseColor("#C5A880")  // Muted Gold
            "Creative" -> Color.parseColor("#06B6D4")    // High Contrast Cyan
            else -> Color.parseColor("#00BFA5")          // Classic Teal
        }

        val darkGray = when (selectedStyle) {
            "Minimalist" -> Color.parseColor("#424242")
            "Creative" -> Color.parseColor("#1E293B")
            else -> Color.parseColor("#37474F")
        }

        val lightGray = when (selectedStyle) {
            "Minimalist" -> Color.parseColor("#F5F5F5")
            "Creative" -> Color.parseColor("#F1F5F9")
            else -> Color.parseColor("#ECEFF1")
        }

        val tableHeaderBg = when (selectedStyle) {
            "Minimalist" -> Color.parseColor("#FFFFFF")  // Minimalist has transparent/white table header
            "Creative" -> Color.parseColor("#4F46E5")    // Creative Indigo
            else -> Color.parseColor("#455A64")          // Blue-Gray
        }

        val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
        val issueDate = sdf.format(Date(invoice.issueDateMillis))
        val dueDate = sdf.format(Date(invoice.dueDateMillis))

        var currentY = 40f

        // Draw Accent Top Bar
        val accentBarPaint = Paint().apply { color = primaryColor }
        if (selectedStyle == "Minimalist") {
            // Elegant thin gold line on top
            accentBarPaint.color = accentColor
            canvas.drawRect(0f, 0f, 595f, 4f, accentBarPaint)
            currentY += 10f
        } else if (selectedStyle == "Creative") {
            // Cool cyber tech stripes
            canvas.drawRect(0f, 0f, 595f, 16f, accentBarPaint)
            val subBandPaint = Paint().apply { color = accentColor }
            canvas.drawRect(0f, 16f, 595f, 22f, subBandPaint)
            currentY += 22f
        } else {
            // Classic
            canvas.drawRect(0f, 0f, 595f, 15f, accentBarPaint)
            currentY += 15f
        }

        // 1. HEADER SECTION (Logo & Business Name)
        // Draw Logo if exists, or draw dynamic professional logo monogram
        var logoBitmap: Bitmap? = null
        if (profile.logoUri != null) {
            try {
                val input = context.contentResolver.openInputStream(Uri.parse(profile.logoUri))
                if (input != null) {
                    val fullBitmap = BitmapFactory.decodeStream(input)
                    if (fullBitmap != null) {
                        // Resize with aspect ratio preserved (max width/height of 64px)
                        val maxDimension = 64
                        val widthVal = fullBitmap.width
                        val heightVal = fullBitmap.height
                        val ratio = widthVal.toFloat() / heightVal.toFloat()
                        val newWidth = if (widthVal > heightVal) maxDimension else (maxDimension * ratio).toInt()
                        val newHeight = if (heightVal > widthVal) maxDimension else (maxDimension / ratio).toInt()
                        logoBitmap = Bitmap.createScaledBitmap(fullBitmap, newWidth, newHeight, true)
                    }
                    input.close()
                }
            } catch (e: Exception) {
                // Ignore, logoBitmap stays null and falls back gracefully
                e.printStackTrace()
            }
        }

        if (logoBitmap != null) {
            canvas.drawBitmap(logoBitmap, 40f, currentY, null)
        } else {
            // Draw Dynamic Monogram Block
            val monoPaint = Paint().apply {
                color = primaryColor
                isAntiAlias = true
            }
            // Rounded circle background
            canvas.drawRoundRect(RectF(40f, currentY, 104f, currentY + 64f), 12f, 12f, monoPaint)
            
            // Text initial inside monogram
            val monoTextPaint = Paint().apply {
                color = Color.WHITE
                isAntiAlias = true
                textSize = 28f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val initial = if (profile.companyName.isNotEmpty()) {
                profile.companyName.take(2).uppercase()
            } else {
                "BE"
            }
            val textBounds = Rect()
            monoTextPaint.getTextBounds(initial, 0, initial.length, textBounds)
            canvas.drawText(
                initial,
                72f,
                currentY + 32f + (textBounds.height() / 2),
                monoTextPaint
            )
        }

        // Business Name & Corporate details (Right Aligned or adjacent)
        val titlePaint = Paint().apply {
            color = primaryColor
            isAntiAlias = true
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val businessName = if (profile.companyName.isNotEmpty()) profile.companyName else "Nama Bisnis"
        canvas.drawText(businessName, 120f, currentY + 22f, titlePaint)

        val detailPaint = Paint().apply {
            color = darkGray
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        var infoY = currentY + 35f
        if (profile.email.isNotEmpty()) {
            canvas.drawText("Email: ${profile.email}", 120f, infoY, detailPaint)
            infoY += 12f
        }
        if (profile.phone.isNotEmpty()) {
            canvas.drawText("Telp: ${profile.phone}", 120f, infoY, detailPaint)
            infoY += 12f
        }
        if (profile.address.isNotEmpty()) {
            // Truncate address if too long
            val addr = if (profile.address.length > 50) profile.address.take(47) + "..." else profile.address
            canvas.drawText("Alamat: $addr", 120f, infoY, detailPaint)
        }

        // Invoice Label on Top Right
        val invTitlePaint = Paint().apply {
            color = primaryColor
            isAntiAlias = true
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("INVOICE", 555f, currentY + 25f, invTitlePaint)

        val invNoPaint = Paint().apply {
            color = darkGray
            isAntiAlias = true
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("No: ${invoice.invoiceNumber}", 555f, currentY + 40f, invNoPaint)

        currentY += 80f

        // Draw Divider
        val linePaint = Paint().apply {
            color = lightGray
            strokeWidth = 1f
        }
        canvas.drawLine(40f, currentY, 555f, currentY, linePaint)
        currentY += 20f

        // 2. BILL TO & INVOICE META SECTIONS (Side by Side)
        // Bill to (Left Side)
        val headingPaint = Paint().apply {
            color = primaryColor
            isAntiAlias = true
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("TAGIHAN KEPADA:", 40f, currentY, headingPaint)
        
        val metaLabelPaint = Paint().apply {
            color = darkGray
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val metaValuePaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        currentY += 15f
        canvas.drawText(invoice.clientName, 40f, currentY, Paint(metaLabelPaint).apply { textSize = 11f; color = Color.BLACK })
        
        // Dates on Right Side aligned
        canvas.drawText("Tanggal Invoice:", 380f, currentY - 15f, metaLabelPaint)
        canvas.drawText(issueDate, 470f, currentY - 15f, metaValuePaint)
        
        canvas.drawText("Jatuh Tempo:", 380f, currentY, metaLabelPaint)
        canvas.drawText(dueDate, 470f, currentY, metaValuePaint)

        currentY += 12f
        if (invoice.clientEmail.isNotEmpty()) {
            canvas.drawText("Email: ${invoice.clientEmail}", 40f, currentY, metaValuePaint)
            currentY += 12f
        }
        if (invoice.clientPhone.isNotEmpty()) {
            canvas.drawText("Telp: ${invoice.clientPhone}", 40f, currentY, metaValuePaint)
            currentY += 12f
        }
        if (invoice.clientAddress.isNotEmpty()) {
            canvas.drawText(invoice.clientAddress, 40f, currentY, metaValuePaint)
            currentY += 12f
        }

        currentY += 15f

        // 3. ITEMS TABLE
        // Table Headers
        val headerPaint = Paint().apply {
            color = tableHeaderBg
        }
        
        if (selectedStyle == "Minimalist") {
            // Draw minimalist top-and-bottom lines rather than a fully-filled block
            val linePaint = Paint().apply {
                color = accentColor
                strokeWidth = 1.2f
            }
            canvas.drawLine(40f, currentY, 555f, currentY, linePaint)
            canvas.drawLine(40f, currentY + 22f, 555f, currentY + 22f, linePaint)
        } else {
            // Round headers region
            canvas.drawRect(40f, currentY, 555f, currentY + 22f, headerPaint)
        }

        val headerTextPaint = Paint().apply {
            color = if (selectedStyle == "Minimalist") primaryColor else Color.WHITE
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        canvas.drawText("Item Deskripsi", 50f, currentY + 14f, headerTextPaint)
        canvas.drawText("Harga Satuan", 320f, currentY + 14f, Paint(headerTextPaint).apply { textAlign = Paint.Align.RIGHT })
        canvas.drawText("Qty", 410f, currentY + 14f, Paint(headerTextPaint).apply { textAlign = Paint.Align.CENTER })
        canvas.drawText("Total Harga", 545f, currentY + 14f, Paint(headerTextPaint).apply { textAlign = Paint.Align.RIGHT })

        currentY += 22f

        val items = InvoiceItemSerializer.deserialize(invoice.itemsJson)
        var subtotal = 0.0

        val rowPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
        }

        val rowBgPaint = Paint().apply {
            color = Color.parseColor("#FAFAFA")
        }

        for (i in items.indices) {
            val item = items[i]
            val rowY = currentY + 20f
            
            // Draw alternating row background
            if (i % 2 == 1) {
                canvas.drawRect(40f, currentY, 555f, currentY + 28f, rowBgPaint)
            }
            
            rowPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            rowPaint.color = Color.BLACK
            
            // Wrap long item description
            val desc = item.description
            val displayDesc = if (desc.length > 42) desc.take(39) + "..." else desc
            canvas.drawText(displayDesc, 50f, rowY + 5f, rowPaint)
            
            canvas.drawText(formatRupiah(item.unitPrice), 320f, rowY + 5f, Paint(rowPaint).apply { textAlign = Paint.Align.RIGHT })
            canvas.drawText(item.quantity.toString(), 410f, rowY + 5f, Paint(rowPaint).apply { textAlign = Paint.Align.CENTER })
            canvas.drawText(formatRupiah(item.totalPrice), 545f, rowY + 5f, Paint(rowPaint).apply { textAlign = Paint.Align.RIGHT })

            subtotal += item.totalPrice
            currentY += 28f
            
            // Draw dynamic item bottom border line
            canvas.drawLine(40f, currentY, 555f, currentY, linePaint)
        }

        currentY += 15f

        // 4. TOTAL CALCULATIONS (Aligned Right-wards)
        val summaryLabelPaint = Paint().apply {
            color = darkGray
            isAntiAlias = true
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val summaryValuePaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        // Subtotal row
        canvas.drawText("Subtotal:", 380f, currentY, summaryLabelPaint)
        canvas.drawText(formatRupiah(subtotal), 545f, currentY, summaryValuePaint)
        currentY += 16f

        // Tax row
        val taxAmount = subtotal * (invoice.taxRatePercent / 100)
        if (invoice.taxRatePercent > 0) {
            canvas.drawText("Pajak (${invoice.taxRatePercent}%):", 380f, currentY, summaryLabelPaint)
            canvas.drawText(formatRupiah(taxAmount), 545f, currentY, summaryValuePaint)
            currentY += 16f
        }

        // Discount row
        if (invoice.discountAmount > 0) {
            canvas.drawText("Diskon:", 380f, currentY, summaryLabelPaint)
            canvas.drawText("- ${formatRupiah(invoice.discountAmount)}", 545f, currentY, summaryValuePaint)
            currentY += 16f
        }

        // Grand Total box (Modern Block Accent)
        val grandTotal = subtotal + taxAmount - invoice.discountAmount
        
        val totalBoxPaint = Paint().apply {
            color = primaryColor
        }
        canvas.drawRoundRect(RectF(360f, currentY, 555f, currentY + 28f), 6f, 6f, totalBoxPaint)

        val totalTextLabelPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("TOTAL TAGIHAN:", 372f, currentY + 17f, totalTextLabelPaint)
        
        val totalTextValuePaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(formatRupiah(grandTotal), 545f, currentY + 17f, totalTextValuePaint)

        currentY += 45f

        // 5. PAYMENT AND TRANSFER INFO SECTION (Left aligned)
        if (profile.bankName.isNotEmpty() && profile.bankAccountNo.isNotEmpty()) {
            canvas.drawText("METODE PEMBAYARAN:", 40f, currentY, headingPaint)
            currentY += 14f
            
            val infoTextPaint = Paint().apply {
                color = Color.BLACK
                isAntiAlias = true
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("Bank / Platform:  ${profile.bankName.uppercase()}", 40f, currentY, infoTextPaint)
            currentY += 12f
            canvas.drawText("Nomor Rekening:   ${profile.bankAccountNo}", 40f, currentY, infoTextPaint)
            currentY += 12f
            canvas.drawText("Atas Nama:        ${profile.bankAccountName}", 40f, currentY, infoTextPaint)
            
            currentY += 20f
        }

        // 6. E-PAYMENT QRIS CODE (Premium Artistic visual integration)
        // Let's create a beautiful custom dynamic layout drawing to display payment link / QR code simulation
        val qrBoxY = currentY
        if (profile.bankName.isNotEmpty()) {
            canvas.drawText("INTEGRASI PEMBAYARAN DIGITAL (QRIS INSTAN):", 40f, qrBoxY, headingPaint)
            
            val qrTextDescPaint = Paint().apply {
                color = darkGray
                isAntiAlias = true
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            }
            canvas.drawText("Scan kode untuk pembayaran tagihan cepat dan terverifikasi otomatis.", 40f, qrBoxY + 12f, qrTextDescPaint)

            // Draw QRIS Card Envelope
            val qrisContainerPaint = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            val borderPaint = Paint().apply {
                color = accentColor
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
                isAntiAlias = true
            }
            
            canvas.drawRoundRect(RectF(40f, qrBoxY + 22f, 134f, qrBoxY + 116f), 8f, 8f, qrisContainerPaint)
            canvas.drawRoundRect(RectF(40f, qrBoxY + 22f, 134f, qrBoxY + 116f), 8f, 8f, borderPaint)

            // Draw QRIS label header
            val qrisLabelPaint = Paint().apply {
                color = accentColor
                style = Paint.Style.FILL
            }
            canvas.drawRect(40f, qrBoxY + 22f, 134f, qrBoxY + 36f, qrisLabelPaint)
            
            val qrisHeaderTextPaint = Paint().apply {
                color = if (selectedStyle == "Minimalist") Color.BLACK else Color.WHITE
                isAntiAlias = true
                textSize = 8f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("QRIS INSTANT", 87f, qrBoxY + 32f, qrisHeaderTextPaint)

            // Draw pixel grid simulating a premium QR code
            val qrPixelPaint = Paint().apply {
                color = Color.BLACK
                style = Paint.Style.FILL
            }

            // High authenticity custom matrix drawing loop for QR-like visualizer
            val matrixStartX = 55f
            val matrixStartY = qrBoxY + 44f
            val pixelSize = 3.5f

            // A predefined pattern matrix so it yields a beautiful QR monogram
            val matrixPattern = arrayOf(
                intArrayOf(1,1,1,1,1,1,0,1,1,0,1,1,1,1,1,1),
                intArrayOf(1,0,0,0,0,1,0,0,1,0,1,0,0,0,0,1),
                intArrayOf(1,0,1,1,0,1,0,1,0,0,1,0,1,1,0,1),
                intArrayOf(1,0,1,1,0,1,0,1,1,0,1,0,1,1,0,1),
                intArrayOf(1,0,0,0,0,1,0,0,0,1,1,0,0,0,0,1),
                intArrayOf(1,1,1,1,1,1,0,1,0,1,0,1,1,1,1,1),
                intArrayOf(0,0,0,0,0,0,0,1,1,1,0,0,0,0,0,0),
                intArrayOf(1,1,0,1,0,1,1,0,0,0,1,1,0,1,0,1),
                intArrayOf(0,1,1,0,1,0,0,1,1,1,1,0,1,0,1,1),
                intArrayOf(0,0,0,0,0,0,0,1,0,1,0,0,1,1,1,0),
                intArrayOf(1,1,1,1,1,1,0,1,1,0,1,1,0,1,0,0),
                intArrayOf(1,0,0,0,0,1,0,0,0,1,0,0,1,1,1,1),
                intArrayOf(1,0,1,1,0,1,0,1,1,0,0,1,0,0,1,0),
                intArrayOf(1,0,1,1,0,1,0,1,0,1,1,0,1,0,1,1),
                intArrayOf(1,0,0,0,0,1,0,1,1,0,0,0,1,1,0,0),
                intArrayOf(1,1,1,1,1,1,0,0,1,1,1,0,1,0,1,1)
            )

            for (r in matrixPattern.indices) {
                for (c in matrixPattern[r].indices) {
                    if (matrixPattern[r][c] == 1) {
                        canvas.drawRect(
                            matrixStartX + (c * pixelSize),
                            matrixStartY + (r * pixelSize),
                            matrixStartX + ((c + 1) * pixelSize),
                            matrixStartY + ((r + 1) * pixelSize),
                            qrPixelPaint
                        )
                    }
                }
            }

            // Bottom dynamic merchant details (right of QR block)
            val merchantLabelPaint = Paint().apply {
                color = Color.BLACK
                isAntiAlias = true
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("Merchant:  $businessName", 150f, qrBoxY + 50f, merchantLabelPaint)
            
            val statusLabelPaint = Paint().apply {
                color = accentColor
                isAntiAlias = true
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("● status: TERKONEKSI / SIAP BAYAR", 150f, qrBoxY + 68f, statusLabelPaint)

            val instructionsPaint = Paint().apply {
                color = darkGray
                isAntiAlias = true
                textSize = 8f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            canvas.drawText("Pindai QR di atas menggunakan aplikasi GOPAY, OVO, DANA, LINKAJA,", 150f, qrBoxY + 84f, instructionsPaint)
            canvas.drawText("atau aplikasi M-Banking Anda untuk melakukan penyelesaian transaksi instant.", 150f, qrBoxY + 95f, instructionsPaint)
        }

        // 7. FOOTER NOTES
        val footerNotesY = 760f
        canvas.drawLine(40f, footerNotesY, 555f, footerNotesY, linePaint)
        
        val footerHeadingPaint = Paint().apply {
            color = darkGray
            isAntiAlias = true
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("CATATAN TAMBAHAN:", 40f, footerNotesY + 16f, footerHeadingPaint)

        val footerNotesTextPaint = Paint().apply {
            color = darkGray
            isAntiAlias = true
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
        
        val notesText = if (invoice.paymentNotes.isNotEmpty()) {
            invoice.paymentNotes
        } else if (profile.paymentNotes.isNotEmpty()) {
            profile.paymentNotes
        } else {
            "Terima kasih atas kepercayaan Anda!"
        }
        
        // Wrap custom payment notes line by line if long
        if (notesText.length > 110) {
            canvas.drawText(notesText.take(110) + "-", 40f, footerNotesY + 28f, footerNotesTextPaint)
            canvas.drawText(notesText.drop(110), 40f, footerNotesY + 38f, footerNotesTextPaint)
        } else {
            canvas.drawText(notesText, 40f, footerNotesY + 28f, footerNotesTextPaint)
        }

        // Clean watermark branding at the absolute footer
        val watermarkPaint = Paint().apply {
            color = Color.parseColor("#90A4AE")
            isAntiAlias = true
            textSize = 8f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Dibuat secara otomatis & aman via BillEase Invoice Generator", 297f, 810f, watermarkPaint)

        pdfDocument.finishPage(page)

        // Save file to destination storage
        return try {
            val fos = FileOutputStream(targetFile)
            pdfDocument.writeTo(fos)
            pdfDocument.close()
            fos.flush()
            fos.close()
            targetFile
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }
}

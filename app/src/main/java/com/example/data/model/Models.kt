package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.NumberFormat
import java.util.Locale

@Entity(tableName = "business_profiles")
data class BusinessProfile(
    @PrimaryKey val id: Int = 1,
    val companyName: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val taxId: String = "",
    val bankName: String = "",
    val bankAccountNo: String = "",
    val bankAccountName: String = "",
    val logoUri: String? = null,
    val paymentNotes: String = "Mohon pembayaran ditransfer sesuai instruksi di atas. Terima kasih atas kerja samanya.",
    val firestoreProjectId: String = "billease-invoice-demo",
    val firestoreApiKey: String = "",
    val firestoreCollection: String = "clients",
    val useFirestore: Boolean = false
)

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val firestoreDocId: String? = null
)

@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String = "",
    val issueDateMillis: Long = System.currentTimeMillis(),
    val dueDateMillis: Long = System.currentTimeMillis() + 7 * 24 * 3600 * 1000L,
    val clientName: String = "",
    val clientEmail: String = "",
    val clientPhone: String = "",
    val clientAddress: String = "",
    val itemsJson: String = "", // serialized array of InvoiceItem
    val taxRatePercent: Double = 0.0,
    val discountAmount: Double = 0.0,
    val isPaid: Boolean = false,
    val paymentNotes: String = "",
    val layoutStyle: String = "Classic"
)

data class InvoiceItem(
    val description: String = "",
    val unitPrice: Double = 0.0,
    val quantity: Int = 1
) {
    val totalPrice: Double
        get() = unitPrice * quantity
}

// Utility functions for calculation and formatting
fun formatRupiah(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
    return formatter.format(amount).replace("Rp", "Rp ").replace(",00", "")
}

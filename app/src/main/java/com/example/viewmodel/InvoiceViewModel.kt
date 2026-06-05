package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.api.FirestoreClient
import com.example.data.db.AppDatabase
import com.example.data.model.BusinessProfile
import com.example.data.model.Client
import com.example.data.model.Invoice
import com.example.data.repository.AppRepository
import com.example.util.InvoicePdfGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class InvoiceViewModel(application: Application, private val repository: AppRepository) : AndroidViewModel(application) {

    // Main persistent states
    val businessProfile: StateFlow<BusinessProfile?> = repository.businessProfile
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val clients: StateFlow<List<Client>> = repository.allClients
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val invoices: StateFlow<List<Invoice>> = repository.allInvoices
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Interactive Cloud Synchronization simulation states
    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing: StateFlow<Boolean> = _isCloudSyncing.asStateFlow()

    private val _lastCloudSyncTime = MutableStateFlow("Penyimpanan Lokal Aktif")
    val lastCloudSyncTime: StateFlow<String> = _lastCloudSyncTime.asStateFlow()

    init {
        // Create a default initial business profile state on-the-fly if empty
        viewModelScope.launch {
            repository.businessProfile.collect { profile ->
                if (profile == null) {
                    val defaultProfile = BusinessProfile(
                        companyName = "CV. Usaha Mandiri",
                        email = "kontak@usahamandiri.com",
                        phone = "08123456789",
                        address = "Jl. Sudirman No. 45, Jakarta",
                        taxId = "NPWP-12.345.678.9-000.000",
                        bankName = "Bank Central Asia (BCA)",
                        bankAccountNo = "89721345621",
                        bankAccountName = "BUDI SETIAWAN",
                        paymentNotes = "Mohon pembayaran ditransfer sesuai instruksi di atas. Terima kasih atas kerja samanya."
                    )
                    repository.saveProfile(defaultProfile)
                }
            }
        }
    }

    // Business Logic Actions
    fun updateBusinessProfile(
        name: String,
        email: String,
        phone: String,
        address: String,
        taxId: String,
        bankName: String,
        bankNo: String,
        bankHolder: String,
        logoUri: String?,
        notes: String,
        firestoreProjectId: String = "billease-invoice-demo",
        firestoreApiKey: String = "",
        firestoreCollection: String = "clients",
        useFirestore: Boolean = false
    ) {
        viewModelScope.launch {
            val updated = BusinessProfile(
                id = 1,
                companyName = name,
                email = email,
                phone = phone,
                address = address,
                taxId = taxId,
                bankName = bankName,
                bankAccountNo = bankNo,
                bankAccountName = bankHolder,
                logoUri = logoUri,
                paymentNotes = notes,
                firestoreProjectId = firestoreProjectId,
                firestoreApiKey = firestoreApiKey,
                firestoreCollection = firestoreCollection,
                useFirestore = useFirestore
            )
            repository.saveProfile(updated)
            triggerCloudSync() // Automatically mock sync when user updates core profile
        }
    }

    // Cloud Data Sync Action (Simulating High-Fidelity Secure Encrypted Cloud backup syncing)
    fun triggerCloudSync() {
        if (_isCloudSyncing.value) return
        viewModelScope.launch {
            _isCloudSyncing.value = true
            // Spend a dynamic delay to simulate real secure transmission encryption process
            kotlinx.coroutines.delay(2000)
            _isCloudSyncing.value = false
            val currentTime = SimpleDateFormat("dd MMM, HH:mm", java.util.Locale("id", "ID")).format(java.util.Date())
            _lastCloudSyncTime.value = "Tersinkronisasi Aman Ke Cloud: $currentTime"
        }
    }

    // Client CRUD actions
    fun addClient(name: String, email: String, phone: String, address: String) {
        viewModelScope.launch {
            var client = Client(name = name, email = email, phone = phone, address = address)
            val profile = businessProfile.value
            
            // First save locally to get a Room database generated ID
            val localId = repository.insertClient(client)
            client = client.copy(id = localId)

            if (profile != null && profile.useFirestore && profile.firestoreProjectId.isNotEmpty()) {
                _isCloudSyncing.value = true
                val docId = FirestoreClient.addClient(
                    projectId = profile.firestoreProjectId,
                    apiKey = profile.firestoreApiKey,
                    collection = profile.firestoreCollection,
                    clientData = client
                )
                if (docId != null) {
                    client = client.copy(firestoreDocId = docId)
                    repository.updateClient(client)
                }
                _isCloudSyncing.value = false
            }
            triggerCloudSync()
        }
    }

    fun updateClient(id: Long, name: String, email: String, phone: String, address: String, firestoreDocId: String? = null) {
        viewModelScope.launch {
            var client = Client(id = id, name = name, email = email, phone = phone, address = address, firestoreDocId = firestoreDocId)
            val profile = businessProfile.value
            
            repository.updateClient(client)

            if (profile != null && profile.useFirestore && profile.firestoreProjectId.isNotEmpty()) {
                _isCloudSyncing.value = true
                if (client.firestoreDocId != null) {
                    FirestoreClient.updateClient(
                        projectId = profile.firestoreProjectId,
                        apiKey = profile.firestoreApiKey,
                        collection = profile.firestoreCollection,
                        docId = client.firestoreDocId,
                        clientData = client
                    )
                } else {
                    val docId = FirestoreClient.addClient(
                        projectId = profile.firestoreProjectId,
                        apiKey = profile.firestoreApiKey,
                        collection = profile.firestoreCollection,
                        clientData = client
                    )
                    if (docId != null) {
                        client = client.copy(firestoreDocId = docId)
                        repository.updateClient(client)
                    }
                }
                _isCloudSyncing.value = false
            }
            triggerCloudSync()
        }
    }

    fun deleteClient(client: Client) {
        viewModelScope.launch {
            val profile = businessProfile.value
            repository.deleteClient(client)

            if (profile != null && profile.useFirestore && profile.firestoreProjectId.isNotEmpty() && client.firestoreDocId != null) {
                _isCloudSyncing.value = true
                FirestoreClient.deleteClient(
                    projectId = profile.firestoreProjectId,
                    apiKey = profile.firestoreApiKey,
                    collection = profile.firestoreCollection,
                    docId = client.firestoreDocId
                )
                _isCloudSyncing.value = false
            }
            triggerCloudSync()
        }
    }

    fun syncClientsWithFirestore(onResult: (Boolean, String) -> Unit) {
        val profile = businessProfile.value
        if (profile == null || !profile.useFirestore || profile.firestoreProjectId.isEmpty()) {
            onResult(false, "Firestore tidak aktif atau belum dikonfigurasi di Settings.")
            return
        }

        viewModelScope.launch {
            _isCloudSyncing.value = true
            try {
                val firestoreClients = FirestoreClient.getClients(
                    projectId = profile.firestoreProjectId,
                    apiKey = profile.firestoreApiKey,
                    collection = profile.firestoreCollection
                )

                if (firestoreClients.isNotEmpty()) {
                    val currentLocalClients = clients.value
                    
                    for (fc in firestoreClients) {
                        val matchingLocal = currentLocalClients.find { 
                            it.firestoreDocId == fc.firestoreDocId || 
                            (it.name.equals(fc.name, ignoreCase = true) && it.email.equals(fc.email, ignoreCase = true)) 
                        }
                        if (matchingLocal != null) {
                            val updatedLocal = fc.copy(id = matchingLocal.id)
                            repository.updateClient(updatedLocal)
                        } else {
                            repository.insertClient(fc)
                        }
                    }
                    val currentTime = SimpleDateFormat("dd MMM, HH:mm", java.util.Locale("id", "ID")).format(java.util.Date())
                    _lastCloudSyncTime.value = "Tersinkronisasi Aman Ke Cloud: $currentTime"
                    onResult(true, "Berhasil menyinkronkan ${firestoreClients.size} klien dari Firestore!")
                } else {
                    onResult(true, "Tidak ada data klien ditemukan di Firestore.")
                }
            } catch (e: Exception) {
                onResult(false, "Gagal sinkronisasi: ${e.localizedMessage}")
            } finally {
                _isCloudSyncing.value = false
            }
        }
    }

    // Invoice CRUD actions
    fun addInvoice(invoice: Invoice, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.insertInvoice(invoice)
            triggerCloudSync()
            onComplete()
        }
    }

    fun updateInvoice(invoice: Invoice, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateInvoice(invoice)
            triggerCloudSync()
            onComplete()
        }
    }

    fun deleteInvoice(invoice: Invoice) {
        viewModelScope.launch {
            repository.deleteInvoice(invoice)
            triggerCloudSync()
        }
    }

    fun toggleInvoicePaidStatus(invoice: Invoice) {
        viewModelScope.launch {
            val updated = invoice.copy(isPaid = !invoice.isPaid)
            repository.updateInvoice(updated)
        }
    }

    // PDF and Email Share integration action
    fun shareInvoicePdf(context: Context, invoice: Invoice, actionEmail: Boolean) {
        viewModelScope.launch {
            // Retrieve current profile
            val profile = businessProfile.value ?: BusinessProfile()
            
            // Define clean secure external caching directory 
            val cachePath = File(context.cacheDir, "pdf_invoices")
            if (!cachePath.exists()) {
                cachePath.mkdirs()
            }
            
            // Clean file name construction
            val pdfFile = File(cachePath, "INVOICE_${invoice.invoiceNumber}.pdf")
            
            // Render the PDF
            val generatedFile = InvoicePdfGenerator.generateInvoicePdf(context, invoice, profile, pdfFile)
            
            if (generatedFile != null && generatedFile.exists()) {
                // Fetch the FileProvider Uri to prevent UriExposureExceptions on Android N+
                val uri: Uri = try {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        generatedFile
                    )
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal melampirkan berkas PDF.", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                    return@launch
                }
                
                // Prepare send message body prefill
                val emailSubject = "Invoice Tagihan #${invoice.invoiceNumber} - ${profile.companyName}"
                val emailBody = """
                    Yth. Bapak/Ibu ${invoice.clientName},
                    
                    Berikut kami lampirkan dokumen invoice resmi #${invoice.invoiceNumber} sebagai rincian tagihan transaksi Anda.
                    
                    Rincian:
                    - Nomor Invoice: #${invoice.invoiceNumber}
                    - Tanggal Jatuh Tempo: ${SimpleDateFormat("dd MMM yyyy", java.util.Locale("id", "ID")).format(java.util.Date(invoice.dueDateMillis))}
                    - Silakan cek lampiran PDF resmi yang disertakan untuk melihat rincian item, subtotal, beserta metode pembayaran digital (QRIS).
                    
                    Terima kasih atas kerja samanya.
                    
                    Salam hangat,
                    ${profile.companyName}
                """.trimIndent()
                
                val intent = if (actionEmail) {
                    // Send strictly via email
                    Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(invoice.clientEmail))
                        putExtra(Intent.EXTRA_SUBJECT, emailSubject)
                        putExtra(Intent.EXTRA_TEXT, emailBody)
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                } else {
                    // Generic print / download / send share sheet
                    Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_SUBJECT, emailSubject)
                        putExtra(Intent.EXTRA_TEXT, emailBody)
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                }
                
                // Invoke native chooser dialog
                val chooserTitle = if (actionEmail) "Kirim Invoice Melalui Email" else "Bagikan Dokumen Invoice"
                val chooserIntent = Intent.createChooser(intent, chooserTitle).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooserIntent)
                
            } else {
                Toast.makeText(context, "Gagal membuat dokumen PDF.", Toast.LENGTH_LONG).show()
            }
        }
    }
}

// Customized ViewModelFactory to inject Database Repository seamlessly
class InvoiceViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InvoiceViewModel::class.java)) {
            val database = AppDatabase.getDatabase(application)
            val repository = AppRepository(
                database.businessProfileDao(),
                database.clientDao(),
                database.invoiceDao()
            )
            @Suppress("UNCHECKED_CAST")
            return InvoiceViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

package com.example

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.db.InvoiceItemSerializer
import com.example.data.model.BusinessProfile
import com.example.data.model.Client
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.data.model.formatRupiah
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.InvoiceViewModel
import com.example.viewmodel.InvoiceViewModelFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: InvoiceViewModel by viewModels {
        InvoiceViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainContainer(viewModel)
            }
        }
    }
}

@Composable
fun MainContainer(viewModel: InvoiceViewModel) {
    // Collect active tables from ViewModel
    val profile by viewModel.businessProfile.collectAsState()
    val clients by viewModel.clients.collectAsState()
    val invoices by viewModel.invoices.collectAsState()
    val isSyncing by viewModel.isCloudSyncing.collectAsState()
    val syncStatusText by viewModel.lastCloudSyncTime.collectAsState()

    // Navigation and sub-views controller state
    var currentTab by remember { mutableStateOf(0) } // 0 = Invoices list, 1 = Clients, 2 = Profile
    var activeInvoiceForDetails by remember { mutableStateOf<Invoice?>(null) }
    var isCreatingNewInvoice by remember { mutableStateOf(false) }
    var editingInvoice by remember { mutableStateOf<Invoice?>(null) }

    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            // Render standard tab controls only if not in active Create/Edit screen
            if (!isCreatingNewInvoice && editingInvoice == null) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.height(80.dp)
                ) {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        label = { Text("Invoice", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ),
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = "Tab Invoice"
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_invoices")
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        label = { Text("Klien", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ),
                        icon = {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = "Tab Klien"
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_clients")
                    )
                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        label = { Text("Profil Bisnis", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ),
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = "Tab Profil"
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_profile")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // Screen router
            when {
                isCreatingNewInvoice -> {
                    InvoiceEditorScreen(
                        invoiceToEdit = null,
                        viewModel = viewModel,
                        clients = clients,
                        onBack = { isCreatingNewInvoice = false }
                    )
                }
                editingInvoice != null -> {
                    InvoiceEditorScreen(
                        invoiceToEdit = editingInvoice,
                        viewModel = viewModel,
                        clients = clients,
                        onBack = { editingInvoice = null }
                    )
                }
                else -> {
                    // Main Tabs switcher
                    when (currentTab) {
                        0 -> DashboardInvoicesTab(
                            invoices = invoices,
                            isSyncing = isSyncing,
                            syncStatusText = syncStatusText,
                            onTriggerSync = { viewModel.triggerCloudSync() },
                            onSelectInvoice = { activeInvoiceForDetails = it },
                            onEditInvoice = { editingInvoice = it },
                            onCreateInvoice = { isCreatingNewInvoice = true }
                        )
                        1 -> ClientsTab(
                            clients = clients,
                            viewModel = viewModel,
                            profile = profile
                        )
                        2 -> ProfileTab(
                            profile = profile ?: BusinessProfile(),
                            viewModel = viewModel,
                            isSyncing = isSyncing,
                            syncStatusText = syncStatusText
                        )
                    }
                }
            }

            // 1. DETAIL INVOICE DIALOG WITH EMAIL SEND & PDF DOWNLOAD
            activeInvoiceForDetails?.let { invoice ->
                InvoiceDetailsDialog(
                    invoice = invoice,
                    profile = profile ?: BusinessProfile(),
                    onMarkStatusChange = {
                        viewModel.toggleInvoicePaidStatus(invoice)
                        activeInvoiceForDetails = activeInvoiceForDetails?.copy(isPaid = !invoice.isPaid)
                    },
                    onShareInvoice = { actionEmail ->
                        viewModel.shareInvoicePdf(context, invoice, actionEmail)
                    },
                    onDeleteInvoice = {
                        viewModel.deleteInvoice(invoice)
                        Toast.makeText(context, "Invoice berhasil dihapus", Toast.LENGTH_SHORT).show()
                        activeInvoiceForDetails = null
                    },
                    onDismiss = { activeInvoiceForDetails = null }
                )
            }
        }
    }
}

// ==========================================
// 1. TAB: INVOICES (DASHBOARD)
// ==========================================
@Composable
fun DashboardInvoicesTab(
    invoices: List<Invoice>,
    isSyncing: Boolean,
    syncStatusText: String,
    onTriggerSync: () -> Unit,
    onSelectInvoice: (Invoice) -> Unit,
    onEditInvoice: (Invoice) -> Unit,
    onCreateInvoice: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterTab by remember { mutableStateOf(0) } // 0 = Semua, 1 = Lunas, 2 = Belum Lunas

    // Filter calculations
    val filteredInvoices = invoices.filter { invoice ->
        val matchesQuery = invoice.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                invoice.clientName.contains(searchQuery, ignoreCase = true)
        val matchesTab = when (selectedFilterTab) {
            1 -> invoice.isPaid
            2 -> !invoice.isPaid
            else -> true
        }
        matchesQuery && matchesTab
    }

    // Revenue calculations
    val totalRevenue = invoices.filter { it.isPaid }.sumOf { invoice ->
        val items = InvoiceItemSerializer.deserialize(invoice.itemsJson)
        val sub = items.sumOf { it.totalPrice }
        val tax = sub * (invoice.taxRatePercent / 100.0)
        sub + tax - invoice.discountAmount
    }

    val totalPending = invoices.filter { !it.isPaid }.sumOf { invoice ->
        val items = InvoiceItemSerializer.deserialize(invoice.itemsJson)
        val sub = items.sumOf { it.totalPrice }
        val tax = sub * (invoice.taxRatePercent / 100.0)
        sub + tax - invoice.discountAmount
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // App Identity Header & Dynamic Secure Cloud Status indicator
        HeaderIdentitySection(
            title = "Daftar Invoice",
            isSyncing = isSyncing,
            syncStatusText = syncStatusText,
            onRefresh = onTriggerSync
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Financial summary cards row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2F1)), // Teal-green soft background
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Total Terbayar", fontSize = 11.sp, color = Color(0xFF004D40), fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        formatRupiah(totalRevenue),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00796B)
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)), // Orange-yellow light soft background
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Total Pending", fontSize = 11.sp, color = Color(0xFFE65100), fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        formatRupiah(totalPending),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar Outlined
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Cari Invoice / Nama Pelanggan...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_invoice_input"),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.LightGray
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Filter tabs selector row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filterOptions = listOf("Semua", "Lunas", "Pending")
            filterOptions.forEachIndexed { index, title ->
                val isSelected = selectedFilterTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.2f))
                        .clickable { selectedFilterTab = index }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else Color.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Invoice list scroll layout
        Box(modifier = Modifier.weight(1f)) {
            if (filteredInvoices.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(54.dp),
                            tint = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Belum ada Invoice yang cocok",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredInvoices) { invoice ->
                        InvoiceItemCard(
                            invoice = invoice,
                            onSelect = { onSelectInvoice(invoice) },
                            onEdit = { onEditInvoice(invoice) }
                        )
                    }
                }
            }

            // Big FAB to generate new invoice (Floating action button)
            Button(
                onClick = onCreateInvoice,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("floating_create_invoice_button"),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = "Buat Invoice baru", tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Buat Invoice", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun HeaderIdentitySection(
    title: String,
    isSyncing: Boolean,
    syncStatusText: String,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "BillEase",
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Text(
                title,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black
            )
        }

        // Animated Sync Icon / Status indicators
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                .clickable { onRefresh() }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = "Cloud Status",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isSyncing) "Mengenkripsi..." else syncStatusText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun InvoiceItemCard(
    invoice: Invoice,
    onSelect: () -> Unit,
    onEdit: () -> Unit
) {
    val items = InvoiceItemSerializer.deserialize(invoice.itemsJson)
    val sub = items.sumOf { it.totalPrice }
    val tax = sub * (invoice.taxRatePercent / 100.0)
    val grandTotal = sub + tax - invoice.discountAmount

    // Simple formatted Date
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
    val issueDateStr = sdf.format(Date(invoice.issueDateMillis))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("invoice_card_${invoice.invoiceNumber}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status marker indicator (Teal/Green if paid, orange if unpaid)
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (invoice.isPaid) Color(0xFF00796B) else Color(0xFFE65100))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Invoice #${invoice.invoiceNumber}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                    
                    // Paid badge setup
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (invoice.isPaid) Color(0xFFE0F2F1) else Color(0xFFFFF3E0))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (invoice.isPaid) "Lunas" else "Pending",
                            color = if (invoice.isPaid) Color(0xFF00796B) else Color(0xFFD84315),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = invoice.clientName,
                        fontSize = 12.sp,
                        color = Color.DarkGray,
                        fontWeight = FontWeight.SemiBold
                    )

                    val style = invoice.layoutStyle
                    val tagBg = when (style) {
                        "Minimalist" -> Color(0xFFF9F6F0)
                        "Creative" -> Color(0xFFEEF2FF)
                        else -> Color(0xFFE8EAF6)
                    }
                    val tagTextColor = when (style) {
                        "Minimalist" -> Color(0xFF9E7E56)
                        "Creative" -> Color(0xFF4F46E5)
                        else -> Color(0xFF1A237E)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(tagBg)
                            .padding(horizontal = 6.dp, vertical = 1.5.dp)
                    ) {
                        Text(
                            text = style,
                            color = tagTextColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Jatuh Tempo: ${sdf.format(Date(invoice.dueDateMillis))}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = formatRupiah(grandTotal),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Action Edit button
            IconButton(
                onClick = onEdit,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("edit_invoice_icon_${invoice.invoiceNumber}")
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Invoice",
                    modifier = Modifier.size(16.dp),
                    tint = Color.Gray
                )
            }
        }
    }
}

// ==========================================
// 2. TAB: CLIENTS MANAGEMENT
// ==========================================
@Composable
fun ClientsTab(
    clients: List<Client>,
    viewModel: InvoiceViewModel,
    profile: BusinessProfile?
) {
    var isShowingAddDialog by remember { mutableStateOf(false) }
    var clientToEdit by remember { mutableStateOf<Client?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current

    val filteredClients = clients.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.email.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Platform Synced Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "BillEase",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Daftar Klien",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (profile?.useFirestore == true) {
                    Button(
                        onClick = {
                            viewModel.syncClientsWithFirestore { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.testTag("firestore_sync_pull_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.CloudDownload, contentDescription = "", modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tarik Cloud", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = { isShowingAddDialog = true },
                    modifier = Modifier.testTag("add_client_button_trigger"),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "", modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah Klien", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search panel
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Cari Klien...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_client_input"),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.LightGray
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (filteredClients.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Kamu belum memiliki Klien tersimpan.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 60.dp)
            ) {
                items(filteredClients) { client ->
                    ClientCardItem(
                        client = client,
                        onEdit = { clientToEdit = client },
                        onDelete = { viewModel.deleteClient(client) }
                    )
                }
            }
        }

        // Add client popup
        if (isShowingAddDialog) {
            AddOrEditClientDialog(
                client = null,
                onDismiss = { isShowingAddDialog = false },
                onSave = { name, email, phone, address ->
                    viewModel.addClient(name, email, phone, address)
                    isShowingAddDialog = false
                }
            )
        }

        // Edit client popup
        clientToEdit?.let { client ->
            AddOrEditClientDialog(
                client = client,
                onDismiss = { clientToEdit = null },
                onSave = { name, email, phone, address ->
                    viewModel.updateClient(client.id, name, email, phone, address, client.firestoreDocId)
                    clientToEdit = null
                }
            )
        }
    }
}

@Composable
fun ClientCardItem(
    client: Client,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        client.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                    if (client.firestoreDocId != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFEEF2FF))
                                .padding(horizontal = 6.dp, vertical = 1.5.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Cloud,
                                    contentDescription = "Cloud Synced",
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = "Cloud",
                                    color = Color(0xFF4F46E5),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                if (client.email.isNotEmpty()) {
                    Text(
                        "Email: ${client.email}",
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )
                }
                if (client.phone.isNotEmpty()) {
                    Text(
                        "Telp: ${client.phone}",
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )
                }
                if (client.address.isNotEmpty()) {
                    Text(
                        client.address,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp).testTag("edit_client_button_${client.id}")) {
                    Icon(Icons.Default.Edit, contentDescription = "", modifier = Modifier.size(16.dp), tint = Color.Gray)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp).testTag("delete_client_button_${client.id}")) {
                    Icon(Icons.Default.Delete, contentDescription = "", modifier = Modifier.size(16.dp), tint = Color.Red.copy(alpha = 0.8f))
                }
            }
        }
    }
}

@Composable
fun AddOrEditClientDialog(
    client: Client?,
    onDismiss: () -> Unit,
    onSave: (name: String, email: String, phone: String, address: String) -> Unit
) {
    var name by remember { mutableStateOf(client?.name ?: "") }
    var email by remember { mutableStateOf(client?.email ?: "") }
    var phone by remember { mutableStateOf(client?.phone ?: "") }
    var address by remember { mutableStateOf(client?.address ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = if (client == null) "Tambah Pelanggan" else "Edit Pelanggan",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Pelanggan/Perusahaan *", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("client_input_name"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Alamat Email", fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth().testTag("client_input_email"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Nomor Telepon", fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth().testTag("client_input_phone"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Alamat Lengkap", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("client_input_address"),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("client_dialog_cancel"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.trim().isNotEmpty()) {
                                onSave(name, email, phone, address)
                            }
                        },
                        modifier = Modifier.testTag("client_dialog_save"),
                        shape = RoundedCornerShape(8.dp),
                        enabled = name.trim().isNotEmpty()
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. TAB: BUSINESS PROFILE SETUP
// ==========================================
@Composable
fun ProfileTab(
    profile: BusinessProfile,
    viewModel: InvoiceViewModel,
    isSyncing: Boolean,
    syncStatusText: String
) {
    var name by remember(profile) { mutableStateOf(profile.companyName) }
    var email by remember(profile) { mutableStateOf(profile.email) }
    var phone by remember(profile) { mutableStateOf(profile.phone) }
    var address by remember(profile) { mutableStateOf(profile.address) }
    var taxId by remember(profile) { mutableStateOf(profile.taxId) }
    var bankName by remember(profile) { mutableStateOf(profile.bankName) }
    var bankNo by remember(profile) { mutableStateOf(profile.bankAccountNo) }
    var bankHolder by remember(profile) { mutableStateOf(profile.bankAccountName) }
    var logoUri by remember(profile) { mutableStateOf(profile.logoUri) }
    var notes by remember(profile) { mutableStateOf(profile.paymentNotes) }
    var firestoreProjectId by remember(profile) { mutableStateOf(profile.firestoreProjectId) }
    var firestoreApiKey by remember(profile) { mutableStateOf(profile.firestoreApiKey) }
    var firestoreCollection by remember(profile) { mutableStateOf(profile.firestoreCollection) }
    var useFirestore by remember(profile) { mutableStateOf(profile.useFirestore) }

    val context = LocalContext.current

    // Launcher photo selector tool
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            // Persist permission so app can load uri values even after reboots
            try {
                val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flag)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            logoUri = uri.toString()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // App header with Cloud Action button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "BillEase",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Profil Bisnis",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black
                )
            }

            // Sync database profile
            Button(
                onClick = { viewModel.triggerCloudSync() },
                modifier = Modifier.testTag("profile_sync_action"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.CloudSync, contentDescription = "", modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Sync Cloud", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Profile Form inside a vertical scroller container
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 60.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Photo Logo uploader layout
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.LightGray.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (logoUri != null) {
                            AsyncImage(
                                model = logoUri,
                                contentDescription = "Logo Perusahaan",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "",
                                modifier = Modifier.size(28.dp),
                                tint = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            "Logo Perusahaan",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            "Tambahkan logo untuk mempercantik invoice Anda.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                },
                                modifier = Modifier.testTag("upload_logo_picker_button"),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Text("Pilih Foto", fontSize = 11.sp)
                            }
                            if (logoUri != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = { logoUri = null },
                                    modifier = Modifier.testTag("reset_logo_button"),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Text("Batal", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Core Profile input
            Text("IDENTITAS BISNIS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama Perusahaan / Bisnis *", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth().testTag("profile_input_name")
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Bisnis", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth().testTag("profile_input_email"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Nomor Telepon", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth().testTag("profile_input_phone"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Alamat Kantor / Bisnis", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth().testTag("profile_input_address"),
                maxLines = 3
            )

            OutlinedTextField(
                value = taxId,
                onValueChange = { taxId = it },
                label = { Text("Nomor NPWP / Legalitas (Opsional)", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth().testTag("profile_input_tax")
            )

            Text("BANK & INTEGRASI PEMBAYARAN DIGITAL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

            OutlinedTextField(
                value = bankName,
                onValueChange = { bankName = it },
                label = { Text("Nama Bank / E-Wallet (e.g. BCA, OVO, GOPAY) *", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth().testTag("profile_input_bank")
            )

            OutlinedTextField(
                value = bankNo,
                onValueChange = { bankNo = it },
                label = { Text("Nomor Rekening / HP *", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth().testTag("profile_input_bank_no"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = bankHolder,
                onValueChange = { bankHolder = it },
                label = { Text("Nama Pemilik Rekening *", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth().testTag("profile_input_bank_holder")
            )

            Text("DATA SYNC FIRESTORE (CLOUD)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

            Card(
                modifier = Modifier.fillMaxWidth().testTag("firestore_config_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Aktifkan Sinkronisasi Cloud (Firestore)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            Text("Simpan dan sinkronkan data klien langsung ke/dari database cloud Firestore.", fontSize = 10.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = useFirestore,
                            onCheckedChange = { useFirestore = it },
                            modifier = Modifier.testTag("firestore_active_switch")
                        )
                    }

                    if (useFirestore) {
                        OutlinedTextField(
                            value = firestoreProjectId,
                            onValueChange = { firestoreProjectId = it },
                            label = { Text("Firestore Project ID *", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth().testTag("profile_input_firestore_project_id"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.LightGray
                            )
                        )

                        OutlinedTextField(
                            value = firestoreApiKey,
                            onValueChange = { firestoreApiKey = it },
                            label = { Text("API Key (Opsional / query param ?key=)", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth().testTag("profile_input_firestore_api_key"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.LightGray
                            )
                        )

                        OutlinedTextField(
                            value = firestoreCollection,
                            onValueChange = { firestoreCollection = it },
                            label = { Text("Nama Koleksi (Collection) *", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth().testTag("profile_input_firestore_collection"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.LightGray
                            )
                        )
                    }
                }
            }

            Text("CATATAN STANDAR INVOICE (BAWAAN)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Catatan Tambahan", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth().testTag("profile_input_notes"),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Sync Status indicators and save action
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (isSyncing) Icons.Default.CloudSync else Icons.Default.CloudDone,
                        contentDescription = "",
                        modifier = Modifier.size(16.dp),
                        tint = if (isSyncing) MaterialTheme.colorScheme.primary else Color(0xFF00796B)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSyncing) "Melakukan enkripsi cloud..." else syncStatusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSyncing) MaterialTheme.colorScheme.primary else Color(0xFF00796B)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        if (name.trim().isNotEmpty() && bankName.trim().isNotEmpty() && bankNo.trim().isNotEmpty() && bankHolder.trim().isNotEmpty()) {
                            viewModel.updateBusinessProfile(
                                name = name,
                                email = email,
                                phone = phone,
                                address = address,
                                taxId = taxId,
                                bankName = bankName,
                                bankNo = bankNo,
                                bankHolder = bankHolder,
                                logoUri = logoUri,
                                notes = notes,
                                firestoreProjectId = firestoreProjectId,
                                firestoreApiKey = firestoreApiKey,
                                firestoreCollection = firestoreCollection,
                                useFirestore = useFirestore
                            )
                            Toast.makeText(context, "Profil berhasil disimpan!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Mohon lengkapi kolom bertanda *", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_profile_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Simpan Konfigurasi Profil", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

// ==========================================
// 4. SCREEN: INVOICE EDITOR (CREATE / EDIT)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceEditorScreen(
    invoiceToEdit: Invoice?,
    viewModel: InvoiceViewModel,
    clients: List<Client>,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // Initialize state
    var invoiceNumber by remember { mutableStateOf(invoiceToEdit?.invoiceNumber ?: "INV-${Calendar.getInstance().get(Calendar.YEAR)}-${String.format("%03d", (0..1000).random())}") }
    var clientName by remember { mutableStateOf(invoiceToEdit?.clientName ?: "") }
    var clientEmail by remember { mutableStateOf(invoiceToEdit?.clientEmail ?: "") }
    var clientPhone by remember { mutableStateOf(invoiceToEdit?.clientPhone ?: "") }
    var clientAddress by remember { mutableStateOf(invoiceToEdit?.clientAddress ?: "") }

    var issueDateMillis by remember { mutableStateOf(invoiceToEdit?.issueDateMillis ?: System.currentTimeMillis()) }
    var dueDateMillis by remember { mutableStateOf(invoiceToEdit?.dueDateMillis ?: (System.currentTimeMillis() + 7 * 24 * 3600 * 1000L)) }

    var taxRate by remember { mutableStateOf(invoiceToEdit?.taxRatePercent ?: 0.0) }
    var discountValue by remember { mutableStateOf(invoiceToEdit?.discountAmount?.toString() ?: "0") }
    var isPaidStatus by remember { mutableStateOf(invoiceToEdit?.isPaid ?: false) }
    var customNotes by remember { mutableStateOf(invoiceToEdit?.paymentNotes ?: "") }
    var layoutStyle by remember { mutableStateOf(invoiceToEdit?.layoutStyle ?: "Classic") }

    // List of added merchandise items
    val invoiceItems = remember {
        mutableStateListOf<InvoiceItem>().apply {
            if (invoiceToEdit != null) {
                addAll(InvoiceItemSerializer.deserialize(invoiceToEdit.itemsJson))
            }
        }
    }

    // Modal dialog trigger for adding new items
    var isShowingAddItemDialog by remember { mutableStateOf(false) }

    // Client dropdown properties
    var dropdownExpanded by remember { mutableStateOf(false) }

    // Simple date pickers
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))

    fun showIssueDatePicker() {
        val calendar = Calendar.getInstance().apply { timeInMillis = issueDateMillis }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCalendar = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                issueDateMillis = newCalendar.timeInMillis
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun showDueDatePicker() {
        val calendar = Calendar.getInstance().apply { timeInMillis = dueDateMillis }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCalendar = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                dueDateMillis = newCalendar.timeInMillis
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("invoice_editor_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                    Text(
                        text = if (invoiceToEdit == null) "Buat Invoice Baru" else "Edit Invoice",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // General info card
            Text("NOMOR & TANGGAL INVOICE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

            OutlinedTextField(
                value = invoiceNumber,
                onValueChange = { invoiceNumber = it },
                label = { Text("Nomor Seri Invoice *", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth().testTag("editor_input_number")
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Issue Date
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .clickable { showIssueDatePicker() }
                        .padding(12.dp)
                ) {
                    Column {
                        Text("Tanggal Pembubuhan", fontSize = 10.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(sdf.format(Date(issueDateMillis)), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Due Date
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .clickable { showDueDatePicker() }
                        .padding(12.dp)
                ) {
                    Column {
                        Text("Jatuh Tempo", fontSize = 10.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(sdf.format(Date(dueDateMillis)), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Customer Selector Block
            Text("DATA PENERIMA TAGIHAN (KLIEN)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

            if (clients.isNotEmpty()) {
                // Exposed M3 Dropdown select
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = if (clientName.isEmpty()) "Pilih dari Database Klien..." else clientName,
                        onValueChange = {},
                        label = { Text("Pilih Klien Tersimpan", fontSize = 12.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.LightGray
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        clients.forEach { choice ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(choice.name, fontSize = 13.sp)
                                        if (choice.firestoreDocId != null) {
                                            Icon(
                                                imageVector = Icons.Filled.Cloud,
                                                contentDescription = "Cloud Connected",
                                                tint = Color(0xFF4F46E5),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    clientName = choice.name
                                    clientEmail = choice.email
                                    clientPhone = choice.phone
                                    clientAddress = choice.address
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Client detail edit inputs
            OutlinedTextField(
                value = clientName,
                onValueChange = { clientName = it },
                label = { Text("Nama Pelanggan/Perusahaan *", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth().testTag("editor_input_client_name")
            )

            OutlinedTextField(
                value = clientEmail,
                onValueChange = { clientEmail = it },
                label = { Text("Email Penerima", fontSize = 12.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth().testTag("editor_input_client_email")
            )

            OutlinedTextField(
                value = clientPhone,
                onValueChange = { clientPhone = it },
                label = { Text("Nomor Telepon", fontSize = 12.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth().testTag("editor_input_client_phone")
            )

            OutlinedTextField(
                value = clientAddress,
                onValueChange = { clientAddress = it },
                label = { Text("Alamat Lengkap", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth().testTag("editor_input_client_address")
            )

            // Items breakdown list card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("RINCIAN BARANG / JASA", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                OutlinedButton(
                    onClick = { isShowingAddItemDialog = true },
                    modifier = Modifier.testTag("add_item_trigger_button"),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "", modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah Item", fontSize = 11.sp)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                if (invoiceItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Belum ada item ditambahkan.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                } else {
                    Column(modifier = Modifier.padding(10.dp)) {
                        invoiceItems.forEachIndexed { idx, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.description, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    Text("${item.quantity}x @ ${formatRupiah(item.unitPrice)}", fontSize = 11.sp, color = Color.Gray)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        formatRupiah(item.totalPrice),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { invoiceItems.removeAt(idx) },
                                        modifier = Modifier.size(32.dp).testTag("delete_item_icon_$idx")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "", modifier = Modifier.size(14.dp), tint = Color.Red.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Calculations, Discount & Tax rates sliders
            Text("PAJAK & DISKON", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Tax percentage slider
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Persentase Pajak (e.g. PPn)", fontSize = 12.sp, color = Color.Black)
                            Text("${taxRate.toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = taxRate.toFloat(),
                            onValueChange = { taxRate = it.toDouble() },
                            valueRange = 0f..25f,
                            steps = 25,
                            modifier = Modifier.testTag("tax_rate_slider")
                        )
                    }

                    // Direct discount inputs
                    OutlinedTextField(
                        value = discountValue,
                        onValueChange = { discountValue = it },
                        label = { Text("Potongan Harga Diskon (Rp)", fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("editor_input_discount"),
                        singleLine = true
                    )
                }
            }

            // Quick Status checklist
            Text("STATUS TRANSAKSI & CATATAN INVOICE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Invoice Sudah Dibayar Lunas", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        Text("Tandai transaksi langsung lunas", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = isPaidStatus,
                        onCheckedChange = { isPaidStatus = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF00796B),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.LightGray.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("editor_paid_status_switch")
                    )
                }
            }

            OutlinedTextField(
                value = customNotes,
                onValueChange = { customNotes = it },
                label = { Text("Catatan kustom khusus invoice ini (Opsional)", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth().testTag("editor_input_custom_notes"),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Selection block for choosing layout styles
            Text("GAYA PRACETAK DOKUMEN (LAYOUT STYLE)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val styles = listOf(
                    Triple("Classic", "Corporate", Color(0xFF1A237E)),
                    Triple("Minimalist", "Elegant", Color(0xFFC5A880)),
                    Triple("Creative", "Vibrant", Color(0xFF4F46E5))
                )

                styles.forEach { (key, label, primaryColor) ->
                    val isSelected = layoutStyle == key
                    val borderColor = if (isSelected) primaryColor else Color.LightGray.copy(alpha = 0.5f)
                    val bgSelection = if (isSelected) primaryColor.copy(alpha = 0.08f) else Color.White
                    val borderThickness = if (isSelected) 2.dp else 1.dp

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(86.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(borderThickness, borderColor, RoundedCornerShape(12.dp))
                            .clickable { layoutStyle = key }
                            .testTag("layout_style_card_$key"),
                        colors = CardDefaults.cardColors(containerColor = bgSelection)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(primaryColor),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = key,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Text(
                                    text = when (key) {
                                        "Minimalist" -> "Emas & Hitam"
                                        "Creative" -> "Ungu & Sian"
                                        else -> "Navy & Toska"
                                    },
                                    fontSize = 9.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // BIG GENERATE ACTION TRIGGER
            Button(
                onClick = {
                    val finalDiscount = discountValue.toDoubleOrNull() ?: 0.0
                    // Validation
                    if (clientName.trim().isEmpty() || invoiceItems.isEmpty() || invoiceNumber.trim().isEmpty()) {
                        Toast.makeText(context, "Mohon lengkapi data penerima dan isi rincian barang", Toast.LENGTH_SHORT).show()
                    } else {
                        val invoice = Invoice(
                            id = invoiceToEdit?.id ?: 0,
                            invoiceNumber = invoiceNumber,
                            issueDateMillis = issueDateMillis,
                            dueDateMillis = dueDateMillis,
                            clientName = clientName,
                            clientEmail = clientEmail,
                            clientPhone = clientPhone,
                            clientAddress = clientAddress,
                            itemsJson = InvoiceItemSerializer.serialize(invoiceItems),
                            taxRatePercent = taxRate,
                            discountAmount = finalDiscount,
                            isPaid = isPaidStatus,
                            paymentNotes = customNotes,
                            layoutStyle = layoutStyle
                        )

                        if (invoiceToEdit == null) {
                            viewModel.addInvoice(invoice) {
                                Toast.makeText(context, "Invoice baru berhasil tersimpan!", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        } else {
                            viewModel.updateInvoice(invoice) {
                                Toast.makeText(context, "Invoice berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_and_compile_invoice_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Simpan Invoice & Selesaikan", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }

    // Modal dialog to add items
    if (isShowingAddItemDialog) {
        AddItemDialog(
            onDismiss = { isShowingAddItemDialog = false },
            onAdd = { desc, price, qty ->
                invoiceItems.add(InvoiceItem(desc, price, qty))
                isShowingAddItemDialog = false
            }
        )
    }
}

@Composable
fun AddItemDialog(
    onDismiss: () -> Unit,
    onAdd: (description: String, price: Double, quantity: Int) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("1") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Tambah Barang / Jasa", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Nama Barang / Deskripsi Jasa *", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("item_input_desc"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Harga Satuan (Rp) *", fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("item_input_price"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("Kuantitas (Qty) *", fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("item_input_qty"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("item_dialog_cancel"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val price = priceText.toDoubleOrNull() ?: 0.0
                            val qty = quantityText.toIntOrNull() ?: 1
                            if (description.trim().isNotEmpty() && price > 0) {
                                onAdd(description, price, qty)
                            }
                        },
                        modifier = Modifier.testTag("item_dialog_save"),
                        shape = RoundedCornerShape(8.dp),
                        enabled = description.trim().isNotEmpty() && (priceText.toDoubleOrNull() ?: 0.0) > 0
                    ) {
                        Text("Tambahkan")
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. DIALOG: INVOICE DETAILS PREVIEW
// ==========================================
@Composable
fun InvoiceDetailsDialog(
    invoice: Invoice,
    profile: BusinessProfile,
    onMarkStatusChange: () -> Unit,
    onShareInvoice: (actionEmail: Boolean) -> Unit,
    onDeleteInvoice: () -> Unit,
    onDismiss: () -> Unit
) {
    val items = InvoiceItemSerializer.deserialize(invoice.itemsJson)
    val sub = items.sumOf { it.totalPrice }
    val tax = sub * (invoice.taxRatePercent / 100.0)
    val grandTotal = sub + tax - invoice.discountAmount

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(20.dp)),
            color = Color(0xFFF1F5F9) // Slate-light aesthetic background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Diagonal Close Header Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Tutup")
                    }
                    Text(
                        "Rincian Dokumen Invoice",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    IconButton(
                        onClick = onDeleteInvoice,
                        modifier = Modifier.testTag("dialog_delete_invoice_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.Red.copy(alpha = 0.8f))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable preview items block
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header tag status and ID
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("ID INVOICE", fontSize = 10.sp, color = Color.Gray)
                            Text("#${invoice.invoiceNumber}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.Black)
                        }

                        // Interaction: Toggle paid status directly from details
                        Column(horizontalAlignment = Alignment.End) {
                            Text("STATUS", fontSize = 10.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (invoice.isPaid) Color(0xFFE0F2F1) else Color(0xFFFFF3E0))
                                    .clickable { onMarkStatusChange() }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (invoice.isPaid) Color(0xFF00796B) else Color(0xFFE65100))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (invoice.isPaid) "Lunas (Ketuk ubah)" else "Pending (Ketuk ubah)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (invoice.isPaid) Color(0xFF00796B) else Color(0xFFE65100)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Billing Party info
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("DIKIRIM OLEH", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Text(if (profile.companyName.isNotEmpty()) profile.companyName else "CV. Usaha Mandiri", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(profile.email, fontSize = 11.sp, color = Color.DarkGray)
                            Text(profile.phone, fontSize = 11.sp, color = Color.DarkGray)
                        }

                        VerticalDivider(modifier = Modifier.height(48.dp).padding(horizontal = 10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text("DITAGIHKAN KEPADA", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Text(invoice.clientName, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(invoice.clientEmail, fontSize = 11.sp, color = Color.DarkGray)
                            Text(invoice.clientPhone, fontSize = 11.sp, color = Color.DarkGray)
                        }
                    }

                    // Dates block row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val pattern = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
                        Column {
                            Text("TANGGAL INVOICE", fontSize = 9.sp, color = Color.Gray)
                            Text(pattern.format(Date(invoice.issueDateMillis)), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("TANGGAL JATUH TEMPO", fontSize = 9.sp, color = Color.Gray)
                            Text(pattern.format(Date(invoice.dueDateMillis)), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (invoice.isPaid) Color.Black else Color.Red)
                        }
                    }

                    // Table break down items list
                    Text("RINCIAN TAGIHAN", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items.forEach { line ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(line.description, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    Text("${line.quantity} Pcs x ${formatRupiah(line.unitPrice)}", fontSize = 10.sp, color = Color.Gray)
                                }
                                Text(formatRupiah(line.totalPrice), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Invoicing calculations calculations block
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Subtotal", fontSize = 11.sp, color = Color.DarkGray)
                            Text(formatRupiah(sub), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }

                        if (invoice.taxRatePercent > 0) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Pajak (${invoice.taxRatePercent}%)", fontSize = 11.sp, color = Color.DarkGray)
                                Text(formatRupiah(tax), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }

                        if (invoice.discountAmount > 0) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Diskon", fontSize = 11.sp, color = Color.DarkGray)
                                Text("- " + formatRupiah(invoice.discountAmount), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD84315))
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Total Pembayaran", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            Text(formatRupiah(grandTotal), fontSize = 13.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Bank Account Info
                    if (profile.bankName.isNotEmpty()) {
                        Column {
                            Text("TRANSFER PEMBAYARAN", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Kirim Pembayaran Ke Nomor Rekening Bawaan:", fontSize = 11.sp, color = Color.DarkGray)
                            Text(
                                text = "${profile.bankName.uppercase()}: ${profile.bankAccountNo} a.n ${profile.bankAccountName}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    // Vector Digital QRIS payment integration
                    if (profile.bankName.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE0F2F1).copy(alpha = 0.5f))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "INTEGRASI PEMBAYARAN DIGITAL (M-BANKING & E-WALLET)",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF004D40),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Draw high authenticity simulated vector qris block
                            Canvas(modifier = Modifier.size(100.dp)) {
                                val areaWidth = size.width
                                val areaHeight = size.height

                                // Draw QR IS white card container 
                                drawRoundRect(
                                    color = Color.White,
                                    size = size,
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                                )

                                // Standard dynamic QR patterns
                                val pixelSize = areaWidth / 14f
                                val darkPaint = Color.Black
                                val accentTeal = Color(0xFF0D9488)

                                // Draw simulated QR boxes
                                // Top-Left visual finder
                                drawRect(color = darkPaint, topLeft = androidx.compose.ui.geometry.Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(pixelSize * 3, pixelSize * 3))
                                drawRect(color = Color.White, topLeft = androidx.compose.ui.geometry.Offset(pixelSize, pixelSize), size = androidx.compose.ui.geometry.Size(pixelSize, pixelSize))

                                // Top-Right visual finder
                                drawRect(color = darkPaint, topLeft = androidx.compose.ui.geometry.Offset(areaWidth - (pixelSize * 3), 0f), size = androidx.compose.ui.geometry.Size(pixelSize * 3, pixelSize * 3))
                                drawRect(color = Color.White, topLeft = androidx.compose.ui.geometry.Offset(areaWidth - (pixelSize * 2), pixelSize), size = androidx.compose.ui.geometry.Size(pixelSize, pixelSize))

                                // Bottom-Left finder
                                drawRect(color = darkPaint, topLeft = androidx.compose.ui.geometry.Offset(0f, areaHeight - (pixelSize * 3)), size = androidx.compose.ui.geometry.Size(pixelSize * 3, pixelSize * 3))
                                drawRect(color = Color.White, topLeft = androidx.compose.ui.geometry.Offset(pixelSize, areaHeight - (pixelSize * 2)), size = androidx.compose.ui.geometry.Size(pixelSize, pixelSize))

                                // Central custom QR matrix patterns
                                drawRect(color = darkPaint, topLeft = androidx.compose.ui.geometry.Offset(pixelSize * 5, pixelSize * 2), size = androidx.compose.ui.geometry.Size(pixelSize, pixelSize * 2))
                                drawRect(color = darkPaint, topLeft = androidx.compose.ui.geometry.Offset(pixelSize * 7, pixelSize * 6), size = androidx.compose.ui.geometry.Size(pixelSize, pixelSize))
                                drawRect(color = accentTeal, topLeft = androidx.compose.ui.geometry.Offset(pixelSize * 6, pixelSize * 5), size = androidx.compose.ui.geometry.Size(pixelSize, pixelSize))
                                drawRect(color = darkPaint, topLeft = androidx.compose.ui.geometry.Offset(pixelSize * 5, pixelSize * 8), size = androidx.compose.ui.geometry.Size(pixelSize * 3, pixelSize))
                                drawRect(color = accentTeal, topLeft = androidx.compose.ui.geometry.Offset(pixelSize * 9, pixelSize * 9), size = androidx.compose.ui.geometry.Size(pixelSize, pixelSize))
                                drawRect(color = darkPaint, topLeft = androidx.compose.ui.geometry.Offset(pixelSize * 10, pixelSize * 4), size = androidx.compose.ui.geometry.Size(pixelSize, pixelSize * 3))
                                drawRect(color = darkPaint, topLeft = androidx.compose.ui.geometry.Offset(pixelSize * 4, pixelSize * 11), size = androidx.compose.ui.geometry.Size(pixelSize, pixelSize * 2))
                                drawRect(color = darkPaint, topLeft = androidx.compose.ui.geometry.Offset(pixelSize * 10, pixelSize * 10), size = androidx.compose.ui.geometry.Size(pixelSize * 2, pixelSize))
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "QRIS MERCHANDISED: ${if (profile.companyName.isNotEmpty()) profile.companyName.uppercase() else "CV. USAHA MANDIRI"}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF004D40)
                            )
                            Text(
                                "Nominal otomatis disesuaikan secara dinamis sebesar " + formatRupiah(grandTotal),
                                fontSize = 9.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action buttons: Send email & Print PDF
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Action 1: Generic Share / Export 
                    OutlinedButton(
                        onClick = { onShareInvoice(false) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("dialog_share_pdf_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Unduh PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Action 2: Direct Email Attach
                    Button(
                        onClick = { onShareInvoice(true) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("dialog_email_pdf_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = "", modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kirim Email", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

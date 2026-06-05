package com.example.data.repository

import com.example.data.db.BusinessProfileDao
import com.example.data.db.ClientDao
import com.example.data.db.InvoiceDao
import com.example.data.model.BusinessProfile
import com.example.data.model.Client
import com.example.data.model.Invoice
import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val profileDao: BusinessProfileDao,
    private val clientDao: ClientDao,
    private val invoiceDao: InvoiceDao
) {
    val businessProfile: Flow<BusinessProfile?> = profileDao.getProfile()
    val allClients: Flow<List<Client>> = clientDao.getAllClients()
    val allInvoices: Flow<List<Invoice>> = invoiceDao.getAllInvoices()

    fun getInvoiceById(id: Long): Flow<Invoice?> {
        return invoiceDao.getInvoiceById(id)
    }

    suspend fun saveProfile(profile: BusinessProfile) {
        profileDao.insertProfile(profile)
    }

    suspend fun insertClient(client: Client): Long {
        return clientDao.insertClient(client)
    }

    suspend fun updateClient(client: Client) {
        clientDao.updateClient(client)
    }

    suspend fun deleteClient(client: Client) {
        clientDao.deleteClient(client)
    }

    suspend fun insertInvoice(invoice: Invoice): Long {
        return invoiceDao.insertInvoice(invoice)
    }

    suspend fun updateInvoice(invoice: Invoice) {
        invoiceDao.updateInvoice(invoice)
    }

    suspend fun deleteInvoice(invoice: Invoice) {
        invoiceDao.deleteInvoice(invoice)
    }
}

package com.example.data.db

import com.example.data.model.InvoiceItem

object InvoiceItemSerializer {
    fun serialize(items: List<InvoiceItem>): String {
        return items.joinToString("||") { item ->
            // Replace any existing pipes to avoid breaking serialization
            val descSafe = item.description.replace("|", " ").replace("\n", " ").trim()
            "${descSafe}|${item.unitPrice}|${item.quantity}"
        }
    }

    fun deserialize(serialized: String): List<InvoiceItem> {
        if (serialized.trim().isEmpty()) return emptyList()
        return try {
            serialized.split("||").map { part ->
                val tokens = part.split("|")
                if (tokens.size >= 3) {
                    val desc = tokens[0]
                    val price = tokens[1].toDoubleOrNull() ?: 0.0
                    val qty = tokens[2].toIntOrNull() ?: 1
                    InvoiceItem(desc, price, qty)
                } else {
                    InvoiceItem()
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

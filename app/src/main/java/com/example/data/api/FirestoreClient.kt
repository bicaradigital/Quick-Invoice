package com.example.data.api

import android.util.Log
import com.example.data.model.Client
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object FirestoreClient {
    private val client = OkHttpClient()
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    private const val TAG = "FirestoreClient"

    private fun getBaseUrl(projectId: String, collection: String, apiKey: String): String {
        val base = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/$collection"
        return if (apiKey.isNotEmpty()) {
            "$base?key=$apiKey"
        } else {
            base
        }
    }

    private fun getDocumentUrl(projectId: String, collection: String, docId: String, apiKey: String): String {
        val base = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/$collection/$docId"
        return if (apiKey.isNotEmpty()) {
            "$base?key=$apiKey"
        } else {
            base
        }
    }

    suspend fun getClients(projectId: String, apiKey: String, collection: String): List<Client> = withContext(Dispatchers.IO) {
        val url = getBaseUrl(projectId, collection, apiKey)
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to get clients: ${response.code} - ${response.message}")
                    return@withContext emptyList()
                }

                val bodyStr = response.body?.string() ?: return@withContext emptyList()
                val jsonResponse = JSONObject(bodyStr)
                if (!jsonResponse.has("documents")) {
                    return@withContext emptyList()
                }

                val documentsJson = jsonResponse.getJSONArray("documents")
                val clientsList = mutableListOf<Client>()

                for (i in 0 until documentsJson.length()) {
                    try {
                        val docObj = documentsJson.getJSONObject(i)
                        val docPathName = docObj.getString("name")
                        val docId = docPathName.substringAfterLast("/")
                        
                        val fieldsObj = docObj.optJSONObject("fields") ?: continue
                        val name = fieldsObj.optJSONObject("name")?.optString("stringValue") ?: ""
                        val email = fieldsObj.optJSONObject("email")?.optString("stringValue") ?: ""
                        val phone = fieldsObj.optJSONObject("phone")?.optString("stringValue") ?: ""
                        val address = fieldsObj.optJSONObject("address")?.optString("stringValue") ?: ""

                        clientsList.add(
                            Client(
                                name = name,
                                email = email,
                                phone = phone,
                                address = address,
                                firestoreDocId = docId
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing doc index $i", e)
                    }
                }
                clientsList
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error getting clients", e)
            emptyList()
        }
    }

    suspend fun addClient(projectId: String, apiKey: String, collection: String, clientData: Client): String? = withContext(Dispatchers.IO) {
        val url = getBaseUrl(projectId, collection, apiKey)
        
        val fieldsJson = JSONObject().apply {
            put("name", JSONObject().put("stringValue", clientData.name))
            put("email", JSONObject().put("stringValue", clientData.email))
            put("phone", JSONObject().put("stringValue", clientData.phone))
            put("address", JSONObject().put("stringValue", clientData.address))
        }

        val bodyJson = JSONObject().apply {
            put("fields", fieldsJson)
        }

        val requestBody = bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to add client: ${response.code} - ${response.message}")
                    return@withContext null
                }
                val bodyStr = response.body?.string() ?: return@withContext null
                val resObj = JSONObject(bodyStr)
                val docPathName = resObj.getString("name")
                docPathName.substringAfterLast("/")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error adding client", e)
            null
        }
    }

    suspend fun updateClient(projectId: String, apiKey: String, collection: String, docId: String, clientData: Client): Boolean = withContext(Dispatchers.IO) {
        val baseUrl = getDocumentUrl(projectId, collection, docId, apiKey)
        val url = if (baseUrl.contains("?")) {
            "$baseUrl&updateMask.fieldPaths=name&updateMask.fieldPaths=email&updateMask.fieldPaths=phone&updateMask.fieldPaths=address"
        } else {
            "$baseUrl?updateMask.fieldPaths=name&updateMask.fieldPaths=email&updateMask.fieldPaths=phone&updateMask.fieldPaths=address"
        }

        val fieldsJson = JSONObject().apply {
            put("name", JSONObject().put("stringValue", clientData.name))
            put("email", JSONObject().put("stringValue", clientData.email))
            put("phone", JSONObject().put("stringValue", clientData.phone))
            put("address", JSONObject().put("stringValue", clientData.address))
        }

        val bodyJson = JSONObject().apply {
            put("fields", fieldsJson)
        }

        val requestBody = bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(url)
            .patch(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to update client: ${response.code} - ${response.message}")
                    return@withContext false
                }
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error updating client", e)
            false
        }
    }

    suspend fun deleteClient(projectId: String, apiKey: String, collection: String, docId: String): Boolean = withContext(Dispatchers.IO) {
        val url = getDocumentUrl(projectId, collection, docId, apiKey)
        val request = Request.Builder()
            .url(url)
            .delete()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to delete client: ${response.code} - ${response.message}")
                    return@withContext false
                }
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error deleting client", e)
            false
        }
    }
}

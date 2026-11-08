package com.example.data.remote

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType {
    CREATE, UPDATE, DELETE, LIST, GET, WRITE
}

fun handleFirestoreError(
    error: Exception,
    operationType: OperationType,
    path: String?
) {
    val auth = try {
        FirebaseAuth.getInstance()
    } catch (_: Exception) {
        null
    }
    val user = auth?.currentUser

    val providerInfo = JSONArray().apply {
        user?.providerData?.forEach { profile ->
            put(
                JSONObject().apply {
                    put("providerId", profile.providerId)
                    put("displayName", profile.displayName)
                    put("email", profile.email)
                    put("photoUrl", profile.photoUrl?.toString())
                }
            )
        }
    }

    val authInfo = JSONObject().apply {
        put("userId", user?.uid)
        put("email", user?.email)
        put("emailVerified", user?.isEmailVerified)
        put("isAnonymous", user?.isAnonymous)
        put("tenantId", user?.tenantId)
        put("providerInfo", providerInfo)
    }

    val errInfo = JSONObject().apply {
        put("error", error.message ?: error.toString())
        put("operationType", operationType.name.lowercase())
        put("path", path)
        put("authInfo", authInfo)
    }

    val jsonString = errInfo.toString()
    Log.e("FirestoreError", jsonString, error)
    throw RuntimeException(jsonString, error)
}

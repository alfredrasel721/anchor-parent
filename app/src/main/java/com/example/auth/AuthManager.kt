package com.example.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

sealed class AuthResult {
    data class Success(val user: FirebaseUser) : AuthResult()
    data object Cancelled : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthManager(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    suspend fun signInWithGoogleButton(activityContext: Context): AuthResult {
        val credentialManager = CredentialManager.create(activityContext)
        val webClientId = activityContext.getString(R.string.default_web_client_id)

        val signInOption = GetSignInWithGoogleOption.Builder(webClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        return executeCredentialRequest(activityContext, credentialManager, request)
    }

    suspend fun trySilentGoogleSignIn(activityContext: Context): AuthResult {
        val credentialManager = CredentialManager.create(activityContext)
        val webClientId = activityContext.getString(R.string.default_web_client_id)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return executeCredentialRequest(activityContext, credentialManager, request)
    }

    private suspend fun executeCredentialRequest(
        activityContext: Context,
        credentialManager: CredentialManager,
        request: GetCredentialRequest
    ): AuthResult {
        return try {
            val result = credentialManager.getCredential(
                request = request,
                context = activityContext
            )
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(
                    googleIdTokenCredential.idToken,
                    null
                )
                val authResult = auth.signInWithCredential(firebaseCredential).await()
                val user = authResult.user
                if (user != null) {
                    AuthResult.Success(user)
                } else {
                    AuthResult.Error("Firebase Authentication returned null user.")
                }
            } else {
                AuthResult.Error("Unsupported credential type returned.")
            }
        } catch (_: GetCredentialCancellationException) {
            AuthResult.Cancelled
        } catch (_: NoCredentialException) {
            AuthResult.Error(
                "No Google account is configured on this device/emulator. Sign in to a Google account in Android Settings or use the Interactive Guardian Sandbox below."
            )
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Google Sign-In failed.")
        }
    }

    suspend fun signOut(context: Context) {
        auth.signOut()
        try {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        } catch (_: Exception) {
            // Ignore credential manager clear failure if play services unavailable
        }
    }
}

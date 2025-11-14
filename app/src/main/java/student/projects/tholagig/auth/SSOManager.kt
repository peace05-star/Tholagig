package student.projects.tholagig.auth

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import student.projects.tholagig.R
import student.projects.tholagig.models.User

class SSOManager(private val context: Context) {

    private val TAG = "SSOManager"
    private val firebaseAuth = FirebaseAuth.getInstance()

    // Google Sign-In
    private lateinit var googleSignInClient: GoogleSignInClient

    companion object {
        const val RC_GOOGLE_SIGN_IN = 1001
    }

    fun initializeGoogleSignIn() {
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

            googleSignInClient = GoogleSignIn.getClient(context, gso)
            Log.d(TAG, "🟢 Google Sign-In initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "🔴 Error initializing Google Sign-In: ${e.message}")
        }
    }

    fun getGoogleSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    suspend fun handleGoogleSignInResult(data: Intent?): SSOResult {
        return try {
            if (data == null) {
                return SSOResult.Error("Sign-in data is null")
            }

            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)

            Log.d(TAG, "🟡 Google Sign-In account received: ${account?.email}")

            // Authenticate with Firebase
            val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()

            val firebaseUser = authResult.user
            if (firebaseUser != null) {
                Log.d(TAG, "🟢 Firebase authentication successful: ${firebaseUser.uid}")

                // Create user object from Google account
                val user = User(
                    userId = firebaseUser.uid,
                    email = account?.email ?: firebaseUser.email ?: "",
                    fullName = account?.displayName ?: firebaseUser.displayName ?: "User",
                    userType = "freelancer", // Default type
                    profileImage = account?.photoUrl?.toString(),
                    isSSO = true,
                    ssoProvider = "google",
                    ssoId = account?.id,
                    createdAt = System.currentTimeMillis(),
                    lastLogin = System.currentTimeMillis()
                )

                SSOResult.Success(user)
            } else {
                SSOResult.Error("Firebase authentication failed - no user returned")
            }

        } catch (e: ApiException) {
            Log.e(TAG, "🔴 Google Sign-In API exception: ${e.statusCode}, ${e.message}")
            when (e.statusCode) {
                com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes.SIGN_IN_CANCELLED ->
                    SSOResult.Cancelled
                else ->
                    SSOResult.Error("Google Sign-In failed: ${e.statusCode} - ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "🔴 Google Sign-In general exception: ${e.message}")
            SSOResult.Error("Authentication failed: ${e.message}")
        }
    }

    // Check if user is already signed in
    fun isUserSignedIn(): Boolean {
        return GoogleSignIn.getLastSignedInAccount(context) != null
    }

    // Sign out
    suspend fun signOut() {
        try {
            googleSignInClient.signOut().await()
            firebaseAuth.signOut()
            Log.d(TAG, "🟢 Sign-out completed")
        } catch (e: Exception) {
            Log.e(TAG, "🔴 Sign-out error: ${e.message}")
        }
    }
}

// SSO Result Sealed Class
sealed class SSOResult {
    data class Success(val user: User) : SSOResult()
    data class Error(val message: String) : SSOResult()
    object Cancelled : SSOResult()
}
package ac.id.umn.projectutsmap

import android.content.Context
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.user.UserInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.security.MessageDigest
import java.util.UUID

sealed interface AuthResponse {
    data object Success : AuthResponse
    data class Error(val message: String?) : AuthResponse
}

data class UserProfile(
    val email: String,
    val userId: String? = null,
    val displayName: String? = null
)

class AuthManager(
    private val context: Context
) {
    private val supabase: SupabaseClient = createSupabaseClient(
        supabaseUrl = "https://ikpkwymuxvvuunzzxjem.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImlrcGt3eW11eHZ2dXVuenp4amVtIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDY1MzgwOTUsImV4cCI6MjA2MjExNDA5NX0.T4wbtUYG_KqnmHpiT_aWNJTYrU63w5qN_9YFQw4FLWc"
    ) {
        install(Auth)
    }

    fun signUpWithEmail(email: String, password: String, displayName: String?): Flow<AuthResponse> = flow {
        try {
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
                this.data = buildJsonObject {
                    put("displayName", JsonPrimitive(displayName))
                }
            }
            emit(AuthResponse.Success)
        } catch (e: Exception) {
            println("Error during sign-up: ${e.message}")
            val message = e.localizedMessage ?: "Unknown error occurred"
            emit(AuthResponse.Error(message))
        }
    }

    fun signInWithEmail(email: String, password: String, displayName: String?): Flow<AuthResponse> = flow {
        try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
                this.data = buildJsonObject {
                    put("display_name", JsonPrimitive(displayName))
                }
            }
            emit(AuthResponse.Success)
        } catch (e: Exception) {
            println("Error during sign-in: ${e.message}")
            val message = e.localizedMessage ?: "Unknown error occurred"
            emit(AuthResponse.Error(message))
        }
    }

    suspend fun signOut(): AuthResponse {
        return try {
            supabase.auth.signOut()
            AuthResponse.Success
        } catch (e: Exception) {
            AuthResponse.Error(e.localizedMessage)
        }
    }

    fun getCurrentUser(): UserProfile? {
        return supabase.auth.currentUserOrNull()?.let { user ->
            val displayName = user.userMetadata?.get("displayName")?.jsonPrimitive?.contentOrNull
            UserProfile(
                email = user.email ?: "",
                userId = user.id,
                displayName = displayName
            )
        }
    }

    fun isUserLoggedIn(): Boolean {
        return supabase.auth.currentUserOrNull() != null
    }

    fun createNonce(): String {
        val rawNonce = UUID.randomUUID().toString()
        val bytes = rawNonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
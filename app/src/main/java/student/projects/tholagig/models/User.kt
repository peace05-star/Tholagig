package student.projects.tholagig.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import java.util.Date

data class User(
    @get:PropertyName("userId")
    @set:PropertyName("userId")
    var userId: String = "",

    @get:PropertyName("fullName")
    @set:PropertyName("fullName")
    var fullName: String = "",

    @get:PropertyName("email")
    @set:PropertyName("email")
    var email: String = "",

    @get:PropertyName("phone")
    @set:PropertyName("phone")
    var phone: String = "",

    @get:PropertyName("userType")
    @set:PropertyName("userType")
    var userType: String = "",

    @get:PropertyName("company")
    @set:PropertyName("company")
    var company: String = "",

    @get:PropertyName("bio")
    @set:PropertyName("bio")
    var bio: String = "",

    @get:PropertyName("skills")
    @set:PropertyName("skills")
    var skills: List<String> = emptyList(),

    @get:PropertyName("profileImage")
    @set:PropertyName("profileImage")
    var profileImage: String? = null,

    // 🆕 FIX: Use Any to handle both Timestamp and Long
    @get:PropertyName("createdAt")
    @set:PropertyName("createdAt")
    var createdAt: Any? = null,

    @get:PropertyName("updatedAt")
    @set:PropertyName("updatedAt")
    var updatedAt: Any? = null,

    // Exclude these from Firestore
    @Exclude
    var password: String = "",

    @Exclude
    var rating: Float = 0.0f,

    @Exclude
    var completedJobs: Int = 0
) {
    // 🆕 FIX: Safe date getters that handle both Timestamp and Long
    @Exclude
    fun getCreatedAtDate(): Date? {
        return when (createdAt) {
            is Timestamp -> (createdAt as Timestamp).toDate()
            is Long -> Date(createdAt as Long)
            is com.google.firebase.Timestamp -> (createdAt as com.google.firebase.Timestamp).toDate()
            else -> null
        }
    }

    @Exclude
    fun getUpdatedAtDate(): Date? {
        return when (updatedAt) {
            is Timestamp -> (updatedAt as Timestamp).toDate()
            is Long -> Date(updatedAt as Long)
            is com.google.firebase.Timestamp -> (updatedAt as com.google.firebase.Timestamp).toDate()
            else -> null
        }
    }

    @Exclude
    fun getCreatedAtLong(): Long {
        return getCreatedAtDate()?.time ?: System.currentTimeMillis()
    }

    @Exclude
    fun getUpdatedAtLong(): Long {
        return getUpdatedAtDate()?.time ?: System.currentTimeMillis()
    }

    // Helper to get company name
    fun getCompanyName(): String {
        return if (company.isNotEmpty()) company else "Your Company"
    }

    // 🆕 FIX: Helper to check if user data is valid
    @Exclude
    fun isValid(): Boolean {
        return userId.isNotEmpty() && fullName.isNotEmpty() && email.isNotEmpty()
    }
}
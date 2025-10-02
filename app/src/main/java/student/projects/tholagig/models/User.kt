package student.projects.tholagig.models

import java.util.Date

/*
data class User(
    val userId: String = "",
    val email: String = "",
    val password: String = "", // Will be encrypted
    val userType: String = "", // "client" or "freelancer"
    val fullName: String = "",
    val phone: String = "",
    val profileImage: String = "",
    val skills: List<String> = emptyList(), // For freelancers
    val company: String = "", // For clients
    val bio: String = "",
    val createdAt: Date = Date()
)
*/



import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("id")
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

    // Use Timestamp for Firestore, but provide Long getters for convenience
    @get:PropertyName("createdAt")
    @set:PropertyName("createdAt")
    var createdAt: Timestamp? = null,

    @get:PropertyName("updatedAt")
    @set:PropertyName("updatedAt")
    var updatedAt: Timestamp? = null,

    // Exclude these from Firestore
    @Exclude
    var password: String = "",

    @Exclude
    var rating: Float = 0.0f,

    @Exclude
    var completedJobs: Int = 0
) {
    // Helper getter for Long timestamp (converts from Firestore Timestamp)
    @Exclude
    fun getCreatedAtLong(): Long {
        return createdAt?.toDate()?.time ?: System.currentTimeMillis()
    }

    @Exclude
    fun getUpdatedAtLong(): Long {
        return updatedAt?.toDate()?.time ?: System.currentTimeMillis()
    }

    // Helper to get company name for JSONPlaceholder compatibility
    fun getCompanyName(): String {
        return if (company.isNotEmpty()) company else "Your Company"
    }
}
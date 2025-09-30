package student.projects.tholagig.models

import java.util.Date

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
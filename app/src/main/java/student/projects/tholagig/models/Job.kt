package student.projects.tholagig.models

import java.util.Date

data class Job(
    val jobId: String = "",
    val clientId: String = "",
    val clientName: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val skillsRequired: List<String> = emptyList(),
    val budget: Double = 0.0,
    val deadline: Date = Date(),
    val location: String = "",
    val company: String? = "",
    val status: String = "open", // "open", "in_progress", "completed"
    val postedAt: Date = Date(),
    val createdDate: Date = Date(),
    val applicationsCount: Int = 0,
    val clientRating: Double? = 0.0,
    val totalReviews: Int? = 0,
    val clientBio: String? = "",
    val experienceLevel: String? = "",
    val projectType: String? = "",
    val estimatedDuration: String? = ""
)
{
    // Helper function to get the correct date
    fun getPostDate(): Date {
        return postedAt ?: createdDate ?: Date()
    }
}
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
    val company: String? = null,
    val status: String = "open",

    val postDate: Date = Date(),
    val postedAt: Date = Date(),
    val createdDate: Date = Date(),
    val applicationsCount: Int = 0,
    val clientRating: Double? = null,
    val totalReviews: Int? = null,
    val clientBio: String? = null,
    val experienceLevel: String? = null,
    val projectType: String? = null,
    val estimatedDuration: String? = null
)
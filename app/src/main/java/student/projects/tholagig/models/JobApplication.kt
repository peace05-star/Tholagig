package student.projects.tholagig.models


import java.util.Date

data class JobApplication(
    val applicationId: String = "",
    val jobId: String = "",
    val freelancerId: String = "",
    val freelancerName: String = "",
    val freelancerEmail: String = "",
    val coverLetter: String = "",
    val proposedBudget: Double = 0.0,
    val status: String = "pending", // "pending", "accepted", "rejected", "withdrawn"
    val appliedAt: Date = Date(),
    val clientId: String = "",
    val jobTitle: String = "",
    val clientName: String = "",
    val estimatedTime: String = "",
    val freelancerRating: Double = 0.0,
    val freelancerCompletedJobs: Int = 0,
    val freelancerSkills: List<String> = emptyList(),
)
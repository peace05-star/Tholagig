package student.projects.tholagig.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class JobApplication(
    val applicationId: String = "",
    val jobId: String = "",
    val freelancerId: String = "",
    val freelancerName: String = "",
    val freelancerEmail: String = "",
    val coverLetter: String = "",
    val proposedBudget: Double = 0.0,
    val status: String = "pending",
    val appliedAt: Date = Date(),
    val clientId: String = "",
    val jobTitle: String = "",
    val clientName: String = "",
    val estimatedTime: String = "",
    val freelancerRating: Double = 0.0,
    val freelancerCompletedJobs: Int = 0,
    val freelancerSkills: List<String> = emptyList(),
) : Parcelable
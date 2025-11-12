package student.projects.tholagig.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import java.util.Date

@Entity(
    tableName = "offline_applications",
    indices = [Index(value = ["jobId"]), Index(value = ["freelancerId"])]
)
data class OfflineApplication(
    @PrimaryKey
    val applicationId: String = "",
    val jobId: String = "",
    val freelancerId: String = "",
    val freelancerName: String = "",
    val freelancerEmail: String = "",
    val coverLetter: String = "",
    val proposedBudget: Double = 0.0,
    val status: String = "pending",
    val appliedAt: Long = Date().time,
    val clientId: String = "",
    val jobTitle: String = "",
    val clientName: String = "",
    val estimatedTime: String = "",
    val freelancerRating: Double = 0.0,
    val freelancerCompletedJobs: Int = 0,
    val freelancerSkills: List<String> = emptyList(),
    val isSynced: Boolean = true, // New field for sync tracking
    val lastUpdated: Long = System.currentTimeMillis() // New field for sync
) {
    // Convert from JobApplication to OfflineApplication
    companion object {
        fun fromJobApplication(application: JobApplication): OfflineApplication {
            return OfflineApplication(
                applicationId = application.applicationId,
                jobId = application.jobId,
                freelancerId = application.freelancerId,
                freelancerName = application.freelancerName,
                freelancerEmail = application.freelancerEmail,
                coverLetter = application.coverLetter,
                proposedBudget = application.proposedBudget,
                status = application.status,
                appliedAt = application.appliedAt.time,
                clientId = application.clientId,
                jobTitle = application.jobTitle,
                clientName = application.clientName,
                estimatedTime = application.estimatedTime,
                freelancerRating = application.freelancerRating,
                freelancerCompletedJobs = application.freelancerCompletedJobs,
                freelancerSkills = application.freelancerSkills,
                isSynced = true
            )
        }
    }

    // Convert back to JobApplication
    fun toJobApplication(): JobApplication {
        return JobApplication(
            applicationId = applicationId,
            jobId = jobId,
            freelancerId = freelancerId,
            freelancerName = freelancerName,
            freelancerEmail = freelancerEmail,
            coverLetter = coverLetter,
            proposedBudget = proposedBudget,
            status = status,
            appliedAt = Date(appliedAt),
            clientId = clientId,
            jobTitle = jobTitle,
            clientName = clientName,
            estimatedTime = estimatedTime,
            freelancerRating = freelancerRating,
            freelancerCompletedJobs = freelancerCompletedJobs,
            freelancerSkills = freelancerSkills
        )
    }
}
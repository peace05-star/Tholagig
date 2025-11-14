package student.projects.tholagig.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import student.projects.tholagig.database.Converters
import java.util.Date

@Entity(tableName = "offline_jobs")
@TypeConverters(Converters::class) // ADD THIS LINE
data class OfflineJob(
    @PrimaryKey
    val jobId: String = "",
    val clientId: String = "",
    val clientName: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",

    // This will now work with the TypeConverter
    val skillsRequired: List<String> = emptyList(),

    val budget: Double = 0.0,
    val deadline: Long = Date().time,
    val location: String = "",
    val company: String? = "",
    val status: String = "open",
    val postedAt: Long = Date().time,
    val createdDate: Long = Date().time,
    val applicationsCount: Int = 0,
    val clientRating: Double? = 0.0,
    val totalReviews: Int? = 0,
    val clientBio: String? = "",
    val experienceLevel: String? = "",
    val projectType: String? = "",
    val estimatedDuration: String? = "",
    val isSynced: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    // ... your existing companion object and methods
    companion object {
        fun fromJob(job: Job): OfflineJob {
            return OfflineJob(
                jobId = job.jobId,
                clientId = job.clientId,
                clientName = job.clientName,
                title = job.title,
                description = job.description,
                category = job.category,
                skillsRequired = job.skillsRequired,
                budget = job.budget,
                deadline = job.deadline.time,
                location = job.location,
                company = job.company,
                status = job.status,
                postedAt = job.postedAt.time,
                createdDate = job.createdDate.time,
                applicationsCount = job.applicationsCount,
                clientRating = job.clientRating,
                totalReviews = job.totalReviews,
                clientBio = job.clientBio,
                experienceLevel = job.experienceLevel,
                projectType = job.projectType,
                estimatedDuration = job.estimatedDuration,
                isSynced = true
            )
        }
    }

    fun toJob(): Job {
        return Job(
            jobId = jobId,
            clientId = clientId,
            clientName = clientName,
            title = title,
            description = description,
            category = category,
            skillsRequired = skillsRequired,
            budget = budget,
            deadline = Date(deadline),
            location = location,
            company = company,
            status = status,
            postedAt = Date(postedAt),
            createdDate = Date(createdDate),
            applicationsCount = applicationsCount,
            clientRating = clientRating,
            totalReviews = totalReviews,
            clientBio = clientBio,
            experienceLevel = experienceLevel,
            projectType = projectType,
            estimatedDuration = estimatedDuration
        )
    }
}
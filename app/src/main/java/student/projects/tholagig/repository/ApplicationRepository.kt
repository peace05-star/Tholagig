package student.projects.tholagig.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import student.projects.tholagig.database.AppDatabase
import student.projects.tholagig.models.JobApplication
import student.projects.tholagig.models.OfflineApplication

class ApplicationRepository(private val database: AppDatabase) {
    private val applicationDao = database.applicationDao()

    fun getApplicationsByFreelancer(freelancerId: String): Flow<List<JobApplication>> {
        return applicationDao.getApplicationsByFreelancer(freelancerId).map { offlineApps ->
            offlineApps.map { it.toJobApplication() }
        }
    }

    fun getApplicationsByJob(jobId: String): Flow<List<JobApplication>> {
        return applicationDao.getApplicationsByJob(jobId).map { offlineApps ->
            offlineApps.map { it.toJobApplication() }
        }
    }

    suspend fun createApplication(application: JobApplication): Boolean {
        return try {
            val offlineApp = OfflineApplication.fromJobApplication(application).copy(isSynced = false)
            applicationDao.insertApplication(offlineApp)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getUnsyncedApplications(): List<JobApplication> {
        return applicationDao.getUnsyncedApplications().map { it.toJobApplication() }
    }

    suspend fun markApplicationAsSynced(applicationId: String) {
        applicationDao.markApplicationAsSynced(applicationId)
    }
}
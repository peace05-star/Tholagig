package student.projects.tholagig.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import student.projects.tholagig.database.AppDatabase
import student.projects.tholagig.models.Job
import student.projects.tholagig.models.OfflineJob

class JobRepository(private val database: AppDatabase) {
    private val jobDao = database.jobDao()
    // Remove API service for now - we'll add it back later
    // private val apiService = ApiClient.getService()

    fun getAllJobs(): Flow<List<Job>> {
        return jobDao.getAllJobs().map { offlineJobs ->
            offlineJobs.map { it.toJob() }
        }
    }

    suspend fun syncJobsWithApi() {
        try {
            // Temporarily comment out API call
            // val jobs = apiService.getJobs()
            // val offlineJobs = jobs.map { job ->
            //     OfflineJob.fromJob(job)
            // }
            // jobDao.insertAllJobs(offlineJobs)
        } catch (e: Exception) {
            // API call failed, we'll use local data
        }
    }

    suspend fun createJob(job: Job): Boolean {
        return try {
            // Save locally first
            val offlineJob = OfflineJob.fromJob(job).copy(isSynced = false)
            jobDao.insertJob(offlineJob)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getJobById(jobId: String): Job? {
        return jobDao.getJobById(jobId)?.toJob()
    }

    suspend fun getUnsyncedJobs(): List<Job> {
        return jobDao.getUnsyncedJobs().map { it.toJob() }
    }

    suspend fun markJobAsSynced(jobId: String) {
        jobDao.markJobAsSynced(jobId)
    }
}
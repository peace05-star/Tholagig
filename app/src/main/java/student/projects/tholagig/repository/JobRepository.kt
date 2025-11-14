package student.projects.tholagig.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import student.projects.tholagig.database.AppDatabase
import student.projects.tholagig.models.Job
import student.projects.tholagig.models.OfflineJob

class JobRepository(private val database: AppDatabase) {

    private val jobDao = database.JobDao()

    suspend fun createJob(job: Job) {
        val offlineJob = OfflineJob.fromJob(job)
        jobDao.insertJob(offlineJob)
    }

    suspend fun createJobs(jobs: List<Job>) {
        val offlineJobs = jobs.map { OfflineJob.fromJob(it) }
        jobDao.insertAllJobs(offlineJobs)
    }

    fun getAllJobs(): Flow<List<Job>> {
        return jobDao.getAllJobs().map { offlineJobs ->
            offlineJobs.map { it.toJob() }
        }
    }

    suspend fun getJobById(jobId: String): Job? {
        return jobDao.getJobById(jobId)?.toJob()
    }

    suspend fun deleteJob(jobId: String) {
        jobDao.deleteJob(jobId)
    }

    suspend fun clearAllJobs() {
        jobDao.deleteAllJobs()
    }
}
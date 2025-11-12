package student.projects.tholagig.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import student.projects.tholagig.models.OfflineJob

@Dao
interface JobDao {
    @Query("SELECT * FROM offline_jobs ORDER BY lastUpdated DESC")
    fun getAllJobs(): Flow<List<OfflineJob>>

    @Query("SELECT * FROM offline_jobs WHERE jobId = :jobId")
    suspend fun getJobById(jobId: String): OfflineJob?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: OfflineJob)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllJobs(jobs: List<OfflineJob>)

    @Query("SELECT * FROM offline_jobs WHERE isSynced = 0")
    suspend fun getUnsyncedJobs(): List<OfflineJob>

    @Query("UPDATE offline_jobs SET isSynced = 1 WHERE jobId = :jobId")
    suspend fun markJobAsSynced(jobId: String)

    @Query("DELETE FROM offline_jobs WHERE jobId = :jobId")
    suspend fun deleteJob(jobId: String)

    @Query("DELETE FROM offline_jobs")
    suspend fun deleteAllJobs()
}
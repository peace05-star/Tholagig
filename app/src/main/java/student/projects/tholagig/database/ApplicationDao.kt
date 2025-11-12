package student.projects.tholagig.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import student.projects.tholagig.models.OfflineApplication

@Dao
interface ApplicationDao {
    @Query("SELECT * FROM offline_applications WHERE freelancerId = :freelancerId ORDER BY appliedAt DESC")
    fun getApplicationsByFreelancer(freelancerId: String): Flow<List<OfflineApplication>>

    @Query("SELECT * FROM offline_applications WHERE jobId = :jobId ORDER BY appliedAt DESC")
    fun getApplicationsByJob(jobId: String): Flow<List<OfflineApplication>>

    @Query("SELECT * FROM offline_applications WHERE applicationId = :applicationId")
    suspend fun getApplicationById(applicationId: String): OfflineApplication?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApplication(application: OfflineApplication)

    @Query("SELECT * FROM offline_applications WHERE isSynced = 0")
    suspend fun getUnsyncedApplications(): List<OfflineApplication>

    @Query("UPDATE offline_applications SET isSynced = 1 WHERE applicationId = :applicationId")
    suspend fun markApplicationAsSynced(applicationId: String)

    @Query("DELETE FROM offline_applications WHERE applicationId = :applicationId")
    suspend fun deleteApplication(applicationId: String)
}
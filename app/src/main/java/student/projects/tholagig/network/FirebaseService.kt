package student.projects.tholagig.network

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import student.projects.tholagig.models.User
import student.projects.tholagig.models.Job
import student.projects.tholagig.models.JobApplication
import java.util.Date

class FirebaseService {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "FirebaseService"

    // === ENHANCED USER OPERATIONS ===

    suspend fun registerUser(user: User): Result<User> {
        return try {
            Log.d(TAG, "🟡 Registering user: ${user.email}")

            // Create Firebase auth user
            val authResult = auth.createUserWithEmailAndPassword(user.email, user.password).await()
            Log.d(TAG, "🟡 Auth user created: ${authResult.user?.uid}")

            // Save user data to Firestore
            val userWithId = user.copy(userId = authResult.user?.uid ?: "")
            db.collection("users").document(userWithId.userId).set(userWithId).await()
            Log.d(TAG, "🟢 User data saved to Firestore")

            Result.success(userWithId)
        } catch (e: Exception) {
            Log.e(TAG, "🔴 Registration failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun loginUser(email: String, password: String): Result<User> {
        return try {
            Log.d(TAG, "🟡 Attempting login for: $email")

            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: ""
            Log.d(TAG, "🟡 Auth successful, user ID: $userId")

            val userDoc = db.collection("users").document(userId).get().await()

            if (userDoc.exists()) {
                Log.d(TAG, "🟡 User document found in Firestore")
                val user = userDoc.toObject(User::class.java)

                if (user != null) {
                    Log.d(TAG, "🟢 Login successful! User type: ${user.userType}")
                    Result.success(user)
                } else {
                    Log.e(TAG, "🔴 User object is null after conversion")
                    Result.failure(Exception("User data conversion failed"))
                }
            } else {
                Log.e(TAG, "🔴 User document NOT found in Firestore for UID: $userId")
                Log.e(TAG, "🔴 Check if user was properly registered in Firestore")
                Result.failure(Exception("User not found in database - please register again"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "🔴 Login error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getUserById(userId: String): Result<User> {
        return try {
            Log.d(TAG, "🟡 Fetching user data for: $userId")

            val userDoc = db.collection("users").document(userId).get().await()

            if (userDoc.exists()) {
                val user = userDoc.toObject(User::class.java)
                if (user != null) {
                    Log.d(TAG, "🟢 User data retrieved: ${user.fullName}, ${user.email}")
                    Result.success(user)
                } else {
                    Log.e(TAG, "🔴 User data conversion failed")
                    Result.failure(Exception("User data conversion failed"))
                }
            } else {
                Log.e(TAG, "🔴 User document not found")
                Result.failure(Exception("User not found in database"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "🔴 Error fetching user: ${e.message}")
            Result.failure(e)
        }
    }

    // === ENHANCED JOB OPERATIONS ===

    // In FirebaseService.kt - Update getJobs() function
    suspend fun getJobs(): Result<List<Job>> {
        return try {
            Log.d("FirebaseService", "📡 Fetching jobs from Firestore...")

            val snapshot = db.collection("jobs")
                .whereEqualTo("status", "open")
                .get()
                .await()

            val jobs = mutableListOf<Job>()

            for (document in snapshot.documents) {
                try {
                    val job = document.toObject(Job::class.java)
                    if (job != null) {
                        jobs.add(job)
                    } else {
                        Log.e("FirebaseService", "❌ Failed to convert document to Job: ${document.id}")
                    }
                } catch (e: Exception) {
                    Log.e("FirebaseService", "❌ Error converting document ${document.id}: ${e.message}")
                }
            }

            // Sort by posted date (handle both postedAt and createdDate)
            val sortedJobs = jobs.sortedByDescending { job ->
                job.postedAt ?: job.createdDate ?: Date()
            }

            Log.d("FirebaseService", "✅ Found ${sortedJobs.size} jobs in Firestore")

            Result.success(sortedJobs)
        } catch (e: Exception) {
            Log.e("FirebaseService", "❌ Error fetching jobs: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getRecommendedJobs(freelancerSkills: List<String>): Result<List<Job>> {
        return try {
            val allJobsResult = getJobs()
            if (allJobsResult.isFailure) return Result.success(emptyList())

            val allJobs = allJobsResult.getOrNull() ?: emptyList()

            // Simple recommendation based on skills matching
            val recommended = allJobs.filter { job ->
                job.skillsRequired.any { requiredSkill ->
                    freelancerSkills.any { freelancerSkill ->
                        freelancerSkill.contains(requiredSkill, ignoreCase = true) ||
                                requiredSkill.contains(freelancerSkill, ignoreCase = true)
                    }
                }
            }.take(10) // Limit to 10 recommended jobs

            Result.success(recommended)
        } catch (e: Exception) {
            Result.success(emptyList()) // Return empty list instead of failure
        }
    }

    suspend fun getSimilarJobs(jobId: String, category: String): Result<List<Job>> {
        return try {
            val snapshot = db.collection("jobs")
                .whereEqualTo("category", category)
                .whereEqualTo("status", "open")
                .whereNotEqualTo("jobId", jobId)
                .limit(5)
                .get()
                .await()

            val jobs = snapshot.toObjects(Job::class.java)
            Result.success(jobs)
        } catch (e: Exception) {
            Result.success(emptyList())
        }
    }

    suspend fun getJobById(jobId: String): Result<Job> {
        return try {
            val document = db.collection("jobs").document(jobId).get().await()
            if (document.exists()) {
                val job = document.toObject(Job::class.java)
                if (job != null) {
                    Result.success(job)
                } else {
                    Result.failure(Exception("Job data conversion failed"))
                }
            } else {
                Result.failure(Exception("Job not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createJob(job: Job): Result<Job> {
        return try {
            val jobWithId = job.copy(jobId = System.currentTimeMillis().toString())
            db.collection("jobs").document(jobWithId.jobId).set(jobWithId).await()
            Result.success(jobWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // === ENHANCED APPLICATION OPERATIONS ===

    suspend fun submitApplication(application: JobApplication): Result<JobApplication> {
        return try {
            Log.d(TAG, "🟡 Submitting application for job: ${application.jobId}")

            // Generate unique application ID
            val applicationWithId = application.copy(
                applicationId = "app_${System.currentTimeMillis()}",
                appliedAt = Date(),
                status = "pending"
            )

            // Save application to Firestore
            db.collection("applications")
                .document(applicationWithId.applicationId)
                .set(applicationWithId)
                .await()

            Log.d(TAG, "🟢 Application submitted successfully")
            Result.success(applicationWithId)
        } catch (e: Exception) {
            Log.e(TAG, "🔴 Error submitting application: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun checkIfApplied(jobId: String, freelancerId: String): Result<Boolean> {
        return try {
            Log.d(TAG, "🟡 Checking if user applied for job: $jobId")

            val snapshot = db.collection("applications")
                .whereEqualTo("jobId", jobId)
                .whereEqualTo("freelancerId", freelancerId)
                .get()
                .await()

            val hasApplied = !snapshot.isEmpty
            Log.d(TAG, "🟡 User has applied: $hasApplied")
            Result.success(hasApplied)
        } catch (e: Exception) {
            Log.e(TAG, "🔴 Error checking application status: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getApplicationsByFreelancer(freelancerId: String): Result<List<JobApplication>> {
        return try {
            val snapshot = db.collection("applications")
                .whereEqualTo("freelancerId", freelancerId)
                .orderBy("appliedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val applications = snapshot.toObjects(JobApplication::class.java)
            Result.success(applications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getApplicationsByClient(clientId: String): Result<List<JobApplication>> {
        return try {
            val snapshot = db.collection("applications")
                .whereEqualTo("clientId", clientId)
                .orderBy("appliedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val applications = snapshot.toObjects(JobApplication::class.java)
            Result.success(applications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateApplicationStatus(applicationId: String, status: String): Result<Boolean> {
        return try {
            db.collection("applications")
                .document(applicationId)
                .update("status", status)
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // === SAVED JOBS OPERATIONS ===

    suspend fun saveJob(freelancerId: String, jobId: String): Result<Boolean> {
        return try {
            val savedJobData = hashMapOf(
                "freelancerId" to freelancerId,
                "jobId" to jobId,
                "savedAt" to Date()
            )

            db.collection("saved_jobs").add(savedJobData).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unsaveJob(freelancerId: String, jobId: String): Result<Boolean> {
        return try {
            val querySnapshot = db.collection("saved_jobs")
                .whereEqualTo("freelancerId", freelancerId)
                .whereEqualTo("jobId", jobId)
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                for (document in querySnapshot.documents) {
                    document.reference.delete().await()
                }
            }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSavedJobs(freelancerId: String): Result<List<Job>> {
        return try {
            // First get all saved job IDs
            val savedJobsSnapshot = db.collection("saved_jobs")
                .whereEqualTo("freelancerId", freelancerId)
                .orderBy("savedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val jobIds = savedJobsSnapshot.documents.map { it["jobId"] as String }

            if (jobIds.isEmpty()) return Result.success(emptyList())

            // Then get the actual job details
            val jobsSnapshot = db.collection("jobs")
                .whereIn("jobId", jobIds)
                .get()
                .await()

            val jobs = jobsSnapshot.toObjects(Job::class.java)
            Result.success(jobs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isJobSaved(freelancerId: String, jobId: String): Result<Boolean> {
        return try {
            val querySnapshot = db.collection("saved_jobs")
                .whereEqualTo("freelancerId", freelancerId)
                .whereEqualTo("jobId", jobId)
                .get()
                .await()

            Result.success(!querySnapshot.isEmpty)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // === FREELANCER PROFILE OPERATIONS ===

    suspend fun getFreelancerSkills(freelancerId: String): Result<List<String>> {
        return try {
            val userDoc = db.collection("users").document(freelancerId).get().await()
            val skills = userDoc.get("skills") as? List<String> ?: emptyList()
            Result.success(skills)
        } catch (e: Exception) {
            Result.success(emptyList())
        }
    }

    suspend fun updateFreelancerSkills(freelancerId: String, skills: List<String>): Result<Boolean> {
        return try {
            db.collection("users")
                .document(freelancerId)
                .update("skills", skills)
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateFreelancerProfile(freelancerId: String, updates: Map<String, Any>): Result<Boolean> {
        return try {
            db.collection("users")
                .document(freelancerId)
                .update(updates)
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // === STATISTICS OPERATIONS ===

    data class FreelancerStats(
        val totalApplications: Int,
        val pendingApplications: Int,
        val acceptedApplications: Int,
        val totalEarnings: Double,
        val savedJobsCount: Int
    )

    suspend fun getFreelancerStats(freelancerId: String): Result<FreelancerStats> {
        return try {
            val applicationsResult = getApplicationsByFreelancer(freelancerId)
            if (applicationsResult.isFailure) {
                return Result.success(FreelancerStats(0, 0, 0, 0.0, 0))
            }

            val applications = applicationsResult.getOrNull() ?: emptyList()

            val stats = FreelancerStats(
                totalApplications = applications.size,
                pendingApplications = applications.count { it.status == "pending" },
                acceptedApplications = applications.count { it.status == "accepted" },
                totalEarnings = applications.filter { it.status == "accepted" }.sumOf { it.proposedBudget },
                savedJobsCount = getSavedJobs(freelancerId).getOrNull()?.size ?: 0
            )

            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getJobsByClient(clientId: String): Result<List<Job>> {
        return try {
            Log.d("FirebaseService", "📡 Fetching all jobs for client: $clientId")

            val snapshot = db.collection("jobs")
                // Filter by clientId, but NOT by status
                .whereEqualTo("clientId", clientId)
                .orderBy("postedAt", Query.Direction.DESCENDING) // Optional: order them
                .get()
                .await()

            val jobs = snapshot.toObjects(Job::class.java)

            Log.d("FirebaseService", "✅ Found ${jobs.size} jobs for client $clientId")

            Result.success(jobs)
        } catch (e: Exception) {
            Log.e("FirebaseService", "❌ Error fetching client jobs: ${e.message}", e)
            Result.failure(e)
        }
    }
}
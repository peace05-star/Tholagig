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
import kotlin.math.abs

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

    // Add this function to your FirebaseService class
    suspend fun getSimilarJobsAdvanced(currentJob: Job): Result<List<Job>> {
        return try {
            Log.d("FirebaseService", "🔍 Finding similar jobs for: ${currentJob.title}")
            Log.d("FirebaseService", "Category: ${currentJob.category}, Skills: ${currentJob.skillsRequired}")

            // Try multiple query strategies in sequence
            val similarJobs = findSimilarJobsWithMultipleStrategies(currentJob)

            Log.d("FirebaseService", "✅ Total similar jobs found: ${similarJobs.size}")
            Result.success(similarJobs)
        } catch (e: Exception) {
            Log.e("FirebaseService", "❌ Error in getSimilarJobsAdvanced: ${e.message}")
            Result.success(emptyList()) // Return empty instead of failure for graceful handling
        }
    }

    private suspend fun findSimilarJobsWithMultipleStrategies(currentJob: Job): List<Job> {
        val allPossibleJobs = mutableListOf<Job>()

        // Strategy 1: Same category + skills overlap (highest priority)
        val categorySkillsJobs = getJobsByCategoryAndSkills(currentJob)
        allPossibleJobs.addAll(categorySkillsJobs)
        Log.d("FirebaseService", "Strategy 1 - Category+Skills: ${categorySkillsJobs.size} jobs")

        // Strategy 2: Same category only
        if (allPossibleJobs.size < 4) {
            val categoryOnlyJobs = getJobsByCategoryOnly(currentJob)
                .filterNot { job -> allPossibleJobs.any { it.jobId == job.jobId } }
            allPossibleJobs.addAll(categoryOnlyJobs)
            Log.d("FirebaseService", "Strategy 2 - Category only: ${categoryOnlyJobs.size} jobs")
        }

        // Strategy 3: Skills overlap only (different category)
        if (allPossibleJobs.size < 4) {
            val skillsOnlyJobs = getJobsBySkillsOnly(currentJob)
                .filterNot { job -> allPossibleJobs.any { it.jobId == job.jobId } }
            allPossibleJobs.addAll(skillsOnlyJobs)
            Log.d("FirebaseService", "Strategy 3 - Skills only: ${skillsOnlyJobs.size} jobs")
        }

        // Strategy 4: Any recent jobs as fallback
        if (allPossibleJobs.size < 3) {
            val recentJobs = getRecentJobs(6)
                .filterNot { job -> allPossibleJobs.any { it.jobId == job.jobId } || job.jobId == currentJob.jobId }
            allPossibleJobs.addAll(recentJobs)
            Log.d("FirebaseService", "Strategy 4 - Recent jobs: ${recentJobs.size} jobs")
        }

        // Remove current job and duplicates, then score and sort by relevance
        return allPossibleJobs
            .distinctBy { it.jobId }
            .filter { it.jobId != currentJob.jobId }
            .map { job -> Pair(job, calculateRelevanceScore(currentJob, job)) }
            .sortedByDescending { it.second }
            .take(6) // Return top 6 most relevant
            .map { it.first }
    }

    private suspend fun getJobsByCategoryAndSkills(currentJob: Job): List<Job> {
        return try {
            // Get jobs in same category first
            val categoryQuery = db.collection("jobs")
                .whereEqualTo("category", currentJob.category)
                .whereNotEqualTo("jobId", currentJob.jobId)
                .limit(10)

            val snapshot = categoryQuery.get().await()
            val jobsInCategory = snapshot.toObjects(Job::class.java)

            // Filter by skills overlap
            jobsInCategory.filter { job ->
                val commonSkills = currentJob.skillsRequired.intersect(job.skillsRequired.toSet()).size
                commonSkills >= 1 // At least 1 common skill
            }
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error in getJobsByCategoryAndSkills: ${e.message}")
            emptyList()
        }
    }

    private suspend fun getJobsByCategoryOnly(currentJob: Job): List<Job> {
        return try {
            val query = db.collection("jobs")
                .whereEqualTo("category", currentJob.category)
                .whereNotEqualTo("jobId", currentJob.jobId)
                .limit(8)

            val snapshot = query.get().await()
            snapshot.toObjects(Job::class.java)
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error in getJobsByCategoryOnly: ${e.message}")
            emptyList()
        }
    }

    private suspend fun getJobsBySkillsOnly(currentJob: Job): List<Job> {
        return try {
            // Get recent jobs and filter by skills
            val recentJobs = getRecentJobs(15)
            recentJobs.filter { job ->
                job.jobId != currentJob.jobId &&
                        job.category != currentJob.category && // Different category but similar skills
                        currentJob.skillsRequired.intersect(job.skillsRequired.toSet()).isNotEmpty()
            }
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error in getJobsBySkillsOnly: ${e.message}")
            emptyList()
        }
    }

    private suspend fun getRecentJobs(limit: Int): List<Job> {
        return try {
            val query = db.collection("jobs")
                .orderBy("postedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit.toLong()) // Convert Int to Long

            val snapshot = query.get().await()
            snapshot.toObjects(Job::class.java)
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error in getRecentJobs: ${e.message}")
            emptyList()
        }
    }
    private fun calculateRelevanceScore(currentJob: Job, otherJob: Job): Double {
        var score = 0.0

        // 1. Category match (high weight)
        if (currentJob.category == otherJob.category) {
            score += 3.0
            Log.d("Relevance", "${otherJob.title}: +3.0 for category match")
        }

        // 2. Skills overlap (highest weight)
        val commonSkills = currentJob.skillsRequired.intersect(otherJob.skillsRequired.toSet()).size
        val totalSkills = currentJob.skillsRequired.size
        val skillsScore = if (totalSkills > 0) (commonSkills.toDouble() / totalSkills) * 4.0 else 0.0
        score += skillsScore
        Log.d("Relevance", "${otherJob.title}: +$skillsScore for $commonSkills/$totalSkills skills")

        // 3. Experience level match
        if (currentJob.experienceLevel == otherJob.experienceLevel) {
            score += 2.0
            Log.d("Relevance", "${otherJob.title}: +2.0 for experience level match")
        }

        // 4. Budget similarity
        val budgetDifference = abs(currentJob.budget - otherJob.budget)
        val budgetRatio = budgetDifference / maxOf(currentJob.budget, otherJob.budget)
        if (budgetRatio < 0.3) { // Within 30% budget range
            score += 1.5
            Log.d("Relevance", "${otherJob.title}: +1.5 for budget similarity")
        } else if (budgetRatio < 0.6) { // Within 60% budget range
            score += 0.5
            Log.d("Relevance", "${otherJob.title}: +0.5 for budget range")
        }

        Log.d("Relevance", "${otherJob.title}: TOTAL SCORE = $score")
        return score
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
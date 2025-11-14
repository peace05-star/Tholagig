package student.projects.tholagig.network

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import student.projects.tholagig.models.User
import student.projects.tholagig.models.Job
import student.projects.tholagig.models.JobApplication
import student.projects.tholagig.models.Message
import java.util.Date
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FirebaseService {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "FirebaseService"
    private val firestore: FirebaseFirestore = Firebase.firestore


    fun getFirestore(): FirebaseFirestore {
        return db
    }
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

    // === USER PROFILE UPDATE OPERATIONS ===

    suspend fun updateUser(userId: String, updateData: Map<String, Any>): Result<Boolean> {
        return try {
            Log.d(TAG, "🟡 Updating user profile for: $userId")
            Log.d(TAG, "🟡 Update data: $updateData")

            db.collection("users")
                .document(userId)
                .update(updateData)
                .await()

            Log.d(TAG, "🟢 User profile updated successfully")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "🔴 Error updating user profile: ${e.message}")
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


    // Add this function to your FirebaseService class
    suspend fun getSimilarJobsAdvanced(currentJob: Job): Result<List<Job>> {
        return try {
            Log.d("FirebaseService", "🔍 Finding similar jobs for: ${currentJob.title}")
            Log.d("FirebaseService", "Category: ${currentJob.category}, Skills: ${currentJob.skillsRequired}")

            // Try multiple query strategies in sequence
            val similarJobs = findSimilarJobsWithMultipleStrategies(currentJob)

            Log.d("FirebaseService", "✅ Total similar jobs found: ${similarJobs.size}")

            // ✅ ADD THIS: Final filtering - remove jobs that are too different
            val filteredJobs = similarJobs.filter { job ->
                isJobTrulySimilar(currentJob, job)
            }

            Log.d("FirebaseService", "✅ After final filtering: ${filteredJobs.size} truly similar jobs")
            Result.success(filteredJobs.take(6))
        } catch (e: Exception) {
            Log.e("FirebaseService", "❌ Error in getSimilarJobsAdvanced: ${e.message}")
            Result.success(emptyList())
        }
    }

    // ✅ ADD THIS FUNCTION: Strict similarity checking
    private fun isJobTrulySimilar(currentJob: Job, otherJob: Job): Boolean {
        // Must have at least ONE of these similarity criteria:

        // 1. Same category AND at least 1 common skill
        val sameCategoryAndSkills = currentJob.category.equals(otherJob.category, ignoreCase = true) &&
                currentJob.skillsRequired.intersect(otherJob.skillsRequired.toSet()).isNotEmpty()

        // 2. Different category but STRONG skills overlap (at least 2 common skills)
        val strongSkillsOverlap = !currentJob.category.equals(otherJob.category, ignoreCase = true) &&
                currentJob.skillsRequired.intersect(otherJob.skillsRequired.toSet()).size >= 2

        // 3. Same experience level AND budget within 50% range
        val similarLevelAndBudget = currentJob.experienceLevel.equals(otherJob.experienceLevel, ignoreCase = true) &&
                abs(currentJob.budget - otherJob.budget) / maxOf(currentJob.budget, otherJob.budget) < 0.5

        val isSimilar = sameCategoryAndSkills || strongSkillsOverlap || similarLevelAndBudget

        Log.d("SimilarityCheck",
            "Comparing: '${currentJob.title}' (${currentJob.category}) vs '${otherJob.title}' (${otherJob.category})\n" +
                    "Same Category: ${currentJob.category.equals(otherJob.category, ignoreCase = true)}\n" +
                    "Common Skills: ${currentJob.skillsRequired.intersect(otherJob.skillsRequired.toSet())}\n" +
                    "Common Skills Count: ${currentJob.skillsRequired.intersect(otherJob.skillsRequired.toSet()).size}\n" +
                    "Same Experience Level: ${currentJob.experienceLevel.equals(otherJob.experienceLevel, ignoreCase = true)}\n" +
                    "Budget Similar: ${abs(currentJob.budget - otherJob.budget) / maxOf(currentJob.budget, otherJob.budget) < 0.5}\n" +
                    "FINAL DECISION: $isSimilar\n" +
                    "---"
        )

        return isSimilar
    }

    private suspend fun findSimilarJobsWithMultipleStrategies(currentJob: Job): List<Job> {
        val allPossibleJobs = mutableListOf<Job>()

        // Strategy 1: Same category + skills overlap (highest priority)
        val categorySkillsJobs = getJobsByCategoryAndSkills(currentJob)
        allPossibleJobs.addAll(categorySkillsJobs)
        Log.d("FirebaseService", "Strategy 1 - Category+Skills: ${categorySkillsJobs.size} jobs")

        // Strategy 2: Same category only (but filter out completely unrelated ones)
        if (allPossibleJobs.size < 4) {
            val categoryOnlyJobs = getJobsByCategoryOnly(currentJob)
                .filterNot { job -> allPossibleJobs.any { it.jobId == job.jobId } }
                .filter { job ->
                    // Only include if they share at least some relevance
                    job.experienceLevel.equals(currentJob.experienceLevel, ignoreCase = true) ||
                            abs(job.budget - currentJob.budget) / maxOf(job.budget, currentJob.budget) < 0.7
                }
            allPossibleJobs.addAll(categoryOnlyJobs)
            Log.d("FirebaseService", "Strategy 2 - Category only (filtered): ${categoryOnlyJobs.size} jobs")
        }

        // Strategy 3: Skills overlap only (different category) - be more strict
        if (allPossibleJobs.size < 4) {
            val skillsOnlyJobs = getJobsBySkillsOnly(currentJob)
                .filterNot { job -> allPossibleJobs.any { it.jobId == job.jobId } }
                .filter { job ->
                    // Require at least 2 common skills for different categories
                    currentJob.skillsRequired.intersect(job.skillsRequired.toSet()).size >= 2
                }
            allPossibleJobs.addAll(skillsOnlyJobs)
            Log.d("FirebaseService", "Strategy 3 - Skills only (strict): ${skillsOnlyJobs.size} jobs")
        }

        // Strategy 4: Any recent jobs as fallback (only if we have very few results)
        if (allPossibleJobs.size < 2) {
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
            .take(8) // Take a few more for final filtering
            .map { it.first }
    }

    private suspend fun getJobsByCategoryAndSkills(currentJob: Job): List<Job> {
        return try {
            // Get jobs in same category first
            val categoryQuery = db.collection("jobs")
                .whereEqualTo("category", currentJob.category)
                .whereNotEqualTo("jobId", currentJob.jobId)
                .limit(10L) // Change to Long

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
                .limit(8L) // Change to Long

            val snapshot = query.get().await()
            snapshot.toObjects(Job::class.java)
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error in getJobsByCategoryOnly: ${e.message}")
            emptyList()
        }
    }

    suspend fun getSimilarJobs(jobId: String, category: String): Result<List<Job>> {
        return try {
            val snapshot = db.collection("jobs")
                .whereEqualTo("category", category)
                .whereEqualTo("status", "open")
                .whereNotEqualTo("jobId", jobId)
                .limit(5L) // Change to Long
                .get()
                .await()

            val jobs = snapshot.toObjects(Job::class.java)
            Result.success(jobs)
        } catch (e: Exception) {
            Result.success(emptyList())
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

            // Auto-create a welcome message to start conversation
            val welcomeMessage = Message(
                messageId = "msg_${System.currentTimeMillis()}",
                conversationId = if (application.freelancerId < application.clientId)
                    "${application.freelancerId}-${application.clientId}"
                else
                    "${application.clientId}-${application.freelancerId}",
                senderId = application.clientId,
                receiverId = application.freelancerId,
                content = "Thanks for applying to '${application.jobTitle}'! I'll review your application and get back to you soon.",
                timestamp = Date(),
                isRead = false
            )

            // Send the welcome message
            sendMessage(welcomeMessage)
            Log.d(TAG, "💬 Auto-created welcome message for application")

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

    suspend fun getApplicationsByClient(clientId: String): Result<List<JobApplication>> = withContext(Dispatchers.IO) {
        try {
            Log.d("FirebaseService", "📡 Fetching applications for client: $clientId")

            val snapshot = db.collection("applications")
                .whereEqualTo("clientId", clientId)
                .get()
                .await()

            Log.d("FirebaseService", "✅ Firestore query completed, documents: ${snapshot.documents.size}")

            val applications = snapshot.documents.mapNotNull { document ->
                try {
                    Log.d("FirebaseService", "📄 Processing application document: ${document.id}")
                    val application = document.toObject(JobApplication::class.java)
                    if (application != null) {
                        Log.d("FirebaseService", "✅ Loaded application: ${application.jobTitle} | Status: ${application.status} | Client: ${application.clientId}")
                    } else {
                        Log.e("FirebaseService", "❌ Failed to convert document to JobApplication: ${document.id}")
                    }
                    application
                } catch (e: Exception) {
                    Log.e("FirebaseService", "💥 Error parsing application ${document.id}: ${e.message}")
                    null
                }
            }

            Log.d("FirebaseService", "🎯 Final applications count for client $clientId: ${applications.size}")
            Result.success(applications)

        } catch (e: Exception) {
            Log.e("FirebaseService", "💥 ERROR in getApplicationsByClient: ${e.message}", e)
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

    suspend fun saveJobApplication(application: JobApplication): Result<Boolean> {
        return try {
            firestore.collection("jobApplications")
                .document(application.applicationId)
                .set(application)
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // === REAL-TIME MESSAGING METHODS ===

    suspend fun sendMessage(message: Message): Result<Boolean> {
        return try {
            Log.d(TAG, "🟡 Sending message: ${message.content}")

            // Save message to Firestore
            db.collection("messages")
                .document(message.messageId)
                .set(message)
                .await()

            Log.d(TAG, "🟢 Message saved successfully")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "🔴 Error sending message: ${e.message}")
            Result.failure(e)
        }
    }

    fun listenToMessages(
        conversationId: String,
        onMessagesReceived: (List<Message>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return db.collection("messages")
            .whereEqualTo("conversationId", conversationId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(Exception(error.message))
                    return@addSnapshotListener
                }

                snapshot?.let { querySnapshot ->
                    val messages = mutableListOf<Message>()
                    for (document in querySnapshot.documents) {
                        try {
                            val message = document.toObject(Message::class.java)
                            message?.let { messages.add(it) }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error converting message: ${e.message}")
                        }
                    }
                    onMessagesReceived(messages)
                }
            }
    }


    suspend fun getUserName(userId: String): Result<String> {
        return try {
            val userDoc = db.collection("users").document(userId).get().await()
            if (userDoc.exists()) {
                val name = userDoc.getString("fullName") ?: "User"
                Result.success(name)
            } else {
                Result.success("Unknown User")
            }
        } catch (e: Exception) {
            Result.success("Unknown User")
        }
    }

    /**
     * Create a simple notification in Firestore (for demo purposes)
     */
    suspend fun createDemoNotification(
        receiverId: String,
        title: String,
        message: String,
        conversationId: String
    ): Result<Boolean> {
        return try {
            val notificationData = hashMapOf(
                "receiverId" to receiverId,
                "title" to title,
                "message" to message,
                "conversationId" to conversationId,
                "timestamp" to com.google.firebase.Timestamp.now(),
                "read" to false
            )

            db.collection("demo_notifications")
                .add(notificationData)
                .await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all conversations for a user
     */
    suspend fun getUserConversations(userId: String): Result<List<Message>> {
        return try {
            Log.d(TAG, "🟡 Getting conversations for user: $userId")

            // Get messages where user is either sender or receiver
            val sentSnapshot = db.collection("messages")
                .whereEqualTo("senderId", userId)
                .get()
                .await()

            val receivedSnapshot = db.collection("messages")
                .whereEqualTo("receiverId", userId)
                .get()
                .await()

            val allMessages = mutableListOf<Message>()
            allMessages.addAll(sentSnapshot.toObjects(Message::class.java))
            allMessages.addAll(receivedSnapshot.toObjects(Message::class.java))

            // Sort by timestamp (newest first)
            val sortedMessages = allMessages.sortedByDescending { it.timestamp }

            Log.d(TAG, "🟢 Found ${sortedMessages.size} messages for user")
            Result.success(sortedMessages)
        } catch (e: Exception) {
            Log.e(TAG, "🔴 Error getting conversations: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Mark messages as read in a conversation
     */
    suspend fun markConversationAsRead(otherUserId: String, currentUserId: String): Result<Boolean> {
        return try {
            val conversationId = if (currentUserId < otherUserId)
                "$currentUserId-$otherUserId"
            else
                "$otherUserId-$currentUserId"

            val snapshot = db.collection("messages")
                .whereEqualTo("conversationId", conversationId)
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("isRead", false)
                .get()
                .await()

            val batch = db.batch()
            for (document in snapshot.documents) {
                batch.update(document.reference, "isRead", true)
            }

            if (!snapshot.isEmpty) {
                batch.commit().await()
                Log.d(TAG, "🟢 Marked ${snapshot.documents.size} messages as read")
            }

            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "🔴 Error marking messages as read: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get user's name by ID
     */
    suspend fun getUserByIdForMessaging(userId: String): Result<String> {
        return try {
            val userDoc = db.collection("users").document(userId).get().await()
            if (userDoc.exists()) {
                val name = userDoc.getString("fullName") ?:
                userDoc.getString("username") ?:
                userDoc.getString("email")?.substringBefore("@") ?:
                "User"
                Result.success(name)
            } else {
                Result.success("Unknown User")
            }
        } catch (e: Exception) {
            Log.e(TAG, "🔴 Error getting user name: ${e.message}")
            Result.success("Unknown User")
        }
    }

    // Add this to your FirebaseService class
    fun listenForNewMessages(userId: String, onNewMessage: (Message) -> Unit) {
        try {
            db.collection("messages")
                .whereEqualTo("receiverId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FIREBASE_SERVICE", "❌ Error listening to messages: ${error.message}")
                        return@addSnapshotListener
                    }

                    snapshot?.documentChanges?.forEach { change ->
                        if (change.type == DocumentChange.Type.ADDED) {
                            val message = change.document.toObject(Message::class.java)
                            Log.d("FIREBASE_SERVICE", "📨 New message detected for user $userId")
                            onNewMessage(message)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e("FIREBASE_SERVICE", "❌ Error setting up message listener: ${e.message}")
        }
    }

    // Add these methods to your FirebaseService class

    /**
     * Get user by email for SSO login
     */
    private suspend fun getUserByEmail(email: String): User? {
        return try {
            Log.d(TAG, "🟡 Searching for user by email: $email")

            val query = db.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()

            if (!query.isEmpty) {
                val document = query.documents[0]
                val user = document.toObject(User::class.java)
                Log.d(TAG, "🟢 User found by email: ${user?.fullName}")
                user
            } else {
                Log.d(TAG, "🟡 No user found with email: $email")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "🔴 Error getting user by email: ${e.message}")
            null
        }
    }

    /**
     * Enhanced SSO registration/login method
     */
    suspend fun registerOrLoginWithSSO(user: User): Result<User> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "🟡 Starting SSO registration/login for: ${user.email}")

            // Check if user already exists in Firestore
            val existingUser = getUserByEmail(user.email)

            val userToSave = if (existingUser != null) {
                Log.d(TAG, "🟡 User exists, updating with SSO info")
                // Update existing user with SSO info and last login
                existingUser.copy(
                    isSSO = true,
                    ssoProvider = user.ssoProvider,
                    ssoId = user.ssoId,
                    profileImage = user.profileImage ?: existingUser.profileImage,
                    lastLogin = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                Log.d(TAG, "🟡 Creating new SSO user")
                // Create new user with SSO info
                user.copy(
                    userId = user.userId, // Use the Firebase Auth UID
                    createdAt = System.currentTimeMillis(),
                    lastLogin = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            }

            // Save user to Firestore
            val saveResult = saveUser(userToSave)

            if (saveResult.isSuccess) {
                Log.d(TAG, "🟢 SSO User ${if (existingUser != null) "updated" else "registered"}: ${user.email}")
                Result.success(userToSave)
            } else {
                Log.e(TAG, "🔴 Failed to save SSO user")
                Result.failure(Exception("Failed to save user data"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "🔴 SSO authentication error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Save user to Firestore
     */
    private suspend fun saveUser(user: User): Result<User> {
        return try {
            Log.d(TAG, "🟡 Saving user to Firestore: ${user.userId}")

            db.collection("users")
                .document(user.userId)
                .set(user)
                .await()

            Log.d(TAG, "🟢 User saved successfully: ${user.userId}")
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "🔴 Error saving user: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get user profile picture URL
     */
    suspend fun getUserProfilePicture(userId: String): Result<String> {
        return try {
            val userDoc = db.collection("users").document(userId).get().await()

            if (userDoc.exists()) {
                val profileImage = userDoc.getString("profileImage")
                if (!profileImage.isNullOrEmpty()) {
                    Result.success(profileImage)
                } else {
                    // Return default avatar or empty string
                    Result.success("")
                }
            } else {
                Result.success("")
            }
        } catch (e: Exception) {
            Log.e(TAG, "🔴 Error getting profile picture: ${e.message}")
            Result.success("") // Return empty string instead of failure
        }
    }

    /**
     * Update user profile picture
     */
    suspend fun updateUserProfilePicture(userId: String, imageUrl: String): Result<Boolean> {
        return try {
            db.collection("users")
                .document(userId)
                .update("profileImage", imageUrl)
                .await()

            Log.d(TAG, "🟢 Profile picture updated for user: $userId")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "🔴 Error updating profile picture: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Copy user data (useful for creating new users from existing ones)
     */
    suspend fun copyUserData(sourceUserId: String, targetUserId: String): Result<Boolean> {
        return try {
            val sourceUserDoc = db.collection("users").document(sourceUserId).get().await()

            if (sourceUserDoc.exists()) {
                val sourceUser = sourceUserDoc.toObject(User::class.java)
                if (sourceUser != null) {
                    val copiedUser = sourceUser.copy(
                        userId = targetUserId,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )

                    saveUser(copiedUser)
                    Log.d(TAG, "🟢 User data copied from $sourceUserId to $targetUserId")
                    Result.success(true)
                } else {
                    Result.failure(Exception("Failed to convert source user data"))
                }
            } else {
                Result.failure(Exception("Source user not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "🔴 Error copying user data: ${e.message}")
            Result.failure(e)
        }
    }
    private fun showLocalNotification(message: Message) {
        // This would create a system notification
        // You'd need to implement NotificationCompat.Builder here
    }

    // Add to FirebaseService class
    suspend fun saveFCMToken(userId: String, token: String): Result<Boolean> {
        return try {
            db.collection("users")
                .document(userId)
                .update("fcmToken", token)
                .await()
            Log.d(TAG, "✅ FCM token saved for user: $userId")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving FCM token: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getFCMToken(userId: String): Result<String> {
        return try {
            val userDoc = db.collection("users").document(userId).get().await()
            val token = userDoc.getString("fcmToken") ?: ""
            Result.success(token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // In FirebaseService - enhance sendMessage function
    suspend fun sendMessageWithNotification(message: Message): Result<Boolean> {
        return try {
            // 1. Save message to Firestore
            db.collection("messages")
                .document(message.messageId)
                .set(message)
                .await()

            // 2. Get receiver's FCM token and send notification
            val receiverTokenResult = getFCMToken(message.receiverId)
            if (receiverTokenResult.isSuccess) {
                val token = receiverTokenResult.getOrNull()
                if (!token.isNullOrEmpty()) {
                    // You can send a push notification here via:
                    // - Firebase Cloud Functions
                    // - Your own server
                    // - Direct FCM API call (for testing)
                    Log.d(TAG, "📲 Would send push notification to token: $token")
                }
            }

            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "🔴 Error sending message with notification: ${e.message}")
            Result.failure(e)
        }
    }
}
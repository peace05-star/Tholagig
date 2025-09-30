package student.projects.tholagig.network

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import student.projects.tholagig.models.User
import student.projects.tholagig.models.Job
import student.projects.tholagig.models.JobApplication

class FirebaseService {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "FirebaseService"

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

    suspend fun getJobs(): Result<List<Job>> {
        return try {
            val snapshot = db.collection("jobs").get().await()
            val jobs = snapshot.toObjects(Job::class.java)
            Result.success(jobs)
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

    suspend fun submitApplication(application: JobApplication): Result<JobApplication> {
        return try {
            Log.d(TAG, "🟡 Submitting application for job: ${application.jobId}")

            // Save application to Firestore
            db.collection("applications")
                .document(application.applicationId)
                .set(application)
                .await()

            Log.d(TAG, "🟢 Application submitted successfully")
            Result.success(application)
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
                .get()
                .await()

            val applications = snapshot.toObjects(JobApplication::class.java)
            Result.success(applications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
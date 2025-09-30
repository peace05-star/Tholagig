package student.projects.tholagig.jobs

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.*
import student.projects.tholagig.R
import student.projects.tholagig.Adapters.JobsAdapter
import student.projects.tholagig.models.Job
import student.projects.tholagig.models.JobApplication
import student.projects.tholagig.network.FirebaseService
import student.projects.tholagig.network.SessionManager
import java.text.SimpleDateFormat
import java.util.*

class JobDetailsActivity : AppCompatActivity() {

    private lateinit var tvJobTitle: TextView
    private lateinit var tvClientName: TextView
    private lateinit var tvBudget: TextView
    private lateinit var tvDeadline: TextView
    private lateinit var tvCategory: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvClientCompany: TextView
    private lateinit var tvClientRating: TextView
    private lateinit var tvClientBio: TextView
    private lateinit var btnApply: Button
    private lateinit var btnSave: ImageButton
    private lateinit var chipGroupSkills: ChipGroup
    private lateinit var rvSimilarJobs: RecyclerView
    private lateinit var progressBar: ProgressBar

    private lateinit var sessionManager: SessionManager
    private lateinit var firebaseService: FirebaseService
    private lateinit var similarJobsAdapter: JobsAdapter

    private var currentJob: Job? = null
    private var hasApplied = false
    private var isSaved = false
    private val similarJobs = mutableListOf<Job>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_job_details)

        sessionManager = SessionManager(this)
        firebaseService = FirebaseService()

        initializeViews()
        setupRecyclerView()
        setupClickListeners()
        loadJobDetails()
        checkApplicationStatus()
    }

    private fun initializeViews() {
        tvJobTitle = findViewById(R.id.tvJobTitle)
        tvClientName = findViewById(R.id.tvClientName)
        tvBudget = findViewById(R.id.tvBudget)
        tvDeadline = findViewById(R.id.tvDeadline)
        tvCategory = findViewById(R.id.tvCategory)
        tvLocation = findViewById(R.id.tvLocation)
        tvDescription = findViewById(R.id.tvDescription)
        tvClientCompany = findViewById(R.id.tvClientCompany)
        tvClientRating = findViewById(R.id.tvClientRating)
        tvClientBio = findViewById(R.id.tvClientBio)
        btnApply = findViewById(R.id.btnApply)
        btnSave = findViewById(R.id.btnSave)
        chipGroupSkills = findViewById(R.id.chipGroupSkills)
        rvSimilarJobs = findViewById(R.id.rvSimilarJobs)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupRecyclerView() {
        similarJobsAdapter = JobsAdapter(similarJobs) { job ->
            // Navigate to this similar job
            val intent = Intent(this, JobDetailsActivity::class.java).apply {
                putExtra("JOB_ID", job.jobId)
                putExtra("JOB_TITLE", job.title)
            }
            startActivity(intent)
        }
        rvSimilarJobs.apply {
            layoutManager = LinearLayoutManager(this@JobDetailsActivity)
            adapter = similarJobsAdapter
        }
    }

    private fun setupClickListeners() {
        btnApply.setOnClickListener {
            if (hasApplied) {
                Toast.makeText(this, "You have already applied for this job", Toast.LENGTH_SHORT).show()
            } else {
                applyForJob()
            }
        }

        btnSave.setOnClickListener {
            toggleSaveJob()
        }
    }

    private fun loadJobDetails() {
        progressBar.visibility = View.VISIBLE

        val jobId = intent.getStringExtra("JOB_ID") ?: ""
        val jobTitle = intent.getStringExtra("JOB_TITLE") ?: ""

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // In a real app, fetch job details from Firebase
                // For now, we'll enhance the mock data
                val mockJob = createEnhancedMockJob(jobId, jobTitle)
                currentJob = mockJob

                // Load similar jobs
                loadSimilarJobs(mockJob.category)

                withContext(Dispatchers.Main) {
                    displayJobDetails(mockJob)
                    progressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@JobDetailsActivity,
                        "Error loading job details: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun createEnhancedMockJob(jobId: String, title: String): Job {
        return Job(
            jobId = jobId,
            title = title,
            clientId = "client_123",
            clientName = "Sarah Johnson",
            description = "We are looking for an experienced mobile app developer to build a modern e-commerce application from scratch. The ideal candidate should have extensive experience with Kotlin, Android SDK, and Firebase.\n\nResponsibilities:\n• Develop and maintain advanced Android applications\n• Collaborate with cross-functional teams\n• Implement clean, modern UI/UX designs\n• Integrate with REST APIs and third-party services\n• Ensure application performance and quality\n\nRequirements:\n• 3+ years of Android development experience\n• Strong knowledge of Kotlin and Java\n• Experience with Firebase, Retrofit, and Room\n• Understanding of Material Design principles\n• Experience with version control (Git)",
            budget = 15000.0,
            category = "Mobile Development",
            skillsRequired = listOf("Kotlin", "Android SDK", "Firebase", "REST APIs", "Material Design", "Git"),
            location = "Remote",
            deadline = Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000),
            postedAt = Date(System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000)
        )
    }

    private fun displayJobDetails(job: Job) {
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

        tvJobTitle.text = job.title
        tvClientName.text = "Posted by ${job.clientName}"
        tvBudget.text = "R ${job.budget.toInt()}"
        tvDeadline.text = "Due: ${dateFormat.format(job.deadline)}"
        tvCategory.text = job.category
        tvLocation.text = job.location
        tvDescription.text = job.description

        // Mock client information
        tvClientCompany.text = "Tech Innovations Ltd."
        tvClientRating.text = "⭐ 4.8 (24 reviews)"
        tvClientBio.text = "Tech Innovations is a leading software development company specializing in mobile and web applications. We've been delivering high-quality solutions for over 5 years."

        // Add skills chips
        chipGroupSkills.removeAllViews()
        job.skillsRequired.forEach { skill ->
            val chip = Chip(this).apply {
                text = skill
                setChipBackgroundColorResource(R.color.light_gray)
                setTextColor(resources.getColor(R.color.black))
                isClickable = false
            }
            chipGroupSkills.addView(chip)
        }
    }

    private fun loadSimilarJobs(category: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = firebaseService.getJobs()

                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        val allJobs = result.getOrNull() ?: emptyList()
                        // Filter similar jobs by category, excluding current job
                        similarJobs.clear()
                        similarJobs.addAll(
                            allJobs.filter {
                                it.category == category && it.jobId != currentJob?.jobId
                            }.take(3) // Show max 3 similar jobs
                        )
                        similarJobsAdapter.notifyDataSetChanged()
                    }
                }
            } catch (e: Exception) {
                Log.e("JobDetails", "Error loading similar jobs: ${e.message}")
            }
        }
    }

    private fun checkApplicationStatus() {
        val userId = sessionManager.getUserId() ?: ""
        val jobId = intent.getStringExtra("JOB_ID") ?: ""

        if (userId.isEmpty() || jobId.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Check if user has already applied
                val result = firebaseService.checkIfApplied(jobId, userId)

                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        hasApplied = result.getOrNull() ?: false
                        updateUIState()
                    }
                }
            } catch (e: Exception) {
                Log.e("JobDetails", "Error checking application status: ${e.message}")
            }
        }
    }

    private fun applyForJob() {
        val userId = sessionManager.getUserId() ?: ""
        val userEmail = sessionManager.getEmail() ?: ""
        val job = currentJob ?: return

        if (userId.isEmpty()) {
            Toast.makeText(this, "Please login to apply for jobs", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        btnApply.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get user's actual name from profile
                val userResult = firebaseService.getUserById(userId)
                val userName = if (userResult.isSuccess) {
                    userResult.getOrNull()?.fullName ?: "Freelancer"
                } else {
                    "Freelancer"
                }

                // Create job application with proper named parameters
                val application = JobApplication(
                    applicationId = "app_${System.currentTimeMillis()}",
                    jobId = job.jobId,
                    freelancerId = userId,
                    freelancerName = userName,
                    freelancerEmail = userEmail,
                    coverLetter = "I'm interested in this position and believe my skills are a great match for your requirements. I have experience with ${job.skillsRequired.joinToString(", ")} and I'm confident I can deliver high-quality results for your project.",
                    proposedBudget = job.budget,
                    status = "pending",
                    appliedAt = Date(),
                    clientId = job.clientId,
                    jobTitle = job.title,
                    clientName = job.clientName
                )

                // TODO: Implement actual application submission to Firebase
                // val result = firebaseService.submitApplication(application)

                // For now, we'll simulate the API call
                simulateApplicationSubmission(application)

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnApply.isEnabled = true
                    Toast.makeText(
                        this@JobDetailsActivity,
                        "Error submitting application: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private suspend fun simulateApplicationSubmission(application: JobApplication) {
        // Simulate API call delay
        delay(2000)

        withContext(Dispatchers.Main) {
            progressBar.visibility = View.GONE
            btnApply.isEnabled = true
            hasApplied = true
            updateUIState()

            // Show success message with application details
            val successMessage = """
            🎉 Application Submitted!
            
            Position: ${application.jobTitle}
            Client: ${application.clientName}
            Proposed Budget: R ${application.proposedBudget.toInt()}
            
            You will be notified when the client reviews your application.
        """.trimIndent()

            androidx.appcompat.app.AlertDialog.Builder(this@JobDetailsActivity)
                .setTitle("Application Sent!")
                .setMessage(successMessage)
                .setPositiveButton("Great!") { dialog, which ->
                    // Optionally navigate back or stay on the page
                }
                .show()
        }
    }

    private fun toggleSaveJob() {
        isSaved = !isSaved
        updateUIState()

        val message = if (isSaved) "Job saved to favorites!" else "Job removed from favorites"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

        // TODO: Implement actual save/unsave to Firebase
    }

    private fun updateUIState() {
        // Update apply button
        if (hasApplied) {
            btnApply.text = "Applied ✓"
            btnApply.backgroundTintList = resources.getColorStateList(R.color.green)
            btnApply.isEnabled = false
        } else {
            btnApply.text = "Apply for Job"
            btnApply.backgroundTintList = resources.getColorStateList(R.color.orange)
            btnApply.isEnabled = true
        }

        // Update save button
        val saveIcon = if (isSaved) {
            R.drawable.ic_bookmark_filled
        } else {
            R.drawable.ic_bookmark_border
        }
        btnSave.setImageResource(saveIcon)
    }
}
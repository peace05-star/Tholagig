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
import student.projects.tholagig.dialogs.ApplicationFormDialog
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
    private lateinit var tvSimilarJobsTitle: TextView
    private lateinit var tvPostedDate: TextView
    private lateinit var tvExperienceLevel: TextView

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
        checkIfJobSaved()
        checkIfJobOwner()
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
        tvSimilarJobsTitle = findViewById(R.id.tvSimilarJobsTitle)
        tvPostedDate = findViewById(R.id.tvPostedDate)
        tvExperienceLevel = findViewById(R.id.tvExperienceLevel)
    }

    private fun setupRecyclerView() {
        similarJobsAdapter = JobsAdapter(similarJobs) { job ->
            // Navigate to this similar job
            val intent = Intent(this, JobDetailsActivity::class.java).apply {
                putExtra("JOB_ID", job.jobId)
            }
            startActivity(intent)
        }
        rvSimilarJobs.apply {
            layoutManager = LinearLayoutManager(this@JobDetailsActivity, LinearLayoutManager.HORIZONTAL, false)
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

        // Initialize back button
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            onBackPressed()
        }
    }

    private fun loadJobDetails() {
        progressBar.visibility = View.VISIBLE

        val jobId = intent.getStringExtra("JOB_ID") ?: ""

        if (jobId.isEmpty()) {
            Toast.makeText(this, "Job not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Fetch job from Firebase
                val jobResult = firebaseService.getJobById(jobId)

                if (jobResult.isSuccess) {
                    currentJob = jobResult.getOrNull()
                    Log.d("JobDetails", "Loaded job from Firebase: ${currentJob?.title}")

                    withContext(Dispatchers.Main) {
                        currentJob?.let { job ->
                            displayJobDetails(job)
                            // Load similar jobs based on actual job category
                            loadSimilarJobs(job.category)
                        } ?: run {
                            Toast.makeText(this@JobDetailsActivity, "Job not found", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@JobDetailsActivity, "Failed to load job: ${jobResult.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@JobDetailsActivity,
                        "Error loading job details: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("JobDetails", "Error loading job: ${e.message}", e)
                }
            }
        }
    }

    private fun displayJobDetails(job: Job) {
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        val postedDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        tvJobTitle.text = job.title
        tvClientName.text = "Posted by ${job.clientName}"
        tvBudget.text = "R ${job.budget.toInt()}"
        tvDeadline.text = "Due: ${dateFormat.format(job.deadline)}"
        tvCategory.text = job.category
        tvLocation.text = job.location
        tvDescription.text = job.description
        tvPostedDate.text = "Posted: ${postedDateFormat.format(job.postedAt)}"

        // Display client information from actual job data
        tvClientCompany.text = job.company ?: "Independent Client"
        tvClientRating.text = "⭐ ${job.clientRating ?: "4.5"} (${job.totalReviews ?: "10"} reviews)"
        tvClientBio.text = job.clientBio ?: "Experienced client looking for quality work."

        // Experience level
        tvExperienceLevel.text = when (job.experienceLevel ?: "Intermediate") {
            "Beginner" -> "👶 Beginner Level"
            "Intermediate" -> "🚀 Intermediate Level"
            "Expert" -> "🏆 Expert Level"
            else -> "🚀 ${job.experienceLevel}"
        }

        // Add skills chips
        chipGroupSkills.removeAllViews()
        job.skillsRequired.forEach { skill ->
            val chip = Chip(this).apply {
                text = skill
                setChipBackgroundColorResource(R.color.light_gray)
                setTextColor(resources.getColor(R.color.black))
                isClickable = false
                setPadding(32, 16, 32, 16)
            }
            chipGroupSkills.addView(chip)
        }

        progressBar.visibility = View.GONE
        checkIfJobOwner()
    }

    private fun loadSimilarJobs(category: String) {
        val currentJob = currentJob ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("JobDetails", "=== STARTING ENHANCED SIMILAR JOBS SEARCH ===")
                Log.d("JobDetails", "Current Job: ${currentJob.title}")
                Log.d("JobDetails", "Category: $category")
                Log.d("JobDetails", "Skills: ${currentJob.skillsRequired}")

                // Use the advanced similar jobs function instead of the basic one
                val result = firebaseService.getSimilarJobsAdvanced(currentJob)

                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        val similarJobsList = result.getOrNull() ?: emptyList()
                        similarJobs.clear()
                        similarJobs.addAll(similarJobsList)
                        similarJobsAdapter.notifyDataSetChanged()

                        Log.d("JobDetails", "=== ENHANCED SEARCH COMPLETE ===")
                        Log.d("JobDetails", "Final similar jobs count: ${similarJobs.size}")

                        // Log each similar job found
                        similarJobs.forEachIndexed { index, job ->
                            Log.d("JobDetails", "Similar job ${index + 1}: ${job.title} (${job.category}) - Skills: ${job.skillsRequired}")
                        }

                        // Update UI
                        tvSimilarJobsTitle.text = when {
                            similarJobs.isEmpty() -> "No similar jobs found"
                            similarJobs.size == 1 -> "Similar Job (1)"
                            else -> "Similar Jobs (${similarJobs.size})"
                        }

                    } else {
                        Log.e("JobDetails", "Failed to load similar jobs")
                        tvSimilarJobsTitle.text = "Error loading similar jobs"
                    }
                }
            } catch (e: Exception) {
                Log.e("JobDetails", "Error in loadSimilarJobs: ${e.message}")
                withContext(Dispatchers.Main) {
                    tvSimilarJobsTitle.text = "Error loading similar jobs"
                }
            }
        }
    }

    private fun checkIfJobOwner() {
        val currentUserId = sessionManager.getUserId() ?: ""
        val jobClientId = currentJob?.clientId ?: ""

        // If current user is the job owner, hide the apply button
        if (currentUserId == jobClientId) {
            btnApply.visibility = View.GONE
            Toast.makeText(this, "This is your job posting", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkApplicationStatus() {
        val userId = sessionManager.getUserId() ?: ""
        val jobId = currentJob?.jobId ?: intent.getStringExtra("JOB_ID") ?: ""

        if (userId.isEmpty() || jobId.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
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

    private fun checkIfJobSaved() {
        val userId = sessionManager.getUserId() ?: ""
        val jobId = currentJob?.jobId ?: intent.getStringExtra("JOB_ID") ?: ""

        if (userId.isEmpty() || jobId.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = firebaseService.isJobSaved(userId, jobId)

                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        isSaved = result.getOrNull() ?: false
                        updateUIState()
                    }
                }
            } catch (e: Exception) {
                Log.e("JobDetails", "Error checking saved status: ${e.message}")
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

        // Show loading while fetching user data
        progressBar.visibility = View.VISIBLE
        btnApply.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get user data for the form
                val userResult = firebaseService.getUserById(userId)
                val skillsResult = firebaseService.getFreelancerSkills(userId)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnApply.isEnabled = true

                    if (userResult.isSuccess && skillsResult.isSuccess) {
                        val user = userResult.getOrNull()
                        val userSkills = skillsResult.getOrNull() ?: emptyList()
                        val userName = user?.fullName ?: "Freelancer"

                        // Show application form dialog
                        showApplicationForm(job, userSkills, userName, userEmail, userId)
                    } else {
                        Toast.makeText(this@JobDetailsActivity, "Failed to load profile data", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnApply.isEnabled = true
                    Toast.makeText(this@JobDetailsActivity, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showApplicationForm(job: Job, userSkills: List<String>, userName: String, userEmail: String, userId: String) {
        val dialog = ApplicationFormDialog.newInstance(
            job = job,
            userSkills = userSkills,
            userName = userName,
            onApply = { coverLetter, proposedBudget, estimatedTime ->
                submitApplicationWithFormData(
                    job,
                    userId,
                    userEmail,
                    userName,
                    userSkills,
                    coverLetter,
                    proposedBudget,
                    estimatedTime
                )
            }
        )

        dialog.show(supportFragmentManager, "ApplicationFormDialog")
    }

    private fun submitApplicationWithFormData(
        job: Job,
        userId: String,
        userEmail: String,
        userName: String,
        userSkills: List<String>,
        coverLetter: String,
        proposedBudget: Double,
        estimatedTime: String
    ) {
        progressBar.visibility = View.VISIBLE
        btnApply.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val application = JobApplication(
                    jobId = job.jobId,
                    freelancerId = userId,
                    freelancerName = userName,
                    freelancerEmail = userEmail,
                    coverLetter = coverLetter,
                    proposedBudget = proposedBudget,
                    clientId = job.clientId,
                    jobTitle = job.title,
                    clientName = job.clientName,
                    estimatedTime = estimatedTime,
                    freelancerSkills = userSkills
                )

                val result = firebaseService.submitApplication(application)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnApply.isEnabled = true

                    if (result.isSuccess) {
                        hasApplied = true
                        updateUIState()
                        showApplicationSuccessDialog(result.getOrNull()!!)
                    } else {
                        Toast.makeText(
                            this@JobDetailsActivity,
                            "Failed to submit application: ${result.exceptionOrNull()?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnApply.isEnabled = true
                    Toast.makeText(
                        this@JobDetailsActivity,
                        "Error submitting application: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showApplicationSuccessDialog(application: JobApplication) {
        val successMessage = """
        🎉 Application Submitted Successfully!
        
        Position: ${application.jobTitle}
        Client: ${application.clientName}
        Proposed Budget: R ${application.proposedBudget.toInt()}
        Estimated Time: ${application.estimatedTime}
        
        Your application has been sent to the client. 
        You will be notified when they review your application.
        
        You can track your application status in the "My Applications" section.
    """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Application Sent!")
            .setMessage(successMessage)
            .setPositiveButton("View My Applications") { dialog, which ->
                // Navigate to MyApplications activity
                val intent = Intent(this, student.projects.tholagig.jobs.MyApplicationsActivity::class.java)
                startActivity(intent)
            }
            .setNegativeButton("Continue Browsing") { dialog, which ->
                // Stay on current page
            }
            .setCancelable(false)
            .show()
    }
    private fun toggleSaveJob() {
        val userId = sessionManager.getUserId() ?: ""
        val jobId = currentJob?.jobId ?: return

        if (userId.isEmpty()) {
            Toast.makeText(this, "Please login to save jobs", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = if (isSaved) {
                    firebaseService.unsaveJob(userId, jobId)
                } else {
                    firebaseService.saveJob(userId, jobId)
                }

                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        isSaved = !isSaved
                        updateUIState()
                        val message = if (isSaved) "Job saved to favorites!" else "Job removed from favorites"
                        Toast.makeText(this@JobDetailsActivity, message, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@JobDetailsActivity, "Failed to update saved status", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@JobDetailsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateUIState() {
        // Update apply button
        if (hasApplied) {
            btnApply.text = "Applied ✓"
            btnApply.setBackgroundColor(resources.getColor(R.color.green))
            btnApply.isEnabled = false
        } else {
            btnApply.text = "Apply for Job"
            btnApply.setBackgroundColor(resources.getColor(R.color.orange))
            btnApply.isEnabled = true
        }

        // Update save button
        val saveIcon = if (isSaved) {
            R.drawable.ic_bookmark_filled
        } else {
            R.drawable.ic_bookmark_border
        }
        btnSave.setImageResource(saveIcon)

        // Update save button color
        btnSave.setColorFilter(
            resources.getColor(if (isSaved) R.color.orange else R.color.gray)
        )
    }
}
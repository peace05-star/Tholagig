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

        // Safely initialize optional views
        tvPostedDate = findViewById(R.id.tvPostedDate) ?: TextView(this)
        tvExperienceLevel = findViewById(R.id.tvExperienceLevel) ?: TextView(this)
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

        // Initialize back button safely
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack?.setOnClickListener {
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

        // Safely handle posted date
        if (::tvPostedDate.isInitialized && tvPostedDate != null) {
            tvPostedDate.text = "Posted: ${postedDateFormat.format(job.postedAt)}"
        }

        // Use safe values for client information (since these fields don't exist in your Job model)
        tvClientCompany.text = "Looking for Talent" // Default since company field doesn't exist
        tvClientRating.text = "⭐ New Client" // Default since rating fields don't exist
        tvClientBio.text = "Client looking for quality work to complete their project."

        // Safely handle experience level
        if (::tvExperienceLevel.isInitialized && tvExperienceLevel != null) {
            tvExperienceLevel.text = "🚀 ${job.category} Level" // Use category as experience level
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
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = firebaseService.getSimilarJobs(currentJob?.jobId ?: "", category)

                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        val similarJobsList = result.getOrNull() ?: emptyList()
                        similarJobs.clear()
                        similarJobs.addAll(similarJobsList)
                        similarJobsAdapter.notifyDataSetChanged()

                        // Update similar jobs title
                        tvSimilarJobsTitle.text = if (similarJobs.isEmpty()) {
                            "No similar jobs found"
                        } else {
                            "Similar Jobs (${similarJobs.size})"
                        }
                    } else {
                        tvSimilarJobsTitle.text = "Failed to load similar jobs"
                    }
                }
            } catch (e: Exception) {
                Log.e("JobDetails", "Error loading similar jobs: ${e.message}")
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
            findViewById<TextView>(R.id.tvOwnerNotice)?.text = "This is your job posting"
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
                        Log.d("JobDetails", "Application status: $hasApplied")
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

                // Get freelancer skills for the cover letter
                val skillsResult = firebaseService.getFreelancerSkills(userId)
                val userSkills = skillsResult.getOrNull() ?: emptyList()

                // Create job application with ALL required fields
                val application = JobApplication(
                    applicationId = "app_${System.currentTimeMillis()}",
                    jobId = job.jobId,
                    freelancerId = userId,
                    freelancerName = userName,
                    freelancerEmail = userEmail,
                    coverLetter = generateCoverLetter(job, userName, userSkills),
                    proposedBudget = job.budget,
                    status = "pending",
                    appliedAt = Date(),
                    clientId = job.clientId,
                    jobTitle = job.title,
                    clientName = job.clientName,
                    estimatedTime = "2-4 weeks",
                    freelancerSkills = userSkills,
                    freelancerRating = 0.0,
                    freelancerCompletedJobs = 0
                )

                Log.d("JobDetails", "Submitting application: ${application.applicationId}")
                val result = firebaseService.submitApplication(application)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnApply.isEnabled = true

                    if (result.isSuccess) {
                        hasApplied = true
                        updateUIState()
                        showApplicationSuccessDialog(application)
                        Log.d("JobDetails", "Application submitted successfully")
                    } else {
                        val error = result.exceptionOrNull()
                        Log.e("JobDetails", "Failed to submit application: ${error?.message}")
                        Toast.makeText(
                            this@JobDetailsActivity,
                            "Failed to submit application: ${error?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnApply.isEnabled = true
                    Log.e("JobDetails", "Exception submitting application: ${e.message}", e)
                    Toast.makeText(
                        this@JobDetailsActivity,
                        "Error submitting application: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun generateCoverLetter(job: Job, userName: String, skills: List<String>): String {
        val matchingSkills = skills.intersect(job.skillsRequired.toSet())
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())

        return """
            Dear ${job.clientName},
            
            I am excited to apply for the ${job.title} position. With my experience in ${matchingSkills.joinToString(", ")}, I am confident I can deliver high-quality results for your project.
            
            My relevant skills include:
            ${matchingSkills.joinToString("\n") { "• $it" }}
            
            I am available to start immediately and committed to meeting your deadline of ${dateFormat.format(job.deadline)}.
            
            Thank you for considering my application.
            
            Best regards,
            $userName
        """.trimIndent()
    }

    private fun showApplicationSuccessDialog(application: JobApplication) {
        val successMessage = """
            🎉 Application Submitted!
            
            Position: ${application.jobTitle}
            Client: ${application.clientName}
            Proposed Budget: R ${application.proposedBudget.toInt()}
            
            You will be notified when the client reviews your application.
        """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Application Sent!")
            .setMessage(successMessage)
            .setPositiveButton("Great!") { dialog, which ->
                // Refresh the dashboard and applications page
                setResult(RESULT_OK)
            }
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

    override fun onBackPressed() {
        // Notify that application was submitted
        if (hasApplied) {
            setResult(RESULT_OK)
        }
        super.onBackPressed()
    }
}
package student.projects.tholagig.jobs

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import student.projects.tholagig.R
import student.projects.tholagig.jobs.JobDetailsActivity
import student.projects.tholagig.models.Job
import student.projects.tholagig.models.JobApplication
import student.projects.tholagig.network.FirebaseService
import student.projects.tholagig.network.SessionManager
import java.text.SimpleDateFormat
import java.util.*

class ApplicationDetailsActivity : AppCompatActivity() {

    private lateinit var tvJobTitle: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvAppliedDate: TextView
    private lateinit var tvClientName: TextView
    private lateinit var tvProposedBudget: TextView
    private lateinit var tvAppliedTime: TextView
    private lateinit var ivReviewStatus: ImageView
    private lateinit var tvReviewStatus: TextView
    private lateinit var ivDecisionStatus: ImageView
    private lateinit var tvDecisionStatus: TextView
    private lateinit var tvCoverLetter: TextView
    private lateinit var tvJobCategory: TextView
    private lateinit var tvJobLocation: TextView
    private lateinit var tvJobDeadline: TextView
    private lateinit var btnViewJob: Button
    private lateinit var btnWithdraw: Button
    private lateinit var progressBar: ProgressBar

    private lateinit var sessionManager: SessionManager
    private lateinit var firebaseService: FirebaseService
    private var currentApplication: JobApplication? = null
    private var jobDetails: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_application_details)

        sessionManager = SessionManager(this)
        firebaseService = FirebaseService()

        initializeViews()
        setupClickListeners()
        loadApplicationDetails()
    }

    private fun initializeViews() {
        tvJobTitle = findViewById(R.id.tvJobTitle)
        tvStatus = findViewById(R.id.tvStatus)
        tvAppliedDate = findViewById(R.id.tvAppliedDate)
        tvClientName = findViewById(R.id.tvClientName)
        tvProposedBudget = findViewById(R.id.tvProposedBudget)
        tvAppliedTime = findViewById(R.id.tvAppliedTime)
        ivReviewStatus = findViewById(R.id.ivReviewStatus)
        tvReviewStatus = findViewById(R.id.tvReviewStatus)
        ivDecisionStatus = findViewById(R.id.ivDecisionStatus)
        tvDecisionStatus = findViewById(R.id.tvDecisionStatus)
        tvCoverLetter = findViewById(R.id.tvCoverLetter)
        tvJobCategory = findViewById(R.id.tvJobCategory)
        tvJobLocation = findViewById(R.id.tvJobLocation)
        tvJobDeadline = findViewById(R.id.tvJobDeadline)
        btnViewJob = findViewById(R.id.btnViewJob)
        btnWithdraw = findViewById(R.id.btnWithdraw)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupClickListeners() {
        btnViewJob.setOnClickListener {
            viewJobPosting()
        }

        btnWithdraw.setOnClickListener {
            withdrawApplication()
        }
    }

    private fun loadApplicationDetails() {
        progressBar.visibility = View.VISIBLE

        val applicationId = intent.getStringExtra("APPLICATION_ID") ?: ""
        val jobId = intent.getStringExtra("JOB_ID") ?: ""

        if (applicationId.isEmpty() || jobId.isEmpty()) {
            Toast.makeText(this, "Invalid application data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("ApplicationDetails", "🟡 Loading application: $applicationId and job: $jobId")

                val userId = sessionManager.getUserId() ?: ""

                // Load all user applications and find the specific one
                val applicationsResult = firebaseService.getApplicationsByFreelancer(userId)
                val jobResult = firebaseService.getJobById(jobId)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE

                    if (applicationsResult.isSuccess && jobResult.isSuccess) {
                        val applications = applicationsResult.getOrNull() ?: emptyList()
                        val application = applications.find { it.applicationId == applicationId }

                        jobDetails = jobResult.getOrNull()

                        if (application != null && jobDetails != null) {
                            currentApplication = application
                            displayApplicationDetails(application, jobDetails!!)
                            Log.d("ApplicationDetails", "🟢 Successfully loaded application and job details")
                        } else {
                            showErrorState("Application or job not found")
                        }
                    } else {
                        val appError = applicationsResult.exceptionOrNull()?.message ?: "Unknown error"
                        val jobError = jobResult.exceptionOrNull()?.message ?: "Unknown error"
                        Log.e("ApplicationDetails", "🔴 Failed to load data - App: $appError, Job: $jobError")
                        showErrorState("Failed to load application details")
                    }
                }
            } catch (e: Exception) {
                Log.e("ApplicationDetails", "💥 Error loading details: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    showErrorState("Network error: ${e.message}")
                }
            }
        }
    }

    private fun displayApplicationDetails(application: JobApplication, job: Job) {
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("dd MMM yyyy 'at' HH:mm", Locale.getDefault())

        tvJobTitle.text = application.jobTitle ?: job.title
        tvClientName.text = application.clientName ?: job.clientName
        tvProposedBudget.text = "R ${application.proposedBudget.toInt()}"
        tvAppliedDate.text = "Applied ${dateFormat.format(application.appliedAt)}"
        tvAppliedTime.text = timeFormat.format(application.appliedAt)
        tvCoverLetter.text = application.coverLetter ?: "No cover letter provided"

        // Job details
        tvJobCategory.text = job.category ?: "Not specified"
        tvJobLocation.text = job.location ?: "Not specified"
        tvJobDeadline.text = dateFormat.format(job.deadline)

        // Set status and update timeline
        setApplicationStatus(application.status ?: "pending")
    }

    private fun setApplicationStatus(status: String) {
        // Update status badge
        when (status.lowercase()) {
            "pending" -> {
                tvStatus.text = "⏳ Pending Review"
                tvStatus.setBackgroundResource(R.drawable.status_pending_bg)
                btnWithdraw.visibility = View.VISIBLE
                updateTimeline(false, false, "Client is reviewing your application", "Waiting for client decision")
            }
            "accepted", "hired" -> {
                tvStatus.text = "✅ Hired"
                tvStatus.setBackgroundResource(R.drawable.status_accepted_bg)
                btnWithdraw.visibility = View.GONE
                updateTimeline(true, true, "Application reviewed", "Congratulations! You've been hired")
            }
            "rejected" -> {
                tvStatus.text = "❌ Rejected"
                tvStatus.setBackgroundResource(R.drawable.status_rejected_bg)
                btnWithdraw.visibility = View.GONE
                updateTimeline(true, true, "Application reviewed", "Application was not selected")
            }
            "withdrawn" -> {
                tvStatus.text = "📤 Withdrawn"
                tvStatus.setBackgroundResource(R.drawable.status_withdrawn_bg)
                btnWithdraw.visibility = View.GONE
                updateTimeline(false, false, "Application withdrawn", "You withdrew this application")
            }
            else -> {
                tvStatus.text = status
                tvStatus.setBackgroundResource(R.drawable.status_pending_bg)
                btnWithdraw.visibility = View.VISIBLE
                updateTimeline(false, false, "Application submitted", "Waiting for client response")
            }
        }
    }

    private fun updateTimeline(isReviewed: Boolean, isDecided: Boolean, reviewText: String, decisionText: String) {
        // Update review step
        if (isReviewed) {
            ivReviewStatus.setImageResource(R.drawable.ic_check_circle)
            ivReviewStatus.setColorFilter(ContextCompat.getColor(this, R.color.success_color))
        } else {
            ivReviewStatus.setImageResource(R.drawable.ic_circle)
            ivReviewStatus.setColorFilter(ContextCompat.getColor(this, R.color.gray))
        }
        tvReviewStatus.text = reviewText

        // Update decision step
        if (isDecided) {
            ivDecisionStatus.setImageResource(R.drawable.ic_check_circle)
            ivDecisionStatus.setColorFilter(ContextCompat.getColor(this, R.color.success_color))
        } else {
            ivDecisionStatus.setImageResource(R.drawable.ic_circle)
            ivDecisionStatus.setColorFilter(ContextCompat.getColor(this, R.color.gray))
        }
        tvDecisionStatus.text = decisionText
    }
    private fun viewJobPosting() {
        val job = jobDetails ?: return

        val intent = Intent(this, JobDetailsActivity::class.java).apply {
            putExtra("JOB_ID", job.jobId)
            putExtra("JOB_TITLE", job.title)
        }
        startActivity(intent)
    }

    private fun withdrawApplication() {
        val application = currentApplication ?: return

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Withdraw Application")
            .setMessage("Are you sure you want to withdraw your application for \"${application.jobTitle}\"? This action cannot be undone.")
            .setPositiveButton("Withdraw") { dialog, which ->
                performWithdrawal()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performWithdrawal() {
        val application = currentApplication ?: return

        progressBar.visibility = View.VISIBLE
        btnWithdraw.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("ApplicationDetails", "🟡 Withdrawing application: ${application.applicationId}")
                val result = firebaseService.updateApplicationStatus(application.applicationId, "withdrawn")

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnWithdraw.isEnabled = true

                    if (result.isSuccess) {
                        // Update local data
                        currentApplication = currentApplication?.copy(status = "withdrawn")
                        currentApplication?.let { app ->
                            setApplicationStatus(app.status)
                        }

                        Toast.makeText(
                            this@ApplicationDetailsActivity,
                            "Application withdrawn successfully",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Optional: Finish activity or go back
                        // finish()
                    } else {
                        val error = result.exceptionOrNull()?.message ?: "Unknown error"
                        Toast.makeText(
                            this@ApplicationDetailsActivity,
                            "Failed to withdraw: $error",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("ApplicationDetails", "💥 Error withdrawing: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnWithdraw.isEnabled = true
                    Toast.makeText(
                        this@ApplicationDetailsActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showErrorState(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        // You could also show a specific error layout here
        finish() // Go back if data can't be loaded
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to this activity
        val applicationId = intent.getStringExtra("APPLICATION_ID") ?: ""
        if (applicationId.isNotEmpty()) {
            loadApplicationDetails()
        }
    }
}
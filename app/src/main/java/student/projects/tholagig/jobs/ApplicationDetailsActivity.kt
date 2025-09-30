package student.projects.tholagig.jobs

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // In a real app, fetch application and job details from Firebase
                // For now, we'll use mock data
                val mockApplication = createMockApplication(applicationId, jobId)
                currentApplication = mockApplication

                val mockJob = createMockJob(jobId)
                jobDetails = mockJob

                withContext(Dispatchers.Main) {
                    displayApplicationDetails(mockApplication, mockJob)
                    progressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@ApplicationDetailsActivity,
                        "Error loading application details: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun createMockApplication(applicationId: String, jobId: String): JobApplication {
        return JobApplication(
            applicationId = applicationId,
            jobId = jobId,
            freelancerId = "user_123",
            freelancerName = "You",
            freelancerEmail = "user@example.com",
            coverLetter = "Dear Hiring Manager,\n\nI am writing to express my strong interest in the Mobile App Developer position. With over 3 years of experience in Android development using Kotlin and Java, I am confident in my ability to contribute to your team's success.\n\nMy technical skills include:\n• Kotlin and Java programming\n• Android SDK and Material Design\n• Firebase integration\n• REST API development\n• Git version control\n\nI have successfully delivered multiple mobile applications for clients in various industries, and I'm particularly excited about the e-commerce focus of this project. I believe my experience aligns perfectly with your requirements.\n\nThank you for considering my application. I look forward to the opportunity to discuss how I can contribute to your team.\n\nBest regards,\n[Your Name]",
            proposedBudget = 15000.0,
            status = "pending",
            appliedAt = Date(System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000),
            clientId = "client_1",
            jobTitle = "Mobile App Developer",
            clientName = "Tech Solutions SA"
        )
    }

    private fun createMockJob(jobId: String): Job {
        return Job(
            jobId = jobId,
            title = "Mobile App Developer",
            clientId = "client_1",
            clientName = "Tech Solutions SA",
            description = "We need an experienced mobile app developer...",
            budget = 15000.0,
            category = "Mobile Development",
            skillsRequired = listOf("Kotlin", "Android SDK", "Firebase"),
            location = "Remote",
            deadline = Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000),
            postedAt = Date(System.currentTimeMillis() - 5 * 24 * 60 * 60 * 1000)
        )
    }

    private fun displayApplicationDetails(application: JobApplication, job: Job) {
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("dd MMM yyyy 'at' HH:mm", Locale.getDefault())

        tvJobTitle.text = application.jobTitle
        tvClientName.text = application.clientName
        tvProposedBudget.text = "R ${application.proposedBudget.toInt()}"
        tvAppliedDate.text = "Applied ${dateFormat.format(application.appliedAt)}"
        tvAppliedTime.text = timeFormat.format(application.appliedAt)
        tvCoverLetter.text = application.coverLetter

        // Job details
        tvJobCategory.text = job.category
        tvJobLocation.text = job.location
        tvJobDeadline.text = dateFormat.format(job.deadline)

        // Set status and update timeline
        setApplicationStatus(application.status)
    }

    private fun setApplicationStatus(status: String) {
        // Update status badge
        when (status.lowercase()) {
            "pending" -> {
                tvStatus.text = "Pending Review"
                tvStatus.setBackgroundResource(R.drawable.status_pending_bg)
                btnWithdraw.visibility = View.VISIBLE
                updateTimeline(false, false, "Client is reviewing your application", "Waiting for client decision")
            }
            "accepted" -> {
                tvStatus.text = "Accepted ✓"
                tvStatus.setBackgroundResource(R.drawable.status_accepted_bg)
                btnWithdraw.visibility = View.GONE
                updateTimeline(true, true, "Application reviewed", "Congratulations! Your application was accepted")
            }
            "rejected" -> {
                tvStatus.text = "Rejected"
                tvStatus.setBackgroundResource(R.drawable.status_rejected_bg)
                btnWithdraw.visibility = View.GONE
                updateTimeline(true, true, "Application reviewed", "Application was not selected")
            }
            "withdrawn" -> {
                tvStatus.text = "Withdrawn"
                tvStatus.setBackgroundResource(R.drawable.status_withdrawn_bg)
                btnWithdraw.visibility = View.GONE
                updateTimeline(false, false, "Application withdrawn", "You withdrew this application")
            }
        }
    }

    private fun updateTimeline(isReviewed: Boolean, isDecided: Boolean, reviewText: String, decisionText: String) {
        // Update review step
        if (isReviewed) {
            ivReviewStatus.setImageResource(R.drawable.ic_check_circle)
            ivReviewStatus.setColorFilter(resources.getColor(R.color.green))
        } else {
            ivReviewStatus.setImageResource(R.drawable.ic_circle)
            ivReviewStatus.setColorFilter(resources.getColor(R.color.gray))
        }
        tvReviewStatus.text = reviewText

        // Update decision step
        if (isDecided) {
            ivDecisionStatus.setImageResource(R.drawable.ic_check_circle)
            ivDecisionStatus.setColorFilter(resources.getColor(R.color.green))
        } else {
            ivDecisionStatus.setImageResource(R.drawable.ic_circle)
            ivDecisionStatus.setColorFilter(resources.getColor(R.color.gray))
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
        progressBar.visibility = View.VISIBLE
        btnWithdraw.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Update application status in Firebase
                // TODO: Implement actual withdrawal in Firebase
                // firebaseService.updateApplicationStatus(applicationId, "withdrawn")

                // Simulate API call
                delay(1500)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnWithdraw.isEnabled = true

                    // Update UI
                    currentApplication = currentApplication?.copy(status = "withdrawn")
                    currentApplication?.let { application ->
                        setApplicationStatus(application.status)
                    }

                    Toast.makeText(
                        this@ApplicationDetailsActivity,
                        "Application withdrawn successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnWithdraw.isEnabled = true
                    Toast.makeText(
                        this@ApplicationDetailsActivity,
                        "Error withdrawing application: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
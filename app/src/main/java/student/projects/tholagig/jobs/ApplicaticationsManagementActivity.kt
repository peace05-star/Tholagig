package student.projects.tholagig.jobs

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import student.projects.tholagig.R
import student.projects.tholagig.Adapters.ApplicationsManagementAdapter
import student.projects.tholagig.models.JobApplication
import student.projects.tholagig.network.SessionManager
import java.text.SimpleDateFormat
import java.util.*

class ApplicationsManagementActivity : AppCompatActivity() {

    private lateinit var rvApplications: RecyclerView
    private lateinit var chipGroupStatus: ChipGroup
    private lateinit var tvApplicationsCount: TextView
    private lateinit var tvEmptyState: TextView
    private lateinit var progressBar: ProgressBar

    private lateinit var sessionManager: SessionManager
    private lateinit var applicationsAdapter: ApplicationsManagementAdapter

    private val allApplications = mutableListOf<JobApplication>()
    private val filteredApplications = mutableListOf<JobApplication>()
    private val selectedStatuses = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_applications_management)

        sessionManager = SessionManager(this)

        initializeViews()
        setupRecyclerView()
        setupClickListeners()
        loadApplications()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Manage Applications"
    }

    private fun initializeViews() {
        rvApplications = findViewById(R.id.rvApplications)
        chipGroupStatus = findViewById(R.id.chipGroupStatus)
        tvApplicationsCount = findViewById(R.id.tvApplicationsCount)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupRecyclerView() {
        applicationsAdapter = ApplicationsManagementAdapter(filteredApplications,
            onViewProfile = { application ->
                viewFreelancerProfile(application)
            },
            onAccept = { application ->
                acceptApplication(application)
            },
            onReject = { application ->
                rejectApplication(application)
            },
            onMessage = { application ->
                messageFreelancer(application)
            }
        )
        rvApplications.apply {
            layoutManager = LinearLayoutManager(this@ApplicationsManagementActivity)
            adapter = applicationsAdapter
        }
    }

    private fun setupClickListeners() {
        // Status filter chips
        chipGroupStatus.setOnCheckedStateChangeListener { group, checkedIds ->
            selectedStatuses.clear()
            checkedIds.forEach { chipId ->
                when (chipId) {
                    R.id.chipAll -> selectedStatuses.add("all")
                    R.id.chipPending -> selectedStatuses.add("pending")
                    R.id.chipAccepted -> selectedStatuses.add("accepted")
                    R.id.chipRejected -> selectedStatuses.add("rejected")
                }
            }
            applyFilters()
        }
    }

    private fun loadApplications() {
        progressBar.visibility = View.VISIBLE
        tvApplicationsCount.text = "Loading applications..."

        // For now, load mock data. In real app, fetch from Firebase for this client
        loadMockApplications()
    }

    private fun loadMockApplications() {
        allApplications.clear()

        // Mock applications from different freelancers
        allApplications.addAll(
            listOf(
                JobApplication(
                    applicationId = "app_1",
                    jobId = "job_1",
                    jobTitle = "Mobile App Developer",
                    freelancerId = "freelancer_1",
                    freelancerName = "John Smith",
                    freelancerEmail = "john.smith@email.com",
                    freelancerSkills = listOf("Kotlin", "Android", "Firebase", "REST API"),
                    freelancerRating = 4.8,
                    freelancerCompletedJobs = 24,
                    coverLetter = "I have 5 years of experience in mobile app development...",
                    proposedBudget = 14000.0,
                    estimatedTime = "3 months",
                    status = "pending",
                    appliedAt = Date(System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000),
                    clientId = "client_123",
                    clientName = "Your Company"
                ),
                JobApplication(
                    applicationId = "app_2",
                    jobId = "job_1",
                    jobTitle = "Mobile App Developer",
                    freelancerId = "freelancer_2",
                    freelancerName = "Sarah Johnson",
                    freelancerEmail = "sarah.j@email.com",
                    freelancerSkills = listOf("Java", "Android", "SQLite", "UI/UX"),
                    freelancerRating = 4.5,
                    freelancerCompletedJobs = 18,
                    coverLetter = "As an experienced Android developer...",
                    proposedBudget = 16000.0,
                    estimatedTime = "2.5 months",
                    status = "pending",
                    appliedAt = Date(System.currentTimeMillis() - 1 * 24 * 60 * 60 * 1000),
                    clientId = "client_123",
                    clientName = "Your Company"
                ),
                // ... other applications
            )
        )

        progressBar.visibility = View.GONE
        applyFilters()
        updateApplicationsCount()
    }

    private fun applyFilters() {
        filteredApplications.clear()

        val filtered = if (selectedStatuses.isEmpty() || selectedStatuses.contains("all")) {
            allApplications
        } else {
            allApplications.filter { application ->
                selectedStatuses.any { status ->
                    application.status.equals(status, true)
                }
            }
        }

        // Sort by application date (newest first)
        filteredApplications.addAll(filtered.sortedByDescending { it.appliedAt })
        applicationsAdapter.notifyDataSetChanged()
        updateApplicationsCount()
    }

    private fun updateApplicationsCount() {
        val totalCount = allApplications.size
        val filteredCount = filteredApplications.size

        tvApplicationsCount.text = when {
            totalCount == 0 -> "No applications yet"
            filteredCount == 0 -> "No applications match your filters"
            filteredCount == 1 -> "1 application"
            else -> "$filteredCount applications"
        }

        // Show/hide empty state
        if (filteredApplications.isEmpty()) {
            rvApplications.visibility = View.GONE
            tvEmptyState.visibility = View.VISIBLE
        } else {
            rvApplications.visibility = View.VISIBLE
            tvEmptyState.visibility = View.GONE
        }
    }

    private fun viewFreelancerProfile(application: JobApplication) {
        // TODO: Implement freelancer profile view
        // val intent = Intent(this, FreelancerProfileActivity::class.java).apply {
        //     putExtra("FREELANCER_ID", application.freelancerId)
        // }
        // startActivity(intent)

        // Temporary toast
        android.widget.Toast.makeText(
            this,
            "Viewing ${application.freelancerName}'s profile",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    private fun acceptApplication(application: JobApplication) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Accept Application")
            .setMessage("Are you sure you want to hire ${application.freelancerName} for \"${application.jobTitle}\"?")
            .setPositiveButton("Hire") { dialog, which ->
                performAcceptance(application)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performAcceptance(application: JobApplication) {
        progressBar.visibility = View.VISIBLE

        // In real app, update status in Firebase and notify freelancer
        val updatedApplication = application.copy(status = "accepted")

        // Update local data
        val index = allApplications.indexOfFirst { it.applicationId == application.applicationId }
        if (index != -1) {
            allApplications[index] = updatedApplication
            applyFilters()
        }

        progressBar.visibility = View.GONE
        android.widget.Toast.makeText(
            this,
            "You have hired ${application.freelancerName}!",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    private fun rejectApplication(application: JobApplication) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Reject Application")
            .setMessage("Are you sure you want to reject ${application.freelancerName}'s application for \"${application.jobTitle}\"?")
            .setPositiveButton("Reject") { dialog, which ->
                performRejection(application)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performRejection(application: JobApplication) {
        progressBar.visibility = View.VISIBLE

        // In real app, update status in Firebase and notify freelancer
        val updatedApplication = application.copy(status = "rejected")

        // Update local data
        val index = allApplications.indexOfFirst { it.applicationId == application.applicationId }
        if (index != -1) {
            allApplications[index] = updatedApplication
            applyFilters()
        }

        progressBar.visibility = View.GONE
        android.widget.Toast.makeText(
            this,
            "Application rejected",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    private fun messageFreelancer(application: JobApplication) {
        // TODO: Implement messaging system
        // val intent = Intent(this, ChatActivity::class.java).apply {
        //     putExtra("FREELANCER_ID", application.freelancerId)
        //     putExtra("FREELANCER_NAME", application.freelancerName)
        // }
        // startActivity(intent)

        // Temporary toast
        android.widget.Toast.makeText(
            this,
            "Messaging ${application.freelancerName}",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
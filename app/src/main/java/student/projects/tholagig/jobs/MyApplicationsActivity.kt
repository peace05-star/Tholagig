package student.projects.tholagig.jobs

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.*
import student.projects.tholagig.R
import student.projects.tholagig.Adapters.ApplicationAdapter
import student.projects.tholagig.jobs.JobDetailsActivity
import student.projects.tholagig.models.JobApplication
import student.projects.tholagig.network.FirebaseService
import student.projects.tholagig.network.SessionManager
import java.util.Date
import com.google.android.material.bottomnavigation.BottomNavigationView
import student.projects.tholagig.dashboards.FreelancerDashboardActivity
import student.projects.tholagig.jobs.JobBrowseActivity
import student.projects.tholagig.profile.ProfileActivity

class MyApplicationsActivity : AppCompatActivity() {

    private lateinit var rvApplications: RecyclerView
    private lateinit var chipGroupStatus: ChipGroup
    private lateinit var tvApplicationsCount: TextView
    private lateinit var emptyState: View
    private lateinit var progressBar: ProgressBar
    private lateinit var toolbar: MaterialToolbar
    private lateinit var bottomNavigationView: BottomNavigationView

    private lateinit var sessionManager: SessionManager
    private lateinit var firebaseService: FirebaseService
    private lateinit var applicationsAdapter: ApplicationAdapter

    private val allApplications = mutableListOf<JobApplication>()
    private val filteredApplications = mutableListOf<JobApplication>()
    private val selectedStatuses = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_applications)

        sessionManager = SessionManager(this)
        firebaseService = FirebaseService()

        initializeViews()
        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        setupBottomNavigation()
        loadApplications()
    }

    private fun initializeViews() {
        rvApplications = findViewById(R.id.rvApplications)
        chipGroupStatus = findViewById(R.id.chipGroupStatus)
        tvApplicationsCount = findViewById(R.id.tvApplicationsCount)
        emptyState = findViewById(R.id.emptyState)
        progressBar = findViewById(R.id.progressBar)
        toolbar = findViewById(R.id.toolbar)
        bottomNavigationView = findViewById(R.id.bottom_navigation)
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, FreelancerDashboardActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_jobs -> {
                    val intent = Intent(this, JobBrowseActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_applications -> {
                    // Already on applications page
                    true
                }
                R.id.nav_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }

        bottomNavigationView.selectedItemId = R.id.nav_applications
    }

    private fun setupToolbar() {
        Log.d("MyApplications", "Setting up toolbar")

        // Set the Toolbar as the ActionBar
        setSupportActionBar(toolbar)

        // Enable the back button (the arrow icon)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Hide the default title since we have a custom TextView
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Option 1: Handle back button with navigation click listener
        toolbar.setNavigationOnClickListener {
            Log.d("MyApplications", "Back button clicked")
            onBackPressed()
        }

        Log.d("MyApplications", "Toolbar setup complete")
    }

    private fun setupRecyclerView() {
        applicationsAdapter = ApplicationAdapter(
            applications = filteredApplications,
            onViewDetails = { application ->
                viewApplicationDetails(application)
            },
            onWithdraw = { application ->
                withdrawApplication(application)
            }
        )
        rvApplications.apply {
            layoutManager = LinearLayoutManager(this@MyApplicationsActivity)
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

        val userId = sessionManager.getUserId() ?: ""

        if (userId.isEmpty()) {
            progressBar.visibility = View.GONE
            tvApplicationsCount.text = "Please login to view applications"
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = firebaseService.getApplicationsByFreelancer(userId)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE

                    if (result.isSuccess) {
                        allApplications.clear()
                        val applications = result.getOrNull() ?: emptyList()

                        if (applications.isEmpty()) {
                            // Load mock data for demonstration
                            loadMockApplications()
                        } else {
                            allApplications.addAll(applications)
                            applyFilters()
                            updateApplicationsCount()
                        }
                    } else {
                        // Load mock data if Firebase fails
                        loadMockApplications()
                        Toast.makeText(
                            this@MyApplicationsActivity,
                            "Using demo data",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    // Load mock data on error
                    loadMockApplications()
                    Toast.makeText(
                        this@MyApplicationsActivity,
                        "Error loading applications: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun loadMockApplications() {
        allApplications.clear()
        allApplications.addAll(
            listOf(
                JobApplication(
                    applicationId = "app_1",
                    jobId = "job_1",
                    freelancerId = "user_123",
                    freelancerName = "You",
                    freelancerEmail = "user@example.com",
                    coverLetter = "I'm very interested in this mobile app development position...",
                    proposedBudget = 15000.0,
                    status = "pending",
                    appliedAt = Date(System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000),
                    clientId = "client_1",
                    jobTitle = "Mobile App Developer",
                    clientName = "Tech Solutions SA"
                ),
                JobApplication(
                    applicationId = "app_2",
                    jobId = "job_2",
                    freelancerId = "user_123",
                    freelancerName = "You",
                    freelancerEmail = "user@example.com",
                    coverLetter = "I have extensive experience in UI/UX design...",
                    proposedBudget = 12000.0,
                    status = "accepted",
                    appliedAt = Date(System.currentTimeMillis() - 5 * 24 * 60 * 60 * 1000),
                    clientId = "client_2",
                    jobTitle = "UI/UX Designer",
                    clientName = "Creative Agency"
                ),
                JobApplication(
                    applicationId = "app_3",
                    jobId = "job_3",
                    freelancerId = "user_123",
                    freelancerName = "You",
                    freelancerEmail = "user@example.com",
                    coverLetter = "I'm excited about this backend development opportunity...",
                    proposedBudget = 20000.0,
                    status = "rejected",
                    appliedAt = Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000),
                    clientId = "client_3",
                    jobTitle = "Backend Developer",
                    clientName = "FinTech Startup"
                )
            )
        )
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
            emptyState.visibility = View.VISIBLE
        } else {
            rvApplications.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
        }
    }

    private fun viewApplicationDetails(application: JobApplication) {
        val intent = Intent(this, ApplicationDetailsActivity::class.java).apply {
            putExtra("APPLICATION_ID", application.applicationId)
            putExtra("JOB_ID", application.jobId)
        }
        startActivity(intent)
    }

    private fun withdrawApplication(application: JobApplication) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Withdraw Application")
            .setMessage("Are you sure you want to withdraw your application for \"${application.jobTitle}\"?")
            .setPositiveButton("Withdraw") { dialog, which ->
                performWithdrawal(application)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performWithdrawal(application: JobApplication) {
        progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Update application status in Firebase
                val updatedApplication = application.copy(status = "withdrawn")
                // TODO: Implement actual withdrawal in Firebase
                // firebaseService.updateApplication(updatedApplication)

                // Simulate API call
                delay(1000)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE

                    // Update local data
                    val index = allApplications.indexOfFirst { it.applicationId == application.applicationId }
                    if (index != -1) {
                        allApplications[index] = updatedApplication
                        applyFilters()
                    }

                    Toast.makeText(
                        this@MyApplicationsActivity,
                        "Application withdrawn successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@MyApplicationsActivity,
                        "Error withdrawing application: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
package student.projects.tholagig.jobs

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
        bottomNavigationView = findViewById(R.id.bottom_navigation)
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, FreelancerDashboardActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_jobs -> {
                    val intent = Intent(this, JobBrowseActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_applications -> {
                    // Already on applications page - just refresh
                    loadApplications()
                    true
                }
                R.id.nav_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }

        bottomNavigationView.selectedItemId = R.id.nav_applications
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

        // Browse Jobs button in empty state
        emptyState.findViewById<android.widget.Button>(R.id.browseJobsButton)?.setOnClickListener {
            val intent = Intent(this, JobBrowseActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadApplications() {
        progressBar.visibility = View.VISIBLE
        tvApplicationsCount.text = "Loading applications..."

        val userId = sessionManager.getUserId() ?: ""

        if (userId.isEmpty()) {
            progressBar.visibility = View.GONE
            showEmptyState("Please login to view applications")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("MyApplications", "🟡 Fetching applications for user: $userId")
                val result = firebaseService.getApplicationsByFreelancer(userId)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE

                    if (result.isSuccess) {
                        allApplications.clear()
                        val applications = result.getOrNull() ?: emptyList()

                        Log.d("MyApplications", "🟢 Loaded ${applications.size} applications from Firebase")

                        if (applications.isNotEmpty()) {
                            allApplications.addAll(applications)
                            applyFilters()
                            updateApplicationsCount()
                            Toast.makeText(
                                this@MyApplicationsActivity,
                                "Loaded ${applications.size} applications",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            showEmptyState("No applications yet. Start applying to jobs!")
                            Log.d("MyApplications", "🟡 No applications found in Firebase")
                        }
                    } else {
                        val error = result.exceptionOrNull()
                        Log.e("MyApplications", "🔴 Failed to load applications: ${error?.message}")
                        showEmptyState("Failed to load applications. Please try again.")
                        Toast.makeText(
                            this@MyApplicationsActivity,
                            "Error: ${error?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Log.e("MyApplications", "🔴 Exception loading applications: ${e.message}")
                    showEmptyState("Error loading applications. Please check your connection.")
                    Toast.makeText(
                        this@MyApplicationsActivity,
                        "Network error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
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

            // Update empty state message based on filters
            val emptyStateText = emptyState.findViewById<TextView>(R.id.emptyStateText)
            val emptyStateSubtext = emptyState.findViewById<TextView>(R.id.emptyStateSubtext)

            if (allApplications.isEmpty()) {
                emptyStateText?.text = "No Applications Yet"
                emptyStateSubtext?.text = "Start applying to jobs to see your applications here"
            } else {
                emptyStateText?.text = "No Matching Applications"
                emptyStateSubtext?.text = "Try changing your filters to see more applications"
            }
        } else {
            rvApplications.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
        }
    }

    private fun showEmptyState(message: String) {
        tvApplicationsCount.text = message
        rvApplications.visibility = View.GONE
        emptyState.visibility = View.VISIBLE

        val emptyStateText = emptyState.findViewById<TextView>(R.id.emptyStateText)
        val emptyStateSubtext = emptyState.findViewById<TextView>(R.id.emptyStateSubtext)

        emptyStateText?.text = "No Applications"
        emptyStateSubtext?.text = message
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
                val result = firebaseService.updateApplicationStatus(application.applicationId, "withdrawn")

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE

                    if (result.isSuccess) {
                        // Update local data
                        val index = allApplications.indexOfFirst { it.applicationId == application.applicationId }
                        if (index != -1) {
                            allApplications[index] = application.copy(status = "withdrawn")
                            applyFilters()
                        }

                        Toast.makeText(
                            this@MyApplicationsActivity,
                            "Application withdrawn successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@MyApplicationsActivity,
                            "Error withdrawing application: ${result.exceptionOrNull()?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
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

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to this activity
        bottomNavigationView.selectedItemId = R.id.nav_applications
        loadApplications()
    }
}
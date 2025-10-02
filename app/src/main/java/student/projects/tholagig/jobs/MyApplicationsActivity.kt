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
        try {
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

        } catch (e: Exception) {
            Log.e("BottomNav", "Bottom navigation setup failed: ${e.message}")
        }
    }

    private fun setupToolbar() {
        Log.d("MyApplications", "Setting up toolbar")

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

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
        emptyState.visibility = View.GONE

        val userId = sessionManager.getUserId() ?: ""

        if (userId.isEmpty()) {
            progressBar.visibility = View.GONE
            showEmptyState("Please login to view applications")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("MyApplications", "🟡 Loading applications for user: $userId")
                val result = firebaseService.getApplicationsByFreelancer(userId)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE

                    if (result.isSuccess) {
                        val applications = result.getOrNull() ?: emptyList()
                        Log.d("MyApplications", "🟢 Loaded ${applications.size} applications from Firebase")

                        allApplications.clear()
                        allApplications.addAll(applications)
                        applyFilters()
                        updateApplicationsCount()

                        if (applications.isEmpty()) {
                            showEmptyState("No applications found\nApply to jobs to see them here!")
                        }
                    } else {
                        val error = result.exceptionOrNull()?.message ?: "Unknown error"
                        Log.e("MyApplications", "🔴 Failed to load applications: $error")
                        showEmptyState("Failed to load applications\nPull down to refresh")
                        Toast.makeText(
                            this@MyApplicationsActivity,
                            "Error: $error",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("MyApplications", "💥 Error loading applications: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    showEmptyState("Connection error\nPull down to refresh")
                    Toast.makeText(
                        this@MyApplicationsActivity,
                        "Network error: ${e.message}",
                        Toast.LENGTH_LONG
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
            filteredCount == totalCount -> "$filteredCount application${if (filteredCount != 1) "s" else ""}"
            else -> "$filteredCount of $totalCount application${if (totalCount != 1) "s" else ""}"
        }

        // Show/hide empty state
        if (filteredApplications.isEmpty()) {
            val message = when {
                selectedStatuses.contains("pending") -> "No pending applications"
                selectedStatuses.contains("accepted") -> "No accepted applications"
                selectedStatuses.contains("rejected") -> "No rejected applications"
                allApplications.isEmpty() -> "No applications found\nApply to jobs to see them here!"
                else -> "No applications match your filters"
            }
            showEmptyState(message)
        } else {
            hideEmptyState()
        }
    }

    private fun showEmptyState(message: String) {
        // If you have a specific empty state view, use it
        // Otherwise, you might want to add a TextView for empty state
        rvApplications.visibility = View.GONE
        emptyState.visibility = View.VISIBLE
        // If you want to set custom text, you might need to add a TextView to your emptyState layout
    }

    private fun hideEmptyState() {
        emptyState.visibility = View.GONE
        rvApplications.visibility = View.VISIBLE
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
                Log.d("MyApplications", "🟡 Withdrawing application: ${application.applicationId}")
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
                            "Failed to withdraw application",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@MyApplicationsActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh applications when returning to this activity
        loadApplications()
    }
}
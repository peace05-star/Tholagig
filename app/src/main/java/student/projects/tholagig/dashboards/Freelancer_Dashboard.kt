package student.projects.tholagig.dashboards

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import student.projects.tholagig.R
import student.projects.tholagig.Adapters.JobsAdapter
import student.projects.tholagig.jobs.JobBrowseActivity
import student.projects.tholagig.jobs.JobDetailsActivity
import student.projects.tholagig.models.Job
import student.projects.tholagig.profile.ProfileActivity
import android.content.Intent
import student.projects.tholagig.jobs.MyApplicationsActivity
import student.projects.tholagig.network.SessionManager
import student.projects.tholagig.network.FirebaseService // ADD THIS IMPORT
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.util.Log
import kotlinx.coroutines.* // ADD THIS IMPORT

class FreelancerDashboardActivity : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var tvAppliedCount: TextView
    private lateinit var tvActiveCount: TextView
    private lateinit var tvEarnings: TextView
    private lateinit var layoutBrowseJobs: LinearLayout
    private lateinit var layoutMyApplications: LinearLayout
    private lateinit var btnProfile: ImageButton
    private lateinit var rvJobs: RecyclerView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var progressBar: View // ADD PROGRESS BAR IF YOU HAVE ONE

    private lateinit var jobsAdapter: JobsAdapter
    private lateinit var sessionManager: SessionManager
    private lateinit var firebaseService: FirebaseService // ADD THIS
    private val jobList = mutableListOf<Job>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_freelancer_dashboard)

        sessionManager = SessionManager(this)
        firebaseService = FirebaseService() // INITIALIZE FIREBASE SERVICE

        initializeViews()
        setupRecyclerView()
        setupClickListeners()
        setupBottomNavigation()
        loadUserData()
        loadJobsFromFirebase() // REPLACE loadMockData() WITH THIS
        loadDashboardStats() // ADD THIS FOR STATS
    }

    private fun initializeViews() {
        tvWelcome = findViewById(R.id.tvWelcome)
        tvAppliedCount = findViewById(R.id.tvAppliedCount)
        tvActiveCount = findViewById(R.id.tvActiveCount)
        tvEarnings = findViewById(R.id.tvEarnings)
        layoutBrowseJobs = findViewById(R.id.btnBrowseJobs)
        layoutMyApplications = findViewById(R.id.btnMyApplications)
        btnProfile = findViewById(R.id.btnProfile)
        rvJobs = findViewById(R.id.rvJobs)
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        // If you have a progress bar, initialize it here
        // progressBar = findViewById(R.id.progressBar)
    }

    private fun setupBottomNavigation() {
        try {
            bottomNavigationView.setOnNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home -> {
                        // Already on home, just refresh the data
                        loadJobsFromFirebase()
                        loadDashboardStats()
                        true
                    }
                    R.id.nav_jobs -> {
                        val intent = Intent(this, JobBrowseActivity::class.java)
                        startActivity(intent)
                        true
                    }
                    R.id.nav_applications -> {
                        val intent = Intent(this, MyApplicationsActivity::class.java)
                        startActivity(intent)
                        true
                    }
                    R.id.nav_profile -> {
                        val intent = Intent(this, ProfileActivity::class.java)
                        startActivity(intent)
                        true
                    }
                    else -> false
                }
            }

            bottomNavigationView.selectedItemId = R.id.nav_home

        } catch (e: Exception) {
            Log.e("BottomNav", "Bottom navigation setup failed: ${e.message}")
        }
    }

    private fun loadUserData() {
        val userEmail = sessionManager.getEmail() ?: "User"
        val userName = extractNameFromEmail(userEmail)
        tvWelcome.text = "Welcome, $userName!"
    }

    private fun extractNameFromEmail(email: String): String {
        return try {
            val namePart = email.substringBefore("@")
            namePart.replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            "Freelancer"
        }
    }

    private fun setupRecyclerView() {
        jobsAdapter = JobsAdapter(jobList) { job ->
            onJobItemClick(job)
        }
        rvJobs.apply {
            layoutManager = LinearLayoutManager(this@FreelancerDashboardActivity)
            adapter = jobsAdapter
        }
    }

    private fun setupClickListeners() {
        layoutBrowseJobs.setOnClickListener {
            navigateToJobBrowse()
        }

        layoutMyApplications.setOnClickListener {
            navigateToMyApplications()
        }

        btnProfile.setOnClickListener {
            navigateToProfile()
        }
    }

    private fun loadJobsFromFirebase() {
        // Show loading state if you have a progress bar
        // progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("Dashboard", "🟡 Loading featured jobs from Firebase")
                val result = firebaseService.getJobs()

                withContext(Dispatchers.Main) {
                    // progressBar.visibility = View.GONE

                    if (result.isSuccess) {
                        val jobs = result.getOrNull() ?: emptyList()
                        Log.d("Dashboard", "🟢 Loaded ${jobs.size} jobs from Firebase")

                        jobList.clear()

                        // Show only featured/recent jobs (limit to 3-5 for dashboard)
                        val featuredJobs = jobs
                            .sortedByDescending { it.postedAt } // Show newest first
                            .take(4) // Limit to 4 jobs for dashboard

                        jobList.addAll(featuredJobs)
                        jobsAdapter.notifyDataSetChanged()

                        if (featuredJobs.isEmpty()) {
                            // Show empty state if no jobs
                            Toast.makeText(
                                this@FreelancerDashboardActivity,
                                "No jobs available at the moment",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        val error = result.exceptionOrNull()?.message ?: "Unknown error"
                        Log.e("Dashboard", "🔴 Failed to load jobs: $error")
                        Toast.makeText(
                            this@FreelancerDashboardActivity,
                            "Failed to load jobs",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("Dashboard", "💥 Error loading jobs: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    // progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@FreelancerDashboardActivity,
                        "Network error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun loadDashboardStats() {
        val userId = sessionManager.getUserId() ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Load user's applications to calculate stats
                val applicationsResult = firebaseService.getApplicationsByFreelancer(userId)

                withContext(Dispatchers.Main) {
                    if (applicationsResult.isSuccess) {
                        val applications = applicationsResult.getOrNull() ?: emptyList()

                        // Calculate statistics
                        val totalApplied = applications.size
                        val activeApplications = applications.count { it.status == "pending" }
                        val acceptedApplications = applications.count { it.status == "accepted" }

                        // Calculate earnings (sum of accepted application budgets)
                        val totalEarnings = applications
                            .filter { it.status == "accepted" }
                            .sumOf { it.proposedBudget }

                        // Update UI with real data
                        tvAppliedCount.text = totalApplied.toString()
                        tvActiveCount.text = activeApplications.toString()
                        tvEarnings.text = "R ${"%.2f".format(totalEarnings)}"

                        Log.d("Dashboard", "📊 Stats: $totalApplied applied, $activeApplications active, R$totalEarnings earnings")
                    } else {
                        // Fallback to placeholder stats if data loading fails
                        setPlaceholderStats()
                    }
                }
            } catch (e: Exception) {
                Log.e("Dashboard", "💥 Error loading stats: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    setPlaceholderStats()
                }
            }
        }
    }

    private fun setPlaceholderStats() {
        // Use placeholder data if real data fails to load
        tvAppliedCount.text = "0"
        tvActiveCount.text = "0"
        tvEarnings.text = "R 0.00"
    }

    private fun onJobItemClick(job: Job) {
        val intent = Intent(this, JobDetailsActivity::class.java).apply {
            putExtra("JOB_ID", job.jobId)
            putExtra("JOB_TITLE", job.title)
        }
        startActivity(intent)
    }

    private fun navigateToJobBrowse() {
        val intent = Intent(this, JobBrowseActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToMyApplications() {
        val intent = Intent(this, MyApplicationsActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToProfile() {
        val intent = Intent(this, ProfileActivity::class.java)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to dashboard
        loadJobsFromFirebase()
        loadDashboardStats()
    }
}
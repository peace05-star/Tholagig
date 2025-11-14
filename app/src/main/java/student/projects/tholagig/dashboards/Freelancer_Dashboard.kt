package student.projects.tholagig.dashboards

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import student.projects.tholagig.BaseActivity
import student.projects.tholagig.R
import student.projects.tholagig.Adapters.JobsAdapter
import student.projects.tholagig.jobs.JobBrowseActivity
import student.projects.tholagig.jobs.JobDetailsActivity
import student.projects.tholagig.jobs.MyApplicationsActivity
import student.projects.tholagig.messaging.ConversationsActivity
import student.projects.tholagig.models.Job
import student.projects.tholagig.network.FirebaseService
import student.projects.tholagig.network.SessionManager
import student.projects.tholagig.profile.ProfileActivity

class FreelancerDashboardActivity : BaseActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var tvAppliedCount: TextView
    private lateinit var tvActiveCount: TextView
    private lateinit var tvEarnings: TextView
    private lateinit var tvAppliedLabel: TextView // Changed from tvAppliedJobs
    private lateinit var tvActiveLabel: TextView // Changed from tvActiveJobs
    private lateinit var tvEarningsLabel: TextView // Changed from tvTotalEarnings
    private lateinit var layoutBrowseJobs: LinearLayout
    private lateinit var layoutMyApplications: LinearLayout
    private lateinit var btnProfile: ImageButton
    private lateinit var rvJobs: RecyclerView
    private lateinit var bottomNavigationView: BottomNavigationView

    private lateinit var jobsAdapter: JobsAdapter
    private lateinit var sessionManager: SessionManager
    private lateinit var firebaseService: FirebaseService
    private val jobList = mutableListOf<Job>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_freelancer_dashboard)

        sessionManager = SessionManager(this)
        firebaseService = FirebaseService()

        initializeViews()
        setupRecyclerView()
        setupClickListeners()
        setupBottomNavigation()
        loadUserData()
        loadJobsFromFirebase()
        loadDashboardStats()
        updateUITexts()
    }

    private fun initializeViews() {
        tvWelcome = findViewById(R.id.tvWelcome)
        tvAppliedCount = findViewById(R.id.tvAppliedCount)
        tvActiveCount = findViewById(R.id.tvActiveCount)
        tvEarnings = findViewById(R.id.tvEarnings)
        tvAppliedLabel = findViewById(R.id.tvAppliedLabel) // Correct ID
        tvActiveLabel = findViewById(R.id.tvActiveLabel) // Correct ID
        tvEarningsLabel = findViewById(R.id.tvEarningsLabel) // Correct ID
        layoutBrowseJobs = findViewById(R.id.btnBrowseJobs)
        layoutMyApplications = findViewById(R.id.btnMyApplications)
        btnProfile = findViewById(R.id.btnProfile)
        rvJobs = findViewById(R.id.rvJobs)
        bottomNavigationView = findViewById(R.id.bottom_navigation)
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
                        finish()
                        true
                    }
                    R.id.nav_applications -> {
                        val intent = Intent(this, MyApplicationsActivity::class.java)
                        startActivity(intent)
                        finish()
                        true
                    }
                    R.id.nav_messages -> {
                        // Navigate to Messages/Conversations
                        navigateToMessages()
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

            bottomNavigationView.selectedItemId = R.id.nav_home

        } catch (e: Exception) {
            Log.e("BottomNav", "Bottom navigation setup failed: ${e.message}")
        }
    }

    private fun navigateToMessages() {
        try {
            val intent = Intent(this, ConversationsActivity::class.java)
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e("Navigation", "Messages activity not found: ${e.message}")
            Toast.makeText(this, getString(R.string.messages_feature_coming_soon), Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUITexts() {
        // Update UI texts based on current language
        val welcomeText = String.format(getString(R.string.welcome_user), extractNameFromEmail(sessionManager.getEmail() ?: "User"))
        tvWelcome.text = welcomeText

        // Update quick action texts
        findViewById<TextView>(R.id.tvQuickActions).text = getString(R.string.quick_actions)
        findViewById<TextView>(R.id.tvRecommendedJobs).text = getString(R.string.recommended_jobs)

        // Update card titles with correct TextViews
        tvAppliedLabel.text = getString(R.string.applied_jobs)
        tvActiveLabel.text = getString(R.string.active_jobs)
        tvEarningsLabel.text = getString(R.string.total_earnings)

        // Also update the quick action buttons if needed
        findViewById<TextView>(R.id.tvBrowseJobs).text = getString(R.string.browse_jobs)
        findViewById<TextView>(R.id.tvMyApplications).text = getString(R.string.my_applications)
    }

    private fun loadUserData() {
        val userEmail = sessionManager.getEmail() ?: "User"
        val userName = extractNameFromEmail(userEmail)
        val welcomeText = String.format(getString(R.string.welcome_user), userName)
        tvWelcome.text = welcomeText
    }

    private fun extractNameFromEmail(email: String): String {
        return try {
            val namePart = email.substringBefore("@")
            namePart.replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            getString(R.string.freelancer)
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
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("Dashboard", "Loading featured jobs from Firebase")
                val result = firebaseService.getJobs()

                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        val jobs = result.getOrNull() ?: emptyList()
                        Log.d("Dashboard", "Loaded ${jobs.size} jobs from Firebase")

                        jobList.clear()

                        // Show only featured/recent jobs (limit to 3-5 for dashboard)
                        val featuredJobs = jobs
                            .sortedByDescending { it.postedAt }
                            .take(4)

                        jobList.addAll(featuredJobs)
                        jobsAdapter.notifyDataSetChanged()

                        if (featuredJobs.isEmpty()) {
                            Toast.makeText(
                                this@FreelancerDashboardActivity,
                                getString(R.string.no_jobs_available),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        val error = result.exceptionOrNull()?.message ?: "Unknown error"
                        Log.e("Dashboard", "Failed to load jobs: $error")
                        Toast.makeText(
                            this@FreelancerDashboardActivity,
                            getString(R.string.failed_to_load_jobs),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("Dashboard", "Error loading jobs: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@FreelancerDashboardActivity,
                        "${getString(R.string.network_error)}: ${e.message}",
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

                        Log.d("Dashboard", "Stats: $totalApplied applied, $activeApplications active, R$totalEarnings earnings")
                    } else {
                        // Fallback to placeholder stats if data loading fails
                        setPlaceholderStats()
                    }
                }
            } catch (e: Exception) {
                Log.e("Dashboard", "Error loading stats: ${e.message}", e)
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
        updateUITexts()
    }
}
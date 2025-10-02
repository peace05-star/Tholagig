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
import student.projects.tholagig.models.JobApplication
import student.projects.tholagig.profile.ProfileActivity
import android.content.Intent
import student.projects.tholagig.jobs.MyApplicationsActivity
import student.projects.tholagig.network.SessionManager
import student.projects.tholagig.network.FirebaseService
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.util.Log
import kotlinx.coroutines.*
import java.text.NumberFormat
import java.util.*

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

    private lateinit var jobsAdapter: JobsAdapter
    private lateinit var sessionManager: SessionManager
    private lateinit var firebaseService: FirebaseService

    private val jobList = mutableListOf<Job>()
    private val allApplications = mutableListOf<JobApplication>()

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
        loadRealData()
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
    }

    private fun setupBottomNavigation() {
        try {
            bottomNavigationView.setOnNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home -> {
                        // Already on home, just refresh the data
                        loadRealData()
                        true
                    }
                    R.id.nav_jobs -> {
                        // Navigate to job browse
                        val intent = Intent(this, JobBrowseActivity::class.java)
                        startActivity(intent)
                        overridePendingTransition(0, 0)
                        true
                    }
                    R.id.nav_applications -> {
                        // Navigate to my applications
                        val intent = Intent(this, MyApplicationsActivity::class.java)
                        startActivity(intent)
                        overridePendingTransition(0, 0)
                        true
                    }
                    R.id.nav_profile -> {
                        // Navigate to profile
                        val intent = Intent(this, ProfileActivity::class.java)
                        startActivity(intent)
                        overridePendingTransition(0, 0)
                        true
                    }
                    else -> false
                }
            }

            // Set the home item as selected by default
            bottomNavigationView.selectedItemId = R.id.nav_home

        } catch (e: Exception) {
            Log.e("BottomNav", "Bottom navigation setup failed: ${e.message}")
            Toast.makeText(this, "Navigation error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = sessionManager.getUserId()
                if (!userId.isNullOrEmpty()) {
                    val userResult = firebaseService.getUserById(userId)
                    if (userResult.isSuccess) {
                        val user = userResult.getOrNull()
                        withContext(Dispatchers.Main) {
                            val displayName = user?.fullName ?: sessionManager.getUserName() ?: extractNameFromEmail(sessionManager.getEmail() ?: "User")
                            tvWelcome.text = "Welcome, $displayName!"
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            fallbackUserData()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        fallbackUserData()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    fallbackUserData()
                }
            }
        }
    }

    private fun fallbackUserData() {
        val userEmail = sessionManager.getEmail() ?: "User"
        val userName = sessionManager.getUserName() ?: extractNameFromEmail(userEmail)
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

    private fun loadRealData() {
        // Show loading state
        tvAppliedCount.text = "0"
        tvActiveCount.text = "0"
        tvEarnings.text = "R 0"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = sessionManager.getUserId()
                if (userId.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        loadMockDataAsFallback("User not logged in")
                    }
                    return@launch
                }

                // Fetch all data in parallel
                val jobsDeferred = async { firebaseService.getJobs() }
                val applicationsDeferred = async { firebaseService.getApplicationsByFreelancer(userId) }
                val statsDeferred = async { firebaseService.getFreelancerStats(userId) }

                // Wait for all requests
                val jobsResult = jobsDeferred.await()
                val applicationsResult = applicationsDeferred.await()
                val statsResult = statsDeferred.await()

                withContext(Dispatchers.Main) {
                    // Handle jobs data
                    if (jobsResult.isSuccess) {
                        val allJobs = jobsResult.getOrNull() ?: emptyList()
                        updateRecommendedJobs(allJobs)
                    } else {
                        Log.e("Dashboard", "Failed to load jobs: ${jobsResult.exceptionOrNull()?.message}")
                        loadMockJobsAsFallback()
                    }

                    // Handle applications and stats
                    if (applicationsResult.isSuccess) {
                        allApplications.clear()
                        allApplications.addAll(applicationsResult.getOrNull() ?: emptyList())
                    }

                    if (statsResult.isSuccess) {
                        updateDashboardStats(statsResult.getOrNull())
                    } else {
                        // Fallback to calculating stats from applications
                        updateDashboardStatsFromApplications()
                        Log.e("Dashboard", "Failed to load stats: ${statsResult.exceptionOrNull()?.message}")
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadMockDataAsFallback("Error: ${e.message}")
                    Log.e("Dashboard", "Error loading data: ${e.message}")
                }
            }
        }
    }

    private fun updateRecommendedJobs(allJobs: List<Job>) {
        jobList.clear()

        // Get user skills for personalized recommendations
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = sessionManager.getUserId()
                val recommendedJobs = if (!userId.isNullOrEmpty()) {
                    val skillsResult = firebaseService.getFreelancerSkills(userId)
                    if (skillsResult.isSuccess) {
                        val userSkills = skillsResult.getOrNull() ?: emptyList()
                        getPersonalizedJobs(allJobs, userSkills)
                    } else {
                        getDefaultRecommendedJobs(allJobs)
                    }
                } else {
                    getDefaultRecommendedJobs(allJobs)
                }

                withContext(Dispatchers.Main) {
                    jobList.addAll(recommendedJobs)
                    jobsAdapter.notifyDataSetChanged()

                    if (jobList.isEmpty()) {
                        // Show empty state or fallback to default jobs
                        loadDefaultJobsAsFallback(allJobs)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadDefaultJobsAsFallback(allJobs)
                }
            }
        }
    }

    private fun getPersonalizedJobs(allJobs: List<Job>, userSkills: List<String>): List<Job> {
        return if (userSkills.isNotEmpty()) {
            // Filter jobs that match user skills
            val matchingJobs = allJobs.filter { job ->
                job.skillsRequired.any { requiredSkill ->
                    userSkills.any { userSkill ->
                        userSkill.contains(requiredSkill, ignoreCase = true) ||
                                requiredSkill.contains(userSkill, ignoreCase = true)
                    }
                }
            }

            // If we have matching jobs, return them sorted by relevance
            if (matchingJobs.isNotEmpty()) {
                matchingJobs.sortedByDescending { it.postedAt ?: it.createdDate ?: Date() }.take(3)
            } else {
                getDefaultRecommendedJobs(allJobs)
            }
        } else {
            getDefaultRecommendedJobs(allJobs)
        }
    }

    private fun getDefaultRecommendedJobs(allJobs: List<Job>): List<Job> {
        // Return newest open jobs
        return allJobs
            .filter { it.status == "open" }
            .sortedByDescending { it.postedAt ?: it.createdDate ?: Date() }
            .take(3)
    }

    private fun loadDefaultJobsAsFallback(allJobs: List<Job>) {
        val defaultJobs = getDefaultRecommendedJobs(allJobs)
        jobList.clear()
        jobList.addAll(defaultJobs)
        jobsAdapter.notifyDataSetChanged()

        if (jobList.isEmpty()) {
            // Ultimate fallback - load mock jobs
            loadMockJobsAsFallback()
        }
    }

    private fun updateDashboardStats(stats: FirebaseService.FreelancerStats?) {
        if (stats != null) {
            tvAppliedCount.text = stats.totalApplications.toString()
            tvActiveCount.text = stats.acceptedApplications.toString()
            tvEarnings.text = formatCurrency(stats.totalEarnings)
        } else {
            updateDashboardStatsFromApplications()
        }
    }

    private fun updateDashboardStatsFromApplications() {
        val totalApplications = allApplications.size
        val activeJobs = allApplications.count { it.status == "accepted" }
        val totalEarnings = allApplications
            .filter { it.status == "accepted" }
            .sumOf { it.proposedBudget }

        tvAppliedCount.text = totalApplications.toString()
        tvActiveCount.text = activeJobs.toString()
        tvEarnings.text = formatCurrency(totalEarnings)
    }

    private fun formatCurrency(amount: Double): String {
        return try {
            val format = NumberFormat.getCurrencyInstance()
            format.maximumFractionDigits = 0
            format.currency = Currency.getInstance("ZAR")
            format.format(amount)
        } catch (e: Exception) {
            "R ${amount.toInt()}"
        }
    }

    // Fallback methods
    private fun loadMockJobsAsFallback() {
        jobList.clear()
        jobList.addAll(createMockJobs())
        jobsAdapter.notifyDataSetChanged()
    }

    private fun loadMockDataAsFallback(errorMessage: String) {
        loadMockJobsAsFallback()
        updateMockStats()
        Toast.makeText(this, "Using demo data - $errorMessage", Toast.LENGTH_SHORT).show()
    }

    private fun updateMockStats() {
        tvAppliedCount.text = "0"
        tvActiveCount.text = "0"
        tvEarnings.text = "R 0"
    }

    private fun createMockJobs(): List<Job> {
        return listOf(
            Job(
                jobId = "1",
                title = "Mobile App Developer",
                clientName = "Tech Solutions SA",
                description = "Need experienced Kotlin/Android developer for e-commerce app",
                budget = 15000.0,
                category = "Mobile Development",
                skillsRequired = listOf("Kotlin", "Android SDK", "Firebase"),
                location = "Remote",
                deadline = Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000),
                postedAt = Date(),
                status = "open"
            ),
            Job(
                jobId = "2",
                title = "UI/UX Designer",
                clientName = "Creative Agency",
                description = "Design modern UI for financial application",
                budget = 12000.0,
                category = "Design",
                skillsRequired = listOf("Figma", "Adobe XD", "UI Design"),
                location = "Cape Town",
                deadline = Date(System.currentTimeMillis() + 5 * 24 * 60 * 60 * 1000),
                postedAt = Date(),
                status = "open"
            )
        )
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
        // Refresh data when returning to this activity
        bottomNavigationView.selectedItemId = R.id.nav_home
        loadRealData()
    }
}
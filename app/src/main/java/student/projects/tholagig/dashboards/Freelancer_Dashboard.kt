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
import com.google.android.material.bottomnavigation.BottomNavigationView // ADD THIS IMPORT
import android.util.Log // ADD THIS IMPORT

class FreelancerDashboardActivity : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var tvAppliedCount: TextView
    private lateinit var tvActiveCount: TextView
    private lateinit var tvEarnings: TextView
    private lateinit var layoutBrowseJobs: LinearLayout
    private lateinit var layoutMyApplications: LinearLayout
    private lateinit var btnProfile: ImageButton
    private lateinit var rvJobs: RecyclerView
    private lateinit var bottomNavigationView: BottomNavigationView // ADD THIS

    private lateinit var jobsAdapter: JobsAdapter
    private val jobList = mutableListOf<Job>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_freelancer_dashboard)

        initializeViews()
        setupRecyclerView()
        setupClickListeners()
        setupBottomNavigation() // ADD THIS LINE
        loadUserData()
        loadMockData()
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
        bottomNavigationView = findViewById(R.id.bottom_navigation) // ADD THIS
    }

    // ADD THIS NEW METHOD
    private fun setupBottomNavigation() {
        try {
            bottomNavigationView.setOnNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home -> {
                        // Already on home, just refresh the data
                        loadMockData()
                        Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.nav_jobs -> {
                        // Navigate to job browse
                        val intent = Intent(this, JobBrowseActivity::class.java)
                        startActivity(intent)
                        true
                    }
                    R.id.nav_applications -> {
                        // Navigate to my applications
                        val intent = Intent(this, MyApplicationsActivity::class.java)
                        startActivity(intent)
                        true
                    }
                    R.id.nav_profile -> {
                        // Navigate to profile
                        val intent = Intent(this, ProfileActivity::class.java)
                        startActivity(intent)
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
        val sessionManager = SessionManager(this)
        val userEmail = sessionManager.getEmail() ?: "User"

        // Extract name from email (everything before @) or use a placeholder
        val userName = extractNameFromEmail(userEmail)

        tvWelcome.text = "Welcome, $userName!"
    }

    private fun extractNameFromEmail(email: String): String {
        return try {
            // Get the part before @ and capitalize first letter
            val namePart = email.substringBefore("@")
            namePart.replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            "Freelancer" // Fallback
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

    private fun loadMockData() {
        // Mock data - replace with actual API call
        jobList.clear()
        jobList.addAll(
            listOf(
                Job(
                    jobId = "1",
                    title = "Mobile App Developer",
                    clientName = "Tech Solutions SA",
                    description = "Need experienced Kotlin/Android developer for e-commerce app",
                    budget = 15000.0,
                    category = "Mobile Development",
                    skillsRequired = listOf("Kotlin", "Android SDK", "Firebase"),
                    location = "Remote",
                    deadline = java.util.Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000)
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
                    deadline = java.util.Date(System.currentTimeMillis() + 5 * 24 * 60 * 60 * 1000)
                ),
                Job(
                    jobId = "3",
                    title = "Backend Developer",
                    clientName = "FinTech Startup",
                    description = "Build REST APIs for banking system",
                    budget = 20000.0,
                    category = "Backend Development",
                    skillsRequired = listOf("Node.js", "MongoDB", "AWS"),
                    location = "Johannesburg",
                    deadline = java.util.Date(System.currentTimeMillis() + 10 * 24 * 60 * 60 * 1000)
                )
            )
        )
        jobsAdapter.notifyDataSetChanged()

        // Update stats
        tvAppliedCount.text = "5"
        tvActiveCount.text = "2"
        tvEarnings.text = "R 5,200"
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
}
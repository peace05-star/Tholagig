package student.projects.tholagig.dashboards

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import student.projects.tholagig.R
import student.projects.tholagig.Adapters.RecentJobsAdapter
import student.projects.tholagig.auth.LoginActivity
import student.projects.tholagig.jobs.CreateJobActivity
import student.projects.tholagig.jobs.MyJobsActivity
import student.projects.tholagig.jobs.ApplicationsManagementActivity
import student.projects.tholagig.jobs.JobDetailsActivity
import student.projects.tholagig.models.Job
import student.projects.tholagig.network.SessionManager
import student.projects.tholagig.profile.ProfileActivity
import java.text.NumberFormat
import java.util.*

class ClientDashboardActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var tvWelcome: TextView
    private lateinit var tvActiveJobs: TextView
    private lateinit var tvTotalApplications: TextView
    private lateinit var tvHiredFreelancers: TextView
    private lateinit var tvSpentAmount: TextView
    private lateinit var rvRecentJobs: RecyclerView
    private lateinit var fabCreateJob: FloatingActionButton

    // Client's posted jobs (in real app, this would come from Firebase)
    private val clientJobs = listOf(
        Job(
            jobId = "job_1",
            clientId = "client_123",
            clientName = "Your Company",
            title = "Mobile App Developer",
            description = "Need experienced mobile developer for Android/iOS app",
            category = "Development",
            skillsRequired = listOf("Kotlin", "Android", "Firebase"),
            budget = 15000.0,
            deadline = Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000),
            location = "Johannesburg",
            status = "open",
            postedAt = Date(),
            applicationsCount = 8 // Number of applicants
        ),
        Job(
            jobId = "job_2",
            clientId = "client_123",
            clientName = "Your Company",
            title = "UI/UX Designer",
            description = "Redesign our mobile application interface",
            category = "Design",
            skillsRequired = listOf("Figma", "UI/UX", "Wireframing"),
            budget = 12000.0,
            deadline = Date(System.currentTimeMillis() + 20L * 24 * 60 * 60 * 1000),
            location = "Cape Town",
            status = "open",
            postedAt = Date(System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000),
            applicationsCount = 5
        ),
        Job(
            jobId = "job_3",
            clientId = "client_123",
            clientName = "Your Company",
            title = "Social Media Manager",
            description = "Manage our social media presence",
            category = "Marketing",
            skillsRequired = listOf("Social Media", "Content Creation"),
            budget = 8000.0,
            deadline = Date(System.currentTimeMillis() - 5L * 24 * 60 * 60 * 1000),
            location = "Remote",
            status = "in_progress",
            postedAt = Date(System.currentTimeMillis() - 10 * 24 * 60 * 60 * 1000),
            applicationsCount = 12
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_dashboard)

        sessionManager = SessionManager(this)
        initializeViews()
        setupRecyclerView()
        setupClickListeners()
        loadDashboardData()
    }

    private fun initializeViews() {
        tvWelcome = findViewById(R.id.tvWelcome)
        tvActiveJobs = findViewById(R.id.tvActiveJobs)
        tvTotalApplications = findViewById(R.id.tvTotalApplications)
        tvHiredFreelancers = findViewById(R.id.tvHiredFreelancers)
        tvSpentAmount = findViewById(R.id.tvSpentAmount)
        rvRecentJobs = findViewById(R.id.rvRecentJobs)
        fabCreateJob = findViewById(R.id.fabCreateJob)
    }

    private fun setupRecyclerView() {
        val adapter = RecentJobsAdapter(clientJobs) { job ->
            openJobDetails(job)
        }
        rvRecentJobs.layoutManager = LinearLayoutManager(this)
        rvRecentJobs.adapter = adapter
    }

    private fun setupClickListeners() {
        // Active Jobs Card - View all jobs posted
        findViewById<CardView>(R.id.cardActiveJobs).setOnClickListener {
            startActivity(Intent(this, MyJobsActivity::class.java))
        }

        // Applications Card - Review applications
        findViewById<CardView>(R.id.cardApplications).setOnClickListener {
            startActivity(Intent(this, ApplicationsManagementActivity::class.java))
        }

        // Hired Freelancers Card - View current hires
        findViewById<CardView>(R.id.cardHiredFreelancers).setOnClickListener {
            openHiredFreelancers()
        }

        // Total Spent Card - View spending analytics
        findViewById<CardView>(R.id.cardSpentAmount).setOnClickListener {
            openSpendingAnalytics()
        }

        // Create new job FAB
        fabCreateJob.setOnClickListener {
            startActivity(Intent(this, CreateJobActivity::class.java))
        }
    }

    private fun loadDashboardData() {
        val clientName = sessionManager.getUserName() ?: "Client"
        tvWelcome.text = "Welcome, $clientName!"

        // Calculate dashboard stats from client's jobs
        val activeJobs = clientJobs.count { it.status == "open" }
        val totalApplications = clientJobs.sumOf { it.applicationsCount }
        val hiredFreelancers = clientJobs.count { it.status == "in_progress" }
        val totalSpent = 32000.0 // This would come from completed jobs in real app

        updateStats(activeJobs, totalApplications, hiredFreelancers, totalSpent)
    }

    private fun updateStats(activeJobs: Int, totalApplications: Int, hiredFreelancers: Int, totalSpent: Double) {
        tvActiveJobs.text = activeJobs.toString()
        tvTotalApplications.text = totalApplications.toString()
        tvHiredFreelancers.text = hiredFreelancers.toString()

        val format = NumberFormat.getCurrencyInstance()
        format.maximumFractionDigits = 0
        format.currency = Currency.getInstance("ZAR")
        tvSpentAmount.text = format.format(totalSpent)
    }

    private fun openJobDetails(job: Job) {
        // Open job details to see applications and manage the job
        val intent = Intent(this, JobDetailsActivity::class.java).apply {
            putExtra("JOB_ID", job.jobId)
            putExtra("IS_CLIENT_VIEW", true)
        }
        startActivity(intent)
    }

    private fun openHiredFreelancers() {
        // TODO: Implement hired freelancers management
        Toast.makeText(this, "Hired Freelancers Management - Coming Soon!", Toast.LENGTH_SHORT).show()
    }

    private fun openSpendingAnalytics() {
        // TODO: Implement spending analytics
        Toast.makeText(this, "Spending Analytics - Coming Soon!", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_client_dashboard, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_profile -> {
                startActivity(Intent(this, ProfileActivity::class.java))
                true
            }
            R.id.menu_logout -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logout() {
        sessionManager.logout()
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
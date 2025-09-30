package student.projects.tholagig.dashboards

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.*
import student.projects.tholagig.R
import student.projects.tholagig.Adapters.RecentJobsAdapter
import student.projects.tholagig.auth.LoginActivity
import student.projects.tholagig.jobs.CreateJobActivity
import student.projects.tholagig.jobs.MyJobsActivity
import student.projects.tholagig.jobs.ApplicationsManagementActivity
import student.projects.tholagig.jobs.JobDetailsActivity
import student.projects.tholagig.models.Job
import student.projects.tholagig.models.JobApplication
import student.projects.tholagig.network.FirebaseService
import student.projects.tholagig.network.SessionManager
import student.projects.tholagig.profile.ProfileActivity
import java.text.NumberFormat
import java.util.*

class ClientDashboardActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var firebaseService: FirebaseService
    private lateinit var tvWelcome: TextView
    private lateinit var tvActiveJobs: TextView
    private lateinit var tvTotalApplications: TextView
    private lateinit var tvHiredFreelancers: TextView
    private lateinit var tvSpentAmount: TextView
    private lateinit var rvRecentJobs: RecyclerView
    private lateinit var fabCreateJob: FloatingActionButton
    private lateinit var progressBar: ProgressBar // Make sure this is declared

    private lateinit var recentJobsAdapter: RecentJobsAdapter
    private val recentJobs = mutableListOf<Job>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_dashboard)

        sessionManager = SessionManager(this)
        firebaseService = FirebaseService()

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

        // Initialize progressBar - ADD THIS LINE
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupRecyclerView() {
        recentJobsAdapter = RecentJobsAdapter(recentJobs) { job ->
            openJobDetails(job)
        }
        rvRecentJobs.layoutManager = LinearLayoutManager(this)
        rvRecentJobs.adapter = recentJobsAdapter
    }

    private fun setupClickListeners() {
        findViewById<CardView>(R.id.cardActiveJobs).setOnClickListener {
            startActivity(Intent(this, MyJobsActivity::class.java))
        }

        findViewById<CardView>(R.id.cardApplications).setOnClickListener {
            startActivity(Intent(this, ApplicationsManagementActivity::class.java))
        }

        findViewById<CardView>(R.id.cardHiredFreelancers).setOnClickListener {
            openHiredFreelancers()
        }

        findViewById<CardView>(R.id.cardSpentAmount).setOnClickListener {
            openSpendingAnalytics()
        }

        fabCreateJob.setOnClickListener {
            startActivity(Intent(this, CreateJobActivity::class.java))
        }
    }

    private fun loadDashboardData() {
        progressBar.visibility = View.VISIBLE

        val clientId = sessionManager.getUserId() ?: ""
        if (clientId.isEmpty()) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Load client data
                val userResult = firebaseService.getUserById(clientId)
                val jobsResult = firebaseService.getJobs()
                val applicationsResult = firebaseService.getApplicationsByClient(clientId)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE

                    if (userResult.isSuccess) {
                        val user = userResult.getOrNull()
                        tvWelcome.text = "Welcome, ${user?.fullName ?: "Client"}!"
                    }

                    if (jobsResult.isSuccess) {
                        val allJobs = jobsResult.getOrNull() ?: emptyList()
                        val clientJobs = allJobs.filter { it.clientId == clientId }
                        updateJobsData(clientJobs)

                        if (applicationsResult.isSuccess) {
                            val applications = applicationsResult.getOrNull() ?: emptyList()
                            updateStats(clientJobs, applications) // pass clientJobs
                        }
                    }

                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@ClientDashboardActivity, "Error loading dashboard: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateJobsData(jobs: List<Job>) {
        // Get recent jobs (last 3)
        recentJobs.clear()
        recentJobs.addAll(jobs)
        recentJobsAdapter.notifyDataSetChanged()
        Log.d("ClientDashboard", "🔄 Loaded ${recentJobs.size} jobs into the list")
    }

    private fun updateStats(jobs: List<Job>, applications: List<JobApplication>) {
        // Count only active jobs from this client
        val activeJobs = jobs.count { it.status.equals("open", ignoreCase = true) }

        Log.d("ClientDashboard", "📊 Active jobs: $activeJobs out of ${jobs.size} total jobs")

        tvActiveJobs.text = activeJobs.toString()
        tvTotalApplications.text = applications.size.toString()
        tvHiredFreelancers.text = applications.count { it.status.equals("accepted", ignoreCase = true) }.toString()

        val totalSpent = applications
            .filter { it.status.equals("accepted", ignoreCase = true) }
            .sumOf { it.proposedBudget }

        val format = NumberFormat.getCurrencyInstance().apply {
            maximumFractionDigits = 0
            currency = Currency.getInstance("ZAR")
        }
        tvSpentAmount.text = format.format(totalSpent)
    }



    private fun openJobDetails(job: Job) {
        val intent = Intent(this, JobDetailsActivity::class.java).apply {
            putExtra("JOB_ID", job.jobId)
            putExtra("IS_CLIENT_VIEW", true)
        }
        startActivity(intent)
    }

    private fun openHiredFreelancers() {
        Toast.makeText(this, "Hired Freelancers Management - Coming Soon!", Toast.LENGTH_SHORT).show()
    }

    private fun openSpendingAnalytics() {
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

    override fun onResume() {
        super.onResume()
        loadDashboardData() // Refresh when returning to dashboard
    }
}
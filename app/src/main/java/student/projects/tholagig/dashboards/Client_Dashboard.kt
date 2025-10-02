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
import student.projects.tholagig.profile.ClientProfileActivity
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
    private lateinit var progressBar: ProgressBar

    private lateinit var recentJobsAdapter: RecentJobsAdapter

    private lateinit var btnProfile: ImageButton
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
        progressBar = findViewById(R.id.progressBar)
        btnProfile = findViewById(R.id.btnProfile)
    }

    private fun setupRecyclerView() {
        recentJobsAdapter = RecentJobsAdapter(recentJobs) { job ->
            openJobDetails(job)
        }
        rvRecentJobs.layoutManager = LinearLayoutManager(this)
        rvRecentJobs.adapter = recentJobsAdapter
    }

    private fun setupClickListeners() {

            btnProfile.setOnClickListener {
                val intent = Intent(this, ClientProfileActivity::class.java)
                startActivity(intent)
            }

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
                            updateStats(clientJobs, applications)
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
        recentJobs.clear()
        recentJobs.addAll(jobs)
        recentJobsAdapter.notifyDataSetChanged()
        Log.d("ClientDashboard", "Loaded ${recentJobs.size} jobs into the list")
    }

    private fun updateStats(jobs: List<Job>, applications: List<JobApplication>) {
        val activeJobs = jobs.count { it.status.equals("open", ignoreCase = true) }

        Log.d("ClientDashboard", "Active jobs: $activeJobs out of ${jobs.size} total jobs")

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
        val clientId = sessionManager.getUserId() ?: ""
        if (clientId.isEmpty()) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val applicationsResult = firebaseService.getApplicationsByClient(clientId)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE

                    if (applicationsResult.isSuccess) {
                        val applications = applicationsResult.getOrNull() ?: emptyList()
                        val hiredFreelancers = applications.filter {
                            it.status.equals("accepted", ignoreCase = true) ||
                                    it.status.equals("hired", ignoreCase = true)
                        }

                        if (hiredFreelancers.isEmpty()) {
                            androidx.appcompat.app.AlertDialog.Builder(this@ClientDashboardActivity)
                                .setTitle("No Hired Freelancers Yet")
                                .setMessage("You haven't hired any freelancers yet. Start building your team by reviewing applications and hiring talented freelancers for your projects!")
                                .setPositiveButton("Review Applications") { dialog, which ->
                                    startActivity(Intent(this@ClientDashboardActivity, ApplicationsManagementActivity::class.java))
                                }
                                .setNegativeButton("Maybe Later", null)
                                .show()
                        } else {
                            showHiredFreelancersDialog(hiredFreelancers)
                        }
                    } else {
                        Toast.makeText(this@ClientDashboardActivity, "Failed to load hired freelancers", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@ClientDashboardActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showHiredFreelancersDialog(hiredFreelancers: List<JobApplication>) {
        val freelancerList = hiredFreelancers.joinToString("\n\n") { application ->
            """
        ${application.freelancerName}
        Project: ${application.jobTitle}
        Budget: R ${application.proposedBudget.toInt()}
        Timeline: ${application.estimatedTime}
        Rating: ${application.freelancerRating ?: "No rating"} • Jobs: ${application.freelancerCompletedJobs ?: 0}
        Skills: ${application.freelancerSkills?.take(3)?.joinToString(", ") ?: "No skills"}
        """.trimIndent()
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Your Team (${hiredFreelancers.size})")
            .setMessage(freelancerList)
            .setPositiveButton("Manage Applications") { dialog, which ->
                startActivity(Intent(this, ApplicationsManagementActivity::class.java))
            }
            .setNegativeButton("Contact Team") { dialog, which ->
                contactHiredFreelancers(hiredFreelancers)
            }
            .setNeutralButton("Close", null)
            .show()
    }

    private fun contactHiredFreelancers(freelancers: List<JobApplication>) {
        if (freelancers.isEmpty()) return

        val emailAddresses = freelancers.mapNotNull { it.freelancerEmail }.toTypedArray()

        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, emailAddresses)
            putExtra(Intent.EXTRA_SUBJECT, "Update for My Hired Freelancers")
            putExtra(Intent.EXTRA_TEXT,
                "Hello team!\n\n" +
                        "I wanted to provide an update on your projects. Thank you for your great work!\n\n" +
                        "Best regards,\n" +
                        "${sessionManager.getUserName() ?: "Client"}"
            )
        }

        try {
            startActivity(Intent.createChooser(emailIntent, "Email Your Team"))
        } catch (e: Exception) {
            Toast.makeText(this, "No email app found. Please install an email client.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openSpendingAnalytics() {
        val clientId = sessionManager.getUserId() ?: ""
        if (clientId.isEmpty()) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val applicationsResult = firebaseService.getApplicationsByClient(clientId)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE

                    if (applicationsResult.isSuccess) {
                        val applications = applicationsResult.getOrNull() ?: emptyList()
                        showSpendingAnalytics(applications)
                    } else {
                        Toast.makeText(this@ClientDashboardActivity, "Failed to load spending data", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@ClientDashboardActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showSpendingAnalytics(applications: List<JobApplication>) {
        val hiredApplications = applications.filter {
            it.status.equals("accepted", ignoreCase = true) ||
                    it.status.equals("hired", ignoreCase = true)
        }

        val pendingApplications = applications.filter { it.status.equals("pending", ignoreCase = true) }
        val rejectedApplications = applications.filter { it.status.equals("rejected", ignoreCase = true) }

        val totalSpent = hiredApplications.sumOf { it.proposedBudget }
        val pendingSpending = pendingApplications.sumOf { it.proposedBudget }
        val averagePerProject = if (hiredApplications.isNotEmpty()) totalSpent / hiredApplications.size else 0.0
        val topProject = hiredApplications.maxByOrNull { it.proposedBudget }

        val currencyFormat = NumberFormat.getCurrencyInstance().apply {
            maximumFractionDigits = 0
            currency = Currency.getInstance("ZAR")
        }

        val analyticsInfo = """
        Total Spent: ${currencyFormat.format(totalSpent)}
        
        Project Breakdown:
        Hired: ${hiredApplications.size} projects
        Pending: ${pendingApplications.size} projects  
        Rejected: ${rejectedApplications.size} projects
        Total Applications: ${applications.size}
        
        Financial Overview:
        Average per Project: ${currencyFormat.format(averagePerProject)}
        Potential Spending: ${currencyFormat.format(pendingSpending)}
        Most Expensive: ${if (topProject != null) "${currencyFormat.format(topProject.proposedBudget)} (${topProject.jobTitle})" else "N/A"}
        
        Success Rate: ${if (applications.isNotEmpty()) "${(hiredApplications.size * 100 / applications.size)}% hire rate" else "No applications yet"}
        
        Insights:
        ${getSpendingInsight(hiredApplications.size, totalSpent, applications.size)}
    """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Spending Analytics")
            .setMessage(analyticsInfo)
            .setPositiveButton("Manage Projects") { dialog, which ->
                startActivity(Intent(this, ApplicationsManagementActivity::class.java))
            }
            .setNegativeButton("View Details") { dialog, which ->
                showDetailedSpendingBreakdown(applications, hiredApplications, pendingApplications)
            }
            .setNeutralButton("Close", null)
            .show()
    }

    private fun getSpendingInsight(hiredCount: Int, totalSpent: Double, totalApplications: Int): String {
        return when {
            hiredCount == 0 -> "Start hiring to see your analytics grow! Your first freelancer is waiting."
            hiredCount == 1 -> "Great start! You've hired your first freelancer. Consider building a team for bigger projects."
            hiredCount <= 3 -> "You're building momentum! ${if (totalApplications > hiredCount * 2) "You have many applications to review." else "Keep reviewing applications to find more talent."}"
            hiredCount <= 10 -> "You're a pro! You've built a solid team. Consider scaling up with more complex projects."
            else -> "Elite level! You're managing a large team efficiently. Consider mentoring other clients."
        } + if (totalSpent > 10000) " You're investing significantly in quality talent!" else " Your spending is well-managed."
    }

    private fun showDetailedSpendingBreakdown(
        allApplications: List<JobApplication>,
        hiredApplications: List<JobApplication>,
        pendingApplications: List<JobApplication>
    ) {
        val currencyFormat = NumberFormat.getCurrencyInstance().apply {
            maximumFractionDigits = 0
            currency = Currency.getInstance("ZAR")
        }

        val totalSpent = hiredApplications.sumOf { it.proposedBudget }
        val totalPotential = pendingApplications.sumOf { it.proposedBudget }

        val budgetRanges = mapOf(
            "Under R 1,000" to hiredApplications.count { it.proposedBudget < 1000 },
            "R 1,000 - 5,000" to hiredApplications.count { it.proposedBudget in 1000.0..5000.0 },
            "R 5,000 - 10,000" to hiredApplications.count { it.proposedBudget in 5000.0..10000.0 },
            "Over R 10,000" to hiredApplications.count { it.proposedBudget > 10000 }
        )

        val breakdownInfo = """
        Detailed Financial Breakdown
        
        Hired Projects (${hiredApplications.size}):
        Total Spent: ${currencyFormat.format(totalSpent)}
        Average Project: ${currencyFormat.format(if (hiredApplications.isNotEmpty()) totalSpent / hiredApplications.size else 0.0)}
        
        Pending Projects (${pendingApplications.size}):
        Potential Value: ${currencyFormat.format(totalPotential)}
        Average Proposal: ${currencyFormat.format(if (pendingApplications.isNotEmpty()) totalPotential / pendingApplications.size else 0.0)}
        
        Budget Distribution:
        ${budgetRanges.entries.joinToString("\n") { "• ${it.key}: ${it.value} projects" }}
        
        Top 3 Projects:
        ${hiredApplications.sortedByDescending { it.proposedBudget }.take(3).joinToString("\n") {
            "• ${it.jobTitle}: ${currencyFormat.format(it.proposedBudget)}"
        }}
        
        Efficiency Metrics:
        Application to Hire Rate: ${if (allApplications.isNotEmpty()) "${(hiredApplications.size * 100 / allApplications.size)}%" else "0%"}
        Average Review Time: ${if (hiredApplications.isNotEmpty()) "Active" else "N/A"}
        Project Completion: ${if (hiredApplications.isNotEmpty()) "Ongoing" else "No projects"}
    """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Detailed Spending Analysis")
            .setMessage(breakdownInfo)
            .setPositiveButton("Take Action") { dialog, which ->
                startActivity(Intent(this, ApplicationsManagementActivity::class.java))
            }
            .setNegativeButton("Close", null)
            .show()
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
        loadDashboardData()
    }
}
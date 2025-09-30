package student.projects.tholagig.jobs

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import student.projects.tholagig.R
import student.projects.tholagig.Adapters.JobsAdapter
import student.projects.tholagig.models.Job
import student.projects.tholagig.network.FirebaseService
import student.projects.tholagig.network.SessionManager
import java.util.*

class MyJobsActivity : AppCompatActivity() {

    private lateinit var rvJobs: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var fabCreateJob: FloatingActionButton

    private lateinit var sessionManager: SessionManager
    private lateinit var firebaseService: FirebaseService
    private lateinit var jobsAdapter: JobsAdapter

    private val jobsList = mutableListOf<Job>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_jobs)

        sessionManager = SessionManager(this)
        firebaseService = FirebaseService()

        initializeViews()
        setupRecyclerView()
        setupClickListeners()
        loadJobs()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Jobs"
    }

    private fun initializeViews() {
        rvJobs = findViewById(R.id.rvJobs)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        progressBar = findViewById(R.id.progressBar)
        fabCreateJob = findViewById(R.id.fabCreateJob)
    }

    private fun setupRecyclerView() {
        jobsAdapter = JobsAdapter(jobsList) { job ->  // Only 2 arguments: jobsList and lambda
            openJobDetails(job)
        }
        rvJobs.apply {
            layoutManager = LinearLayoutManager(this@MyJobsActivity)
            adapter = jobsAdapter
        }
    }

    private fun setupClickListeners() {
        fabCreateJob.setOnClickListener {
            startActivity(Intent(this, CreateJobActivity::class.java))
        }
    }

    private fun loadJobs() {
        progressBar.visibility = View.VISIBLE
        val clientId = sessionManager.getUserId() ?: ""

        if (clientId.isEmpty()) {
            progressBar.visibility = View.GONE
            showEmptyState("Please login to view your jobs")
            return
        }

        // For now, load mock data. Replace with actual Firebase call
        loadMockJobs()
    }

    private fun loadMockJobs() {
        jobsList.clear()
        jobsList.addAll(
            listOf(
                Job(
                    jobId = "job_1",
                    clientId = "client_123",
                    clientName = "Your Company",
                    title = "Mobile App Developer",
                    description = "Need an experienced mobile developer to create a cross-platform application for our business. The app should include user authentication, real-time messaging, and payment integration.",
                    category = "Development",
                    skillsRequired = listOf("Kotlin", "Android", "Firebase", "REST API"),
                    budget = 15000.0,
                    deadline = Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000), // 30 days from now
                    location = "Johannesburg",
                    status = "open",
                    postedAt = Date()
                ),
                Job(
                    jobId = "job_2",
                    clientId = "client_123",
                    clientName = "Your Company",
                    title = "UI/UX Designer",
                    description = "Looking for a creative UI/UX designer to redesign our mobile application interface. Focus on improving user experience and modernizing the visual design.",
                    category = "Design",
                    skillsRequired = listOf("Figma", "Adobe XD", "User Research", "Wireframing"),
                    budget = 12000.0,
                    deadline = Date(System.currentTimeMillis() + 20L * 24 * 60 * 60 * 1000), // 20 days from now
                    location = "Cape Town",
                    status = "open",
                    postedAt = Date(System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000) // 2 days ago
                ),
                Job(
                    jobId = "job_3",
                    clientId = "client_123",
                    clientName = "Your Company",
                    title = "Backend Developer",
                    description = "Need a backend developer to build RESTful APIs and database architecture for our new web application.",
                    category = "Development",
                    skillsRequired = listOf("Node.js", "MongoDB", "Express.js", "AWS"),
                    budget = 20000.0,
                    deadline = Date(System.currentTimeMillis() + 45L * 24 * 60 * 60 * 1000), // 45 days from now
                    location = "Remote",
                    status = "in_progress",
                    postedAt = Date(System.currentTimeMillis() - 5 * 24 * 60 * 60 * 1000) // 5 days ago
                ),
                Job(
                    jobId = "job_4",
                    clientId = "client_123",
                    clientName = "Your Company",
                    title = "Social Media Manager",
                    description = "Looking for a social media manager to handle our company's online presence across multiple platforms.",
                    category = "Marketing",
                    skillsRequired = listOf("Social Media", "Content Creation", "Analytics"),
                    budget = 8000.0,
                    deadline = Date(System.currentTimeMillis() - 10L * 24 * 60 * 60 * 1000), // 10 days ago (past deadline)
                    location = "Remote",
                    status = "completed",
                    postedAt = Date(System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000) // 30 days ago
                )
            )
        )

        progressBar.visibility = View.GONE
        updateUI()
    }

    private fun updateUI() {
        if (jobsList.isEmpty()) {
            showEmptyState("You haven't posted any jobs yet")
        } else {
            hideEmptyState()
        }
        jobsAdapter.notifyDataSetChanged()
    }

    private fun showEmptyState(message: String) {
        tvEmptyState.text = message
        tvEmptyState.visibility = View.VISIBLE
        rvJobs.visibility = View.GONE
    }

    private fun hideEmptyState() {
        tvEmptyState.visibility = View.GONE
        rvJobs.visibility = View.VISIBLE
    }

    private fun openJobDetails(job: Job) {
        val intent = Intent(this, JobDetailsActivity::class.java).apply {
            putExtra("JOB_ID", job.jobId)
            putExtra("IS_OWNER", true) // Client owns this job
        }
        startActivity(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh jobs when returning from CreateJobActivity
        loadJobs()
    }
}
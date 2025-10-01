package student.projects.tholagig.jobs

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.*
import student.projects.tholagig.R
import student.projects.tholagig.Adapters.JobsAdapter
import student.projects.tholagig.models.Job
import student.projects.tholagig.network.FirebaseService
import student.projects.tholagig.network.SessionManager

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
        jobsAdapter = JobsAdapter(jobsList) { job ->
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

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = firebaseService.getJobs()

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE

                    if (result.isSuccess) {
                        val allJobs = result.getOrNull() ?: emptyList()
                        // Filter jobs for this client
                        jobsList.clear()
                        jobsList.addAll(allJobs.filter { it.clientId == clientId })
                        updateUI()
                    } else {
                        showEmptyState("Failed to load jobs")
                        Toast.makeText(
                            this@MyJobsActivity,
                            "Error: ${result.exceptionOrNull()?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    showEmptyState("Error loading jobs")
                    Toast.makeText(
                        this@MyJobsActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
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
            putExtra("IS_OWNER", true)
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
        loadJobs() // Refresh when returning
    }
}
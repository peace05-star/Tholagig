package student.projects.tholagig.jobs

import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.*
import student.projects.tholagig.R
import student.projects.tholagig.Adapters.JobsAdapter
import student.projects.tholagig.models.Job
import student.projects.tholagig.network.FirebaseService
import android.widget.SearchView
import android.content.Intent
import android.view.View
import android.widget.Toast
import com.google.android.material.bottomnavigation.BottomNavigationView
import student.projects.tholagig.dashboards.FreelancerDashboardActivity
import student.projects.tholagig.jobs.MyApplicationsActivity
import student.projects.tholagig.profile.ProfileActivity

class JobBrowseActivity : AppCompatActivity() {

    private lateinit var rvJobs: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var chipGroup: ChipGroup
    private lateinit var btnSort: MaterialButton
    private lateinit var btnFilter: MaterialButton
    private lateinit var tvJobsCount: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var bottomNavigationView: BottomNavigationView // ADD THIS

    private lateinit var jobsAdapter: JobsAdapter
    private val allJobs = mutableListOf<Job>()
    private val filteredJobs = mutableListOf<Job>()

    private var currentSort = "newest" // newest, highest_budget, deadline
    private val selectedCategories = mutableSetOf<String>()
    private var searchQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_job_browse)

        initializeViews()
        setupRecyclerView()
        setupClickListeners()
        setupBottomNavigation() // ADD THIS
        loadJobsFromFirebase()
    }

    private fun initializeViews() {
        rvJobs = findViewById(R.id.rvJobs)
        searchView = findViewById(R.id.searchView)
        chipGroup = findViewById(R.id.chipGroup)
        btnSort = findViewById(R.id.btnSort)
        btnFilter = findViewById(R.id.btnFilter)
        tvJobsCount = findViewById(R.id.tvJobsCount)
        progressBar = findViewById(R.id.progressBar)
        bottomNavigationView = findViewById(R.id.bottom_navigation) // ADD THIS
    }

    // ADD THIS METHOD
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
                        // Already on jobs page
                        true
                    }
                    R.id.nav_applications -> {
                        val intent = Intent(this, MyApplicationsActivity::class.java)
                        startActivity(intent)
                        finish()
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

            // Set the jobs item as selected
            bottomNavigationView.selectedItemId = R.id.nav_jobs

        } catch (e: Exception) {
            Log.e("JobBrowseActivity", "Bottom navigation setup failed: ${e.message}")
        }
    }

    private fun setupRecyclerView() {
        jobsAdapter = JobsAdapter(filteredJobs) { job ->
            onJobItemClick(job)
        }
        rvJobs.apply {
            layoutManager = LinearLayoutManager(this@JobBrowseActivity)
            adapter = jobsAdapter
        }
    }

    private fun setupClickListeners() {
        // Search functionality
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                searchQuery = newText ?: ""
                applyFilters()
                return true
            }
        })

        // Category chips
        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            selectedCategories.clear()
            checkedIds.forEach { chipId ->
                when (chipId) {
                    R.id.chipRemote -> selectedCategories.add("Remote")
                    R.id.chipDesign -> selectedCategories.add("Design")
                    R.id.chipDevelopment -> selectedCategories.add("Development")
                    R.id.chipMarketing -> selectedCategories.add("Marketing")
                }
            }
            applyFilters()
        }

        // Sort button
        btnSort.setOnClickListener {
            showSortOptions()
        }

        // Filter button (for more advanced filters)
        btnFilter.setOnClickListener {
            Toast.makeText(this, "Advanced filters coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadJobsFromFirebase() {
        progressBar.visibility = View.VISIBLE
        tvJobsCount.text = "Loading jobs..."

        val firebaseService = FirebaseService()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = firebaseService.getJobs()

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE

                    if (result.isSuccess) {
                        allJobs.clear()
                        allJobs.addAll(result.getOrNull() ?: emptyList())
                        applyFilters()
                        updateJobsCount()
                    } else {
                        Toast.makeText(
                            this@JobBrowseActivity,
                            "Failed to load jobs: ${result.exceptionOrNull()?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        tvJobsCount.text = "Failed to load jobs"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@JobBrowseActivity,
                        "Error loading jobs: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    tvJobsCount.text = "Error loading jobs"
                }
            }
        }
    }

    private fun applyFilters() {
        filteredJobs.clear()

        // Apply search filter
        val searchFiltered = if (searchQuery.isNotEmpty()) {
            allJobs.filter { job ->
                job.title.contains(searchQuery, true) ||
                        job.description.contains(searchQuery, true) ||
                        job.skillsRequired.any { skill -> skill.contains(searchQuery, true) } ||
                        job.category.contains(searchQuery, true)
            }
        } else {
            allJobs
        }

        // Apply category filters
        val categoryFiltered = if (selectedCategories.isNotEmpty()) {
            searchFiltered.filter { job ->
                selectedCategories.any { category ->
                    job.category.contains(category, true) ||
                            job.location.contains(category, true)
                }
            }
        } else {
            searchFiltered
        }

        // Apply sorting
        val sortedJobs = when (currentSort) {
            "highest_budget" -> categoryFiltered.sortedByDescending { it.budget }
            "deadline" -> categoryFiltered.sortedBy { it.deadline }
            else -> categoryFiltered.sortedByDescending { it.postedAt } // newest first
        }

        filteredJobs.addAll(sortedJobs)
        jobsAdapter.notifyDataSetChanged()
        updateJobsCount()
    }

    private fun updateJobsCount() {
        val count = filteredJobs.size
        tvJobsCount.text = when {
            count == 0 -> "No jobs found"
            count == 1 -> "1 job found"
            else -> "$count jobs found"
        }
    }

    private fun showSortOptions() {
        val sortOptions = arrayOf("Newest First", "Highest Budget", "Closest Deadline")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Sort by")
            .setItems(sortOptions) { dialog, which ->
                when (which) {
                    0 -> {
                        currentSort = "newest"
                        btnSort.text = "Sort: Newest"
                    }
                    1 -> {
                        currentSort = "highest_budget"
                        btnSort.text = "Sort: Highest Budget"
                    }
                    2 -> {
                        currentSort = "deadline"
                        btnSort.text = "Sort: Deadline"
                    }
                }
                applyFilters()
            }
            .show()
    }

    private fun onJobItemClick(job: Job) {
        val intent = Intent(this, JobDetailsActivity::class.java).apply {
            putExtra("JOB_ID", job.jobId)
            putExtra("JOB_TITLE", job.title)
        }
        startActivity(intent)
    }
}
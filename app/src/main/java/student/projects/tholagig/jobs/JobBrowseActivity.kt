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
import student.projects.tholagig.profile.ProfileActivity

class JobBrowseActivity : AppCompatActivity() {

    private lateinit var rvJobs: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var chipGroup: ChipGroup
    private lateinit var btnSort: MaterialButton
    private lateinit var btnFilter: MaterialButton
    private lateinit var tvJobsCount: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: TextView

    private lateinit var jobsAdapter: JobsAdapter
    private val allJobs = mutableListOf<Job>()
    private val filteredJobs = mutableListOf<Job>()

    private lateinit var bottomNavigationView: BottomNavigationView

    private lateinit var firebaseService: FirebaseService

    private var currentSort = "newest" // newest, highest_budget, deadline
    private val selectedCategories = mutableSetOf<String>()
    private var searchQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_job_browse)

        firebaseService = FirebaseService()
        initializeViews()
        setupBottomNavigation()
        setupRecyclerView()
        setupClickListeners()
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
        tvEmptyState = findViewById(R.id.tvEmptyState)
        bottomNavigationView = findViewById(R.id.bottom_navigation) // ADD THIS
        // REMOVE THIS LINE: setupCategoryChips()
        // Or keep it but make the function empty
    }

    // Either remove this function completely OR make it empty:
    private fun setupCategoryChips() {
        // EMPTY - chips are already in XML!
    }

    private fun setupBottomNavigation() {
        try {
            bottomNavigationView.setOnNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home -> {
                        // Navigate to home dashboard
                        val intent = Intent(this, student.projects.tholagig.dashboards.FreelancerDashboardActivity::class.java)
                        startActivity(intent)
                        finish()
                        true
                    }
                    R.id.nav_jobs -> {
                        // Already on jobs page
                        true
                    }
                    R.id.nav_applications -> {
                        // Navigate to applications
                        val intent = Intent(this, MyApplicationsActivity::class.java)
                        startActivity(intent)
                        finish()
                        true
                    }
                    R.id.nav_profile -> {
                        // Navigate to profile
                        val intent = Intent(this, ProfileActivity::class.java)
                        startActivity(intent)
                        finish()
                        true
                    }
                    else -> false
                }
            }

            // Set the jobs item as selected (since we're on jobs page)
            bottomNavigationView.selectedItemId = R.id.nav_jobs

        } catch (e: Exception) {
            Log.e("BottomNav", "Bottom navigation setup failed: ${e.message}")
            Toast.makeText(this, "Navigation error", Toast.LENGTH_SHORT).show()
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
                val chip = group.findViewById<Chip>(chipId)
                chip?.text?.toString()?.let { category ->
                    selectedCategories.add(category)
                }
            }
            applyFilters()
        }

        // Sort button
        btnSort.setOnClickListener {
            showSortOptions()
        }

        // Filter button
        btnFilter.setOnClickListener {
            showAdvancedFilters()
        }

        // Back button
        findViewById<View>(R.id.btnBack).setOnClickListener {
            onBackPressed()
        }
    }

    private fun loadJobsFromFirebase() {
        progressBar.visibility = View.VISIBLE
        tvJobsCount.text = "Loading jobs..."
        tvEmptyState.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = firebaseService.getJobs()

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE

                    if (result.isSuccess) {
                        val jobs = result.getOrNull() ?: emptyList()
                        allJobs.clear()
                        allJobs.addAll(jobs)
                        applyFilters()
                        updateEmptyState()

                        Log.d("JobBrowse", "Loaded ${jobs.size} jobs from Firebase")
                    } else {
                        Toast.makeText(
                            this@JobBrowseActivity,
                            "Failed to load jobs: ${result.exceptionOrNull()?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        tvJobsCount.text = "Failed to load jobs"
                        updateEmptyState()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@JobBrowseActivity,
                        "Error loading jobs: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    tvJobsCount.text = "Error loading jobs"
                    updateEmptyState()
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
                        job.category.contains(searchQuery, true) ||
                        job.company?.contains(searchQuery, true) == true
            }
        } else {
            allJobs
        }

        // Apply category filters
        val categoryFiltered = if (selectedCategories.isNotEmpty()) {
            searchFiltered.filter { job ->
                selectedCategories.any { category ->
                    job.category.contains(category, true) ||
                            job.location.contains(category, true) ||
                            (category == "Remote" && job.location.contains("remote", true))
                }
            }
        } else {
            searchFiltered
        }

        // Apply sorting
        val sortedJobs = when (currentSort) {
            "highest_budget" -> categoryFiltered.sortedByDescending { it.budget }
            "deadline" -> categoryFiltered.sortedBy { it.deadline }
            "lowest_budget" -> categoryFiltered.sortedBy { it.budget }
            else -> categoryFiltered.sortedByDescending { it.postedAt } // newest first
        }

        filteredJobs.addAll(sortedJobs)
        jobsAdapter.notifyDataSetChanged()
        updateJobsCount()
        updateEmptyState()
    }

    private fun updateJobsCount() {
        val count = filteredJobs.size
        tvJobsCount.text = when {
            count == 0 -> "No jobs found"
            count == 1 -> "1 job found"
            else -> "$count jobs found"
        }
    }

    private fun updateEmptyState() {
        if (filteredJobs.isEmpty()) {
            tvEmptyState.visibility = View.VISIBLE
            if (allJobs.isEmpty()) {
                tvEmptyState.text = "No jobs available at the moment"
            } else {
                tvEmptyState.text = "No jobs match your filters\nTry adjusting your search or filters"
            }
        } else {
            tvEmptyState.visibility = View.GONE
        }
    }

    private fun showSortOptions() {
        val sortOptions = arrayOf("Newest First", "Highest Budget", "Lowest Budget", "Closest Deadline")

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
                        currentSort = "lowest_budget"
                        btnSort.text = "Sort: Lowest Budget"
                    }
                    3 -> {
                        currentSort = "deadline"
                        btnSort.text = "Sort: Deadline"
                    }
                }
                applyFilters()
            }
            .show()
    }

    private fun showAdvancedFilters() {
        // You can implement more advanced filters here
        // For now, show a simple message
        Toast.makeText(this, "Advanced filters coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun onJobItemClick(job: Job) {
        val intent = Intent(this, JobDetailsActivity::class.java).apply {
            putExtra("JOB_ID", job.jobId)
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // Refresh jobs when returning to this activity
        loadJobsFromFirebase()
    }
}
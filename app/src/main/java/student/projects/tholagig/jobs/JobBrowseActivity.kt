package student.projects.tholagig.jobs

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.*
import student.projects.tholagig.Adapters.JobsAdapter
import student.projects.tholagig.R
import student.projects.tholagig.models.Job
import student.projects.tholagig.network.FirebaseService
import student.projects.tholagig.profile.ProfileActivity
import student.projects.tholagig.messaging.MessagesActivity
import student.projects.tholagig.database.AppDatabase
import student.projects.tholagig.messaging.ConversationsActivity
import student.projects.tholagig.repository.JobRepository

class JobBrowseActivity : AppCompatActivity() {

    private lateinit var rvJobs: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var chipGroup: ChipGroup
    private lateinit var btnSort: MaterialButton
    private lateinit var btnFilter: MaterialButton
    private lateinit var tvJobsCount: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: TextView
    private lateinit var bottomNavigationView: BottomNavigationView

    private lateinit var jobsAdapter: JobsAdapter
    private val allJobs = mutableListOf<Job>()
    private val filteredJobs = mutableListOf<Job>()

    private lateinit var firebaseService: FirebaseService
    private lateinit var jobRepository: JobRepository
    private var currentSort = "newest"
    private val selectedCategories = mutableSetOf<String>()
    private var searchQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_job_browse)

        firebaseService = FirebaseService()
        // Initialize offline repository
        val database = AppDatabase.getInstance(this)
        jobRepository = JobRepository(database)

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
        bottomNavigationView = findViewById(R.id.bottom_navigation)
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Handle home navigation
                    true
                }
                R.id.nav_jobs -> {
                    // Already here
                    true
                }
                R.id.nav_applications -> {
                    // Handle applications navigation
                    true
                }
                R.id.nav_messages -> {
                    startActivity(Intent(this, ConversationsActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
        bottomNavigationView.selectedItemId = R.id.nav_jobs
    }

    private fun setupRecyclerView() {
        jobsAdapter = JobsAdapter(filteredJobs) { job ->
            onJobItemClick(job)
        }
        rvJobs.layoutManager = LinearLayoutManager(this)
        rvJobs.adapter = jobsAdapter
    }

    private fun setupClickListeners() {
        btnSort.setOnClickListener {
            showSortOptions()
        }

        btnFilter.setOnClickListener {
            showAdvancedFilters()
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                searchQuery = query
                applyFilters()
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                searchQuery = newText
                applyFilters()
                return true
            }
        })
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
                        allJobs.clear()
                        val jobsFromFirebase = result.getOrNull() ?: emptyList()
                        allJobs.addAll(jobsFromFirebase)

                        // Save jobs to local database for offline use
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                jobRepository.createJobs(jobsFromFirebase)
                                Log.d("JobBrowse", "✅ Saved ${jobsFromFirebase.size} jobs to local database")
                            } catch (e: Exception) {
                                Log.e("JobBrowse", "❌ Error saving to local database: ${e.message}")
                            }
                        }

                        applyFilters()
                    } else {
                        // If Firebase fails, try loading from local database
                        loadJobsFromLocalDatabase()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    // If any error occurs, try loading from local database
                    loadJobsFromLocalDatabase()
                }
            }
        }
    }

    // Function to load jobs from local database
    private fun loadJobsFromLocalDatabase() {
        progressBar.visibility = View.VISIBLE
        tvJobsCount.text = "Loading cached jobs..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val localJobs = jobRepository.getAllJobs()
                localJobs.collect { jobs ->
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE

                        if (jobs.isNotEmpty()) {
                            allJobs.clear()
                            allJobs.addAll(jobs)
                            applyFilters()

                            // Show offline indicator
                            tvJobsCount.text = "${jobs.size} cached jobs (Offline)"
                            Toast.makeText(
                                this@JobBrowseActivity,
                                "📱 Showing cached jobs - You're offline",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            // No jobs in cache either
                            tvJobsCount.text = "Failed to load jobs"
                            tvEmptyState.visibility = View.VISIBLE
                            tvEmptyState.text = "No internet connection and no cached jobs available"
                            Toast.makeText(
                                this@JobBrowseActivity,
                                "❌ No internet connection",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    tvJobsCount.text = "Error loading jobs"
                    Toast.makeText(this@JobBrowseActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun applyFilters() {
        filteredJobs.clear()

        // Apply search filter
        val searchFiltered = if (searchQuery.isNotEmpty()) {
            allJobs.filter { job ->
                job.title.contains(searchQuery, ignoreCase = true) ||
                        job.description.contains(searchQuery, ignoreCase = true) ||
                        job.category.contains(searchQuery, ignoreCase = true) ||
                        job.skillsRequired.any { it.contains(searchQuery, ignoreCase = true) }
            }
        } else {
            allJobs
        }

        // Apply category filter
        val categoryFiltered = if (selectedCategories.isNotEmpty()) {
            searchFiltered.filter { job ->
                selectedCategories.contains(job.category)
            }
        } else {
            searchFiltered
        }

        // Apply sorting
        val sortedJobs = when (currentSort) {
            "newest" -> categoryFiltered.sortedByDescending { it.postedAt ?: it.createdDate }
            "oldest" -> categoryFiltered.sortedBy { it.postedAt ?: it.createdDate }
            "budget_high" -> categoryFiltered.sortedByDescending { it.budget }
            "budget_low" -> categoryFiltered.sortedBy { it.budget }
            else -> categoryFiltered
        }

        filteredJobs.addAll(sortedJobs)
        jobsAdapter.notifyDataSetChanged()

        // Update UI
        tvJobsCount.text = "${filteredJobs.size} jobs found"
        tvEmptyState.visibility = if (filteredJobs.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showSortOptions() {
        // Implement your sort dialog/popup
        val sorts = arrayOf("Newest First", "Oldest First", "Budget: High to Low", "Budget: Low to High")
        val alertDialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Sort Jobs")
            .setItems(sorts) { dialog, which ->
                currentSort = when (which) {
                    0 -> "newest"
                    1 -> "oldest"
                    2 -> "budget_high"
                    3 -> "budget_low"
                    else -> "newest"
                }
                applyFilters()
                dialog.dismiss()
            }
            .create()
        alertDialog.show()
    }

    private fun showAdvancedFilters() {
        // Implement your filter dialog
        // This would show categories to select from
        Toast.makeText(this, "Filter feature coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun onJobItemClick(job: Job) {
        // Handle job item click - open job details
        val intent = Intent(this, JobDetailsActivity::class.java)
        intent.putExtra("JOB_ID", job.jobId)
        startActivity(intent)
    }
}
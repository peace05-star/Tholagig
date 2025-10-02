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
    private var currentSort = "newest"
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
        bottomNavigationView = findViewById(R.id.bottom_navigation)
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, student.projects.tholagig.dashboards.FreelancerDashboardActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_jobs -> true // Already here
                R.id.nav_applications -> {
                    startActivity(Intent(this, MyApplicationsActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_messages -> {
                    startActivity(Intent(this, MessagesActivity::class.java))
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
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                searchQuery = newText ?: ""
                applyFilters()
                return true
            }
        })

        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            selectedCategories.clear()
            checkedIds.forEach { chipId ->
                val chip = group.findViewById<Chip>(chipId)
                chip?.text?.toString()?.let { selectedCategories.add(it) }
            }
            applyFilters()
        }

        btnSort.setOnClickListener { showSortOptions() }
        btnFilter.setOnClickListener { showAdvancedFilters() }
        findViewById<View>(R.id.btnBack).setOnClickListener { onBackPressed() }
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
                        allJobs.addAll(result.getOrNull() ?: emptyList())
                        applyFilters()
                    } else {
                        tvJobsCount.text = "Failed to load jobs"
                        Toast.makeText(this@JobBrowseActivity, "Failed to load jobs", Toast.LENGTH_LONG).show()
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
        val searchFiltered = if (searchQuery.isNotEmpty()) {
            allJobs.filter { job ->
                job.title.contains(searchQuery, true) ||
                        job.description.contains(searchQuery, true) ||
                        job.skillsRequired.any { it.contains(searchQuery, true) } ||
                        job.category.contains(searchQuery, true)
            }
        } else allJobs

        val categoryFiltered = if (selectedCategories.isNotEmpty()) {
            searchFiltered.filter { job ->
                selectedCategories.any { category ->
                    job.category.contains(category, true) ||
                            job.location.contains(category, true) ||
                            (category == "Remote" && job.location.contains("remote", true))
                }
            }
        } else searchFiltered

        val sortedJobs = when (currentSort) {
            "highest_budget" -> categoryFiltered.sortedByDescending { it.budget }
            "lowest_budget" -> categoryFiltered.sortedBy { it.budget }
            "deadline" -> categoryFiltered.sortedBy { it.deadline }
            else -> categoryFiltered.sortedByDescending { it.postedAt }
        }

        filteredJobs.clear()
        filteredJobs.addAll(sortedJobs)
        jobsAdapter.notifyDataSetChanged()

        tvJobsCount.text = when (filteredJobs.size) {
            0 -> "No jobs found"
            1 -> "1 job found"
            else -> "${filteredJobs.size} jobs found"
        }

        tvEmptyState.visibility = if (filteredJobs.isEmpty()) View.VISIBLE else View.GONE
        tvEmptyState.text = if (allJobs.isEmpty()) "No jobs available" else "No jobs match your filters"
    }

    private fun showSortOptions() {
        val options = arrayOf("Newest", "Highest Budget", "Lowest Budget", "Closest Deadline")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Sort by")
            .setItems(options) { _, which ->
                currentSort = when (which) {
                    1 -> "highest_budget"
                    2 -> "lowest_budget"
                    3 -> "deadline"
                    else -> "newest"
                }
                btnSort.text = "Sort: ${options[which]}"
                applyFilters()
            }.show()
    }

    private fun showAdvancedFilters() {
        Toast.makeText(this, "Advanced filters coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun onJobItemClick(job: Job) {
        val intent = Intent(this, JobDetailsActivity::class.java)
        intent.putExtra("JOB_ID", job.jobId)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        loadJobsFromFirebase()
    }
}

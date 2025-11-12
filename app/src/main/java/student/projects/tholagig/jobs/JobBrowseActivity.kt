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
    private lateinit var jobRepository: JobRepository // NEW: Offline repository
    private var currentSort = "newest"
    private val selectedCategories = mutableSetOf<String>()
    private var searchQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_job_browse)

        firebaseService = FirebaseService()
        // NEW: Initialize offline repository
        val database = AppDatabase.getInstance(this)
        jobRepository = JobRepository(database)

        initializeViews()
        setupBottomNavigation()
        setupRecyclerView()
        setupClickListeners()
        loadJobsFromFirebase()
    }

    // ... REST OF YOUR EXISTING CODE STAYS EXACTLY THE SAME ...

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

                        // NEW: Save jobs to local database for offline use
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                jobsFromFirebase.forEach { job ->
                                    jobRepository.createJob(job)
                                }
                                Log.d("JobBrowse", "Saved ${jobsFromFirebase.size} jobs to local database")
                            } catch (e: Exception) {
                                Log.e("JobBrowse", "Error saving to local database: ${e.message}")
                            }
                        }

                        applyFilters()
                    } else {
                        // NEW: If Firebase fails, try loading from local database
                        loadJobsFromLocalDatabase()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    // NEW: If any error occurs, try loading from local database
                    loadJobsFromLocalDatabase()
                }
            }
        }
    }

    // NEW: Function to load jobs from local database
    private fun loadJobsFromLocalDatabase() {
        progressBar.visibility = View.VISIBLE
        tvJobsCount.text = "Loading cached jobs..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val localJobs = jobRepository.getAllJobs().first()
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE

                    if (localJobs.isNotEmpty()) {
                        allJobs.clear()
                        allJobs.addAll(localJobs)
                        applyFilters()

                        // Show offline indicator
                        tvJobsCount.text = "${localJobs.size} cached jobs (Offline)"
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
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    tvJobsCount.text = "Error loading jobs"
                    Toast.makeText(this@JobBrowseActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // ... REST OF YOUR EXISTING CODE STAYS EXACTLY THE SAME ...
    // (applyFilters, showSortOptions, showAdvancedFilters, onJobItemClick, etc.)
}
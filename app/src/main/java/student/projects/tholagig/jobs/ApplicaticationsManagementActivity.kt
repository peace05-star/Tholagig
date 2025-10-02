package student.projects.tholagig.jobs

import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.*
import student.projects.tholagig.R
import student.projects.tholagig.Adapters.ApplicationsManagementAdapter
import student.projects.tholagig.models.JobApplication
import student.projects.tholagig.network.FirebaseService
import student.projects.tholagig.network.SessionManager

class ApplicationsManagementActivity : AppCompatActivity() {

    private lateinit var rvApplications: RecyclerView
    private lateinit var chipGroupStatus: ChipGroup
    private lateinit var tvApplicationsCount: TextView
    private lateinit var tvEmptyState: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var headerCard: MaterialCardView
    private lateinit var emptyStateLayout: LinearLayout

    private lateinit var sessionManager: SessionManager
    private lateinit var firebaseService: FirebaseService
    private lateinit var applicationsAdapter: ApplicationsManagementAdapter

    private val allApplications = mutableListOf<JobApplication>()
    private val filteredApplications = mutableListOf<JobApplication>()
    private val selectedStatuses = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_applications_management)

        sessionManager = SessionManager(this)
        firebaseService = FirebaseService()

        initializeViews()
        setupRecyclerView()
        setupClickListeners()
        loadApplications()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Manage Applications"
    }

    private fun initializeViews() {
        rvApplications = findViewById(R.id.rvApplications)
        chipGroupStatus = findViewById(R.id.chipGroupStatus)
        tvApplicationsCount = findViewById(R.id.tvApplicationsCount)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        progressBar = findViewById(R.id.progressBar)
        headerCard = findViewById(R.id.headerCard)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
    }

    private fun setupRecyclerView() {
        applicationsAdapter = ApplicationsManagementAdapter(
            applications = filteredApplications,
            onViewProfile = { application ->
            },
            onAccept = { application ->
                acceptApplication(application)
            },
            onReject = { application ->
                rejectApplication(application)
            },
            onMessage = { application ->
                messageFreelancer(application)
            }
        )
        rvApplications.apply {
            layoutManager = LinearLayoutManager(this@ApplicationsManagementActivity)
            adapter = applicationsAdapter
            addItemDecoration(SimpleItemDecoration(resources.getDimensionPixelSize(R.dimen.card_spacing)))
        }
    }

    private fun setupClickListeners() {
        chipGroupStatus.setOnCheckedStateChangeListener { group, checkedIds ->
            selectedStatuses.clear()
            checkedIds.forEach { chipId ->
                when (chipId) {
                    R.id.chipAll -> selectedStatuses.add("all")
                    R.id.chipPending -> selectedStatuses.add("pending")
                    R.id.chipAccepted -> selectedStatuses.add("accepted")
                    R.id.chipRejected -> selectedStatuses.add("rejected")
                }
            }
            applyFilters()
        }

        findViewById<Chip>(R.id.chipAll).isChecked = true
    }

    private fun loadApplications() {
        progressBar.visibility = View.VISIBLE
        tvApplicationsCount.text = "Loading applications..."

        val clientId = sessionManager.getUserId() ?: ""
        if (clientId.isEmpty()) {
            progressBar.visibility = View.GONE
            showEmptyState("Please login to view applications")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = firebaseService.getApplicationsByClient(clientId)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE

                    if (result.isSuccess) {
                        val applications = result.getOrNull() ?: emptyList()
                        allApplications.clear()
                        allApplications.addAll(applications)
                        applyFilters()
                        updateApplicationsCount()

                        if (applications.isEmpty()) {
                            showEmptyState("No applications received yet\nPost jobs to get applications!")
                        }
                    } else {
                        showEmptyState("Error loading applications\nPull down to refresh")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    showEmptyState("Connection error\nPull down to refresh")
                }
            }
        }
    }

    private fun applyFilters() {
        filteredApplications.clear()

        val filtered = if (selectedStatuses.isEmpty() || selectedStatuses.contains("all")) {
            allApplications
        } else {
            allApplications.filter { application ->
                selectedStatuses.any { status ->
                    application.status.equals(status, true)
                }
            }
        }

        filteredApplications.addAll(filtered.sortedByDescending { it.appliedAt })
        applicationsAdapter.notifyDataSetChanged()
        updateApplicationsCount()
    }

    private fun updateApplicationsCount() {
        val totalCount = allApplications.size
        val filteredCount = filteredApplications.size

        tvApplicationsCount.text = when {
            totalCount == 0 -> "📥 No applications yet"
            filteredCount == totalCount -> "📥 $filteredCount application${if (filteredCount != 1) "s" else ""}"
            else -> "📥 $filteredCount of $totalCount application${if (totalCount != 1) "s" else ""}"
        }

        if (filteredApplications.isEmpty()) {
            val message = when {
                selectedStatuses.contains("pending") -> "No pending applications"
                selectedStatuses.contains("accepted") -> "No hired freelancers"
                selectedStatuses.contains("rejected") -> "No rejected applications"
                allApplications.isEmpty() -> "No applications received yet\nPost jobs to get applications!"
                else -> "No applications match your filters"
            }
            showEmptyState(message)
        } else {
            hideEmptyState()
        }
    }

    private fun showEmptyState(message: String) {
        tvEmptyState.text = message
        emptyStateLayout.visibility = View.VISIBLE
        rvApplications.visibility = View.GONE
    }

    private fun hideEmptyState() {
        emptyStateLayout.visibility = View.GONE
        rvApplications.visibility = View.VISIBLE
    }

    private fun viewFreelancerProfile(application: JobApplication) {
        Toast.makeText(this, "👤 Viewing ${application.freelancerName}'s profile", Toast.LENGTH_SHORT).show()
    }

    private fun acceptApplication(application: JobApplication) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("🎉 Hire ${application.freelancerName}?")
            .setMessage("Are you sure you want to hire ${application.freelancerName} for \"${application.jobTitle}\"?")
            .setPositiveButton("Yes, Hire!") { dialog, which ->
                performAcceptance(application)
            }
            .setNegativeButton("Not Now", null)
            .show()
    }

    private fun performAcceptance(application: JobApplication) {
        progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = firebaseService.updateApplicationStatus(application.applicationId, "accepted")

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE

                    if (result.isSuccess) {
                        val index = allApplications.indexOfFirst { it.applicationId == application.applicationId }
                        if (index != -1) {
                            allApplications[index] = application.copy(status = "accepted")
                            applyFilters()
                        }
                        Toast.makeText(this@ApplicationsManagementActivity, "🎉 You hired ${application.freelancerName}!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@ApplicationsManagementActivity, "❌ Failed to accept application", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@ApplicationsManagementActivity, "❌ Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun rejectApplication(application: JobApplication) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Reject Application")
            .setMessage("Are you sure you want to reject ${application.freelancerName}'s application for \"${application.jobTitle}\"?")
            .setPositiveButton("Reject") { dialog, which ->
                performRejection(application)
            }
            .setNegativeButton("Keep Pending", null)
            .show()
    }

    private fun performRejection(application: JobApplication) {
        progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = firebaseService.updateApplicationStatus(application.applicationId, "rejected")

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE

                    if (result.isSuccess) {
                        val index = allApplications.indexOfFirst { it.applicationId == application.applicationId }
                        if (index != -1) {
                            allApplications[index] = application.copy(status = "rejected")
                            applyFilters()
                        }
                        Toast.makeText(this@ApplicationsManagementActivity, "Application rejected", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@ApplicationsManagementActivity, "Failed to reject application", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@ApplicationsManagementActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun messageFreelancer(application: JobApplication) {
        Toast.makeText(this, "💬 Messaging ${application.freelancerName}", Toast.LENGTH_SHORT).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        loadApplications()
    }

    // Simple Item Decoration class
    class SimpleItemDecoration(private val spaceHeight: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            outRect.bottom = spaceHeight
        }
    }
}
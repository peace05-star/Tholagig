package student.projects.tholagig.jobs

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import student.projects.tholagig.Adapters.ClientApplicationsAdapter
import student.projects.tholagig.R
import student.projects.tholagig.models.JobApplication
import student.projects.tholagig.network.FirebaseService
import student.projects.tholagig.network.SessionManager

class ClientApplicationsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var sessionManager: SessionManager
    private lateinit var firebaseService: FirebaseService
    private lateinit var applicationsAdapter: ClientApplicationsAdapter

    private val applications = mutableListOf<JobApplication>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_applications)

        sessionManager = SessionManager(this)
        firebaseService = FirebaseService()

        initializeViews()
        setupRecyclerView()
        loadApplications()
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.rvApplications)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            onBackPressed()
        }

        // Optional: Add refresh functionality
        findViewById<ImageButton>(R.id.btnRefresh)?.setOnClickListener {
            loadApplications()
        }
    }

    private fun setupRecyclerView() {
        applicationsAdapter = ClientApplicationsAdapter(applications) { application, action ->
            handleApplicationAction(application, action)
        }
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ClientApplicationsActivity)
            adapter = applicationsAdapter
        }
    }

    private fun loadApplications() {
        progressBar.visibility = View.VISIBLE
        val clientId = sessionManager.getUserId() ?: ""

        if (clientId.isEmpty()) {
            Toast.makeText(this, "Please login as client", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = firebaseService.getApplicationsByClient(clientId)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE

                    if (result.isSuccess) {
                        val applicationsList = result.getOrNull() ?: emptyList()
                        applications.clear()
                        applications.addAll(applicationsList)
                        applicationsAdapter.updateApplications(applicationsList)

                        // Show empty state if no applications
                        tvEmpty.visibility = if (applications.isEmpty()) View.VISIBLE else View.GONE

                        Log.d("ClientApplications", "Loaded ${applications.size} applications")
                    } else {
                        Toast.makeText(this@ClientApplicationsActivity, "Failed to load applications", Toast.LENGTH_SHORT).show()
                        tvEmpty.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@ClientApplicationsActivity, "Error loading applications: ${e.message}", Toast.LENGTH_SHORT).show()
                    tvEmpty.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun handleApplicationAction(application: JobApplication, action: String) {
        when (action) {
            "accept" -> acceptApplication(application)
            "reject" -> rejectApplication(application)
            "view_profile" -> viewFreelancerProfile(application.freelancerId)
            "contact" -> contactFreelancer(application)
        }
    }

    private fun acceptApplication(application: JobApplication) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = firebaseService.updateApplicationStatus(application.applicationId, "accepted")

                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        Toast.makeText(this@ClientApplicationsActivity, "Application accepted! Contact the freelancer to proceed.", Toast.LENGTH_LONG).show()
                        loadApplications() // Refresh the list
                    } else {
                        Toast.makeText(this@ClientApplicationsActivity, "Failed to accept application", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ClientApplicationsActivity, "Error accepting application", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun rejectApplication(application: JobApplication) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = firebaseService.updateApplicationStatus(application.applicationId, "rejected")

                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        Toast.makeText(this@ClientApplicationsActivity, "Application rejected", Toast.LENGTH_SHORT).show()
                        loadApplications() // Refresh the list
                    } else {
                        Toast.makeText(this@ClientApplicationsActivity, "Failed to reject application", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ClientApplicationsActivity, "Error rejecting application", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun viewFreelancerProfile(freelancerId: String) {
        // Navigate to freelancer profile (you can implement this later)
        Toast.makeText(this, "Viewing freelancer profile: $freelancerId", Toast.LENGTH_SHORT).show()

        // You can create a FreelancerProfileActivity later
        // val intent = Intent(this, FreelancerProfileActivity::class.java)
        // intent.putExtra("FREELANCER_ID", freelancerId)
        // startActivity(intent)
    }

    private fun contactFreelancer(application: JobApplication) {
        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(application.freelancerEmail))
            putExtra(Intent.EXTRA_SUBJECT, "Regarding your application for ${application.jobTitle}")
            putExtra(Intent.EXTRA_TEXT,
                "Hi ${application.freelancerName},\n\n" +
                        "I saw your application for the '${application.jobTitle}' position and would like to discuss it further.\n\n" +
                        "Your proposed budget: R ${application.proposedBudget.toInt()}\n" +
                        "Your skills: ${application.freelancerSkills.joinToString(", ")}\n\n" +
                        "Please let me know when you're available for a discussion.\n\n" +
                        "Best regards,\n" +
                        "${application.clientName}"
            )
        }

        try {
            startActivity(Intent.createChooser(emailIntent, "Send email to ${application.freelancerName}"))
        } catch (e: Exception) {
            Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
        }
    }
}
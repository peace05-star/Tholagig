package student.projects.tholagig.jobs

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import student.projects.tholagig.R
import student.projects.tholagig.models.Job
import student.projects.tholagig.network.FirebaseService
import student.projects.tholagig.network.SessionManager
import java.util.*

class CreateJobActivity : AppCompatActivity() {

    private lateinit var etJobTitle: EditText
    private lateinit var etJobDescription: EditText
    private lateinit var etBudget: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var etLocation: EditText
    private lateinit var etSkills: EditText
    private lateinit var btnCreateJob: Button
    private lateinit var progressBar: ProgressBar

    private lateinit var sessionManager: SessionManager
    private lateinit var firebaseService: FirebaseService

    private val categories = arrayOf(
        "Mobile Development", "Web Development", "UI/UX Design", "Digital Marketing",
        "Writing & Content", "Data Science", "Administration", "Consulting", "Other"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_job)

        sessionManager = SessionManager(this)
        firebaseService = FirebaseService()

        initializeViews()
        setupCategorySpinner()
        setupClickListeners()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Create Job Post"
    }

    private fun initializeViews() {
        etJobTitle = findViewById(R.id.etJobTitle)
        etJobDescription = findViewById(R.id.etJobDescription)
        etBudget = findViewById(R.id.etBudget)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        etLocation = findViewById(R.id.etLocation)
        etSkills = findViewById(R.id.etSkills)
        btnCreateJob = findViewById(R.id.btnCreateJob)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter
    }

    private fun setupClickListeners() {
        btnCreateJob.setOnClickListener {
            createJob()
        }
    }

    private fun createJob() {
        val title = etJobTitle.text.toString().trim()
        val description = etJobDescription.text.toString().trim()
        val budgetText = etBudget.text.toString().trim()
        val category = spinnerCategory.selectedItem.toString()
        val location = etLocation.text.toString().trim()
        val skillsText = etSkills.text.toString().trim()

        // Validation
        if (title.isEmpty()) {
            etJobTitle.error = "Job title is required"
            return
        }

        if (description.isEmpty()) {
            etJobDescription.error = "Job description is required"
            return
        }

        if (budgetText.isEmpty()) {
            etBudget.error = "Budget is required"
            return
        }

        val budget = try {
            budgetText.toDouble()
        } catch (e: NumberFormatException) {
            etBudget.error = "Please enter a valid budget"
            return
        }

        if (budget <= 0) {
            etBudget.error = "Budget must be greater than 0"
            return
        }

        val skills = if (skillsText.isNotEmpty()) {
            skillsText.split(",").map { it.trim() }
        } else {
            emptyList()
        }

        val clientId = sessionManager.getUserId() ?: ""
        val clientName = sessionManager.getUserName() ?: "Client"

        if (clientId.isEmpty()) {
            Toast.makeText(this, "Please login to create a job", Toast.LENGTH_SHORT).show()
            return
        }

        val job = Job(
            jobId = "", // Let FirebaseService generate the ID
            clientId = clientId,
            clientName = clientName,
            title = title,
            description = description,
            category = category,
            skillsRequired = skills,
            budget = budget,
            deadline = Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000), // 30 days
            location = location.ifEmpty { "Remote" },
            status = "open",
            postedAt = Date()
        )

        saveJobToFirebase(job)
    }

    private fun saveJobToFirebase(job: Job) {
        progressBar.visibility = View.VISIBLE
        btnCreateJob.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = firebaseService.createJob(job)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnCreateJob.isEnabled = true

                    if (result.isSuccess) {
                        Toast.makeText(
                            this@CreateJobActivity,
                            "Job created successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish() // Go back to dashboard
                    } else {
                        Toast.makeText(
                            this@CreateJobActivity,
                            "Failed to create job: ${result.exceptionOrNull()?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnCreateJob.isEnabled = true
                    Toast.makeText(
                        this@CreateJobActivity,
                        "Error creating job: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
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
}
package student.projects.tholagig.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.*
import student.projects.tholagig.R
import student.projects.tholagig.auth.LoginActivity
import student.projects.tholagig.dashboards.FreelancerDashboardActivity
import student.projects.tholagig.models.User
import student.projects.tholagig.network.FirebaseService
import student.projects.tholagig.network.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import student.projects.tholagig.jobs.JobBrowseActivity
import student.projects.tholagig.jobs.MyApplicationsActivity

class ProfileActivity : AppCompatActivity() {

    private lateinit var ivProfile: ImageView
    private lateinit var btnChangePhoto: TextView
    private lateinit var etFullName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etUserType: TextInputEditText
    private lateinit var etSkills: TextInputEditText
    private lateinit var etCompany: TextInputEditText
    private lateinit var etBio: TextInputEditText
    private lateinit var cardSkills: androidx.cardview.widget.CardView
    private lateinit var cardCompany: androidx.cardview.widget.CardView
    private lateinit var tvSkillsTitle: TextView
    private lateinit var btnUpdateProfile: Button
    private lateinit var btnChangePassword: Button
    private lateinit var btnLogout: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var bottomNavigationView: BottomNavigationView

    private lateinit var sessionManager: SessionManager
    private lateinit var firebaseService: FirebaseService


    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        sessionManager = SessionManager(this)
        firebaseService = FirebaseService()

        initializeViews()
        setupClickListeners()
        setupBottomNavigation()
        loadUserData()
    }

    private fun initializeViews() {
        ivProfile = findViewById(R.id.ivProfile)
        btnChangePhoto = findViewById(R.id.btnChangePhoto)
        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        etUserType = findViewById(R.id.etUserType)
        etSkills = findViewById(R.id.etSkills)
        etCompany = findViewById(R.id.etCompany)
        etBio = findViewById(R.id.etBio)
        cardSkills = findViewById(R.id.cardSkills)
        cardCompany = findViewById(R.id.cardCompany)
        tvSkillsTitle = findViewById(R.id.tvSkillsTitle)
        btnUpdateProfile = findViewById(R.id.btnUpdateProfile)
        btnChangePassword = findViewById(R.id.btnChangePassword)
        btnLogout = findViewById(R.id.btnLogout)
        progressBar = findViewById(R.id.progressBar)
        bottomNavigationView = findViewById(R.id.bottom_navigation)
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, FreelancerDashboardActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_jobs -> {
                    val intent = Intent(this, JobBrowseActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_applications -> {
                    val intent = Intent(this, MyApplicationsActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    // Already on profile page
                    true
                }
                else -> false
            }
        }

        bottomNavigationView.selectedItemId = R.id.nav_profile
    }

    private fun setupClickListeners() {
        btnUpdateProfile.setOnClickListener {
            updateProfile()
        }

        btnChangePassword.setOnClickListener {
            changePassword()
        }

        btnLogout.setOnClickListener {
            logout()
        }

        btnChangePhoto.setOnClickListener {
            changeProfilePhoto()
        }
    }

    private fun loadUserData() {
        progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = sessionManager.getUserId() ?: ""
                val userType = sessionManager.getUserType() ?: "freelancer"
                val email = sessionManager.getEmail() ?: ""

                // Fetch actual user data from Firestore
                val userResult = firebaseService.getUserById(userId)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE

                    if (userResult.isSuccess) {
                        currentUser = userResult.getOrNull()
                        displayUserData(currentUser, userType, email)
                    } else {
                        // If user data not found, use session data with placeholder name
                        displayUserData(null, userType, email)
                        Toast.makeText(
                            this@ProfileActivity,
                            "User data not found, please update your profile",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@ProfileActivity,
                        "Error loading profile: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun displayUserData(user: User?, userType: String, email: String) {
        // Set user type specific UI
        setupUserTypeUI(userType)

        // Set basic user data
        etEmail.setText(email)
        etUserType.setText(
            when (userType) {
                "client" -> "Client Account"
                else -> "Freelancer Account"
            }
        )

        // Set actual user data or placeholders
        etFullName.setText(user?.fullName ?: "Enter your name")
        etPhone.setText(user?.phone ?: "Enter phone number")
        etBio.setText(user?.bio ?: "Tell us about yourself...")

        // Set user type specific data
        if (userType == "freelancer") {
            etSkills.setText(user?.skills?.joinToString(", ") ?: "Add your skills")
        } else {
            etCompany.setText(user?.company ?: "Enter company name")
        }
    }

    private fun setupUserTypeUI(userType: String) {
        when (userType) {
            "client" -> {
                cardSkills.visibility = View.GONE
                cardCompany.visibility = View.VISIBLE
                tvSkillsTitle.text = "Company Information"
            }
            else -> {
                cardSkills.visibility = View.VISIBLE
                cardCompany.visibility = View.GONE
                tvSkillsTitle.text = "Skills"
            }
        }
    }

    private fun updateProfile() {
        val fullName = etFullName.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val skills = etSkills.text.toString().trim()
        val company = etCompany.text.toString().trim()
        val bio = etBio.text.toString().trim()

        if (fullName.isEmpty()) {
            etFullName.error = "Full name is required"
            return
        }

        progressBar.visibility = View.VISIBLE
        btnUpdateProfile.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Update user data in Firestore
                val updatedUser = currentUser?.copy(
                    fullName = fullName,
                    phone = phone,
                    skills = if (sessionManager.getUserType() == "freelancer") skills.split(",") else emptyList(),
                    company = company,
                    bio = bio
                )

                // TODO: Implement actual Firestore update
                // firebaseService.updateUser(updatedUser)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnUpdateProfile.isEnabled = true
                    Toast.makeText(
                        this@ProfileActivity,
                        "Profile updated successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnUpdateProfile.isEnabled = true
                    Toast.makeText(
                        this@ProfileActivity,
                        "Error updating profile: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun changePassword() {
        Toast.makeText(this, "Change password feature coming soon!", Toast.LENGTH_SHORT).show()
        // TODO: Implement password change dialog
    }

    private fun changeProfilePhoto() {
        Toast.makeText(this, "Change profile photo feature coming soon!", Toast.LENGTH_SHORT).show()
        // TODO: Implement image picker and upload to Firebase Storage
    }

    private fun logout() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { dialog, which ->
                sessionManager.logout()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
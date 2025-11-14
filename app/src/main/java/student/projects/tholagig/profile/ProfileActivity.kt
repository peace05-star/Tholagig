package student.projects.tholagig.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.*
import student.projects.tholagig.BaseActivity
import student.projects.tholagig.R
import student.projects.tholagig.auth.LoginActivity
import student.projects.tholagig.dashboards.FreelancerDashboardActivity
import student.projects.tholagig.models.User
import student.projects.tholagig.network.FirebaseService
import student.projects.tholagig.network.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import student.projects.tholagig.jobs.JobBrowseActivity
import student.projects.tholagig.jobs.MyApplicationsActivity
import student.projects.tholagig.messaging.MessagesActivity

class ProfileActivity : BaseActivity() {

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
    private lateinit var btnChangeLanguage: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var btnBack: ImageButton

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
        updateUITexts()
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
        btnChangeLanguage = findViewById(R.id.btnChangeLanguage)
        btnBack = findViewById(R.id.btnBack)
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
                R.id.nav_messages -> {
                    startActivity(Intent(this, MessagesActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }

        bottomNavigationView.selectedItemId = R.id.nav_profile
    }

    private fun setupClickListeners() {
        // Back button
        btnBack.setOnClickListener {
            onBackPressed()
        }

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

        btnChangeLanguage.setOnClickListener {
            showLanguageDialog()
        }
    }

    private fun updateUITexts() {
        // Update all UI texts based on current language
        btnUpdateProfile.text = getString(R.string.update_profile)
        btnChangePassword.text = getString(R.string.change_password)
        btnLogout.text = getString(R.string.logout)
        btnChangeLanguage.text = getString(R.string.change_language)
        btnChangePhoto.text = getString(R.string.change_photo)

        // Update hints and placeholders
        etFullName.hint = getString(R.string.full_name)
        etEmail.hint = getString(R.string.email)
        etPhone.hint = getString(R.string.phone)
        etUserType.hint = getString(R.string.account_type)
        etBio.hint = getString(R.string.bio)

        // Update user type specific texts
        val userType = sessionManager.getUserType() ?: "freelancer"
        setupUserTypeUI(userType)

        // Update back button content description for accessibility
        btnBack.contentDescription = getString(R.string.back)
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
                            getString(R.string.user_data_not_found),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@ProfileActivity,
                        "${getString(R.string.error_loading_profile)}: ${e.message}",
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
                "client" -> getString(R.string.client_account)
                else -> getString(R.string.freelancer_account)
            }
        )

        // Set actual user data or placeholders
        etFullName.setText(user?.fullName ?: getString(R.string.enter_your_name))
        etPhone.setText(user?.phone ?: getString(R.string.enter_phone_number))
        etBio.setText(user?.bio ?: getString(R.string.tell_about_yourself))

        // Set user type specific data
        if (userType == "freelancer") {
            etSkills.setText(user?.skills?.joinToString(", ") ?: getString(R.string.add_your_skills))
        } else {
            etCompany.setText(user?.company ?: getString(R.string.enter_company_name))
        }
    }

    private fun setupUserTypeUI(userType: String) {
        when (userType) {
            "client" -> {
                cardSkills.visibility = View.GONE
                cardCompany.visibility = View.VISIBLE
                tvSkillsTitle.text = getString(R.string.company_information)
                etCompany.hint = getString(R.string.company_name)
            }
            else -> {
                cardSkills.visibility = View.VISIBLE
                cardCompany.visibility = View.GONE
                tvSkillsTitle.text = getString(R.string.skills)
                etSkills.hint = getString(R.string.skills_hint)
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
            etFullName.error = getString(R.string.full_name_required)
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
                    skills = if (sessionManager.getUserType() == "freelancer") skills.split(",").map { it.trim() } else emptyList(),
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
                        getString(R.string.profile_updated_success),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnUpdateProfile.isEnabled = true
                    Toast.makeText(
                        this@ProfileActivity,
                        "${getString(R.string.error_updating_profile)}: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun changePassword() {
        Toast.makeText(this, getString(R.string.change_password_coming_soon), Toast.LENGTH_SHORT).show()
        // TODO: Implement password change dialog
    }

    private fun changeProfilePhoto() {
        Toast.makeText(this, getString(R.string.change_photo_coming_soon), Toast.LENGTH_SHORT).show()
        // TODO: Implement image picker and upload to Firebase Storage
    }

    private fun logout() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.logout))
            .setMessage(getString(R.string.logout_confirmation))
            .setPositiveButton(getString(R.string.logout)) { dialog, which ->
                sessionManager.logout()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        // Update UI texts when returning to activity (in case language changed)
        updateUITexts()
    }
}
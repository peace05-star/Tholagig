package student.projects.tholagig.auth

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import student.projects.tholagig.R
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import student.projects.tholagig.dashboards.ClientDashboardActivity
import student.projects.tholagig.dashboards.FreelancerDashboardActivity
import java.security.MessageDigest
import student.projects.tholagig.network.SessionManager
import kotlinx.coroutines.*
import student.projects.tholagig.models.User
import student.projects.tholagig.network.FirebaseService


class RegisterActivity : AppCompatActivity() {

    private lateinit var etFullName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var etSkills: TextInputEditText
    private lateinit var etCompany: TextInputEditText
    private lateinit var etBio: TextInputEditText
    private lateinit var layoutSkills: TextInputLayout
    private lateinit var layoutCompany: TextInputLayout
    private lateinit var cbTerms: CheckBox
    private lateinit var btnRegister: Button
    private lateinit var tvLogin: TextView
    private lateinit var tvUserType: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var progressBar: ProgressBar

    private var userType: String = "freelancer" // Default

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Get user type from intent
        userType = intent.getStringExtra("USER_TYPE") ?: "freelancer"

        initializeViews()
        setupUserTypeSpecificFields()
        setupClickListeners()
    }

    private fun initializeViews() {
        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        etSkills = findViewById(R.id.etSkills)
        etCompany = findViewById(R.id.etCompany)
        etBio = findViewById(R.id.etBio)
        layoutSkills = findViewById(R.id.layoutSkills)
        layoutCompany = findViewById(R.id.layoutCompany)
        cbTerms = findViewById(R.id.cbTerms)
        btnRegister = findViewById(R.id.btnRegister)
        tvLogin = findViewById(R.id.tvLogin)
        tvUserType = findViewById(R.id.tvUserType)
        btnBack = findViewById(R.id.btnBack)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupUserTypeSpecificFields() {
        when (userType) {
            "client" -> {
                tvUserType.text = "Client"
                layoutSkills.visibility = View.GONE
                layoutCompany.visibility = View.VISIBLE
            }
            "freelancer" -> {
                tvUserType.text = "Freelancer"
                layoutSkills.visibility = View.VISIBLE
                layoutCompany.visibility = View.GONE
            }
        }
    }

    private fun setupClickListeners() {
        btnRegister.setOnClickListener {
            attemptRegistration()
        }

        tvLogin.setOnClickListener {
            navigateToLogin()
        }

        btnBack.setOnClickListener {
            onBackPressed()
        }
    }

    private fun attemptRegistration() {
        val fullName = etFullName.text?.toString()?.trim() ?: ""
        val email = etEmail.text?.toString()?.trim() ?: ""
        val phone = etPhone.text?.toString()?.trim() ?: ""
        val password = etPassword.text?.toString()?.trim() ?: ""
        val confirmPassword = etConfirmPassword.text?.toString()?.trim() ?: ""
        val skills = etSkills.text?.toString()?.trim() ?: ""
        val company = etCompany.text?.toString()?.trim() ?: ""
        val bio = etBio.text?.toString()?.trim() ?: ""

        if (validateInputs(fullName, email, phone, password, confirmPassword, skills, company, bio)) {
            performRegistration(fullName, email, phone, password, skills, company, bio)
        }
    }

    private fun validateInputs(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String,
        skills: String,
        company: String,
        bio: String
    ): Boolean {
        // Full Name validation
        if (fullName.isEmpty()) {
            etFullName.error = "Full name is required"
            return false
        }

        // Email validation
        if (email.isEmpty()) {
            etEmail.error = "Email is required"
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Valid email is required"
            return false
        }

        // Phone validation
        if (phone.isEmpty()) {
            etPhone.error = "Phone number is required"
            return false
        }

        // Password validation
        if (password.isEmpty()) {
            etPassword.error = "Password is required"
            return false
        }
        if (password.length < 6) {
            etPassword.error = "Password must be at least 6 characters"
            return false
        }

        // Confirm password validation
        if (confirmPassword != password) {
            etConfirmPassword.error = "Passwords do not match"
            return false
        }

        // User type specific validation
        when (userType) {
            "freelancer" -> {
                if (skills.isEmpty()) {
                    etSkills.error = "Skills are required"
                    return false
                }
            }
            "client" -> {
                if (company.isEmpty()) {
                    etCompany.error = "Company name is required"
                    return false
                }
            }
        }

        // Terms and conditions validation
        if (!cbTerms.isChecked) {
            Toast.makeText(this, "Please accept terms and conditions", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun performRegistration(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        skills: String,
        company: String,
        bio: String
    ) {
        progressBar.visibility = View.VISIBLE
        btnRegister.isEnabled = false

        val firebaseService = FirebaseService()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val user = User(
                    userId = "",
                    email = email,
                    password = password,
                    userType = userType,
                    fullName = fullName,
                    phone = phone,
                    skills = if (userType == "freelancer") skills.split(",") else emptyList(),
                    company = if (userType == "client") company else "",
                    bio = bio
                )

                println("🟡 Starting registration for: $email")

                val result = firebaseService.registerUser(user)

                println("🟡 Registration result: $result")

                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        val registeredUser = result.getOrNull()
                        println("🟢 Registration successful! User ID: ${registeredUser?.userId}")

                        SessionManager(this@RegisterActivity).saveUserSession(
                            userId = registeredUser?.userId ?: "",
                            userType = userType,
                            email = email,
                            userName = fullName

                        )

                        showSuccess(" Registration successful! Welcome to TholaGig!")

                        Handler(Looper.getMainLooper()).postDelayed({
                            navigateToAppropriateDashboard()
                        }, 1500)

                    } else {
                        val error = result.exceptionOrNull()
                        println("🔴 Registration failed: ${error?.message}")
                        showError("Registration failed: ${error?.message ?: "Unknown error"}")
                        progressBar.visibility = View.GONE
                        btnRegister.isEnabled = true
                    }
                }
            } catch (e: Exception) {
                println("🔴 Registration exception: ${e.message}")
                withContext(Dispatchers.Main) {
                    showError("Registration error: ${e.message}")
                    progressBar.visibility = View.GONE
                    btnRegister.isEnabled = true
                }
            }
        }
    }

    private fun navigateToAppropriateDashboard() {
        val intent = when (userType) {
            "client" -> Intent(this, ClientDashboardActivity::class.java)
            else -> Intent(this, FreelancerDashboardActivity::class.java)
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showSuccess(message: String) {
        // Using Snackbar for better visibility
        val snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
        snackbar.setBackgroundTint(resources.getColor(R.color.green)) // Add green color for success
        snackbar.setTextColor(resources.getColor(android.R.color.white))
        snackbar.show()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
package student.projects.tholagig.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.*
import student.projects.tholagig.R
import student.projects.tholagig.dashboards.ClientDashboardActivity
import student.projects.tholagig.dashboards.FreelancerDashboardActivity
import student.projects.tholagig.network.FirebaseService
import student.projects.tholagig.network.SessionManager

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvSignUp: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var progressBar: ProgressBar
    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        tvSignUp = findViewById(R.id.tvSignUp)
        btnBack = findViewById(R.id.btnBack)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            attemptLogin()
        }

        tvForgotPassword.setOnClickListener {
            handleForgotPassword()
        }

        tvSignUp.setOnClickListener {
            navigateToUserTypeSelection()
        }

        btnBack.setOnClickListener {
            onBackPressed()
        }
    }

    private fun attemptLogin() {
        val email = etEmail.text?.toString()?.trim() ?: ""
        val password = etPassword.text?.toString()?.trim() ?: ""

        if (validateInputs(email, password)) {
            performLogin(email, password)
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            etEmail.error = "Email is required"
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Valid email is required"
            return false
        }

        if (password.isEmpty()) {
            etPassword.error = "Password is required"
            return false
        }

        if (password.length < 6) {
            etPassword.error = "Password must be at least 6 characters"
            return false
        }

        return true
    }

    private fun performLogin(email: String, password: String) {
        progressBar.visibility = View.VISIBLE
        btnLogin.isEnabled = false

        Log.d(TAG, "🟡 Starting login process for: $email")

        val firebaseService = FirebaseService()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = firebaseService.loginUser(email, password)

                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        val user = result.getOrNull()
                        Log.d(TAG, "🟢 Firebase login successful, navigating to dashboard")

                        // Save user session
                        SessionManager(this@LoginActivity).saveUserSession(
                            userId = user?.userId ?: "",
                            userType = user?.userType ?: "freelancer",
                            email = email,
                            userName = user?.fullName ?: "",

                        )

                        showSuccess("Login successful!")
                        navigateToAppropriateDashboard(user?.userType ?: "freelancer")

                    } else {
                        val error = result.exceptionOrNull()
                        Log.e(TAG, "🔴 Login failed: ${error?.message}")

                        val errorMessage = when {
                            error?.message?.contains("not found in database") == true ->
                                "Account not properly set up. Please register again."
                            error?.message?.contains("invalid") == true ->
                                "Invalid email or password"
                            else ->
                                "Login failed: ${error?.message ?: "Unknown error"}"
                        }

                        showError(errorMessage)
                        progressBar.visibility = View.GONE
                        btnLogin.isEnabled = true
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "🔴 Login exception: ${e.message}")
                withContext(Dispatchers.Main) {
                    showError("Network error: ${e.message}")
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true
                }
            }
        }
    }

    private fun navigateToAppropriateDashboard(userType: String) {
        Log.d(TAG, "🟡 Navigating to dashboard for user type: $userType")

        val intent = when (userType) {
            "client" -> Intent(this, ClientDashboardActivity::class.java)
            else -> Intent(this, FreelancerDashboardActivity::class.java)
        }

        // Clear the back stack so user can't go back to login
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
        Log.d(TAG, "🟢 LoginActivity finished, should be on dashboard now")
    }

    private fun navigateToUserTypeSelection() {
        val intent = Intent(this, UserTypeSelectionActivity::class.java)
        startActivity(intent)
    }

    private fun handleForgotPassword() {
        Toast.makeText(this, "Password reset feature coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
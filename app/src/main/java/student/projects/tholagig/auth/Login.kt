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
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.*
import student.projects.tholagig.BaseActivity
import student.projects.tholagig.R
import student.projects.tholagig.dashboards.ClientDashboardActivity
import student.projects.tholagig.dashboards.FreelancerDashboardActivity
import student.projects.tholagig.network.FirebaseService
import student.projects.tholagig.network.SessionManager

class LoginActivity : BaseActivity() {

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvSignUp: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var progressBar: ProgressBar

    // SSO Buttons
    private lateinit var btnGoogleSignIn: MaterialButton
    private lateinit var btnFacebookSignIn: MaterialButton

    // SSO Manager
    private lateinit var ssoManager: SSOManager

    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initializeViews()
        setupAccessibility()
        setupSSO()
        setupClickListeners()
        updateUITexts()
    }

    private fun initializeViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        tvSignUp = findViewById(R.id.tvSignUp)
        btnBack = findViewById(R.id.btnBack)
        progressBar = findViewById(R.id.progressBar)

        // Initialize SSO buttons
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
        btnFacebookSignIn = findViewById(R.id.btnFacebookSignIn)

        // Initialize SSO Manager
        ssoManager = SSOManager(this)
    }

    private fun setupAccessibility() {
        // Set content descriptions for icons
        btnBack.contentDescription = getString(R.string.back)

        // Set input field hints programmatically for localization
        etEmail.hint = getString(R.string.email)
        etPassword.hint = getString(R.string.password)
    }

    private fun updateUITexts() {
        // Update all text views with localized strings
        findViewById<TextView>(R.id.tvWelcomeBack)?.text = getString(R.string.welcome_back)
        findViewById<TextView>(R.id.tvSignInContinue)?.text = getString(R.string.sign_in_to_continue)

        tvForgotPassword.text = getString(R.string.forgot_password)
        btnLogin.text = getString(R.string.sign_in)

        // Update OR CONTINUE WITH text
        findViewById<TextView>(R.id.tvOrContinueWith)?.text = getString(R.string.or_continue_with)

        // Update DON'T HAVE AN ACCOUNT section
        findViewById<TextView>(R.id.tvDontHaveAccount)?.text = getString(R.string.dont_have_account)
        tvSignUp.text = getString(R.string.sign_up)
    }

    private fun setupSSO() {
        ssoManager.initializeGoogleSignIn()
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

        // Google Sign-In
        btnGoogleSignIn.setOnClickListener {
            startGoogleSignIn()
        }

        // Facebook Sign-In (placeholder for now)
        btnFacebookSignIn.setOnClickListener {
            showMessage(getString(R.string.facebook_sso_coming_soon))
        }
    }

    private fun startGoogleSignIn() {
        progressBar.visibility = View.VISIBLE
        setSSOButtonsEnabled(false)

        try {
            val signInIntent = ssoManager.getGoogleSignInIntent()
            startActivityForResult(signInIntent, SSOManager.RC_GOOGLE_SIGN_IN)
        } catch (e: Exception) {
            Log.e(TAG, "🔴 Error starting Google Sign-In: ${e.message}")
            progressBar.visibility = View.GONE
            setSSOButtonsEnabled(true)
            showError(getString(R.string.unable_to_start_google_signin))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.d(TAG, "🟡 onActivityResult: requestCode=$requestCode, resultCode=$resultCode")

        when (requestCode) {
            SSOManager.RC_GOOGLE_SIGN_IN -> {
                handleGoogleSignInResult(data)
            }
        }
    }

    private fun handleGoogleSignInResult(data: Intent?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = ssoManager.handleGoogleSignInResult(data)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    setSSOButtonsEnabled(true)

                    when (result) {
                        is SSOResult.Success -> {
                            val user = result.user
                            Log.d(TAG, "🟢 SSO Login successful: ${user.email}")

                            // Save user session
                            SessionManager(this@LoginActivity).saveUserSession(
                                userId = user.userId,
                                userType = user.userType,
                                email = user.email,
                                userName = user.fullName
                            )

                            showSuccess(getString(R.string.welcome_message, user.fullName))
                            navigateToAppropriateDashboard(user.userType)
                        }
                        is SSOResult.Error -> {
                            Log.e(TAG, "🔴 SSO Login failed: ${result.message}")
                            showError(result.message)
                        }
                        is SSOResult.Cancelled -> {
                            Log.d(TAG, "🟡 SSO Login cancelled")
                            showMessage(getString(R.string.sign_in_cancelled))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "🔴 Exception in handleGoogleSignInResult: ${e.message}")
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    setSSOButtonsEnabled(true)
                    showError("${getString(R.string.sso_authentication_failed)}: ${e.message}")
                }
            }
        }
    }

    private fun setSSOButtonsEnabled(enabled: Boolean) {
        btnGoogleSignIn.isEnabled = enabled
        btnFacebookSignIn.isEnabled = enabled
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
            etEmail.error = getString(R.string.email_required)
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = getString(R.string.valid_email_required)
            return false
        }

        if (password.isEmpty()) {
            etPassword.error = getString(R.string.password_required)
            return false
        }

        if (password.length < 6) {
            etPassword.error = getString(R.string.password_min_length)
            return false
        }

        return true
    }

    private fun performLogin(email: String, password: String) {
        progressBar.visibility = View.VISIBLE
        btnLogin.isEnabled = false
        setSSOButtonsEnabled(false)

        Log.d(TAG, "🟡 Starting login process for: $email")

        val firebaseService = FirebaseService()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = firebaseService.loginUser(email, password)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true
                    setSSOButtonsEnabled(true)

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

                        showSuccess(getString(R.string.login_successful))
                        navigateToAppropriateDashboard(user?.userType ?: "freelancer")

                    } else {
                        val error = result.exceptionOrNull()
                        Log.e(TAG, "🔴 Login failed: ${error?.message}")

                        val errorMessage = when {
                            error?.message?.contains("not found in database") == true ->
                                getString(R.string.account_not_setup)
                            error?.message?.contains("invalid") == true ->
                                getString(R.string.invalid_email_password)
                            else ->
                                "${getString(R.string.login_failed)}: ${error?.message ?: getString(R.string.unknown_error)}"
                        }

                        showError(errorMessage)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "🔴 Login exception: ${e.message}")
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true
                    setSSOButtonsEnabled(true)
                    showError("${getString(R.string.network_error)}: ${e.message}")
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
        showMessage(getString(R.string.password_reset_coming_soon))
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        // Update texts when returning to activity (in case language changed)
        updateUITexts()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel any ongoing coroutines
        CoroutineScope(Dispatchers.IO).cancel()
    }
}
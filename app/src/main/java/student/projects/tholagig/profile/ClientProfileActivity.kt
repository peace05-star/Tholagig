package student.projects.tholagig.profile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import student.projects.tholagig.R
import student.projects.tholagig.auth.LoginActivity
import student.projects.tholagig.dashboards.ClientDashboardActivity
import student.projects.tholagig.models.User
import student.projects.tholagig.network.ApiClient
import student.projects.tholagig.network.FirebaseService
import student.projects.tholagig.network.SessionManager

class ClientProfileActivity : AppCompatActivity() {

    private lateinit var ivProfile: ImageView
    private lateinit var btnChangePhoto: ImageButton
    private lateinit var etFullName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etCompany: TextInputEditText
    private lateinit var etBio: TextInputEditText
    private lateinit var cardCompany: androidx.cardview.widget.CardView
    private lateinit var tvCompanyTitle: TextView
    private lateinit var btnUpdateProfile: Button
    private lateinit var btnChangePassword: Button
    private lateinit var btnLogout: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvUserName: TextView
    private lateinit var tvUserType: TextView
    private lateinit var btnBack: ImageButton

    private lateinit var sessionManager: SessionManager
    private lateinit var firebaseService: FirebaseService

    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_profile)

        sessionManager = SessionManager(this)
        firebaseService = FirebaseService()

        initializeViews()
        setupClickListeners()
        loadUserDataWithRestApi() // Changed to use REST API
    }

    private fun initializeViews() {
        try {
            ivProfile = findViewById(R.id.ivProfile)
            btnChangePhoto = findViewById(R.id.btnChangePhoto)
            etFullName = findViewById(R.id.etFullName)
            etEmail = findViewById(R.id.etEmail)
            etPhone = findViewById(R.id.etPhone)
            etCompany = findViewById(R.id.etCompany)
            etBio = findViewById(R.id.etBio)
            cardCompany = findViewById(R.id.cardCompany)
            tvCompanyTitle = findViewById(R.id.tvCompanyTitle)
            btnUpdateProfile = findViewById(R.id.btnUpdateProfile)
            btnChangePassword = findViewById(R.id.btnChangePassword)
            btnLogout = findViewById(R.id.btnLogout)
            btnBack = findViewById(R.id.btnBack)
            progressBar = findViewById(R.id.progressBar)
            tvUserName = findViewById(R.id.tvUserName)
            tvUserType = findViewById(R.id.tvUserType)

            Log.d("ClientProfileActivity", "All views initialized successfully")
        } catch (e: Exception) {
            Log.e("ClientProfileActivity", "Error initializing views: ${e.message}")
            Toast.makeText(this, "Error setting up profile screen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            onBackPressed()
        }

        btnUpdateProfile.setOnClickListener {
            updateProfileWithRestApi() // Changed to use REST API
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

    // STEP 5A: NEW METHOD - Load data using REST API
    private fun loadUserDataWithRestApi() {
        progressBar.visibility = View.VISIBLE

        val userId = sessionManager.getUserId() ?: "1" // Using "1" as fallback for testing
        val userType = sessionManager.getUserType() ?: "client"
        val email = sessionManager.getEmail() ?: ""

        Log.d("ClientProfileActivity", "🟡 Loading user via REST API: $userId")

        // Make REST API call
        val call = ApiClient.userApiService.getUserProfile(userId)

        call.enqueue(object : Callback<User> {
            override fun onResponse(call: Call<User>, response: Response<User>) {
                progressBar.visibility = View.GONE

                if (response.isSuccessful) {
                    currentUser = response.body()
                    Log.d("ClientProfileActivity", "✅ REST API SUCCESS - User loaded")
                    Log.d("ClientProfileActivity", "User name: ${currentUser?.fullName}")

                    displayUserData(currentUser, userType, email)
                    Toast.makeText(this@ClientProfileActivity, "Data loaded via REST API!", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("ClientProfileActivity", "❌ REST API Error: ${response.code()} - ${response.message()}")
                    // Fallback to Firestore
                    loadUserDataWithFirestore(userId, userType, email)
                }
            }

            override fun onFailure(call: Call<User>, t: Throwable) {
                progressBar.visibility = View.GONE
                Log.e("ClientProfileActivity", "❌ REST API Failure: ${t.message}")
                Toast.makeText(this@ClientProfileActivity, "API call failed, using local data", Toast.LENGTH_SHORT).show()
                // Fallback to Firestore
                loadUserDataWithFirestore(userId, userType, email)
            }
        })
    }

    // STEP 5B: Fallback method using Firestore
    private fun loadUserDataWithFirestore(userId: String, userType: String, email: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userResult = firebaseService.getUserById(userId)
                withContext(Dispatchers.Main) {
                    if (userResult.isSuccess) {
                        currentUser = userResult.getOrNull()
                        displayUserData(currentUser, userType, email)
                    } else {
                        displayUserData(null, userType, email)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    displayUserData(null, userType, email)
                }
            }
        }
    }

    // STEP 5C: NEW METHOD - Update profile using REST API
    private fun updateProfileWithRestApi() {
        val fullName = etFullName.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val company = etCompany.text.toString().trim()
        val bio = etBio.text.toString().trim()

        if (fullName.isEmpty()) {
            etFullName.error = "Full name is required"
            return
        }

        if (company.isEmpty()) {
            etCompany.error = "Company name is required"
            return
        }

        progressBar.visibility = View.VISIBLE
        btnUpdateProfile.isEnabled = false

        val userId = sessionManager.getUserId() ?: "1" // Using "1" as fallback for testing

        // Create user object for API
        val userToUpdate = User(
            userId = userId,
            fullName = fullName,
            email = sessionManager.getEmail() ?: "",
            phone = phone,
            userType = "client",
            company = company,
            bio = bio
        )

        Log.d("ClientProfileActivity", "🟡 Updating via REST API: $userId")

        // Make REST API call
        val call = ApiClient.userApiService.updateUserProfile(userId, userToUpdate)

        call.enqueue(object : Callback<User> {
            override fun onResponse(call: Call<User>, response: Response<User>) {
                progressBar.visibility = View.GONE
                btnUpdateProfile.isEnabled = true

                if (response.isSuccessful) {
                    currentUser = response.body()
                    tvUserName.text = fullName
                    Toast.makeText(
                        this@ClientProfileActivity,
                        "✅ Profile updated via REST API!",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d("ClientProfileActivity", "✅ REST API update successful")
                } else {
                    Log.e("ClientProfileActivity", "❌ REST API update failed: ${response.code()}")
                    // Fallback to Firestore
                    updateProfileWithFirestore(userId, fullName, phone, company, bio)
                }
            }

            override fun onFailure(call: Call<User>, t: Throwable) {
                progressBar.visibility = View.GONE
                btnUpdateProfile.isEnabled = true
                Log.e("ClientProfileActivity", "❌ REST API update failure: ${t.message}")
                // Fallback to Firestore
                updateProfileWithFirestore(userId, fullName, phone, company, bio)
            }
        })
    }

    // STEP 5D: Fallback update method using Firestore
    private fun updateProfileWithFirestore(userId: String, fullName: String, phone: String, company: String, bio: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val updateData = mapOf(
                    "fullName" to fullName,
                    "phone" to phone,
                    "company" to company,
                    "bio" to bio,
                    "updatedAt" to System.currentTimeMillis()
                )
                firebaseService.updateUser(userId, updateData)
                withContext(Dispatchers.Main) {
                    currentUser = currentUser?.copy(
                        fullName = fullName,
                        phone = phone,
                        company = company,
                        bio = bio
                    )
                    tvUserName.text = fullName
                    Toast.makeText(this@ClientProfileActivity, "Profile updated via Firestore", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ClientProfileActivity, "Update failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ... REST OF YOUR EXISTING METHODS (displayUserData, setupUserTypeUI, changePassword, etc.)
    private fun displayUserData(user: User?, userType: String, email: String) {
        try {
            Log.d("ClientProfileActivity", "Displaying user data...")

            // Set header information
            val displayName = user?.fullName ?: "Client Name"
            tvUserName.text = displayName
            tvUserType.text = "Client Account"

            // Set basic user data - email from session manager
            etEmail.setText(email)

            // Set actual user data from Firestore or placeholders
            etFullName.setText(user?.fullName ?: "Enter your name")
            etPhone.setText(user?.phone ?: "Enter phone number")
            etBio.setText(user?.bio ?: "Tell freelancers about your projects and what you're looking for...")
            etCompany.setText(user?.company ?: "Enter company name")

            // Set user type specific UI
            setupUserTypeUI(userType)

            Log.d("ClientProfileActivity", "User data displayed: $displayName")

        } catch (e: Exception) {
            Log.e("ClientProfileActivity", "Error displaying user data: ${e.message}")
            Toast.makeText(this, "Error displaying profile data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupUserTypeUI(userType: String) {
        // For client, always show company section
        cardCompany.visibility = View.VISIBLE
        tvCompanyTitle.text = "Company Information"
    }

    private fun changePassword() {
        Toast.makeText(this, "Change password feature coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun changeProfilePhoto() {
        Toast.makeText(this, "Change profile photo feature coming soon!", Toast.LENGTH_SHORT).show()
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

    override fun onBackPressed() {
        val intent = Intent(this, ClientDashboardActivity::class.java)
        startActivity(intent)
        finish()
        super.onBackPressed()
    }
}
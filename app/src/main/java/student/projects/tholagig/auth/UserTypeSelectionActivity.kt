package student.projects.tholagig.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import student.projects.tholagig.R

class UserTypeSelectionActivity : AppCompatActivity() {

    private lateinit var cardClient: CardView
    private lateinit var cardFreelancer: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_user_type)
            Log.d("USER_TYPE_DEBUG", "Activity layout set successfully")
        } catch (e: Exception) {
            Log.e("USER_TYPE_DEBUG", "Error setting content view: ${e.message}", e)
            // Fallback to a simple layout
            setContentView(android.R.layout.simple_list_item_1)
            return
        }

        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        try {
            cardClient = findViewById(R.id.cardClient)
            cardFreelancer = findViewById(R.id.cardFreelancer)
            Log.d("USER_TYPE_DEBUG", "Views initialized successfully")
        } catch (e: Exception) {
            Log.e("USER_TYPE_DEBUG", "Error initializing views: ${e.message}", e)
        }
    }

    private fun setupClickListeners() {
        cardClient.setOnClickListener {
            Log.d("USER_TYPE_DEBUG", "Client card clicked")
            navigateToRegister("client")
        }

        cardFreelancer.setOnClickListener {
            Log.d("USER_TYPE_DEBUG", "Freelancer card clicked")
            navigateToRegister("freelancer")
        }
    }

    private fun navigateToRegister(userType: String) {
        try {
            Log.d("USER_TYPE_DEBUG", "Navigating to register as: $userType")
            val intent = Intent(this, RegisterActivity::class.java)
            intent.putExtra("USER_TYPE", userType)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("USER_TYPE_DEBUG", "Error navigating to register: ${e.message}", e)
        }
    }
}
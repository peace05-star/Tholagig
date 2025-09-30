package student.projects.tholagig.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import student.projects.tholagig.R

class UserTypeSelectionActivity : AppCompatActivity() {

    private lateinit var cardClient: CardView
    private lateinit var cardFreelancer: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_type)

        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        cardClient = findViewById(R.id.cardClient)
        cardFreelancer = findViewById(R.id.cardFreelancer)
    }

    private fun setupClickListeners() {
        cardClient.setOnClickListener {
            navigateToRegister("client")
        }

        cardFreelancer.setOnClickListener {
            navigateToRegister("freelancer")
        }
    }

    private fun navigateToRegister(userType: String) {
        val intent = Intent(this, RegisterActivity::class.java)
        intent.putExtra("USER_TYPE", userType)
        startActivity(intent)
    }
}
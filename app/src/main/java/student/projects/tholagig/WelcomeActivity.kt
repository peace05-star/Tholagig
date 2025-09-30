package student.projects.tholagig

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import student.projects.tholagig.auth.LoginActivity
import student.projects.tholagig.auth.UserTypeSelectionActivity

class WelcomeActivity : AppCompatActivity() {

    private lateinit var btnSignIn: Button
    private lateinit var btnSignUp: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        btnSignIn = findViewById(R.id.btnSignIn)
        btnSignUp = findViewById(R.id.btnSignUp)
    }

    private fun setupClickListeners() {
        btnSignIn.setOnClickListener {
            navigateToLogin()
        }

        btnSignUp.setOnClickListener {
            navigateToUserTypeSelection()
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToUserTypeSelection() {
        val intent = Intent(this, UserTypeSelectionActivity::class.java)
        startActivity(intent)
    }
}
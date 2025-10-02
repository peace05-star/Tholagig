package student.projects.tholagig.messaging

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import student.projects.tholagig.R
import student.projects.tholagig.dashboards.FreelancerDashboardActivity
import student.projects.tholagig.jobs.JobBrowseActivity
import student.projects.tholagig.profile.ProfileActivity

class ConversationsActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversations)

        initializeViews()
        setupBottomNavigation()
    }

    private fun initializeViews() {
        bottomNavigationView = findViewById(R.id.bottom_navigation)
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    navigateToActivity(FreelancerDashboardActivity::class.java)
                    true
                }
                R.id.nav_jobs -> {
                    navigateToActivity(JobBrowseActivity::class.java)
                    true
                }
                R.id.nav_applications -> {
                    Toast.makeText(this, "Applications feature coming soon", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_messages -> {
                    // Already on messages page
                    true
                }
                R.id.nav_profile -> {
                    navigateToActivity(ProfileActivity::class.java)
                    true
                }
                else -> false
            }
        }

        bottomNavigationView.selectedItemId = R.id.nav_messages
    }

    private fun <T> navigateToActivity(activityClass: Class<T>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
        finish()
    }
}
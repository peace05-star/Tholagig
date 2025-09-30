package student.projects.tholagig.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import student.projects.tholagig.R
import student.projects.tholagig.models.JobApplication
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class ApplicationsManagementAdapter(
    private val applications: List<JobApplication>,
    private val onViewProfile: (JobApplication) -> Unit,
    private val onAccept: (JobApplication) -> Unit,
    private val onReject: (JobApplication) -> Unit,
    private val onMessage: (JobApplication) -> Unit
) : RecyclerView.Adapter<ApplicationsManagementAdapter.ApplicationViewHolder>() {

    private val currencyFormat = NumberFormat.getCurrencyInstance().apply {
        maximumFractionDigits = 0
        currency = Currency.getInstance("ZAR")
    }

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    class ApplicationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFreelancerName: TextView = itemView.findViewById(R.id.tvFreelancerName)
        val tvJobTitle: TextView = itemView.findViewById(R.id.tvJobTitle)
        val tvSkills: TextView = itemView.findViewById(R.id.tvSkills)
        val tvProposedBudget: TextView = itemView.findViewById(R.id.tvProposedBudget)
        val tvEstimatedTime: TextView = itemView.findViewById(R.id.tvEstimatedTime)
        val tvAppliedDate: TextView = itemView.findViewById(R.id.tvAppliedDate)
        val tvRating: TextView = itemView.findViewById(R.id.tvRating)
        val tvCompletedJobs: TextView = itemView.findViewById(R.id.tvCompletedJobs)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val btnViewProfile: Button = itemView.findViewById(R.id.btnViewProfile)
        val btnAccept: Button = itemView.findViewById(R.id.btnAccept)
        val btnReject: Button = itemView.findViewById(R.id.btnReject)
        val btnMessage: Button = itemView.findViewById(R.id.btnMessage)
        val layoutActions: View = itemView.findViewById(R.id.layoutActions)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplicationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_application_management, parent, false)
        return ApplicationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ApplicationViewHolder, position: Int) {
        val application = applications[position]

        holder.tvFreelancerName.text = application.freelancerName
        holder.tvJobTitle.text = application.jobTitle
        holder.tvSkills.text = application.freelancerSkills?.joinToString(" • ") ?: "No skills listed"
        holder.tvProposedBudget.text = currencyFormat.format(application.proposedBudget)
        holder.tvEstimatedTime.text = application.estimatedTime
        holder.tvAppliedDate.text = "Applied: ${dateFormat.format(application.appliedAt)}"
        holder.tvRating.text = "⭐ ${application.freelancerRating ?: "No rating"}"
        holder.tvCompletedJobs.text = "${application.freelancerCompletedJobs ?: 0} jobs completed"

        // Set status with appropriate color
        setStatusUI(holder.tvStatus, application.status)

        // Show/hide action buttons based on status
        when (application.status.lowercase()) {
            "pending" -> {
                holder.layoutActions.visibility = View.VISIBLE
                holder.btnAccept.visibility = View.VISIBLE
                holder.btnReject.visibility = View.VISIBLE
                holder.btnAccept.text = "Hire"
                holder.btnReject.text = "Reject"
            }
            "accepted" -> {
                holder.layoutActions.visibility = View.VISIBLE
                holder.btnAccept.visibility = View.GONE
                holder.btnReject.visibility = View.GONE
                holder.tvStatus.text = "Hired ✓"
            }
            "rejected" -> {
                holder.layoutActions.visibility = View.VISIBLE
                holder.btnAccept.visibility = View.GONE
                holder.btnReject.visibility = View.GONE
            }
            else -> {
                holder.layoutActions.visibility = View.VISIBLE
            }
        }

        // Set click listeners
        holder.btnViewProfile.setOnClickListener {
            onViewProfile(application)
        }

        holder.btnAccept.setOnClickListener {
            onAccept(application)
        }

        holder.btnReject.setOnClickListener {
            onReject(application)
        }

        holder.btnMessage.setOnClickListener {
            onMessage(application)
        }

        // Whole item click to view cover letter
        holder.itemView.setOnClickListener {
            showCoverLetterDialog(application, holder.itemView.context)
        }
    }

    private fun setStatusUI(statusView: TextView, status: String) {
        when (status.lowercase()) {
            "pending" -> {
                statusView.text = "⏳ Pending Review"
                statusView.setBackgroundResource(R.drawable.status_pending_bg)
                statusView.setTextColor(statusView.context.getColor(R.color.white))
            }
            "accepted" -> {
                statusView.text = "✅ Hired"
                statusView.setBackgroundResource(R.drawable.status_accepted_bg)
                statusView.setTextColor(statusView.context.getColor(R.color.white))
            }
            "rejected" -> {
                statusView.text = "❌ Rejected"
                statusView.setBackgroundResource(R.drawable.status_rejected_bg)
                statusView.setTextColor(statusView.context.getColor(R.color.white))
            }
            else -> {
                statusView.text = status
                statusView.setBackgroundResource(R.drawable.status_pending_bg)
                statusView.setTextColor(statusView.context.getColor(R.color.white))
            }
        }
    }

    private fun showCoverLetterDialog(application: JobApplication, context: android.content.Context) {
        val coverLetter = application.coverLetter ?: "No cover letter provided"

        AlertDialog.Builder(context)
            .setTitle("📝 Cover Letter from ${application.freelancerName}")
            .setMessage(coverLetter)
            .setPositiveButton("Close", null)
            .setNeutralButton("View Profile") { dialog, which ->
                onViewProfile(application)
            }
            .show()
    }

    override fun getItemCount(): Int = applications.size
}
package student.projects.tholagig.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import student.projects.tholagig.R
import student.projects.tholagig.models.JobApplication
import java.text.SimpleDateFormat
import java.util.*

class ClientApplicationsAdapter(
    private var applications: List<JobApplication>,
    private val onActionClick: (JobApplication, String) -> Unit
) : RecyclerView.Adapter<ClientApplicationsAdapter.ClientApplicationViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy 'at' HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClientApplicationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_client_application, parent, false)
        return ClientApplicationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClientApplicationViewHolder, position: Int) {
        holder.bind(applications[position])
    }

    override fun getItemCount(): Int = applications.size

    fun updateApplications(newApplications: List<JobApplication>) {
        applications = newApplications
        notifyDataSetChanged()
    }

    inner class ClientApplicationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvFreelancerName: TextView = itemView.findViewById(R.id.tvFreelancerName)
        private val tvJobTitle: TextView = itemView.findViewById(R.id.tvJobTitle)
        private val tvAppliedDate: TextView = itemView.findViewById(R.id.tvAppliedDate)
        private val tvProposedBudget: TextView = itemView.findViewById(R.id.tvProposedBudget)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvSkills: TextView = itemView.findViewById(R.id.tvSkills)
        private val btnAccept: Button = itemView.findViewById(R.id.btnAccept)
        private val btnReject: Button = itemView.findViewById(R.id.btnReject)
        private val btnViewProfile: Button = itemView.findViewById(R.id.btnViewProfile)
        private val btnContact: Button = itemView.findViewById(R.id.btnContact)

        fun bind(application: JobApplication) {
            tvFreelancerName.text = application.freelancerName
            tvJobTitle.text = application.jobTitle
            tvProposedBudget.text = "R ${application.proposedBudget.toInt()}"
            tvSkills.text = application.freelancerSkills.joinToString(", ")
            tvAppliedDate.text = "Applied: ${dateFormat.format(application.appliedAt ?: Date())}"

            // Set status with color
            setStatusUI(application.status)

            // Set click listeners
            btnAccept.setOnClickListener {
                onActionClick(application, "accept")
            }
            btnReject.setOnClickListener {
                onActionClick(application, "reject")
            }
            btnViewProfile.setOnClickListener {
                onActionClick(application, "view_profile")
            }
            btnContact.setOnClickListener {
                onActionClick(application, "contact")
            }

            // Update button states based on status
            updateButtonStates(application.status)
        }

        private fun setStatusUI(status: String) {
            tvStatus.text = status.replaceFirstChar { it.uppercase() }
            when (status.lowercase()) {
                "pending" -> {
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.orange))
                    tvStatus.setBackgroundResource(R.drawable.status_pending_bg)
                }
                "accepted" -> {
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.green))
                    tvStatus.setBackgroundResource(R.drawable.status_accepted_bg)
                }
                "rejected" -> {
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.red))
                    tvStatus.setBackgroundResource(R.drawable.status_rejected_bg)
                }
                "withdrawn" -> {
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.gray))
                    tvStatus.setBackgroundResource(R.drawable.status_withdrawn_bg)
                }
            }
        }

        private fun updateButtonStates(status: String) {
            when (status.lowercase()) {
                "pending" -> {
                    btnAccept.visibility = View.VISIBLE
                    btnReject.visibility = View.VISIBLE
                    btnAccept.isEnabled = true
                    btnReject.isEnabled = true
                    btnAccept.text = "Accept"
                    btnReject.text = "Reject"
                }
                "accepted" -> {
                    btnAccept.visibility = View.VISIBLE
                    btnReject.visibility = View.VISIBLE
                    btnAccept.isEnabled = false
                    btnReject.isEnabled = true
                    btnAccept.text = "Accepted ✓"
                    btnReject.text = "Revoke"
                }
                "rejected" -> {
                    btnAccept.visibility = View.VISIBLE
                    btnReject.visibility = View.VISIBLE
                    btnAccept.isEnabled = true
                    btnReject.isEnabled = false
                    btnAccept.text = "Reconsider"
                    btnReject.text = "Rejected ✗"
                }
                "withdrawn" -> {
                    btnAccept.visibility = View.GONE
                    btnReject.visibility = View.GONE
                }
            }
        }
    }
}
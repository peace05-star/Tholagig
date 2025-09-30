package student.projects.tholagig.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import student.projects.tholagig.R
import student.projects.tholagig.models.JobApplication
import java.text.SimpleDateFormat
import java.util.*

class ApplicationAdapter(
    private val applications: List<JobApplication>,
    private val onViewDetails: (JobApplication) -> Unit,
    private val onWithdraw: (JobApplication) -> Unit
) : RecyclerView.Adapter<ApplicationAdapter.ApplicationViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    class ApplicationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvJobTitle: TextView = itemView.findViewById(R.id.tvJobTitle)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvClientName: TextView = itemView.findViewById(R.id.tvClientName)
        val tvProposedBudget: TextView = itemView.findViewById(R.id.tvProposedBudget)
        val tvAppliedDate: TextView = itemView.findViewById(R.id.tvAppliedDate)
        val btnViewDetails: Button = itemView.findViewById(R.id.btnViewDetails)
        val btnWithdraw: Button = itemView.findViewById(R.id.btnWithdraw)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplicationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_application, parent, false)
        return ApplicationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ApplicationViewHolder, position: Int) {
        val application = applications[position]

        holder.tvJobTitle.text = application.jobTitle
        holder.tvClientName.text = application.clientName
        holder.tvProposedBudget.text = "R ${application.proposedBudget.toInt()}"
        holder.tvAppliedDate.text = "Applied: ${dateFormat.format(application.appliedAt)}"

        // Set status with appropriate background and text
        setStatusUI(holder.tvStatus, application.status)

        // Show withdraw button only for pending applications
        holder.btnWithdraw.visibility = if (application.status == "pending") View.VISIBLE else View.GONE

        holder.btnViewDetails.setOnClickListener {
            onViewDetails(application)
        }

        holder.btnWithdraw.setOnClickListener {
            onWithdraw(application)
        }
    }

    private fun setStatusUI(statusView: TextView, status: String) {
        when (status.lowercase()) {
            "pending" -> {
                statusView.text = "Pending"
                statusView.setBackgroundResource(R.drawable.status_pending_bg)
            }
            "accepted" -> {
                statusView.text = "Accepted"
                statusView.setBackgroundResource(R.drawable.status_accepted_bg)
            }
            "rejected" -> {
                statusView.text = "Rejected"
                statusView.setBackgroundResource(R.drawable.status_rejected_bg)
            }
            "withdrawn" -> {
                statusView.text = "Withdrawn"
                statusView.setBackgroundResource(R.drawable.status_withdrawn_bg)
            }
            else -> {
                statusView.text = status
                statusView.setBackgroundResource(R.drawable.status_pending_bg)
            }
        }
    }

    override fun getItemCount(): Int = applications.size
}
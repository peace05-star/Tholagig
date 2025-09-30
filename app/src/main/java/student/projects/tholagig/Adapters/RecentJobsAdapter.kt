package student.projects.tholagig.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import student.projects.tholagig.R
import student.projects.tholagig.models.Job
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class RecentJobsAdapter(
    private val jobs: List<Job>,
    private val onJobClick: (Job) -> Unit
) : RecyclerView.Adapter<RecentJobsAdapter.JobViewHolder>() {

    private val currencyFormat = NumberFormat.getCurrencyInstance().apply {
        maximumFractionDigits = 0
        currency = Currency.getInstance("ZAR")
    }

    private val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

    class JobViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // These IDs match your item_job_client.xml exactly
        val tvJobTitle: TextView = itemView.findViewById(R.id.tvJobTitle)
        val tvJobCategory: TextView = itemView.findViewById(R.id.tvJobCategory)
        val tvJobBudget: TextView = itemView.findViewById(R.id.tvJobBudget)
        val tvJobStatus: TextView = itemView.findViewById(R.id.tvJobStatus)
        val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        val tvPostedDate: TextView = itemView.findViewById(R.id.tvPostedDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_job_client, parent, false)
        return JobViewHolder(view)
    }

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        val job = jobs[position]

        // Set data to match your item_job_client.xml layout
        holder.tvJobTitle.text = job.title
        holder.tvJobCategory.text = job.category
        holder.tvJobBudget.text = currencyFormat.format(job.budget)
        holder.tvLocation.text = job.location.ifEmpty { "Remote" }
        holder.tvPostedDate.text = "Posted: ${dateFormat.format(job.postedAt)}"

        // Set status - using simple text colors since bg_gradient might not exist
        when (job.status.lowercase()) {
            "open" -> {
                holder.tvJobStatus.text = "Open"
                holder.tvJobStatus.setBackgroundColor(holder.itemView.context.getColor(R.color.green))
                holder.tvJobStatus.setTextColor(holder.itemView.context.getColor(R.color.white))
            }
            "in_progress" -> {
                holder.tvJobStatus.text = "In Progress"
                holder.tvJobStatus.setBackgroundColor(holder.itemView.context.getColor(R.color.orange))
                holder.tvJobStatus.setTextColor(holder.itemView.context.getColor(R.color.white))
            }
            "completed" -> {
                holder.tvJobStatus.text = "Completed"
                holder.tvJobStatus.setBackgroundColor(holder.itemView.context.getColor(R.color.green))
                holder.tvJobStatus.setTextColor(holder.itemView.context.getColor(R.color.white))
            }
            else -> {
                holder.tvJobStatus.text = job.status
                holder.tvJobStatus.setBackgroundColor(holder.itemView.context.getColor(R.color.gray))
                holder.tvJobStatus.setTextColor(holder.itemView.context.getColor(R.color.white))
            }
        }

        holder.itemView.setOnClickListener {
            onJobClick(job)
        }
    }

    override fun getItemCount(): Int = jobs.size
}
package student.projects.tholagig.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import student.projects.tholagig.models.Job
import java.text.SimpleDateFormat
import java.util.*
import student.projects.tholagig.R

class JobsAdapter(
    private val jobs: List<Job>,
    private val onItemClick: (Job) -> Unit
) : RecyclerView.Adapter<JobsAdapter.JobViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    class JobViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvJobTitle)
        val tvClient: TextView = itemView.findViewById(R.id.tvClient)
        val tvBudget: TextView = itemView.findViewById(R.id.tvBudget)
        val tvDeadline: TextView = itemView.findViewById(R.id.tvDeadline)
        val tvSkills: TextView = itemView.findViewById(R.id.tvSkills)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_job, parent, false)
        return JobViewHolder(view)
    }

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        val job = jobs[position]

        holder.tvTitle.text = job.title
        holder.tvClient.text = "by ${job.clientName}"
        holder.tvBudget.text = "R ${job.budget.toInt()}"
        holder.tvDeadline.text = "Due: ${dateFormat.format(job.deadline)}"
        holder.tvSkills.text = job.skillsRequired.joinToString(" • ")

        holder.itemView.setOnClickListener {
            onItemClick(job)
        }
    }

    override fun getItemCount(): Int = jobs.size
}
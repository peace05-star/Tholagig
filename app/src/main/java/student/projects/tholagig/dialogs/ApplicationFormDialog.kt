package student.projects.tholagig.dialogs

import android.app.Dialog
import android.os.Bundle
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import student.projects.tholagig.R
import student.projects.tholagig.models.Job
import java.text.SimpleDateFormat
import java.util.*

class ApplicationFormDialog : DialogFragment() {

    private lateinit var job: Job
    private lateinit var userSkills: List<String>
    private lateinit var userName: String
    private lateinit var onApply: (String, Double, String) -> Unit

    companion object {
        private const val ARG_JOB_TITLE = "jobTitle"
        private const val ARG_JOB_BUDGET = "jobBudget"
        private const val ARG_JOB_SKILLS = "jobSkills"
        private const val ARG_JOB_CLIENT_NAME = "jobClientName"
        private const val ARG_JOB_DEADLINE = "jobDeadline"
        private const val ARG_USER_SKILLS = "userSkills"
        private const val ARG_USER_NAME = "userName"

        fun newInstance(
            job: Job,
            userSkills: List<String>,
            userName: String,
            onApply: (String, Double, String) -> Unit
        ): ApplicationFormDialog {
            val dialog = ApplicationFormDialog()
            val args = Bundle().apply {
                putString(ARG_JOB_TITLE, job.title)
                putDouble(ARG_JOB_BUDGET, job.budget)
                putStringArrayList(ARG_JOB_SKILLS, ArrayList(job.skillsRequired))
                putString(ARG_JOB_CLIENT_NAME, job.clientName)
                putLong(ARG_JOB_DEADLINE, job.deadline.time)
                putStringArrayList(ARG_USER_SKILLS, ArrayList(userSkills))
                putString(ARG_USER_NAME, userName)
            }
            dialog.arguments = args
            dialog.onApply = onApply
            return dialog
        }
    }

    private lateinit var etCoverLetter: EditText
    private lateinit var etProposedBudget: EditText
    private lateinit var spEstimatedTime: Spinner
    private lateinit var tvMatchingSkills: TextView
    private lateinit var tvJobTitle: TextView
    private lateinit var tvJobBudget: TextView
    private lateinit var tvRequiredSkills: TextView
    private lateinit var btnSubmit: Button
    private lateinit var btnCancel: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieve arguments and recreate the Job object
        arguments?.let { bundle ->
            val jobTitle = bundle.getString(ARG_JOB_TITLE) ?: ""
            val jobBudget = bundle.getDouble(ARG_JOB_BUDGET, 0.0)
            val jobSkills = bundle.getStringArrayList(ARG_JOB_SKILLS) ?: emptyList()
            val jobClientName = bundle.getString(ARG_JOB_CLIENT_NAME) ?: ""
            val jobDeadline = Date(bundle.getLong(ARG_JOB_DEADLINE))

            // Create a minimal Job object with the data we need
            job = Job(
                title = jobTitle,
                budget = jobBudget,
                skillsRequired = jobSkills,
                clientName = jobClientName,
                deadline = jobDeadline
            )

            userSkills = bundle.getStringArrayList(ARG_USER_SKILLS) ?: emptyList()
            userName = bundle.getString(ARG_USER_NAME) ?: "Freelancer"
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_application_form_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        setupForm()
        setupClickListeners()
    }

    private fun initializeViews(view: View) {
        etCoverLetter = view.findViewById(R.id.etCoverLetter)
        etProposedBudget = view.findViewById(R.id.etProposedBudget)
        spEstimatedTime = view.findViewById(R.id.spEstimatedTime)
        tvMatchingSkills = view.findViewById(R.id.tvMatchingSkills)
        tvJobTitle = view.findViewById(R.id.tvJobTitle)
        tvJobBudget = view.findViewById(R.id.tvJobBudget)
        tvRequiredSkills = view.findViewById(R.id.tvRequiredSkills)
        btnSubmit = view.findViewById(R.id.btnSubmit)
        btnCancel = view.findViewById(R.id.btnCancel)
        progressBar = view.findViewById(R.id.progressBar)

        // Set job details
        tvJobTitle.text = job.title
        tvJobBudget.text = "Budget: R ${job.budget.toInt()}"
        tvRequiredSkills.text = "Required Skills: ${job.skillsRequired.joinToString(", ")}"

        // Set job budget as default proposed budget
        etProposedBudget.setText(job.budget.toInt().toString())
    }

    private fun setupForm() {
        // Setup estimated time spinner
        val timeOptions = arrayOf("1-2 weeks", "2-3 weeks", "3-4 weeks", "1 month", "2 months", "3+ months")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, timeOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spEstimatedTime.adapter = adapter
        spEstimatedTime.setSelection(2) // Default to "3-4 weeks"

        // Show matching skills
        val matchingSkills = userSkills.intersect(job.skillsRequired.toSet())
        tvMatchingSkills.text = if (matchingSkills.isNotEmpty()) {
            "Your matching skills: ${matchingSkills.joinToString(", ")}"
        } else {
            "No direct skill matches. Highlight your relevant experience in the cover letter."
        }

        // Pre-fill cover letter with template
        etCoverLetter.setText(generateCoverLetterTemplate())
    }

    private fun setupClickListeners() {
        btnSubmit.setOnClickListener {
            validateAndSubmit()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }

        // Real-time validation
        etProposedBudget.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                validateBudget()
            }
        })
    }

    private fun validateAndSubmit() {
        val coverLetter = etCoverLetter.text.toString().trim()
        val budgetText = etProposedBudget.text.toString().trim()
        val estimatedTime = spEstimatedTime.selectedItem as String

        if (coverLetter.isEmpty()) {
            etCoverLetter.error = "Please write a cover letter"
            etCoverLetter.requestFocus()
            return
        }

        if (coverLetter.length < 50) {
            etCoverLetter.error = "Cover letter should be at least 50 characters"
            etCoverLetter.requestFocus()
            return
        }

        if (budgetText.isEmpty()) {
            etProposedBudget.error = "Please enter your proposed budget"
            etProposedBudget.requestFocus()
            return
        }

        val proposedBudget = budgetText.toDoubleOrNull()
        if (proposedBudget == null || proposedBudget <= 0) {
            etProposedBudget.error = "Please enter a valid budget amount"
            etProposedBudget.requestFocus()
            return
        }

        if (proposedBudget < job.budget * 0.5) {
            showBudgetWarning("Your proposed budget is significantly lower than the job's budget (R ${job.budget.toInt()}).")
            return
        }

        if (proposedBudget > job.budget * 2) {
            showBudgetWarning("Your proposed budget is significantly higher than the job's budget (R ${job.budget.toInt()}).")
            return
        }

        // All validations passed
        progressBar.visibility = View.VISIBLE
        btnSubmit.isEnabled = false
        btnCancel.isEnabled = false

        onApply(coverLetter, proposedBudget, estimatedTime)
    }

    private fun validateBudget() {
        val budgetText = etProposedBudget.text.toString().trim()
        if (budgetText.isNotEmpty()) {
            val proposedBudget = budgetText.toDoubleOrNull() ?: 0.0
            val jobBudget = job.budget

            when {
                proposedBudget < jobBudget * 0.5 -> {
                    etProposedBudget.setBackgroundResource(R.color.orange)
                }
                proposedBudget > jobBudget * 2 -> {
                    etProposedBudget.setBackgroundResource(R.color.orange)
                }
                else -> {
                    etProposedBudget.setBackgroundResource(R.color.gray)
                }
            }
        }
    }

    private fun showBudgetWarning(message: String) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Budget Warning")
            .setMessage("$message\n\nDo you want to proceed anyway?")
            .setPositiveButton("Yes, Proceed") { dialog, which ->
                val coverLetter = etCoverLetter.text.toString().trim()
                val budgetText = etProposedBudget.text.toString().trim()
                val estimatedTime = spEstimatedTime.selectedItem as String
                val proposedBudget = budgetText.toDouble()

                progressBar.visibility = View.VISIBLE
                btnSubmit.isEnabled = false
                btnCancel.isEnabled = false

                onApply(coverLetter, proposedBudget, estimatedTime)
            }
            .setNegativeButton("Edit Budget", null)
            .show()
    }

    private fun generateCoverLetterTemplate(): String {
        val matchingSkills = userSkills.intersect(job.skillsRequired.toSet())
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())

        return """Dear ${job.clientName},

I am excited to apply for the ${job.title} position. ${if (matchingSkills.isNotEmpty()) "With my experience in ${matchingSkills.joinToString(", ")}, I am confident I can deliver high-quality results for your project." else "I believe my skills and experience make me a great fit for this role."}

${if (matchingSkills.isNotEmpty()) "My relevant skills include:\n${matchingSkills.joinToString("\n") { "• $it" }}\n" else ""}
I am available to start immediately and committed to meeting your deadline of ${dateFormat.format(job.deadline)}.

Thank you for considering my application.

Best regards,
$userName"""
    }
}
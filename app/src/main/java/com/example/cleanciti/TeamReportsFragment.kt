package com.example.cleanciti

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cleanciti.databinding.FragmentTeamReportsBinding
import com.example.cleanciti.databinding.ModalTeamActionBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class TeamReportsFragment : Fragment() {

    private var _binding: FragmentTeamReportsBinding? = null
    private val binding get() = _binding!!
    private lateinit var reportsAdapter: ReportsAdapter
    private val db = FirebaseFirestore.getInstance()
    private var teamReportsListener: ListenerRegistration? = null
    private var municipalityId: String? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTeamReportsBinding.inflate(inflater, container,
            false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Try navigation arguments first
        municipalityId = arguments?.getString("MUNICIPALITY_ID")

        // 2. If null, fall back to the activity intent (same as WorkerHomeFragment)
        if (municipalityId.isNullOrEmpty()) {
            municipalityId = requireActivity().intent.getStringExtra("MUNICIPALITY_ID")
        }

        // 3. Log the value for debugging
        android.util.Log.d("TeamReports", "Final Municipality ID: $municipalityId")

        reportsAdapter = ReportsAdapter(emptyList(), true) { report ->
            showVerifyModal(report)
        }

        binding.rvTeamReports.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = reportsAdapter
        }

        binding.teamSwipeRefresh.setOnRefreshListener {
            loadReports()
        }

        loadReports()
    }

    private fun loadReports() {
        if (municipalityId.isNullOrEmpty()) {
            handleNoTasks()
            return
        }
        listenToAssignedReports(municipalityId!!)
    }

    private fun handleNoTasks() {
        binding.teamSwipeRefresh.isRefreshing = false
        binding.tvNoTasks.visibility = View.VISIBLE
        binding.rvTeamReports.visibility = View.GONE
    }

    private fun listenToAssignedReports(municipalityId: String) {
        // Query based on the municipality field found in your reports
        teamReportsListener = db.collection("reports")
            .whereEqualTo("assignedTeamMunicipality", municipalityId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (_binding == null || !isAdded) return@addSnapshotListener
                binding.teamSwipeRefresh.isRefreshing = false

                if (error != null) {
                    android.util.Log.e("FirestoreError", "Error: ${error.message}")
                    // Show error to user (remove in production if desired)
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Failed to load tasks: ${error.message}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    binding.teamSwipeRefresh.isRefreshing = false
                    return@addSnapshotListener
                }

                val reportsList = value?.documents?.mapNotNull { doc ->
                    val report = doc.toObject(Report::class.java)
                    report?.id = doc.id
                    report
                }?.sortedWith(compareByDescending<Report> { it.status == "New" }
                    .thenByDescending { it.createdAt }) ?: emptyList() // NEW: Sort by status first, then date

                if (reportsList.isEmpty()) {
                    binding.tvNoTasks.visibility = View.VISIBLE
                    binding.rvTeamReports.visibility = View.GONE
                } else {
                    binding.tvNoTasks.visibility = View.GONE
                    binding.rvTeamReports.visibility = View.VISIBLE
                    reportsAdapter.updateData(reportsList)
                }
            }
    }

    private fun showVerifyModal(report: Report) {
        // 1. Inflate the custom modal layout
        val dialog = android.app.Dialog(requireContext())
        val modalBinding = ModalTeamActionBinding.inflate(layoutInflater)
        dialog.setContentView(modalBinding.root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.window?.let { window ->
            val params = window.attributes

            // 1. Shrink side spaces (Set width to 90% of screen width)
            params.width = (resources.displayMetrics.widthPixels * 0.90).toInt()

            // 2. Push to top by changing gravity and adding a small offset
            params.gravity = android.view.Gravity.TOP
            params.y = 150 // Distance from the top of the screen in pixels

            window.attributes = params
        }

        // 2. Load the photo (Reuse your Base64 decoding logic)
        report.photoURL?.let { base64 ->
            val clean = if (base64.contains(",")) base64.split(",")[1] else base64
            val bytes = android.util.Base64.decode(clean, android.util.Base64.DEFAULT)
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0,
                bytes.size)
            modalBinding.ivModalReportImg.setImageBitmap(bitmap)
        }

        // 3. Handle the "Verify" button click
        modalBinding.btnVerifyAction.setOnClickListener {
            modalBinding.btnVerifyAction.isEnabled = false
            modalBinding.btnVerifyAction.text = getString(R.string.updating)

            // Create a map for multiple field updates
            val updates = hashMapOf<String, Any>(
                "status" to "Collected", // Standardized status
                "collectedAt" to com.google.firebase.Timestamp.now() // NEW: Vital for dashboard stats
            )

            // Update the report document in Firestore
            db.collection("reports").document(report.id)
                .update(updates)
                .addOnSuccessListener {
                    if (_binding != null) {
                        dialog.dismiss()
                        android.widget.Toast.makeText(context, "Waste Collected",
                            android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    modalBinding.btnVerifyAction.isEnabled = true
                    modalBinding.btnVerifyAction.text = getString(R.string.confirm_collected)
                    android.util.Log.e("FirestoreError", "Update failed: ${e.message}")
                }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        teamReportsListener?.remove()
        _binding = null
    }
}
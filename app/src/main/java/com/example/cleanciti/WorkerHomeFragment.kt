package com.example.cleanciti

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cleanciti.databinding.FragmentWorkerHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*

class WorkerHomeFragment : Fragment() {

    private var _binding: FragmentWorkerHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // Listeners for real-time updates
    private var reportsListener: ListenerRegistration? = null
    private var activityListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkerHomeBinding.inflate(inflater, container, false)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        setupRecyclerView()
        fetchTeamDataAndStartListeners()

        return binding.root
    }

    private fun setupRecyclerView() {
        binding.rvRecentActivity.layoutManager = LinearLayoutManager(context)
        // Note: You will need to create a simple Adapter for your Recent Activity list
    }

    private fun fetchTeamDataAndStartListeners() {
        val userEmail = auth.currentUser?.email ?: return

        // 1. Identify which municipality this team belongs to
        db.collection("team")
            .whereEqualTo("email", userEmail)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val teamDoc = documents.documents[0]
                    val teamName = teamDoc.getString("assignedTeam") ?: "Team"
                    val municipality = teamDoc.getString("locationName") ?: "Unknown Area"
                    val mId = teamDoc.getString("municipalityId") ?: ""

                    // Update Header UI
                    binding.tvWelcomeTeam.text = getString(R.string.hello, teamName)
                    binding.tvMunicipalityHeader.text = municipality

                    // 2. Start listening for reports in THIS municipality
                    startReportsListener(mId)
                    startActivityListener(mId)
                }
            }
    }

    private fun startReportsListener(municipalityId: String) {
        // Query reports filtered by the routing ID
        val reportsQuery = db.collection("reports")
            .whereEqualTo("municipalityId", municipalityId)

        reportsListener = reportsQuery.addSnapshotListener { snapshots, e ->
            if (e != null || snapshots == null) return@addSnapshotListener

            var pending = 0
            var inProgress = 0

            for (doc in snapshots) {
                val status = doc.getString("status")
                if (status == "New") pending++
                if (status == "In-Progress") inProgress++
            }

            // Update Stats Cards
            binding.tvPendingCount.text = String.format("%02d", pending)
            binding.tvInProgressCount.text = String.format("%02d", inProgress)
        }
    }

    private fun startActivityListener(municipalityId: String) {
        // Query notifications meant for this team
        val activityQuery = db.collection("notifications")
            .whereEqualTo("targetTeamId", municipalityId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(10)

        activityListener = activityQuery.addSnapshotListener { snapshots, _ ->
            if (snapshots != null) {
                // Update your RecyclerView adapter here with the new list
                // val activities = snapshots.toObjects(NotificationModel::class.java)
                // adapter.submitList(activities)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        reportsListener?.remove()
        activityListener?.remove()
        _binding = null
    }
}
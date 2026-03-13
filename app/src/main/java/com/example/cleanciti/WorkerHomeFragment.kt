package com.example.cleanciti

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.cleanciti.databinding.FragmentWorkerHomeBinding
import com.google.firebase.firestore.*

class WorkerHomeFragment : Fragment() {

    private var _binding: FragmentWorkerHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: FirebaseFirestore
    private var reportsListener: ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWorkerHomeBinding.inflate(inflater, container, false)
        db = FirebaseFirestore.getInstance()

        // 1. Get the TEAM_ID passed from the Login Intent
        val teamLoginId = arguments?.getString("TEAM_ID")

        if (teamLoginId != null) {
            fetchTeamContextAndStats(teamLoginId)
        } else {
            // Fallback: check activity intent if arguments are null
            activity?.intent?.getStringExtra("TEAM_ID")?.let { fetchTeamContextAndStats(it) }
        }

        return binding.root
    }

    private fun fetchTeamContextAndStats(teamLoginId: String) {
        // 2. Find the team's municipality using their login ID
        db.collection("team")
            .whereEqualTo("assignedTeamId", teamLoginId)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val teamDoc = documents.documents[0]
                    val municipality = teamDoc.getString("municipalityArea") ?: "Unknown"
                    val municipalityId = teamDoc.getString("municipalityId") ?: ""
                    val teamName = teamDoc.getString("assignedTeam") ?: "Team"

                    // Update UI Header
                    binding.tvWelcomeTeam.text = "Hello, $teamName"
                    binding.tvMunicipalityHeader.text = municipality

                    // 3. Start real-time stats listener for THIS municipality
                    if (municipalityId.isNotEmpty()) {
                        startRealTimeStats(municipalityId)
                    }
                }
            }
    }

    private fun startRealTimeStats(mId: String) {
        // We query the "reports" collection where the municipality ID matches the team
        reportsListener = db.collection("reports")
            .whereEqualTo("assignedTeamMunicipality", mId)
            .addSnapshotListener { snapshots, _ ->
                if (snapshots != null) {
                    var pending = 0
                    var collected = 0
                    val total = snapshots.size()

                    for (doc in snapshots) {
                        val status = doc.getString("status")
                        if (status == "New") pending++
                        if (status == "Collected") collected++
                    }

                    // Update the Dashboard Cards
                    binding.tvPendingCount.text = String.format("%02d", pending)
                    binding.tvInProgressCount.text = String.format("%02d", collected)
                    binding.tvTotalCount.text = String.format("%02d", total)
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        reportsListener?.remove() // Cleanup
        _binding = null
    }
}
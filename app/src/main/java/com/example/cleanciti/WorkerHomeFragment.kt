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
    private var activityListener: ListenerRegistration? = null
    private lateinit var activityAdapter: ActivityAdapter

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


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        // Initialize the RecyclerView
        activityAdapter = ActivityAdapter(emptyList())
        binding.rvRecentActivity.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            adapter = activityAdapter
        }
    }

    private fun fetchTeamContextAndStats(teamLoginId: String) {
        //Find the team's municipality using their login ID
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
                    binding.tvWelcomeTeam.text = getString(R.string.hello, teamName)
                    binding.tvMunicipalityHeader.text = municipality

                    // Start real-time stats listener for THIS municipality
                    if (municipalityId.isNotEmpty()) {
                        startRealTimeStats(municipalityId)
                        startActivityListener(municipalityId)
                    }
                }
            }
    }

    private fun updateActivityList(activities: List<String>) {
        // Convert the string list to the notification model
        val notificationList = activities.map { activityStr ->
            val parts = activityStr.split(": ", limit = 2)
            ActivityNotification(
                title = parts.getOrElse(0) { "Alert" },
                message = parts.getOrElse(1) { "" }
            )
        }
        activityAdapter.updateData(notificationList)
    }

    private fun triggerSystemNotification(title: String, message: String) {
        // 1. Ensure the Intent is distinct to prevent OS bundling issues
        val intent = android.content.Intent(requireContext(), MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        // 2. Use a unique request code for the PendingIntent
        val pendingIntent = android.app.PendingIntent.getActivity(
            requireContext(),
            System.currentTimeMillis().toInt(), // Unique code prevents overwriting
            intent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 3. Build the notification with explicit visual flags
        val builder = androidx.core.app.NotificationCompat.Builder(requireContext(), "CLEANCITI_NOTIFS")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX) // MAX for intrusive display
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_MESSAGE) // Helps OS categorize priority
            .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC) // Shows on lockscreen
            .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = androidx.core.app.NotificationManagerCompat.from(requireContext())

        // 4. Final permission check before firing
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                requireContext(), android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            // Use a unique ID so they don't overwrite each other in the tray
            notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }

    private fun startActivityListener(mId: String) {
        activityListener = db.collection("notifications")
            .whereEqualTo("targetMunicipalityId", mId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { snapshots, e ->
                if (e != null || snapshots == null) return@addSnapshotListener

                // 1. Properly use the boolean to detect initial load vs live updates
                val isLiveUpdate = !snapshots.metadata.isFromCache && !snapshots.metadata.hasPendingWrites()

                for (dc in snapshots.documentChanges) {
                    // 2. Only trigger system notification for NEW live documents
                    if (dc.type == DocumentChange.Type.ADDED && isLiveUpdate) {
                        val title = dc.document.getString("title") ?: "New Alert"
                        val msg = dc.document.getString("message") ?: ""
                        triggerSystemNotification(title, msg)
                    }
                }

                val activities = snapshots.map { doc ->
                    "${doc.getString("title")}: ${doc.getString("message")}"
                }
                updateActivityList(activities)
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
        reportsListener?.remove()
        activityListener?.remove()
        _binding = null
    }
}
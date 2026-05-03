package com.example.cleanciti

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cleanciti.databinding.FragmentReportsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!  
    private lateinit var reportsAdapter: ReportsAdapter
    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private var reportsListener: ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState:
    Bundle?): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        reportsAdapter = ReportsAdapter(emptyList(), false){  }

        //RecyclerView
        binding.reportsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = reportsAdapter
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            fetchUserReports()
        }

        fetchUserReports()
    }

    private fun fetchUserReports() {
        if (userId == null) return

        reportsListener = db.collection("reports")
            .whereEqualTo("reporterId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                // SAFETY CHECK: If the fragment is gone, stop here!
                if (_binding == null || !isAdded) return@addSnapshotListener

                binding.swipeRefreshLayout.isRefreshing = false

                if (error != null) {
                    // Log the error so you can see if the Index is still building
                    android.util.Log.e("FirestoreError", "Error fetching reports",
                        error)
                    return@addSnapshotListener
                }

                //Manually map the Document ID to the Report object
                val reportsList = value?.documents?.mapNotNull { doc ->
                    val report = doc.toObject(Report::class.java)
                    report?.id = doc.id // This fills the @Exclude var id in your Data Class
                    report
                } ?: emptyList()

                //UI UPDATE LOGIC
                if (reportsList.isEmpty()) {
                    binding.emptyStateText.visibility = View.VISIBLE
                    binding.reportsRecyclerView.visibility = View.GONE
                } else {
                    binding.emptyStateText.visibility = View.GONE
                    binding.reportsRecyclerView.visibility = View.VISIBLE
                    reportsAdapter.updateData(reportsList)
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        reportsListener?.remove()
        _binding = null
    }
}
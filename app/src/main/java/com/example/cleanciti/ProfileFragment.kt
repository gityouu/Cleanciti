package com.example.cleanciti

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.TooltipCompat
import androidx.fragment.app.Fragment
import com.example.cleanciti.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance() // Access Firestore

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = FirebaseAuth.getInstance().currentUser
        val uid = user?.uid ?: ""

        // 1. Generate the 4-character ID from the UID
        val shortId = if (uid.length >= 4) uid.take(4).uppercase() else "USER"
        binding.profileUsername.text = getString(R.string.user_, shortId)
        binding.profilePhone.text = user?.phoneNumber

        // 2. Set the Tooltip on the Question Mark
        // This replaces the Toast with a native floating label
        TooltipCompat.setTooltipText(binding.infoIcon, "Username auto-generated from your unique ID")

        binding.infoIcon.setOnClickListener {
            // This triggers the tooltip immediately on click
            it.performLongClick()
        }

        // 3. Pull the Role dynamically from Firestore
        if (uid.isNotEmpty()) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // Get Role
                        val role = document.getString("role") ?: "Reporter"
                        binding.profileRole.text = role

                        // Get and Format Joining Date
                        val timestamp = document.getTimestamp("createdAt")
                        if (timestamp != null) {
                            val date = timestamp.toDate()
                            val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                            binding.profileJoinedDate.text =
                                getString(R.string.joined, sdf.format(date))
                        }
                    }
                }
        }

        // 4. Logout Logic
        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(requireContext(), AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
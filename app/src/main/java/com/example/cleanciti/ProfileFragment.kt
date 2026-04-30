package com.example.cleanciti

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.TooltipCompat
import androidx.fragment.app.Fragment
import com.example.cleanciti.databinding.FragmentProfileBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.core.graphics.toColorInt
import androidx.core.content.edit

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authStateListener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            // Update phone number based on current user
            binding.profilePhone.text = if (user?.isAnonymous == true) "Anonymous"
            else (user?.phoneNumber ?: "Unknown")
        }
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener)

        val user = FirebaseAuth.getInstance().currentUser
        val uid = user?.uid ?: ""

        //UI Setup: Auto-generated Username and Tooltip
        val shortId = if (uid.length >= 4) uid.take(4).uppercase() else "USER"
        binding.profileUsername.text = getString(R.string.user_, shortId)

        TooltipCompat.setTooltipText(binding.infoIcon, "Username auto-generated " +
                "from your unique ID")
        binding.infoIcon.setOnClickListener { it.performLongClick() }

        //dynamic user data fetching
        if (uid.isNotEmpty()) {
            fetchUserData(uid)
        }

        //Button Listeners
        binding.btnLogout.setOnClickListener { logout() }
        binding.btnDeleteAccount.setOnClickListener { showDeleteConfirmation() }
    }

    private fun fetchUserData(uid: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (_binding == null || !isAdded) return@addOnSuccessListener

                if (document.exists()) {
                    val isAnon = document.getBoolean("isAnonymous") ?: false
                    // Update UI based on anonymity
                    binding.profileRole.text = if (isAnon) "Guest Reporter" else "Verified Reporter"

                    val timestamp = document.getTimestamp("createdAt")
                    if (timestamp != null) {
                        val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                        binding.profileJoinedDate.text = getString(R.string.joined,
                            sdf.format(timestamp.toDate()))
                    }
                }
            }
    }

    private fun showDeleteConfirmation() {
        // Verification Modal before permanent deletion
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirm Account Deletion")
            .setMessage("Are you sure you want to do this? This action cannot be undone. " +
                    "Your reports will remain in the system, but your profile will be removed.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete Forever") { _, _ ->
                performAccountDeletion()
            }
            .show()
            .apply {
                // Color the delete button red to signal warning
                getButton(AlertDialog.BUTTON_POSITIVE).setTextColor("#ef4444"
                    .toColorInt())
            }
    }

    private fun performAccountDeletion() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val uid = user.uid

        //Deletion of Firestore document from 'users' collection only
        db.collection("users").document(uid).delete()
            .addOnSuccessListener { proceedToAuthDeletion(user) }
            .addOnFailureListener { proceedToAuthDeletion(user) }
    }

    private fun proceedToAuthDeletion(user: com.google.firebase.auth.FirebaseUser) {
        user.delete().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // CRITICAL: Clear the persistent ID so a new one can be created later
                val sharedPrefs = requireContext().getSharedPreferences("CleanCitiPrefs",
                    android.content.Context.MODE_PRIVATE)
                sharedPrefs.edit { remove("persistent_uid") }

                Toast.makeText(context, "Account Deleted Successfully",
                    Toast.LENGTH_SHORT).show()
                navigateToAuth()
            } else {
                Toast.makeText(context, "Please re-login to delete.",
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun isAnonymous(): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return false
        return user.isAnonymous
    }

    private fun logout() {
        if (isAnonymous()) {
            // Anonymous users cannot log out without losing their account.
            // Offer to delete the account permanently instead.
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cannot Log Out")
                .setMessage("Anonymous reporters cannot log out because your account would be " +
                        "lost permanently. Use 'Delete Account' if you wish to remove your profile " +
                        "completely.")
                .setPositiveButton("OK", null)
                .show()
            return
        }
        // For phone-authenticated users, normal sign out
        FirebaseAuth.getInstance().signOut()
        navigateToAuth()
    }

    private fun navigateToAuth() {
        val intent = Intent(requireContext(), AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        activity?.finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
        _binding = null
    }
}
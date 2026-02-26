package com.example.cleanciti

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import com.example.cleanciti.databinding.FragmentHomeBinding
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream

class HomeFragment : Fragment() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var bse64Image: String? = null
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val requestLocationPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            getLocation()
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val bundle = result.data?.extras

            val imageBitmap = bundle?.let {
                BundleCompat.getParcelable(it, "data", Bitmap::class.java)
            }

            imageBitmap?.let {
                binding.photoPreview.setImageBitmap(imageBitmap)
                binding.photoPreview.visibility = View.VISIBLE
                binding.placeholderLayout.visibility = View.GONE
                bse64Image = encodeImageToBase64(it)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        requestLocationPermission.launch(arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ))

        setupSpinner()
        setupClickListeners()
    }

    private fun setupSpinner() {
        val categories = arrayOf("Overflowing Bin", "Illegal Dumping", "Littering", "Hazardous Waste", "Other")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories)
        binding.categorySpinner.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.photoUploadFrame.setOnClickListener {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraLauncher.launch(cameraIntent)
        }

        binding.submitReportBtn.setOnClickListener {
            val desc = binding.reportDescription.text.toString().trim()
            val category = binding.categorySpinner.selectedItem.toString()
            val userId = auth.currentUser?.uid

            if (bse64Image == null) {
                Toast.makeText(requireContext(), "Please provide a photo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (userId != null) {
                submitReport(userId, category, desc)
            }
        }
    }

    private fun submitReport(userId: String, category: String, desc: String) {
        binding.submitReportBtn.isEnabled = false
        binding.submitReportBtn.text = "Submitting..."

        val metadataRef = db.collection("metadata").document("reports_stats")
        val reportsRef = db.collection("reports").document()

        db.runTransaction { transaction ->
            val snapshot = transaction.get(metadataRef)
            val lastNum = if (snapshot.exists()) snapshot.getLong("last_report_number") ?: 0 else 0
            val nextNum = lastNum + 1

            val reportData = hashMapOf(
                "reportNumber" to nextNum.toString(),
                "reporterId" to userId,
                "category" to category,
                "description" to desc,
                "status" to "New",
                "photoURL" to bse64Image,
                "location" to hashMapOf(
                    "lat" to currentLatitude,
                    "lng" to currentLongitude
                ),
                "createdAt" to com.google.firebase.Timestamp.now()
            )

            transaction.set(reportsRef, reportData)
            transaction.set(metadataRef, hashMapOf("last_report_number" to nextNum))

            "CC-$nextNum"
        }.addOnSuccessListener { _ ->
            Toast.makeText(requireContext(), "Report Submitted!", Toast.LENGTH_SHORT).show()
            resetUI()
        }.addOnFailureListener { e ->
            binding.submitReportBtn.isEnabled = true
            binding.submitReportBtn.text = "Submit Report"
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLatitude = location.latitude
                currentLongitude = location.longitude

                binding.locationText.text = "Accra, GH • ${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)}"
            }
        }
    }

    private fun resetUI() {
        binding.reportDescription.text.clear()
        binding.photoPreview.visibility = View.GONE
        binding.placeholderLayout.visibility = View.VISIBLE
        binding.submitReportBtn.isEnabled = true
        binding.submitReportBtn.text = "Submit Report"
        bse64Image = null
    }

    private fun encodeImageToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        val byteArray = outputStream.toByteArray()
        val rawBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT)

        return "data:image/jpeg;base64,$rawBase64"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null //prevent memory leaks
    }
}
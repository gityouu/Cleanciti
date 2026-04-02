package com.example.cleanciti

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.cleanciti.databinding.ItemReportBinding

class ReportsAdapter(private var reports: List<Report>) :
    RecyclerView.Adapter<ReportsAdapter.ReportViewHolder>() {

    class ReportViewHolder(val binding: ItemReportBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val binding = ItemReportBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ReportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val report = reports[position]

        holder.binding.apply {
            itemCategory.text = report.category
            itemID.text = "#${report.reportNumber}"
            itemStatus.text = report.status

            val statusColor = when (report.status) {
                "New" -> "#f97316"       // Orange
                "Collected" -> "#13ec49" // Green
                else -> "#94a3b8"        // Gray (Default)
            }
            itemStatus.setTextColor(android.graphics.Color.parseColor(statusColor))

            // Decode Base64 string to Bitmap for the thumbnail
            report.photoURL?.let { base64String ->
                val cleanBase64 = if (base64String.contains(",")) {
                    base64String.split(",")[1] // Remove the "data:image/jpeg;base64," prefix
                } else {
                    base64String
                }

                try {
                    val imageBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                    val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    reportThumbnail.setImageBitmap(decodedImage)
                } catch (e: Exception) {
                    reportThumbnail.setImageResource(R.drawable.ic_add_circle) // Fallback icon
                }
            }
        }
    }

    override fun getItemCount() = reports.size

    // Function to update data when Firestore sends new results
    fun updateData(newReports: List<Report>) {
        val diffCallback = ReportDiffCallback(this.reports, newReports)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.reports = newReports
        diffResult.dispatchUpdatesTo(this)
    }

    // Helper class to calculate changes
    class ReportDiffCallback(
        private val oldList: List<Report>,
        private val newList: List<Report>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size
        override fun areItemsTheSame(oldPos: Int, newPos: Int) =
            oldList[oldPos].reportNumber == newList[newPos].reportNumber
        override fun areContentsTheSame(oldPos: Int, newPos: Int) =
            oldList[oldPos] == newList[newPos]
    }
}
package com.example.cleanciti

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.cleanciti.databinding.ItemTeamReportsBinding
import com.example.cleanciti.databinding.ItemReportBinding
import androidx.core.graphics.toColorInt

class ReportsAdapter(
    private var reports: List<Report>,
    private val isTeamView: Boolean,
    private var onCollectClicked: (Report) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // Define ViewTypes
    private val vIEWTYPEREPORTER = 0
    private val vIEWTYPETEAM = 1

    override fun getItemViewType(position: Int): Int {
        return if (isTeamView) vIEWTYPETEAM else vIEWTYPEREPORTER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == vIEWTYPETEAM) {
            val binding = ItemTeamReportsBinding.inflate(inflater, parent, false)
            TeamViewHolder(binding)
        } else {
            val binding = ItemReportBinding.inflate(inflater, parent, false)
            ReporterViewHolder(binding)
        }
    }

    // Two different ViewHolders
    class ReporterViewHolder(val binding: ItemReportBinding) : RecyclerView.ViewHolder(
        binding.root)
    class TeamViewHolder(val binding: ItemTeamReportsBinding) : RecyclerView.ViewHolder(
        binding.root)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val report = reports[position]

        // Inside onBindViewHolder in ReportsAdapter.kt
        if (holder is TeamViewHolder) {
            holder.binding.apply {
                tvCategory.text = report.category
                tvLocationName.text = report.locationName

                //Differentiate by Color
                val isFinished = report.status == "Collected"
                val statusColor = if (isFinished) "#13ec49" else "#2b7fff"//Green for finished,Blue for New
                statusStripe.setBackgroundColor(statusColor.toColorInt())

                // Hide/Show Collect Button
                if (isFinished) {
                    btnAction.visibility = android.view.View.VISIBLE
                    btnAction.text = R.string.collected.toString()
                    btnAction.isEnabled = false
                    btnAction.setTextColor("#94a3b8".toColorInt())
                } else {
                    btnAction.visibility = android.view.View.VISIBLE
                    btnAction.setOnClickListener { onCollectClicked(report) }
                }

                decodeImage(report.photoURL, ivReportImg)
            }
        } else if (holder is ReporterViewHolder) {
            holder.binding.apply {
                itemCategory.text = report.category
                itemID.text = "Report #${report.reportNumber}"
                itemStatus.text = report.status
                decodeImage(report.photoURL, reportThumbnail)
            }
        }
    }

    private fun decodeImage(base64: String?, imageView: android.widget.ImageView) {
        base64?.let {
            val clean = if (it.contains(",")) it.split(",")[1] else it
            try {
                val bytes = Base64.decode(clean, Base64.DEFAULT)
                imageView.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0,
                    bytes.size))
            } catch (_: Exception) { imageView.setImageResource(R.drawable.ic_help_outline) }
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
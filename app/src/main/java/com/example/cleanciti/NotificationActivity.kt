package com.example.cleanciti

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

data class ActivityNotification(
    val title: String = "",
    val message: String = "",
    val timestamp: com.google.firebase.Timestamp? = null
)


class ActivityAdapter(private var activities: List<ActivityNotification>) :
    androidx.recyclerview.widget.RecyclerView.Adapter<ActivityAdapter.ViewHolder>() {

    class ViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        val titleTv: android.widget.TextView = view.findViewById(android.R.id.text1)
        val msgTv: android.widget.TextView = view.findViewById(android.R.id.text2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val activity = activities[position]
        holder.titleTv.text = activity.title
        holder.msgTv.text = activity.message
    }

    override fun getItemCount() = activities.size

    fun updateData(newActivities: List<ActivityNotification>) {
        this.activities = newActivities
        notifyDataSetChanged()
    }
}
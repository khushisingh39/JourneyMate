package com.example.journeymatesample

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.journeymatesample.R
import com.example.journeymatesample.Trip

class TripHistoryAdapter(private val tripList: List<Trip>) :
    RecyclerView.Adapter<TripHistoryAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textTripDetails: TextView = itemView.findViewById(R.id.textTripDetails)
        val textDate: TextView = itemView.findViewById(R.id.textDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val trip = tripList[position]
        holder.textTripDetails.text = "${trip.startLocation} to ${trip.destination}"
        holder.textDate.text = "Date: ${trip.date}"
    }

    override fun getItemCount(): Int {
        return tripList.size
    }
}

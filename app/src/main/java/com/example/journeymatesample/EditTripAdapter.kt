package com.example.journeymatesample

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EditTripAdapter(private val tripList: List<Trip>, private val onEditClick: (Trip) -> Unit) :
    RecyclerView.Adapter<EditTripAdapter.TripViewHolder>() {

    class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tripTitle: TextView = itemView.findViewById(R.id.tripTitle)
        val tripSeats: TextView = itemView.findViewById(R.id.tripSeats)
        val tripFare: TextView = itemView.findViewById(R.id.tripFare)
        val btnEdit: Button = itemView.findViewById(R.id.btnEditTrip)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.trip_item, parent, false)
        return TripViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val trip = tripList[position]
        holder.tripTitle.text = "${trip.startLocation} ➝ ${trip.destination}"
        holder.tripSeats.text = "Seats: ${trip.availableSeats}/${trip.totalSeats}"
        holder.tripFare.text = "Fare: ₹${trip.totalFare}"

        // ✅ When "Edit" is clicked, send the correct trip object
        holder.btnEdit.setOnClickListener {
            Log.d("EditTripAdapter", "Editing Trip ID: ${trip.tripId}") // Debugging Log
            onEditClick(trip)  // Send the trip object to `UserTripsActivity`
        }
    }


    override fun getItemCount() = tripList.size
}

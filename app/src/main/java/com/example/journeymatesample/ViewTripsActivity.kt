package com.example.journeymatesample

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class ViewTripsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tripAdapter: TripAdapter
    private var tripList: MutableList<Trip> = mutableListOf()
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var tripsRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_trips)

        recyclerView = findViewById(R.id.recyclerViewTrips)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize Firebase
        firebaseDatabase = FirebaseDatabase.getInstance()
        tripsRef = firebaseDatabase.getReference("Trips")

        // Get search filters from the intent
        val startLocation = intent.getStringExtra("startLocation") ?: ""
        val destination = intent.getStringExtra("destination") ?: ""
        val date = intent.getStringExtra("date") ?: ""

        Log.d("ViewTripsActivity", "Filters received - StartLocation: $startLocation, Destination: $destination, Date: $date")

        // Initialize the trip list and adapter
        tripAdapter = TripAdapter(this, tripList) { selectedTrip ->
            openTripDetails(selectedTrip)  // This will open the trip details when the item is clicked
        }
        recyclerView.adapter = tripAdapter

        // Fetch trips based on search filters
        fetchFilteredTrips(startLocation, destination, date)
    }


    private fun fetchFilteredTrips(startLocation: String, destination: String, date: String) {
        Log.d("ViewTripsActivity", "Fetching trips with filters: StartLocation='$startLocation', Destination='$destination', Date='$date'")

        tripsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                tripList.clear()

                if (snapshot.exists()) {
                    Log.d("ViewTripsActivity", "✅ Data exists in Firebase. Found ${snapshot.childrenCount} records.")

                    for (tripSnapshot in snapshot.children) {
                        val trip = tripSnapshot.getValue(Trip::class.java)

                        if (trip != null) {
                            val tripId = tripSnapshot.key // Get the Firebase-generated ID
                            val tripWithId = trip.copy(tripId = tripId) // Ensure tripid is set
                            Log.d("ViewTripsActivity", "🔹 Trip fetched: ID='${tripSnapshot.key}', Start='${trip.startLocation}', Destination='${trip.destination}', Date='${trip.date}'")

                            // Trim and compare values
                            val isStartLocationMatch = trip.startLocation.trim().equals(startLocation.trim(), ignoreCase = true)
                            val isDestinationMatch = trip.destination.trim().equals(destination.trim(), ignoreCase = true)
                            val isDateMatch = trip.date.trim().equals(date.trim(), ignoreCase = true)

                            Log.d("ViewTripsActivity", "🛠 Checking filters:")
                            Log.d("ViewTripsActivity", "   ➜ Start Location Match: '$startLocation' == '${trip.startLocation.trim()}' ➝ $isStartLocationMatch")
                            Log.d("ViewTripsActivity", "   ➜ Destination Match: '$destination' == '${trip.destination.trim()}' ➝ $isDestinationMatch")
                            Log.d("ViewTripsActivity", "   ➜ Date Match: '$date' == '${trip.date.trim()}' ➝ $isDateMatch")

                            // If all filters match, add to list
                            if (isStartLocationMatch && isDestinationMatch && isDateMatch) {
                                tripList.add(tripWithId)
                                Log.d("ViewTripsActivity", "✅ Trip added to list: $trip")
                            } else {
                                Log.d("ViewTripsActivity", "❌ Trip does not match filters. Skipping.")
                            }
                        } else {
                            Log.d("ViewTripsActivity", "⚠️ Trip is null. Skipping.")
                        }
                    }

                    if (tripList.isEmpty()) {
                        Log.d("ViewTripsActivity", "❌ No trips matched the filters.")
                        Toast.makeText(this@ViewTripsActivity, "No trips found!", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.d("ViewTripsActivity", "✅ ${tripList.size} trips matched and added to the list.")
                    }

                    tripAdapter.notifyDataSetChanged()
                } else {
                    Log.d("ViewTripsActivity", "❌ No trip data exists in Firebase.")
                    Toast.makeText(this@ViewTripsActivity, "No trips available in the database.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ViewTripsActivity", "🔥 Database error: ${error.message}")
                Toast.makeText(this@ViewTripsActivity, "Error: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
    }



    private fun openTripDetails(trip: Trip) {
        val tripId = trip.tripId ?: "" // Ensure non-null trip ID
        val intent = Intent(this, TripActivity::class.java)
        intent.putExtra("tripId", trip.tripId) // Pass trip ID for further use
        Log.d("ViewTripsActivity", "Opening trip details for tripId: ${trip.tripId}")
        startActivity(intent)
    }
}

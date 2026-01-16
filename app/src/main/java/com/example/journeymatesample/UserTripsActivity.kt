package com.example.journeymatesample

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class UserTripsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tripAdapter: EditTripAdapter
    private lateinit var tripList: MutableList<Trip>
    private lateinit var database: DatabaseReference
    private var userPhoneNumber: String? = null
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_trips)

        recyclerView = findViewById(R.id.recyclerViewTrips)
        recyclerView.layoutManager = LinearLayoutManager(this)
        tripList = mutableListOf()

        tripAdapter = EditTripAdapter(tripList) { trip ->
            openEditTripScreen(trip)
        }
        recyclerView.adapter = tripAdapter

        userPhoneNumber = getLoggedInUserPhone()

        if (userPhoneNumber != null) {
            fetchUserIdFromUsersTable(userPhoneNumber!!)
        } else {
            Toast.makeText(this, "Error: No logged-in user found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getLoggedInUserPhone(): String? {
        val pref = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        return pref.getString("user_phone", null)
    }

    private fun fetchUserIdFromUsersTable(phoneNumber: String) {
        val usersRef = FirebaseDatabase.getInstance().getReference("users").child(phoneNumber)

        usersRef.child("user_id").get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                userId = snapshot.value.toString()
                Log.d("UserTripsActivity", "✅ User ID found: $userId")
                fetchUserTrips(userId!!)
            } else {
                Toast.makeText(this, "User ID not found!", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Log.e("UserTripsActivity", "❌ Error fetching user ID: ${e.message}")
        }
    }

    private fun fetchUserTrips(userId: String) {
        database = FirebaseDatabase.getInstance().getReference("Trips")
        database.orderByChild("initiatorId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    tripList.clear()
                    for (tripSnapshot in snapshot.children) {
                        val trip = tripSnapshot.getValue(Trip::class.java)
                        val tripKey = tripSnapshot.key  // ✅ Firebase-generated trip ID

                        if (trip != null && tripKey != null) {
                            trip.tripId = tripKey  // ✅ Ensure tripId is set
                            Log.d("UserTripsActivity", "Fetched trip: $tripKey for user: $userId")
                            tripList.add(trip)
                        } else {
                            Log.e("UserTripsActivity", "❌ Error: trip or tripKey is null")
                        }
                    }

                    if (tripList.isEmpty()) {
                        Toast.makeText(this@UserTripsActivity, "No trips found!", Toast.LENGTH_SHORT).show()
                    }
                    tripAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("UserTripsActivity", "Error fetching trips: ${error.message}")
                }
            })
    }


    private fun openEditTripScreen(trip: Trip) {
        if (trip.tripId.isNullOrEmpty()) {
            Log.e("UserTripsActivity", "❌ Error: tripId is null or empty")
            Toast.makeText(this, "Error: Trip ID is missing!", Toast.LENGTH_SHORT).show()
            return
        }

        // ✅ Store tripId in SharedPreferences
        val pref = getSharedPreferences("MyPrefs", MODE_PRIVATE).edit()
        pref.putString("trip_id", trip.tripId)
        pref.apply()

        Log.d("UserTripsActivity", "Saved Trip ID: ${trip.tripId} in SharedPreferences")


        startActivity(Intent(this, EditTripActivity::class.java))
    }

}

package com.example.journeymatesample

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.google.firebase.auth.FirebaseAuth

class BookingRequestsActivity : AppCompatActivity() {

    private lateinit var requestsRef: DatabaseReference
    private lateinit var tripRef: DatabaseReference
    private lateinit var userId: String // Store current user ID
    private lateinit var recyclerViewRequests: RecyclerView // ✅ Declare RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_requests)

        // ✅ Initialize RecyclerView
        recyclerViewRequests = findViewById(R.id.recyclerViewRequests)
        recyclerViewRequests.layoutManager = LinearLayoutManager(this)

        requestsRef = FirebaseDatabase.getInstance().getReference("BookingRequests")
        tripRef = FirebaseDatabase.getInstance().getReference("Trips")
        // Dynamically set userId using FirebaseAuth
        val firebaseAuth = FirebaseAuth.getInstance()
//        userId = firebaseAuth.currentUser?.uid ?: run{
//            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
//            finish() // Optionally, close the activity if user is not logged in
//            return
//        }

        userId = getCurrentUserId()?:""

        loadBookingRequests()
    }

    private fun getCurrentUserId(): String?{
        val pref = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        return  pref.getString("user_id","")
    }

    private fun getCurrentUserPhone(): String?{
        val pref = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        return  pref.getString("user_phone","")
    }

    private fun loadBookingRequests() {
        requestsRef.orderByChild("initiatorId").equalTo(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val requestList = mutableListOf<BookingRequest>()

                for (requestSnapshot in snapshot.children) {
                    val request = requestSnapshot.getValue(BookingRequest::class.java)
                    if (request != null && request.status == "pending") {
                        requestList.add(request)
                    }
                }

                if (requestList.isEmpty()) {
                    Toast.makeText(this@BookingRequestsActivity, "No pending requests", Toast.LENGTH_SHORT).show()
                } else {
                    fetchAdditionalDetails(requestList)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("BookingRequestsActivity", "Error loading requests: ${error.message}")
            }
        })
    }

    private fun fetchAdditionalDetails(requestList: MutableList<BookingRequest>) {
        val database = FirebaseDatabase.getInstance()
        val usersRef = database.getReference("users") // Reference to users
        val tripsRef = database.getReference("Trips") // Reference to trips
        val updatedRequests = mutableListOf<BookingRequest>() // Store updated request data

        for (request in requestList) {
            // Query users where user_id matches riderId
            usersRef.orderByChild("user_id").equalTo(request.riderId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(userSnapshot: DataSnapshot) {
                        var riderName = "Unknown User" // Default value

                        if (userSnapshot.exists()) {
                            for (user in userSnapshot.children) { // Get the first matching user
                                riderName = user.child("name").getValue(String::class.java) ?: "Unknown User"
                                break
                            }
                        }

                        // Fetch trip details
                        tripsRef.child(request.tripId).addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(tripSnapshot: DataSnapshot) {
                                val trip = tripSnapshot.getValue(Trip::class.java)
                                if (trip != null) {
                                    val tripDetails = "${trip.startLocation} to ${trip.destination}"
                                    request.riderName = riderName // Replace riderId with actual name
                                    request.tripName = tripDetails // Replace tripId with actual trip details
                                    updatedRequests.add(request)
                                }

                                // Update RecyclerView once all data is fetched
                                recyclerViewRequests.adapter = BookingRequestAdapter(updatedRequests, this@BookingRequestsActivity)
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("BookingRequestsActivity", "Error loading trip details: ${error.message}")
                            }
                        })
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("BookingRequestsActivity", "Failed to fetch user: ${error.message}")
                    }
                })
        }
    }


}

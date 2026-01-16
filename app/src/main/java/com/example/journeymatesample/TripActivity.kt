package com.example.journeymatesample

import android.os.Bundle
import android.util.Log
import android.content.Context
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.android.volley.Response
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.auth.oauth2.GoogleCredentials
import org.json.JSONObject
import java.io.InputStream
import java.io.IOException




class TripActivity : AppCompatActivity() {

    private lateinit var tvTripTitle: TextView
    private lateinit var tvTripDetails: TextView
    private lateinit var btnBookTrip: Button
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var tripsRef: DatabaseReference
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip)

        // Initialize UI components
        tvTripTitle = findViewById(R.id.tvTripTitle)
        tvTripDetails = findViewById(R.id.tvTripDetails)
        btnBookTrip = findViewById(R.id.btnBookTrip)

        // Initialize Firebase references
        firebaseDatabase = FirebaseDatabase.getInstance()
        tripsRef = firebaseDatabase.getReference("Trips")
        firebaseAuth = FirebaseAuth.getInstance()

        // Fetch trip ID passed from the previous activity
        val tripId = intent.getStringExtra("tripId")
        if (tripId != null) {
            fetchTripDetails(tripId)
        } else {
            Toast.makeText(this, "Trip ID is missing.", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Booking button logic
        btnBookTrip.setOnClickListener {
            val userId = getCurrentUserId();
            val tripId = intent.getStringExtra("tripId") ?: ""

            if (userId != null) {
                val tripTitle = tvTripTitle.text.toString()

                if (tripTitle.isNotEmpty() && tripId.isNotEmpty()) {
                    bookTrip(tripId, tripTitle, userId)  // Pass the tripId as well
                    btnBookTrip.text = "Pending Approval"
                    btnBookTrip.isEnabled = false  // Disable button after request is sent
                } else {
                    Toast.makeText(this, "Trip details not loaded yet.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "You need to log in to book a trip.", Toast.LENGTH_SHORT).show()
            }
        }



    }

    private fun getCurrentUserId(): String?{
        val pref = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        return  pref.getString("user_id","")
    }

    private fun fetchTripDetails(tripId: String) {
        val userId = firebaseAuth.currentUser?.uid ?: return

        if (tripId.isNullOrEmpty()) {
            Log.e("TripActivity", "❌ tripId is null or empty")
            Toast.makeText(this, "Trip ID is missing.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Log.d("TripActivity", "Fetching trip details for tripId: $tripId")

        tripsRef.child(tripId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val trip = snapshot.getValue(Trip::class.java)
                    if (trip != null) {
                        displayTripDetails(trip)
                        Log.d("TripActivity", "✅ Trip details loaded successfully: $trip")

                        // Check if the current user has a booking request for this trip
                        checkBookingRequestStatus(tripId, userId)
                    } else {
                        tvTripDetails.text = "Trip details not found."
                        Log.e("TripActivity", "❌ Trip data is null")
                    }
                } else {
                    tvTripDetails.text = "Trip not found."
                    Log.e("TripActivity", "❌ No trip found with ID: $tripId")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@TripActivity, "Error fetching trip: ${error.message}", Toast.LENGTH_LONG).show()
                Log.e("TripActivity", "Firebase Error: ${error.message}")
            }
        })
    }

    /**
     * Function to check if the user has an existing booking request for the trip
     * and update the UI accordingly (Pending / Approved).
     */
    private fun checkBookingRequestStatus(tripId: String, userId: String) {
        val requestRef = firebaseDatabase.getReference("BookingRequests")

        requestRef.orderByChild("tripId").equalTo(tripId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (requestSnapshot in snapshot.children) {
                        val request = requestSnapshot.getValue(BookingRequest::class.java)
                        if (request != null && request.riderId == userId) {
                            when (request.status) {
                                "pending" -> {
                                    btnBookTrip.text = "Pending Approval"
                                    btnBookTrip.isEnabled = false
                                }
                                "accepted" -> {
                                    btnBookTrip.text = "Approved"
                                    btnBookTrip.isEnabled = false
                                }
                            }
                            return // Exit loop once the user's request is found
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("TripActivity", "Error checking booking status: ${error.message}")
                }
            })
    }


    private fun displayTripDetails(trip: Trip) {
        val tripTitle = "${trip.startLocation} to ${trip.destination}"
        val perPersonCost = if (trip.totalSeats > 0) trip.totalFare / trip.totalSeats else 0.0
        val tripDetails = """
             Departure: ${trip.startTime}
                            Date: ${trip.date}
                            Seats Available: ${trip.availableSeats}
                            Cost: ₹${trip.totalFare} per seat
                            Car Type: ${trip.cartype}
        """.trimIndent()

        tvTripTitle.text = tripTitle
        tvTripDetails.text = tripDetails
    }

    private fun bookTrip(tripId: String, tripTitle: String, userId: String) {
        val bookingsRef = firebaseDatabase.getReference("BookingRequests")
        val tripRef = firebaseDatabase.getReference("Trips").child(tripId)

        tripRef.child("initiatorId").get().addOnSuccessListener { snapshot ->
            val initiatorId = snapshot.getValue(String::class.java)
            if (initiatorId != null) {
                val requestId = bookingsRef.push().key ?: return@addOnSuccessListener
                val bookingRequest = BookingRequest(
                    requestId = requestId,
                    tripId = tripId,
                    riderId = userId,
                    initiatorId = initiatorId,
                    status = "pending"
                )

                bookingsRef.child(requestId).setValue(bookingRequest).addOnSuccessListener {
                    val userTokenRef = firebaseDatabase.getReference("users").child(initiatorId).child("fcmToken")
                    userTokenRef.get().addOnSuccessListener { tokenSnapshot ->
                        val initiatorToken = tokenSnapshot.getValue(String::class.java)
                        Log.d("FCM", "Fetched Token: $initiatorToken")  // Debug log
                        if (!initiatorToken.isNullOrEmpty()) {
                            sendNotificationToUser(this, initiatorToken, "New Booking Request", "You have a new booking request for $tripTitle.")
                        } else {
                            Log.e("FCM", "❌ Initiator's FCM Token is null or empty")
                        }
                    }.addOnFailureListener {
                        Log.e("FCM", "❌ Failed to fetch initiator's FCM token", it)
                    }

                }
            }
        }
    }

    private fun sendNotificationToUser(context: Context, token: String, title: String, message: String) {
        val fcmUrl = "https://fcm.googleapis.com/v1/projects/genericbooking-1c908/messages:send"
        val accessToken = getAccessToken(context) ?: return

        val jsonPayload = JSONObject().apply {
            put("message", JSONObject().apply {
                put("token", token)
                put("notification", JSONObject().apply {
                    put("title", title)
                    put("body", message)
                })
            })
        }

        val request = object : StringRequest(
            Request.Method.POST, fcmUrl,
            Response.Listener { response ->
                Log.d("FCM", "Notification Sent Successfully: $response")
            },
            Response.ErrorListener { error ->
                Log.e("FCM", "Notification Failed: ${error.message}")
            }
        ) {
            override fun getHeaders(): Map<String, String> {
                return mapOf(
                    "Authorization" to "Bearer $accessToken",
                    "Content-Type" to "application/json"
                )
            }

            override fun getBody(): ByteArray {
                return jsonPayload.toString().toByteArray(Charsets.UTF_8)
            }
        }

        Volley.newRequestQueue(context).add(request)
    }

    private fun getAccessToken(context: Context): String? {
        return try {
            val inputStream: InputStream = context.assets.open("service_account.json")
            val credentials = GoogleCredentials.fromStream(inputStream)
                .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
            credentials.refreshIfExpired()
            credentials.accessToken.tokenValue
        } catch (e: IOException) {
            Log.e("FCM", "Error obtaining access token: ${e.message}")
            null
        }
    }
    private fun moveTripToHistory(tripId: String) {
        val database = FirebaseDatabase.getInstance()
        val tripsRef = database.getReference("Trips").child(tripId)
        val tripHistoryRef = database.getReference("TripHistory")

        tripsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val trip = snapshot.getValue(Trip::class.java)
                if (trip != null) {
                    val userId = trip.initiatorId // Or use the current user's ID
                    tripHistoryRef.child(userId).child(tripId).setValue(trip)
                        .addOnSuccessListener {
                            Log.d("Firebase", "Trip moved to history successfully!")
                        }
                        .addOnFailureListener {
                            Log.e("Firebase", "Failed to move trip to history")
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error fetching trip details: ${error.message}")
            }
        })
    }


}

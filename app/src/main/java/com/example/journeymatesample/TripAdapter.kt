package com.example.journeymatesample

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream

class TripAdapter(
    private val context: Context,
    private val tripList: List<Trip>,
    private val onItemClick: (Trip) -> Unit
) : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_trip, parent, false)
        return TripViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val trip = tripList[position]
        holder.bind(trip)
    }

    override fun getItemCount(): Int = tripList.size

    inner class TripViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTripTitle: TextView = view.findViewById(R.id.tvTripTitle)
        private val tvTripDetails: TextView = view.findViewById(R.id.tvTripDetails)
        private val btnBookTrip: Button = view.findViewById(R.id.btnBookTrip)

        fun bind(trip: Trip) {
            tvTripTitle.text = "${trip.startLocation} to ${trip.destination}"
            tvTripDetails.text = """
                Departure: ${trip.startTime}
                Date: ${trip.date}
                Seats Available: ${trip.availableSeats}
                Cost: ₹${trip.totalFare} per seat
            """.trimIndent()

            btnBookTrip.setOnClickListener {
                bookTrip(trip)
            }

            itemView.setOnClickListener {
                onItemClick(trip)
            }
        }

        private fun getCurrentUserId(): String?{
            val pref = context.getSharedPreferences("MyPrefs", MODE_PRIVATE)
            return  pref.getString("user_id","")
        }

        private fun bookTrip(trip: Trip) {
            val database = FirebaseDatabase.getInstance()
            val bookingRequestsRef = database.getReference("BookingRequests")
            val fcmTokenRef = database.getReference("users").child(trip.initiatorId).child("fcmToken")

            val userId = getCurrentUserId() ?: return
            val requestId = bookingRequestsRef.push().key ?: return

            val bookingRequest = mapOf(
                "requestId" to requestId,
                "tripId" to trip.tripId,
                "riderId" to userId,
                "initiatorId" to trip.initiatorId,
                "status" to "pending"
            )

            bookingRequestsRef.child(requestId).setValue(bookingRequest)
                .addOnSuccessListener {
                fcmTokenRef.get().addOnSuccessListener { snapshot ->
                    val initiatorFcmToken = snapshot.getValue(String::class.java)
                    if (initiatorFcmToken != null) {
                        sendNotificationToUser(context, initiatorFcmToken, "New Booking Request!", "You have a new booking request for ${trip.startLocation} to ${trip.destination}.")
                    }
                }
            }
                .addOnFailureListener { e ->
                Toast.makeText(context,"${e.stackTraceToString()}",Toast.LENGTH_SHORT).show()
            }
        }

        private fun sendNotificationToUser(context: Context, token: String, title: String, message: String) {
            CoroutineScope(Dispatchers.IO).launch {
                val fcmUrl = "https://fcm.googleapis.com/v1/projects/genericbooking-1c908/messages:send"
                val accessToken = getAccessToken(context) ?: return@launch

                val jsonPayload = JSONObject().apply {
                    put("message", JSONObject().apply {
                        put("token", token)
                        put("notification", JSONObject().apply {
                            put("title", title)
                            put("body", message)
                        })
                        put("data", JSONObject().apply {
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
    }
}

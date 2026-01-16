package com.example.journeymatesample

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.google.firebase.database.FirebaseDatabase
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.database.DataSnapshot
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import com.google.firebase.database.Transaction
import com.google.firebase.database.MutableData
import com.google.firebase.database.DatabaseError



class BookingRequestAdapter(
    private val requestList: List<BookingRequest>,
    private val context: Context
) : RecyclerView.Adapter<BookingRequestAdapter.RequestViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_booking_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val request = requestList[position]
        holder.bind(request)
    }

    override fun getItemCount(): Int = requestList.size

    inner class RequestViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTripDetails: TextView = view.findViewById(R.id.tvTripDetails)
        private val tvRequesterName: TextView = view.findViewById(R.id.tvRequesterName)
        private val btnAccept: Button = view.findViewById(R.id.btnAccept)
        private val btnReject: Button = view.findViewById(R.id.btnReject)

        fun bind(request: BookingRequest) {
            tvTripDetails.text = "${request.tripName}"
            tvRequesterName.text = "Requested by: ${request.riderName}"

            btnAccept.setOnClickListener {
                handleBookingRequest(request, "accepted")
            }

            btnReject.setOnClickListener {
                handleBookingRequest(request, "rejected")
            }
        }

        private fun handleBookingRequest(request: BookingRequest, status: String) {
            val database = FirebaseDatabase.getInstance()
            val requestsRef = database.getReference("BookingRequests").child(request.requestId)
            val tripsRef = database.getReference("Trips").child(request.tripId)

            requestsRef.child("status").setValue(status).addOnSuccessListener {
                if (status == "accepted") {
                    Log.d("BookingRequest", "✅ Booking request accepted. Now updating availableSeats.")

                    tripsRef.runTransaction(object : Transaction.Handler {
                        override fun doTransaction(mutableData: MutableData): Transaction.Result {
                            val tripData = mutableData.getValue(Trip::class.java) ?: return Transaction.abort()
                            val availableSeats = tripData.availableSeats
                            Log.d("BookingRequest", "🔍 Current availableSeats: $availableSeats")

                            if (availableSeats > 0) {
                                mutableData.value = tripData.copy(availableSeats = (availableSeats -1))
                                Log.d("BookingRequest", "✅ Updated availableSeats to: ${availableSeats - 1}")
                            } else {
                                Log.e("BookingRequest", "❌ No available seats left!")
                            }

                            return Transaction.success(mutableData)
                        }

                        override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                            if (error != null) {
                                Log.e("BookingRequest", "❌ Failed to update seats: ${error.message}")
                            } else {
                                Log.d("BookingRequest", "🎉 Successfully updated available seats.")
                            }
                        }
                    })
                }
            }.addOnFailureListener { e ->
                Log.e("BookingRequest", "❌ Failed to update request status: ${e.message}")
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
}
}

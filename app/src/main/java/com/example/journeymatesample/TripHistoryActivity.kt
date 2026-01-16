package com.example.journeymatesample

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.journeymatesample.R
import com.example.journeymatesample.TripHistoryAdapter
import com.example.journeymatesample.Trip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class TripHistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tripHistoryAdapter: TripHistoryAdapter
    private lateinit var historyRef: DatabaseReference
    private lateinit var userId: String

    private val tripHistoryList = mutableListOf<Trip>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip_history)

        recyclerView = findViewById(R.id.recyclerViewTripHistory)
        recyclerView.layoutManager = LinearLayoutManager(this)
        tripHistoryAdapter = TripHistoryAdapter(tripHistoryList)
        recyclerView.adapter = tripHistoryAdapter

        loadTripHistory()
    }

    private fun loadTripHistory() {
        val userId = getCurrentUserId() ?: return
        val tripHistoryRef = FirebaseDatabase.getInstance().getReference("TripHistory").child(userId)

        tripHistoryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                tripHistoryList.clear()
                for (tripSnapshot in snapshot.children) {
                    val trip = tripSnapshot.getValue(Trip::class.java)
                    if (trip != null) {
                        tripHistoryList.add(trip)
                    }
                }
                tripHistoryAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error loading trip history: ${error.message}")
            }
        })
    }
    private fun getCurrentUserId(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }

}
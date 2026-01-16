package com.example.journeymatesample

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class EditTripActivity : AppCompatActivity() {

    private lateinit var editTextStartLocation: EditText
    private lateinit var editTextDestination: EditText
    private lateinit var editTextTotalSeats: EditText
    private lateinit var editTextAvailableSeats: EditText
    private lateinit var editTextTotalFare: EditText
    private lateinit var btnSaveTrip: Button
    private var tripId: String? = null  // The trip ID being edited

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_trip)

        editTextStartLocation = findViewById(R.id.editTextStartLocation)
        editTextDestination = findViewById(R.id.editTextDestination)
        editTextTotalSeats = findViewById(R.id.editTextTotalSeats)
        editTextAvailableSeats = findViewById(R.id.editTextAvailableSeats)
        editTextTotalFare = findViewById(R.id.editTextTotalFare)
        btnSaveTrip = findViewById(R.id.btnSaveTrip)

        // ✅ Get trip ID from SharedPreferences instead of Intent extras
        val pref = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        tripId = pref.getString("trip_id", null)

        if (tripId != null) {
            fetchTripDetails(tripId!!)
        } else {
            Log.e("EditTripActivity", "❌ Error: Trip ID is missing!")
            Toast.makeText(this, "Error: Trip ID is missing!", Toast.LENGTH_SHORT).show()
            finish()
        }

        btnSaveTrip.setOnClickListener {
            saveTripChanges()
        }
    }

    /**
     * ✅ Fetch existing trip details from Firebase
     */
    private fun fetchTripDetails(tripId: String) {
        val database = FirebaseDatabase.getInstance().getReference("Trips").child(tripId)

        database.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                editTextStartLocation.setText(snapshot.child("startLocation").value.toString())
                editTextDestination.setText(snapshot.child("destination").value.toString())
                editTextTotalSeats.setText(snapshot.child("totalSeats").value.toString())
                editTextAvailableSeats.setText(snapshot.child("availableSeats").value.toString())
                editTextTotalFare.setText(snapshot.child("totalFare").value.toString())
            } else {
                Toast.makeText(this, "Trip not found!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }.addOnFailureListener { e ->
            Log.e("EditTripActivity", "Failed to fetch trip: ${e.message}")
            Toast.makeText(this, "Failed to load trip details", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * ✅ Save edited trip details back to Firebase
     */
    private fun saveTripChanges() {
        if (tripId.isNullOrEmpty()) {
            Toast.makeText(this, "Error: Invalid trip ID!", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedTrip = mapOf(
            "startLocation" to editTextStartLocation.text.toString(),
            "destination" to editTextDestination.text.toString(),
            "totalSeats" to editTextTotalSeats.text.toString().toInt(),
            "availableSeats" to editTextAvailableSeats.text.toString().toInt(),
            "totalFare" to editTextTotalFare.text.toString().toDouble()
        )

        val database = FirebaseDatabase.getInstance().getReference("Trips").child(tripId!!)
        database.updateChildren(updatedTrip).addOnSuccessListener {
            Toast.makeText(this, "Trip updated successfully!", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, UserTripsActivity::class.java))
            finish()
        }.addOnFailureListener { e ->
            Log.e("EditTripActivity", "Failed to update trip: ${e.message}")
            Toast.makeText(this, "Failed to update trip", Toast.LENGTH_SHORT).show()
        }
    }
}

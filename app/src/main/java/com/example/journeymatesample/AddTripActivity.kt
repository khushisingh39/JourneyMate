package com.example.journeymatesample

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import java.util.*
import com.google.firebase.messaging.FirebaseMessaging
import android.util.Log
import androidx.core.content.ContextCompat

class AddTripActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var firebaseAuth: FirebaseAuth

    private val AUTOCOMPLETE_REQUEST_CODE_START = 1
    private val AUTOCOMPLETE_REQUEST_CODE_DEST = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_trip)

        // Initialize Firebase
        database = FirebaseDatabase.getInstance().reference.child("Trips")
        firebaseAuth = FirebaseAuth.getInstance()

        // Initialize Places API
        Places.initialize(applicationContext, "AIzaSyAErY-sVpOP02FLBt_-1ALfcpn8lB9oZs4")

        val etStartLocation = findViewById<EditText>(R.id.etStartLocation)
        val etDestinationLocation = findViewById<EditText>(R.id.etDestinationLocation)
        val etDate = findViewById<EditText>(R.id.etDate)
        val etStartTime = findViewById<EditText>(R.id.etStartTime)
        val etEndTime = findViewById<EditText>(R.id.etEndTime)
        val etTotalSeats = findViewById<EditText>(R.id.etTotalSeats)
        val etAvailableSeats = findViewById<EditText>(R.id.etAvailableSeats)
        val etTotalFare = findViewById<EditText>(R.id.etTotalFare)
        val etCarType = findViewById<EditText>(R.id.etCarType)
        val btnSubmitTrip = findViewById<Button>(R.id.btnSubmitTrip)

        val btnSearchTrip = findViewById<Button>(R.id.btnSearchTrip)

        val layoutCreateTrip = findViewById<LinearLayout>(R.id.layoutCreateTrip)
        val layoutSearchTrip = findViewById<LinearLayout>(R.id.layoutSearchTrip)

        val btnHost = findViewById<Button>(R.id.btnHost)
        val btnJoin = findViewById<Button>(R.id.btnJoin)

        val ivProfile = findViewById<ImageButton>(R.id.ivProfile)

        // Prevent keyboard popup for date & time fields
        etDate.isFocusable = false
        etStartTime.isFocusable = false
        etEndTime.isFocusable = false



        ivProfile.setOnClickListener {
            val intent = Intent(this, UserDashboardActivity::class.java)
            startActivity(intent)
        }
        layoutCreateTrip.visibility = View.VISIBLE
        layoutSearchTrip.visibility = View.GONE

        // Toggle between Create Trip and Search Trip Layouts
        btnHost.setOnClickListener {
            layoutCreateTrip.visibility = View.VISIBLE
            layoutSearchTrip.visibility = View.GONE

            btnHost.background = ContextCompat.getDrawable(this, R.drawable.toggle_selected)
            btnJoin.background = ContextCompat.getDrawable(this, R.drawable.toggle_unselected)
        }
        btnJoin.setOnClickListener {
            layoutCreateTrip.visibility = View.GONE
            layoutSearchTrip.visibility = View.VISIBLE

            btnJoin.background = ContextCompat.getDrawable(this, R.drawable.toggle_selected)
            btnHost.background = ContextCompat.getDrawable(this, R.drawable.toggle_unselected)
        }

        // Start Location Picker
        etStartLocation.setOnClickListener {
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, listOf())
                .build(this)
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE_START)
        }

        // Destination Location Picker
        etDestinationLocation.setOnClickListener {
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, listOf())
                .build(this)
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE_DEST)
        }

        // Date Picker
        etDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    etDate.setText(
                        String.format(
                            Locale.getDefault(),
                            "%02d/%02d/%d",
                            selectedDay,
                            selectedMonth + 1,
                            selectedYear
                        )
                    )
                },
                year, month, day
            ).show()
        }

        // **Date Picker for Search Trip** - Add this section for searching trips
        val etSearchDate = findViewById<EditText>(R.id.etSearchDate)
        etSearchDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    etSearchDate.setText(
                        String.format(
                            Locale.getDefault(),
                            "%02d/%02d/%d",
                            selectedDay,
                            selectedMonth + 1,
                            selectedYear
                        )
                    )
                },
                year, month, day
            ).show()
        }

        // Time Picker for Start Time
        etStartTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            TimePickerDialog(
                this,
                { _, selectedHour, selectedMinute ->
                    etStartTime.setText(
                        String.format(
                            Locale.getDefault(),
                            "%02d:%02d",
                            selectedHour,
                            selectedMinute
                        )
                    )
                },
                hour, minute, true
            ).show()
        }

        // Time Picker for End Time
        etEndTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            TimePickerDialog(
                this,
                { _, selectedHour, selectedMinute ->
                    etEndTime.setText(
                        String.format(
                            Locale.getDefault(),
                            "%02d:%02d",
                            selectedHour,
                            selectedMinute
                        )
                    )
                },
                hour, minute, true
            ).show()
        }

        // Submit Trip
        btnSubmitTrip.setOnClickListener {
            val startLocation = etStartLocation.text.toString().trim()
            val destination = etDestinationLocation.text.toString().trim()
            val totalSeats = etTotalSeats.text.toString().trim()
            val availableSeats = etAvailableSeats.text.toString().trim()
            val date = etDate.text.toString().trim()
            val startTime = etStartTime.text.toString().trim()
            val endTime = etEndTime.text.toString().trim()
            val totalFare = etTotalFare.text.toString().trim()
            val cartype = etCarType.text.toString().trim()

            if (startLocation.isEmpty() || destination.isEmpty() || totalSeats.isEmpty() ||
                availableSeats.isEmpty() || date.isEmpty() || startTime.isEmpty() ||
                endTime.isEmpty() || totalFare.isEmpty()
            ) {
                Toast.makeText(this, "Please fill all fields!", Toast.LENGTH_SHORT).show()
            } else if (availableSeats.toInt() > totalSeats.toInt()) {
                Toast.makeText(
                    this,
                    "Available seats cannot exceed total seats!",
                    Toast.LENGTH_SHORT
                ).show()
            } else {

                //You are checking user here using firebase authentication but you are not creating any user using authentication method
                //First signup user using firebase authentication method.
                //Then this currentUser value will be available otherwise it will always return null value.

                //I have changed the previous check of current user because it was causing the issue

//                val currentUser = firebaseAuth.currentUser
                val currentUser = getCurrentUserId()

                if (currentUser != null) {
//                    val userId = currentUser.uid // Get the user ID
                    val userId = currentUser // Get the user ID
                    val tripId = database.push().key ?: return@setOnClickListener
                    val tripData = mapOf(
                        "tripId" to tripId,
                        "initiatorId" to userId, // Associate trip with user ID
                        "startLocation" to startLocation,
                        "destination" to destination,
                        "totalSeats" to totalSeats.toInt(),
                        "availableSeats" to availableSeats.toInt(),
                        "date" to date,
                        "startTime" to startTime,
                        "endTime" to endTime,
                        "totalFare" to totalFare.toDouble(),
                        "cartype" to cartype

                    )
                    database.child(tripId).setValue(tripData).addOnSuccessListener {
                        Toast.makeText(this, "Trip Created Successfully!", Toast.LENGTH_SHORT)
                            .show()
                        // Optionally refresh or clear fields instead of redirecting
                        etStartLocation.text.clear()
                        etDestinationLocation.text.clear()
                        etTotalSeats.text.clear()
                        etAvailableSeats.text.clear()
                        etDate.text.clear()
                        etStartTime.text.clear()
                        etEndTime.text.clear()
                        etTotalFare.text.clear()
                        etCarType.text.clear()

                    }.addOnFailureListener {
                        Toast.makeText(this, "Failed to create trip!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "You must log in to create a trip.", Toast.LENGTH_SHORT)
                        .show()
                    startActivity(Intent(this, LoginActivity::class.java))
                }
            }
        }

        // Search Trips based on user criteria
        btnSearchTrip.setOnClickListener {
            val searchStartLocation =
                findViewById<EditText>(R.id.etSearchStartLocation).text.toString().trim()
            val searchDestinationLocation =
                findViewById<EditText>(R.id.etSearchDestinationLocation).text.toString().trim()
            val searchDate = findViewById<EditText>(R.id.etSearchDate).text.toString().trim()

            val query = database.orderByChild("startLocation").equalTo(searchStartLocation)
            query.get().addOnSuccessListener { dataSnapshot ->
                val tripList = mutableListOf<Map<String, String>>()
                for (snapshot in dataSnapshot.children) {
                    val trip = snapshot.value as Map<String, String>
                    if (trip["destination"] == searchDestinationLocation && trip["date"] == searchDate) {
                        tripList.add(trip)
                    }
                }
                if (tripList.isEmpty()) {
                    Toast.makeText(this, "No trips found.", Toast.LENGTH_SHORT).show()
                } else {
                    val intent = Intent(this, ViewTripsActivity::class.java)
                    intent.putExtra("startLocation", searchStartLocation)  //
                    intent.putExtra("destination", searchDestinationLocation)  //
                    intent.putExtra("date", searchDate)  //
                    //intent.putExtra("trips", ArrayList(tripList))
                    startActivity(intent)
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to fetch trips: ${it.message}", Toast.LENGTH_SHORT)
                    .show()
            }

        }

        updateFcmToken()
    }

    private fun updateFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val newToken = task.result
                val userId = getCurrentUserId() // Assuming you have this method to get user ID from SharedPreferences
                if (userId != null) {
                    FirebaseDatabase.getInstance().getReference("users").child(userId).child("fcmToken").setValue(newToken)
                        .addOnSuccessListener {
                            Log.d("FCM", "Token updated successfully")
                        }
                        .addOnFailureListener {
                            Log.e("FCM", "Failed to update token", it)
                        }
                }
            } else {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
            }
        }
    }

    private fun getCurrentUserId(): String?{
        val pref = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        return  pref.getString("user_id","")
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            val place = Autocomplete.getPlaceFromIntent(data!!)
            when (requestCode) {
                AUTOCOMPLETE_REQUEST_CODE_START -> {
                    findViewById<EditText>(R.id.etStartLocation).setText(place.address)
                }
                AUTOCOMPLETE_REQUEST_CODE_DEST -> {
                    findViewById<EditText>(R.id.etDestinationLocation).setText(place.address)
                }
            }
        } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
            val status: Status = Autocomplete.getStatusFromIntent(data!!)
            Toast.makeText(this, status.statusMessage, Toast.LENGTH_SHORT).show()
        }
    }
}

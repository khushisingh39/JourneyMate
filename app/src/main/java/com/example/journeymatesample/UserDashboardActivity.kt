package com.example.journeymatesample

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class UserDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_dashboard)

        // Initialize buttons
        val editProfileButton = findViewById<Button>(R.id.btn_edit_profile)
        val requestButton = findViewById<Button>(R.id.btn_request)
        val editTripButton = findViewById<Button>(R.id.btn_edit_trip)
        val settingsButton = findViewById<Button>(R.id.btn_settings)
        val chatListButton = findViewById<Button>(R.id.chatListButton)
        val acceptedUserId = "user456" // Get from booking data
        // Set up button click listeners
        editProfileButton.setOnClickListener {
            val pref = getSharedPreferences("MyPrefs", MODE_PRIVATE)
            val phoneNumber = pref.getString("user_phone", "") ?: ""
            val userType = pref.getString("user_type", "users") // Default to "users"

            if (phoneNumber.isNotEmpty()) {
                val intent = Intent(this, EditProfileActivity::class.java)
                intent.putExtra("phone_number", phoneNumber)
                intent.putExtra("user_type", userType)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Error: No phone number found", Toast.LENGTH_SHORT).show()
            }
        }

        // Set up click listener for editTripButton
        editTripButton.setOnClickListener {
            // Navigate to EditTripActivity
            val intent = Intent(this, UserTripsActivity::class.java)
            startActivity(intent)
        }

        requestButton.setOnClickListener {
            val intent = Intent(this, BookingRequestsActivity::class.java)
            startActivity(intent)
        }

        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        if (acceptedUserId.isNotEmpty()) {
            chatListButton.visibility = View.VISIBLE
        }

        chatListButton.setOnClickListener {
            val intent = Intent(this, ChatListActivity::class.java)
            startActivity(intent)
        }


    }
}

package com.example.journeymatesample

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.regex.Pattern

class IDVerificationActivity : AppCompatActivity() {

    private val validDriverIds = listOf("BTBTD1234", "BTBTD4567", "BTBTD7890")  // Example pre-defined Banasthali driver IDs
    private lateinit var licenseNumber: EditText
    private lateinit var vehicleNumber: EditText
    private lateinit var vehicleType: EditText
    private lateinit var banasthaliNumber: EditText
    private lateinit var verifyButton: Button
    private lateinit var errorMessage: TextView

    // Regex pattern for vehicle number validation (for example: "XX 00 XX 0000")
    private val vehicleNumberRegex = "^[A-Z]{2}\\s\\d{2}\\s[A-Z]{2}\\s\\d{4}\$".toRegex()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_id_verification)

        licenseNumber = findViewById(R.id.licenseNumber)
        vehicleNumber = findViewById(R.id.vehicleNumber)
        vehicleType = findViewById(R.id.vehicleType)
        banasthaliNumber = findViewById(R.id.banasthaliNumber)
        verifyButton = findViewById(R.id.verifyButton)
        errorMessage = findViewById(R.id.errorMessage)

        verifyButton.setOnClickListener {
            val enteredBanasthaliNumber = banasthaliNumber.text.toString()
            val enteredVehicleNumber = vehicleNumber.text.toString()

            if (validDriverIds.contains(enteredBanasthaliNumber)) {
                // Validate vehicle number using regex
                if (isValidVehicleNumber(enteredVehicleNumber)) {
                    // If ID is valid and vehicle number is correct, proceed to create trip
                    val intent = Intent(this, AddTripActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // Invalid vehicle number
                    errorMessage.text = "Invalid vehicle number format."
                    errorMessage.visibility = View.VISIBLE
                }
            } else {
                // If ID is not valid, show error message
                errorMessage.text = "Your ID is not registered. Please try again."
                errorMessage.visibility = View.VISIBLE
            }
        }
    }

    // Function to validate vehicle number using regex
    private fun isValidVehicleNumber(vehicleNumber: String): Boolean {
        return vehicleNumberRegex.matches(vehicleNumber)
    }
}

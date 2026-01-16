package com.example.journeymatesample

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import android.util.Log

class EditProfileActivity : AppCompatActivity() {

    private lateinit var editTextName: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var editTextPhone: EditText
    private lateinit var editTextOldPassword: EditText
    private lateinit var editTextNewPassword: EditText
    private lateinit var btnSaveProfile: Button
    private var userPhoneNumber: String? = null
    private var userType: String? = null // "student" -> users table, "captain" -> drivers table

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        editTextName = findViewById(R.id.editTextName)
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPhone = findViewById(R.id.editTextPhone)
        editTextOldPassword = findViewById(R.id.editTextOldPassword)
        editTextNewPassword = findViewById(R.id.editTextNewPassword)
        btnSaveProfile = findViewById(R.id.btnSaveProfile)

        // ✅ Load user details from SharedPreferences
        val pref = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        userPhoneNumber = pref.getString("user_phone", null) // Fetch stored phone number
        userType = pref.getString("user_type", null) // "student" or "captain"

        if (userPhoneNumber != null && userType != null) {
            val table = if (userType == "student") "users" else "drivers"
            Log.d("EditProfile", "Fetching data from $table for phone: $userPhoneNumber")
            fetchUserDataFromRealtimeDB(userPhoneNumber!!, table)
        } else {
            Toast.makeText(this, "Error: No logged-in user found", Toast.LENGTH_SHORT).show()
        }

        btnSaveProfile.setOnClickListener {
            val newName = editTextName.text.toString()
            val newEmail = editTextEmail.text.toString()
            val oldPassword = editTextOldPassword.text.toString()
            val newPassword = editTextNewPassword.text.toString()

            if (userPhoneNumber != null && userType != null) {
                val table = if (userType == "student") "users" else "drivers"

                reAuthenticateUser(userPhoneNumber!!, oldPassword, table) { isAuthenticated ->
                    if (isAuthenticated) {
                        updateUserDataInRealtimeDB(userPhoneNumber!!, newName, newEmail, newPassword, table)
                    } else {
                        Toast.makeText(this, "Incorrect old password!", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Failed to retrieve user details", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Fetch user details from the correct table (`users` or `drivers`).
     */
    private fun fetchUserDataFromRealtimeDB(phoneNumber: String, table: String) {
        val database = FirebaseDatabase.getInstance().getReference(table).child(phoneNumber)

        database.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                editTextName.setText(snapshot.child("name").value.toString())
                editTextEmail.setText(snapshot.child("email").value.toString())
                editTextPhone.setText(phoneNumber) // Don't allow changing phone as it's the key
                Log.d("EditProfile", "Data fetched: Name=${editTextName.text}, Email=${editTextEmail.text}")
            } else {
                Toast.makeText(this, "User data not found in $table!", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Log.e("DBError", "Failed to fetch user data from $table: ${e.message}")
            Toast.makeText(this, "Failed to load user details", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Authenticate user by checking the old password stored in Firebase.
     */
    private fun reAuthenticateUser(phoneNumber: String, oldPassword: String, table: String, callback: (Boolean) -> Unit) {
        val database = FirebaseDatabase.getInstance().getReference(table).child(phoneNumber)

        database.child("password").get().addOnSuccessListener { snapshot ->
            val storedPassword = snapshot.value.toString()
            callback(storedPassword == oldPassword) // Check if old password matches
        }.addOnFailureListener {
            callback(false)
        }
    }

    /**
     * Update user details in the correct table (`users` or `drivers`).
     */
    private fun updateUserDataInRealtimeDB(phoneNumber: String, newName: String, newEmail: String, newPassword: String, table: String) {
        val database = FirebaseDatabase.getInstance().getReference(table).child(phoneNumber)

        val updates = mapOf(
            "name" to newName,
            "email" to newEmail,
            "password" to newPassword
        )

        database.updateChildren(updates).addOnSuccessListener {
            // ✅ Save updated details in SharedPreferences
            val pref = getSharedPreferences("MyPrefs", MODE_PRIVATE).edit()
            pref.putString("user_name", newName)
            pref.putString("user_email", newEmail)
            pref.apply()

            Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, UserDashboardActivity::class.java))
            finish()
        }.addOnFailureListener { e ->
            Log.e("DBError", "Failed to update profile in $table: ${e.message}")
            Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
        }
    }
}

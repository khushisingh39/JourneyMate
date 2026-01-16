package com.example.journeymatesample

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class CaptainLoginActivity : AppCompatActivity() {

    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_captain_login) // Using a separate layout for Captain login

        // Initialize Firebase Database reference
        databaseReference = FirebaseDatabase.getInstance().reference

        val phoneInput = findViewById<EditText>(R.id.enter_phone_number)
        val passwordInput = findViewById<EditText>(R.id.enter_password)
        val loginButton = findViewById<Button>(R.id.login_button)
        val notAMemberText = findViewById<TextView>(R.id.not_a_member_text)

        // Handle login button click
        loginButton.setOnClickListener {
            val phone = phoneInput.text.toString().trim().filter { it.isDigit() }
            val password = passwordInput.text.toString().trim()

            if (phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate driver credentials from Firebase
            databaseReference.child("drivers")
                .orderByChild("phone")
                .equalTo(phone)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        Log.d("CaptainLogin", "Firebase Data: ${snapshot.value}")

                        if (snapshot.exists()) {
                            for (driverSnapshot in snapshot.children) {
                                val storedPassword = driverSnapshot.child("password").value.toString()

                                if (storedPassword.trim() == password.trim()) {

                                    val pref = getSharedPreferences("MyPrefs", MODE_PRIVATE).edit()
                                    pref.putString("user_phone", phone)
                                    pref.putString("user_type", "captain") // Set login type as "drivers"
                                    pref.apply()


                                    Toast.makeText(this@CaptainLoginActivity, "Login successful!", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this@CaptainLoginActivity, IDVerificationActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                    return
                                }
                            }
                            Toast.makeText(this@CaptainLoginActivity, "Incorrect password", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@CaptainLoginActivity, "Driver does not exist", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@CaptainLoginActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        // Handle create account button click
        notAMemberText.setOnClickListener {
            val intent = Intent(this, CaptainSignUpActivity::class.java) // Redirect to Captain Signup
            startActivity(intent)
        }
    }
}

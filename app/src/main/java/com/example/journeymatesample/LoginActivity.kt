package com.example.journeymatesample

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.messaging.FirebaseMessaging // Add this import for FCM

class LoginActivity : AppCompatActivity() {

    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Database reference
                databaseReference = FirebaseDatabase.getInstance().reference

        val var_phone_no = findViewById<EditText>(R.id.enter_phone_number)
        val var_password = findViewById<EditText>(R.id.enter_password)
        val var_btnLogin = findViewById<Button>(R.id.login_button)
        val var_notamember = findViewById<TextView>(R.id.not_a_member_text)
        val var_forgotPassword = findViewById<TextView>(R.id.forgot_your_password_text)

        // Handle login button click
        var_btnLogin.setOnClickListener {
            val phone = var_phone_no.text.toString().trim().filter { it.isDigit() }
            val password = var_password.text.toString().trim()

            if (phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate user credentials from Firebase
            databaseReference.child("users")
                .orderByChild("phone")
                .equalTo(phone)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        Log.d("LoginDebug", "Raw Firebase Snapshot: ${snapshot.value}")
                        Log.d("LoginDebug", "Entered Phone: $phone")
                        Log.d("LoginDebug", "Entered Password: $password")

                        if (snapshot.exists()) {
                            for (userSnapshot in snapshot.children) {
                                val storedPassword = userSnapshot.child("password").value.toString()
                                Log.d("LoginDebug", "Stored Password (trimmed): ${storedPassword.trim()}")
                                Log.d("LoginDebug", "Entered Password (trimmed): ${password.trim()}")

                                if (storedPassword.trim() == password.trim()) {
                                    try {
                                        val uid = userSnapshot.child("user_id").value.toString()
                                        saveUserDataLocally(uid, phone)

                                        // ✅ Store User Login Info in SharedPreferences
                                        val pref = getSharedPreferences("MyPrefs", MODE_PRIVATE).edit()
                                        pref.putString("user_phone", phone)
                                        pref.putString("user_type", "student")
                                        pref.apply()

                                        // Update FCM token here
                                        updateFcmToken(phone)

                                        Toast.makeText(this@LoginActivity, "Login successful!", Toast.LENGTH_SHORT).show()
                                        val intent = Intent(this@LoginActivity, AddTripActivity::class.java)
                                        startActivity(intent)
                                        finish()
                                        return
                                    } catch (_: Exception) {
                                        Log.e("LoginDebug", "Error saving user data locally")
                                    }
                                }
                            }
                            Toast.makeText(this@LoginActivity, "Incorrect password", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@LoginActivity, "User does not exist", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@LoginActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        // Handle create account button click
        var_notamember.setOnClickListener {
            // Navigate to SignUpActivity
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
        var_forgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun saveUserDataLocally(uid: String, phone: String) {
        val pref = getSharedPreferences("MyPrefs", MODE_PRIVATE).edit()
        pref.putString("user_id", uid)
        pref.putString("user_phone", phone)
        pref.apply()
    }

    private fun updateFcmToken(userId: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val newToken = task.result
                databaseReference.child("users").child(userId).child("fcmToken").setValue(newToken)
                    .addOnSuccessListener {
                        Log.d("FCM", "FCM Token updated successfully for user $userId")
                    }
                    .addOnFailureListener {
                        Log.e("FCM", "Failed to update FCM Token for user $userId", it)
                    }
            } else {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
            }
        }
    }
}
package com.example.journeymatesample

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SettingsActivity : AppCompatActivity() {

    private lateinit var btnLogout: Button
    private lateinit var btnDeleteAccount: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        btnLogout = findViewById(R.id.btnLogout)
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount)

        // Logout Function
        btnLogout.setOnClickListener {
            // ✅ Clear SharedPreferences to remove stored login data
            val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
            val userType = sharedPreferences.getString("user_type", null)

            val editor = sharedPreferences.edit()
            editor.clear()
            editor.apply()

            // ✅ Sign out from Firebase Authentication
            FirebaseAuth.getInstance().signOut()
            // ✅ Redirect to appropriate login screen
            val intent = when (userType) {
                "student" -> Intent(this, LoginActivity::class.java)
                "captain" -> Intent(this, CaptainLoginActivity::class.java)
                else -> Intent(this, MainActivity::class.java) // Default to student login
            }


            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }


        // Delete Account Function
        btnDeleteAccount.setOnClickListener {
            showDeleteAccountDialog()
        }
    }

    private fun showDeleteAccountDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_delete_account, null)
        val editTextPhone = dialogView.findViewById<EditText>(R.id.editTextPhone)
        val editTextPassword = dialogView.findViewById<EditText>(R.id.editTextPassword)

        AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setView(dialogView)
            .setPositiveButton("Delete") { _, _ ->
                val phone = editTextPhone?.text?.toString()?.trim() ?: ""
                val password = editTextPassword?.text?.toString()?.trim() ?: ""

                if (phone.isNotEmpty() && password.isNotEmpty()) {
                    verifyAccountAndRedirect(phone, password)  // ✅ Ensure both parameters are passed
                } else {
                    Toast.makeText(this, "Please enter phone number and password", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun verifyAccountAndRedirect(phone: String, enteredPassword: String) {
        Log.d("VerifyAccount", "Checking phone and password before redirecting...")

        // 🔹 Step 1: Fetch stored password from Firebase Realtime Database
        val databaseRef = FirebaseDatabase.getInstance().getReference("users").child(phone)
        databaseRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val storedPassword = snapshot.child("password").value.toString()

                // 🔹 Step 2: Compare entered password with stored password
                if (storedPassword == enteredPassword) {
                    Log.d("VerifyAccount", "Correct credentials. Redirecting to login page...")

                    Toast.makeText(this, "Deleted successfully!", Toast.LENGTH_SHORT).show()

                    // 🔹 Step 3: Redirect to Login Page
                    redirectToLogin()
                } else {
                    Toast.makeText(this, "Incorrect password!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Phone number not found in database!", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to fetch data from Firebase!", Toast.LENGTH_SHORT).show()
            Log.e("VerifyAccount", "Firebase Database Error: ${it.message}")
        }
    }

    // 🔹 Function to Redirect to Login Page
    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

}

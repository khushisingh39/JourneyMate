package com.example.journeymatesample

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val userType = sharedPreferences.getString("user_type", null)

        // 🚀 Redirect based on stored login type
        if (userType == "student") {
            startActivity(Intent(this, AddTripActivity::class.java))
            finish()
            return
        } else if (userType == "captain") {
            startActivity(Intent(this, AddTripActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        val loginAsStudentButton = findViewById<Button>(R.id.loginasstudent)
        val loginAsCaptainButton = findViewById<Button>(R.id.loginascaptain)

        loginAsStudentButton.setOnClickListener {
            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
        }

        loginAsCaptainButton.setOnClickListener {
            startActivity(Intent(this@MainActivity, CaptainLoginActivity::class.java))
        }
    }
}

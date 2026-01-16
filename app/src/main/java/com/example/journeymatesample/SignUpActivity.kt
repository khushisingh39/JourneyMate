package com.example.journeymatesample

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.database.FirebaseDatabase
import java.util.concurrent.TimeUnit
import com.google.firebase.messaging.FirebaseMessaging
import java.security.AuthProvider

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var verificationId: String
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val etName: EditText = findViewById(R.id.name)
        val etEmail: EditText = findViewById(R.id.emailaddress)
        val etPhone: EditText = findViewById(R.id.phone_number)
        val etPassword: EditText = findViewById(R.id.password)
        val etConfirmPassword: EditText = findViewById(R.id.confirm_password)
        val genderRadioGroup: RadioGroup = findViewById(R.id.gender)
        val etOtp: EditText = findViewById(R.id.otp)
        val btnGenerateOtp: Button = findViewById(R.id.generate_otp_button)
        val btnVerifyOtp: Button = findViewById(R.id.verify_otp_button)
        val btnSignIn: Button = findViewById(R.id.btn_sign_up)

        auth = FirebaseAuth.getInstance()
        dbHelper = DatabaseHelper(this)

        var selectedGender = ""
        genderRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedGender = when (checkedId) {
                R.id.gender_male -> "Male"
                R.id.gender_female -> "Female"
                R.id.gender_other -> "Other"
                else -> ""
            }
        }

        btnGenerateOtp.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (phone.length != 10) {
                Toast.makeText(this, "Enter a valid 10-digit phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendOtp("+91$phone")
        }

        btnVerifyOtp.setOnClickListener {
            val otp = etOtp.text.toString().trim()
            if (otp.isNotEmpty() && otp.length == 6) {
                verifyOtp(otp)
            } else {
                Toast.makeText(this, "Enter a valid OTP", Toast.LENGTH_SHORT).show()
            }
        }

        btnSignIn.setOnClickListener {
            if (auth.currentUser != null) {
                startActivity(Intent(this, AddTripActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Please verify OTP first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendOtp(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    auth.signInWithCredential(credential)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(this@SignUpActivity, "Automatically verified", Toast.LENGTH_SHORT).show()
                                saveUserData(task.result.user?.uid?:"")
                            } else {
                                Toast.makeText(this@SignUpActivity, "Verification failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Toast.makeText(this@SignUpActivity, "OTP Verification Failed: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("FirebaseAuth", "Verification failed", e)
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    this@SignUpActivity.verificationId = verificationId
                    Toast.makeText(this@SignUpActivity, "OTP Sent Successfully", Toast.LENGTH_SHORT).show()
                    showOtpDialog()

                    val etOtp: EditText = findViewById(R.id.otp)
                    val tvVerifyOtp: TextView = findViewById(R.id.verify_otp)
                    val etName: EditText = findViewById(R.id.name)
                    val etEmail: EditText = findViewById(R.id.emailaddress)
                    val etPhone: EditText = findViewById(R.id.phone_number)
                    val etPassword: EditText = findViewById(R.id.password)
                    val etConfirmPassword: EditText = findViewById(R.id.confirm_password)
                    val genderRadioGroup: RadioGroup = findViewById(R.id.gender)
                    val btnGenerateOtp: Button = findViewById(R.id.generate_otp_button)
                    val btnVerifyOtp: Button = findViewById(R.id.verify_otp_button)
                    val tvCreateAccount: TextView = findViewById(R.id.createyouraccount)
                    val btnSignIn: Button = findViewById(R.id.btn_sign_up)

                    etOtp.visibility = View.VISIBLE
                    btnVerifyOtp.visibility = View.VISIBLE
                    tvVerifyOtp.visibility = View.VISIBLE
                    btnSignIn.visibility = View.VISIBLE

                    etName.visibility = View.GONE
                    etEmail.visibility = View.GONE
                    etPhone.visibility = View.GONE
                    etPassword.visibility = View.GONE
                    etConfirmPassword.visibility = View.GONE
                    genderRadioGroup.visibility = View.GONE
                    btnGenerateOtp.visibility = View.GONE
                    tvCreateAccount.visibility = View.GONE
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun showOtpDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enter OTP")

        val input = EditText(this)
        input.hint = "Enter OTP"
        builder.setView(input)

        builder.setPositiveButton("Submit") { _, _ ->
            val enteredOtp = input.text.toString().trim()
            if (enteredOtp.isEmpty() || enteredOtp.length != 6) {
                Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show()
            } else {
                verifyOtp(enteredOtp)
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    private fun verifyOtp(otp: String) {
        val credential = PhoneAuthProvider.getCredential(verificationId, otp)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "OTP Verified Successfully!", Toast.LENGTH_SHORT).show()
                    saveUserData(task.result.user?.uid?:"")
                } else {
                    Toast.makeText(this, "OTP Verification Failed!", Toast.LENGTH_SHORT).show()
                }
            }
    }
    private fun saveUserDataLocally(uid: String, phone: String){
        val pref = getSharedPreferences("MyPrefs", MODE_PRIVATE).edit()
        pref.putString("user_id", uid)
        pref.putString("user_phone", phone)
        pref.apply()
    }

    private fun saveUserData(uid: String) {
        val name = findViewById<EditText>(R.id.name).text.toString().trim()
        val email = findViewById<EditText>(R.id.emailaddress).text.toString().trim()
        val phone =
            findViewById<EditText>(R.id.phone_number).text.toString().trim().filter { it.isDigit() }
        val password = findViewById<EditText>(R.id.password).text.toString().trim()
        val genderRadioGroup = findViewById<RadioGroup>(R.id.gender)
        val selectedGender = when (genderRadioGroup.checkedRadioButtonId) {
            R.id.gender_male -> "Male"
            R.id.gender_female -> "Female"
            R.id.gender_other -> "Other"
            else -> ""
        }

        // Fetch FCM token from the device
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            val fcmToken = task.result ?: "default_token"

            val userId = dbHelper.addUser(name, email, phone, password, fcmToken,this)
            if (userId > 0) {
                val database = FirebaseDatabase.getInstance().reference
                val user = mapOf(
                    "name" to name,
                    "email" to email,
                    "phone" to phone,
                    "gender" to selectedGender,
                    "password" to password,
                    "user_id" to uid,
                    "fcmToken" to fcmToken
                )
                database.child("users").child(phone).setValue(user)
                    .addOnSuccessListener {
                        saveUserDataLocally(uid, phone)
                        Toast.makeText(this, "Sign-up successful!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, AddTripActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Error saving data to Firebase: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            } else {
                Toast.makeText(
                    this,
                    "Error signing up: Phone number already exists or invalid.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    }
}

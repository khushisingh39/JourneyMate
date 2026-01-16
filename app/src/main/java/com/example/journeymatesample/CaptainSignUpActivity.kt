package com.example.journeymatesample

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import java.util.concurrent.TimeUnit

class CaptainSignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var verificationId: String
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_captain_signup)

        val etName: EditText = findViewById(R.id.name)
        val etEmail: EditText = findViewById(R.id.emailaddress)
        val etPhone: EditText = findViewById(R.id.phone_number)
        val etPassword: EditText = findViewById(R.id.password)
        val etConfirmPassword: EditText = findViewById(R.id.confirm_password)
        val etVehicle: EditText = findViewById(R.id.vehicle_number)
        val etLicense: EditText = findViewById(R.id.license_number)
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
            val phone = etPhone.text.toString().trim()
            if (phone.length != 10) {
                Toast.makeText(this, "Enter a valid 10-digit phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendOtp("+91$phone")
        }

        btnVerifyOtp.setOnClickListener {
            val otp = etOtp.text.toString().trim()
            if (otp.length == 6) verifyOtp(otp)
            else Toast.makeText(this, "Enter a valid OTP", Toast.LENGTH_SHORT).show()
        }

        btnSignIn.setOnClickListener {
            if (auth.currentUser != null) {
                startActivity(Intent(this, IDVerificationActivity::class.java))
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
                                Toast.makeText(this@CaptainSignUpActivity, "Automatically verified", Toast.LENGTH_SHORT).show()
                                saveCaptainData()
                            }
                        }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Toast.makeText(this@CaptainSignUpActivity, "OTP Verification Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    this@CaptainSignUpActivity.verificationId = verificationId
                    Toast.makeText(this@CaptainSignUpActivity, "OTP Sent Successfully", Toast.LENGTH_SHORT).show()

                    val etOtp: EditText = findViewById(R.id.otp)
                    val tvVerifyOtp: TextView = findViewById(R.id.verify_otp)
                    val etName: EditText = findViewById(R.id.name)
                    val etEmail: EditText = findViewById(R.id.emailaddress)
                    val etPhone: EditText = findViewById(R.id.phone_number)
                    val etPassword: EditText = findViewById(R.id.password)
                    val etConfirmPassword: EditText = findViewById(R.id.confirm_password)
                    val etVehicle: EditText = findViewById(R.id.vehicle_number)
                    val etLicense: EditText = findViewById(R.id.license_number)
                    val genderRadioGroup: RadioGroup = findViewById(R.id.gender)
                    val btnGenerateOtp: Button = findViewById(R.id.generate_otp_button)
                    val btnVerifyOtp: Button = findViewById(R.id.verify_otp_button)
                    val etVehicleType: EditText = findViewById(R.id.vehicle_type)
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
                    etVehicle.visibility = View.GONE
                    etVehicleType.visibility = View.GONE
                    etLicense.visibility = View.GONE
                    genderRadioGroup.visibility = View.GONE
                    btnGenerateOtp.visibility = View.GONE
                    tvCreateAccount.visibility = View.GONE
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun verifyOtp(otp: String) {
        val credential = PhoneAuthProvider.getCredential(verificationId, otp)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "OTP Verified Successfully!", Toast.LENGTH_SHORT).show()
                    saveCaptainData()
                } else {
                    Toast.makeText(this, "OTP Verification Failed!", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveCaptainData() {
        val name = findViewById<EditText>(R.id.name).text.toString().trim()
        val email = findViewById<EditText>(R.id.emailaddress).text.toString().trim()
        val phone = findViewById<EditText>(R.id.phone_number).text.toString().trim().filter { it.isDigit() }
        val password = findViewById<EditText>(R.id.password).text.toString().trim()
        val vehicle = findViewById<EditText>(R.id.vehicle_number).text.toString().trim()
        val license = findViewById<EditText>(R.id.license_number).text.toString().trim()
        val genderRadioGroup = findViewById<RadioGroup>(R.id.gender)
        val selectedGender = when (genderRadioGroup.checkedRadioButtonId) {
            R.id.gender_male -> "Male"
            R.id.gender_female -> "Female"
            R.id.gender_other -> "Other"
            else -> ""
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            val fcmToken = task.result

            val database = FirebaseDatabase.getInstance().reference
            val captain = mapOf(
                "name" to name,
                "email" to email,
                "phone" to phone,
                "gender" to selectedGender,
                "password" to password,
                "vehicle" to vehicle,
                "licenseNumber" to license,
                "fcmToken" to fcmToken
            )

            // ✅ Save ONLY in "drivers" table, NOT in "users"
            database.child("drivers").child(phone).setValue(captain)
                .addOnSuccessListener {
                    Toast.makeText(this, "Captain sign-up successful!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, IDVerificationActivity::class.java))
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Error saving data to Firebase: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }   }
}

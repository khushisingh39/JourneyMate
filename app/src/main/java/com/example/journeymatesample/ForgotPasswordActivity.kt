package com.example.journeymatesample

import android.content.Intent
import android.os.Bundle
import android.util.Log // ✅ Import for logging
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.database.*
import java.util.concurrent.TimeUnit

class ForgotPasswordActivity : AppCompatActivity() {
    private var auth: FirebaseAuth? = null
    private var usersRef: DatabaseReference? = null
    private lateinit var phoneInput: EditText
    private lateinit var otpInput: EditText
    private lateinit var newPasswordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var sendOtpButton: Button
    private lateinit var verifyOtpButton: Button
    private lateinit var resetPasswordButton: Button
    private var verificationId: String? = null
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        auth = FirebaseAuth.getInstance()
        usersRef = FirebaseDatabase.getInstance().getReference("users")

        phoneInput = findViewById(R.id.phone_number_input)
        otpInput = findViewById(R.id.otp_input)
        newPasswordInput = findViewById(R.id.new_password_input)
        confirmPasswordInput = findViewById(R.id.confirm_password_input)
        sendOtpButton = findViewById(R.id.send_otp_button)
        verifyOtpButton = findViewById(R.id.verify_otp_button)
        resetPasswordButton = findViewById(R.id.reset_password_button)

        sendOtpButton.setOnClickListener {
            var phone = phoneInput.text.toString().trim()

            Log.d("ForgotPassword", "Entered Phone: $phone") // ✅ Log phone input

            if (phone.isEmpty() || phone.length < 10) {
                Toast.makeText(this, "Enter a valid phone number", Toast.LENGTH_SHORT).show()
                Log.e("ForgotPassword", "Invalid phone number entered") // ✅ Log error
            } else {
                val formattedPhone = formatPhoneNumber(phone)
                Log.d("ForgotPassword", "Formatted Phone: $formattedPhone") // ✅ Log formatted number

                checkUserExists(phone.replace("+91", ""))
            }
        }

        verifyOtpButton.setOnClickListener {
            val otp = otpInput.text.toString().trim()
            Log.d("ForgotPassword", "Entered OTP: $otp") // ✅ Log OTP

            if (otp.isEmpty() || otp.length != 6) {
                Toast.makeText(this, "Enter a valid OTP", Toast.LENGTH_SHORT).show()
                Log.e("ForgotPassword", "Invalid OTP entered") // ✅ Log error
            } else {
                verifyOtp(verificationId, otp)
            }
        }

        resetPasswordButton.setOnClickListener {
            val newPassword = newPasswordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            Log.d("ForgotPassword", "New Password: $newPassword, Confirm Password: $confirmPassword") // ✅ Log passwords

            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please enter both passwords", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            updatePassword(newPassword)
        }
    }

    private fun formatPhoneNumber(phone: String): String {
        return if (phone.startsWith("+91")) phone else "+91$phone" // ✅ Ensure E.164 format
    }

    private fun checkUserExists(phone: String) {
        usersRef!!.child(phone)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        Log.d("ForgotPassword", "User exists. Sending OTP...") // ✅ Log user existence
                        sendOtp(formatPhoneNumber(phone))
                    } else {
                        Toast.makeText(this@ForgotPasswordActivity, "User not found", Toast.LENGTH_SHORT).show()
                        Log.e("ForgotPassword", "User does not exist") // ✅ Log error
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ForgotPasswordActivity, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
                    Log.e("ForgotPassword", "Database error: ${error.message}") // ✅ Log database error
                }
            })
    }

    private fun sendOtp(phone: String) {
        Log.d("ForgotPassword", "Sending OTP to: $phone") // ✅ Log OTP sending

        val options = PhoneAuthOptions.newBuilder(auth!!)
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Toast.makeText(this@ForgotPasswordActivity, "Verification Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("ForgotPassword", "Verification failed: ${e.message}") // ✅ Log verification error
                }

                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    verificationId = id
                    otpInput.visibility = View.VISIBLE
                    verifyOtpButton.visibility = View.VISIBLE
                    Toast.makeText(this@ForgotPasswordActivity, "OTP Sent", Toast.LENGTH_SHORT).show()
                    Log.d("ForgotPassword", "OTP Sent successfully") // ✅ Log success
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun verifyOtp(verificationId: String?, otp: String) {
        if (verificationId.isNullOrEmpty()) {
            Toast.makeText(this, "Verification ID is missing!", Toast.LENGTH_SHORT).show()
            Log.e("ForgotPassword", "Verification ID is null") // ✅ Log error
            return
        }

        val credential = PhoneAuthProvider.getCredential(verificationId, otp)
        signInWithCredential(credential)
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        auth!!.signInWithCredential(credential).addOnCompleteListener { task: Task<AuthResult?> ->
            if (task.isSuccessful) {
                Toast.makeText(this, "OTP Verified!", Toast.LENGTH_SHORT).show()
                Log.d("ForgotPassword", "OTP Verified!") // ✅ Log success
                newPasswordInput.visibility = View.VISIBLE
                confirmPasswordInput.visibility = View.VISIBLE
                resetPasswordButton.visibility = View.VISIBLE
            } else {
                Toast.makeText(this, "Invalid OTP! Try again.", Toast.LENGTH_SHORT).show()
                otpInput.text.clear()
                Log.e("ForgotPassword", "OTP verification failed") // ✅ Log error
            }
        }
    }

    private fun updatePassword(newPassword: String) {
        val phone = phoneInput.text.toString().trim()

        if (phone.isEmpty() || phone.length < 10) {
            Toast.makeText(this, "Invalid phone number", Toast.LENGTH_SHORT).show()
            Log.e("ForgotPassword", "Invalid phone format for password update") // ✅ Log error
            return
        }

        usersRef!!.child(phone).child("password").setValue(newPassword)
            .addOnSuccessListener {
                Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show()
                Log.d("ForgotPassword", "Password updated successfully!") // ✅ Log success
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update password!", Toast.LENGTH_SHORT).show()
                Log.e("ForgotPassword", "Failed to update password") // ✅ Log error
            }
    }
}

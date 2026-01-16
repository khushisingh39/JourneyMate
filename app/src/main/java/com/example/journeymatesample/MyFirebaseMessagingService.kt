package com.example.journeymatesample

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.util.Log


class MyFirebaseMessagingService : FirebaseMessagingService() {

    // This method will be used to test manually by calling sendNotification directly
    fun sendTestNotification() {
        val title = "Test Title"
        val message = "This is a test notification."
        sendNotification(title, message)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Handle incoming messages (for now, it just sends a notification)
        remoteMessage.data.isNotEmpty().let {
            val title = remoteMessage.data["title"] ?: "JourneyMate"
            val message = remoteMessage.data["body"] ?: "New notification"
            sendNotification(title, message)
        }
    }

    private fun sendNotification(title: String, messageBody: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "journeymate_channel"

        // Create the notification channel if on Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "JourneyMate Notifications", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        // Build the notification
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.baseline_3p_24)  // Replace with your own icon
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))

        // Show the notification
        notificationManager.notify(0, notificationBuilder.build())
    }

    // This function simulates sending a push notification for testing purposes
    fun sendPushNotificationForTesting(userId: String) {
        // Simulate getting a token (replace this with actual token retrieval)
        val simulatedFcmToken = "sample_fcm_token_for_testing"

        if (!simulatedFcmToken.isNullOrEmpty()) {
            // Create a dummy message data
            val messageData = mapOf(
                "title" to "Test Notification",
                "message" to "This is a test notification from Firebase"
            )

            // Simulate sending the notification (use remote message for testing)
            val remoteMessage = RemoteMessage.Builder(simulatedFcmToken)
                .setData(messageData)
                .build()

            // Send the message (just for testing, will not send if the server key isn't set up)
            FirebaseMessaging.getInstance().send(remoteMessage)
        } else {
            Log.e("MyFirebaseMessagingService", "FCM Token is null or empty")
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Refreshed token: $token")

        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val userId = sharedPreferences.getString("user_id", "")

        if (!userId.isNullOrEmpty()) {
            val database = FirebaseDatabase.getInstance().reference.child("Users").child(userId)
            database.child("fcmToken").setValue(token)
        }
    }

    private fun saveTokenToDatabase(token: String) {
        val userId = "CURRENT_USER_ID" // Replace with the actual logged-in user ID
        val database = FirebaseDatabase.getInstance().getReference("Users").child(userId)
        database.child("fcmToken").setValue(token)
    }

    fun sendBookingRequestNotification(initiatorToken: String, tripDetails: String) {
        val messageData = mapOf(
            "title" to "New Booking Request",
            "message" to "You have a new booking request: $tripDetails"
        )

        val remoteMessage = RemoteMessage.Builder(initiatorToken)
            .setData(messageData)
            .build()

        FirebaseMessaging.getInstance().send(remoteMessage)
    }
}

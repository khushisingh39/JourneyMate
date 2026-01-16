package com.example.journeymatesample

import android.content.Intent
import android.os.Bundle
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ChatListActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var chatListAdapter: ChatListAdapter
    private val acceptedUsersList = mutableListOf<users>()
    private val database = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_list)

        listView = findViewById(R.id.chatListView)
        chatListAdapter = ChatListAdapter(this, acceptedUsersList)
        listView.adapter = chatListAdapter

        loadAcceptedUsers()

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedUser = acceptedUsersList[position]
            val chatId = generateChatId(auth.currentUser?.uid ?: "", selectedUser.user_id)

            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("CHAT_ID", chatId)
            intent.putExtra("RECEIVER_NAME", selectedUser.name) // optional, for UI
            intent.putExtra("RECEIVER_UID", selectedUser.user_id)
            startActivity(intent)
        }

    }

    private fun loadAcceptedUsers() {
        val currentUserId = auth.currentUser?.uid ?: return
        val bookingRequestsRef = database.child("BookingRequests")

        bookingRequestsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                acceptedUsersList.clear()
                val acceptedUserIds = mutableSetOf<String>()


                for (requestSnapshot in snapshot.children) {
                    val request = requestSnapshot.getValue(BookingRequest::class.java)
                    if (request != null) {
                        if (request.initiatorId == currentUserId && request.status == "accepted") {
                            request.riderId?.let { acceptedUserIds.add(it) }
                        } else if (request.riderId == currentUserId && request.status == "accepted") {
                            request.initiatorId?.let { acceptedUserIds.add(it) }
                        }
                    }
                }

                if (acceptedUserIds.isEmpty()) {
                    Toast.makeText(this@ChatListActivity, "No chats available", Toast.LENGTH_LONG).show()
                    return
                }

                database.child("users").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(userSnapshot: DataSnapshot) {
                        acceptedUsersList.clear()

                        for (userSnap in userSnapshot.children) {
                            val userMap = userSnap.value as? Map<*, *> ?: continue
                            val userId = userMap["user_id"] as? String ?: continue

                            if (userId in acceptedUserIds) {
                                val name = userMap["name"] as? String ?: ""
                                val email = userMap["email"] as? String ?: ""
                                val phone = userMap["phone"] as? String ?: ""
                                val gender = userMap["gender"] as? String ?: ""
                                val uid = userMap["user_id"] as? String ?: ""
                                val fcmToken = userMap["fcmToken"] as? String ?: ""

                                val user = users(name, email, phone, gender, uid, fcmToken)
                                acceptedUsersList.add(user)

                            }
                        }

                        chatListAdapter.notifyDataSetChanged()

                        if (acceptedUsersList.isEmpty()) {
                            Toast.makeText(this@ChatListActivity, "No chats available", Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        println("DEBUG: Error fetching users: ${error.message}")
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                println("DEBUG: Failed to load booking requests - ${error.message}")
            }
        })
    }









    private fun generateChatId(user1: String, user2: String): String {
        return if (user1 < user2) "${user1}_${user2}" else "${user2}_${user1}"
    }

}
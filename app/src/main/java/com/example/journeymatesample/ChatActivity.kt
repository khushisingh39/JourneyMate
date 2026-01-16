package com.example.journeymatesample

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ChatActivity : AppCompatActivity() {

    private lateinit var messagesListView: ListView
    private lateinit var sendButton: Button
    private lateinit var messageEditText: EditText
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var chatId: String
    private lateinit var receiverUid: String
    private val messages = mutableListOf<String>()

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // View bindings
        messagesListView = findViewById(R.id.messagesListView)
        sendButton = findViewById(R.id.sendButton)
        messageEditText = findViewById(R.id.messageEditText)

        // Initialized message list adapter
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, messages)
        messagesListView.adapter = adapter

        // getting chat ID and receiver UID from intent
        chatId = intent.getStringExtra("CHAT_ID") ?: return
        receiverUid = intent.getStringExtra("RECEIVER_UID") ?: return

        // listening for chat updates
        listenForMessages()

        // Sending message on button click
        sendButton.setOnClickListener {
            val messageText = messageEditText.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
                messageEditText.text.clear()
            }
        }
    }

    private fun listenForMessages() {
        database.child("Chats").child(chatId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    messages.clear()
                    for (msgSnap in snapshot.children) {
                        val msgObj = msgSnap.getValue(ChatMessage::class.java)
                        msgObj?.let {
                            val prefix = if (it.senderId == auth.currentUser?.uid) "You: " else "Them: "
                            messages.add(prefix + it.message)
                        }
                    }
                    adapter.notifyDataSetChanged()
                    messagesListView.setSelection(messages.size - 1) // Scroll to latest
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ChatActivity, "Failed to load messages", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // Push message to Firebase
    private fun sendMessage(message: String) {
        val msgObj = ChatMessage(
            senderId = auth.currentUser?.uid ?: "",
            receiverId = receiverUid,
            message = message,
            timestamp = System.currentTimeMillis()
        )

        database.child("Chats").child(chatId).push().setValue(msgObj)
    }
}

package com.example.journeymatesample

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.BaseAdapter

class ChatListAdapter(private val context: Context, private val users: List<users>) : BaseAdapter() {

    override fun getCount(): Int = users.size

    override fun getItem(position: Int): Any = users[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_user, parent, false)
        val userNameTextView: TextView = view.findViewById(R.id.userNameTextView)
        val user = users[position]
        userNameTextView.text = user.name
        return view
    }
}
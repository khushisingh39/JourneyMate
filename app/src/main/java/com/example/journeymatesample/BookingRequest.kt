package com.example.journeymatesample

data class BookingRequest(
    var requestId: String = "",
    var tripId: String = "",
    var tripName: String = "",
    var riderId: String = "",
    var riderName: String = "",
    var initiatorId: String = "",
    var status: String = "pending"
)

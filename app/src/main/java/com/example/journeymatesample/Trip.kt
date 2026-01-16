package com.example.journeymatesample

data class Trip(
    var tripId: String? = "",  // Firebase ID
    var startLocation: String = "",
    var destination: String = "",
    var totalSeats: Int = 0,
    var availableSeats: Int = 0,
    var totalFare: Double = 0.0,
    var date: String = "",
    var startTime: String = "",
    var endTime: String = "",
    var cartype: String = "",
    var initiatorId: String = ""
)
package com.example.journeymatesample

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.widget.Toast
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError



class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "JourneyMate.db"
        private const val DATABASE_VERSION = 3

        // User table
        private const val TABLE_USER = "User"
        private const val COLUMN_USER_ID = "user_id"
        private const val COLUMN_USER_NAME = "name"
        private const val COLUMN_USER_EMAIL = "email"
        private const val COLUMN_USER_PHONE = "phone"
        private const val COLUMN_USER_PASSWORD = "password"
        private const val COLUMN_FCM_TOKEN = "fcm_token"

        // Trip table
        private const val TABLE_TRIP = "Trip"
        private const val COLUMN_TRIP_ID = "trip_id"
        private const val COLUMN_START_LOCATION = "start_location"
        private const val COLUMN_DESTINATION_LOCATION = "destination_location"
        private const val COLUMN_TOTAL_SEATS = "total_seats"
        private const val COLUMN_AVAILABLE_SEATS = "available_seats"
        private const val COLUMN_FARE = "fare"
        private const val COLUMN_TRIP_DATE = "trip_date"
        private const val COLUMN_START_TIME = "start_time"
        private const val COLUMN_END_TIME = "end_time"

        // Driver table
        private const val TABLE_DRIVER = "Driver"
        private const val COLUMN_DRIVER_ID = "driver_id"
        private const val COLUMN_DRIVER_NAME = "name"
        private const val COLUMN_DRIVER_EMAIL = "email"
        private const val COLUMN_DRIVER_PHONE = "phone"
        private const val COLUMN_DRIVER_PASSWORD = "password"
        private const val COLUMN_LICENSE_NUMBER = "license_number"
        private const val COLUMN_VEHICLE_NUMBER = "vehicle_number"
        private const val COLUMN_VEHICLE_TYPE = "vehicle_type"
        private const val COLUMN_GENDER = "gender"
    }

    // Firebase Database reference
    private val firebaseDatabase: DatabaseReference = FirebaseDatabase.getInstance().reference

    override fun onCreate(db: SQLiteDatabase?) {
        val createUserTable = """
            CREATE TABLE $TABLE_USER (
                $COLUMN_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USER_NAME TEXT NOT NULL,
                $COLUMN_USER_EMAIL TEXT NOT NULL,
                $COLUMN_USER_PHONE TEXT NOT NULL UNIQUE,
                $COLUMN_USER_PASSWORD TEXT NOT NULL,
                  $COLUMN_FCM_TOKEN TEXT
            )
        """.trimIndent()

        val createTripTable = """
            CREATE TABLE $TABLE_TRIP (
                $COLUMN_TRIP_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_START_LOCATION TEXT NOT NULL,
                $COLUMN_DESTINATION_LOCATION TEXT NOT NULL,
                $COLUMN_TOTAL_SEATS INTEGER NOT NULL,
                $COLUMN_AVAILABLE_SEATS INTEGER NOT NULL,
                $COLUMN_FARE REAL NOT NULL,
                $COLUMN_TRIP_DATE TEXT NOT NULL,
                $COLUMN_START_TIME TEXT NOT NULL,
                $COLUMN_END_TIME TEXT NOT NULL
            )
        """.trimIndent()

        val createDriverTable = """
            CREATE TABLE $TABLE_DRIVER (
                $COLUMN_DRIVER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_DRIVER_NAME TEXT NOT NULL,
                $COLUMN_DRIVER_EMAIL TEXT NOT NULL,
                $COLUMN_DRIVER_PHONE TEXT NOT NULL UNIQUE,
                $COLUMN_DRIVER_PASSWORD TEXT NOT NULL,
                $COLUMN_LICENSE_NUMBER TEXT NOT NULL,
                $COLUMN_VEHICLE_NUMBER TEXT NOT NULL,
                $COLUMN_VEHICLE_TYPE TEXT NOT NULL,
                $COLUMN_GENDER TEXT NOT NULL
            )
        """.trimIndent()

        db?.execSQL(createUserTable)
        db?.execSQL(createTripTable)
        db?.execSQL(createDriverTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion < DATABASE_VERSION) {
            db?.execSQL("DROP TABLE IF EXISTS $TABLE_TRIP")
            db?.execSQL("DROP TABLE IF EXISTS $TABLE_USER")
            db?.execSQL("DROP TABLE IF EXISTS $TABLE_DRIVER")
            onCreate(db)
        }
    }

    /**
     * Add a user to the SQLite database
     */
    fun addUser(name: String, email: String, phone: String, password: String,fcmToken: String, context: Context): Long {
        val db = this.writableDatabase

        //Check if user already exists
        if (isUserExists(phone)) {
            Toast.makeText(context, "User already exists!", Toast.LENGTH_SHORT).show()
            return -1
        }
        val values = ContentValues().apply {
            put(COLUMN_USER_NAME, name)
            put(COLUMN_USER_EMAIL, email)
            put(COLUMN_USER_PHONE, phone)
            put(COLUMN_USER_PASSWORD, password)
            put(COLUMN_FCM_TOKEN, fcmToken)
        }

        val result = db.insert(TABLE_USER, null, values)

        if (result > 0) {
            val user = mapOf(
                "name" to name,
                "email" to email,
                "phone" to phone,
                "password" to password,
                "fcmToken" to fcmToken
            )
            firebaseDatabase.child("users").child(phone).setValue(user)
            Toast.makeText(context, "User registered successfully!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Failed to register user. Phone number might already exist.", Toast.LENGTH_SHORT).show()
        }

        return result
    }

    /**
     * Add a trip to the SQLite database and sync with Firebase.
     */
    fun addTrip(
        startLocation: String,
        destinationLocation: String,
        totalSeats: Int,
        availableSeats: Int,
        fare: Double,
        date: String,
        startTime: String,
        endTime: String,
        context: Context
    ): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_START_LOCATION, startLocation)
            put(COLUMN_DESTINATION_LOCATION, destinationLocation)
            put(COLUMN_TOTAL_SEATS, totalSeats)
            put(COLUMN_AVAILABLE_SEATS, availableSeats)
            put(COLUMN_FARE, fare)
            put(COLUMN_TRIP_DATE, date)
            put(COLUMN_START_TIME, startTime)
            put(COLUMN_END_TIME, endTime)
        }

        val result = db.insert(TABLE_TRIP, null, values)

        if (result > 0) {
            val trip = mapOf(
                "startLocation" to startLocation,
                "destinationLocation" to destinationLocation,
                "totalSeats" to totalSeats,
                "availableSeats" to availableSeats,
                "fare" to fare,
                "date" to date,
                "startTime" to startTime,
                "endTime" to endTime
            )

            firebaseDatabase.child("Trips").push().setValue(trip)
                .addOnSuccessListener {
                    Toast.makeText(context, "Trip synced to Firebase successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to sync trip to Firebase: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(context, "Failed to save trip locally.", Toast.LENGTH_SHORT).show()
        }

        return result
    }

    /**
     * Retrieve all trips from SQLite.
     */
    fun getAllTrips(): List<Map<String, Any>> {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_TRIP,
            arrayOf(
                COLUMN_START_LOCATION,
                COLUMN_DESTINATION_LOCATION,
                COLUMN_TOTAL_SEATS,
                COLUMN_AVAILABLE_SEATS,
                COLUMN_FARE,
                COLUMN_TRIP_DATE,
                COLUMN_START_TIME,
                COLUMN_END_TIME
            ),
            null, null, null, null, null
        )

        val trips = mutableListOf<Map<String, Any>>()
        while (cursor.moveToNext()) {
            val trip = mapOf(
                "startLocation" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_START_LOCATION)),
                "destinationLocation" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESTINATION_LOCATION)),
                "totalSeats" to cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_SEATS)),
                "availableSeats" to cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_AVAILABLE_SEATS)),
                "fare" to cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_FARE)),
                "date" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TRIP_DATE)),
                "startTime" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_START_TIME)),
                "endTime" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_END_TIME))
            )
            trips.add(trip)
        }
        cursor.close()
        return trips
    }

    /**
     * Check if a trip exists in the SQLite database by trip_id
     */
    fun isTripExists(tripId: String?): Boolean {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_TRIP,
            arrayOf(COLUMN_TRIP_ID),
            "$COLUMN_TRIP_ID = ?",
            arrayOf(tripId),
            null, null, null
        )
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    /**
     * Fetch trips from Firebase and sync them to SQLite.
     */
    fun syncTripsFromFirebase(context: Context) {
        firebaseDatabase.child("Trips").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val db = this@DatabaseHelper.writableDatabase
                for (tripSnapshot in snapshot.children) {
                    val trip = tripSnapshot.getValue(Trip::class.java)
                    if (trip != null) {
                        // Check if the trip exists in SQLite (e.g., by trip_id)
                        if (!isTripExists(trip.tripId)) {
                            addTrip(
                                trip.startLocation,
                                trip.destination,
                                trip.totalSeats,
                                trip.availableSeats,
                                trip.totalFare,
                                trip.date,
                                trip.startTime,
                                trip.endTime,
                                context
                            )
                        }
                    }
                }
                Toast.makeText(context, "Trips synced from Firebase", Toast.LENGTH_SHORT).show()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to sync trips from Firebase: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun addDriver(
        name: String, email: String, phone: String, password: String,
        licenseNumber: String, vehicleNumber: String, vehicleType: String, gender: String, context: Context
    ): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_DRIVER_NAME, name)
            put(COLUMN_DRIVER_EMAIL, email)
            put(COLUMN_DRIVER_PHONE, phone)
            put(COLUMN_DRIVER_PASSWORD, password)
            put(COLUMN_LICENSE_NUMBER, licenseNumber)
            put(COLUMN_VEHICLE_NUMBER, vehicleNumber)
            put(COLUMN_VEHICLE_TYPE, vehicleType)
            put(COLUMN_GENDER, gender)
        }

        val result = db.insert(TABLE_DRIVER, null, values)

        if (result > 0) {
            val driverData = mapOf(
                "name" to name,
                "email" to email,
                "phone" to phone,
                "licenseNumber" to licenseNumber,
                "vehicleNumber" to vehicleNumber,
                "vehicleType" to vehicleType,
                "gender" to gender
            )

            firebaseDatabase.child("Drivers").child(phone).setValue(driverData)
                .addOnSuccessListener {
                    Toast.makeText(context, "Driver registered successfully and synced to Firebase!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to sync driver to Firebase: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(context, "Failed to register driver.", Toast.LENGTH_SHORT).show()
        }

        return result
    }
    fun isUserExists(phone: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_USER WHERE $COLUMN_USER_PHONE=?", arrayOf(phone))
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }
}

    /**
     * Check if the trip already exists in SQLite based on trip_id.
     */



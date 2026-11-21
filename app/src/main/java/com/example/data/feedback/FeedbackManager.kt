package com.example.data.feedback

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.example.BuildConfig
import com.example.data.model.Vehicle
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class FeedbackEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val rating: Int, // 1 to 5
    val category: String, // "Feature Request", "Bug Report", "Fuel Tracking", "UI / Usability", "Other"
    val message: String,
    val userEmail: String = "",
    val deviceDiagnostics: String = ""
) {
    val formattedDate: String
        get() = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
}

object FeedbackManager {
    private const val PREFS_NAME = "pitstop_feedback_prefs"
    private const val KEY_FEEDBACK_LIST = "feedback_history_json"
    const val SUPPORT_EMAIL = "support@pitstop.app"

    fun saveFeedback(context: Context, entry: FeedbackEntry) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentList = getFeedbackHistory(context).toMutableList()
        currentList.add(0, entry) // Add newest first

        val jsonArray = JSONArray()
        currentList.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("timestamp", item.timestamp)
                put("rating", item.rating)
                put("category", item.category)
                put("message", item.message)
                put("userEmail", item.userEmail)
                put("deviceDiagnostics", item.deviceDiagnostics)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_FEEDBACK_LIST, jsonArray.toString()).apply()
    }

    fun getFeedbackHistory(context: Context): List<FeedbackEntry> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawJson = prefs.getString(KEY_FEEDBACK_LIST, null) ?: return emptyList()

        val list = mutableListOf<FeedbackEntry>()
        try {
            val jsonArray = JSONArray(rawJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    FeedbackEntry(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        rating = obj.optInt("rating", 5),
                        category = obj.optString("category", "General Feedback"),
                        message = obj.optString("message", ""),
                        userEmail = obj.optString("userEmail", ""),
                        deviceDiagnostics = obj.optString("deviceDiagnostics", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun clearFeedbackHistory(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_FEEDBACK_LIST).apply()
    }

    fun createEmailIntent(entry: FeedbackEntry): Intent {
        val stars = "★".repeat(entry.rating.coerceIn(1, 5))
        val subject = "[PitStop Feedback] ${entry.category} ($stars)"
        val body = buildString {
            append("--- PitStop User Feedback ---\n")
            append("Category: ${entry.category}\n")
            append("Rating: ${entry.rating} / 5 ($stars)\n")
            if (entry.userEmail.isNotBlank()) {
                append("User Contact: ${entry.userEmail}\n")
            }
            append("Submitted At: ${entry.formattedDate}\n\n")
            append("--- Message ---\n")
            append("${entry.message}\n\n")
            if (entry.deviceDiagnostics.isNotBlank()) {
                append("--- Diagnostics & App Info ---\n")
                append(entry.deviceDiagnostics)
            }
        }

        val mailtoUri = Uri.parse("mailto:$SUPPORT_EMAIL" +
                "?subject=" + Uri.encode(subject) +
                "&body=" + Uri.encode(body))

        return Intent(Intent.ACTION_SENDTO).apply {
            data = mailtoUri
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun createShareIntent(entry: FeedbackEntry): Intent {
        val stars = "★".repeat(entry.rating.coerceIn(1, 5))
        val text = buildString {
            append("[PitStop App Feedback]\n")
            append("Category: ${entry.category} | Rating: $stars\n\n")
            append("${entry.message}\n\n")
            if (entry.deviceDiagnostics.isNotBlank()) {
                append("Diagnostics:\n${entry.deviceDiagnostics}")
            }
        }

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "PitStop Feedback: ${entry.category}")
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun getDeviceDiagnosticString(activeVehicle: Vehicle?): String {
        return buildString {
            append("App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n")
            append("Android OS: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
            append("Device: ${Build.MANUFACTURER} ${Build.MODEL}\n")
            if (activeVehicle != null && activeVehicle.id > 0) {
                append("Active Vehicle: ${activeVehicle.name} (${activeVehicle.vehicleType}, ${activeVehicle.year})\n")
                append("Odometer: ${activeVehicle.currentOdometer.toInt()} ${activeVehicle.preferredDistanceUnit}\n")
            }
        }
    }
}

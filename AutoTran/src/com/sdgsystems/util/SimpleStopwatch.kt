package com.sdgsystems.util

import com.sdgsystems.util.SimpleTimeStamp;
import java.text.SimpleDateFormat
import java.util.*

class SimpleStopwatch {
    private var startTime: Long = 0
    private var stopTime: Long = 0

    fun startTimer(): Long {
        startTime = System.currentTimeMillis()
        return startTime
    }

    fun stopTimer(): Double {
        stopTime = System.currentTimeMillis()
        return calculateElapsedTime()
    }

    private fun calculateElapsedTime(): Double {
        val elapsedMillis = stopTime - startTime
        return elapsedMillis / 1000.0
    }

    fun getStartTimeMillis(): Long {
        return startTime
    }

    fun getStopTimeMillis(): Long {
        return stopTime
    }

    fun getStartTimestamp(utc: Boolean): String {
        return getFormattedTimestamp(startTime, utc)
    }

    fun getStopTimestamp(utc: Boolean): String {
        return getFormattedTimestamp(stopTime, utc)
    }

    private fun getFormattedTimestamp(timeInMillis: Long, utc: Boolean): String {
        /*
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault())
        if (utc) {
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        }
        return dateFormat.format(Date(timeInMillis))
         */
        return SimpleTimeStamp(utc, Date(timeInMillis)).getTimeStamp()
    }

    fun getElapsedTime(): Double {
        return if (stopTime > startTime) {
            calculateElapsedTime()
        } else {
            val currentElapsedTime = System.currentTimeMillis() - startTime
            currentElapsedTime / 1000.0
        }
    }
}
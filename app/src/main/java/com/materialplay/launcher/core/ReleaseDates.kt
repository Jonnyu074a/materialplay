package com.materialplay.launcher.core

/** Formats a date string coming from a metadata provider into yyyy-MM-dd-ish text. */
object ReleaseDates {
    fun igdbTimestamp(seconds: Long?): String? {
        if (seconds == null || seconds <= 0) return null
        return Text.date(seconds * 1000)
    }
}

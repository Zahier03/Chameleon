package com.sotech.chameleon.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * Formats file size from bytes to human readable format
 */
fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0

    return when {
        gb >= 1.0 -> String.format("%.2f GB", gb)
        mb >= 1.0 -> String.format("%.1f MB", mb)
        kb >= 1.0 -> String.format("%.0f KB", kb)
        else -> "$bytes B"
    }
}

/**
 * Formats a timestamp to a readable date string
 */
fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

/**
 * Formats large numbers with K/M suffixes
 */
fun formatNumber(num: Int): String {
    return when {
        num >= 1000000 -> "%.1fM".format(num / 1000000f)
        num >= 1000 -> "%.1fK".format(num / 1000f)
        else -> num.toString()
    }
}

/**
 * Formats token count with appropriate suffix
 */
fun formatTokenCount(count: Int): String {
    return when {
        count >= 1000 -> "${count / 1000}K"
        else -> count.toString()
    }
}

/**
 * Formats duration in milliseconds to readable format
 */
fun formatDuration(milliseconds: Long): String {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    val hours = minutes / 60

    return when {
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m ${seconds % 60}s"
        seconds > 0 -> "${seconds}s"
        else -> "${milliseconds}ms"
    }
}

/**
 * Formats speed (tokens per second) to readable format
 */
fun formatSpeed(speed: Float): String {
    return when {
        speed <= 0 -> "N/A"
        speed < 1 -> "%.2f".format(speed)
        speed < 10 -> "%.1f".format(speed)
        else -> "%.0f".format(speed)
    }
}

/**
 * Formats latency in seconds to readable format
 */
fun formatLatency(seconds: Float): String {
    return when {
        seconds < 0 -> "N/A"
        seconds < 1 -> "%.0fms".format(seconds * 1000)
        else -> "%.2fs".format(seconds)
    }
}
package com.easytier

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 日志缓冲区：同时输出到 logcat + 内存缓冲区
 * 方便在 App 内查看和复制
 */
object AppLogger {
    private const val MAX_LINES = 300
    private val buffer = ArrayList<String>()
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    @Synchronized
    fun d(tag: String, msg: String) {
        Log.d(tag, msg)
        append("D", tag, msg)
    }

    @Synchronized
    fun i(tag: String, msg: String) {
        Log.i(tag, msg)
        append("I", tag, msg)
    }

    @Synchronized
    fun w(tag: String, msg: String, t: Throwable? = null) {
        if (t != null) {
            Log.w(tag, msg, t)
            val sw = java.io.StringWriter()
            val pw = java.io.PrintWriter(sw)
            t.printStackTrace(pw)
            append("W", tag, "$msg\n${sw}")
        } else {
            Log.w(tag, msg)
            append("W", tag, msg)
        }
    }

    @Synchronized
    fun e(tag: String, msg: String, t: Throwable? = null) {
        if (t != null) {
            Log.e(tag, msg, t)
            val sw = java.io.StringWriter()
            val pw = java.io.PrintWriter(sw)
            t.printStackTrace(pw)
            append("E", tag, "$msg\n${sw}")
        } else {
            Log.e(tag, msg)
            append("E", tag, msg)
        }
    }

    private fun append(level: String, tag: String, msg: String) {
        val time = dateFormat.format(Date())
        buffer.add("$time $level/$tag: $msg")
        if (buffer.size > MAX_LINES) {
            buffer.removeAt(0)
        }
    }

    @Synchronized
    fun getLogs(): String = buffer.joinToString("\n")

    /**
     * 返回最近 [count] 条日志，避免全部加载导致卡顿
     */
    @Synchronized
    fun getLatestLines(count: Int): String {
        if (buffer.size <= count) return buffer.joinToString("\n")
        return buffer.subList(buffer.size - count, buffer.size).joinToString("\n")
    }

    @Synchronized
    fun clear() {
        buffer.clear()
    }

    @Synchronized
    fun size(): Int = buffer.size
}

package top.loryn.support

import org.slf4j.Logger
import org.slf4j.Marker
import org.slf4j.helpers.MessageFormatter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object StdOutLogger : Logger {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

    private fun String.fixedLength(length: Int) =
        if (this.length > length) substring(this.length - length) else padStart(length)

    private fun printFormattedMessage(level: String, msg: String?, args: Array<out Any?>) {
        MessageFormatter.arrayFormat(msg, args).let {
            println(
                "${formatter.format(LocalDateTime.now())} ${level.fixedLength(5)} --- [${
                    Thread.currentThread().name.fixedLength(15)
                }] : ${it.message}"
            )
            it.throwable?.printStackTrace()
        }
    }

    override fun getName(): String {
        return StdOutLogger::class.java.name
    }

    private fun trace0(msg: String?, vararg args: Any?) {
        printFormattedMessage("TRACE", msg, args)
    }

    override fun isTraceEnabled(): Boolean {
        return true
    }

    override fun trace(msg: String?) {
        trace0(msg)
    }

    override fun trace(format: String?, arg: Any?) {
        trace0(format, arg)
    }

    override fun trace(format: String?, arg1: Any?, arg2: Any?) {
        trace0(format, arg1, arg2)
    }

    override fun trace(format: String?, vararg arguments: Any?) {
        trace0(format, *arguments)
    }

    override fun trace(msg: String?, t: Throwable?) {
        trace0(msg, t)
    }

    override fun isTraceEnabled(marker: Marker?): Boolean {
        return true
    }

    override fun trace(marker: Marker?, msg: String?) {
        trace(msg)
    }

    override fun trace(marker: Marker?, format: String?, arg: Any?) {
        trace(format, arg)
    }

    override fun trace(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        trace(format, arg1, arg2)
    }

    override fun trace(marker: Marker?, format: String?, vararg argArray: Any?) {
        trace(format, argArray)
    }

    override fun trace(marker: Marker?, msg: String?, t: Throwable?) {
        trace(msg, t)
    }

    private fun debug0(msg: String?, vararg args: Any?) {
        printFormattedMessage("DEBUG", msg, args)
    }

    override fun isDebugEnabled(): Boolean {
        return true
    }

    override fun debug(msg: String?) {
        debug0(msg)
    }

    override fun debug(format: String?, arg: Any?) {
        debug0(format, arg)
    }

    override fun debug(format: String?, arg1: Any?, arg2: Any?) {
        debug0(format, arg1, arg2)
    }

    override fun debug(format: String?, vararg arguments: Any?) {
        debug0(format, *arguments)
    }

    override fun debug(msg: String?, t: Throwable?) {
        debug0(msg, t)
    }

    override fun isDebugEnabled(marker: Marker?): Boolean {
        return true
    }

    override fun debug(marker: Marker?, msg: String?) {
        debug(msg)
    }

    override fun debug(marker: Marker?, format: String?, arg: Any?) {
        debug(format, arg)
    }

    override fun debug(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        debug(format, arg1, arg2)
    }

    override fun debug(marker: Marker?, format: String?, vararg arguments: Any?) {
        debug(format, *arguments)
    }

    override fun debug(marker: Marker?, msg: String?, t: Throwable?) {
        debug(msg, t)
    }

    private fun info0(msg: String?, vararg args: Any?) {
        printFormattedMessage("INFO", msg, args)
    }

    override fun isInfoEnabled(): Boolean {
        return true
    }

    override fun info(msg: String?) {
        info0(msg)
    }

    override fun info(format: String?, arg: Any?) {
        info0(format, arg)
    }

    override fun info(format: String?, arg1: Any?, arg2: Any?) {
        info0(format, arg1, arg2)
    }

    override fun info(format: String?, vararg arguments: Any?) {
        info0(format, *arguments)
    }

    override fun info(msg: String?, t: Throwable?) {
        info0(msg, t)
    }

    override fun isInfoEnabled(marker: Marker?): Boolean {
        return true
    }

    override fun info(marker: Marker?, msg: String?) {
        info(msg)
    }

    override fun info(marker: Marker?, format: String?, arg: Any?) {
        info(format, arg)
    }

    override fun info(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        info(format, arg1, arg2)
    }

    override fun info(marker: Marker?, format: String?, vararg arguments: Any?) {
        info(format, *arguments)
    }

    override fun info(marker: Marker?, msg: String?, t: Throwable?) {
        info(msg, t)
    }

    private fun warn0(msg: String?, vararg args: Any?) {
        printFormattedMessage("WARN", msg, args)
    }

    override fun isWarnEnabled(): Boolean {
        return true
    }

    override fun warn(msg: String?) {
        warn0(msg)
    }

    override fun warn(format: String?, arg: Any?) {
        warn0(format, arg)
    }

    override fun warn(format: String?, vararg arguments: Any?) {
        warn0(format, *arguments)
    }

    override fun warn(format: String?, arg1: Any?, arg2: Any?) {
        warn0(format, arg1, arg2)
    }

    override fun warn(msg: String?, t: Throwable?) {
        warn0(msg, t)
    }

    override fun isWarnEnabled(marker: Marker?): Boolean {
        return true
    }

    override fun warn(marker: Marker?, msg: String?) {
        warn(msg)
    }

    override fun warn(marker: Marker?, format: String?, arg: Any?) {
        warn(format, arg)
    }

    override fun warn(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        warn(format, arg1, arg2)
    }

    override fun warn(marker: Marker?, format: String?, vararg arguments: Any?) {
        warn(format, *arguments)
    }

    override fun warn(marker: Marker?, msg: String?, t: Throwable?) {
        warn(msg, t)
    }

    private fun error0(msg: String?, vararg args: Any?) {
        printFormattedMessage("ERROR", msg, args)
    }

    override fun isErrorEnabled(): Boolean {
        return true
    }

    override fun error(msg: String?) {
        error0(msg)
    }

    override fun error(format: String?, arg: Any?) {
        error0(format, arg)
    }

    override fun error(format: String?, arg1: Any?, arg2: Any?) {
        error0(format, arg1, arg2)
    }

    override fun error(format: String?, vararg arguments: Any?) {
        error0(format, *arguments)
    }

    override fun error(msg: String?, t: Throwable?) {
        error0(msg, t)
    }

    override fun isErrorEnabled(marker: Marker?): Boolean {
        return true
    }

    override fun error(marker: Marker?, msg: String?) {
        error(msg)
    }

    override fun error(marker: Marker?, format: String?, arg: Any?) {
        error(format, arg)
    }

    override fun error(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        error(format, arg1, arg2)
    }

    override fun error(marker: Marker?, format: String?, vararg arguments: Any?) {
        error(format, *arguments)
    }

    override fun error(marker: Marker?, msg: String?, t: Throwable?) {
        error(msg, t)
    }
}

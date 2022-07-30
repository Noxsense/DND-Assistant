package de.nox.dndassistant.core

enum class LoggingLevel {
	ERROR, WARN, INFO, VERBOSE, DEBUG
}

public object LoggerFactory {

	// shared variable, so all loggers from this factory are on the same logging level.
	private var LOG_LEVEL: LoggingLevel = LoggingLevel.INFO

	public fun getLogger(tag: String) : Logger
		= object : Logger {
			private fun now(): String
				= "%-19s".format(System.currentTimeMillis()) // yyyy-mm-dd HH:MM:SS

			override fun getLoggingLevel() = LoggerFactory.LOG_LEVEL

			override fun displayLevel(t: LoggingLevel) {
				LoggerFactory.LOG_LEVEL = t
			}

			override fun log(t: LoggingLevel, msg: Any?) {
				// abort if log level not demaded
				if (t > LOG_LEVEL) return

				val now = ""
				val l = t.name.first()

				// split message lines and print them all indented.
				("$msg").split("\n").forEach { line ->
					println("$now $l $tag  -  $line")
				}
			}
		}
}

interface Logger {
	abstract fun log(t: LoggingLevel, msg: Any?)

	fun displayLevel(t: LoggingLevel)
	fun getLoggingLevel() : LoggingLevel

	fun error(msg: Any?) = log(LoggingLevel.ERROR, msg)
	fun info(msg: Any?) = log(LoggingLevel.INFO, msg)
	fun warn(msg: Any?) = log(LoggingLevel.WARN, msg)
	fun verbose(msg: Any?) = log(LoggingLevel.VERBOSE, msg)
	fun debug(msg: Any?) = log(LoggingLevel.DEBUG, msg)
}

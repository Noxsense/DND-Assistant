package de.nox.dndassistant.core

enum class LoggingLevel {
	ERROR, INFO, WARN, VERBOSE, DEBUG
}

object LoggerFactory {
	fun getLogger(tag: String) : Logger
		= object : Logger {
			private fun now(): String
				= "%-19s".format(System.currentTimeMillis()) // yyyy-mm-dd HH:MM:SS

			override fun log(t: LoggingLevel, msg: Any?) {
				println("${t.name.first()} ${tag}  -  $msg")
			}
		}
}

interface Logger {
	abstract fun log(t: LoggingLevel, msg: Any?)

	fun error(msg: Any?) = log(LoggingLevel.ERROR, msg)
	fun info(msg: Any?) = log(LoggingLevel.INFO, msg)
	fun warn(msg: Any?) = log(LoggingLevel.WARN, msg)
	fun verbose(msg: Any?) = log(LoggingLevel.VERBOSE, msg)
	fun debug(msg: Any?) = log(LoggingLevel.DEBUG, msg)
}

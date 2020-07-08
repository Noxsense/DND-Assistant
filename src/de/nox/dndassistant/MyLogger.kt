package de.nox.dndassistant

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object LoggerFactory {
	fun getLogger(name: String) : Logger = Logger(name)
}

enum class LoggingLevel {
	INFO, ERROR, VERBOSE, DEBUG
}

data class Logger(
	val name: String,
	val level: LoggingLevel = LoggingLevel.ERROR
) {

	private val formatter = DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm:ss")

	fun log(t: LoggingLevel, s: Any?) {
		print("%s %-16s %-8s - ".format(
			LocalDateTime.now().format(formatter),
			name + ":",
			t.toString()))
		println(s)
	}

	fun info(s: Any?) {
		log(LoggingLevel.INFO, s)
	}

	fun error(s: Any?) {
		log(LoggingLevel.ERROR, s)
	}

	fun verbose(s: Any?) {
		log(LoggingLevel.VERBOSE, s)
	}

	fun debug(s: Any?) {
		log(LoggingLevel.DEBUG, s)
	}
}

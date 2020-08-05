package de.nox.dndassistant

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object LoggerFactory {
	fun getLogger(name: String) : Logger = Logger(name)
}

enum class LoggingLevel {
	INFO, ERROR, WARN, VERBOSE, DEBUG
}

data class Logger(
	val name: String,
	val level: LoggingLevel = LoggingLevel.ERROR
) {

	private val formatter = DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm:ss")

	fun log(t: LoggingLevel, s: Any?) {
		println("%s %-16s %-8s - ".format(
			LocalDateTime.now().format(formatter), name + ":", t.toString()) +
			s) // avoid formatting.
	}

	fun log(t: String, s: Any?) {
		println("%s %-16s %-8s - ".format(
			LocalDateTime.now().format(formatter), name + ":", t) +
			s) // avoid formatting.
	}

	fun info(s: Any?) {
		log(LoggingLevel.INFO, s)
	}

	fun warn(s: Any?) {
		log(LoggingLevel.WARN, s)
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

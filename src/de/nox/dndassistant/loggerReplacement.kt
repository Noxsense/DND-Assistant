package de.nox.dndassistant

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

	fun log(t: LoggingLevel, s: Any?) {
		print("Logger %s: %-10s - ".format(name, t.toString()))
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

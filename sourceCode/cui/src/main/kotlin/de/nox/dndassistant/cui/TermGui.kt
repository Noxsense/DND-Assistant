package de.nox.dndassistant.cui

import de.nox.dndassistant.core.*

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun main(args: Array<String>) {
	println(
		"""
		DnD Application, display stats and roll your dice!
		Happy Gaming! :D
		==================================================
		${args}
		""".trimIndent())

	// XXX (2020-11-27) implement me
}

object LoggerFactory {
	fun getLogger(tag: String) : Logger
		= object : Logger {
			private val formatter
				= DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm:ss")

			private fun now(): String
				= LocalDateTime.now().format(formatter)

			override fun log(t: LoggingLevel, msg: Any?) {
				println("${now()} ${t.name.first()} ${tag}  -  $msg")
			}
		}
}

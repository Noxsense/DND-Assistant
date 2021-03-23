package de.nox.dndassistant.cui

import de.nox.dndassistant.core.*

import java.io.File
import java.io.FileNotFoundException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val log: Logger = LoggerFactory.getLogger("TermGui").also {
	it.displayLevel(LoggingLevel.DEBUG)
}

fun main(args: Array<String>) {
	println(
		"""
		DnD Application, display stats and roll your dice!
		Happy Gaming! :D
		==================================================
		${args}
		""".trimIndent())

	// XXX (2020-11-27) implement me

	val filename = "../core/src/test/resources/Hero.json"
	try {
		var hero = loadHero(File(filename).getAbsolutePath())
		println(hero.toMarkdown())
		File("/tmp/Hero.md").writeText(hero.toMarkdown())
	} catch (e: FileNotFoundException) {
		log.error("File \"$filename\" not found.")
	}

	println("Done")
}

object LoggerFactory {
	private var LOG_LEVEL: LoggingLevel = LoggingLevel.INFO

	public fun getLogger(tag: String) : Logger
		= object : Logger {
			private val formatter
				= DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm:ss")

			private fun now(): String
				= LocalDateTime.now().format(formatter)

			override fun displayLevel(t: LoggingLevel) {
				LOG_LEVEL = t
			}

			override fun log(t: LoggingLevel, msg: Any?) {
				// abort if log level not demaded
				if (t > LOG_LEVEL) return

				val now = now()
				val l = t.name.first()

				// split message lines and print them all indented.
				("$msg").split("\n").forEach { line ->
					println("$now $l $tag  -  $line".trimEnd())
				}
			}
		}
}

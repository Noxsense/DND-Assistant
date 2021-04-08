package de.nox.dndassistant.app

import android.util.Log

import de.nox.dndassistant.core.Logger
import de.nox.dndassistant.core.LoggerFactory
import de.nox.dndassistant.core.LoggingLevel

/** Android specific log. */
public object LoggerFactory {
	fun getLogger(tag: String) : Logger = AndroidLogger(tag)
}

public class AndroidLogger(val tag: String) : Logger {

	private var LOG_LEVEL: LoggingLevel = LoggingLevel.INFO

	override fun getLoggingLevel() = LOG_LEVEL

	override fun displayLevel(t: LoggingLevel) {
		LOG_LEVEL = t
	}

	override fun log(t: LoggingLevel, msg: Any?) {
		when (t) {
			LoggingLevel.ERROR -> Log.e(tag, "${msg}")
			LoggingLevel.INFO -> Log.i(tag, "${msg}")
			LoggingLevel.WARN -> Log.w(tag, "${msg}")
			LoggingLevel.VERBOSE -> Log.v(tag, "${msg}")
			LoggingLevel.DEBUG -> Log.d(tag, "${msg}")
		}
	}
}

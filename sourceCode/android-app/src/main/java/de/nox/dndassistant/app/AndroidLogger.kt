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

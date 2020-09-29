package de.nox.dndassistant.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import kotlinx.android.synthetic.main.activity_main.*

class NextActivity : AppCompatActivity() {

	private val LOG_TAG = "D&D Next"

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		Log.i(LOG_TAG, "Opened next view.")

		val bundle: Bundle? = intent.extras

		Log.i(LOG_TAG, "Extract the bundle.")

		/* Get the value. */
		val value = bundle?.getString("key") ?: "value-alternative"

		Log.d(LOG_TAG, "value: $value")
	}
}

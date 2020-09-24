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

		text.text = "Value: $value"

		button.setOnClickListener {
			if (true) {
				// Success -> Return to Main.
				Toast.makeText(this, "Ok", Toast.LENGTH_LONG).show()
				onBackPressed()
			} else {
				// hint possible errors.
				Toast.makeText(this, "Nope", Toast.LENGTH_LONG).show()
			}
		}
	}
}

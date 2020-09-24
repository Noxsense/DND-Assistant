package de.nox.dndassistant.app

import de.nox.dndassistant.core.*

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

	private val LOG_TAG = "D&D Main"

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		Log.d(LOG_TAG, "Initiated Activity.")

		// val pc = playgroundWithOnyx()

		/* Show Player Character Name. */
		text.text = "{pc.name}" // pc.name

		val xs = listOf("a", "b", "c")

		val adapter = ArrayAdapter(
			this@MainActivity,
			android.R.layout.simple_list_item_1,
			// pc.bags.toList())
			xs)

		list.setAdapter(adapter)
		list.setOnItemClickListener { _ , view, position, id ->
			Toast.makeText(this, "Show item ${xs[position]}", Toast.LENGTH_LONG).show()
		}

		Log.d(LOG_TAG, "Initiated Listview.")

		/* Click a button to open a new view. */
		button.setOnClickListener {
			Log.i(LOG_TAG, "Clicked the button")

			val intent = Intent(this@MainActivity, NextActivity::class.java)
			intent.putExtra("key", "value")
			startActivity(intent)
		}

		Log.d(LOG_TAG, "Initiated Button.")
	}
}

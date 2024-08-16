package de.nox.dndassistant.app

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.GridLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import de.nox.dndassistant.app.databinding.ActivityMainBinding
// import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), View.OnClickListener {

	private val log = LoggerFactory.getLogger("D&D Main")

	private lateinit var li: LayoutInflater

	private lateinit var binding: ActivityMainBinding

	companion object {
		lateinit var instance: AppCompatActivity
			private set
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		/* default loading. */
		super.onCreate(savedInstanceState)
		// setContentView(R.layout.activity_main)

		li = LayoutInflater.from(this)

		binding = ActivityMainBinding.inflate(li)
		setContentView(binding.root)

		instance = this

		log.debug("Initiated Activity.")

		/* Update the hero specific panels:
		 * Fill them with current hero's data. */
		// TODO (2024-08-13) replace setup views.

		// TODO (2020-11-22) keep screen on?
		window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
	}

	// TODO (2020-10-12) handle on resume, to reload views after standby?
	// override fun onResume()

	// TODO (2020-10-06) separate single logics. (onClick(view))
	override fun onClick(view: View) {
		log.debug("Clicked on $view")

		when (view.getId()) {
			// R.id.label_rolls -> {
			binding.labelRolls -> {
				/* Update roll shower. */
				// notifyRollsUpdated()
			}

			else -> {
				Toast.makeText(this@MainActivity,
					"Clicked on ${view}",
					Toast.LENGTH_SHORT
				).show()
			  // TODO (2020-09-29) rest case? any click without purpose?
			}
		}
	}

	/** Toggle View's visibility between "GONE" and "VISIBLE".
	 * @return true, if visible. */
	fun View.toggleVisibility(): Boolean
		= when (visibility) {
			View.GONE -> true.also {
				visibility = View.VISIBLE
				log.debug("VISIBLE    $this")
			}
			else -> false.also {
				visibility = View.GONE
				log.debug("GONE       $this")
			}
		}
}

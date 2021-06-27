package de.nox.dndassistant.app

import android.app.AlertDialog
import android.app.Dialog
import android.app.Activity
import android.app.AlertDialog.Builder
import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.EditText
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.GridView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

// import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_dice.*

// import de.nox.dndassistant.core.toAttackString
import de.nox.dndassistant.core.Ability
// import de.nox.dndassistant.core.Attack
// import de.nox.dndassistant.core.CastSpell
import de.nox.dndassistant.core.Hero
import de.nox.dndassistant.core.RollHistory
import de.nox.dndassistant.core.RollingTerm
import de.nox.dndassistant.core.Number
// import de.nox.dndassistant.core.SimpleSpell
import de.nox.dndassistant.core.Speciality

public class RollingTermDialog(activity: Activity) : Dialog(activity, R.drawable.framed), View.OnClickListener, View.OnLongClickListener, View.OnKeyListener {

	lateinit var insertTermView : EditText
	lateinit var insertTermRollView : TextView
	lateinit var insertTermAddView : TextView
	lateinit var extraTermView : GridView
	lateinit var historyView : ListView

	val rollHistoryAdapter by lazy { RollHistoryAdapter(context) }

	val extraTermAdapter by lazy {
		val exampleDice = mutableListOf(
			"d2", "d4", "d6", "d8",
			"d10", "d12", "d20", "d100"
		)

		ArrayAdapter<String>(
			context,
			R.layout.list_item_rollingterm, R.id.term,
			exampleDice,
		)
	}

	override protected fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		// requestWindowFeature(Window.FEATURE_NO_TITLE)

		getWindow()?.apply {
			setBackgroundDrawableResource(R.drawable.framed)
		// 	setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
		}

		setContentView(R.layout.dialog_dice)

		// DisplayDie: [Name, Term] -> [edit name, edit term, change pos]

		val baseView = this

		insertTermView = findViewById<EditText>(R.id.insert_term)
		insertTermRollView = findViewById<TextView>(R.id.insert_term_roll)
		insertTermAddView = findViewById<TextView>(R.id.insert_term_add)
		extraTermView = findViewById<GridView>(R.id.grid_dice_new)
		historyView = findViewById<ListView>(R.id.roll_history)

		insertTermView.setOnKeyListener { view, code, event ->
			if (code == KeyEvent.KEYCODE_ENTER) {
				if (event.action == KeyEvent.ACTION_DOWN) {
					insertTermRollView?.performClick()
				}
				view.setNextFocusDownId(view.getId()) // stay in view after enter.
				true
			}
			false // otherwise do not handle.
		}

		insertTermRollView.setOnClickListener(this)
		insertTermAddView.setOnClickListener(this)

		// assign extra dice
		extraTermView.adapter = extraTermAdapter
		extraTermView.setOnItemClickListener { parent, view, position, id ->
			onClick(view.findViewById(R.id.term))
		}
		extraTermView.setOnItemLongClickListener { parent, view, position, id ->
			extraTermAdapter.run {
				remove(getItem(position)) // remove on position
				notifyDataSetChanged()
			}
			true
		}

		// rolling history.
		historyView.adapter = rollHistoryAdapter

		setOnShowListener {
			// focus insertTermView, cursor to end
			insertTermView.run {
				requestFocus()
				setSelection(text.toString().length)
			}
		}
	}

	override fun onClick(view: View) {
		if (insertTermView == null) return

		val viewId = view.getId()

		when (viewId) {
			R.id.insert_term_add -> {
				// get latest insert_term
				val termStr = insertTermView!!.text.toString()

				if (termStr.trim().length > 0) {
					extraTermAdapter.add(termStr)
					extraTermAdapter.notifyDataSetChanged()
				} else {
					Toast.makeText(context, "Cannot add an empty rolling term.", 0).show()
				}
			}

			R.id.insert_term_roll, R.id.term ->  {
				// get latest insert term or the clicked term
				try {
					val termStr
						= if (viewId == R.id.insert_term_roll) {
							RollingTerm.parse(insertTermView!!.text.toString())
						} else {
							// TODO do not parse every time, placeholder for now.
							RollingTerm.parse((view as TextView).text.toString())
						}

					RollHistory.roll("", termStr)
					rollHistoryAdapter.notifyDataSetChanged()
					historyView.setSelection(rollHistoryAdapter.getCount()-1) // scroll bottom
				} catch (e: Exception) {
					Toast.makeText(context, "Could not roll an invalid rolling term.", 0).show()
				}
			}

			else -> {}
		}
	}

	override fun onLongClick(view: View) : Boolean
		= true

	override fun onKey(view: View, code: Int, event: KeyEvent) : Boolean {
		if (view.getId() != R.id.insert_term)
			return false

		if (code != KeyEvent.KEYCODE_ENTER)
			return false

		// on enter: roll inserted (and parsed) term
		if (event.action == KeyEvent.ACTION_DOWN) {
			insertTermRollView?.performClick()
		}

		view.setNextFocusDownId(view.getId()) // stay in view after enter.
		return true
	}
}

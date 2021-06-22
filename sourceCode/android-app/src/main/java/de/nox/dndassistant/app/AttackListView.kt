package de.nox.dndassistant.app

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ListView
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import android.widget.AdapterView
import android.widget.Toast
import android.widget.AdapterView.OnItemClickListener

import de.nox.dndassistant.core.Attack
import de.nox.dndassistant.core.RollingTerm

private val logger = LoggerFactory.getLogger("AttackViewList")

/** An adapther for story list items.
 * A story item can be a multilined string.
 * Only the first line will be displayed. On Click it opens the full text.
 * On long click, the item can be edited.
 *
 * Each Attack displays:
 * - Name
 * - the attack roll (chance to hit)
 * - the (average) damage
 * - the reach and damage types
 * On Click the full damage description can be unfold and more / better information can be displayed.
 */
class AttackListView(context: Context) : ListView(context), AdapterView.OnItemClickListener {

	private val attacks: MutableList<Pair<Attack, String>> = mutableListOf()

	private val defaultAttack: Pair<Attack, String> = Attack.UNARMED to Attack.UNARMED_ATTACK_ROLL

	protected val variables: MutableMap<String, Int> = mutableMapOf()

	/** Display and actions for the displayed attacks. */
	private val attackAdapter = object: BaseAdapter() {
		override fun getCount() : Int
			= attacks.size

		/** Get the story string or an empty string of it is out of boundaries. */
		override fun getItem(position: Int) : Pair<Attack, String>
			= attacks.getOrNull(position) ?: defaultAttack

		override fun getItemId(position: Int) : Long
			= position.toLong()

		override fun getView(position: Int, convertView: View?, parent: ViewGroup) : View
			= ((convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_attack, parent, false)))
			.apply {
				val nameView = this.findViewById<TextView>(R.id.attack_name)
				val rollView = this.findViewById<TextView>(R.id.attack_roll)
				val dmgView = this.findViewById<TextView>(R.id.attack_dmg)
				val rangeView = this.findViewById<TextView>(R.id.attack_range)
				val typesView = this.findViewById<TextView>(R.id.attack_types)
				val noteView = this.findViewById<TextView>(R.id.attack_note)

				if (nameView == null || rollView == null) return this

				val (atk, atkRoll) = getItem(position)

				// show the name / label of the attack
				nameView.text = atk.label

				// show the attack roll or difficulty class
				// XXX (differentiate difficulty class and attack roll? by type? by pattern matching?)
				// TODO (show attack roll pretty? just the evaluated addition, but roll as d20 + bonus, display just the bonus from STR + X as [[ STR + X ]]
				rollView.text = atkRoll.toString()

				// TODO (2021-06-10) show them correctly and evaluated
				// show the average damage (by rolls)
				dmgView.text = atk.damage.joinToString(" + "){ d -> "(${d.value.toString()})" }

				// show range and all damage types
				rangeView.text = "${atk.reach} ft"
				typesView.text = atk.damage.joinToString(", "){ d -> d.type.toString() }

				// show (on demand / hidden) more about the attack
				noteView.text = atk.description

				/* Set the listeners. */
				/* Name.onClick: Show more. */
				// TODO own variable
				nameView.setOnClickListener {
					noteView.visibility = when (noteView.visibility) {
						View.GONE -> View.VISIBLE.also {
							// also scroll to nee visiblr item
							this@AttackListView.setSelection(position)
						}
						else -> View.GONE
					}
				}

				/* Click on attack: Roll attack. */
				rollView.setOnClickListener {
					triggerActivated(atk)

					// can be rolled, is not just a difficulty class.
					if (!atkRoll.startsWith("DC")) {
						val term = RollingTerm.parse(atkRoll) // TODO better linking, less repetitive anonymous classes
						Utils.showRolledTerm(parent, atk.label, term)
					}
				}

				/** Click on damage, to roll the damge dice. */
				dmgView.setOnClickListener {
					val term = atk.damage.joinToString("+") { (_, term) -> term.toString() }
					Toast.makeText(context, "TODO (roll attack roll)", 1).show() // TODO remove DEBUG
				}
			}
	}

	init {
		this.adapter = attackAdapter
		// this.setOnItemClickListener(this)
	}

	/** Trigger that the action is executed.
	 * Spells, Items and other things may onsume the needed resources. */
	private fun triggerActivated(atk: Attack) {
		// XXX implement me.
	}

	/** Add a map of attacks. This may also replace already inserted Items. */
	public fun addAttacks(attacks: MutableMap<Attack, String>) {
		// old plus new
		// avoid duplicates by handle them as maps
		val combined = this.attacks.toMap() + attacks
		this.attacks.clear()
		this.attacks.addAll(combined.toList())
	}

	/** Remove all attacks. */
	public fun clearAttacks() {
		this.attacks.clear()
	}

	override fun onItemClick(p0: AdapterView<*>, p1: View, p2: Int, p3: Long) {
	}
}

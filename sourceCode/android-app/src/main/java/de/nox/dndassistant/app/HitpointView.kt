package de.nox.dndassistant.app

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.GridLayout.LayoutParams
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView

import de.nox.dndassistant.core.Ability
import de.nox.dndassistant.core.Logger
import de.nox.dndassistant.core.Condition
import de.nox.dndassistant.core.PlayerCharacter
import de.nox.dndassistant.core.DiceTerm

/**
 * HitpointView.
 * A Control panel which shows hit points related statitistcs
 * and which may modify them.
 * Like hitpoints bar and taking hits or healing, resting
 * and showing the current death save successes or fails.
 *
 * Also the armor class is displayed here.
 */
public class HitpointView : LinearLayout {

	companion object {
		protected val log : Logger = LoggerFactory.getLogger("D&D HitpointView")
	}

	private val li = LayoutInflater.from(this.getContext())

	constructor(c: Context, attrs: AttributeSet? = null) : super(c, attrs);

	// TODO (2020-10-20) detach from this class and move to shared lib.
	/** Attached character. */
	private val character: PlayerCharacter get() = CharacterManager.INSTANCE.character

	/* Quick shorcut: Hit Points. */
	private val hpBase: Int get() = character.hitpoints
	private val hpCurrent: Int get() = character.current.hitpoints
	private val hpTemporary: Int get() = character.current.hitpointsTMP
	private val hpMaximal: Int get() = character.current.hitpointsMax

	/* Quick shorcut: Hit dice. */
	private val hitdiceAll: List<Int> get() = character.hitdice.map { it.value }
	private val hitdiceCurrent: List<Int> get() = character.current.hitdice.map { it.value }

	/* Quick shorcut: Conditions dice. */
	private val conditions: Map<Condition, Int> get() = character.current.conditions

	/* Quick rest functions. */
	private fun restLong() = character.current.restLong()
	private fun restShort(d: Int, heal: Int) = character.current.restShort(listOf(DiceTerm.Die(d)), heal)
	private fun heal(hp: Int) = character.current.heal(hp)
	private fun takeHit(hp: Int, crit: Boolean) = character.current.takeHit(hp, crit)

	/** Hit point preview.
	 * Also click handler top open content,
	 * or also dialog to add damage or heal. */
	private val preview: ProgressBar by lazy {
		findViewById<ProgressBar>(R.id.healthbar).also { bar ->
			/* On bar click: Show/Hide content. */
			bar.setOnClickListener { toggleContent() }

			/* On bar long-click: More details of HP and options to change. */
			bar.setOnLongClickListener {
				displayCurrentHealth()

				true
			}

			// displayCurrentHealth() // show current health.

			log.debug("Initiated Preview / Progressbar")
		}
	}

	/** Content.
	 * Contains minor previews for hit point related information. */
	private val content: ViewGroup by lazy {
		findViewById<ViewGroup>(R.id.content_health_foldable).also {
			log.debug("Initiated Content / ViewGroup")

			/* load sub views. */
			acView.text = "AC: ${character.armorClass}"

			/* initiate hit dice. */
			restView // lazy initiate
			longrestView // lazy initiate
			displayHitdice(hitdiceAll, hitdiceCurrent)

			/* show current death fight and update health bar. */
			displayDeathFightResults()
			displayCurrentHealth()

			/* Load also hp controller view (lazy initialisation). */
			damageView;
			healView;
		}
	}

	/** Show the current character state. */
	public fun displayNow() {
		log.debug("Loaded $preview")
		log.debug("Loaded $content")
		log.debug("Loaded $condition")

		/* Filter the first matching hitdie, which match the backend. */
		displayHitdice(hitdiceAll, hitdiceCurrent)

		/* Update health bar. */
		displayCurrentHealth()

		/* Display death fight state. */
		displayDeathFightResults()

		/* Display current conditions. */
		displayConditions()
	}

	/** Condition preview.
	 * Contains Condition states. */
	private val condition: TextView by lazy {
		findViewById<TextView>(R.id.conditions).also { cView ->
			/* OnClick: Show more details and options to change the conditions. */
			cView.setOnClickListener {
				// TODO (2020-10-22) implement condition control dialog/window.

				Toast.makeText(getContext(),
					"Condition Controller",
					Toast.LENGTH_SHORT
				).show()
			}

			log.debug("Initiated Conditions View")
		}
	}

	/** Show the current conditions. */
	public fun displayConditions() {
		// TODO (2020-10-22) condition preview.
		condition.text = conditions.toList().joinToString("\n")
	}

	/** Fold content (state) with effect on set. */
	public val folded: Boolean
		get() = content.visibility != View.VISIBLE

	/** View to show the hitpoints in text format. */
	private val hitpointView by lazy {
		findViewById<TextView>(R.id.hitpoints)
	}

	/** View where you can add or remove hitpoints. */
	private val hitpointControlView by lazy {
		findViewById<TextView>(R.id.hp_controller)
	}

	/** View where the hit points will be confirmed as damage. */
	private val damageView by lazy {
		findViewById<TextView>(R.id.take_hit).also { view ->
			/** OnClick: Add hpModifierView as damage. */
			view.setOnClickListener(hpModifying)
		}
	}

	/** View where the hit points will be confirmed as healing. */
	private val healView by lazy {
		findViewById<TextView>(R.id.heal).also { view ->
			/** OnClick: Add hpModifierView as heal. */
			view.setOnClickListener(hpModifying)
		}
	}

	/** OnClickListener: Modify the hitpoints by the hitpoint_modifier. */
	private val hpModifying: View.OnClickListener
		= View.OnClickListener { view ->
			log.debug("Heal or damage the character.")

			/* Values and parameters. */
			val hp = hpModifyingNum
			val crit = false

			Toast.makeText(getContext(),
				"Heal/Damage: $hp",
				Toast.LENGTH_SHORT).show()

			/* Heal or damage depends on view */
			when (view.id) {
				healView.id -> {
					log.info("Heal by $hp")
					heal(hp)
				}
				damageView.id -> {
					log.info("Hurt with $hp")
					takeHit(hp, crit)
				}
			}

			/* Display updates. */
			displayNow()
		}

	/** View the amount of hit point change is controlled. */
	private val hpModifierView by lazy {
		findViewById<TextView>(R.id.hitpoint_modifier).also {
			it.setOnClickListener(hpModifying)
		}
	}

	/** Read out the parsed number of the hpModifierView. */
	private val hpModifyingNum: Int get()
		= try { hpModifierView.text.toString().toInt() } catch (e: NumberFormatException) { 0 }

	/** Get remaining life proportionally. */
	public val lifePercentage: Double
		get() = preview.progress * 100.0 / preview.max

	/** On Healthbar Click: Unfold, if folded or fold if unfolded. */
	public fun toggleContent() = apply {
		content.visibility = if (folded) View.VISIBLE else View.GONE
		log.debug("Toggle Fold, now: $folded")
	}

	/** Show attached character's current health in health bar. */
	fun displayCurrentHealth() {
		/* Display the hitpoints in bar format. */
		preview.apply {
			progress = hpCurrent
			max = hpMaximal
			log.info("Healthbar shows $progress/$max")
		}

		/* Display the hitpoints in text format. */
		hitpointView.text = "%d / %d (%+d)".format(hpCurrent, hpBase, hpTemporary)
	}

	/** Display and control for death saves. */
	private val deathFightView: TextView by lazy {
		findViewById<TextView>(R.id.deathsaves).also {
			// TODO (2020-10-21) implement death fight (success/fail) <= display and control
			log.debug("Death Fight (Save Success/Fail) view initated.")
		}
	}

	/** Show the current death fight result. */
	public fun displayDeathFightResults() {
		val fail: String = getContext().getString(R.string.deathfail)
		val success: String = getContext().getString(R.string.deathsuccess)

		deathFightView.text = (
			fail.repeat(character.current.deathsaveFail)
			+ success.repeat(character.current.deathsaveSuccess))
	}

	/** Display and control for armor class. */
	private val acView: TextView by lazy {
		findViewById<TextView>(R.id.armorclass).also { ac ->
			/* On Click: Show Details, current Armor and options to change it. */
			ac.setOnClickListener {
				// TODO (2020-10-21) implement armor dresser
				Toast.makeText(getContext(), "Show Details, current Armor and options to change it", Toast.LENGTH_SHORT).show()
			}
			log.debug("ArmorClass view initated.")
		}
	}

	/** Display and control for long and short rest / hit dice. */
	private val restView: ViewGroup by lazy {
		(findViewById<GridLayout>(R.id.resting))
	}

	/** Long rest. On Click, a long rest is done. Front end will be updated. */
	private val longrestView: TextView by lazy {
		findViewById<TextView>(R.id.longrest).also { restL ->
			/* OnClick: Let the character rest long. */
			restL.setOnClickListener {
				log.info("Long rest, 8h.")

				Toast.makeText(getContext(),
					"Long rest, 8h.",
					Toast.LENGTH_SHORT
				).show()

				/* Backend healing: Restoration. */
				restLong()

				displayNow()
			}

			/* OnLongClick:
			 * Toggle visibility of other resting views / hit "dice". */
			restL.setOnLongClickListener { toggleShortRest(); true }

			log.debug("Longrest view initated.")
		}
	}

	/** Show or hide the view for the short rest / hit die views.*/
	public fun toggleShortRest() {
		restView.visibility = when (restView.visibility) {
			View.GONE -> View.VISIBLE.also { log.debug("Show short rest views.") }
			else -> View.GONE.also { log.debug("Hide short rest views.") }
		}
	}

	/** List of all hit dice:
	 * OnClick, the character rests shortly and spends the selected die. */
	private var shortrestViews: List<HitdieView> = listOf()

	/** Display the hitdice which are given by the list.
	 * Set their available status according to the available list.
	 * By default: the current character's hitdice. */
	public fun displayHitdice(
		hitdice: List<Int> = hitdiceAll,
		available: List<Int> = hitdiceCurrent
	) {
		/* Measure the differences. */

		/* supportive queues: what to add/remove */
		var hs: List<Int> = hitdice.map { it } // queue, to add
		var dump: List<HitdieView> = listOf() // queue, to remove

		/* supportive queue: state of dice (delayed, don't change at once) */
		var availableQueue: List<Int> = available.map { it }.sorted()
		var readyingQueue: List<HitdieView> = listOf() // can be filled

		var availableBySrc: Boolean

		for (view in shortrestViews) {
			/* Check, if still needed */
			if (view.face in hs) {
				/* => yes, remove one face from queue/todo as "already there". */

				hs -= view.face
				availableBySrc = view.face in availableQueue

				if (!view.available) {
					/* Is an available view,
					 * which can be made available, but isn't yet. */
					readyingQueue += view

				} else if (availableBySrc && view.available) {
					/* This available view already satisfies a die from the availableQueue.
					=> remove from queue/todo list*/
					availableQueue -= view.face

				} else if (view.available) {
					/* The view is ready, but doesn't need to be: Reset. */
					view.available = false
				}
			} else {
				/* => no, put whole view to dump to remove later. */
				dump += view
				log.debug("Todo: Remove HitdieView ($view)")
			}
		}

		/* Remove unneeded views. */
		dump.forEach {
			shortrestViews -= it
			restView.removeView(it)
			log.debug("Removed HitdieView ($it)")
		}

		/* If views still needs to be made available, do the waiting list first. */
		for (view in readyingQueue) {
			if (availableQueue.size < 1) {
				/* nothing needs to be ready anymore. */
				break
			}

			/* Can be made available again, remove from queues. */
			if (view.face in availableQueue) {
				availableQueue -= view.face
				view.available = true
			}
		}

		/* Fill with still needed views. */
		hs.forEach { face ->
			/*
			val hitdieView = (li.inflate(R.layout.hitdieview, this, false) as TextView).also {
				it.text = "T$face"
				it.setClickable(face in availableQueue)
				availableQueue -= face
			}
			*/

			val hitdieView = HitdieView(face).also {
				/* Set availability. */
				it.available = face in availableQueue
				availableQueue -= face
				shortrestViews += it
			}

			restView.addView(hitdieView, restGridParams) // (recreates actively)

			log.debug("Added new HitdieView ($hitdieView)")
		}

		log.debug("Hitdice: $hs, available: $available")
		log.debug("ShortrestView: $shortrestViews")
	}

	/** Hidden Constitution Modifier for the hit die rest (short rest). */
	private val conModHack = TextView(getContext()).also {
		it.visibility = View.GONE
		it.text = "${character.abilityModifier(Ability.CON)}"
	}

	/** Drawable for a HitdieView. */
	private val hitdieDrawable = getContext().getDrawable(R.drawable.framed)

	/** GridLayout.LayoutParams for each HitdieView. */
	private val restGridParams: GridLayout.LayoutParams get()
		= GridLayout.LayoutParams(
				/*col.*/ GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, 1f),
				/*row.*/ GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, 1f)
			).also {
				it.setGravity(Gravity.FILL) // fill cell (horizontally, vertically)
				it.setMargins(3, 3, 3, 3) // pixels
			}

	/** Inner class displaying a hitdie as button, to use. */
	private inner class HitdieView(_face: Int): AppCompatTextView(getContext()) {
		val face: Int = _face

		init {
			text = "D$face"
			setBackground(hitdieDrawable)
			setPadding(2, 2, 2, 2) // in pixels
			setGravity(Gravity.CENTER) // text align CENTER

			/** Set the wrapped listener. */
			setOnClickListener {
				/* Rolle the heal. */
				var heal = roller.run {
					onClick(this@HitdieView)
					lastRoll
				}

				log.info("Short Rest, d$face => healed $heal hp")

				/* Mark this hit die as used. */
				available = false

				/* Apply the hitdie usage and healing. */
				restShort(face, heal)

				/* Update health bar. */
				displayCurrentHealth()
			}
		}

		override fun toString() : String
			= "HitdieView [d$face / ${if (available) "Available" else "_" }]"

		/** The actual rolling event, later wrapped in heal. */
		private val roller: OnEventRoller
			= OnEventRoller.Builder(DiceTerm.xDy(x = 1, y = face))
				.addDiceView(conModHack)
				.setReasonString("Hitdie D$face + CON")
				.create()

		/** The front end state, if the die is still usable or not. */
		public var available: Boolean
			set(value) {
				setClickable(value)
				setAlpha(when (value) {
					true -> 1.0f
					false -> 0.3f
				})
				log.debug("Set availability of d$face: $value => $this")
			}
			get() = isClickable() && getAlpha() > 0.5f
	}
}

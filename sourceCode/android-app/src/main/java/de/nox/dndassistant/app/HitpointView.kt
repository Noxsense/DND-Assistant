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
import de.nox.dndassistant.core.PlayerCharacter
import de.nox.dndassistant.core.SimpleDice
import de.nox.dndassistant.core.playgroundWithOnyx

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

	constructor(c: Context, attrs: AttributeSet? = null) : super(c, attrs) {
	}

	public fun displayNow() {
		log.debug("Loaded $preview")
		log.debug("Loaded $content")
		log.debug("Loaded $condition")

		/* Filter the first matching hitdie, which match the backend. */
		displayHitdice(character.hitdice, character.current.hitdice)

		/* Update health bar. */
		displayCurrentHealth()

		/* Display death fight state. */
		displayDeathFightResults()

		/* Display current conditions. */
		displayConditions()
	}

	/** Hit point preview.
	 * Also click handler top open content,
	 * or also dialog to add damage or heal. */
	private val preview: ProgressBar by lazy {
		findViewById<ProgressBar>(R.id.healthbar).also { bar ->
			/* On bar click: Show/Hide content. */
			bar.setOnClickListener { toggleFold() }

			/* On bar long-click: More details of HP and options to change. */
			bar.setOnLongClickListener {
				displayCurrentHealth()

				Toast.makeText(getContext(), "HP Dialog? ${bar.progress}/${bar.max}", Toast.LENGTH_LONG).show()
				log.debug("Open Heal/Hit Dialog.")

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
			displayHitdice(character.hitdice, character.current.hitdice)

			/* show current death fight and update health bar. */
			displayDeathFightResults()
			displayCurrentHealth()
		}
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
		condition.text = character.current.conditions.toList().joinToString("\n")
	}

	/** Attached character.
	 * TODO (2020-10-20) detach from this class and move to shared lib. */
	private val character: PlayerCharacter = playgroundWithOnyx()

	/** Fold content (state) with effect on set. */
	public val folded: Boolean
		get() = content.visibility != View.VISIBLE

	/** Get remaining life proportionally. */
	public val lifePercentage: Double
		get() = preview.progress * 100.0 / preview.max

	/** temporary hit points. */
	public var tmpHP: Int = 0

	/** On Healthbar Click: Unfold, if folded or fold if unfolded. */
	public fun toggleFold() = apply {
		content.visibility = if (folded) View.VISIBLE else View.GONE
		log.debug("Toggle Fold, now: $folded")
	}

	/** Show attached character's current health in health bar. */
	fun displayCurrentHealth() {
		/* Display the healing. */
		preview.apply {
			progress = character.current.hitpoints
			max = character.hitpoints
			log.info("Healthbar shows $progress/$max")
		}
	}
	// TODO (2020-10-21) show heal / damage dialog.

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
				character.current.restLong()

				displayNow()
			}

			/* OnLongClick,
			 * Rest 1d4 longer, to regain at least one hit point,
			 * when the character wasn't stabilized.  */
			restL.setOnLongClickListener {
				log.info("Long rest, (1d4 + 8) h.")

				Toast.makeText(getContext(),
					"Stabalizing Long rest, 1d4 + 8h.",
					Toast.LENGTH_SHORT
				).show()

				/* Backend healing: Restoration. */
				character.current.restHours(12 /*h*/)

				displayNow()

				/* long click is used up. */
				true
			}

			log.debug("Longrest view initated.")
		}
	}

	/** List of all hit dice:
	 * OnClick, the character rests shortly and spends the selected die. */
	private var shortrestViews: List<HitdieView> = listOf()

	/** Display the hitdice which are given by the list.
	 * Set their available status according to the available list.
	 * By default: the current character's hitdice. */
	public fun displayHitdice(
		hitdice: List<Int> = character.hitdice,
		available: List<Int> = character.current.hitdice
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
				character.current.restShort(listOf(face), heal)

				/* Update health bar. */
				displayCurrentHealth()
			}
		}

		override fun toString() : String
			= "HitdieView [d$face / ${if (available) "Available" else "_" }]"

		/** The actual rolling event, later wrapped in heal. */
		private val roller: OnEventRoller
			= OnEventRoller.Builder(SimpleDice(face, 1))
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

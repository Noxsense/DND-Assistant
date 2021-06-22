package de.nox.dndassistant.app

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

import kotlinx.android.synthetic.main.abilities.*

import kotlin.properties.Delegates

import de.nox.dndassistant.core.Ability
import de.nox.dndassistant.core.Die
import de.nox.dndassistant.core.Number
import de.nox.dndassistant.core.Logger
import de.nox.dndassistant.core.Proficiency
import de.nox.dndassistant.core.RollingTerm


// TODO make to ViewModel -> listen directly to Hero's abilities and their proficiency.

public class AbilitiesView : LinearLayout {

	companion object {
		protected val log : Logger = LoggerFactory.getLogger("D&D AbilitiesView")

		private val abilityList: List<Ability> = enumValues<Ability>().toList()
	}

	var checkLabel : String = "Check"

	private val ABILITY_LAYOUT = R.layout.ability
	private var views: Map<Ability, AbilityPanel> = mapOf()
	private val parentView: ViewGroup
		get() = getParent() as ViewGroup

	// click a modifier view: roll a value.
	private val modifierOnClickListener: OnClickListener = OnClickListener { v ->
		views.values.find { aPanel -> aPanel.modView == v }?.let { aPanel ->
			// roll the linked term
			Utils.showRolledTerm(parentView, "${aPanel.ability.fullname} $checkLabel", aPanel.rollModifier)
		}
	}

	/** Proficiency bonus to roll for proficient save rolls. */
	public var proficiencyBonus: Int = 0
	// TODO (2021-06-18) keep or delete? proficieny bonus for the abilitiesview? how to fetch, hot to update?

	private val li = LayoutInflater.from(this.getContext())

	constructor(c: Context, attrs: AttributeSet) : super(c, attrs) // otherwise it cannot be inflated
	constructor(c: Context, attrs: AttributeSet? = null, dAttr: Int = 0, dRes: Int = 0) : super(c, attrs, dAttr, dRes)

	init {
		/* Associate all known abilities with a new panel view. */
		abilityList.forEach {
			 // adds automatically new views for missing view.
			 getViewsFor(it)
		}
	}

	/** Get all known abilities for the requesting player character.
	 */
	public fun setScores(scores: Map<Ability, Pair<Int, Boolean>>) {
		/* Own Proficiency bonus, a bit hacked as hidden text view?. */
		proficiencyBonus = 0 // XXX

		/* Add values for ability scores. */
		abilityList.forEach { a ->
			val score = scores.get(a)?.first ?: 10 // XXX
			val save = scores.get(a)?.second ?: false // XXX

			// set score.
			setScore(a, score)
			log.debug("Set score for PC <NAME>: $a.")

			// set saving throw.
			setSavingThrow(a, save)
			log.debug("Set saving throw PC <NAME>: $a?")
		}
	}

	/** Set the score for the given ability.
	 * This updates the small score frame and the bigger mod frame.
	 */
	public fun setScore(a: Ability, score: Int) {
		getViewsFor(a).score = score
		AbilitiesView.log.debug("Set score for $a: $score")
	}

	/** Set the display according to the state if the given ability is a save or not.
	 * This updates the background colour of the view..
	 */
	public fun setSavingThrow(a: Ability, isSavingThrow: Boolean = false) {
		getViewsFor(a).isSavingThrow = isSavingThrow
		AbilitiesView.log.debug("Set save for $a: $isSavingThrow")
	}

	/** * Get a View for a certain ability, if there is not a view yet, create one.
	 */
	private fun getViewsFor(a: Ability) : AbilityPanel {
		return views.getOrElse(a) {
			val inflated = li.inflate(ABILITY_LAYOUT, this, false)
			// add newly created view to map.
			log.debug("Newly inflated view: ${inflated.getId()}/ $inflated")

			val aPanel: AbilityPanel = AbilityPanel.from(inflated)!!
			log.debug("Newly parsed AbilityPanel: ${aPanel.wrapView.getId()}/ $aPanel")

			aPanel.ability = a
			log.debug("Added new ability ($a) titleView.text: ${aPanel.ability}")

			// add to references / mapping.
			views += (a to aPanel)
			log.debug("Added new ability ($a) view: ${aPanel.wrapView}")
			log.debug("Known views: $views")

			// add listeners
			aPanel.setModifierOnClickListener(modifierOnClickListener)

			// add to this panel (extended LinearLayout).
			this@AbilitiesView.addView(aPanel.wrapView)

			aPanel
		}
	}

	/** (Supportive) Storage for ability values.
	 * Wraps up the views of the title, the value and the modifiier.
	 * Additionally holds the holding view of the three values.
	 */
	private data class AbilityPanel(
		val wrapView: View,
		val titleView: TextView,
		val scoreView: TextView,
		val modView: TextView)
	{
		/**
		 * Set OnClickListener for the modifier view (the big offset number).
		 */
		public fun setModifierOnClickListener(listener: OnClickListener) {
			modView.setOnClickListener(listener)
		}

		/** Set and display title. */
		public var ability: Ability = abilityList.get(0)
			set(value) {
				log.debug("Set titleView.text to title: $value")
				titleView.text = value.fullname
			}

		/** Set and display ability score. */
		public var score: Int = 10
			set(value) {
				var newMod = Ability.scoreToModifier(value)

				// displays the updated value.
				scoreView.text = value.toString()
				modView.text = "%+d".format(newMod)

				AbilitiesView.log.debug("Set score $value: into $scoreView")
				AbilitiesView.log.debug(" \u21d2 proof: ${scoreView.text}")
				AbilitiesView.log.debug(" \u21d2 proof: ${modView.text}")

				// update the rollModifier
				rollModifier = Die(20) + Number(newMod)
			}

		/** Get modifier. */
		public val mod: Int
			get() = Ability.scoreToModifier(score)

		/**
		 * The Term / Offset when a roll for the ability needs to be done.
		 * This is basically the modifer as Additional Number to a D20.
		 */
		public var rollModifier: RollingTerm = Die(20)
			private set

		/** Set and display, if it has proficiency for saving throws. */
		public var isSavingThrow: Boolean by Delegates.observable(false) {
			_, _, value ->

			wrapView.setBackground(getDrawable(value))

			AbilitiesView.log.debug("Set ability as proficient ($value) in Saving Throws")
		}

		private fun getDrawable(proficient: Boolean)
			= getContext().getDrawable(when(proficient) {
				true -> R.drawable.bg_proficient
				else -> R.drawable.framed
			})

		public fun getContext() = wrapView.getContext()

		companion object {
			val TITLE_ID = R.id.ability_title
			val MOD_ID = R.id.ability_modifier
			val SCORE_ID = R.id.ability_value

			/**
			 * Create a new view for an ability (title, modifier, full score).
			 * This fills in the given view with the title, the modifier and the score.
			 */
			public fun from(wrap: View) : AbilityPanel?
				= try {
					AbilityPanel(
						wrap,
						// get views in given wrap view area.
						titleView = wrap.findViewById(TITLE_ID)!!,
						scoreView = wrap.findViewById(SCORE_ID)!!,
						modView = wrap.findViewById(MOD_ID)!!
					).apply {
					}

				} catch (e: NullPointerException) {
					null
				}
		}
	}
}

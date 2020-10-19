package de.nox.dndassistant.app

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

import kotlinx.android.synthetic.main.abilities.*

import de.nox.dndassistant.core.Logger
import de.nox.dndassistant.core.D20
import de.nox.dndassistant.core.SimpleDice
import de.nox.dndassistant.core.DiceTerm
import de.nox.dndassistant.core.Ability
import de.nox.dndassistant.core.Proficiency
import de.nox.dndassistant.core.PlayerCharacter

public class AbilitiesView : LinearLayout {

	companion object {
		protected val log : Logger = LoggerFactory.getLogger("D&D AbilitiesView")

		private val abilityList: List<Ability> = enumValues<Ability>().toList()

		public fun fromView(view: LinearLayout) : AbilitiesView?
			= null
	}

	private val ABILITY_LAYOUT = R.layout.ability
	private var views: Map<Ability, AbilityPanel> = mapOf()

	private val li = LayoutInflater.from(this.getContext())

	constructor(c: Context) : super(c);
	constructor(c: Context, attrs: AttributeSet?) : super(c, attrs);
	constructor(c: Context, attrs: AttributeSet?, dAttr: Int) : super(c, attrs, dAttr);
	constructor(c: Context, attrs: AttributeSet, dAttr: Int, dRes: Int) : super(c, attrs, dAttr, dRes);

	init {
		/* Associate all known abilities with a new panel view. */
		abilityList.forEach {
			 // adds automatically new views for missing view.
			 getViewsFor(it)
		}
	}

	/** Get all known abilities for the requesting player character. */
	public fun setScores(pc: PlayerCharacter) {
		abilityList.forEach { a ->
			val score = pc.abilityScore(a)
			val save = pc.getProficiencyFor(a)

			// set score.
			setScore(a, score)
			log.debug("Set score for PC ${pc.name}: $a.")

			// set saving throw.
			setSavingThrow(a, save.first != Proficiency.NONE)
			log.debug("Set saving throw PC ${pc.name}: $a?")

			// add ability check.

			// add saving throws.
		}
	}

	/** Set the score for the given ability.
	 * This updates the small score frame and the bigger mod frame. */
	public fun setScore(a: Ability, score: Int) {
		getViewsFor(a).score = score
		AbilitiesView.log.debug("Set score for $a: $score")
	}

	/** Set the display according to the state if the given ability is a save or not.
	 * This updates the background colour of the view.. */
	public fun setSavingThrow(a: Ability, isSavingThrow: Boolean = false) {
		getViewsFor(a).isSavingThrow = isSavingThrow
		AbilitiesView.log.debug("Set save for $a: $isSavingThrow")
	}

	/** Get a View for a certain ability. */
	private fun getViewsFor(a: Ability) : AbilityPanel {
		return views.getOrElse(a) {
			val inflated = li.inflate(ABILITY_LAYOUT, this, false)
			// add newly created view to map.
			log.debug("Newly inflated view: ${inflated.getId()}/ $inflated")

			val aPanel: AbilityPanel = AbilityPanel.from(inflated)!!
			log.debug("Newly parsed AbilityPanel: ${aPanel.wrapView.getId()}/ $aPanel")

			aPanel.title = a.fullname
			log.debug("Added new ability ($a) titleView.text: ${aPanel.title}")

			// add to references / mapping.
			views += (a to aPanel)
			log.debug("Added new ability ($a) view: ${aPanel.wrapView}")
			log.debug("Known views: $views")

			// add to view.
			this@AbilitiesView.addView(aPanel.wrapView)

			aPanel
		}
	}

	/** (Supportive) Storage for ability values. */
	private data class AbilityPanel(
		val wrapView: View,
		private val titleView: TextView,
		private val scoreView: TextView,
		private val modView: TextView)
	{
		private var abilityCheckRoller: OnEventRoller? = null
			set(value) {
				log.info("Set onClickListener: $value, $title")
				// value?.reason = title + " Check"
				modView.setOnClickListener(value)
			}

		private fun getCheckRoller() = abilityCheckRoller

		private var savingThrowRoller: OnEventRoller? = null
			set(value) {
				log.info("Set onLongClickListener: $value, $title")
				// value?.reason = title + " Save"
				modView.setOnLongClickListener(value)
			}

		private fun getSaveRoller() = savingThrowRoller

		/** Set and display title. */
		public var title: String = "Ability"
			set(value) {
				log.debug("Set titleView.text to title: $value")
				titleView.text = value

				// XXX (2020-10-15) this does not update the base term.
				// getCheckRoller()?.reason = "$value Check"
				// getSaveRoller()?.reason = "$value Save"

				log.debug(" > roller.reason: $value: ${getCheckRoller()?.reason}")
				log.debug(" > roller.reason: $value: ${getSaveRoller()?.reason}")
			}

		/** Set and display ability score. */
		public var score: Int = 10
			set(value) {
				// displays the updated value.

				val mod = Ability.scoreToModifier(value)

				scoreView.text = value.toString()
				modView.text = "%+d".format(mod)

				// XXX (2020-10-15) this does not update the base term.
				// getCheckRoller()?.baseTerm = D20.toTerm() + mod
				// getSaveRoller()?.baseTerm = D20.toTerm() + mod

				AbilitiesView.log.debug("Set score $value: into $scoreView")
				AbilitiesView.log.debug(" \u21d2 proof: ${scoreView.text}")
				AbilitiesView.log.debug(" \u21d2 proof: ${modView.text}")
			}

		/** Get modifier. */
		public val mod: Int
			get() = Ability.scoreToModifier(score)

		/** Set and display, if it has proficiency for saving throws. */
		public var isSavingThrow: Boolean = false
			set(value) {
				/* Displays the updated value. */
				wrapView.setBackground(getContext().getDrawable(when (value) {
					true -> R.drawable.bg_proficient
					else -> R.drawable.framed
				}))

				AbilitiesView.log.debug("Set ability as proficient ($value) in Saving Throws")
			}

		public fun getContext() = wrapView.getContext()

		private fun createOnEventRoller(type: String) : OnEventRoller
			= OnEventRoller
				.Builder(D20)
				.addDiceView(modView)
				.setReasonView(titleView)
				.setFormatString("%s $type")
				.create()

		companion object {
			val TITLE_ID = R.id.ability_title
			val MOD_ID = R.id.ability_modifier
			val SCORE_ID = R.id.ability_value

			public fun from(wrap: View) : AbilityPanel?
				= try {
					AbilityPanel(
						wrap,
						// get views in given wrap view area.
						titleView = wrap.findViewById(TITLE_ID)!!,
						scoreView = wrap.findViewById(SCORE_ID)!!,
						modView = wrap.findViewById(MOD_ID)!!
					).apply {
						/* Add ability check roller. */
						abilityCheckRoller = createOnEventRoller("Check")

						/* Add saving throw roller, maybe connect to proficiency value. */
						savingThrowRoller = createOnEventRoller("Save")

						log.debug("Added default roller.")
					}

				} catch (e: NullPointerException) {
					null
				}
		}
	}
}

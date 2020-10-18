package de.nox.dndassistant.app

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView

import de.nox.dndassistant.core.Logger
import de.nox.dndassistant.core.Background
import de.nox.dndassistant.core.Race
import de.nox.dndassistant.core.Alignment
import de.nox.dndassistant.core.Size
import de.nox.dndassistant.core.PlayerCharacter

public class StoryView : LinearLayout {

	companion object {
		protected val log : Logger = LoggerFactory.getLogger("D&D StoryView")
	}

	private val speciesTxt: TextView by lazy  {
		findViewById<TextView>(R.id.the_species)
	}
	private val speciesMoreTxt: TextView by lazy  {
		findViewById<TextView>(R.id.about_species)
	}
	private val backgroundTxt: TextView by lazy  {
		findViewById<TextView>(R.id.the_background)
	}
	private val backgroundMoreTxt: TextView by lazy  {
		findViewById<TextView>(R.id.about_background)
	}
	private val appearanceText: TextView by lazy  {
		findViewById<TextView>(R.id.about_appearance)
	}
	private val historyText: TextView by lazy  {
		findViewById<TextView>(R.id.history)
	}

	public var displayedRace: Pair<Race, String>? = null
		private set

	public var displayedBackground: Pair<Background, String>? = null
		private set

	public var displayedSize: Size = Size.MEDIUM

	constructor(c: Context) : super(c);
	constructor(c: Context, attrs: AttributeSet?) : super(c, attrs);
	constructor(c: Context, attrs: AttributeSet?, dAttr: Int) : super(c, attrs, dAttr);
	constructor(c: Context, attrs: AttributeSet, dAttr: Int, dRes: Int) : super(c, attrs, dAttr, dRes);

	init {
		/*
		try {
			log.debug("Initiate a new StoryView.")

			// "@+id/the_species"
			// "@+id/about_species"
			// "@+id/the_background"
			// "@+id/about_background"
			// "@+id/about_appearance"
			// "@+id/history"

			speciesTxt = findViewById<TextView>(R.id.the_species)
			speciesMoreTxt = findViewById<TextView>(R.id.about_species)

			backgroundTxt = findViewById<TextView>(R.id.the_background)
			backgroundMoreTxt = findViewById<TextView>(R.id.about_background)

			appearanceText = findViewById<TextView>(R.id.about_appearance)
			historyText = findViewById<TextView>(R.id.history)

		} catch (e: Exception) {
			log.error("Initation Problems")
			throw e
		}
		*/
	}

	/** Show race, background, appearance and history of the character.
	 * @return String which can be used as preview. */
	public fun showCharacter(character: PlayerCharacter) : String {
		log.debug("Show story and appearance of new character ($character)")
		// display race
		showRace(character.race, character.subrace)

		// display background,
		showBackground(character.background, character.backgroundFlavour)

		// display alignent and traits and flaws.
		showAlignent(
			alignment = character.alignment,
			trait = character.trait,
			ideal = character.ideal,
			bonds = character.bonds,
			flaws = character.flaws)

		// display the appearance
		showAppearance(
			age = character.ageString,
			size = character.size,
			weight = character.weight,
			height = character.height,
			form = character.form,
			description = character.appearance)

		// list history
		showHistory(character.history)

		return generatePreview()
	}

	public fun generatePreview() : String {
		var string = ""
		if (displayedRace != null) {
			val (race, subrace) = displayedRace!!

			val darkvision = race.darkvision

			/* Display race. */
			string += "${race}:${subrace}"

			/* Display extra size. */
			string += when (displayedSize) {
				Size.MEDIUM -> ""
				else -> "/${displayedRace}"
			}

			/* Display extra dark vision. */
			if (darkvision > 0) string += "/$darkvision"
		}

		if (displayedBackground != null) {
			val (bg, bgMore) = displayedBackground!!
			string += " - $bg ($bgMore)"
		}

		return string
	}

	public fun showRace(race: Race, subrace: String = "") = apply {
		log.debug("Show race ($race, $subrace)")

		speciesTxt.text = when (subrace) {
			"" -> "${race}"
			else -> "${race} (${subrace})"
		}

		speciesMoreTxt .text = """
			Darkvision: ${race.darkvision}
			Base-Speed: ${race.speed}
			Languages: ${race.languages}
			Features: ${race.features}
			Description: ${race.description}
			""".trimIndent()

		displayedRace = race to subrace
	}

	public fun showBackground(bg: Background, flavour: String) = apply {
		log.debug("Show background ($bg, $flavour)")
		backgroundTxt.text = "${bg} (flavour)"

		displayedBackground = bg to flavour
		showAlignent()
	}

	public fun showAlignent(
		alignment: Alignment = Alignment.NEUTRAL_NEUTRAL,
		trait: String = "",
		ideal: String = "",
		bonds: String = "",
		flaws: String = ""
	) = apply {
		log.debug("Show alignment ($alignment | $trait | $ideal | $bonds | $flaws)")
		backgroundMoreTxt.text = """
			Alignment: ${alignment}
			Trait: ${trait}
			Ideal: ${ideal}
			Bonds: ${bonds}
			Flaws: ${flaws}

			Background: ${displayedBackground?.first?.description ?: ""}
			""".trimIndent()
	}

	public fun showAppearance(
		age: String = "0 yrs",
		size: Size = Size.MEDIUM,
		weight: Double = 0.0,
		height: Double = 0.0,
		form: String = "average",
		description: String = "average"
	) = apply {
		log.debug("Show appearance ($age, $size, $weight lb, $height lb, $form, $description )")
		appearanceText.text = """
			Age: ${age}
			Size: ${size}
			Height: ${height} ft
			Weight: ${weight} lb
			Form: ${form}
			Appearance: ${description}
			""".trimIndent()
	}

	public fun showHistory(history: List<String>) = apply {
		log.debug("Show history.")
		historyText.text = history.joinToString("\n")
	}
}

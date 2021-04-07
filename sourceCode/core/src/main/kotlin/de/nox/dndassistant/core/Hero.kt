package de.nox.dndassistant.core

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** Refactor IDEA the PlayerCharacter. */
class Hero(name: String, race: Pair<String, String>, player: String? = null ) {
	companion object {
		private val log = LoggerFactory.getLogger("Hero")
	}

	/** String representation of an Hero. */
	override public fun toString() : String
		= "$name (${race.first} (${race.second}), lvl. $level)"

	/* (Fixed) Race. */
	public val race: Pair<String, String> = race

	/* (Variable) name and level. */
	public var name: String = name
		set(value) {
			field = value.trim() // trimmed name.
		}

	/* level and experience and resulting proficiency bonus. */
	public var level = 1
		set(value) {
			field = if (value < 1) 1 else value // not smaller than 1
		}

	public val proficiencyBonus: Int get() = when {
		level >= 17 -> +6
		level >= 13 -> +5
		level >= 9 -> +4
		level >= 5 -> +3
		else -> +2
	}

	public var experience: Experience = Experience()

	/** Experience a Hero can gain. */
	public class Experience(var points: Int = 0, var method: String = "milestone") {
		override public fun equals(other: Any?) : Boolean
			= other != null && other is Experience && (other.points == this.points && other.method == this.method)

		override public fun toString() = "$points ($method)"

		public operator fun plus(v: Int) = Experience(points + v, method)
		public operator fun plusAssign(v: Int) { points + v }

		public operator fun minus(v: Int) = Experience(points - v, method)
		public operator fun minusAssign(v: Int) { points - v }

		/** Try to translate the experience points into a level. */
		public fun toLevel() : Int = 0
	}

	// play-game focussed attributes, variable.
	public var player: String? = player

	// "collected" inspiration points
	public var inspiration: Int = 0
		set(value) {
			field = if (value < 0) 0 else value // prohibit smaller than 0
		}

	// movement speeds ("normal", walking, jumping, swimming, etc. ...)
	public var speed: MutableMap<String, Int> = mutableMapOf(
		"walking" to 30 // in feet
	)

	// walking or normal / base speed
	public var walkingSpeed: Int
		get() = speed["walking"] ?: 30
		set(value) { speed["walking"] = value }

	public var armorSources: List<String> = listOf()
	public val armorClass: Int // XXX get Smror class, depending on clothings and abilities. ???
		get() = naturalBaseArmorClass
		// else  armorSources.map { 1 }.reduce { b, e -> b + e } // sum up all sources
		// private set
	public val naturalBaseArmorClass: Int get() = 10 + abilityModifier(Ability.DEX)

	/* Get the current relation of current "hitpoints" and temporary maximal hitpoints. */
	public val hitpoints: Pair<Int, Int> get() = (hitpointsNow + hitpointsTmp) to (hitpointsMax + hitpointsTmp)

	// hitpoints and life
	public var hitpointsMax: Int = 0 // maximal hit points
	public var hitpointsTmp: Int = 0 // (optional) temporary offset
	public var hitpointsNow: Int = 0 // current hit points
		set (value) {
			when {
				/* Hit points reduce: "Damaged". */
				value < field -> {
					var damage = field - value // positive damage.

					/* Reduce Buffer: damage taken: maybe reduce optional temporary hit points first. */
					if (hitpointsTmp > 0) {
						var tmp = hitpointsTmp
						hitpointsTmp -= damage
						damage -= tmp // rest damage
					}

					/* Death Save Fail: if current hit points were already zero. */
					if (field == 0) {
						deathsaves.addFailure(critical = false)
					}

					/* Instant Death: if rest damage (below zero) is equals or exceeds the maximum hit points. */
					if (damage >= 2 * hitpointsMax) {
						// instant death.
						deathsaves.addFailure(critical = true)
						deathsaves.addFailure(critical = true) // dead for sure.
					}

					field = max(field - damage, 0) // minimum of 0 hp.
				}

				/* Hit points gained: "Healed". */
				value > field -> {
					/* Don't get more than max. */
					field = min(value, hitpointsMax)

					/* If "healed", reset the death fight. */
					deathsaves.reset()
				}

				/* Field == Value: No Change. */
				else -> {}
			}
		}

	public var deathsaves: DeathSaveFight = DeathSaveFight()
		private set

	/** DeathSaveFight: Sixtuple (five to maximally needed throw to define dead or alive and one more). */
	public class DeathSaveFight() {
		/** Result of the death fight. */
		public enum class Result {
			SUCCESS, CRITICAL_SUCCESS,
			FAILURE, CRITICAL_FAILURE,
		}

		private val throws = mutableMapOf<String, Result?>(
			"throw0" to null,
			"throw1" to null,
			"throw2" to null,
			"throw3" to null,
			"throw4" to null,
			"throw5" to null,
		)

		var throw0 by throws
			private set
		var throw1 by throws
			private set
		var throw2 by throws
			private set
		var throw3 by throws
			private set
		var throw4 by throws
			private set
		var throw5 by throws
			private set

		private var nextThrow: Int = 0

		/** Initiate from multiple booleans (as simple success or not). */
		public constructor(throw0: Boolean, vararg ts: Boolean) : this() {
			val throws: List<Boolean> = listOf(throw0) + ts.toList()

			val optBoolToSuccess = { b: Boolean? -> when (b) {
				null -> null
				true -> Result.SUCCESS
				false -> Result.FAILURE
			}}

			this.throw0 = optBoolToSuccess(throws.getOrNull(0))
			this.throw1 = optBoolToSuccess(throws.getOrNull(1))
			this.throw2 = optBoolToSuccess(throws.getOrNull(2))
			this.throw3 = optBoolToSuccess(throws.getOrNull(3))
			this.throw4 = optBoolToSuccess(throws.getOrNull(4))
			this.throw5 = optBoolToSuccess(throws.getOrNull(5))
		}

		/** String representation: As list of successes. */
		public override fun toString() : String
			= (when (evalSaved()) { null -> ""; true -> " Saved!!  "; false -> " Dead!!  " }
			+ this.throws.values.filter { it != null }.joinToString(", ", "[", "]"))

		/** Equally by evaluation. */
		public override fun equals(other: Any?) : Boolean
			= other != null && other is DeathSaveFight && this.toList() == other.toList()

		/** Equal by all means. */
		public fun equalsByCrits(other: DeathSaveFight?) : Boolean
			= (other != null
			// equal or both null
			&& (throw0?.equals(other.throw0) ?: other.throw0 == null)
			&& (throw1?.equals(other.throw1) ?: other.throw1 == null)
			&& (throw2?.equals(other.throw2) ?: other.throw2 == null)
			&& (throw3?.equals(other.throw3) ?: other.throw3 == null)
			&& (throw4?.equals(other.throw4) ?: other.throw4 == null)
			&& (throw5?.equals(other.throw5) ?: other.throw5 == null)
			)

		/** Return the death save fight as list of booleans.
		 * The representing list drops meta information of critical successes/failures
		 * and with that depended actual number of throws.
		 */
		public fun toList() : List<Boolean>
			= throws.values
				// take first not null.
				.takeWhile { t -> t != null }
				// critical evals duplicated elemnts.
				.flatMap { t -> when (t) {
					Result.SUCCESS -> listOf(true)
					Result.FAILURE -> listOf(false)
					Result.CRITICAL_SUCCESS -> listOf(true, false)
					Result.CRITICAL_FAILURE -> listOf(false, false)
					null -> listOf() // redudant but surely.
				}}

		public fun countSuccesses() : Int
			= toList().count { success -> success }

		public fun countFailures() : Int
			= toList().count { success -> !success }

		/* Check if the deaths save fight is won or the final death happens.
		 * Returns null, if there are not enough saving throws to decide. */
		public fun evalSaved() : Boolean? {
			var numSaved = 0
			var numFailed = 0
			for (i in toList()) {
				when (i) {
					true -> { numSaved +=1; if (numSaved > 2) return true }
					false -> { numFailed +=1; if (numFailed > 2) return false }
				}
			}
			return null
		}

		/** Remove all results. */
		public fun reset() {
			throws.mapValues { null }
		}

		/** Update the next throw. */
		private fun addThrow(critical: Boolean = false, success: Boolean) {
			// no throw free.
			if (nextThrow < 0 || nextThrow > 5) return

			throws["throw${nextThrow}"] = if (success) {
				if (critical) Result.CRITICAL_SUCCESS else Result.SUCCESS
			} else {
				if (critical) Result.CRITICAL_FAILURE else Result.FAILURE
			}

			this.nextThrow += 1
		}

		/** Add a new success to the Death Saving throws, optionally a critical one. */
		public fun addSuccess(critical: Boolean = false) = addThrow(critical, true)

		/** Add a new failure to the Death Saving throws, optionally a critical one. */
		public fun addFailure(critical: Boolean = false) = addThrow(critical, false)

		/* Undo the last addition, if the last addition was a crit, the both last additions.
		 * If */
		public fun undoLastThrow() {
			// no throw yet, cannot undo nothing.
			if (nextThrow < 1 || nextThrow > 5) return

			// undo nextThrow - 1: Set to null and nextThrow -= 1
			throws["throw${nextThrow - 1}"] = null

			nextThrow -= 1 // undid the last,
		}
	}

	// the abilities: Pure value and if saving throws are proficient.
	public var abilities: MutableMap<Ability, Pair<Int, Boolean>> = mutableMapOf(
		Ability.STR to (10 to false),
		Ability.CON to (10 to false),
		Ability.DEX to (10 to false),
		Ability.INT to (10 to false),
		Ability.WIS to (10 to false),
		Ability.CHA to (10 to false),
	)
		private set

	/** Get the full value for the ability. Default 10 => modifier 0. */
	public fun Pair<Int,Boolean>?.value() : Int
		= this?.first ?: 10

	/** Check if the ability has a proficiency for the saving rolls. */
	public fun Pair<Int, Boolean>?.isSaveProficient() : Boolean
		= this?.second ?: false

	/** Get the modifier for the given int. */
	public fun Pair<Int, Boolean>?.getModifier() : Int
		= this?.first?.getModifier() ?: 0

	/** Get the modifier for the given int. */
	public fun Int?.getModifier() : Int
		= if (this == null) 0 else floor((this - 10) / 2.0).toInt()

	/** Get the pure ability value. */
	public fun ability(a: Ability) : Int
		= abilities.get(a)?.first ?: 10

	/** Check if the hero is proficient for the Saving Throw of the given Ability. */
	public fun isSavingThrowProficient(a: Ability) : Boolean
		= abilities.get(a)?.second ?: false


	/** Get the modifier for the ability. */
	public fun abilityModifier(a: Ability) : Int
		= floor((ability(a) - 10) / 2.0).toInt()

	/** Lists proficient skills and why tney are proficient. */
	public var skills: MutableMap<SimpleSkill, Pair<SimpleProficiency, String>> = mutableMapOf()
		private set

	/** Get a skill value. */
	public fun skill(s: SimpleSkill) : Int
		= (abilityModifier(s.ability)
		+ (skills.get(s)?.let { (p, _) -> proficiencyBonus * p.multiplier } ?: 0 )
		)

	public val skillValues: Map<SimpleSkill, Int> get()
		= (SimpleSkill.DEFAULT_SKILLS + skills.keys).map { it to skill(it) }.toMap()

	/** Lists proficiencies for tools or weapons. A tool is given by it's name or the category.
	 * Proficiencies can be <proficient> or <expert>.
	 * As with skills, the reason why the hero is proficicient with the tool is also stored.*/
	public var tools: MutableMap<Pair<String, String>, Pair<SimpleProficiency, String>> = mutableMapOf()
		private set

	/** Check if the Hero can proficiently use an item. */
	public fun hasToolProficiency(item: SimpleItem) : Boolean
		= getToolProficiency(item) != null

	/** Get optional proficiency for a tool or its category. */
	public fun getToolProficiency(item: SimpleItem)
		= this.tools.toList().find { (tool, _) ->
			tool.first == item.name || tool.second == item.category
		}

	/* Classes and skills and proficiencies */

	/* Occupations of the Hero. */
	public var klasses: MutableList<Triple<String, String?, Int>> = mutableListOf()
		private set

	public fun setBaseKlass(klass: String, subklass: String? = null, klasslevel: Int = 1) {
		/* Hero's level as maximum level. */
		val lvl = when {
			klasslevel < 1 -> 1
			klasslevel > this.level -> this.level
			else -> klasslevel
		}
		klasses.set(0, Triple(klass, subklass, lvl))
	}

	/* Update klasses, maybe add another klass, maybe set subklass or level.
	 * Attention:
	 * 1. Subclass cannot be changed from one suclass to another.
	 * 2. If the Level will overfit the hero's level, set as it would not overfit.
	 * 3. Level cannot be negative.
	 */
	public fun updateKlass(klass: String, subklass: String? = null, level: Int) {
		log.debug("Update Klass: '$klass'.")

		/* If this klass is already known, update. */
		var i: Int = klasses.indexOfFirst { (name, _, _) -> name == klass }

		/* Check, if subklass can be still overwritten or is already fixed. */
		val sub: String? = when {
			i < 0 -> subklass
			else -> klasses[i].second ?: subklass // already established or new.
		}

		log.debug("Subklass arg: $subklass / null = ${subklass == null}, subklass-to-be: $sub, null? = ${sub == null}")

		/* Check, if the klasses' levels do not overfit. */
		val lvlPre = klasses.getOrNull(i)?.third ?: 0 //  previous klass' level
		val lvlSum = klasses.fold(0) { sum, (_, _, l) -> sum + l } - lvlPre //  all other klass sum

		val lvlNew = when {
			level < 0 -> lvlPre //  new negative level => use old / null
			lvlSum + lvlPre + level > this.level -> this.level - lvlSum //  sum lvl
			else -> lvlPre + level //  increase level
		}

		if (i < 0) {
			log.debug("Add new klass: '$name', $sub, $lvlNew")
			klasses.add(Triple(klass, sub, lvlNew))
		} else {
			log.debug("Update new klass: '$name', $sub, $lvlNew")
			klasses.set(i, Triple(klass, sub, lvlNew))
		}
		log.debug("Updated now klass: ${klasses[if (i < 0) klasses.size - 1 else i]}")
	}

	public val hitdiceMax: Map<String, Int> get()
		= klasses.map { (klass, sub, lvl) -> "Die($klass)" to lvl }.toMap()

	public var hitdice: MutableMap<String, Int> = mutableMapOf() // available hitdice
		// private set

	/* Languages which can be spoken, written and understood. */
	public var languages: List<String> = listOf()


	// collection of feats, class features, race features, special items' abilities and custom counters.
	// how to influence the hero when having these traits?
	// like a temporary equipped item or so.
	public var specialities: List<Speciality> = listOf()

	// TODO: difference between having an item equipped and as long as there the effect or being born with the effect/speciality?
	public var conditions: List<Effect> = listOf()
		private set

	// TODO (2021-03-16) is a condition (like PRONE, bewitched with 'Mage Armor') a (timed) speciality?
	// TODO (2021-03-16) is an equipped magic item like an equipped speciality?
	// TODO (2021-03-16) is an equipped spell like a equipped speciality?

	/** List of spells which are currently equipped. */
	public val spellsPrepared: Set<Pair<SimpleSpell, String>> get() = spells.filter { it.value }.keys

	// TODO restrictions of max 14 spells, max 6 cantrips or infinitely many, etc...
	public val maxPreparedSpells: List<Int>
		get() = specialities.filter { false }.map { 1 } // TODO define max spell count depended by specialities?
		// => maxPreparedSpells[0] = Infitiny or 6
		// => maxPreparedSpells[1] = 13 ...

	/** List of spells, which can be equipped. Given by klasses, race or finding scrolls.
	 * The value (Boolean) represents, if the spell is currently prepared or not. */
	// var spells: MutableMap<Pair<String, String>, Boolean> = mutableMapOf()
	public var spells: Map<Pair<SimpleSpell, String>, Boolean> = mapOf()
		private set

	/** Prepare a spell. It must be from the preparable list.
	 * @param autoLearn if true, it will make the spell to be also preparable, if possible
	 * @return true, if the spell is sucessfully prepared. */
	public fun prepareSpell(spellName: String) : Boolean
		= spells.keys.find { it.first.name == spellName }?.let { spellKey ->
			// spell is learnable
			// maxium of prepared spells
			// all requirements are met.

			// spells[spellKey] = true
			spells += spellKey to true

			true
		} ?: false // not prepared / known.

	/** Learn a new spell, make the spell preparable (but don't prepare it yet).
	* @return true, if the spell is sucessfully prepared. */
	public fun learnSpell(spell: SimpleSpell, spellSource: String) : Boolean
		= if (true) {
			// TODO (2021-03-14) requirements met to learn the spell
			// all requirements are met
			spells += (spell to spellSource) to false
			true
		} else {
			false
		}

	/** Cast a prepraed spell. The spell must be prepared.
	 * TODO or cast spell from equipped items / features?
	 * @param consume if false, casting this spell won't use any spell slot or components or concentration;
	 *                this might simply show if casting would have been possible.
	 * @return if the cast was successful or not. */
	public fun castSpell(spellName: String, useFocus: Boolean = getArcaneFocus() != null, consume: Boolean = true) : Boolean
		= spellsPrepared.find { it.first.name == spellName } != null

	/** Check if the Hero has equipped an Arcane Focus. */
	public fun getArcaneFocus() : SimpleItem?
		= inventory.find { it.first.category == "Arcane Focus" }?.first

	/** Flat hierarchy of which items are currently carried,
	 * linked with how they are carried (with hierarchy).
	 * If the storage reference is empty, it is directly worn / held by the Hero. */
	public var inventory: MutableList<Pair<SimpleItem, String>> = mutableListOf()
		private set

	// eg. clothes are worn
	// put a ring around a necklace? Combined somethings?
	// belts are on clothes
	// backpacks are worn
	// pouches on belt or in backpack
	// smaller pouches in pouches? etc.
	// rope is more or less attached to the backpack?
	// no backpack with special pouches sewn in clothes.

	public fun maxCarriageWeight() : Double = ability(Ability.STR) * 15.0

	/** Get the weight of the equopped / carried items (in pounds / double). */
	public fun getCarriedWeight(storage: String? = null) : Double
		= this.inventory.weight(storage)
}

/** SimpleSkill. */
data class SimpleSkill(val name: String, val ability: Ability) {
	companion object {
		val DEFAULT_SKILLS: Array<SimpleSkill> = arrayOf (
			SimpleSkill("Acrobatics",      Ability.DEX),
			SimpleSkill("Animal Handling", Ability.WIS),
			SimpleSkill("Arcana",          Ability.INT),
			SimpleSkill("Athletics",       Ability.STR),
			SimpleSkill("Deception",       Ability.CHA),
			SimpleSkill("History",         Ability.INT),
			SimpleSkill("Insight",         Ability.WIS),
			SimpleSkill("Intimidation",    Ability.CHA),
			SimpleSkill("Investigation",   Ability.INT),
			SimpleSkill("Medicine",        Ability.WIS),
			SimpleSkill("Nature",          Ability.INT),
			SimpleSkill("Perception",      Ability.WIS),
			SimpleSkill("Performance",     Ability.CHA),
			SimpleSkill("Persuasion",      Ability.CHA),
			SimpleSkill("Religion",        Ability.INT),
			SimpleSkill("Sleight of Hand", Ability.DEX),
			SimpleSkill("Stealth",         Ability.DEX),
			SimpleSkill("Survival",        Ability.WIS),
		)
	}
}

/** Proficient Value for the SimpleSkill. */
enum class SimpleProficiency(val multiplier: Int) { P(1), E(2); };

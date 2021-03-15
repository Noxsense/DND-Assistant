package de.nox.dndassistant.core

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** Refactor IDEA the PlayerCharacter. */
class Hero(player: String?, name: String, race: Pair<String, String>) {
	companion object {
		private val log = LoggerFactory.getLogger("Hero")
	}

	/** String representation of an Hero. */
	override public fun toString() : String
		= "$name (${race.first} (${race.second}), lvl. $level)"

	/* (Fixed) Race. */
	val race: Pair<String, String> = race

	/* (Variable) name and level. */
	var name: String = name
		set(value) {
			field = value.trim() // trimmed name.
		}

	/* level and experience and resulting proficiency bonus. */
	var level = 1
		set(value) {
			field = if (value < 1) 1 else value // not smaller than 1
		}

	val proficiencyBonus: Int get() = when {
		level >= 17 -> +6
		level >= 13 -> +5
		level >= 9 -> +4
		level >= 5 -> +3
		else -> +2
	}

	var experience: Experience = Experience()

	/** Experience a Hero can gain. */
	public class Experience(var points: Int = 0, var method: String = "milestone") {
		override public fun toString() = "$points ($method)"

		public operator fun plus(v: Int) = Experience(points + v, method)
		public operator fun plusAssign(v: Int) { points + v }

		public operator fun minus(v: Int) = Experience(points - v, method)
		public operator fun minusAssign(v: Int) { points - v }

		/** Try to translate the experience points into a level. */
		public fun toLevel() : Int = 0
	}

	// play-game focussed attributes, variable.
	var player: String? = player

	// "collected" inspiration points
	var inspiration: Int = 0
		set(value) {
			field = if (value < 0) 0 else value // prohibit smaller than 0
		}

	// movement speeds ("normal", walking, jumping, swimming, etc. ...)
	var speed: MutableMap<String, Int> = mutableMapOf(
		"walking" to 30 // in feet
	)

	// walking or normal / base speed
	var walkingSpeed: Int
		get() = speed["walking"] ?: 30
		set(value) { speed["walking"] = value }

	var armorSources: List<String> = listOf()
	var armorClass: Int = 1 // XXX ???
		get() = 10 + abilityModifier(Ability.DEX)
		// else  armorSources.map { 1 }.reduce { b, e -> b + e } // sum up all sources
		// private set

	/* Get the current relation of current hitpoints and temporary maximal hitpoints. */
	val hitpoints: Pair<Int, Int> get() = (hitpointsNow + hitpointsTmp) to (hitpointsMax + hitpointsTmp)

	// hitpoints and life
	var hitpointsMax: Int = 0 // maximal hit points
	var hitpointsTmp: Int = 0 // (optional) temporary offset
	var hitpointsNow: Int = 0 // current hit points
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

	var STR: Pair<Int, Boolean> get() = abilities[Ability.STR] ?: (10 to false); set (value) { abilities[Ability.STR] = value }
	var CON: Pair<Int, Boolean> get() = abilities[Ability.CON] ?: (10 to false); set (value) { abilities[Ability.CON] = value }
	var DEX: Pair<Int, Boolean> get() = abilities[Ability.DEX] ?: (10 to false); set (value) { abilities[Ability.DEX] = value }
	var INT: Pair<Int, Boolean> get() = abilities[Ability.INT] ?: (10 to false); set (value) { abilities[Ability.INT] = value }
	var WIS: Pair<Int, Boolean> get() = abilities[Ability.WIS] ?: (10 to false); set (value) { abilities[Ability.WIS] = value }
	var CHA: Pair<Int, Boolean> get() = abilities[Ability.CHA] ?: (10 to false); set (value) { abilities[Ability.CHA] = value }

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

	/* Saving throw proficiencies, by base klass, race and feats. */
	var saveProficiences: List<Ability> = listOf()

	/** Get the pure ability value. */
	public fun ability(a: Ability) : Int
		= abilities.get(a)?.first ?: 10

	/** Get the modifier for the ability. */
	public fun abilityModifier(a: Ability) : Int
		= floor((ability(a) - 10) / 2.0).toInt()

	/** Lists proficient skills and why tney are proficient. */
	var skills: MutableMap<SimpleSkill, Pair<SimpleProficiency, String>> = mutableMapOf()
		private set

	/** Get a skill value. */
	public fun skill(s: SimpleSkill) : Int
		= (abilityModifier(s.ability)
		+ (skills.get(s)?.let { (p, _) -> proficiencyBonus * p.multiplier } ?: 0 )
		)

	public val skillValues: Map<SimpleSkill, Int> get()
		= (SimpleSkill.DEFAULT_SKILLS + skills.keys).map { it to skill(it) }.toMap()

	/* Classes and skills and proficiencies */

	/* Occupations of the Hero. */
	var klasses: MutableList<Triple<String, String?, Int>> = mutableListOf()
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

	val hitdiceMax: Map<String, Int> get()
		= klasses.map { "Hitdie of $it" to 1 }.toMap()
		// = klasses.map { "Hitdie of $it" to "Level of $it" }.toMap()
		// get() = klasses.mapKeys { "Hitdie og ${it}" }.mapKeys { "Level of it."" }

	var hitdice: MutableMap<String, Int> = mutableMapOf() // available hitdice
		// private set

	/* Languages which can be spoken, written and understood. */
	var languages: List<String> = listOf()

	var proficiencies: Map<String, Pair<Boolean, String>> = mapOf()
		private set

	/** Set a skill as proficient. */
	public fun setProficientFor(skill: String, reason: String)
		= this.setProficiencies(skill, false, reason)

	/** Set a skill as expertised. */
	public fun setExpertFor(skill: String, reason: String)
		= this.setProficiencies(skill, true, reason)

	/** Remove any expertise or proficiency of a skill. */
	public fun removeProficientFor(skill: String) {
		this.proficiencies - skill // remove from keys.
	}

	public fun setProficiencies(skill: String, asExpert: Boolean, reason: String) {
		this.proficiencies += skill to (asExpert to reason)
	}

	// collection of feats, class features, race features, special items' abilities and custom counters.
	// how to influence the hero when having these traits?
	// like a temporary equipped item or so.
	var specialities: List<Speciality> = listOf()

	/** List of spells which are currently equipped. */
	val spellsPrepared: Set<Pair<SimpleSpell, String>> get() = spells.filter { it.value }.keys

	// TODO restrictions of max 14 spells, max 6 cantrips, etc...

	/** List of spells, which can be equipped. Given by klasses, race or finding scrolls.
	 * The value (Boolean) represents, if the spell is currently prepared or not. */
	// var spells: MutableMap<Pair<String, String>, Boolean> = mutableMapOf()
	var spells: Map<Pair<SimpleSpell, String>, Boolean> = mapOf()
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
	var inventory: MutableList<Pair<SimpleItem, String>> = mutableListOf()
		private set

	// eg. clothes are worn
	// put a ring around a necklace? Combined somethings?
	// belts are on clothes
	// backpacks are worn
	// pouches on belt or in backpack
	// smaller pouches in pouches? etc.
	// rope is more or less attached to the backpack?
	// no backpack with special pouches sewn in clothes.

	public fun maxCarriageWeight() : Double = STR.first * 15.0

	public fun Collection<Pair<SimpleItem, String>>.printNested() : String {
		// group items by storage place.
		val bagsWithContent = this.groupBy { it.second }

		if (bagsWithContent.size < 1 || !bagsWithContent.containsKey("")) {
			return "No Items held or worn, therefore no bags are carried as well."
		}

		val directlyCarried = bagsWithContent[""]!!

		/* Pretty print:
		 * - nested
		 * - sum up same objects (if they don't carry anything)
		 */
		lateinit var printNestedBags: ((List<Pair<SimpleItem, String>>, Int) -> String)
		printNestedBags = { items, level ->
			val (bagsPre, nobagsPre) = items.partition {
				(i,_) -> i.identifier in bagsWithContent.keys
			}

			// indentation of each nesting level
			val itemSep = "\n" + "\t".repeat(level)

			// sum up same objects, not containing anything.
			val nobags = nobagsPre.groupBy { (i, _) -> i.name }.toList()
				.joinToString(itemSep, itemSep) { (name, allSame) ->
					"- (${allSame.size}x) $name"
				}

			// print nested bags.
			val bags = bagsPre
				.joinToString(itemSep, itemSep) { (it, _) ->
					(bagsWithContent[it.identifier]!!.let { nestedBag ->
						("+ " + it.name
						+ " (carries: ${nestedBag.size}, ${nestedBag.weight()} lb)"
						+ printNestedBags(nestedBag, level + 1))
					})
				}

			((if (nobagsPre.size > 0) nobags else "") // show summed items
			+ (if (bagsPre.size > 0) bags else "") // show carrying items
			)
		}

		return ("Worn / Held"
			+ " (${directlyCarried.size} items, ${directlyCarried.weight()})"
			+ printNestedBags(directlyCarried, 1)
			)
	}

	/** Get the Weight (Double, Pounds) of the collection of items.
	 * If the optional storage is null, get the overall carried weight. */
	public fun Collection<Pair<SimpleItem, String>>.weight(storage: String? = null) : Double
		= this.fold(0.0) { b, i -> b + (if (storage == null || storage == i.second) i.first.weight else 0.0) }

	/** Get the Value (Int, in Coppers) of the collection of items.
	 * If the optional storage is null, get the overall carried value. */
	public fun Collection<Pair<SimpleItem, String>>.copperValue(storage: String? = null) : Int
		= this.fold(0) { b, i -> b + (if (storage == null || storage == i.second) i.first.copperValue else 0) }

	/** Find the item by name. @return Item and its storage. */
	public fun Collection<Pair<SimpleItem, String>>.getItemByName(name: String) : Pair<SimpleItem, String>?
		= this.find { (i, _) -> i.name == name }

	/** Find the item by name. @return Item and its storage. */
	public fun Collection<Pair<SimpleItem, String>>.getItemByID(identifier: String) : Pair<SimpleItem, String>?
		= this.find { (i, _) -> i.identifier == identifier }

	/** Check if an item has other items containing / attached on (aka serves as bag). */
	public fun Collection<Pair<SimpleItem, String>>.isStoringItemsByID(identifier: String) : Boolean
		= this.find { (_, storage) -> storage == identifier } != null // is a storage.

	/** Get all items, that are stored in the given bag, also return the bag object itself. */
	public fun Collection<Pair<SimpleItem, String>>.getBag(bagIdentifier: String) : Triple<SimpleItem, String, List<SimpleItem>>?
		= this.getItemByID(bagIdentifier)?.let { (bag, bagsStorage) ->
			val stored = this.filter { (_, storage) -> storage == bagIdentifier }.map { (i, _) -> i }
			Triple(bag, bagsStorage, stored)
		}

	/** Store or change the current storage of an item to another storage.
	 * If the storage (reference) is null, the item is worn or directly held by the hero.
	 * @param storage item referenence the new item is put i or attached to.
	 * @param force if true, ignore that the storage is not available.
	 * @return true, if the item was successfully stored. */
	public fun MutableCollection<Pair<SimpleItem, String>>.putItem(item: SimpleItem, storage: String = "", force: Boolean = false) : Boolean {
		// XXX (2021-03-12) avoid looping Pouch 1 > Pouch 2 > Pouch 3 > Pouch 1

		/* If `storage` represents an ID, put it to the id, if available.
		 * If `storage` represents just the name of an item, find the next item's ID and put it there. */
		val storageId = when {
			storage == "" -> storage // worn or directl held
			getItemByID(storage) != null -> storage // is already the storage id
			else -> getItemByName(storage)?.first?.identifier
		}
		return if (storageId != null || force) {
			this.plusAssign(item to (storageId ?: storage))
			true
		} else {
			false
		}
	}

	/** Check the inventory and drop all items whose storage is not a stored
	 * item or the hero themself.
	 * @return dropped items and intended storages. */
	public fun MutableCollection<Pair<SimpleItem, String>>.dropIncorrectlyStoredItems() : List<Pair<SimpleItem, String>> {
		val badlyStored = this.filter { (_, s) -> s != "" && getItemByID(s) == null }
		this.minusAssign(badlyStored) // drop
		return badlyStored
	}

	/** Try to drop an item which may be stored or held.
	 * If there is no such item, return false, otherwise (on success) true.
	 * If the optional storage is null, remove from the first storage that appears to have the item.
	 * If the optinal storage is given, remove it from the storage if possible.
	 * If you drop an item, which holds other items, they will be dropped as well.
	 *
	 * Return a possible new collection of items and how they were hold.
	 */
	public fun MutableCollection<Pair<SimpleItem, String>>.dropItem(item: SimpleItem, storage: String? = null) : List<Pair<SimpleItem, String>> {
		// TODO (2021-03-10) what if the item is stored multiple times, but also in different bags (like a dagger in hand and in backpack)

		/* Find all items that matches. */
		val matches = this.filter {
			(i, s) -> i == item && (storage == null || s == storage)
		}

		this.minusAssign(matches)

		return matches
	}
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

data class SimpleSpell(
	val name: String,
	val school: String,
	val castingTime: String,
	val ritual: Boolean,
	val components: Components,
	val range: String,
	val duration: String,
	val concentration: Boolean,
	val description: String,
	val levels: Map<Int, Map<String, Any>>,
	val optAttackRoll: Boolean = false,
	val optSpellDC: Boolean = false, // needs spell DC to be used.
	) {

	public data class Components(
		val verbal: Boolean,
		val somatic: Boolean,
		val materialGP: List<Pair<String, Int>>, // material and least worth (if necessary)
	) {

		/** Custom constructer with no attribute. */
		public constructor() : this(false, false, listOf());

		public companion object {
			/* Only verbal components. */
			val V = Components(true, false, listOf())

			/* Only somatic components. */
			val S = Components(false, true, listOf())

			/** Only Material Components. */
			fun M(materialGP: List<Pair<String, Int>>) = Components(false, false, materialGP)

			/* Verbal somatic components. */
			val VS = Components(true, true, listOf())

			/** Verbal, Material Components. */
			fun VM(materialGP: List<Pair<String, Int>>) = Components(true, false, materialGP)

			/** Somatic, Material Components. */
			fun SM(materialGP: List<Pair<String, Int>>) = Components(false, true, materialGP)

			/** Verbal, Somatic, Material Components. */
			fun VSM(materialGP: List<Pair<String, Int>>) = Components(true, true, materialGP)
		}

		val leastWorthGP: Int = materialGP.fold(0) { b, i -> b + i.second }
		val material: List<String> = materialGP.map { it.first }

		override public fun toString() : String
			= ( (if (verbal) "V" else "")
			+ (if (somatic) "S" else "")
			+ (if (material.size > 0) "M" + (if (leastWorthGP > 0) "gp" else "") else "")
			)
	};

	/** Minimum level this spell needs to be cast with.
	 * Interpreting negeativly given levels as klass levels for levelling cantrips.
	 */
	val baseLevel: Int get() = levels.keys.minOrNull()?.let { min ->
		if (min < 0) 0 else min // negative level => abs(level) as Klass' level; base level is 0
	} ?: 1


	/** Magic Schools of each Spell. */
	public enum class School(val description: String) {
		ABJURATION(
			"""
			Spells are protective in nature, though some of them have aggressive uses.
			They create magical barriers, negate harmful effects, harm trespassers,
			or banish creatures to other planes of existence.
			""".trimIndent()),
		CONJURATION(
			"""
			Spells involve the transportation of objects and creatures from one
			location to another. Some spells summon creatures or objects to the
			caster's side, whereas others allow the caster to teleport to another
			location. Some conjurations create objects or effects out of nothing.
			""".trimIndent()),
		DIVINATION(
			"""
			Spells reveal information, whether in the form of secrets long
			forgotten, glimpses of the future, the locations of hidden things, the
			truth behind illusions, or visions of distant people or places.
			""".trimIndent()),
		ENCHANTMENT(
			"""
			Spells affect the minds of others, influencing or controlling their
			behavior. Such spells can make enemies see the caster as a friend,
			force creatures to take a course of action, or even control another
			creature like a puppet.
			""".trimIndent()),
		EVOCATION(
			"""
			Spells manipulate magical energy to produce a desired effect. Some
			call up blasts of fire or lightning. Others channel positive energy to
			heal wounds.
			""".trimIndent()),
		ILLUSION(
			"""
			Spells deceive the senses or minds of others. They cause people to see
			things that are not there, to miss things that are there, to hear
			phantom noises, or to remember things that never happened. Some
			illusions create phantom images that any creature can see, but the most
			insidious illusions plant an image directly in the mind of a creature.
			""".trimIndent()),
		NECROMANCY(
			"""
			Spells manipulate the  energies of life and death. Such spells can
			grant an extra reserve of life force, drain the life energy from
			another creature, create the undead, or even bring the dead back to
			life.
			Creating the undead through the use of necromancy spells such
			as animate dead is not a good act,
			and only evil casters use such spells frequently.
			""".trimIndent()),
		TRANSMUTATION(
			"""
			Spells change the properties of a creature, object, or environment.
			They might turn an enemy into a harmless creature, bolster the strength
			of an ally, make an object move at the caster's command, or enhance a
			creature's innate healing abilities to rapidly recover from injury.
			""".trimIndent());

		override fun toString()
			= name.capitalize()

		val descriptionLine: String get()
			= description.replace("\n", " ")
	}
}

// val cc = SimpleItem(  "Copper Coin", category: "Currency", 0.02, 1)
// val cs = SimpleItem(  "Silver Coin", category: "Currency", 0.02, 10)
// val cg = SimpleItem(    "Gold Coin", category: "Currency", 0.02, 50)
// val ce = SimpleItem("Electrum Coin", category: "Currency", 0.02, 100)
// val cp = SimpleItem("Platinum Coin", category: "Currency", 0.02, 1000)

// TODO (2021-03-12) effects of the itmes: +3 weapon or weapon at all... with additional attacks
/** SimpleItem.
 * Each item has a weight (@see #weight) and a price value (@see #copperValue), and a category (@see #category).
 * Additionally partical items like a bag of sand an be divided (@see #devidable).
 * A SimpleItem also holds an identifier (@see #identifier), to specify a certain weapon or item is used, dropped or etc, and not all of their kind.
 */
data class SimpleItem(val name: String, val identifier: String, val category: String, val weight: Double, val copperValue: Int, val dividable: Boolean = false) {
	companion object {
		val CC_TO_CC : Int = 1; // copper to copper
		val SC_TO_CC : Int = 10; // silver to copper
		val GC_TO_CC : Int = 50; // gold to copper
		val EC_TO_CC : Int = 100; // electrum to copper
		val PC_TO_CC : Int = 1000; // platinum to copper
	}

	override fun toString() = "$name [$identifier]"

	public fun sameByName(other: SimpleItem) : Boolean
		= this.name == other.name

	public fun sameByNameAndIdentifier(other: SimpleItem) : Boolean
		= this.sameByName(other) && this.identifier == other.identifier
}

//----------------------------------------------------------------------------//

// optional feat [+ (optinal) Flavour]
// Actor; Performance, Charisma, etc.
// Spell sniper: No Coverage but full, doubled ranged attack range
// Ritualer: Ability to have rituals like bard or cleric or etc. (chosen)
abstract class Speciality(val name: String, val count: Count?, val description: String) {
	//
	// count: maximal uses, maybe rechargable, loaded items
	/**
	 * (Optional) Count of a Feature or Trait.
	 * It can have a counted maximal value or is just a counter if the max is 0.
	 */
	class Count(val recharge: String, val max: Int = 0, var current: Int = max, var loaded: List<Any>? = null)  {
		override fun toString() : String
			= ("$current"
			+ (if (max > 0) "/$max" else "")
			+ (if (loaded != null) " $loaded" else "")
			+ (if (recharge.length > 0) " (rechaging: $recharge)" else ""))
	}

	// class LoadedLimit(recharge: String, var items: List<Any>) : Count(recharge, max = items.size(), 0)

	override fun toString() : String
		= name + if (count != null) " { counted: $count }" else ""

	/** Reset counter:Set Current to max. */
	public fun resetCounter() = count?.apply { current = max }

	/** Update the counter: Decrease its current count. */
	public fun countDown(steps: Int = 1) = countUp(-steps)

	/** Update the counter: Increase its current count. */
	public fun countUp(steps: Int = 1) = count?.apply {
		// update
		current += steps

		// don't cross bounds 0 .. max
		if (current < 0) current = 0
		if (max > 0 && current > max) current = max
	}
}

// TODO (2021-03-11) Speciality of an Item (like +3 or additional spells) vs Buff of an Item (one time for period +3 or so)

// Effect like Buff, Debuf or Condition.
// handle count as countdown, which will be used up by time and not by the user. if no countdoen is given, the time alsomdon't uses up the countdown.
// Prone is an Effect("Prone", 0, "Lies on the floor, disadventage on attacks and being attacked by ranged attacks").
// Potion of Haste is an Effect("Haste", 60, "Haste stuff").
// Shield (Spell) is an Effect("Shield", 6, "+2 AC and blocked attack"). // until turn
// Dying?
/** Effect on the Hero, maybe by being pushed or bewitched or other reasons.
 * @param seconds, if given, the time will eat up the effect, otherwise the player or enemy must undo it.
 * @param removable if the hero themselves can remove the effect or if other methods (like remove curse or so) must be done.'*/
class Effect(name: String, seconds: Int = 0, val removable: Boolean = true, description: String) : Speciality(name, Speciality.Count(recharge = "", max = seconds, current = seconds, loaded = null), description);

// TODO should it link to it's klass or the klass to the trait or double linked?
/** KlassTrait given by a klass and subklass on a certain klass level.
 * A Klass Trait may give special abilities, improvements or resources to the Hero.*/
class KlassTrait(name: String, val klass: Triple<String, String, Int>, count: Count?, description: String) : Speciality(name, count, description)

// TODO should it link to it's klass or the klass to the trait or double linked?
/** RaceFeature, given by the race and certain Hero Levels.
 * A race feature may give special abilities, improvements or resources. */
class RaceFeature(name: String, val race: Pair<String, String>, val level: Int = 1, count: Count?) : Speciality(name, count, "");

// TODO should it link to it's klass or the klass to the trait or double linked?
/** A Feat which can be optionally gained by certain circumstances.
 * It may give certain sepcial abilities, improvements or resources. */
class Feat(name: String, count: Count?) : Speciality(name, count, "");

// TODO should it link to it's klass or the klass to the trait or double linked?
// eg. Mage Ring with 5 loaded spells or combinations of summing up a level 5 spell
// eg. each atack with it grants +3 attack bonus and +3 attack damage.
/** An ItemFeature can be hold as lomg as the corresponding Item is held and probably atuned by the Hero.
 * It may give certain improvements, abilities and resources. */
class ItemFeature(name: String, count: Count?) : Speciality(name, count, "");

// custom counter like: defeated dragons or days in this campaign
/** A CustomCount is a counter or countdown made by the Hero's Player to get track of personal and other ideas.
 * For example encountered Dragons or days trained with a unproficient weapon. */
class CustomCount(name: String, count: Count?) : Speciality(name, count, "");


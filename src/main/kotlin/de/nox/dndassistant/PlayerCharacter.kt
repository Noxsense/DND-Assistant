package de.nox.dndassistant

import kotlin.math.floor

private fun getModifier(value: Int) = floor((value - 10) / 2.0).toInt()

private val logger = LoggerFactory.getLogger("PlayerCharacter")

data class PlayerCharacter(
	val name: String,
	val gender: String = "divers",
	val player: String = ""
	) {

	var expiriencePoints: Int
		= 0

	val level : Int get() = when {
		expiriencePoints >= 355000 -> 20
		expiriencePoints >= 305000 -> 19
		expiriencePoints >= 265000 -> 18
		expiriencePoints >= 225000 -> 17
		expiriencePoints >= 195000 -> 16
		expiriencePoints >= 165000 -> 15
		expiriencePoints >= 140000 -> 14
		expiriencePoints >= 120000 -> 13
		expiriencePoints >= 100000 -> 12
		expiriencePoints >= 85000 -> 11
		expiriencePoints >= 64000 -> 10
		expiriencePoints >= 48000 -> 9
		expiriencePoints >= 34000 -> 8
		expiriencePoints >= 23000 -> 7
		expiriencePoints >= 14000 -> 6
		expiriencePoints >= 6500 -> 5
		expiriencePoints >= 2700 -> 4
		expiriencePoints >= 900 -> 3
		expiriencePoints >= 300 -> 2
		else -> 1
	}

	val proficientValue: Int get() = when {
		level >= 17 -> +6
		level >= 13 -> +5
		level >= 9 -> +4
		level >= 5 -> +3
		else -> +2
	}

	var abilityScore: Map<Ability, Int>
		= enumValues<Ability>().map { it }.associateWith { 10 }
		private set

	var abilityModifier: Map<Ability, Int>
		= abilityScore.mapValues { getModifier(it.value) }
		private set

	var savingThrows: List<Ability>
		= listOf()
		private set

	var proficiencies: Map<Skillable, Proficiency>
		= mapOf()
		private set

	fun setAbilityScores(xs: Map<Ability,Int>) {
		abilityScore = xs
		setAbilityModifiers()
	}

	private fun setAbilityModifiers() {
		abilityModifier = abilityScore.mapValues { getModifier(it.value) }
	}

	/* Set the attributes' values.
	 * If a param is set to (-1), it will be rolled with a D20.
	 */
	fun setAbilityScore(a: Ability, v: Int) {
		abilityScore += Pair(a, if (v > 0) v else 0)
		abilityModifier = abilityScore.mapValues { getModifier(it.value) }
	}

	fun savingScore(a: Ability) : Int
		= abilityModifier.getOrDefault(a, 0) +
			if (a in savingThrows) proficientValue else 0

	fun skillScore(s: Skill) : Int
		= abilityModifier.getOrDefault(s.source, 0) +
			getProficiencyFor(s).factor * proficientValue

	fun abilityScore(a: Ability) : Int
		= abilityScore.getOrDefault(a, 10)

	fun abilityModifier(a: Ability) : Int
		= abilityModifier.getOrDefault(a, 10)

	fun getProficiencyFor(skill: Skill) : Proficiency
		= proficiencies.getOrDefault(skill, Proficiency.NONE)

	fun getProficiencyFor(saving: Ability) : Proficiency
		= when (saving in savingThrows) {
			true -> Proficiency.PROFICIENT
			else -> Proficiency.NONE
		}

	/* Add proficiency to saving trhow.*/
	fun addProficiency(saving: Ability) {
		if (!(saving in savingThrows)) savingThrows += saving
	}

	/* Add proficiency to a skill. If twice, add expertise.*/
	fun addProficiency(skill: Skill) {
		// increase the proficcient value.
		proficiencies += Pair(skill, Proficiency.PROFICIENT + proficiencies[skill])
	}

	val initiative: Int get()
		= this.abilityModifier.getOrDefault(Ability.DEX,
			getModifier(this.abilityScore.getOrDefault(Ability.DEX, 0)))

	var conditions : Set<Condition> = setOf()

	private var speedMap : Map<String, Int> = mapOf("walking" to 30 /*feet*/)

	val speed : Map<String, Int> get()
		= if (conditions.any{ when (it) {
			Condition.GRAPPLED, Condition.INCAPACITATED, Condition.PARALYZED,
			Condition.PETRIFIED, Condition.PRONE, Condition.RESTRAINED,
			Condition.STUNNED, Condition.UNCONSCIOUS -> true
			else -> false
		}}) {
			mapOf("prone" to 0)
		} else {
			speedMap
		}

	/* Age of the character, in years. (If younger than a year, use negative as days.) */
	var age : Int = 0

	val alignment : Alignment = Alignment.NEUTRAL_NEUTRAL;

	/* The history of the character. */
	var background: Background = Background("Just Born", listOf(), listOf(), Money())
		private set

	var speciality : String = ""
	var trait : String = ""
	var ideal : String = ""
	var bonds : String = ""
	var flaws : String = ""

	/* The history of the character. */
	var history : List<String>
		= listOf()

	private var backgroundSet = false

	/** Set background, but only once! */
	fun setBackground(
		bg: Background,
		addProf: Boolean = false,
		addItems: Boolean = false,
		chooseSpecial: Int = -1,
		chooseTrait: Int = -1,
		chooseIdeal: Int = -1,
		chooseBonds: Int = -1,
		chooseFlaws: Int = -1
	) {
		if (backgroundSet) return // only set once!

		background = bg
		backgroundSet = true

		if (addProf) {
			// add only as proficient.
			proficiencies += bg.proficiencies.associateWith { Proficiency.PROFICIENT }
		}

		if (addItems) {
			inventory += bg.equipment
		}

		if (chooseSpecial in (1 until bg.suggestedSpeciality.size))
			speciality = bg.suggestedSpeciality[chooseSpecial]

		if (chooseTrait in (1 until bg.suggestedTraits.size))
			trait = bg.suggestedTraits[chooseTrait]

		if (chooseIdeal in (1 until bg.suggestedIdeals.size))
			ideal = bg.suggestedIdeals[chooseIdeal]

		if (chooseBonds in (1 until bg.suggestedBonds.size))
			bonds = bg.suggestedBonds[chooseBonds]

		if (chooseFlaws in (1 until bg.suggestedFlaws.size))
			flaws = bg.suggestedFlaws[chooseFlaws]
	}

	var klasses : Map<Klass, Pair<Int, String>> = mapOf()
		private set

	var klassTraits: Map<String, String> = mapOf()
		private set

	fun addKlassLevel(klass: Klass, specialisation: String = "") {
		/* Check, if conditions are met, to gain a level for that class.*/
		val curKlassLevels = klasses.values.sumBy { it.first }

		if (curKlassLevels >= level) {
			logger.log("WARN", "Cannot add another level, character level is already reached.")
			return
		}

		val old = klasses.getOrDefault(klass, Pair(0, ""))

		val newLevel = old.first + 1
		val newSpecial = if (old.second == "") specialisation else old.second

		klasses += klass to Pair(newLevel, newSpecial)
	}

	var race: SubRace = SubRace( /* Default race. */
		"Human", "", // race
		mapOf("walking" to 30), // speed
		"medium", 1.80, 75.0) // size, weight
		private set

	var space : String = "medium"
	var height : Double = 5.5 /*feet*/
	var weight : Double = 40.0 /*lb*/
	var form: String = "" // short body fitness, description, headliner.
	var appearance: String = "" // more description.

	private var raceSet = false

	/** Set the race. */
	fun setRace(newRace: SubRace, setSpace: Boolean = true, setHeight: Boolean = true, setWeight: Boolean = true) {
		if (raceSet) return
		raceSet = true
		race = newRace
		speedMap += race.speed

		if (setSpace) space = race.space
		if (setHeight) height = race.height
		if (setWeight) weight = race.weight
	}

	var knownLanguages: List<String>
		= listOf("Common")

	var maxHitPoints: Int = -1
	var curHitPoints: Int = -1
	var tmpHitPoints: Int = -1

	val hasTmpHitpoints: Boolean get()
		= tmpHitPoints > 0 && tmpHitPoints != maxHitPoints

	var deathSaves: Array<Int> = arrayOf(0, 0, 0, 0, 0) // maximal 5 chances.
		private set

	fun deathSavesSuccess() {
		for (i in (0 until deathSaves.size)) {
			if (deathSaves[i] == 0) {
				deathSaves[i] = 1
				break
			}
		}
	}

	fun deathSavesFail() {
		for (i in (0 until deathSaves.size)) {
			if (deathSaves[i] == 0) {
				deathSaves[i] = -1
				break
			}
		}
	}

	fun resetDeathSaves() {
		deathSaves.map { _ -> 0 }
	}

	/* Count fails and successes and returns the more significcant value.*/
	fun checkDeathFight() : Int {
		val success = deathSaves.count { it > 0 }
		val failed = deathSaves.count { it < 0 }

		return when {
			success > 2 -> 3
			failed > 2 -> -3
			success > failed -> 1
			success < failed -> -1
			else -> 0
		}
	}

	/* Take a short rest. Recover hitpoints, maybe magic points, etc. */
	fun rest(shortRest: Boolean = true) {
		println("Rest " + (if (shortRest) "short" else "long"))
		// TODO (2020-06-26)
		if (shortRest) {
			// use up to all hit dice and add rolled hit points up to full HP.
			// do other reloadings.
		}
	}

	var spellSlots : List<Int> = (0..9).map { if (it == 0) -1 else Math.max(D10.roll() - D20.roll(), 0) }
		// private set

	// spell, spellcasting source, prepared?
	private var spellsLearnt : Map<Spell, String> = mapOf()
		private set

	/** List the spells, the character has learnt. */
	val spellsKnown: List<Spell> get() = spellsLearnt.keys.toList()

	var spellsPrepared: Map<Spell, Int> = mapOf()
		private set // changes in prepareSpell(Spell,Int)

	/** A map of activated spells with their left duration (seconds). */
	var spellsActive: Map<Spell, Int> = mapOf()
		private set // changes on spellCast(Spell,Int) and spellEnd(Spell)

	/** Get the (first) active spell, which needs concentration. */
	val spellConcentration: Spell? get()
		= spellsActive.keys.find { it.concentration }

	/** Learn a new spell with the given source, this character has.
	 * If the spell is already learnt, do no update.
	 * If the spell source is unfitting for the spell or the spell caster (this),
	 * the spell is also not learnt.
	 * @param spell the new spell to learn.
	 * @param spellSource the source and spellcasting ability, the spell is learnt on the first place.
	 */
	fun learnSpell(spell: Spell, spellSource: String) {
		/* Abort, if the spell is already known. */
		if (spellsLearnt.containsKey(spell))
			return

		/* Abort, if the spell cannot be learnt with the current classes and race. */
		if (false) // TODO (2020-07-25)
			return

		/* If all checked, add new spell to learnt spells. */
		spellsLearnt += spell to spellSource
		logger.info("Learnt spell ${spell}  (as ${spellSource})")

		/* Always prepared cantrip.*/
		if (spell.level < 1) {
			prepareSpell(spell)
		}
	}

	/** Prepare a (learnt) spell.
	 * Prepare a spell for a spell slot.
	 * @param spell the spell to prepare
	 * @param slot the spell level, to prepare the spell for,
	 *     if not given or zero, the prepartion level is set to the spell level,
	 *     if lesse than zero, a prepared spell will be unprepared.
	 * @see prepareSpell(Int, Int).
	 */
	fun prepareSpell(spell: Spell, slot: Int = 0) {
		/* Do not prepare unknown spell. */
		if (!spellsLearnt.containsKey(spell)) {
			return
		}

		if (slot < 0 && spell.level > 0) {
			/* Unprepare (except of cantrips, which cannot be unprepared). */
			spellsPrepared -= spell
			logger.info("Not prepared anymore: ${spell.name}")
		} else {
			/* Prepare spell, at least spell level. */
			spellsPrepared += spell to Math.max(slot, spell.level)
			logger.info("Prepared: ${spell.name}")
		}
	}

	/** Cast a learnt spell.
	 * If neccessary, the spell must also be prepared to be casted.
	 * If the new spell holds concentration, and another spell is already
	 * holding concentration, replace/deactivate that old spell.
	 * Also use up the used spell slot.
	 * If there is no spell slot left, abort the spell cast.
	 * If spell is unknown, abort the spell cast.
	 * If preparation was a requirement, but not done, abort the spell cast.
	 * @param index index of the spell in the current spell list.
	 * @param slot intended spell slot to use, minimum spell.level.
	 */
	fun castSpell(spell: Spell, slot: Int = 0) {
		/* If the spell is unknown, it cannot be casted (abort). */
		if (!spellsLearnt.containsKey(spell)) {
			return
		}

		// TODO (2020-07-25)
		/* If the spell caster needs to prepare,
		 * check if the spell is prepared, otherwise abort casting.*/
		if (false && !spellsPrepared.containsKey(spell)) {
			return
		}

		/* If the new spell needs concentration,
		 * but another spell also holds concentration, replace the old spell. */

		if (spell.concentration) {
			spellConcentration ?. let {
				spellsActive -= it // remove old concentration spell
			}
		}

		// TODO (2020-07-27) SLOT power?

		/* Cast spell for at least 1 second (instantious). */
		spellsActive += spell to Math.max(1, spell.duration)

		logger.info("Casts ${spell.name}, left duration ${spell.duration} seconds")
	}

	/** Decrease left duration of all activated spells.
	 * Remove spells, with left duration below 1 seconds.
	 * @param sec seconds to reduce from active spells.
	 */
	fun tickSpellsActivated(sec: Int = 6) {
		spellsActive = spellsActive
			.mapValues { it.value -  Math.abs(sec) }
			.filterValues { it > 0 }
	}

	var purse: Money = Money()

	var inventory:  List<Item>
		= listOf()
		private set

	/*
	 * Lifting and Carrying (https://dnd.wizards.com/products/tabletop/players-basic-rules#lifting-and-carrying)
	 */

	/* Maximum of the weight, this character could carry. */
	val carryingCapacity: Double get ()
		= abilityScore(Ability.STR) * 15.0

	/* Weight of the inventory and the purse.*/
	val inventoryWeight: Double get()
		= inventory.sumByDouble { it.weight } + purse.weight

	/** Variant: Encumbrance
	 * The rules for lifting and carrying are intentionally simple. Here is a
	 * variant if you are looking for more detailed rules for determining how
	 * a character is hindered by the weight of equipment. When you use this
	 * variant, ignore the Strength column of the Armor table in chapter 5.
	 *
	 * If you carry weight in excess of 5 times your Strength score, you are
	 * encumbered, which means your speed drops by 10 feet.
	 *
	 * If you carry weight in excess of 10 times your Strength score, up to
	 * your maximum carrying capacity, you are instead heavily encumbered, which
	 * means your speed drops by 20 feet and you have disadvantage on ability
	 * checks, attack rolls, and saving throws that use Strength, Dexterity, or
	 * Constitution.
	 */
	fun carryEncumbranceLevel() : Double
		= inventoryWeight / abilityScore(Ability.STR)

	/* Try to buy an item. On success, return true. */
	fun buyItem(i: Item) : Boolean {
		if (purse >= i.cost) {
			purse -= i.cost // give money
			pickupItem(i) // get item
			logger.info("${name} - Bought new ${i} for ${i.cost}, left ${purse}")
			return true
		} else {
			val missed = (purse - i.cost)
			logger.info("${name} - Not enough money to buy. Missed: ${missed}")
			return false
		}
	}

	/* Try to sell an item. On success, return true. */
	fun sellItem(i: Item) : Boolean {
		if (i in inventory) {
			purse += i.cost // earn the money
			// TODO (2020-07-28) not always the same value of stock items.
			dropItem(i) // remove from inventory.
			logger.info("${name} - Sold own ${i} for ${i.cost}, now ${purse}")
			return true
		} else {
			logger.info("${name} - Cannot sell the item (${i}), they do not own this.")
			return false
		}
	}

	fun pickupItem(i: Item) {
		inventory += i
		logger.info("${name} - Picked up ${i}")
	}

	fun dropItem(i: Item) : Boolean {
		// return true on success.
		if (i in inventory) {
			inventory -= (i) // remove from inventory.
			logger.info("${name} - Dropped ${i}")
			return true
		}
		return false
	}

	fun dropItemAt(i: Int) : Item? {
		// return true on success.
		// TODO (2020-07-08) implement
		logger.info("${name} - Dropped ${i}th item")
		return null
	}

	////////////////////////////////////////////////

	val armorClass: Int get() {
		// TODO (2020-06-26)
		// Look up, what the PC's wearing. Maybe add DEX modifier.
		// Not wearing anything: AC 10 + DEX
		return 10 + abilityModifier.getOrDefault(Ability.DEX, 0)
	}

	var equippedArmor: List<Armor> = listOf()
		private set

	/* Equip a piece of armor.
	 * If replace, put the currently worn armor into inventory,
	 * otherwise do not equip.
	 * Only armor, which is in inventory (in possession) can be equipped.*/
	fun wear(a: Armor, replace: Boolean = true) {
		if (a !in inventory) { // abort
			return
		}

		/* Check, if there is already something worn.*/
		val worn = equippedArmor.filter { it.type == a.type }
		val wornSize = worn.size

		/* Human specific. Update for others! TODO */
		var free: Boolean = when (a.type) {
			BodyType.HEAD -> wornSize < 1
			BodyType.SHOULDERS -> wornSize < 1
			BodyType.ACCESSOIRE -> wornSize < 1
			BodyType.HAND -> wornSize < 2
			BodyType.RING -> wornSize < 10
			BodyType.FOOT -> wornSize < 2
		}

		/* Replace the worn equipment, put back to inventory.*/
		if (replace && !free) {
			inventory += worn
			equippedArmor -= worn
			free = true
		}

		if (free) {
			equippedArmor += a
			inventory -= a
		}
	}

	///////////////////////////////////////////////

	var handMain: Weapon? = null
		private set

	var handOff: Weapon? = null
		private set

	fun hand(main: Boolean = true) : Weapon? = when (main) {
		true -> handMain
		false -> handOff
	}

	fun isWieldingAny() : Boolean
		= handMain != null || handOff != null

	/** Check if the hand is wielding a weapon.*/
	fun isWielding(mainHand: Boolean) = when {
		mainHand && handMain != null -> true
		!mainHand && handOff != null -> true
		else -> false
	}

	/* Equip a weapon.
	 * If replace, put the currently wield weapon into inventory,
	 * otherwise do not equip the weapon.
	 * Only weapons, which is in inventory (in possession) can be equipped.*/
	fun wield(w: Weapon, mainHand: Boolean = true, replace: Boolean = false) {
		if (w !in inventory) { // abort
			return
		}

		var success: Boolean = true

		if (replace) unwield(mainHand)

		if (w.isTwoHanded) {
			// needs both hands to be free.
			if (handMain == null && handOff == null) {
				// free hands.
				handMain = w
				handOff = w
			} else if (replace) {
				unwield()
				handMain = w
				handOff = w
			} else {
				success = false
			}
		} else if (mainHand) {
			if (handMain == null) {
				handMain = w // free hand
			} else if (replace) {
				unwield()
				handMain = w
			} else {
				success = false
			}
		} else {
			if (handOff == null) {
				handOff = w // free hand
			} else if (replace) {
				unwield()
				handOff = w
			} else {
				success = false
			}
		}

		if (success) inventory -= w // remove from inventory on success.
	}

	/** Unequip the current weapon(s). */
	fun unwield(mainHand: Boolean = true, both: Boolean = false) {
		if ((mainHand || both) && handMain != null) {
			inventory += handMain!!

			// unequip both hands.
			if (handMain!!.isTwoHanded) {
				handOff = null
			}

			handMain = null
		}

		if ((!mainHand || both) && handOff != null) {
			inventory += handOff!!

			// unequip both hands.
			if (handOff!!.isTwoHanded) {
				handMain = null
			}

			handOff = null
		}
	}
}

/* The base ability score.*/
enum class Ability(val fullname: String) {
	STR("STRENGTH"),
	DEX("DEXTERITY"),
	CON("CONSTITUTION"),
	INT("INTELLIGENCE"),
	WIS("WISDOM"),
	CHA("CHARISMA")
}

enum class BodyType {
	HEAD, // hat, helmet...
	SHOULDERS, // like cloaks ...
	ACCESSOIRE, // like necklace ...
	HAND, // like for one glove (2x)
	RING, // for fingers (10x... species related)
	FOOT, // for one shoe or so (2x)
	// SHIELD // only one // ARM
}

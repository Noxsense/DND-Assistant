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

	var race: Race = Race("Human")
		private set

	var subrace: String = ""
		private set

	var size : Size = Size.MEDIUM // the space this character uses.
	var height : Double = 5.5 /*feet*/
	var weight : Double = 40.0 /*lb*/
	var form: String = "" // short body fitness, description, headliner.
	var appearance: String = "" // more description.

	private var raceSet = false

	/** Set the race. */
	fun setRace(newRace: Race, newSubrace: String) {
		if (raceSet) return

		raceSet = true
		race = newRace
		subrace = newSubrace
		size = newRace.size

		/* Add speed and languages. */
		speedMap += newRace.speed
		println(newRace.languages)
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

	var armor: Armor? = null
		private set

	var clothes: Item? = null
		private set

	var hands: Pair<Item?, Item?> = Pair(null, null)
		private set

	var handsBoth: Item? = null
		private set

	var bag: Container? = null
		private set

	var inventory:  List<Item>
		= listOf()
		private set

	/*
	 * Lifting and Carrying (https://dnd.wizards.com/products/tabletop/players-basic-rules#lifting-and-carrying)
	 */

	/* Maximum of the weight, this character could carry. */
	val carryingCapacity: Double get ()
		= abilityScore(Ability.STR) * 15.0

	val carriedWeight: Double get()
		= ((bag?.sumWeight() ?: 0.0)
		+ (clothes?.weight ?: 0.0)
		+ (armor?.weight ?: 0.0))

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
		= (carriedWeight / abilityScore(Ability.STR))

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

	/** Get an environmental item, and maybe store it into the bag.
	 * If it's a bag and no bag is equpped, equip the bag.
	 * If ${param:storeToBag} is false or no bag is not available,
	 * hold it, maybe swap (if no storing was intended) it with currently hold items.
	 */
	fun pickupItem(i: Item, storeToBag: Boolean = true) {
		// TODO (2020-08-01) sacks and pouches can be hung onto the belt

		/* If no bag is equipped and
		 * the new item is a handfree-carriable container,
		 * use the container as bag. */
		if (i is Container && bag == null) {
			bag = i
			logger.info("${name} - New bag: ${i}")
			return
		}

		if (storeToBag && bag != null) {
			/* Store it into the bag, if .*/
			storeItem(i)
			logger.info("${name} - Pick up: ${i}")
		} else {
			/* Maybe Hold item in the hand. */
			hold(i, !storeToBag)
		}
	}

	/** Hold a new item, maybe drop the recently hold items.
	 * @param i the item to be hold
	 * @param swap if true, replace the currently hold items,
	 *   otherwise do not hold the new item,
	 *   if the hands are filled.
	 * @return a list of items, which were just hold and are replaced by the new item.*/
	fun hold(i: Item, swap: Boolean = true) : List<Item> {
		/* Do we need both hands? */
		val bothHands = false

		var justFreed : List<Item> = listOf()

		/* If swap, maybe free the hands. */
		if (swap) {
			logger.info("Drop something, to hold new thing.")
			justFreed += dropItem(handFirst = true)
			logger.info("Dropped items: ${justFreed}")
		}

		if (bothHands) {
			hands = Pair(null, null)
			handsBoth = i
			logger.info("Holding a big item: $handsBoth")
		} else if (hands.first == null) {
			/* Hold in main hand. */
			hands = Pair(i, hands.second)
			handsBoth = null
			logger.info("Holding in main hand: ${hands.first}")
		} else if (hands.second == null) {
			/* Hold in second hand. */
			hands = Pair(hands.first, i)
			handsBoth = null
			logger.info("Holding in off hand: ${hands.second}")
		} else {
			logger.warn("Cannot hold $i, no free hand, no will to swap.")
		}
		return justFreed
	}

	/** Put an item from the hands or environment into the bag.
	 * If no bag is equipped, do not store the item.
	 * @param i item to store. */
	fun storeItem(i: Item) {
		if (bag != null) {
			bag?.add(i)
			logger.info("${name} - Put item ${i} into bag (${bag})")
		}
	}

	/** Drop one or more items.
	 * @param handFirst if true, drop the item, which is currently held with the main hand.
	 * @param handSecond if true, drop the item, which is currently held with the second hand.
	 * @param bagRemove drop all items, which matches the filter.
	 * @return a list of the dropped items.
	 */
	fun dropItem(handFirst: Boolean = true, handSecond: Boolean = false, dropFilter: Int = -1) : List<Item> {
		var dropped: List<Item> = listOf()

		logger.debug("Drop items: main hand: ${handFirst}, off hand: ${handSecond}, drop filter: ${dropFilter}")

		/* Remove from first hand. */
		if (handFirst && hands.first != null) {
			dropped += (hands.first)!!
			hands = Pair(null, hands.second)
			logger.info("Dropped from first hand \u21d2 ${hands}.")
		}

		/* Remove from second hand. */
		if (handSecond && hands.second != null) {
			dropped += (hands.second)!!
			hands = Pair(hands.first, null)
			logger.info("Dropped from second hand \u21d2 ${hands}.")
		}

		/* Remove from both hands. */
		if ((handFirst || handSecond) && handsBoth != null) {
			dropped += handsBoth!!
			handsBoth = null
			logger.info("Dropped from both hands \u21d2 ${hands}.")
		}

		/* Remove from bag, if bag is equipped. */
		if (bag != null) {
			bag!!.removeAt(dropFilter)
			logger.info("Dropped anything which matched the filter.")
		}

		return dropped
	}

	fun dropItem(i: Item) : Boolean {
		// TODO (2020-08-06) refactor inventory
		// return true on success.
		if (i in inventory) {
			inventory -= (i) // remove from inventory.
			logger.info("${name} - Dropped ${i}")
			return true
		}
		return false
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
		// TODO (2020-08-06) refactor inventory
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

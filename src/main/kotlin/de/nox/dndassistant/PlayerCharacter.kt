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
			// TODO (2020-08-06) implement inventory / equipment
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

	var worn: Map<String, Item> = mapOf() // empty == naked
		private set

	// main hand; off hand; true, if both hands for one item are used.
	var hands: Triple<Item?, Item?, Boolean> = Triple(null, null, false)
		private set

	/** List of extra storage containers.
	 * Main bag, Bags hung on belt, skirt' pocket, a carried chest, etc. */
	var bags: Map<String, Container> = mapOf()
		private set

	/** Maximum of the weight, this character could carry.
	 * @see url(https://dnd.wizards.com/products/tabletop/players-basic-rules#lifting-and-carrying) */
	val carryingCapacity: Double get ()
		= abilityScore(Ability.STR) * 15.0

	/** Get the weight, the character is currenlty carrying. */
	val carriedWeight: Double get()
		= bags.values.sumByDouble {
			if (it is Container) it.sumWeight(true) else it.weight
		}

	/** Variant: Encumbrance
	 * The rules for lifting and carrying are intentionally simple.
	 * When you use this variant, ignore the Strength column of the
	 * Armor table in chapter 5.
	 *
	 * 00x - 05x STR: OK!
	 * 05x - 10x STR: SPEED - 10ft
	 * 10x - 15x STR: SPEED - 20ft, disadventage on DEX/STR/CON (ability, attack rolls, saving throws)
	 */
	fun carryEncumbranceLevel() : Double
		= (carriedWeight / abilityScore(Ability.STR))

	/** Try to get an environmental item, and maybe store it into the bag, if
	 * no hand is free and no valid storage is listed, the item is not picked up.
	 * If the item is a container and the {intoBag} is a not yet assigned bag and not empty name,
	 * equip the item as new bag.
	 * If it's a bag and no bag is equpped, equip the bag.
	 * If ${param:storeToBag} is false or no bag is not available,
	 * hold it, maybe swap (if no storing was intended) it with currently hold items.
	 * @return true on success (the item is actually picked up), else false.
	 */
	fun pickupItem(item: Item, intoBag: String = "") : Boolean {
		// TODO (2020-08-06) refactor inventory: pick up new item.

		val validBag = validStorage(intoBag)
		val newBag = intoBag.startsWith("BAG:") && item is Container

		/* If it's a bag in a bag, also get it it's own bag key */

		/* If not put into a bag, try to hold the item.
		 * If no hand is free, don't pig it up. */
		return when {
			!validBag && newBag -> {
				bags += intoBag to (item as Container)
				logger.info("Got a new bag ($item as $intoBag): SUCCESS")
				return true
			}
			validBag && newBag -> {
				if (storeItem(item = item, bag = intoBag)) {
					// stored first, then add own bag key.

					var bagKey = "${intoBag}:${item.name} No. "

					// add an index number to the new key.
					bagKey += (1 + bags.keys.filter{ it.startsWith(bagKey) }.size)
						.toString()

					// add new bag as "sub bag" with index.
					bags += bagKey to (item as Container)
					logger.info("Got a new bag ($item as $intoBag) inside another bag: SUCCESS")
					return true
				} else {
					return false
				}
			}
			!validBag -> holdItem(item).also {
				logger.info("Try to hold item ($item): ${if (it) "SUCCESS" else "FAILED"}")
			}
			else -> storeItem(item = item, bag = intoBag).also {
				logger.info("Try to store item ($item): ${if (it) "SUCCESS" else "FAILED"}")
			}
		}
	}

	private fun validStorage(storage: String) : Boolean
		= storage.startsWith("BAG:") && bags.containsKey(storage)

	/** Hold a new item, if the needed hands are not free, don't hold this item.
	 * @param item the item to be hold
	 * @param preferOff If true, and both hands are free, use the off hand; otherwise default: use main hand.
	 * @param usedHands
	 *   If 0, use the suggested count of hands of the item,
	 *   if 1 enforce to hold the item with one hand,
	 *   if 2 enforst to hold the item with both hands.
	 * @return true on success (the item is now held), else false. */
	fun holdItem(item: Item, preferOff: Boolean = false, usedHands: Int = 0) : Boolean {
		/* Do we need both hands? */
		val bothHands = (false && usedHands == 0) || (usedHands == 2)

		if (bothHands && hands.first == null && hands.second == null) {
			// hold one item with both hands.
			hands = Triple(item, null, true)
			logger.info("Hold item ($item) with both hands ($hands)")
			return true
		} else if ((preferOff || hands.first != null) && hands.second == null) {
			// hold item in off hand, if it is free
			// if a two-hand wielded item was just hold, it's now single hand wielded.
			hands = Triple(hands.first, item, false)
			logger.info("Hold item ($item) with off hand ($hands)")
			return true
		} else if (hands.first == null) {
			// hold item in main hand, if it is free
			// if a two-hand wielded item was just hold, it's now single hand wielded.
			hands = Triple(item, hands.second, false)
			logger.info("Hold item ($item) with main hand ($hands)")
			return true
		}
		return false
	}

	/** Put an item from the hands or environment into an container equipped by the key "bag".
	 * If no bag is equipped, do not store the item.
	 * @param bag key for a container to store the given item.
	 * @param item item to store.
	 * @return true on success (the item is now held), else false. */
	fun storeItem(item: Item, bag: String) : Boolean {
		val bagContainer = bags.getOrElse(bag, { null })
		if (bagContainer != null) {
			bagContainer?.insert(item)
			logger.info("${name} - Put item ${item} into bag (${bag}: ${bagContainer})")
			return true
		}
		return false
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

		/* Remove from bag, if bag is equipped. */

		return dropped
	}

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
		// TODO (2020-08-06) implement.
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

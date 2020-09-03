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
	fun addProficiency(skill: Skillable) {
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

		/* Add hitdie to hitdice */
		addHitdie(klass.hitdie.faces)

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

	/** A list of Hitdice and the number of used dice.
	 * Example: 6d12 (3 used / as barbarian) and 2d6 (1 used / as sorcered). */
	var hitdice : Map<SimpleDice, Int> = mapOf()
		private set

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
		if (shortRest) {
			// use up to all hit dice and add rolled hit points up to full HP.
			// do other reloadings.
			// TODO (2020-09-03) add param with how many hitdice will be spent
			logger.info("Short rest")
		} else {
			logger.info("Long rest")

			/* Back to full hp. */
			curHitPoints = maxHitPoints

			/* Restore half of used hit dice. */
			// hitdice.mapValues { it / 2 }

			/* Restore spell slots and other features. */
			// TODO (2020-09-03) implement magic points restoration [ RULES needed ]
		}
	}

	/** Use a hitdie, return how many are left.
	 * @param face face of the hitdie, which will be used.
	 * @return left hitdice for that face.
	 */
	fun useHitdie(face: Int) : Int {
		var dice = hitdice.filterKeys { it.faces == face }

		/* No such hit die available to spend. */
		if (dice.keys.size < 1) {
			return 0
		} else {
			val count = dice.keys.sumBy { it.factor }
			var spent = dice.values.sumBy { it }

			/* Accidently splitted to multiple hit die stacks. => Fix that. */
			val newdice = SimpleDice(face, count)
			hitdice -= dice.keys

			/* If enough hit dice available, spent another die. */
			if (spent < count) {
				spent += 1
			}

			hitdice += newdice to spent

			return count - spent
		}
	}

	/** Get a new hitdie (by leveling up a klass).
	 * @param face new hitdie's face.
	 * @return get the new maximum number of hitdice.
	 */
	fun addHitdie(face: Int) : Int {
		logger.debug("Add new hitdie d${face}")

		val curDice : Map<SimpleDice, Int>
			= hitdice.filterKeys { it.faces == face }

		val spentDice = curDice.values.sumBy { it }
		val newCount = (curDice.keys.sumBy { it.factor }) + 1

		val newDice = SimpleDice(face, newCount)

		hitdice -= curDice.keys
		hitdice += newDice to spentDice

		logger.debug("New hitdice: ${hitdice}")

		return newCount
	}

	/** ordered list of max and available spellslots. */
	var spellSlots : List<Pair<Int,Int>>
		= (0..9).map { if (it == 0) (-1 to -1) else (0 to 0) }
		private set

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

	/** The number of free hands. */
	val countFreeHands: Int get()
		= hands.toList().filter { it == null }.size

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
		= (carriedWeightWorn // worn weight
		+ carriedWeightHands // currently hold
		+ carriedWeightBags) // in bags, with bags.

	val carriedWeightWorn : Double get()
		= worn.values.sumByDouble { it.weight }

	val carriedWeightHands : Double get()
		= hands.toList().sumByDouble { when {
			it == null -> 0.0
			it is Container -> it.sumWeight(true)
			it is Item -> it.weight
			else -> 0.0
		}}

	val carriedWeightBags : Double get()
		= bags.values.sumByDouble { it.sumWeight(true) }

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

					var bagKey = "${intoBag}:NESTED ${item.name} No. "

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
		= (storage.startsWith("BAG:") && bags.containsKey(storage))

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

	/** If the item is held with two hands hold now with one hand,
	 * otherwise if an item is hold with one hand and both hands are free,
	 * hold with two hands.
	 * @return true, if the state changed (on success), otherwise false. */
	fun toggleBothHands() : Boolean {
		if (hands.third) {
			hands = Triple(hands.first, hands.second, false)
			return true
		} else if (hands.first != null && hands.second == null) {
			hands = Triple(hands.second, null, true)
			return true
		} else if (hands.second != null && hands.first == null) {
			hands = Triple(hands.second, null, true)
			return true
		} else {
			return false // other hand is not free or no item is carried at all.
		}
	}

	/** Put an item from the hands or environment into an container equipped by the key "bag".
	 * If no bag is equipped, do not store the item.
	 * @param bag key for a container to store the given item.
	 * @param item item to store.
	 * @return true on success (the item is now held), else false. */
	fun storeItem(item: Item, bag: String) : Boolean {
		val bagContainer = bags.getOrElse(bag, { null })
		if (bagContainer != null) {
			val s0 = bagContainer.size // count items insied
			bagContainer.insert(item) // try to store, only reference differenciable objects.
			val success = bagContainer.size != s0 // success, if one more.
			return success
		}
		return false
	}

	/** Put on clothes or armor.
	 * If no neccessary position is free, don't wear the wearable item.
	 * @param wearable the item to be worn.
	 * @return true, if the wearable is successfully equipped, else false.
	 */
	fun wear(wearable: Wearable) : Boolean {
		/* Check, if there is already something worn.*/
		val bodyPosition = wearable.position
		val wornThere = worn.filterKeys { it == bodyPosition.name }
		val clothesCount = wornThere.size

		/* Human specific. Update for others! TODO */
		var full: Boolean = when (bodyPosition) {
			BodyType.HEAD,
			BodyType.SHOULDERS,
			BodyType.NECK,
			BodyType.BODY,
			BodyType.SHIELD,
			BodyType.ARMOR -> clothesCount > 0

			BodyType.HAND,
			BodyType.FOOT -> clothesCount > 1

			BodyType.RING -> clothesCount > 9
		}

		if (full) {
			logger.info("Does not wear ${wearable}, already full at $bodyPosition")
			return false
		} else {
			worn += bodyPosition.name to wearable
			logger.info("Wears now ${wearable}")
			return true
		}
	}

	/** Drop one or more items.
	 * @param handFirst if true, drop the item, which is currently held with the main hand.
	 * @param handSecond if true, drop the item, which is currently held with the second hand.
	 * @param bagRemove drop all items, which matches the filter.
	 * @return a list of the dropped items.
	 */
	fun dropFromHands(handFirst: Boolean = true, handSecond: Boolean = false) : List<Item> {
		var dropped: List<Item> = listOf()

		logger.debug("Drop items: main hand: ${handFirst}, off hand: ${handSecond}")

		/* Remove from hands first. */
		if (handFirst || handSecond) {
			if (hands.third) {
				dropped += hands.toList()
					.filter { it != null && it is Item }
					.map { it as Item }
				hands = Triple(null, null, false)
			}

			if (handFirst && hands.first != null) {
				dropped += hands.first!!
				hands = Triple(null, hands.second, false)
			}

			if (handSecond && hands.second != null) {
				dropped += hands.second!!
				hands = Triple(hands.second, null, false)
			}
		}

		logger.info("Dropped ${dropped}")

		return dropped
	}

	/** Drop matching items, which are stored in a held bag (also nested bags.)
	 * @return a list of all dropped items. */
	fun dropFromBags(bagKeys: Collection<String>, predicate: (index: Int, item: Item) -> Boolean) : List<Item> {
		/* Drop from every valid bag, if the bagKeys are not specified. */
		if (bagKeys.isEmpty()) {
			return dropFromEveryBag(predicate)
		}

		var dropped: List<Item> = listOf()
		logger.info("Drop fron ${bagKeys.associateWith { validStorage(it) } }")
		bagKeys.forEach { key ->
			if (validStorage(key)) {
				// bag exists.
				val d0 = bags[key]!!.removeAll(predicate)
				logger.info("From $key, dropped ${d0.size} items ${d0}")
				dropped += d0
			}
		}
		logger.info("Totally dropped ${dropped.size} items:  ${dropped}")
		return dropped
	}

	/** Drop matching items from one bag.
	 * @see dropFromBags(Collection<String>,(Item)->Boolean) */
	fun dropFromBag(bagKey: String, predicate: (index: Int, item: Item) -> Boolean) : List<Item>
		= dropFromBags(listOf(bagKey), predicate)

	/** Drop matching items from every bag.
	 * @see dropFromBags(Collection<String>,(Item)->Boolean) */
	fun dropFromEveryBag(predicate: (index: Int, item: Item) -> Boolean) : List<Item>
		= dropFromBags(bags.keys, predicate)

	/** Drop items on matching equipment positions.
	 * (such as clothings or equipped bags, not nested bags.)
	 * @return a list of all dropped items. */
	fun dropEquipped(positions: Collection<String>) : List<Item> {
		var dropped: List<Item> = listOf()

		// drop clothings and what's worn.
		dropped += worn.filterKeys { it in positions }.values
		worn -= positions

		// drop bags
		// XXX do we also want to drop nested bags like that?
		dropped += bags.filterKeys { it in positions }.values
		bags -= positions

		logger.info("Dropped ${dropped}")
		return dropped
	}

	/** Drop items in matching positions.
	 * @see dropEquipped(Collection<String>) */
	fun dropEquipped(position: String) : List<Item>
		= dropEquipped(listOf(position))

	/** Drop items from all positions.
	 * @see dropEquipped(Collection<String>) */
	fun dropEquipped() : List<Item>
		= dropEquipped(worn.keys + bags.keys)

	val armorClass: Int get() {
		val dex = abilityModifier(Ability.DEX)
		val dex2 = Math.min(dex, 2)

		val wornArmor = worn[BodyType.ARMOR.name] as Armor?
		val wornShield = worn[BodyType.SHIELD.name] as Armor?

		val armorProcicienies = proficiencies
			.filterKeys { it is Armor }
			.map { (it.key as Armor).weightClass }

		val L = Armor.Weight.LIGHT
		val M = Armor.Weight.MEDIUM
		val H = Armor.Weight.HEAVY
		val S = Armor.Weight.SHIELD

		/* if shield: (proficient & 0~1 one-handed weapon) or (0 weapon) => +2 */
		val shieldAC = when {
			wornShield == null -> 0
			(S in armorProcicienies && countFreeHands < 2
				|| countFreeHands > 1) -> wornShield.armorClass
			else -> 0
		}

		val armed = wornArmor != null || wornShield != null
		val unarmedeBoni = if (!armed && false) abilityModifier(Ability.WIS) else 0

		val wAC = wornArmor?.armorClass ?: 0 // armored AC
		val uAC = 10 + dex + unarmedeBoni // unarmored AC

		// TODO (2020-08-13) unarmered defense
		// - races and classes benefitting from unarmed defense / natural armor ...
		// - no armor or few => alternative modifiers.
		
		// TODO (2020-08-13) additional spells, changed AC
		// conditions, etc..

		/* Eventually calculate
		 * armored or unarmored armor class with modifiers.
		 * - Not wearing anything: AC 10 + DEX
		 * - LIGHT Armor: Armor's AC + DEX
		 * - MEDIUM Armor: Armor's AC + Min(DEX, 2)
		 * - HEAVY Armor: Armor's AC */
		val ac = when {
			wornArmor == null -> uAC
			wornArmor.weightClass == L -> wAC + dex
			wornArmor.weightClass == M -> wAC + dex2
			wornArmor.weightClass == H -> wAC
			else -> uAC
		}
		
		return ac + shieldAC
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
	NECK, // like necklace ...
	BODY, // like main outfit, onsies or dresses ...
	HAND, // like for one glove (2x)
	RING, // for fingers (10x... species related)
	FOOT, // for one shoe or so (2x)
	ARMOR, // worn above clothes.
	SHIELD; // only one; can strapped on one arm, maximally one weapon can be wield, and other non-combat items can be hold.
}

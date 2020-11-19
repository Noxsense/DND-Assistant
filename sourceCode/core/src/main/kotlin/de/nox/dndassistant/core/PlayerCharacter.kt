package de.nox.dndassistant.core

import kotlin.math.floor
import kotlin.math.min

// TODO (2020-09-30) renaming, genderfluid, ...

public class PlayerCharacter private constructor(
	val player: String,

	val race: Race,
	val subrace: String,

	val background: Background,
	val backgroundFlavour: String,

	val klassFirst: Klass,
	var baseKlassSpeciality: String = "",

	var name: String,
	var gender: String,
	var age: Int
) {
	val log: Logger = LoggerFactory.getLogger("D&D PC")

	/** The character Creator, Builder for a PlayerCharacter. */
	data class Creator(val playername: String) {
		var race: Race? = null
		var subrace: String? = null

		var background: Background? = null
		var backgroundFlavour: String? = null

		var klassFirst: Klass? = null
		var baseKlassSpeciality: String = ""

		var name: String = "Name"
		var gender: String = "divers"
		var age: Int = 0

		/** Set race (and subrace). @return this@Creator. */
		fun race(race: Race, subrace: String) = apply {
			this.race = race
			this.subrace = subrace
		}

		/** Set Background (and flavour). @return this@Creator. */
		fun background(background: Background, flavour: String) = apply {
			this.background = background
			this.backgroundFlavour = flavour
		}

		/** Set the base klass. @return this@Creator. */
		fun klass(klass: Klass, speciality: String = "") = apply {
			this.klassFirst = klass
			this.baseKlassSpeciality = speciality
		}

		/** Set the age in years. @return this@Creator. */
		fun ageInYears(years: Int) = apply {
			this.age = Math.abs(years)
		}

		/** Set the age in days, if larger than 5 years, translate it to years.
		 * @return this@Creator. */
		fun ageInDays(days: Int) = apply {
			if (days > 5 * 365 + 1) {
				ageInYears(days / 365)
			} else {
				this.age = - Math.abs(days) // make sure, it's a negative number.
			}
		}

		/** Set name. @return this@Creator. */
		fun name(name: String) = apply { this.name = name }

		/** Set gender. @return this@Creator. */
		fun gender(gender: String) = apply { this.gender = gender }

		/** Build, but a fancy word for living things.
		 * @return the new PlayerCharacter.
		 * @throws NullPointerException if an attribute is still null.
		 */
		fun awake() = PlayerCharacter(
			playername,
			race!!, subrace!!,
			background!!, backgroundFlavour!!,
			klassFirst!!, baseKlassSpeciality,
			name, gender, age
		).apply {
			hitpoints = klassFirst.hitdie.faces + abilityModifier(Ability.CON)
		}
	}

	/** The one and only player, unless the character is free for adoption. */
	val playername: String = player

	/* ------------------------------------------------------------------------
	 * Stats, Values and Scores defined by Klass, Race and Numbers.
	 */

	var experiencePoints : Int
		= 0
		// public getter and setter
		// changes => will influence level and proficiency bonus and depended values.
		// additional fun: setLevel() which sets the XP accordingly

	/** Character level according to experience points. */
	val level : Int get() = xpToLevel(experiencePoints)

	/** Race and subrace. */
	private var raceSubrace: Pair<Race, String> = Race("Human") to "Normal"

	/** Map of speed on different terrains and specialities. */
	var speedMap : Map<String, Int>
		= mapOf("normal" to 30 /*feet*/).apply {
			plus("difficult" to (get("normal")?.div(2) ?: 0)) /*feet*/
			plus("climbing" to  (get("normal")?.div(2) ?: 0)) /*feet*/
			plus("swimming" to  (get("normal")?.div(2) ?: 0)) /*feet*/
			plus("crawling" to  (get("normal")?.div(2) ?: 0)) /*feet*/
			plus("flying" to 0) /*feet*/

			// "longjump": run/spent 10ft => STR score ft
			// "highjump": run/spent 10ft => 3 + STR mod ft
		}
		private set

	/** Proficiency bonus according to character level. */
	val proficiencyBonus: Int get() = levelToProficiencyBonus(level)

	/** THE basic ability scores. */
	private var abilityScore: Map<Ability, Int>
		= enumValues<Ability>().map { it }.associateWith { 10 }
		private set

	/** Get the pure ability score. */
	fun abilityScore(a: Ability) : Int
		= abilityScore.getOrElse(a, { 10 })

	/** Get the ability modifier. */
	fun abilityModifier(a: Ability) : Int
		= Ability.scoreToModifier(abilityScore.getOrElse(a, { 0 }))

	/** The combat's initiative, mostly the DEX modifier. (in-combat) */
	val initiative: Int get()
		= this.abilityModifier(Ability.DEX)

	/** Map of K/Classes (with Level and Specialities), the character has. */
	var klasses : Map<Klass, Pair<Int, String>> = mapOf()
		private set
		// see also klassFirst

	/** Map of traits, gained from reaching levels in a certain class. */
	var klassTraits: Map<String, String> = mapOf()
		private set

	/** Hit dice as a list of faces, gained by every class level up. */
	val hitdice : List<Int>
		get() = klasses.toList().flatMap { (klass, lvlSpec) ->
			/* Add level times the hitdie face to the hitdice list. */
			(1 .. (lvlSpec.first)).map { klass.hitdie.faces }
		}

	/** Maximal hit points of the character.
	 * If dropped to 0, the character becomes unconscious.
	 * If dropped to negative max HP in One hit, the character dies at once. */
	var hitpoints: Int
		= klassFirst.hitdie.faces + abilityModifier(Ability.CON) // first HP

	/** The abilities, the character has proficiency for saving throws. */
	var savingThrows: List<Ability>
		= listOf()
		private set

	/** The list of skills, the character has proficiency bonus (and maybe expertise). */
	var proficiencies: Map<Skillable, Proficiency>
		= mapOf()
		private set

	/** The list of languages the character has learnt, can understand and uses. */
	var knownLanguages: List<String> = listOf()
		private set

	/** Get the score for the requested ability saving throw. */
	fun savingScore(a: Ability) : Int
		= abilityModifier(a) + getProficiencyFor(a).second

	/** Get the score for the requested skill check. */
	fun skillModifier(s: Skill) : Int
		= abilityModifier(s.source) + getProficiencyFor(s).second

	// TODO (2020-10-05) other spell sources ? other feat sources: like x times a long/short rest. ki points, etc.

	/** Spell slots the character has available and used.
	 * Ordered list of max and available spell slots. */
	public var spellSlots: IntArray
		= IntArray(9) { 0 } // maximal available, 1 to 9 as [0 .. 8]
		private set

	/** Getter for a available, left spell slot. */
	fun spellSlot(slot: Int) : Int
		= spellSlots[Math.min(Math.max(slot - 1, 0), 8)]

	/** List of spells, this character has learnt, and the source, where it was learnt.
	 * The source may influence the spell casting ability and strength of a cast spell. */
	var spellsLearnt : Map<Spell, String> = mapOf()
		private set

	/** List the spells, the character has learnt
	 * They are sorted by: Cocnentration > Cast > Prepared > Rest. */
	val spellsKnown: List<Spell> get() = spellsLearnt.keys.toList()
		.sortedWith{ a, b ->

			/* Check, if both spells are prepared. */
			val aPrep = a in current.spellsPrepared
			val bPrep = b in current.spellsPrepared

			/* Check if the spell is cast or not. */
			val aCast = current.spellsCast.containsKey(a)
			val bCast = current.spellsCast.containsKey(b)

			when {
				a == b -> {
					0 /* spells are the same. */
				}
				aPrep != bPrep -> {
					/* One spell is prepared, the other is not. */
					-aPrep.compareTo(bPrep) // descending: true smaller false
				}
				!aPrep || (aPrep && !aCast && !bCast) -> {
					/* Not different and one is not prepared
					 * => Both spells are not prepared/equipped.
					 * OR:
					 * NOt different and one is prepared
					 * => Both spells are prepared, both are not cast. */
					a.compareTo(b) // compares by level and name.
				}
				aCast != bCast -> {
					/* Both spells are prepared/equipped. (Else caught before.)
					 * Only one is cast. */
					-aCast.compareTo(bCast) // descending: true smaller false
				}
				else -> {
					/* Both spells are prepared, both are cast.
					 * Get each rest time; sort concentration to head. */
					val spellContr = current.spellConcentration?.first ?: null

					val aRestTime = if (a == spellContr) 0 else current.spellsCast[a]!!.second
					val bRestTime = if (b == spellContr) 0 else current.spellsCast[b]!!.second

					aRestTime.compareTo(bRestTime)
				}

			}
		}

	/* ------------------------------------------------------------------------
	 * Equipment and inventory.
	 */

	/** The pure money the character carries. Probably in an extra bag. */
	var purse: Money = Money()

	/** The equipment, the character wears. */
	var worn: Map<String, Item> = mapOf() // empty == naked
		private set

	// see armor class. (in-combat value).
	// depended on currently worn equipment and classes. and proficiencies (armor)

	// main hand; off hand; true, if both hands for one item are used.
	var hands: Triple<Item?, Item?, Boolean> = Triple(null, null, false)
		private set

	/** List of extra storage containers.
	 * Main bag, Bags hung on belt, skirt' pocket, a carried chest, etc. */
	var bags: Map<String, Container> = mapOf()
		private set

	/** Supporting value to build an attack with the current STR and DEX modifiers. */
	private val modsStrDex: Pair<Int, Int> get()
		= abilityModifier(Ability.STR) to abilityModifier(Ability.DEX)

	/** Get the attack damage for an unarmed strike. */
	val attackUnarmed: Attack get()
		= Attack(
			name = "Unarmed Strike",
			note = """
				Punch, kick, head-butt, or similar forceful blow.
				Anything without a weapon or spell.
				""".trimIndent(),
			ranged = false,
			finesse = false, // TODO (2020-10-10) unarmed strike is finesse with certain exceptions. classes
			damage = DiceTerm(SimpleDice(1)) to setOf(DamageType.BLUDGEONING),
			proficientValue = proficiencyBonus, // proficient with own hands
			modifierStrDex = modsStrDex // XXX (2020-10-07) ...
		)

	/** Get the attack damage for an improvised (melee) attack. */
	val attackImprovised: Attack get()
		= Attack(
			name = "Improvised Attack",
			note = """
				Use another thing as you describe,
				but it doesn't really resemble any weapon.
				Damage Type depends on the way
				it was used or its own characterists.
				""".trimIndent(),
			ranged = false,
			finesse = false,
			damage = DiceTerm(SimpleDice(4)) to setOf(), // depends on the chosen item/ way to use.
			proficientValue = proficiencyBonus, // XXX (2020-10-07) depends on the way it was intended?
			modifierStrDex = modsStrDex // XXX (2020-10-07) ...
		)

	/** Get the attack damage for the currently hold item. */
	val attackEquipped: Attack? get()
		= null // TODO (2020-10-10) equipped attack getter()

	/** Get the attack damage for the weapons (or items) which needs to be drawn. */
	val attackDrawNew: List<Attack> get()
		= bags.values.flatMap { it.inside }.distinct()
			.filter { it is Weapon /* && it !=  equippedStuff */}
			.map{ (it as Weapon).let { wpn -> Attack(
				name = "Draw and Attack with '${wpn.name}'",
				note = wpn.note,
				ranged = wpn.weaponType == Weapon.Type.SIMPLE_RANGED,
				finesse = wpn.isFinesse,
				damage = wpn.damage to setOf(),
				proficientValue = proficiencyBonus,
				modifierStrDex = modsStrDex
			)}}

	/* ------------------------------------------------------------------------
	 * Personality and character, roleplaying.
	 * Getter and Setter are all public and modifiable, to show progress in play.
	 * Their change should not affect other attributes.
	 */

	/** Age of the character, in years.
	 * (If younger than a year, use negative as days.) */
	// var age : Int = 0
	val ageString: String get()
		= when {
			age > 0 -> "$age yrs"
			else -> "${-age} days"
		}

	/** Alignment of the character (roleplay). */
	val alignment : Alignment = Alignment.NEUTRAL_NEUTRAL;

	/** Visible appearance of the character. (roleplay, also positioning). */
	var size : Size = Size.MEDIUM // the space this character uses.

	/** Visible appearance of the character. (roleplay). */
	var height : Double = 5.5 /*feet*/

	/** Visible appearance of the character. (roleplay, suggested by race). */
	var weight : Double = 40.0 /*lb*/

	/** Visible appearance of the character. (roleplay, suggested by race). */
	var form: String = "" // short body fitness, description, head liner.

	/** Visible appearance of the character. (roleplay). */
	var appearance: String = "" // more description.

	/** Background and story information. (roleplay) */
	var trait : String = ""

	/** Background and story information. (roleplay) */
	var ideal : String = ""

	/** Background and story information. (roleplay) */
	var bonds : String = ""

	/** Background and story information. (roleplay) */
	var flaws : String = ""

	/* The history of the character. (roleplay) */
	var history : List<String>
		= listOf()

	/* ------------------------------------------------------------------------
	 * Status and temporarily conditions.
	 */

	var current: State = State(this)

	/* TODO (2020-10-02) DELETEME, Refactor me

	val speed : Map<String, Int> get()
		= if (state.conditions.any{ when (it) {
			Condition.GRAPPLED, Condition.INCAPACITATED, Condition.PARALYZED,
			Condition.PETRIFIED, Condition.PRONE, Condition.RESTRAINED,
			Condition.STUNNED, Condition.UNCONSCIOUS -> true
			else -> false
		}}) {
			mapOf("prone" to 0)
		} else {
			speedMap
		}
	*/

	/* ------------------------------------------------------------------------
	 * Override basic class methods.
	 */

	/** Initiate the character. */
	init {
		// add base klass also to the learnt klasses.
		// baseKlassSpeciality by default ("") aka not given.
		klasses += klassFirst to Pair(1, baseKlassSpeciality)
	}

	/** PlayerCharacter is equal to another PlayerCharacter,
	 * if the player and the character name are the same. */
	override fun equals(other: Any?) : Boolean
		= (other != null
		&& other is PlayerCharacter
		&& other.playername == playername
		&& other.name == name)

	/** String representation: Name, The Klass (char.lvl). */
	override fun toString() : String
		= "${name}, The ${klassFirst} (lvl ${level})"

	/* ------------------------------------------------------------------------
	 * (Simple) Getter and Setters..
	 */

	/** Set the experience points to fit the given level. */
	fun setLevel(lvl: Int) {
		experiencePoints = levelToXP(lvl)
	}

	/* Set the attributes' values.
	 * If a param is set to (-1), it will be rolled with a D20.
	 * If the constitution is changed while on first level:
	 * adjust the first-level hitpoints. */
	fun setAbilityScore(a: Ability, v: Int) {
		abilityScore += Pair(a, if (v > 0) v else 0)

		if (a == Ability.CON && level < 2) {
			hitpoints = abilityModifier(a) + klassFirst.hitdie.faces
		}
	}

	/** Get a proficiency and the resulting bonus value for any skill.
	 * If the parameter was a skill or saving throw, the character has actually
	 * proficiency for, return the proficiency value, otherwise it has none.
	 * @param x anything, which could have proficiency.
	 * @return (Proficiency.NONE, 0) by default. */
	fun getProficiencyFor(x: Any) : Pair<Proficiency, Int> {
		val proficiency = when {
			x is Skill -> proficiencies.getOrDefault(x, Proficiency.NONE)
			x is Ability && (x in savingThrows) -> Proficiency.PROFICIENT
			x is Weapon -> proficiencies
				.getOrDefault(x, proficiencies // get proficiency for the weapon itself
				.getOrDefault(x.weaponType, // get proficiency for the weapon type instead
				Proficiency.NONE)) // or return no proficiency at all
			else -> Proficiency.NONE
		}

		val value = proficiency.factor * proficiencyBonus

		return proficiency to value
	}

	fun isProficientIn(any: Skillable): Boolean
		= (proficiencies.contains(any)
		|| (any is Weapon && proficiencies.contains(any.weaponType)))

	/* Add proficiency to saving throw.*/
	fun addProficiency(saving: Ability) {
		if (!(saving in savingThrows)) savingThrows += saving
	}

	/* Add proficiency to a skill. If twice, add expertise.*/
	fun addProficiency(skill: Skillable) {
		// increase the proficient value.
		proficiencies += Pair(skill, Proficiency.PROFICIENT + proficiencies[skill])
	}

	/** Add a new level for a certain class.
	 * @param klass the class, the character levels in.
	 * @param specialisation the path or way or school,
	 * the character takes in their occupation(s).
	 *
	 * If the maximum character level is already reached with the sum of the levels,
	 * the new klass level won't apply.
	 *
	 * Other selections like spells or fighting styles needs to be added separately.
	 */
	fun addKlassLevel(klass: Klass, specialisation: String = "") {
		// for each newly learnt klass, check if it's applicable. (multi-classing)

		/* Check, if conditions are met, to gain a level for that class.*/
		val curKlassLevels = klasses.values.sumBy { it.first }

		if (curKlassLevels >= level) {
			log.warn("Cannot add another level, character level is already reached.")
			return
		}

		val old = klasses.getOrDefault(klass, Pair(0, ""))

		val newLevel = old.first + 1
		val newSpecial = if (old.second == "") specialisation else old.second

		klasses += klass to Pair(newLevel, newSpecial)
	}

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
		log.info("Learnt spell ${spell}  (as ${spellSource})")
	}

	/** The number of free hands. */
	val countFreeHands: Int get()
		= hands.toList().filter { it == null }.size

	/** Maximum of the weight, this character could carry.
	 * @see url(https://dnd.wizards.com/products/tabletop/players-basic-rules#lifting-and-carrying) */
	val carryingCapacity: Double get ()
		= abilityScore(Ability.STR) * 15.0

	/** Get the weight, the character is currently carrying. */
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
	 * 10x - 15x STR: SPEED - 20ft, disadvantage on DEX/STR/CON (ability, attack rolls, saving throws)
	 */
	fun carryEncumbranceLevel() : Double
		= (carriedWeight / abilityScore(Ability.STR))

	/** Try to get an environmental item, and maybe store it into the bag, if
	 * no hand is free and no valid storage is listed, the item is not picked up.
	 * If the item is a container and the {intoBag} is a not yet assigned bag and not empty name,
	 * equip the item as new bag.
	 * If it's a bag and no bag is equipped, equip the bag.
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
				log.info("Got a new bag ($item as $intoBag): SUCCESS")
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
					log.info("Got a new bag ($item as $intoBag) inside another bag: SUCCESS")
					return true
				} else {
					return false
				}
			}
			!validBag -> holdItem(item).also {
				log.info("Try to hold item ($item): ${if (it) "SUCCESS" else "FAILED"}")
			}
			else -> storeItem(item = item, bag = intoBag).also {
				log.info("Try to store item ($item): ${if (it) "SUCCESS" else "FAILED"}")
			}
		}
	}

	/** Check if the storage name is valid / available. */
	private fun validStorage(storage: String) : Boolean
		= (storage.startsWith("BAG:") && bags.containsKey(storage))

	/** Hold a new item, if the needed hands are not free, don't hold this item.
	 * @param item the item to be hold
	 * @param preferOff If true, and both hands are free, use the off hand; otherwise default: use main hand.
	 * @param usedHands
	 *   If 0, use the suggested count of hands of the item,
	 *   if 1 enforce to hold the item with one hand,
	 *   if 2 enforce to hold the item with both hands.
	 * @return true on success (the item is now held), else false. */
	fun holdItem(item: Item, preferOff: Boolean = false, usedHands: Int = 0) : Boolean {
		/* Do we need both hands? */
		val bothHands = (false && usedHands == 0) || (usedHands == 2)

		if (bothHands && hands.first == null && hands.second == null) {
			// hold one item with both hands.
			hands = Triple(item, null, true)
			log.info("Hold item ($item) with both hands ($hands)")
			return true
		} else if ((preferOff || hands.first != null) && hands.second == null) {
			// hold item in off hand, if it is free
			// if a two-hand wielded item was just hold, it's now single hand wielded.
			hands = Triple(hands.first, item, false)
			log.info("Hold item ($item) with off hand ($hands)")
			return true
		} else if (hands.first == null) {
			// hold item in main hand, if it is free
			// if a two-hand wielded item was just hold, it's now single hand wielded.
			hands = Triple(item, hands.second, false)
			log.info("Hold item ($item) with main hand ($hands)")
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
			val s0 = bagContainer.countItems // count items inside
			bagContainer.insert(item) // try to store, only reference differentiable objects.
			val success = bagContainer.countItems != s0 // success, if one more.
			return success
		}
		return false
	}

	/** Put on clothes or armor.
	 * If no necessary position is free, don't wear the wearable item.
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
			log.info("Does not wear ${wearable}, already full at $bodyPosition")
			return false
		} else {
			worn += bodyPosition.name to wearable
			log.info("Wears now ${wearable}")
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

		log.debug("Drop items: main hand: ${handFirst}, off hand: ${handSecond}")

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

		log.info("Dropped ${dropped}")

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
		log.info("Drop fron ${bagKeys.associateWith { validStorage(it) } }")
		bagKeys.forEach { key ->
			if (validStorage(key)) {
				// bag exists.
				val d0 = bags[key]!!.removeAll(predicate)
				log.info("From $key, dropped ${d0.size} items ${d0}")
				dropped += d0
			}
		}
		log.info("Totally dropped ${dropped.size} items:  ${dropped}")
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

		log.info("Dropped ${dropped}")
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

		// TODO (2020-08-13) unarmored defense
		// - races and classes benefiting from unarmed defense / natural armor ...
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

	/** Companion object with basic translation methods. */
	companion object {

		private val nextLevel = listOf(
			355000 /*20*/, 305000 /*19*/, 265000 /*18*/, 225000 /*17*/
			, 195000 /*16*/, 165000 /*15*/, 140000 /*14*/, 120000 /*13*/
			, 100000 /*12*/, 85000 /*11*/, 64000 /*10*/, 48000 /*9*/
			, 34000 /*8*/, 23000 /*7*/, 14000 /*6*/, 6500  /*5*/
			, 2700 /*4*/, 900 /*3*/, 300 /*2*/)

		/** A simple mapping from experience points to level.
		 * @param xp experience points to translate.
		 * @return level as int, from 1 to 20.
		 */
		fun xpToLevel(xp: Int) : Int {
			val maxLevel = nextLevel.size // minus one, since we don't need 1

			for (lvlPre in (0 until maxLevel)) {
				if (xp >= nextLevel[lvlPre]) {
					return nextLevel.size - lvlPre + 1
				}
			}
			return 1
		}

		/** Get the experience points which need to be reached to get the level. */
		fun levelToXP(lvl: Int) : Int {
			return nextLevel.getOrElse(20 - lvl, { 0 })
		}

		/** A simple mapping from level to proficiency bonus.
		 * @param level to translate.
		 * @return proficiency bonus, which is reached on a certain level. */
		fun levelToProficiencyBonus(lvl: Int) = when {
			lvl >= 17 -> +6
			lvl >= 13 -> +5
			lvl >= 9 -> +4
			lvl >= 5 -> +3
			else -> +2
		}
	}
}

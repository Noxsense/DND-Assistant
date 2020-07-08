package de.nox.dndassistant

import kotlin.math.floor;

private fun getModifier(value : Int) = floor((value - 10) / 2.0).toInt()

private val logger = LoggerFactory.getLogger("PlayerCharacter")

data class PlayerCharacter(
	val name : String,
	val race: String = "Human" /*TODO*/,
	val gender: String = "divers",
	val player: String = ""
	) {

	var expiriencePoints : Int
		= 0

	var abilityScore : Map<Ability, Int>
		= enumValues<Ability>().map { it }.associateWith { 10 }
		private set

	var abilityModifier : Map<Ability, Int>
		= abilityScore.mapValues { getModifier(it.value) }
		private set

	var savingThrows : List<Ability>
		= listOf()
		private set

	var proficientSkills : Map<Skill, Proficiency>
		= mapOf()
		private set

	var proficientTools : Map<String, Proficiency>
		= mapOf()
		private set

	var proficiencies : Map<Skillable, Proficiency>
		= mapOf()
		private set

	var proficientValue : Int
		= 2

	fun rollAbilityScores() {
		abilityScore = abilityScore.mapValues { D20.roll() }
		setAbilityModifiers()
	}

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
	fun setAbilityScore(a : Ability, v: Int) {
		val d20 : SimpleDice = SimpleDice(20, 1);

		abilityScore += Pair(a, if (v > 0) v else d20.roll())

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

	/* Get a random roll (D20) for a requesting ability, adding its bonus.*/
	fun rollCheck(a: Ability) : Int = D20.roll() + abilityModifier(a)

	/* Get a random roll (D20) for a requesting skill, adding its bonus.*/
	fun rollCheck(s: Skill) : Int = D20.roll() + skillScore(s)

	/* Get a random roll (D20) for a requesting saving throw, adding its bonus.*/
	fun rollSave(a: Ability) : Int = D20.roll() + savingScore(a)

	fun getProficiencyFor(skill: Skill) : Proficiency
		= proficientSkills.getOrDefault(skill, Proficiency.NONE)

	/* Add proficiency to saving trhow.*/
	fun addProficiency(saving : Ability) {
		if (!(saving in savingThrows)) savingThrows += saving
	}

	/* Add proficiency to a skill. If twice, add expertise.*/
	fun addProficiency(skill : Skill) {
		val level = if (skill in proficientSkills.keys) {
				Proficiency.EXPERT
			} else {
				Proficiency.PROFICIENT
			}

		proficientSkills += Pair(skill, level)
	}

	val initiative : Int get()
		= this.abilityModifier.getOrDefault(Ability.DEX,
			getModifier(this.abilityScore.getOrDefault(Ability.DEX, 0)))

	fun initiativeRoll() : Int = initiative + D20.roll()

	val maxHitPoints : Int get()
		= -1 // TODO(2020-06-26)

	val curHitPoints : Int get()
		= -1 // TODO(2020-06-26)

	val tmpHitPoints : Int get()
		= -1 // TODO(2020-06-26)

	val hasTmpHitpoints : Boolean get()
		= tmpHitPoints > 0 && tmpHitPoints != maxHitPoints

	var deathSaves : Array<Int> = arrayOf(0, 0, 0, 0, 0) // maximal 5 chances.
		private set

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
	}

	var knownLanguages : List<String>
		= listOf("Common")

	var purse : Money = Money()

	var inventory :  List<Item>
		= listOf()
		private set

	/*
	 * Lifting and Carrying (https://dnd.wizards.com/products/tabletop/players-basic-rules#lifting-and-carrying)
	 */

	/* Maximum of the weight, this character could carry. */
	fun carryingCapacity() : Double
		= abilityScore(Ability.STR) * 15.0

	/* Weight of the inventory and the purse.*/
	fun inventoryWeight() : Double
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
		= inventoryWeight() / abilityScore(Ability.STR)

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

	val armorClass : Int get() {
		// TODO (2020-06-26)
		// Look up, what the PC's wearing. Maybe add DEX modifier.
		// Not wearing anything: AC 10 + DEX
		return 10 + abilityModifier.getOrDefault(Ability.DEX, 0)
	}

}

/* The base ability score.*/
enum class Ability(val fullname : String) {
	STR("STRENGTH"),
	DEX("DEXTERITY"),
	CON("CONSTITUTION"),
	INT("INTELLIGENCE"),
	WIS("WISDOM"),
	CHA("CHARISMA")
}

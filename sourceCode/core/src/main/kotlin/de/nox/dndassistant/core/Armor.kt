package de.nox.dndassistant.core

/** Armor <- Skillable
 * One can be proficient with armor: Know-How to wear.
 * If worn and not proficient: Disadvantage (DEX, STR)
 * on any ability, saving throw, attack; cannot cast spells
 */
data class Armor(
	/* Inherit from item.*/
	override val name: String, // inherit from Item
	override val weight: Double, // inherit from item
	override val cost: Money, // inherit from item

	val weightClass: Weight,

	val armorClass: Int,
	val strength: Int = 0, // bad for DEX, reduced movement, if STR 13 or 15 (mentioned) not hit.
	val stealhy: Boolean = true, // false => disadvantage on stealth.

	override val position: BodyType = BodyType.ARMOR,
	val description : String = ""
) : Wearable, Skillable {

	private val Logger = LoggerFactory.getLogger("Armor")

	enum class Weight {
		LIGHT, // for agile adventures: DEX plus armor's AC
		MEDIUM, // bad for movement, DEX (max +2) plus armor's AC
		HEAVY, // worst for movement, no DEX modifier
		SHIELD; // extra class.
	}

	override fun toString() : String = name
}

// default items to be skilled with.
val ARMOR_LIGHT = Armor("LIGHT ARMOR",  0.0, Money(), Armor.Weight.LIGHT, 0)
val ARMOR_MEDIUM = Armor("MEDIUM ARMOR", 0.0, Money(), Armor.Weight.MEDIUM, 0)
val ARMOR_HEAVY = Armor("HEAVY ARMOR",  0.0, Money(), Armor.Weight.HEAVY, 0)
val ARMOR_SHIELD = Armor("SHIELD", 0.0, Money(), Armor.Weight.SHIELD, 0)

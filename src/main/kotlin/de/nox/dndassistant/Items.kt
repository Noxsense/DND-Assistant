package de.nox.dndassistant

/* TODO (2020-07-07) https://roll20.net/compendium/dnd5e/Rules:Objects#content
 * - Armor Class of an Item
 * - Hit Points of an Item
 */
interface Item {
	val name: String
	val weight: Double
	val cost: Money
}

enum class WeightClass {
	NONE,
	LIGHT,
	HEAVY
}

enum class Size {
	TINY,
	SMALL,
	MEDIUM,
	LARGE,
	HUGE,
	GIANT; // GARGANTUM
}

///////////////////////////////////////////////////////////////////////////////

data class Container(
	override val name: String,
	override val weight: Double,
	override val cost: Money,
	val maxWeight: Double = 0.0,
	val maxItems: Int = 0,
	val capacity: String
) : Item {
	var inside : List<Item> = listOf()
		private set

	val insideGrouped: Map<String, List<Item>> get()
		= inside.groupBy { it.name }

	val size : Int get()
		= inside.size

	fun isEmpty() : Boolean
		= inside.isEmpty()

	/** Check, if this container is full. */
	fun isFull() : Boolean = when {
		maxWeight <= 0 && maxItems <= 0 -> false // unlimited.
		maxWeight <= 0 -> maxItems <= inside.size // only item count => if more items than allowed.
		maxItems <= 0 -> maxWeight <= sumWeight() // only weight => if max weight is reached or overstepped.
		else -> maxItems <= inside.size || maxWeight <= sumWeight() // or at least one.
	}

	/** The value of the items, maybe with the bag's value included.
	 * @return a double, which represents the weight in lb. */
	fun sumValue(thisInclusive: Boolean = false) : Money
		= ((if (thisInclusive) cost else Money())
		+ inside.fold(Money(), { acc, i -> i.cost + acc }))

	/** The weight of the items, maybe add weight of the bag.
	 * @return a double, which represents the weight in lb. */
	fun sumWeight(thisInclusive: Boolean = false) : Double
		= ((if (thisInclusive) weight else 0.0 )
		+ inside.sumByDouble { if (it is Container) it.sumWeight(true) else it.weight })

	/** Check if this container contains a specific item, by reference. */
	operator fun contains(item: Item) : Boolean
		= inside.filter { it === item }.size > 0

	/** Check if this item contains an item with the matching name. */
	operator fun contains(itemName: String) : Boolean
		= inside.filter { it.name == itemName }.size > 0

	/** Check if the Container contains all requested items, by references. */
	fun containsAll(items: Collection<Item>): Boolean
		= items.all { contains(it) }

	/** Check if the Container contains all requested items names. */
	fun containsAllNames(itemNames: Collection<String>) : Boolean
		= itemNames.all { contains(it) }

	/** Add a new item to the bag. */
	fun insert(item: Item) {
		if (item !in this) inside += item
	}

	fun filter(filter: (item: Item) -> Boolean) : List<Item>
		= inside.filter(filter)

	fun indexOf(item: Item) : Int
		= inside.withIndex().filter { it === item }.run {
			if (isEmpty()) -1 else get(0).index
		}

	fun insideOf(itemName: String) : Int
		= inside.map { it.name }.indexOf(itemName)

	fun indexOfGroupedItem(groupName: String, indexInGroup: Int) : Int {
		return inside.withIndex()
			.filter { it.value.name == groupName }
			.get(indexInGroup).index
	}

	fun clear() : List<Item> {
		val dropped = inside
		inside = listOf()
		return dropped
	}

	/** Remove all items, that matches the filter.
	 * @return dropped items. */
	fun removeAll(predicate: (index: Int, item: Item) -> Boolean): List<Item> {
		val partition = inside.withIndex().partition { predicate(it.index, it.value) }
		inside = partition.second.map { it.value }
		return partition.first.map { it.value }
	}

	/** Retain all items, that matches the filter.
	 * @return dropped items. */
	fun retainAll(filter: (index: Int, item: Item) -> Boolean): List<Item>
		= removeAll { i, x -> !filter(i, x) } // drop non matching items.

	/** Remove an item from the bag.
	 * @param item item to remove. */
	fun remove(item: Item) : Item? {
		return removeAll { _, it -> it === item }[0]
	}

	/** Remove the first item, that matches the name. */
	fun remove(itemName: String) : Item? {
		val item : Item?
			= inside.filter { it.name == itemName }.getOrElse(0, { null })

		if (item != null) {
			remove(item)
		}

		return item
	}

	/** Remove an item from the bag.
	 * @param index index of the item, which will be removed. */
	fun remove(index: Int) : Item? {
		if (index in (0 until inside.size)) {
			val item = inside[index]

			// remove all with matching index.
			inside = removeAll { i, _ -> i == index }

			return item

		} else {
			return null
		}
	}

	override fun equals(other: Any?) : Boolean
		= other != null && other is Container && name == other.name

	override fun toString() : String
		= name
}

data class LooseItem(
	override val name: String,
	override val weight: Double, // per unit. // may be calculated
	override val cost: Money,
	val validContainer: List<Container>,
	val count: Double, // 1 (piece), 0.5 (liter), 15.5 (gramm)
	val measure: LooseItem.Measure, // eg. ounce(s)
	val filledDescription: String // e.g "Vial (4 ounces): 1lb"
) : Item {

	enum class Measure { PIECE, GRAMM, LITER; };

	override fun toString() : String
		= "${name}, ${count} ${measure}"

}

///////////////////////////////////////////////////////////////////////////////

/** Weapon <- Skillable.
 * "Your class grants proficiency in certain Weapons, reflecting both the
 * class’s focus and the tools you are most likely to use. Whether you favor a
 * Longsword or a Longbow, your weapon and your ability to wield it effectively
 * can mean the difference between life and death while Adventuring.
 *
 * The Weapons table shows the most Common Weapons used in the fantasy gaming
 * worlds, their price and weight, the damage they deal when they hit, and any
 * special properties they possess. Every weapon is classified as either melee or
 * ranged. A melee weapon is used to Attack a target within 5 feet of you,
 * whereas a ranged weapon is used to Attack a target at a distance."
 */
data class Weapon(
	/* Inherit from item.*/
	override val name: String, // inherit from Item
	override val weight: Double, // inherit from item
	override val cost: Money, // inherit from item

	/* LIGHT weapons can be dual-wielded.
	 * SMALL creatures have disadvantage with HEAVY weapons. */
	val weightClass: WeightClass = WeightClass.NONE,

	/* Weapon specific attributes.*/

	val weaponType: Type,
	val range: Range = Range(5), // range in feet. => disadvantage out of range.

	/* The damage, which is dealt, on hit. */
	val damageType: Set<DamageType>,

	val damage: DiceTerm, // damage on hit and type.

	/* A thrown melee weapon without thrown property deals 1D4
	 * (like ranged weapon on melee attack, out of range). */
	val throwable : Boolean = false,
	val thrownRange: Range = Range(20,60),
	val thrown: DiceTerm = DiceTerm(D4),

	/* Can also be used two-handed => has a second dice term. */
	val versatile: DiceTerm? = null,

	/* needs two hand to wield, no off-hand possible. */
	val isTwoHanded: Boolean = false,

	/* Ranged attack with AMMUNITION, only if available, expended on use.
	 * Draw from quiver needs free other-hand.
	 * Recover ammunition up to half (on battlefield). */
	val ammunition: Array<String> = arrayOf(),

	/* can use dexterity modifier => use highest modifier (on default),
	 * otherwise, use STR for melee and DEX for ranged. */
	val isFinesse: Boolean = false,

	// TODO (2020-07-08) silvered

	val note: String  // notes
) : Item, Skillable {

	private val logger = LoggerFactory.getLogger("Weapon")

	override fun toString() : String = name

	/* A range contains two numbers: Normal and long.
	 * Beyond normal range: Disadvantage.
	 * Beyond long range: Out-of-range.
	 * (Long can also be normal.) */
	data class Range(val normal_: Int, val long_: Int = 0) : Comparable<Range> {

		val normal = normal_
		val long = if (long_ < normal) normal else long_

		/* True, if x is in range of the range aka. not out-of-range.*/
		operator fun contains(x: Int) : Boolean
			= x <= long

		fun hasDisadventage(x: Int) : Boolean
			= normal != long // no disadvantage, on any range.
			|| normal < x // disadvantage, beyond normal range.

		/* Compare by length in-between. */
		override fun compareTo(other: Range) : Int = normal - other.normal
	}

	/* Basic: Weapon Types: Simple or Martial, Melee or Ranged.
	 * Also the "other" option is available.*/
	enum class Type(val simple: Boolean, val melee: Boolean) : Skillable {
		SIMPLE_MELEE(true, true),
		SIMPLE_RANGED(true, false),
		MARTIAL_MELEE(false, true),
		MARTIAL_RANGED(false, false),
		OTHER(false, false)
	}

	/* TWO HANDED ATTACK.
	 * Versatile Weapon as two-handed: Use versatile Damage Dice.
	 * One-Handed Weapon as two-handed (wo/ versatility): No change.
	 */
	val damageUsingTwoHanded : DiceTerm
		= versatile ?: damage
		// = if (versatile != null) versatile?? else damage
	// can be used two handed.

	/* One HANDED ATTACK.
	 * Two-Handed weapon use as one-handed: see suggestions.
	 * [last update: 2020-06-13](https://rpg.stackexchange.com/a/128940)
	 * - Less control (Disadvantage).
	 * - reduced power, like versatile weapons.
	 * - perquisites to wield the weapon at all (like STR > 15)
	 * - lose/alter proficiency
	 */
	val damageUsingOneHanded : DiceTerm = when {
		isTwoHanded -> damage // with disadvantage or lesser dice term.
		else -> damage // can have disadvantage.
	}

	private val improvised = DiceTerm(D4)

	/* RANGED ATTACK
	 * Additionally (like a two-handed ranged weapon: any bow)
	 * Ranged weapon as melee : "Improvised" 1D4 ...
	 * Melee Weapon thrown (20/60): "Improvised" 1d4
	 * Ranged weapon in long-range: Disadvantage
	 */
	val damageUsingMelee : DiceTerm = when {
		weaponType != Type.OTHER && !weaponType.melee -> improvised
		else -> damage
	}

	val damageUsingRanged : DiceTerm = when {
		weaponType != Type.OTHER && weaponType.melee -> improvised
		else -> damage // can have disadvantage.
	}
}

///////////////////////////////////////////////////////////////////////////////

/** Something a player can wear, like shoes, acccessoires or also armor. */
interface Wearable : Item {
	override val name: String
	override val weight: Double
	override val cost: Money
	val position: BodyType
}

/** A basic class for wearable stuff, capes and neclaces included. */
data class Clothes(
	/* Inherit from item.*/
	override val name: String, // inherit from Item
	override val weight: Double, // inherit from item
	override val cost: Money, // inherit from item

	override val position: BodyType = BodyType.BODY,
	val description : String = ""
) : Wearable {

	override fun toString(): String
		= name

}

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

///////////////////////////////////////////////////////////////////////////////

/** Consumables.
 * Food to eat. Potions, to swallow or other body contact. Scrolls to read.
 */
data class Consumables(
	override val name: String,
	override val weight: Double,
	override val cost: Money
) : Item {

	override fun toString() : String = name
}

///////////////////////////////////////////////////////////////////////////////

/** Tool <- Skillable.
 * "A tool helps you to do something you couldn't otherwise do, such as craft
 * or repair an item, forge a document, or pick a lock. Your race, class,
 * Background, or feats give you proficiency with certain tools. Proficiency with
 * a tool allows you to add your Proficiency Bonus to any ability check you make
 * using that tool. Tool use is not tied to a single ability, since proficiency
 * with a tool represents broader knowledge of its use. For example, the GM might
 * ask you to make a Dexterity check to carve a fine detail with your woodcarver's
 * tools, or a Strength check to make something out of particularly hard wood."
 */
data class Tool(
	override val name: String,
	override val weight: Double,
	override val cost: Money,

	val type: Type

) : Item, Skillable {

	enum class Type {
		ARTISIAN_TOOL,
		GAMING_SET,
		MUSICAL_INSTRUMENT,
		VEHICLE
	}

	override fun toString() : String = name
}

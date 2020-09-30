package de.nox.dndassistant.core

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

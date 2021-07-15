package de.nox.dndassistant.core


// TODO (2021-03-12) effects of the itmes: +3 weapon or weapon at all... with additional attacks


/** SimpleItem.
 * Each item has a weight (@see #weight) and a price value (@see #copperValue), and a category (@see #category).
 * Additionally partical items like a bag of sand an be divided (@see #devidable).
 * A SimpleItem also holds an identifier (@see #identifier), to specify a certain weapon or item is used, dropped or etc, and not all of their kind.
 */
public data class SimpleItem(
	val name: String,
	val identifier: String,
	val category: String,
	val weight: Double,
	val copperValue: Int,
	val dividable: Boolean = false
) {
	companion object {
		val CP_TO_CP : Int = 1; // copper piece to copper piece
		val SP_TO_CP : Int = 10; // silver piece to copper piece
		val GP_TO_CP : Int = 50; // gold piece to copper piece
		val EP_TO_CP : Int = 100; // electrum piece to copper piece
		val PP_TO_CP : Int = 1000; // platinum piece to copper piece

		// Initiator for Coins / Currency Pieces.
		// private fun Coin(t: String) : SimpleItem = SimpleItem("$t Coin", "Currency", 0.02, SimpleItem.CP_TO_CP * when (t[0]) { 'C' -> 1; 'S' -> 10; 'G' -> 50; 'E' -> 100;'P' -> 1000; else -> 0)
		//

		var Catalog: Map<String, PreSimpleItem> = mapOf()

		/** All known Identifier, given by newItem. */
		private var identifiers: MutableSet<String> = mutableSetOf()

		/** Try to get a new instance for the item, with an optional identifier. */
		public fun newItem(name: String, identifier: String? = null) : SimpleItem?
			= Catalog.get(name)?.let { pre ->

				var x = identifiers.size
				var uniqueID = identifier ?: "${name}@${x + 1}" // custom ID or counted by known identifiers.

				// get an unique ID.
				// TODO (2021-04-03) better unique ids.
				while (uniqueID in identifiers) {
					x += 1
					uniqueID = "${name}@${x + 1}" // increase the appendix number
				}

				identifiers.plusAssign(uniqueID) // store new ID.

				SimpleItem(name, uniqueID, pre.category, pre.weight, pre.copperValue, pre.dividable)
			}
	}

	override fun equals(other: Any?)
		= other != null && other is SimpleItem && other.name == this.name && other.category == this.category

	override fun toString() = "$name [$identifier]"

	public fun sameByName(other: SimpleItem) : Boolean
		= this.name == other.name

	public fun sameByNameAndIdentifier(other: SimpleItem) : Boolean
		= this.sameByName(other) && this.identifier == other.identifier
}

/**
 * A Pre-Item, which will be used to look up an item and therefore holds many needed attributes, to identify the final one.
 * e.g. without id.
 * */
data class PreSimpleItem(val category: String, val weight: Double, val copperValue: Int, val dividable: Boolean);


// val cc = SimpleItem(  "Copper Coin", category: "Currency", 0.02, 1)
// val cs = SimpleItem(  "Silver Coin", category: "Currency", 0.02, 10)
// val cg = SimpleItem(    "Gold Coin", category: "Currency", 0.02, 50)
// val ce = SimpleItem("Electrum Coin", category: "Currency", 0.02, 100)
// val cp = SimpleItem("Platinum Coin", category: "Currency", 0.02, 1000)


public fun Collection<Pair<SimpleItem, String>>.getStorageTree() : Map<String, List<String>> {
	return mapOf()
}

/** Get the Weight (Double, Pounds) of the collection of items.
 * If the optional storage is null, get the overall carried weight.
 */
public fun Collection<Pair<SimpleItem, String>>.weight(storage: String? = null) : Double
	= this.fold(0.0) { b, i -> b + (if (storage == null || storage == i.second) i.first.weight else 0.0) }

/** Get the Value (Int, in Coppers) of the collection of items.
 * If the optional storage is null, get the overall carried value.
 */
public fun Collection<Pair<SimpleItem, String>>.copperValue(storage: String? = null) : Int
	= this.fold(0) { b, i -> b + (if (storage == null || storage == i.second) i.first.copperValue else 0) }

/**
 * Check if an item collection contains a certain item (or of its kind) give by its name.
 */
public operator fun Collection<Pair<SimpleItem, String>>.contains(itemName: String) : Boolean
	= this.getItemByName(itemName) != null

/**
 * Check if an item collection contains a certain item.
 */
public operator fun Collection<Pair<SimpleItem, String>>.contains(item: SimpleItem) : Boolean
	= this.getItemByID(item.identifier) != null

/**
 * Check if an item collection contains a certain item in the given storage.
 */
public operator fun Collection<Pair<SimpleItem, String>>.contains(itemStorage: Pair<SimpleItem, String>) : Boolean
	= this.getItemByName(itemStorage.first.identifier)?.second == itemStorage.second

/** Find the item by name.
 * @return Item and its storage.
 */
public fun Collection<Pair<SimpleItem, String>>.getItemByName(name: String) : Pair<SimpleItem, String>?
	= this.find { (i, _) -> i.name == name }

/** Find the item by name.
 * @return Item and its storage.
 */
public fun Collection<Pair<SimpleItem, String>>.getItemByID(identifier: String) : Pair<SimpleItem, String>?
	= this.find { (i, _) -> i.identifier == identifier }

/**
 * Check if an item is a storage inside the given collection.
 */
public fun Collection<Pair<SimpleItem, String>>.isStoring(item: SimpleItem) : Boolean
	= this.isStoringItemsByID(item.identifier)

/**
 * Check if an item has other items containing / attached on (aka serves as bag).
 */
public fun Collection<Pair<SimpleItem, String>>.isStoringItemsByID(identifier: String) : Boolean
	= this.find { (_, storage) -> storage == identifier } != null // is a storage.

/**
 * Get all items, that are stored in the given bag, also return the bag object itself.
 */
public fun Collection<Pair<SimpleItem, String>>.getBag(bagIdentifier: String) : Triple<SimpleItem, String, List<SimpleItem>>?
	= this.getItemByID(bagIdentifier)?.let { (bag, bagsStorage) ->
		val stored = this.filter { (_, storage) -> storage == bagIdentifier }.map { (i, _) -> i }
		Triple(bag, bagsStorage, stored)
	}

/** Store or change the current storage of an item to another storage.
 * If the storage (reference) is null, the item is worn or directly held by the hero.
 * @param storage item referenence the new item is put i or attached to.
 * @param force if true, ignore that the storage is not available.
 * @return true, if the item was successfully stored.
 */
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
 * @return dropped items and intended storages.
 */
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
	// TODO (2021-03-10) (dropItem) what if the item is stored multiple times, but also in different bags (like a dagger in hand and in backpack)

	/* Find all items that matches. */
	val matches = this.filter {
		(i, s) -> i == item && (storage == null || s == storage)
	}

	this.minusAssign(matches)

	return matches
}

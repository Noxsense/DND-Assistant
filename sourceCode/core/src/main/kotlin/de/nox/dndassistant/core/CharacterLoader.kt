package de.nox.dndassistant.core

import java.io.File
import java.io.BufferedReader

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener

private val log = LoggerFactory.getLogger("Hero-Loader")

// XXX (2020-10-26) implement character saver.
/** Save a player character to source. */
public fun saveCharater(hero: Hero, filepath: String) {
	var newObj = JSONObject()
	newObj.put("name", hero.name)
	newObj.put("num", hero.level)
	newObj.put("double", 3.14129)
	newObj.put("bool", true)

	log.debug(newObj)

	log.debug("Save to $filepath")
}

/** Load the Hero from a given resource. */
public fun loadHero(filepath: String) : Hero {
	val br: BufferedReader = File(filepath).bufferedReader()
	val txt = br.use { it.readText() }

	try {
		val tokener = JSONTokener(txt)

		var obj: JSONObject = tokener.nextValue() as JSONObject

		/* Name of the Hero. */
		val name = obj.getString("name") // String!

		/* (optional) Player of the Hero. */
		val player = obj.optStringNull("player") // String? (null if not adopted)

		/* Race => {race-name, subrace-name} */
		val race = obj.getJSONObject("race").let { race ->
			race.getString("race-name") to race.getString("subrace-name")
		}

		return Hero(player, name, race).apply {
			/* Level. */
			this.level = obj.getInt("level") // Int!
			log.info("Set level: ${obj.getInt("level")} => $level")

			/* Insipiration: Number of collected inspiration points. */
			this.inspiration = obj.getInt("inspiration")

			/* Expirience points. */
			obj.getJSONObject("experience").also { xp ->
				experience = Hero.Experience(points = xp.getInt("num"), method = xp.getString("type"))
			}

			/* Hitpoints => object {max, current, temporary} */
			obj.getJSONObject("hitpoints").also { hp ->
				hitpointsMax = hp.getInt("max")
				hitpointsNow = hp.getInt("current")
				hitpointsTmp = hp.getInt("temporary-offset")
			}

			/* Abilities. */
			obj.getJSONObject("attributes").also { abs ->
				val saves = obj.getJSONArray("save-proficiencies")
					.map<String, String> { "$it" }

				this.abilities[Ability.STR] = abs.getInt("STR") to ("STR" in saves)
				this.abilities[Ability.DEX] = abs.getInt("DEX") to ("DEX" in saves)
				this.abilities[Ability.CON] = abs.getInt("CON") to ("CON" in saves)
				this.abilities[Ability.INT] = abs.getInt("INT") to ("INT" in saves)
				this.abilities[Ability.WIS] = abs.getInt("WIS") to ("WIS" in saves)
				this.abilities[Ability.CHA] = abs.getInt("CHA") to ("CHA" in saves)
			}

			/* Occupations. */
			// klasses => [Klass = {class, sublclass, lvl} ]?
			obj.getJSONArray("classes").let { cls ->
				cls.forEach<JSONObject> { c ->
					this.updateKlass(
						klass = c.getString("class-name"),
						level = c.getInt("class-level"),
						subklass = c.optStringNull("subclass-name"))
				}
			}

			/* Skills => list [list [name, is-expert, source]].
			 * Fill proficiencies. */
			obj.getJSONArray("profiencies").let { array ->
				array.forEach<JSONObject> { jsonSkill ->
					this.setProficiencies(
						skill = jsonSkill.getString("skill-name"),
						reason = jsonSkill.getString("skill-source"),
						asExpert = jsonSkill.optBoolean("is-expert", false))
				}
			}

			/* Languages => list [string]. */
			this.languages = obj.getJSONArray("languages").let { langs ->
				(0 until langs.length()).map<Int, String> { i -> langs.getString(i) }
			}

			/* Armor-class:  armor class as points, and what is worn for that (shield, weapons, armor). */
			obj.getJSONObject("armor").let { armor ->
				// XXX (2021-03-05)
				armor.optInt("armor-class", 10 + this.abilityModifier(Ability.DEX))
				armorSources = armor.getJSONArray("armor-sources").map<String,String> { it }
			}

			/* Speed of the Hero.
			 * speed => list of pairs [ why, speed in feet] , mention only special walking styles.
			 * If empty, or not available use default of 30. */
			obj.getJSONArray("speed").let { speeds ->

				/* Anonymous function to fetch speed (object) at index. */
				val getSpeed = { i: Int -> speeds.getJSONObject(i).let { o ->
					o.getString("movement-name") to o.getInt("speed-in-feet")
				}}

				/* Feed hero's speed with speed array. */
				(0 until speeds.length())
					.map { getSpeed(it) }
					.toMap(this.speed)
			}

			// available-hitdice => list pairs: [die, count] // not spent; still spendable.
			obj.getJSONObject("available-hitdice").let { hds ->
				hds.keys().forEach { die ->
					log.debug("Hitdice: Keys: ${die}")
					hitdice["$die"] = hds.getInt("$die")
				}
			}

			// traits
			// XXX (2021-03-07) traits and featss and features and custom counters?

			// race features
			// class traits
			// optional feats
			// item features
			// custom feats / notes / abilities (like memory walking or just a custom counter)
			// >  spell level are feat depended counters? or if gained by a feat, by that feat.
			//
			this.specialities = obj.getJSONArray("traits").map<JSONObject, Speciality> { o ->
				/* Load the counts of the traits and features. */
				val count: Speciality.Count? = o.optJSONObject("count")?.let { l ->
					val max = l.optInt("max", 0) // maximal invocations
					val lts = l.optJSONArray("loaded-items")?.toList<String>() // e.g spells of a magic ring.
					val now = l.optInt("current", lts?.size ?: max) // currently left invocations

					Speciality.Count(l.optString("recharge"), max, now, lts)
				}
				/* Parse the Features or Traits. */
				// TODO description fetched from thez source of the trait or feat or Speciality. => linked from source not by holder of the feat/ trait (except custom)
				when {
					o.has("race-feature-name") -> {
						// register own Race+Subrace as RaceFeature's race
						RaceFeature(o.getString("race-feature-name"), race, 1, count)
					}
					o.has("feat-name") -> {
						// optional feats with optional flavors
						Feat(o.getString("feat-name"), count)
					}
					o.has("klass-trait-name") -> {
						KlassTrait(o.getString("klass-trait-name"), Triple(o.optString("trait-source"), "", 1), count, "About")
					}
					o.has("item-feature-name") -> {
						ItemFeature(o.getString("item-feature-name"), count)
					}
					o.has("custom-name") -> {
						CustomCount(o.getString("custom-name"), count)
					}
					else -> Feat("-- Unparsable Feat? --", count)
				}
			}

			// spells => object with two lists
			// XXX (2021-03-05) spells
			// list? of spells prepared, available at all.
			obj.optJSONObject("spells")?.let { metaSpells ->
				// XXX (2021-03-05) loteinit or look up in a library
				val prepared = metaSpells.getJSONArray("prepared").map<String,String> { it }.forEach {
					log.info("- Prepare Spell: $it")
				}
				val available = metaSpells.getJSONArray("available").map<JSONObject, Any?> {
					it.also { log.info("Available Spell: '$it'") }
				}
			}

			// XXX DELETE ME: Item Catalog => repository of item, identifier := name
			val itemCatalog: Map<String, Triple<String, Double, Int>> = mapOf (
				"Copper Coin"   to Triple("Currency", 0.02, 1),
				"Silver Coin"   to Triple("Currency", 0.02, SimpleItem.SC_TO_CC),
				"Gold Coin"     to Triple("Currency", 0.02, SimpleItem.GC_TO_CC),
				"Electrum Coin" to Triple("Currency", 0.02, SimpleItem.EC_TO_CC),
				"Platinum Coin" to Triple("Currency", 0.02, SimpleItem.PC_TO_CC),

				"Mage Armor Vest" to Triple("Clothing",  3.0, SimpleItem.SC_TO_CC * 5),

				"Ember Collar" to Triple("Artefact",  0.0, 0),
				"Ring of Spell Storing" to Triple("Ring",  0.0, SimpleItem.GC_TO_CC * 20000),
				"Focus (pet collar)" to Triple("Arcane Focus",  1.0, 0),
				"Potion of Greater Healing" to Triple("Potion",  0.0, 0),
				"Sword of Answering" to Triple("Weapon",  0.0, 0),

				"Pouch"  to Triple("Adventuring Gear", 1.0, 0), // can hold 0.2 ft^3 or 6 lb

				"Ball"   to Triple("Miscelleanous", 0.01, 1),

				"Flask"  to Triple("Container", 1.0, 2), // can hold 1 pint of liquid (0.00056826125 m^3 = 568.26125 ml)
				"Oil"    to Triple("Miscelleanous", 1.0, SimpleItem.SC_TO_CC), // 1lb oil is worth 1sp
			)

			/* Pack the inventory. */
			obj.getJSONArray("inventory").forEach<JSONObject> { preItem ->

				val itemname = preItem.getString("item-name")

				/* Get Identifier for one special item or
				 * the amount of items for "newly created" copies."
				 * If a the item is dividable and a weight is given, use the new weight.
				 */
				val identifier = preItem.optStringNull("identifier")
				val amount = if (identifier != null) 1 else preItem.optInt("amount", 1)
				val optWeight = preItem.opt("weight-lb") as? Double

				/* Get x copies from the item catalog. */
				val itemProp = itemCatalog.get(itemname)

				/* Reference to another item to "combine into" */
				val storage = preItem.optString("storage", "")

				if (itemProp != null && amount > 0) {
					for (i in 0 until amount) {
						// force for late storage resolving / linking
						val item = itemProp.let { (category, weight, price) ->
							SimpleItem(
								name = itemname,
								identifier = identifier ?: "$itemname@${inventory.size}",
								category = category,
								weight = if (optWeight != null && true) optWeight else weight,
								copperValue = price
							)
						}
						this.inventory.putItem(item = item, storage = storage, force = true)
					}
				}
			}
			this.inventory.dropIncorrectlyStoredItems() // drop items whose storages couldn't be resolved
		}
	} catch (e: JSONException) {
		throw JSONException("Cannot parse the Hero", e)
	}

}

@Suppress("UNCHECKED_CAST")
private fun <T> JSONArray.toList() : List<T>
	= (0 until length()).map { get(it) as T }

private fun <T, U> JSONArray.map(transform: ((T) -> U)) : List<U>
	= this.toList<T>().map<T,U> { transform.invoke(it) }

@Suppress("UNCHECKED_CAST")
private fun <T> JSONArray.forEach(transform: ((T) -> Unit))
	= (0 until this.length()).forEach { transform.invoke(this.get(it) as T) }

/** Alternative optString() which returns null instead of an empty string. */
private fun JSONObject.optStringNull(name: String) : String?
	= try { this.getString(name) } catch (e: JSONException) { null }

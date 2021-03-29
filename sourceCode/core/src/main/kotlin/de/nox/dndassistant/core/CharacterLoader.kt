package de.nox.dndassistant.core

import java.io.File
import java.io.BufferedReader

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener

private val log = LoggerFactory.getLogger("Hero-Loader")

/** Load Hero from JSON string.
 * @throws JSONException
 * /throws Semantic Errrors - like invalid values for read-in fields?
 */
public fun Hero.Companion.fromJSON(jsonStr: String) : Hero {
	val tokener = JSONTokener(jsonStr)

	var obj: JSONObject = tokener.nextValue() as JSONObject

	/* Name of the Hero. */
	val name = obj.getString("name") // String!

	/* (optional) Player of the Hero. */
	val player = obj.optStringNull("player") // String? (null if not adopted)

	/* Race => {race-name, subrace-name} */
	val race = obj.getJSONObject("race").let { race ->
		race.getString("race-name") to race.getString("subrace-name")
	}

	return Hero(name, race, player).apply {
		/* Level. */
		this.level = obj.getInt("level") // Int!
		log.info("Set level: ${obj.getInt("level")} => $level")

		/* Insipiration: Number of collected inspiration points. */
		this.inspiration = obj.getInt("inspiration")

		/* Expirience points. */
		obj.getJSONObject("experience").also { xp ->
			experience = Hero.Experience(points = xp.getInt("points"), method = xp.getString("type"))
		}

		/* Hitpoints => object {max, now, temporary} */
		obj.getJSONObject("hitpoints").also { hp ->
			hitpointsMax = hp.getInt("max")
			hitpointsNow = hp.getInt("now")
			hitpointsTmp = hp.getInt("tmp")
		}

		/* Abilities. */
		obj.getJSONObject("abilities").also { abs ->
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
		// klasses => [Klass = {klass, sublklass, lvl} ]?
		obj.getJSONArray("klasses").let { cls ->
			cls.forEach<JSONObject> { c ->
				this.updateKlass(
					klass = c.getString("klass-name"),
					level = c.getInt("klass-level"),
					subklass = c.optStringNull("subklass-name"))
			}
		}

		/* Skills => list [list [name, is-expert, source]].
		 * Fill skills (proficiencies). */
		obj.getJSONArray("skills").forEach<JSONObject> {
			var skillName = it.getString("skill-name")
			var level = when (it.optBoolean("is-expert", false)) {
				true -> SimpleProficiency.E
				else -> SimpleProficiency.P
			}
			var skillSource = it.getString("skill-source")

			try {
				val skill: SimpleSkill = (
					SimpleSkill.DEFAULT_SKILLS.find { it.name == skillName }
					?: SimpleSkill(name = skillName, ability = Ability.fromString(it.getString("custom-attribute")))
				)

				log.debug("Set proficiencies: $it => matching SimpleSkill ${skill}")
				skills[skill] = Pair(level, skillSource)

			} catch (e: Exception) {
				log.error("Could set a skill ($skillName) given by $it.")
				log.error("Caught Exception: $e")
			}
		}

		/* Skills => list [list [name, is-expert, source]].
		 * Fill proficiencies. */
		obj.getJSONArray("tools").forEach<JSONObject> {
			val tool = it.optString("tool-name") to it.optString("tool-category")
			var level = when (it.optBoolean("is-expert", false)) {
				true -> SimpleProficiency.E
				else -> SimpleProficiency.P
			}
			var skillSource = it.getString("tool-source")

			tools[tool] = level to skillSource
		}

		/* Languages => list [string]. */
		this.languages = obj.getJSONArray("languages").let { langs ->
			(0 until langs.length()).map<Int, String> { i -> langs.getString(i) }
		}

		/* Armor-class:  armor class as points, and what is worn for that (shield, weapons, armor). */
		armorSources = obj.getJSONArray("armor-sources").map<String,String> { it }

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
		// klass traits
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

		val spellCatalog: Map<String, SimpleSpell> = listOf(
			// just a spell
			SimpleSpell(name = "Mage Armor",    school = "???", castingTime = "1 act", ritual = false, components = SimpleSpell.Components.VSM(listOf("Piece of cured Leather" to 0)),       range = "Touch",               duration = "Instantaneous", concentration = false, description = "?", levels = mapOf(1 to mapOf()), optAttackRoll = false, optSpellDC = false),
			// spel with concentration
			SimpleSpell(name = "Wall of Stone", school = "???", castingTime = "1 act", ritual = false, components = SimpleSpell.Components.VSM(listOf("Small Block of Granite" to 0)),       range = "Touch",               duration = "10 min",        concentration =  true, description = "?", levels = mapOf(5 to mapOf()), optAttackRoll = false, optSpellDC = false),
			// levelling spell
			SimpleSpell(name = "Heal",          school = "???", castingTime = "1 act", ritual = false, components = SimpleSpell.Components.VS,                                               range = "Touch",               duration = "Instantaneous", concentration = false, description = "?", levels = (6 .. 9).map { l -> l to mapOf("Heal" to "${(l + 1)*10} hp")}.toMap(), optAttackRoll = false, optSpellDC = false),
			// leveling cantrip
			SimpleSpell(name = "Firebolt",      school = "???", castingTime = "1 act", ritual = false, components = SimpleSpell.Components.VS,                                               range = "120 ft",              duration = "Instantaneous", concentration = false, description = "?", levels = mapOf(0 to mapOf("Attack-Damage" to "1d10 (fire)"), -5 to mapOf("Attack-Damage" to "2d10 (fire)"), -11 to mapOf("Attack-Damage" to "3d10 (fire)"), -17 to mapOf("Attack-Damage" to "4d10 (fire)")), optAttackRoll = true, optSpellDC = false),
			// spell with ritual (and concentration)
			SimpleSpell(name = "Detect Magic",  school = "???", castingTime = "1 act", ritual =  true, components = SimpleSpell.Components.VS,                                               range = "self + globe (30ft)", duration = "10 min",        concentration =  true, description = "?", levels = mapOf(1 to mapOf()), optAttackRoll = false, optSpellDC = false),
			// spell components with money
			SimpleSpell(name = "Clone",         school = "???", castingTime =   "1 h", ritual = false, components = SimpleSpell.Components.VSM(listOf("Diamond" to 1000, "Vessel" to 2000)), range = "Touch",               duration = "Instantaneous", concentration = false, description = "?", levels = mapOf(8 to mapOf()), optAttackRoll = false, optSpellDC = false),
		).map { it.name to it }.toMap()

		log.debug("Spell Catalog: $spellCatalog")

		// spells => object with two lists
		// XXX (2021-03-05) spells
		// list? of spells prepared, available at all.
		obj.optJSONObject("spells")?.let { metaSpells ->
			// XXX (2021-03-05) loteinit or look up in a library
			val available = metaSpells.getJSONArray("available").map<JSONObject, Any?> {
				spellCatalog[it.getString("spell-name")]?.let { spell ->
					learnSpell(spell = spell, it.optString("spell-source", "race"))
				} ?: false
			}

			val prepared = metaSpells.getJSONArray("prepared").map<String,String> { it }.forEach {
				prepareSpell(spellName = it as String)
			}
		}

		// XXX DELETE ME: Item Catalog => repository of item, identifier := name
		val itemCatalog: Map<String, Triple<String, Double, Int>> = mapOf (
			"Copper Coin"   to Triple("Currency", 0.02, 1),
			"Silver Coin"   to Triple("Currency", 0.02, SimpleItem.SP_TO_CP),
			"Gold Coin"     to Triple("Currency", 0.02, SimpleItem.GP_TO_CP),
			"Electrum Coin" to Triple("Currency", 0.02, SimpleItem.EP_TO_CP),
			"Platinum Coin" to Triple("Currency", 0.02, SimpleItem.PP_TO_CP),

			"Mage Armor Vest" to Triple("Clothing",  3.0, SimpleItem.SP_TO_CP * 5),

			"Ember Collar" to Triple("Artefact",  0.0, 0),
			"Ring of Spell Storing" to Triple("Ring",  0.0, SimpleItem.GP_TO_CP * 20000),
			"Focus (pet collar)" to Triple("Arcane Focus",  1.0, 0),
			"Potion of Greater Healing" to Triple("Potion",  0.0, 0),
			"Sword of Answering" to Triple("Weapon",  0.0, 0),

			"Backpack" to Triple("Adventuring Gear", 1.0, 0),

			"Pouch"  to Triple("Adventuring Gear", 1.0, 0), // can hold 0.2 ft^3 or 6 lb

			"Ball"   to Triple("Miscelleanous", 0.01, 1),

			"Dagger" to Triple("Simple Meelee Weapon", 1.0, 4), // 1d4 piercing, finesse, simple melee, throwable

			"Flask"  to Triple("Container", 1.0, 2), // can hold 1 pint of liquid (0.00056826125 m^3 = 568.26125 ml)
			"Oil"    to Triple("Miscelleanous", 1.0, SimpleItem.SP_TO_CP), // 1lb oil is worth 1sp
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
}

/** Write the Hero to a JSON String. */
public fun Hero.toJSON(indentSpaces: Int = -1) : String
	= this.let { hero ->
		JSONObject().apply {
			put("name", hero.name)
			put("race", JSONObject(mutableMapOf(
				"race-name" to hero.race.first,
				"subrace-name" to hero.race.second,
			)))

			put("level", hero.level)
			put("experience", JSONObject(mutableMapOf(
				"points" to hero.experience.points,
				"type" to hero.experience.method,
			)))

			putOpt("player", player) // puts optionally (if not null)
			put("inspiration", hero.inspiration)

			put("hitpoints", JSONObject(mutableMapOf(
				"max" to hero.hitpointsMax,
				"tmp" to hero.hitpointsTmp,
				"now" to hero.hitpointsNow,
			)))

			put("deathsaves", "XXX type of death saves.") // TODO

			// leftover hitdice, of all max (calculated by Klass levels)
			put("available-hitdice", JSONObject(hero.hitdice.toMutableMap()))

			// var speed: MutableMap<String, Int>
			// var walkingSpeed: Int
			put("speed", hero.speed.toList().map { (name, value) ->
				JSONObject(mapOf("movement-name" to name, "speed-in-feet" to value))
			})

			// Ability STR, DEX, CON, INT, WIS, CHA
			put("abilities", JSONObject(Ability.values().associateWith { a -> hero.ability(a) }))
			put("save-proficiencies", Ability.values().filter { a -> hero.isSavingThrowProficient(a) })

			// var klasses: MutableList<Triple<String, String?, Int>>
			put("klasses", hero.klasses.map { (klass, sub, lvl) ->
				JSONObject(mutableMapOf(
					"klass-name" to klass,
					"subklass-name" to sub,
					"klass-level" to lvl,
				))
			})

			// var skills: MutableMap<SimpleSkill, Pair<SimpleProficiency, String>>
			put("skills", hero.skills.toList().map { (skill, prof) ->
				JSONObject().apply {
					put("skill-name", skill.name)
					put("is-expert", prof.first == SimpleProficiency.E)

					put("skill-source", prof.second)  // why proficient

					if (skill !in SimpleSkill.DEFAULT_SKILLS) {
						/* Custom Skill's Ability. If default skip (redudant info). */
						put("custom-attribute", skill.ability)
					}
				}
			})

			// var tools: MutableMap<Pair<String, String>, Pair<SimpleProficiency, String>>
			put("tools", JSONArray(hero.tools.toList().map { (tool, prof) ->
				JSONObject().apply {
					when {
						tool.first != "" -> put("tool-name", tool.first)
						else -> put("tool-category", tool.second)
					}
					put("is-expert", prof.first == SimpleProficiency.E)
					put("tool-source", prof.second)  // why proficient
				}
			}))

			// var languages: List<String>
			put("languages", hero.languages)

			// var specialities: List<Speciality>
			// var conditions: List<Effect>
			// TODO (imeplemt me)


			put("traits", hero.specialities.map { speciality -> JSONObject().apply {
				/* Name Type indicating the Code's Type.*/
				put(when (speciality) {
					is RaceFeature -> "race-feature-name"
					is KlassTrait -> "klass-trait-name"
					is Feat -> "feat-name"
					is ItemFeature -> "item-feature-name"
					else -> "custom-name"
				}, speciality.name)

				put("description", speciality.description)
				put("count", speciality.count)
			}})

			// var spells: Map<Pair<SimpleSpell, String>, Boolean>
			// val maxPreparedSpells: List<Int>  // depends on klasses, race and feats
			// val spellsPrepared: Set<Pair<SimpleSpell, String>>  // depends on spell

			// var armorSources: List<String>
			// val armorClass: Int  // depends on feats, spells and equipped clothes
			// val naturalBaseArmorClass: Int // depends on feats and spells
			// TODO
			put("armor-sources", listOf("naked"))

			// var inventory: MutableList<Pair<SimpleItem, String>>
			/* Just save the items name, identifer, ouccurence time (per storage).
			 * The Item should be in the item catalogue. */
			put("inventory", hero.inventory
				// grouping: stored, by item name and of it is a storage itself.
				// for all with triple.third == true (storage itself): store with identifier
				.groupBy { (item, storageID) -> Triple(item.name, storageID, hero.inventory.isStoring(item)) }
				.toList()
				// part if is-a-storage or one-of-many-same
				.partition { (triple, items) -> triple.third == false && items.size > 1 }
				.let { (summableWithCount, necessarilyIdenfiable) ->

					/* List items, which can be compressed to be listed as sums of the item.
					 * (e.g. 6x Gold Pieces) */
					(summableWithCount
						// count amount of items
						.map { (tri, its) -> Triple(tri.first, tri.second, its.size) }
						// to JSON
						.map { (itemName, storageID, count) -> JSONObject(mapOf(
							"item-name" to itemName,
							"amount" to count,
							"storage" to storageID,
						))}
					+

					/* List items, which needs to be identifiable
					 * (e.g. Pouch #5 with Special Gems) */
					necessarilyIdenfiable
						// just the items and their storages again.
						.flatMap { it.second }
						// to JSON
						.map { (item, storageID) -> JSONObject(mapOf(
							"item-name" to item.name,
							"identifier" to item.identifier,
							"storage" to storageID,
						))}
					) // list or JSONObject objects
				}
			)

			log.debug(this)

			// TODO alternative ? log.debug("alternatively: ${JSONObject(hero)}") // looks too stupid.

		}.let { jo -> if (indentSpaces < 0) jo.toString() else jo.toString(indentSpaces) }
	}

/** Save a player character as JSON to file.
  * @throws FileNotFoundException
  * @see Hero.toJSONObject()
  */
public fun Hero.saveCharater(hero: Hero, filepath: String) {
	log.debug("Save to $filepath")
	File(filepath).writeText(hero.toJSON())
}

/** Load the Hero from a given JSON file.
  * @throws FileNotFoundException
  * @throws JSONException
  * @see Hero.fromJSON(String)
  */
public fun loadHero(filepath: String) : Hero {
	val br: BufferedReader = File(filepath).bufferedReader()
	val txt = br.use { it.readText() }

	return Hero.fromJSON(txt) // parse the hero
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

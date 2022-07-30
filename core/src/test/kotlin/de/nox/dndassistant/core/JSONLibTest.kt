package de.nox.dndassistant.core

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.json.JSONArray
import org.json.JSONTokener

class JSONLibTest {

    private val log = LoggerFactory.getLogger("Hero-Loader-Test")

    private val testItemCatalog: Map<String, PreSimpleItem> =
            mapOf(
                    "Copper Coin" to PreSimpleItem("Currency", 0.02, 1, false),
                    "Silver Coin" to PreSimpleItem("Currency", 0.02, SimpleItem.SP_TO_CP, false),
                    "Gold Coin" to PreSimpleItem("Currency", 0.02, SimpleItem.GP_TO_CP, false),
                    "Electrum Coin" to PreSimpleItem("Currency", 0.02, SimpleItem.EP_TO_CP, false),
                    "Platinum Coin" to PreSimpleItem("Currency", 0.02, SimpleItem.PP_TO_CP, false),
                    "Mage Armor Vest" to
                            PreSimpleItem("Clothing", 3.0, SimpleItem.SP_TO_CP * 5, false),
                    "Ember Collar" to PreSimpleItem("Artefact", 0.0, 0, false),
                    "Ring of Spell Storing" to
                            PreSimpleItem("Ring", 0.0, SimpleItem.GP_TO_CP * 20000, false),
                    "Focus (pet collar)" to PreSimpleItem("Arcane Focus", 1.0, 0, false),
                    "Potion of Greater Healing" to PreSimpleItem("Potion", 0.0, 0, true),
                    "Sword of Answering" to PreSimpleItem("Weapon", 0.0, 0, false),
                    "Pouch" to
                            PreSimpleItem(
                                    "Adventuring Gear",
                                    1.0,
                                    0,
                                    false
                            ), // can hold 0.2 ft^3 or 6 lb
                    "Ball" to PreSimpleItem("Miscelleanous", 0.01, 1, false),
                    "Dagger" to
                            PreSimpleItem(
                                    "Simple Meelee Weapon",
                                    1.0,
                                    4,
                                    false
                            ), // 1d4 piercing, finesse, simple melee, throwable
                    "Flask" to
                            PreSimpleItem(
                                    "Container",
                                    1.0,
                                    2,
                                    false
                            ), // can hold 1 pint of liquid (0.00056826125 m^3 = 568.26125 ml)
                    "Oil" to
                            PreSimpleItem(
                                    "Miscelleanous",
                                    1.0,
                                    SimpleItem.SP_TO_CP,
                                    false
                            ) // 1lb oil is worth 1sp
            )

    private val spells =
            listOf(
                    SimpleSpell(
                            name = "Simple Spell",
                            school = "???",
                            castingTime = "1 act",
                            ritual = false,
                            components =
                                    SimpleSpell.Components.VSM(
                                            listOf("Piece of cured Leather" to 0)
                                    ),
                            reach = 5,
                            targets = "Touch",
                            duration = "Instantaneous",
                            concentration = false,
                            description = "?",
                            levels = mapOf(1 to mapOf()),
                            optAttackRoll = false,
                            optSpellDC = false
                    ),
                    SimpleSpell(
                            name = "Concentration Spell",
                            school = "???",
                            castingTime = "1 act",
                            ritual = false,
                            components =
                                    SimpleSpell.Components.VSM(
                                            listOf("Small Block of Granite" to 0)
                                    ),
                            reach = 5,
                            targets = "Touch",
                            duration = "10 min",
                            concentration = true,
                            description = "?",
                            levels = mapOf(5 to mapOf()),
                            optAttackRoll = false,
                            optSpellDC = false
                    ),
                    SimpleSpell(
                            name = "Levelling Spell",
                            school = "???",
                            castingTime = "1 act",
                            ritual = false,
                            components = SimpleSpell.Components.VS,
                            reach = 5,
                            targets = "Touch",
                            duration = "Instantaneous",
                            concentration = false,
                            description = "?",
                            levels =
                                    (6..9)
                                            .map { l -> l to mapOf("Heal" to "${(l + 1)*10} hp") }
                                            .toMap(),
                            optAttackRoll = false,
                            optSpellDC = false
                    ),
                    SimpleSpell(
                            name = "Levelling Cantrip",
                            school = "???",
                            castingTime = "1 act",
                            ritual = false,
                            components = SimpleSpell.Components.VS,
                            reach = 120,
                            targets = "One Target",
                            duration = "Instantaneous",
                            concentration = false,
                            description = "?",
                            levels =
                                    mapOf(
                                            0 to mapOf("Attack-Damage" to "1d10 (fire)"),
                                            -5 to mapOf("Attack-Damage" to "2d10 (fire)"),
                                            -11 to mapOf("Attack-Damage" to "3d10 (fire)"),
                                            -17 to mapOf("Attack-Damage" to "4d10 (fire)")
                                    ),
                            optAttackRoll = true,
                            optSpellDC = false
                    ),
                    SimpleSpell(
                            name = "Spell with many targets",
                            school = "???",
                            castingTime = "1 act",
                            ritual = false,
                            components = SimpleSpell.Components.VS,
                            reach = 120,
                            targets = "20-ft radius",
                            duration = "Instantaneous",
                            concentration = false,
                            description = "?",
                            levels =
                                    mapOf(
                                            0 to mapOf("Attack-Damage" to "1d10 (fire)"),
                                            -5 to mapOf("Attack-Damage" to "2d10 (fire)"),
                                            -11 to mapOf("Attack-Damage" to "3d10 (fire)"),
                                            -17 to mapOf("Attack-Damage" to "4d10 (fire)")
                                    ),
                            optAttackRoll = true,
                            optSpellDC = false
                    ),
                    SimpleSpell(
                            name = "Ritual Spell",
                            school = "???",
                            castingTime = "1 act",
                            ritual = true,
                            components = SimpleSpell.Components.VS,
                            reach = 0,
                            targets = "self + globe (30ft)",
                            duration = "10 min",
                            concentration = false,
                            description = "?",
                            levels = mapOf(1 to mapOf()),
                            optAttackRoll = false,
                            optSpellDC = false
                    ),
                    SimpleSpell(
                            name = "Spell with Materials (GP) Needed",
                            school = "???",
                            castingTime = "1 h",
                            ritual = false,
                            components =
                                    SimpleSpell.Components.VSM(
                                            listOf("Diamond" to 1000, "Vessel" to 2000)
                                    ),
                            reach = 5,
                            targets = "Touch",
                            duration = "Instantaneous",
                            concentration = false,
                            description = "?",
                            levels = mapOf(8 to mapOf()),
                            optAttackRoll = false,
                            optSpellDC = false
                    ),
            )

    // get from testItemCatalog
    private fun Map<String, PreSimpleItem>.getItem(name: String, id: String) =
            this.get(name)?.let { (category, weight, coppers, dividable) ->
                SimpleItem(
                        name = name,
                        identifier = id,
                        category = category,
                        weight = weight,
                        copperValue = coppers,
                        dividable = dividable,
                )
            }

    private val exampleHero =
            Hero("Name", "Race" to "Subrace", player = null as String?).apply {
                // val race: Pair<String, String>
                // var name: String
                name = "Re Name"

                // var level = 1
                // val proficiencyBonus: Int
                level = 14

                // var experience: Experience
                experience.points += 200 // prbly to low for lvl 14

                // var player: String?
                player = "Adopting Player"

                // var inspiration: Int
                inspiration += 3 // accumulated as token

                // var speed: MutableMap<String, Int>
                // var walkingSpeed: Int

                // abilities
                // TODO

                // var armorSources: List<String>
                // val armorClass: Int // depends on feats, spells and equipped clothes
                // val naturalBaseArmorClass: Int // depends on feats and spells

                // var hitpointsMax: Int
                // var hitpointsTmp: Int
                // var hitpointsNow: Int
                // val hitpoints: Pair<Int, Int> // (current + buffer) to (max + buffer)

                hitpointsMax = 109
                hitpointsTmp = 5 // buffer
                hitpointsNow = 24

                deathsaves.addFailure()
                deathsaves.addSuccess(critical = true)

                // var klasses: MutableList<Triple<String, String?, Int>>

                klasses.plusAssign(Triple("Base Klass", null, 3))
                klasses.plusAssign(Triple("Second Klass", "Chosen Klass Branch", 11))

                // var skills: MutableMap<SimpleSkill, Pair<SimpleProficiency, String>>
                // var tools: MutableMap<Pair<String, String>, Pair<SimpleProficiency, String>>

                // saveProficiences += Ability.WIS
                skills.plusAssign(
                        SimpleSkill.DEFAULT_SKILLS["Acrobatics"]!! to
                                (SimpleProficiency.P to "Base Klass")
                ) // TODO better syntax
                skills.plusAssign(
                        SimpleSkill("Additional Knitting", Ability.DEX) to
                                (SimpleProficiency.E to "Background")
                ) // TODO better syntax

                // setSkillProficiency(SimpleSkill.DEFAULT_SKILLS["Acrobatics"],
                // SimpleProficiency.E, "Base Klass")

                tools.plusAssign(
                        ("" to "Simple Meelee Weapon") to (SimpleProficiency.P to "Base Klass")
                ) // TODO better syntax
                tools.plusAssign(
                        ("Knitting Set" to "") to (SimpleProficiency.P to "Background")
                ) // TODO better syntax

                // val hitdiceMax: Map<String, Int> // generated by klasses
                // var hitdice: MutableMap<String, Int>
                hitdice =
                        (hitdiceMax - ("d6" to 1)) as
                                MutableMap<
                                        String,
                                        Int> // use one up? // TODO better syntax. hitdice -= "d6"
                // => hitdice { "d6": 4, "d4": 1} to { "d6": 3, "d4":
                // 1}

                // var languages: List<String>
                // var specialities: List<Speciality>
                // var conditions: List<Effect>
                languages += "Common"

                specialities += RaceFeature("Race Feature", "Race" to "Subrace")
                specialities +=
                        KlassTrait("Klass Trait", Triple("Second Klass", "Chosen Klass Branch", 11))
                specialities += Feat("Chosen Feat")
                specialities += ItemFeature("Feature by atuned or carried Item")
                specialities += CustomCount("Custom Count")

                // - Feat(name: String, count: Count?, description: String)
                // - KlassTrait(name: String, val klass: Triple<String, String, Int>, count: Count?,
                // description: String)
                // - RaceFeature(name: String, val race: Pair<String, String>, val level: Int,
                // count: Count?, description: String)
                // - ItemFeature(name: String, count: Count?, description: String)
                // - CustomCount(name: String, count: Count?, description: String)

                // TODO later human interface: setting klasses or leveling up should add the option
                // to auto add the specialities, without duplicating

                log.debug(conditions)
                // - Prone / grapelled
                // - under spell influence
                // - Effect(name: String, seconds: Int, val removable: Boolean, description: String)

                // var spells: Map<Pair<SimpleSpell, String>, Boolean>
                // val maxPreparedSpells: List<Int> // depends on klasses, race and feats
                // val spellsPrepared: Set<Pair<SimpleSpell, String>> // depends on spell
                log.debug(spells)

                // var attacks: MutableMap<Attack, String>
                attacks[
                        Attack(
                                "Unarmed Strike",
                                listOf(Attack.Damage(DamageType.BLUDGEONING, "1d3 + DEX"))
                        )] = "d20 + STR"

                attacks[Attack("Some Spell as Source", DamageType.BLUDGEONING, "1d8 + CHA")] =
                        "d20 + CHA"
                attacks[
                        Attack(
                                "Some other Spell as Source",
                                DamageType.BLUDGEONING,
                                "12d6",
                                120,
                                "20ft radius"
                        )] = "DC 18 (DEX)" // hits anyways, if in range or so.

                // var inventory: MutableList<Pair<SimpleItem, String>>
                inventory.plusAssign(testItemCatalog.getItem("Dagger", "Dagger@0")!! to "")
                inventory.plusAssign(
                        SimpleItem("Backpack", "Backpack@0", "Adventuring Gear", 1.0, 0, false) to
                                ""
                )

                inventory.plusAssign(
                        testItemCatalog.getItem("Sword of Answering", "SoA@0")!! to "Backpack@0"
                )

                // add attack for item.
                attacks[
                        Attack(
                                inventory.getItemByName("Sword of Answering")!!.first,
                                DamageType.SLASHING,
                                "1d8 + 3 + STR + proficiencyBonus"
                        )] = "1d20 + 3 + STR + proficiencyBonus"

                inventory.plusAssign(testItemCatalog.getItem("Pouch", "Pouch@0")!! to "Backpack@0")
                (0..5).forEach {
                    inventory.plusAssign(
                            testItemCatalog.getItem("Silver Coin", "SP@$it")!! to "Pouch@0"
                    )
                }

                inventory.plusAssign(testItemCatalog.getItem("Gold Coin", "GP@0")!! to "Pouch@0")

                inventory.plusAssign(testItemCatalog.getItem("Pouch", "Pouch@1")!! to "Backpack@0")
                (1..3).forEach {
                    inventory.plusAssign(
                            testItemCatalog.getItem("Gold Coin", "GP@$it")!! to "Pouch@1"
                    )
                }

                inventory.plusAssign(
                        testItemCatalog.getItem("Pouch", "Pouch@Empty")!! to "Backpack@0"
                )

                log.debug(inventory)
            }

    // save and check if the re-loaded hero equals contextual the saved.
    @Test
    fun testStoreRestoreJSON() {
        log.info("Test.testStoreRestoreJSON()")
        log.displayLevel(LoggingLevel.DEBUG)

        // Example Hero to JSON
        val heroJSON = exampleHero.toJSON()
        var msg: String

        log.debug("Example Hero to Hero.json:\n" + heroJSON)

        // write to file.
        File("/tmp/HeroJSON.json").writeText(heroJSON)

        // load SimpleItem.Catalog for loading equipped SimpleItem
        SimpleItem.Catalog =
                testItemCatalog +
                        mapOf(
                                // custom items.
                                "Backpack" to PreSimpleItem("Adventuring Gear", 1.0, 0, false),
                        )

        // heroJSON resored to a (new) Hero.
        val restoredHero = Hero.fromJSON(heroJSON)

        log.debug("Example Hero.json to Hero (restored):\n" + restoredHero)

        log.debug(
                "Compare Restored Hero with Example Hero. Should be the same at the state of storing."
        )

        msg = "Hero's race"
        assertEquals(exampleHero.race, restoredHero.race, msg)

        msg = "Hero's name"
        assertEquals(exampleHero.name, restoredHero.name, msg)

        msg = "Hero's level"
        assertEquals(exampleHero.level, restoredHero.level, msg)

        msg = "Hero's proficiencyBonus"
        assertEquals(exampleHero.proficiencyBonus, restoredHero.proficiencyBonus, msg)

        msg = "Hero's experience"
        assertEquals(exampleHero.experience, restoredHero.experience, msg)

        msg = "Hero's player"
        assertEquals(exampleHero.player, restoredHero.player, msg)

        msg = "Hero's inspiration"
        assertEquals(exampleHero.inspiration, restoredHero.inspiration, msg)

        msg = "Hero's speed"
        assertEquals(exampleHero.speed, restoredHero.speed, msg)

        msg = "Hero's walkingSpeed"
        assertEquals(exampleHero.walkingSpeed, restoredHero.walkingSpeed, msg)

        // msg =  "Hero's armorSources"
        // assertEquals(exampleHero.armorSources, restoredHero.armorSources, msg)

        msg = "Hero's armorClass"
        assertEquals(exampleHero.armorClass, restoredHero.armorClass, msg)

        msg = "Hero's naturalBaseArmorClass"
        assertEquals(exampleHero.naturalBaseArmorClass, restoredHero.naturalBaseArmorClass, msg)

        msg = "Hero's hitpoints"
        assertEquals(exampleHero.hitpoints, restoredHero.hitpoints, msg)

        msg = "Hero's hitpointsMax"
        assertEquals(exampleHero.hitpointsMax, restoredHero.hitpointsMax, msg)

        msg = "Hero's hitpointsTmp"
        assertEquals(exampleHero.hitpointsTmp, restoredHero.hitpointsTmp, msg)

        msg = "Hero's hitpointsNow"
        assertEquals(exampleHero.hitpointsNow, restoredHero.hitpointsNow, msg)

        msg = "Hero's deathsaves"
        assertEquals(exampleHero.deathsaves, restoredHero.deathsaves, msg)

        msg = "Hero's abilities"
        assertEquals(exampleHero.abilities, restoredHero.abilities, msg)

        msg = "Hero's skills"
        assertEquals(exampleHero.skills, restoredHero.skills, msg)

        msg = "Hero's skillValues"
        assertEquals(exampleHero.skillValues, restoredHero.skillValues, msg)

        msg = "Hero's tools"
        assertEquals(exampleHero.tools, restoredHero.tools, msg)

        msg = "Hero's klasses"
        assertEquals(exampleHero.klasses, restoredHero.klasses, msg)

        msg = "Hero's hitdiceMax"
        assertEquals(exampleHero.hitdiceMax, restoredHero.hitdiceMax, msg)

        msg = "Hero's hitdice"
        assertEquals(exampleHero.hitdice, restoredHero.hitdice, msg)

        msg = "Hero's languages"
        assertEquals(exampleHero.languages, restoredHero.languages, msg)

        // TODO (comparing test, comparable Speciality Lists.)
        // msg =  "Hero's specialities"
        // assertEqualsBy(exampleHero.specialities, restoredHero.specialities, msg)

        msg = "Hero's conditions"
        assertEquals(exampleHero.conditions, restoredHero.conditions, msg)

        msg = "Hero's spellsPrepared"
        assertEquals(exampleHero.spellsPrepared, restoredHero.spellsPrepared, msg)

        // TODO (comparing test, comparable <Simple Spells, String> List.)

        msg = "Hero's spells"
        assertEquals(exampleHero.spells, restoredHero.spells, msg)

        // TODO (comparing test, comparable <SimpleItem, String> List.)
        // contains all attributes.
        msg = "Hero's inventory"
        val a = exampleHero.inventory.sortedBy { it.second }
        val b = restoredHero.inventory.sortedBy { it.second }
        val inventoriesMatch = a.all { it in b } && b.all { it in a }
        assertTrue(inventoriesMatch, msg)

        log.debug("Hero example attack: ${exampleHero.attacks}")
        log.debug("Hero restored attack: ${restoredHero.attacks}")

        msg = "Hero's attacks"
        val heroAttacksMatch =
                exampleHero.attacks.toList().all { (attack, roll) ->
                    restoredHero.attacks[attack] == roll
                } &&
                        restoredHero.attacks.toList().all { (attack, roll) ->
                            exampleHero.attacks[attack] == roll
                        }
        assertTrue(heroAttacksMatch, msg)

        log.info("Test: testLoading: OK!")
    }

    @Test
    fun testLoadCatalog() {
        log.info("Test.testLoadCatalog() / loadSimpleItemCatalog")
        var filepath = File("src/test/resources/ItemCatalog.json").getAbsolutePath()
        val txt = readText(filepath)

        var msg: String

        // start with an empty SimpleItem.Catalog.
        SimpleItem.Catalog = mapOf()

        msg = "Start with empty SimpleItem.Catalog"
        assertEquals(0, SimpleItem.Catalog.size, msg)

        // load from file.
        val catalog = loadSimpleItemCatalog(txt)

        msg = "Loaded File successfully"
        assertEquals(true, catalog.size > 0, msg)

        msg = "Loaded also into SimpleItem.Catalog (greater than 0)"
        assertEquals(true, SimpleItem.Catalog.size > 0, msg)

        msg = "All file loaded catalog items are now in SimpleItem.Catalog"

        assertEquals(true, SimpleItem.Catalog.keys.containsAll(catalog.keys), msg)

        log.info("Test.testLoadCatalog() DONE")
    }

    @Test
    fun loadSimpleSpells() {
        log.info("Test.loadSimpleSpells() / SimpleSpell.toJSON(), SimpleSpell.Companion.fromJSON()")

        val spellsJSON = spells.toJSON()

        File("/tmp/Spells.json").writeText(spellsJSON) // XXX

        val spellsFromJSON =
                (JSONTokener(spellsJSON).nextValue() as JSONArray).let { array ->
                    (0 until array.length()).map { i ->
                        SimpleSpell.fromJSON(array.getJSONObject(i).toString())
                    }
                }

        // to JSON and back.
        assertTrue(spellsFromJSON.containsAll(spells))
        assertTrue(spells.containsAll(spellsFromJSON))

        log.info("Test.loadSimpleSpells() DONE")
    }

    @Test
    fun castSpell() {
        log.info("Test.castSpells()")

        var msg: String
        var castSuccess: Either<CastSpell, String>
        var spellSlots: List<KlassTrait> =
                listOf(-1, 4, 3, 3, 3, 3, 2, 2, 1, 1).mapIndexed { spellSlotLevel, countMax ->
                    KlassTrait(
                            "Spell Slot $spellSlotLevel",
                            Triple("Base Klass", "", 20),
                            count = Speciality.Count(recharge = "longrest", max = countMax)
                    )
                }

        castSuccess = exampleHero.checkSpellCastable(spells[0].name)

        msg = "Cannot cast unknown spells."
        assertEquals(true, castSuccess is Either.Right, msg)

        msg = "No Spell leaned"
        assertEquals(0, exampleHero.spells.size, msg)

        msg = "No Spell Slots"
        assertEquals(0, exampleHero.specialities.filter { it.name.startsWith("Slot") }.size, msg)

        log.info("Learn Spells")
        spells.forEach { exampleHero.learnSpell(it, "Base Klass") }

        msg = "Spell is now known."
        assertEquals(true, exampleHero.spells.find { it.spell.name == spells[0].name } != null, msg)

        log.info("Prepare Spells")
        exampleHero.spells.forEach { exampleHero.prepareSpell(it.spell.name) }

        msg = "Spell is now prepared."
        assertEquals(
                true,
                exampleHero.spellsPrepared.find { it.spell.name == spells[0].name } != null,
                msg
        )

        log.info("Available Spells: ${exampleHero.spells}")

        castSuccess = exampleHero.checkSpellCastable(spells[0].name)

        msg = "Cannot cast without spell slots."
        assertEquals(true, castSuccess is Either.Right, msg)

        log.info("Casting Spell Success: $castSuccess")

        log.info("Add Spell slots")
        spellSlots.drop(1).forEach { exampleHero.specialities += it }

        msg = "Spell Slots succssfull added, now hero has such counters."
        assertEquals(
                true,
                0 != exampleHero.specialities.filter { it.name.startsWith("Spell Slot") }.size,
                msg
        )

        // spellslot[0] = exampleHero.specialities.find { it.name == "Spell Slot 1" }!!

        log.info("Spell Slots Before: $spellSlots")
        log.info("Spell Slots Before (hero): ${exampleHero.specialities}")

        msg = "Spell Slot 1: Before Cast (4/4)."
        assertEquals(4, spellSlots[1].count!!.current, msg)

        log.info("Cast spell again.")

        castSuccess = exampleHero.checkSpellCastable(spells[0].name)

        log.info(
                "castSuccess = $castSuccess = ${if (castSuccess is Either.Left) castSuccess.left else (castSuccess as Either.Right).right}"
        )

        // XXX source to counter

        msg = "Spell learned, prepared and spell slots available: Successful!."
        assertEquals(true, castSuccess is Either.Left, msg)

        log.info("Spell Slots Afterwards: $spellSlots")
        log.info("Spell Slots Afterwards (hero): ${exampleHero.specialities}")

        msg = "Spell Slot 1: After Cast (3/4) ."
        assertEquals(3, spellSlots[1].count!!.current, msg)

        log.info(
                "Repeat casting spell on lvl 1, until spell slots are used up. Use higher automatically."
        )

        for (i in 0 until 3) {
            exampleHero.checkSpellCastable(spells[0].name)
            log.info("Spell Slots Afterwards (hero): ${exampleHero.specialities}")
        }

        msg = "Spell Slot 1: After Multiple Casts (0/4) ."
        assertEquals(0, spellSlots[1].count!!.current, msg)

        castSuccess = exampleHero.checkSpellCastable(spells[0].name)
        msg = "New casting attempt tries to use spell slot 2"
        assertEquals(true, castSuccess is Either.Left, msg)

        msg = "Only a casting attempt, no spell slot actually used"
        assertEquals(spellSlots[2].count!!.max, spellSlots[2].count!!.current, msg)

        var checkOnlySuccess2: Either<CastSpell, String> =
                exampleHero.checkSpellCastable(spells[0].name)

        log.info("checkSpellCastable: $checkOnlySuccess2")

        // spell by magic item or so.
        val itemCastSpell =
                SimpleSpell(
                        "Item Cast Spell",
                        "ENCHANTMENT",
                        "1 action",
                        false,
                        components = SimpleSpell.Components.V,
                        reach = 120,
                        targets = "One Target",
                        duration = "10 minutes",
                        concentration = true,
                        description = "",
                        levels = mapOf(9 to mapOf())
                )

        exampleHero.learnSpell(itemCastSpell, "Magic Item X")
        exampleHero.prepareSpell(itemCastSpell.name)
        exampleHero.specialities +=
                ItemFeature("Magic Item X - The Counter!", count = Speciality.Count("longrest", 2))

        checkOnlySuccess2 = exampleHero.checkSpellCastable(itemCastSpell.name)

        when (checkOnlySuccess2) {
            is Either.Left -> log.info("Cast from Item Success: ${checkOnlySuccess2.left}")
            is Either.Right -> log.info("Cast from Item FAIL: ${checkOnlySuccess2.right}")
        }

        log.info("Show learned and prepared spells: ${exampleHero.spells}")

        log.info("Cast Item Cast Spell: $checkOnlySuccess2")

        log.info("Test.castSpells() DONE")
    }
}

package de.nox.dndassistant.core

// private val logger = LoggerFactory.getLogger("ExamplePC")

fun playgroundWithOnyx() : PlayerCharacter {
	val pc : PlayerCharacter
		= PlayerCharacter("Onyx Necklace", player = "Nox")

	// pc.rollAbilityScores()

	pc.setAbilityScores(mapOf(
		Ability.STR to 6,
		Ability.DEX to 17,
		Ability.CON to 11,
		Ability.INT to 16,
		Ability.WIS to 15,
		Ability.CHA to 10
	))

	pc.addProficiency(Skill.SLEIGHT_OF_HAND) // proficient
	pc.addProficiency(Skill.STEALTH) // proficient
	pc.addProficiency(Skill.SLEIGHT_OF_HAND) // expert
	pc.addProficiency(Ability.DEX) // saving throw
	pc.addProficiency(Ability.INT) // saving throw
	// pc.addProficiency(Skill.ARCANA) // proficient (set by sage)
	// pc.addProficiency(Skill.HISTORY) // proficient (set by sage)
	pc.addProficiency(Skill.INSIGHT) // proficient
	pc.addProficiency(Skill.PERCEPTION) // proficient
	pc.addProficiency(Skill.PERCEPTION) // expert

	// logger.debug("Proficent Skills: " + pc.proficiencies)

	val dagger =  Weapon("Dagger", 1.0, Money(gp=2),
		weightClass = WeightClass.LIGHT,
		weaponType = Weapon.Type.SIMPLE_MELEE,
		damage = DiceTerm(D4),
		damageType = setOf(DamageType.PIERCING),
		throwable = true,
		isFinesse = true,
		note = "Finesse, light, thrown (range 20/60)")

	val spear =  Weapon("Spear", 3.0, Money(gp=1),
		weightClass = WeightClass.NONE,
		weaponType = Weapon.Type.SIMPLE_MELEE,
		damage = DiceTerm(D6),
		damageType = setOf(DamageType.PIERCING),
		throwable = true,
		// versatile: 1d8
		note = "Thrown (range 20/60) | versatile (1d8)")

	pc.pickupItem(Container(
		name = "Backpack",
		weight = 5.0, cost = Money(gp=2),
		maxWeight = 30.0,
		maxItems = 0,
		capacity = "1 cubic foot/ 30 pounds; also items can be straped to the outside"
	), "BAG:Backpack")

	pc.pickupItem(spear)
	pc.pickupItem(dagger)

	pc.pickupItem(dagger)
	pc.pickupItem(dagger)

	pc.dropFromHands(true)

	pc.dropFromHands(true, true)

	(1..60).forEach { pc.pickupItem(dagger.copy(), "BAG:Backpack") } // get +60 daggers => inventory +60lb
	// pc.pickupItem(Item("Crowbar", 5.0, Money()))

	val pouch = Container("Pouch", 1.0, Money(sp=5), 6.0, 0, "0.2 cubic foot / 6.0 lb")

	pc.pickupItem(pouch.copy(), "BAG:Backpack")
	pc.pickupItem(pouch.copy(), "BAG:Backpack")
	pc.pickupItem(pouch.copy(), "BAG:Backpack:NESTED Pouch No. 1")
	pc.pickupItem(pouch.copy(), "BAG:Backpack")

	pc.dropFromBag("BAG:Backpack", { index, item -> item == dagger && index < 10 })

	val clothes = Clothes("Clothes, Common", 3.0, Money(sp = 5), BodyType.BODY)
	val leather = Armor("Leather Armor", 10.0, Money(gp = 10), Armor.Weight.LIGHT, 11, 0, true)

	val weapons : List<Weapon> = listOf(dagger, spear)

	// TODO (2020-09-03) add proficiency for common/all SIMPLE MELEE WEAPON
	weapons.filter { it.weaponType == Weapon.Type.SIMPLE_MELEE }.forEach {
		pc.addProficiency(it)
	}

	pc.addProficiency(leather) // proficient
	// TODO (2020-09-03) add proficiency for common/all LIGHT ARMOR

	pc.wear(clothes)
	pc.wear(leather)

	pc.dropEquipped(BodyType.ARMOR.name)

	pc.wear(leather)

	val healing = LooseItem(
		name = "Potion of Healing",
		weight = 0.5,
		cost = Money(gp = 50),
		validContainer = listOf(), // vial, bottle, flask, jug, pot, waterskin
		count = 0.1137, // 4 ounces = 0.1137 l
		measure = LooseItem.Measure.LITER,
		filledDescription = "Vial (4 ounces): 0.5 lb"
		)

	val vial = Container("Vial", 0.0, Money(gp = 1), 0.0, 1, "5 ounces liquid")

	val vialHealing = vial.copy().apply { insert(healing) }

	pc.pickupItem(vialHealing, "BAG:Backpack")

	val mageHand = Spell(
		"Mage Hand", // name
		"Conjuration", 0, // school, level
		"1 action", "30 feet", "V,S", 60, false, // casting time, range, components, duration, concentration
		ritual = false,
		note = """
		Vansishes over 30ft range, or re-cast;
		manipulate objects, open / unlock container, stow / retrive item, pour contents;
		cannot attack, cannot activate magic items, cannot carry more than 10 pounds
		""")

	val guidance = Spell(
		name = "Guidance",
		school = "Divination",
		level = 0,
		castingTime = "1 action",
		range = "Touch",
		components = "V,S",
		duration = 60, concentration = true,
		ritual = false, // is ritual
		note = """
		1. Touch a willing creature.
		2. Roll d4, add to one ability check of choice (pre/post). End.
		""".trimIndent() // description
		)

	val spells: List<Spell> = listOf(
		  Spell("Spell 5", "Illusion",      5, "1 action", "5 feet", "V", 1,     false, true , "...")
		, Spell("Spell 1", "Conjuration",   1, "1 action", "Touch",  "V", 60,    false, false, "...")
		, mageHand
		, Spell("Spell 0", "Abjuration",    0, "1 action", "Touch",  "V", 60,    false, false, "...")
		, Spell("Spell 6", "Necromancy",    6, "1 action", "6 feet", "V", 1,     false, false, "...")
		, guidance
		, Spell("Spell 2", "Divination",    2, "1 action", "1 feet", "V", 1,     false, false, "...")
		, Spell("Spell 4", "Evocation",     4, "1 action", "4 feet", "V", 86400, false, true , "...")
		, Spell("Spell 7", "Transmutation", 7, "1 action", "7 feet", "V", 60,    false, false, "...")
		, Spell("Spell 3", "Enchantment",   3, "1 action", "3 feet", "V", 1,     false, false, "...")
		)

	spells.forEach { pc.learnSpell(it, "Wizard") }

	pc.castSpell(spells.find{ it == guidance }!!)
	pc.prepareSpell(spells.find{ it.name == "Spell 4" }!!)
	pc.castSpell(spells.find{ it.name == "Spell 4" }!!)
	pc.prepareSpell(spells.find{ it.name == "Spell 7" }!!)

	pc.maxHitPoints = 12
	pc.curHitPoints = 7

	pc.setBackground(Background(
		name = "Sage",
		proficiencies = listOf(Skill.ARCANA, Skill.HISTORY),
		equipment = listOf(/*bottle,ink,pen,small knife, letter,clothes*/),
		money = Money(gp=10)).apply {
		extraLanguages = 2
		},
		true)

	val rogue = Klass("Rogue",
	hitdie = D8,
	savingThrows = arrayOf(Ability.DEX, Ability.INT),
	klassLevelTable = setOf( // "table" of level features. == Features per Level.
		Klass.Feature(1, "Experties", "Double skill prof & (skill prof | thieves' tools)."),
		Klass.Feature(1, "Sneak Attack", "1x/turn + advantage* on creature + finesse/ranged \u21d2 +2d6 dmg"),
		Klass.Feature(1, "Thieves' Cant", "dialect + 0.25% speed \u21d2 hide msg in normal conversation"),
		Klass.Feature(2, "Cunning Action", "1x/turn \u21d2 bonus action: Dash, Disengage, Hide."),
		Klass.Feature(3, "Roguish Archetype", "{:specialisations:}")
	),
	specialisations = mapOf(
		"Thief" to setOf(
			Klass.Feature(3, "Fast Hands", "Bonus Action (by Cunning Action): DEX (Sleight of Hand), Thieves' Tools against trap, Open Lock or Object use."),
			Klass.Feature(3, "Second-Story Work", "Climb fast and long. Running Jumb."),
			Klass.Feature(9, "Supreme Sneak", "Sneak, but not to half speed."),
			Klass.Feature(13, "Use magic Device", "Use magic device despise class/race/level requirements!"),
			Klass.Feature(17, "Thief's Reflexes", "Ambush, escape; Two turns (Initative, Initiative - 10) in first round in any combat.")
		),
		"Assassin" to setOf(
			Klass.Feature(3, "Assassinate", "Description")
		)
	),
	description = """
	A scoundrel who uses stealth and trickery to overcome obstacles and enemies.
	""".trimIndent())

	pc.addKlassLevel(rogue)
	pc.expiriencePoints = 3000 // at leat level 4
	pc.addKlassLevel(rogue, "Assassin")
	pc.addKlassLevel(rogue, "Assassin")
	pc.addKlassLevel(Klass("Multithing"))
	pc.addKlassLevel(Klass("Multithing"))

	pc.setRace(Race("Gnome",
		abilityScoreIncrease = mapOf(Ability.INT to 2),
		age = mapOf("adult" to 40, "dead" to 350, "limit" to 500),
		size = Size.SMALL,
		speed = mapOf("walking" to 25),
		languages = listOf("Common", "Gnomish"),
		darkvision = 60,
		features = listOf(
			Race.Feature("Gnome Cunning", "Advantage on INT, WIS, CHA saves against Magic")
		),
		subrace = mapOf(
			"Forest" to listOf(
				Race.Feature("Natural Illusionist", "Can cast 'Minor Illusion'")),
			"Rock" to listOf(
				Race.Feature("CON + 1", "(Ability Score Increase)")),
			"Stone" to listOf()
		)), newSubrace = "Forest")
	pc.height = 1.99
	pc.weight = 13.0

	pc.speciality = "Libarian"
	pc.trait = "Watch and Learn."
	pc.ideal = "Knowledge."
	pc.bonds = "Protect the weak."
	pc.flaws = "Stupid, hurtful men."

	pc.history += "Born in a gnome village"
	pc.history += "Childhood with loving and caring gnome parents, both libarians"
	pc.history += "Still childhood, Parents left for own small adventure, but disappeared unplanned"
	pc.history += "Raised by village and friend's parent"
	pc.history += "Friend left for better lifestyle without being judged"
	pc.history += "Left to search friend and parents"
	pc.history += "Got abducted, sold into a brothel "
	pc.history += "Run away after learning how to fight back by mysterious elf woman"
	pc.history += "Became Assassin (Rogue)"


	pc.conditions += Condition.UNCONSCIOUS
	pc.deathSavesSuccess()
	pc.deathSavesFail()
	pc.deathSavesFail()
	pc.deathSavesSuccess()
	pc.deathSavesSuccess()

	return pc
}


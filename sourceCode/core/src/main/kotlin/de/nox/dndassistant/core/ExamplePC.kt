package de.nox.dndassistant.core

// private val logger = LoggerFactory.getLogger("ExamplePC")

public fun playgroundWithOnyx() : PlayerCharacter {

	/* Gnome race. */
	val gnome = Race("Gnome",
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
		))

	/* Sage background. */
	val sage = Background(
			name = "Sage",
			proficiencies = listOf(Skill.ARCANA, Skill.HISTORY),
			equipment = listOf(/*bottle,ink,pen,small knife, letter,clothes*/),
			money = Money(gp=10)
		).apply {
			extraLanguages = 2
		}

	val rogue = Klass("Rogue",
		hitdie = D8,
		savingThrows = listOf(Ability.DEX, Ability.INT),
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

	val pc : PlayerCharacter = PlayerCharacter.Creator("Nox")
		.race(gnome, "Forest")
		.background(sage, "Liberian")
		.klass(rogue)
		.name("Onyx (Bigbook) Necklace")
		.ageInYears(21)
		.awake()

	// pc.rollAbilityScores()

	listOf(
		Ability.STR to 6,
		Ability.DEX to 17,
		Ability.CON to 11,
		Ability.INT to 16,
		Ability.WIS to 15,
		Ability.CHA to 10
	).forEach { pc.setAbilityScore(it.first, it.second) }

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

	// logger.debug("Proficient Skills: " + pc.proficiencies)

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

	pc.pickupItem(dagger) // hold dagger.

	val clothes = Clothes("Clothes, Common", 3.0, Money(sp = 5), BodyType.BODY)
	val leather = Armor("Leather Armor", 10.0, Money(gp = 10), Armor.Weight.LIGHT, 11, 0, true)

	val weapons : List<Weapon> = listOf(dagger, spear)

	// TODO (2020-09-03) add proficiency for common/all SIMPLE MELEE WEAPON
	weapons.filter { it.weaponType == Weapon.Type.SIMPLE_MELEE }.forEach {
		pc.addProficiency(it)
	}

	pc.addProficiency(leather) // proficient
	// TODO (2020-09-03) add proficiency for common/all LIGHT ARMOR

	val illusion = Spell(
		name = "Minor Illusion", // name
		school = Spell.School.ILLUSION,

		casting = Spell.Casting(
			duration = "1 action",
			ritual = false,
			verbal = true, somatic = true, materials = mapOf(), // "V,S",
		),

		effects = listOf(
			Spell.Effect(
				level = 0, // school, level
				concentration = false, duration = "1 minute", //, durationSeconds = 60,
				distance = 0 /*ft*/, area = "Spell.Area.CUBE",
				savingThrow = null, // no attack
				result = "Create small visual image",
			)
		),
		casterKlasses = listOf("Sorcerer", "Cleric"),
		description = "Minor Illusion Description!!!! Make an illusion.")

	pc.learnSpell(illusion, "Gnome (Forest)")

	pc.wear(clothes)
	pc.wear(leather)

	pc.dropEquipped(BodyType.ARMOR.name)

	pc.wear(leather)

	pc.hitpoints = 12
	// pc.curHitPoints = 7

	pc.addKlassLevel(rogue)
	pc.experiencePoints = 3000 // at least level 4

	// modifiable
	pc.height = 1.99
	pc.weight = 13.0

	// pc.background.speciality = "Liberian"
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

	return pc
}

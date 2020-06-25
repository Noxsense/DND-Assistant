package de.nox.dndassistant;

fun main() {
	println(
		"DnD Application, display stats and roll your dice!\n"
		+ "Happy Gaming! :D\n"
		+ "==================================================\n");

	val die: SimpleDice = SimpleDice(20);

	for (i in 1..10) {
		println(die.roll());
	}

	for (i in 1..10) {
		print(SimpleDice(1, -1).roll())
		print("; ")
		print(SimpleDice(3, -1).roll())
		print("; ")
		println(SimpleDice(1, -3).roll())
	}

	val diceRegex = "3d8 + d12 - D21 + 3 + 3 - 3"
	val diceTerm = parseDiceTerm(diceRegex)

	println(diceTerm)

	// val diceRegexInvalid = "3d8 + d12 + 3 + 3 - 3 + 12d"
	// parseDiceTerm(diceRegexInvalid)
}

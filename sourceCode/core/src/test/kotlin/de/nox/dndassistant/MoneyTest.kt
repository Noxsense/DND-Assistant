package de.nox.dndassistant

import kotlin.test.Test
import kotlin.test.assertEquals

class MoneyTest {
	private val logger = LoggerFactory.getLogger("MoneyTest")

	@Test
	fun testMoneyInitEmpty() {
		/// initiate empty.
		val purse = Money()

		assertEquals(0, purse.asCopper, "Initiated an empty Purse (normalized)")
		assertEquals(0, purse.pp, "Initiated an empty Purse: Platinum")
		assertEquals(0, purse.gp, "Initiated an empty Purse: Gold")
		assertEquals(0, purse.ep, "Initiated an empty Purse: Electrum")
		assertEquals(0, purse.sp, "Initiated an empty Purse: Silver")
		assertEquals(0, purse.cp, "Initiated an empty Purse: Copper")
	}

	@Test
	fun testMoneyPlus() {
		/// start empty, add 5 silver, 30 gold coins.
		val purse = Money() + Money(sp = 5, gp = 3 * Money.PP_GP /*30*/)

		assertEquals(3050, purse.asCopper, "Plus => (30gp, 5sp) normalized")
		assertEquals(30, purse.gp, "Plus => (30gp, 5sp) Gold")
		assertEquals(5, purse.sp, "Plus => (30gp, 5sp) Silver")
		assertEquals(Money(0, 30, 0, 5, 0), purse, "Plus => (30gp, 5sp) Equal")
	}

	@Test
	fun testMoneyChangeUp() {
		val purse
			= Money(sp = 5, gp = 3 * Money.PP_GP /*30*/)
			.changeUp(Money.GP)

		// change ten coins to one coin: still the same value.
		assertEquals(3050, purse.asCopper, "gp \u2191 pp (normalized)")
		assertEquals(1, purse.pp, "gp \u2191 pp (Platinum, more)")
		assertEquals(20, purse.gp, "gp \u2191 pp (Gold, less)")
		assertEquals(5, purse.sp, "gp \u2191 pp (Silver, unchanged)")
		assertEquals(Money(1, 20, 0, 5, 0), purse, "gp \u2191 pp (Result)")
	}

	@Test
	fun testMoneyChangeDown() {
		val purse
			= Money(sp = 5, gp = 3 * Money.PP_GP /*30*/) // 30gp 5sp
			.changeUp(Money.GP)
			.changeDown(Money.SP)

		// still the same value.
		assertEquals(3050, purse.asCopper, "SP \u2193 CP (normalized)")
		assertEquals(1, purse.pp, "SP \u2193 CP (Platinum, unchanged)")
		assertEquals(20, purse.gp, "SP \u2193 CP (Gold, unchanged)")
		assertEquals(4, purse.sp, "SP \u2193 CP (Silver, less)")
		assertEquals(10, purse.cp, "SP \u2193 CP (Copper, more)")
		assertEquals(Money(1, 20, 0, 4, 10), purse, "SP \u2193 CP (Result)")
	}

	@Test
	fun testElectrum() {
		var purse = Money(ep = 10, ignoreElectrum = true)
		assertEquals(500, purse.asCopper, "Initiated electrum: 10ep == 500cp")

		purse = purse.changeUp(Money.EP)
		assertEquals(500, purse.asCopper, "Change up: EP(10) \u2191 GP, all (2)")
		assertEquals(5, purse.gp, "Change up: EP(10) \u2191 GP, all (2)")
		assertEquals(0, purse.ep, "Change up: EP(10) \u2191 GP, all (2)")

		purse = Money(ep = 10, ignoreElectrum = true)
		assertEquals(500, purse.asCopper, "init: ep:10")

		purse = purse.changeDown(Money.EP)
		assertEquals(500, purse.asCopper, "change down: EP \u2192 CP, all (50)")
		assertEquals(0, purse.ep, "change down: EP \u2192 CP, all (50)")
		assertEquals(50, purse.sp, "change down: EP \u2192 CP, all (50)")
	}

	@Test
	fun testMoneyMinusAbort() {
		val purse = Money(cp = 1) - Money(cp = 2)
		assertEquals(1, purse.asCopper, "Aborted Reduce (normalized)")
		assertEquals(Money(cp = 1), purse, "Aborted Reduce (Result)")
	}

	/* Thoughts
	 * x:02pp 01gp 0ep 99sp 20cp = 3110cp
	 * -: 0pp 25gp 0ep  0sp 25cp ⇒ 2523cp possible
	 * ------------------------------------------------
	 * =:0pp -24gp 0ep  0sp -2cp ⇒ but it is negative
	 *
	 * vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
	 * x: 2pp  1gp 0ep 99sp 20cp
	 * -: 1pp 15gp 0ep  0sp 25cp ⇒ other.changeUp(G)
	 * -: 2pp  5gp 0ep  0sp 25cp ⇒ other.changeUp(G)
	 * -: 2pp  1gp 0ep 40sp 25cp ⇒ other.changeDown(S)
	 * -: 2pp  1gp 0ep 41sp 15cp ⇒ other.changeUp(C)
	 * ================================================
	 * =: 0pp  0gp 0ep 58sp  2cp ⇒ result option
	 */
	@Test
	fun testPayMatchingWithAllCoins() {
		var purse = Money(pp = 10) // 100g
		assertEquals(10000, purse.asCopper, "Initiated: 10pp") // initiated, worth

		purse -= Money(gp = 25)

		assertEquals(10000 - 2500, purse.asCopper, "Bought Bow (25gp): Rest (normalized)")
		assertEquals(Money(pp=7, gp=5), purse, "Bought Bow (25gp): Rest")

		purse = Money(2, 1, 0, 99, 20)

		assertEquals(3110, purse.asCopper, "Initiated: 2pp 99sp 20cp") // initiated, worth

		// by a bow and some arrows (5 arrpws = 100/20cp * 5 = 25cp)
		purse -= Money(0, 25, 0, 0, 25)

		assertEquals(3110 - 2500 - 25, purse.asCopper, "Paid Bow (normalized)")
		assertEquals(Money(sp = 58, cp = 5), purse, "Paid bow (result)")
	}
}

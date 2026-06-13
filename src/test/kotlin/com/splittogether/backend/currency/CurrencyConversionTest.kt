package com.splittogether.backend.currency

import com.splittogether.backend.AbstractIntegrationTest
import com.splittogether.backend.balance.repository.BalanceRepository
import com.splittogether.backend.balance.service.BalanceService
import com.splittogether.backend.expense.dto.CreateExpenseRequest
import com.splittogether.backend.expense.dto.ParticipantRequest
import com.splittogether.backend.expense.dto.UpdateExpenseRequest
import com.splittogether.backend.expense.repository.ExpenseParticipantRepository
import com.splittogether.backend.expense.service.ExpenseService
import com.splittogether.backend.group.dto.CreateGroupRequest
import com.splittogether.backend.group.dto.CreateInvitationRequest
import com.splittogether.backend.group.dto.JoinGroupRequest
import com.splittogether.backend.group.dto.UpdateGroupRequest
import com.splittogether.backend.group.entity.Group
import com.splittogether.backend.group.repository.GroupRepository
import com.splittogether.backend.group.service.GroupService
import com.splittogether.backend.currency.service.ExchangeRateService
import com.splittogether.backend.settlement.dto.CreateSettlementRequest
import com.splittogether.backend.settlement.service.SettlementService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals

class CurrencyConversionTest : AbstractIntegrationTest() {

    @Autowired private lateinit var expenseService: ExpenseService
    @Autowired private lateinit var settlementService: SettlementService
    @Autowired private lateinit var exchangeRateService: ExchangeRateService
    @Autowired private lateinit var balanceService: BalanceService
    @Autowired private lateinit var groupService: GroupService
    @Autowired private lateinit var groupRepository: GroupRepository
    @Autowired private lateinit var expenseParticipantRepository: ExpenseParticipantRepository
    @Autowired private lateinit var balanceRepository: BalanceRepository

    private val date: LocalDate = LocalDate.of(2026, 6, 1)

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun createGroup(ownerId: Long, currency: String = "RUB"): Group {
        groupService.createGroup(ownerId, CreateGroupRequest("Test Group", null, currency))
        return groupRepository.findAll().last()
    }

    private fun joinGroup(userId: Long, ownerId: Long, groupId: Long) {
        val result = groupService.createInvitation(ownerId, groupId, CreateInvitationRequest("LINK"))
        groupService.joinGroup(userId, JoinGroupRequest(result.token!!))
    }

    private fun createExpense(
        payerId: Long, groupId: Long, amount: BigDecimal, participantIds: List<Long>,
        currency: String, expenseDate: LocalDate = date, split: String = "EQUAL",
        participants: List<ParticipantRequest>? = null
    ) = expenseService.createExpense(
        payerId, groupId,
        CreateExpenseRequest(
            title = "Expense", amount = amount, currencyCode = currency,
            splitMethod = split, expenseDate = expenseDate, paidByUserId = payerId,
            participants = participants ?: participantIds.map { ParticipantRequest(it) }
        )
    )

    private fun assertZeroSum(groupId: Long) {
        val net = mutableMapOf<Long, BigDecimal>()
        balanceRepository.findByGroupId(groupId).forEach { b ->
            net[b.creditor.id] = (net[b.creditor.id] ?: BigDecimal.ZERO).add(b.amount)
            net[b.debtor.id] = (net[b.debtor.id] ?: BigDecimal.ZERO).subtract(b.amount)
        }
        val total = net.values.fold(BigDecimal.ZERO, BigDecimal::add)
        assertEquals(0, total.compareTo(BigDecimal.ZERO), "Σ B(i) must be zero, was $total")
    }

    // ── conversion on create ──────────────────────────────────────────────────

    @Test
    fun `createExpense converts share into group base currency on expense date`() {
        val owner = generator.user(email = "owner@test.com")
        val member = generator.user(email = "member@test.com")
        val group = createGroup(owner.id, "RUB")
        joinGroup(member.id, owner.id, group.id)
        stubExchangeRateProvider.setRates(date, mapOf("USD" to BigDecimal("80")))

        val response = createExpense(owner.id, group.id, BigDecimal("100.00"), listOf(owner.id, member.id), "USD")

        val memberShare = response.participants.first { it.userId == member.id }
        assertEquals(BigDecimal("50.00"), memberShare.share)
        assertEquals(BigDecimal("4000.00"), memberShare.baseShare)

        val balances = balanceService.getBalances(owner.id, group.id)
        assertEquals(1, balances.size)
        assertEquals(member.id, balances[0].debtorId)
        assertEquals(owner.id, balances[0].creditorId)
        assertEquals(BigDecimal("4000.00"), balances[0].amount)
        assertZeroSum(group.id)
    }

    @Test
    fun `mixed currency operations preserve zero sum`() {
        val owner = generator.user(email = "owner@test.com")
        val m1 = generator.user(email = "m1@test.com")
        val m2 = generator.user(email = "m2@test.com")
        val group = createGroup(owner.id, "RUB")
        joinGroup(m1.id, owner.id, group.id)
        joinGroup(m2.id, owner.id, group.id)
        stubExchangeRateProvider.setRates(date, mapOf("USD" to BigDecimal("80"), "EUR" to BigDecimal("90")))

        createExpense(owner.id, group.id, BigDecimal("100.00"), listOf(owner.id, m1.id, m2.id), "USD")
        createExpense(m1.id, group.id, BigDecimal("50.00"), listOf(owner.id, m1.id, m2.id), "EUR")
        createExpense(m2.id, group.id, BigDecimal("33.00"), listOf(owner.id, m1.id, m2.id), "RUB")

        assertZeroSum(group.id)
    }

    @Test
    fun `settlement converts amount on settlement date and preserves zero sum`() {
        val payer = generator.user(email = "payer@test.com")
        val receiver = generator.user(email = "receiver@test.com")
        val group = createGroup(payer.id, "RUB")
        joinGroup(receiver.id, payer.id, group.id)
        stubExchangeRateProvider.setRates(date, mapOf("USD" to BigDecimal("80")))

        createExpense(receiver.id, group.id, BigDecimal("100.00"), listOf(payer.id, receiver.id), "USD")

        val settlement = settlementService.createSettlement(
            payer.id, group.id,
            CreateSettlementRequest(receiverId = receiver.id, amount = BigDecimal("50.00"), currencyCode = "USD", settlementDate = date)
        )
        val confirmed = settlementService.confirmSettlement(receiver.id, group.id, settlement.id)

        assertEquals(BigDecimal("4000.00"), confirmed.baseAmount)
        assertEquals("RUB", confirmed.baseCurrencyCode)
        assertEquals(0, balanceRepository.findByGroupId(group.id).size)
        assertZeroSum(group.id)
    }

    // ── reverse uses stored base_share, not a re-conversion ────────────────────

    @Test
    fun `updateExpense reverses by stored base share even if rate changed`() {
        val owner = generator.user(email = "owner@test.com")
        val member = generator.user(email = "member@test.com")
        val group = createGroup(owner.id, "RUB")
        joinGroup(member.id, owner.id, group.id)
        stubExchangeRateProvider.setRates(date, mapOf("USD" to BigDecimal("80")))

        val created = createExpense(owner.id, group.id, BigDecimal("100.00"), listOf(owner.id, member.id), "USD")

        stubExchangeRateProvider.setRates(date, mapOf("USD" to BigDecimal("90")))
        exchangeRateService.refreshRates(date)
        expenseService.updateExpense(
            owner.id, group.id, created.id,
            UpdateExpenseRequest(
                title = "Expense", description = null, amount = BigDecimal("100.00"), currencyCode = "USD",
                categoryCode = null, splitMethod = "EQUAL", expenseDate = date, paidByUserId = owner.id,
                participants = listOf(ParticipantRequest(owner.id), ParticipantRequest(member.id))
            )
        )

        val balances = balanceService.getBalances(owner.id, group.id)
        assertEquals(1, balances.size)
        assertEquals(BigDecimal("4500.00"), balances[0].amount)
        assertZeroSum(group.id)
    }

    @Test
    fun `deleteExpense reverses by stored base share even if rate changed`() {
        val owner = generator.user(email = "owner@test.com")
        val member = generator.user(email = "member@test.com")
        val group = createGroup(owner.id, "RUB")
        joinGroup(member.id, owner.id, group.id)
        stubExchangeRateProvider.setRates(date, mapOf("USD" to BigDecimal("80")))

        val created = createExpense(owner.id, group.id, BigDecimal("100.00"), listOf(owner.id, member.id), "USD")

        stubExchangeRateProvider.setRates(date, mapOf("USD" to BigDecimal("90")))
        exchangeRateService.refreshRates(date)
        expenseService.deleteExpense(owner.id, group.id, created.id)

        assertEquals(0, balanceRepository.findByGroupId(group.id).size)
        assertZeroSum(group.id)
    }

    // ── group currency change → full rebuild ───────────────────────────────────

    @Test
    fun `changing group currency rebuilds balances and base shares`() {
        val owner = generator.user(email = "owner@test.com")
        val member = generator.user(email = "member@test.com")
        val group = createGroup(owner.id, "RUB")
        joinGroup(member.id, owner.id, group.id)
        stubExchangeRateProvider.setRates(date, mapOf("USD" to BigDecimal("80")))

        val created = createExpense(owner.id, group.id, BigDecimal("100.00"), listOf(owner.id, member.id), "RUB")
        assertEquals(BigDecimal("50.00"), balanceService.getBalances(owner.id, group.id)[0].amount)

        groupService.updateGroup(owner.id, group.id, UpdateGroupRequest("Test Group", null, "USD"))

        val balances = balanceService.getBalances(owner.id, group.id)
        assertEquals(1, balances.size)
        assertEquals(member.id, balances[0].debtorId)
        assertEquals(BigDecimal("0.63"), balances[0].amount)
        assertZeroSum(group.id)

        val memberShare = expenseParticipantRepository.findByExpenseId(created.id).first { it.user.id == member.id }
        assertEquals(BigDecimal("0.63"), memberShare.baseShare)
    }

    @Test
    fun `rebuild after currency change accounts for confirmed settlements`() {
        val owner = generator.user(email = "owner@test.com")
        val m1 = generator.user(email = "m1@test.com")
        val m2 = generator.user(email = "m2@test.com")
        val group = createGroup(owner.id, "RUB")
        joinGroup(m1.id, owner.id, group.id)
        joinGroup(m2.id, owner.id, group.id)
        stubExchangeRateProvider.setRates(date, mapOf("USD" to BigDecimal("80")))

        createExpense(owner.id, group.id, BigDecimal("90.00"), listOf(owner.id, m1.id, m2.id), "RUB")
        val settlement = settlementService.createSettlement(
            m1.id, group.id,
            CreateSettlementRequest(receiverId = owner.id, amount = BigDecimal("30.00"), currencyCode = "RUB", settlementDate = date)
        )
        settlementService.confirmSettlement(owner.id, group.id, settlement.id)

        groupService.updateGroup(owner.id, group.id, UpdateGroupRequest("Test Group", null, "USD"))

        val balances = balanceService.getBalances(owner.id, group.id)
        assertEquals(1, balances.size)
        assertEquals(m2.id, balances[0].debtorId)
        assertEquals(BigDecimal("0.38"), balances[0].amount)
        assertZeroSum(group.id)
    }

    @Test
    fun `changing group currency to same code is a no-op`() {
        val owner = generator.user(email = "owner@test.com")
        val member = generator.user(email = "member@test.com")
        val group = createGroup(owner.id, "RUB")
        joinGroup(member.id, owner.id, group.id)
        createExpense(owner.id, group.id, BigDecimal("100.00"), listOf(owner.id, member.id), "RUB")

        groupService.updateGroup(owner.id, group.id, UpdateGroupRequest("Renamed", null, "RUB"))

        val balances = balanceService.getBalances(owner.id, group.id)
        assertEquals(BigDecimal("50.00"), balances[0].amount)
    }

    // ── display currency (?currency=) ──────────────────────────────────────────

    @Test
    fun `getBalances converts to requested display currency without changing storage`() {
        val owner = generator.user(email = "owner@test.com")
        val member = generator.user(email = "member@test.com")
        val group = createGroup(owner.id, "RUB")
        joinGroup(member.id, owner.id, group.id)
        createExpense(owner.id, group.id, BigDecimal("100.00"), listOf(owner.id, member.id), "RUB")
        stubExchangeRateProvider.setRates(LocalDate.now(), mapOf("USD" to BigDecimal("80")))
        exchangeRateService.refreshRates(LocalDate.now())

        val inUsd = balanceService.getBalances(owner.id, group.id, "USD")
        assertEquals("USD", inUsd[0].currencyCode)
        assertEquals(BigDecimal("0.63"), inUsd[0].amount)

        val inGroup = balanceService.getBalances(owner.id, group.id)
        assertEquals("RUB", inGroup[0].currencyCode)
        assertEquals(BigDecimal("50.00"), inGroup[0].amount)
    }

    @Test
    fun `getUserBalance aggregates across groups of different currencies into RUB`() {
        val a = generator.user(email = "a@test.com")
        val b = generator.user(email = "b@test.com")

        val rubGroup = createGroup(b.id, "RUB")
        joinGroup(a.id, b.id, rubGroup.id)
        createExpense(b.id, rubGroup.id, BigDecimal("100.00"), listOf(a.id, b.id), "RUB")

        val usdGroup = createGroup(b.id, "USD")
        joinGroup(a.id, b.id, usdGroup.id)
        createExpense(b.id, usdGroup.id, BigDecimal("100.00"), listOf(a.id, b.id), "USD")

        stubExchangeRateProvider.setRates(LocalDate.now(), mapOf("USD" to BigDecimal("80")))
        exchangeRateService.refreshRates(LocalDate.now())

        val balance = balanceService.getUserBalance(a.id)
        assertEquals("RUB", balance.currencyCode)
        assertEquals(BigDecimal("0.00"), balance.totalOwed)
        assertEquals(BigDecimal("4050.00"), balance.totalOwing)
        assertEquals(BigDecimal("-4050.00"), balance.netBalance)
    }

    // ── rounding edge case ─────────────────────────────────────────────────────

    @Test
    fun `cross currency exact split preserves zero sum with awkward rate`() {
        val owner = generator.user(email = "owner@test.com")
        val m1 = generator.user(email = "m1@test.com")
        val m2 = generator.user(email = "m2@test.com")
        val group = createGroup(owner.id, "RUB")
        joinGroup(m1.id, owner.id, group.id)
        joinGroup(m2.id, owner.id, group.id)
        stubExchangeRateProvider.setRates(date, mapOf("KZT" to BigDecimal("0.152591")))

        createExpense(
            owner.id, group.id, BigDecimal("10.00"), emptyList(), "KZT", split = "EXACT",
            participants = listOf(
                ParticipantRequest(owner.id, exactAmount = BigDecimal("3.34")),
                ParticipantRequest(m1.id, exactAmount = BigDecimal("3.33")),
                ParticipantRequest(m2.id, exactAmount = BigDecimal("3.33"))
            )
        )

        assertZeroSum(group.id)
    }
}

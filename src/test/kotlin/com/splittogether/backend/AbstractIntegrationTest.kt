package com.splittogether.backend

import com.splittogether.backend.auth.repository.EmailVerificationRepository
import com.splittogether.backend.auth.repository.RefreshTokenRepository
import com.splittogether.backend.balance.repository.BalanceRepository
import com.splittogether.backend.expense.repository.ExpenseParticipantRepository
import com.splittogether.backend.expense.repository.ExpenseRepository
import com.splittogether.backend.friendship.repository.FriendshipRepository
import com.splittogether.backend.group.repository.GroupInvitationRepository
import com.splittogether.backend.group.repository.GroupMemberRepository
import com.splittogether.backend.group.repository.GroupRepository
import com.splittogether.backend.group.repository.InvitationUseRepository
import com.splittogether.backend.settlement.repository.SettlementRepository
import com.splittogether.backend.user.repository.UserRepository
import com.splittogether.backend.email.CapturingMailSender
import com.splittogether.backend.generator.Generator
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(
    properties = [
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false"
    ]
)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestcontainersConfig::class)
abstract class AbstractIntegrationTest {

    @Autowired protected lateinit var generator: Generator
    @Autowired protected lateinit var capturingMailSender: CapturingMailSender

    @Autowired private lateinit var invitationUseRepository: InvitationUseRepository
    @Autowired private lateinit var groupInvitationRepository: GroupInvitationRepository
    @Autowired private lateinit var expenseParticipantRepository: ExpenseParticipantRepository
    @Autowired private lateinit var balanceRepository: BalanceRepository
    @Autowired private lateinit var expenseRepository: ExpenseRepository
    @Autowired private lateinit var settlementRepository: SettlementRepository
    @Autowired private lateinit var groupMemberRepository: GroupMemberRepository
    @Autowired private lateinit var groupRepository: GroupRepository
    @Autowired private lateinit var emailVerificationRepository: EmailVerificationRepository
    @Autowired private lateinit var refreshTokenRepository: RefreshTokenRepository
    @Autowired private lateinit var friendshipRepository: FriendshipRepository
    @Autowired private lateinit var userRepository: UserRepository

    @BeforeEach
    fun cleanDatabase() {
        capturingMailSender.clear()
        invitationUseRepository.deleteAll()
        groupInvitationRepository.deleteAll()
        expenseParticipantRepository.deleteAll()
        balanceRepository.deleteAll()
        expenseRepository.deleteAll()
        settlementRepository.deleteAll()
        groupMemberRepository.deleteAll()
        groupRepository.deleteAll()
        emailVerificationRepository.deleteAll()
        refreshTokenRepository.deleteAll()
        friendshipRepository.deleteAll()
        userRepository.deleteAll()
    }
}

package com.splittogether.backend

import com.splittogether.backend.auth.repository.EmailVerificationRepository
import com.splittogether.backend.auth.repository.RefreshTokenRepository
import com.splittogether.backend.group.repository.GroupInvitationRepository
import com.splittogether.backend.group.repository.GroupMemberRepository
import com.splittogether.backend.group.repository.GroupRepository
import com.splittogether.backend.group.repository.InvitationUseRepository
import com.splittogether.backend.user.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestcontainersConfig::class)
abstract class AbstractIntegrationTest {

    @Autowired private lateinit var invitationUseRepository: InvitationUseRepository
    @Autowired private lateinit var groupInvitationRepository: GroupInvitationRepository
    @Autowired private lateinit var groupMemberRepository: GroupMemberRepository
    @Autowired private lateinit var groupRepository: GroupRepository
    @Autowired private lateinit var emailVerificationRepository: EmailVerificationRepository
    @Autowired private lateinit var refreshTokenRepository: RefreshTokenRepository
    @Autowired private lateinit var userRepository: UserRepository

    @BeforeEach
    fun cleanDatabase() {
        invitationUseRepository.deleteAll()
        groupInvitationRepository.deleteAll()
        groupMemberRepository.deleteAll()
        groupRepository.deleteAll()
        emailVerificationRepository.deleteAll()
        refreshTokenRepository.deleteAll()
        userRepository.deleteAll()
    }
}

package com.splittogether.backend.generator

import com.splittogether.backend.user.repository.GroupInvitePolicyRepository
import com.splittogether.backend.user.repository.SearchVisibilityRepository
import com.splittogether.backend.user.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class Generator(
    val userRepository: UserRepository,
    val searchVisibilityRepository: SearchVisibilityRepository,
    val groupInvitePolicyRepository: GroupInvitePolicyRepository,
    private val passwordEncoder: PasswordEncoder
) : UserGenerator {

    override val generator: Generator get() = this

    val defaultPassword: String = "Password1!"
    val passwordHash: String by lazy { passwordEncoder.encode(defaultPassword)!! }

    fun <T> make(block: Generator.() -> T): T = block()
}

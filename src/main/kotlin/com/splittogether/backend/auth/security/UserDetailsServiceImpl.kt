package com.splittogether.backend.auth.security

import com.splittogether.backend.user.repository.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class UserDetailsServiceImpl(
    private val userRepository: UserRepository
) : UserDetailsService {

    override fun loadUserByUsername(email: String): AppUserDetails {
        val user = userRepository.findByEmail(email)
            ?: throw UsernameNotFoundException("User not found: $email")

        return AppUserDetails(
            userId = user.id,
            email = user.email,
            password = user.passwordHash,
            authorities = user.platformRoles.map { SimpleGrantedAuthority("ROLE_${it.code}") }
        )
    }
}

package com.splittogether.backend.generator

import com.splittogether.backend.user.entity.GroupInvitePolicy
import com.splittogether.backend.user.entity.SearchVisibility
import com.splittogether.backend.user.entity.User
import java.time.Instant

interface GeneratorBase {
    val generator: Generator
}

interface UserGenerator : GeneratorBase {
    fun user(
        email: String = "user@test.com",
        displayName: String = "Test User",
        emailVerified: Boolean = true,
        searchVisibility: String = SearchVisibility.EVERYONE,
        groupInvitePolicy: String = GroupInvitePolicy.ANYONE
    ): User = generator.make {
        userRepository.save(
            User(
                email = email,
                passwordHash = passwordHash,
                displayName = displayName,
                emailVerifiedAt = if (emailVerified) Instant.now() else null,
                searchVisibility = searchVisibilityRepository.findByCode(searchVisibility)!!,
                groupInvitePolicy = groupInvitePolicyRepository.findByCode(groupInvitePolicy)!!
            )
        )
    }
}

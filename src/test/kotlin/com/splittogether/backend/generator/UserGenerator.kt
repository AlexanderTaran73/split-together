package com.splittogether.backend.generator

import com.splittogether.backend.user.entity.User

interface GeneratorBase {
    val generator: Generator
}

interface UserGenerator : GeneratorBase {
    fun user(
        email: String = "user@test.com",
        displayName: String = "Test User",
        emailVerified: Boolean = true
    ): User = generator.make {
        userRepository.save(
            User(email = email, passwordHash = passwordHash, displayName = displayName, emailVerified = emailVerified)
        )
    }
}

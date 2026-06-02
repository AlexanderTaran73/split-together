package com.splittogether.backend.common.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "platform_roles")
class PlatformRole(
    @Id val id: Int = 0,
    @Column(nullable = false, unique = true) val code: String,
    @Column(nullable = false) val name: String
) {
    companion object {
        const val USER = "USER"
        const val ADMIN = "ADMIN"
    }
}

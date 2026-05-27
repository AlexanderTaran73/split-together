package com.splittogether.backend.common.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "email_verification_purposes")
class EmailVerificationPurpose(
    @Id val id: Int = 0,
    @Column(nullable = false, unique = true) val code: String,
    @Column(nullable = false) val name: String
)

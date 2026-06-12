package com.splittogether.backend.settlement.entity

import jakarta.persistence.*

@Entity
@Table(name = "settlement_statuses")
class SettlementStatus(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Column(nullable = false, unique = true)
    val code: String,

    @Column(nullable = false)
    val name: String
) {
    companion object {
        const val PENDING   = "PENDING"
        const val CONFIRMED = "CONFIRMED"
        const val REJECTED  = "REJECTED"
    }
}

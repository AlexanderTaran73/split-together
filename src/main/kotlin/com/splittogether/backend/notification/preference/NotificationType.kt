package com.splittogether.backend.notification.preference

import jakarta.persistence.*

@Entity
@Table(name = "notification_types")
class NotificationType(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Column(nullable = false, unique = true)
    val code: String,

    @Column(nullable = false)
    val name: String
) {
    companion object {
        const val GROUP_INVITATION = "GROUP_INVITATION"
        const val EXPENSE_ADDED = "EXPENSE_ADDED"
        const val SETTLEMENT_REQUESTED = "SETTLEMENT_REQUESTED"
        const val SETTLEMENT_CONFIRMED = "SETTLEMENT_CONFIRMED"
    }
}

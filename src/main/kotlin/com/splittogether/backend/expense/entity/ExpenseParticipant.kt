package com.splittogether.backend.expense.entity

import com.splittogether.backend.user.entity.User
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "expense_participants")
class ExpenseParticipant(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false)
    val expense: Expense,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false, precision = 19, scale = 2)
    var share: BigDecimal,

    @Column(name = "base_share", nullable = false, precision = 19, scale = 2)
    var baseShare: BigDecimal,

    @Column(precision = 10, scale = 4)
    var weight: BigDecimal? = null,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "confirmation_status_id", nullable = false)
    var confirmationStatus: ExpenseConfirmationStatus,

    @Column(name = "dispute_reason", length = 500)
    var disputeReason: String? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)

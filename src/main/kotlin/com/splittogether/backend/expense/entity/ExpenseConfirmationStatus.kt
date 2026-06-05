package com.splittogether.backend.expense.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "expense_confirmation_statuses")
class ExpenseConfirmationStatus(
    @Id val id: Int = 0,
    @Column(nullable = false, unique = true) val code: String,
    @Column(nullable = false) val name: String
) {
    companion object {
        const val PENDING   = "PENDING"
        const val CONFIRMED = "CONFIRMED"
        const val DISPUTED  = "DISPUTED"
    }
}

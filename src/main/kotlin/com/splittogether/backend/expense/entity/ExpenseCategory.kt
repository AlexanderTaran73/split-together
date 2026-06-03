package com.splittogether.backend.expense.entity

import jakarta.persistence.*

@Entity
@Table(name = "expense_categories")
class ExpenseCategory(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Column(nullable = false, unique = true)
    val code: String,

    @Column(nullable = false)
    val name: String
)

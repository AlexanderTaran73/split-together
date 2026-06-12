package com.splittogether.backend.expense.entity

import jakarta.persistence.*

@Entity
@Table(name = "split_methods")
class SplitMethod(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Column(nullable = false, unique = true)
    val code: String,

    @Column(nullable = false)
    val name: String
) {
    companion object {
        const val EQUAL = "EQUAL"
        const val SHARES = "SHARES"
        const val EXACT = "EXACT"
    }
}

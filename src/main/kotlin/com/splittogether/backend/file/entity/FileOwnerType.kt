package com.splittogether.backend.file.entity

import jakarta.persistence.*

@Entity
@Table(name = "file_owner_types")
class FileOwnerType(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Column(nullable = false, unique = true)
    val code: String,

    @Column(nullable = false)
    val name: String
) {
    companion object {
        const val GROUP = "GROUP"
        const val EXPENSE = "EXPENSE"
    }
}

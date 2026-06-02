package com.splittogether.backend.group.entity

import jakarta.persistence.*

@Entity
@Table(name = "group_statuses")
class GroupStatus(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Column(nullable = false, unique = true)
    val code: String,

    @Column(nullable = false)
    val name: String
) {
    companion object {
        const val ACTIVE = "ACTIVE"
        const val ARCHIVED = "ARCHIVED"
    }
}

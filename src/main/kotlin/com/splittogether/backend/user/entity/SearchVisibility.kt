package com.splittogether.backend.user.entity

import jakarta.persistence.*

@Entity
@Table(name = "search_visibilities")
class SearchVisibility(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Column(nullable = false, unique = true)
    val code: String,

    @Column(nullable = false)
    val name: String
) {
    companion object {
        const val EVERYONE = "EVERYONE"
        const val FRIENDS = "FRIENDS"
        const val NOBODY = "NOBODY"
    }
}

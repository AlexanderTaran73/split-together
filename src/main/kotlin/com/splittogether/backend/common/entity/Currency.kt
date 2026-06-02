package com.splittogether.backend.common.entity

import jakarta.persistence.*

@Entity
@Table(name = "currencies")
class Currency(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Column(nullable = false, unique = true)
    val code: String,

    @Column(nullable = false)
    val name: String
)

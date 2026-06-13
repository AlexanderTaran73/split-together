package com.splittogether.backend.currency.entity

import com.splittogether.backend.common.entity.Currency
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "exchange_rates")
class ExchangeRate(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_id", nullable = false)
    val currency: Currency,

    @Column(name = "rate_date", nullable = false)
    val rateDate: LocalDate,

    @Column(nullable = false, precision = 19, scale = 6)
    var rate: BigDecimal,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)

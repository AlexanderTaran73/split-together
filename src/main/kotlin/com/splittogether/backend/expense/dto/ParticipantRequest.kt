package com.splittogether.backend.expense.dto

import java.math.BigDecimal

data class ParticipantRequest(
    val userId: Long,
    val weight: BigDecimal? = null,
    val exactAmount: BigDecimal? = null
)

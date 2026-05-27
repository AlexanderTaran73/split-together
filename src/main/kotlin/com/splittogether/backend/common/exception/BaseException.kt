package com.splittogether.backend.common.exception

import org.springframework.http.HttpStatus

abstract class BaseException(
    message: String,
    val status: HttpStatus
) : RuntimeException(message)

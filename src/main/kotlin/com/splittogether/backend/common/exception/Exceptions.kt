package com.splittogether.backend.common.exception

import org.springframework.http.HttpStatus

class EmailAlreadyExistsException(message: String) :
    BaseException(message, HttpStatus.CONFLICT)

class EmailNotVerifiedException(message: String) :
    BaseException(message, HttpStatus.FORBIDDEN)

class EmailAlreadyVerifiedException(message: String) :
    BaseException(message, HttpStatus.CONFLICT)

class InvalidCredentialsException(message: String) :
    BaseException(message, HttpStatus.UNAUTHORIZED)

class InvalidTokenException(message: String) :
    BaseException(message, HttpStatus.UNAUTHORIZED)

class InvalidVerificationCodeException(message: String) :
    BaseException(message, HttpStatus.BAD_REQUEST)

class UserNotFoundException(message: String) :
    BaseException(message, HttpStatus.NOT_FOUND)

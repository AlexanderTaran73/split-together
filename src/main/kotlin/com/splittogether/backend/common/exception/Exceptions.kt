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

class GroupNotFoundException(message: String) :
    BaseException(message, HttpStatus.NOT_FOUND)

class InvitationNotFoundException(message: String) :
    BaseException(message, HttpStatus.NOT_FOUND)

class CurrencyNotFoundException(message: String) :
    BaseException(message, HttpStatus.NOT_FOUND)

class NotGroupMemberException(message: String) :
    BaseException(message, HttpStatus.FORBIDDEN)

class InsufficientPermissionsException(message: String) :
    BaseException(message, HttpStatus.FORBIDDEN)

class AlreadyGroupMemberException(message: String) :
    BaseException(message, HttpStatus.CONFLICT)

class GroupArchivedException(message: String) :
    BaseException(message, HttpStatus.CONFLICT)

class InvalidInvitationException(message: String) :
    BaseException(message, HttpStatus.BAD_REQUEST)

class CannotRemoveOwnerException(message: String) :
    BaseException(message, HttpStatus.CONFLICT)

class ExpenseNotFoundException(message: String) :
    BaseException(message, HttpStatus.NOT_FOUND)

class InvalidExpenseException(message: String) :
    BaseException(message, HttpStatus.BAD_REQUEST)

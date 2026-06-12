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

class SettlementNotFoundException(message: String) :
    BaseException(message, HttpStatus.NOT_FOUND)

class InvalidSettlementException(message: String) :
    BaseException(message, HttpStatus.BAD_REQUEST)

class FriendshipNotFoundException(message: String) :
    BaseException(message, HttpStatus.NOT_FOUND)

class InvalidFriendRequestException(message: String) :
    BaseException(message, HttpStatus.BAD_REQUEST)

class AlreadyFriendsException(message: String) :
    BaseException(message, HttpStatus.CONFLICT)

class UserBlockedException(message: String) :
    BaseException(message, HttpStatus.FORBIDDEN)

class CannotInviteUserException(message: String) :
    BaseException(message, HttpStatus.FORBIDDEN)

class InvalidPrivacySettingException(message: String) :
    BaseException(message, HttpStatus.BAD_REQUEST)

class InvalidFileException(message: String) :
    BaseException(message, HttpStatus.BAD_REQUEST)

class FileTooLargeException(message: String) :
    BaseException(message, HttpStatus.PAYLOAD_TOO_LARGE)

class UnsupportedFileTypeException(message: String) :
    BaseException(message, HttpStatus.UNSUPPORTED_MEDIA_TYPE)

class StoredFileNotFoundException(message: String) :
    BaseException(message, HttpStatus.NOT_FOUND)

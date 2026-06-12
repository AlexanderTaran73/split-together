package com.splittogether.backend.common.exception

import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.multipart.MaxUploadSizeExceededException
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(BaseException::class)
    fun handleBase(ex: BaseException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(ex.status).body(ErrorResponse(ex.status.value(), ex.message ?: "Error"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message = ex.bindingResult.fieldErrors
            .joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity.badRequest().body(ErrorResponse(400, message))
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(ex: ConstraintViolationException): ResponseEntity<ErrorResponse> {
        val message = ex.constraintViolations.joinToString("; ") { it.message }
        return ResponseEntity.badRequest().body(ErrorResponse(400, message))
    }

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxUpload(ex: MaxUploadSizeExceededException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body(ErrorResponse(413, "Uploaded file is too large"))

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ResponseEntity<ErrorResponse> =
        ResponseEntity.internalServerError().body(ErrorResponse(500, "Internal server error"))
}

data class ErrorResponse(
    val status: Int,
    val message: String,
    val timestamp: Instant = Instant.now()
)

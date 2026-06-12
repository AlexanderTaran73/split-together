package com.splittogether.backend.storage.service

import com.splittogether.backend.common.exception.FileTooLargeException
import com.splittogether.backend.common.exception.InvalidFileException
import com.splittogether.backend.common.exception.UnsupportedFileTypeException
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

@Component
class FileValidator {

    fun validate(
        file: MultipartFile,
        maxBytes: Long,
        contentTypeAllowed: (String) -> Boolean = { true }
    ) {
        if (file.isEmpty) throw InvalidFileException("File is empty")
        if (file.size > maxBytes)
            throw FileTooLargeException("File exceeds the maximum allowed size of ${maxBytes / (1024 * 1024)} MB")
        val contentType = file.contentType
        if (contentType == null || !contentTypeAllowed(contentType))
            throw UnsupportedFileTypeException("Unsupported file type: ${contentType ?: "unknown"}")
    }
}

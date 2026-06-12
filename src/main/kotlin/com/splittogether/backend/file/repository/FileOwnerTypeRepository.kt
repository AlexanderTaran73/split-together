package com.splittogether.backend.file.repository

import com.splittogether.backend.file.entity.FileOwnerType
import org.springframework.data.jpa.repository.JpaRepository

interface FileOwnerTypeRepository : JpaRepository<FileOwnerType, Int> {
    fun findByCode(code: String): FileOwnerType?
}

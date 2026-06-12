package com.splittogether.backend.file.repository

import com.splittogether.backend.file.entity.StoredFile
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface StoredFileRepository : JpaRepository<StoredFile, Long> {

    @Query(
        "SELECT f FROM StoredFile f WHERE f.ownerType.code = :ownerTypeCode AND f.ownerId = :ownerId " +
            "AND f.deletedAt IS NULL ORDER BY f.createdAt DESC"
    )
    fun findActiveByOwner(ownerTypeCode: String, ownerId: Long): List<StoredFile>

    @Query("SELECT f FROM StoredFile f WHERE f.id = :id AND f.deletedAt IS NULL")
    fun findActiveById(id: Long): StoredFile?
}

package com.splittogether.backend.user.repository

import com.splittogether.backend.user.entity.SearchVisibility
import org.springframework.data.jpa.repository.JpaRepository

interface SearchVisibilityRepository : JpaRepository<SearchVisibility, Int> {
    fun findByCode(code: String): SearchVisibility?
}

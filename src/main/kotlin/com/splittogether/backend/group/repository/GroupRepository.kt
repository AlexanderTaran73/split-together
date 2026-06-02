package com.splittogether.backend.group.repository

import com.splittogether.backend.group.entity.Group
import org.springframework.data.jpa.repository.JpaRepository

interface GroupRepository : JpaRepository<Group, Long>

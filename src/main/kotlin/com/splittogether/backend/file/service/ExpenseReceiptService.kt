package com.splittogether.backend.file.service

import com.splittogether.backend.common.exception.ExpenseNotFoundException
import com.splittogether.backend.file.dto.FileResponse
import com.splittogether.backend.file.entity.FileOwnerType
import com.splittogether.backend.expense.repository.ExpenseRepository
import com.splittogether.backend.group.entity.GroupRole
import com.splittogether.backend.group.service.MembershipGuard
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Service
class ExpenseReceiptService(
    private val fileService: FileService,
    private val membershipGuard: MembershipGuard,
    private val expenseRepository: ExpenseRepository
) {

    fun upload(userId: Long, groupId: Long, expenseId: Long, file: MultipartFile, description: String?): FileResponse {
        membershipGuard.requireActiveMember(groupId, userId)
        requireExpenseInGroup(groupId, expenseId)
        return fileService.upload(
            FileOwnerType.EXPENSE, expenseId, userId, file, description,
            "groups/$groupId/expenses/$expenseId/receipts"
        )
    }

    fun list(userId: Long, groupId: Long, expenseId: Long): List<FileResponse> {
        membershipGuard.requireActiveMember(groupId, userId)
        requireExpenseInGroup(groupId, expenseId)
        return fileService.list(FileOwnerType.EXPENSE, expenseId)
    }

    fun get(userId: Long, groupId: Long, expenseId: Long, fileId: Long): FileResponse {
        membershipGuard.requireActiveMember(groupId, userId)
        requireExpenseInGroup(groupId, expenseId)
        return fileService.get(FileOwnerType.EXPENSE, expenseId, fileId)
    }

    fun delete(userId: Long, groupId: Long, expenseId: Long, fileId: Long) {
        val member = membershipGuard.requireActiveMember(groupId, userId)
        requireExpenseInGroup(groupId, expenseId)
        val canManage = member.role.code != GroupRole.MEMBER
        fileService.softDelete(FileOwnerType.EXPENSE, expenseId, fileId, userId, canManage)
    }

    private fun requireExpenseInGroup(groupId: Long, expenseId: Long) {
        val expense = expenseRepository.findById(expenseId)
            .orElseThrow { ExpenseNotFoundException("Expense not found") }
        if (expense.group.id != groupId || expense.deletedAt != null)
            throw ExpenseNotFoundException("Expense not found")
    }
}

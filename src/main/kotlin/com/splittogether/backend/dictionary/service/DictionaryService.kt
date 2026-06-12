package com.splittogether.backend.dictionary.service

import com.splittogether.backend.common.repository.CurrencyRepository
import com.splittogether.backend.dictionary.dto.DictionaryItemResponse
import com.splittogether.backend.expense.repository.ExpenseCategoryRepository
import com.splittogether.backend.expense.repository.SplitMethodRepository
import com.splittogether.backend.friendship.repository.FriendshipStatusRepository
import com.splittogether.backend.user.repository.GroupInvitePolicyRepository
import com.splittogether.backend.user.repository.SearchVisibilityRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DictionaryService(
    private val currencyRepository: CurrencyRepository,
    private val expenseCategoryRepository: ExpenseCategoryRepository,
    private val splitMethodRepository: SplitMethodRepository,
    private val friendshipStatusRepository: FriendshipStatusRepository,
    private val searchVisibilityRepository: SearchVisibilityRepository,
    private val groupInvitePolicyRepository: GroupInvitePolicyRepository
) {

    @Transactional(readOnly = true)
    fun getCurrencies(): List<DictionaryItemResponse> =
        currencyRepository.findAll().map { DictionaryItemResponse(it.code, it.name) }

    @Transactional(readOnly = true)
    fun getExpenseCategories(): List<DictionaryItemResponse> =
        expenseCategoryRepository.findAll().map { DictionaryItemResponse(it.code, it.name) }

    @Transactional(readOnly = true)
    fun getSplitMethods(): List<DictionaryItemResponse> =
        splitMethodRepository.findAll().map { DictionaryItemResponse(it.code, it.name) }

    @Transactional(readOnly = true)
    fun getFriendshipStatuses(): List<DictionaryItemResponse> =
        friendshipStatusRepository.findAll().map { DictionaryItemResponse(it.code, it.name) }

    @Transactional(readOnly = true)
    fun getSearchVisibilities(): List<DictionaryItemResponse> =
        searchVisibilityRepository.findAll().map { DictionaryItemResponse(it.code, it.name) }

    @Transactional(readOnly = true)
    fun getGroupInvitePolicies(): List<DictionaryItemResponse> =
        groupInvitePolicyRepository.findAll().map { DictionaryItemResponse(it.code, it.name) }
}

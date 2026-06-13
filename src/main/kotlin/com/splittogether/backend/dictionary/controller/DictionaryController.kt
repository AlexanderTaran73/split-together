package com.splittogether.backend.dictionary.controller

import com.splittogether.backend.dictionary.dto.DictionaryItemResponse
import com.splittogether.backend.dictionary.service.DictionaryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/dictionaries")
class DictionaryController(private val dictionaryService: DictionaryService) {

    @GetMapping("/currencies")
    fun getCurrencies(): ResponseEntity<List<DictionaryItemResponse>> =
        ResponseEntity.ok(dictionaryService.getCurrencies())

    @GetMapping("/expense-categories")
    fun getExpenseCategories(): ResponseEntity<List<DictionaryItemResponse>> =
        ResponseEntity.ok(dictionaryService.getExpenseCategories())

    @GetMapping("/split-methods")
    fun getSplitMethods(): ResponseEntity<List<DictionaryItemResponse>> =
        ResponseEntity.ok(dictionaryService.getSplitMethods())

    @GetMapping("/friendship-statuses")
    fun getFriendshipStatuses(): ResponseEntity<List<DictionaryItemResponse>> =
        ResponseEntity.ok(dictionaryService.getFriendshipStatuses())

    @GetMapping("/search-visibilities")
    fun getSearchVisibilities(): ResponseEntity<List<DictionaryItemResponse>> =
        ResponseEntity.ok(dictionaryService.getSearchVisibilities())

    @GetMapping("/group-invite-policies")
    fun getGroupInvitePolicies(): ResponseEntity<List<DictionaryItemResponse>> =
        ResponseEntity.ok(dictionaryService.getGroupInvitePolicies())

    @GetMapping("/device-platforms")
    fun getDevicePlatforms(): ResponseEntity<List<DictionaryItemResponse>> =
        ResponseEntity.ok(dictionaryService.getDevicePlatforms())

    @GetMapping("/notification-types")
    fun getNotificationTypes(): ResponseEntity<List<DictionaryItemResponse>> =
        ResponseEntity.ok(dictionaryService.getNotificationTypes())

    @GetMapping("/notification-channels")
    fun getNotificationChannels(): ResponseEntity<List<DictionaryItemResponse>> =
        ResponseEntity.ok(dictionaryService.getNotificationChannels())
}

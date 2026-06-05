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
}

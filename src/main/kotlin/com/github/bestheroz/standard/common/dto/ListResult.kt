package com.github.bestheroz.standard.common.dto

import org.springframework.data.domain.Page

data class ListResult<T>(
    val page: Int,
    val pageSize: Int,
    val total: Long,
    val items: List<T>,
) {
    companion object {
        fun <T> of(page: Page<T>): ListResult<T> =
            ListResult(
                page = page.number,
                pageSize = page.size,
                total = page.totalElements,
                items = page.content,
            )
    }
}

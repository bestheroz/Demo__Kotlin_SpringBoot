package com.github.bestheroz.demo.notice

import com.github.bestheroz.demo.repository.NoticeRepository
import com.github.bestheroz.standard.common.dto.ListResult
import com.github.bestheroz.standard.common.exception.BadRequest400Exception
import com.github.bestheroz.standard.common.exception.ExceptionCode
import com.github.bestheroz.standard.common.security.Operator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NoticeService(
    private val noticeRepository: NoticeRepository,
) {
    suspend fun getNoticeList(request: NoticeDto.Request): ListResult<NoticeDto.Response> =
        withContext(Dispatchers.IO) {
            this@NoticeService
                .noticeRepository
                .findAllByRemovedFlagIsFalse(
                    PageRequest.of(request.page - 1, request.pageSize, Sort.by("id").descending()),
                ).map(NoticeDto.Response::of)
        }.let { ListResult.of(it) }

    suspend fun getNotice(id: Long): NoticeDto.Response =
        withContext(Dispatchers.IO) {
            noticeRepository.findById(id).map(NoticeDto.Response::of).orElseThrow {
                BadRequest400Exception(ExceptionCode.UNKNOWN_NOTICE)
            }
        }

    suspend fun createNotice(
        request: NoticeCreateDto.Request,
        operator: Operator,
    ): NoticeDto.Response = noticeRepository.save(request.toEntity(operator)).let { NoticeDto.Response.of(it) }

    @Transactional
    suspend fun updateNotice(
        id: Long,
        request: NoticeCreateDto.Request,
        operator: Operator,
    ): NoticeDto.Response =
        noticeRepository
            .findById(id)
            .map { notice ->
                notice.update(request.title, request.content, request.useFlag, operator)
                notice
            }.orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_NOTICE) }
            .let { NoticeDto.Response.of(it) }

    @Transactional
    suspend fun deleteNotice(
        id: Long,
        operator: Operator,
    ) = noticeRepository
        .findById(id)
        .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_NOTICE) }
        .remove(operator)
}

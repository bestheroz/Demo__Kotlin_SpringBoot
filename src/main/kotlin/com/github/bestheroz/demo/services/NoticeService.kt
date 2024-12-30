package com.github.bestheroz.demo.services

import com.github.bestheroz.demo.dtos.notice.NoticeCreateDto
import com.github.bestheroz.demo.dtos.notice.NoticeDto
import com.github.bestheroz.demo.repository.NoticeRepository
import com.github.bestheroz.standard.common.dto.ListResult
import com.github.bestheroz.standard.common.exception.BadRequest400Exception
import com.github.bestheroz.standard.common.exception.ExceptionCode
import com.github.bestheroz.standard.common.security.Operator
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NoticeService(
    private val noticeRepository: NoticeRepository,
) {
    fun getNoticeList(request: NoticeDto.Request): ListResult<NoticeDto.Response> =
        noticeRepository
            .findAllByRemovedFlagIsFalse(
                PageRequest.of(request.page - 1, request.pageSize, Sort.by("id").descending()),
            ).map(NoticeDto.Response::of)
            .let { ListResult.Companion.of(it) }

    fun getNotice(id: Long): NoticeDto.Response =
        noticeRepository.findById(id).map(NoticeDto.Response::of).orElseThrow {
            BadRequest400Exception(ExceptionCode.UNKNOWN_NOTICE)
        }

    @Transactional
    fun createNotice(
        request: NoticeCreateDto.Request,
        operator: Operator,
    ): NoticeDto.Response = noticeRepository.save(request.toEntity(operator)).let { NoticeDto.Response.of(it) }

    @Transactional
    fun updateNotice(
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
    fun deleteNotice(
        id: Long,
        operator: Operator,
    ) = noticeRepository
        .findById(id)
        .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_NOTICE) }
        .remove(operator)
}

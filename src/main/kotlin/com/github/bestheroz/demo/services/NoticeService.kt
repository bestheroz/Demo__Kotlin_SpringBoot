package com.github.bestheroz.demo.services

import com.github.bestheroz.demo.dtos.notice.NoticeCreateDto
import com.github.bestheroz.demo.dtos.notice.NoticeDto
import com.github.bestheroz.demo.repository.NoticeRepository
import com.github.bestheroz.demo.repository.NoticeSpecification
import com.github.bestheroz.standard.common.dto.ListResult
import com.github.bestheroz.standard.common.exception.BadRequest400Exception
import com.github.bestheroz.standard.common.exception.ExceptionCode
import com.github.bestheroz.standard.common.security.Operator
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NoticeService(
    private val noticeRepository: NoticeRepository,
) {
    fun getNoticeList(payload: NoticeDto.Request): ListResult<NoticeDto.Response> =
        noticeRepository
            .findAll(
                Specification.allOf(
                    listOfNotNull(
                        NoticeSpecification.removedFlagIsFalse(),
                        payload.id?.let { NoticeSpecification.equalId(it) },
                        payload.title?.let { NoticeSpecification.containsTitle(it) },
                        payload.useFlag?.let { NoticeSpecification.equalUseFlag(it) },
                    ),
                ),
                PageRequest.of(payload.page - 1, payload.pageSize, Sort.by("id").descending()),
            ).map(NoticeDto.Response::of)
            .let(ListResult.Companion::of)

    fun getNotice(id: Long): NoticeDto.Response =
        noticeRepository.findById(id).map(NoticeDto.Response::of).orElseThrow {
            BadRequest400Exception(ExceptionCode.UNKNOWN_NOTICE)
        }

    @Transactional
    fun createNotice(
        payload: NoticeCreateDto.Request,
        operator: Operator,
    ): NoticeDto.Response = noticeRepository.save(payload.toEntity(operator)).let(NoticeDto.Response::of)

    @Transactional
    fun updateNotice(
        id: Long,
        payload: NoticeCreateDto.Request,
        operator: Operator,
    ): NoticeDto.Response =
        noticeRepository
            .findById(id)
            .map { notice ->
                notice.update(payload.title, payload.content, payload.useFlag, operator)
                notice
            }.orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_NOTICE) }
            .let(NoticeDto.Response::of)

    @Transactional
    fun deleteNotice(
        id: Long,
        operator: Operator,
    ) = noticeRepository
        .findById(id)
        .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_NOTICE) }
        .remove(operator)
}

package com.github.bestheroz.demo.services

import com.github.bestheroz.demo.dtos.notice.NoticeCreateDto
import com.github.bestheroz.demo.dtos.notice.NoticeDto
import com.github.bestheroz.demo.repository.NoticeRepository
import com.github.bestheroz.demo.repository.NoticeSpecification
import com.github.bestheroz.standard.common.dto.ListResult
import com.github.bestheroz.standard.common.exception.BadRequest400Exception
import com.github.bestheroz.standard.common.exception.ExceptionCode
import com.github.bestheroz.standard.common.security.Operator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class NoticeService(
    private val noticeRepository: NoticeRepository,
) {
    suspend fun getNoticeList(payload: NoticeDto.Request): ListResult<NoticeDto.Response> =
        withContext(Dispatchers.IO) {
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
        }

    suspend fun getNotice(id: Long): NoticeDto.Response =
        withContext(Dispatchers.IO) {
            noticeRepository
                .findById(id)
                .map(NoticeDto.Response::of)
                .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_NOTICE) }
        }

    @Transactional
    suspend fun createNotice(
        payload: NoticeCreateDto.Request,
        operator: Operator,
    ): NoticeDto.Response =
        withContext(Dispatchers.IO) {
            val saved = noticeRepository.save(payload.toEntity(operator))
            noticeRepository
                .findById(saved.id)
                .get()
                .let(NoticeDto.Response::of)
        }

    @Transactional
    suspend fun updateNotice(
        id: Long,
        payload: NoticeCreateDto.Request,
        operator: Operator,
    ): NoticeDto.Response =
        withContext(Dispatchers.IO) {
            val notice =
                noticeRepository
                    .findById(id)
                    .orElseThrow { BadRequest400Exception(ExceptionCode.UNKNOWN_NOTICE) }
            notice.update(payload.title, payload.content, payload.useFlag, operator)
            noticeRepository.save(notice)
            noticeRepository
                .findById(id)
                .get()
                .let(NoticeDto.Response::of)
        }

    @Transactional
    suspend fun deleteNotice(
        id: Long,
        operator: Operator,
    ) {
        val notice =
            withContext(Dispatchers.IO) {
                noticeRepository.findById(id).orElseThrow {
                    BadRequest400Exception(ExceptionCode.UNKNOWN_NOTICE)
                }
            }
        notice.remove(operator)
        withContext(Dispatchers.IO) { noticeRepository.save(notice) }
    }
}

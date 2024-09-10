package com.github.bestheroz.demo.notice

import com.github.bestheroz.demo.entity.Notice
import com.github.bestheroz.demo.repository.NoticeRepository
import com.github.bestheroz.standard.common.dto.ListResult
import com.github.bestheroz.standard.common.exception.ExceptionCode
import com.github.bestheroz.standard.common.exception.RequestException400
import com.github.bestheroz.standard.common.security.Operator
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class NoticeService(
    private val noticeRepository: NoticeRepository,
) {
    @Transactional(readOnly = true)
    fun getNoticeList(request: NoticeDto.Request): ListResult<NoticeDto.Response> =
        noticeRepository
            .findAllByRemovedFlagIsFalse(
                PageRequest.of(
                    request.page - 1,
                    request.pageSize,
                    Sort.by("id").descending(),
                ),
            ).map(NoticeDto.Response::of)
            .let {
                ListResult.of(it)
            }

    @Transactional(readOnly = true)
    fun getNotice(id: Long): NoticeDto.Response =
        noticeRepository
            .findById(id)
            .map(NoticeDto.Response::of)
            .orElseThrow { RequestException400(ExceptionCode.UNKNOWN_NOTICE) }

    fun createNotice(
        request: NoticeCreateDto.Request,
        operator: Operator,
    ): NoticeDto.Response = noticeRepository.save(request.toEntity(operator)).let { NoticeDto.Response.of(it) }

    fun updateNotice(
        id: Long,
        request: NoticeCreateDto.Request,
        operator: Operator,
    ): NoticeDto.Response =
        noticeRepository
            .findById(id)
            .map { notice: Notice ->
                notice.update(
                    request.title,
                    request.content,
                    request.useFlag,
                    operator,
                )
                notice
            }.orElseThrow { RequestException400(ExceptionCode.UNKNOWN_NOTICE) }
            .let {
                NoticeDto.Response.of(it)
            }

    fun deleteNotice(
        id: Long,
        operator: Operator,
    ) = noticeRepository
        .findById(id)
        .orElseThrow { RequestException400(ExceptionCode.UNKNOWN_NOTICE) }
        .remove(operator)
}

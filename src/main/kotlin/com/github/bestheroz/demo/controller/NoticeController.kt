package com.github.bestheroz.demo.controller

import com.github.bestheroz.demo.dtos.notice.NoticeCreateDto
import com.github.bestheroz.demo.dtos.notice.NoticeDto
import com.github.bestheroz.demo.services.NoticeService
import com.github.bestheroz.standard.common.authenticate.CurrentUser
import com.github.bestheroz.standard.common.dto.ListResult
import com.github.bestheroz.standard.common.security.Operator
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/v1/notices")
@Tag(name = "Notice", description = "공지사항 API")
class NoticeController(
    private val noticeService: NoticeService,
) {
    @GetMapping
    fun getNoticeList(
        @Schema(example = "1") @RequestParam page: Int,
        @Schema(example = "10") @RequestParam pageSize: Int,
    ): ListResult<NoticeDto.Response> = noticeService.getNoticeList(NoticeDto.Request(page, pageSize))

    @GetMapping("{id}")
    fun getNotice(
        @PathVariable id: Long,
    ): NoticeDto.Response = noticeService.getNotice(id)

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('NOTICE_EDIT')")
    fun createNotice(
        @RequestBody request: NoticeCreateDto.Request,
        @CurrentUser operator: Operator,
    ): NoticeDto.Response = noticeService.createNotice(request, operator)

    @PutMapping("{id}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('NOTICE_EDIT')")
    fun updateNotice(
        @PathVariable id: Long,
        @RequestBody request: NoticeCreateDto.Request,
        @CurrentUser operator: Operator,
    ): NoticeDto.Response = noticeService.updateNotice(id, request, operator)

    @DeleteMapping("{id}")
    @Operation(description = "(Soft delete)", responses = [ApiResponse(responseCode = "204")])
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAuthority('NOTICE_EDIT')")
    fun deleteNotice(
        @PathVariable id: Long,
        @CurrentUser operator: Operator,
    ) = noticeService.deleteNotice(id, operator)
}

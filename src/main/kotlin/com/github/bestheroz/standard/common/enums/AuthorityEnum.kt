package com.github.bestheroz.standard.common.enums

import com.github.bestheroz.standard.common.entity.converter.GenericEnumListJsonConverter

enum class AuthorityEnum(
    private val value: String,
) {
    ADMIN_VIEW("ADMIN_VIEW"),
    ADMIN_EDIT("ADMIN_EDIT"),
    USER_VIEW("USER_VIEW"),
    USER_EDIT("USER_EDIT"),
    NOTICE_VIEW("NOTICE_VIEW"),
    NOTICE_EDIT("NOTICE_EDIT"),
    ;

    class AuthorityEnumListConverter : GenericEnumListJsonConverter<AuthorityEnum>(AuthorityEnum::class.java)
}

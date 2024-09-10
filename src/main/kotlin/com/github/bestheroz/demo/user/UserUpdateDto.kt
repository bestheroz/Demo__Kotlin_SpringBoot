package com.github.bestheroz.demo.user;

import io.swagger.v3.oas.annotations.media.Schema;
;
;

public class UserUpdateDto {
  
  
  public static class Request extends UserCreateDto.Request {
    @Schema(description = "비밀번호", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String password;
  }
}

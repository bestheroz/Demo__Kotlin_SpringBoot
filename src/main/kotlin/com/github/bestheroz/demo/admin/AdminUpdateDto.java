package com.github.bestheroz.demo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
;
;

public class AdminUpdateDto {
  
  
  public static class Request extends AdminCreateDto.Request {
    @Schema(description = "비밀번호", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String password;
  }
}

package com.github.bestheroz.demo.user;

import io.swagger.v3.oas.annotations.media.Schema;
;

public class UserChangePasswordDto {
  
  public static class Request {
    @Schema(description = "비밀번호", requiredMode = Schema.RequiredMode.REQUIRED)
    private String oldPassword;

    @Schema(description = "새 비밀번호", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;
  }
}

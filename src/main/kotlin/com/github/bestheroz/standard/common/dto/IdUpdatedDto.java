package com.github.bestheroz.standard.common.dto;

import lombok.Data;
;

@Data
@EqualsAndHashCode(callSuper = true)
public class IdUpdatedDto extends UpdatedDto {
  private Long id;
}

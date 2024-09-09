package com.github.bestheroz.standard.common.entity;

import com.github.bestheroz.demo.entity.Admin;
import com.github.bestheroz.demo.entity.User;
import com.github.bestheroz.standard.common.dto.UserSimpleDto;
import com.github.bestheroz.standard.common.enums.UserTypeEnum;
import com.github.bestheroz.standard.common.security.Operator;
import jakarta.persistence.*;
import java.time.Instant;





@MappedSuperclass
public class IdCreatedUpdated extends IdCreated {
  @Column(name = "updated_object_type")
  private UserTypeEnum updatedObjectType;

  private Instant updatedAt;

  @Column(name = "updated_object_id")
  private Long updatedObjectId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "updated_object_id",
      referencedColumnName = "id",
      insertable = false,
      updatable = false)
  private Admin updatedByAdmin;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "updated_object_id",
      referencedColumnName = "id",
      insertable = false,
      updatable = false)
  private User updatedByUser;

  public void setUpdatedBy(Operator operator, Instant instant) {
    if (operator.type.equals(UserTypeEnum.ADMIN)) {
      this.updatedObjectType = UserTypeEnum.ADMIN;
      this.updatedByAdmin = Admin.fromOperator(operator);
    } else if (operator.type.equals(UserTypeEnum.USER)) {
      this.updatedObjectType = UserTypeEnum.USER;
      this.updatedByUser = User.fromOperator(operator);
    }
    this.setUpdatedAt(instant);
    this.setUpdatedObjectId(operator.getId());
    this.setUpdatedObjectType(operator.type);
  }

  public UserSimpleDto getUpdatedBy() {
    return switch (this.updatedObjectType) {
      case ADMIN -> UserSimpleDto.fromEntity(this.updatedByAdmin);
      case USER -> UserSimpleDto.fromEntity(this.updatedByUser);
    };
  }
}
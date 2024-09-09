package com.github.bestheroz.demo.entity;

import com.github.bestheroz.standard.common.entity.IdCreatedUpdated;
import com.github.bestheroz.standard.common.enums.AuthorityEnum;
import com.github.bestheroz.standard.common.enums.UserTypeEnum;
import com.github.bestheroz.standard.common.security.Operator;
import com.github.bestheroz.standard.common.util.PasswordUtil;
import io.micrometer.common.util.StringUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.time.Instant;
import java.util.List;







@Entity
@DiscriminatorValue("admin")
public class Admin extends IdCreatedUpdated {
  @Column(nullable = false)
  private String loginId;

  private String password;
  private String token;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private Boolean useFlag;

  @Column(nullable = false)
  private Boolean managerFlag;

  @Convert(converter = AuthorityEnum.AuthorityEnumListConverter.class)
  @Column(columnDefinition = "json", nullable = false)
  private List<AuthorityEnum> authorities;

  private Instant changePasswordAt;
  private Instant latestActiveAt;

  private Instant joinedAt;

  @Column(nullable = false)
  private Boolean removedFlag;

  private Instant removedAt;

  public UserTypeEnum type {
    return UserTypeEnum.ADMIN;
  }

  public List<AuthorityEnum> getAuthorities() {
    return this.managerFlag ? List.of(AuthorityEnum.values()) : this.authorities;
  }

  public Admin(
      String loginId,
      String password,
      String name,
      Boolean useFlag,
      Boolean managerFlag,
      List<AuthorityEnum> authorities,
      Operator operator) {
    Instant now = Instant.now();
    this.loginId = loginId;
    this.password = PasswordUtil.getPasswordHash(password);
    this.name = name;
    this.useFlag = useFlag;
    this.managerFlag = managerFlag;
    this.authorities = authorities;
    this.joinedAt = now;
    this.removedFlag = false;
    this.setCreatedBy(operator, now);
    this.setUpdatedBy(operator, now);
  }

  public static Admin fromOperator(Operator operator) {
    Admin admin = new Admin();
    admin.setId(operator.getId());
    admin.setLoginId(operator.getLoginId());
    admin.setName(operator.getName());
    admin.setManagerFlag(operator.getManagerFlag());
    return admin;
  }

  public void update(
      String loginId,
      String password,
      String name,
      Boolean useFlag,
      Boolean managerFlag,
      List<AuthorityEnum> authorities,
      Operator operator) {
    this.loginId = loginId;
    this.name = name;
    this.useFlag = useFlag;
    this.managerFlag = managerFlag;
    this.authorities = authorities;
    Instant now = Instant.now();
    this.setUpdatedBy(operator, now);
    if (StringUtils.isNotEmpty(password)) {
      this.password = PasswordUtil.getPasswordHash(password);
      this.changePasswordAt = now;
    }
  }

  public void changePassword(String password, Operator operator) {
    this.password = PasswordUtil.getPasswordHash(password);
    Instant now = Instant.now();
    this.changePasswordAt = now;
    this.setUpdatedBy(operator, now);
  }

  public void remove(Operator operator) {
    this.removedFlag = true;
    Instant now = Instant.now();
    this.removedAt = now;
    this.setUpdatedBy(operator, now);
  }

  public void renewToken(String token) {
    this.token = token;
    this.latestActiveAt = Instant.now();
  }

  public void logout() {
    this.token = null;
  }
}
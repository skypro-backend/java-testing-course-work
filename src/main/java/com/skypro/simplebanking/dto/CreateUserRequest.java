package com.skypro.simplebanking.dto;

import javax.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public class CreateUserRequest {
  private String username;
  private String password;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}

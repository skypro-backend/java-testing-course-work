package com.skypro.simplebanking.dto;

import com.skypro.simplebanking.entity.User;

public class ListUserDTO {
  private final long id;
  private final String username;

  public ListUserDTO(long id, String username) {
    this.id = id;
    this.username = username;
  }

  public long getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public static ListUserDTO from(User user) {
    return new ListUserDTO(user.getId(), user.getUsername());
  }
}

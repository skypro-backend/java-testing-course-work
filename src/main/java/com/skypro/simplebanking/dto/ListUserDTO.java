package com.skypro.simplebanking.dto;

import com.skypro.simplebanking.entity.User;
import java.util.List;
import java.util.stream.Collectors;

public class ListUserDTO {
  private final long id;
  private final String username;

  private final List<ListAccountDTO> accounts;

  public ListUserDTO(long id, String username, List<ListAccountDTO> accounts) {
    this.id = id;
    this.username = username;
    this.accounts = accounts;
  }

  public long getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public List<ListAccountDTO> getAccounts() {
    return accounts;
  }

  public static ListUserDTO from(User user) {
    return new ListUserDTO(
        user.getId(),
        user.getUsername(),
        user.getAccounts().stream().map(ListAccountDTO::from).collect(Collectors.toList()));
  }
}

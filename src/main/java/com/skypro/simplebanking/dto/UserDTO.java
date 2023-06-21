package com.skypro.simplebanking.dto;

import com.skypro.simplebanking.entity.User;
import java.util.List;
import java.util.stream.Collectors;

public class UserDTO {
  private final long id;
  private final String username;
  private final List<AccountDTO> accounts;

  public UserDTO(long id, String username, List<AccountDTO> accounts) {
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

  public List<AccountDTO> getAccounts() {
    return accounts;
  }

  public static UserDTO from(User user) {
    return new UserDTO(
        user.getId(),
        user.getUsername(),
        user.getAccounts().stream().map(AccountDTO::from).collect(Collectors.toList()));
  }
}

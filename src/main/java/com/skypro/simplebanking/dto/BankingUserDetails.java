package com.skypro.simplebanking.dto;

import com.skypro.simplebanking.entity.User;
import java.util.Collection;
import java.util.Collections;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class BankingUserDetails implements UserDetails {
  private final long id;
  private final String username;
  private final String password;
  private final boolean isAdmin;

  public BankingUserDetails(long id, String username, String password, boolean isAdmin) {
    this.id = id;
    this.username = username;
    this.password = password;
    this.isAdmin = isAdmin;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    SimpleGrantedAuthority authority =
        isAdmin
            ? new SimpleGrantedAuthority("ROLE_ADMIN")
            : new SimpleGrantedAuthority("ROLE_USER");
    return Collections.singleton(authority);
  }

  @Override
  public String getPassword() {
    return this.password;
  }

  @Override
  public String getUsername() {
    return this.username;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  public long getId() {
    return id;
  }

  public static BankingUserDetails from(User user) {
    return new BankingUserDetails(user.getId(), user.getUsername(), user.getPassword(), false);
  }
}

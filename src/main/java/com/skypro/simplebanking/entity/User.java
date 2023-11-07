package com.skypro.simplebanking.entity;

import javax.persistence.*;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user-generator")
  @SequenceGenerator(name = "user-generator", sequenceName = "user_sequence")
  private Long id;
  @Column(name = "username", unique = true)
  private String username;
  @Column(name = "password")
  private String password;
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "user")
  private Collection<Account> accounts;
  @Column(name = "role")
  @Enumerated(EnumType.STRING)
  private Role role;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

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

  public Collection<Account> getAccounts() {
    return accounts;
  }

  public void setAccounts(Collection<Account> accounts) {
    this.accounts = accounts;
  }

  public Role getRole() {
    return role;
  }

  public void setRole(Role role) {
    this.role = role;
  }
}
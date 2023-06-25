package com.skypro.simplebanking.service;

import com.skypro.simplebanking.dto.BankingUserDetails;
import com.skypro.simplebanking.dto.ListUserDTO;
import com.skypro.simplebanking.dto.UserDTO;
import com.skypro.simplebanking.entity.User;
import com.skypro.simplebanking.exception.UserAlreadyExistsException;
import com.skypro.simplebanking.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService implements UserDetailsService {
  private final UserRepository userRepository;
  private final AccountService accountService;
  private final PasswordEncoder passwordEncoder;

  public UserService(
      UserRepository userRepository,
      AccountService accountService,
      PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.accountService = accountService;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return userRepository
        .findByUsername(username)
        .map(BankingUserDetails::from)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
  }

  @Transactional
  public UserDTO createUser(String username, String password) {
    Optional<User> existingUser = userRepository.findByUsername(username);
    if (existingUser.isPresent()) {
      throw new UserAlreadyExistsException();
    }
    User user = new User();
    user.setUsername(username);
    user.setPassword(passwordEncoder.encode(password));
    userRepository.save(user);
    accountService.createDefaultAccounts(user);
    return UserDTO.from(user);
  }
  @Transactional(readOnly = true)
  public UserDTO getUser(long id) {
    return userRepository.findById(id).map(UserDTO::from).orElseThrow();
  }
  @Transactional(readOnly = true)
  public List<ListUserDTO> listUsers() {
    return userRepository.findAll().stream().map(ListUserDTO::from).collect(Collectors.toList());
  }
}

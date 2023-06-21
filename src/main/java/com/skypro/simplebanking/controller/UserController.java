package com.skypro.simplebanking.controller;

import com.skypro.simplebanking.dto.BankingUserDetails;
import com.skypro.simplebanking.dto.CreateUserRequest;
import com.skypro.simplebanking.dto.ListUserDTO;
import com.skypro.simplebanking.dto.UserDTO;
import javax.validation.Valid;

import com.skypro.simplebanking.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {
  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @PostMapping
  public UserDTO createUser(@RequestBody @Valid CreateUserRequest userRequest) {
    return userService.createUser(userRequest.getUsername(), userRequest.getPassword());
  }
  @GetMapping("/list")
  public List<ListUserDTO> getAllUsers(){
    return userService.listUsers();
  }
  @GetMapping("/me")
  public UserDTO getMyProfile(Authentication authentication){
    BankingUserDetails bankingUserDetails = (BankingUserDetails) authentication.getPrincipal();
    return userService.getUser(bankingUserDetails.getId());
  }
}

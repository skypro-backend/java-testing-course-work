package com.skypro.simplebanking.constants;

import com.skypro.simplebanking.entity.User;
import com.skypro.simplebanking.repository.UserRepository;
import com.skypro.simplebanking.service.AccountService;
import com.skypro.simplebanking.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
public class InitialData {
    private final UserRepository userRepository;
    private final AccountService accountService;
    private final UserService userService;

    public User addUserToBase() {
        User user1 = new User("User1", "passwordUser1");
        return userRepository.save(user1);
    }

    public User addUserWithAccountToBase() {
        User user = new User("User1", "passwordUser1");
        userRepository.save(user);
        accountService.createDefaultAccounts(user);
        return user;
    }

    public UserDetails getAuthorizedUser() {
        return userService.loadUserByUsername("User1");
    }

    public List<User> getUsersList() {
        User user1 = new User("User1");
        User user2 = new User("User2");
        userRepository.save(user1);
        userRepository.save(user2);
        List<User> usersList = userRepository.findAll();
        return usersList;
    }
}
package com.skypro.simplebanking.controller;

import com.skypro.simplebanking.SimpleBankingApplication;
import com.skypro.simplebanking.dto.BankingUserDetails;
import com.skypro.simplebanking.entity.User;
import com.skypro.simplebanking.repository.AccountRepository;
import com.skypro.simplebanking.repository.UserRepository;
import com.skypro.simplebanking.service.UserService;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;


import static com.skypro.simplebanking.TestData.*;


@SpringBootTest(classes = SimpleBankingApplication.class)
@AutoConfigureMockMvc
public class UserControllerTests {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private UserService userService;

    @BeforeEach
    void createTestRepository() {
        userService.createUser("Sergey", "123");
    }

    @AfterEach
    void clearTestRepository() {
        userRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "USER")
    void createUserWithNotCorrectRole_expected_exception() throws Exception {
        JSONObject userRequest = new JSONObject();
        userRequest.put("username", "Ivan");
        userRequest.put("password", "1234");

        mockMvc.perform(post("/user/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userRequest.toString()))
                .andExpect(status().is4xxClientError());

    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_expected_OK() throws Exception {
        JSONObject userRequest = new JSONObject();
        userRequest.put("username", "Ivan");
        userRequest.put("password", "123");

        mockMvc.perform(post("/user/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userRequest.toString()))
                .andExpect(status().isOk());

    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_expected_userAlreadyExistException() throws Exception {

        JSONObject userRequest = new JSONObject();
        userRequest.put("username", "Sergey");
        userRequest.put("password", "123");

        mockMvc.perform(post("/user/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userRequest.toString()))
                .andExpect(status().is4xxClientError());

    }

    @Test
    @WithMockUser(roles = "USER")
    void getAllUsers_expectedNonEmptyArray_thenClearRepository_andExpectEmptyArray() throws Exception {

        mockMvc.perform(get("/user/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$.length()").value(1));

        clearTestRepository();
        mockMvc.perform(get("/user/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

    }

    @Test
    @WithMockUser(roles = "USER")
    void getAllUsers_expectedEmptyArray() throws Exception {
        clearTestRepository();
        mockMvc.perform(get("/user/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_WithAdminRole_expected_exception() throws Exception {
        mockMvc.perform(get("/user/list"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void shouldReturnUserDTO_SaveToDataBase_ThenReturnCorrectUserDTO() throws Exception {
        User user = userRepository.save(getTestUser());
        BankingUserDetails bankingUserDetails = BankingUserDetails.from(user);

        mockMvc.perform(get("/user/me")
                        .with(user(bankingUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.username").value(user.getUsername()))
                .andExpect(jsonPath("$.accounts").isArray())
                .andExpect(jsonPath("$.accounts").isNotEmpty());
    }
}


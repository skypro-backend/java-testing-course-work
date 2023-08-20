package com.skypro.simplebanking.controller;

import com.skypro.simplebanking.dto.AccountDTO;
import com.skypro.simplebanking.dto.BalanceChangeRequest;
import com.skypro.simplebanking.dto.BankingUserDetails;
import com.skypro.simplebanking.entity.Account;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.Base64Utils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class UserControllerTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    private DataSource dataSource;
    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }


    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private UserService userService;


    @BeforeEach
    void createRepository() {
        userService.createUser("username1","password1" );
        userService.createUser("username2","password2" );
        userService.createUser("username3","password3" );
        userService.createUser("username4","password4" );
        userService.createUser("username5","password5" );
    }

    @AfterEach
    void cleanRepository() {
        userRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void testPostgresql() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            assertThat(conn).isNotNull();
        }
    }

    @Test
    void getTranzaction_Test_OK() throws Exception {
        JSONObject transfer = new JSONObject();
        transfer.put("fromAccountId", getAccountIdByUsername("username1"));
        transfer.put("toUserId", getUserIdByUserName("username2"));
        transfer.put("toAccountId", getAccountIdByUsername("username2"));
        transfer.put("amount", 1);

        mockMvc.perform(post("/transfer")
                        .header(HttpHeaders.AUTHORIZATION, getBasicAuthenticationHeader("username1", "password1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transfer.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void givenUsers_OK() throws Exception {
        mockMvc.perform(get("/user/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$.length()").value(5));
    }


    @Test
    @WithMockUser(roles = "USER")
    void givenNoBody_whenEmptyJsonArray() throws Exception {
        userRepository.deleteAll();
        mockMvc.perform(get("/user/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void givenUsers_AdminNoAccess_Error403() throws Exception {
        mockMvc.perform(get("/user/list"))
                .andExpect(status().is4xxClientError());
    }

    private String getBasicAuthenticationHeader(String username, String password) {
        return "Basic " + Base64Utils.encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void getTranzaction_Test_WrongAccountCurrency_Status400() throws Exception {

        JSONObject transfer = new JSONObject();
        transfer.put("fromAccountId", 1);
        transfer.put("toUserId", 2);
        transfer.put("toAccountId", 5);
        transfer.put("amount", 1);

        mockMvc.perform(post("/transfer")
                        .header(HttpHeaders.AUTHORIZATION, getBasicAuthenticationHeader("username1", "password1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transfer.toString()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getTranzaction_Test_AccountNotFoundException() throws Exception {

        JSONObject transfer = new JSONObject();
        transfer.put("fromAccountId", 1);
        transfer.put("toUserId", 2);
        transfer.put("toAccountId", 44);
        transfer.put("amount", 1);

        mockMvc.perform(post("/transfer")
                        .header(HttpHeaders.AUTHORIZATION, getBasicAuthenticationHeader("username1", "password1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transfer.toString()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getMyProfile_Test_OK() throws Exception {
        mockMvc.perform(get("/user/me")
                .header(HttpHeaders.AUTHORIZATION, getBasicAuthenticationHeader("username1", "password1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("username1"))
                .andExpect(jsonPath("$.accounts.length()").value(3));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_Test_OK() throws Exception {
        JSONObject userRequest = new JSONObject();
        userRequest.put("username", "username");
        userRequest.put("password", "password");

        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userRequest.toString()))
                .andExpect(status().isOk());
    }
    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_Test_TrowUserAlreadyExistsException() throws Exception {
        JSONObject userRequest = new JSONObject();
        userRequest.put("username", "username1");
        userRequest.put("password", "password1");

        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userRequest.toString()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void depositToAccount_Test_OK() throws Exception {
        JSONObject balanceChangeRequest = new JSONObject();
        balanceChangeRequest.put("amount", 10000L);

        mockMvc.perform(post("/account/deposit/{id}", getAccountIdByUsername("username1"))
                .header(HttpHeaders.AUTHORIZATION, getBasicAuthenticationHeader("username1", "password1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(balanceChangeRequest.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(10001));

    }
    @Test
    void withdrawFromAccount_Test_notOK_TrowInsufficientFundsException() throws Exception {
        JSONObject balanceChangeRequest = new JSONObject();
        balanceChangeRequest.put("amount", 10000L);

        mockMvc.perform(post("/account/withdraw/{id}", 1)
                        .header(HttpHeaders.AUTHORIZATION, getBasicAuthenticationHeader("username1", "password1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(balanceChangeRequest.toString()))
                .andExpect(status().is4xxClientError());

    }


    private User getUserByName(String username){
        return userRepository.findByUsername(username).orElseThrow();
    }
    private long getUserIdByUserName(String username){
        return getUserByName(username).getId();
    }
    private long getAccountIdByUsername(String username){
        long userId = getUserIdByUserName(username);
        long ost = userId % userRepository.count();
        long count = (userId - ost) / userRepository.count();
        return count * 3 * userRepository.count() + ost*3;
    }
}
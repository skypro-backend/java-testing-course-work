package com.skypro.simplebanking.controller;

import com.skypro.simplebanking.dto.BankingUserDetails;
import com.skypro.simplebanking.dto.TransferRequest;
import com.skypro.simplebanking.entity.Account;
import com.skypro.simplebanking.entity.AccountCurrency;
import com.skypro.simplebanking.entity.User;
import com.skypro.simplebanking.repository.AccountRepository;
import com.skypro.simplebanking.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import javax.validation.Valid;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

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


    @BeforeEach
    void createRepository() {
        User user1 = new User();
        user1.setUsername("username1");
        user1.setPassword("password1");
        User user2 = new User();
        user2.setUsername("username2");
        user2.setPassword("password2");
        User user3 = new User();
        user3.setUsername("username3");
        user3.setPassword("password3");
        User user4 = new User();
        user4.setUsername("username4");
        user4.setPassword("password4");
        User user5 = new User();
        user5.setUsername("username5");
        user5.setPassword("password5");

        Account account1 = new Account();
        account1.setId(1L);
        account1.setAccountCurrency(AccountCurrency.RUB);
        account1.setAmount(10000L);
        account1.setUser(user1);
        Account account2 = new Account();
        account2.setId(2L);
        account2.setAccountCurrency(AccountCurrency.EUR);
        account2.setAmount(1000L);
        account2.setUser(user2);
        Account account3 = new Account();
        account3.setId(3L);
        account3.setAccountCurrency(AccountCurrency.USD);
        account3.setAmount(2000L);
        account3.setUser(user3);
        Account account4 = new Account();
        account4.setId(4L);
        account4.setAccountCurrency(AccountCurrency.EUR);
        account4.setAmount(4000L);
        account4.setUser(user4);
        Account account5 = new Account();
        account5.setId(5L);
        account5.setAccountCurrency(AccountCurrency.RUB);
        account5.setAmount(50000L);
        account5.setUser(user5);

        List<User> users = List.of(user1, user2, user3, user4, user5);
        List<Account> accounts = List.of(account1, account2, account3, account4, account5);

        userRepository.saveAll(users);
    }

    @AfterEach
    void cleanRepository() {
        userRepository.deleteAll();
    }

    @Test
    void testPostgresql() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            assertThat(conn).isNotNull();
        }
    }

    // Проверка получения списка юзеров ("/user/list")
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

    // Проверка транзакции ("/transfer")



    @Test
    @WithMockUser(roles = "USER", username = "username2")
    void getTranzaction() throws Exception {
 // Нужно передать аутенфикацию
        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setFromAccountId(2);
        transferRequest.setToUserId(4);
        transferRequest.setToAccountId(4);
        transferRequest.setAmount(5000);

        ObjectMapper mapper = new ObjectMapper();
        String jsonString = mapper.writeValueAsString(transferRequest);

        mockMvc.perform(post("/transfer")
                .content(jsonString))
                .andExpect(status().isOk());

    }

}
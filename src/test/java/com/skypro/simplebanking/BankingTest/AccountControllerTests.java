package com.skypro.simplebanking.BankingTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skypro.simplebanking.constants.InitialData;
import com.skypro.simplebanking.entity.User;
import com.skypro.simplebanking.repository.AccountRepository;
import com.skypro.simplebanking.repository.UserRepository;
import com.skypro.simplebanking.service.AccountService;
import com.skypro.simplebanking.service.UserService;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Testcontainers
public class AccountControllerTests {
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
    private DataSource dataSource;
    @Autowired
    MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private InitialData initialData;

    @BeforeEach
    void cleanTables() {
        accountRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testPostgresql() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            assertThat(conn).isNotNull();
        }
    }

    @Test
    void givenAccountId_whenAdminRights_thenIsForbidden
            (@Value("${app.security.admin-token}") String adminToken) throws Exception {
        User user = new User("User");
        userRepository.save(user);
        long accountId = user.getId();
        mockMvc.perform(get("/account/{id}", accountId)
                        .header("X-SECURITY-ADMIN-KEY", adminToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/account/deposit/{id}", accountId)
                        .header("X-SECURITY-ADMIN-KEY", adminToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/account/withdraw/{id}", accountId)
                        .header("X-SECURITY-ADMIN-KEY", adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    public void givenAccountId_whenGetUserId_thenReturnAccount() throws Exception {
        User user = initialData.addUserWithAccountToBase();
        long accountId = user.getId();
        mockMvc.perform(get("/account/{id}", accountId)
                        .with(user(initialData.getAuthorizedUser()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").isNotEmpty(),
                        jsonPath("$.id").isNumber(),
                        jsonPath("$.amount").value("1"),
                        jsonPath("$.currency").value("USD")
                );
        mockMvc.perform(get("/account/{id}", accountId + 1L)
                        .with(user(initialData.getAuthorizedUser()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").isNotEmpty(),
                        jsonPath("$.id").isNumber(),
                        jsonPath("$.amount").value("1"),
                        jsonPath("$.currency").value("EUR")
                );
    }

    @Test
    @Transactional
    public void givenAccountId_whenUnderGetUserId_thenNotFound() throws Exception {
        User user = initialData.addUserWithAccountToBase();
        long accountId = user.getId() + 3L;
        mockMvc.perform(get("/account/{id}", accountId)
                        .with(user(initialData.getAuthorizedUser()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void givenAccountId_whenGetUserId_thenReturnAccountWithDeposit() throws Exception {
        User user = initialData.addUserWithAccountToBase();
        long accountId = user.getId();
        JSONObject deposit = new JSONObject();
        deposit.put("amount", 50L);
        mockMvc.perform(post("/account/deposit/{id}", accountId)
                        .with(user(initialData.getAuthorizedUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deposit.toString()))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").isNotEmpty(),
                        jsonPath("$.id").isNumber(),
                        jsonPath("$.amount").value("51"),
                        jsonPath("$.currency").value("USD")
                );
    }

    @Test
    @Transactional
    public void givenAccountId_whenGetUserId_thenReturnAccountWithWithdraw() throws Exception {
        User user = initialData.addUserWithAccountToBase();
        long accountId = user.getId();
        JSONObject withdraw = new JSONObject();
        withdraw.put("amount", 1L);
        mockMvc.perform(post("/account/withdraw/{id}", accountId)
                        .with(user(initialData.getAuthorizedUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(withdraw.toString()))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").isNotEmpty(),
                        jsonPath("$.id").isNumber(),
                        jsonPath("$.amount").value("0"),
                        jsonPath("$.currency").value("USD")
                );
    }

    @Test
    @Transactional
    public void givenAccountId_whenGetUserIdButAmountMoreAccessible_thenNotWithdraw() throws Exception {
        User user = initialData.addUserWithAccountToBase();
        long accountId = user.getId();
        JSONObject withdraw = new JSONObject();
        withdraw.put("amount", 10L);
        mockMvc.perform(post("/account/withdraw/{id}", accountId)
                        .with(user(initialData.getAuthorizedUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(withdraw.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    public void givenAccountId_whenGetUserIdButAmountIsNegative_thenBadRequest() throws Exception {
        User user = initialData.addUserWithAccountToBase();
        long accountId = user.getId();
        JSONObject amount = new JSONObject();
        amount.put("amount", -1);
        mockMvc.perform(post("/account/deposit/{id}", accountId)
                        .with(user(initialData.getAuthorizedUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(amount.toString()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/account/withdraw/{id}", accountId)
                        .with(user(initialData.getAuthorizedUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(amount.toString()))
                .andExpect(status().isBadRequest());
    }
}
package com.skypro.simplebanking.BankingTest;

import com.skypro.simplebanking.constants.InitialData;
import com.skypro.simplebanking.entity.User;
import com.skypro.simplebanking.repository.AccountRepository;
import com.skypro.simplebanking.repository.UserRepository;
import com.skypro.simplebanking.service.AccountService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Testcontainers
public class TransferControllerTests {
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
    private AccountService accountService;
    @Autowired
    private InitialData initialData;

    @BeforeEach
    void cleanUsersTable() {
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
    void transfer_whenAdminRights_thenIsForbidden
            (@Value("${app.security.admin-token}") String adminToken) throws Exception {
        mockMvc.perform(get("/transfer")
                        .header("X-SECURITY-ADMIN-KEY", adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    public void transferGetUserIdToUnderUserId_whenSameCurrency_thenTransfer() throws Exception {
        User user1 = initialData.addUserWithAccountToBase();
        User user2 = new User("User2", "passwordUser2");
        userRepository.save(user2);
        accountService.createDefaultAccounts(user2);
        long id = user1.getId();
        long idToUser = user2.getId();
        JSONObject transferRequest = new JSONObject();
        transferRequest.put("fromAccountId", id);
        transferRequest.put("toUserId", idToUser);
        transferRequest.put("toAccountId", idToUser + 2L);
        transferRequest.put("amount", 1L);
        mockMvc.perform(post("/transfer")
                        .with(user(initialData.getAuthorizedUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferRequest.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    @Transactional
    public void transferGetUserIdToUnderUserId_whenNotSameCurrency_thenBadRequest() throws Exception {
        User user1 = initialData.addUserWithAccountToBase();
        User user2 = new User("User2", "passwordUser2");
        userRepository.save(user2);
        accountService.createDefaultAccounts(user2);
        long id = user1.getId();
        long idToUser = user2.getId() + 1L;
        JSONObject transferRequest = new JSONObject();
        transferRequest.put("fromAccountId", id);
        transferRequest.put("toUserId", idToUser);
        transferRequest.put("toAccountId", idToUser + 2L);
        transferRequest.put("amount", 1L);
        mockMvc.perform(post("/transfer")
                        .with(user(initialData.getAuthorizedUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferRequest.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    public void transferGetUserIdToNoUnderUserId_whenSameCurrency_thenNotFound() throws Exception {
        User user1 = initialData.addUserWithAccountToBase();
        long id = user1.getId();
        JSONObject transferRequest = new JSONObject();
        transferRequest.put("fromAccountId", id);
        transferRequest.put("toUserId", 200L);
        transferRequest.put("toAccountId", 200L);
        transferRequest.put("amount", 1L);
        mockMvc.perform(post("/transfer")
                        .with(user(initialData.getAuthorizedUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferRequest.toString()))
                .andExpect(status().isNotFound());
    }
}
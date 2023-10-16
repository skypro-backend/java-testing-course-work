package com.skypro.simplebanking.BankingTest;

import com.skypro.simplebanking.constants.InitialData;
import com.skypro.simplebanking.entity.User;
import com.skypro.simplebanking.repository.UserRepository;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Testcontainers
public class UserControllerTests {
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
    private InitialData initialData;

    @BeforeEach
    void cleanUsersTable() {
        userRepository.deleteAll();
    }

    @Test
    void testPostgresql() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            assertThat(conn).isNotNull();
        }
    }

    @Test
    @WithMockUser
    void givenNoUsersInDatabase_whenGetUsers_thenEmptyJsonArray() throws Exception {
        mockMvc.perform(get("/user/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void givenNoUserInDatabase_whenUserCreated_thenItReturnUser(
            @Value("${app.security.admin-token}") String adminToken) throws Exception {
        JSONObject newUser = new JSONObject();
        newUser.put("username", "UserNew");
        newUser.put("password", "passwordUserNew");

        mockMvc.perform(post("/user")
                        .header("X-SECURITY-ADMIN-KEY", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newUser.toString()))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").isNotEmpty(),
                        jsonPath("$.id").isNumber(),
                        jsonPath("$.username").value("UserNew")
                );
    }

    @Test
    void givenUserInDatabase_whenUserAlreadyExists_thenBadRequest(
            @Value("${app.security.admin-token}") String adminToken) throws Exception {
        userRepository.save(new User("UserExists"));
        JSONObject newUser = new JSONObject();
        newUser.put("username", "UserExists");

        mockMvc.perform(post("/user")
                        .header("X-SECURITY-ADMIN-KEY", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newUser.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_whenUnderNonAdminRights_thenItReturnUser() throws Exception {
        initialData.addUserToBase();
        JSONObject newUser = new JSONObject();
        newUser.put("username", "UserNew");
        newUser.put("password", "passwordUserNew");

        mockMvc.perform(post("/user")
                        .with(user(initialData.getAuthorizedUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newUser.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void givenUsersInDatabase_whenGetUsers_thenItExistsInList() throws Exception {
        initialData.getUsersList();
        mockMvc.perform(get("/user/list")
                        .with(user(initialData.getAuthorizedUser()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$").isArray(),
                        jsonPath("$.length()").value(2)
                );
    }

    @Test
    void givenUserProfile_whenGetUserId_thenItReturnUser() throws Exception {
        initialData.addUserToBase();
        mockMvc.perform(get("/user/me")
                        .with(user(initialData.getAuthorizedUser())))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").isNumber(),
                        jsonPath("$.username").value("User1")
                );
    }

    @Test
    void givenUserProfileOrUsersList_whenAdminRights_thenIsForbidden
            (@Value("${app.security.admin-token}") String adminToken) throws Exception {
        initialData.addUserToBase();
        mockMvc.perform(get("/user/me")
                        .header("X-SECURITY-ADMIN-KEY", adminToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/user/list")
                        .header("X-SECURITY-ADMIN-KEY", adminToken))
                .andExpect(status().isForbidden());
    }
}
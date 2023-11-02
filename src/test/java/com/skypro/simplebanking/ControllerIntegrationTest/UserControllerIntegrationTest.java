package com.skypro.simplebanking.ControllerIntegrationTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skypro.simplebanking.dto.*;
import com.skypro.simplebanking.entity.AccountCurrency;
import com.skypro.simplebanking.service.AccountService;
import com.skypro.simplebanking.service.UserService;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class UserControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private AccountService accountService;

    private UserDTO userAdmin;
    private UserDTO userUser;

    @BeforeEach
    public void setupTestUsers() {
        userAdmin = userService.createUser("adminUser", "adminPassword", "ROLE_ADMIN");
        userUser = userService.createUser("regularUser", "regularPassword", "ROLE_USER");
    }
    @AfterEach
    public void cleanup() {
        userService.deleteAllUsers();
    }

    @Test
    @WithMockUser(username = "adminUser", password = "adminPassword", roles = "ADMIN")
    public void testCreateUserWithAdminRole_Success() throws Exception {
        CreateUserRequest userRequest = new CreateUserRequest();
        userRequest.setUsername("newUser");
        userRequest.setPassword("newPassword");
        userRequest.setRole("ROLE_USER");

        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newUser"));
    }


    @Test
    @WithMockUser(username = "regularUser", password = "regularPassword", roles = "USER")
    public void testGetAllUsersByRegularUser_Success() throws Exception {
        mockMvc.perform(get("/user/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }


    @Test
    public void testDepositToOwnAccount_Success() throws Exception {
        UserDetails userDetails = userService.loadUserByUsername(userUser.getUsername());
        long userId = userService.getUserIdByUsername(userUser.getUsername());

        Long sourceAccountId = createAccountForUserWithCurrency(userId, AccountCurrency.USD);

        BankingUserDetails bankingDetails = new BankingUserDetails(userUser.getId(), userUser.getUsername(), userDetails.getPassword(), false);

        Authentication authentication = new UsernamePasswordAuthenticationToken(bankingDetails, null, bankingDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        long depositAmount = 500L;
        long finalDepositAmount = 1500L;

        mockMvc.perform(post("/account/deposit/{id}", sourceAccountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BalanceChangeRequest(depositAmount))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(finalDepositAmount));
    }

    @Test
    public void testWithdrawFromOwnAccount_Success() throws Exception {
        UserDetails userDetails = userService.loadUserByUsername(userUser.getUsername());
        long userId = userService.getUserIdByUsername(userUser.getUsername());

        Long sourceAccountId = createAccountForUserWithCurrency(userId, AccountCurrency.USD);
        long withdrawAmount = 200L;
        BankingUserDetails bankingDetails = new BankingUserDetails(userUser.getId(), userUser.getUsername(), userDetails.getPassword(), false);

        Authentication authentication = new UsernamePasswordAuthenticationToken(bankingDetails, null, bankingDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);



        mockMvc.perform(post("/account/withdraw/{id}", sourceAccountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BalanceChangeRequest(withdrawAmount))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(800L));
    }

    @Test
    public void testTransferBetweenOwnAccounts_Success() throws Exception {

        UserDetails userDetails = userService.loadUserByUsername(userUser.getUsername());
        long userId = userService.getUserIdByUsername(userUser.getUsername());

        Long fromAccountID = createAccountForUserWithCurrency(userId, AccountCurrency.USD);
        Long toAccountID = createAccountForUserWithCurrency(userId, AccountCurrency.USD);
        long amount = 200L;

        BankingUserDetails bankingDetails = new BankingUserDetails(userUser.getId(), userUser.getUsername(), userDetails.getPassword(), false);

        Authentication authentication = new UsernamePasswordAuthenticationToken(bankingDetails, null, bankingDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        JSONObject requestBody = new JSONObject();
        requestBody.put("fromAccountId", fromAccountID);
        requestBody.put("toAccountId", toAccountID);
        requestBody.put("toUserId", userId);
        requestBody.put("amount", amount);

        mockMvc.perform(post("/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody.toString()))
                .andExpect(status().isOk());

        AccountDTO sourceAccount = accountService.getAccount(userId, fromAccountID);
        AccountDTO destinationAccount = accountService.getAccount(userId, toAccountID);

        assertThat(sourceAccount.getAmount()).isEqualByComparingTo(800L);
        assertThat(destinationAccount.getAmount()).isEqualByComparingTo(1200L);
    }

    @Test
    public void testTransferBetweenAccounts_WithDifferentCurrencies_Failure() throws Exception {
        UserDetails userDetails = userService.loadUserByUsername(userUser.getUsername());
        long userId = userService.getUserIdByUsername(userUser.getUsername());

        Long sourceAccountId = createAccountForUserWithCurrency(userId, AccountCurrency.USD);
        Long destinationAccountId = createAccountForUserWithCurrency(userId, AccountCurrency.EUR);
        long transferAmount = 200L;

        BankingUserDetails bankingDetails = new BankingUserDetails(userUser.getId(), userUser.getUsername(), userDetails.getPassword(), false);

        Authentication authentication = new UsernamePasswordAuthenticationToken(bankingDetails, null, bankingDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        mockMvc.perform(post("/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransferRequest(sourceAccountId, destinationAccountId, transferAmount))))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testWithdrawFromOwnAccount_InsufficientFunds_Failure() throws Exception {
        UserDetails userDetails = userService.loadUserByUsername(userUser.getUsername());
        long userId = userService.getUserIdByUsername(userUser.getUsername());

        Long sourceAccountId = createAccountForUserWithCurrency(userId, AccountCurrency.USD);
        long withdrawAmount = 1200L;
        BankingUserDetails bankingDetails = new BankingUserDetails(userUser.getId(), userUser.getUsername(), userDetails.getPassword(), false);

        Authentication authentication = new UsernamePasswordAuthenticationToken(bankingDetails, null, bankingDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        mockMvc.perform(post("/account/withdraw/{id}", sourceAccountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BalanceChangeRequest(withdrawAmount))))
                .andExpect(status().isBadRequest());
    }

    private long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userService.getUserIdByUsername(((UserDetails) authentication.getPrincipal()).getUsername());
    }

    private Long createAccountForUser(long userId) {
        AccountDTO accountDTO = accountService.createTestAccount(userId, 1000L, 1L);
        return accountDTO.getId();
    }

    private Long createAccountForUserWithCurrency(long userId, AccountCurrency currency) {
        AccountDTO accountDTO = accountService.createAccount(userId, 1000L);
        accountService.changeAccountCurrency(accountDTO.getId(), currency);
        return accountDTO.getId();
    }


    public TestRestTemplate restTemplate() {
        return new TestRestTemplate();
    }
}

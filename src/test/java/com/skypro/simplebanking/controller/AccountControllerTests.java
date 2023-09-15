package com.skypro.simplebanking.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.skypro.simplebanking.SimpleBankingApplication;
import com.skypro.simplebanking.dto.BalanceChangeRequest;
import com.skypro.simplebanking.dto.BankingUserDetails;
import com.skypro.simplebanking.entity.Account;
import com.skypro.simplebanking.entity.User;
import com.skypro.simplebanking.repository.UserRepository;
import com.skypro.simplebanking.repository.AccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import static com.skypro.simplebanking.TestData.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest(classes = SimpleBankingApplication.class)
@AutoConfigureMockMvc
@Transactional
public class AccountControllerTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AccountRepository accountRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BalanceChangeRequest balanceChangeRequest = new BalanceChangeRequest();


    @BeforeEach
    void createBalanceChangeRequest(){
        balanceChangeRequest.setAmount(getTestRandomLong());
    }

    @AfterEach
    void clearTestRepository() {
        userRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void getUserAccount_allDataCorrect_expectedCorrectAccount() throws Exception {
        User user = userRepository.save(getTestUser());
        BankingUserDetails userDetails = BankingUserDetails.from(user);

        for (long id : user.getAccounts().stream().map(Account::getId).toList()) {
            mockMvc.perform(get("/account/{id}", id)
                            .with(user(userDetails)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").exists())
                    .andExpect(jsonPath("$.id").value(id));
        }
    }
    @Test
    void getUserAccount_notCorrectRole_expected_exception() throws Exception {
        User user = userRepository.save(getTestUser());
        BankingUserDetails userDetails = getAdminBankingUserDetails(user);

        for (long id : user.getAccounts().stream().map(Account::getId).toList()) {
            mockMvc.perform(get("/account/{id}", id)
                            .with(user(userDetails)))
                    .andExpect(status().is4xxClientError());
        }
    }
    @Test
    void getUserAccount_userDoesntExist_expected_exception() throws Exception {
        User user = getTestUser();
        BankingUserDetails userDetails = BankingUserDetails.from(user);

        for (long id : user.getAccounts().stream().map(Account::getId).toList()) {
            mockMvc.perform(get("/account/{id}", id)
                            .with(user(userDetails)))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Test
    void depositToAccount_allDataCorrect_expected_ok() throws Exception {

        User user = userRepository.save(getTestUser());
        BankingUserDetails userDetails = BankingUserDetails.from(user);

        for (Account account : user.getAccounts()) {
            mockMvc.perform(post("/account/deposit/{id}", account.getId())
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(balanceChangeRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").exists())
                    .andExpect(jsonPath("$.id").value(account.getId()))
                    .andExpect(jsonPath("$.currency").value(account.getAccountCurrency().name()))
                    .andExpect(jsonPath("$.amount").value(user.getAccounts()
                            .stream()
                            .filter(x -> x.getId().equals(account.getId()))
                            .findFirst()
                            .orElseThrow()
                            .getAmount()));
        }
    }
    @Test
    void depositToAccount_notCorrectRole_expected_exception() throws Exception {

        User user = userRepository.save(getTestUser());
        BankingUserDetails userDetails = getAdminBankingUserDetails(user);

        for (Account account : user.getAccounts()) {
            mockMvc.perform(post("/account/deposit/{id}", account.getId())
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(balanceChangeRequest)))
                    .andExpect(status().is4xxClientError());
        }
    }
    @Test
    void depositToAccount_userDoesntExist_expected_exception() throws Exception {

        User user = getTestUser();
        BankingUserDetails userDetails = BankingUserDetails.from(user);

        for (Account account : user.getAccounts()) {
            mockMvc.perform(post("/account/deposit/{id}", account.getId())
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(balanceChangeRequest)))
                    .andExpect(status().is4xxClientError());
        }
    }
    @Test
    void depositToAccount_notCorrectAmount_expected_exception() throws Exception {

        balanceChangeRequest.setAmount(-100L);

        User user = userRepository.save(getTestUser());
        BankingUserDetails userDetails = BankingUserDetails.from(user);

        for (Account account : user.getAccounts()) {
            mockMvc.perform(post("/account/deposit/{id}", account.getId())
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(balanceChangeRequest)))
                    .andExpect(status().is4xxClientError());
        }
    }
    @Test
    void withdrawFromAccount_allDataCorrect_expected_ok() throws Exception {
        User user = userRepository.save(getTestUser());
        BankingUserDetails userDetails = BankingUserDetails.from(user);
        balanceChangeRequest.setAmount(1);

        for (Account account : user.getAccounts()) {
            mockMvc.perform(post("/account/withdraw/{id}", account.getId())
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(balanceChangeRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").exists())
                    .andExpect(jsonPath("$.id").value(account.getId()))
                    .andExpect(jsonPath("$.currency").value(account.getAccountCurrency().name()))
                    .andExpect(jsonPath("$.amount").value(user.getAccounts()
                            .stream()
                            .filter(x -> x.getId().equals(account.getId()))
                            .findFirst()
                            .orElseThrow()
                            .getAmount()));
        }
    }
    @Test
    void withdrawFromAccount_notCorrectRole_expected_exception() throws Exception {
        User user = userRepository.save(getTestUser());
        BankingUserDetails userDetails = getAdminBankingUserDetails(user);
        balanceChangeRequest.setAmount(1);

        for (Account account : user.getAccounts()) {
            mockMvc.perform(post("/account/withdraw/{id}", account.getId())
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(balanceChangeRequest)))
                    .andExpect(status().is4xxClientError());
        }
    }
    @Test
    void withdrawFromAccount_userDoesntExist_expected_exception() throws Exception {
        User user = getTestUser();
        BankingUserDetails userDetails = BankingUserDetails.from(user);
        balanceChangeRequest.setAmount(1);

        for (Account account : user.getAccounts()) {
            mockMvc.perform(post("/account/withdraw/{id}", account.getId())
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(balanceChangeRequest)))
                    .andExpect(status().is4xxClientError());
        }
    }
    @Test
    void withdrawFromAccount_notCorrectAmount_expected_exception() throws Exception {
        User user = userRepository.save(getTestUser());
        BankingUserDetails userDetails = BankingUserDetails.from(user);
        balanceChangeRequest.setAmount(-100L);

        for (Account account : user.getAccounts()) {
            mockMvc.perform(post("/account/withdraw/{id}", account.getId())
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(balanceChangeRequest)))
                    .andExpect(status().is4xxClientError());
        }
    }
}

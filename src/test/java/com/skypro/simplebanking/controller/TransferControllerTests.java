package com.skypro.simplebanking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skypro.simplebanking.SimpleBankingApplication;
import com.skypro.simplebanking.dto.BankingUserDetails;
import com.skypro.simplebanking.dto.TransferRequest;
import com.skypro.simplebanking.entity.AccountCurrency;
import com.skypro.simplebanking.entity.User;
import com.skypro.simplebanking.repository.UserRepository;
import com.skypro.simplebanking.repository.AccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static com.skypro.simplebanking.TestData.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;


@SpringBootTest(classes = SimpleBankingApplication.class)
@AutoConfigureMockMvc
@Transactional
public class TransferControllerTests {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AccountRepository accountRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();


    @AfterEach
    void clearTestRepository() {
        userRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void transfer_allDataCorrect_expected_ok() throws Exception {

        User fromUser = userRepository.save(getTestUser());
        BankingUserDetails userDetails = BankingUserDetails.from(fromUser);
        User toUser = userRepository.save(getTestUser());
        long senderAmount = getUserAmount(fromUser, AccountCurrency.RUB);
        long receiverAmount = getUserAmount(toUser, AccountCurrency.RUB);
        TransferRequest transferRequest = getTestTransferRequest(fromUser, AccountCurrency.RUB, toUser, AccountCurrency.RUB, senderAmount);

        mockMvc.perform(post("/transfer/")
                        .with(user(userDetails))
                        .content(objectMapper.writeValueAsString(transferRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        long senderActualAmount = getUserAmount(userRepository.findById(fromUser.getId()).orElseThrow(), AccountCurrency.RUB);
        long receiverActualAmount = getUserAmount(userRepository.findById(toUser.getId()).orElseThrow(), AccountCurrency.RUB);

        assertThat(senderActualAmount).isEqualTo(0);
        assertThat(receiverActualAmount).isEqualTo(receiverAmount + senderAmount);

    }

    @Test
    void transfer_notCorrectRole_expected_exception() throws Exception {

        User fromUser = userRepository.save(getTestUser());
        BankingUserDetails userDetails = getAdminBankingUserDetails(fromUser);
        User toUser = userRepository.save(getTestUser());
        long senderAmount = getUserAmount(fromUser, AccountCurrency.RUB);
        TransferRequest transferRequest = getTestTransferRequest(fromUser, AccountCurrency.RUB, toUser, AccountCurrency.RUB, senderAmount);

        mockMvc.perform(post("/transfer/")
                        .with(user(userDetails))
                        .content(objectMapper.writeValueAsString(transferRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());

    }

    @Test
    void transfer_senderDoesntExist_expected_exception() throws Exception {

        User fromUser = getTestUser();
        BankingUserDetails userDetails = BankingUserDetails.from(fromUser);
        User toUser = userRepository.save(getTestUser());
        long senderAmount = getUserAmount(fromUser, AccountCurrency.RUB);
        TransferRequest transferRequest = getTestTransferRequest(fromUser, AccountCurrency.RUB, toUser, AccountCurrency.RUB, senderAmount);

        mockMvc.perform(post("/transfer/")
                        .with(user(userDetails))
                        .content(objectMapper.writeValueAsString(transferRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());

    }

    @Test
    void transfer_receiverDoesntExist_expected_exception() throws Exception {

        User fromUser = userRepository.save(getTestUser());
        BankingUserDetails userDetails = BankingUserDetails.from(fromUser);
        User toUser = getTestUser();
        long senderAmount = getUserAmount(fromUser, AccountCurrency.RUB);
        TransferRequest transferRequest = getTestTransferRequest(fromUser, AccountCurrency.RUB, toUser, AccountCurrency.RUB, senderAmount);

        mockMvc.perform(post("/transfer/")
                        .with(user(userDetails))
                        .content(objectMapper.writeValueAsString(transferRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());

    }

    @Test
    void transfer_notCorrectAmount_expected_ok() throws Exception {

        User fromUser = userRepository.save(getTestUser());
        BankingUserDetails userDetails = BankingUserDetails.from(fromUser);
        User toUser = userRepository.save(getTestUser());
        long senderAmount = -100L;

        TransferRequest transferRequest = getTestTransferRequest(fromUser, AccountCurrency.RUB, toUser, AccountCurrency.RUB, senderAmount);

        mockMvc.perform(post("/transfer/")
                        .with(user(userDetails))
                        .content(objectMapper.writeValueAsString(transferRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());


    }
}

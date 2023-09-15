package com.skypro.simplebanking;

import com.github.javafaker.Faker;
import com.skypro.simplebanking.dto.BankingUserDetails;
import com.skypro.simplebanking.dto.TransferRequest;
import com.skypro.simplebanking.entity.Account;
import com.skypro.simplebanking.entity.AccountCurrency;
import com.skypro.simplebanking.entity.User;

import java.util.ArrayList;
import java.util.Random;

public class TestData {


    public static final Faker faker = new Faker();

    public static Long getTestRandomLong() {
        return new Random().nextLong(1000);
    }
    public static String getTestPassword() {
        return faker.number().digits(10);
    }
    public static String getTestUserName() {
        return faker.name().name();
    }

    public static User getTestUser() {
        User user = new User();
        user.setId(getTestRandomLong());
        user.setUsername(getTestUserName());
        user.setPassword(getTestPassword());
        user.setAccounts(new ArrayList<>());

        for (AccountCurrency currency : AccountCurrency.values()) {
            Account account = new Account();
            account.setId(getTestRandomLong());
            account.setUser(user);
            account.setAccountCurrency(currency);
            account.setAmount(getTestRandomLong());
            user.getAccounts().add(account);
        }
        return user;
    }

    public static BankingUserDetails getAdminBankingUserDetails(User user) {
        return new BankingUserDetails(getTestRandomLong(), user.getUsername(), user.getPassword(), true);
    }
    public static long getUserAmount(User user, AccountCurrency accountCurrency) {
        return user.getAccounts()
                .stream()
                .filter(x -> x.getAccountCurrency().equals(accountCurrency))
                .mapToLong(Account::getAmount)
                .findAny()
                .orElseThrow();
    }
    public static TransferRequest getTestTransferRequest(User fromUser, AccountCurrency fromAccountCurrency, User toUser, AccountCurrency toAccountCurrency, long amount) {
        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setFromAccountId(fromUser.getAccounts()
                .stream()
                .filter(x -> x.getAccountCurrency().equals(fromAccountCurrency))
                .findFirst()
                .orElseThrow()
                .getId());
        transferRequest.setToUserId(toUser.getId());
        transferRequest.setToAccountId(toUser.getAccounts()
                .stream()
                .filter(x -> x.getAccountCurrency().equals(toAccountCurrency))
                .findFirst()
                .orElseThrow()
                .getId());
        transferRequest.setAmount(fromUser.getAccounts()
                .stream()
                .filter(x -> x.getAccountCurrency().equals(AccountCurrency.RUB))
                .findFirst()
                .map(x -> x.getAmount() - 1)
                .orElseThrow());
        transferRequest.setAmount(amount);
        return transferRequest;
    }
}

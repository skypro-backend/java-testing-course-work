package com.skypro.simplebanking.service;

import com.skypro.simplebanking.dto.AccountDTO;
import com.skypro.simplebanking.entity.Account;
import com.skypro.simplebanking.entity.AccountCurrency;
import com.skypro.simplebanking.entity.User;
import com.skypro.simplebanking.exception.*;
import com.skypro.simplebanking.repository.AccountRepository;
import java.util.ArrayList;

import com.skypro.simplebanking.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
  private final AccountRepository accountRepository;
  private final UserRepository userRepository;

  public AccountService(AccountRepository accountRepository, UserRepository userRepository) {
    this.accountRepository = accountRepository;
    this.userRepository = userRepository;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void createDefaultAccounts(User user) {
    user.setAccounts(new ArrayList<>());
    for (AccountCurrency currency : AccountCurrency.values()) {
      Account account = new Account();
      account.setUser(user);
      account.setAccountCurrency(currency);
      account.setAmount(1L);
      accountRepository.save(account);
    }
  }

  @Transactional(readOnly = true)
  public AccountDTO getAccount(long userId, Long accountId) {
    return accountRepository
            .getAccountByUser_IdAndId(userId, accountId)
            .map(AccountDTO::from)
            .orElseThrow(AccountNotFoundException::new);
  }

  @Transactional
  public void validateCurrency(long sourceAccount, long destinationAccount) {
    Account acc1 = accountRepository.findById(sourceAccount).orElseThrow(AccountNotFoundException::new);
    Account acc2 = accountRepository.findById(destinationAccount).orElseThrow(AccountNotFoundException::new);
    if (!acc1.getAccountCurrency().equals(acc2.getAccountCurrency())) {
      throw new WrongCurrencyException();
    }
  }

  @Transactional
  public AccountDTO depositToAccount(long userId, Long accountId, long amount) {
    if (amount < 0) {
      throw new InvalidAmountException();
    }
    Account account = accountRepository
            .getAccountByUser_IdAndId(userId, accountId)
            .orElseThrow(AccountNotFoundException::new);
    account.setAmount(account.getAmount() + amount);
    return AccountDTO.from(account);
  }

  @Transactional
  public AccountDTO withdrawFromAccount(long id, Long accountId, long amount) {
    if (amount < 0) {
      throw new InvalidAmountException();
    }
    Account account = accountRepository
            .getAccountByUser_IdAndId(id, accountId)
            .orElseThrow(AccountNotFoundException::new);
    if (account.getAmount() < amount) {
      throw new InsufficientFundsException("Cannot withdraw " + amount + " " + account.getAccountCurrency().name());
    }
    account.setAmount(account.getAmount() - amount);
    return AccountDTO.from(account);
  }

  @Transactional
  public void transferBetweenAccounts(long userId, Long sourceAccountId, Long destinationAccountId, long amount) {
    if (amount < 0) {
      throw new InvalidAmountException();
    }
    Account sourceAccount = getAccountByUserIdAndAccountId(userId, sourceAccountId);
    Account destinationAccount = getAccountByAccountId(destinationAccountId);

    if (!sourceAccount.getAccountCurrency().equals(destinationAccount.getAccountCurrency())) {
      throw new WrongCurrencyException();
    }

    if (sourceAccount.getAmount() < amount) {
      throw new InsufficientFundsException("Cannot transfer " + amount + " " + sourceAccount.getAccountCurrency().name() + " from account " + sourceAccountId);
    }

    sourceAccount.setAmount(sourceAccount.getAmount() - amount);
    destinationAccount.setAmount(destinationAccount.getAmount() + amount);
  }

  @Transactional
  public AccountDTO createAccount(Long userId, long initialBalance) {
    Account account = new Account();
    account.setUser(userRepository.findById(userId).orElseThrow(UserNotFoundException::new));
    account.setAmount(initialBalance);
    account.setAccountCurrency(AccountCurrency.USD);
    Account saved = accountRepository.save(account);
    return AccountDTO.from(saved);
  }

  @Transactional
  public void changeAccountCurrency(Long accountId, AccountCurrency newCurrency) {
    Account account = accountRepository.findById(accountId).orElseThrow(AccountNotFoundException::new);
    account.setAccountCurrency(newCurrency);
  }
  private Account getAccountByUserIdAndAccountId(long userId, Long accountId) {
    return accountRepository.getAccountByUser_IdAndId(userId, accountId).orElseThrow(AccountNotFoundException::new);
  }

  private Account getAccountByAccountId(Long accountId) {
    return accountRepository.findById(accountId).orElseThrow(AccountNotFoundException::new);
  }
  @Transactional
  public AccountDTO createTestAccount(Long userId, long initialBalance, Long accountId) {
    Account account = new Account();
    account.setId(accountId);
    account.setUser(userRepository.findById(userId).orElseThrow(UserNotFoundException::new));
    account.setAmount(initialBalance);
    account.setAccountCurrency(AccountCurrency.USD);
    Account saved = accountRepository.save(account);
    return AccountDTO.from(saved);
  }

}

package com.skypro.simplebanking.service;

import com.skypro.simplebanking.exception.InsufficientFundsException;
import com.skypro.simplebanking.dto.AccountDTO;
import com.skypro.simplebanking.entity.Account;
import com.skypro.simplebanking.entity.AccountCurrency;
import com.skypro.simplebanking.entity.User;
import java.util.ArrayList;

import com.skypro.simplebanking.exception.AccountNotFoundException;
import com.skypro.simplebanking.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
  private final AccountRepository accountRepository;

  public AccountService(AccountRepository accountRepository) {
    this.accountRepository = accountRepository;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void createDefaultAccounts(User user) {
    user.setAccounts(new ArrayList<>());
    for (AccountCurrency currency : AccountCurrency.values()) {
      Account account = new Account();
      account.setUser(user);
      account.setAccountCurrency(currency);
      account.setAmount(1L);
      user.getAccounts().add(account);
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
  public AccountDTO depositToAccount(long userId, Long accountId, long amount) {
    Account account =
        accountRepository
            .getAccountByUser_IdAndId(userId, accountId)
            .orElseThrow(AccountNotFoundException::new);
    account.setAmount(account.getAmount() + amount);
    return AccountDTO.from(account);
  }

  @Transactional
  public AccountDTO withdrawFromAccount(long id, Long accountId, long amount) {
    Account account =
        accountRepository
            .getAccountByUser_IdAndId(id, accountId)
            .orElseThrow(AccountNotFoundException::new);
    if (account.getAmount() < amount) {
      throw new InsufficientFundsException(
          "Cannot withdraw " + amount + " " + account.getAccountCurrency().name());
    }
    account.setAmount(account.getAmount() - amount);
    return AccountDTO.from(account);
  }
}

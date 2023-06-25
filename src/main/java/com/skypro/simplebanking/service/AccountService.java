package com.skypro.simplebanking.service;

import com.skypro.simplebanking.dto.AccountDTO;
import com.skypro.simplebanking.entity.Account;
import com.skypro.simplebanking.entity.AccountCurrency;
import com.skypro.simplebanking.entity.User;
import com.skypro.simplebanking.exception.AccountNotFoundException;
import com.skypro.simplebanking.exception.InsufficientFundsException;
import com.skypro.simplebanking.exception.InvalidAmountException;
import com.skypro.simplebanking.exception.WrongCurrencyException;
import com.skypro.simplebanking.repository.AccountRepository;
import java.util.ArrayList;
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
  public void validateCurrency(long sourceAccount, long destinationAccount) {
    Account acc1 =
        accountRepository.findById(sourceAccount).orElseThrow(AccountNotFoundException::new);
    Account acc2 =
        accountRepository.findById(destinationAccount).orElseThrow(AccountNotFoundException::new);
    if (!acc1.getAccountCurrency().equals(acc2.getAccountCurrency())){
      throw new WrongCurrencyException();
    }
  }

  @Transactional
  public AccountDTO depositToAccount(long userId, Long accountId, long amount) {
    if (amount < 0) {
      throw new InvalidAmountException();
    }
    Account account =
        accountRepository
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

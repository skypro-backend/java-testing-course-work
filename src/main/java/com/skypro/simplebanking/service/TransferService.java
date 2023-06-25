package com.skypro.simplebanking.service;

import com.skypro.simplebanking.dto.TransferRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

@Service
public class TransferService {
  private final AccountService accountService;

  public TransferService(AccountService accountService) {
    this.accountService = accountService;
  }

  @Transactional
  public void transfer(long id, @RequestBody TransferRequest transferRequest) {
    accountService.validateCurrency(
        transferRequest.getFromAccountId(), transferRequest.getToAccountId());
    accountService.withdrawFromAccount(
        id, transferRequest.getFromAccountId(), transferRequest.getAmount());
    accountService.depositToAccount(
        transferRequest.getToUserId(),
        transferRequest.getToAccountId(),
        transferRequest.getAmount());
  }
}

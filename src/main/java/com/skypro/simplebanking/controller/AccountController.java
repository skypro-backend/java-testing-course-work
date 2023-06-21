package com.skypro.simplebanking.controller;

import com.skypro.simplebanking.dto.AccountDTO;
import com.skypro.simplebanking.dto.BalanceChangeRequest;
import com.skypro.simplebanking.dto.BankingUserDetails;
import com.skypro.simplebanking.service.AccountService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/account")
public class AccountController {
  private final AccountService accountService;

  public AccountController(AccountService accountService) {
    this.accountService = accountService;
  }

  @GetMapping("/{id}")
  public AccountDTO getUserAccount(Authentication authentication, @PathVariable("id") Long accountId) {
    BankingUserDetails bankingUserDetails = (BankingUserDetails) authentication.getPrincipal();
    return accountService.getAccount(bankingUserDetails.getId(), accountId);
  }

  @PostMapping("/deposit/{id}")
  public AccountDTO depositToAccount(Authentication authentication,
                                     @PathVariable("id") Long accountId,
                                     @RequestBody BalanceChangeRequest balanceChangeRequest){
    BankingUserDetails bankingUserDetails = (BankingUserDetails) authentication.getPrincipal();
    return accountService.depositToAccount(bankingUserDetails.getId(),accountId, balanceChangeRequest.getAmount());
  }

  @PostMapping("/withdraw/{id}")
  public AccountDTO withdrawFromAccount(Authentication authentication,
                                     @PathVariable("id") Long accountId,
                                     @RequestBody BalanceChangeRequest balanceChangeRequest){
    BankingUserDetails bankingUserDetails = (BankingUserDetails) authentication.getPrincipal();
    return accountService.withdrawFromAccount(bankingUserDetails.getId(),accountId, balanceChangeRequest.getAmount());
  }
}

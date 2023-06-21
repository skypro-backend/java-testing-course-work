package com.skypro.simplebanking.dto;

import com.skypro.simplebanking.entity.AccountCurrency;

public class TransferRequest {
  private long fromAccountId;
  private long toUserId;
  private long toAccountId;
  private long amount;

  public long getToUserId() {
    return toUserId;
  }

  public void setToUserId(long toUserId) {
    this.toUserId = toUserId;
  }

  public long getAmount() {
    return amount;
  }

  public void setAmount(long amount) {
    this.amount = amount;
  }

  public long getFromAccountId() {
    return fromAccountId;
  }

  public long getToAccountId() {
    return toAccountId;
  }
}

package com.skypro.simplebanking.dto;

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

  public void setFromAccountId(long fromAccountId) {
    this.fromAccountId = fromAccountId;
  }

  public void setToAccountId(long toAccountId) {
    this.toAccountId = toAccountId;
  }
}

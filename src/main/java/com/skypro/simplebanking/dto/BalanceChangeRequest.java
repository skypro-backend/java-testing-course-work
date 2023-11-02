package com.skypro.simplebanking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Range;

@NoArgsConstructor
public class BalanceChangeRequest {
  @JsonProperty("amount")
  private long amount;


  public BalanceChangeRequest(long amount) {
    this.amount = amount;
  }

  public long getAmount() {
    return amount;
  }

  public void setAmount(long amount) {
    this.amount = amount;
  }
}
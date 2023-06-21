package com.skypro.simplebanking.controller;

import com.skypro.simplebanking.exception.AccountNotFoundException;
import com.skypro.simplebanking.exception.InsufficientFundsException;
import com.skypro.simplebanking.exception.UserAlreadyExistsException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class BankingExceptionHandlers {
  @ExceptionHandler(value = {AccountNotFoundException.class})
  public ResponseEntity<?> handleAccountNotFound() {
    return ResponseEntity.notFound().build();
  }

  @ExceptionHandler(value = {InsufficientFundsException.class})
  public ResponseEntity<?> handleInsufficientFunds(InsufficientFundsException exception) {
    return ResponseEntity.badRequest().body(exception.getMessage());
  }

  @ExceptionHandler(value = {UserAlreadyExistsException.class})
  public ResponseEntity<?> handleUserAlreadyExists() {
    return ResponseEntity.badRequest().build();
  }
}

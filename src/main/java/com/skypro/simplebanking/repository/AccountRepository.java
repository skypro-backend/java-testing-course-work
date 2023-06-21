package com.skypro.simplebanking.repository;

import com.skypro.simplebanking.entity.Account;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
  Optional<Account> getAccountByUser_IdAndId(Long userId, Long accountId);
}

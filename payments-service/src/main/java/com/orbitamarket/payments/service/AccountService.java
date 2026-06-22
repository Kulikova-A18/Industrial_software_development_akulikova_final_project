package com.orbitamarket.payments.service;

import com.orbitamarket.payments.model.Account;
import com.orbitamarket.payments.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    @Transactional
    public Account createAccount(String userId) {
        if (accountRepository.existsByUserId(userId)) {
            throw new IllegalStateException("Account already exists for user: " + userId);
        }

        Account account = Account.builder()
                .userId(userId)
                .balance(0)
                .createdAt(LocalDateTime.now())
                .build();

        account = accountRepository.save(account);
        log.info("Account created for user: {}", userId);
        return account;
    }

    @Transactional
    public Account getAccount(String userId) {
        return accountRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Account not found for user: " + userId));
    }

    @Transactional
    public Account topUp(String userId, Integer amount) {
        Account account = accountRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new IllegalStateException("Account not found for user: " + userId));

        account.setBalance(account.getBalance() + amount);
        account.setUpdatedAt(LocalDateTime.now());
        account = accountRepository.save(account);

        log.info("Account topped up: {} +{} = {}", userId, amount, account.getBalance());
        return account;
    }

    @Transactional
    public boolean debitBalance(String userId, Integer amount, String orderId) {
        Account account = accountRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new IllegalStateException("Account not found for user: " + userId));

        if (account.getBalance() < amount) {
            log.warn("Insufficient balance: user={}, balance={}, required={}",
                    userId, account.getBalance(), amount);
            return false;
        }

        account.setBalance(account.getBalance() - amount);
        account.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);

        log.info("Balance debited: user={}, order={}, amount={}, new_balance={}",
                userId, orderId, amount, account.getBalance());
        return true;
    }
}
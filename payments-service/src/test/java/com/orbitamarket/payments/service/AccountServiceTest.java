package com.orbitamarket.payments.service;

import com.orbitamarket.payments.model.Account;
import com.orbitamarket.payments.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    private String testUserId;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        testUserId = "user-123";
        testAccount = Account.builder()
                .id(1L)
                .userId(testUserId)
                .balance(100)
                .createdAt(LocalDateTime.now())
                .version(0)
                .build();
    }

    @Test
    void createAccount_Success() {
        when(accountRepository.existsByUserId(testUserId)).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        Account result = accountService.createAccount(testUserId);

        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
        assertEquals(0, result.getBalance());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void createAccount_AlreadyExists_ThrowsException() {
        when(accountRepository.existsByUserId(testUserId)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> {
            accountService.createAccount(testUserId);
        });

        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void getAccount_Success() {
        when(accountRepository.findByUserId(testUserId)).thenReturn(Optional.of(testAccount));

        Account result = accountService.getAccount(testUserId);

        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
        assertEquals(100, result.getBalance());
    }

    @Test
    void getAccount_NotFound_ThrowsException() {
        when(accountRepository.findByUserId(testUserId)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> {
            accountService.getAccount(testUserId);
        });
    }

    @Test
    void topUp_Success() {
        Integer amount = 50;
        Account updatedAccount = Account.builder()
                .id(1L)
                .userId(testUserId)
                .balance(150)
                .createdAt(LocalDateTime.now())
                .version(1)
                .build();

        when(accountRepository.findByUserIdWithLock(testUserId))
                .thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(updatedAccount);

        Account result = accountService.topUp(testUserId, amount);

        assertNotNull(result);
        assertEquals(150, result.getBalance());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void topUp_AccountNotFound_ThrowsException() {
        when(accountRepository.findByUserIdWithLock(testUserId))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> {
            accountService.topUp(testUserId, 50);
        });
    }

    @Test
    void debitBalance_Success() {
        Integer amount = 30;
        String orderId = "order-456";
        Account updatedAccount = Account.builder()
                .id(1L)
                .userId(testUserId)
                .balance(70)
                .createdAt(LocalDateTime.now())
                .version(1)
                .build();

        when(accountRepository.findByUserIdWithLock(testUserId))
                .thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(updatedAccount);

        boolean result = accountService.debitBalance(testUserId, amount, orderId);

        assertTrue(result);
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void debitBalance_InsufficientFunds_ReturnsFalse() {
        Integer amount = 200;

        when(accountRepository.findByUserIdWithLock(testUserId))
                .thenReturn(Optional.of(testAccount));

        boolean result = accountService.debitBalance(testUserId, amount, "order-456");

        assertFalse(result);
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void debitBalance_AccountNotFound_ThrowsException() {
        when(accountRepository.findByUserIdWithLock(testUserId))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> {
            accountService.debitBalance(testUserId, 30, "order-456");
        });
    }
}
package com.orbitamarket.payments.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orbitamarket.payments.dto.TopUpRequest;
import com.orbitamarket.payments.model.Account;
import com.orbitamarket.payments.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @Autowired
    private ObjectMapper objectMapper;

    private String testUserId;
    private Account testAccount;
    private TopUpRequest topUpRequest;

    @BeforeEach
    void setUp() {
        testUserId = "user-123";
        testAccount = Account.builder()
                .id(1L)
                .userId(testUserId)
                .balance(100)
                .createdAt(LocalDateTime.now())
                .build();

        topUpRequest = new TopUpRequest();
        topUpRequest.setAmount(50);
    }

    @Test
    void createAccount_Success() throws Exception {
        when(accountService.createAccount(anyString())).thenReturn(testAccount);

        mockMvc.perform(post("/api/v1/payments/accounts")
                .header("X-User-Id", testUserId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_id").value(testUserId))
                .andExpect(jsonPath("$.balance").value(100))
                .andExpect(jsonPath("$.currency").value("geocredits"));
    }

    @Test
    void createAccount_MissingUserId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/payments/accounts")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("MISSING_USER_ID"));
    }

    @Test
    void createAccount_AlreadyExists_ReturnsConflict() throws Exception {
        when(accountService.createAccount(anyString()))
                .thenThrow(new IllegalStateException("Account already exists"));

        mockMvc.perform(post("/api/v1/payments/accounts")
                .header("X-User-Id", testUserId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error_code").value("ACCOUNT_ALREADY_EXISTS"));
    }

    @Test
    void topUpAccount_Success() throws Exception {
        Account updatedAccount = Account.builder()
                .id(1L)
                .userId(testUserId)
                .balance(150)
                .createdAt(LocalDateTime.now())
                .build();

        when(accountService.topUp(anyString(), any(Integer.class))).thenReturn(updatedAccount);

        mockMvc.perform(post("/api/v1/payments/accounts/top-up")
                .header("X-User-Id", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(topUpRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_id").value(testUserId))
                .andExpect(jsonPath("$.balance").value(150));
    }

    @Test
    void topUpAccount_MissingUserId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/payments/accounts/top-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(topUpRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("MISSING_USER_ID"));
    }

    @Test
    void topUpAccount_InvalidAmount_ReturnsBadRequest() throws Exception {
        topUpRequest.setAmount(-10);

        mockMvc.perform(post("/api/v1/payments/accounts/top-up")
                .header("X-User-Id", testUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(topUpRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("INVALID_AMOUNT"));
    }

    @Test
    void getBalance_Success() throws Exception {
        when(accountService.getAccount(anyString())).thenReturn(testAccount);

        mockMvc.perform(get("/api/v1/payments/accounts/balance")
                .header("X-User-Id", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_id").value(testUserId))
                .andExpect(jsonPath("$.balance").value(100))
                .andExpect(jsonPath("$.currency").value("geocredits"));
    }

    @Test
    void getBalance_MissingUserId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/payments/accounts/balance")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("MISSING_USER_ID"));
    }

    @Test
    void getBalance_AccountNotFound_ReturnsNotFound() throws Exception {
        when(accountService.getAccount(anyString()))
                .thenThrow(new IllegalStateException("Account not found"));

        mockMvc.perform(get("/api/v1/payments/accounts/balance")
                .header("X-User-Id", testUserId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error_code").value("ACCOUNT_NOT_FOUND"));
    }
}
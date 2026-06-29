package com.orbitamarket.payments.controller;

import com.orbitamarket.payments.dto.AccountResponse;
import com.orbitamarket.payments.dto.BalanceResponse;
import com.orbitamarket.payments.dto.ErrorResponse;
import com.orbitamarket.payments.dto.TopUpRequest;
import com.orbitamarket.payments.model.Account;
import com.orbitamarket.payments.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/accounts")
    public ResponseEntity<?> createAccount(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");

        if (userId == null || userId.isBlank()) {
            return buildError(HttpStatus.BAD_REQUEST, "MISSING_USER_ID",
                    "X-User-Id header is required");
        }

        try {
            Account account = accountService.createAccount(userId);
            AccountResponse response = AccountResponse.builder()
                    .userId(account.getUserId())
                    .balance(account.getBalance())
                    .currency("geocredits")
                    .build();
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return buildError(HttpStatus.CONFLICT, "ACCOUNT_ALREADY_EXISTS",
                    e.getMessage());
        } catch (Exception e) {
            return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                    "Failed to create account: " + e.getMessage());
        }
    }

    @PostMapping("/accounts/top-up")
    public ResponseEntity<?> topUpAccount(
            @RequestBody TopUpRequest request,
            HttpServletRequest httpRequest) {
        String userId = httpRequest.getHeader("X-User-Id");

        if (userId == null || userId.isBlank()) {
            return buildError(HttpStatus.BAD_REQUEST, "MISSING_USER_ID",
                    "X-User-Id header is required");
        }

        if (request.getAmount() == null || request.getAmount() <= 0) {
            return buildError(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT",
                    "Amount must be greater than zero");
        }

        try {
            Account account = accountService.topUp(userId, request.getAmount());
            AccountResponse response = AccountResponse.builder()
                    .userId(account.getUserId())
                    .balance(account.getBalance())
                    .currency("geocredits")
                    .build();
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return buildError(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND",
                    e.getMessage());
        } catch (Exception e) {
            return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                    "Failed to top up: " + e.getMessage());
        }
    }

    @GetMapping("/accounts/balance")
    public ResponseEntity<?> getBalance(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");

        if (userId == null || userId.isBlank()) {
            return buildError(HttpStatus.BAD_REQUEST, "MISSING_USER_ID",
                    "X-User-Id header is required");
        }

        try {
            Account account = accountService.getAccount(userId);
            BalanceResponse response = new BalanceResponse(
                    account.getUserId(),
                    account.getBalance(),
                    "geocredits"
            );
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return buildError(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND",
                    e.getMessage());
        } catch (Exception e) {
            return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                    "Failed to get balance: " + e.getMessage());
        }
    }

    private ResponseEntity<ErrorResponse> buildError(
            HttpStatus status, String errorCode, String message) {
        ErrorResponse error = new ErrorResponse(
                errorCode,
                message,
                LocalDateTime.now().toString()
        );
        return ResponseEntity.status(status).body(error);
    }
}
package com.gustavo.trackflowapp.modules.transaction;

import com.gustavo.trackflowapp.modules.transaction.dto.TransactionDataDTO;
import com.gustavo.trackflowapp.modules.transaction.dto.TransactionRegisterDTO;
import com.gustavo.trackflowapp.modules.transaction.dto.TransactionSettleDTO;
import com.gustavo.trackflowapp.modules.transaction.dto.TransactionUpdateDTO;
import com.gustavo.trackflowapp.modules.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionDataDTO> createTransaction(@RequestBody @Valid TransactionRegisterDTO dto, UriComponentsBuilder uriBuilder, @AuthenticationPrincipal User user) {
        var transactionData = transactionService.create(dto, user);
        var uri = uriBuilder.path("/transactions/{id}").buildAndExpand(transactionData.transactionId()).toUri();
        return ResponseEntity.created(uri).body(transactionData);
    }

    @PatchMapping("/settle")
    public ResponseEntity<TransactionDataDTO> settleTransaction(@RequestBody @Valid TransactionSettleDTO dto, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(transactionService.settleTransaction(dto, user));
    }

    @PatchMapping("/reopen")
    public ResponseEntity<TransactionDataDTO> reopenTransaction(@RequestBody @Valid TransactionSettleDTO dto, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(transactionService.reopenTransaction(dto, user));
    }

    @PutMapping
    public ResponseEntity<TransactionDataDTO> updateTransaction(@RequestBody @Valid TransactionUpdateDTO dto, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(transactionService.updateTransaction(dto, user));
    }

    @GetMapping("/active")
    public ResponseEntity<Page<TransactionDataDTO>> listTransactions(@AuthenticationPrincipal User user,
                                                                     @RequestParam(required = false) Long categoryId,
                                                                     @RequestParam(required = false) TransactionType type,
                                                                     @RequestParam(required = false) TransactionStatus status,
                                                                     Pageable pageable) {
        return ResponseEntity.ok(transactionService.listTransactions(user, categoryId, type, status, pageable));
    }

    @GetMapping("/inactive")
    public ResponseEntity<Page<TransactionDataDTO>> listTransactionsAccountInactive(@AuthenticationPrincipal User user,
                                                                     @RequestParam(required = false) Long categoryId,
                                                                     @RequestParam(required = false) TransactionType type,
                                                                     @RequestParam(required = false) TransactionStatus status,
                                                                     Pageable pageable) {
        return ResponseEntity.ok(transactionService.listTransactionsInactiveAccount(user, categoryId, type, status, pageable));
    }

    @DeleteMapping("/{transactionId}")
    public ResponseEntity<TransactionDataDTO> delete(@PathVariable Long transactionId,
                                                     @AuthenticationPrincipal User user) {
        transactionService.deleteTransaction(transactionId, user);
        return ResponseEntity.noContent().build();
    }
}

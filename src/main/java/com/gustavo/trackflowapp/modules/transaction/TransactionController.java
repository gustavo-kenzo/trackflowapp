package com.gustavo.trackflowapp.modules.transaction;

import com.gustavo.trackflowapp.modules.transaction.dto.TransactionDataDTO;
import com.gustavo.trackflowapp.modules.transaction.dto.TransactionRegisterDTO;
import com.gustavo.trackflowapp.modules.transaction.dto.TransactionSettleDTO;
import com.gustavo.trackflowapp.modules.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
        System.out.println("ENTROU PELO MENOS");
        return ResponseEntity.ok(transactionService.settleTransaction(dto, user));
    }

    @PatchMapping("/reopen")
    public ResponseEntity<TransactionDataDTO> reopenTransaction(@RequestBody @Valid TransactionSettleDTO dto, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(transactionService.reopenTransaction(dto,user));
    }
}

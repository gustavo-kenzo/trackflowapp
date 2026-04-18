package com.gustavo.trackflowapp.modules.account;

import com.gustavo.trackflowapp.modules.account.dto.AccountDataDTO;
import com.gustavo.trackflowapp.modules.account.dto.AccountRegisterDTO;
import com.gustavo.trackflowapp.modules.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountDataDTO> createAccount(@RequestBody AccountRegisterDTO accountRegisterDTO, UriComponentsBuilder uriBuilder, @AuthenticationPrincipal User user) {
        var accountData = accountService.create(accountRegisterDTO, user);
        var uri = uriBuilder.path("/accounts/{id}").buildAndExpand(accountData.id()).toUri();
        return ResponseEntity.created(uri).body(accountData);
    }

    @GetMapping
    public ResponseEntity<Page<AccountDataDTO>> findMyAccounts(@AuthenticationPrincipal(expression = "id") Long id, Pageable pageable) {
        return ResponseEntity.ok(accountService.findMyAccounts(id, pageable));
    }

    @DeleteMapping("/{accountId}")
    public ResponseEntity deleteMyAccount(@PathVariable Long accountId, @AuthenticationPrincipal(expression = "id") Long userId) {
        accountService.delete(accountId, userId);
        return ResponseEntity.noContent().build();
    }

    //usuario desativa apenas conta que ele mesmo criou
    @PatchMapping("/{accountId}/deactivate")
    public ResponseEntity deactivateMyAccount(@PathVariable Long accountId, @AuthenticationPrincipal(expression = "id") Long userId) {
        return ResponseEntity.ok(accountService.deactivate(accountId, userId));
    }

    //usuario ativa apenas conta que ele mesmo criou
    @PatchMapping("/{accountId}/activate")
    public ResponseEntity activateMyAccount(@PathVariable Long accountId, @AuthenticationPrincipal(expression = "id") Long userId) {
        return ResponseEntity.ok(accountService.activate(accountId, userId));
    }
}

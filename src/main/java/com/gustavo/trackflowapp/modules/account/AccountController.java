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
    public ResponseEntity<Page<AccountDataDTO>> findAccounts(@RequestParam(required = false) String owner, Pageable pageable) {
        return ResponseEntity.ok(accountService.findAll(owner, pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity deleteAccount(@PathVariable Long id) {
        accountService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity deactivateAccount(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.deactivate(id));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity activateAccount(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.activate(id));
    }
}

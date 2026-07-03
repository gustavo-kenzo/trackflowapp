package com.gustavo.trackflowapp.modules.account;

import com.gustavo.trackflowapp.modules.account.dto.AccountDataDTO;
import com.gustavo.trackflowapp.modules.account.dto.AccountRegisterDTO;
import com.gustavo.trackflowapp.modules.user.User;
import com.gustavo.trackflowapp.shared.exception.BusinessRuleException;
import com.gustavo.trackflowapp.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    @Transactional
    public AccountDataDTO create(AccountRegisterDTO dto, User user) {
        var account = new Account(user, dto.name(), dto.openingBalance());
        accountRepository.save(account);
        return new AccountDataDTO(account);
    }

    public Page<AccountDataDTO> findMyAccounts(Long id, Pageable pageable) {
        var accounts = accountRepository.findMyAccounts(id, pageable);
        return accounts.map(AccountDataDTO::new);
    }

    @Transactional
    public void delete(Long accountId, Long userId) {
        var rowsAffected = accountRepository.deleteMyAccountAndReturnCount(accountId, userId);
        if (rowsAffected == 0)
            throw new ResourceNotFoundException("Account id not found for delete");
    }

    @Transactional
    public AccountDataDTO deactivate(Long accountId, Long userId) {
        var account = accountRepository.findMyAccount(accountId, userId).orElseThrow(() -> new ResourceNotFoundException("Account id not found for deactivate"));
        if (!account.isActive())
            throw new BusinessRuleException("Account is already inactive");
        account.deactivate();
        return new AccountDataDTO(account);
    }

    @Transactional
    public AccountDataDTO activate(Long accountId, Long userId) {
        var account = accountRepository.findMyAccount(accountId, userId).orElseThrow(() -> new ResourceNotFoundException("Account id not found for activate"));
        if (account.isActive())
            throw new BusinessRuleException("Account is already active");
        account.activate();
        return new AccountDataDTO(account);
    }

    public Account getAccount(Long accountId, Long userId) {
        return accountRepository.findMyAccount(accountId, userId).orElseThrow(() -> new ResourceNotFoundException("Account id not found"));
    }
}

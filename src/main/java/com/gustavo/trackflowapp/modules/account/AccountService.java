package com.gustavo.trackflowapp.modules.account;

import com.gustavo.trackflowapp.modules.account.dto.AccountDataDTO;
import com.gustavo.trackflowapp.modules.account.dto.AccountRegisterDTO;
import com.gustavo.trackflowapp.modules.user.User;
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
        var account = new Account(user, dto.name(), dto.type(), dto.openingBalance());
        accountRepository.save(account);
        return new AccountDataDTO(account);
    }

    public Page<AccountDataDTO> findAll(String owner, Pageable pageable) {
        if (owner != null && !owner.isBlank())
            return accountRepository.findByUserNameIgnoreCaseAndActiveTrue(owner, pageable).map(AccountDataDTO::new);
        return accountRepository.findByActiveTrue(pageable).map(AccountDataDTO::new);
    }

    @Transactional
    public void delete(Long id) {
        var rowsAffected = accountRepository.deleteByIdAndReturnCount(id);
        if (rowsAffected == 0)
            throw new RuntimeException("Account id not found for delete");
    }

    @Transactional
    public AccountDataDTO deactivate(Long id) {
        var account = accountRepository.findById(id).orElseThrow(() -> new RuntimeException("Account id not found for deactivate"));
        if (!account.isActive())
            throw new RuntimeException("Account is already inactive");
        account.deactivate();
        return new AccountDataDTO(account);
    }

    @Transactional
    public AccountDataDTO activate(Long id) {
        var account = accountRepository.findById(id).orElseThrow(() -> new RuntimeException("Account id not found for deactivate"));
        if (account.isActive())
            throw new RuntimeException("Account is already active");
        account.activate();
        return new AccountDataDTO(account);
    }
}

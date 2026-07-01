package com.gustavo.trackflowapp.modules.transaction;

import com.gustavo.trackflowapp.modules.account.AccountService;
import com.gustavo.trackflowapp.modules.category.Category;
import com.gustavo.trackflowapp.modules.category.CategoryService;
import com.gustavo.trackflowapp.modules.transaction.dto.TransactionDataDTO;
import com.gustavo.trackflowapp.modules.transaction.dto.TransactionRegisterDTO;
import com.gustavo.trackflowapp.modules.transaction.dto.TransactionSettleDTO;
import com.gustavo.trackflowapp.modules.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final AccountService accountService;
    private final CategoryService categoryService;

    private final TransactionRepository transactionRepository;

    @Transactional
    public TransactionDataDTO create(TransactionRegisterDTO dto, User user) {
        var account = accountService.getAccount(dto.accountId(), user.getId());
        Category category = null;
        if (dto.categoryId() != null)
            category = categoryService.getCategory(user.getId(), dto.categoryId());

        var transaction = new Transaction(
                user,
                account,
                category,
                null,
                dto.amount(),
                dto.type(),
                dto.competenceDate(),
                dto.settlementDate(),
                dto.description()
        );
        if (transaction.getStatus() == TransactionStatus.SETTLED)
            if (transaction.getType() == TransactionType.INCOME)
                transaction.getAccount().credit(transaction.getAmount());
            else
                transaction.getAccount().debit(transaction.getAmount());

        transactionRepository.save(transaction);
        return new TransactionDataDTO(transaction);
    }

    public TransactionDataDTO createByRecurrence() {
        return null;
    }

    @Transactional
    public TransactionDataDTO settleTransaction(TransactionSettleDTO dto, User user) {
        var transaction = transactionRepository.findMyTransactionById(dto.id(), user.getId()).orElseThrow(() -> new RuntimeException("transaction not found"));
        transaction.settle(dto.settlementDate());
        switch (transaction.getType()) {
            case INCOME -> transaction.getAccount().credit(transaction.getAmount());
            case EXPENSE -> transaction.getAccount().debit(transaction.getAmount());
        }
        return new TransactionDataDTO(transaction);
    }

    @Transactional
    public TransactionDataDTO reopenTransaction(TransactionSettleDTO dto, User user) {
        var transaction = transactionRepository.findMyTransactionById(dto.id(), user.getId()).orElseThrow(() -> new RuntimeException("transaction not found"));
        transaction.reopen();
        switch (transaction.getType()) {
            case INCOME -> transaction.getAccount().debit(transaction.getAmount());
            case EXPENSE -> transaction.getAccount().credit(transaction.getAmount());
        }
        return new TransactionDataDTO(transaction);
    }
}

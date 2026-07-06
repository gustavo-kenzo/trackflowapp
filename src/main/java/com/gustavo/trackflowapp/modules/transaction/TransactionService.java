package com.gustavo.trackflowapp.modules.transaction;

import com.gustavo.trackflowapp.modules.account.Account;
import com.gustavo.trackflowapp.modules.account.AccountService;
import com.gustavo.trackflowapp.modules.category.Category;
import com.gustavo.trackflowapp.modules.category.CategoryService;
import com.gustavo.trackflowapp.modules.transaction.dto.TransactionDataDTO;
import com.gustavo.trackflowapp.modules.transaction.dto.TransactionRegisterDTO;
import com.gustavo.trackflowapp.modules.transaction.dto.TransactionSettleDTO;
import com.gustavo.trackflowapp.modules.transaction.dto.TransactionUpdateDTO;
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
public class TransactionService {

    private final AccountService accountService;
    private final CategoryService categoryService;

    private final TransactionRepository transactionRepository;

    @Transactional
    public TransactionDataDTO create(TransactionRegisterDTO dto, User user) {
        var account = accountService.getAccount(dto.accountId(), user.getId());
        validadeAccountIsActive(account);
        Category category = null;
        if (dto.categoryId() != null)
            category = categoryService.getCategory(dto.categoryId(), user.getId());

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
        if (transaction.getStatus() == TransactionStatus.SETTLED) {
            if (transaction.getType() == TransactionType.INCOME)
                transaction.getAccount().credit(transaction.getAmount());
            else
                transaction.getAccount().debit(transaction.getAmount());
        }

        transactionRepository.save(transaction);
        return new TransactionDataDTO(transaction);
    }

    private void validadeAccountIsActive(Account account) {
        if (!account.isActive())
            throw new BusinessRuleException("This account is inactive. Active first to submit transactions");
    }

    public TransactionDataDTO createByRecurrence() {
        return null;
    }

    @Transactional
    public TransactionDataDTO settleTransaction(TransactionSettleDTO dto, User user) {
        var transaction = transactionRepository.findMyTransactionById(dto.id(), user.getId()).orElseThrow(() -> new ResourceNotFoundException("transaction not found"));
        validadeAccountIsActive(transaction.getAccount());
        transaction.settle(dto.settlementDate());
        switch (transaction.getType()) {
            case INCOME -> transaction.getAccount().credit(transaction.getAmount());
            case EXPENSE -> transaction.getAccount().debit(transaction.getAmount());
        }
        return new TransactionDataDTO(transaction);
    }

    @Transactional
    public TransactionDataDTO reopenTransaction(TransactionSettleDTO dto, User user) {
        var transaction = transactionRepository.findMyTransactionById(dto.id(), user.getId()).orElseThrow(() -> new ResourceNotFoundException("transaction not found"));
        validadeAccountIsActive(transaction.getAccount());
        transaction.reopen();
        switch (transaction.getType()) {
            case INCOME -> transaction.getAccount().debit(transaction.getAmount());
            case EXPENSE -> transaction.getAccount().credit(transaction.getAmount());
        }
        return new TransactionDataDTO(transaction);
    }

    @Transactional
    public TransactionDataDTO updateTransaction(TransactionUpdateDTO dto, User user) {
        var transaction = transactionRepository.findMyTransactionById(dto.id(), user.getId()).orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        validadeAccountIsActive(transaction.getAccount());

        var account = dto.accountId() != null ? accountService.getAccount(dto.accountId(), user.getId()) : null;
        var category = dto.categoryId() != null ? categoryService.getCategory(dto.categoryId(), user.getId()) : null;

        switch (transaction.getStatus()) {
            case PENDING ->
                    transaction.update(account, category, dto.amount(), dto.type(), dto.competenceDate(), dto.description());
            case SETTLED -> transaction.update(null, category, null, null, null, dto.description());
        }
        return new TransactionDataDTO(transaction);
    }

    public Page<TransactionDataDTO> listTransactions(User user, Long categoryId, TransactionType type, TransactionStatus status, Pageable pageable) {
        var transaction = transactionRepository.findMyTransactions(user.getId(), categoryId, type, status, pageable);
        return transaction.map(TransactionDataDTO::new);
    }

    public Page<TransactionDataDTO> listTransactionsInactiveAccount(User user, Long categoryId, TransactionType type, TransactionStatus status, Pageable pageable) {
        var transaction = transactionRepository.findMyTransactionsInactive(user.getId(), categoryId, type, status, pageable);
        return transaction.map(TransactionDataDTO::new);
    }

    @Transactional
    public void deleteTransaction(Long transactionId, User user) {
        var transaction = transactionRepository.findMyTransactionById(transactionId, user.getId()).orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        var account = transaction.getAccount();
        if (transaction.getStatus() == TransactionStatus.SETTLED) {
            switch (transaction.getType()) {
                case EXPENSE -> account.credit(transaction.getAmount());
                case INCOME -> account.debit(transaction.getAmount());
            }
        }
        var rowsAffected = transactionRepository.deleteMyTransaction(transactionId, user.getId());
        if (rowsAffected == 0)
            throw new ResourceNotFoundException("Transaction id not found for hard delete");
    }
}

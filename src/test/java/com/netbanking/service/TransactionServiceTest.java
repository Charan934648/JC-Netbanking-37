package com.netbanking.service;

import com.netbanking.entity.*;
import com.netbanking.exception.InsufficientBalanceException;
import com.netbanking.exception.InvalidTransactionException;
import com.netbanking.repository.AccountRepository;
import com.netbanking.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private TransactionService transactionService;

    private User sourceUser;
    private User targetUser;
    private Account sourceAccount;
    private Account targetAccount;

    @BeforeEach
    void setUp() {
        sourceUser = User.builder()
                .username("user1")
                .password("encoded_pass")
                .email("user1@test.com")
                .role(Role.ROLE_USER)
                .build();

        targetUser = User.builder()
                .username("user2")
                .password("encoded_pass")
                .email("user2@test.com")
                .role(Role.ROLE_USER)
                .build();

        sourceAccount = Account.builder()
                .id(1L)
                .accountNumber("1111111111")
                .accountType("SAVINGS")
                .balance(BigDecimal.valueOf(1000.00))
                .user(sourceUser)
                .build();

        targetAccount = Account.builder()
                .id(2L)
                .accountNumber("2222222222")
                .accountType("SAVINGS")
                .balance(BigDecimal.valueOf(500.00))
                .user(targetUser)
                .build();
    }

    @Test
    void testTransferFunds_Success() {
        // Arrange
        BigDecimal amount = BigDecimal.valueOf(200.00);
        
        when(accountRepository.findByAccountNumberWithLock("1111111111")).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumberWithLock("2222222222")).thenReturn(Optional.of(targetAccount));
        
        Transaction mockTx = Transaction.builder()
                .transactionReference("TXN-123456")
                .sourceAccount(sourceAccount)
                .targetAccount(targetAccount)
                .amount(amount)
                .transferType(TransferType.IMPS)
                .status(TransactionStatus.SUCCESS)
                .build();
        
        when(transactionRepository.save(any(Transaction.class))).thenReturn(mockTx);

        // Act
        Transaction result = transactionService.transferFunds(
                "1111111111",
                "2222222222",
                amount,
                TransferType.IMPS,
                "Test Transfer",
                "user1",
                "127.0.0.1"
        );

        // Assert
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(800.00), sourceAccount.getBalance());
        assertEquals(BigDecimal.valueOf(700.00), targetAccount.getBalance());
        assertEquals(TransactionStatus.SUCCESS, result.getStatus());
        
        verify(accountRepository, times(1)).save(sourceAccount);
        verify(accountRepository, times(1)).save(targetAccount);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(auditLogService, times(1)).log(eq("TRANSFER_SUCCESS"), any(), any(), any());
    }

    @Test
    void testTransferFunds_InsufficientBalance() {
        // Arrange
        BigDecimal amount = BigDecimal.valueOf(1200.00); // Exceeds balance of 1000
        
        when(accountRepository.findByAccountNumberWithLock("1111111111")).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumberWithLock("2222222222")).thenReturn(Optional.of(targetAccount));

        // Act & Assert
        assertThrows(InsufficientBalanceException.class, () -> {
            transactionService.transferFunds(
                    "1111111111",
                    "2222222222",
                    amount,
                    TransferType.IMPS,
                    "Test Transfer",
                    "user1",
                    "127.0.0.1"
            );
        });

        // Verify balance was NOT modified
        assertEquals(BigDecimal.valueOf(1000.00), sourceAccount.getBalance());
        assertEquals(BigDecimal.valueOf(500.00), targetAccount.getBalance());
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(auditLogService, times(1)).log(eq("TRANSFER_FAILED"), any(), any(), any());
    }

    @Test
    void testTransferFunds_ExceedsLimit() {
        // Arrange
        BigDecimal amount = BigDecimal.valueOf(6000000.00); // Exceeds IMPS limit of 5,000,000

        // Act & Assert
        assertThrows(InvalidTransactionException.class, () -> {
            transactionService.transferFunds(
                    "1111111111",
                    "2222222222",
                    amount,
                    TransferType.IMPS,
                    "Test Transfer",
                    "user1",
                    "127.0.0.1"
            );
        });

        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }
}

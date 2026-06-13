package com.netbanking.repository;

import com.netbanking.entity.Account;
import com.netbanking.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findBySourceAccountOrTargetAccountOrderByTimestampDesc(Account sourceAccount, Account targetAccount);

    @Query("select coalesce(sum(t.amount), 0) from Transaction t where t.sourceAccount = :account and t.status = com.netbanking.entity.TransactionStatus.SUCCESS and t.timestamp >= :since")
    BigDecimal sumSuccessfulDebitsSince(@Param("account") Account account, @Param("since") LocalDateTime since);
}

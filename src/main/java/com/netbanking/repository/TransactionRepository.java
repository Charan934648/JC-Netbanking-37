package com.netbanking.repository;

import com.netbanking.entity.Account;
import com.netbanking.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findBySourceAccountOrTargetAccountOrderByTimestampDesc(Account sourceAccount, Account targetAccount);
}

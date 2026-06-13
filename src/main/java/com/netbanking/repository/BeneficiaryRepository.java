package com.netbanking.repository;

import com.netbanking.entity.Beneficiary;
import com.netbanking.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {
    List<Beneficiary> findByUserOrderByCreatedAtDesc(User user);

    Optional<Beneficiary> findByIdAndUser(Long id, User user);

    boolean existsByUserAndAccountNumber(User user, String accountNumber);
}

package com.netbanking.repository;

import com.netbanking.entity.PendingTransfer;
import com.netbanking.entity.PendingTransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PendingTransferRepository extends JpaRepository<PendingTransfer, Long> {
    List<PendingTransfer> findByStatusOrderByCreatedAtAsc(PendingTransferStatus status);
}

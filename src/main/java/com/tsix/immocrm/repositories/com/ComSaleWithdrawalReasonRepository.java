package com.tsix.immocrm.repositories.com;

import com.tsix.immocrm.entities.com.ComSaleWithdrawalReason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ComSaleWithdrawalReasonRepository extends JpaRepository<ComSaleWithdrawalReason, UUID> {
}

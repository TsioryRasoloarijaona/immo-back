package com.tsix.immocrm.repositories.stl;

import com.tsix.immocrm.entities.stl.StlDeposit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StlDepositRepository extends JpaRepository<StlDeposit, UUID> {

    List<StlDeposit> findAllByOrderByDescriptionAsc();
}

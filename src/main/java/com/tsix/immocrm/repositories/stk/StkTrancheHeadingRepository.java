package com.tsix.immocrm.repositories.stk;

import com.tsix.immocrm.entities.stk.StkTrancheHeading;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StkTrancheHeadingRepository extends JpaRepository<StkTrancheHeading, UUID> {

    List<StkTrancheHeading> findAllByOrderBySortingAscCodeAsc();
}

package com.tsix.immocrm.repositories.stl;

import com.tsix.immocrm.entities.stl.StlDeductionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StlDeductionTypeRepository extends JpaRepository<StlDeductionType, UUID> {

    List<StlDeductionType> findAllByOrderBySortingAscCodeAsc();
}

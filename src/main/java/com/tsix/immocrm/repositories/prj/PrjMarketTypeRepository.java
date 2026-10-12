package com.tsix.immocrm.repositories.prj;

import com.tsix.immocrm.entities.prj.PrjMarketType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PrjMarketTypeRepository extends JpaRepository<PrjMarketType, UUID> {
}

package com.tsix.immocrm.repositories.prj;

import com.tsix.immocrm.entities.prj.PrjMarketLineGrouping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PrjMarketLineGroupingRepository extends JpaRepository<PrjMarketLineGrouping, UUID> {
}

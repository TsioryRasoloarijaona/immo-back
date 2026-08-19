package com.tsix.immocrm.repositories.stk;

import com.tsix.immocrm.entities.stk.StkBuilding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StkBuildingRepository extends JpaRepository<StkBuilding, UUID> {
}

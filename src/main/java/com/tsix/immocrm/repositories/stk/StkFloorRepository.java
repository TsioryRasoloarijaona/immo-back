package com.tsix.immocrm.repositories.stk;

import com.tsix.immocrm.entities.stk.StkFloor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StkFloorRepository extends JpaRepository<StkFloor, UUID> {
}

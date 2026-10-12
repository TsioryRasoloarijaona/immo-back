package com.tsix.immocrm.repositories.stk;

import com.tsix.immocrm.entities.stk.StkFloor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StkFloorRepository extends JpaRepository<StkFloor, UUID> {

    /** Ordre d'affichage d'OMRAN (Sous-sol −3 … Étage 10). */
    List<StkFloor> findAllByOrderBySortingAscCodeAsc();
}

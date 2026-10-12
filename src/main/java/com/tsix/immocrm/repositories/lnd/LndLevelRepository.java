package com.tsix.immocrm.repositories.lnd;

import com.tsix.immocrm.entities.lnd.LndLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LndLevelRepository extends JpaRepository<LndLevel, UUID> {

    List<LndLevel> findAllByOrderByCodeAsc();
}

package com.tsix.immocrm.repositories.str;

import com.tsix.immocrm.entities.str.StrHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StrHistoryRepository extends JpaRepository<StrHistory, UUID> {

    Page<StrHistory> findByObjectIdOrderByDocDateDesc(UUID objectId, Pageable pageable);
}

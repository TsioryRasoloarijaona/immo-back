package com.tsix.immocrm.repositories.str;

import com.tsix.immocrm.entities.str.StrStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StrStatusRepository extends JpaRepository<StrStatus, UUID> {

    List<StrStatus> findByStrConfigEntityId(UUID configEntityId);

    StrStatus findByStrConfigEntityIdAndCode(UUID configEntityId, String code);
}

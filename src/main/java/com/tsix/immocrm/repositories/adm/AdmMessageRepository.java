package com.tsix.immocrm.repositories.adm;

import com.tsix.immocrm.entities.adm.AdmMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdmMessageRepository extends JpaRepository<AdmMessage, UUID> {

    List<AdmMessage> findByCategoryIdOrderByDocDateDescIdDesc(UUID categoryId);
}

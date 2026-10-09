package com.tsix.immocrm.repositories.str;

import com.tsix.immocrm.entities.str.StrHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface StrHistoryRepository extends JpaRepository<StrHistory, UUID> {

    Page<StrHistory> findByObjectIdOrderByDocDateDesc(UUID objectId, Pageable pageable);

    List<StrHistory> findByObjectIdInOrderByDocDateDesc(Collection<UUID> objectIds);

    /**
     * Événements écrits par l'API sur un objet : sans statut, avec entité et libellé. Une ligne
     * « jumelle » de statut (sans libellé) n'en est pas un — elle ne sert qu'à signer l'auteur.
     */
    List<StrHistory> findByObjectIdAndStrConfigEntityIdAndStrStatusIdIsNullAndDescriptionIsNotNullOrderByDocDateAsc(
            UUID objectId, UUID strConfigEntityId);
}

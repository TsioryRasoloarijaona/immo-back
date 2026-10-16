package com.tsix.immocrm.repositories.app;

import com.tsix.immocrm.entities.app.AppModeleDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppModeleDocumentRepository extends JpaRepository<AppModeleDocument, UUID> {

    List<AppModeleDocument> findByCategorieAndActifTrueOrderByOrdreAscLibelleAsc(String categorie);

    List<AppModeleDocument> findByCategorieOrderByOrdreAscLibelleAsc(String categorie);

    Optional<AppModeleDocument> findByCategorieAndCode(String categorie, String code);

    boolean existsByCategorieAndCode(String categorie, String code);
}

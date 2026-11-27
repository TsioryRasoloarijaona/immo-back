package com.tsix.immocrm.repositories.app;

import com.tsix.immocrm.domain.StatutDemandeRemise;
import com.tsix.immocrm.entities.app.AppDemandeRemise;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AppDemandeRemiseRepository extends JpaRepository<AppDemandeRemise, UUID> {

    List<AppDemandeRemise> findByDossierIdOrderByDemandeLeDesc(UUID dossierId);

    List<AppDemandeRemise> findByStatutOrderByDemandeLeAsc(StatutDemandeRemise statut);
}

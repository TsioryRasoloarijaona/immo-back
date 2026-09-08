package com.tsix.immocrm.repositories.stl;

import com.tsix.immocrm.entities.stl.StlQuote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface StlQuoteRepository extends JpaRepository<StlQuote, UUID>, JpaSpecificationExecutor<StlQuote> {

    Page<StlQuote> findByComProspectionIdOrderByDocDateDesc(UUID prospectionId, Pageable pageable);

    /**
     * La dernière offre émise sur une prospection, ou rien. Utilisée par l'envoi d'offre de prix
     * ({@code instructions/FEATURE-offre-apres-visite.md} §4) pour réexpédier l'offre existante
     * plutôt que d'en créer une seconde à l'identique — 2,8 % des prospections de l'étape
     * « 4 APRES VISITES » en portent déjà une.
     */
    Optional<StlQuote> findFirstByComProspectionIdOrderByDocDateDesc(UUID prospectionId);
}

package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.domain.TypeActionRecente;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.services.prospection.AlerteRelanceService;
import com.tsix.immocrm.services.prospection.HistoriqueProspection;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Prospections ouvertes des acquéreurs d'un dossier ({@code Sale.cs:GetProspectionsToArchive}) et leur archivage
 * « Création dossier de vente » ({@code ManageProspectionToArchive}), par le chemin de l'archivage manuel : clôture,
 * relances désarmées, historique « archivée ». Voir FEATURE-dossier-finitions.md §2.
 */
@Component
public class ArchivageProspectionsVente {

    private final ComProspectionRepository prospectionRepository;
    private final AlerteRelanceService relances;
    private final HistoriqueProspection historique;
    private final UtilisateurAffiche utilisateurAffiche;
    private final Clock clock;
    private final EntityManager entityManager;

    public ArchivageProspectionsVente(ComProspectionRepository prospectionRepository, AlerteRelanceService relances,
                                      HistoriqueProspection historique, UtilisateurAffiche utilisateurAffiche,
                                      Clock clock, EntityManager entityManager) {
        this.prospectionRepository = prospectionRepository;
        this.relances = relances;
        this.historique = historique;
        this.utilisateurAffiche = utilisateurAffiche;
        this.clock = clock;
        this.entityManager = entityManager;
    }

    /** Ouvertes ({@code IsClosed = 0}), d'un des tiers donnés, hors {@code exclue} (la prospection du dossier). */
    public List<ComProspection> ouvertes(Collection<UUID> tiers, UUID exclue) {
        if (tiers.isEmpty()) {
            return List.of();
        }
        return entityManager.createQuery(
                        "SELECT p FROM ComProspection p WHERE p.cfgProspect.id IN :tiers AND p.isClosed = false",
                        ComProspection.class)
                .setParameter("tiers", tiers)
                .getResultList().stream()
                .filter(p -> !p.getId().equals(exclue))
                .toList();
    }

    /** Archive les prospections ouvertes des tiers donnés ; rend leurs codes, triés. */
    public List<String> archiverCellesDe(Collection<UUID> tiers, UUID exclue) {
        List<ComProspection> aArchiver = ouvertes(tiers, exclue);
        aArchiver.forEach(this::archiver);
        return aArchiver.stream()
                .map(ComProspection::getCode)
                .sorted(Comparator.nullsLast(Comparator.naturalOrder()))
                .toList();
    }

    public void archiver(ComProspection prospection) {
        LocalDateTime maintenant = LocalDateTime.now(clock);
        prospection.setIsClosed(true);
        prospection.setClosingDate(maintenant);
        prospection.setClosingReasonText(DossierService.TEXTE_CLOTURE_VENTE);
        prospection.setUserUpdate(utilisateurAffiche.courant().nom());
        prospection.setUserUpdateDateTime(maintenant);
        prospectionRepository.save(prospection);
        relances.desarmerToutes(prospection.getId());
        historique.enregistrerHistorique(prospection.getId(), TypeActionRecente.ARCHIVEE,
                DossierService.TEXTE_CLOTURE_VENTE);
    }
}

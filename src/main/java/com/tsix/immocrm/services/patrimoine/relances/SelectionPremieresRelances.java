package com.tsix.immocrm.services.patrimoine.relances;

import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.cfg.TrancheVisibleRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Seule sélection des premières relances ({@code CfgTranche.GenerateLandTitleRevival}) : aperçu, écriture et lettres.
 * Voir FEATURE-patrimoine-import-export.md §4.1.
 */
@Component
public class SelectionPremieresRelances {

    static final String SANS_DATE = "Vous devez saisir une Date de 1ère relance!";
    // Jointure interne sur le statut : un dossier sans statut est exclu, comme OMRAN.
    private static final String DOSSIERS = """
            SELECT f FROM ComFolder f JOIN FETCH f.strStatus s
            WHERE EXISTS (SELECT 1 FROM StkItem i WHERE i.comFolderId = f.id AND i.stkHierarchy.id = :tranche
                          AND i.isUnderItem = false)
            AND (s.annulation IS NULL OR s.annulation = false) AND (s.resiliation IS NULL OR s.resiliation = false)
            ORDER BY f.code ASC""";

    private final TrancheVisibleRepository tranches;
    private final EntityManager em;

    public SelectionPremieresRelances(TrancheVisibleRepository tranches, EntityManager em) {
        this.tranches = tranches;
        this.em = em;
    }

    /** {@code alerte} : échéance du dossier changée et prospection présente (comparée avant toute écriture). */
    public record Relance(ComFolder dossier, boolean alerte) {
    }

    /** {@code premierDelai} null = 0 jour. */
    public record Selection(CfgTranche tranche, LocalDateTime dateRelance, Integer premierDelai, LocalDateTime echeance,
                            List<Relance> relances) {

        public long alertes() {
            return relances.stream().filter(Relance::alerte).count();
        }
    }

    /** 404 tranche invisible, 400 sans {@code Revival1Date}. */
    public Selection selectionner(UUID trancheId) {
        CfgTranche tranche = tranches.exigerVisible(trancheId);
        if (tranche.getRevival1Date() == null) {
            throw new InvalidRequestException(SANS_DATE);
        }
        LocalDateTime dateRelance = tranche.getRevival1Date();
        Integer delai = tranche.getAgreementFirstDelay();
        LocalDateTime echeance = dateRelance.plusDays(delai == null ? 0 : delai);
        List<Relance> relances = em.createQuery(DOSSIERS, ComFolder.class).setParameter("tranche", trancheId)
                .getResultList().stream()
                .map(d -> new Relance(d, d.getComProspection() != null
                        && !Objects.equals(d.getRevival1ValidityDate(), echeance)))
                .toList();
        return new Selection(tranche, dateRelance, delai, echeance, relances);
    }
}

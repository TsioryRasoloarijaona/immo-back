package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.domain.MontantRestant;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Seule sélection de « Mettre à jour les schémas de financement », pour l'aperçu et l'exécution
 * ({@code Tranche.cs:btnUpdate_Click}, FEATURE-patrimoine-tranche-regles.md §4, §9.5, §11.8).
 */
@Component
public class SelectionDocumentsSchemas {

    /** Dossier courant = celui que l'article pointe : les dossiers annulés ou désistés en sont exclus. */
    private static final String CANDIDATS = "SELECT d FROM ComDocument d JOIN FETCH d.comFolder f "
            + "JOIN FETCH d.comSettlementCategory c WHERE c.isConfermentBasedDate = true AND d.docDate < :debut "
            + "AND f.id IN (SELECT i.comFolderId FROM StkItem i WHERE i.stkHierarchy.id = :tranche) "
            + "ORDER BY f.code, d.docDate, d.id";

    private final EntityManager em;
    private final StlSettlementLineRepository reglements;

    public SelectionDocumentsSchemas(EntityManager em, StlSettlementLineRepository reglements) {
        this.em = em;
        this.reglements = reglements;
    }

    /** Échéance à déplacer et son reste dû calculé (jamais la colonne {@code AmountRest}). */
    public record Eligible(ComDocument echeance, BigDecimal reste) {
    }

    public static LocalDateTime debutDesFinalisations(CfgTranche tranche) {
        if (tranche.getStartDateFinalisation() == null) {
            throw new InvalidRequestException("Vous devez saisir la date de début des finalisations");
        }
        return tranche.getStartDateFinalisation();
    }

    public List<Eligible> selection(CfgTranche tranche) {
        List<ComDocument> candidats = em.createQuery(CANDIDATS, ComDocument.class)
                .setParameter("debut", debutDesFinalisations(tranche)).setParameter("tranche", tranche.getId())
                .getResultList();
        Map<UUID, BigDecimal> regles = reglements.reglesParEcheance(candidats.stream().map(ComDocument::getId).toList());
        return candidats.stream()
                .map(d -> new Eligible(d, MontantRestant.echeance(d.getAmount(), regles.get(d.getId()))))
                .filter(e -> e.reste().signum() > 0)
                .toList();
    }
}

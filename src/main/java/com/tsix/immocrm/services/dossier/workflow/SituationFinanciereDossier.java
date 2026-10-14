package com.tsix.immocrm.services.dossier.workflow;

import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.requireNonNullElse;

/**
 * Charge la {@link SituationFinanciere} d'un dossier : échéancier ({@code ComDocument}) et réglé de chaque échéance
 * par la seule règle de signe du réglé ({@link StlSettlementLineRepository#MONTANT_SIGNE}).
 */
@Component
public class SituationFinanciereDossier {

    private final ComDocumentRepository documentRepository;
    private final StlSettlementLineRepository settlementLineRepository;

    public SituationFinanciereDossier(ComDocumentRepository documentRepository,
                                      StlSettlementLineRepository settlementLineRepository) {
        this.documentRepository = documentRepository;
        this.settlementLineRepository = settlementLineRepository;
    }

    public SituationFinanciere de(ComFolder dossier) {
        Map<UUID, BigDecimal> regle = new HashMap<>();
        for (Object[] ligne : settlementLineRepository.sumAmountByComFolderIdGroupedByComDocument(dossier.getId())) {
            regle.put((UUID) ligne[0], (BigDecimal) ligne[1]);
        }
        Cumul cumul = new Cumul();
        for (ComDocument document : documentRepository.findByComFolderIdOrderByDocDateAsc(dossier.getId())) {
            cumul.ajouter(document, regle.getOrDefault(document.getId(), BigDecimal.ZERO));
        }
        return new SituationFinanciere(cumul.avance, cumul.credit, cumul.autoFinancement, cumul.avanceReglee,
                cumul.creditRegle, cumul.autoFinancementRegle, cumul.modifications, cumul.echeances);
    }

    private static final class Cumul {
        private BigDecimal avance = BigDecimal.ZERO;
        private BigDecimal credit = BigDecimal.ZERO;
        private BigDecimal autoFinancement = BigDecimal.ZERO;
        private BigDecimal avanceReglee = BigDecimal.ZERO;
        private BigDecimal creditRegle = BigDecimal.ZERO;
        private BigDecimal autoFinancementRegle = BigDecimal.ZERO;
        private BigDecimal modifications = BigDecimal.ZERO;
        private final List<SituationFinanciere.Echeance> echeances = new ArrayList<>();

        void ajouter(ComDocument document, BigDecimal regleLigne) {
            ComSettlementCategory c = document.getComSettlementCategory();
            if (c == null) {
                return;
            }
            BigDecimal montant = requireNonNullElse(document.getAmount(), BigDecimal.ZERO);
            boolean auCredit = Boolean.TRUE.equals(c.getIsCredit());
            // Sale.cs : montants au crédit moins montants au débit ; réglés et modifications au crédit seulement.
            BigDecimal signe = auCredit ? montant : montant.negate();
            if (Boolean.TRUE.equals(c.getIsSaleAdvance())) {
                avance = avance.add(signe);
                avanceReglee = auCredit ? avanceReglee.add(regleLigne) : avanceReglee;
            }
            if (Boolean.TRUE.equals(c.getIsSaleCredit())) {
                credit = credit.add(signe);
                creditRegle = auCredit ? creditRegle.add(regleLigne) : creditRegle;
            }
            if (Boolean.TRUE.equals(c.getIsSaleAutoFinancment())) {
                autoFinancement = autoFinancement.add(signe);
                autoFinancementRegle = auCredit ? autoFinancementRegle.add(regleLigne) : autoFinancementRegle;
            }
            if (Boolean.TRUE.equals(c.getIsSaleModification()) && auCredit) {
                modifications = modifications.add(montant);
            }
            boolean creditBancaire = Boolean.TRUE.equals(c.getIsSaleCredit());
            boolean auto = Boolean.TRUE.equals(c.getIsSaleAutoFinancment());
            if (auCredit && (creditBancaire || auto)) {
                echeances.add(new SituationFinanciere.Echeance(document.getDocDate(), creditBancaire, auto,
                        montant.subtract(regleLigne)));
            }
        }
    }
}

package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.domain.MontantRestant;
import com.tsix.immocrm.dto.dossier.AffectationReglementRequest;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.utils.Champs;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Contrôles d'une répartition de règlement sur les échéances d'un dossier (FEATURE-reglement-multi-echeances.md
 * §2.1 règles 3 à 7). Ne fait que lire : l'appelant n'écrit qu'une fois tout validé.
 */
@Component
public class AffectationsReglement {

    /** Une échéance du dossier et le montant qui lui est imputé, déjà contrôlés. */
    public record LigneAffectee(ComDocument echeance, BigDecimal montant) {
    }

    private static final int ECHELLE_MONTANT = 6;

    private final ComDocumentRepository documentRepository;
    private final StlSettlementLineRepository settlementLineRepository;

    public AffectationsReglement(ComDocumentRepository documentRepository,
                                 StlSettlementLineRepository settlementLineRepository) {
        this.documentRepository = documentRepository;
        this.settlementLineRepository = settlementLineRepository;
    }

    /**
     * @param dejaAffectees échéances qui ont déjà une ligne dans le règlement (vide à la création)
     */
    public List<LigneAffectee> valider(UUID dossierId, List<AffectationReglementRequest> affectations,
                                       Collection<UUID> dejaAffectees) {
        if (affectations == null || affectations.isEmpty()) {
            throw new InvalidRequestException("Au moins une échéance doit être affectée");
        }
        Set<UUID> vues = new HashSet<>(dejaAffectees);
        List<LigneAffectee> lignes = new ArrayList<>();
        for (AffectationReglementRequest affectation : affectations) {
            lignes.add(validerUne(dossierId, affectation, vues));
        }
        return lignes;
    }

    private LigneAffectee validerUne(UUID dossierId, AffectationReglementRequest affectation, Set<UUID> vues) {
        if (affectation == null || affectation.echeanceId() == null) {
            throw new InvalidRequestException("echeanceId est requis pour chaque affectation");
        }
        UUID echeanceId = affectation.echeanceId();
        if (!vues.add(echeanceId)) {
            throw new InvalidRequestException("L'échéance " + echeanceId
                    + " figure déjà dans ce règlement : une échéance ne peut être affectée qu'une fois");
        }
        BigDecimal montant = affectation.montant();
        // À l'échelle de la colonne decimal(26,6) : 0,0000001 s'écrirait 0, la ligne à 0 qu'OMRAN refuse.
        if (montant == null || montant.setScale(ECHELLE_MONTANT, RoundingMode.HALF_UP).signum() <= 0) {
            throw new InvalidRequestException("Le montant affecté à l'échéance " + echeanceId
                    + " doit être strictement positif");
        }
        ComDocument echeance = documentRepository.findById(echeanceId)
                .filter(d -> d.getComFolder() != null && dossierId.equals(d.getComFolder().getId()))
                .orElseThrow(() -> new InvalidRequestException(
                        "L'échéance " + echeanceId + " n'appartient pas à ce dossier"));
        BigDecimal reste = resteDu(echeance);
        if (montant.compareTo(reste) > 0) {
            throw new InvalidRequestException("montant (" + montant.toPlainString() + ") dépasse le reste à payer "
                    + "de l'échéance " + libelle(echeance) + " (" + reste.toPlainString() + ")");
        }
        return new LigneAffectee(echeance, montant);
    }

    /** Le reste que l'échéancier affiche, lu avant ce règlement. */
    BigDecimal resteDu(ComDocument echeance) {
        return MontantRestant.echeance(echeance.getAmount(),
                settlementLineRepository.sumAmountByComDocumentId(echeance.getId()));
    }

    public static BigDecimal somme(List<LigneAffectee> lignes) {
        return lignes.stream().map(LigneAffectee::montant).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Ce que l'échéancier affiche pour une ligne : code de catégorie, à défaut sa description. */
    public static String libelle(ComDocument echeance) {
        String categorie = Champs.de(echeance.getComSettlementCategory(), ComSettlementCategory::getCode);
        return StringUtils.hasText(categorie) ? categorie : echeance.getDescription();
    }
}

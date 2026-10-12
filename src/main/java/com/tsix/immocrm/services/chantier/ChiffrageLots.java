package com.tsix.immocrm.services.chantier;

import com.tsix.immocrm.domain.chantier.CumulsDecomptes;
import com.tsix.immocrm.domain.chantier.LigneMontant;
import com.tsix.immocrm.domain.chantier.MontantsLot;
import com.tsix.immocrm.domain.chantier.SoldeFacture;
import com.tsix.immocrm.domain.chantier.Totaux;
import com.tsix.immocrm.entities.buy.BuyDocument;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.repositories.prj.PrjMarketLineEstimationRepository;
import com.tsix.immocrm.repositories.prj.PrjMarketLineRepository;
import com.tsix.immocrm.repositories.prj.PrjWorkingOutRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Chiffre un ensemble de lots en quelques requêtes groupées, puis applique {@link MontantsLot} et
 * {@link SoldeFacture} lot par lot : liste, fiche et totaux projet passent tous par ici.
 */
@Component
public class ChiffrageLots {

    private final PrjMarketLineRepository ligneRepository;
    private final PrjMarketLineEstimationRepository estimationRepository;
    private final PrjWorkingOutRepository decompteRepository;
    private final StlSettlementLineRepository reglementLigneRepository;

    public ChiffrageLots(PrjMarketLineRepository ligneRepository,
                         PrjMarketLineEstimationRepository estimationRepository,
                         PrjWorkingOutRepository decompteRepository,
                         StlSettlementLineRepository reglementLigneRepository) {
        this.ligneRepository = ligneRepository;
        this.estimationRepository = estimationRepository;
        this.decompteRepository = decompteRepository;
        this.reglementLigneRepository = reglementLigneRepository;
    }

    /** Clé = id du lot, dans l'ordre des lots reçus. */
    public Map<UUID, LotChiffre> chiffrer(Collection<PrjMarket> lots) {
        List<UUID> ids = lots.stream().map(PrjMarket::getId).toList();
        Map<UUID, List<LigneMontant>> bordereaux = parLot(ParBlocs.executer(ids, ligneRepository::findLignesMontant));
        Map<UUID, List<LigneMontant>> estimations =
                parLot(ParBlocs.executer(ids, estimationRepository::findLignesMontant));
        Map<UUID, CumulsDecomptes> cumuls = cumulsParLot(ids);
        Map<UUID, SoldeFacture> soldes = soldesParLot(ids);

        Map<UUID, LotChiffre> resultat = new LinkedHashMap<>();
        for (PrjMarket lot : lots) {
            CumulsDecomptes cumul = cumuls.getOrDefault(lot.getId(), CumulsDecomptes.AUCUN);
            Totaux forfait = Totaux.forfait(lot.getEstimatedPriceHTManual(), lot.getEstimatedVatAmountManual(),
                    lot.getEstimatedPriceTTCManual());
            MontantsLot montants = MontantsLot.calculer(
                    Totaux.deLignes(bordereaux.getOrDefault(lot.getId(), List.of())),
                    Totaux.deLignes(estimations.getOrDefault(lot.getId(), List.of())),
                    forfait, lot.getWarrantyRatio(), lot.getAdvanceRatio(), cumul.ttc());
            resultat.put(lot.getId(), new LotChiffre(montants, forfait, cumul,
                    soldes.getOrDefault(lot.getId(), SoldeFacture.AUCUNE)));
        }
        return resultat;
    }

    /** Réglé non annulé par facture ; une facture sans règlement est absente de la map. */
    public Map<UUID, BigDecimal> regleParFacture(Collection<UUID> factureIds) {
        Map<UUID, BigDecimal> regle = new HashMap<>();
        for (Object[] row : ParBlocs.executer(Set.copyOf(factureIds), reglementLigneRepository::sumAmountByBuyDocumentIds)) {
            regle.put((UUID) row[0], (BigDecimal) row[1]);
        }
        return regle;
    }

    /** Solde d'une facture de décompte ; {@link SoldeFacture#AUCUNE} si le décompte n'est pas facturé. */
    public static SoldeFacture solde(BuyDocument facture, Map<UUID, BigDecimal> regleParFacture) {
        if (facture == null) {
            return SoldeFacture.AUCUNE;
        }
        return SoldeFacture.de(facture.getAmountToPay(), regleParFacture.get(facture.getId()));
    }

    private static Map<UUID, List<LigneMontant>> parLot(List<LigneMontant> lignes) {
        return lignes.stream().collect(Collectors.groupingBy(LigneMontant::lotId));
    }

    private Map<UUID, CumulsDecomptes> cumulsParLot(List<UUID> ids) {
        Map<UUID, CumulsDecomptes> cumuls = new HashMap<>();
        for (Object[] row : ParBlocs.executer(ids, decompteRepository::sommesParLot)) {
            cumuls.put((UUID) row[0], CumulsDecomptes.de(((Number) row[1]).longValue(), (BigDecimal) row[2],
                    (BigDecimal) row[3], (BigDecimal) row[4], (BigDecimal) row[5], (BigDecimal) row[6]));
        }
        return cumuls;
    }

    private Map<UUID, SoldeFacture> soldesParLot(List<UUID> ids) {
        List<Object[]> factures = ParBlocs.executer(ids, decompteRepository::facturesParLot);
        Set<UUID> factureIds = new HashSet<>();
        factures.forEach(row -> factureIds.add((UUID) row[1]));
        Map<UUID, BigDecimal> regle = regleParFacture(factureIds);

        Map<UUID, SoldeFacture> soldes = new HashMap<>();
        for (Object[] row : factures) {
            SoldeFacture solde = SoldeFacture.de((BigDecimal) row[2], regle.get(row[1]));
            soldes.merge((UUID) row[0], solde, SoldeFacture::plus);
        }
        return soldes;
    }
}

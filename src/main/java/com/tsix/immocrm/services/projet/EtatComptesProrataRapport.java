package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.projet.MontantsLot;
import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.domain.rapport.TypeColonne;
import com.tsix.immocrm.entities.prj.PrjWorkingOut;
import com.tsix.immocrm.services.projet.LotsProjetService.LotCalcule;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** R4 {@code PrjProrataState} « ETAT DES COMPTES PRORATA » : retenues par lot, puis remboursements par décompte. */
@Component
public class EtatComptesProrataRapport extends RapportProjet {

    private static final String DECOMPTE = "decompte";

    public EtatComptesProrataRapport(SourcesRapportsProjet sources) {
        super(sources);
    }

    @Override
    public String code() {
        return "PrjProrataState";
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        List<LotCalcule> lots = lots(projetsDemandes(contexte));
        List<Map<String, Object>> retenues = lots.stream().map(l -> {
            Map<String, Object> ligne = ligne(l.lot(), l.lot().getPrjProject().getDescription());
            ligne.put("taux", MontantsLot.pourcentage(l.lot().getProportionRatio()));
            ligne.put("previsionnel", l.calcul().prorataPrevisionnel());
            ligne.put(DECOMPTE, l.calcul().prorataDecompte());
            return ligne;
        }).toList();
        return new ResultatRapport(List.of(
                new SectionRapport("prorata", "Retenues", avec(colonnesLot(),
                        sansTotal("taux", "Taux", TypeColonne.POURCENTAGE),
                        ColonneRapport.montant("previsionnel", "Prévisionnel", null),
                        ColonneRapport.montant(DECOMPTE, "Décompté", null)), retenues, null, null, List.of()),
                new SectionRapport("remboursements", "Remboursements", avec(colonnesLot(),
                        ColonneRapport.texte(DECOMPTE, "Décompte"),
                        sansTotal("date", "Date", TypeColonne.DATE),
                        ColonneRapport.montant("rembourse", "Remboursé", null)), remboursements(lots), null, null,
                        List.of())), List.of());
    }

    /** Un décompte à remboursement non nul d'un lot visible, trié par date (null en tête, comme SQL Server). */
    private List<Map<String, Object>> remboursements(List<LotCalcule> lots) {
        if (lots.isEmpty()) {
            return List.of();
        }
        Map<UUID, LotCalcule> parId = lots.stream().collect(Collectors.toMap(l -> l.lot().getId(), Function.identity()));
        List<UUID> projets = lots.stream().map(l -> l.lot().getPrjProject().getId()).distinct().toList();
        return sources.em().createQuery("SELECT w FROM PrjWorkingOut w WHERE w.prjMarket.prjProject.id IN :projets"
                        + " AND w.prorataRefundAmount <> 0 ORDER BY w.docDate ASC", PrjWorkingOut.class)
                .setParameter("projets", projets).getResultList().stream()
                .filter(w -> parId.containsKey(w.getPrjMarket().getId()))
                .map(w -> {
                    LotCalcule l = parId.get(w.getPrjMarket().getId());
                    Map<String, Object> ligne = ligne(l.lot(), l.lot().getPrjProject().getCode());
                    ligne.put(DECOMPTE, w.getDescription());
                    ligne.put("date", w.getDocDate() == null ? null : w.getDocDate().toLocalDate());
                    ligne.put("rembourse", MontantsLot.arrondi(w.getProrataRefundAmount()));
                    return ligne;
                }).toList();
    }
}

package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.dto.projet.BlocRetenueDto;
import com.tsix.immocrm.dto.projet.RetenuesLotDto;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.services.projet.LotsProjetService.LotCalcule;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** R2 / R3 : lots du filtre de R1 dont le taux d'assurance est non nul (FEATURE-n5b-rapports-projet.md). */
abstract class EtatAssurancesRapport extends RapportProjet {

    private final String code;
    private final String titre;
    private final Function<PrjMarket, BigDecimal> ratio;
    private final Function<RetenuesLotDto, BlocRetenueDto> bloc;

    EtatAssurancesRapport(SourcesRapportsProjet sources, String code, String titre, Function<PrjMarket, BigDecimal> ratio,
                          Function<RetenuesLotDto, BlocRetenueDto> bloc) {
        super(sources);
        this.code = code;
        this.titre = titre;
        this.ratio = ratio;
        this.bloc = bloc;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        List<Map<String, Object>> lignes = new ArrayList<>();
        for (LotCalcule l : lotsExiges(contexte)) {
            BigDecimal taux = ratio.apply(l.lot());
            if (taux != null && taux.signum() != 0 && Garantie.de(l).nonNulle()) {
                Map<String, Object> ligne = ligne(l.lot(), l.lot().getPrjProject().getCode());
                ligne.put("montantCollecte", bloc.apply(l.calcul().suivi().retenues()).facture().total());
                ligne.put("montantEstime", l.calcul().general().marche().ttc().multiply(taux)
                        .setScale(2, RoundingMode.HALF_UP));
                lignes.add(ligne);
            }
        }
        List<ColonneRapport> colonnes = avec(colonnesLot(), ColonneRapport.montant("montantCollecte", "Collecté", null),
                ColonneRapport.montant("montantEstime", "Estimé (marché initial × taux)", null));
        return new ResultatRapport(List.of(new SectionRapport("assurances", titre, colonnes, lignes, null, null,
                List.of())), List.of());
    }
}

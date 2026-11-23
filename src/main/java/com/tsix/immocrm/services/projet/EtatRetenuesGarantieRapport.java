package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.domain.rapport.TypeColonne;
import com.tsix.immocrm.services.projet.LotsProjetService.LotCalcule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** R1 {@code PRJWarrantyStatus} « ETAT DES RETENUES DE GARANTIE » (FEATURE-n5b-rapports-projet.md). */
@Component
public class EtatRetenuesGarantieRapport extends RapportProjet {

    public EtatRetenuesGarantieRapport(SourcesRapportsProjet sources) {
        super(sources);
    }

    @Override
    public String code() {
        return "PRJWarrantyStatus";
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        List<Map<String, Object>> lignes = new ArrayList<>();
        for (LotCalcule l : lotsExiges(contexte)) {
            Garantie g = Garantie.de(l);
            if (g.nonNulle()) {
                Map<String, Object> ligne = ligne(l.lot(), l.lot().getPrjProject().getCode());
                ligne.put("marcheAjusteTtc", l.calcul().general().marcheAjuste().ttc());
                ligne.put("retenue", g.retenue());
                ligne.put("restituee", g.restituee());
                ligne.put("resteARestituer", g.reste());
                ligne.put("penalite", g.penalite());
                ligne.put("bloque", Boolean.TRUE.equals(l.lot().getBlokWarrantyRestitution()));
                lignes.add(ligne);
            }
        }
        List<ColonneRapport> colonnes = avec(colonnesLot(),
                sansTotal("marcheAjusteTtc", "Marché ajusté TTC", TypeColonne.MONTANT),
                ColonneRapport.montant("retenue", "Retenue", null), ColonneRapport.montant("restituee", "Restituée", null),
                ColonneRapport.montant("resteARestituer", "Reste à restituer", null),
                ColonneRapport.montant("penalite", "Pénalité", null),
                sansTotal("bloque", "Restitution bloquée", TypeColonne.BOOLEEN));
        return new ResultatRapport(List.of(new SectionRapport("retenues", "État des retenues de garantie", colonnes,
                lignes, null, null, List.of())), List.of());
    }
}

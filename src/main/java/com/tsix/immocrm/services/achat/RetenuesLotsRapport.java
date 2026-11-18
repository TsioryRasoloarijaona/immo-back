package com.tsix.immocrm.services.achat;

import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.dto.projet.SuiviLotDto;
import com.tsix.immocrm.services.projet.LotsProjetService;
import com.tsix.immocrm.services.projet.LotsProjetService.SuiviLot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * R13 « SUIVI DES RETENUES » (FEATURE-n7-etats-achat.md R13) : une ligne par lot visible dont un montant est non nul ;
 * montants = onglet Suivi de la fiche lot ({@link LotsProjetService#suivis}), jamais la vue {@code PrjMarketMonitoringView}.
 */
@Component
public class RetenuesLotsRapport extends RapportAchat {

    private static final String PROJETS = "projets";
    private static final String AVANCE = "Avance";
    private static final String GARANTIE = "Retenue de garantie";
    private static final String FACTURE = "Facturé";

    private record Montant(String nom, String libelle, String groupe, Function<SuiviLotDto, BigDecimal> valeur) {
    }

    private static final List<Montant> MONTANTS = List.of(
            new Montant("avanceReglee", "Réglée", AVANCE, s -> s.avanceMarche().reglee()),
            new Montant("avanceAmortie", "Amortie", AVANCE, s -> s.avanceMarche().amortie()),
            new Montant("avanceResteAAmortir", "Reste à amortir", AVANCE, s -> s.avanceMarche().resteAAmortir()),
            new Montant("retenueGarantie", "Retenue", GARANTIE, s -> s.retenueGarantie().retenue()),
            new Montant("retenueRestituee", "Restituée", GARANTIE, s -> s.retenueGarantie().restituee()),
            new Montant("retenueResteARestituer", "Reste à restituer", GARANTIE, s -> s.retenueGarantie().resteARestituer()),
            new Montant("prorataFacture", "Prorata", FACTURE, s -> s.retenues().compteProrata().facture().total()),
            new Montant("decennaleFacturee", "Décennale", FACTURE, s -> s.retenues().assuranceDecennale().facture().total()),
            new Montant("trcFacturee", "TRC", FACTURE, s -> s.retenues().assuranceTrc().facture().total()),
            new Montant("retenueForfaitaireFacturee", "Retenue forfaitaire", FACTURE,
                    s -> s.retenues().facture().retenueForfaitaire()),
            new Montant("prorataRembourse", "Prorata remboursé", null, s -> s.retenueGarantie().remboursement()));

    private final LotsProjetService lots;
    private final RequetesRapportsAchat requetes;

    public RetenuesLotsRapport(LotsProjetService lots, RequetesRapportsAchat requetes) {
        this.lots = lots;
        this.requetes = requetes;
    }

    @Override
    public String code() {
        return "BuyTdbMarketsRetainedAmounts";
    }

    @Override
    public List<ParametreRapport> parametres() {
        return List.of(requetes.projets(PROJETS, "Projets", true));
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        List<UUID> projets = contexte.choix(PROJETS).stream().map(UUID::fromString).toList();
        List<Map<String, Object>> lignes = new ArrayList<>();
        for (SuiviLot s : lots.suivis(projets)) {
            Map<String, Object> ligne = new LinkedHashMap<>();
            MONTANTS.forEach(m -> ligne.put(m.nom(), m.valeur().apply(s.suivi())));
            if (ligne.values().stream().anyMatch(v -> v instanceof BigDecimal b && b.signum() != 0)) {
                Map<String, Object> complete = new LinkedHashMap<>();
                complete.put("code", s.lot().getCode());
                complete.put("description", s.lot().getDescription());
                complete.put("projet", s.lot().getPrjProject().getDescription());
                complete.putAll(ligne);
                lignes.add(complete);
            }
        }
        List<ColonneRapport> colonnes = Stream.concat(Stream.of(ColonneRapport.texte("code", "Lot"),
                        ColonneRapport.texte("description", "Description"), ColonneRapport.texte("projet", "Projet")),
                MONTANTS.stream().map(m -> ColonneRapport.montant(m.nom(), m.libelle(), m.groupe()))).toList();
        return resultat(new SectionRapport("retenues", "Suivi des retenues", colonnes, lignes, null, null, List.of()));
    }
}

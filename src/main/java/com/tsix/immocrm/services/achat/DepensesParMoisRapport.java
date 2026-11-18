package com.tsix.immocrm.services.achat;

import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.FonctionTotal;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.domain.rapport.TypeColonne;
import com.tsix.immocrm.services.rapport.vente.flux.LignesMensuelles;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** R12 « SUIVI DES DEPENSES PAR MOIS » (FEATURE-n7-etats-achat.md R12, Q4) : prévu, réel, décaissements et leurs cumuls. */
@Component
public class DepensesParMoisRapport extends RapportAchat {

    private static final List<String> SOURCES = List.of("P", "R", "D");
    private static final List<String> COLONNES = List.of("prevu", "reel", "decaissements");
    private static final List<String> CUMULS = List.of("cumulPrevu", "cumulReel", "cumulDecaissements");
    private static final List<String> LIBELLES = List.of("Prévu", "Réel", "Décaissements");

    private final RequetesRapportsAchat requetes;
    private final Clock clock;

    public DepensesParMoisRapport(RequetesRapportsAchat requetes, Clock clock) {
        this.requetes = requetes;
        this.clock = clock;
    }

    @Override
    public String code() {
        return "BuyTdbActivityByMoth";
    }

    @Override
    public List<ParametreRapport> parametres() {
        return List.of(LignesMensuelles.annee(clock));
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        int annee = annee(contexte);
        BigDecimal[][] parMois = new BigDecimal[SOURCES.size()][13];
        Arrays.stream(parMois).forEach(s -> Arrays.fill(s, BigDecimal.ZERO));
        // PrevisionMonth est saisi librement dans OMRAN : un mois nul ou hors 1–12 est ignoré.
        requetes.depenses(annee).stream().filter(m -> m.mois() >= 1 && m.mois() <= 12 && m.montant() != null)
                .forEach(m -> {
                    int i = SOURCES.indexOf(m.colonne());
                    parMois[i][m.mois()] = parMois[i][m.mois()].add(m.montant());
                });
        BigDecimal[] cumuls = {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
        List<Map<String, Object>> lignes = LignesMensuelles.douzeMois((ligne, mois) -> {
            for (int i = 0; i < SOURCES.size(); i++) {
                cumuls[i] = cumuls[i].add(parMois[i][mois]);
                ligne.put(COLONNES.get(i), parMois[i][mois]);
                ligne.put(CUMULS.get(i), cumuls[i]);
            }
        });
        List<ColonneRapport> colonnes = new ArrayList<>(List.of(ColonneRapport.texte(LignesMensuelles.MOIS, "Mois")));
        for (int i = 0; i < SOURCES.size(); i++) {
            colonnes.add(ColonneRapport.montant(COLONNES.get(i), LIBELLES.get(i), null));
        }
        for (int i = 0; i < SOURCES.size(); i++) {
            colonnes.add(new ColonneRapport(CUMULS.get(i), LIBELLES.get(i), TypeColonne.MONTANT, "Cumul",
                    FonctionTotal.AUCUNE, true, 0));
        }
        return resultat(new SectionRapport(LignesMensuelles.MOIS, "Suivi des dépenses par mois — %d".formatted(annee),
                colonnes, lignes, null, null, List.of()));
    }
}

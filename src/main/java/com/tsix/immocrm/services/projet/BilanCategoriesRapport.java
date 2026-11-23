package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.domain.rapport.TypeColonne;
import com.tsix.immocrm.dto.projet.NoeudBilanDto;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.services.projet.LotsProjetService.LotCalcule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.services.projet.BilansProjetService.ajouter;
import static com.tsix.immocrm.services.projet.travaux.ValeursNatives.decimal;
import static com.tsix.immocrm.services.projet.travaux.ValeursNatives.uuid;
import static java.math.BigDecimal.ZERO;

/** R1 {@code PrjTdbBilan} « BILAN » : montants TTC des lots visibles ventilés par catégorie (FEATURE-n5b-b-rapports-projet.md). */
@Component
public class BilanCategoriesRapport extends RapportProjet {

    private static final String CATEGORIE = "categorie";
    private static final String NIVEAU = "niveau";
    private static final List<ColonneRapport> MONTANTS = List.of(
            sansTotal("estimationBase", "Estimation de base", TypeColonne.MONTANT),
            sansTotal("estimationConfidentielle", "Estimation confidentielle", TypeColonne.MONTANT),
            sansTotal("marche", "Marché", TypeColonne.MONTANT),
            sansTotal("cumulTravaux", "Cumul travaux", TypeColonne.MONTANT),
            sansTotal("aPayer", "À payer", TypeColonne.MONTANT),
            sansTotal("regle", "Réglé", TypeColonne.MONTANT),
            sansTotal("reste", "Reste", TypeColonne.MONTANT),
            sansTotal("etudeFaisabilite", "Étude de faisabilité", TypeColonne.MONTANT));
    private static final List<String> CODES = MONTANTS.stream().map(ColonneRapport::nom).toList();
    private static final int N = CODES.size();
    private static final int CUMUL = 3;
    private static final int A_PAYER = 4;
    private static final int REGLE = 5;
    private static final int RESTE = 6;
    private static final int FAISABILITE = 7;

    private final BilansProjetService bilans;
    private final MontantsBilanCategories pieces;

    public BilanCategoriesRapport(SourcesRapportsProjet sources, BilansProjetService bilans,
                                  MontantsBilanCategories pieces) {
        super(sources);
        this.bilans = bilans;
        this.pieces = pieces;
    }

    @Override
    public String code() {
        return "PrjTdbBilan";
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        List<UUID> demandes = projetsDemandes(contexte);
        Set<UUID> projets = sources.perimetre().restreindre(demandes);
        Map<UUID, BigDecimal[]> montants = new HashMap<>();
        if (!projets.isEmpty()) {
            bilans.faisabilite(projets, true, (categorie, m) -> ajouter(montants, categorie, FAISABILITE, m, N));
            Map<UUID, List<Object[]>> ventilations = bilans.ventilations(projets);
            // Lot d'un projet commun réparti par tranche (branche C) : estimations comme N5a, pièces non réparties → exclues.
            Set<UUID> projetsDesLots = new HashSet<>(demandes);
            ventilations.values().forEach(l -> l.forEach(r -> projetsDesLots.add(uuid(r[1]))));
            List<LotCalcule> lots = lots(demandes.isEmpty() ? demandes : List.copyOf(projetsDesLots));
            List<UUID> ids = lots.stream().filter(l -> projets.contains(l.lot().getPrjProject().getId()))
                    .map(l -> l.lot().getId()).toList();
            if (!ids.isEmpty()) {
                pieces.pieces(ids).forEach(p -> {
                    ajouter(montants, p.categorie(), CUMUL, p.cumul(), N);
                    ajouter(montants, p.categorie(), A_PAYER, p.aPayer(), N);
                    ajouter(montants, p.categorie(), REGLE, p.regle(), N);
                });
            }
            estimations(lots, ventilations, montants);
        }
        montants.values().forEach(v -> v[RESTE] = v[A_PAYER].subtract(v[REGLE]));
        List<Map<String, Object>> sortie = new ArrayList<>();
        List<NoeudBilanDto> racines = bilans.arbre(montants, CODES);
        racines.forEach(n -> aplatir(n, sortie));
        Map<String, Object> totaux = new LinkedHashMap<>();
        CODES.forEach(code -> totaux.put(code, racines.stream().map(n -> n.valeurs().get(code)).reduce(ZERO,
                BigDecimal::add)));
        List<ColonneRapport> colonnes = Stream.concat(Stream.of(ColonneRapport.texte(CATEGORIE, "Catégorie"),
                sansTotal(NIVEAU, "Niveau", TypeColonne.NOMBRE)), MONTANTS.stream()).toList();
        return new ResultatRapport(List.of(new SectionRapport("bilan", "Bilan", colonnes, sortie, null, totaux,
                List.of())), List.of());
    }

    /** Estimation de base, confidentielle et marché ajusté TTC × taux du lot ; lot résilié : son cumul travaux total (D5). */
    private void estimations(List<LotCalcule> lots, Map<UUID, List<Object[]>> ventilations,
                             Map<UUID, BigDecimal[]> montants) {
        List<UUID> resilies = lots.stream().map(LotCalcule::lot).filter(BilanCategoriesRapport::resilie)
                .map(PrjMarket::getId).toList();
        Map<UUID, BigDecimal> cumuls = resilies.isEmpty() ? Map.of() : pieces.cumuls(resilies);
        for (LotCalcule l : lots) {
            BigDecimal[] valeurs;
            if (resilies.contains(l.lot().getId())) {
                BigDecimal cumul = cumuls.getOrDefault(l.lot().getId(), ZERO);
                valeurs = new BigDecimal[]{cumul, cumul, cumul};
            } else {
                BigDecimal[] colonnes = BilansProjetService.colonnesLot(l.calcul(), true);
                valeurs = new BigDecimal[]{colonnes[1], colonnes[2], colonnes[4]};
            }
            for (Object[] r : ventilations.getOrDefault(l.lot().getId(), List.of())) {
                BigDecimal taux = decimal(r[3]);
                for (int i = 0; i < valeurs.length && taux != null; i++) {
                    ajouter(montants, uuid(r[2]), i, valeurs[i] == null ? null : valeurs[i].multiply(taux), N);
                }
            }
        }
    }

    private static boolean resilie(PrjMarket lot) {
        return lot.getStrStatus() != null
                && MontantsListeLots.TYPE_RESILIE.equals(lot.getStrStatus().getStrStatusTypeId());
    }

    private static void aplatir(NoeudBilanDto n, List<Map<String, Object>> lignes) {
        Map<String, Object> ligne = new LinkedHashMap<>();
        ligne.put(CATEGORIE, n.libelle());
        ligne.put(NIVEAU, n.niveau());
        ligne.putAll(n.valeurs());
        lignes.add(ligne);
        n.enfants().forEach(e -> aplatir(e, lignes));
    }
}

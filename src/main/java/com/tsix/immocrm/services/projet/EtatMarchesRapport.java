package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.projet.CalculLot;
import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.domain.rapport.TypeColonne;
import com.tsix.immocrm.dto.projet.GeneralLotDto;
import com.tsix.immocrm.services.projet.LotsProjetService.LotCalcule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

import static com.tsix.immocrm.services.projet.travaux.ValeursNatives.decimal;
import static com.tsix.immocrm.services.projet.travaux.ValeursNatives.uuid;
import static java.math.BigDecimal.ZERO;

/** {@code PrjMarketTresorie} « État des marchés » : une ligne par type de lot × catégorie (FEATURE-n5b-c-etat-marches.md). */
@Component
public class EtatMarchesRapport extends RapportProjet {

    private static final String TYPE_LOT = "typeLot";
    private static final String CATEGORIE = "categorie";
    private static final String ESTIMATION = "estimationBase";
    private static final String ESTIMATION_M2 = "estimationBaseM2";
    private static final String MARCHE = "marche";
    private static final String SUPPLEMENTAIRES = "supplementairesAvenants";
    private static final String RATIO = "ratioSupplementaires";
    private static final String TOTAL_MARCHE = "totalMarche";
    private static final String TOTAL_MARCHE_M2 = "totalMarcheM2";
    private static final String ECART = "ecart";
    // Termes du ratio, sommés dans le groupe mais hors colonnes.
    private static final String EXTRAS_FACTURES = "extrasFactures";
    private static final String OFFERT = "offertAPayer";
    private static final List<ColonneRapport> COLONNES = List.of(
            ColonneRapport.texte(TYPE_LOT, "Type de lot"), ColonneRapport.texte(CATEGORIE, "Catégorie"),
            montant(ESTIMATION, "Estimation de base TTC"),
            montant(ESTIMATION_M2, "Estimation de base — prix/m² couvert"),
            montant(MARCHE, "Valeur marché"), montant(SUPPLEMENTAIRES, "Travaux sup. et avenants"),
            ColonneRapport.pourcentage(RATIO, "Travaux sup % du marché", null),
            montant(TOTAL_MARCHE, "Total marché TTC"), montant(TOTAL_MARCHE_M2, "Total marché — prix/m² couvert"),
            sansTotal(ECART, "Écart", TypeColonne.MONTANT), montant("paye", "Payé"),
            montant("resteAPayer", "Reste à payer"), montant("retenueGarantie", "Montant R.G."),
            montant("retenueRestituee", "Montant restitué"), montant("retenueResteARestituer", "Reste à restituer"),
            montant("travauxNonPayes", "Travaux à ne pas payer (divers retenues)"),
            montant("resteMarches", "Total reste à payer — marchés"),
            montant("resteEstimations", "Total reste à payer — sur estimations"),
            montant("resteTotal", "Total reste à payer"));
    private static final Comparator<List<String>> ORDRE = Comparator.<List<String>, String>comparing(List::getFirst)
            .thenComparing(List::getLast);
    // Une ligne par lot × tranche de ventilation du projet (D6) ; sans ventilation : taux 1, pas de surface.
    private static final String SQL_LOTS = """
            SELECT m.PKey, ty.Description, ca.Description, ISNULL(v.VentilationRate, 1), t.CoveredFloorArea
            FROM PrjMarket m LEFT JOIN PrjMarketType ty ON ty.PKey = m.PrjMarketTypeId
            LEFT JOIN StlCategory ca ON ca.PKey = m.StlCategoryId
            LEFT JOIN PrjProjectCfgTrancheVentilation v ON v.PrjProjectId = m.PrjProjectId
            LEFT JOIN CfgTranche t ON t.PKey = v.CfgTrancheId
            WHERE m.PrjProjectId IN (:projets)""";

    public EtatMarchesRapport(SourcesRapportsProjet sources) {
        super(sources);
    }

    @Override
    public String code() {
        return "PrjMarketTresorie";
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        List<LotCalcule> lots = lots(projetsDemandes(contexte));
        Map<List<String>, Map<String, BigDecimal>> groupes = new TreeMap<>(ORDRE);
        Map<UUID, List<Object[]>> tranches = lots.isEmpty() ? Map.of() : tranches(lots);
        for (LotCalcule l : lots) {
            GeneralLotDto general = l.calcul().general();
            BigDecimal estimation = general.estimationBase().ttc();
            BigDecimal marche = general.marcheAjuste().ttc();
            Map<String, BigDecimal> montants = montantsLot(l, estimation, marche);
            for (Object[] r : tranches.getOrDefault(l.lot().getId(), List.of())) {
                Map<String, BigDecimal> g = groupes.computeIfAbsent(
                        List.of(Objects.toString(r[1], "(sans type)"), Objects.toString(r[2], "(sans catégorie)")),
                        k -> new HashMap<>());
                BigDecimal taux = Objects.requireNonNullElse(decimal(r[3]), BigDecimal.ONE);
                BigDecimal surface = decimal(r[4]);
                montants.forEach((nom, v) -> g.merge(nom, v.multiply(taux), BigDecimal::add));
                g.merge(ESTIMATION_M2, parM2(estimation, taux, surface), BigDecimal::add);
                g.merge(TOTAL_MARCHE_M2, parM2(marche, taux, surface), BigDecimal::add);
            }
        }
        List<Map<String, Object>> lignes = new ArrayList<>();
        groupes.forEach((cle, g) -> lignes.add(ligne(cle, g)));
        return new ResultatRapport(List.of(new SectionRapport("etat-marches", "État des marchés", COLONNES, lignes,
                TYPE_LOT, null, List.of())), List.of());
    }

    /** Montants TTC d'un lot avant ventilation (R2) ; {@code ecart} et le ratio se calculent sur le groupe. */
    private static Map<String, BigDecimal> montantsLot(LotCalcule l, BigDecimal estimation, BigDecimal marche) {
        CalculLot c = l.calcul();
        Garantie rg = Garantie.de(l);
        Map<String, BigDecimal> m = new HashMap<>();
        m.put(ESTIMATION, estimation);
        m.put(MARCHE, marche);
        m.put(SUPPLEMENTAIRES, c.supplementairesEtAvenants());
        m.put(TOTAL_MARCHE, marche.add(c.supplementairesEtAvenants()));
        m.put("paye", c.regle());
        m.put("retenueRestituee", rg.restituee());
        m.put("retenueResteARestituer", rg.reste());
        m.put("travauxNonPayes", c.retenuesFactures());
        BigDecimal reste = c.resteAPayer();
        BigDecimal resteEstimations = l.lot().getCfgSupplier() == null ? c.estimationAPayer() : ZERO;
        m.put("resteAPayer", reste);
        m.put("resteMarches", reste);
        m.put("resteEstimations", resteEstimations);
        m.put("resteTotal", reste.add(resteEstimations));
        m.put("retenueGarantie", c.retenueGarantiePrevue());
        m.put(EXTRAS_FACTURES, c.supplementairesFacturesTTC());
        m.put(OFFERT, c.offertAPayer());
        m.replaceAll((nom, v) -> Objects.requireNonNullElse(v, ZERO));
        return m;
    }

    private static BigDecimal parM2(BigDecimal montant, BigDecimal taux, BigDecimal surface) {
        return surface == null || surface.signum() <= 0 ? ZERO
                : montant.multiply(taux).divide(surface, MathContext.DECIMAL64);
    }

    private static Map<String, Object> ligne(List<String> cle, Map<String, BigDecimal> g) {
        g.put(ECART, g.getOrDefault(MARCHE, ZERO).subtract(g.getOrDefault(ESTIMATION, ZERO)));
        BigDecimal offert = g.getOrDefault(OFFERT, ZERO);
        g.put(RATIO, offert.signum() == 0 ? ZERO
                : g.getOrDefault(EXTRAS_FACTURES, ZERO).divide(offert, MathContext.DECIMAL64));
        Map<String, Object> ligne = new LinkedHashMap<>();
        ligne.put(TYPE_LOT, cle.getFirst());
        ligne.put(CATEGORIE, cle.getLast());
        COLONNES.stream().skip(2).forEach(col -> ligne.put(col.nom(), g.getOrDefault(col.nom(), ZERO)));
        return ligne;
    }

    private Map<UUID, List<Object[]>> tranches(List<LotCalcule> lots) {
        List<UUID> projets = lots.stream().map(l -> l.lot().getPrjProject().getId()).distinct().toList();
        Map<UUID, List<Object[]>> parLot = new HashMap<>();
        for (Object o : sources.em().createNativeQuery(SQL_LOTS).setParameter(PROJETS, projets).getResultList()) {
            Object[] r = (Object[]) o;
            parLot.computeIfAbsent(uuid(r[0]), k -> new ArrayList<>()).add(r);
        }
        return parLot;
    }

    private static ColonneRapport montant(String nom, String libelle) {
        return ColonneRapport.montant(nom, libelle, null);
    }
}

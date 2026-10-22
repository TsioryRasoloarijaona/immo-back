package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.ColonneDossier;
import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.services.dossier.liste.MontantsParNatureDossier;
import com.tsix.immocrm.services.rapport.LotsTranches;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.tsix.immocrm.domain.ColonneDossier.*;

/**
 * Sections « ventes » et « annulations » du n° 12 (FEATURE-tdb-vente-rapports-socle.md §6) : montants par nature de
 * {@link MontantsParNatureDossier} (aucun second calcul), dossiers ni annulés ni désistés, sommés par tranche.
 */
@Component
public class MontantsVenteParTranche {

    static final String TRANCHE_LIBELLE = "trancheLibelle";
    private static final String MONTANT = "Montant";
    private static final String REGLE = "Regle";
    private static final String RESTE = "Reste";
    private static final String PARAM_TRANCHES = "tranches";

    /** Nature : préfixe des trois colonnes, en-tête, colonnes du calcul des dossiers (montant {@code null} = prix). */
    private record Nature(String prefixe, String libelle, ColonneDossier montant, ColonneDossier regle,
                          ColonneDossier reste) {
    }

    private static final List<Nature> NATURES = List.of(
            new Nature("prix", "Prix de vente après remise", null, PRIX_REGLE, PRIX_RESTE),
            new Nature("avance", "Avance", AVANCE, AVANCE_REGLEE, AVANCE_RESTE),
            new Nature("autofinancement", "Autofinancement", AUTOFINANCEMENT, AUTOFINANCEMENT_REGLE,
                    AUTOFINANCEMENT_RESTE),
            new Nature("credit", "Crédit", CREDIT, CREDIT_REGLE, CREDIT_RESTE),
            new Nature("frais", "Frais", FRAIS, FRAIS_REGLES, FRAIS_RESTE),
            new Nature("modifications", "Modifications", MODIFICATIONS, MODIFICATIONS_REGLEES, MODIFICATIONS_RESTE));

    // Statut NULL gardé : un dossier sans statut n'est ni annulé ni désisté (RV3).
    private static final String DOSSIERS = """
            SELECT f FROM ComFolder f JOIN FETCH f.stkItem i JOIN FETCH i.stkHierarchy t LEFT JOIN f.strStatus s
            WHERE t.id IN :tranches AND (s.id IS NULL
                OR (COALESCE(s.annulation, false) = false AND COALESCE(s.resiliation, false) = false))""";

    private static final String RESILIATIONS = """
            SELECT t.id, t.description, SUM(d.amount) FROM ComDocument d JOIN d.comSettlementCategory c
            JOIN d.comFolder f JOIN f.stkItem i JOIN i.stkHierarchy t
            WHERE c.isSaleResiliation = true AND t.id IN :tranches GROUP BY t.id, t.description""";

    private static final String RESILIATIONS_REGLEES = "SELECT t.id, SUM(" + StlSettlementLineRepository.MONTANT_SIGNE
            + ") FROM StlSettlementLine l JOIN l.stlSettlement st JOIN l.comDocument d JOIN d.comSettlementCategory c "
            + "JOIN d.comFolder f JOIN f.stkItem i JOIN i.stkHierarchy t "
            + "WHERE c.isSaleResiliation = true AND t.id IN :tranches" + StlSettlementLineRepository.COMPTE
            + " GROUP BY t.id";

    private final EntityManager em;
    private final MontantsParNatureDossier montants;

    public MontantsVenteParTranche(EntityManager em, MontantsParNatureDossier montants) {
        this.em = em;
        this.montants = montants;
    }

    static List<ColonneRapport> colonnesVentes() {
        List<ColonneRapport> colonnes = new ArrayList<>(List.of(ColonneRapport.texte(TRANCHE_LIBELLE, "Tranche")));
        for (Nature n : NATURES) {
            colonnes.add(ColonneRapport.montant(n.prefixe() + MONTANT, MONTANT, n.libelle()));
            colonnes.add(ColonneRapport.montant(n.prefixe() + REGLE, "Réglé", n.libelle()));
            colonnes.add(ColonneRapport.montant(n.prefixe() + RESTE, RESTE, n.libelle()));
        }
        return colonnes;
    }

    /** Une ligne par tranche ayant au moins un dossier retenu, triées par libellé. */
    List<Map<String, Object>> ventes(List<UUID> tranches) {
        List<ComFolder> dossiers = LotsTranches.lire(tranches, lot -> em.createQuery(DOSSIERS, ComFolder.class)
                .setParameter(PARAM_TRANCHES, lot).getResultList());
        Map<UUID, Map<ColonneDossier, BigDecimal>> parDossier = montants.pour(dossiers);
        Map<UUID, Map<String, Object>> parTranche = new HashMap<>();
        for (ComFolder f : dossiers) {
            CfgTranche t = f.getStkItem().getStkHierarchy();
            Map<String, Object> ligne = parTranche.computeIfAbsent(t.getId(), k -> ligneVide(t.getDescription()));
            Map<ColonneDossier, BigDecimal> m = parDossier.get(f.getId());
            for (Nature n : NATURES) {
                ajouter(ligne, n.prefixe() + MONTANT, n.montant() == null ? f.getPriceAfterDiscount() : m.get(n.montant()));
                ajouter(ligne, n.prefixe() + REGLE, m.get(n.regle()));
                ajouter(ligne, n.prefixe() + RESTE, m.get(n.reste()));
            }
        }
        return trier(parTranche.values());
    }

    /** Échéances de résiliation ({@code IsSaleResiliation}) par tranche : montant, réglé signé, reste. */
    List<Map<String, Object>> annulations(List<UUID> tranches) {
        List<Object[]> montantsParTranche = LotsTranches.lire(tranches, lot -> em.createQuery(RESILIATIONS,
                Object[].class).setParameter(PARAM_TRANCHES, lot).getResultList());
        Map<Object, BigDecimal> regles = new HashMap<>();
        LotsTranches.lire(tranches, lot -> em.createQuery(RESILIATIONS_REGLEES, Object[].class)
                .setParameter(PARAM_TRANCHES, lot).getResultList()).forEach(r -> regles.put(r[0], (BigDecimal) r[1]));
        List<Map<String, Object>> lignes = new ArrayList<>();
        for (Object[] r : montantsParTranche) {
            BigDecimal montant = nonNul((BigDecimal) r[2]);
            BigDecimal regle = nonNul(regles.get(r[0]));
            Map<String, Object> ligne = new LinkedHashMap<>();
            ligne.put(TRANCHE_LIBELLE, r[1]);
            ligne.put("montantAnnulation", montant);
            ligne.put("regleAnnulation", regle);
            ligne.put("resteAnnulation", montant.subtract(regle));
            lignes.add(ligne);
        }
        return trier(lignes);
    }

    private static Map<String, Object> ligneVide(String libelle) {
        Map<String, Object> ligne = new LinkedHashMap<>();
        ligne.put(TRANCHE_LIBELLE, libelle);
        for (Nature n : NATURES) {
            ligne.put(n.prefixe() + MONTANT, BigDecimal.ZERO);
            ligne.put(n.prefixe() + REGLE, BigDecimal.ZERO);
            ligne.put(n.prefixe() + RESTE, BigDecimal.ZERO);
        }
        return ligne;
    }

    private static void ajouter(Map<String, Object> ligne, String nom, BigDecimal valeur) {
        ligne.put(nom, ((BigDecimal) ligne.get(nom)).add(nonNul(valeur)));
    }

    private static List<Map<String, Object>> trier(Collection<Map<String, Object>> lignes) {
        return lignes.stream().sorted(Comparator.comparing(l -> Objects.toString(l.get(TRANCHE_LIBELLE), ""))).toList();
    }

    private static BigDecimal nonNul(BigDecimal valeur) {
        return valeur == null ? BigDecimal.ZERO : valeur;
    }
}

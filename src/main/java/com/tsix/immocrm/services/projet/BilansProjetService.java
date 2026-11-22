package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.projet.CalculLot;
import com.tsix.immocrm.dto.projet.BilanProjetDto;
import com.tsix.immocrm.dto.projet.ColonneBilanDto;
import com.tsix.immocrm.dto.projet.FiltresBilansProjet;
import com.tsix.immocrm.dto.projet.NoeudBilanDto;
import com.tsix.immocrm.dto.projet.TrioDto;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.services.droits.DroitsOmran;
import com.tsix.immocrm.services.perimetre.PerimetreProjetService;
import com.tsix.immocrm.web.export.ColonneFixe;
import com.tsix.immocrm.web.export.FeuilleExport;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.tsix.immocrm.domain.TypeExport.MONTANT;
import static com.tsix.immocrm.domain.TypeExport.NOMBRE;
import static com.tsix.immocrm.services.projet.travaux.ValeursNatives.decimal;
import static com.tsix.immocrm.services.projet.travaux.ValeursNatives.uuid;
import static java.math.BigDecimal.ZERO;

/**
 * Bilans projet (FEATURE-n5-tableaux-de-bord-projet.md R9–R13, R15, « Bilans — formules sur tables ») : étude de faisabilité
 * ventilée par tranche et lots ventilés par catégorie, lus sur tables ; montants de lot = {@link CalculLot}.
 */
@Service
@Transactional(readOnly = true)
public class BilansProjetService {

    private static final String DROIT = "Yes.Prj.Front.Tdb.ProjectReview";
    private static final String LIBELLE = "libelle";
    private static final String NIVEAU = "niveau";
    private static final String ECART = "ecart";
    private static final String PROJETS = "projets";
    private static final int ABSENTE = -1;

    private static final List<ColonneBilanDto> COLONNES = List.of(
            new ColonneBilanDto("etudeFaisabilite", "ÉTUDE DE FAISABILITÉ"),
            new ColonneBilanDto("estimationBase", "ESTIMATION DE BASE"),
            new ColonneBilanDto("estimationConfidentielle", "ESTIMATION CONFIDENTIELLE"),
            new ColonneBilanDto("marche", "MARCHÉ"),
            new ColonneBilanDto("marcheAjuste", "MARCHÉ AJUSTÉ"),
            new ColonneBilanDto("previsions", "PRÉVISIONS"),
            new ColonneBilanDto("coutPrevisionnel", "COÛT PRÉVISIONNEL"),
            new ColonneBilanDto("coutReel", "COÛT RÉEL"));
    private static final int N = COLONNES.size();
    private static final List<ColonneFixe> COLONNES_EXPORT = Stream.of(
            Stream.of(ColonneFixe.texte(LIBELLE, "Libellé"), ColonneFixe.de(NIVEAU, "Niveau", NOMBRE)),
            COLONNES.stream().map(c -> ColonneFixe.de(c.code(), c.libelle(), MONTANT)),
            Stream.of(ColonneFixe.de(ECART, "Écart", MONTANT)))
            .flatMap(Function.identity()).toList();

    // Branche A : part = taux du projet / Σ taux des projets non communs de la même tranche.
    private static final String SQL_FAISABILITE = """
            SELECT fp.StlCategoryId, fp.AmountHT * w.Weight * v.VentilationRate / t.Total,
                   fp.Amount * w.Weight * v.VentilationRate / t.Total
            FROM FSPrevision fp
            JOIN StkFeasibilityStudy s ON s.PKey = fp.FeasibilityStudyId
            JOIN CfgCompany c ON c.PKey = s.CfgCompanyId
            JOIN StkFeasibilityStudyCfgTranche w ON w.FeasibilityStudyId = s.PKey
            JOIN PrjProjectCfgTrancheVentilation v ON v.CfgTrancheId = w.CfgTrancheId
            JOIN PrjProject p ON p.PKey = v.PrjProjectId
            JOIN (SELECT vt.CfgTrancheId, SUM(vt.VentilationRate) AS Total
                  FROM PrjProjectCfgTrancheVentilation vt JOIN PrjProject pt ON pt.PKey = vt.PrjProjectId
                  WHERE ISNULL(pt.IsACommon, 0) = 0 GROUP BY vt.CfgTrancheId) t
              ON t.CfgTrancheId = v.CfgTrancheId AND t.Total <> 0
            WHERE fp.IsForProject = 1 AND ISNULL(p.IsACommon, 0) = 0 AND p.PKey IN (:projets)""";
    // Branches B (projet non commun) et C (lot d'un projet commun réparti par tranche partagée).
    private static final String SQL_LOTS = """
            SELECT m.PKey, m.PrjProjectId, vc.StlCategoryId, vc.VentilationRate
            FROM PrjMarket m JOIN PrjProject p ON p.PKey = m.PrjProjectId
            JOIN PrjMarketVentilationCategory vc ON vc.PrjMarketId = m.PKey
            WHERE ISNULL(p.IsACommon, 0) = 0 AND p.PKey IN (:projets)
            UNION ALL
            SELECT m.PKey, m.PrjProjectId, vc.StlCategoryId, vc.VentilationRate * vt1.VentilationRate * vt2.VentilationRate
            FROM PrjMarket m JOIN PrjProject p ON p.PKey = m.PrjProjectId
            JOIN PrjMarketVentilationCategory vc ON vc.PrjMarketId = m.PKey
            JOIN PrjProjectCfgTrancheVentilation vt1 ON vt1.PrjProjectId = p.PKey
            JOIN PrjProjectCfgTrancheVentilation vt2 ON vt2.CfgTrancheId = vt1.CfgTrancheId
            JOIN PrjProject p2 ON p2.PKey = vt2.PrjProjectId
            WHERE p.IsACommon = 1 AND ISNULL(p2.IsACommon, 0) = 0 AND p2.PKey IN (:projets)""";

    private final DroitsOmran droits;
    private final PerimetreProjetService perimetre;
    private final MontantsListeLots montantsLots;
    private final EntityManager em;

    BilansProjetService(DroitsOmran droits, PerimetreProjetService perimetre, MontantsListeLots montantsLots,
                        EntityManager em) {
        this.droits = droits;
        this.perimetre = perimetre;
        this.montantsLots = montantsLots;
        this.em = em;
    }

    public BilanProjetDto lire(FiltresBilansProjet filtres) {
        droits.exiger(DROIT);
        Set<UUID> projets = filtres.projets().isEmpty() ? Set.of() : perimetre.restreindre(filtres.projets());
        if (projets.isEmpty()) {
            throw new InvalidRequestException("Veuillez sélectionner au moins un projet !");
        }
        Arbre a = new Arbre(enfantsParParent(), montants(projets, Boolean.TRUE.equals(filtres.ttc())),
                index(filtres.colonne1()), index(filtres.colonne2()), new HashMap<>());
        return new BilanProjetDto(COLONNES, noeuds(null, 0, a), filtres.colonne1(), filtres.colonne2(),
                Boolean.TRUE.equals(filtres.ttc()));
    }

    public List<FeuilleExport> feuillesExport(FiltresBilansProjet filtres) {
        List<Map<String, Object>> lignes = new ArrayList<>();
        lire(filtres).lignes().forEach(n -> aplatir(n, lignes));
        return List.of(new FeuilleExport("Bilans", COLONNES_EXPORT, lignes, null));
    }

    private static int index(String code) {
        if (code == null) {
            return ABSENTE;
        }
        return IntStream.range(0, N).filter(i -> COLONNES.get(i).code().equals(code)).findFirst()
                .orElseThrow(() -> new InvalidRequestException("Colonne inconnue : " + code));
    }

    // ------------------------------------------------------------------ montants par catégorie

    private Map<UUID, BigDecimal[]> montants(Set<UUID> projets, boolean ttc) {
        Map<UUID, BigDecimal[]> parCategorie = new HashMap<>();
        for (Object o : em.createNativeQuery(SQL_FAISABILITE).setParameter(PROJETS, projets).getResultList()) {
            Object[] r = (Object[]) o;
            ajouter(parCategorie, uuid(r[0]), 0, decimal(ttc ? r[2] : r[1]));
        }
        Map<UUID, List<Object[]>> ventilations = new HashMap<>();
        Set<UUID> projetsDesLots = new HashSet<>();
        for (Object o : em.createNativeQuery(SQL_LOTS).setParameter(PROJETS, projets).getResultList()) {
            Object[] r = (Object[]) o;
            ventilations.computeIfAbsent(uuid(r[0]), k -> new ArrayList<>()).add(r);
            projetsDesLots.add(uuid(r[1]));
        }
        if (ventilations.isEmpty()) {
            return parCategorie;
        }
        List<PrjMarket> lots = em.createQuery("select m from PrjMarket m where m.prjProject.id in :projets", PrjMarket.class)
                .setParameter(PROJETS, projetsDesLots).getResultList().stream()
                .filter(m -> ventilations.containsKey(m.getId())).toList();
        montantsLots.calculer(lots).forEach((lot, calcul) -> {
            BigDecimal[] valeurs = colonnesLot(calcul, ttc);
            for (Object[] r : ventilations.getOrDefault(lot, List.of())) {
                BigDecimal taux = decimal(r[3]);
                for (int i = 1; i < N; i++) {
                    ajouter(parCategorie, uuid(r[2]), i, valeurs[i].multiply(taux));
                }
            }
        });
        return parCategorie;
    }

    /** Colonnes 2 à 8 d'un lot ; {@code CancelRatio} multiplie tout sauf le Marché (déjà appliqué aux estimations par general()). */
    private static BigDecimal[] colonnesLot(CalculLot c, boolean ttc) {
        Function<TrioDto, BigDecimal> v = ttc ? TrioDto::ttc : TrioDto::ht;
        BigDecimal ratio = c.ratioAnnulation();
        var g = c.general();
        return new BigDecimal[]{ZERO, v.apply(g.estimationBase()), v.apply(g.estimationConfidentielle()),
                v.apply(g.marche()), v.apply(g.marcheAjuste()).multiply(ratio), ttc ? c.previsionTTC() : c.previsionHT(),
                v.apply(c.coutPrevisionnel().total()).multiply(ratio), v.apply(c.coutReel().total()).multiply(ratio)};
    }

    private static void ajouter(Map<UUID, BigDecimal[]> parCategorie, UUID categorie, int colonne, BigDecimal montant) {
        if (categorie == null || montant == null) {
            return;
        }
        BigDecimal[] v = parCategorie.computeIfAbsent(categorie, k -> zeros());
        v[colonne] = v[colonne].add(montant);
    }

    private static BigDecimal[] zeros() {
        BigDecimal[] v = new BigDecimal[N];
        Arrays.fill(v, ZERO);
        return v;
    }

    // ------------------------------------------------------------------ arbre (R10)

    private Map<UUID, List<Categorie>> enfantsParParent() {
        Map<UUID, List<Categorie>> parParent = new HashMap<>();
        for (Object o : em.createNativeQuery("SELECT PKey, Description, ParentId, Sorting FROM StlCategory").getResultList()) {
            Object[] r = (Object[]) o;
            Integer ordre = r[3] == null ? null : ((Number) r[3]).intValue();
            parParent.computeIfAbsent(uuid(r[2]), k -> new ArrayList<>())
                    .add(new Categorie(uuid(r[0]), (String) r[1], ordre));
        }
        Comparator<Categorie> tri = Comparator.comparing(Categorie::ordre, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Categorie::libelle, Comparator.nullsFirst(Comparator.naturalOrder()));
        parParent.values().forEach(l -> l.sort(tri));
        return parParent;
    }

    private static List<NoeudBilanDto> noeuds(UUID parent, int niveau, Arbre a) {
        return a.enfants().getOrDefault(parent, List.of()).stream().map(c -> noeud(c, niveau, a))
                .filter(Objects::nonNull).toList();
    }

    /** Parent = sa catégorie + ses descendants ; null (masqué) si les 8 colonnes valent 0. */
    private static NoeudBilanDto noeud(Categorie c, int niveau, Arbre a) {
        List<NoeudBilanDto> enfants = noeuds(c.id(), niveau + 1, a);
        BigDecimal[] total = a.montants().getOrDefault(c.id(), zeros()).clone();
        for (NoeudBilanDto e : enfants) {
            BigDecimal[] t = a.cumuls().get(e.categorieId());
            for (int i = 0; i < N; i++) {
                total[i] = total[i].add(t[i]);
            }
        }
        if (Arrays.stream(total).allMatch(x -> x.signum() == 0)) {
            return null;
        }
        a.cumuls().put(c.id(), total);
        Map<String, BigDecimal> valeurs = new LinkedHashMap<>();
        for (int i = 0; i < N; i++) {
            valeurs.put(COLONNES.get(i).code(), arrondi(total[i]));
        }
        BigDecimal ecart = a.i1() == ABSENTE || a.i2() == ABSENTE ? ZERO : arrondi(total[a.i1()].subtract(total[a.i2()]));
        return new NoeudBilanDto(c.id(), c.libelle(), niveau, valeurs, ecart, enfants);
    }

    private static BigDecimal arrondi(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private static void aplatir(NoeudBilanDto n, List<Map<String, Object>> lignes) {
        Map<String, Object> ligne = new HashMap<>(n.valeurs());
        ligne.put(LIBELLE, n.libelle());
        ligne.put(NIVEAU, n.niveau());
        ligne.put(ECART, n.ecart());
        lignes.add(ligne);
        n.enfants().forEach(e -> aplatir(e, lignes));
    }

    private record Categorie(UUID id, String libelle, Integer ordre) {
    }

    /** {@code cumuls} : totaux bruts des nœuds affichés, lus par leur parent. */
    private record Arbre(Map<UUID, List<Categorie>> enfants, Map<UUID, BigDecimal[]> montants, int i1, int i2,
                         Map<UUID, BigDecimal[]> cumuls) {
    }
}

package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.ColonneDossier;
import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.FonctionTotal;
import com.tsix.immocrm.domain.rapport.ModuleRapport;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.Rapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.domain.rapport.TypeColonne;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.services.dossier.liste.MontantsParNatureDossier;
import com.tsix.immocrm.services.rapport.LotsTranches;
import com.tsix.immocrm.utils.TierNomFormatter;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * N° 2 SUIVI HEBDOMADAIRE (FEATURE-tdb-vente-rapports-commercial.md §3), modèle OMRAN {@code ComTdbWeeklyMonitoring} :
 * concrétisations, désistements, versements, contrats et livraisons de la période, dossiers de vente des tranches
 * visibles et des commerciaux retenus.
 */
@Component
public class SuiviHebdomadaireRapport implements Rapport {

    static final String CODE = "ComTdbWeeklyMonitoring";

    private static final String DU = "du";
    private static final String AU = "au";
    private static final String DEBUT = "debut";
    private static final String FIN = "fin";
    private static final String PARAM_TRANCHES = "tranches";
    private static final String PARAM_IDS = "ids";
    private static final String PROJET = "projet";
    private static final String TRANCHE = "tranche";
    private static final String PRODUIT = "produit";
    private static final String CLIENT = "client";
    private static final String NATURE = "nature";
    private static final String COMMERCIAL = "commercial";
    private static final String PRIX = "prixVenteTtc";
    private static final String REMISE = "remise";
    private static final String QUANTITE = "quantite";
    private static final String MONTANT = "montant";
    private static final String LIB_PROJET = "Projet";
    private static final String LIB_TRANCHE = "Tranche";
    private static final String LIB_PRIX = "Prix de vente TTC";
    private static final String LIB_REMISE = "Remise";

    private static final String DOSSIERS = """
            SELECT f FROM ComFolder f JOIN FETCH f.stkItem i JOIN FETCH i.stkHierarchy t LEFT JOIN FETCH t.cfgProject
            LEFT JOIN FETCH i.stkItemCategory LEFT JOIN FETCH f.strStatus LEFT JOIN FETCH f.cfgCustomer
            LEFT JOIN FETCH f.cfgCommercial JOIN f.stkVocation v
            WHERE t.id IN :tranches AND v.code = 'VENTE'
              AND ((f.advanceDate >= :debut AND f.advanceDate < :fin) OR (f.cancelDate >= :debut AND f.cancelDate < :fin)
                OR (f.agreementSavingDate >= :debut AND f.agreementSavingDate < :fin)
                OR (f.deliveryDateReal >= :debut AND f.deliveryDateReal < :fin))""";

    // Versements : affectations de vente hors frais et hors types non comptabilisés, montant signé partagé.
    private static final String VERSEMENTS = "SELECT pr.description, t.id, t.description, c.id, SUM("
            + StlSettlementLineRepository.MONTANT_SIGNE + ") FROM StlSettlementLine l JOIN l.stlSettlement st "
            + "JOIN l.comDocument d JOIN d.comSettlementCategory cat JOIN d.comFolder f JOIN f.stkVocation v "
            + "JOIN f.stkItem i JOIN i.stkHierarchy t LEFT JOIN t.cfgProject pr LEFT JOIN f.cfgCommercial c "
            + "LEFT JOIN st.stlSettlementType ty WHERE t.id IN :tranches AND v.code = 'VENTE' "
            + "AND COALESCE(cat.isSaleExpense, false) = false AND COALESCE(ty.isNotContabilized, false) = false "
            + "AND COALESCE(st.settlementDate, st.echeanceDate) >= :debut "
            + "AND COALESCE(st.settlementDate, st.echeanceDate) < :fin" + StlSettlementLineRepository.COMPTE
            + " GROUP BY pr.description, t.id, t.description, c.id";

    private static final String SOUS_SURFACES = """
            SELECT i.parent.id, SUM(i.area) FROM StkItem i WHERE i.parent.id IN :ids GROUP BY i.parent.id""";

    private static final String A_REMBOURSER = """
            SELECT w.comFolder.id, SUM(w.reimburseAmount) FROM ComSaleWithdrawalCancellation w
            WHERE w.comFolder.id IN :ids GROUP BY w.comFolder.id""";

    private final EntityManager em;
    private final SeuilsVente seuils;
    private final PorteeCommerciauxRapport portee;
    private final MontantsParNatureDossier montants;
    private final Clock clock;

    public SuiviHebdomadaireRapport(EntityManager em, SeuilsVente seuils, PorteeCommerciauxRapport portee,
                                    MontantsParNatureDossier montants, Clock clock) {
        this.em = em;
        this.seuils = seuils;
        this.portee = portee;
        this.montants = montants;
        this.clock = clock;
    }

    /** Période retenue : bornes inclusives en jours, comparées en {@code [du 00:00 ; au + 1 j[}. */
    private record Periode(LocalDateTime debut, LocalDateTime fin) {

        boolean contient(LocalDateTime date) {
            return date != null && !date.isBefore(debut) && date.isBefore(fin);
        }
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public ModuleRapport module() {
        return ModuleRapport.VENTE;
    }

    @Override
    public List<ParametreRapport> parametres() {
        LocalDate lundi = LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return List.of(ParametreRapport.date(DU, "Du", lundi), ParametreRapport.date(AU, "Au", lundi.plusDays(6)),
                ParametreRapport.tranches());
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        Periode periode = new Periode(contexte.date(DU).atStartOfDay(), contexte.date(AU).plusDays(1).atStartOfDay());
        Predicate<UUID> retenu = portee.retenus(contexte);
        List<ComFolder> dossiers = LotsTranches.lire(contexte.tranchesVisibles(), lot -> em
                .createQuery(DOSSIERS, ComFolder.class).setParameter(PARAM_TRANCHES, lot)
                .setParameter(DEBUT, periode.debut()).setParameter(FIN, periode.fin()).getResultList()).stream()
                .filter(f -> retenu.test(f.getCfgCommercial() == null ? null : f.getCfgCommercial().getId()))
                .toList();
        Map<String, Integer> s = seuils.lire();
        List<String> avertissements = new ArrayList<>();
        Integer conc = seuil(s, SeuilsVente.CONCRETISATION, "Concrétisations", avertissements);
        Integer fin = seuil(s, SeuilsVente.FINALISATION, "Contrats", avertissements);
        Integer livraison = seuil(s, SeuilsVente.LIVRAISON, "Livraisons", avertissements);
        List<SectionRapport> sections = List.of(
                concretisations(dossiers.stream().filter(f -> periode.contient(f.getAdvanceDate()) && racine(f)
                        && f.getCfgCustomer() != null && enCours(f) && atteint(f, conc)).toList()),
                desistements(dossiers.stream().filter(f -> periode.contient(f.getCancelDate()) && racine(f)
                        && f.getStrStatus() != null && Boolean.TRUE.equals(f.getStrStatus().getResiliation()))
                        .toList()),
                versements(contexte.tranchesVisibles(), periode, retenu),
                quantites("contrats", "Contrats", dossiers.stream()
                        .filter(f -> periode.contient(f.getAgreementSavingDate()) && atteint(f, fin)).toList()),
                quantites("livraisons", "Livraisons", dossiers.stream()
                        .filter(f -> periode.contient(f.getDeliveryDateReal()) && atteint(f, livraison)).toList()));
        return new ResultatRapport(sections, avertissements);
    }

    private static Integer seuil(Map<String, Integer> seuils, String parametre, String section, List<String> avis) {
        Integer valeur = seuils.get(parametre);
        if (valeur == null) {
            avis.add(SeuilsVente.absent(parametre, section));
        }
        return valeur;
    }

    private static boolean racine(ComFolder f) {
        return !Boolean.TRUE.equals(f.getStkItem().getIsUnderItem());
    }

    private static boolean enCours(ComFolder f) {
        StrStatus s = f.getStrStatus();
        return s != null && !Boolean.TRUE.equals(s.getAnnulation()) && !Boolean.TRUE.equals(s.getResiliation());
    }

    /** Statut au moins au seuil ({@code Sorting}) ; seuil absent ⇒ aucun dossier. */
    private static boolean atteint(ComFolder f, Integer seuil) {
        StrStatus s = f.getStrStatus();
        return seuil != null && s != null && s.getSorting() != null && s.getSorting() >= seuil;
    }

    // ------------------------------------------------------------------ sections

    private SectionRapport concretisations(List<ComFolder> dossiers) {
        Map<UUID, Map<ColonneDossier, BigDecimal>> parDossier = montants.pour(dossiers);
        Map<UUID, BigDecimal> sous = parId(SOUS_SURFACES, dossiers.stream().map(f -> f.getStkItem().getId()).toList());
        List<Map<String, Object>> lignes = new ArrayList<>();
        for (ComFolder f : dossiers) {
            BigDecimal surface = nonNul(f.getArea());
            BigDecimal base = surface.add(nonNul(sous.get(f.getStkItem().getId())).divide(BigDecimal.TWO));
            BigDecimal prix = nonNul(f.getPrice());
            Map<String, Object> ligne = identite(f);
            ligne.put("dateConcretisation", f.getAdvanceDate().toLocalDate());
            ligne.put("surface", surface);
            ligne.put("prixM2", base.signum() == 0 ? BigDecimal.ZERO : prix.divide(base, 2, RoundingMode.HALF_UP));
            ligne.put(PRIX, prix);
            ligne.put("avanceReglee", nonNul(parDossier.get(f.getId()).get(ColonneDossier.AVANCE_REGLEE)));
            ligne.put(REMISE, nonNul(f.getDiscountAmount()));
            ligne.put(COMMERCIAL, TierNomFormatter.nom(f.getCfgCommercial()));
            lignes.add(ligne);
        }
        List<ColonneRapport> colonnes = new ArrayList<>(colonnesIdentite());
        colonnes.addAll(List.of(date("dateConcretisation", "Date de concrétisation"),
                new ColonneRapport("surface", "Surface", TypeColonne.NOMBRE, null, FonctionTotal.SOMME, true, 2),
                new ColonneRapport("prixM2", "Prix au m²", TypeColonne.MONTANT, null, FonctionTotal.AUCUNE, true, 0),
                ColonneRapport.montant(PRIX, LIB_PRIX, null), ColonneRapport.montant("avanceReglee", "Avance réglée", null),
                ColonneRapport.montant(REMISE, LIB_REMISE, null), ColonneRapport.texte(COMMERCIAL, "Commercial")));
        return new SectionRapport("concretisations", "CONCRETISATIONS (%d)".formatted(lignes.size()), colonnes,
                trier(lignes), null, null, List.of());
    }

    private SectionRapport desistements(List<ComFolder> dossiers) {
        Map<UUID, BigDecimal> aRembourser = parId(A_REMBOURSER, dossiers.stream().map(ComFolder::getId).toList());
        List<Map<String, Object>> lignes = new ArrayList<>();
        for (ComFolder f : dossiers) {
            Map<String, Object> ligne = identite(f);
            ligne.put("dateDesistement", f.getCancelDate().toLocalDate());
            ligne.put(PRIX, nonNul(f.getPrice()));
            ligne.put("aRembourser", nonNul(aRembourser.get(f.getId())));
            ligne.put("details", f.getCancelDescription());
            ligne.put(REMISE, nonNul(f.getDiscountAmount()));
            lignes.add(ligne);
        }
        List<ColonneRapport> colonnes = new ArrayList<>(colonnesIdentite());
        colonnes.addAll(List.of(date("dateDesistement", "Date de désistement"),
                ColonneRapport.montant(PRIX, LIB_PRIX, null), ColonneRapport.montant("aRembourser", "À rembourser", null),
                ColonneRapport.texte("details", "Détails"), ColonneRapport.montant(REMISE, LIB_REMISE, null)));
        return new SectionRapport("desistements", "DESISTEMENTS (%d)".formatted(lignes.size()), colonnes,
                trier(lignes), null, null, List.of());
    }

    private SectionRapport versements(List<UUID> tranches, Periode periode, Predicate<UUID> retenu) {
        Map<UUID, Map<String, Object>> parTranche = new LinkedHashMap<>();
        LotsTranches.lire(tranches, lot -> em.createQuery(VERSEMENTS, Object[].class)
                .setParameter(PARAM_TRANCHES, lot).setParameter(DEBUT, periode.debut())
                .setParameter(FIN, periode.fin()).getResultList()).stream()
                .filter(r -> retenu.test((UUID) r[3]))
                .forEach(r -> {
                    Map<String, Object> ligne = parTranche.computeIfAbsent((UUID) r[1],
                            k -> ligneTranche((String) r[0], (String) r[2], MONTANT, BigDecimal.ZERO));
                    ligne.put(MONTANT, ((BigDecimal) ligne.get(MONTANT)).add(nonNul((BigDecimal) r[4])));
                });
        return new SectionRapport("versements", "Versements", List.of(ColonneRapport.texte(PROJET, LIB_PROJET),
                ColonneRapport.texte(TRANCHE, LIB_TRANCHE), ColonneRapport.montant(MONTANT, "Montant", null)),
                trierParTranche(parTranche.values()), PROJET, null, List.of());
    }

    /** Effectif de dossiers par (projet, tranche). */
    private static SectionRapport quantites(String id, String titre, List<ComFolder> dossiers) {
        Map<UUID, Map<String, Object>> parTranche = new LinkedHashMap<>();
        for (ComFolder f : dossiers) {
            CfgTranche t = f.getStkItem().getStkHierarchy();
            Map<String, Object> ligne = parTranche.computeIfAbsent(t.getId(),
                    k -> ligneTranche(projet(t), t.getDescription(), QUANTITE, 0L));
            ligne.put(QUANTITE, (Long) ligne.get(QUANTITE) + 1);
        }
        return new SectionRapport(id, titre, List.of(ColonneRapport.texte(PROJET, LIB_PROJET),
                ColonneRapport.texte(TRANCHE, LIB_TRANCHE), ColonneRapport.effectif(QUANTITE, "Quantité", null)),
                trierParTranche(parTranche.values()), PROJET, null, List.of());
    }

    // ------------------------------------------------------------------ outils

    private Map<UUID, BigDecimal> parId(String requete, List<UUID> ids) {
        Map<UUID, BigDecimal> valeurs = new HashMap<>();
        LotsTranches.lire(ids, lot -> em.createQuery(requete, Object[].class).setParameter(PARAM_IDS, lot)
                .getResultList()).forEach(r -> valeurs.put((UUID) r[0], (BigDecimal) r[1]));
        return valeurs;
    }

    private static Map<String, Object> identite(ComFolder f) {
        Map<String, Object> ligne = new LinkedHashMap<>();
        ligne.put(TRANCHE, f.getStkItem().getStkHierarchy().getDescription());
        ligne.put(PRODUIT, f.getStkItem().getCode());
        ligne.put(CLIENT, TierNomFormatter.nom(f.getCfgCustomer()));
        ligne.put(NATURE, f.getStkItem().getStkItemCategory() == null ? null
                : f.getStkItem().getStkItemCategory().getDescription());
        return ligne;
    }

    private static List<ColonneRapport> colonnesIdentite() {
        return List.of(ColonneRapport.texte(TRANCHE, LIB_TRANCHE), ColonneRapport.texte(PRODUIT, "Produit"),
                ColonneRapport.texte(CLIENT, "Client"), ColonneRapport.texte(NATURE, "Nature"));
    }

    private static ColonneRapport date(String nom, String libelle) {
        return new ColonneRapport(nom, libelle, TypeColonne.DATE, null, FonctionTotal.AUCUNE, true, 0);
    }

    private static Map<String, Object> ligneTranche(String projet, String tranche, String colonne, Object zero) {
        Map<String, Object> ligne = new LinkedHashMap<>();
        ligne.put(PROJET, projet);
        ligne.put(TRANCHE, tranche);
        ligne.put(colonne, zero);
        return ligne;
    }

    private static String projet(CfgTranche t) {
        CfgProject p = t.getCfgProject();
        return p == null ? null : p.getDescription();
    }

    private static List<Map<String, Object>> trier(List<Map<String, Object>> lignes) {
        return lignes.stream().sorted(Comparator.comparing(texte(TRANCHE)).thenComparing(texte(PRODUIT))).toList();
    }

    private static List<Map<String, Object>> trierParTranche(Collection<Map<String, Object>> lignes) {
        return lignes.stream().sorted(Comparator.comparing(texte(PROJET)).thenComparing(texte(TRANCHE))).toList();
    }

    private static Function<Map<String, Object>, String> texte(String nom) {
        return l -> Objects.toString(l.get(nom), "");
    }

    private static BigDecimal nonNul(BigDecimal valeur) {
        return valeur == null ? BigDecimal.ZERO : valeur;
    }
}

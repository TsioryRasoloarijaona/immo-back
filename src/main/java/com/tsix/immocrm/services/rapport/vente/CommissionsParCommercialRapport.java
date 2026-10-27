package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.commission.TypeCommission;
import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.FonctionTotal;
import com.tsix.immocrm.domain.rapport.ModuleRapport;
import com.tsix.immocrm.domain.rapport.ParametreRapport;
import com.tsix.immocrm.domain.rapport.ParametreRapport.Option;
import com.tsix.immocrm.domain.rapport.Rapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.domain.rapport.TypeColonne;
import com.tsix.immocrm.domain.rapport.TypeParametre;
import com.tsix.immocrm.dto.commission.LigneCommissionCalculeeDto;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComCommissionLine;
import com.tsix.immocrm.services.commission.VueLignesCommission;
import com.tsix.immocrm.services.rapport.LotsTranches;
import com.tsix.immocrm.utils.TierNomFormatter;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * N° 34 COMMISSIONS DE VENTE PAR COMMERCIAL (FEATURE-vente-commissions.md §6), modèle {@code ComCommissionsByCommercial} :
 * lignes de commission des tranches visibles datées dans la période ; réglé par {@code SoldesCommission} (C7), pas le
 * {@code SUM(DISTINCT …)} du modèle.
 */
@Component
public class CommissionsParCommercialRapport implements Rapport {

    static final String CODE = "ComCommissionsByCommercial";

    private static final String DU = "du";
    private static final String AU = "au";
    private static final String COMMERCIAL_ID = "commercialId";
    private static final String CATEGORIE = "categorie";
    private static final String TYPE = "type";
    private static final String COMMERCIAL = "commercial";
    private static final String MONTANT = "montant";
    private static final String REGLE = "regle";
    private static final String RESTE = "reste";
    private static final String LIBELLE_COMMERCIAL = "Commercial/Gestionnaire";
    private static final String LIGNES = """
            SELECT l FROM ComCommissionLine l WHERE l.cfgTrancheId IN :tranches AND l.docDate >= :du AND l.docDate < :lendemain""";
    private static final String PERSONNES = """
            SELECT DISTINCT e FROM ComCommissionLine l, CfgTier e WHERE e.id = l.cfgEmployeeId""";
    private static final String DATES = """
            SELECT f.id, f.cancelDate, f.advanceDate, f.agreementDate, f.agreementSavingDate FROM ComFolder f WHERE f.id IN :ids""";

    private final EntityManager em;
    private final VueLignesCommission vue;
    private final Clock clock;

    public CommissionsParCommercialRapport(EntityManager em, VueLignesCommission vue, Clock clock) {
        this.em = em;
        this.vue = vue;
        this.clock = clock;
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
        LocalDate aujourdhui = LocalDate.now(clock);
        List<Option> personnes = em.createQuery(PERSONNES, CfgTier.class).getResultList().stream()
                .map(t -> new Option(t.getId().toString(), TierNomFormatter.nom(t)))
                .sorted(Comparator.comparing(o -> Objects.toString(o.libelle(), ""))).toList();
        List<Option> categories = em.createQuery(ConcretisationsLibresRapport.CATEGORIES, Object[].class).getResultList()
                .stream().map(r -> new Option(r[0].toString(), (String) r[1])).toList();
        List<Option> types = Arrays.stream(TypeCommission.values())
                .flatMap(t -> Stream.of(t.libelle(), t.libelle() + " (support)")).map(t -> new Option(t, t)).toList();
        return List.of(date(DU, "Du", aujourdhui.withDayOfYear(1)), date(AU, "Au", aujourdhui), ParametreRapport.tranches(),
                choix(COMMERCIAL_ID, "Commerciaux / gestionnaires", personnes), choix(CATEGORIE, "Catégories", categories),
                choix(TYPE, "Types de commission", types));
    }

    private static ParametreRapport date(String nom, String libelle, LocalDate parDefaut) {
        return new ParametreRapport(nom, libelle, TypeParametre.DATE, true, false, parDefaut.toString(), List.of(), null,
                null);
    }

    private static ParametreRapport choix(String nom, String libelle, List<Option> options) {
        return new ParametreRapport(nom, libelle, TypeParametre.CHOIX, false, true, null, options, null, null);
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        LocalDate du = Objects.requireNonNull(contexte.date(DU));
        LocalDate au = Objects.requireNonNull(contexte.date(AU));
        List<String> personnes = contexte.choix(COMMERCIAL_ID);
        List<String> categories = contexte.choix(CATEGORIE);
        List<String> types = contexte.choix(TYPE);
        List<ComCommissionLine> retenues = LotsTranches.lire(contexte.tranchesVisibles(), lot -> em.createQuery(LIGNES,
                        ComCommissionLine.class).setParameter("tranches", lot).setParameter(DU, du.atStartOfDay())
                        .setParameter("lendemain", au.plusDays(1).atStartOfDay()).getResultList()).stream()
                .filter(l -> personnes.isEmpty() || personnes.contains(Objects.toString(l.getCfgEmployeeId())))
                .filter(l -> categories.isEmpty() || categories.contains(Objects.toString(l.getStkItemCategoryId())))
                .filter(l -> types.isEmpty() || types.contains(l.getCommissionType()))
                .toList();
        List<LigneCommissionCalculeeDto> lignes = vue.dtos(retenues).stream()
                .sorted(Comparator.comparing((LigneCommissionCalculeeDto l) -> Objects.toString(l.commercial(), ""))
                        .thenComparing(LigneCommissionCalculeeDto::date, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        return new ResultatRapport(List.of(commerciaux(lignes), detail(lignes)), List.of());
    }

    /** Une ligne par personne commissionnée : montant, réglé, reste. */
    private static SectionRapport commerciaux(List<LigneCommissionCalculeeDto> lignes) {
        Map<UUID, Map<String, Object>> parPersonne = new LinkedHashMap<>();
        for (LigneCommissionCalculeeDto l : lignes) {
            Map<String, Object> ligne = parPersonne.computeIfAbsent(l.commercialId(), k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put(COMMERCIAL, l.commercial());
                m.put(MONTANT, BigDecimal.ZERO);
                m.put(REGLE, BigDecimal.ZERO);
                m.put(RESTE, BigDecimal.ZERO);
                return m;
            });
            ligne.merge(MONTANT, l.montant(), CommissionsParCommercialRapport::ajouter);
            ligne.merge(REGLE, l.regle(), CommissionsParCommercialRapport::ajouter);
            ligne.merge(RESTE, l.reste(), CommissionsParCommercialRapport::ajouter);
        }
        return new SectionRapport("commerciaux", "Commissions par commercial", List.of(
                ColonneRapport.texte(COMMERCIAL, LIBELLE_COMMERCIAL), ColonneRapport.montant(MONTANT, "Total", null),
                ColonneRapport.montant(REGLE, "Total du reglé", null), ColonneRapport.montant(RESTE, "Reste", null)),
                new ArrayList<>(parPersonne.values()), null, null, List.of());
    }

    private SectionRapport detail(List<LigneCommissionCalculeeDto> lignes) {
        Map<UUID, Object[]> dates = new HashMap<>();
        List<UUID> dossiers = lignes.stream().map(LigneCommissionCalculeeDto::dossierId).filter(Objects::nonNull)
                .distinct().toList();
        LotsTranches.lire(dossiers, lot -> em.createQuery(DATES, Object[].class).setParameter("ids", lot).getResultList())
                .forEach(r -> dates.put((UUID) r[0], r));
        List<Map<String, Object>> rangees = new ArrayList<>();
        for (LigneCommissionCalculeeDto l : lignes) {
            Object[] d = dates.getOrDefault(l.dossierId(), new Object[5]);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put(COMMERCIAL, l.commercial());
            m.put("tranche", l.tranche());
            m.put("article", l.articleCode());
            m.put("typeArticle", l.categorie());
            m.put("typeCommission", l.type());
            m.put(MONTANT, l.montant());
            m.put("dateDesistement", jour(d[1]));
            m.put("dateConcretisation", jour(d[2]));
            m.put("dateEnvoiNotaire", jour(d[3]));
            m.put("dateMinute", jour(d[4]));
            m.put("client", l.client());
            rangees.add(m);
        }
        return new SectionRapport("lignes", "Détail des commissions", List.of(
                ColonneRapport.texte(COMMERCIAL, LIBELLE_COMMERCIAL), ColonneRapport.texte("tranche", "Tranche"),
                ColonneRapport.texte("article", "Article"), ColonneRapport.texte("typeArticle", "Type article"),
                ColonneRapport.texte("typeCommission", "Type commission"), ColonneRapport.montant(MONTANT, "Montant", null),
                dateColonne("dateDesistement", "Date désistement"), dateColonne("dateConcretisation", "Date concrétisation"),
                dateColonne("dateEnvoiNotaire", "Date envoi notaire"), dateColonne("dateMinute", "Date minute"),
                ColonneRapport.texte("client", "Client")), rangees, COMMERCIAL, null, List.of());
    }

    private static Object ajouter(Object a, Object b) {
        return ((BigDecimal) a).add((BigDecimal) b);
    }

    private static ColonneRapport dateColonne(String nom, String libelle) {
        return new ColonneRapport(nom, libelle, TypeColonne.DATE, null, FonctionTotal.AUCUNE, true, 0);
    }

    private static LocalDate jour(Object date) {
        return date instanceof LocalDateTime t ? t.toLocalDate() : null;
    }
}

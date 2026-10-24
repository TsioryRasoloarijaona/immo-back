package com.tsix.immocrm.services.suivicommerciaux;

import com.tsix.immocrm.dto.suivicommerciaux.MensuelSuiviCommerciauxDto.BlocMensuel;
import com.tsix.immocrm.dto.suivicommerciaux.MensuelSuiviCommerciauxDto;
import com.tsix.immocrm.dto.suivicommerciaux.MensuelSuiviCommerciauxDto.IndicateursCommercial;
import com.tsix.immocrm.domain.InteractionKpi;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComActionMessageSuivi;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.entities.stl.StlSettlementType;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.services.equipe.CommerciauxVisibles;
import com.tsix.immocrm.services.equipe.MembreEquipe;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Vue mois / année d'OMRAN (FEATURE-suivi-commerciaux.md R11, §7.5, §9.8) : prospections et ventes au commercial
 * assigné, visites à l'auteur, versements au commercial du dossier. Visibilité par les filtres du périmètre ; sans
 * tranche demandée, ce sont eux qui bornent aux tranches habilitées.
 */
@Service
@Transactional(readOnly = true)
public class SuiviCommerciauxMensuelService {

    private static final String ID = "id";
    private static final String CFG_COMMERCIAL = "cfgCommercial";
    private static final String DOC_DATE = "docDate";
    private static final String STK_ITEM = "stkItem";
    private static final String STK_HIERARCHY = "stkHierarchy";
    private static final String COMMERCIAL_CLE = "commercialCle";
    private static final String SYSTEME = "Système";
    private static final String MONTH = "month";
    private static final List<String> ENTRANTES = List.of(InteractionKpi.TYPE_APPEL_ENTRANT, "Mail entrant");
    private static final List<String> SORTANTES = List.of(InteractionKpi.TYPE_APPEL_SORTANT, "Mail Sortant");
    private static final List<String> TYPES_COMPTES = List.of(InteractionKpi.TYPE_VISITE, ENTRANTES.get(0),
            ENTRANTES.get(1), SORTANTES.get(0), SORTANTES.get(1));

    private final SuiviCommerciauxService suiviService;
    private final PerimetreCommercialService perimetreService;
    private final CommerciauxVisibles commerciaux;
    private final AdmUserHabilitationRepository habilitationRepository;
    private final EntityManager em;

    public SuiviCommerciauxMensuelService(SuiviCommerciauxService suiviService,
                                          PerimetreCommercialService perimetreService,
                                          CommerciauxVisibles commerciaux,
                                          AdmUserHabilitationRepository habilitationRepository, EntityManager em) {
        this.suiviService = suiviService;
        this.perimetreService = perimetreService;
        this.commerciaux = commerciaux;
        this.habilitationRepository = habilitationRepository;
        this.em = em;
    }

    /** Indicateurs cumulés d'un commercial ; ratios calculés à la restitution. */
    private record Indicateurs(BigDecimal prospections, BigDecimal visitesProspection, BigDecimal visitesVente,
                               BigDecimal ventes, BigDecimal ventesIssues, BigDecimal prix, BigDecimal versements) {

        static final Indicateurs ZERO = new Indicateurs(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        Indicateurs plus(Indicateurs o) {
            return new Indicateurs(prospections.add(o.prospections), visitesProspection.add(o.visitesProspection),
                    visitesVente.add(o.visitesVente), ventes.add(o.ventes), ventesIssues.add(o.ventesIssues),
                    prix.add(o.prix), versements.add(o.versements));
        }

        Indicateurs divise(int n) {
            UnaryOperator<BigDecimal> d = v -> v.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
            return new Indicateurs(d.apply(prospections), d.apply(visitesProspection), d.apply(visitesVente),
                    d.apply(ventes), d.apply(ventesIssues), d.apply(prix), d.apply(versements));
        }

        IndicateursCommercial versDto(UUID cle, String nom) {
            return new IndicateursCommercial(cle, nom, prospections, visitesProspection, visitesVente, ventes,
                    ventesIssues, prix, versements, ratio(ventes, prospections), ratio(ventes, visitesProspection),
                    ratio(ventesIssues, prospections), ratio(versements, prix));
        }

        private static BigDecimal ratio(BigDecimal numerateur, BigDecimal denominateur) {
            return denominateur.signum() == 0 ? null : numerateur.divide(denominateur, 4, RoundingMode.HALF_UP);
        }
    }

    public MensuelSuiviCommerciauxDto mensuel(int annee, int mois, List<UUID> trancheIds, List<UUID> commercialIds) {
        if (mois < 1 || mois > 12) {
            throw new InvalidRequestException("mois doit être compris entre 1 et 12 (reçu : " + mois + ").");
        }
        List<MembreEquipe> retenus = commerciaux.retenus(commercialIds);
        boolean filtre = !CollectionUtils.isEmpty(commercialIds);
        // §9.8 : sans tranche demandée, les tranches habilitées de l'appelant ; aucune habilitation = toutes.
        List<UUID> tranches = CollectionUtils.isEmpty(trancheIds)
                ? PerimetreCommercialService.admUserIdCourant()
                        .map(habilitationRepository::findTrancheIdsByAdmUserId).orElse(List.of())
                : trancheIds;
        LocalDate debutMois = LocalDate.of(annee, mois, 1);
        LocalDate debutAnnee = LocalDate.of(annee, Month.JANUARY, 1);
        return new MensuelSuiviCommerciauxDto(
                bloc(retenus, filtre, debutMois.atStartOfDay(), debutMois.plusMonths(1).atStartOfDay(), tranches),
                bloc(retenus, filtre, debutAnnee.atStartOfDay(), debutAnnee.plusYears(1).atStartOfDay(), tranches));
    }

    private BlocMensuel bloc(List<MembreEquipe> retenus, boolean filtre, LocalDateTime debut, LocalDateTime fin,
                             List<UUID> tranches) {
        Map<UUID, Indicateurs> parCle = new HashMap<>();
        prospections(debut, fin, tranches, parCle);
        visites(retenus.stream().map(CommerciauxVisibles::cle).toList(), debut, fin, tranches, parCle);
        ventes(debut, fin, tranches, parCle);
        versements(debut, fin, tranches, parCle);

        List<IndicateursCommercial> lignes = new ArrayList<>();
        Indicateurs total = Indicateurs.ZERO;
        for (MembreEquipe m : retenus) {
            UUID cle = CommerciauxVisibles.cle(m);
            Indicateurs i = parCle.get(cle);
            if (i != null || filtre) {
                Indicateurs valeurs = i != null ? i : Indicateurs.ZERO;
                lignes.add(valeurs.versDto(cle, m.nom()));
                total = total.plus(valeurs);
            }
        }
        Indicateurs moyenne = lignes.isEmpty() ? Indicateurs.ZERO : total.divise(lignes.size());
        return new BlocMensuel(lignes, total.versDto(null, null), moyenne.versDto(null, null));
    }

    private static Indicateurs indicateurs(BigDecimal prospections, BigDecimal visitesProspection,
                                           BigDecimal visitesVente) {
        return new Indicateurs(prospections, visitesProspection, visitesVente, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO);
    }

    private static BigDecimal decimal(Tuple t, int i) {
        Object v = t.get(i);
        if (v instanceof BigDecimal b) {
            return b;
        }
        return v instanceof Number n ? BigDecimal.valueOf(n.longValue()) : BigDecimal.ZERO;
    }

    private static void dansLesTranches(List<Predicate> p, Path<UUID> tranche, List<UUID> tranches) {
        if (!CollectionUtils.isEmpty(tranches)) {
            p.add(tranche.in(tranches));
        }
    }

    private static Predicate entre(CriteriaBuilder cb, Expression<LocalDateTime> date, LocalDateTime debut,
                                   LocalDateTime fin) {
        return cb.and(cb.greaterThanOrEqualTo(date, debut), cb.lessThan(date, fin));
    }

    /** NULL compte comme faux, comme les vues OMRAN ({@code ISNULL(x, 0) = 0}). */
    private static Predicate pasVrai(CriteriaBuilder cb, Path<Boolean> drapeau) {
        return cb.or(cb.isNull(drapeau), cb.isFalse(drapeau));
    }

    /** Une ligne par commercial : {@code select[0]} = sa clé, les suivantes = les valeurs lues par {@code lire}. */
    private void cumuler(CriteriaQuery<Tuple> cq, Map<UUID, Indicateurs> parCle, Function<Tuple, Indicateurs> lire) {
        for (Tuple t : em.createQuery(cq).getResultList()) {
            UUID cle = t.get(0, UUID.class);
            if (cle != null) {
                parCle.merge(cle, lire.apply(t), Indicateurs::plus);
            }
        }
    }

    private void prospections(LocalDateTime debut, LocalDateTime fin, List<UUID> tranches,
                              Map<UUID, Indicateurs> parCle) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<ComProspection> p = cq.from(ComProspection.class);
        Path<UUID> commercial = p.get(CFG_COMMERCIAL).get(ID);
        List<Predicate> w = new ArrayList<>(List.of(perimetreService.filtreProspections().toPredicate(p, cq, cb),
                cb.isNotNull(commercial), entre(cb, p.get(DOC_DATE), debut, fin)));
        dansLesTranches(w, p.get(STK_HIERARCHY).get(ID), tranches);
        cq.multiselect(commercial, cb.count(p)).where(w.toArray(new Predicate[0])).groupBy(commercial);
        cumuler(cq, parCle, t -> indicateurs(decimal(t, 1), BigDecimal.ZERO, BigDecimal.ZERO));
    }

    private void visites(List<UUID> cles, LocalDateTime debut, LocalDateTime fin, List<UUID> tranches,
                         Map<UUID, Indicateurs> parCle) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<ComActionMessageSuivi> r = cq.from(ComActionMessageSuivi.class);
        cq.multiselect(r.get(COMMERCIAL_CLE),
                        SuiviCommerciauxService.compter(cb, SuiviCommerciauxService.visite(r, cb, ComActionMessageSuivi.PROSPECTION)),
                        SuiviCommerciauxService.compter(cb, SuiviCommerciauxService.visite(r, cb, ComActionMessageSuivi.VENTE)))
                .where(interactions(r, cq, cb, cles, debut, fin, tranches)).groupBy(r.get(COMMERCIAL_CLE));
        cumuler(cq, parCle, t -> indicateurs(BigDecimal.ZERO, decimal(t, 1), decimal(t, 2)));
    }

    /** Interactions visibles des commerciaux {@code cles} saisies sur la période, bornées aux tranches. */
    private Predicate[] interactions(Root<ComActionMessageSuivi> r, CriteriaQuery<?> cq, CriteriaBuilder cb,
                                     List<UUID> cles, LocalDateTime debut, LocalDateTime fin, List<UUID> tranches) {
        List<Predicate> w = new ArrayList<>(List.of(suiviService.base(cles).toPredicate(r, cq, cb),
                SuiviCommerciauxService.saisieEntre(r, cb, debut, fin)));
        dansLesTranches(w, r.get("trancheId"), tranches);
        return w.toArray(new Predicate[0]);
    }

    // ---------------------------------------------------------------- rapport n° 17 (FEATURE-tdb-vente-rapports-commercial.md §4)

    /** Activité d'un commercial sur un mois de l'année ; communications = téléphone et mail. */
    public record ActiviteMois(UUID cle, int mois, long prospections, long visites, long entrantes, long sortantes) {
    }

    /**
     * Les mêmes comptes que {@link #mensuel} pour chaque mois de l'année, plus les communications, en une requête par
     * source groupée par mois. Aucune tranche ou aucun commercial ⇒ rien ; {@code tranches} ≤ 2 000 (lots à l'appelant).
     */
    public List<ActiviteMois> parMois(int annee, List<UUID> tranches, List<UUID> cles) {
        List<ActiviteMois> activite = new ArrayList<>();
        if (tranches.isEmpty() || cles.isEmpty()) {
            return activite;
        }
        LocalDateTime debut = LocalDate.of(annee, Month.JANUARY, 1).atStartOfDay();
        LocalDateTime fin = debut.plusYears(1);
        // [prospections, visites, entrantes, sortantes] par commercial et par mois
        Map<UUID, long[][]> parCle = new HashMap<>();
        prospectionsParMois(debut, fin, tranches, parCle);
        interactionsParMois(cles, debut, fin, tranches, parCle);
        for (int mois = 1; mois <= 12; mois++) {
            for (UUID cle : cles) {
                long[] c = parCle.getOrDefault(cle, new long[12][4])[mois - 1];
                if (c[0] + c[1] + c[2] + c[3] > 0) {
                    activite.add(new ActiviteMois(cle, mois, c[0], c[1], c[2], c[3]));
                }
            }
        }
        return activite;
    }

    private static long[] mois(Map<UUID, long[][]> parCle, Tuple t) {
        return parCle.computeIfAbsent(t.get(0, UUID.class), c -> new long[12][4])[t.get(1, Integer.class) - 1];
    }

    private void prospectionsParMois(LocalDateTime debut, LocalDateTime fin, List<UUID> tranches,
                                     Map<UUID, long[][]> parCle) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<ComProspection> p = cq.from(ComProspection.class);
        Path<UUID> commercial = p.get(CFG_COMMERCIAL).get(ID);
        Expression<Integer> mois = cb.function(MONTH, Integer.class, p.get(DOC_DATE));
        List<Predicate> w = new ArrayList<>(List.of(perimetreService.filtreProspections().toPredicate(p, cq, cb),
                cb.isNotNull(commercial), entre(cb, p.get(DOC_DATE), debut, fin)));
        dansLesTranches(w, p.get(STK_HIERARCHY).get(ID), tranches);
        cq.multiselect(commercial, mois, cb.count(p)).where(w.toArray(new Predicate[0])).groupBy(commercial, mois);
        em.createQuery(cq).getResultList().forEach(t -> mois(parCle, t)[0] += SuiviCommerciauxService.nombre(t, 2));
    }

    /** Visites (de prospection et de vente) et communications, comme {@link #visites} : une requête sur l'année. */
    private void interactionsParMois(List<UUID> cles, LocalDateTime debut, LocalDateTime fin, List<UUID> tranches,
                                     Map<UUID, long[][]> parCle) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<ComActionMessageSuivi> r = cq.from(ComActionMessageSuivi.class);
        Path<String> type = r.get("type");
        Expression<Integer> mois = cb.function(MONTH, Integer.class, r.get("dateSaisie"));
        cq.multiselect(r.get(COMMERCIAL_CLE), mois,
                        SuiviCommerciauxService.compter(cb, SuiviCommerciauxService.visite(r, cb, ComActionMessageSuivi.PROSPECTION)),
                        SuiviCommerciauxService.compter(cb, SuiviCommerciauxService.visite(r, cb, ComActionMessageSuivi.VENTE)),
                        SuiviCommerciauxService.compter(cb, type.in(ENTRANTES)),
                        SuiviCommerciauxService.compter(cb, type.in(SORTANTES)))
                .where(interactions(r, cq, cb, cles, debut, fin, tranches)).groupBy(r.get(COMMERCIAL_CLE), mois);
        for (Tuple t : em.createQuery(cq).getResultList()) {
            if (t.get(0) != null) {
                long[] c = mois(parCle, t);
                c[1] += SuiviCommerciauxService.nombre(t, 2) + SuiviCommerciauxService.nombre(t, 3);
                c[2] += SuiviCommerciauxService.nombre(t, 4);
                c[3] += SuiviCommerciauxService.nombre(t, 5);
            }
        }
    }

    /**
     * Visites et communications visibles de l'année que le suivi n'attribue à personne (auteur non résolu), donc
     * absentes de {@link #parMois} ; « Système » exclu comme dans {@link ComActionMessageSuivi}.
     */
    public long interactionsSansAuteur(int annee, List<UUID> tranches) {
        if (tranches.isEmpty()) {
            return 0;
        }
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<ComActionMessage> m = cq.from(ComActionMessage.class);
        Expression<UUID> tranche = cb.coalesce(
                m.join("comProspection", JoinType.LEFT).join(STK_HIERARCHY, JoinType.LEFT).<UUID>get(ID),
                m.join("comFolder", JoinType.LEFT).join(STK_ITEM, JoinType.LEFT).join(STK_HIERARCHY, JoinType.LEFT)
                        .<UUID>get(ID));
        Subquery<UUID> attribuees = cq.subquery(UUID.class);
        attribuees.select(attribuees.from(ComActionMessageSuivi.class).get(ID));
        Path<String> auteur = m.get("userName");
        LocalDateTime debut = LocalDate.of(annee, Month.JANUARY, 1).atStartOfDay();
        cq.select(cb.count(m)).where(perimetreService.filtreInteractions().toPredicate(m, cq, cb),
                m.join("comActionMessageType").get("code").in(TYPES_COMPTES), tranche.in(tranches),
                entre(cb, m.get(DOC_DATE),debut, debut.plusYears(1)),
                cb.or(cb.isNull(auteur), cb.notEqual(auteur, SYSTEME)), cb.not(m.get(ID).in(attribuees)));
        return em.createQuery(cq).getSingleResult();
    }

    /** Ventes : dossiers accordés sur la période ({@code AgreementDate}, §7.5), annulés ou désistés exclus (§9.8). */
    private void ventes(LocalDateTime debut, LocalDateTime fin, List<UUID> tranches, Map<UUID, Indicateurs> parCle) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<ComFolder> f = cq.from(ComFolder.class);
        Join<ComFolder, StrStatus> statut = f.join("strStatus", JoinType.LEFT);
        Path<UUID> commercial = f.get(CFG_COMMERCIAL).get(ID);
        List<Predicate> w = new ArrayList<>(List.of(perimetreService.filtreDossiers().toPredicate(f, cq, cb),
                cb.isNotNull(commercial), entre(cb, f.get("agreementDate"), debut, fin),
                pasVrai(cb, statut.get("annulation")), pasVrai(cb, statut.get("resiliation"))));
        dansLesTranches(w, f.get(STK_ITEM).get(STK_HIERARCHY).get(ID), tranches);
        cq.multiselect(commercial, cb.count(f), SuiviCommerciauxService.compter(cb, cb.isNotNull(f.get("comProspection"))),
                        cb.sum(f.get("price")))
                .where(w.toArray(new Predicate[0])).groupBy(commercial);
        cumuler(cq, parCle, t -> new Indicateurs(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, decimal(t, 1),
                decimal(t, 2), decimal(t, 3), BigDecimal.ZERO));
    }

    /**
     * Seul calcul des versements (règle 6) : lignes de règlement au crédit et comptabilisées, datées
     * {@code ISNULL(EcheanceDate, DocDate)} du règlement, au commercial du dossier visible de l'échéance (OMRAN SETTLEMENT).
     */
    private void versements(LocalDateTime debut, LocalDateTime fin, List<UUID> tranches,
                            Map<UUID, Indicateurs> parCle) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<StlSettlementLine> ligne = cq.from(StlSettlementLine.class);
        Join<StlSettlementLine, StlSettlement> reglement = ligne.join("stlSettlement");
        Join<StlSettlement, StlSettlementType> type = reglement.join("stlSettlementType", JoinType.LEFT);
        Join<ComDocument, ComFolder> dossier = ligne.<StlSettlementLine, ComDocument>join("comDocument").join("comFolder");
        Path<UUID> commercial = dossier.get(CFG_COMMERCIAL).get(ID);

        Subquery<UUID> visibles = cq.subquery(UUID.class);
        Root<ComFolder> v = visibles.from(ComFolder.class);
        visibles.select(v.get(ID)).where(perimetreService.filtreDossiers().toPredicate(v, cq, cb));

        List<Predicate> w = new ArrayList<>(List.of(cb.isTrue(reglement.get("isCredit")),
                pasVrai(cb, type.get("isNotContabilized")), cb.isNotNull(commercial),
                entre(cb, cb.coalesce(reglement.<LocalDateTime>get("echeanceDate"),
                        reglement.<LocalDateTime>get(DOC_DATE)), debut, fin),
                dossier.get(ID).in(visibles)));
        dansLesTranches(w, dossier.get(STK_ITEM).get(STK_HIERARCHY).get(ID), tranches);
        cq.multiselect(commercial, cb.sum(ligne.get("amount"))).where(w.toArray(new Predicate[0])).groupBy(commercial);
        cumuler(cq, parCle, t -> new Indicateurs(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, decimal(t, 1)));
    }
}

package com.tsix.immocrm.services.suivicommerciaux;

import com.tsix.immocrm.dto.suivicommerciaux.MensuelSuiviCommerciauxDto.BlocMensuel;
import com.tsix.immocrm.dto.suivicommerciaux.MensuelSuiviCommerciauxDto;
import com.tsix.immocrm.dto.suivicommerciaux.MensuelSuiviCommerciauxDto.IndicateursCommercial;
import com.tsix.immocrm.entities.com.ComActionMessageSuivi;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.entities.stl.StlSettlementType;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.exceptions.InvalidRequestException;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

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
    private static final String STK_ITEM = "stkItem";
    private static final String STK_HIERARCHY = "stkHierarchy";

    private final SuiviCommerciauxService suiviService;
    private final PerimetreCommercialService perimetreService;
    private final CommerciauxVisibles commerciaux;
    private final EntityManager em;

    public SuiviCommerciauxMensuelService(SuiviCommerciauxService suiviService,
                                          PerimetreCommercialService perimetreService,
                                          CommerciauxVisibles commerciaux, EntityManager em) {
        this.suiviService = suiviService;
        this.perimetreService = perimetreService;
        this.commerciaux = commerciaux;
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
            Function<BigDecimal, BigDecimal> d = v -> v.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
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
        LocalDate debutMois = LocalDate.of(annee, mois, 1);
        LocalDate debutAnnee = LocalDate.of(annee, 1, 1);
        return new MensuelSuiviCommerciauxDto(
                bloc(retenus, filtre, debutMois.atStartOfDay(), debutMois.plusMonths(1).atStartOfDay(), trancheIds),
                bloc(retenus, filtre, debutAnnee.atStartOfDay(), debutAnnee.plusYears(1).atStartOfDay(), trancheIds));
    }

    private BlocMensuel bloc(List<MembreEquipe> retenus, boolean filtre, LocalDateTime debut, LocalDateTime fin,
                             List<UUID> tranches) {
        Map<UUID, Indicateurs> parCle = new HashMap<>();
        prospections(debut, fin, tranches, parCle);
        visites(retenus, debut, fin, tranches, parCle);
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
                cb.isNotNull(commercial), entre(cb, p.get("docDate"), debut, fin)));
        dansLesTranches(w, p.get(STK_HIERARCHY).get(ID), tranches);
        cq.multiselect(commercial, cb.count(p)).where(w.toArray(new Predicate[0])).groupBy(commercial);
        cumuler(cq, parCle, t -> indicateurs(decimal(t, 1), BigDecimal.ZERO, BigDecimal.ZERO));
    }

    private void visites(List<MembreEquipe> retenus, LocalDateTime debut, LocalDateTime fin, List<UUID> tranches,
                         Map<UUID, Indicateurs> parCle) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<ComActionMessageSuivi> r = cq.from(ComActionMessageSuivi.class);
        Path<UUID> cle = r.get("commercialCle");
        List<Predicate> w = new ArrayList<>(List.of(
                suiviService.base(retenus.stream().map(CommerciauxVisibles::cle).toList()).toPredicate(r, cq, cb),
                SuiviCommerciauxService.saisieEntre(r, cb, debut, fin)));
        dansLesTranches(w, r.get("trancheId"), tranches);
        cq.multiselect(cle,
                        SuiviCommerciauxService.compter(cb, SuiviCommerciauxService.visite(r, cb, ComActionMessageSuivi.PROSPECTION)),
                        SuiviCommerciauxService.compter(cb, SuiviCommerciauxService.visite(r, cb, ComActionMessageSuivi.VENTE)))
                .where(w.toArray(new Predicate[0])).groupBy(cle);
        cumuler(cq, parCle, t -> indicateurs(BigDecimal.ZERO, decimal(t, 1), decimal(t, 2)));
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
                        reglement.<LocalDateTime>get("docDate")), debut, fin),
                dossier.get(ID).in(visibles)));
        dansLesTranches(w, dossier.get(STK_ITEM).get(STK_HIERARCHY).get(ID), tranches);
        cq.multiselect(commercial, cb.sum(ligne.get("amount"))).where(w.toArray(new Predicate[0])).groupBy(commercial);
        cumuler(cq, parCle, t -> new Indicateurs(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, decimal(t, 1)));
    }
}

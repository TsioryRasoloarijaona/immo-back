package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.achat.SoldeDocumentAchat;
import com.tsix.immocrm.domain.projet.CalculDecompte;
import com.tsix.immocrm.domain.projet.MontantsLot;
import com.tsix.immocrm.domain.projet.NetAPayerDecompte;
import com.tsix.immocrm.dto.projet.DecompteDetailDto;
import com.tsix.immocrm.dto.projet.DecompteListItemDto;
import com.tsix.immocrm.dto.projet.ExtraDecompteDto;
import com.tsix.immocrm.dto.projet.FactureDecompteDto;
import com.tsix.immocrm.dto.projet.LigneApprovisionnementDto;
import com.tsix.immocrm.dto.projet.LigneDecompteDto;
import com.tsix.immocrm.dto.projet.RetenueDecompteDto;
import com.tsix.immocrm.dto.projet.TotauxDecomptesDto;
import com.tsix.immocrm.entities.buy.BuyDocument;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.entities.prj.PrjWorkingOut;
import com.tsix.immocrm.entities.prj.PrjWorkingOutLine;
import com.tsix.immocrm.entities.prj.PrjWorkingOutLineExtra;
import com.tsix.immocrm.entities.prj.PrjWorkingOutRetainedLine;
import com.tsix.immocrm.entities.prj.PrjWorkingOutStockLine;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.prj.PrjWorkingOutRepository;
import com.tsix.immocrm.services.perimetre.PerimetreProjetService;
import com.tsix.immocrm.utils.TierNomFormatter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class DecomptesProjetService {

    static final String DECOMPTE_INTROUVABLE = "Décompte introuvable";
    private static final FacturesDecomptes.Facture SANS_FACTURE =
            new FacturesDecomptes.Facture(null, null, null, null, null, null, false, SoldeDocumentAchat.de(null, null));
    private static final AvancementDecomptes.Avancement SANS_AVANCEMENT =
            new AvancementDecomptes.Avancement(BigDecimal.ZERO, BigDecimal.ZERO);
    private static final String DESCRIPTION = "description";
    private static final String CODE = "code";
    private static final String ID = "id";

    private final PrjWorkingOutRepository decomptes;
    private final FacturesDecomptes factures;
    private final PerimetreProjetService perimetre;
    private final EntityManager em;
    private final VerrouDecompte verrou;
    private final AvancementDecomptes avancement;
    private final DonneesDecompte donnees;

    public DecomptesProjetService(PrjWorkingOutRepository decomptes, FacturesDecomptes factures,
                                  PerimetreProjetService perimetre, EntityManager em, VerrouDecompte verrou,
                                  AvancementDecomptes avancement, DonneesDecompte donnees) {
        this.decomptes = decomptes;
        this.factures = factures;
        this.perimetre = perimetre;
        this.em = em;
        this.verrou = verrou;
        this.avancement = avancement;
        this.donnees = donnees;
    }

    /** Ce qu'une ligne de liste lit hors de son décompte, chargé une fois pour toute la page. */
    private record Contexte(Map<UUID, FacturesDecomptes.Facture> factures, Map<UUID, AvancementDecomptes.Avancement> avancements,
                            Map<UUID, Integer> derniers, Set<String> droits) {
    }

    public Page<DecompteListItemDto> decomptes(FiltresDecomptes filtres, Pageable pageable) {
        Set<UUID> projets = perimetre.restreindre(filtres.projetIds());
        if (projets.isEmpty()) {
            return Page.empty(pageable);
        }
        Page<PrjWorkingOut> page = decomptes.findAll(specification(filtres, projets), pageable);
        return new PageImpl<>(lignes(page.getContent()), pageable, page.getTotalElements());
    }

    public TotauxDecomptesDto totaux(FiltresDecomptes filtres) {
        Set<UUID> projets = perimetre.restreindre(filtres.projetIds());
        List<DecompteListItemDto> l = projets.isEmpty() ? List.of() : lignes(decomptes.findAll(specification(filtres, projets)));
        return new TotauxDecomptesDto(l.size(), somme(l, DecompteListItemDto::montantHT), somme(l, DecompteListItemDto::montantTTC),
                somme(l, DecompteListItemDto::lignesTTC), somme(l, DecompteListItemDto::depassementTTC),
                somme(l, DecompteListItemDto::travauxSupplementaires), somme(l, DecompteListItemDto::approvisionnementAPayer),
                somme(l, DecompteListItemDto::fluctuationsAPayer), somme(l, DecompteListItemDto::totalTTC),
                somme(l, DecompteListItemDto::retenueGarantie), somme(l, DecompteListItemDto::netAPayer),
                somme(l, DecompteListItemDto::montantRegle), somme(l, DecompteListItemDto::resteARegler));
    }

    /** Décomptes d'un lot déjà reconnu visible, tri numéro puis date. */
    List<DecompteListItemDto> duLot(UUID lotId) {
        return lignes(decomptes.findByPrjMarketIdOrderBySortingAscDocDateAsc(lotId));
    }

    /** Seule lecture d'un décompte par id : 404 hors des projets habilités. */
    PrjWorkingOut visible(UUID id) {
        Set<UUID> projets = perimetre.projetsVisibles();
        return projets.isEmpty() ? introuvable()
                : decomptes.findByIdAndPrjMarketPrjProjectIdIn(id, projets).orElseGet(DecomptesProjetService::introuvable);
    }

    /** Lot d'un projet habilité, sinon 404. */
    PrjMarket lotVisible(UUID lotId) {
        Set<UUID> projets = perimetre.projetsVisibles();
        return (projets.isEmpty() ? Stream.<PrjMarket>empty()
                : em.createQuery("select m from PrjMarket m where m.id = :id and m.prjProject.id in :projets", PrjMarket.class)
                .setParameter(ID, lotId).setParameter("projets", projets).getResultStream())
                .findFirst().orElseThrow(() -> new ResourceNotFoundException("Lot introuvable"));
    }

    public DecompteDetailDto decompte(UUID id) {
        PrjWorkingOut w = visible(id);
        Contexte ctx = contexte(List.of(w));
        DecompteListItemDto item = ligne(w, ctx);
        FacturesDecomptes.Facture f = ctx.factures().getOrDefault(id, SANS_FACTURE);
        boolean peutModifier = item.motifVerrou() == null && ctx.droits().contains(VerrouDecompte.MODIFIER);
        BigDecimal cumul = cumulTTC(w);
        return new DecompteDetailDto(item, w.getAmountVat(), MontantsLot.pourcentage(w.getWarrantyRatio()),
                w.getWorkingOutAmountTTC(), w.getAdvanceAmount(), w.getWorkingOutAdvanceAmount(), w.getProportionAmount(),
                w.getInsuranceDECAmount(), w.getInsuranceTRCAmount(), w.getNonRefundableRetainer(), w.getRefundableRetainer(),
                w.getFluctuations(), w.getAmountToBePaid(), cumul,
                w.getUserCreateDateTime(), w.getUserCreate(), w.getUserUpdateDateTime(), w.getUserUpdate(),
                facture(w.getBuyDocument(), f), f.id() == null ? List.of() : factures.reglements(f.id()),
                lignes(id), extras(id), retenues(id), w.getDocRef(), vrai(w.getAdvanceBasedOnAmount()),
                vrai(w.getWarrantyBasedOnAmount()), vrai(w.getDecBasedOnAmount()), vrai(w.getTrcBasedOnAmount()),
                vrai(w.getProrataBasedOnAmount()), MontantsLot.pourcentage(w.getAdvanceRatio()),
                MontantsLot.pourcentage(w.getInsuranceDECRatio()), MontantsLot.pourcentage(w.getInsuranceTRCRatio()),
                MontantsLot.pourcentage(w.getProportionRatio()), w.getProrataRefundAmount(),
                z(cumul).subtract(z(w.getAmountTTC())), MontantsLot.pourcentage(w.getStockPileRatio()), w.getStockAmountOld(),
                w.getStockAmount(), w.getFluctuationsOld(), w.getAdvanceRest(), w.getWorkingOutAdvanceAmountRestAtCreation(),
                w.getAmountRegul(), approvisionnements(id), peutModifier,
                peutModifier && ctx.droits().contains(VerrouDecompte.RECALCULER),
                peutModifier && !item.dernier());
    }

    private List<DecompteListItemDto> lignes(List<PrjWorkingOut> liste) {
        Contexte ctx = contexte(liste);
        return liste.stream().map(w -> ligne(w, ctx)).toList();
    }

    private Contexte contexte(List<PrjWorkingOut> liste) {
        Set<UUID> lots = liste.stream().map(w -> w.getPrjMarket().getId()).collect(Collectors.toSet());
        return new Contexte(factures.parDecompte(liste.stream().map(PrjWorkingOut::getId).toList()),
                avancement.parDecompte(lots), verrou.derniersNumeros(lots), verrou.droitsCourants());
    }

    private static DecompteListItemDto ligne(PrjWorkingOut w, Contexte ctx) {
        PrjMarket lot = w.getPrjMarket();
        FacturesDecomptes.Facture f = ctx.factures().getOrDefault(w.getId(), SANS_FACTURE);
        AvancementDecomptes.Avancement a = ctx.avancements().getOrDefault(w.getId(), SANS_AVANCEMENT);
        String motif = VerrouDecompte.motif(w, ctx.derniers().get(lot.getId()));
        CfgTier fournisseur = w.getCfgTier() != null ? w.getCfgTier() : lot.getCfgSupplier();
        NetAPayerDecompte net = NetAPayerDecompte.de(new NetAPayerDecompte.Montants(w.getAmountTTC(), w.getAdvanceAmount(),
                w.getWorkingOutAdvanceAmount(), w.getWarrantyAmount(), w.getNonRefundableRetainer(), w.getExtraAmount(),
                w.getProrataRefundAmount(), w.getStockAmountToPay(), w.getFluctuationsToPay(), w.getAmountToPay()));
        return new DecompteListItemDto(w.getId(), w.getCode(), w.getSorting(), jour(w.getDocDate()), lot.getId(),
                lot.getCode(), lot.getDescription(), lot.getPrjProject().getId(), lot.getPrjProject().getCode(),
                fournisseur == null ? null : fournisseur.getId(), TierNomFormatter.nom(fournisseur),
                w.getAmountHT(), w.getAmountTTC(), w.getExtraAmount(), w.getWarrantyAmount(), w.getAmountToPay(),
                net.recalcule(), net.ecart(), net.significatif(), w.getAmountTTCAfterDeductions(),
                Boolean.TRUE.equals(w.getFinalWorkingOut()), f.id(), f.code(), f.statutCode(), f.statutDescription(),
                f.solde().montantRegle(), f.solde().resteARegler(), w.getNotes(),
                z(w.getAmountTTC()).subtract(a.depassementTTC()), a.avaPct(), a.depassementTTC(), w.getStockAmountToPay(),
                w.getFluctuationsToPay(), CalculDecompte.totalTtc(w.getAmountTTC(), w.getExtraAmount(), w.getStockAmountToPay(),
                w.getFluctuationsToPay()), motif != null, motif,
                motif == null && ctx.droits().contains(VerrouDecompte.SUPPRIMER));
    }

    private static FactureDecompteDto facture(BuyDocument b, FacturesDecomptes.Facture f) {
        if (b == null || f.id() == null) {
            return null;
        }
        return new FactureDecompteDto(b.getId(), b.getCode(), jour(b.getDocDate()), jour(b.getMaturityDate()),
                b.getAmountHT(), b.getAmountTTC(), b.getAmountToPay(), b.getAmountRegul(), f.statutCode(), f.statutDescription(),
                f.okPourPaiement());
    }

    private BigDecimal cumulTTC(PrjWorkingOut w) {
        if (w.getSorting() == null) {
            return w.getAmountTTC();
        }
        return em.createQuery("select coalesce(sum(o.amountTTC), 0) from PrjWorkingOut o"
                        + " where o.prjMarket.id = :lot and o.sorting <= :numero", BigDecimal.class)
                .setParameter("lot", w.getPrjMarket().getId())
                .setParameter("numero", w.getSorting())
                .getSingleResult();
    }

    private List<LigneDecompteDto> lignes(UUID decompte) {
        List<Object[]> lignes = em.createQuery("select l, ml.quantity from PrjWorkingOutLine l"
                        + " left join PrjMarketLine ml on ml.id = l.prjMarketLineId"
                        + " where l.prjWorkingOutId = :d order by l.sorting, l.code", Object[].class)
                .setParameter("d", decompte)
                .getResultList();
        return lignes.stream().map(r -> ligneDecompte((PrjWorkingOutLine) r[0], (BigDecimal) r[1])).toList();
    }

    static LigneDecompteDto ligneDecompte(PrjWorkingOutLine l, BigDecimal quantiteMarche) {
        BigDecimal cumulee = somme(l.getQuantityOld(), l.getQuantity());
        return new LigneDecompteDto(l.getId(), l.getSorting(), l.getDescription(), l.getUnity(), quantiteMarche,
                l.getUnitPriceHT(), l.getQuantityOld(), l.getQuantity(), cumulee, l.getCoefficient(),
                MontantsLot.applique(l.getQuantity(), l.getUnitPriceHT()), MontantsLot.avancementPct(cumulee, quantiteMarche),
                l.getPrjMarketLineId(), l.getQuantityOrigin());
    }

    private List<ExtraDecompteDto> extras(UUID decompte) {
        return donnees.enfants(PrjWorkingOutLineExtra.class, decompte).stream()
                .map(e -> new ExtraDecompteDto(e.getId(), e.getDescription(), e.getUnity(), e.getQuantity(), e.getUnitPriceHT(),
                        MontantsLot.pourcentage(e.getVatRatio()), MontantsLot.applique(e.getQuantity(), e.getUnitPriceHT()),
                        MontantsLot.applique(e.getQuantity(), e.getUnitPriceTTC())))
                .toList();
    }

    private List<RetenueDecompteDto> retenues(UUID decompte) {
        return donnees.enfants(PrjWorkingOutRetainedLine.class, decompte).stream()
                .map(r -> new RetenueDecompteDto(r.getId(), r.getDescription(), r.getAmount()))
                .toList();
    }

    private List<LigneApprovisionnementDto> approvisionnements(UUID decompte) {
        return donnees.enfants(PrjWorkingOutStockLine.class, decompte).stream().map(s -> {
            CalculDecompte.Ligne l = new CalculDecompte.Ligne(s.getQuantity(), s.getUnitPriceHT(), s.getFodecRatio(), s.getVatRatio());
            return new LigneApprovisionnementDto(s.getId(), s.getSorting(), s.getCode(), s.getDescription(), s.getUnity(),
                    s.getQuantity(), s.getUnitPriceHT(), MontantsLot.pourcentage(s.getVatRatio()),
                    l.ht().setScale(2, RoundingMode.HALF_UP), l.ttc().setScale(2, RoundingMode.HALF_UP));
        }).toList();
    }

    private static Specification<PrjWorkingOut> specification(FiltresDecomptes filtres, Collection<UUID> projets) {
        return (root, q, cb) -> filtre(root, cb, filtres, projets);
    }

    private static Predicate filtre(Root<PrjWorkingOut> root, CriteriaBuilder cb, FiltresDecomptes f, Collection<UUID> projets) {
        From<?, ?> lot = root.join("prjMarket");
        List<Predicate> p = new ArrayList<>();
        p.add(lot.get("prjProject").get(ID).in(projets));
        if (f.lotId() != null) {
            p.add(cb.equal(lot.get(ID), f.lotId()));
        }
        if (f.societeId() != null) {
            p.add(cb.equal(lot.get("prjProject").get("cfgCompany").get(ID), f.societeId()));
        }
        From<?, ?> tiers = root.join("cfgTier", JoinType.LEFT);
        From<?, ?> fournisseurLot = lot.join("cfgSupplier", JoinType.LEFT);
        if (f.fournisseurId() != null) {
            p.add(cb.or(cb.equal(tiers.get(ID), f.fournisseurId()),
                    cb.and(cb.isNull(tiers.get(ID)), cb.equal(fournisseurLot.get(ID), f.fournisseurId()))));
        }
        if (f.du() != null) {
            p.add(cb.greaterThanOrEqualTo(root.get("docDate"), f.du().atStartOfDay()));
        }
        if (f.au() != null) {
            p.add(cb.lessThan(root.get("docDate"), f.au().plusDays(1).atStartOfDay()));
        }
        From<?, ?> facture = root.join("buyDocument", JoinType.LEFT);
        if (f.statutFactureId() != null) {
            p.add(cb.equal(facture.get("buyDocumentStatusId"), f.statutFactureId()));
        }
        if (Boolean.TRUE.equals(f.sansFacture())) {
            p.add(cb.isNull(facture.get(ID)));
        }
        if (StringUtils.hasText(f.search())) {
            String motif = "%" + f.search().strip() + "%";
            p.add(cb.or(cb.like(root.get(CODE), motif), cb.like(root.get("docRef"), motif), cb.like(root.get("notes"), motif),
                    cb.like(lot.get(CODE), motif), cb.like(lot.get(DESCRIPTION), motif),
                    cb.like(tiers.get("socialReason"), motif), cb.like(tiers.get(DESCRIPTION), motif),
                    cb.like(fournisseurLot.get("socialReason"), motif), cb.like(fournisseurLot.get(DESCRIPTION), motif)));
        }
        return cb.and(p.toArray(Predicate[]::new));
    }

    private static PrjWorkingOut introuvable() {
        throw new ResourceNotFoundException(DECOMPTE_INTROUVABLE);
    }

    static LocalDate jour(LocalDateTime instant) {
        return instant == null ? null : instant.toLocalDate();
    }

    private static BigDecimal somme(List<DecompteListItemDto> lignes, Function<DecompteListItemDto, BigDecimal> colonne) {
        return lignes.stream().map(colonne).map(DecomptesProjetService::z).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal somme(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) {
            return null;
        }
        return z(a).add(z(b));
    }

    private static boolean vrai(Boolean b) {
        return Boolean.TRUE.equals(b);
    }

    private static BigDecimal z(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}

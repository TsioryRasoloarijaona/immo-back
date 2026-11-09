package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.achat.SoldeDocumentAchat;
import com.tsix.immocrm.domain.projet.MontantsLot;
import com.tsix.immocrm.domain.projet.NetAPayerDecompte;
import com.tsix.immocrm.dto.projet.DecompteDetailDto;
import com.tsix.immocrm.dto.projet.DecompteListItemDto;
import com.tsix.immocrm.dto.projet.ExtraDecompteDto;
import com.tsix.immocrm.dto.projet.FactureDecompteDto;
import com.tsix.immocrm.dto.projet.LigneDecompteDto;
import com.tsix.immocrm.dto.projet.RetenueDecompteDto;
import com.tsix.immocrm.entities.buy.BuyDocument;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.entities.prj.PrjWorkingOut;
import com.tsix.immocrm.entities.prj.PrjWorkingOutLine;
import com.tsix.immocrm.entities.prj.PrjWorkingOutLineExtra;
import com.tsix.immocrm.entities.prj.PrjWorkingOutRetainedLine;
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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DecomptesProjetService {

    static final String DECOMPTE_INTROUVABLE = "Décompte introuvable";
    private static final FacturesDecomptes.Facture SANS_FACTURE =
            new FacturesDecomptes.Facture(null, null, null, null, null, null, SoldeDocumentAchat.de(null, null));
    private static final String DESCRIPTION = "description";
    private static final String CODE = "code";
    private static final String ID = "id";

    private final PrjWorkingOutRepository decomptes;
    private final FacturesDecomptes factures;
    private final PerimetreProjetService perimetre;
    private final EntityManager em;

    public DecomptesProjetService(PrjWorkingOutRepository decomptes, FacturesDecomptes factures,
                                  PerimetreProjetService perimetre, EntityManager em) {
        this.decomptes = decomptes;
        this.factures = factures;
        this.perimetre = perimetre;
        this.em = em;
    }

    public Page<DecompteListItemDto> decomptes(FiltresDecomptes filtres, Pageable pageable) {
        Set<UUID> projets = perimetre.restreindre(filtres.projetIds());
        if (projets.isEmpty()) {
            return Page.empty(pageable);
        }
        Page<PrjWorkingOut> page = decomptes.findAll((root, q, cb) -> filtre(root, cb, filtres, projets), pageable);
        Map<UUID, FacturesDecomptes.Facture> parDecompte = factures.parDecompte(page.map(PrjWorkingOut::getId).getContent());
        return page.map(w -> ligne(w, parDecompte.getOrDefault(w.getId(), SANS_FACTURE)));
    }

    /** Décomptes d'un lot déjà reconnu visible, tri numéro puis date. */
    List<DecompteListItemDto> duLot(UUID lotId) {
        List<PrjWorkingOut> liste = decomptes.findByPrjMarketIdOrderBySortingAscDocDateAsc(lotId);
        Map<UUID, FacturesDecomptes.Facture> parDecompte = factures.parDecompte(liste.stream().map(PrjWorkingOut::getId).toList());
        return liste.stream().map(w -> ligne(w, parDecompte.getOrDefault(w.getId(), SANS_FACTURE))).toList();
    }

    public DecompteDetailDto decompte(UUID id) {
        Set<UUID> projets = perimetre.projetsVisibles();
        PrjWorkingOut w = (projets.isEmpty() ? Optional.<PrjWorkingOut>empty()
                : decomptes.findByIdAndPrjMarketPrjProjectIdIn(id, projets))
                .orElseThrow(() -> new ResourceNotFoundException(DECOMPTE_INTROUVABLE));
        FacturesDecomptes.Facture f = factures.parDecompte(List.of(id)).getOrDefault(id, SANS_FACTURE);
        return new DecompteDetailDto(ligne(w, f), w.getAmountVat(), MontantsLot.pourcentage(w.getWarrantyRatio()),
                w.getWorkingOutAmountTTC(), w.getAdvanceAmount(), w.getWorkingOutAdvanceAmount(), w.getProportionAmount(),
                w.getInsuranceDECAmount(), w.getInsuranceTRCAmount(), w.getNonRefundableRetainer(), w.getRefundableRetainer(),
                w.getFluctuations(), w.getFluctuationsToPay(), w.getAmountToBePaid(), cumulTTC(w),
                w.getUserCreateDateTime(), w.getUserCreate(), w.getUserUpdateDateTime(), w.getUserUpdate(),
                facture(w.getBuyDocument(), f), f.id() == null ? List.of() : factures.reglements(f.id()),
                lignes(id), extras(id), retenues(id));
    }

    private static DecompteListItemDto ligne(PrjWorkingOut w, FacturesDecomptes.Facture f) {
        PrjMarket lot = w.getPrjMarket();
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
                f.solde().montantRegle(), f.solde().resteARegler());
    }

    private static FactureDecompteDto facture(BuyDocument b, FacturesDecomptes.Facture f) {
        if (b == null || f.id() == null) {
            return null;
        }
        return new FactureDecompteDto(b.getId(), b.getCode(), jour(b.getDocDate()), jour(b.getMaturityDate()),
                b.getAmountHT(), b.getAmountTTC(), b.getAmountToPay(), b.getAmountRegul(), f.statutCode(), f.statutDescription());
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

    private static LigneDecompteDto ligneDecompte(PrjWorkingOutLine l, BigDecimal quantiteMarche) {
        BigDecimal cumulee = somme(l.getQuantityOld(), l.getQuantity());
        return new LigneDecompteDto(l.getId(), l.getSorting(), l.getDescription(), l.getUnity(), quantiteMarche,
                l.getUnitPriceHT(), l.getQuantityOld(), l.getQuantity(), cumulee, l.getCoefficient(),
                MontantsLot.applique(l.getQuantity(), l.getUnitPriceHT()), MontantsLot.avancementPct(cumulee, quantiteMarche));
    }

    private List<ExtraDecompteDto> extras(UUID decompte) {
        return em.createQuery("select e from PrjWorkingOutLineExtra e where e.prjWorkingOutId = :d order by e.sorting, e.code",
                        PrjWorkingOutLineExtra.class)
                .setParameter("d", decompte)
                .getResultList().stream()
                .map(e -> new ExtraDecompteDto(e.getId(), e.getDescription(), e.getUnity(), e.getQuantity(), e.getUnitPriceHT(),
                        MontantsLot.pourcentage(e.getVatRatio()), MontantsLot.applique(e.getQuantity(), e.getUnitPriceHT()),
                        MontantsLot.applique(e.getQuantity(), e.getUnitPriceTTC())))
                .toList();
    }

    private List<RetenueDecompteDto> retenues(UUID decompte) {
        return em.createQuery("select r from PrjWorkingOutRetainedLine r where r.prjWorkingOutId = :d order by r.sorting",
                        PrjWorkingOutRetainedLine.class)
                .setParameter("d", decompte)
                .getResultList().stream()
                .map(r -> new RetenueDecompteDto(r.getId(), r.getDescription(), r.getAmount()))
                .toList();
    }

    private static Predicate filtre(Root<PrjWorkingOut> root, CriteriaBuilder cb, FiltresDecomptes f, Collection<UUID> projets) {
        From<?, ?> lot = root.join("prjMarket");
        List<Predicate> p = new ArrayList<>();
        p.add(lot.get("prjProject").get(ID).in(projets));
        if (f.lotId() != null) {
            p.add(cb.equal(lot.get(ID), f.lotId()));
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
            p.add(cb.or(cb.like(root.get(CODE), motif), cb.like(lot.get(CODE), motif), cb.like(lot.get(DESCRIPTION), motif),
                    cb.like(tiers.get("socialReason"), motif), cb.like(tiers.get(DESCRIPTION), motif),
                    cb.like(fournisseurLot.get("socialReason"), motif), cb.like(fournisseurLot.get(DESCRIPTION), motif)));
        }
        return cb.and(p.toArray(Predicate[]::new));
    }

    static LocalDate jour(LocalDateTime instant) {
        return instant == null ? null : instant.toLocalDate();
    }

    private static BigDecimal somme(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) {
            return null;
        }
        return (a == null ? BigDecimal.ZERO : a).add(b == null ? BigDecimal.ZERO : b);
    }
}

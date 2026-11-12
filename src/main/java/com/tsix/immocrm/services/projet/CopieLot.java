package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.projet.MontantsLot.Trio;
import com.tsix.immocrm.dto.projet.LotDetailDto;
import com.tsix.immocrm.entities.AuditOmran;
import com.tsix.immocrm.entities.prj.GroupeBordereauOmran;
import com.tsix.immocrm.entities.prj.LigneBordereauOmran;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.entities.prj.PrjMarketLine;
import com.tsix.immocrm.entities.prj.PrjMarketLineEstimation;
import com.tsix.immocrm.entities.prj.PrjMarketLineGrouping;
import com.tsix.immocrm.entities.prj.PrjMarketLineGroupingEstimation;
import com.tsix.immocrm.security.UtilisateurAffiche;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Copie d'un lot comme OMRAN {@code MarketMonitoring.Clone} (FEATURE-lot-fiche-parite.md E8, réponse du manager 3). */
@Service
public class CopieLot {

    private static final String STOCK = "isStock";

    private final EcrituresLotService ecritures;
    private final LotsProjetService lots;
    private final TransitionsLot transitions;
    private final MontantsListeLots montants;
    private final UtilisateurAffiche utilisateur;
    private final EntityManager em;
    private final Clock clock;

    public CopieLot(EcrituresLotService ecritures, LotsProjetService lots, TransitionsLot transitions,
                    MontantsListeLots montants, UtilisateurAffiche utilisateur, EntityManager em, Clock clock) {
        this.ecritures = ecritures;
        this.lots = lots;
        this.transitions = transitions;
        this.montants = montants;
        this.utilisateur = utilisateur;
        this.em = em;
        this.clock = clock;
    }

    /**
     * Le bordereau source (définitif s'il a des lignes hors multi-fournisseur, sinon estimatif) devient l'estimatif de la copie ;
     * le définitif de la copie reste vide, la source n'est jamais modifiée. Lignes de stock hors périmètre (L3).
     */
    @Transactional
    public LotDetailDto copier(UUID id) {
        PrjMarket m = ecritures.copiable(id);
        boolean multi = Boolean.TRUE.equals(m.getIsMultiSupplier());
        List<? extends LigneBordereauOmran> lignes = multi ? List.of() : duLot(PrjMarketLine.class, id);
        boolean definitif = !lignes.isEmpty();
        if (!definitif) {
            lignes = duLot(PrjMarketLineEstimation.class, id);
        }
        Class<? extends GroupeBordereauOmran> classeGroupes =
                definitif ? PrjMarketLineGrouping.class : PrjMarketLineGroupingEstimation.class;
        List<? extends GroupeBordereauOmran> groupes = duLot(classeGroupes, id);
        Trio estimation = montants.estimation(m);

        em.detach(m);
        UUID copie = UUID.randomUUID();
        nouvelle(m, copie);
        m.setCode(ecritures.codeLibre());
        m.setDescription(m.getDescription() + " (Copie)");
        m.setIsClosed(false);
        m.setStrStatus(ecritures.statutDeDepart(EcrituresLotService.genre(m)));
        if (!multi) {
            enTeteHorsMultiFournisseur(m, estimation);
        }
        em.persist(m);
        transitions.historiser(m, TransitionsLot.CREATION, null, "");

        Map<UUID, UUID> cles = new HashMap<>();
        groupes.forEach(g -> cles.put(g.getId(), UUID.randomUUID()));
        groupes.forEach(g -> em.persist(groupe(g, cles, copie)));
        lignes.forEach(l -> em.persist(ligne(l, cles, copie)));
        em.flush();
        return lots.lot(copie);
    }

    /** {@code Clone} l.3135-3155 : fournisseur, dates d'appel d'offres et de marché, résiliation vidés ; estimation figée. */
    private static void enTeteHorsMultiFournisseur(PrjMarket m, Trio estimation) {
        m.setCfgSupplier(null);
        m.setLaunchOfTenderDate(null);
        m.setWithdrawalTenderDocumentsDate(null);
        m.setTenderMaturityDate(null);
        m.setTendersResortDate(null);
        m.setBidOpeningDate(null);
        m.setTendersReportDate(null);
        m.setTenderDate(null);
        m.setContractSignatureDate(null);
        m.setServiceOrderDate(null);
        m.setWorkLaunchDate(null);
        m.setWorkCompletionDate(null);
        m.setReattachmentDate(null);
        m.setCancellationDate(null);
        m.setCancellationCause(null);
        m.setEstimatedPriceHTManual(estimation.ht());
        m.setEstimatedVatAmountManual(estimation.tva());
        m.setEstimatedPriceTTCManual(estimation.ttc());
    }

    private PrjMarketLineGroupingEstimation groupe(GroupeBordereauOmran source, Map<UUID, UUID> cles, UUID copie) {
        PrjMarketLineGroupingEstimation g = new PrjMarketLineGroupingEstimation();
        nouvelle(g, cles.get(source.getId()));
        g.setCode(source.getCode());
        g.setDescription(source.getDescription());
        g.setSorting(source.getSorting());
        g.setNotes(source.getNotes());
        g.setParentId(cles.get(source.getParentId()));
        g.setPrjMarketId(copie);
        g.setIsStock(false);
        return g;
    }

    private PrjMarketLineEstimation ligne(LigneBordereauOmran source, Map<UUID, UUID> cles, UUID copie) {
        PrjMarketLineEstimation l = new PrjMarketLineEstimation();
        nouvelle(l, UUID.randomUUID());
        l.setCode(source.getCode());
        l.setDescription(source.getDescription());
        l.setSorting(source.getSorting());
        l.setUnity(source.getUnity());
        l.setQuantity(source.getQuantity());
        l.setUnitPriceHT(source.getUnitPriceHT());
        l.setUnitPriceTTC(source.getUnitPriceTTC());
        l.setVatRatio(source.getVatRatio());
        l.setFodecRatio(source.getFodecRatio());
        l.setNotes(source.getNotes());
        l.setStlCategoryId(source.getStlCategoryId());
        l.setPrjMarketLineGroupingId(cles.get(source.getPrjMarketLineGroupingId()));
        l.setPrjMarketId(copie);
        l.setIsStock(false);
        return l;
    }

    private <T> List<T> duLot(Class<T> classe, UUID lot) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<T> q = cb.createQuery(classe);
        Root<T> r = q.from(classe);
        q.where(cb.equal(r.get("prjMarketId"), lot), cb.or(cb.isNull(r.get(STOCK)), cb.isFalse(r.<Boolean>get(STOCK))));
        return em.createQuery(q).getResultList();
    }

    /** Ligne insérée sous une nouvelle clé (l'en-tête source, détaché, est réinséré ainsi). */
    private void nouvelle(AuditOmran e, UUID id) {
        e.setId(id);
        e.setUserCreate(utilisateur.courant().nom());
        e.setUserCreateDateTime(LocalDateTime.now(clock));
        e.setUserUpdate(null);
        e.setUserUpdateDateTime(null);
    }
}

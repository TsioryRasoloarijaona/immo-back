package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.domain.CalculPrixArticle;
import com.tsix.immocrm.domain.CalculPrixArticle.PrixLot;
import com.tsix.immocrm.dto.patrimoine.TrancheDetailDto;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.cfg.CfgTrancheCoveredFloor;
import com.tsix.immocrm.entities.cfg.CfgTrancheFolderEntity;
import com.tsix.immocrm.entities.cfg.CfgTrancheLevel;
import com.tsix.immocrm.entities.cfg.CfgTrancheSettlementCategory;
import com.tsix.immocrm.entities.cfg.LigneTrancheOmran;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.lnd.LndLevel;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.str.StrEntity;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.cfg.TrancheVisibleRepository;
import com.tsix.immocrm.utils.Champs;
import jakarta.persistence.EntityManager;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.ou;

/**
 * « Charger à partir d'une autre tranche » (FEATURE-patrimoine-reliquat-v2.md §A ; OMRAN
 * {@code CfgTranche.GetDefaultValuesFromTranche}) : une transaction, rien d'écrit si un contrôle échoue.
 */
@Service
public class ChargementTrancheService {

    /** Propriétés de {@link CfgTranche} copiées telles quelles, null compris (§A.2). */
    private static final List<String> COPIEES = List.of("cfgTrancheStanding", "lndLevel", "cfgClaimsManager",
            "address", "address2", "address3", "zipCode", "city", "admCountry", "phone", "fax", "mobile", "email",
            "webSite", "savAddress",
            "beforeReservationAmount", "advanceBasedOnAmount", "advancePercent", "advanceAmount", "defaultExpenseAmount",
            "cancellationPenalityPercent", "cancellationPenalityBasedOnItemPrice", "articleTransferPenality",
            "sliceTransferPenality", "customerTransferPenality", "syndicAmount", "syndicPeriod", "otherExpenseAmount",
            "agreementFirstDelay", "agreementSecondDelay", "agreementFirstPenaltyPercent", "agreementSecondPenaltyPercent",
            "paymentBalanceDelay", "warrantyDelay", "warrantyDelayAdded", "commerceTribunal", "cancellationRightCode",
            "isWithGiftForProposer", "isWithGiftForOldCustomer", "isWithGiftForProspectionOrigin", "accumulatedGifts",
            "beforeReservationAmountDiscount", "advancePercentDiscount", "isActiveAdvancePercentDiscount",
            "defaultExpenseAmountDiscount", "cancellationPenalityPercentDiscount", "customerTransferPenalityDiscount",
            "articleTransferPenalityDiscount", "sliceTransferPenalityDiscount", "syndicAmountDiscount",
            "otherExpenseAmountDiscount",
            "interventionMinimumDelai", "maxVisitNumber", "delayLawyerMinute", "delayMinuteAllSettled", "descriptionLong");

    private static final String ETAGES = "SELECT l FROM CfgTrancheLevel l WHERE l.cfgTranche.id = :id";
    private static final String PLANCHERS = "SELECT l FROM CfgTrancheCoveredFloor l WHERE l.cfgTranche.id = :id";
    private static final String FRAIS = "SELECT l FROM CfgTrancheSettlementCategory l WHERE l.cfgTranche.id = :id";
    private static final String FINALISATIONS = "SELECT l FROM CfgTrancheFolderEntity l WHERE l.cfgTranche.id = :id";

    private final TrancheVisibleRepository tranches;
    private final ReconciliationLignes reconciliation;
    private final TracePatrimoine trace;
    private final TrancheService trancheService;
    private final EntityManager em;

    public ChargementTrancheService(TrancheVisibleRepository tranches, ReconciliationLignes reconciliation,
                                    TracePatrimoine trace, TrancheService trancheService, EntityManager em) {
        this.tranches = tranches;
        this.reconciliation = reconciliation;
        this.trace = trace;
        this.trancheService = trancheService;
        this.em = em;
    }

    @Transactional
    public TrancheDetailDto charger(UUID cibleId, UUID sourceId) {
        CfgTranche cible = tranches.exigerVisible(cibleId);
        CfgTranche source = tranches.exigerVisible(sourceId);
        if (cible.getId().equals(source.getId())) {
            throw new InvalidRequestException("sourceId : la tranche source doit être une autre tranche.");
        }
        BeanWrapper de = new BeanWrapperImpl(source);
        BeanWrapper vers = new BeanWrapperImpl(cible);
        COPIEES.forEach(p -> vers.setPropertyValue(p, de.getPropertyValue(p)));
        cible.setConcretisationPercent(cible.getAdvancePercent());

        TracePatrimoine.Trace t = trace.maintenant();
        fusionner(source, cible, t, new Liste<>(ETAGES, CfgTrancheLevel.class, CfgTrancheLevel::new,
                l -> Champs.de(l.getLndLevel(), LndLevel::getId), (s, c) -> c.setLndLevel(s.getLndLevel())));
        List<CfgTrancheCoveredFloor> planchers = fusionner(source, cible, t, new Liste<>(PLANCHERS,
                CfgTrancheCoveredFloor.class, CfgTrancheCoveredFloor::new,
                l -> Champs.de(l.getStkItemCategory(), StkItemCategory::getId), (s, c) -> {
                    c.setStkItemCategory(s.getStkItemCategory());
                    c.setArea(s.getArea());
                }));
        cible.setCoveredFloorArea(OngletsTrancheService.sommeSurfaces(planchers));
        fusionner(source, cible, t, new Liste<>(FRAIS, CfgTrancheSettlementCategory.class,
                CfgTrancheSettlementCategory::new, l -> Champs.de(l.getComSettlementCategory(), ComSettlementCategory::getId),
                ChargementTrancheService::copierFrais));
        fusionner(source, cible, t, new Liste<>(FINALISATIONS, CfgTrancheFolderEntity.class,
                CfgTrancheFolderEntity::new, l -> Champs.de(l.getStrFolderEntity(), StrEntity::getId), (s, c) -> {
                    c.setStrFolderEntity(s.getStrFolderEntity());
                    c.setStrStatusDelivery1(s.getStrStatusDelivery1());
                    c.setStrStatusDelivery2(s.getStrStatusDelivery2());
                }));

        cible.setUserUpdate(t.auteur());
        cible.setUserUpdateDateTime(t.le());
        return trancheService.findById(cible.getId());
    }

    /** Liste fusionnée par clé (entité référencée) : requête des lignes ({@code :id}), création, copie source → cible. */
    private record Liste<E extends LigneTrancheOmran>(String jpql, Class<E> classe, Supplier<E> nouvelle,
                                                      Function<E, UUID> cle, BiConsumer<E, E> copier) {
    }

    /** Ligne de la source absente de la cible → créée, commune → reprise ; propre à la cible → conservée. */
    private <E extends LigneTrancheOmran> List<E> fusionner(CfgTranche source, CfgTranche cible,
                                                           TracePatrimoine.Trace t, Liste<E> liste) {
        List<E> lignesCible = new ArrayList<>(reconciliation.lignes(cible, liste.jpql(), liste.classe()));
        Map<UUID, E> parCle = new HashMap<>();
        lignesCible.forEach(l -> parCle.putIfAbsent(liste.cle().apply(l), l));
        for (E ligneSource : reconciliation.lignes(source, liste.jpql(), liste.classe())) {
            E ligne = parCle.get(liste.cle().apply(ligneSource));
            if (ligne == null) {
                ligne = creer(liste.nouvelle(), cible, t);
                parCle.put(liste.cle().apply(ligneSource), ligne);
                lignesCible.add(ligne);
            }
            liste.copier().accept(ligneSource, ligne);
            ligne.setUserUpdate(t.auteur());
            ligne.setUserUpdateDateTime(t.le());
        }
        return lignesCible;
    }

    private <E extends LigneTrancheOmran> E creer(Supplier<E> nouvelle, CfgTranche cible, TracePatrimoine.Trace t) {
        E ligne = nouvelle.get();
        ligne.setId(UUID.randomUUID());
        ligne.setCfgTranche(cible);
        ligne.setUserCreate(t.auteur());
        ligne.setUserCreateDateTime(t.le());
        em.persist(ligne);
        return ligne;
    }

    /** TTC recalculé depuis HT et TVA, comme l'onglet Frais (montant forfaitaire = prix d'une surface de 1). */
    private static void copierFrais(CfgTrancheSettlementCategory s, CfgTrancheSettlementCategory c) {
        BigDecimal tva = ou(s.getVatRatio(), BigDecimal.ZERO);
        PrixLot montants = CalculPrixArticle.depuisHT(BigDecimal.ONE, ou(s.getAmountHT(), BigDecimal.ZERO), tva);
        c.setComSettlementCategory(s.getComSettlementCategory());
        c.setSorting(s.getSorting());
        c.setAmountHT(montants.prixHT());
        c.setAmountTTC(montants.prixTTC());
        c.setVatRatio(tva);
        c.setPercentOf(s.getPercentOf());
        c.setNotes(s.getNotes());
    }
}

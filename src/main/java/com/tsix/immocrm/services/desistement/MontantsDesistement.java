package com.tsix.immocrm.services.desistement;

import com.tsix.immocrm.domain.CalculDesistement;
import com.tsix.immocrm.domain.MontantRestant;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSaleWithdrawalCancellation;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.utils.Champs;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.requireNonNullElse;

/**
 * Lecture du dossier pour {@link CalculDesistement} ({@code SaleWithdrawalCancellation.cs:RefrechAmounts}) : total réglé
 * = {@code Sale.PriceSettled} (avance + crédit + autofinancement), reste frais = frais « toujours payés » restant dus.
 */
@Component
public class MontantsDesistement {

    static final String NON_ELIGIBLE = "Le statut du dossier ne permet pas d'effectuer un désistement "
            + "(« Peut effectuer un désistement » non coché).";

    private final ComDocumentRepository documentRepository;
    private final StlSettlementLineRepository settlementLineRepository;

    public MontantsDesistement(ComDocumentRepository documentRepository,
                               StlSettlementLineRepository settlementLineRepository) {
        this.documentRepository = documentRepository;
        this.settlementLineRepository = settlementLineRepository;
    }

    /** R2 : 409 si le statut n'a pas {@code BoolField010}. {@code remise} null = celle de la tranche pour un ancien client. */
    public CalculDesistement.Entree lire(ComFolder folder, BigDecimal remise) {
        if (folder.getStrStatus() == null || !Boolean.TRUE.equals(folder.getStrStatus().getPeutDesister())) {
            throw new ConflictException(NON_ELIGIBLE);
        }
        CfgTranche tranche = Champs.de(folder.getStkItem(), StkItem::getStkHierarchy);
        Map<UUID, BigDecimal> regles = new HashMap<>();
        for (Object[] ligne : settlementLineRepository.sumAmountByComFolderIdGroupedByComDocument(folder.getId())) {
            regles.put((UUID) ligne[0], (BigDecimal) ligne[1]);
        }
        BigDecimal totalRegle = BigDecimal.ZERO;
        BigDecimal resteFrais = BigDecimal.ZERO;
        for (ComDocument document : documentRepository.findByComFolderIdOrderByDocDateAsc(folder.getId())) {
            ComSettlementCategory categorie = document.getComSettlementCategory();
            BigDecimal regle = regles.getOrDefault(document.getId(), BigDecimal.ZERO);
            if (categorie != null && compteDansLePrix(categorie)) {
                totalRegle = totalRegle.add(regle);
            }
            if (categorie != null && fraisToujoursPaye(categorie)) {
                resteFrais = resteFrais.add(MontantRestant.echeance(document.getAmount(), regle));
            }
        }
        BigDecimal remiseRetenue = remise != null ? remise : remiseParDefaut(folder, tranche);
        return new CalculDesistement.Entree(
                requireNonNullElse(Champs.de(tranche, CfgTranche::getCancellationPenalityPercent), BigDecimal.ZERO),
                Boolean.TRUE.equals(Champs.de(tranche, CfgTranche::getCancellationPenalityBasedOnItemPrice)),
                requireNonNullElse(folder.getPrice(), BigDecimal.ZERO), totalRegle, remiseRetenue, resteFrais);
    }

    /** Montants enregistrés sur le désistement : ceux de la liste et de la fiche. */
    public static void appliquer(ComSaleWithdrawalCancellation desistement, CalculDesistement.Entree e) {
        desistement.setTotalToPay(e.prix());
        desistement.setTotalSettled(e.totalRegle());
        desistement.setTotalRest(e.prix().subtract(e.totalRegle()));
        desistement.setNewStkItemTotalToPay(e.resteFrais());
        desistement.setTransfertExpenseAmount(e.taux());
        desistement.setDiscount(e.remise());
        desistement.setTransfertExpenseAmountAfterDiscount(CalculDesistement.penaliteApresRemise(e));
        desistement.setReimburseAmount(CalculDesistement.aRembourser(e));
    }

    private static BigDecimal remiseParDefaut(ComFolder folder, CfgTranche tranche) {
        if (!Boolean.TRUE.equals(folder.getIsOldCustomer())) {
            return BigDecimal.ZERO;
        }
        return requireNonNullElse(Champs.de(tranche, CfgTranche::getCancellationPenalityPercentDiscount), BigDecimal.ZERO);
    }

    private static boolean compteDansLePrix(ComSettlementCategory c) {
        return Boolean.TRUE.equals(c.getIsSaleAdvance()) || Boolean.TRUE.equals(c.getIsSaleCredit())
                || Boolean.TRUE.equals(c.getIsSaleAutoFinancment());
    }

    /** {@code SetTransfertItem} L1334 : toujours payé, au crédit, et frais, pénalité ou modification. */
    private static boolean fraisToujoursPaye(ComSettlementCategory c) {
        return Boolean.TRUE.equals(c.getIsAlwaysPayed()) && Boolean.TRUE.equals(c.getIsCredit())
                && (Boolean.TRUE.equals(c.getIsSaleExpense()) || Boolean.TRUE.equals(c.getIsSalePenality())
                || Boolean.TRUE.equals(c.getIsSaleModification()));
    }
}

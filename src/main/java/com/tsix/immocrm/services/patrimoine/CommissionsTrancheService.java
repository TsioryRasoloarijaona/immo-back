package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.domain.ChevauchementCommissions;
import com.tsix.immocrm.domain.ChevauchementCommissions.Periode;
import com.tsix.immocrm.dto.patrimoine.BlocCommissionDto;
import com.tsix.immocrm.dto.patrimoine.LigneCommissionDto;
import com.tsix.immocrm.dto.patrimoine.LignesTrancheDto;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.cfg.ComSalesCommission;
import com.tsix.immocrm.entities.com.ComAction;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.cfg.TrancheVisibleRepository;
import com.tsix.immocrm.services.patrimoine.ReconciliationLignes.Onglet;
import com.tsix.immocrm.services.patrimoine.ReconciliationLignes.Position;
import com.tsix.immocrm.utils.Champs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.fraction;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.ou;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.positif;

/** Onglet Commission vendeur de la tranche (FEATURE-patrimoine-tranche-regles.md §2) : paramétrage seulement. */
@Service
@Transactional(readOnly = true)
public class CommissionsTrancheService {

    private static final String LIGNES = "SELECT l FROM ComSalesCommission l WHERE l.cfgTranche.id = :id "
            + "ORDER BY l.startDate, l.id";
    private static final Onglet<ComSalesCommission, LigneCommissionDto> ONGLET = new Onglet<>(LIGNES,
            ComSalesCommission.class, ComSalesCommission::new, null, null, false);
    private static final String CHEVAUCHEMENT = "Vous devez vérifier les dates qui sont paramétrées pour les commissions.\n"
            + " On ne doit pas avoir un chevauchement des périodes pour deux lignes de paramétrage identiques";

    /** Clé de chevauchement de {@code CfgTranche.Verif} : deux lignes « identiques » ont la même. */
    private record Cle(UUID personnel, UUID action, UUID commercial, UUID categorie) {
    }

    private final TrancheVisibleRepository tranches;
    private final ReconciliationLignes reconciliation;

    public CommissionsTrancheService(TrancheVisibleRepository tranches, ReconciliationLignes reconciliation) {
        this.tranches = tranches;
        this.reconciliation = reconciliation;
    }

    public LignesTrancheDto<LigneCommissionDto> commissions(UUID trancheId) {
        return lire(tranches.exigerVisible(trancheId));
    }

    @Transactional
    public LignesTrancheDto<LigneCommissionDto> enregistrerCommissions(UUID trancheId,
                                                                       LignesTrancheDto<LigneCommissionDto> corps) {
        CfgTranche tranche = tranches.exigerVisible(trancheId);
        verifier(ReconciliationLignes.lignesRecues(corps));
        reconciliation.reconcilier(tranche, corps, ONGLET, this::remplir);
        return lire(tranche);
    }

    /** {@code SalesCommission.Verif} ligne par ligne, puis {@code CfgTranche.Verif} sur la liste entière. */
    private static void verifier(List<LigneCommissionDto> lignes) {
        for (int i = 0; i < lignes.size(); i++) {
            LigneCommissionDto l = lignes.get(i);
            if (l.dateDebut() != null && l.dateFin() != null && l.dateDebut().isAfter(l.dateFin())) {
                throw new InvalidRequestException(new Position(i, 0).champ("dateFin")
                        + " : La Date début doit être < à la Date fin!");
            }
        }
        List<Periode> periodes = lignes.stream().map(l -> new Periode(new Cle(l.personnelId(), l.actionCommercialeId(),
                l.commercialId(), l.categorieArticleId()), l.dateDebut(), l.dateFin())).toList();
        if (ChevauchementCommissions.present(periodes)) {
            throw new InvalidRequestException(CHEVAUCHEMENT);
        }
    }

    private void remplir(ComSalesCommission e, LigneCommissionDto l, Position p) {
        e.setStartDate(l.dateDebut());
        e.setEndDate(l.dateFin());
        e.setCfgTier(reconciliation.referenceOuNull(CfgTier.class, l.commercialId(), p.champ("commercialId")));
        e.setComEmployee(reconciliation.referenceOuNull(CfgTier.class, l.personnelId(), p.champ("personnelId")));
        e.setStkItemCategory(reconciliation.referenceOuNull(StkItemCategory.class, l.categorieArticleId(),
                p.champ("categorieArticleId")));
        e.setComAction(reconciliation.referenceOuNull(ComAction.class, l.actionCommercialeId(),
                p.champ("actionCommercialeId")));
        bloc(l.concretisation(), p.champ("concretisation"), e::setSaleCommissionBasedOnAmount,
                e::setSaleCommissionPercent, e::setSaleCommissionAmount);
        bloc(l.concretisationExterne(), p.champ("concretisationExterne"), e::setSaleCommissionExternBasedOnAmount,
                e::setSaleCommissionPercentExtern, e::setSaleCommissionAmountExtern);
        bloc(l.finalisation1(), p.champ("finalisation1"), e::setDeliveryCommissionBasedOnAmount,
                e::setDeliveryCommissionPercent, e::setDeliveryCommissionAmount);
        bloc(l.finalisation1Externe(), p.champ("finalisation1Externe"), e::setDeliveryCommissionExternBasedOnAmount,
                e::setDeliveryCommissionPercentExtern, e::setDeliveryCommissionAmountExtern);
        bloc(l.finalisation2(), p.champ("finalisation2"), e::setDeliveryCommission2BasedOnAmount,
                e::setDeliveryCommissionPercent2, e::setDeliveryCommissionAmount2);
        bloc(l.finalisation2Externe(), p.champ("finalisation2Externe"), e::setDeliveryCommissionExtern2BasedOnAmount,
                e::setDeliveryCommissionPercentExtern2, e::setDeliveryCommissionAmountExtern2);
    }

    /** Bloc absent = base sur le montant, 0 % et 0 DH (défauts de {@code SalesCommission.cs}). */
    private static void bloc(BlocCommissionDto b, String nom, Consumer<Boolean> base, Consumer<BigDecimal> pourcentage,
                             Consumer<BigDecimal> montant) {
        BlocCommissionDto saisi = ou(b, new BlocCommissionDto(null, null, null));
        base.accept(ou(saisi.baseSurMontant(), Boolean.TRUE));
        pourcentage.accept(ou(fraction(saisi.pourcentage(), nom + ".pourcentage"), BigDecimal.ZERO));
        montant.accept(ou(positif(saisi.montant(), nom + ".montant"), BigDecimal.ZERO));
    }

    private LignesTrancheDto<LigneCommissionDto> lire(CfgTranche tranche) {
        return new LignesTrancheDto<>(reconciliation.lignes(tranche, LIGNES, ComSalesCommission.class).stream()
                .map(CommissionsTrancheService::dto).toList());
    }

    private static LigneCommissionDto dto(ComSalesCommission l) {
        return new LigneCommissionDto(l.getId(), l.getStartDate(), l.getEndDate(), Champs.de(l.getCfgTier(), CfgTier::getId),
                Champs.de(l.getStkItemCategory(), StkItemCategory::getId), Champs.de(l.getComEmployee(), CfgTier::getId),
                Champs.de(l.getComAction(), ComAction::getId),
                new BlocCommissionDto(l.getSaleCommissionBasedOnAmount(), l.getSaleCommissionPercent(),
                        l.getSaleCommissionAmount()),
                new BlocCommissionDto(l.getSaleCommissionExternBasedOnAmount(), l.getSaleCommissionPercentExtern(),
                        l.getSaleCommissionAmountExtern()),
                new BlocCommissionDto(l.getDeliveryCommissionBasedOnAmount(), l.getDeliveryCommissionPercent(),
                        l.getDeliveryCommissionAmount()),
                new BlocCommissionDto(l.getDeliveryCommissionExternBasedOnAmount(), l.getDeliveryCommissionPercentExtern(),
                        l.getDeliveryCommissionAmountExtern()),
                new BlocCommissionDto(l.getDeliveryCommission2BasedOnAmount(), l.getDeliveryCommissionPercent2(),
                        l.getDeliveryCommissionAmount2()),
                new BlocCommissionDto(l.getDeliveryCommissionExtern2BasedOnAmount(),
                        l.getDeliveryCommissionPercentExtern2(), l.getDeliveryCommissionAmountExtern2()));
    }
}

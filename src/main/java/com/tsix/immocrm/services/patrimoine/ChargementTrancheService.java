package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.domain.CalculFraisVente;
import com.tsix.immocrm.dto.patrimoine.TrancheDetailDto;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.cfg.CfgTrancheCoveredFloor;
import com.tsix.immocrm.entities.cfg.CfgTrancheLevel;
import com.tsix.immocrm.entities.cfg.CfgTrancheSettlementCategory;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.lnd.LndLevel;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.exceptions.ChampInvalideException;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.utils.Champs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * « Charger à partir d'une autre tranche » (FEATURE-patrimoine-tranche-onglets.md §3), une transaction : paramètres
 * recopiés ({@link CopieParametresTranche}), étages / plancher couvert / frais fusionnés par clé (absente de la cible =
 * ajoutée, présente = valeurs de la source), plancher couvert recalculé. Ni terrains, ni commissions, ni seuils.
 */
@Service
public class ChargementTrancheService {

    private final TrancheVisible trancheVisible;
    private final CfgTrancheRepository trancheRepository;
    private final EnfantsTranche enfants;
    private final SignatureLigne signatureLigne;

    public ChargementTrancheService(TrancheVisible trancheVisible, CfgTrancheRepository trancheRepository,
                                    EnfantsTranche enfants, SignatureLigne signatureLigne) {
        this.trancheVisible = trancheVisible;
        this.trancheRepository = trancheRepository;
        this.enfants = enfants;
        this.signatureLigne = signatureLigne;
    }

    @Transactional
    public TrancheDetailDto modifierDepuisTranche(UUID cibleId, UUID sourceId) {
        if (cibleId.equals(sourceId)) {
            throw new ChampInvalideException("sourceId", "Choisir une autre tranche que la tranche en cours.");
        }
        CfgTranche cible = trancheVisible.trouver(cibleId);
        CfgTranche source = trancheVisible.trouver(sourceId);
        SignatureLigne.Signature signature = signatureLigne.courante();

        CopieParametresTranche.copier(source, cible);
        fusionnerEtages(source, cible, signature);
        List<CfgTrancheCoveredFloor> planchers = fusionnerPlanchers(source, cible, signature);
        fusionnerFrais(source, cible, signature);

        cible.setCoveredFloorArea(PlancherCouvertService.total(planchers));
        cible.setUserUpdate(signature.nom());
        cible.setUserUpdateDateTime(signature.instant());
        return VueTranche.fiche(trancheRepository.save(cible));
    }

    private void fusionnerEtages(CfgTranche source, CfgTranche cible, SignatureLigne.Signature signature) {
        List<CfgTrancheLevel> existants = new ArrayList<>(enfants.etages().findByCfgTrancheId(cible.getId()));
        for (CfgTrancheLevel origine : enfants.etages().findByCfgTrancheId(source.getId())) {
            UUID cle = Champs.de(origine.getLndLevel(), LndLevel::getId);
            CfgTrancheLevel ligne = trouverOuCreer(existants, e -> Champs.de(e.getLndLevel(), LndLevel::getId), cle,
                    () -> {
                        CfgTrancheLevel nouveau = new CfgTrancheLevel();
                        nouveau.setId(UUID.randomUUID());
                        nouveau.setCfgTranche(cible);
                        nouveau.setUserCreate(signature.nom());
                        nouveau.setUserCreateDateTime(signature.instant());
                        return nouveau;
                    });
            ligne.setLndLevel(origine.getLndLevel());
            ligne.setUserUpdate(signature.nom());
            ligne.setUserUpdateDateTime(signature.instant());
        }
        enfants.etages().saveAll(existants);
    }

    private List<CfgTrancheCoveredFloor> fusionnerPlanchers(CfgTranche source, CfgTranche cible,
                                                           SignatureLigne.Signature signature) {
        List<CfgTrancheCoveredFloor> existants = new ArrayList<>(enfants.planchers().findByCfgTrancheId(cible.getId()));
        for (CfgTrancheCoveredFloor origine : enfants.planchers().findByCfgTrancheId(source.getId())) {
            UUID cle = Champs.de(origine.getStkItemCategory(), StkItemCategory::getId);
            CfgTrancheCoveredFloor ligne = trouverOuCreer(existants,
                    e -> Champs.de(e.getStkItemCategory(), StkItemCategory::getId), cle, () -> {
                        CfgTrancheCoveredFloor nouveau = new CfgTrancheCoveredFloor();
                        nouveau.setId(UUID.randomUUID());
                        nouveau.setCfgTranche(cible);
                        nouveau.setUserCreate(signature.nom());
                        nouveau.setUserCreateDateTime(signature.instant());
                        return nouveau;
                    });
            ligne.setStkItemCategory(origine.getStkItemCategory());
            ligne.setArea(origine.getArea());
            ligne.setUserUpdate(signature.nom());
            ligne.setUserUpdateDateTime(signature.instant());
        }
        return enfants.planchers().saveAll(existants);
    }

    private void fusionnerFrais(CfgTranche source, CfgTranche cible, SignatureLigne.Signature signature) {
        List<CfgTrancheSettlementCategory> existants =
                new ArrayList<>(enfants.frais().findByCfgTrancheIdOrderBySortingAsc(cible.getId()));
        for (CfgTrancheSettlementCategory origine
                : enfants.frais().findByCfgTrancheIdOrderBySortingAsc(source.getId())) {
            UUID cle = Champs.de(origine.getComSettlementCategory(), ComSettlementCategory::getId);
            CfgTrancheSettlementCategory ligne = trouverOuCreer(existants,
                    e -> Champs.de(e.getComSettlementCategory(), ComSettlementCategory::getId), cle, () -> {
                        CfgTrancheSettlementCategory nouveau = new CfgTrancheSettlementCategory();
                        nouveau.setId(UUID.randomUUID());
                        nouveau.setCfgTranche(cible);
                        nouveau.setUserCreate(signature.nom());
                        nouveau.setUserCreateDateTime(signature.instant());
                        return nouveau;
                    });
            CalculFraisVente.Montants montants =
                    CalculFraisVente.depuisSaisie(origine.getAmountHT(), origine.getVatRatio(), false);
            ligne.setComSettlementCategory(origine.getComSettlementCategory());
            ligne.setSorting(origine.getSorting());
            ligne.setVatRatio(origine.getVatRatio());
            ligne.setAmountHT(montants.ht());
            ligne.setAmountTTC(montants.ttc());
            ligne.setPercentOf(origine.getPercentOf());
            ligne.setUserUpdate(signature.nom());
            ligne.setUserUpdateDateTime(signature.instant());
        }
        enfants.frais().saveAll(existants);
    }

    /** Ligne de la cible de même clé (NULL = NULL, comme OMRAN), sinon une nouvelle ajoutée à {@code lignes}. */
    private static <E> E trouverOuCreer(List<E> lignes, Function<E, UUID> cleDe, UUID cle, Supplier<E> nouvelle) {
        Optional<E> existante = lignes.stream().filter(e -> Objects.equals(cleDe.apply(e), cle)).findFirst();
        return existante.orElseGet(() -> {
            E creee = nouvelle.get();
            lignes.add(creee);
            return creee;
        });
    }
}

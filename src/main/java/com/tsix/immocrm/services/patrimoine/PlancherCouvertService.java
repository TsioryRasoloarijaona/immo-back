package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.domain.ClassementArticle;
import com.tsix.immocrm.dto.patrimoine.PlancherCouvertDto;
import com.tsix.immocrm.dto.patrimoine.PlancherCouvertEcritureDto;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.cfg.CfgTrancheCoveredFloor;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.exceptions.ChampInvalideException;
import com.tsix.immocrm.repositories.cfg.CfgTrancheCoveredFloorRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.utils.Champs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Onglet Plancher couvert d'une tranche, {@code CfgTrancheCoveredFloor} (FEATURE-patrimoine-tranche-onglets.md §2.3) :
 * chaque enregistrement réécrit {@code CfgTranche.CoveredFloorArea} = Σ des superficies, dans la même transaction.
 */
@Service
@Transactional(readOnly = true)
public class PlancherCouvertService {

    private static final String CHAMP_CATEGORIE = "categorieId";
    private static final Comparator<PlancherCouvertDto> PAR_CODE =
            Comparator.comparing(p -> Objects.toString(p.categorieCode(), ""));

    private final TrancheVisible trancheVisible;
    private final CfgTrancheCoveredFloorRepository plancherRepository;
    private final CfgTrancheRepository trancheRepository;
    private final StkItemCategoryRepository categorieRepository;
    private final SignatureLigne signatureLigne;

    public PlancherCouvertService(TrancheVisible trancheVisible, CfgTrancheCoveredFloorRepository plancherRepository,
                                  CfgTrancheRepository trancheRepository,
                                  StkItemCategoryRepository categorieRepository, SignatureLigne signatureLigne) {
        this.trancheVisible = trancheVisible;
        this.plancherRepository = plancherRepository;
        this.trancheRepository = trancheRepository;
        this.categorieRepository = categorieRepository;
        this.signatureLigne = signatureLigne;
    }

    /** Seul calcul du plancher couvert d'une tranche (« ∑ des superficies des catégories »). */
    static BigDecimal total(List<CfgTrancheCoveredFloor> lignes) {
        return lignes.stream().map(CfgTrancheCoveredFloor::getArea).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<PlancherCouvertDto> lister(UUID trancheId) {
        trancheVisible.trouver(trancheId);
        return versDtos(plancherRepository.findByCfgTrancheId(trancheId));
    }

    @Transactional
    public List<PlancherCouvertDto> enregistrerPlancher(UUID trancheId, List<PlancherCouvertEcritureDto> lignes) {
        CfgTranche tranche = trancheVisible.trouver(trancheId);
        SignatureLigne.Signature signature = signatureLigne.courante();
        ReconciliationLignes.Resultat<CfgTrancheCoveredFloor> resultat = ReconciliationLignes.reconcilier(
                plancherRepository.findByCfgTrancheId(trancheId), CfgTrancheCoveredFloor::getId, lignes,
                new ReconciliationLignes.Onglet<PlancherCouvertEcritureDto, CfgTrancheCoveredFloor>() {
                    @Override
                    public UUID idLigne(PlancherCouvertEcritureDto ligne) {
                        return ligne.id();
                    }

                    @Override
                    public Object cle(PlancherCouvertEcritureDto ligne, String prefixe) {
                        return ReglesSaisie.requis(ligne.categorieId(), prefixe + CHAMP_CATEGORIE, "Catégorie");
                    }

                    @Override
                    public CfgTrancheCoveredFloor nouvelle() {
                        CfgTrancheCoveredFloor plancher = new CfgTrancheCoveredFloor();
                        plancher.setId(UUID.randomUUID());
                        plancher.setCfgTranche(tranche);
                        plancher.setUserCreate(signature.nom());
                        plancher.setUserCreateDateTime(signature.instant());
                        return plancher;
                    }

                    @Override
                    public void appliquer(CfgTrancheCoveredFloor plancher, PlancherCouvertEcritureDto ligne, int rang,
                                          String prefixe) {
                        StkItemCategory actuelle = plancher.getStkItemCategory();
                        if (actuelle == null || !actuelle.getId().equals(ligne.categorieId())) {
                            plancher.setStkItemCategory(categorie(ligne.categorieId(), prefixe + CHAMP_CATEGORIE));
                        }
                        BigDecimal surface = ReglesSaisie.positifOuNul(ligne.surface(), prefixe + "surface");
                        plancher.setArea(surface == null ? BigDecimal.ZERO : surface);
                        plancher.setUserUpdate(signature.nom());
                        plancher.setUserUpdateDateTime(signature.instant());
                    }
                });
        plancherRepository.deleteAll(resultat.supprimees());
        List<CfgTrancheCoveredFloor> gardees = plancherRepository.saveAll(resultat.gardees());
        tranche.setCoveredFloorArea(total(gardees));
        tranche.setUserUpdate(signature.nom());
        tranche.setUserUpdateDateTime(signature.instant());
        trancheRepository.save(tranche);
        return versDtos(gardees);
    }

    private StkItemCategory categorie(UUID id, String champ) {
        StkItemCategory categorie = categorieRepository.findById(id)
                .orElseThrow(() -> new ChampInvalideException(champ, "Catégorie introuvable : " + id));
        if (!ClassementArticle.categorieBatie(Champs.de(categorie.getStkKind(), StkKind::getCode),
                categorie.getForUnderItem())) {
            throw new ChampInvalideException(champ, "La catégorie « %s » n'est pas une catégorie d'article bâti."
                    .formatted(categorie.getCode()));
        }
        return categorie;
    }

    private static List<PlancherCouvertDto> versDtos(List<CfgTrancheCoveredFloor> lignes) {
        return lignes.stream().map(PlancherCouvertService::versDto).sorted(PAR_CODE).toList();
    }

    private static PlancherCouvertDto versDto(CfgTrancheCoveredFloor plancher) {
        StkItemCategory categorie = plancher.getStkItemCategory();
        return categorie == null
                ? new PlancherCouvertDto(plancher.getId(), null, null, null, plancher.getArea())
                : new PlancherCouvertDto(plancher.getId(), categorie.getId(), categorie.getCode(),
                categorie.getDescription(), plancher.getArea());
    }
}

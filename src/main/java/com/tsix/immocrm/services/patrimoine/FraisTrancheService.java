package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.domain.CalculFraisVente;
import com.tsix.immocrm.dto.patrimoine.FraisTrancheDto;
import com.tsix.immocrm.dto.patrimoine.FraisTrancheEcritureDto;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.cfg.CfgTrancheSettlementCategory;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.exceptions.ChampInvalideException;
import com.tsix.immocrm.repositories.cfg.CfgTrancheSettlementCategoryRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import com.tsix.immocrm.services.referentiel.ReferencesOmran;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Onglet Frais (frais de vente) d'une tranche, {@code CfgTrancheSettlementCategory} (§2.4 de la spec P1b). */
@Service
@Transactional(readOnly = true)
public class FraisTrancheService {

    private static final String CHAMP_CATEGORIE = "categorieReglementId";
    private static final Comparator<CfgTrancheSettlementCategory> PAR_ORDRE = Comparator.comparing(
            CfgTrancheSettlementCategory::getSorting, Comparator.nullsLast(Comparator.naturalOrder()));

    private final TrancheVisible trancheVisible;
    private final CfgTrancheSettlementCategoryRepository fraisRepository;
    private final ComSettlementCategoryRepository categorieRepository;
    private final ReferencesOmran referencesOmran;
    private final SignatureLigne signatureLigne;

    public FraisTrancheService(TrancheVisible trancheVisible, CfgTrancheSettlementCategoryRepository fraisRepository,
                               ComSettlementCategoryRepository categorieRepository, ReferencesOmran referencesOmran,
                               SignatureLigne signatureLigne) {
        this.trancheVisible = trancheVisible;
        this.fraisRepository = fraisRepository;
        this.categorieRepository = categorieRepository;
        this.referencesOmran = referencesOmran;
        this.signatureLigne = signatureLigne;
    }

    /** Liste de choix : frais de vente moins les trois catégories que la vente pose elle-même. */
    public List<ReferentielItemDto> categories() {
        return eligibles().stream()
                .map(c -> new ReferentielItemDto(c.getId(), c.getCode(), c.getDescription(), null))
                .toList();
    }

    public List<FraisTrancheDto> lister(UUID trancheId) {
        trancheVisible.trouver(trancheId);
        return versDtos(fraisRepository.findByCfgTrancheIdOrderBySortingAsc(trancheId));
    }

    @Transactional
    public List<FraisTrancheDto> enregistrerFrais(UUID trancheId, List<FraisTrancheEcritureDto> lignes) {
        CfgTranche tranche = trancheVisible.trouver(trancheId);
        SignatureLigne.Signature signature = signatureLigne.courante();
        Set<UUID> eligibles = eligibles().stream().map(ComSettlementCategory::getId)
                .collect(Collectors.toUnmodifiableSet());
        ReconciliationLignes.Resultat<CfgTrancheSettlementCategory> resultat = ReconciliationLignes.reconcilier(
                fraisRepository.findByCfgTrancheIdOrderBySortingAsc(trancheId), CfgTrancheSettlementCategory::getId,
                lignes, new ReconciliationLignes.Onglet<FraisTrancheEcritureDto, CfgTrancheSettlementCategory>() {
                    @Override
                    public UUID idLigne(FraisTrancheEcritureDto ligne) {
                        return ligne.id();
                    }

                    @Override
                    public Object cle(FraisTrancheEcritureDto ligne, String prefixe) {
                        return ReglesSaisie.requis(ligne.categorieReglementId(), prefixe + CHAMP_CATEGORIE,
                                "Frais");
                    }

                    @Override
                    public CfgTrancheSettlementCategory nouvelle() {
                        CfgTrancheSettlementCategory frais = new CfgTrancheSettlementCategory();
                        frais.setId(UUID.randomUUID());
                        frais.setCfgTranche(tranche);
                        frais.setUserCreate(signature.nom());
                        frais.setUserCreateDateTime(signature.instant());
                        return frais;
                    }

                    @Override
                    public void appliquer(CfgTrancheSettlementCategory frais, FraisTrancheEcritureDto ligne, int rang,
                                          String prefixe) {
                        ComSettlementCategory actuelle = frais.getComSettlementCategory();
                        if (actuelle == null || !actuelle.getId().equals(ligne.categorieReglementId())) {
                            frais.setComSettlementCategory(
                                    categorie(ligne.categorieReglementId(), eligibles, prefixe + CHAMP_CATEGORIE));
                        }
                        appliquerMontants(frais, ligne, rang, prefixe);
                        frais.setUserUpdate(signature.nom());
                        frais.setUserUpdateDateTime(signature.instant());
                    }
                });
        fraisRepository.deleteAll(resultat.supprimees());
        return versDtos(fraisRepository.saveAll(resultat.gardees()));
    }

    private static void appliquerMontants(CfgTrancheSettlementCategory frais, FraisTrancheEcritureDto ligne, int rang,
                                          String prefixe) {
        Integer ordre = ReglesSaisie.positifOuNul(ligne.ordre(), prefixe + "ordre");
        BigDecimal tva = zero(ReglesSaisie.fraction(ligne.tauxTva(), prefixe + "tauxTva"));
        boolean saisiEnTtc = Boolean.TRUE.equals(ligne.saisiEnTTC());
        BigDecimal saisi = saisiEnTtc
                ? ReglesSaisie.positifOuNul(ligne.montantTTC(), prefixe + "montantTTC")
                : ReglesSaisie.positifOuNul(ligne.montantHT(), prefixe + "montantHT");
        CalculFraisVente.Montants montants = CalculFraisVente.depuisSaisie(saisi, tva, saisiEnTtc);
        frais.setSorting(ordre == null ? rang : ordre);
        frais.setVatRatio(tva);
        frais.setAmountHT(montants.ht());
        frais.setAmountTTC(montants.ttc());
        frais.setPercentOf(zero(ReglesSaisie.positifOuNul(ligne.pourcentageDe(), prefixe + "pourcentageDe")));
    }

    private List<ComSettlementCategory> eligibles() {
        Set<UUID> exclues = referencesOmran.categoriesFraisAutomatiques();
        return categorieRepository.findByIsCreditTrueAndIsSaleExpenseTrueOrderByCodeAsc().stream()
                .filter(c -> !exclues.contains(c.getId()))
                .toList();
    }

    private ComSettlementCategory categorie(UUID id, Set<UUID> eligibles, String champ) {
        if (!eligibles.contains(id)) {
            throw new ChampInvalideException(champ, "Cette catégorie n'est pas un frais de vente proposé : " + id);
        }
        return categorieRepository.findById(id)
                .orElseThrow(() -> new ChampInvalideException(champ, "Catégorie introuvable : " + id));
    }

    private static BigDecimal zero(BigDecimal valeur) {
        return valeur == null ? BigDecimal.ZERO : valeur;
    }

    private static List<FraisTrancheDto> versDtos(List<CfgTrancheSettlementCategory> lignes) {
        return lignes.stream().sorted(PAR_ORDRE).map(FraisTrancheService::versDto).toList();
    }

    private static FraisTrancheDto versDto(CfgTrancheSettlementCategory frais) {
        ComSettlementCategory categorie = frais.getComSettlementCategory();
        return new FraisTrancheDto(frais.getId(), categorie == null ? null : categorie.getId(),
                categorie == null ? null : categorie.getCode(), categorie == null ? null : categorie.getDescription(),
                frais.getSorting(), frais.getAmountHT(), frais.getVatRatio(), frais.getAmountTTC(),
                frais.getPercentOf());
    }
}

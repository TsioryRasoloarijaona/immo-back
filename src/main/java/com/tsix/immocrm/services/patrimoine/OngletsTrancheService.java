package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.domain.CalculPrixArticle;
import com.tsix.immocrm.domain.CalculPrixArticle.PrixLot;
import com.tsix.immocrm.dto.patrimoine.LigneEtageDto;
import com.tsix.immocrm.dto.patrimoine.LigneFraisDto;
import com.tsix.immocrm.dto.patrimoine.LignePlancherDto;
import com.tsix.immocrm.dto.patrimoine.LigneTerrainDto;
import com.tsix.immocrm.dto.patrimoine.LignesTrancheDto;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.entities.adm.AdmParamsGuid;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.cfg.CfgTrancheCoveredFloor;
import com.tsix.immocrm.entities.cfg.CfgTrancheLevel;
import com.tsix.immocrm.entities.cfg.CfgTrancheSettlementCategory;
import com.tsix.immocrm.entities.cfg.CfgTrancheStkItem;
import com.tsix.immocrm.entities.cfg.LigneTrancheOmran;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.lnd.LndLevel;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmParamsGuidRepository;
import com.tsix.immocrm.repositories.cfg.TrancheVisibleRepository;
import com.tsix.immocrm.utils.Champs;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.fraction;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.ou;
import static com.tsix.immocrm.services.patrimoine.SaisiePatrimoine.positif;

/**
 * Onglets Terrains, Étages, Plancher couvert et Frais de la fiche tranche (FEATURE-patrimoine-tranche-onglets.md §1, §2)
 * et suppression de la tranche (§4). Chaque PUT reçoit la liste complète et la réconcilie dans une transaction.
 */
@Service
@Transactional(readOnly = true)
public class OngletsTrancheService {

    private static final String ID = "id";
    private static final String TERRAINS = "SELECT l FROM CfgTrancheStkItem l WHERE l.cfgTranche.id = :id ORDER BY l.sorting";
    private static final String ETAGES = "SELECT l FROM CfgTrancheLevel l LEFT JOIN l.lndLevel n "
            + "WHERE l.cfgTranche.id = :id ORDER BY n.code";
    private static final String PLANCHERS = "SELECT l FROM CfgTrancheCoveredFloor l LEFT JOIN l.stkItemCategory c "
            + "WHERE l.cfgTranche.id = :id ORDER BY c.code";
    private static final String FRAIS = "SELECT l FROM CfgTrancheSettlementCategory l WHERE l.cfgTranche.id = :id "
            + "ORDER BY l.sorting";
    private static final String TERRAINS_DISPONIBLES = "SELECT i FROM StkItem i "
            + "WHERE i.stkKind.code = 'TER' AND i.stkVocation.code = 'PROJET' ORDER BY i.code";
    private static final String CATEGORIES_FRAIS = "SELECT c FROM ComSettlementCategory c "
            + "WHERE c.isCredit = true AND c.isSaleExpense = true ORDER BY c.code";
    /** Catégories par défaut d'OMRAN retirées du choix des frais (paramètres {@code AdmParamsGuid}). */
    private static final List<String> CATEGORIES_PAR_DEFAUT = List.of("DefaultCustomerTransfertDocumentType",
            "DefaultProductTransfertDocumentType", "DefaultCancellationDocumentType");

    private final TrancheVisibleRepository tranches;
    private final AdmParamsGuidRepository parametres;
    private final SuppressionProjet suppression;
    private final TracePatrimoine trace;
    private final EntityManager em;

    public OngletsTrancheService(TrancheVisibleRepository tranches, AdmParamsGuidRepository parametres,
                                 SuppressionProjet suppression, TracePatrimoine trace, EntityManager em) {
        this.tranches = tranches;
        this.parametres = parametres;
        this.suppression = suppression;
        this.trace = trace;
        this.em = em;
    }

    /** Ligne de la liste reçue : son nom dans un message 400 et son rang parmi les lignes gardées. */
    private record Position(int index, int rang) {
        String champ(String nom) {
            return "lignes[%d].%s".formatted(index, nom);
        }
    }

    @FunctionalInterface
    private interface Remplir<E, L> {
        void appliquer(E entite, L ligne, Position position);
    }

    // ------------------------------------------------------------------ lecture

    public LignesTrancheDto<LigneTerrainDto> terrains(UUID trancheId) {
        return lire(visible(trancheId), TERRAINS, CfgTrancheStkItem.class, OngletsTrancheService::terrain);
    }

    public LignesTrancheDto<LigneEtageDto> etages(UUID trancheId) {
        return lire(visible(trancheId), ETAGES, CfgTrancheLevel.class, OngletsTrancheService::etage);
    }

    public LignesTrancheDto<LignePlancherDto> plancherCouvert(UUID trancheId) {
        return lire(visible(trancheId), PLANCHERS, CfgTrancheCoveredFloor.class, OngletsTrancheService::plancher);
    }

    public LignesTrancheDto<LigneFraisDto> frais(UUID trancheId) {
        return lire(visible(trancheId), FRAIS, CfgTrancheSettlementCategory.class, OngletsTrancheService::frais);
    }

    public List<ReferentielItemDto> terrainsDisponibles() {
        return em.createQuery(TERRAINS_DISPONIBLES, StkItem.class).getResultList().stream()
                .map(i -> new ReferentielItemDto(i.getId(), i.getCode(), i.getDescription(), null)).toList();
    }

    public List<ReferentielItemDto> categoriesFrais() {
        Set<UUID> parDefaut = CATEGORIES_PAR_DEFAUT.stream().map(parametres::findById)
                .flatMap(p -> p.map(AdmParamsGuid::getCodeValue).stream()).collect(Collectors.toSet());
        return em.createQuery(CATEGORIES_FRAIS, ComSettlementCategory.class).getResultList().stream()
                .filter(c -> !parDefaut.contains(c.getId()))
                .map(c -> new ReferentielItemDto(c.getId(), c.getCode(), c.getDescription(), null)).toList();
    }

    // ------------------------------------------------------------------ écriture

    @Transactional
    public LignesTrancheDto<LigneTerrainDto> enregistrerTerrains(UUID trancheId, LignesTrancheDto<LigneTerrainDto> corps) {
        CfgTranche tranche = visible(trancheId);
        reconcilier(tranche, corps, TERRAINS, CfgTrancheStkItem.class, CfgTrancheStkItem::new,
                LigneTerrainDto::terrainId, "terrainId", (e, l, p) -> {
                    e.setStkItem(reference(StkItem.class, l.terrainId(), p.champ("terrainId")));
                    e.setRatio(ou(positif(l.ratio(), p.champ("ratio")), BigDecimal.ONE));
                    e.setSorting(p.rang());
                });
        return lire(tranche, TERRAINS, CfgTrancheStkItem.class, OngletsTrancheService::terrain);
    }

    @Transactional
    public LignesTrancheDto<LigneEtageDto> enregistrerEtages(UUID trancheId, LignesTrancheDto<LigneEtageDto> corps) {
        CfgTranche tranche = visible(trancheId);
        reconcilier(tranche, corps, ETAGES, CfgTrancheLevel.class, CfgTrancheLevel::new,
                LigneEtageDto::niveauTerrainId, "niveauTerrainId",
                (e, l, p) -> e.setLndLevel(reference(LndLevel.class, l.niveauTerrainId(), p.champ("niveauTerrainId"))));
        return lire(tranche, ETAGES, CfgTrancheLevel.class, OngletsTrancheService::etage);
    }

    /** Σ des surfaces gardées → {@code CfgTranche.CoveredFloorArea}, dans la même transaction (§2.3). */
    @Transactional
    public LignesTrancheDto<LignePlancherDto> enregistrerPlancherCouvert(UUID trancheId,
                                                                        LignesTrancheDto<LignePlancherDto> corps) {
        CfgTranche tranche = visible(trancheId);
        List<CfgTrancheCoveredFloor> gardees = reconcilier(tranche, corps, PLANCHERS, CfgTrancheCoveredFloor.class,
                CfgTrancheCoveredFloor::new, LignePlancherDto::categorieId, "categorieId", (e, l, p) -> {
                    e.setStkItemCategory(reference(StkItemCategory.class, l.categorieId(), p.champ("categorieId")));
                    e.setArea(ou(positif(l.surface(), p.champ("surface")), BigDecimal.ZERO));
                });
        tranche.setCoveredFloorArea(gardees.stream().map(CfgTrancheCoveredFloor::getArea)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(6, RoundingMode.HALF_UP));
        return lire(tranche, PLANCHERS, CfgTrancheCoveredFloor.class, OngletsTrancheService::plancher);
    }

    /** Le montant saisi (HT, ou TTC si {@code saisiEnTTC}) donne l'autre par {@link CalculPrixArticle} (§2.4). */
    @Transactional
    public LignesTrancheDto<LigneFraisDto> enregistrerFrais(UUID trancheId, LignesTrancheDto<LigneFraisDto> corps) {
        CfgTranche tranche = visible(trancheId);
        reconcilier(tranche, corps, FRAIS, CfgTrancheSettlementCategory.class, CfgTrancheSettlementCategory::new,
                LigneFraisDto::categorieReglementId, "categorieReglementId", (e, l, p) -> {
                    e.setComSettlementCategory(reference(ComSettlementCategory.class, l.categorieReglementId(),
                            p.champ("categorieReglementId")));
                    BigDecimal tva = ou(fraction(l.tauxTva(), p.champ("tauxTva")), BigDecimal.ZERO);
                    // Montant forfaitaire = prix d'une surface de 1 : mêmes échelle et arrondi que le prix d'un article.
                    PrixLot montants = Boolean.TRUE.equals(l.saisiEnTTC())
                            ? CalculPrixArticle.depuisTTC(BigDecimal.ONE, montant(l.montantTTC(), p.champ("montantTTC")), tva)
                            : CalculPrixArticle.depuisHT(BigDecimal.ONE, montant(l.montantHT(), p.champ("montantHT")), tva);
                    e.setAmountHT(montants.prixHT());
                    e.setAmountTTC(montants.prixTTC());
                    e.setVatRatio(tva);
                    e.setPercentOf(fraction(l.pourcentageDe(), p.champ("pourcentageDe")));
                    e.setSorting(ou(l.ordre(), p.rang()));
                });
        return lire(tranche, FRAIS, CfgTrancheSettlementCategory.class, OngletsTrancheService::frais);
    }

    @Transactional
    public void supprimerTranche(UUID trancheId) {
        suppression.tranche(visible(trancheId));
    }

    // ------------------------------------------------------------------ outils

    private CfgTranche visible(UUID id) {
        return tranches.trancheVisible(id).orElseThrow(() -> new ResourceNotFoundException("Tranche introuvable: " + id));
    }

    private <E, D extends LignesTrancheDto.Ligne> LignesTrancheDto<D> lire(CfgTranche tranche, String jpql,
                                                                         Class<E> classe, Function<E, D> versDto) {
        return new LignesTrancheDto<>(lignes(tranche, jpql, classe).stream().map(versDto).toList());
    }

    private <E> List<E> lignes(CfgTranche tranche, String jpql, Class<E> classe) {
        return em.createQuery(jpql, classe).setParameter(ID, tranche.getId()).getResultList();
    }

    /**
     * {@code id} connu = mise à jour, absent = création, ligne de la base absente du corps = suppression ; une clé
     * déjà vue (doublon) est ignorée, comme OMRAN. Renvoie les lignes gardées, dans l'ordre du corps.
     */
    private <E extends LigneTrancheOmran, L extends LignesTrancheDto.Ligne> List<E> reconcilier(
            CfgTranche tranche, LignesTrancheDto<L> corps, String jpql, Class<E> classe, Supplier<E> nouvelle,
            Function<L, UUID> cle, String champCle, Remplir<E, L> remplir) {
        if (corps == null || corps.lignes() == null) {
            throw new InvalidRequestException("lignes est obligatoire.");
        }
        Map<UUID, E> existantes = lignes(tranche, jpql, classe).stream()
                .collect(Collectors.toMap(LigneTrancheOmran::getId, e -> e, (a, b) -> a, LinkedHashMap::new));
        TracePatrimoine.Trace t = trace.maintenant();
        Set<UUID> vues = new HashSet<>();
        List<E> gardees = new ArrayList<>();
        for (int i = 0; i < corps.lignes().size(); i++) {
            L ligne = corps.lignes().get(i);
            Position position = new Position(i, gardees.size() + 1);
            if (ligne == null || cle.apply(ligne) == null) {
                throw new InvalidRequestException(position.champ(champCle) + " est obligatoire.");
            }
            if (vues.add(cle.apply(ligne))) {
                E entite = ligne.id() == null ? creer(nouvelle, tranche, t) : existantes.remove(ligne.id());
                if (entite == null) {
                    throw new InvalidRequestException(position.champ(ID) + " : ligne inconnue de la tranche.");
                }
                remplir.appliquer(entite, ligne, position);
                entite.setUserUpdate(t.auteur());
                entite.setUserUpdateDateTime(t.le());
                gardees.add(entite);
            }
        }
        existantes.values().forEach(em::remove);
        return gardees;
    }

    private <E extends LigneTrancheOmran> E creer(Supplier<E> nouvelle, CfgTranche tranche, TracePatrimoine.Trace t) {
        E entite = nouvelle.get();
        entite.setId(UUID.randomUUID());
        entite.setCfgTranche(tranche);
        entite.setUserCreate(t.auteur());
        entite.setUserCreateDateTime(t.le());
        em.persist(entite);
        return entite;
    }

    private <T> T reference(Class<T> classe, UUID id, String champ) {
        T trouvee = em.find(classe, id);
        if (trouvee == null) {
            throw new InvalidRequestException("%s : référence inconnue (%s).".formatted(champ, id));
        }
        return trouvee;
    }

    private static BigDecimal montant(BigDecimal valeur, String champ) {
        return ou(positif(valeur, champ), BigDecimal.ZERO);
    }

    // ------------------------------------------------------------------ DTO

    private static LigneTerrainDto terrain(CfgTrancheStkItem l) {
        StkItem t = l.getStkItem();
        return new LigneTerrainDto(l.getId(), Champs.de(t, StkItem::getId), l.getRatio(), l.getSorting(),
                Champs.de(t, StkItem::getCode), Champs.de(t, StkItem::getDescription), Champs.de(t, StkItem::getArea));
    }

    private static LigneEtageDto etage(CfgTrancheLevel l) {
        LndLevel n = l.getLndLevel();
        return new LigneEtageDto(l.getId(), Champs.de(n, LndLevel::getId), Champs.de(n, LndLevel::getCode),
                Champs.de(n, LndLevel::getDescription));
    }

    private static LignePlancherDto plancher(CfgTrancheCoveredFloor l) {
        StkItemCategory c = l.getStkItemCategory();
        return new LignePlancherDto(l.getId(), Champs.de(c, StkItemCategory::getId), l.getArea(),
                Champs.de(c, StkItemCategory::getCode), Champs.de(c, StkItemCategory::getDescription));
    }

    private static LigneFraisDto frais(CfgTrancheSettlementCategory l) {
        ComSettlementCategory c = l.getComSettlementCategory();
        return new LigneFraisDto(l.getId(), Champs.de(c, ComSettlementCategory::getId), l.getSorting(), l.getAmountHT(),
                l.getVatRatio(), l.getAmountTTC(), l.getPercentOf(), null,
                Champs.de(c, ComSettlementCategory::getCode), Champs.de(c, ComSettlementCategory::getDescription));
    }
}

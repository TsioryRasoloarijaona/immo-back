package com.tsix.immocrm.services.encaissement;

import com.tsix.immocrm.domain.AgregatDemande;
import com.tsix.immocrm.domain.ChampDateEncaissement;
import com.tsix.immocrm.domain.ColonneEncaissement;
import com.tsix.immocrm.domain.Granularite;
import com.tsix.immocrm.domain.MontantRestant;
import com.tsix.immocrm.domain.TriGroupe;
import com.tsix.immocrm.services.groupement.GroupementService.ResultatGroupement;
import com.tsix.immocrm.dto.encaissement.AffectationDto;
import com.tsix.immocrm.dto.encaissement.EncaissementDetailDto;
import com.tsix.immocrm.dto.encaissement.EncaissementDto;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.stl.StlAccount;
import com.tsix.immocrm.entities.stl.StlDeposit;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.entities.stl.StlSettlementType;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import com.tsix.immocrm.services.groupement.GroupementService;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.TierNomFormatter;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import static java.util.Objects.requireNonNullElse;

/**
 * Écran « Encaissements Vente » d'OMRAN. Le filtre {@code AdmModuleId} du légataire n'est pas repris
 * (il ferait entrer les non affectés) — voir {@code FEATURE-encaissements-conformite-legataire.md} §4.
 */
@Service
@Transactional(readOnly = true)
public class EncaissementService {

    private final StlSettlementRepository settlementRepository;
    private final StlSettlementLineRepository settlementLineRepository;
    private final PerimetreCommercialService perimetreService;
    private final GroupementService groupementService;

    public EncaissementService(StlSettlementRepository settlementRepository,
                                StlSettlementLineRepository settlementLineRepository,
                                PerimetreCommercialService perimetreService,
                                GroupementService groupementService) {
        this.settlementRepository = settlementRepository;
        this.settlementLineRepository = settlementLineRepository;
        this.perimetreService = perimetreService;
        this.groupementService = groupementService;
    }

    public Page<EncaissementDto> findAll(List<UUID> entiteIds, List<UUID> typeIds, List<String> etats,
                                          UUID compteId, UUID depotId, UUID dossierId, List<UUID> trancheIds,
                                          UUID tierId, String reference, String search,
                                          Boolean comptabilise, BigDecimal montantDe, BigDecimal montantA,
                                          ChampDateEncaissement champDate, LocalDateTime dateDebut, LocalDateTime dateFin,
                                          ColonneEncaissement groupeColonne, String groupeValeur,
                                          Granularite granularite, Pageable pageable,
                                          Set<ColonneEncaissement> colonnes) {
        Specification<StlSettlement> spec = specEncaissements(new FiltresEncaissements(entiteIds, typeIds, etats,
                compteId, depotId, dossierId, trancheIds, tierId, reference, search, comptabilise, montantDe,
                montantA, champDate, dateDebut, dateFin));
        if (groupeColonne != null) {
            // Dépliage d'un groupe : la même expression de clé que /groupes, donc une valeur
            // réémise telle quelle par le front retombe exactement sur les lignes comptées.
            spec = spec.and((root, query, cb) -> GroupementService.predicatDeGroupe(
                    root, cb, groupeColonne.chemin(), granularite, groupeValeur));
        }
        // Les règlements qu'aucun projet ne résout restent visibles (FEATURE-perimetre-commercial.md §6.4).
        return settlementRepository.findAll(spec.and(perimetreService.filtreEncaissements()), pageable)
                .map(s -> toDto(s, colonnes));
    }

    /**
     * Même {@link #specEncaissements} et même périmètre que la liste : la somme des groupes retombe
     * sur son {@code totalElements}, sans règlement que l'appelant n'a pas le droit de lire.
     */
    public ResultatGroupement grouper(List<UUID> entiteIds, List<UUID> typeIds, List<String> etats,
                                       UUID compteId, UUID depotId, UUID dossierId, List<UUID> trancheIds,
                                       UUID tierId, String reference, String search,
                                       Boolean comptabilise, BigDecimal montantDe, BigDecimal montantA,
                                       ChampDateEncaissement champDate, LocalDateTime dateDebut, LocalDateTime dateFin,
                                       ColonneEncaissement groupeBy, Granularite granularite,
                                       List<AgregatDemande> agregats, TriGroupe tri,
                                       boolean inclureSansValeur, Pageable pageable) {
        Specification<StlSettlement> spec = specEncaissements(new FiltresEncaissements(entiteIds, typeIds, etats,
                compteId, depotId, dossierId, trancheIds, tierId, reference, search, comptabilise, montantDe,
                montantA, champDate, dateDebut, dateFin))
                .and(perimetreService.filtreEncaissements());
        return groupementService.grouper(StlSettlement.class, spec, groupeBy.chemin(), granularite,
                agregats, tri, inclureSansValeur, pageable);
    }

    /** Filtres de la liste des encaissements, tels que reçus du contrôleur. */
    private record FiltresEncaissements(
            List<UUID> entiteIds, List<UUID> typeIds, List<String> etats, UUID compteId,
            UUID depotId, UUID dossierId, List<UUID> trancheIds, UUID tierId, String reference,
            String search, Boolean comptabilise, BigDecimal montantDe, BigDecimal montantA,
            ChampDateEncaissement champDate, LocalDateTime dateDebut, LocalDateTime dateFin) {
    }

    /** Sans le périmètre commercial, que chaque appelant ajoute explicitement (comme {@code specDossiers}). */
    private static Specification<StlSettlement> specEncaissements(FiltresEncaissements f) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Vente seulement : un StlSettlement peut aussi servir un achat (BuyDocumentId) ou une
            // location (RntDocumentId).
            predicates.add(existeLigne(root, query, cb, cb::isNotNull));

            // Encaissement, jamais décaissement : « IsCredit = 1 » du légataire. Jamais NULL en base
            // (FEATURE-encaissements-conformite-legataire.md §3.1), le test strict n'escamote rien.
            predicates.add(cb.isTrue(root.get("isCredit")));

            ajouterFiltresReferentiels(predicates, root, cb, f);
            if (f.dossierId() != null) {
                predicates.add(existeLigne(root, query, cb,
                        echeance -> cb.equal(echeance.get("comFolder").get("id"), f.dossierId())));
            }
            if (!CollectionUtils.isEmpty(f.trancheIds())) {
                // Le légataire passe par StlSettlementRelationView ; côté Vente, le chemin par les
                // lignes donne le même résultat (FEATURE-encaissements-conformite-legataire.md §3.2).
                predicates.add(existeLigne(root, query, cb,
                        echeance -> echeance.get("comFolder").get("stkItem")
                                .get("stkHierarchy").get("id").in(f.trancheIds())));
            }
            ajouterFiltresTexte(predicates, root, cb, f);
            if (f.comptabilise() != null) {
                predicates.add(predicatComptabilise(root, cb, f.comptabilise()));
            }
            ajouterFiltresMontantEtDate(predicates, root, cb, f);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** {@code EXISTS} une ligne de ce règlement dont l'échéance ({@code comDocument}) vérifie la condition. */
    private static Predicate existeLigne(Root<StlSettlement> root, CriteriaQuery<?> query, CriteriaBuilder cb,
                                         Function<Path<Object>, Predicate> conditionEcheance) {
        Subquery<UUID> subquery = query.subquery(UUID.class);
        var line = subquery.from(StlSettlementLine.class);
        subquery.select(line.get("id"))
                .where(cb.and(
                        cb.equal(line.get("stlSettlement"), root),
                        conditionEcheance.apply(line.get("comDocument"))));
        return cb.exists(subquery);
    }

    private static void ajouterFiltresReferentiels(List<Predicate> predicates, Root<StlSettlement> root,
                                                   CriteriaBuilder cb, FiltresEncaissements f) {
        if (!CollectionUtils.isEmpty(f.entiteIds())) {
            predicates.add(root.get("cfgCompany").get("id").in(f.entiteIds()));
        }
        if (!CollectionUtils.isEmpty(f.typeIds())) {
            predicates.add(root.get("stlSettlementType").get("id").in(f.typeIds()));
        }
        if (!CollectionUtils.isEmpty(f.etats())) {
            List<String> etatsLower = f.etats().stream().map(String::toLowerCase).toList();
            predicates.add(cb.lower(root.get("status")).in(etatsLower));
        }
        if (f.compteId() != null) {
            predicates.add(cb.equal(root.get("stlAccount").get("id"), f.compteId()));
        }
        if (f.depotId() != null) {
            predicates.add(cb.equal(root.get("stlDeposit").get("id"), f.depotId()));
        }
        if (f.tierId() != null) {
            predicates.add(cb.equal(root.get("tier").get("id"), f.tierId()));
        }
    }

    private static void ajouterFiltresTexte(List<Predicate> predicates, Root<StlSettlement> root,
                                            CriteriaBuilder cb, FiltresEncaissements f) {
        if (StringUtils.hasText(f.reference())) {
            predicates.add(cb.like(cb.lower(root.get("code")), "%" + f.reference().toLowerCase() + "%"));
        }
        if (StringUtils.hasText(f.search())) {
            String pattern = "%" + f.search().toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("code")), pattern),
                    cb.like(cb.lower(root.get("tier").get("description")), pattern),
                    cb.like(cb.lower(root.get("tier").get("firstName")), pattern),
                    cb.like(cb.lower(root.get("tier").get("lastName")), pattern)
            ));
        }
    }

    /** Non comptabilisé = {@code IsAccounted} à faux <b>ou</b> NULL. */
    private static Predicate predicatComptabilise(Root<StlSettlement> root, CriteriaBuilder cb, boolean comptabilise) {
        Path<Boolean> isAccounted = root.get("isAccounted");
        if (comptabilise) {
            return cb.isTrue(isAccounted);
        }
        return cb.or(cb.isFalse(isAccounted), cb.isNull(isAccounted));
    }

    private static void ajouterFiltresMontantEtDate(List<Predicate> predicates, Root<StlSettlement> root,
                                                    CriteriaBuilder cb, FiltresEncaissements f) {
        if (f.montantDe() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), f.montantDe()));
        }
        if (f.montantA() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("amount"), f.montantA()));
        }
        if (f.dateDebut() == null && f.dateFin() == null) {
            return;
        }
        ChampDateEncaissement champDate = f.champDate() != null ? f.champDate() : ChampDateEncaissement.DATE_PIECE;
        var colonne = root.<LocalDateTime>get(champDate.getChampEntite());
        if (f.dateDebut() != null) {
            predicates.add(cb.greaterThanOrEqualTo(colonne, f.dateDebut()));
        }
        if (f.dateFin() != null) {
            predicates.add(cb.lessThanOrEqualTo(colonne, f.dateFin()));
        }
    }

    /** Pas de restriction « Vente » : un id direct désigne un règlement précis, quelle que soit son origine. */
    public EncaissementDetailDto findById(UUID id) {
        StlSettlement settlement = settlementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Encaissement introuvable: " + id));
        // Toutes les colonnes : la fiche n'est pas filtrable par ?colonnes=.
        return EncaissementDetailDto.de(
                toDto(settlement, EnumSet.allOf(ColonneEncaissement.class)),
                affectations(id));
    }

    /**
     * Échéances soldées, montant croissant. La liste n'expose que le dossier de la première ligne,
     * or un règlement peut couvrir plusieurs dossiers ({@code FEATURE-affectations-encaissement.md}).
     */
    private List<AffectationDto> affectations(UUID settlementId) {
        return settlementLineRepository.findAffectationsBySettlementId(settlementId).stream()
                .map(EncaissementService::toAffectation)
                .toList();
    }

    private static AffectationDto toAffectation(StlSettlementLine ligne) {
        ComDocument echeance = ligne.getComDocument();
        ComFolder dossier = Champs.de(echeance, ComDocument::getComFolder);
        ComSettlementCategory categorie = Champs.de(echeance, ComDocument::getComSettlementCategory);
        return new AffectationDto(
                ligne.getId(),
                ligne.getAmount(),
                Champs.de(echeance, ComDocument::getId),
                Champs.de(echeance, ComDocument::getCode),
                Champs.de(echeance, ComDocument::getDescription),
                Champs.de(echeance, ComDocument::getDocDate),
                Champs.de(echeance, ComDocument::getAmount),
                Champs.de(categorie, ComSettlementCategory::getId),
                Champs.de(categorie, ComSettlementCategory::getCode),
                Champs.de(dossier, ComFolder::getId),
                Champs.de(dossier, ComFolder::getCode));
    }

    /** {@code dossierId}/{@code dossierCode} coûtent une requête par ligne : chargés seulement si demandés. */
    private EncaissementDto toDto(StlSettlement s, Set<ColonneEncaissement> colonnes) {
        UUID dossierId = null;
        String dossierCode = null;
        if (colonnes.contains(ColonneEncaissement.DOSSIER_ID)
                || colonnes.contains(ColonneEncaissement.DOSSIER_CODE)) {
            List<Object[]> rows = settlementLineRepository.findDossierBySettlementId(s.getId());
            if (!rows.isEmpty()) {
                dossierId = (UUID) rows.get(0)[0];
                dossierCode = (String) rows.get(0)[1];
            }
        }

        BigDecimal montantBrut = s.getAmount() != null ? s.getAmount() : BigDecimal.ZERO;

        // Part affectée pré-agrégée (sommable par /groupes). Le total de groupe fait la même
        // soustraction, SUM(amount) - SUM(affecte) : ligne et bande « Total : » ne divergent pas.
        BigDecimal affecte = s.getAffectation() != null ? s.getAffectation().getMontantAffecte() : null;
        BigDecimal montantBrutRestant = MontantRestant.reglement(montantBrut, affecte);

        // Net = brut − RS − timbre, porté par la @Formula de l'entité ; le repli Java couvre l'entité
        // construite en mémoire, qu'une formule ne peuple jamais.
        BigDecimal montantNet = s.getMontantNetCalcule() != null
                ? s.getMontantNetCalcule()
                : montantBrut
                        .subtract(requireNonNullElse(s.getAmountRS(), BigDecimal.ZERO))
                        .subtract(requireNonNullElse(s.getAmountStamp(), BigDecimal.ZERO));

        StlSettlementType type = s.getStlSettlementType();
        CfgCompany entite = s.getCfgCompany();
        StlAccount compte = s.getStlAccount();
        StlDeposit depot = s.getStlDeposit();
        CfgTier tier = s.getTier();
        CfgTier banque = s.getCfgBank();
        // Une jointure paresseuse par ligne, payée seulement si la colonne est demandée.
        CfgTier tierPayeur = colonnes.contains(ColonneEncaissement.TIER_PAYEUR_ID)
                || colonnes.contains(ColonneEncaissement.TIER_PAYEUR_NOM)
                ? s.getTierPayer() : null;

        return new EncaissementDto(
                s.getId(),
                s.getCode(),
                s.getStatus(),
                dossierId,
                dossierCode,
                s.getDocDate(),
                s.getEcheanceDate(),
                s.getSettlementDate(),
                Champs.de(tier, CfgTier::getId),
                tier != null ? TierNomFormatter.nom(tier) : null,
                montantBrut,
                montantBrutRestant,
                montantNet,
                s.getIsAccounted(),
                Champs.de(type, StlSettlementType::getId),
                Champs.de(type, StlSettlementType::getCode),
                Champs.de(type, StlSettlementType::getDescription),
                Champs.de(entite, CfgCompany::getId),
                Champs.de(entite, CfgCompany::getCode),
                Champs.de(entite, CfgCompany::getDescription),
                Champs.de(compte, StlAccount::getId),
                Champs.de(compte, StlAccount::getCode),
                Champs.de(depot, StlDeposit::getId),
                Champs.de(depot, StlDeposit::getCode),
                Champs.de(banque, CfgTier::getId),
                banque != null ? TierNomFormatter.nom(banque) : null,
                s.getNotes(),
                s.getDescription(),
                s.getAmountBase(),
                Champs.de(tierPayeur, CfgTier::getId),
                tierPayeur != null ? TierNomFormatter.nom(tierPayeur) : null,
                s.getIsDcoumentDeliveredToCenter(),
                s.getReceiptCode(),
                s.getDeliveredToCenterDate(),
                s.getSlipCode(),
                s.getSlipDate(),
                s.getUserCreate(),
                s.getUserCreateDateTime(),
                s.getUserUpdate(),
                s.getUserUpdateDateTime()
        );
    }
}

package com.tsix.immocrm.services.encaissement;

import com.tsix.immocrm.domain.ChampDateEncaissement;
import com.tsix.immocrm.dto.encaissement.EncaissementDto;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.stl.StlAccount;
import com.tsix.immocrm.entities.stl.StlDeposit;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.entities.stl.StlSettlementType;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.TierNomFormatter;
import jakarta.persistence.criteria.Predicate;
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
import java.util.List;
import java.util.UUID;

/**
 * Backs {@code GET /api/encaissements} — reproduit l'écran legacy "Encaissements Vente" (module
 * Vente → onglet "Encaissements Vente" d'OMRAN Estate). Toujours restreint aux règlements liés à
 * au moins une échéance de Dossier de vente ({@code StlSettlementLine.ComDocumentId} non nul) —
 * un {@code StlSettlement} peut en théorie aussi servir des achats/locations (BuyDocumentId/
 * RntDocumentId), hors périmètre de cet endpoint.
 *
 * <p>Le filtre réel de l'écran legacy a été relevé dans la trace d'audit SQL Server du 11/09/2026
 * (écran {@code Yes.Com.Front.Credit.SettlementList}) et est transcrit dans
 * {@code instructions/FEATURE-encaissements-conformite-legataire.md}. Deux de ses clauses y sont
 * reproduites — {@code IsCredit = 1} et le filtre par tranche ; une troisième ne l'est pas : le
 * rattachement au module Vente par {@code AdmModuleId}, qui ferait entrer les encaissements non
 * affectés. Voir le §4 de la spec, décision produit ouverte.
 */
@Service
@Transactional(readOnly = true)
public class EncaissementService {

    private final StlSettlementRepository settlementRepository;
    private final StlSettlementLineRepository settlementLineRepository;
    private final PerimetreCommercialService perimetreService;

    public EncaissementService(StlSettlementRepository settlementRepository,
                                StlSettlementLineRepository settlementLineRepository,
                                PerimetreCommercialService perimetreService) {
        this.settlementRepository = settlementRepository;
        this.settlementLineRepository = settlementLineRepository;
        this.perimetreService = perimetreService;
    }

    public Page<EncaissementDto> findAll(List<UUID> entiteIds, List<UUID> typeIds, List<String> etats,
                                          UUID compteId, UUID depotId, UUID dossierId, List<UUID> trancheIds,
                                          UUID tierId, String reference, String search,
                                          Boolean comptabilise, BigDecimal montantDe, BigDecimal montantA,
                                          ChampDateEncaissement champDate, LocalDateTime dateDebut, LocalDateTime dateFin,
                                          Pageable pageable) {
        Specification<StlSettlement> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Restreint aux règlements "Vente" : au moins une ligne rattachée à une échéance
            // (ComDocument) de Dossier de vente — un StlSettlement pourrait sinon aussi bien
            // servir un achat (BuyDocumentId) ou une location (RntDocumentId).
            Subquery<UUID> venteSubquery = query.subquery(UUID.class);
            var line = venteSubquery.from(StlSettlementLine.class);
            venteSubquery.select(line.get("id"))
                    .where(cb.and(
                            cb.equal(line.get("stlSettlement"), root),
                            cb.isNotNull(line.get("comDocument"))));
            predicates.add(cb.exists(venteSubquery));

            // Encaissement, jamais décaissement. L'écran legacy pose « AND IsCredit = 1 » sans
            // condition ; nous le reproduisons à l'identique. Colonne renseignée sur la totalité
            // des lignes réelles (0 NULL sur 3 243), et EcheancierService pose Boolean.TRUE à la
            // création, donc le test strict n'escamote rien.
            predicates.add(cb.isTrue(root.get("isCredit")));

            if (!CollectionUtils.isEmpty(entiteIds)) {
                predicates.add(root.get("cfgCompany").get("id").in(entiteIds));
            }
            if (!CollectionUtils.isEmpty(typeIds)) {
                predicates.add(root.get("stlSettlementType").get("id").in(typeIds));
            }
            if (!CollectionUtils.isEmpty(etats)) {
                List<String> etatsLower = etats.stream().map(String::toLowerCase).toList();
                predicates.add(cb.lower(root.get("status")).in(etatsLower));
            }
            if (compteId != null) {
                predicates.add(cb.equal(root.get("stlAccount").get("id"), compteId));
            }
            if (depotId != null) {
                predicates.add(cb.equal(root.get("stlDeposit").get("id"), depotId));
            }
            if (tierId != null) {
                predicates.add(cb.equal(root.get("tier").get("id"), tierId));
            }
            if (dossierId != null) {
                Subquery<UUID> dossierSubquery = query.subquery(UUID.class);
                var dossierLine = dossierSubquery.from(StlSettlementLine.class);
                dossierSubquery.select(dossierLine.get("id"))
                        .where(cb.and(
                                cb.equal(dossierLine.get("stlSettlement"), root),
                                cb.equal(dossierLine.get("comDocument").get("comFolder").get("id"), dossierId)));
                predicates.add(cb.exists(dossierSubquery));
            }
            if (!CollectionUtils.isEmpty(trancheIds)) {
                // L'écran legacy passe par StlSettlementRelationView.CfgTrancheId ; le chemin par
                // les lignes donne exactement le même résultat côté Vente (mesuré : 644 couples
                // (règlement, tranche) des deux côtés, aucune divergence dans ce sens). Les 5
                // couples que seule la vue porte sont des relations d'achat et de location, déjà
                // hors du périmètre de cet endpoint — inutile donc de mapper la vue.
                Subquery<UUID> trancheSubquery = query.subquery(UUID.class);
                var trancheLine = trancheSubquery.from(StlSettlementLine.class);
                trancheSubquery.select(trancheLine.get("id"))
                        .where(cb.and(
                                cb.equal(trancheLine.get("stlSettlement"), root),
                                trancheLine.get("comDocument").get("comFolder").get("stkItem")
                                        .get("stkHierarchy").get("id").in(trancheIds)));
                predicates.add(cb.exists(trancheSubquery));
            }
            if (StringUtils.hasText(reference)) {
                predicates.add(cb.like(cb.lower(root.get("code")), "%" + reference.toLowerCase() + "%"));
            }
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), pattern),
                        cb.like(cb.lower(root.get("tier").get("description")), pattern),
                        cb.like(cb.lower(root.get("tier").get("firstName")), pattern),
                        cb.like(cb.lower(root.get("tier").get("lastName")), pattern)
                ));
            }
            if (comptabilise != null) {
                predicates.add(comptabilise
                        ? cb.isTrue(root.get("isAccounted"))
                        : cb.or(cb.isFalse(root.get("isAccounted")), cb.isNull(root.get("isAccounted"))));
            }
            if (montantDe != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), montantDe));
            }
            if (montantA != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("amount"), montantA));
            }
            if (dateDebut != null || dateFin != null) {
                String champ = (champDate != null ? champDate : ChampDateEncaissement.DATE_PIECE).getChampEntite();
                var colonne = root.<LocalDateTime>get(champ);
                if (dateDebut != null) {
                    predicates.add(cb.greaterThanOrEqualTo(colonne, dateDebut));
                }
                if (dateFin != null) {
                    predicates.add(cb.lessThanOrEqualTo(colonne, dateFin));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // Périmètre commercial : ne renvoie que les règlements rattachés à un dossier d'un projet
        // affecté (plus ceux qu'aucun projet ne résout — §6.4). Prédicat neutre si l'appelant
        // n'est pas restreint.
        return settlementRepository.findAll(spec.and(perimetreService.filtreEncaissements()), pageable)
                .map(this::toDto);
    }

    /**
     * {@code GET /api/encaissements/{id}} : détail d'un règlement, même forme que les lignes de
     * {@link #findAll}. Pas de restriction "Vente uniquement" ici contrairement à {@code findAll}
     * — un id direct désigne un {@code StlSettlement} précis, peu importe son origine ; en pratique
     * seuls des ids issus de la liste (donc déjà scopés Vente) sont attendus côté front.
     */
    public EncaissementDto findById(UUID id) {
        StlSettlement settlement = settlementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Encaissement introuvable: " + id));
        return toDto(settlement);
    }

    private EncaissementDto toDto(StlSettlement s) {
        UUID dossierId = null;
        String dossierCode = null;
        List<Object[]> rows = settlementLineRepository.findDossierBySettlementId(s.getId());
        if (!rows.isEmpty()) {
            dossierId = (UUID) rows.get(0)[0];
            dossierCode = (String) rows.get(0)[1];
        }

        BigDecimal montantBrut = s.getAmount() != null ? s.getAmount() : BigDecimal.ZERO;
        BigDecimal affecte = settlementLineRepository.sumAmountByStlSettlementId(s.getId());
        BigDecimal montantBrutRestant = montantBrut.subtract(affecte != null ? affecte : BigDecimal.ZERO);
        BigDecimal montantNet = montantBrut
                .subtract(s.getAmountRS() != null ? s.getAmountRS() : BigDecimal.ZERO)
                .subtract(s.getAmountStamp() != null ? s.getAmountStamp() : BigDecimal.ZERO);

        StlSettlementType type = s.getStlSettlementType();
        CfgCompany entite = s.getCfgCompany();
        StlAccount compte = s.getStlAccount();
        StlDeposit depot = s.getStlDeposit();
        CfgTier tier = s.getTier();
        CfgTier banque = s.getCfgBank();

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
                s.getNotes()
        );
    }
}

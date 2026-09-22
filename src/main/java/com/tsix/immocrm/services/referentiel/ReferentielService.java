package com.tsix.immocrm.services.referentiel;

import com.tsix.immocrm.domain.StatutArticle;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.patrimoine.ArticleListItemDto;
import com.tsix.immocrm.dto.referentiel.OrigineProspectionDto;
import com.tsix.immocrm.dto.referentiel.PaysDto;
import com.tsix.immocrm.dto.referentiel.ProfilCategorieDto;
import com.tsix.immocrm.dto.referentiel.ProfilDto;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.dto.referentiel.ResultatInteractionDto;
import com.tsix.immocrm.dto.referentiel.TitreDto;
import com.tsix.immocrm.dto.referentiel.TypeReglementDto;
import com.tsix.immocrm.dto.referentiel.VilleDto;
import com.tsix.immocrm.entities.adm.AdmCity;
import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.adm.AdmFamilySituation;
import com.tsix.immocrm.entities.adm.AdmIdentityDocumentType;
import com.tsix.immocrm.entities.adm.AdmMatrimonialRegime;
import com.tsix.immocrm.entities.adm.AdmNationality;
import com.tsix.immocrm.entities.adm.AdmProfession;
import com.tsix.immocrm.entities.adm.AdmResidenceType;
import com.tsix.immocrm.entities.adm.AdmSalary;
import com.tsix.immocrm.entities.adm.AdmSexType;
import com.tsix.immocrm.entities.adm.AdmTierTitle;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgRelationType;
import com.tsix.immocrm.entities.cfg.CfgTierKind;
import com.tsix.immocrm.entities.cfg.CfgTierType;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComAction;
import com.tsix.immocrm.entities.com.ComActionMessageSubCategory;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.entities.com.ComPaymentMethod;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.com.ComProspectionClosingReason;
import com.tsix.immocrm.entities.com.ComProspectionKind;
import com.tsix.immocrm.entities.com.ComProspectionOrigin;
import com.tsix.immocrm.entities.com.ComProspectionOriginSupport;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.stl.StlSettlementType;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmAttachedFileTypeRepository;
import com.tsix.immocrm.repositories.adm.AdmCityRepository;
import com.tsix.immocrm.repositories.adm.AdmCountryRepository;
import com.tsix.immocrm.repositories.adm.AdmFamilySituationRepository;
import com.tsix.immocrm.repositories.adm.AdmIdentityDocumentTypeRepository;
import com.tsix.immocrm.repositories.adm.AdmMatrimonialRegimeRepository;
import com.tsix.immocrm.repositories.adm.AdmNationalityRepository;
import com.tsix.immocrm.repositories.adm.AdmProfessionRepository;
import com.tsix.immocrm.repositories.adm.AdmResidenceTypeRepository;
import com.tsix.immocrm.repositories.adm.AdmRoleRepository;
import com.tsix.immocrm.repositories.adm.AdmSalaryRepository;
import com.tsix.immocrm.repositories.adm.AdmSexTypeRepository;
import com.tsix.immocrm.repositories.adm.AdmTierTitleRepository;
import com.tsix.immocrm.repositories.cfg.CfgCompanyRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgRelationTypeRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierKindRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierTypeRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageSubCategoryRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageTypeRepository;
import com.tsix.immocrm.repositories.com.ComActionRepository;
import com.tsix.immocrm.repositories.com.ComPaymentMethodRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import com.tsix.immocrm.repositories.com.ComProspectionClosingReasonRepository;
import com.tsix.immocrm.repositories.com.ComProspectionKindRepository;
import com.tsix.immocrm.repositories.com.ComProspectionOriginRepository;
import com.tsix.immocrm.repositories.com.ComProspectionOriginSupportRepository;
import com.tsix.immocrm.repositories.com.ComProspectionProfileCategoryRepository;
import com.tsix.immocrm.repositories.com.ComProspectionProfileRepository;
import com.tsix.immocrm.repositories.stk.StkFloorRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.stk.StkKindRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.repositories.stl.StlAccountRepository;
import com.tsix.immocrm.repositories.stl.StlDepositRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementTypeRepository;
import com.tsix.immocrm.services.patrimoine.ArticleService;
import com.tsix.immocrm.services.patrimoine.StkItemSpecifications;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Backs the {@code /api/referentiels/*} lookup endpoints (GAP-02). */
@Service
@Transactional(readOnly = true)
public class ReferentielService {

    /**
     * {@code ComActionMessageSubCategory.Code} values that require a "Date du rendez-vous" (RDV
     * ou visite planifiée) on the interaction they're attached to — mirrors
     * {@code ProspectionService.RESULTATS_AVEC_DATE_PLANIFIEE}, confirmed empirically.
     */
    private static final Set<String> RESULTATS_AVEC_DATE_PLANIFIEE = Set.of(
            "Rendez vous pris", "RDV reporté", "Le prospect veut faire une visite du projet");

    /** {@code ComActionMessageCategory.Code} the "Résultat" dropdown is scoped to. */
    private static final String CATEGORIE_RESULTAT_INTERACTION = "Prospection";

    private final ComProspectionKindRepository prospectionKindRepository;
    private final ComProspectionOriginRepository prospectionOriginRepository;
    private final ComProspectionOriginSupportRepository prospectionOriginSupportRepository;
    private final ComProspectionClosingReasonRepository closingReasonRepository;
    private final ComActionMessageTypeRepository actionMessageTypeRepository;
    private final ComActionMessageSubCategoryRepository actionMessageSubCategoryRepository;
    private final ComActionRepository actionRepository;
    private final StkVocationRepository vocationRepository;
    private final CfgTierTypeRepository tierTypeRepository;
    private final CfgTierKindRepository tierKindRepository;
    private final AdmTierTitleRepository tierTitleRepository;
    private final AdmSexTypeRepository sexTypeRepository;
    private final AdmCountryRepository countryRepository;
    private final AdmNationalityRepository nationalityRepository;
    private final AdmFamilySituationRepository familySituationRepository;
    private final AdmResidenceTypeRepository residenceTypeRepository;
    private final AdmMatrimonialRegimeRepository matrimonialRegimeRepository;
    private final AdmIdentityDocumentTypeRepository identityDocumentTypeRepository;
    private final AdmProfessionRepository professionRepository;
    private final AdmSalaryRepository salaryRepository;
    private final StkItemCategoryRepository itemCategoryRepository;
    private final StkItemTypeRepository itemTypeRepository;
    private final CfgRelationTypeRepository relationTypeRepository;
    private final AdmCityRepository cityRepository;
    private final ComProspectionProfileCategoryRepository profileCategoryRepository;
    private final ComProspectionProfileRepository profileRepository;
    private final ComPaymentMethodRepository paymentMethodRepository;
    private final ComSettlementCategoryRepository settlementCategoryRepository;
    private final CfgProjectRepository projectRepository;
    private final CfgTrancheRepository trancheRepository;
    private final StkItemRepository stkItemRepository;
    private final AdmRoleRepository roleRepository;
    private final StlSettlementTypeRepository settlementTypeRepository;
    private final CfgTierRepository tierRepository;
    private final CfgCompanyRepository companyRepository;
    private final StlAccountRepository accountRepository;
    private final StlDepositRepository depositRepository;
    private final AdmAttachedFileTypeRepository attachedFileTypeRepository;
    private final StkKindRepository kindRepository;
    private final StkFloorRepository floorRepository;

    public ReferentielService(ComProspectionKindRepository prospectionKindRepository,
                               ComProspectionOriginRepository prospectionOriginRepository,
                               ComProspectionOriginSupportRepository prospectionOriginSupportRepository,
                               ComProspectionClosingReasonRepository closingReasonRepository,
                               ComActionMessageTypeRepository actionMessageTypeRepository,
                               ComActionMessageSubCategoryRepository actionMessageSubCategoryRepository,
                               ComActionRepository actionRepository,
                               StkVocationRepository vocationRepository,
                               CfgTierTypeRepository tierTypeRepository,
                               CfgTierKindRepository tierKindRepository,
                               AdmTierTitleRepository tierTitleRepository,
                               AdmSexTypeRepository sexTypeRepository,
                               AdmCountryRepository countryRepository,
                               AdmNationalityRepository nationalityRepository,
                               AdmFamilySituationRepository familySituationRepository,
                               AdmResidenceTypeRepository residenceTypeRepository,
                               AdmMatrimonialRegimeRepository matrimonialRegimeRepository,
                               AdmIdentityDocumentTypeRepository identityDocumentTypeRepository,
                               AdmProfessionRepository professionRepository,
                               AdmSalaryRepository salaryRepository,
                               StkItemCategoryRepository itemCategoryRepository,
                               StkItemTypeRepository itemTypeRepository,
                               CfgRelationTypeRepository relationTypeRepository,
                               AdmCityRepository cityRepository,
                               ComProspectionProfileCategoryRepository profileCategoryRepository,
                               ComProspectionProfileRepository profileRepository,
                               ComPaymentMethodRepository paymentMethodRepository,
                               ComSettlementCategoryRepository settlementCategoryRepository,
                               CfgProjectRepository projectRepository,
                               CfgTrancheRepository trancheRepository,
                               StkItemRepository stkItemRepository,
                               AdmRoleRepository roleRepository,
                               StlSettlementTypeRepository settlementTypeRepository,
                               CfgTierRepository tierRepository,
                               CfgCompanyRepository companyRepository,
                               StlAccountRepository accountRepository,
                               StlDepositRepository depositRepository,
                               AdmAttachedFileTypeRepository attachedFileTypeRepository,
                               StkKindRepository kindRepository,
                               StkFloorRepository floorRepository) {
        this.prospectionKindRepository = prospectionKindRepository;
        this.prospectionOriginRepository = prospectionOriginRepository;
        this.prospectionOriginSupportRepository = prospectionOriginSupportRepository;
        this.closingReasonRepository = closingReasonRepository;
        this.actionMessageTypeRepository = actionMessageTypeRepository;
        this.actionMessageSubCategoryRepository = actionMessageSubCategoryRepository;
        this.actionRepository = actionRepository;
        this.vocationRepository = vocationRepository;
        this.tierTypeRepository = tierTypeRepository;
        this.tierKindRepository = tierKindRepository;
        this.tierTitleRepository = tierTitleRepository;
        this.sexTypeRepository = sexTypeRepository;
        this.countryRepository = countryRepository;
        this.nationalityRepository = nationalityRepository;
        this.familySituationRepository = familySituationRepository;
        this.residenceTypeRepository = residenceTypeRepository;
        this.matrimonialRegimeRepository = matrimonialRegimeRepository;
        this.identityDocumentTypeRepository = identityDocumentTypeRepository;
        this.professionRepository = professionRepository;
        this.salaryRepository = salaryRepository;
        this.itemCategoryRepository = itemCategoryRepository;
        this.itemTypeRepository = itemTypeRepository;
        this.relationTypeRepository = relationTypeRepository;
        this.cityRepository = cityRepository;
        this.profileCategoryRepository = profileCategoryRepository;
        this.profileRepository = profileRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.settlementCategoryRepository = settlementCategoryRepository;
        this.projectRepository = projectRepository;
        this.trancheRepository = trancheRepository;
        this.stkItemRepository = stkItemRepository;
        this.roleRepository = roleRepository;
        this.settlementTypeRepository = settlementTypeRepository;
        this.tierRepository = tierRepository;
        this.companyRepository = companyRepository;
        this.accountRepository = accountRepository;
        this.depositRepository = depositRepository;
        this.attachedFileTypeRepository = attachedFileTypeRepository;
        this.kindRepository = kindRepository;
        this.floorRepository = floorRepository;
    }

    /**
     * {@code GET /api/referentiels/roles} — {@code AdmRole} disponibles, à cocher lors de la
     * création d'un employé ({@code POST /api/utilisateurs}, champ {@code roles}, une liste de
     * codes). Sans cet endpoint, le front n'avait aucun moyen de peupler ce choix autrement qu'en
     * codant les valeurs en dur.
     */
    public List<ReferentielItemDto> roles() {
        return roleRepository.findAll().stream()
                .map(r -> new ReferentielItemDto(r.getId(), r.getCode(), r.getDescription(), null))
                .toList();
    }

    public List<ReferentielItemDto> etapesProspection() {
        return prospectionKindRepository.findAll().stream().map(k -> new ReferentielItemDto(
                k.getId(), k.getCode(), k.getDescription(), null)).toList();
    }

    public List<OrigineProspectionDto> originesProspection() {
        return prospectionOriginRepository.findAllByOrderBySortingAsc().stream().map(o -> new OrigineProspectionDto(
                o.getId(), o.getCode(), o.getDescription(),
                o.getIsWithProposer(), o.getIsWithGift(), o.getIsWithSupport())).toList();
    }

    public List<ReferentielItemDto> supportsForOrigine(UUID origineId) {
        return prospectionOriginSupportRepository.findByComProspectionOriginId(origineId).stream()
                .map(s -> new ReferentielItemDto(s.getId(), s.getCode(), s.getDescription(), null)).toList();
    }

    public List<ReferentielItemDto> raisonsCloture() {
        return closingReasonRepository.findAll().stream().map(r -> new ReferentielItemDto(
                r.getId(), r.getCode(), r.getDescription(), null)).toList();
    }

    public List<ReferentielItemDto> typesContactProspection() {
        return actionMessageTypeRepository.findAll().stream().map(t -> new ReferentielItemDto(
                t.getId(), t.getCode(), t.getDescription(), null)).toList();
    }

    /**
     * {@code GET /api/referentiels/resultats-interaction} — the "Résultat" dropdown for
     * {@code POST /api/prospections/{id}/interactions}. See {@link ResultatInteractionDto}.
     */
    public List<ResultatInteractionDto> resultatsInteraction() {
        return actionMessageSubCategoryRepository
                .findByComActionMessageCategoryCodeOrderByDescriptionAsc(CATEGORIE_RESULTAT_INTERACTION)
                .stream()
                .map(ReferentielService::toResultatInteractionDto)
                .toList();
    }

    private static ResultatInteractionDto toResultatInteractionDto(ComActionMessageSubCategory s) {
        return new ResultatInteractionDto(
                s.getId(),
                s.getCode(),
                s.getDescription(),
                RESULTATS_AVEC_DATE_PLANIFIEE.contains(s.getCode()),
                Boolean.TRUE.equals(s.getIsRevivalMandatory())
        );
    }

    public List<ReferentielItemDto> actionsCommerciales() {
        return actionRepository.findAll().stream().map(a -> new ReferentielItemDto(
                a.getId(), a.getCode(), a.getDescription(), null)).toList();
    }

    public List<ReferentielItemDto> vocations() {
        return vocationRepository.findAll().stream().map(v -> new ReferentielItemDto(
                v.getId(), v.getCode(), v.getDescription(), null)).toList();
    }

    /**
     * {@code GET /api/referentiels/natures} — dropdown "Nature" ({@code StkKind}) pour le filtre
     * {@code natureId} de {@code GET /api/articles}/{@code GET /api/tranches/{id}/articles}
     * (instructions/from_front.md #4). Distinct de Catégorie/Type — voir {@code StkKind} javadoc.
     */
    public List<ReferentielItemDto> natures() {
        return kindRepository.findAll().stream().map(k -> new ReferentielItemDto(
                k.getId(), k.getCode(), k.getDescription(), null)).toList();
    }

    /**
     * {@code GET /api/referentiels/etages} — dropdown "Étage" ({@code StkFloor}) pour le filtre
     * {@code etageId} des mêmes endpoints Article (instructions/from_front.md #4).
     */
    public List<ReferentielItemDto> etages() {
        return floorRepository.findAll().stream().map(f -> new ReferentielItemDto(
                f.getId(), f.getCode(), f.getDescription(), null)).toList();
    }

    public List<ReferentielItemDto> tierTypes() {
        return tierTypeRepository.findAllByOrderBySortingAsc().stream().map(t -> new ReferentielItemDto(
                t.getId(), t.getCode(), t.getDescription(), t.getSorting())).toList();
    }

    public List<ReferentielItemDto> tierKindsForType(UUID typeId) {
        return tierKindRepository.findByCfgTierTypeIdOrderBySortingAsc(typeId).stream().map(k -> new ReferentielItemDto(
                k.getId(), k.getCode(), k.getDescription(), k.getSorting())).toList();
    }

    /**
     * {@code GET /api/referentiels/types-employe} — {@code CfgTierKind} codes valid for
     * {@code POST /api/utilisateurs}'s {@code typeEmployeCode} (kinds under the "EMP" CfgTierType).
     */
    public List<ReferentielItemDto> typesEmploye() {
        return tierKindRepository.findByCfgTierTypeCodeOrderBySortingAsc("EMP").stream().map(k -> new ReferentielItemDto(
                k.getId(), k.getCode(), k.getDescription(), k.getSorting())).toList();
    }

    public List<TitreDto> titres() {
        return tierTitleRepository.findAllByOrderBySortingAsc().stream().map(t -> new TitreDto(
                t.getId(), t.getCode(), t.getDescription(),
                t.getAdmSexType() != null ? t.getAdmSexType().getId() : null)).toList();
    }

    public List<ReferentielItemDto> sexes() {
        return sexTypeRepository.findAllByOrderBySortingAsc().stream().map(s -> new ReferentielItemDto(
                s.getId(), s.getCode(), s.getDescription(), s.getSorting())).toList();
    }

    public List<PaysDto> pays() {
        return countryRepository.findAllByOrderByDescriptionAsc().stream().map(ReferentielService::toPaysDto).toList();
    }

    public List<ReferentielItemDto> nationalites() {
        return nationalityRepository.findAllByOrderByDescriptionAsc().stream().map(n -> new ReferentielItemDto(
                n.getId(), n.getCode(), n.getDescription(), null)).toList();
    }

    public List<ReferentielItemDto> situationsFamiliales() {
        return familySituationRepository.findAllByOrderBySortingAsc().stream().map(s -> new ReferentielItemDto(
                s.getId(), s.getCode(), s.getDescription(), s.getSorting())).toList();
    }

    public List<ReferentielItemDto> typesResidence() {
        return residenceTypeRepository.findAllByOrderBySortingAsc().stream().map(t -> new ReferentielItemDto(
                t.getId(), t.getCode(), t.getDescription(), t.getSorting())).toList();
    }

    public List<ReferentielItemDto> regimesMatrimoniaux() {
        return matrimonialRegimeRepository.findAllByOrderBySortingAsc().stream().map(r -> new ReferentielItemDto(
                r.getId(), r.getCode(), r.getDescription(), r.getSorting())).toList();
    }

    public List<ReferentielItemDto> typesPieceIdentite() {
        return identityDocumentTypeRepository.findAllByOrderBySortingAsc().stream().map(t -> new ReferentielItemDto(
                t.getId(), t.getCode(), t.getDescription(), t.getSorting())).toList();
    }

    /**
     * {@code GET /api/referentiels/types-piece-jointe} — {@code AdmAttachedFileType} disponibles
     * (ex. "PIECE D'IDENTITE", "CONTRAT"...), pour peupler le sélecteur de type au dépôt d'une
     * pièce jointe de dossier ({@code POST /api/dossiers/{id}/pieces-jointes}, paramètre
     * {@code typeId}).
     */
    public List<ReferentielItemDto> typesPieceJointe() {
        return attachedFileTypeRepository.findAll().stream()
                .sorted(Comparator.comparing(t -> Objects.toString(t.getDescription(), "")))
                .map(t -> new ReferentielItemDto(t.getId(), t.getCode(), t.getDescription(), null))
                .toList();
    }

    public List<ReferentielItemDto> professions() {
        return professionRepository.findAllByOrderByDescriptionAsc().stream().map(p -> new ReferentielItemDto(
                p.getId(), p.getCode(), p.getDescription(), null)).toList();
    }

    public List<ReferentielItemDto> salaires() {
        return salaryRepository.findAllByOrderBySortingAsc().stream().map(s -> new ReferentielItemDto(
                s.getId(), s.getCode(), s.getDescription(), s.getSorting())).toList();
    }

    public List<ReferentielItemDto> categoriesBien() {
        return itemCategoryRepository.findAll().stream().map(c -> new ReferentielItemDto(
                c.getId(), c.getCode(), c.getDescription(), null)).toList();
    }

    public List<ReferentielItemDto> typesBien() {
        return itemTypeRepository.findAll().stream().map(t -> new ReferentielItemDto(
                t.getId(), t.getCode(), t.getDescription(), null)).toList();
    }

    public List<ReferentielItemDto> typesRelationContact() {
        return relationTypeRepository.findAll().stream().map(r -> new ReferentielItemDto(
                r.getId(), r.getCode(), r.getDescription(), null)).toList();
    }

    public Page<VilleDto> villes(UUID paysId, String search, Pageable pageable) {
        Specification<AdmCity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (paysId != null) {
                predicates.add(cb.equal(root.get("admCountry").get("id"), paysId));
            }
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return cityRepository.findAll(spec, pageable).map(ReferentielService::toVilleDto);
    }

    public List<ReferentielItemDto> modesPaiement() {
        return paymentMethodRepository.findAll().stream().map(m -> new ReferentielItemDto(
                m.getId(), m.getCode(), m.getDescription(), null)).toList();
    }

    /**
     * {@code GET /api/referentiels/types-reglement} — {@code StlSettlementType}, une table
     * distincte de {@code ComPaymentMethod} (voir {@link #modesPaiement}) : confirmé en suivant
     * {@code EcheancierService.ajouterReglement}, qui résout {@code CreerReglementRequest.typeId}
     * contre {@code StlSettlementType}, jamais {@code ComPaymentMethod}. Alimente le "Type" du
     * formulaire "+ Règlement" sur la fiche Dossier.
     */
    public List<TypeReglementDto> typesReglement() {
        return settlementTypeRepository.findAllByOrderByDescriptionAsc().stream()
                .map(ReferentielService::toTypeReglementDto).toList();
    }

    private static TypeReglementDto toTypeReglementDto(StlSettlementType t) {
        return new TypeReglementDto(t.getId(), t.getCode(), t.getDescription(),
                t.getIsBankMandatory(), t.getIsReferenceMandatory());
    }

    /**
     * {@code GET /api/referentiels/banques} — {@code CfgTier} de type "BAN" (confirmé sur données
     * réelles : 21 tiers), pour {@code CreerReglementRequest.banqueId}. N'a de sens que pour les
     * types de règlement où {@code TypeReglementDto.banqueObligatoire} est vrai (ex. CHÈQUE,
     * VIREMENT) — pas pour ESPÈCES par exemple.
     */
    public List<ReferentielItemDto> banques() {
        return tierRepository.findByCfgTierType_CodeOrderByDescriptionAsc("BAN").stream()
                .map(t -> new ReferentielItemDto(t.getId(), t.getCode(), t.getDescription(), null)).toList();
    }

    /**
     * {@code GET /api/referentiels/entites} — dropdown "Entité" pour {@code GET /api/encaissements}
     * (colonne/filtre "Entité" de l'écran legacy "Encaissements Vente"). Restreint aux entités
     * en exploitation ({@code CfgCompany.isActive}), comme les autres listings.
     */
    public List<ReferentielItemDto> entites() {
        return companyRepository.findActivesOrderByDescriptionAsc().stream()
                .map(c -> new ReferentielItemDto(c.getId(), c.getCode(), c.getDescription(), null)).toList();
    }

    /**
     * {@code GET /api/referentiels/comptes} — dropdown "Compte" pour {@code GET /api/encaissements}
     * ({@code StlAccount}, un compte bancaire par entité juridique en données réelles).
     */
    public List<ReferentielItemDto> comptes() {
        return accountRepository.findAllByOrderByDescriptionAsc().stream()
                .map(a -> new ReferentielItemDto(a.getId(), a.getCode(), a.getDescription(), null)).toList();
    }

    /**
     * {@code GET /api/referentiels/depots} — dropdown "Dépôt" pour {@code GET /api/encaissements}
     * ({@code StlDeposit} — seulement 3 lignes en données réelles : FOURNISSEUR/CLIENT/BANQUE).
     */
    public List<ReferentielItemDto> depots() {
        return depositRepository.findAllByOrderByDescriptionAsc().stream()
                .map(d -> new ReferentielItemDto(d.getId(), d.getCode(), d.getDescription(), null)).toList();
    }

    /** {@code ComSettlementCategory} — the "Catégorie" lookup for échéancier lines (AVANCE, AUTO-FINANCEMENT, FRAIS SYNDIC...). */
    public List<ReferentielItemDto> categoriesEcheance() {
        return settlementCategoryRepository.findAll().stream().map(c -> new ReferentielItemDto(
                c.getId(), c.getCode(), c.getDescription(), null)).toList();
    }

    public List<ProfilCategorieDto> profilsProspection() {
        return profileCategoryRepository.findAllByOrderBySortingAsc().stream().map(cat -> new ProfilCategorieDto(
                cat.getId(), cat.getCode(), cat.getDescription(), cat.getMultiSelect(),
                profileRepository.findByComProspectionProfileCategoryIdOrderBySortingAsc(cat.getId()).stream()
                        .map(p -> new ProfilDto(p.getId(), p.getCode(), p.getDescription()))
                        .toList()
        )).toList();
    }

    /**
     * {@code GET /api/referentiels/projets} — dropdown "Projet" (CfgProject.Code/Description).
     * Restreint aux entités juridiques en exploitation
     * ({@code CfgCompany.isActive}).
     */
    public List<ReferentielItemDto> projets() {
        return projectRepository.findDesEntitesActives().stream()
                .map(p -> new ReferentielItemDto(p.getId(), p.getCode(), p.getDescription(), null)).toList();
    }

    /**
     * {@code GET /api/referentiels/tranches} — dropdown "Tranche", optionnellement scopé à un
     * projet via {@code projetId} (mêmes cascades de sélection Projet → Tranche → Article que
     * {@code GET /api/tranches?projetId=} mais sans pagination, pour peupler un `<select>`).
     * Restreint aux entités juridiques en exploitation
     * ({@code CfgCompany.isActive}).
     */
    public List<ReferentielItemDto> tranches(UUID projetId) {
        List<CfgTranche> tranches = projetId != null
                ? trancheRepository.findDesEntitesActivesParProjet(projetId)
                : trancheRepository.findDesEntitesActives();
        return tranches.stream().map(t -> new ReferentielItemDto(
                t.getId(), t.getCode(), t.getDescription(), null)).toList();
    }

    /**
     * {@code GET /api/referentiels/articles} — dropdown "Article" scopé à une tranche
     * (`trancheId` obligatoire, pour éviter de charger tout le parc), filtré sur la disponibilité
     * (défaut: DISPONIBLE) et optionnellement sur la catégorie/le type — même cascade de cases à
     * cocher (Tranche → Catégorie → Type → Article) que {@code GET /api/tranches/{id}/articles}.
     * N'inclut que les articles racine (pas les sous-articles — ceux-ci ont leur propre dropdown,
     * voir {@link #sousArticles}), et écarte les <b>catégories annexes</b> (parking, terrasse,
     * jardin — {@link StkItemSpecifications#CATEGORIES_ANNEXES}) : jamais un lot principal sur
     * les 358 dossiers réels, mais 241 parkings racine noyaient la sélection. L'exclusion tombe
     * dès qu'une {@code categorieId} est demandée explicitement — cocher PARKING dans la cascade
     * doit rendre les parkings, pas une liste vide. Renvoie {@link ArticleListItemDto} (pas le
     * générique {@link ReferentielItemDto}) pour exposer le nom (`description`) et la
     * catégorie/le type de chaque article, pas seulement son code.
     */
    public List<ArticleListItemDto> articles(UUID trancheId, StatutArticle statut,
                                              List<UUID> categorieIds, List<UUID> typeIds) {
        if (!trancheRepository.existsById(trancheId)) {
            throw new ResourceNotFoundException("Tranche introuvable: " + trancheId);
        }
        StatutArticle effectiveStatut = statut != null ? statut : StatutArticle.DISPONIBLE;
        var spec = StkItemSpecifications.build(trancheId, null, true, effectiveStatut, categorieIds, typeIds);
        if (categorieIds == null || categorieIds.isEmpty()) {
            spec = spec.and(StkItemSpecifications.horsCategoriesAnnexes());
        }
        return stkItemRepository.findAll(spec).stream()
                .map(item -> ArticleService.toListItem(item, Collections.emptyMap(), Collections.emptyMap()))
                .toList();
    }

    /**
     * {@code GET /api/referentiels/sous-articles} — dropdown "Sous-article" scopé à un article
     * parent (`articleId` obligatoire), même filtres (disponibilité, catégorie, type) que
     * {@link #articles} / {@code GET /api/articles/{id}/sous-articles}. Chaque entrée porte son
     * `description` (le nom du sous-article), pas seulement son `code`.
     */
    public List<ArticleListItemDto> sousArticles(UUID articleId, StatutArticle statut,
                                                  List<UUID> categorieIds, List<UUID> typeIds) {
        if (!stkItemRepository.existsById(articleId)) {
            throw new ResourceNotFoundException("Article introuvable: " + articleId);
        }
        StatutArticle effectiveStatut = statut != null ? statut : StatutArticle.DISPONIBLE;
        var spec = StkItemSpecifications.build(null, articleId, false, effectiveStatut, categorieIds, typeIds);
        return stkItemRepository.findAll(spec).stream()
                .map(item -> ArticleService.toListItem(item, Collections.emptyMap(), Collections.emptyMap()))
                .toList();
    }

    private static PaysDto toPaysDto(AdmCountry c) {
        return new PaysDto(c.getId(), c.getCode(), c.getIsoAlpha3(), c.getDescription(), parseIntOrNull(c.getPhoneCode()));
    }

    private static VilleDto toVilleDto(AdmCity c) {
        return new VilleDto(c.getId(), c.getCode(), c.getDescription(),
                c.getAdmCountry() != null ? c.getAdmCountry().getId() : null);
    }

    private static Integer parseIntOrNull(String value) {
        if (!StringUtils.hasText(value)) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

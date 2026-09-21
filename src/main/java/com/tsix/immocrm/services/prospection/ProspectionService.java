package com.tsix.immocrm.services.prospection;

import com.tsix.immocrm.dto.prospect.CreerProspectRequest;
import com.tsix.immocrm.dto.prospect.HistoriqueDto;
import com.tsix.immocrm.dto.prospection.ArchiverProspectionRequest;
import com.tsix.immocrm.dto.prospection.ChangerEtapeRequest;
import com.tsix.immocrm.dto.prospection.CreerInteractionRequest;
import com.tsix.immocrm.dto.prospection.CreerProspectionRequest;
import com.tsix.immocrm.dto.prospection.InteractionDto;
import com.tsix.immocrm.dto.prospection.ModifierProspectionRequest;
import com.tsix.immocrm.dto.prospection.ModifierTranchePrincipaleRequest;
import com.tsix.immocrm.domain.AgregatDemande;
import com.tsix.immocrm.domain.Chemin;
import com.tsix.immocrm.domain.ColonneProspection;
import com.tsix.immocrm.domain.Granularite;
import com.tsix.immocrm.domain.TriGroupe;
import com.tsix.immocrm.domain.MarqueurTriProspection;
import com.tsix.immocrm.dto.prospection.ActiviteProspectionDto;
import com.tsix.immocrm.dto.prospection.FiltresAvancesProspection;
import com.tsix.immocrm.dto.prospection.ProspectionDetailDto;
import com.tsix.immocrm.dto.prospection.ProspectionListItemDto;
import com.tsix.immocrm.dto.prospection.TransformerProspectionRequest;
import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.adm.AdmEvent;
import com.tsix.immocrm.entities.adm.AdmSequence;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierType;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComAction;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComActionMessageSubCategory;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComSaleProspectionView;
import com.tsix.immocrm.entities.com.ComProspectionClosingReason;
import com.tsix.immocrm.entities.com.ComProspectionKind;
import com.tsix.immocrm.entities.com.ComProspectionOrigin;
import com.tsix.immocrm.entities.com.ComProspectionOriginSupport;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.str.StrEntity;
import com.tsix.immocrm.entities.str.StrHistory;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.mail.VisiteEffectuee;
import com.tsix.immocrm.mail.VisitePlanifiee;
import com.tsix.immocrm.repositories.adm.AdmEventRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository.DerniereAlerteProjection;
import com.tsix.immocrm.repositories.com.ComActionMessageCategoryRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageSubCategoryRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageTypeRepository;
import com.tsix.immocrm.repositories.com.ComActionRepository;
import com.tsix.immocrm.repositories.com.ComProspectionClosingReasonRepository;
import com.tsix.immocrm.repositories.com.ComProspectionKindRepository;
import com.tsix.immocrm.repositories.com.ComProspectionOriginRepository;
import com.tsix.immocrm.repositories.com.ComProspectionOriginSupportRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.com.ComSaleProspectionViewRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.repositories.str.StrEntityRepository;
import com.tsix.immocrm.repositories.str.StrHistoryRepository;
import com.tsix.immocrm.dto.common.GroupeDto;
import com.tsix.immocrm.services.groupement.GroupementService;
import com.tsix.immocrm.services.groupement.GroupementService.ResultatGroupement;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.services.prospect.ProspectService;
import com.tsix.immocrm.services.sequence.GenerateurCodeSequence;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.Requis;
import com.tsix.immocrm.utils.TierNomFormatter;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ProspectionService {

    /**
     * {@code ComActionMessageSubCategory.Code} values (under the "Prospection" category) that
     * require a "Date du rendez-vous" and create a calendar entry ({@link AdmEvent}) — confirmed
     * empirically against real data. Covers both actual rendez-vous ("Rendez vous pris"/"RDV
     * reporté") and planned site visits ("Le prospect veut faire une visite du projet", 5708 real
     * uses) — same underlying mechanism, distinguished on the calendar by
     * {@code AdmEvent.comActionMessageSubCategory}. See {@code GET /api/calendrier/evenements}.
     */
    private static final Set<String> RESULTATS_AVEC_DATE_PLANIFIEE = Set.of(
            "Rendez vous pris", "RDV reporté", "Le prospect veut faire une visite du projet");

    /**
     * {@code ComProspectionKind.Code} de l'étape qui déclenche le remerciement d'après-visite —
     * voir {@code instructions/FEATURE-offre-apres-visite.md}.
     *
     * <p>C'est bien l'étape et non un résultat d'interaction : sur les 44 152 interactions réelles,
     * les résultats {@code Réalisée} et {@code Non réalisée} comptent <b>zéro</b> ligne. 1 266
     * prospections portent cette étape, dont 99,7 % joignables sur WhatsApp.
     */
    static final String ETAPE_APRES_VISITE = "4 APRES VISITES";

    /**
     * Format du libellé {@code ComActionMessage.Description}, repris à l'identique de
     * l'application de bureau : {@code 07/09/2026 17:13 Khadija ELKORRI}. Vérifié sur les
     * 46 546 interactions de {@code yesRemsProd}, sans une seule exception.
     */
    private static final DateTimeFormatter FORMAT_LIBELLE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ComProspectionRepository prospectionRepository;
    /**
     * Vue légataire fournissant le bloc « activité » de la fiche — voir
     * {@code instructions/FEATURE-vues-legacy-prospection.md}. Lecture seule, une ligne à la fois
     * (26 ms) : ne jamais l'utiliser pour une liste.
     */
    private final ComSaleProspectionViewRepository prospectionViewRepository;
    /**
     * Référentiel des types de document du légataire (162 lignes) — voir
     * {@code instructions/FEATURE-strentity-prospection.md}. Résout {@code StrEntityId} en code et
     * libellé, et sert le filtre {@code ?typeProspection=}.
     */
    private final StrEntityRepository strEntityRepository;
    private final ProspectService prospectService;
    private final CfgTierRepository tierRepository;
    private final ComProspectionKindRepository prospectionKindRepository;
    private final ComProspectionClosingReasonRepository closingReasonRepository;
    private final ComActionMessageRepository actionMessageRepository;
    private final ComProspectionOriginRepository prospectionOriginRepository;
    private final ComProspectionOriginSupportRepository prospectionOriginSupportRepository;
    private final StkVocationRepository vocationRepository;
    private final ComActionMessageTypeRepository actionMessageTypeRepository;
    private final ComActionMessageSubCategoryRepository actionMessageSubCategoryRepository;
    private final ComActionMessageCategoryRepository actionMessageCategoryRepository;

    /**
     * {@code ComActionMessageCategory.Code} porté par les interactions de création — 2 250 des
     * 2 634 mesurées. Les 384 autres sont catégorisées « Dossier de vente (Réservation) » et
     * relèvent de la transformation en vente, pas de la création.
     */
    private static final String CATEGORIE_PROSPECTION = "Prospection";

    /**
     * Type de document du legataire pour une prospection de vente — {@code StrEntity.Code}.
     * Voir {@code instructions/FEATURE-strentity-prospection.md}.
     */
    private static final String CODE_STR_ENTITY_PROSPECTION_VENTE = "ComProspection";

    /**
     * Repli si le referentiel ne repond pas. Identique dans yesRems, yesRemsProd et yesRemsTest —
     * c'est du referentiel legataire restaure de la meme source, pas une valeur inventee ici.
     */
    private static final UUID ID_STR_ENTITY_PROSPECTION_VENTE =
            UUID.fromString("E7C73F70-1111-40D9-AA78-A2825C42B601");
    private final AdmEventRepository admEventRepository;
    private final ComActionRepository actionRepository;
    private final CfgTrancheRepository trancheRepository;
    private final StkItemRepository stkItemRepository;
    private final StrHistoryRepository historyRepository;
    private final GenerateurCodeSequence generateurCode;
    private final PerimetreCommercialService perimetreService;
    private final GroupementService groupementService;
    /** Résout l'auteur d'une interaction depuis le jeton — voir {@code FIX-interaction-note.md}. */
    private final AdmUserRepository userRepository;
    /**
     * Publie {@link VisitePlanifiee} — voir {@code FEATURE-mail-confirmation-visite.md}. Un
     * événement plutôt qu'un appel direct : le mail ne doit partir que si l'interaction est
     * réellement écrite, et l'envoi ne doit rien pouvoir annuler.
     */
    private final ApplicationEventPublisher publisher;

    public ProspectionService(ComProspectionRepository prospectionRepository, ProspectService prospectService,
                               CfgTierRepository tierRepository, ComProspectionKindRepository prospectionKindRepository,
                               ComProspectionClosingReasonRepository closingReasonRepository,
                               ComActionMessageRepository actionMessageRepository,
                               ComProspectionOriginRepository prospectionOriginRepository,
                               ComProspectionOriginSupportRepository prospectionOriginSupportRepository,
                               StkVocationRepository vocationRepository,
                               ComActionMessageTypeRepository actionMessageTypeRepository,
                               ComActionMessageSubCategoryRepository actionMessageSubCategoryRepository,
                               ComActionMessageCategoryRepository actionMessageCategoryRepository,
                               AdmEventRepository admEventRepository,
                               ComActionRepository actionRepository,
                               CfgTrancheRepository trancheRepository,
                               StkItemRepository stkItemRepository,
                               StrHistoryRepository historyRepository,
                               GenerateurCodeSequence generateurCode,
                               PerimetreCommercialService perimetreService,
                               GroupementService groupementService,
                               AdmUserRepository userRepository,
                               ComSaleProspectionViewRepository prospectionViewRepository,
                               StrEntityRepository strEntityRepository,
                               ApplicationEventPublisher publisher) {
        this.prospectionViewRepository = prospectionViewRepository;
        this.strEntityRepository = strEntityRepository;
        this.publisher = publisher;
        this.userRepository = userRepository;
        this.perimetreService = perimetreService;
        this.groupementService = groupementService;
        this.prospectionRepository = prospectionRepository;
        this.prospectService = prospectService;
        this.tierRepository = tierRepository;
        this.prospectionKindRepository = prospectionKindRepository;
        this.closingReasonRepository = closingReasonRepository;
        this.actionMessageRepository = actionMessageRepository;
        this.prospectionOriginRepository = prospectionOriginRepository;
        this.prospectionOriginSupportRepository = prospectionOriginSupportRepository;
        this.vocationRepository = vocationRepository;
        this.actionMessageTypeRepository = actionMessageTypeRepository;
        this.actionMessageSubCategoryRepository = actionMessageSubCategoryRepository;
        this.actionMessageCategoryRepository = actionMessageCategoryRepository;
        this.admEventRepository = admEventRepository;
        this.actionRepository = actionRepository;
        this.trancheRepository = trancheRepository;
        this.stkItemRepository = stkItemRepository;
        this.historyRepository = historyRepository;
        this.generateurCode = generateurCode;
    }

    /**
     * {@code GET /api/prospections}. Excludes pré-prospections (CfgProspectId IS NULL AND
     * CfgCommercialId IS NULL) — those are exposed separately by
     * {@link #findPreProspections(List, List, List, List, LocalDateTime, LocalDateTime, String, Boolean, Pageable)}
     * to avoid the two endpoints returning overlapping rows.
     *
     * @param etapes     {@code ComProspectionKind.code} filter ("Nature") — OR-match, repeated
     *                   query param, 0/1/2+ values behave like {@code DossierService.findAll}'s
     *                   {@code statuts}. See {@code instructions/filtres-avances.md}.
     * @param origines   {@code ComProspectionOrigin.code} filter — same OR-match convention.
     * @param commerciaux {@code CfgTier.id} (commercial) filter — same OR-match convention.
     * @param trancheIds {@code CfgTranche.id} filter ({@code ComProspection.stkHierarchy} — the
     *                   tranche visée), OR-match. Unlike Dossier, this is a direct FK on
     *                   {@code ComProspection}, no EXISTS subquery needed.
     * @param dateDebut  inclusive lower bound on {@code docDate} (only date field exposed by
     *                   {@link ProspectionListItemDto} today).
     * @param dateFin    inclusive upper bound on {@code docDate}.
     * @param search     free-text match on code/prospect name/temp contact fields
     * @param ouvertes   when true, only non-closed (IsClosed = false) prospections
     */
    public Page<ProspectionListItemDto> findAll(List<String> etapes, List<String> origines, List<UUID> commerciaux,
            List<UUID> trancheIds, LocalDateTime dateDebut, LocalDateTime dateFin, String search,
            Boolean ouvertes, FiltresAvancesProspection filtres, Set<ColonneProspection> colonnes,
            ColonneProspection groupeColonne, String groupeValeur, Granularite granularite,
            Pageable pageable) {
        Specification<ComProspection> spec = specProspections(etapes, origines, commerciaux, trancheIds,
                dateDebut, dateFin, search, ouvertes, filtres)
                // Périmètre commercial — prédicat neutre si l'appelant n'est pas restreint.
                .and(perimetreService.filtreProspections())
                .and(filtreDeGroupe(groupeColonne, groupeValeur, granularite));
        return versListe(prospectionRepository.findAll(spec, pageable), colonnes);
    }

    /**
     * {@code GET /api/pre-prospections}: CfgProspectId IS NULL AND CfgCommercialId IS NULL. Same
     * filter set as {@link #findAll} — see {@code instructions/filtres-avances.md} point 5.
     */
    public Page<ProspectionListItemDto> findPreProspections(List<String> etapes, List<String> origines,
            List<UUID> commerciaux, List<UUID> trancheIds, LocalDateTime dateDebut, LocalDateTime dateFin,
            String search, Boolean ouvertes, ColonneProspection groupeColonne, String groupeValeur,
            Granularite granularite, Pageable pageable) {
        Specification<ComProspection> spec = specPreProspections(etapes, origines, commerciaux, trancheIds,
                dateDebut, dateFin, search, ouvertes)
                // Périmètre commercial : une pré-prospection a par construction un commercial NULL
                // (0 sur 2800 en base réelle), donc seul le projet peut la scoper.
                .and(perimetreService.filtreProspections())
                .and(filtreDeGroupe(groupeColonne, groupeValeur, granularite));
        // Sélection par défaut, sans discussion : ComSaleProspectionView exclut par construction
        // les pré-prospections, donc aucune colonne d'activité ne peut être servie ici. Demander
        // la vue reviendrait à payer une requête pour n'obtenir que des null.
        return versListe(prospectionRepository.findAll(spec, pageable), ColonneProspection.parDefautSet());
    }

    /**
     * Replie {@code GET /api/prospections} en groupes — voir
     * {@code instructions/FEATURE-groupement-listes.md}.
     */
    public ResultatGroupement grouperProspections(List<String> etapes, List<String> origines,
            List<UUID> commerciaux, List<UUID> trancheIds, LocalDateTime dateDebut, LocalDateTime dateFin,
            String search, Boolean ouvertes, FiltresAvancesProspection filtres,
            ColonneProspection groupeBy, Granularite granularite, List<AgregatDemande> agregats,
            TriGroupe tri, boolean inclureSansValeur, Pageable pageable) {
        return grouper(specProspections(etapes, origines, commerciaux, trancheIds, dateDebut, dateFin,
                search, ouvertes, filtres), groupeBy, granularite, agregats, tri, inclureSansValeur, pageable);
    }

    /** Replie {@code GET /api/pre-prospections} en groupes, même discriminant que la liste. */
    public ResultatGroupement grouperPreProspections(List<String> etapes, List<String> origines,
            List<UUID> commerciaux, List<UUID> trancheIds, LocalDateTime dateDebut, LocalDateTime dateFin,
            String search, Boolean ouvertes, ColonneProspection groupeBy, Granularite granularite,
            List<AgregatDemande> agregats, TriGroupe tri, boolean inclureSansValeur, Pageable pageable) {
        return grouper(specPreProspections(etapes, origines, commerciaux, trancheIds, dateDebut, dateFin,
                search, ouvertes), groupeBy, granularite, agregats, tri, inclureSansValeur, pageable);
    }

    private ResultatGroupement grouper(Specification<ComProspection> spec, ColonneProspection groupeBy,
            Granularite granularite, List<AgregatDemande> agregats, TriGroupe tri,
            boolean inclureSansValeur, Pageable pageable) {
        return groupementService.grouper(ComProspection.class,
                spec.and(perimetreService.filtreProspections()),
                groupeBy.chemin(), granularite, agregats, tri, inclureSansValeur, pageable);
    }

    /**
     * Prédicat de dépliage, ou prédicat neutre si aucun groupe n'est demandé.
     *
     * <p>Neutre plutôt que {@code null} : {@code Specification.and(null)} est licite mais rendrait
     * la chaîne d'appel illisible dès qu'on en enchaîne plusieurs.
     */
    private static Specification<ComProspection> filtreDeGroupe(ColonneProspection groupeColonne,
            String groupeValeur, Granularite granularite) {
        if (groupeColonne == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> GroupementService.predicatDeGroupe(
                root, cb, groupeColonne.chemin(), granularite, groupeValeur);
    }

    /**
     * Filtres de {@code GET /api/prospections}, périmètre commercial <strong>exclu</strong> — les
     * appelants l'ajoutent explicitement, ce qui rend son absence visible à la lecture plutôt que
     * cachée dans une composition. Partagé avec le groupement pour que la liste et ses groupes
     * portent exactement les mêmes lignes.
     */
    private static Specification<ComProspection> specProspections(List<String> etapes, List<String> origines,
            List<UUID> commerciaux, List<UUID> trancheIds, LocalDateTime dateDebut, LocalDateTime dateFin,
            String search, Boolean ouvertes, FiltresAvancesProspection filtres) {
        return buildSpec(etapes, origines, commerciaux, trancheIds, dateDebut, dateFin, search, ouvertes)
                .and(filtresAvances(filtres))
                .and((root, query, cb) -> cb.or(
                        cb.isNotNull(root.get("cfgProspect")),
                        cb.isNotNull(root.get("cfgCommercial"))
                ));
    }

    /** Idem pour les pré-prospections : le discriminant est l'inverse exact du précédent. */
    private static Specification<ComProspection> specPreProspections(List<String> etapes, List<String> origines,
            List<UUID> commerciaux, List<UUID> trancheIds, LocalDateTime dateDebut, LocalDateTime dateFin,
            String search, Boolean ouvertes) {
        return buildSpec(etapes, origines, commerciaux, trancheIds, dateDebut, dateFin, search, ouvertes)
                .and((root, query, cb) -> cb.and(
                        cb.isNull(root.get("cfgProspect")),
                        cb.isNull(root.get("cfgCommercial"))
                ));
    }

    /** Shared filter predicates for {@link #findAll} and {@link #findPreProspections}. */
    private static Specification<ComProspection> buildSpec(List<String> etapes, List<String> origines,
            List<UUID> commerciaux, List<UUID> trancheIds, LocalDateTime dateDebut, LocalDateTime dateFin,
            String search, Boolean ouvertes) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (etapes != null && !etapes.isEmpty()) {
                List<String> etapesLower = etapes.stream().map(String::toLowerCase).toList();
                predicates.add(cb.lower(root.get("comProspectionKind").get("code")).in(etapesLower));
            }
            if (origines != null && !origines.isEmpty()) {
                List<String> originesLower = origines.stream().map(String::toLowerCase).toList();
                predicates.add(cb.lower(root.get("comProspectionOrigin").get("code")).in(originesLower));
            }
            if (commerciaux != null && !commerciaux.isEmpty()) {
                predicates.add(root.get("cfgCommercial").get("id").in(commerciaux));
            }
            if (trancheIds != null && !trancheIds.isEmpty()) {
                predicates.add(root.get("stkHierarchy").get("id").in(trancheIds));
            }
            if (dateDebut != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("docDate"), dateDebut));
            }
            if (dateFin != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("docDate"), dateFin));
            }
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), pattern),
                        cb.like(cb.lower(root.get("tempDescription")), pattern),
                        cb.like(cb.lower(root.get("tempTel")), pattern),
                        cb.like(cb.lower(root.get("tempEMail")), pattern)
                ));
            }
            if (ouvertes != null && ouvertes) {
                predicates.add(cb.or(cb.isFalse(root.get("isClosed")), cb.isNull(root.get("isClosed"))));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Filtres du lot C — voir {@link FiltresAvancesProspection}.
     *
     * <p>Prédicat neutre quand aucun n'est renseigné : {@code findPreProspections} et les appels
     * historiques ne changent pas de comportement.
     */
    private static Specification<ComProspection> filtresAvances(FiltresAvancesProspection f) {
        return (root, query, cb) -> {
            if (f == null) {
                return cb.conjunction();
            }
            List<Predicate> predicates = new ArrayList<>();
            // -- Portés par l'entité. Chacun traverse une association : quand le filtre est actif,
            //    la jointure devient interne et écarte les lignes sans valeur, ce qui est le
            //    comportement attendu d'un filtre (une prospection sans tranche n'a pas de projet).
            if (estRenseignee(f.projetId())) {
                predicates.add(root.get("stkHierarchy").get("cfgProject").get("id").in(f.projetId()));
            }
            if (estRenseignee(f.societeId())) {
                predicates.add(root.get("stkHierarchy").get("cfgProject").get("cfgCompany").get("id")
                        .in(f.societeId()));
            }
            if (estRenseignee(f.vocationId())) {
                predicates.add(root.get("stkVocation").get("id").in(f.vocationId()));
            }
            if (estRenseignee(f.typeContactId())) {
                predicates.add(root.get("comActionMessageType").get("id").in(f.typeContactId()));
            }
            if (estRenseignee(f.raisonFermetureId())) {
                predicates.add(root.get("comProspectionClosingReason").get("id").in(f.raisonFermetureId()));
            }
            if (estRenseignee(f.paysId())) {
                predicates.add(root.get("admCountry").get("id").in(f.paysId()));
            }
            if (estRenseignee(f.actionId())) {
                predicates.add(root.get("comAction").get("id").in(f.actionId()));
            }
            if (estRenseignee(f.origineSupportId())) {
                predicates.add(root.get("comProspectionOriginSupport").get("id").in(f.origineSupportId()));
            }
            if (f.ancienClient() != null) {
                predicates.add(f.ancienClient()
                        ? cb.isTrue(root.get("isOldCustomer"))
                        : cb.or(cb.isFalse(root.get("isOldCustomer")), cb.isNull(root.get("isOldCustomer"))));
            }
            // -- Type de document légataire. Sous-requête sur StrEntity plutôt que résolution
            //    préalable des codes en identifiants : ça garde la méthode statique, et surtout ça
            //    évite qu'un code inconnu doive être distingué d'un code sans correspondance — les
            //    deux rendent un résultat vide, ce qui est le comportement voulu.
            //    Une prospection sans StrEntityId est écartée d'office (NULL IN (…) est inconnu),
            //    ce qui est correct : elle n'a pas de type.
            if (estRenseignee(f.typeProspection())) {
                Subquery<UUID> types = query.subquery(UUID.class);
                Root<StrEntity> typeRoot = types.from(StrEntity.class);
                types.select(typeRoot.get("id")).where(typeRoot.get("code").in(f.typeProspection()));
                predicates.add(root.get("strEntityId").in(types));
            }
            // -- Portés par la vue : une seule sous-requête, quel que soit le nombre de critères.
            if (f.exigeLaVue()) {
                predicates.add(root.get("id").in(sousRequeteVue(f, query, cb)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Sous-requête sur {@code ComSaleProspectionView} portant les critères calculés.
     *
     * <p>C'est le compromis retenu après mesure : la liste reste servie par {@code ComProspection}
     * — table indexée, pagination profonde, périmètre commercial inchangés — et seul le prédicat
     * traverse la vue. Coût mesuré : <strong>231 ms page 1, 302 ms à l'{@code OFFSET 5000}</strong>,
     * contre 100/134 ms sans filtre de cette famille, et 515 ms dès la page 1 si la liste elle-même
     * était servie depuis la vue.
     *
     * <p>Effet de bord assumé : la vue exclut les pré-prospections et les lignes sans
     * {@code StrEntityId}. Activer un de ces filtres les écarte donc toutes — ce qui est cohérent,
     * ces lignes n'ayant par construction aucun indicateur d'activité.
     */
    private static Subquery<UUID> sousRequeteVue(FiltresAvancesProspection f,
                                                 CriteriaQuery<?> query, CriteriaBuilder cb) {
        Subquery<UUID> sousRequete = query.subquery(UUID.class);
        Root<ComSaleProspectionView> vue = sousRequete.from(ComSaleProspectionView.class);
        sousRequete.select(vue.get("id"));

        List<Predicate> criteres = new ArrayList<>();
        if (f.inactiviteMin() != null) {
            criteres.add(cb.greaterThanOrEqualTo(vue.get("inactivityTimeout"), f.inactiviteMin()));
        }
        if (f.nbInteractionsMin() != null) {
            criteres.add(cb.greaterThanOrEqualTo(vue.get("actionsMessageNumber"), f.nbInteractionsMin()));
        }
        if (f.nbVisitesMin() != null) {
            criteres.add(cb.greaterThanOrEqualTo(vue.get("nbrVisit"), f.nbVisitesMin()));
        }
        if (f.communicationDepuis() != null) {
            criteres.add(cb.greaterThanOrEqualTo(vue.get("lastCommunicationDate"), f.communicationDepuis()));
        }
        if (f.communicationJusqua() != null) {
            criteres.add(cb.lessThanOrEqualTo(vue.get("lastCommunicationDate"), f.communicationJusqua()));
        }
        if (f.avecAlerteActive() != null) {
            criteres.add(f.avecAlerteActive()
                    ? cb.isNotNull(vue.get("lastActiveAlertDate"))
                    : cb.isNull(vue.get("lastActiveAlertDate")));
        }
        sousRequete.where(cb.and(criteres.toArray(new Predicate[0])));
        return sousRequete;
    }

    /**
     * Générique plutôt que typée {@code List<UUID>} : le filtre de type de document porte des
     * codes ({@code List<String>}), et dupliquer la méthode pour une différence de paramètre de
     * type aurait été le genre de copie qui finit par diverger.
     */
    private static boolean estRenseignee(List<?> valeurs) {
        return valeurs != null && !valeurs.isEmpty();
    }

    public ProspectionDetailDto findById(UUID id) {
        return prospectionRepository.findById(id)
                .map(this::detailEnrichi)
                .orElseThrow(() -> new ResourceNotFoundException("Prospection introuvable: " + id));
    }

    /**
     * Construit le détail depuis {@link ComProspection}, puis le complète du bloc « activité »
     * calculé par {@code ComSaleProspectionView}.
     *
     * <p>Une requête de plus, mesurée à 26 ms sur la base réelle. Quand la vue ne rend rien — cas
     * systématique des <strong>pré-prospections</strong>, qu'elle exclut par construction — le
     * détail est renvoyé tel quel avec {@code activite = null}. Ce n'est pas une erreur : les
     * pré-prospections ont leur propre vue, et un 404 ici serait faux.
     */
    private ProspectionDetailDto detailEnrichi(ComProspection p) {
        ActiviteProspectionDto activite = prospectionViewRepository.findById(p.getId())
                .map(ProspectionService::toActivite)
                .orElse(null);
        // Contrairement à la liste, la fiche sert toujours le type : il n'y a pas de sélection de
        // colonnes ici, et la requête porte sur une clé unique d'une table de 162 lignes. Reste
        // null sur les 4 prospections dépourvues de StrEntityId — celles-là mêmes qui sont
        // invisibles du logiciel de bureau.
        StrEntity type = p.getStrEntityId() == null
                ? null
                : strEntityRepository.findById(p.getStrEntityId()).orElse(null);
        return toDetail(p, activite, type, resoudreDoublon(p.getTempField10()));
    }

    /**
     * Suit le pointeur de doublon {@code ComProspection.TempField10}, qui porte un
     * {@code CfgTier.Code} — 849 valeurs réelles, 849 résolues.
     *
     * <p><b>Une requête de plus, mais seulement sur moins de 5 % des fiches</b> : le champ est vide
     * partout ailleurs, et on sort avant d'interroger quoi que ce soit.
     *
     * <p><b>{@code null} sur une correspondance multiple, délibérément.</b> {@code CfgTier.Code}
     * n'est pas unique (14 516 tiers pour 14 504 codes distincts). Aucun des 849 pointeurs réels
     * n'est ambigu aujourd'hui, mais prendre le premier de plusieurs reviendrait à désigner un
     * tiers au hasard, et le front l'afficherait comme un fait. Le code brut reste servi par
     * {@code doublonDeCode}, donc l'information n'est pas perdue — elle est seulement présentée
     * comme non résolue, ce qu'elle est.
     */
    private CfgTier resoudreDoublon(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        List<CfgTier> trouves = tierRepository.findByCode(code.trim());
        return trouves.size() == 1 ? trouves.getFirst() : null;
    }

    /**
     * Traduit la ligne de vue en bloc d'activité.
     *
     * <p>Trois colonnes de la vue rendent une <strong>chaîne vide</strong> plutôt que
     * {@code NULL} quand il n'y a rien à dire : elles sont non-{@code NULL} sur 100 % des lignes
     * mais non-vides sur 90 % (résultat et message de communication) et 25 %
     * ({@code LastActiveAlertReason}) seulement. Sans la normalisation ci-dessous, l'API
     * répondrait {@code ""} là où elle doit répondre {@code null}, et le front afficherait un
     * libellé vide au lieu de masquer le champ.
     */
    private static ActiviteProspectionDto toActivite(ComSaleProspectionView v) {
        return new ActiviteProspectionDto(
                v.getInactivityTimeout(),
                v.getActionsMessageNumber(),
                v.getLastActionDate(),
                v.getLastCommunicationDate(),
                videEnNull(v.getLastCommunicationResult()),
                videEnNull(v.getLastCommunicationMessage()),
                v.getLastActiveAlertDate(),
                videEnNull(v.getLastActiveAlertReason()),
                v.getNbrVisit(),
                v.getFirstVisit(),
                v.getLastVisitDate(),
                v.getCfgProjectId(),
                videEnNull(v.getCfgProjectCode()),
                videEnNull(v.getCfgProjectDescription()),
                videEnNull(v.getCfgTierIdNumber()),
                v.getComFolderId(),
                videEnNull(v.getComFolderCode()),
                v.getCfgCustomerId(),
                videEnNull(v.getStatus())
        );
    }

    /** {@code null} pour une chaîne nulle ou blanche — voir {@link #toActivite}. */
    private static String videEnNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    /**
     * Champ de saisie libre : {@code null} si vide, espaces de bordure retirés sinon.
     *
     * <p>Le contenu est servi tel quel — on ne corrige ni la casse ni l'orthographe, puisque c'est
     * justement ce qu'on ne sait pas interpréter. Mais les espaces de bordure ne portent aucune
     * information et se voient à l'affichage : {@code TempField1} contient réellement des valeurs
     * comme {@code "  TRAITÉ  "}. Les laisser passer obligerait chaque client à les retirer.
     *
     * <p>Distinct de {@link #videEnNull} qui, lui, ne touche pas au contenu : celui-là sert aux
     * colonnes de vue, où la chaîne vide est un artefact de la base et non une saisie.
     */
    private static String texteLibre(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /**
     * {@code GET /api/prospections/{id}/historique}: piste d'audit complète du parcours d'un lead
     * — {@code StrHistory} filtré par {@code ObjectId = prospectionId}, même mécanisme que
     * {@code GET /api/prospects/{id}/historique} et {@code GET /api/dossiers/{id}/historique}.
     * Comble une lacune constatée: avant cet endpoint, rien ne traçait les dates de "devenu
     * prospect" ({@link #creerProspect}) ni "devenu prospection" ({@link #transformer}) — la même
     * ligne {@code ComProspection} était mutée en place sans horodatage, y compris côté Omran
     * (confirmé: {@code ComProspection.UserUpdateDateTime} reste toujours {@code NULL} en base
     * réelle, même sur des lignes déjà transformées). La date de création du pré-prospect reste
     * disponible via {@code ProspectionDetailDto.dateDocument} (pas de doublon ici, aucun
     * endpoint ne permettant de créer une {@code ComProspection} pour intercepter cet évènement).
     * Le stade "Dossier de vente" a son propre historique — voir
     * {@code GET /api/dossiers/{id}/historique}.
     */
    public Page<HistoriqueDto> historique(UUID prospectionId, Pageable pageable) {
        if (!prospectionRepository.existsById(prospectionId)) {
            throw new ResourceNotFoundException("Prospection introuvable: " + prospectionId);
        }
        return historyRepository.findByObjectIdOrderByDocDateDesc(prospectionId, pageable)
                .map(ProspectionService::toHistoriqueDto);
    }

    private static HistoriqueDto toHistoriqueDto(StrHistory h) {
        return new HistoriqueDto(
                h.getId(),
                h.getDocDate(),
                h.getDescription(),
                h.getAdmUserDescription(),
                h.getStrWorkflowDescription(),
                h.getStrStatusDescription()
        );
    }

    /**
     * Écrit une ligne {@code StrHistory} pour une transition du parcours pré-prospect → prospect
     * → prospection, consultable via {@link #historique}. Voir {@link #creerProspect} et
     * {@link #transformer}.
     */
    private void enregistrerHistorique(UUID prospectionId, String description) {
        StrHistory historique = new StrHistory();
        historique.setId(UUID.randomUUID());
        historique.setDocDate(LocalDateTime.now());
        historique.setObjectId(prospectionId);
        historique.setDescription(description);
        historyRepository.save(historique);
    }

    /**
     * {@code POST /api/prospections}: creates a fully-qualified prospection in one call, for a
     * prospect that already exists — see {@link CreerProspectionRequest}'s javadoc for how this
     * differs from the pré-prospection flow. {@code prospectId} is required; every other field is
     * optional and validated for existence only when provided, same convention as
     * {@link #modifierProspection}.
     */
    @Transactional
    public ProspectionDetailDto creerProspection(CreerProspectionRequest request) {
        Requis.champ(request.prospectId(), "prospectId est requis");
        CfgTier prospect = tierRepository.findById(request.prospectId())
                .orElseThrow(() -> new InvalidRequestException("Prospect (CfgTier) introuvable: " + request.prospectId()));

        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode(genererCodeProspection());
        prospection.setDocDate(LocalDateTime.now());
        prospection.setCfgProspect(prospect);
        // `IsClosed` à false, et c'est le défaut qui rendait invisible dans le logiciel de
        // bureau toute prospection créée par le CRM : sa vue de travail filtre en
        // `IsClosed = 0`, et `NULL = 0` n'est jamais vrai en SQL. La ligne était pourtant bien
        // écrite — elle n'apparaissait simplement dans aucune liste. Vérifié en comparant une
        // prospection du CRM à une prospection saisie dans le logiciel.
        prospection.setIsClosed(false);
        prospection.setIsOldCustomer(false);
        // Le discriminant de type de document du legataire. Sans lui, la ligne n'existe pour
        // aucun ecran du logiciel de bureau : sa vue de travail l'ecarte par
        // `StrEntityId <> <pre-prospection>` et son ecran refiltre par `StrEntityId = <vente>`,
        // deux comparaisons qu'un NULL ne satisfait jamais. Le manque etait connu et
        // documente sur ComProspection.strEntityId ; il se referme ici.
        prospection.setStrEntityId(idProspectionDeVente());

        if (request.commercialId() != null) {
            CfgTier commercial = tierRepository.findById(request.commercialId())
                    .orElseThrow(() -> new InvalidRequestException("Commercial (CfgTier) introuvable: " + request.commercialId()));
            prospection.setCfgCommercial(commercial);
        }
        if (request.origineId() != null) {
            ComProspectionOrigin origine = prospectionOriginRepository.findById(request.origineId())
                    .orElseThrow(() -> new InvalidRequestException("ComProspectionOrigin introuvable: " + request.origineId()));
            prospection.setComProspectionOrigin(origine);
        }
        if (request.origineSupportId() != null) {
            ComProspectionOriginSupport support = prospectionOriginSupportRepository.findById(request.origineSupportId())
                    .orElseThrow(() -> new InvalidRequestException("ComProspectionOriginSupport introuvable: " + request.origineSupportId()));
            prospection.setComProspectionOriginSupport(support);
        }
        if (request.vocationId() != null) {
            StkVocation vocation = vocationRepository.findById(request.vocationId())
                    .orElseThrow(() -> new InvalidRequestException("StkVocation introuvable: " + request.vocationId()));
            prospection.setStkVocation(vocation);
        }
        if (request.typeContactId() != null) {
            ComActionMessageType typeContact = actionMessageTypeRepository.findById(request.typeContactId())
                    .orElseThrow(() -> new InvalidRequestException("ComActionMessageType introuvable: " + request.typeContactId()));
            prospection.setComActionMessageType(typeContact);
        }
        if (request.actionCommercialeId() != null) {
            ComAction action = actionRepository.findById(request.actionCommercialeId())
                    .orElseThrow(() -> new InvalidRequestException("ComAction introuvable: " + request.actionCommercialeId()));
            prospection.setComAction(action);
        }
        if (request.etapeId() != null) {
            ComProspectionKind kind = prospectionKindRepository.findById(request.etapeId())
                    .orElseThrow(() -> new InvalidRequestException("ComProspectionKind introuvable: " + request.etapeId()));
            prospection.setComProspectionKind(kind);
        }

        // trancheId/articleId: même validation croisée que modifierTranchePrincipale (l'article
        // doit appartenir à la tranche fournie) — voir sa doc pour le raisonnement.
        if (request.trancheId() != null) {
            CfgTranche tranche = trancheRepository.findById(request.trancheId())
                    .orElseThrow(() -> new InvalidRequestException("CfgTranche introuvable: " + request.trancheId()));
            // Périmètre commercial : créer une prospection sur un projet non affecté est refusé (403).
            perimetreService.verifierEcritureSurTranche(tranche,
                    "Création de prospection sur la tranche " + tranche.getCode());
            if (request.articleId() != null) {
                StkItem article = stkItemRepository.findById(request.articleId())
                        .orElseThrow(() -> new InvalidRequestException("Article introuvable: " + request.articleId()));
                if (article.getStkHierarchy() == null || !tranche.getId().equals(article.getStkHierarchy().getId())) {
                    throw new InvalidRequestException(
                            "L'article " + article.getCode() + " n'appartient pas à la tranche " + tranche.getCode());
                }
                prospection.setStkItem(article);
            }
            prospection.setStkHierarchy(tranche);
        } else if (request.articleId() != null) {
            throw new InvalidRequestException("articleId nécessite trancheId");
        }

        if (request.surface() != null) prospection.setArea(request.surface());
        if (request.surfaceJusqua() != null) prospection.setAreaTo(request.surfaceJusqua());
        if (request.prix() != null) prospection.setPrice(request.prix());
        if (request.prixJusqua() != null) prospection.setPriceTo(request.prixJusqua());
        if (request.avance() != null) prospection.setAdvanceAmount(request.avance());
        if (request.financementFondsPropres() != null) prospection.setFundingOwnBase(request.financementFondsPropres());
        if (request.financementCredit() != null) prospection.setFundingCredit(request.financementCredit());
        if (request.financementAutre() != null) prospection.setFundingOther(request.financementAutre());
        if (request.ancienClient() != null) prospection.setIsOldCustomer(request.ancienClient());
        if (request.notes() != null) prospection.setNotes(request.notes());

        prospection = prospectionRepository.save(prospection);
        ecrireInteractionDeCreation(prospection);
        enregistrerHistorique(prospection.getId(), "Prospection créée : prospect " + TierNomFormatter.nom(prospect));
        return detailEnrichi(prospection);
    }

    /**
     * Numbers a new Prospection the same way the legacy app does — confirmed against real data:
     * {@code AdmSequence} (row {@code Code = "Prospection"}) holds the live counter
     * ({@code SeqValue = 5932} at time of writing), format
     * {@code "{année sur 2 chiffres}/{numéro sur 5 chiffres}"} (e.g. "26/05932" — real examples
     * confirmed on {@code ComProspection.Code}). Same mechanism as
     * {@code DossierService#genererCodeDossier}, but with no per-project prefix (a single global
     * counter, no "APF"/"ART"-style prefix on Prospection codes).
     */
    /**
     * Identifiant du type « Prospection vente » dans {@code StrEntity}.
     *
     * <p>Resolu par code plutot que code en dur, pour que le jour ou une base porterait un autre
     * identifiant la resolution suive. Le repli sur la constante couvre le cas inverse — un
     * referentiel incomplet — parce que rendre une prospection invisible en silence serait pire
     * que de la rattacher a l'identifiant connu : c'est le meme dans les trois bases du projet.
     */
    private UUID idProspectionDeVente() {
        return strEntityRepository.findByCodeIn(List.of(CODE_STR_ENTITY_PROSPECTION_VENTE)).stream()
                .findFirst()
                .map(StrEntity::getId)
                .orElse(ID_STR_ENTITY_PROSPECTION_VENTE);
    }

    private String genererCodeProspection() {
        return generateurCode.suivant(GenerateurCodeSequence.SEQUENCE_PROSPECTION, 2, 5);
    }

    /**
     * {@code POST /api/pre-prospections/{id}/creer-prospect}: creates a {@code CfgTier} and
     * links it as the prospection's {@code CfgProspectId}. Only valid on a pré-prospection
     * (CfgProspectId IS NULL) — 400 if the prospection already has a prospect.
     */
    @Transactional
    public ProspectionDetailDto creerProspect(UUID prospectionId, CreerProspectRequest request) {
        ComProspection prospection = prospectionRepository.findById(prospectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prospection introuvable: " + prospectionId));

        if (prospection.getCfgProspect() != null) {
            throw new InvalidRequestException("Cette prospection a déjà un prospect lié: " + prospectionId);
        }

        CfgTier tier = prospectService.createEntity(request);
        prospection.setCfgProspect(tier);
        prospection = prospectionRepository.save(prospection);
        enregistrerHistorique(prospectionId, "Prospect créé et lié : " + TierNomFormatter.nom(tier));
        return detailEnrichi(prospection);
    }

    /**
     * {@code POST /api/prospections/{id}/transformer}: pré-prospection → prospection, by
     * assigning the commercial. Requires a prospect to already be linked (via
     * {@link #creerProspect}) — 400 otherwise.
     */
    @Transactional
    public ProspectionDetailDto transformer(UUID id, TransformerProspectionRequest request) {
        ComProspection prospection = prospectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prospection introuvable: " + id));

        if (prospection.getCfgProspect() == null) {
            throw new InvalidRequestException(
                    "Un prospect doit être créé (POST /api/pre-prospections/{id}/creer-prospect) avant de transformer: " + id);
        }
        Requis.champ(request.commercialId(), "commercialId est requis");

        CfgTier commercial = tierRepository.findById(request.commercialId())
                .orElseThrow(() -> new InvalidRequestException("Commercial (CfgTier) introuvable: " + request.commercialId()));

        prospection.setCfgCommercial(commercial);
        prospection = prospectionRepository.save(prospection);
        enregistrerHistorique(id, "Transformée en prospection : commercial assigné, " + TierNomFormatter.nom(commercial));
        return detailEnrichi(prospection);
    }

    /**
     * {@code PATCH /api/prospections/{id}/etape}: changes {@code ComProspectionKindId}
     * ("Nature"). Only membership is validated — no transition table.
     *
     * <p>Le passage à {@link #ETAPE_APRES_VISITE} publie {@link VisiteEffectuee}, qui déclenche le
     * remerciement au prospect après commit. Aucune règle métier n'est ajoutée ici : le
     * changement d'étape reste libre, seule sa valeur d'arrivée est observée.
     */
    @Transactional
    public ProspectionDetailDto changerEtape(UUID id, ChangerEtapeRequest request) {
        ComProspection prospection = prospectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prospection introuvable: " + id));

        Requis.champ(request.etapeId(), "etapeId est requis");
        ComProspectionKind kind = prospectionKindRepository.findById(request.etapeId())
                .orElseThrow(() -> new InvalidRequestException("ComProspectionKind introuvable: " + request.etapeId()));

        prospection.setComProspectionKind(kind);
        if (request.note() != null) {
            prospection.setNotes(request.note());
        }
        ComProspection enregistree = prospectionRepository.save(prospection);

        // La livraison n'a lieu qu'après commit : une étape annulée ne remercie personne.
        if (estEtapeApresVisite(kind)) {
            publisher.publishEvent(new VisiteEffectuee(enregistree.getId()));
        }
        return detailEnrichi(enregistree);
    }

    /**
     * Le libellé d'étape, comparé sans casse ni espaces de bord. Ce n'est pas de la prudence
     * gratuite : le référentiel voisin des résultats porte des valeurs réellement suffixées d'un
     * espace ({@code "Numéro erroné "}), et une égalité stricte y serait silencieusement fausse.
     */
    private static boolean estEtapeApresVisite(ComProspectionKind kind) {
        return kind != null && kind.getCode() != null
                && ETAPE_APRES_VISITE.equalsIgnoreCase(kind.getCode().trim());
    }

    /**
     * {@code PATCH /api/prospections/{id}}: edits qualification fields (origine, vocation,
     * budget, tranche/article, notes...). Partial update — {@code null} fields are left
     * unchanged, same convention as {@link com.tsix.immocrm.services.prospect.ProspectService#update}.
     */
    @Transactional
    public ProspectionDetailDto modifierProspection(UUID id, ModifierProspectionRequest request) {
        ComProspection prospection = prospectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prospection introuvable: " + id));

        if (request.origineId() != null) {
            ComProspectionOrigin origine = prospectionOriginRepository.findById(request.origineId())
                    .orElseThrow(() -> new InvalidRequestException("ComProspectionOrigin introuvable: " + request.origineId()));
            prospection.setComProspectionOrigin(origine);
        }
        if (request.origineSupportId() != null) {
            ComProspectionOriginSupport support = prospectionOriginSupportRepository.findById(request.origineSupportId())
                    .orElseThrow(() -> new InvalidRequestException("ComProspectionOriginSupport introuvable: " + request.origineSupportId()));
            prospection.setComProspectionOriginSupport(support);
        }
        if (request.vocationId() != null) {
            StkVocation vocation = vocationRepository.findById(request.vocationId())
                    .orElseThrow(() -> new InvalidRequestException("StkVocation introuvable: " + request.vocationId()));
            prospection.setStkVocation(vocation);
        }
        if (request.typeContactId() != null) {
            ComActionMessageType typeContact = actionMessageTypeRepository.findById(request.typeContactId())
                    .orElseThrow(() -> new InvalidRequestException("ComActionMessageType introuvable: " + request.typeContactId()));
            prospection.setComActionMessageType(typeContact);
        }
        if (request.actionCommercialeId() != null) {
            ComAction action = actionRepository.findById(request.actionCommercialeId())
                    .orElseThrow(() -> new InvalidRequestException("ComAction introuvable: " + request.actionCommercialeId()));
            prospection.setComAction(action);
        }
        if (request.surface() != null) prospection.setArea(request.surface());
        if (request.surfaceJusqua() != null) prospection.setAreaTo(request.surfaceJusqua());
        if (request.prix() != null) prospection.setPrice(request.prix());
        if (request.prixJusqua() != null) prospection.setPriceTo(request.prixJusqua());
        if (request.avance() != null) prospection.setAdvanceAmount(request.avance());
        if (request.financementFondsPropres() != null) prospection.setFundingOwnBase(request.financementFondsPropres());
        if (request.financementCredit() != null) prospection.setFundingCredit(request.financementCredit());
        if (request.financementAutre() != null) prospection.setFundingOther(request.financementAutre());
        if (request.notes() != null) prospection.setNotes(request.notes());

        return detailEnrichi(prospectionRepository.save(prospection));
    }

    /**
     * {@code PATCH /api/prospections/{id}/tranche-principale}: remplace la tranche/l'article
     * principaux en une opération atomique et cohérente — distinct du PATCH générique
     * ({@link #modifierProspection}) et des "autres tranches" ({@code ComProspectionProject},
     * intérêts secondaires indépendants gérés par {@code ProspectionQualificationService}).
     * <p>
     * Contrairement à l'ancien comportement (tranche/article modifiables séparément et sans
     * validation croisée via le PATCH générique), {@code articleId} doit obligatoirement
     * appartenir à {@code trancheId} — confirmé empiriquement: 100% des prospections en base avec
     * un article principal ont une tranche principale qui correspond à celle de l'article
     * (aucune exception sur 807 lignes). Si {@code articleId} est omis et que l'article
     * actuellement assigné n'appartient plus à la nouvelle tranche, il est désassigné
     * automatiquement plutôt que laissé incohérent.
     */
    @Transactional
    public ProspectionDetailDto modifierTranchePrincipale(UUID id, ModifierTranchePrincipaleRequest request) {
        ComProspection prospection = prospectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prospection introuvable: " + id));

        Requis.champ(request.trancheId(), "trancheId est requis");
        CfgTranche tranche = trancheRepository.findById(request.trancheId())
                .orElseThrow(() -> new InvalidRequestException("CfgTranche introuvable: " + request.trancheId()));
        // Périmètre commercial : déplacer une prospection vers un projet non affecté est refusé
        // (403) — sinon le périmètre se contournerait en sortant la ressource de son projet.
        perimetreService.verifierEcritureSurTranche(tranche,
                "Rattachement de prospection à la tranche " + tranche.getCode());

        if (request.articleId() != null) {
            StkItem article = stkItemRepository.findById(request.articleId())
                    .orElseThrow(() -> new InvalidRequestException("Article introuvable: " + request.articleId()));
            if (article.getStkHierarchy() == null || !tranche.getId().equals(article.getStkHierarchy().getId())) {
                throw new InvalidRequestException(
                        "L'article " + article.getCode() + " n'appartient pas à la tranche " + tranche.getCode());
            }
            prospection.setStkItem(article);
        } else if (prospection.getStkItem() != null
                && (prospection.getStkItem().getStkHierarchy() == null
                    || !tranche.getId().equals(prospection.getStkItem().getStkHierarchy().getId()))) {
            // L'article actuellement assigné appartient à l'ancienne tranche : on le désassigne
            // plutôt que de laisser trancheId/articleId incohérents (voir doc de la méthode).
            prospection.setStkItem(null);
        }

        prospection.setStkHierarchy(tranche);
        return detailEnrichi(prospectionRepository.save(prospection));
    }

    /** {@code PATCH /api/prospections/{id}/archiver}: closes the prospection with a reason. */
    @Transactional
    public ProspectionDetailDto archiver(UUID id, ArchiverProspectionRequest request) {
        ComProspection prospection = prospectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prospection introuvable: " + id));

        if (request.raisonId() == null && !StringUtils.hasText(request.raisonTexte())) {
            throw new InvalidRequestException("raisonId ou raisonTexte est requis");
        }

        if (request.raisonId() != null) {
            ComProspectionClosingReason reason = closingReasonRepository.findById(request.raisonId())
                    .orElseThrow(() -> new InvalidRequestException("ComProspectionClosingReason introuvable: " + request.raisonId()));
            prospection.setComProspectionClosingReason(reason);
        }
        if (StringUtils.hasText(request.raisonTexte())) {
            prospection.setClosingReasonText(request.raisonTexte());
        }
        prospection.setIsClosed(true);
        prospection.setClosingDate(LocalDateTime.now());

        return detailEnrichi(prospectionRepository.save(prospection));
    }

    /** {@code GET /api/prospections/{id}/interactions}: journal, most recent first. */
    public Page<InteractionDto> findInteractions(UUID prospectionId, Pageable pageable) {
        if (!prospectionRepository.existsById(prospectionId)) {
            throw new ResourceNotFoundException("Prospection introuvable: " + prospectionId);
        }
        return actionMessageRepository.findByComProspectionIdOrderByDocDateDesc(prospectionId, pageable)
                .map(ProspectionService::toInteractionDto);
    }

    /** {@code POST /api/prospections/{id}/interactions}. */
    @Transactional
    public InteractionDto addInteraction(UUID prospectionId, CreerInteractionRequest request) {
        ComProspection prospection = prospectionRepository.findById(prospectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prospection introuvable: " + prospectionId));

        if (!StringUtils.hasText(request.description())) {
            throw new InvalidRequestException("description est requise");
        }

        LocalDateTime maintenant = LocalDateTime.now();
        AdmUser auteur = auteurCourant();
        String nomAuteur = auteur != null ? auteur.getDescription() : null;

        ComActionMessage message = new ComActionMessage();
        message.setId(UUID.randomUUID());
        message.setComProspection(prospection);
        message.setDocDate(maintenant);

        // Le texte de l'appelant va dans AlertMessage, PAS dans Description : c'est là que
        // l'application de bureau écrit et lit la note. Description est un libellé « date +
        // auteur » qu'elle régénère, et que nous générons donc à l'identique — mesuré sur
        // 46 546 lignes réelles, sans une seule exception. Voir FIX-interaction-note.md §2.
        message.setAlertMessage(request.description());
        message.setDescription(libelleInteraction(maintenant, nomAuteur));

        // L'auteur vient du jeton, jamais du corps : le laisser choisir fausserait les KPI de
        // performance commerciale. AdmUser est le porteur fiable de l'auteur — CfgEmployeeId
        // est vide 4 fois sur 5 en base réelle.
        message.setAdmUser(auteur);
        message.setCfgEmployee(auteur != null ? auteur.getCfgEmployee() : null);
        message.setUserName(nomAuteur);
        message.setUserCreate(nomAuteur);
        message.setUserCreateDateTime(maintenant);

        message.setIsIn(request.entrante() != null ? request.entrante() : true);
        message.setAlertValidityDate(request.dateAlerte());

        // ------------------------------------------------------------------
        // Les quatre drapeaux du légataire, et le tiers. Nous ne les posions pas, et ce n'était
        // pas anodin : ComSaleProspectionView calcule LastCommunicationDate et
        // LastCommunicationResult avec `AND IsSystem = 0`. Un NULL ne satisfait pas `= 0`, donc
        // nos interactions étaient EXCLUES du « dernier échange » — dans l'écran du logiciel de
        // bureau comme dans notre propre liste, qui lit cette colonne depuis la vue.
        //
        // Mesuré sur yesRems avant correctif : 14 lignes à IsSystem NULL, toutes écrites par
        // cette API (UserCreate nul, note dans Description), touchant 8 prospections dont le
        // dernier échange restait faux. Script de rattrapage :
        // src/main/resources/db/migration/007_ComActionMessage_drapeaux_api.sql
        //
        // Les valeurs suivent la convention mesurée sur les 43 540 interactions manuelles.
        // ------------------------------------------------------------------

        // 0 = saisie humaine. 1 est réservé aux alertes que le légataire génère lui-même
        // (4 431 lignes), et c'est exactement ce que la vue écarte du dernier échange.
        message.setIsSystem(false);
        // 43 493 lignes sur 43 540 sont à 0 ; le 1 signale une note à lire, pas une interaction.
        message.setIsNotRead(false);
        message.setIsUnpaid(false);
        // 0 à la création. Le passage à 1 est un acte distinct — c'est la transformation en vente
        // qui désarme les relances de la prospection (voir DossierService.create).
        message.setWithoutRevival(false);
        // Renseigné sur 42 028 des 43 540 interactions manuelles (96,5 %). Le prospect de la
        // prospection : la même valeur que le légataire y met.
        message.setCfgTier(prospection.getCfgProspect());

        if (request.actionTypeId() != null) {
            ComActionMessageType actionType = actionMessageTypeRepository.findById(request.actionTypeId())
                    .orElseThrow(() -> new InvalidRequestException("ComActionMessageType introuvable: " + request.actionTypeId()));
            message.setComActionMessageType(actionType);
        }

        if (request.resultatId() != null) {
            ComActionMessageSubCategory resultat = actionMessageSubCategoryRepository.findById(request.resultatId())
                    .orElseThrow(() -> new InvalidRequestException("ComActionMessageSubCategory introuvable: " + request.resultatId()));
            message.setComActionMessageSubCategory(resultat);
            // The category is never client-supplied — always derived from the chosen résultat.
            message.setComActionMessageCategory(resultat.getComActionMessageCategory());

            if (RESULTATS_AVEC_DATE_PLANIFIEE.contains(resultat.getCode())) {
                if (request.dateRdv() == null) {
                    throw new InvalidRequestException(
                            "dateRdv est requise lorsque le résultat est \"" + resultat.getCode() + "\"");
                }
                AdmEvent evenement = new AdmEvent();
                evenement.setId(UUID.randomUUID());
                evenement.setSubject(request.description());
                evenement.setStartHour(request.dateRdv());
                evenement.setComActionMessageSubCategory(resultat);
                message.setAdmEvent(admEventRepository.save(evenement));
            }
        } else if (request.dateRdv() != null) {
            throw new InvalidRequestException("dateRdv nécessite un resultatId (\"Rendez vous pris\", "
                    + "\"RDV reporté\" ou \"Le prospect veut faire une visite du projet\")");
        }

        ComActionMessage enregistree = actionMessageRepository.save(message);

        // Confirmation au prospect — publiée seulement si un rendez-vous a effectivement été créé,
        // et donc sur exactement l'ensemble de résultats qui alimente déjà le calendrier. Aucune
        // règle métier nouvelle. La livraison, elle, n'a lieu qu'après commit : voir VisitePlanifiee.
        if (enregistree.getAdmEvent() != null) {
            publisher.publishEvent(new VisitePlanifiee(enregistree.getId()));
        }

        return toInteractionDto(enregistree);
    }

    /**
     * Format exact du libellé de l'application de bureau : {@code dd/MM/yyyy HH:mm } suivi du
     * nom de l'auteur. Vérifié ligne à ligne sur {@code yesRemsProd} — la date correspond
     * toujours à {@code DocDate}, et le nom à {@code AdmUser.Description}.
     *
     * <p>Sans auteur connu (R3 de la spec), le libellé se réduit à la date plutôt que de
     * traîner un « null » : certains contextes n'ont pas de principal authentifié, et une
     * interaction sans auteur vaut mieux qu'une exception.
     */
    /**
     * L'interaction que le logiciel de bureau écrit <b>systématiquement</b> juste après avoir créé
     * une prospection — relevée dans sa trace d'audit : {@code INSERT INTO ComProspection} suivi,
     * dans la même seconde, d'un {@code INSERT INTO ComActionMessage}.
     *
     * <p>Nous ne l'écrivions pas, et une prospection naissait donc sans aucun échange. Tout ce qui
     * se fonde sur les interactions s'en trouvait faussé dès la création : dernier échange, délai
     * de prise en charge, compteurs d'activité de {@code /api/kpi/commerciaux}.
     *
     * <h2>Les drapeaux, et pourquoi ils s'écartent de {@code creerInteraction}</h2>
     *
     * <p>{@code IsSystem = 1} et {@code WithoutRevival = 1}, à l'inverse d'une interaction saisie.
     * Ce n'est pas une incohérence : {@code ComSaleProspectionView} calcule le dernier échange avec
     * {@code AND IsSystem = 0}, et cette ligne-ci <b>doit</b> en être exclue — créer une
     * prospection n'est pas un échange avec le client. Le légataire fait exactement ce choix,
     * mesuré sur les 2 634 interactions de création réelles.
     *
     * <p>{@code AlertMessage} reprend les notes de la prospection (identique au caractère près sur
     * 1 507 des 2 634, les écarts restants n'étant que des espaces), et {@code AlertValidityDate}
     * reste nulle : ce n'est pas une relance.
     *
     * <p>Le type reprend celui de la prospection (83 % des cas réels) et la catégorie est
     * {@code Prospection}. Une catégorie introuvable ne fait pas échouer la création : la ligne
     * part sans, ce qui vaut mieux que de refuser une prospection pour un référentiel incomplet.
     */
    private void ecrireInteractionDeCreation(ComProspection prospection) {
        LocalDateTime maintenant = LocalDateTime.now();
        AdmUser auteur = auteurCourant();
        String nomAuteur = auteur != null ? auteur.getDescription() : null;

        ComActionMessage message = new ComActionMessage();
        message.setId(UUID.randomUUID());
        message.setComProspection(prospection);
        message.setDocDate(maintenant);
        message.setDescription(libelleInteraction(maintenant, nomAuteur));
        message.setUserName(nomAuteur);
        message.setUserCreate(nomAuteur);
        message.setUserCreateDateTime(maintenant);
        message.setAdmUser(auteur);
        message.setCfgTier(prospection.getCfgProspect());
        message.setComActionMessageType(prospection.getComActionMessageType());
        actionMessageCategoryRepository.findByCode(CATEGORIE_PROSPECTION)
                .ifPresent(message::setComActionMessageCategory);

        message.setIsSystem(true);
        message.setWithoutRevival(true);
        message.setIsNotRead(false);
        message.setIsUnpaid(false);
        message.setAlertValidityDate(null);
        message.setAlertMessage(prospection.getNotes());

        actionMessageRepository.save(message);
    }

    private static String libelleInteraction(LocalDateTime date, String nomAuteur) {
        String horodatage = date.format(FORMAT_LIBELLE);
        return StringUtils.hasText(nomAuteur) ? horodatage + " " + nomAuteur : horodatage;
    }

    /**
     * Compte authentifié, ou {@code null} hors contexte web. {@code @WithMockUser} et les
     * endpoints ouverts portent un principal d'un autre type : traités comme « pas d'auteur »,
     * jamais comme une erreur.
     */
    private AdmUser auteurCourant() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser utilisateur
                && utilisateur.admUserId() != null) {
            return userRepository.findById(utilisateur.admUserId()).orElse(null);
        }
        return null;
    }

    private static InteractionDto toInteractionDto(ComActionMessage m) {
        // Contexte projet/tranche : déduit, jamais stocké sur ComActionMessage — voir
        // ContexteInteraction, qui reproduit la règle de la vue légataire.
        CfgTranche tranche = ContexteInteraction.tranche(m);
        CfgProject projet = tranche != null ? tranche.getCfgProject() : null;
        return new InteractionDto(
                m.getId(),
                m.getDocDate(),
                // AlertMessage porte le texte du commercial ; Description n'est qu'un libellé
                // « date + auteur » régénéré à chaque écriture. Voir FIX-interaction-note.md.
                m.getAlertMessage(),
                m.getDescription(),
                m.getIsIn(),
                m.getCfgEmployee() != null ? m.getCfgEmployee().getId() : null,
                m.getCfgEmployee() != null
                        ? ((m.getCfgEmployee().getFirstName() != null ? m.getCfgEmployee().getFirstName() + " " : "")
                        + (m.getCfgEmployee().getLastName() != null ? m.getCfgEmployee().getLastName() : "")).trim()
                        : null,
                m.getComActionMessageType() != null ? m.getComActionMessageType().getId() : null,
                m.getComActionMessageType() != null ? m.getComActionMessageType().getDescription() : null,
                m.getComActionMessageSubCategory() != null ? m.getComActionMessageSubCategory().getId() : null,
                m.getComActionMessageSubCategory() != null ? m.getComActionMessageSubCategory().getDescription() : null,
                m.getAlertValidityDate(),
                m.getAdmEvent() != null ? m.getAdmEvent().getStartHour() : null,
                m.getComActionMessageCategory() != null ? m.getComActionMessageCategory().getId() : null,
                m.getComActionMessageCategory() != null ? m.getComActionMessageCategory().getDescription() : null,
                tranche != null ? tranche.getId() : null,
                tranche != null ? tranche.getCode() : null,
                tranche != null ? tranche.getDescription() : null,
                projet != null ? projet.getId() : null,
                projet != null ? projet.getCode() : null,
                projet != null ? projet.getDescription() : null
        );
    }

    /**
     * Résout la page en DTO en chargeant les alertes de toute la page <b>en une requête</b>.
     * Les résoudre ligne par ligne ferait 20 requêtes pour une page de 20 — le genre de N+1 qui
     * ne se voit pas en test sur trois lignes et qui coûte cher sur un écran réel.
     */
    private Page<ProspectionListItemDto> versListe(Page<ComProspection> page,
                                                   Set<ColonneProspection> colonnes) {
        List<UUID> ids = page.getContent().stream().map(ComProspection::getId).toList();
        // IN () est une erreur de syntaxe SQL : ne pas interroger sur une page vide.
        Map<UUID, DerniereAlerteProjection> alertes = ids.isEmpty()
                ? Map.of()
                : actionMessageRepository.dernieresAlertes(ids).stream()
                        .collect(Collectors.toMap(DerniereAlerteProjection::getProspectionId, a -> a));
        // La vue n'est interrogée que si la sélection contient au moins une colonne d'activité :
        // sans `?colonnes=`, cette requête n'est pas émise et le coût du lot est nul.
        Map<UUID, ComSaleProspectionView> activite = ids.isEmpty() || !ColonneProspection.exigeLaVue(colonnes)
                ? Map.of()
                : prospectionViewRepository.findByIdIn(ids).stream()
                        .collect(Collectors.toMap(ComSaleProspectionView::getId, v -> v));
        // Même discipline pour le référentiel StrEntity : résolu seulement si l'une des trois
        // colonnes de type est demandée. Les identifiants sont dédoublonnés — en pratique un ou
        // deux types distincts sur une page de 20, la quasi-totalité des lignes portant
        // ComProspection — donc la requête porte sur une poignée de clés d'une table de 162 lignes.
        Map<UUID, StrEntity> types = chargerTypes(page.getContent(), colonnes);
        return page.map(p -> toListItem(p, alertes.get(p.getId()), activite.get(p.getId()),
                typeDe(types, p), colonnes));
    }

    /**
     * Types de document de la page, indexés par identifiant — vide si aucune colonne de type n'est
     * demandée, auquel cas aucune requête n'est émise.
     */
    private Map<UUID, StrEntity> chargerTypes(List<ComProspection> prospections,
                                              Set<ColonneProspection> colonnes) {
        if (!ColonneProspection.exigeLeTypeProspection(colonnes)) {
            return Map.of();
        }
        Set<UUID> typeIds = prospections.stream()
                .map(ComProspection::getStrEntityId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (typeIds.isEmpty()) {
            return Map.of();
        }
        return strEntityRepository.findByIdIn(typeIds).stream()
                .collect(Collectors.toMap(StrEntity::getId, e -> e));
    }

    /** Type d'une prospection, ou {@code null} si elle n'en porte pas — 4 lignes en base réelle. */
    private static StrEntity typeDe(Map<UUID, StrEntity> types, ComProspection p) {
        return p.getStrEntityId() == null ? null : types.get(p.getStrEntityId());
    }

    /**
     * Téléphone joignable : la fiche d'abord (mobile, à défaut fixe), puis le numéro saisi au
     * premier contact. Mesuré sur les 8 613 prospections ouvertes — 5 034 par la fiche, 7 596
     * par le champ temporaire, <b>8 591 par l'une ou l'autre</b> : aucune des deux sources ne
     * suffit seule, le repli n'est pas une commodité.
     *
     * <p><b>Publique parce qu'elle est la seule</b>, comme {@link #emailProspect}. Le tableau de
     * bord des relances sert le même numéro dans sa tuile ; en recopier une variante ferait
     * diverger deux règles mesurées sur la même donnée.
     */
    public static String telephoneProspect(CfgTier prospect, ComProspection p) {
        if (prospect != null) {
            if (StringUtils.hasText(prospect.getMobile())) {
                return prospect.getMobile();
            }
            if (StringUtils.hasText(prospect.getPhone())) {
                return prospect.getPhone();
            }
        }
        return StringUtils.hasText(p.getTempTel()) ? p.getTempTel() : null;
    }

    /**
     * Même règle de repli que {@link #telephoneProspect} — 4 752 des 8 613 ouvertes (55 %).
     *
     * <p><b>Publique parce qu'elle est la seule.</b> {@code ConfirmationVisiteService} résout le
     * destinataire du mail de visite avec exactement cette règle ; en recopier une variante est
     * précisément ce qui avait fait diverger les trois mappeurs de {@code ProspectionListItemDto}.
     *
     * <p>Aucune validation de forme ici, délibérément : cette méthode sert d'abord à
     * <i>afficher</i> le contact, et 33 lignes réelles portent une valeur non vide qui n'est pas
     * une adresse. Les montrer telles quelles est utile — un commercial voit qu'il y a une saisie
     * à corriger. C'est l'appelant qui <i>envoie</i> qui doit exiger davantage.
     */
    public static String emailProspect(CfgTier prospect, ComProspection p) {
        if (prospect != null && StringUtils.hasText(prospect.getEMail())) {
            return prospect.getEMail();
        }
        return StringUtils.hasText(p.getTempEMail()) ? p.getTempEMail() : null;
    }

    /**
     * Mappeur commun de {@link ProspectionListItemDto}. Public parce qu'il est le seul :
     * {@code HotlineService} et {@code ProspectSubResourcesService} en portaient chacun une copie
     * identique au caractère près, et c'est cette duplication qui les a fait diverger dès que la
     * liste a gagné le contact et l'alerte.
     *
     * <p>Cette variante laisse les champs d'alerte nuls. Ils ne sont résolus que par les listes
     * de prospections, qui les chargent pour toute la page en une requête — voir
     * {@link #versListe}.
     */
    public static ProspectionListItemDto toListItem(ComProspection p) {
        return toListItem(p, null, null, null, Set.of());
    }

    /**
     * @param vue  ligne de {@code ComSaleProspectionView}, ou {@code null} quand aucune colonne
     *             d'activité n'est demandée — ou que la prospection n'y figure pas
     *             (pré-prospection, ou {@code StrEntityId} absent).
     * @param type type de document légataire, ou {@code null} quand aucune colonne de type n'est
     *             demandée — ou que la prospection ne porte pas de {@code StrEntityId}. Les deux
     *             cas produisent la même sortie ; c'est la sélection de colonnes, et non le DTO,
     *             qui distingue « non demandé » de « absent ».
     */
    private static ProspectionListItemDto toListItem(ComProspection p, DerniereAlerteProjection alerte,
                                                     ComSaleProspectionView vue,
                                                     StrEntity type,
                                                     Set<ColonneProspection> colonnes) {
        // Les associations du lot B ne sont déréférencées QUE si la colonne est demandée : chacune
        // est un chargement paresseux de plus par ligne (deux pour la société, qui traverse
        // tranche → projet). Non demandée, elle ne coûte rien.
        CfgTranche tranche = p.getStkHierarchy();
        CfgProject projet = colonnes.contains(ColonneProspection.SOCIETE_ID)
                || colonnes.contains(ColonneProspection.SOCIETE_CODE)
                || colonnes.contains(ColonneProspection.SOCIETE_LIBELLE)
                ? (tranche != null ? tranche.getCfgProject() : null)
                : null;
        CfgCompany societe = projet != null ? projet.getCfgCompany() : null;
        return new ProspectionListItemDto(
                p.getId(),
                p.getCode(),
                p.getDocDate(),
                p.getTempDescription(),
                p.getCfgProspect() != null ? p.getCfgProspect().getId() : null,
                TierNomFormatter.nom(p.getCfgProspect()),
                p.getCfgCommercial() != null ? p.getCfgCommercial().getId() : null,
                TierNomFormatter.nom(p.getCfgCommercial()),
                p.getComProspectionOrigin() != null ? p.getComProspectionOrigin().getCode() : null,
                p.getComProspectionKind() != null ? p.getComProspectionKind().getCode() : null,
                p.getIsClosed(),
                p.getPrice(),
                p.getArea(),
                p.getStkHierarchy() != null ? p.getStkHierarchy().getId() : null,
                p.getStkHierarchy() != null ? p.getStkHierarchy().getCode() : null,
                p.getStkHierarchy() != null ? p.getStkHierarchy().getDescription() : null,
                telephoneProspect(p.getCfgProspect(), p),
                emailProspect(p.getCfgProspect(), p),
                alerte != null ? alerte.getDateAlerte() : null,
                alerte != null ? alerte.getMessage() : null,
                vue != null ? vue.getInactivityTimeout() : null,
                vue != null ? vue.getActionsMessageNumber() : null,
                vue != null ? vue.getLastActionDate() : null,
                vue != null ? vue.getLastCommunicationDate() : null,
                vue != null ? videEnNull(vue.getLastCommunicationResult()) : null,
                vue != null ? vue.getNbrVisit() : null,
                vue != null ? vue.getLastVisitDate() : null,
                vue != null ? vue.getCfgProjectId() : null,
                vue != null ? videEnNull(vue.getCfgProjectCode()) : null,
                vue != null ? videEnNull(vue.getCfgProjectDescription()) : null,
                vue != null ? videEnNull(vue.getCfgTierIdNumber()) : null,
                vue != null ? vue.getComFolderId() : null,
                vue != null ? videEnNull(vue.getComFolderCode()) : null,
                vue != null ? videEnNull(vue.getStatus()) : null,
                p.getCfgProspect() != null ? p.getCfgProspect().getCode() : null,
                p.getCfgProspect() != null ? p.getCfgProspect().getMobile() : null,
                p.getCfgProspect() != null ? p.getCfgProspect().getMobileIndicatif() : null,
                p.getCfgProspect() != null ? p.getCfgProspect().getEMail() : null,
                p.getCfgCommercial() != null ? p.getCfgCommercial().getCode() : null,
                p.getIsOldCustomer(),
                p.getClosingDate(),
                colonnes.contains(ColonneProspection.RAISON_FERMETURE)
                        && p.getComProspectionClosingReason() != null
                        ? p.getComProspectionClosingReason().getDescription() : null,
                colonnes.contains(ColonneProspection.TYPE_PROSPECT)
                        && p.getCfgProspect() != null && p.getCfgProspect().getCfgTierType() != null
                        ? p.getCfgProspect().getCfgTierType().getCode() : null,
                colonnes.contains(ColonneProspection.ORIGINE_SUPPORT)
                        && p.getComProspectionOriginSupport() != null
                        ? p.getComProspectionOriginSupport().getDescription() : null,
                colonnes.contains(ColonneProspection.ACTION_COMMERCIALE) && p.getComAction() != null
                        ? p.getComAction().getDescription() : null,
                colonnes.contains(ColonneProspection.PAYS) && p.getAdmCountry() != null
                        ? p.getAdmCountry().getDescription() : null,
                societe != null ? societe.getId() : null,
                societe != null ? societe.getCode() : null,
                societe != null ? societe.getDescription() : null,
                p.getUserCreate(),
                p.getUserCreateDateTime(),
                p.getUserUpdate(),
                p.getUserUpdateDateTime(),
                // typeProspectionId vient de la prospection elle-même, pas du référentiel : il est
                // connu sans jointure. Mais il reste conditionné à la sélection, sinon il
                // apparaîtrait alors que les deux autres champs du groupe seraient nuls.
                colonnes.contains(ColonneProspection.TYPE_PROSPECTION_ID) ? p.getStrEntityId() : null,
                type != null ? type.getCode() : null,
                type != null ? type.getDescription() : null
        );
    }

    /**
     * Détail « nu », sans le bloc activité. Passer par {@link #detailEnrichi(ComProspection)} pour
     * une réponse d'API : c'est lui qui interroge la vue et remplit {@code activite}.
     */
    private static ProspectionDetailDto toDetail(ComProspection p, ActiviteProspectionDto activite,
                                                 StrEntity type, CfgTier doublonDe) {
        // Chaque association est nommée une fois plutôt que déréférencée à chaque champ : depuis
        // que la fiche sert aussi le code des référentiels, un `p.getX() != null ? p.getX()...`
        // traverserait le proxy LAZY trois fois par association. Même raison d'être que
        // com.tsix.immocrm.utils.Champs.
        CfgTier prospect = p.getCfgProspect();
        CfgTierType typeTiers = Champs.de(prospect, CfgTier::getCfgTierType);
        CfgTier commercial = p.getCfgCommercial();
        CfgTier proposeur = p.getCfgProposer();
        ComProspectionOrigin origine = p.getComProspectionOrigin();
        ComProspectionOriginSupport origineSupport = p.getComProspectionOriginSupport();
        ComProspectionKind etape = p.getComProspectionKind();
        StkVocation vocation = p.getStkVocation();
        ComActionMessageType typeContact = p.getComActionMessageType();
        ComAction actionCommerciale = p.getComAction();
        ComProspectionClosingReason raisonFermeture = p.getComProspectionClosingReason();
        CfgTranche tranche = p.getStkHierarchy();
        StkItem article = p.getStkItem();
        AdmCountry pays = p.getAdmCountry();
        // Société : dérivée de la tranche (CfgTranche → CfgProject → CfgCompany), exactement comme
        // le fait ComSaleProspectionView. Elle n'est jamais saisie sur la prospection. Contrairement
        // à la liste, rien n'est ici conditionné à une sélection de colonnes : la fiche sert un
        // enregistrement unique, les deux chargements paresseux sont payés une fois, pas par ligne.
        CfgCompany societe = Champs.de(tranche, CfgTranche::getCfgProject, CfgProject::getCfgCompany);
        return new ProspectionDetailDto(
                p.getId(),
                p.getCode(),
                p.getDocDate(),
                p.getTempDescription(),
                p.getTempTel(),
                p.getTempEMail(),
                Champs.de(prospect, CfgTier::getId),
                TierNomFormatter.nom(prospect),
                Champs.de(prospect, CfgTier::getCode),
                Champs.de(prospect, CfgTier::getMobile),
                Champs.de(prospect, CfgTier::getMobileIndicatif),
                Champs.de(prospect, CfgTier::getEMail),
                Champs.de(typeTiers, CfgTierType::getId),
                Champs.de(typeTiers, CfgTierType::getCode),
                Champs.de(typeTiers, CfgTierType::getDescription),
                Champs.de(commercial, CfgTier::getId),
                TierNomFormatter.nom(commercial),
                Champs.de(commercial, CfgTier::getCode),
                Champs.de(proposeur, CfgTier::getId),
                TierNomFormatter.nom(proposeur),
                Champs.de(origine, ComProspectionOrigin::getId),
                Champs.de(origine, ComProspectionOrigin::getDescription),
                Champs.de(origine, ComProspectionOrigin::getCode),
                Champs.de(origineSupport, ComProspectionOriginSupport::getId),
                Champs.de(origineSupport, ComProspectionOriginSupport::getDescription),
                Champs.de(origineSupport, ComProspectionOriginSupport::getCode),
                Champs.de(etape, ComProspectionKind::getId),
                Champs.de(etape, ComProspectionKind::getDescription),
                Champs.de(etape, ComProspectionKind::getCode),
                Champs.de(vocation, StkVocation::getId),
                Champs.de(vocation, StkVocation::getDescription),
                Champs.de(vocation, StkVocation::getCode),
                Champs.de(typeContact, ComActionMessageType::getId),
                Champs.de(typeContact, ComActionMessageType::getDescription),
                Champs.de(typeContact, ComActionMessageType::getCode),
                Champs.de(actionCommerciale, ComAction::getId),
                Champs.de(actionCommerciale, ComAction::getDescription),
                Champs.de(actionCommerciale, ComAction::getCode),
                p.getNotes(),
                p.getArea(),
                p.getAreaTo(),
                p.getPrice(),
                p.getPriceTo(),
                p.getAdvanceAmount(),
                p.getFundingOwnBase(),
                p.getFundingCredit(),
                p.getFundingOther(),
                p.getIsClosed(),
                p.getClosingDate(),
                Champs.de(raisonFermeture, ComProspectionClosingReason::getId),
                Champs.de(raisonFermeture, ComProspectionClosingReason::getDescription),
                Champs.de(raisonFermeture, ComProspectionClosingReason::getCode),
                p.getClosingReasonText(),
                p.getIsOldCustomer(),
                Champs.de(tranche, CfgTranche::getId),
                Champs.de(tranche, CfgTranche::getCode),
                Champs.de(tranche, CfgTranche::getDescription),
                Champs.de(article, StkItem::getId),
                Champs.de(article, StkItem::getCode),
                Champs.de(article, StkItem::getDescription),
                Champs.de(article, StkItem::getStkItemCategory, StkItemCategory::getDescription),
                Champs.de(article, StkItem::getStkItemType, StkItemType::getDescription),
                Champs.de(societe, CfgCompany::getId),
                Champs.de(societe, CfgCompany::getCode),
                Champs.de(societe, CfgCompany::getDescription),
                Champs.de(pays, AdmCountry::getId),
                Champs.de(pays, AdmCountry::getCode),
                Champs.de(pays, AdmCountry::getDescription),
                p.getUserCreate(),
                p.getUserCreateDateTime(),
                p.getUserUpdate(),
                p.getUserUpdateDateTime(),
                // Le texte brut est servi tel quel ; le marqueur normalisé vaut null dès que ce
                // texte n'en est pas un — ce qui est le cas le plus fréquent (1 239 lignes).
                texteLibre(p.getTempField1()),
                Champs.de(MarqueurTriProspection.depuis(p.getTempField1()), MarqueurTriProspection::name),
                texteLibre(p.getTempField10()),
                Champs.de(doublonDe, CfgTier::getId),
                TierNomFormatter.nom(doublonDe),
                p.getStrEntityId(),
                type != null ? type.getCode() : null,
                type != null ? type.getDescription() : null,
                activite
        );
    }
}

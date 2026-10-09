package com.tsix.immocrm.services.prospection;

import com.tsix.immocrm.dto.prospect.CreerProspectRequest;
import com.tsix.immocrm.dto.prospect.HistoriqueDto;
import com.tsix.immocrm.dto.prospection.ArchiverProspectionRequest;
import com.tsix.immocrm.dto.prospection.ChangerEtapeRequest;
import com.tsix.immocrm.dto.prospection.CreerInteractionRequest;
import com.tsix.immocrm.dto.prospection.CreerPreProspectionRequest;
import com.tsix.immocrm.dto.prospection.CreerProspectionRequest;
import com.tsix.immocrm.dto.prospection.DoublonPreProspectionDto;
import com.tsix.immocrm.dto.prospection.InteractionDto;
import com.tsix.immocrm.dto.prospection.LierProspectRequest;
import com.tsix.immocrm.dto.prospection.ModifierProspectionRequest;
import com.tsix.immocrm.dto.prospection.ModifierTranchePrincipaleRequest;
import com.tsix.immocrm.domain.AgregatDemande;
import com.tsix.immocrm.domain.Chemin;
import com.tsix.immocrm.domain.ColonneProspection;
import com.tsix.immocrm.domain.Granularite;
import com.tsix.immocrm.domain.TriGroupe;
import com.tsix.immocrm.domain.TypeActionRecente;
import com.tsix.immocrm.domain.MarqueurTriProspection;
import com.tsix.immocrm.dto.prospection.ActiviteProspectionDto;
import com.tsix.immocrm.dto.prospection.FiltresAvancesProspection;
import com.tsix.immocrm.dto.prospection.PreProspectionCreeeDto;
import com.tsix.immocrm.dto.prospection.ProspectionDetailDto;
import com.tsix.immocrm.dto.prospection.ProspectionExistanteDto;
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
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ProspectionExistanteException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.mail.VisiteEffectuee;
import com.tsix.immocrm.mail.VisitePlanifiee;
import com.tsix.immocrm.repositories.adm.AdmEventRepository;
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
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.services.groupement.GroupementService;
import com.tsix.immocrm.services.groupement.GroupementService.ResultatGroupement;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.services.prospect.ProspectService;
import com.tsix.immocrm.services.sequence.GenerateurCodeSequence;
import com.tsix.immocrm.services.tranche.AppartenanceTranche;
import com.tsix.immocrm.utils.PhoneNumberUtils;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ProspectionService {

    // Le remerciement suit l'étape, pas un résultat d'interaction : « Réalisée » / « Non réalisée »
    // ne sont jamais saisis. Voir FEATURE-offre-apres-visite.md §1.
    static final String ETAPE_APRES_VISITE = "4 APRES VISITES";

    /** {@code ComActionMessage.Description} comme OMRAN, sans exception : {@code 07/09/2026 17:13 Khadija ELKORRI}. */
    private static final DateTimeFormatter FORMAT_LIBELLE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final String IS_CLOSED = "isClosed";
    private static final String IS_OLD_CUSTOMER = "isOldCustomer";

    private static final String PROSPECTION_INTROUVABLE = "Prospection introuvable: ";
    private static final String ORIGINE_INTROUVABLE = "ComProspectionOrigin introuvable: ";
    private static final String ORIGINE_SUPPORT_INTROUVABLE = "ComProspectionOriginSupport introuvable: ";
    private static final String VOCATION_INTROUVABLE = "StkVocation introuvable: ";
    private static final String TYPE_CONTACT_INTROUVABLE = "ComActionMessageType introuvable: ";
    private static final String ACTION_INTROUVABLE = "ComAction introuvable: ";
    private static final String ETAPE_INTROUVABLE = "ComProspectionKind introuvable: ";
    private static final String TRANCHE_INTROUVABLE = "CfgTranche introuvable: ";
    private static final String ARTICLE_INTROUVABLE = "Article introuvable: ";

    private final ComProspectionRepository prospectionRepository;
    // Vue légataire du bloc « activité » : une ligne à la fois, jamais pour une liste
    // (FEATURE-vues-legacy-prospection.md §5).
    private final ComSaleProspectionViewRepository prospectionViewRepository;
    /** Types de document du légataire — voir {@code instructions/FEATURE-strentity-prospection.md}. */
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

    // Catégorie des interactions de création ; celles en « Dossier de vente (Réservation) »
    // relèvent de la transformation (FIX-creation-prospect-prospection.md §4).
    private static final String CATEGORIE_PROSPECTION = "Prospection";

    private static final String CODE_STR_ENTITY_PROSPECTION_VENTE = "ComProspection";

    // Repli si le référentiel ne répond pas : même PKey dans toutes les bases (référentiel
    // légataire restauré d'une même source).
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
    /** Auteur d'une interaction — voir {@code FIX-interaction-note.md} et {@code FEATURE-suivi-actions.md} §6. */
    private final UtilisateurAffiche utilisateurAffiche;
    private final AlerteRelanceService alerteRelanceService;
    // Événement plutôt qu'appel direct : le mail ne part que si l'interaction est écrite, et
    // l'envoi ne peut rien annuler (FEATURE-mail-confirmation-visite.md §3).
    private final ApplicationEventPublisher publisher;
    private final Clock clock;

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
                               UtilisateurAffiche utilisateurAffiche,
                               ComSaleProspectionViewRepository prospectionViewRepository,
                               StrEntityRepository strEntityRepository,
                               ApplicationEventPublisher publisher,
                               AlerteRelanceService alerteRelanceService,
                               Clock clock) {
        this.clock = clock;
        this.alerteRelanceService = alerteRelanceService;
        this.prospectionViewRepository = prospectionViewRepository;
        this.strEntityRepository = strEntityRepository;
        this.publisher = publisher;
        this.utilisateurAffiche = utilisateurAffiche;
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
     * Pré-prospections exclues : {@link #findPreProspections} les sert, sans recouvrement.
     *
     * @param ouvertes {@code null} = pas de filtre ; ouvertes = {@code IsClosed} à 0 <b>ou NULL</b>
     */
    public Page<ProspectionListItemDto> findAll(List<String> etapes, List<String> origines, List<UUID> commerciaux,
            List<UUID> trancheIds, LocalDateTime dateDebut, LocalDateTime dateFin, String search,
            Boolean ouvertes, FiltresAvancesProspection filtres, Set<ColonneProspection> colonnes,
            ColonneProspection groupeColonne, String groupeValeur, Granularite granularite,
            Pageable pageable) {
        Specification<ComProspection> spec = specProspections(new CriteresListe(etapes, origines, commerciaux,
                trancheIds, dateDebut, dateFin, search, ouvertes), filtres)
                .and(perimetreService.filtreProspections())
                .and(filtreDeGroupe(groupeColonne, groupeValeur, granularite));
        return versListe(prospectionRepository.findAll(spec, pageable), colonnes);
    }

    /**
     * Même spec et même périmètre que {@link #findAll} : les compteurs du tableau de bord égalent
     * la liste par construction ({@code FEATURE-dashboard-filtre-tranche.md} R3).
     */
    public long compterProspections(UUID trancheId, Boolean ouvertes) {
        return prospectionRepository.count(specProspections(new CriteresListe(null, null, null,
                List.of(trancheId), null, null, null, ouvertes), null)
                .and(perimetreService.filtreProspections()));
    }

    public Page<ProspectionListItemDto> findPreProspections(List<String> etapes, List<String> origines,
            List<UUID> commerciaux, List<UUID> trancheIds, LocalDateTime dateDebut, LocalDateTime dateFin,
            String search, Boolean ouvertes, ColonneProspection groupeColonne, String groupeValeur,
            Granularite granularite, Pageable pageable) {
        Specification<ComProspection> spec = specPreProspections(new CriteresListe(etapes, origines, commerciaux,
                trancheIds, dateDebut, dateFin, search, ouvertes))
                .and(perimetreService.filtreProspections())
                .and(filtreDeGroupe(groupeColonne, groupeValeur, granularite));
        // Sélection par défaut forcée : ComSaleProspectionView exclut les pré-prospections, la
        // lire ne rendrait que des null.
        return versListe(prospectionRepository.findAll(spec, pageable), ColonneProspection.parDefautSet());
    }

    /** Voir {@code instructions/FEATURE-groupement-listes.md}. */
    public ResultatGroupement grouperProspections(List<String> etapes, List<String> origines,
            List<UUID> commerciaux, List<UUID> trancheIds, LocalDateTime dateDebut, LocalDateTime dateFin,
            String search, Boolean ouvertes, FiltresAvancesProspection filtres,
            ColonneProspection groupeBy, Granularite granularite, List<AgregatDemande> agregats,
            TriGroupe tri, boolean inclureSansValeur, Pageable pageable) {
        return grouper(specProspections(new CriteresListe(etapes, origines, commerciaux, trancheIds, dateDebut,
                dateFin, search, ouvertes), filtres), groupeBy, granularite, agregats, tri, inclureSansValeur, pageable);
    }

    public ResultatGroupement grouperPreProspections(List<String> etapes, List<String> origines,
            List<UUID> commerciaux, List<UUID> trancheIds, LocalDateTime dateDebut, LocalDateTime dateFin,
            String search, Boolean ouvertes, ColonneProspection groupeBy, Granularite granularite,
            List<AgregatDemande> agregats, TriGroupe tri, boolean inclureSansValeur, Pageable pageable) {
        return grouper(specPreProspections(new CriteresListe(etapes, origines, commerciaux, trancheIds, dateDebut,
                dateFin, search, ouvertes)), groupeBy, granularite, agregats, tri, inclureSansValeur, pageable);
    }

    private ResultatGroupement grouper(Specification<ComProspection> spec, ColonneProspection groupeBy,
            Granularite granularite, List<AgregatDemande> agregats, TriGroupe tri,
            boolean inclureSansValeur, Pageable pageable) {
        return groupementService.grouper(ComProspection.class,
                spec.and(perimetreService.filtreProspections()),
                groupeBy.chemin(), granularite, agregats, tri, inclureSansValeur, pageable);
    }

    private static Specification<ComProspection> filtreDeGroupe(ColonneProspection groupeColonne,
            String groupeValeur, Granularite granularite) {
        if (groupeColonne == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> GroupementService.predicatDeGroupe(
                root, cb, groupeColonne.chemin(), granularite, groupeValeur);
    }

    /**
     * Partagé par la liste et ses groupes, pour qu'ils portent les mêmes lignes. Périmètre
     * <b>exclu</b> : chaque appelant l'ajoute explicitement.
     */
    private static Specification<ComProspection> specProspections(CriteresListe criteres,
                                                                  FiltresAvancesProspection filtres) {
        return buildSpec(criteres)
                .and(filtresAvances(filtres))
                .and(DiscriminantPreProspection.nEstPasPreProspection());
    }

    private static Specification<ComProspection> specPreProspections(CriteresListe criteres) {
        return buildSpec(criteres)
                .and(DiscriminantPreProspection.estPreProspection());
    }

    /** Filtres simples communs aux listes de prospections et de pré-prospections. */
    private record CriteresListe(List<String> etapes, List<String> origines, List<UUID> commerciaux,
                                 List<UUID> trancheIds, LocalDateTime dateDebut, LocalDateTime dateFin,
                                 String search, Boolean ouvertes) {
    }

    private static Specification<ComProspection> buildSpec(CriteresListe c) {
        return (root, query, cb) -> predicatDeBase(c, root, cb);
    }

    private static Predicate predicatDeBase(CriteresListe c, Root<ComProspection> root, CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();
        if (estRenseignee(c.etapes())) {
            List<String> etapesLower = c.etapes().stream().map(String::toLowerCase).toList();
            predicates.add(cb.lower(root.get("comProspectionKind").get("code")).in(etapesLower));
        }
        if (estRenseignee(c.origines())) {
            List<String> originesLower = c.origines().stream().map(String::toLowerCase).toList();
            predicates.add(cb.lower(root.get("comProspectionOrigin").get("code")).in(originesLower));
        }
        if (estRenseignee(c.commerciaux())) {
            predicates.add(root.get("cfgCommercial").get("id").in(c.commerciaux()));
        }
        if (estRenseignee(c.trancheIds())) {
            predicates.add(AppartenanceTranche.prospection(root, c.trancheIds()));
        }
        if (c.dateDebut() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("docDate"), c.dateDebut()));
        }
        if (c.dateFin() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("docDate"), c.dateFin()));
        }
        if (StringUtils.hasText(c.search())) {
            predicates.add(predicatRecherche(root, cb, c.search()));
        }
        if (c.ouvertes() != null) {
            predicates.add(predicatOuvertes(root, cb, c.ouvertes()));
        }
        return cb.and(predicates.toArray(new Predicate[0]));
    }

    private static Predicate predicatRecherche(Root<ComProspection> root, CriteriaBuilder cb, String search) {
        String pattern = "%" + search.toLowerCase() + "%";
        return cb.or(
                cb.like(cb.lower(root.get("code")), pattern),
                cb.like(cb.lower(root.get("tempDescription")), pattern),
                cb.like(cb.lower(root.get("tempTel")), pattern),
                cb.like(cb.lower(root.get("tempEMail")), pattern)
        );
    }

    // Ternaire : absent = tout, true = ouvertes, false = archivées. NULL compte comme
    // ouverte — OMRAN écrit 0 et NULL (FEATURE-creation-pre-prospection.md §2).
    private static Predicate predicatOuvertes(Root<ComProspection> root, CriteriaBuilder cb, boolean ouvertes) {
        return ouvertes
                ? cb.or(cb.isFalse(root.get(IS_CLOSED)), cb.isNull(root.get(IS_CLOSED)))
                : cb.isTrue(root.get(IS_CLOSED));
    }

    /** Voir {@link FiltresAvancesProspection} ; prédicat neutre quand aucun n'est renseigné. */
    private static Specification<ComProspection> filtresAvances(FiltresAvancesProspection f) {
        return (root, query, cb) -> {
            if (f == null) {
                return cb.conjunction();
            }
            List<Predicate> predicates = predicatsDeLEntite(f, root);
            if (f.ancienClient() != null) {
                predicates.add(Boolean.TRUE.equals(f.ancienClient())
                        ? cb.isTrue(root.get(IS_OLD_CUSTOMER))
                        : cb.or(cb.isFalse(root.get(IS_OLD_CUSTOMER)), cb.isNull(root.get(IS_OLD_CUSTOMER))));
            }
            // -- Type de document : sous-requête par code, un code inconnu rend un résultat vide.
            //    Une ligne sans StrEntityId est écartée (NULL IN (…) est inconnu) : voulu.
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
     * Filtres avancés portés par l'entité. Filtre actif = jointure interne, qui écarte les lignes
     * sans valeur : voulu (une prospection sans tranche n'a pas de projet).
     */
    private static List<Predicate> predicatsDeLEntite(FiltresAvancesProspection f, Root<ComProspection> root) {
        List<Predicate> predicates = new ArrayList<>();
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
        return predicates;
    }

    /**
     * Seul le prédicat traverse la vue, la liste reste servie par la table (voir
     * {@code INVENTAIRE-colonnes-prospection.md} §3). La vue exclut pré-prospections et lignes sans
     * {@code StrEntityId} : un de ces filtres les écarte donc, ce qui est voulu.
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
            criteres.add(Boolean.TRUE.equals(f.avecAlerteActive())
                    ? cb.isNotNull(vue.get("lastActiveAlertDate"))
                    : cb.isNull(vue.get("lastActiveAlertDate")));
        }
        sousRequete.where(cb.and(criteres.toArray(new Predicate[0])));
        return sousRequete;
    }

    private static boolean estRenseignee(List<?> valeurs) {
        return valeurs != null && !valeurs.isEmpty();
    }

    public ProspectionDetailDto findById(UUID id) {
        return prospectionRepository.findById(id)
                .map(this::detailEnrichi)
                .orElseThrow(() -> new ResourceNotFoundException(PROSPECTION_INTROUVABLE + id));
    }

    /**
     * Complète le détail du bloc « activité » de {@code ComSaleProspectionView}. La vue exclut les
     * pré-prospections : {@code activite = null} pour elles, pas un 404.
     */
    private ProspectionDetailDto detailEnrichi(ComProspection p) {
        ActiviteProspectionDto activite = prospectionViewRepository.findById(p.getId())
                .map(ProspectionService::toActivite)
                .orElse(null);
        // La fiche sert toujours le type (pas de sélection de colonnes ici) ; null sans StrEntityId.
        StrEntity type = p.getStrEntityId() == null
                ? null
                : strEntityRepository.findById(p.getStrEntityId()).orElse(null);
        return toDetail(p, activite, type, resoudreDoublon(p.getTempField10()));
    }

    /**
     * {@code TempField10} porte un {@code CfgTier.Code} (FEATURE-marqueurs-tri-pre-prospection.md §2).
     * Ce code n'est pas unique : plusieurs correspondances rendent {@code null} plutôt qu'un tiers
     * au hasard ; le code brut reste servi par {@code doublonDeCode}.
     */
    private CfgTier resoudreDoublon(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        List<CfgTier> trouves = tierRepository.findByCode(code.trim());
        return trouves.size() == 1 ? trouves.getFirst() : null;
    }

    // Des colonnes de la vue rendent "" au lieu de NULL quand il n'y a rien : videEnNull, sinon le
    // front afficherait un libellé vide au lieu de masquer le champ.
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

    private static String videEnNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    /** Saisie libre servie telle quelle, espaces de bord retirés ({@code TempField1} porte {@code "  TRAITÉ  "}). */
    private static String texteLibre(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /**
     * {@code StrHistory} par {@code ObjectId} : seule trace datée de « devenu prospect » et
     * « devenu prospection », la ligne étant mutée en place et OMRAN laissant
     * {@code ComProspection.UserUpdateDateTime} à {@code NULL}.
     */
    public Page<HistoriqueDto> historique(UUID prospectionId, Pageable pageable) {
        if (!prospectionRepository.existsById(prospectionId)) {
            throw new ResourceNotFoundException(PROSPECTION_INTROUVABLE + prospectionId);
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
                h.getStrStatusDescription(),
                null,
                null,
                null
        );
    }

    /** Sans utilisateur authentifié (formulaire public, import), auteur NULL : pas une erreur. */
    private void enregistrerHistorique(UUID prospectionId, TypeActionRecente type, String suite) {
        UtilisateurAffiche.Auteur auteur = utilisateurAffiche.courant();
        LocalDateTime maintenant = LocalDateTime.now(clock);
        String description = type.description(suite);
        StrHistory historique = new StrHistory();
        historique.setId(UUID.randomUUID());
        historique.setDocDate(maintenant);
        historique.setUserDate(maintenant);
        historique.setObjectId(prospectionId);
        historique.setDescription(description.length() > 255 ? description.substring(0, 255) : description);
        historique.setAdmUserId(auteur.compteId());
        historique.setAdmUserDescription(auteur.nom());
        historyRepository.save(historique);
    }

    /**
     * Prospection complète pour un prospect existant. Les cinq champs que l'écran OMRAN exige sont
     * exigés ici aussi, {@code commercialId} reste optionnel comme dans OMRAN — voir
     * {@code FEATURE-transformation-tranche-obligatoire.md}, « Champs obligatoires à la création ».
     */
    @Transactional
    public ProspectionDetailDto creerProspection(CreerProspectionRequest request) {
        Requis.champ(request.prospectId(), "prospectId est requis");
        CfgTier prospect = tierRepository.findById(request.prospectId())
                .orElseThrow(() -> new InvalidRequestException("Prospect (CfgTier) introuvable: " + request.prospectId()));

        // texteLibre() : une note blanche est refusée comme une note absente.
        String notes = Requis.champ(texteLibre(request.notes()), "notes est requis (champ Notes)");
        Requis.champ(request.origineId(), "origineId est requis (champ Source de contact)");
        Requis.champ(request.typeContactId(), "typeContactId est requis (champ Type)");
        Requis.champ(request.etapeId(), "etapeId est requis (champ Nature)");
        Requis.champ(request.trancheId(), "trancheId est requis (champ Tranche)");

        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode(genererCodeProspection());
        prospection.setDocDate(LocalDateTime.now(clock));
        prospection.setCfgProspect(prospect);
        // false et non NULL : la vue de travail OMRAN filtre en IsClosed = 0, que NULL ne
        // satisfait jamais — la prospection n'apparaîtrait dans aucune liste.
        prospection.setIsClosed(false);
        prospection.setIsOldCustomer(false);
        // Sans StrEntityId, aucun écran OMRAN ne voit la ligne : ses filtres StrEntityId <> … et
        // = … ne sont jamais vrais pour NULL (FEATURE-vues-legacy-prospection.md §8).
        prospection.setStrEntityId(idProspectionDeVente());

        rattacherReferentiels(prospection, request);
        rattacherTrancheALaCreation(prospection, prospect, request);
        appliquerMontantsDemandes(prospection, request);
        prospection.setNotes(notes);

        prospection = prospectionRepository.save(prospection);
        ecrireInteractionDeCreation(prospection);
        enregistrerHistorique(prospection.getId(), TypeActionRecente.PROSPECTION_CREEE,
                "prospect " + TierNomFormatter.nom(prospect));
        return detailEnrichi(prospection);
    }

    private void rattacherReferentiels(ComProspection prospection, CreerProspectionRequest request) {
        if (request.commercialId() != null) {
            CfgTier commercial = tierRepository.findById(request.commercialId())
                    .orElseThrow(() -> new InvalidRequestException("Commercial (CfgTier) introuvable: " + request.commercialId()));
            prospection.setCfgCommercial(commercial);
        }
        if (request.origineId() != null) {
            ComProspectionOrigin origine = prospectionOriginRepository.findById(request.origineId())
                    .orElseThrow(() -> new InvalidRequestException(ORIGINE_INTROUVABLE + request.origineId()));
            prospection.setComProspectionOrigin(origine);
        }
        if (request.origineSupportId() != null) {
            ComProspectionOriginSupport support = prospectionOriginSupportRepository.findById(request.origineSupportId())
                    .orElseThrow(() -> new InvalidRequestException(ORIGINE_SUPPORT_INTROUVABLE + request.origineSupportId()));
            prospection.setComProspectionOriginSupport(support);
        }
        if (request.vocationId() != null) {
            StkVocation vocation = vocationRepository.findById(request.vocationId())
                    .orElseThrow(() -> new InvalidRequestException(VOCATION_INTROUVABLE + request.vocationId()));
            prospection.setStkVocation(vocation);
        }
        if (request.typeContactId() != null) {
            ComActionMessageType typeContact = actionMessageTypeRepository.findById(request.typeContactId())
                    .orElseThrow(() -> new InvalidRequestException(TYPE_CONTACT_INTROUVABLE + request.typeContactId()));
            prospection.setComActionMessageType(typeContact);
        }
        if (request.actionCommercialeId() != null) {
            ComAction action = actionRepository.findById(request.actionCommercialeId())
                    .orElseThrow(() -> new InvalidRequestException(ACTION_INTROUVABLE + request.actionCommercialeId()));
            prospection.setComAction(action);
        }
        if (request.etapeId() != null) {
            ComProspectionKind kind = prospectionKindRepository.findById(request.etapeId())
                    .orElseThrow(() -> new InvalidRequestException(ETAPE_INTROUVABLE + request.etapeId()));
            prospection.setComProspectionKind(kind);
        }
    }

    // Même validation croisée que modifierTranchePrincipale : l'article appartient à la tranche.
    private void rattacherTrancheALaCreation(ComProspection prospection, CfgTier prospect,
                                             CreerProspectionRequest request) {
        if (request.trancheId() != null) {
            CfgTranche tranche = trancheRepository.findById(request.trancheId())
                    .orElseThrow(() -> new InvalidRequestException(TRANCHE_INTROUVABLE + request.trancheId()));
            perimetreService.verifierEcritureSurTranche(tranche,
                    "Création de prospection sur la tranche " + tranche.getCode());
            verifierUniciteProspectProjet(prospect, tranche, null);
            if (request.articleId() != null) {
                prospection.setStkItem(articleDeLaTranche(request.articleId(), tranche));
            }
            prospection.setStkHierarchy(tranche);
        } else if (request.articleId() != null) {
            throw new InvalidRequestException("articleId nécessite trancheId");
        }
    }

    private static void appliquerMontantsDemandes(ComProspection prospection, CreerProspectionRequest request) {
        if (request.surface() != null) prospection.setArea(request.surface());
        if (request.surfaceJusqua() != null) prospection.setAreaTo(request.surfaceJusqua());
        if (request.prix() != null) prospection.setPrice(request.prix());
        if (request.prixJusqua() != null) prospection.setPriceTo(request.prixJusqua());
        if (request.avance() != null) prospection.setAdvanceAmount(request.avance());
        if (request.financementFondsPropres() != null) prospection.setFundingOwnBase(request.financementFondsPropres());
        if (request.financementCredit() != null) prospection.setFundingCredit(request.financementCredit());
        if (request.financementAutre() != null) prospection.setFundingOther(request.financementAutre());
        if (request.ancienClient() != null) prospection.setIsOldCustomer(request.ancienClient());
    }

    /** L'article demandé, à condition qu'il appartienne à la tranche (400 sinon). */
    private StkItem articleDeLaTranche(UUID articleId, CfgTranche tranche) {
        StkItem article = stkItemRepository.findById(articleId)
                .orElseThrow(() -> new InvalidRequestException(ARTICLE_INTROUVABLE + articleId));
        if (article.getStkHierarchy() == null || !tranche.getId().equals(article.getStkHierarchy().getId())) {
            throw new InvalidRequestException(
                    "L'article " + article.getCode() + " n'appartient pas à la tranche " + tranche.getCode());
        }
        return article;
    }

    /** Vrai si la prospection porte un article qui n'est pas de {@code tranche}. */
    private static boolean articleHorsTranche(ComProspection prospection, CfgTranche tranche) {
        StkItem article = prospection.getStkItem();
        return article != null
                && (article.getStkHierarchy() == null || !tranche.getId().equals(article.getStkHierarchy().getId()));
    }

    // Résolu par code ; le repli sur la constante évite, sur un référentiel incomplet, une
    // prospection invisible d'OMRAN.
    private UUID idProspectionDeVente() {
        return strEntityRepository.findByCodeIn(List.of(CODE_STR_ENTITY_PROSPECTION_VENTE)).stream()
                .findFirst()
                .map(StrEntity::getId)
                .orElse(ID_STR_ENTITY_PROSPECTION_VENTE);
    }

    // Compteur AdmSequence « Prospection » d'OMRAN, global, sans préfixe de projet : YY/NNNNN.
    private String genererCodeProspection() {
        return generateurCode.suivant(GenerateurCodeSequence.SEQUENCE_PROSPECTION, 2, 5);
    }

    /** Vocation par défaut d'une pré-prospection : toutes les pré-prospections réelles la portent. */
    private static final String CODE_VOCATION_VENTE = "VENTE";

    private static final int DOUBLONS_MAX = 50;

    /**
     * Rejoue la création d'OMRAN relevée dans sa trace d'audit ; le contrôle de doublon
     * <b>ne bloque pas</b> (décision utilisateur). Voir {@code FEATURE-creation-pre-prospection.md} §1, §3.
     */
    @Transactional
    public PreProspectionCreeeDto creerPreProspection(CreerPreProspectionRequest request) {
        ComProspection p = nouvellePreProspection(request, LocalDateTime.now(clock));
        List<DoublonPreProspectionDto> doublons = visibles(doublonsContact(p)).stream()
                .map(d -> new DoublonPreProspectionDto(d.getId(), d.getCode(), d.getUserCreateDateTime()))
                .toList();
        return new PreProspectionCreeeDto(detailEnrichi(p), doublons);
    }

    /** Règle de doublon de {@link #creerPreProspection}, partagée avec l'import CSV ; {@code p} exclue. */
    public List<ComProspection> doublonsContact(ComProspection p) {
        return doublonsContact(p.getId(), p.getTempDescription(), p.getTempEMail(), p.getTempTel());
    }

    /** Même règle, pour un contact pas encore écrit (aperçu d'import) — {@code exclu} peut être nul. */
    public List<ComProspection> doublonsContact(UUID exclu, String nom, String email, String telephone) {
        return prospectionRepository.trouverDoublonsContact(
                exclu != null ? exclu : new UUID(0, 0),
                cleContact(nom),
                cleContact(email),
                PhoneNumberUtils.normalize(telephone),
                PageRequest.of(0, DOUBLONS_MAX));
    }

    /** Même contact soumis à la même minute que {@code minute} (tronquée à la minute), sans plafond. */
    public List<ComProspection> memeSoumission(String nom, String email, String telephone, LocalDateTime minute) {
        return prospectionRepository.trouverMemeSoumission(cleContact(nom), cleContact(email),
                PhoneNumberUtils.normalize(telephone), minute, minute.plusMinutes(1));
    }

    /**
     * Même prédicat que les listes. Un doublon hors périmètre n'est jamais nommé : ce serait
     * révéler un contact que le reste de l'API masque en 404.
     */
    public List<ComProspection> visibles(List<ComProspection> prospections) {
        if (prospections.isEmpty()) {
            return prospections;
        }
        List<UUID> ids = prospections.stream().map(ComProspection::getId).toList();
        Set<UUID> autorises = prospectionRepository.findAll(
                        Specification.<ComProspection>where((root, query, cb) -> root.get("id").in(ids))
                                .and(perimetreService.filtreProspections())).stream()
                .map(ComProspection::getId)
                .collect(Collectors.toSet());
        return prospections.stream().filter(p -> autorises.contains(p.getId())).toList();
    }

    private static String cleContact(String valeur) {
        return valeur == null ? "" : valeur.trim().toLowerCase(Locale.ROOT);
    }

    /** @param dateDocument maintenant pour une saisie, la colonne « Créé » de l'export pour un import */
    @Transactional
    public ComProspection ecrirePreProspection(CreerPreProspectionRequest request, LocalDateTime dateDocument) {
        return nouvellePreProspection(request, dateDocument);
    }

    private ComProspection nouvellePreProspection(CreerPreProspectionRequest request, LocalDateTime dateDocument) {
        String nom =Requis.champ(texteLibre(request.nom()), "nom est requis (champ Nom)");
        String telephone = texteLibre(request.telephone());
        String email = texteLibre(request.email());
        String notes = texteLibre(request.notes());
        longueurMax(nom, 255, "nom");
        longueurMax(telephone, 255, "telephone");
        longueurMax(email, 255, "email");
        longueurMax(notes, 1000, "notes");

        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode(genererCodeProspection());
        LocalDateTime maintenant = LocalDateTime.now(clock);
        p.setDocDate(dateDocument);
        p.setStrEntityId(DiscriminantPreProspection.ID_PRE_PROSPECTION);
        p.setTempDescription(nom);
        p.setTempTel(telephone);
        p.setTempEMail(email);
        p.setNotes(notes);
        // false plutôt que le NULL d'OMRAN : la ligne reste visible de sa vue de travail
        // (IsClosed = 0) sans dépendre de transformer.
        p.setIsClosed(false);
        p.setIsOldCustomer(false);
        p.setUserCreate(utilisateurAffiche.courant().nom());
        p.setUserCreateDateTime(maintenant);

        if (request.trancheId() != null) {
            CfgTranche tranche = trancheRepository.findById(request.trancheId())
                    .orElseThrow(() -> new InvalidRequestException(TRANCHE_INTROUVABLE + request.trancheId()));
            perimetreService.verifierEcritureSurTranche(tranche,
                    "Création de pré-prospection sur la tranche " + tranche.getCode());
            if (request.articleId() != null) {
                p.setStkItem(articleDeLaTranche(request.articleId(), tranche));
            }
            p.setStkHierarchy(tranche);
        } else if (request.articleId() != null) {
            throw new InvalidRequestException("articleId nécessite trancheId");
        }
        if (request.etapeId() != null) {
            p.setComProspectionKind(prospectionKindRepository.findById(request.etapeId())
                    .orElseThrow(() -> new InvalidRequestException(ETAPE_INTROUVABLE + request.etapeId())));
        }
        if (request.origineId() != null) {
            p.setComProspectionOrigin(prospectionOriginRepository.findById(request.origineId())
                    .orElseThrow(() -> new InvalidRequestException(ORIGINE_INTROUVABLE + request.origineId())));
        }
        if (request.origineSupportId() != null) {
            p.setComProspectionOriginSupport(prospectionOriginSupportRepository.findById(request.origineSupportId())
                    .orElseThrow(() -> new InvalidRequestException(ORIGINE_SUPPORT_INTROUVABLE + request.origineSupportId())));
        }
        if (request.typeContactId() != null) {
            p.setComActionMessageType(actionMessageTypeRepository.findById(request.typeContactId())
                    .orElseThrow(() -> new InvalidRequestException(TYPE_CONTACT_INTROUVABLE + request.typeContactId())));
        }
        if (request.actionCommercialeId() != null) {
            p.setComAction(actionRepository.findById(request.actionCommercialeId())
                    .orElseThrow(() -> new InvalidRequestException(ACTION_INTROUVABLE + request.actionCommercialeId())));
        }
        if (request.vocationId() != null) {
            p.setStkVocation(vocationRepository.findById(request.vocationId())
                    .orElseThrow(() -> new InvalidRequestException(VOCATION_INTROUVABLE + request.vocationId())));
        } else {
            // Défaut silencieux : une base sans référentiel de vocation (yesRemsTest) laisse NULL.
            vocationRepository.findFirstByCode(CODE_VOCATION_VENTE).ifPresent(p::setStkVocation);
        }

        p = prospectionRepository.save(p);
        enregistrerHistorique(p.getId(), TypeActionRecente.PRE_PROSPECTION_CREEE, nom);
        return p;
    }

    /** 400 plutôt que l'erreur de contrainte de la base au flush. */
    private static void longueurMax(String valeur, int max, String champ) {
        if (valeur != null && valeur.length() > max) {
            throw new InvalidRequestException(champ + " dépasse " + max + " caractères");
        }
    }

    @Transactional
    public ProspectionDetailDto creerProspect(UUID prospectionId, CreerProspectRequest request) {
        ComProspection prospection = prospectionRepository.findById(prospectionId)
                .orElseThrow(() -> new ResourceNotFoundException(PROSPECTION_INTROUVABLE + prospectionId));

        if (prospection.getCfgProspect() != null) {
            throw new InvalidRequestException("Cette prospection a déjà un prospect lié: " + prospectionId);
        }

        CfgTier tier = prospectService.createEntity(request);
        prospection.setCfgProspect(tier);
        prospection = prospectionRepository.save(prospection);
        enregistrerHistorique(prospectionId, TypeActionRecente.PROSPECT_CREE, TierNomFormatter.nom(tier));
        return detailEnrichi(prospection);
    }

    /**
     * Réponse « c'est l'une de ces personnes » à la liste de doublons — voir
     * {@code FEATURE-doublons-prospect-projet.md} §2. Avec une tranche déjà posée, le couple
     * (prospect, projet) se forme ici : la règle d'unicité s'applique.
     */
    @Transactional
    public ProspectionDetailDto lierProspect(UUID prospectionId, LierProspectRequest request) {
        ComProspection prospection = prospectionRepository.findById(prospectionId)
                .orElseThrow(() -> new ResourceNotFoundException(PROSPECTION_INTROUVABLE + prospectionId));

        if (prospection.getCfgProspect() != null) {
            throw new InvalidRequestException("Cette prospection a déjà un prospect lié: " + prospectionId);
        }
        Requis.champ(request.prospectId(), "prospectId est requis");
        CfgTier tier = tierRepository.findById(request.prospectId())
                .orElseThrow(() -> new InvalidRequestException("Prospect (CfgTier) introuvable: " + request.prospectId()));
        if (!ProspectService.estProspectOuClient(tier)) {
            throw new InvalidRequestException("Le tiers " + TierNomFormatter.nom(tier)
                    + " n'est pas une fiche prospect ou client (type "
                    + (tier.getCfgTierType() != null ? tier.getCfgTierType().getCode() : "absent") + ")");
        }

        verifierUniciteProspectProjet(tier, prospection.getStkHierarchy(), prospectionId);

        prospection.setCfgProspect(tier);
        prospection = prospectionRepository.save(prospection);
        enregistrerHistorique(prospectionId, TypeActionRecente.PROSPECT_RATTACHE, TierNomFormatter.nom(tier));
        return detailEnrichi(prospection);
    }

    /**
     * Une prospection <b>ouverte</b> par prospect × projet ; les re-prospections après clôture
     * passent. Voir {@code FEATURE-doublons-prospect-projet.md} §3. {@code exclu} : la ligne modifiée.
     */
    private void verifierUniciteProspectProjet(CfgTier prospect, CfgTranche tranche, UUID exclu) {
        UUID projetId = projetDe(tranche);
        if (prospect == null || projetId == null) {
            return;
        }
        List<ComProspection> ouvertes = prospectionRepository.trouverOuvertesMemeProspectMemeProjet(
                prospect.getId(), projetId, exclu != null ? exclu : new UUID(0L, 0L));
        if (ouvertes.isEmpty()) {
            return;
        }
        ComProspection existante = ouvertes.get(0);
        CfgTranche trancheExistante = existante.getStkHierarchy();
        throw new ProspectionExistanteException(TierNomFormatter.nom(prospect), new ProspectionExistanteDto(
                existante.getId(),
                existante.getCode(),
                existante.getDocDate(),
                existante.getComProspectionKind() != null ? existante.getComProspectionKind().getDescription() : null,
                TierNomFormatter.nom(existante.getCfgCommercial()),
                trancheExistante != null ? trancheExistante.getCode() : null,
                trancheExistante != null && trancheExistante.getCfgProject() != null
                        ? trancheExistante.getCfgProject().getCode() : null));
    }

    private static UUID projetDe(CfgTranche tranche) {
        return tranche != null && tranche.getCfgProject() != null ? tranche.getCfgProject().getId() : null;
    }

    /**
     * Tranche obligatoire à la sortie, plus strict que le légataire : fournie ici, ou déjà portée.
     * Voir {@code FEATURE-transformation-tranche-obligatoire.md}.
     */
    @Transactional
    public ProspectionDetailDto transformer(UUID id, TransformerProspectionRequest request) {
        ComProspection prospection = prospectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(PROSPECTION_INTROUVABLE + id));

        if (prospection.getCfgProspect() == null) {
            throw new InvalidRequestException(
                    "Un prospect doit être créé (POST /api/pre-prospections/{id}/creer-prospect) avant de transformer: " + id);
        }
        Requis.champ(request.commercialId(), "commercialId est requis");

        CfgTier commercial = tierRepository.findById(request.commercialId())
                .orElseThrow(() -> new InvalidRequestException("Commercial (CfgTier) introuvable: " + request.commercialId()));

        rattacherTrancheALaTransformation(prospection, request);

        // Le couple (prospect, projet) devient une prospection ici.
        verifierUniciteProspectProjet(prospection.getCfgProspect(), prospection.getStkHierarchy(), id);

        prospection.setCfgCommercial(commercial);
        // Commercial référent du tiers (lu par la Hotline) : renseigné s'il manque, jamais écrasé.
        CfgTier prospect = prospection.getCfgProspect();
        if (prospect.getCfgCommercial() == null) {
            prospect.setCfgCommercial(commercial);
            tierRepository.save(prospect);
        }
        // Bascule de type en place, comme OMRAN (FEATURE-creation-pre-prospection.md §1). Une ligne
        // sans type est typée au passage ; un type d'une autre nature (location…) n'est pas touché.
        if (prospection.getStrEntityId() == null || DiscriminantPreProspection.estPreProspection(prospection)) {
            prospection.setStrEntityId(idProspectionDeVente());
        }
        // La vue de travail OMRAN filtre en IsClosed = 0 ; OMRAN corrige lui aussi le NULL à la bascule.
        if (prospection.getIsClosed() == null) {
            prospection.setIsClosed(false);
        }
        prospection = prospectionRepository.save(prospection);
        enregistrerHistorique(id, TypeActionRecente.TRANSFORMEE_EN_PROSPECTION,
                "commercial assigné, " + TierNomFormatter.nom(commercial));
        return detailEnrichi(prospection);
    }

    private void rattacherTrancheALaTransformation(ComProspection prospection, TransformerProspectionRequest request) {
        if (request.trancheId() != null) {
            CfgTranche tranche = trancheRepository.findById(request.trancheId())
                    .orElseThrow(() -> new InvalidRequestException(TRANCHE_INTROUVABLE + request.trancheId()));
            perimetreService.verifierEcritureSurTranche(tranche,
                    "Transformation de prospection sur la tranche " + tranche.getCode());
            if (request.articleId() != null) {
                prospection.setStkItem(articleDeLaTranche(request.articleId(), tranche));
            } else if (articleHorsTranche(prospection, tranche)) {
                // Article de l'ancienne tranche : désassigné plutôt que laissé incohérent.
                prospection.setStkItem(null);
            }
            prospection.setStkHierarchy(tranche);
        } else if (request.articleId() != null) {
            throw new InvalidRequestException("articleId nécessite trancheId");
        } else if (prospection.getStkHierarchy() == null) {
            throw new InvalidRequestException(
                    "Une tranche (projet) est obligatoire pour transformer en prospection : "
                    + "fournir trancheId, ou rattacher d'abord une tranche (PATCH /api/prospections/{id}/tranche-principale).");
        }
    }

    /** Pas de table de transitions : seule la valeur d'arrivée compte ({@link #ETAPE_APRES_VISITE} → remerciement). */
    @Transactional
    public ProspectionDetailDto changerEtape(UUID id, ChangerEtapeRequest request) {
        ComProspection prospection = prospectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(PROSPECTION_INTROUVABLE + id));

        Requis.champ(request.etapeId(), "etapeId est requis");
        ComProspectionKind kind = prospectionKindRepository.findById(request.etapeId())
                .orElseThrow(() -> new InvalidRequestException(ETAPE_INTROUVABLE + request.etapeId()));

        ComProspectionKind ancienne = prospection.getComProspectionKind();
        prospection.setComProspectionKind(kind);
        if (request.note() != null) {
            prospection.setNotes(request.note());
        }
        ComProspection enregistree = prospectionRepository.save(prospection);
        if (ancienne == null || !ancienne.getId().equals(kind.getId())) {
            enregistrerHistorique(id, TypeActionRecente.ETAPE_CHANGEE,
                    codeEtape(ancienne) + " → " + codeEtape(kind));
        }

        // La livraison n'a lieu qu'après commit : une étape annulée ne remercie personne.
        if (estEtapeApresVisite(kind)) {
            publisher.publishEvent(new VisiteEffectuee(enregistree.getId()));
        }
        return detailEnrichi(enregistree);
    }

    private static String codeEtape(ComProspectionKind kind) {
        return kind != null && StringUtils.hasText(kind.getCode()) ? kind.getCode().trim() : "—";
    }

    // Sans casse ni espaces de bord : le référentiel voisin porte des codes suffixés d'un espace
    // ("Numéro erroné ").
    private static boolean estEtapeApresVisite(ComProspectionKind kind) {
        return kind != null && kind.getCode() != null
                && ETAPE_APRES_VISITE.equalsIgnoreCase(kind.getCode().trim());
    }

    /** Mise à jour partielle : un champ {@code null} reste inchangé. */
    @Transactional
    public ProspectionDetailDto modifierProspection(UUID id, ModifierProspectionRequest request) {
        ComProspection prospection = prospectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(PROSPECTION_INTROUVABLE + id));

        if (request.origineId() != null) {
            ComProspectionOrigin origine = prospectionOriginRepository.findById(request.origineId())
                    .orElseThrow(() -> new InvalidRequestException(ORIGINE_INTROUVABLE + request.origineId()));
            prospection.setComProspectionOrigin(origine);
        }
        if (request.origineSupportId() != null) {
            ComProspectionOriginSupport support = prospectionOriginSupportRepository.findById(request.origineSupportId())
                    .orElseThrow(() -> new InvalidRequestException(ORIGINE_SUPPORT_INTROUVABLE + request.origineSupportId()));
            prospection.setComProspectionOriginSupport(support);
        }
        if (request.vocationId() != null) {
            StkVocation vocation = vocationRepository.findById(request.vocationId())
                    .orElseThrow(() -> new InvalidRequestException(VOCATION_INTROUVABLE + request.vocationId()));
            prospection.setStkVocation(vocation);
        }
        if (request.typeContactId() != null) {
            ComActionMessageType typeContact = actionMessageTypeRepository.findById(request.typeContactId())
                    .orElseThrow(() -> new InvalidRequestException(TYPE_CONTACT_INTROUVABLE + request.typeContactId()));
            prospection.setComActionMessageType(typeContact);
        }
        if (request.actionCommercialeId() != null) {
            ComAction action = actionRepository.findById(request.actionCommercialeId())
                    .orElseThrow(() -> new InvalidRequestException(ACTION_INTROUVABLE + request.actionCommercialeId()));
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
     * Tranche et article principaux ensemble : l'article doit appartenir à la tranche, ce que
     * respectent toutes les prospections réelles. Article omis et hors nouvelle tranche :
     * désassigné. Les « autres tranches » sont dans {@code ProspectionQualificationService}.
     */
    @Transactional
    public ProspectionDetailDto modifierTranchePrincipale(UUID id, ModifierTranchePrincipaleRequest request) {
        ComProspection prospection = prospectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(PROSPECTION_INTROUVABLE + id));

        Requis.champ(request.trancheId(), "trancheId est requis");
        CfgTranche tranche = trancheRepository.findById(request.trancheId())
                .orElseThrow(() -> new InvalidRequestException(TRANCHE_INTROUVABLE + request.trancheId()));
        // Sinon le périmètre se contournerait en déplaçant la prospection vers un autre projet.
        perimetreService.verifierEcritureSurTranche(tranche,
                "Rattachement de prospection à la tranche " + tranche.getCode());
        // Seul un changement de projet forme un nouveau couple (prospect, projet).
        if (projetDe(tranche) != null && !projetDe(tranche).equals(projetDe(prospection.getStkHierarchy()))) {
            verifierUniciteProspectProjet(prospection.getCfgProspect(), tranche, id);
        }

        if (request.articleId() != null) {
            StkItem article = stkItemRepository.findById(request.articleId())
                    .orElseThrow(() -> new InvalidRequestException(ARTICLE_INTROUVABLE + request.articleId()));
            if (article.getStkHierarchy() == null || !tranche.getId().equals(article.getStkHierarchy().getId())) {
                throw new InvalidRequestException(
                        "L'article " + article.getCode() + " n'appartient pas à la tranche " + tranche.getCode());
            }
            prospection.setStkItem(article);
        } else if (prospection.getStkItem() != null
                && (prospection.getStkItem().getStkHierarchy() == null
                    || !tranche.getId().equals(prospection.getStkItem().getStkHierarchy().getId()))) {
            // Article de l'ancienne tranche : désassigné plutôt que laissé incohérent.
            prospection.setStkItem(null);
        }

        prospection.setStkHierarchy(tranche);
        return detailEnrichi(prospectionRepository.save(prospection));
    }

    /**
     * Séquence d'OMRAN : clôture, puis désarmement des relances, sans quoi l'archivée alimenterait
     * encore le tableau de bord des relances. Voir {@code FEATURE-desarchiver-prospection.md}.
     */
    @Transactional
    public ProspectionDetailDto archiver(UUID id, ArchiverProspectionRequest request) {
        ComProspection prospection = prospectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(PROSPECTION_INTROUVABLE + id));

        if (request.raisonId() == null && !StringUtils.hasText(request.raisonTexte())) {
            throw new InvalidRequestException("raisonId ou raisonTexte est requis");
        }

        String raison = null;
        if (request.raisonId() != null) {
            ComProspectionClosingReason reason = closingReasonRepository.findById(request.raisonId())
                    .orElseThrow(() -> new InvalidRequestException("ComProspectionClosingReason introuvable: " + request.raisonId()));
            prospection.setComProspectionClosingReason(reason);
            raison = StringUtils.hasText(reason.getDescription()) ? reason.getDescription().trim() : reason.getCode();
        }
        if (!StringUtils.hasText(raison)) {
            raison = request.raisonTexte();
        }
        if (StringUtils.hasText(request.raisonTexte())) {
            prospection.setClosingReasonText(request.raisonTexte());
        }
        LocalDateTime maintenant = LocalDateTime.now(clock);
        prospection.setIsClosed(true);
        prospection.setClosingDate(maintenant);
        marquerModification(prospection, maintenant);

        ComProspection sauvee = prospectionRepository.save(prospection);
        alerteRelanceService.desarmerToutes(sauvee.getId());
        enregistrerHistorique(id, TypeActionRecente.ARCHIVEE, raison);
        return detailEnrichi(sauvee);
    }

    /**
     * Comme OMRAN, seul {@code IsClosed} change : date et raison de clôture restent, relances non
     * réarmées. « Archivée » se lit donc sur {@code IsClosed}, jamais sur {@code ClosingDate}.
     * Le 409 est propre à l'API : OMRAN ne refuse rien.
     */
    @Transactional
    public ProspectionDetailDto desarchiver(UUID id) {
        ComProspection prospection = prospectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(PROSPECTION_INTROUVABLE + id));

        if (!Boolean.TRUE.equals(prospection.getIsClosed())) {
            throw new ConflictException("La prospection " + id + " n'est pas archivée");
        }

        prospection.setIsClosed(false);
        marquerModification(prospection, LocalDateTime.now(clock));

        ComProspection sauvee = prospectionRepository.save(prospection);
        enregistrerHistorique(id, TypeActionRecente.DESARCHIVEE, null);
        return detailEnrichi(sauvee);
    }

    /** OMRAN réécrit ces deux colonnes à chaque modification (trace d'audit). */
    private void marquerModification(ComProspection prospection, LocalDateTime quand) {
        prospection.setUserUpdate(utilisateurAffiche.courant().nom());
        prospection.setUserUpdateDateTime(quand);
    }

    public Page<InteractionDto> findInteractions(UUID prospectionId, Pageable pageable) {
        if (!prospectionRepository.existsById(prospectionId)) {
            throw new ResourceNotFoundException(PROSPECTION_INTROUVABLE + prospectionId);
        }
        return actionMessageRepository.findByComProspectionIdOrderByDocDateDesc(prospectionId, pageable)
                .map(ProspectionService::toInteractionDto);
    }

    @Transactional
    public InteractionDto addInteraction(UUID prospectionId, CreerInteractionRequest request) {
        ComProspection prospection = prospectionRepository.findById(prospectionId)
                .orElseThrow(() -> new ResourceNotFoundException(PROSPECTION_INTROUVABLE + prospectionId));

        ComActionMessageType actionType = request.actionTypeId() == null ? null
                : actionMessageTypeRepository.findById(request.actionTypeId())
                .orElseThrow(() -> new InvalidRequestException(TYPE_CONTACT_INTROUVABLE + request.actionTypeId()));
        ComActionMessageSubCategory resultat = request.resultatId() == null ? null
                : actionMessageSubCategoryRepository.findById(request.resultatId())
                .orElseThrow(() -> new InvalidRequestException("ComActionMessageSubCategory introuvable: " + request.resultatId()));
        ReglesInteraction.verifier(actionType, resultat, request);

        LocalDateTime maintenant = LocalDateTime.now(clock);
        UtilisateurAffiche.Auteur resolu = utilisateurAffiche.courant();
        AdmUser auteur = resolu.compte();
        String nomAuteur = resolu.nom();

        ComActionMessage message = new ComActionMessage();
        message.setId(UUID.randomUUID());
        message.setComProspection(prospection);
        message.setDocDate(maintenant);

        // La note va dans AlertMessage, là où OMRAN l'écrit et la lit ; Description est un libellé
        // « date + auteur » régénéré à l'identique. Voir FIX-interaction-note.md §2.
        message.setAlertMessage(request.description());
        message.setDescription(libelleInteraction(maintenant, nomAuteur));

        // Auteur pris du jeton, jamais du corps (sinon KPI faussés) ; AdmUser le porte de façon
        // fiable, pas CfgEmployeeId (FIX-interaction-note.md §2).
        message.setAdmUser(auteur);
        message.setCfgEmployee(auteur != null ? auteur.getCfgEmployee() : null);
        message.setUserName(nomAuteur);
        message.setUserCreate(nomAuteur);
        message.setUserCreateDateTime(maintenant);

        message.setIsIn(!Boolean.FALSE.equals(request.entrante()));
        message.setAlertValidityDate(request.dateAlerte());

        // Drapeaux posés comme les saisies manuelles du légataire : la vue calcule le dernier
        // échange avec AND IsSystem = 0, qu'un NULL ne satisfait pas (FEATURE-conformite-legataire.md §1).
        message.setIsSystem(false);
        // 1 signale une note à lire, pas une interaction.
        message.setIsNotRead(false);
        message.setIsUnpaid(false);
        // Passe à 1 quand une relance suivante ou la vente la désarme, jamais à la création.
        message.setWithoutRevival(false);
        message.setCfgTier(prospection.getCfgProspect());
        message.setComActionMessageType(actionType);
        message.setComActionMessageSubCategory(resultat);
        // Jamais fournie par le client : celle du résultat, sinon celle de l'écran parent, comme OMRAN.
        // Sans catégorie, la vue ComSaleProspectionView ignore l'interaction.
        message.setComActionMessageCategory(Optional.ofNullable(resultat)
                .map(ComActionMessageSubCategory::getComActionMessageCategory)
                .orElseGet(() -> actionMessageCategoryRepository.findById(ContexteInteraction.CATEGORIE_PROSPECTION)
                        .orElse(null)));

        if (ReglesInteraction.exigeUneDateDeRdv(resultat)) {
            AdmEvent evenement = new AdmEvent();
            evenement.setId(UUID.randomUUID());
            evenement.setSubject(request.description());
            evenement.setStartHour(request.dateRdv());
            evenement.setComActionMessageSubCategory(resultat);
            message.setAdmEvent(admEventRepository.save(evenement));
        }

        ComActionMessage enregistree = actionMessageRepository.save(message);

        // Sans date d'alerte, rien n'est touché : règle du légataire (voir AlerteRelanceService).
        if (enregistree.getAlertValidityDate() != null) {
            alerteRelanceService.poserAlerte(enregistree);
        }

        // Seulement si un rendez-vous a été créé ; livraison après commit (voir VisitePlanifiee).
        if (enregistree.getAdmEvent() != null) {
            publisher.publishEvent(new VisitePlanifiee(enregistree.getId()));
        }

        return toInteractionDto(enregistree);
    }

    /**
     * L'interaction qu'OMRAN écrit toujours juste après une création de prospection. Voir
     * {@code FIX-creation-prospect-prospection.md} §4 ; une catégorie introuvable n'empêche rien.
     */
    private void ecrireInteractionDeCreation(ComProspection prospection) {
        LocalDateTime maintenant = LocalDateTime.now(clock);
        UtilisateurAffiche.Auteur resolu = utilisateurAffiche.courant();
        AdmUser auteur = resolu.compte();
        String nomAuteur = resolu.nom();

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

        // IsSystem = 1, à l'inverse d'une saisie : créer une prospection n'est pas un échange, la
        // vue doit l'exclure du dernier échange — comme le fait OMRAN.
        message.setIsSystem(true);
        message.setWithoutRevival(true);
        message.setIsNotRead(false);
        message.setIsUnpaid(false);
        message.setAlertValidityDate(null);
        message.setAlertMessage(prospection.getNotes());

        actionMessageRepository.save(message);
    }

    // Sans auteur connu, la date seule plutôt qu'un « null » traîné dans le libellé.
    private static String libelleInteraction(LocalDateTime date, String nomAuteur) {
        String horodatage = date.format(FORMAT_LIBELLE);
        return StringUtils.hasText(nomAuteur) ? horodatage + " " + nomAuteur : horodatage;
    }

    /**
     * Employé lié, sinon {@code UserName} puis {@code UserCreate} : l'employé manque souvent
     * (voir {@code FIX-auteur-interactions.md}). Partagé avec {@code HotlineService}, la règle
     * ne doit vivre qu'ici.
     */
    public static String nomAuteur(ComActionMessage m) {
        String employe = TierNomFormatter.nom(m.getCfgEmployee());
        if (StringUtils.hasText(employe)) {
            return employe;
        }
        return StringUtils.hasText(m.getUserName()) ? m.getUserName() : m.getUserCreate();
    }

    private static InteractionDto toInteractionDto(ComActionMessage m) {
        CfgTranche tranche = ContexteInteraction.tranche(m);
        CfgProject projet = tranche != null ? tranche.getCfgProject() : null;
        return new InteractionDto(
                m.getId(),
                m.getDocDate(),
                // AlertMessage porte la note ; Description n'est qu'un libellé (FIX-interaction-note.md).
                m.getAlertMessage(),
                m.getDescription(),
                m.getIsIn(),
                m.getCfgEmployee() != null ? m.getCfgEmployee().getId() : null,
                nomAuteur(m),
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

    /** Alertes, activité et types chargés pour toute la page en une requête chacun, pas ligne par ligne. */
    private Page<ProspectionListItemDto> versListe(Page<ComProspection> page,
                                                   Set<ColonneProspection> colonnes) {
        List<UUID> ids = page.getContent().stream().map(ComProspection::getId).toList();
        // IN () est une erreur de syntaxe SQL : ne pas interroger sur une page vide.
        Map<UUID, DerniereAlerteProjection> alertes = ids.isEmpty()
                ? Map.of()
                : actionMessageRepository.dernieresAlertes(ids).stream()
                        .collect(Collectors.toMap(DerniereAlerteProjection::getProspectionId, a -> a));
        // Vue interrogée seulement si une colonne d'activité est demandée.
        Map<UUID, ComSaleProspectionView> activite = ids.isEmpty() || !ColonneProspection.exigeLaVue(colonnes)
                ? Map.of()
                : prospectionViewRepository.findByIdIn(ids).stream()
                        .collect(Collectors.toMap(ComSaleProspectionView::getId, v -> v));
        // Idem pour StrEntity : seulement si une colonne de type est demandée.
        Map<UUID, StrEntity> types = chargerTypes(page.getContent(), colonnes);
        return page.map(p -> toListItem(p, alertes.get(p.getId()), activite.get(p.getId()),
                typeDe(types, p), colonnes));
    }

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

    private static StrEntity typeDe(Map<UUID, StrEntity> types, ComProspection p) {
        return p.getStrEntityId() == null ? null : types.get(p.getStrEntityId());
    }

    /**
     * Fiche tiers, sinon nom du premier contact (une pré-prospection n'a pas de fiche). Seule
     * règle, partagée avec les relances et le calendrier ; {@code p} peut être {@code null}.
     */
    public static String nomProspect(CfgTier prospect, ComProspection p) {
        String nomFiche = TierNomFormatter.nom(prospect);
        if (StringUtils.hasText(nomFiche)) {
            return nomFiche;
        }
        if (p != null && StringUtils.hasText(p.getTempDescription())) {
            return p.getTempDescription().trim();
        }
        return null;
    }

    /**
     * Fiche (mobile, puis fixe), puis numéro du premier contact : aucune des deux sources ne
     * suffit seule. Seule règle, partagée avec les relances.
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
     * Même repli que {@link #telephoneProspect} ; seule règle, partagée avec les envois. Pas de
     * contrôle de forme : afficher une saisie fautive aide à la corriger, c'est à l'appelant qui
     * <i>envoie</i> d'exiger davantage.
     */
    public static String emailProspect(CfgTier prospect, ComProspection p) {
        if (prospect != null && StringUtils.hasText(prospect.getEMail())) {
            return prospect.getEMail();
        }
        return StringUtils.hasText(p.getTempEMail()) ? p.getTempEMail() : null;
    }

    /**
     * Seul mappeur de {@link ProspectionListItemDto}, partagé par la hotline et la fiche prospect.
     * Champs d'alerte nuls : seul {@link #versListe} les charge, pour toute la page.
     */
    public static ProspectionListItemDto toListItem(ComProspection p) {
        return toListItem(p, null, null, null, Set.of());
    }

    /** {@code vue} et {@code type} : {@code null} si non demandés <b>ou</b> absents — même sortie. */
    private static ProspectionListItemDto toListItem(ComProspection p, DerniereAlerteProjection alerte,
                                                     ComSaleProspectionView vue,
                                                     StrEntity type,
                                                     Set<ColonneProspection> colonnes) {
        // Associations déréférencées seulement si leur colonne est demandée : chacune coûte un
        // chargement paresseux par ligne (deux pour la société).
        CfgTranche tranche = p.getStkHierarchy();
        boolean societeDemandee = colonnes.contains(ColonneProspection.SOCIETE_ID)
                || colonnes.contains(ColonneProspection.SOCIETE_CODE)
                || colonnes.contains(ColonneProspection.SOCIETE_LIBELLE);
        CfgProject projet = societeDemandee ? Champs.de(tranche, CfgTranche::getCfgProject) : null;
        CfgCompany societe = Champs.de(projet, CfgProject::getCfgCompany);
        CfgTier prospect = p.getCfgProspect();
        CfgTier commercial = p.getCfgCommercial();
        return new ProspectionListItemDto(
                p.getId(),
                p.getCode(),
                p.getDocDate(),
                p.getTempDescription(),
                Champs.de(prospect, CfgTier::getId),
                TierNomFormatter.nom(prospect),
                Champs.de(commercial, CfgTier::getId),
                TierNomFormatter.nom(commercial),
                Champs.de(p.getComProspectionOrigin(), ComProspectionOrigin::getCode),
                Champs.de(p.getComProspectionKind(), ComProspectionKind::getCode),
                p.getIsClosed(),
                p.getPrice(),
                p.getArea(),
                Champs.de(tranche, CfgTranche::getId),
                Champs.de(tranche, CfgTranche::getCode),
                Champs.de(tranche, CfgTranche::getDescription),
                telephoneProspect(prospect, p),
                emailProspect(prospect, p),
                Champs.de(alerte, DerniereAlerteProjection::getDateAlerte),
                Champs.de(alerte, DerniereAlerteProjection::getMessage),
                Champs.de(vue, ComSaleProspectionView::getInactivityTimeout),
                Champs.de(vue, ComSaleProspectionView::getActionsMessageNumber),
                Champs.de(vue, ComSaleProspectionView::getLastActionDate),
                Champs.de(vue, ComSaleProspectionView::getLastCommunicationDate),
                videEnNull(Champs.de(vue, ComSaleProspectionView::getLastCommunicationResult)),
                Champs.de(vue, ComSaleProspectionView::getNbrVisit),
                Champs.de(vue, ComSaleProspectionView::getLastVisitDate),
                Champs.de(vue, ComSaleProspectionView::getCfgProjectId),
                videEnNull(Champs.de(vue, ComSaleProspectionView::getCfgProjectCode)),
                videEnNull(Champs.de(vue, ComSaleProspectionView::getCfgProjectDescription)),
                videEnNull(Champs.de(vue, ComSaleProspectionView::getCfgTierIdNumber)),
                Champs.de(vue, ComSaleProspectionView::getComFolderId),
                videEnNull(Champs.de(vue, ComSaleProspectionView::getComFolderCode)),
                videEnNull(Champs.de(vue, ComSaleProspectionView::getStatus)),
                Champs.de(prospect, CfgTier::getCode),
                Champs.de(prospect, CfgTier::getMobile),
                Champs.de(prospect, CfgTier::getMobileIndicatif),
                Champs.de(prospect, CfgTier::getEMail),
                Champs.de(commercial, CfgTier::getCode),
                p.getIsOldCustomer(),
                p.getClosingDate(),
                siDemandee(colonnes, ColonneProspection.RAISON_FERMETURE, () -> Champs.de(
                        p.getComProspectionClosingReason(), ComProspectionClosingReason::getDescription)),
                siDemandee(colonnes, ColonneProspection.TYPE_PROSPECT, () -> Champs.de(
                        prospect, CfgTier::getCfgTierType, CfgTierType::getCode)),
                siDemandee(colonnes, ColonneProspection.ORIGINE_SUPPORT, () -> Champs.de(
                        p.getComProspectionOriginSupport(), ComProspectionOriginSupport::getDescription)),
                siDemandee(colonnes, ColonneProspection.ACTION_COMMERCIALE, () -> Champs.de(
                        p.getComAction(), ComAction::getDescription)),
                siDemandee(colonnes, ColonneProspection.PAYS, () -> Champs.de(
                        p.getAdmCountry(), AdmCountry::getDescription)),
                Champs.de(societe, CfgCompany::getId),
                Champs.de(societe, CfgCompany::getCode),
                Champs.de(societe, CfgCompany::getDescription),
                p.getUserCreate(),
                p.getUserCreateDateTime(),
                p.getUserUpdate(),
                p.getUserUpdateDateTime(),
                // Connu sans jointure, mais conditionné comme ses deux voisins pour rester cohérent.
                siDemandee(colonnes, ColonneProspection.TYPE_PROSPECTION_ID, p::getStrEntityId),
                Champs.de(type, StrEntity::getCode),
                Champs.de(type, StrEntity::getDescription)
        );
    }

    /** Valeur lue seulement si sa colonne est demandée : la lecture peut coûter un chargement paresseux. */
    private static <T> T siDemandee(Set<ColonneProspection> colonnes, ColonneProspection colonne,
                                    Supplier<T> lecture) {
        return colonnes.contains(colonne) ? lecture.get() : null;
    }

    /** Passer par {@link #detailEnrichi(ComProspection)} pour une réponse d'API. */
    private static ProspectionDetailDto toDetail(ComProspection p, ActiviteProspectionDto activite,
                                                 StrEntity type, CfgTier doublonDe) {
        // Chaque association lue une fois, pour ne pas traverser le proxy LAZY à chaque champ.
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
        // Société jamais saisie sur la prospection : dérivée de la tranche, comme la vue légataire.
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
                // Texte brut tel quel ; le marqueur normalisé est null quand le texte n'en est pas un.
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

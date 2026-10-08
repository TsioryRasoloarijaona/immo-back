package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.domain.AgregatDemande;
import com.tsix.immocrm.domain.ChampDateDossier;
import com.tsix.immocrm.domain.Chemin;
import com.tsix.immocrm.domain.Granularite;
import com.tsix.immocrm.domain.TriGroupe;
import com.tsix.immocrm.domain.ColonneDossier;
import com.tsix.immocrm.domain.EtapeMilestone;
import com.tsix.immocrm.domain.SchemaFinancementCalculator;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.domain.TransitionsValidees;
import com.tsix.immocrm.dto.common.GroupeDto;
import com.tsix.immocrm.dto.dossier.ArticleSummaryDto;
import com.tsix.immocrm.dto.dossier.CategorieMontantDto;
import com.tsix.immocrm.dto.dossier.SyntheseMontantDto;
import com.tsix.immocrm.dto.dossier.ChangerStatutRequest;
import com.tsix.immocrm.dto.dossier.CreerDossierRequest;
import com.tsix.immocrm.dto.dossier.DossierDetailDto;
import com.tsix.immocrm.dto.dossier.DossierListItemDto;
import com.tsix.immocrm.dto.dossier.EtapeSuivanteDto;
import com.tsix.immocrm.dto.dossier.LigneSchemaFinancementDto;
import com.tsix.immocrm.dto.dossier.MontantsDossierDto;
import com.tsix.immocrm.dto.dossier.SchemaFinancementDto;
import com.tsix.immocrm.dto.dossier.SimulerSchemaFinancementRequest;
import com.tsix.immocrm.entities.adm.AdmSequence;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComPaymentMethod;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionKind;
import com.tsix.immocrm.entities.com.ComProspectionOrigin;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkBuilding;
import com.tsix.immocrm.entities.stk.StkFloor;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkPresentationType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.str.StrHistory;
import com.tsix.immocrm.entities.str.StrFolderStatusHistory;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.InvalidStatutTransitionException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComPaymentMethodRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.str.StrHistoryRepository;
import com.tsix.immocrm.repositories.str.StrFolderStatusHistoryRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.services.groupement.GroupementService;
import com.tsix.immocrm.services.groupement.GroupementService.ResultatGroupement;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.services.tranche.AppartenanceTranche;
import com.tsix.immocrm.services.prospection.AlerteRelanceService;
import com.tsix.immocrm.services.sequence.GenerateurCodeSequence;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.Requis;
import com.tsix.immocrm.utils.TierNomFormatter;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import static java.util.Objects.requireNonNullElse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

@Service
@Transactional(readOnly = true)
public class DossierService {

    /** Texte exact d'OMRAN : c'est lui qui distingue une prospection vendue d'une prospection perdue. */
    public static final String TEXTE_CLOTURE_VENTE = "Création dossier de vente";

    private static final String DOSSIER_INTROUVABLE = "Dossier introuvable: ";
    private static final String ARTICLE_INTROUVABLE = "Article introuvable: ";

    private final ComFolderRepository folderRepository;
    private final StkItemRepository stkItemRepository;
    private final ComProspectionRepository prospectionRepository;
    private final StrStatusRepository strStatusRepository;
    private final ComDocumentRepository documentRepository;
    private final StlSettlementLineRepository settlementLineRepository;
    private final ComPaymentMethodRepository paymentMethodRepository;
    private final CfgTierRepository tierRepository;
    private final ComSettlementCategoryRepository settlementCategoryRepository;
    private final StrFolderStatusHistoryRepository statusHistoryRepository;
    private final GenerateurCodeSequence generateurCode;
    private final PerimetreCommercialService perimetreService;
    private final GroupementService groupementService;
    private final ComActionMessageRepository actionMessageRepository;
    private final StrHistoryRepository strHistoryRepository;
    private final UtilisateurAffiche utilisateurAffiche;
    private final AlerteRelanceService alerteRelanceService;
    private final Clock clock;

    public DossierService(ComFolderRepository folderRepository, StkItemRepository stkItemRepository,
                           ComProspectionRepository prospectionRepository, StrStatusRepository strStatusRepository,
                           ComDocumentRepository documentRepository, StlSettlementLineRepository settlementLineRepository,
                           ComPaymentMethodRepository paymentMethodRepository, CfgTierRepository tierRepository,
                           ComSettlementCategoryRepository settlementCategoryRepository,
                           StrFolderStatusHistoryRepository statusHistoryRepository,
                           GenerateurCodeSequence generateurCode,
                           PerimetreCommercialService perimetreService,
                           GroupementService groupementService,
                           ComActionMessageRepository actionMessageRepository,
                           StrHistoryRepository strHistoryRepository,
                           UtilisateurAffiche utilisateurAffiche,
                           AlerteRelanceService alerteRelanceService,
                           Clock clock) {
        this.clock = clock;
        this.alerteRelanceService = alerteRelanceService;
        this.perimetreService = perimetreService;
        this.groupementService = groupementService;
        this.actionMessageRepository = actionMessageRepository;
        this.strHistoryRepository = strHistoryRepository;
        this.utilisateurAffiche = utilisateurAffiche;
        this.folderRepository = folderRepository;
        this.stkItemRepository = stkItemRepository;
        this.prospectionRepository = prospectionRepository;
        this.documentRepository = documentRepository;
        this.settlementCategoryRepository = settlementCategoryRepository;
        this.strStatusRepository = strStatusRepository;
        this.settlementLineRepository = settlementLineRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.tierRepository = tierRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.generateurCode = generateurCode;
    }

    /**
     * {@code "APF 2026/00380"} : compteur {@code AdmSequence} unique pour tous les projets (pas de
     * remise à zéro par préfixe) ; projet lu via la tranche, {@code StkItem.CfgProjectId} étant vide.
     */
    private String genererCodeDossier(StkItem premierArticle) {
        String prefixe = "DOS";
        if (premierArticle.getStkHierarchy() != null && premierArticle.getStkHierarchy().getCfgProject() != null
                && premierArticle.getStkHierarchy().getCfgProject().getCode() != null) {
            prefixe = premierArticle.getStkHierarchy().getCfgProject().getCode();
        }
        // Année sur 4 chiffres ici, contrairement au tiers et à la prospection.
        return prefixe + " " + generateurCode.suivant(GenerateurCodeSequence.SEQUENCE_DOSSIER, 4, 5);
    }

    public Page<DossierListItemDto> findAll(List<String> statuts, UUID commercial, UUID projet,
            List<UUID> trancheIds, ChampDateDossier champDate, LocalDateTime dateDebut, LocalDateTime dateFin,
            String search, ColonneDossier groupeColonne, String groupeValeur,
            Granularite granularite, Pageable pageable, Set<ColonneDossier> colonnes) {
        Specification<ComFolder> spec = specDossiers(new FiltresDossiers(statuts, commercial, projet, trancheIds,
                champDate, dateDebut, dateFin, search));
        if (groupeColonne != null) {
            // Dépliage d'un groupe : la même expression de clé que /groupes, donc une valeur
            // réémise telle quelle par le front retombe exactement sur les lignes comptées.
            spec = spec.and((root, query, cb) -> GroupementService.predicatDeGroupe(
                    root, cb, groupeColonne.chemin(), granularite, groupeValeur));
        }
        return folderRepository.findAll(spec.and(perimetreService.filtreDossiers()), pageable)
                .map(f -> toListItem(f, colonnes));
    }

    /** Même {@link #specDossiers} et même périmètre que la liste : la somme des groupes retombe sur son total. */
    public ResultatGroupement grouper(List<String> statuts, UUID commercial, UUID projet,
            List<UUID> trancheIds, ChampDateDossier champDate, LocalDateTime dateDebut, LocalDateTime dateFin,
            String search, ColonneDossier groupeBy, Granularite granularite,
            List<AgregatDemande> agregats, TriGroupe tri, boolean inclureSansValeur, Pageable pageable) {
        Specification<ComFolder> spec = specDossiers(new FiltresDossiers(statuts, commercial, projet, trancheIds,
                champDate, dateDebut, dateFin, search))
                .and(perimetreService.filtreDossiers());
        return groupementService.grouper(ComFolder.class, spec, groupeBy.chemin(), granularite,
                agregats, tri, inclureSansValeur, pageable);
    }

    /**
     * Assiette du tableau de bord par tranche : même spec et même périmètre que {@link #findAll},
     * pour retomber sur son total ({@code FEATURE-dashboard-filtre-tranche.md} R2/R3).
     */
    public Set<UUID> idsDossiersDeTranche(UUID trancheId) {
        Specification<ComFolder> spec = specDossiers(new FiltresDossiers(null, null, null, List.of(trancheId),
                null, null, null, null))
                .and(perimetreService.filtreDossiers());
        return folderRepository.findAll(spec).stream()
                .map(ComFolder::getId)
                .collect(java.util.stream.Collectors.toSet());
    }

    /** Filtres de la liste des dossiers, tels que reçus du contrôleur. */
    private record FiltresDossiers(List<String> statuts, UUID commercial, UUID projet, List<UUID> trancheIds,
                                   ChampDateDossier champDate, LocalDateTime dateDebut, LocalDateTime dateFin,
                                   String search) {
    }

    /** Sans le périmètre commercial, que chaque appelant ajoute explicitement. */
    private static Specification<ComFolder> specDossiers(FiltresDossiers filtres) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filtres.statuts() != null && !filtres.statuts().isEmpty()) {
                List<String> statutsLower = filtres.statuts().stream().map(String::toLowerCase).toList();
                predicates.add(cb.lower(root.get("strStatus").get("code")).in(statutsLower));
            }
            if (filtres.commercial() != null) {
                predicates.add(cb.equal(root.get("cfgCommercial").get("id"), filtres.commercial()));
            }
            if (filtres.projet() != null) {
                // Par la tranche de l'article principal, jamais StkItem.CfgProjectId, colonne morte
                // (FEATURE-perimetre-commercial.md, piège 1).
                predicates.add(cb.equal(
                        root.get("stkItem").get("stkHierarchy").get("cfgProject").get("id"), filtres.projet()));
            }
            if (filtres.trancheIds() != null && !filtres.trancheIds().isEmpty()) {
                // Partagé avec le tableau de bord et GET /api/clients (FEATURE-dashboard-filtre-tranche.md R2).
                predicates.add(AppartenanceTranche.dossier(root, query, cb, filtres.trancheIds()));
            }
            ajouterFiltreDates(predicates, root, cb, filtres);
            if (StringUtils.hasText(filtres.search())) {
                predicates.add(predicatRecherche(root, cb, filtres.search()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void ajouterFiltreDates(List<Predicate> predicates, jakarta.persistence.criteria.Root<ComFolder> root,
                                           jakarta.persistence.criteria.CriteriaBuilder cb, FiltresDossiers filtres) {
        LocalDateTime dateDebut = filtres.dateDebut();
        LocalDateTime dateFin = filtres.dateFin();
        if (dateDebut == null && dateFin == null) {
            return;
        }
        ChampDateDossier champDate = filtres.champDate() != null ? filtres.champDate() : ChampDateDossier.RESERVATION;
        jakarta.persistence.criteria.Path<LocalDateTime> colonne = root.get(champDate.getChampEntite());
        if (dateDebut != null) {
            predicates.add(cb.greaterThanOrEqualTo(colonne, dateDebut));
        }
        if (dateFin != null) {
            predicates.add(cb.lessThanOrEqualTo(colonne, dateFin));
        }
    }

    private static Predicate predicatRecherche(jakarta.persistence.criteria.Root<ComFolder> root,
                                               jakarta.persistence.criteria.CriteriaBuilder cb, String search) {
        String pattern = "%" + search.toLowerCase() + "%";
        jakarta.persistence.criteria.Join<ComFolder, CfgTier> client = root.join("cfgCustomer",
                jakarta.persistence.criteria.JoinType.LEFT);
        return cb.or(
                cb.like(cb.lower(root.get("code")), pattern),
                cb.like(cb.lower(client.get("lastName")), pattern),
                cb.like(cb.lower(client.get("firstName")), pattern),
                cb.like(cb.lower(client.get("socialReason")), pattern)
        );
    }

    public DossierDetailDto findById(UUID id) {
        ComFolder folder = folderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(DOSSIER_INTROUVABLE + id));
        List<ArticleSummaryDto> articles = stkItemRepository.findByComFolderId(id).stream()
                .map(DossierService::toArticleSummary)
                .toList();
        return toDetail(folder, articles);
    }

    /**
     * Articles DISPONIBLE ({@code ComFolderId IS NULL}) ; {@code StkVocation}, NOT NULL, vient du
     * premier article. {@code modePaieId} et {@code cin} sont exigés par décision métier (colonnes
     * nullables) ; le CIN saisi à la vente écrase celui du client. Sans {@code prix}, ni montants ni schéma.
     */
    @Transactional
    public DossierDetailDto create(CreerDossierRequest request) {
        Requis.champ(request.prospectionId(), "prospectionId est requis");
        ComProspection prospection = prospectionRepository.findById(request.prospectionId())
                .orElseThrow(() -> new InvalidRequestException("Prospection introuvable: " + request.prospectionId()));
        if (prospection.getCfgProspect() == null) {
            throw new InvalidRequestException("La prospection n'a pas de prospect lié: " + request.prospectionId());
        }
        if (request.articleIds() == null || request.articleIds().isEmpty()) {
            throw new InvalidRequestException("articleIds est requis et ne peut pas être vide");
        }
        Requis.champ(request.modePaieId(), "modePaieId est requis");
        if (!StringUtils.hasText(request.cin())) {
            throw new InvalidRequestException("cin est requis");
        }
        // Validée même sans `prix`, où elle serait sans effet : refusée, pas ignorée.
        validerRemisePercent(request.remisePercent());
        ComPaymentMethod modePaie = paymentMethodRepository.findById(request.modePaieId())
                .orElseThrow(() -> new InvalidRequestException("ComPaymentMethod introuvable: " + request.modePaieId()));

        List<StkItem> articles = new ArrayList<>();
        for (UUID articleId : request.articleIds()) {
            StkItem item = stkItemRepository.findById(articleId)
                    .orElseThrow(() -> new InvalidRequestException(ARTICLE_INTROUVABLE + articleId));
            if (item.getComFolderId() != null) {
                throw new InvalidRequestException("Article déjà occupé (lié à un autre dossier): " + articleId);
            }
            articles.add(item);
        }

        // 403 et non 404 : l'appelant a désigné les articles lui-même, rien à cacher. TOUS sont
        // contrôlés, pas seulement le principal : chacun reçoit ComFolderId plus bas, donc devient vendu.
        for (StkItem article : articles) {
            perimetreService.verifierEcritureSurTranche(article.getStkHierarchy(),
                    "Création de dossier sur l'article " + article.getCode());
        }

        CfgTier client = prospection.getCfgProspect();
        client.setIDNumber(request.cin());
        tierRepository.save(client);

        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setDocDate(LocalDateTime.now(clock));
        folder.setCode(genererCodeDossier(articles.get(0)));
        // ComFolder.StkItemId = le lot principal ; les annexes (TER, PARKING) ne portent que le
        // lien inverse StkItem.ComFolderId, posé plus bas.
        folder.setStkItem(articles.get(0));
        folder.setStkVocation(articles.get(0).getStkVocation());
        folder.setCfgCustomer(client);
        folder.setCfgCommercial(prospection.getCfgCommercial());
        folder.setComProspection(prospection);
        folder.setNotes(request.notes());
        folder.setComPaymentMethod(modePaie);
        // Tout dossier réel commence en RESERVATION. Ligne StrStatus absente : on crée quand même.
        StrStatus statutInitial = strStatusRepository.findByStrConfigEntityIdAndCode(
                StatutDossier.CONFIG_ENTITY_ID, StatutDossier.RESERVATION.getCode());
        folder.setStrStatus(statutInitial);

        CfgTranche tranche = articles.get(0).getStkHierarchy();
        if (request.prix() != null) {
            List<StkItem> annexes = articles.size() > 1 ? articles.subList(1, articles.size()) : List.of();
            BigDecimal fraisSyndic = SchemaFinancementCalculator.calculer(tranche, request.prix()).stream()
                    .filter(ligne -> "FRAIS SYNDIC".equals(ligne.categorieCode()))
                    .map(SchemaFinancementCalculator.Ligne::montant)
                    .findFirst().orElse(BigDecimal.ZERO);
            appliquerMontants(folder, articles.get(0), annexes, request.prix(), request.remisePercent(), fraisSyndic);
        }

        folder = folderRepository.save(folder);
        if (statutInitial != null) {
            enregistrerHistoriqueStatut(folder, statutInitial, null);
        }

        for (StkItem item : articles) {
            item.setComFolderId(folder.getId());
            stkItemRepository.save(item);
        }

        if (request.prix() != null) {
            // Base = prix APRÈS remise, pas le `prix` de la requête : avec une remise explicite,
            // AVANCE et AUTO-FINANCEMENT portent sur le prix réellement dû.
            genererSchemaFinancement(folder, tranche, folder.getPriceAfterDiscount());
        }

        cloturerLaProspection(prospection);

        List<ArticleSummaryDto> articleDtos = articles.stream().map(DossierService::toArticleSummary).toList();
        return toDetail(folder, articleDtos);
    }

    /**
     * Ventilation HT/TTC/remise, comme {@code DevisService#calculerMontants} (annexes = sous-articles).
     * Remise explicite : {@code prixNegocie} est le prix <b>avant</b> remise ; sinon c'est le prix
     * <b>après</b> remise et le taux s'en déduit. {@code TotalToPay} = prix après remise TTC + frais syndic.
     */
    // Package-private : DossierMontantsBackfill (src/test) réutilise ce calcul exact.
    static void appliquerMontants(ComFolder folder, StkItem principal, List<StkItem> annexes,
                                           BigDecimal prixNegocie, BigDecimal remisePercent,
                                           BigDecimal fraisSyndic) {
        boolean remiseExplicite = remisePercent != null;
        BigDecimal vatRatio = principal.getVat() != null ? principal.getVat() : BigDecimal.ZERO;

        PrixTtcHt prixPrincipal = prixPrincipalAvantRemise(principal, prixNegocie, remiseExplicite, vatRatio);
        BigDecimal itemPrice = prixPrincipal.ttc();
        BigDecimal itemPriceHT = prixPrincipal.ht();

        BigDecimal underItemsPrice = BigDecimal.ZERO;
        BigDecimal underItemsPriceHT = BigDecimal.ZERO;
        for (StkItem annexe : annexes) {
            BigDecimal prixAnnexe = annexe.getComSalePrice() != null ? annexe.getComSalePrice() : BigDecimal.ZERO;
            BigDecimal prixAnnexeHT = annexe.getComSalePriceHT() != null
                    ? annexe.getComSalePriceHT() : diviserParTaux(prixAnnexe, vatRatio);
            underItemsPrice = underItemsPrice.add(prixAnnexe);
            underItemsPriceHT = underItemsPriceHT.add(prixAnnexeHT);
        }

        BigDecimal priceBeforeDiscount = itemPrice.add(underItemsPrice);
        BigDecimal priceBeforeDiscountHT = itemPriceHT.add(underItemsPriceHT);

        BigDecimal discountPercent;
        BigDecimal discountAmount;
        BigDecimal discountAmountHT;
        BigDecimal priceAfterDiscount;
        BigDecimal priceAfterDiscountHT;
        if (remiseExplicite) {
            discountPercent = remisePercent;
            discountAmount = priceBeforeDiscount.multiply(remisePercent)
                    .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
            discountAmountHT = priceBeforeDiscountHT.multiply(remisePercent)
                    .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
            priceAfterDiscount = priceBeforeDiscount.subtract(discountAmount);
            priceAfterDiscountHT = priceBeforeDiscountHT.subtract(discountAmountHT);
        } else {
            priceAfterDiscount = prixNegocie;
            priceAfterDiscountHT = diviserParTaux(priceAfterDiscount, vatRatio);
            discountAmount = priceBeforeDiscount.subtract(priceAfterDiscount);
            discountAmountHT = priceBeforeDiscountHT.subtract(priceAfterDiscountHT);
            discountPercent = priceBeforeDiscount.signum() != 0
                    ? discountAmount.multiply(BigDecimal.valueOf(100)).divide(priceBeforeDiscount, 6, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
        }

        // Remise répartie au prorata HT entre principal et annexes, comme DevisService#calculerMontants.
        BigDecimal partPrincipal = priceBeforeDiscountHT.signum() != 0
                ? itemPriceHT.divide(priceBeforeDiscountHT, 10, RoundingMode.HALF_UP) : BigDecimal.ONE;
        BigDecimal itemPriceAfterHT = itemPriceHT.subtract(discountAmountHT.multiply(partPrincipal)).setScale(6, RoundingMode.HALF_UP);
        BigDecimal itemPriceAfter = itemPrice.subtract(discountAmount.multiply(partPrincipal)).setScale(6, RoundingMode.HALF_UP);
        BigDecimal underItemsPriceAfterHT = underItemsPriceHT
                .subtract(discountAmountHT.multiply(BigDecimal.ONE.subtract(partPrincipal))).setScale(6, RoundingMode.HALF_UP);
        BigDecimal underItemsPriceAfter = underItemsPrice
                .subtract(discountAmount.multiply(BigDecimal.ONE.subtract(partPrincipal))).setScale(6, RoundingMode.HALF_UP);

        folder.setVATRatio(vatRatio);
        folder.setItemPrice(itemPrice);
        folder.setItemPriceHT(itemPriceHT);
        folder.setUnderItemsPrice(underItemsPrice);
        folder.setUnderItemsPriceHT(underItemsPriceHT);
        folder.setPriceBeforeDiscount(priceBeforeDiscount);
        folder.setPriceBeforeDiscountHT(priceBeforeDiscountHT);
        folder.setDiscount(discountPercent);
        folder.setDiscountAmount(discountAmount);
        folder.setPriceAfterDiscount(priceAfterDiscount);
        folder.setPriceAfterDiscountHT(priceAfterDiscountHT);
        folder.setItemPriceAfter(itemPriceAfter);
        folder.setItemPriceAfterHT(itemPriceAfterHT);
        folder.setUnderItemsPriceAfter(underItemsPriceAfter);
        folder.setUnderItemsPriceAfterHT(underItemsPriceAfterHT);
        folder.setPrice(priceAfterDiscount);
        folder.setPriceHT(priceAfterDiscountHT);

        BigDecimal syndic = fraisSyndic != null ? fraisSyndic : BigDecimal.ZERO;
        folder.setTotalToPay(priceAfterDiscount.add(syndic));
    }

    private record PrixTtcHt(BigDecimal ttc, BigDecimal ht) {
    }

    private static PrixTtcHt prixPrincipalAvantRemise(StkItem principal, BigDecimal prixNegocie,
                                                      boolean remiseExplicite, BigDecimal vatRatio) {
        if (remiseExplicite && prixNegocie != null) {
            // Prix saisi = prix avant remise du principal ; HT redérivé du taux de TVA.
            return new PrixTtcHt(prixNegocie, diviserParTaux(prixNegocie, vatRatio));
        }
        BigDecimal itemPrice;
        if (principal.getComSalePrice() != null) {
            itemPrice = principal.getComSalePrice();
        } else if (prixNegocie != null) {
            itemPrice = prixNegocie;
        } else {
            itemPrice = BigDecimal.ZERO;
        }
        BigDecimal itemPriceHT = principal.getComSalePriceHT() != null
                ? principal.getComSalePriceHT() : diviserParTaux(itemPrice, vatRatio);
        return new PrixTtcHt(itemPrice, itemPriceHT);
    }

    /** Hors de [0 ; 100] ⇒ 400 : sinon prix gonflé, ou prix après remise et schéma négatifs. */
    private static void validerRemisePercent(BigDecimal remisePercent) {
        if (remisePercent == null) {
            return;
        }
        if (remisePercent.signum() < 0 || remisePercent.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new InvalidRequestException("remisePercent doit être compris entre 0 et 100: " + remisePercent);
        }
    }

    /** HT = TTC / (1 + taux), TTC tel quel si le taux est nul/absent. Même formule que DevisService. */
    private static BigDecimal diviserParTaux(BigDecimal montantTTC, BigDecimal vatRatio) {
        if (vatRatio == null || vatRatio.signum() == 0) {
            return montantTTC;
        }
        return montantTTC.divide(BigDecimal.ONE.add(vatRatio), 6, RoundingMode.HALF_UP);
    }

    /**
     * Une échéance {@code ComDocument} par ligne ; catégorie cherchée par {@code Code} dans la
     * vocation du dossier, ligne ignorée si elle n'y existe pas (la création n'est jamais bloquée).
     */
    private void genererSchemaFinancement(ComFolder folder, CfgTranche tranche, BigDecimal prix) {
        UUID vocationId = folder.getStkVocation().getId();
        for (SchemaFinancementCalculator.Ligne ligne : SchemaFinancementCalculator.calculer(tranche, prix)) {
            settlementCategoryRepository.findByCodeAndStkVocationId(ligne.categorieCode(), vocationId)
                    .ifPresent(categorie -> {
                        ComDocument document = new ComDocument();
                        document.setId(UUID.randomUUID());
                        document.setComFolder(folder);
                        document.setDocDate(LocalDateTime.now(clock));
                        document.setAmount(ligne.montant());
                        document.setComSettlementCategory(categorie);
                        document.setExcludeFromPrice(categorie.getExcludeFromPrice());
                        document.setDescription(categorie.getDescription());
                        documentRepository.save(document);
                    });
        }
    }

    /**
     * Le schéma que {@link #create} générerait, sans rien persister. Les articles n'ont pas à être
     * DISPONIBLE : rien n'est réservé.
     */
    public SchemaFinancementDto simulerSchemaFinancement(SimulerSchemaFinancementRequest request) {
        if (request.articleIds() == null || request.articleIds().isEmpty()) {
            throw new InvalidRequestException("articleIds est requis et ne peut pas être vide");
        }
        Requis.champ(request.prix(), "prix est requis");
        validerRemisePercent(request.remisePercent());

        StkItem premierArticle = stkItemRepository.findById(request.articleIds().get(0))
                .orElseThrow(() -> new InvalidRequestException(ARTICLE_INTROUVABLE + request.articleIds().get(0)));
        // Les articles suivants sont les annexes du calcul de remise, comme à la création.
        List<StkItem> annexes = request.articleIds().subList(1, request.articleIds().size()).stream()
                .map(articleId -> stkItemRepository.findById(articleId)
                        .orElseThrow(() -> new InvalidRequestException(ARTICLE_INTROUVABLE + articleId)))
                .toList();

        UUID vocationId = premierArticle.getStkVocation().getId();
        CfgTranche tranche = premierArticle.getStkHierarchy();

        // appliquerMontants sur un ComFolder jetable, jamais persisté, plutôt qu'une copie de la
        // formule : la simulation ne peut pas diverger de POST /api/dossiers.
        ComFolder apercu = new ComFolder();
        appliquerMontants(apercu, premierArticle, annexes, request.prix(), request.remisePercent(), BigDecimal.ZERO);
        BigDecimal prixApresRemise = apercu.getPriceAfterDiscount();

        List<LigneSchemaFinancementDto> lignes = SchemaFinancementCalculator.calculer(tranche, prixApresRemise).stream()
                .map(ligne -> settlementCategoryRepository.findByCodeAndStkVocationId(ligne.categorieCode(), vocationId)
                        .map(categorie -> new LigneSchemaFinancementDto(
                                ligne.categorieCode(), ligne.montant(), categorie.getExcludeFromPrice())))
                .flatMap(Optional::stream)
                .toList();

        BigDecimal totalGeneral = lignes.stream()
                .map(LigneSchemaFinancementDto::montant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SchemaFinancementDto(lignes, totalGeneral);
    }

    /**
     * 409 hors {@link TransitionsValidees}. Un dossier sans statut courant (ou à code inconnu)
     * accepte n'importe quel {@link StatutDossier} comme premier statut.
     */
    @Transactional
    public DossierDetailDto changerStatut(UUID id, ChangerStatutRequest request) {
        ComFolder folder = folderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(DOSSIER_INTROUVABLE + id));

        Requis.champ(request.statut(), "statut est requis");
        StatutDossier cible;
        try {
            cible = StatutDossier.fromCode(request.statut());
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Statut inconnu: " + request.statut());
        }

        Optional<StatutDossier> actuel = statutActuel(folder);
        if (actuel.isPresent() && !TransitionsValidees.estValide(actuel.get(), cible)) {
            StatutDossier depuis = actuel.get();
            List<String> autorisees = TransitionsValidees.transitionsAutorisees(depuis).stream()
                    .map(StatutDossier::getCode)
                    .toList();
            throw new InvalidStatutTransitionException(
                    "Transition invalide: " + depuis.getCode() + " -> " + cible.getCode(), autorisees);
        }

        StrStatus strStatus = strStatusRepository.findByStrConfigEntityIdAndCode(StatutDossier.CONFIG_ENTITY_ID, cible.getCode());
        if (strStatus == null) {
            throw new InvalidRequestException("StrStatus introuvable pour le code: " + cible.getCode());
        }
        folder.setStrStatus(strStatus);
        appliquerChampEtape(folder, cible, request);
        enregistrerHistoriqueStatut(folder, strStatus, request.note());

        List<ArticleSummaryDto> articles = stkItemRepository.findByComFolderId(id).stream()
                .map(DossierService::toArticleSummary)
                .toList();
        return toDetail(folderRepository.save(folder), articles);
    }

    /**
     * Comme OMRAN à chaque vente : ferme la prospection avec le texte de clôture qu'il écrit
     * (pas de motif codé), et désarme toutes ses alertes, comme l'archivage. Voir
     * {@code FEATURE-conformite-legataire.md} §2.
     */
    private void cloturerLaProspection(ComProspection prospection) {
        prospection.setIsClosed(true);
        prospection.setClosingDate(LocalDateTime.now(clock));
        prospection.setClosingReasonText(TEXTE_CLOTURE_VENTE);
        prospectionRepository.save(prospection);
        alerteRelanceService.desarmerToutes(prospection.getId());
    }

    /**
     * Dans les <strong>deux</strong> tables qu'alimente OMRAN, auteur compris ; {@code StrWorkflowId}
     * reste nul, cas admis par le légataire. Voir {@code FEATURE-conformite-legataire.md} §3.
     */
    private void enregistrerHistoriqueStatut(ComFolder folder, StrStatus nouveauStatut, String note) {
        UtilisateurAffiche.Auteur auteur = utilisateurAffiche.courant();
        LocalDateTime maintenant = LocalDateTime.now(clock);

        StrFolderStatusHistory historique = new StrFolderStatusHistory();
        historique.setId(UUID.randomUUID());
        historique.setDocDate(maintenant);
        historique.setComFolder(folder);
        historique.setStrFolderStatus(nouveauStatut);
        historique.setDescription(note);
        historique.setAdmUserId(auteur.compteId());
        historique.setCfgTier(folder.getCfgCustomer());
        statusHistoryRepository.save(historique);

        StrHistory generique = new StrHistory();
        generique.setId(UUID.randomUUID());
        generique.setDocDate(maintenant);
        generique.setUserDate(maintenant);
        generique.setObjectId(folder.getId());
        generique.setStrConfigEntityId(StatutDossier.CONFIG_ENTITY_ID);
        generique.setAdmUserId(auteur.compteId());
        generique.setAdmUserDescription(auteur.nom());
        generique.setStrStatusId(nouveauStatut != null ? nouveauStatut.getId() : null);
        generique.setStrStatusDescription(nouveauStatut != null ? nouveauStatut.getCode() : null);
        generique.setDescription(note);
        strHistoryRepository.save(generique);
    }

    // La date va sur la colonne jalon de l'étape (EtapeMilestone) ; la note va sur CancelDescription
    // pour ANNULATION (la cause d'annulation), sur Notes sinon.
    private void appliquerChampEtape(ComFolder folder, StatutDossier cible, ChangerStatutRequest request) {
        if (cible == StatutDossier.ANNULATION) {
            folder.setCancelDate(request.date() != null ? request.date() : LocalDateTime.now(clock));
            if (request.note() != null) {
                folder.setCancelDescription(request.note());
            }
            return;
        }
        if (request.note() != null) {
            folder.setNotes(request.note());
        }
        if (request.date() == null) {
            return;
        }
        switch (cible) {
            case CONCRETISATION -> folder.setAdvanceDate(request.date());
            case MEILLEURTAUX -> folder.setLoanCompanyDecisionDate(request.date());
            case COMPROMIS -> folder.setCreditApprovalDate(request.date());
            case ENVOI_AU_NOTAIRE -> folder.setAgreementDate(request.date());
            case CREDIT_DEBLOQUE -> folder.setCreditPossessionDate(request.date());
            case MINUTE -> folder.setAgreementSavingDate(request.date());
            case REMISE_DES_CLEFS -> folder.setDeliveryDateReal(request.date());
            case FORMALISE -> folder.setAgreementRegistryDate(request.date());
            default -> {
                // RESERVATION, CREDIT_EN_COURS, LIVRAISON_PROVISOIRE, DESISTEMENT : pas de colonne
                // jalon confirmée — voir EtapeMilestone.
            }
        }
    }

    /**
     * Calculé depuis l'échéancier, jamais stocké. Les lignes {@code excludeFromPrice} (FRAIS) comptent
     * dans {@code parCategorie}/{@code totalGeneral} mais pas dans {@code totalSoumisReglePrixTTC}.
     */
    public MontantsDossierDto montants(UUID dossierId) {
        ComFolder folder = folderRepository.findById(dossierId)
                .orElseThrow(() -> new ResourceNotFoundException(DOSSIER_INTROUVABLE + dossierId));
        List<ComDocument> lignes = documentRepository.findByComFolderIdOrderByDocDateAsc(dossierId);

        Map<UUID, BigDecimal> regleParEcheance = new LinkedHashMap<>();
        for (Object[] row : settlementLineRepository.sumAmountByComFolderIdGroupedByComDocument(dossierId)) {
            regleParEcheance.put((UUID) row[0], (BigDecimal) row[1]);
        }

        Map<String, BigDecimal> montantParCategorie = new LinkedHashMap<>();
        Map<String, Integer> nombreParCategorie = new LinkedHashMap<>();
        Map<String, BigDecimal> regleParCategorie = new LinkedHashMap<>();
        BigDecimal totalGeneral = BigDecimal.ZERO;
        BigDecimal totalSoumisReglePrixTTC = BigDecimal.ZERO;
        BigDecimal fraisExclusDuPrix = BigDecimal.ZERO;
        BigDecimal regleTotal = BigDecimal.ZERO;
        BigDecimal regleSoumisAuPrix = BigDecimal.ZERO;

        for (ComDocument ligne : lignes) {
            String categorie = ligne.getComSettlementCategory() != null ? ligne.getComSettlementCategory().getCode() : "SANS_CATEGORIE";
            BigDecimal montant = ligne.getAmount() != null ? ligne.getAmount() : BigDecimal.ZERO;
            BigDecimal regle = regleParEcheance.getOrDefault(ligne.getId(), BigDecimal.ZERO);

            montantParCategorie.merge(categorie, montant, BigDecimal::add);
            nombreParCategorie.merge(categorie, 1, Integer::sum);
            regleParCategorie.merge(categorie, regle, BigDecimal::add);
            totalGeneral = totalGeneral.add(montant);
            regleTotal = regleTotal.add(regle);
            if (Boolean.TRUE.equals(ligne.getExcludeFromPrice())) {
                fraisExclusDuPrix = fraisExclusDuPrix.add(montant);
            } else {
                totalSoumisReglePrixTTC = totalSoumisReglePrixTTC.add(montant);
                regleSoumisAuPrix = regleSoumisAuPrix.add(regle);
            }
        }

        List<CategorieMontantDto> parCategorie = montantParCategorie.entrySet().stream()
                .map(e -> {
                    BigDecimal montant = e.getValue();
                    BigDecimal regle = regleParCategorie.get(e.getKey());
                    return new CategorieMontantDto(e.getKey(), montant, nombreParCategorie.get(e.getKey()),
                            regle, montant.subtract(regle));
                })
                .toList();

        return new MontantsDossierDto(parCategorie, totalGeneral, totalSoumisReglePrixTTC,
                prixVente(folder, regleSoumisAuPrix),
                netAPayer(folder, fraisExclusDuPrix, regleTotal),
                estSorti(folder));
    }

    /**
     * « Prix de vente » de l'onglet Montants : {@code ComFolder.Price}, <strong>pas</strong> re-dérivé
     * de l'échéancier — le prix stocké fait foi, c'est ce que lit la vue légataire.
     */
    private static SyntheseMontantDto prixVente(ComFolder f, BigDecimal regleSoumisAuPrix) {
        BigDecimal montant = requireNonNullElse(f.getPrice(), BigDecimal.ZERO);
        return new SyntheseMontantDto(montant, regleSoumisAuPrix, montant.subtract(regleSoumisAuPrix));
    }

    /**
     * « Net à payer » = prix de vente + frais exclus du prix ; sur un dossier sorti, = déjà encaissé,
     * reste 0. Voir {@code FEATURE-fiche-dossier-alignement-ecran.md} §5.
     */
    private static SyntheseMontantDto netAPayer(ComFolder f, BigDecimal fraisExclusDuPrix,
                                                BigDecimal regleTotal) {
        if (estSorti(f)) {
            return new SyntheseMontantDto(regleTotal, regleTotal, BigDecimal.ZERO);
        }
        BigDecimal montant = requireNonNullElse(f.getPrice(), BigDecimal.ZERO).add(fraisExclusDuPrix);
        return new SyntheseMontantDto(montant, regleTotal, montant.subtract(regleTotal));
    }

    /** Équivaut au {@code IsCancellation} de la vue légataire, d'où aucune colonne mappée pour l'obtenir. */
    private static boolean estSorti(ComFolder f) {
        if (f.getStrStatus() == null || f.getStrStatus().getCode() == null) {
            return false;
        }
        // fromCode lève sur un code hors référentiel : pas de 500 sur les montants pour autant.
        try {
            return StatutDossier.fromCode(f.getStrStatus().getCode()).isSortie();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** Vide si le dossier n'a pas de statut <b>ou</b> un code inconnu : les appelants les traitent pareil. */
    private static Optional<StatutDossier> statutActuel(ComFolder folder) {
        return folder.getStrStatus() == null
                ? Optional.empty()
                : tryFromCode(folder.getStrStatus().getCode());
    }

    /** {@code StrStatus.Code} est alimenté par OMRAN : une valeur inconnue ne fait pas échouer la lecture. */
    private static Optional<StatutDossier> tryFromCode(String code) {
        try {
            return Optional.ofNullable(code).map(StatutDossier::fromCode);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** Pour chaque statut atteignable, la colonne jalon où {@code PATCH .../statut} écrirait sa date. */
    public List<EtapeSuivanteDto> etapesSuivantes(UUID id) {
        ComFolder folder = folderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(DOSSIER_INTROUVABLE + id));

        return statutActuel(folder)
                .map(actuel -> TransitionsValidees.transitionsAutorisees(actuel).stream()
                        .map(statut -> {
                            EtapeMilestone.Info info = EtapeMilestone.of(statut);
                            return new EtapeSuivanteDto(
                                    statut.getCode(), info.champ(), info.libelle(), info.confiance().name());
                        })
                        .toList())
                .orElseGet(List::of);
    }

    private static List<String> etapesAtteignables(ComFolder folder) {
        if (folder.getStrStatus() == null || folder.getStrStatus().getCode() == null) {
            return List.of();
        }
        StatutDossier statut;
        try {
            statut = StatutDossier.fromCode(folder.getStrStatus().getCode());
        } catch (IllegalArgumentException e) {
            return List.of();
        }
        return TransitionsValidees.transitionsAutorisees(statut).stream()
                .map(StatutDossier::getCode)
                .toList();
    }

    /**
     * Patrimoine (trois jointures paresseuses par ligne), prospection et article ne sont traversés
     * que si leurs colonnes sont demandées. {@code colonnes} : {@code Set.of()} hors liste.
     */
    public static DossierListItemDto toListItem(ComFolder f, Set<ColonneDossier> colonnes) {
        boolean patrimoine = ColonneDossier.exigeLePatrimoine(colonnes);
        CfgTranche tranche = patrimoine ? tranche(f) : null;
        CfgProject projet = patrimoine ? projet(f) : null;
        CfgCompany societe = patrimoine ? societe(f) : null;
        ComProspection prospection = ColonneDossier.exigeLaProspection(colonnes)
                ? f.getComProspection() : null;
        StkItem article = ColonneDossier.exigeLArticle(colonnes) ? f.getStkItem() : null;
        StkVocation vocation = patrimoine ? f.getStkVocation() : null;
        return new DossierListItemDto(
                f.getId(),
                f.getCode(),
                f.getDocDate(),
                Champs.de(f.getStrStatus(), StrStatus::getCode),
                Champs.de(f.getCfgCustomer(), CfgTier::getId),
                TierNomFormatter.nom(f.getCfgCustomer()),
                Champs.de(f.getCfgCommercial(), CfgTier::getId),
                TierNomFormatter.nom(f.getCfgCommercial()),
                f.getPrice(),
                f.getTotalToPay(),
                f.getAdvanceDate(),
                Champs.de(f.getComProspection(), ComProspection::getId),
                Champs.de(f.getComProspection(), ComProspection::getCode),
                f.getPriceBeforeDiscount(),
                f.getDiscount(),
                f.getDiscountAmount(),
                f.getPriceAfterDiscount(),
                // Le « montant d'avance » vient d'AgreementAmount : ComFolder n'a pas d'AdvanceAmount.
                f.getAgreementAmount(),
                f.getProbabilityOfFinalisation(),
                Champs.de(projet, CfgProject::getId),
                Champs.de(projet, CfgProject::getCode),
                Champs.de(projet, CfgProject::getDescription),
                Champs.de(tranche, CfgTranche::getId),
                Champs.de(tranche, CfgTranche::getCode),
                Champs.de(tranche, CfgTranche::getDescription),
                Champs.de(societe, CfgCompany::getId),
                Champs.de(societe, CfgCompany::getCode),
                Champs.de(societe, CfgCompany::getDescription),
                Champs.de(vocation, StkVocation::getCode),
                Champs.de(vocation, StkVocation::getDescription),
                siDemande(colonnes, ColonneDossier.AVOCAT_ID, f.getCfgLawyer(), CfgTier::getId),
                siDemande(colonnes, ColonneDossier.AVOCAT_NOM, f.getCfgLawyer(), TierNomFormatter::nom),
                siDemande(colonnes, ColonneDossier.MODE_PAIE, f.getComPaymentMethod(), ComPaymentMethod::getCode),
                Champs.de(prospection, ComProspection::getComProspectionKind, ComProspectionKind::getDescription),
                Champs.de(prospection, ComProspection::getComProspectionOrigin, ComProspectionOrigin::getDescription),
                Champs.de(prospection, ComProspection::getDocDate),
                f.getUserCreate(),
                f.getUserCreateDateTime(),
                f.getUserUpdate(),
                f.getUserUpdateDateTime(),
                Champs.de(article, StkItem::getId),
                Champs.de(article, StkItem::getCode),
                Champs.de(article, StkItem::getDescription),
                Champs.de(article, StkItem::getTitleNumber),
                Champs.de(article, StkItem::getStkBuilding, StkBuilding::getDescription),
                Champs.de(article, StkItem::getStkFloor, StkFloor::getDescription),
                Champs.de(article, StkItem::getStkItemCategory, StkItemCategory::getCode),
                Champs.de(article, StkItem::getStkItemType, StkItemType::getCode),
                Champs.de(article, StkItem::getStkPresentationType, StkPresentationType::getCode),
                siDemande(colonnes, ColonneDossier.RESPONSABLE_VENTE_ID, f.getCfgSaleManager(), CfgTier::getId),
                siDemande(colonnes, ColonneDossier.RESPONSABLE_VENTE_NOM, f.getCfgSaleManager(), TierNomFormatter::nom),
                f.getAgreementDate(),
                f.getDeliveryDateReal(),
                // Scalaires @Formula : lus avec l'entité, aucun coût conditionnel à gérer.
                f.getDelaiConcretisation(),
                f.getDelaiNotaire(),
                f.getDelaiLivraison()
        );
    }

    /** Lu seulement si la colonne est demandée : une colonne absente vaut {@code null}. */
    private static <T, R> R siDemande(Set<ColonneDossier> colonnes, ColonneDossier colonne,
                                      T source, Function<T, R> lecteur) {
        return colonnes.contains(colonne) ? Champs.de(source, lecteur) : null;
    }

    private static DossierDetailDto toDetail(ComFolder f, List<ArticleSummaryDto> articles) {
        CfgProject projet = projet(f);
        CfgTranche tranche = tranche(f);
        CfgCompany societe = societe(f);
        ComProspection prospection = f.getComProspection();
        return new DossierDetailDto(
                f.getId(),
                f.getCode(),
                f.getDocDate(),
                Champs.de(f.getStrStatus(), StrStatus::getCode),
                etapesAtteignables(f),
                Champs.de(f.getCfgCustomer(), CfgTier::getId),
                TierNomFormatter.nom(f.getCfgCustomer()),
                Champs.de(f.getCfgCommercial(), CfgTier::getId),
                TierNomFormatter.nom(f.getCfgCommercial()),
                Champs.de(f.getCfgLawyer(), CfgTier::getId),
                TierNomFormatter.nom(f.getCfgLawyer()),
                articles,
                f.getPriceBeforeDiscount(),
                f.getDiscount(),
                f.getDiscountAmount(),
                f.getPriceAfterDiscount(),
                f.getPrice(),
                f.getAgreementAmount(),
                f.getTotalToPay(),
                f.getNotes(),
                f.getProbabilityOfFinalisation(),
                Champs.de(f.getComPaymentMethod(), ComPaymentMethod::getCode),
                f.getUnitCost(),
                f.getPriceHT(),
                f.getPriceBeforeDiscountHT(),
                f.getVATRatio(),
                f.getStatusValidityDate(),
                f.getPartPercent(),
                Champs.de(prospection, ComProspection::getId),
                Champs.de(prospection, ComProspection::getCode),
                // Chaque maillon article → tranche → projet → société est testé : la vue légataire
                // les traverse en LEFT JOIN, rien en base n'interdit qu'il manque.
                Champs.de(projet, CfgProject::getId),
                Champs.de(projet, CfgProject::getCode),
                Champs.de(projet, CfgProject::getDescription),
                Champs.de(tranche, CfgTranche::getId),
                Champs.de(tranche, CfgTranche::getCode),
                Champs.de(tranche, CfgTranche::getDescription),
                Champs.de(societe, CfgCompany::getId),
                Champs.de(societe, CfgCompany::getCode),
                Champs.de(societe, CfgCompany::getDescription),
                Champs.de(f.getStkVocation(), StkVocation::getId),
                Champs.de(f.getStkVocation(), StkVocation::getCode),
                Champs.de(f.getStkVocation(), StkVocation::getDescription),
                f.getUserCreate(),
                f.getUserCreateDateTime(),
                f.getUserUpdate(),
                f.getUserUpdateDateTime(),
                Champs.de(prospection, ComProspection::getComProspectionKind, ComProspectionKind::getDescription),
                Champs.de(prospection, ComProspection::getComProspectionOrigin, ComProspectionOrigin::getDescription),
                Champs.de(prospection, ComProspection::getDocDate),
                Champs.de(f.getCfgSaleManager(), CfgTier::getId),
                TierNomFormatter.nom(f.getCfgSaleManager()),
                f.getAgreementDate(),
                f.getDeliveryDateReal(),
                f.getDelaiConcretisation(),
                f.getDelaiNotaire(),
                f.getDelaiLivraison()
        );
    }

    /** {@code ComFolder} n'a pas de tranche à lui : celle de l'article principal ({@code StkHierarchyId}). */
    private static CfgTranche tranche(ComFolder f) {
        return f.getStkItem() != null ? f.getStkItem().getStkHierarchy() : null;
    }

    /** Via la tranche, jamais {@code StkItem.CfgProjectId}, toujours {@code null} en base réelle. */
    private static CfgProject projet(ComFolder f) {
        CfgTranche t = tranche(f);
        return t != null ? t.getCfgProject() : null;
    }

    private static CfgCompany societe(ComFolder f) {
        CfgProject p = projet(f);
        return p != null ? p.getCfgCompany() : null;
    }

    private static ArticleSummaryDto toArticleSummary(StkItem item) {
        return new ArticleSummaryDto(
                item.getId(), item.getCode(), item.getDescription(), item.getArea(), item.getSalePrice(),
                item.getStkItemCategory() != null ? item.getStkItemCategory().getId() : null,
                item.getStkItemCategory() != null ? item.getStkItemCategory().getCode() : null,
                item.getStkItemCategory() != null ? item.getStkItemCategory().getDescription() : null,
                item.getStkItemType() != null ? item.getStkItemType().getId() : null,
                item.getStkItemType() != null ? item.getStkItemType().getCode() : null,
                item.getStkItemType() != null ? item.getStkItemType().getDescription() : null,
                item.getStkFloor() != null ? item.getStkFloor().getId() : null,
                item.getStkFloor() != null ? item.getStkFloor().getCode() : null,
                item.getStkFloor() != null ? item.getStkFloor().getDescription() : null,
                item.getStkBuilding() != null ? item.getStkBuilding().getId() : null,
                item.getStkBuilding() != null ? item.getStkBuilding().getCode() : null,
                item.getStkBuilding() != null ? item.getStkBuilding().getDescription() : null,
                item.getTitleNumber(),
                Champs.de(item.getStkPresentationType(), StkPresentationType::getCode),
                Champs.de(item.getStkPresentationType(), StkPresentationType::getDescription)
        );
    }
}

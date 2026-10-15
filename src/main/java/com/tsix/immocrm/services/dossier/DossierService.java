package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.domain.Chemin;
import com.tsix.immocrm.domain.ColonneDossier;
import com.tsix.immocrm.domain.ConventionRemise;
import com.tsix.immocrm.domain.DisponibiliteArticle;
import com.tsix.immocrm.domain.PrixArticle;
import com.tsix.immocrm.domain.SchemaFinancementCalculator;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.dto.common.GroupeDto;
import com.tsix.immocrm.dto.dossier.ArticleSummaryDto;
import com.tsix.immocrm.dto.dossier.ChangerStatutRequest;
import com.tsix.immocrm.dto.dossier.CreerDossierRequest;
import com.tsix.immocrm.dto.dossier.DossierDetailDto;
import com.tsix.immocrm.dto.dossier.DossierListItemDto;
import com.tsix.immocrm.dto.dossier.EtapeSuivanteDto;
import com.tsix.immocrm.dto.dossier.LigneSchemaFinancementDto;
import com.tsix.immocrm.dto.dossier.MontantsDossierDto;
import com.tsix.immocrm.dto.dossier.SchemaFinancementDto;
import com.tsix.immocrm.dto.dossier.SimulerSchemaFinancementRequest;
import com.tsix.immocrm.dto.dossier.TransitionSuggereeDto;
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
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkPresentationType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.stl.StlQuoteDocument;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComPaymentMethodRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.services.dossier.OffreOrigineDossier.OffreRetenue;
import com.tsix.immocrm.services.dossier.liste.FiltresListeDossiers;
import com.tsix.immocrm.services.dossier.liste.MappingListeDossier;
import com.tsix.immocrm.services.dossier.liste.SpecificationsListeDossiers;
import com.tsix.immocrm.services.dossier.workflow.TransitionDossier;
import com.tsix.immocrm.services.dossier.workflow.WorkflowDossier;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.services.prospection.AlerteRelanceService;
import com.tsix.immocrm.services.sequence.GenerateurCodeSequence;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.Requis;
import com.tsix.immocrm.utils.TierNomFormatter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class DossierService {

    /** Texte exact d'OMRAN : c'est lui qui distingue une prospection vendue d'une prospection perdue. */
    public static final String TEXTE_CLOTURE_VENTE = "Création dossier de vente";

    private static final String DOSSIER_INTROUVABLE = "Dossier introuvable: ";
    private static final String ARTICLE_INTROUVABLE = "Article introuvable: ";
    private static final String CAUSE_OBLIGATOIRE = "La cause est obligatoire pour une annulation ou un désistement";
    static final String DOSSIER_SANS_STATUT =
            "Dossier sans statut : reprise nécessaire (script instructions/sql/VENTE-reprise-interop.sql)";
    static final String SEUIL_IGNORE = "Seuil de versement non vérifié (Administrateur) pour le statut ";

    private final ComFolderRepository folderRepository;
    private final StkItemRepository stkItemRepository;
    private final ComProspectionRepository prospectionRepository;
    private final StrStatusRepository strStatusRepository;
    private final ComDocumentRepository documentRepository;
    private final ComPaymentMethodRepository paymentMethodRepository;
    private final CfgTierRepository tierRepository;
    private final ComSettlementCategoryRepository settlementCategoryRepository;
    private final GenerateurCodeSequence generateurCode;
    private final PerimetreCommercialService perimetreService;
    private final UtilisateurAffiche utilisateurAffiche;
    private final AlerteRelanceService alerteRelanceService;
    private final TransformationProspectClient transformationProspectClient;
    private final Clock clock;
    private final LiberationLot liberationLot;
    private final CreationDossierOmran creationOmran;
    private final HistoriqueDossier historique;
    private final LigneEcheancierOmran ligneOmran;
    private final ControleSortieDossier controleSortie;
    private final MontantsDossier montantsDossier;
    private final WorkflowDossier workflow;
    private final OffreOrigineDossier offreOrigine;

    public DossierService(ComFolderRepository folderRepository, StkItemRepository stkItemRepository,
                           ComProspectionRepository prospectionRepository, StrStatusRepository strStatusRepository,
                           ComDocumentRepository documentRepository,
                           ComPaymentMethodRepository paymentMethodRepository, CfgTierRepository tierRepository,
                           ComSettlementCategoryRepository settlementCategoryRepository,
                           GenerateurCodeSequence generateurCode,
                           PerimetreCommercialService perimetreService,
                           UtilisateurAffiche utilisateurAffiche,
                           AlerteRelanceService alerteRelanceService,
                           TransformationProspectClient transformationProspectClient,
                           Clock clock, LiberationLot liberationLot, EcrituresOmranDossier ecrituresOmran) {
        this.clock = clock;
        this.liberationLot = liberationLot;
        this.transformationProspectClient = transformationProspectClient;
        this.alerteRelanceService = alerteRelanceService;
        this.perimetreService = perimetreService;
        this.utilisateurAffiche = utilisateurAffiche;
        this.folderRepository = folderRepository;
        this.stkItemRepository = stkItemRepository;
        this.prospectionRepository = prospectionRepository;
        this.documentRepository = documentRepository;
        this.settlementCategoryRepository = settlementCategoryRepository;
        this.strStatusRepository = strStatusRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.tierRepository = tierRepository;
        this.generateurCode = generateurCode;
        this.creationOmran = ecrituresOmran.creation();
        this.historique = ecrituresOmran.historique();
        this.ligneOmran = ecrituresOmran.ligneEcheancier();
        this.controleSortie = ecrituresOmran.controleSortie();
        this.montantsDossier = ecrituresOmran.montants();
        this.workflow = ecrituresOmran.workflow();
        this.offreOrigine = ecrituresOmran.offreOrigine();
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

    /**
     * Assiette du tableau de bord par tranche : même spec et même périmètre que la liste, sortis compris,
     * pour retomber sur son total ({@code FEATURE-dashboard-filtre-tranche.md} R2/R3).
     */
    public Set<UUID> idsDossiersDeTranche(UUID trancheId) {
        Specification<ComFolder> spec = SpecificationsListeDossiers.de(FiltresListeDossiers.deTranche(trancheId),
                        Optional.empty())
                .and(perimetreService.filtreDossiers());
        return folderRepository.findAll(spec).stream()
                .map(ComFolder::getId)
                .collect(java.util.stream.Collectors.toSet());
    }

    public DossierDetailDto findById(UUID id) {
        ComFolder folder = folderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(DOSSIER_INTROUVABLE + id));
        return toDetail(folder, articlesDuDossier(folder));
    }

    /**
     * Dossier actif : ses articles liés ({@code StkItem.ComFolderId}). Dossier sorti : ses articles
     * ont été libérés, il montre son lot principal ({@code ComFolder.StkItemId}). FEATURE-liberer-lot.md R8.
     */
    private List<ArticleSummaryDto> articlesDuDossier(ComFolder folder) {
        if (estSorti(folder) && folder.getStkItem() != null) {
            return List.of(toArticleSummary(folder.getStkItem()));
        }
        return stkItemRepository.findByComFolderId(folder.getId()).stream()
                .map(DossierService::toArticleSummary)
                .toList();
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
        Optional<OffreRetenue> offre = offreOrigine.verifier(request.offreId(), prospection);
        PrixSousArticles prixAnnexes = offre.map(OffreRetenue::prixSousArticles).orElse(PrixSousArticles.CATALOGUE);

        List<StkItem> articles = new ArrayList<>();
        for (UUID articleId : request.articleIds()) {
            StkItem item = stkItemRepository.findById(articleId)
                    .orElseThrow(() -> new InvalidRequestException(ARTICLE_INTROUVABLE + articleId));
            if (DisponibiliteArticle.estOccupe(item)) {
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

        // OMRAN refuse un dossier sans statut : contrôlé avant la première écriture.
        StrStatus statutInitial = creationOmran.statutInitial();

        CfgTier client = prospection.getCfgProspect();
        client.setIDNumber(request.cin());
        tierRepository.save(client);

        StkItem principal = articles.get(0);
        List<StkItem> annexes = annexesDuDossier(principal, articles.subList(1, articles.size()));
        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setDocDate(LocalDate.now(clock).atStartOfDay());
        folder.setCode(genererCodeDossier(principal));
        // ComFolder.StkItemId = le lot principal ; les annexes (TER, PARKING) sont ses sous-articles.
        folder.setStkItem(principal);
        folder.setStkVocation(principal.getStkVocation());
        folder.setCfgCustomer(client);
        folder.setCfgCommercial(prospection.getCfgCommercial());
        folder.setComProspection(prospection);
        folder.setStlQuoteId(offre.map(o -> o.offre().getId()).orElse(null));
        folder.setNotes(request.notes());
        folder.setComPaymentMethod(modePaie);
        folder.setStrStatus(statutInitial);

        CfgTranche tranche = principal.getStkHierarchy();
        appliquerMontantsCreation(folder, principal, new AnnexesPrix(annexes, prixAnnexes), tranche, request);
        creationOmran.completer(folder, principal, annexes, client);
        // Le formulaire ne saisit pas la commission : celle de l'offre, comme GenerateComFolderFromQuote.
        offre.map(o -> o.offre().getCommissionRate()).ifPresent(folder::setCommissionRate);

        folder = folderRepository.save(folder);
        creationOmran.rattacherAnnexes(folder, principal, annexes, prixAnnexes);
        creationOmran.historiserCreation(folder, principal);
        principal.setComFolderId(folder.getId());
        stkItemRepository.save(principal);

        List<ComDocument> echeancier = echeancierInitial(folder, tranche, request.prix(),
                offre.map(OffreRetenue::schema).orElse(List.of()));
        creationOmran.completerAvecEcheancier(folder, echeancier);
        folder = folderRepository.save(folder);
        synchroniserTypeTiers(folder);

        cloturerLaProspection(prospection);

        List<ArticleSummaryDto> articleDtos = Stream.concat(Stream.of(principal), annexes.stream())
                .map(DossierService::toArticleSummary).toList();
        return toDetail(folder, articleDtos);
    }

    /**
     * Annexes saisies, puis tous les sous-articles libres du lot ({@code ParentId}) même non saisis : OMRAN
     * les prend tous ({@code Sale.cs:SetStkItemId}) ; ils entrent dans les prix des sous-articles.
     */
    private List<StkItem> annexesDuDossier(StkItem principal, List<StkItem> saisies) {
        List<StkItem> annexes = new ArrayList<>(saisies);
        Set<UUID> deja = new HashSet<>();
        deja.add(principal.getId());
        saisies.forEach(a -> deja.add(a.getId()));
        stkItemRepository.findByParentId(principal.getId()).stream()
                .filter(enfant -> enfant.getComFolderId() == null && deja.add(enfant.getId()))
                .sorted(Comparator.comparing(StkItem::getCode, Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(annexes::add);
        return List.copyOf(annexes);
    }

    /**
     * Montants toujours écrits, comme OMRAN : sans {@code prix} saisi, le prix de référence des articles
     * (0 s'ils n'en ont pas), sans frais de syndic puisqu'aucun schéma n'est généré.
     */
    private void appliquerMontantsCreation(ComFolder folder, StkItem principal, AnnexesPrix annexes,
                                           CfgTranche tranche, CreerDossierRequest request) {
        PrixArticle.PrixDeReference prixLot = prixDuLotSeul(principal);
        if (request.prix() == null) {
            BigDecimal prixReference = annexes.articles().stream()
                    .map(annexes.prix()::ttc)
                    .reduce(prixLot != null ? prixLot.ttc() : BigDecimal.ZERO, BigDecimal::add);
            appliquerMontants(folder, principal, prixLot, annexes, prixReference, request.remisePercent(), BigDecimal.ZERO);
            return;
        }
        BigDecimal fraisSyndic = SchemaFinancementCalculator.calculer(tranche, request.prix()).stream()
                .filter(ligne -> "FRAIS SYNDIC".equals(ligne.categorieCode()))
                .map(SchemaFinancementCalculator.Ligne::montant)
                .findFirst().orElse(BigDecimal.ZERO);
        appliquerMontants(folder, principal, prixLot, annexes, request.prix(), request.remisePercent(), fraisSyndic);
    }

    /** Prix de référence du lot hors sous-articles, qui sont comptés comme annexes ; null sans prix. */
    private PrixArticle.PrixDeReference prixDuLotSeul(StkItem principal) {
        return PrixArticle.prixDuLotSeul(principal, stkItemRepository.findByParentId(principal.getId())).orElse(null);
    }

    /** Lot sans sous-article connu : son prix de référence tel quel. */
    static void appliquerMontants(ComFolder folder, StkItem principal, List<StkItem> annexes,
                                  BigDecimal prixNegocie, BigDecimal remisePercent, BigDecimal fraisSyndic) {
        appliquerMontants(folder, principal, PrixArticle.prixDeReference(principal).orElse(null), annexes,
                prixNegocie, remisePercent, fraisSyndic);
    }

    /**
     * Ventilation HT/TTC/remise, comme {@code DevisService#calculerMontants} (annexes = sous-articles).
     * Remise explicite : {@code prixNegocie} est le total (principal + annexes) <b>avant</b> remise ;
     * sinon c'est le prix <b>après</b> remise et le taux s'en déduit. {@code TotalToPay} = prix après remise TTC + frais syndic.
     */
    static void appliquerMontants(ComFolder folder, StkItem principal, PrixArticle.PrixDeReference prixLot,
                                  List<StkItem> annexes, BigDecimal prixNegocie, BigDecimal remisePercent,
                                  BigDecimal fraisSyndic) {
        appliquerMontants(folder, principal, prixLot, new AnnexesPrix(annexes, PrixSousArticles.CATALOGUE),
                prixNegocie, remisePercent, fraisSyndic);
    }

    /** Sous-articles du dossier et la source de leurs prix (offre d'origine ou catalogue). */
    record AnnexesPrix(List<StkItem> articles, PrixSousArticles prix) {
    }

    static void appliquerMontants(ComFolder folder, StkItem principal, PrixArticle.PrixDeReference prixLot,
                                  AnnexesPrix annexes, BigDecimal prixNegocie, BigDecimal remisePercent,
                                  BigDecimal fraisSyndic) {
        boolean remiseExplicite = remisePercent != null;
        BigDecimal vatRatio = principal.getVat() != null ? principal.getVat() : BigDecimal.ZERO;

        BigDecimal underItemsPrice = BigDecimal.ZERO;
        BigDecimal underItemsPriceHT = BigDecimal.ZERO;
        for (StkItem annexe : annexes.articles()) {
            BigDecimal prixAnnexe = annexes.prix().ttc(annexe);
            // HT du sous-article à son propre taux (un jardin est souvent à 0 %), comme son SalePriceHT chez OMRAN.
            BigDecimal prixAnnexeHT = annexes.prix().ht(annexe, vatRatio);
            underItemsPrice = underItemsPrice.add(prixAnnexe);
            underItemsPriceHT = underItemsPriceHT.add(prixAnnexeHT);
        }

        PrixTtcHt prixPrincipal = prixPrincipalAvantRemise(prixLot, prixNegocie, remiseExplicite,
                vatRatio, underItemsPrice);
        BigDecimal itemPrice = prixPrincipal.ttc();
        BigDecimal itemPriceHT = prixPrincipal.ht();

        BigDecimal priceBeforeDiscount = itemPrice.add(underItemsPrice);
        BigDecimal priceBeforeDiscountHT = itemPriceHT.add(underItemsPriceHT);

        // Sale.cs:RecalcDiscount : un seul taux, appliqué au HT du lot et de chaque sous-article ; sans taux
        // saisi, il se déduit des TTC (prix négocié / prix avant remise), jamais d'un HT à TVA unique.
        BigDecimal taux = tauxDeRemise(remisePercent, prixNegocie, priceBeforeDiscount);
        BigDecimal resteATaux = BigDecimal.ONE.subtract(taux);
        BigDecimal discountAmountHT = priceBeforeDiscountHT.multiply(taux).setScale(6, RoundingMode.HALF_UP);
        BigDecimal priceAfterDiscountHT = priceBeforeDiscountHT.subtract(discountAmountHT);
        BigDecimal itemPriceAfterHT = itemPriceHT.multiply(resteATaux).setScale(6, RoundingMode.HALF_UP);
        BigDecimal underItemsPriceAfterHT = priceAfterDiscountHT.subtract(itemPriceAfterHT);
        BigDecimal itemPriceAfter = itemPriceAfterHT.multiply(BigDecimal.ONE.add(vatRatio)).setScale(6, RoundingMode.HALF_UP);
        BigDecimal priceAfterDiscount = remiseExplicite
                ? priceBeforeDiscount.subtract(priceBeforeDiscount.multiply(taux).setScale(6, RoundingMode.HALF_UP))
                : prixNegocie;
        BigDecimal underItemsPriceAfter = priceAfterDiscount.subtract(itemPriceAfter);

        folder.setVATRatio(vatRatio);
        folder.setItemPrice(itemPrice);
        folder.setItemPriceHT(itemPriceHT);
        folder.setUnderItemsPrice(underItemsPrice);
        folder.setUnderItemsPriceHT(underItemsPriceHT);
        folder.setPriceBeforeDiscount(priceBeforeDiscount);
        folder.setPriceBeforeDiscountHT(priceBeforeDiscountHT);
        // Convention OMRAN : ratio du HT et montant HT (ComFolder n'a pas de DiscountAmountHT).
        folder.setDiscount(ConventionRemise.ratioDuHT(discountAmountHT, priceBeforeDiscountHT));
        folder.setDiscountAmount(discountAmountHT);
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

    private static final BigDecimal DEMI_CENTIME = new BigDecimal("0.005");

    /** Taux 0–1 : celui saisi, sinon l'écart du prix négocié au prix avant remise, en TTC ; 0 sans base. */
    static BigDecimal tauxDeRemise(BigDecimal remisePercent, BigDecimal prixNegocie, BigDecimal prixAvantRemise) {
        if (remisePercent != null) {
            return remisePercent.divide(BigDecimal.valueOf(100), 20, RoundingMode.HALF_UP);
        }
        if (prixNegocie == null || prixAvantRemise.signum() == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal ecart = prixAvantRemise.subtract(prixNegocie);
        // Un écart sous le demi-centime vient de l'arrondi du prix saisi (prix de base à 6 décimales) : pas une remise.
        if (ecart.abs().compareTo(DEMI_CENTIME) < 0) {
            return BigDecimal.ZERO;
        }
        return ecart.divide(prixAvantRemise, 20, RoundingMode.HALF_UP);
    }

    private static PrixTtcHt prixPrincipalAvantRemise(PrixArticle.PrixDeReference reference, BigDecimal prixNegocie,
                                                      boolean remiseExplicite, BigDecimal vatRatio,
                                                      BigDecimal prixAnnexes) {
        if (remiseExplicite && prixNegocie != null) {
            // Prix saisi = total avant remise, annexes comprises (même sens que sans remise) :
            // le principal est le reste ; HT redérivé du taux de TVA. Voir FEATURE-remise-dossier.md §2.
            if (prixNegocie.compareTo(prixAnnexes) < 0) {
                throw new InvalidRequestException("prix inférieur au prix des annexes (" + prixAnnexes
                        + ") : avec remisePercent, prix est le total avant remise, annexes comprises: " + prixNegocie);
            }
            BigDecimal itemPrice = prixNegocie.subtract(prixAnnexes);
            return new PrixTtcHt(itemPrice, diviserParTaux(itemPrice, vatRatio));
        }
        if (reference != null) {
            return new PrixTtcHt(reference.ttc(), reference.ht(vatRatio));
        }
        // Sans prix de référence, la base avant remise est le prix négocié : remise nulle.
        BigDecimal itemPrice = prixNegocie != null ? prixNegocie : BigDecimal.ZERO;
        return new PrixTtcHt(itemPrice, diviserParTaux(itemPrice, vatRatio));
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

    private static BigDecimal diviserParTaux(BigDecimal montantTTC, BigDecimal vatRatio) {
        return PrixArticle.diviserParTaux(montantTTC, vatRatio);
    }

    /**
     * Le schéma saisi sur l'offre d'origine ({@code StlQuoteDocument}) s'il y en a un, comme
     * {@code GenerateComFolderFromQuote} ; sinon le schéma par défaut, et rien sans prix saisi.
     */
    private List<ComDocument> echeancierInitial(ComFolder folder, CfgTranche tranche, BigDecimal prixSaisi,
                                                List<StlQuoteDocument> schemaOffre) {
        if (!schemaOffre.isEmpty()) {
            return schemaOffre.stream()
                    .map(ligne -> {
                        ComDocument document = nouvelleEcheance(folder, ligne.getComSettlementCategory(),
                                ligne.getAmount(), ligne.getDocDate(), ligne.getDiscountRatio());
                        if (ligne.getCfgCompany() != null) {
                            document.setCfgCompany(ligne.getCfgCompany());
                        }
                        return documentRepository.save(document);
                    })
                    .toList();
        }
        // Base = prix APRÈS remise, pas le `prix` de la requête : avec une remise explicite,
        // AVANCE et AUTO-FINANCEMENT portent sur le prix réellement dû.
        return prixSaisi != null ? genererSchemaFinancement(folder, tranche, folder.getPriceAfterDiscount()) : List.of();
    }

    /**
     * Une échéance {@code ComDocument} par ligne ; catégorie cherchée par {@code Code} dans la
     * vocation du dossier, ligne ignorée si elle n'y existe pas (la création n'est jamais bloquée).
     */
    private List<ComDocument> genererSchemaFinancement(ComFolder folder, CfgTranche tranche, BigDecimal prix) {
        UUID vocationId = folder.getStkVocation().getId();
        List<ComDocument> lignes = new ArrayList<>();
        for (SchemaFinancementCalculator.Ligne ligne : SchemaFinancementCalculator.calculer(tranche, prix)) {
            settlementCategoryRepository.findByCodeAndStkVocationId(ligne.categorieCode(), vocationId)
                    .ifPresent(categorie -> lignes.add(documentRepository.save(
                            nouvelleEcheance(folder, categorie, ligne.montant(), null, null))));
        }
        return lignes;
    }

    /** Seule construction d'une échéance initiale ; date absente = date du dossier, remise absente = défaut OMRAN. */
    private ComDocument nouvelleEcheance(ComFolder folder, ComSettlementCategory categorie, BigDecimal montant,
                                         LocalDateTime date, BigDecimal ratioRemise) {
        ComDocument document = new ComDocument();
        document.setId(UUID.randomUUID());
        document.setComFolder(folder);
        document.setDocDate(date != null ? date : folder.getDocDate());
        ligneOmran.signerCreation(document);
        document.setAmount(montant);
        document.setComSettlementCategory(categorie);
        document.setDiscountRatio(ratioRemise);
        if (categorie != null) {
            document.setExcludeFromPrice(categorie.getExcludeFromPrice());
            document.setDescription(categorie.getDescription());
        }
        ligneOmran.completer(document, folder);
        return document;
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
        // Les articles suivants, puis les sous-articles libres du lot : mêmes annexes qu'à la création.
        List<StkItem> saisies = request.articleIds().subList(1, request.articleIds().size()).stream()
                .map(articleId -> stkItemRepository.findById(articleId)
                        .orElseThrow(() -> new InvalidRequestException(ARTICLE_INTROUVABLE + articleId)))
                .toList();
        List<StkItem> annexes = annexesDuDossier(premierArticle, saisies);

        UUID vocationId = premierArticle.getStkVocation().getId();
        CfgTranche tranche = premierArticle.getStkHierarchy();

        // appliquerMontants sur un ComFolder jetable, jamais persisté, plutôt qu'une copie de la
        // formule : la simulation ne peut pas diverger de POST /api/dossiers.
        ComFolder apercu = new ComFolder();
        appliquerMontants(apercu, premierArticle, prixDuLotSeul(premierArticle), annexes, request.prix(),
                request.remisePercent(), BigDecimal.ZERO);
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

        // DiscountAmount est HT (convention OMRAN) : le montant TTC de la réponse est l'écart des prix TTC.
        return new SchemaFinancementDto(lignes, totalGeneral, apercu.getPriceBeforeDiscount(),
                apercu.getDiscount().signum() == 0 ? BigDecimal.ZERO : apercu.getPriceBeforeDiscount().subtract(prixApresRemise),
                ConventionRemise.pourcentage(apercu.getDiscount()), prixApresRemise,
                annexes.subList(saisies.size(), annexes.size()).stream()
                        .map(a -> new SchemaFinancementDto.AnnexeAutomatique(a.getId(), a.getCode(), a.getDescription(),
                                a.getArea(), PrixArticle.ttcOuZero(a)))
                        .toList());
    }

    /**
     * Moteur {@link WorkflowDossier} : 409 hors transition paramétrée ({@code StrWorkFlow}, repli
     * {@code TransitionsValidees}) ou dossier sans statut, 403 si l'appelant n'est pas intervenant, 400 champs ou
     * pièces manquants, 409 seuil de versement / schéma. Voir FEATURE-vente-workflow.md.
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
        if (folder.getStrStatus() == null) {
            throw new ConflictException(DOSSIER_SANS_STATUT);
        }

        TransitionDossier transition = workflow.transitionAutorisee(folder, cible);
        Optional<StatutDossier> actuel = statutActuel(folder);
        if (cible.isSortie() && !StringUtils.hasText(request.note())) {
            throw new InvalidRequestException(CAUSE_OBLIGATOIRE);
        }
        boolean dejaSorti = actuel.map(StatutDossier::isSortie).orElse(false);
        boolean reactivation = dejaSorti && !cible.isSortie();
        if (cible.isSortie() && !dejaSorti) {
            controleSortie.verifier(folder);
        }
        if (reactivation) {
            liberationLot.verifierReoccupable(folder);
        }

        StrStatus strStatus = strStatusRepository.findByStrConfigEntityIdAndCode(StatutDossier.CONFIG_ENTITY_ID, cible.getCode());
        if (strStatus == null) {
            throw new InvalidRequestException("StrStatus introuvable pour le code: " + cible.getCode());
        }
        // Date et note posées avant les contrôles, comme la saisie d'OMRAN ; un refus annule la transaction.
        appliquerChampEtape(folder, strStatus, cible, request);
        boolean seuilIgnore = workflow.verifier(folder, transition, strStatus, Boolean.TRUE.equals(request.ignorerSeuil()));

        // Avant toute écriture sur le dossier : un 403 de périmètre sur un article le laisse intact.
        if (cible.isSortie()) {
            liberationLot.liberer(folder, strStatus);
        } else if (reactivation) {
            liberationLot.reoccuper(folder, strStatus);
        }
        folder.setStrStatus(strStatus);
        if (reactivation) {
            // Sale.cs:BeforeUpdate : un dossier réactivé perd sa date et sa cause d'annulation.
            folder.setCancelDate(null);
            folder.setCancelDescription("");
        }
        workflow.apresChangement(folder, strStatus, cible, this::employeCourant);
        marquerModification(folder);
        historique.ligneStatut(folder, strStatus, folder.getStkItem(), request.note());
        historique.statutGenerique(folder, strStatus, request.note(), transition.workflowId(),
                transition.parametree() ? transition.description() : null);
        if (seuilIgnore) {
            historique.evenement(folder, SEUIL_IGNORE + strStatus.getDescription());
        }

        ComFolder enregistre = folderRepository.save(folder);
        synchroniserTypeTiers(enregistre);
        return toDetail(enregistre, articlesDuDossier(enregistre));
    }

    /** Personnel de l'utilisateur connecté ({@code ConnectedUser.CfgEmployeeId}), pour le gestionnaire auto. */
    private CfgTier employeCourant() {
        AdmUser compte = utilisateurAffiche.courant().compte();
        return compte != null ? compte.getCfgEmployee() : null;
    }

    /**
     * Type client / prospect des tiers du dossier ({@code Sale.cs:ManageTier}), une ligne d'historique
     * par tiers changé : la note du changement de statut reste intacte (NVARCHAR(255)).
     */
    private void synchroniserTypeTiers(ComFolder folder) {
        transformationProspectClient.synchroniser(folder)
                .forEach(mention -> historique.evenement(folder, mention));
    }

    /** GEN0016 : toute écriture sur le dossier signe {@code UserUpdate}/{@code UserUpdateDateTime}. */
    private void marquerModification(ComFolder folder) {
        folder.setUserUpdate(utilisateurAffiche.courant().nom());
        folder.setUserUpdateDateTime(LocalDateTime.now(clock));
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

    // La date va sur la date associée du statut cible (WorkflowDossier) ; la note va sur CancelDescription
    // pour les deux sorties ANNULATION et DESISTEMENT (la cause), sur Notes sinon.
    private void appliquerChampEtape(ComFolder folder, StrStatus strStatus, StatutDossier cible,
                                     ChangerStatutRequest request) {
        if (cible.isSortie()) {
            folder.setCancelDate(request.date() != null ? request.date() : LocalDateTime.now(clock));
            if (request.note() != null) {
                folder.setCancelDescription(request.note());
            }
            return;
        }
        if (request.note() != null) {
            folder.setNotes(request.note());
        }
        workflow.ecrireDateAssociee(folder, strStatus, cible, request.date());
    }

    /**
     * Calculé depuis l'échéancier, jamais stocké. Les lignes {@code excludeFromPrice} (FRAIS) comptent
     * dans {@code parCategorie}/{@code totalGeneral} mais pas dans {@code totalSoumisReglePrixTTC}.
     */
    public MontantsDossierDto montants(UUID dossierId) {
        ComFolder folder = folderRepository.findById(dossierId)
                .orElseThrow(() -> new ResourceNotFoundException(DOSSIER_INTROUVABLE + dossierId));
        return montantsDossier.calculer(folder);
    }

    /** Équivaut au {@code IsCancellation} de la vue légataire, d'où aucune colonne mappée pour l'obtenir. */
    static boolean estSorti(ComFolder f) {
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

    /** Statuts atteignables par l'appelant, et la date associée où {@code PATCH .../statut} écrirait sa date. */
    public List<EtapeSuivanteDto> etapesSuivantes(UUID id) {
        ComFolder folder = folderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(DOSSIER_INTROUVABLE + id));
        return workflow.etapesSuivantes(folder);
    }

    /** Passage automatique proposé (statut et date), ou une proposition vide. */
    public TransitionSuggereeDto transitionSuggeree(UUID id) {
        ComFolder folder = folderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(DOSSIER_INTROUVABLE + id));
        return workflow.suggestion(folder);
    }

    /** Codes des statuts atteignables par l'appelant (même source que {@link #etapesSuivantes}). */
    private List<String> etapesAtteignables(ComFolder folder) {
        return workflow.statutsAtteignables(folder);
    }

    /**
     * Patrimoine (trois jointures paresseuses par ligne), prospection et article ne sont traversés
     * que si leurs colonnes sont demandées. {@code colonnes} : {@code Set.of()} hors liste.
     */
    public static DossierListItemDto toListItem(ComFolder f, Set<ColonneDossier> colonnes) {
        return MappingListeDossier.depuisDossier(f, colonnes, null).versDto();
    }

    private DossierDetailDto toDetail(ComFolder f, List<ArticleSummaryDto> articles) {
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
                ConventionRemise.pourcentage(f.getDiscount()),
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

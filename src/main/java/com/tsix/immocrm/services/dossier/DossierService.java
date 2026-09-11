package com.tsix.immocrm.services.dossier;

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
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkPresentationType;
import com.tsix.immocrm.entities.str.StrFolderStatusHistory;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.InvalidStatutTransitionException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmSequenceRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComPaymentMethodRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.str.StrFolderStatusHistoryRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import com.tsix.immocrm.services.groupement.GroupementService;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
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
import java.time.LocalDateTime;
import static java.util.Objects.requireNonNullElse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DossierService {

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
    private final AdmSequenceRepository sequenceRepository;
    private final PerimetreCommercialService perimetreService;
    private final GroupementService groupementService;

    public DossierService(ComFolderRepository folderRepository, StkItemRepository stkItemRepository,
                           ComProspectionRepository prospectionRepository, StrStatusRepository strStatusRepository,
                           ComDocumentRepository documentRepository, StlSettlementLineRepository settlementLineRepository,
                           ComPaymentMethodRepository paymentMethodRepository, CfgTierRepository tierRepository,
                           ComSettlementCategoryRepository settlementCategoryRepository,
                           StrFolderStatusHistoryRepository statusHistoryRepository,
                           AdmSequenceRepository sequenceRepository,
                           PerimetreCommercialService perimetreService,
                           GroupementService groupementService) {
        this.perimetreService = perimetreService;
        this.groupementService = groupementService;
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
        this.sequenceRepository = sequenceRepository;
    }

    /**
     * Numbers a new Dossier the same way the legacy app does — confirmed against real data:
     * {@code AdmSequence} (row {@code Code = "Dossier de vente"}) holds the actual live counter
     * ({@code SeqValue}), a single global sequence shared by every project/prefix (codes count
     * down 379→378→377 across different prefixes APF/ART/ARS with no gaps or per-project reset).
     * Format: {@code "{ProjectCode} {année}/{numéro sur 5 chiffres}"} (e.g. "APF 2026/00380").
     * The project code comes from the article's tranche ({@code StkItem.StkHierarchyId} →
     * {@code CfgTranche.CfgProjectId}) — {@code StkItem.CfgProjectId} itself is never populated
     * in real data, so it can't be used directly.
     */
    private String genererCodeDossier(StkItem premierArticle) {
        AdmSequence sequence = sequenceRepository.findByCode("Dossier de vente").orElseGet(() -> {
            // Self-heals if the seed row is ever missing (e.g. a fresh test database) — bootstraps
            // at 0 rather than blocking Dossier creation entirely.
            AdmSequence nouvelle = new AdmSequence();
            nouvelle.setId(UUID.randomUUID());
            nouvelle.setCode("Dossier de vente");
            nouvelle.setSeqValue(0);
            nouvelle.setSeqValueNumber(5);
            nouvelle.setSeqYearNumber(4);
            nouvelle.setBasedOnSystemYear(true);
            return nouvelle;
        });
        int prochain = (sequence.getSeqValue() != null ? sequence.getSeqValue() : 0) + 1;
        sequence.setSeqValue(prochain);
        sequenceRepository.save(sequence);

        String prefixe = "DOS";
        if (premierArticle.getStkHierarchy() != null && premierArticle.getStkHierarchy().getCfgProject() != null
                && premierArticle.getStkHierarchy().getCfgProject().getCode() != null) {
            prefixe = premierArticle.getStkHierarchy().getCfgProject().getCode();
        }

        int largeurAnnee = sequence.getSeqYearNumber() != null ? sequence.getSeqYearNumber() : 4;
        int largeurNumero = sequence.getSeqValueNumber() != null ? sequence.getSeqValueNumber() : 5;
        String annee = String.format("%0" + largeurAnnee + "d", LocalDateTime.now().getYear());
        String numero = String.format("%0" + largeurNumero + "d", prochain);
        return prefixe + " " + annee + "/" + numero;
    }

    /**
     * @param statuts    {@code StrStatus.code} filter — 0 values means no filter, 1 value
     *                   behaves exactly like the pre-multi-select single-value filter, 2+
     *                   values match any of them (OR). Repeated query param
     *                   ({@code ?statut=a&statut=b}), see {@code instructions/filtres-avances.md}.
     * @param commercial {@code CfgTier.id} (cfgCommercial) filter
     * @param projet     {@code CfgProject.id} filter, résolu par la <b>tranche</b> de l'article
     *                   principal ({@code ComFolder.stkItem.stkHierarchy.cfgProject}) et non par
     *                   {@code StkItem.cfgProject}, colonne morte en base réelle (0 ligne sur
     *                   1068) sur laquelle ce filtre ne renvoyait jamais aucun dossier. Le chemin
     *                   retenu couvre 358 dossiers sur 358.
     * @param trancheIds {@code CfgTranche.id} filter — retient un dossier si l'un de ses articles
     *                   liés ({@code StkItem.comFolderId}) <b>ou</b> son article principal
     *                   ({@code ComFolder.stkItem}) appartient à l'une de ces tranches. Les deux
     *                   branches sont nécessaires : le lien inverse ne couvre que 328 des 358
     *                   dossiers réels, l'article principal les 358. Un dossier peut porter
     *                   plusieurs articles (lot principal + annexes) éventuellement répartis sur
     *                   plusieurs tranches, d'où l'{@code EXISTS} plutôt qu'une simple jointure.
     *                   Voir {@code instructions/filtres-avances.md} point 2.
     * @param champDate  which {@code ComFolder} milestone column {@code dateDebut}/{@code dateFin}
     *                   apply to (defaults to {@link ChampDateDossier#RESERVATION} — i.e.
     *                   {@code docDate} — if a range is given without an explicit field). Ignored
     *                   if neither {@code dateDebut} nor {@code dateFin} is given. See
     *                   {@code instructions/filtres-avances.md} point 3.
     * @param dateDebut  inclusive lower bound on {@code champDate}'s column
     * @param dateFin    inclusive upper bound on {@code champDate}'s column
     * @param search     free-text match on {@code code} OR the linked client's
     *                   nom/prénom/raison sociale ({@code CfgTier} via {@code ComFolder.cfgCustomer}
     *                   — LEFT JOIN, so folders without a linked client still match on
     *                   {@code code} alone). Same OR-across-fields convention as
     *                   {@code ProspectService.findAll}. See {@code instructions/from_front.md} #6.
     */
    public Page<DossierListItemDto> findAll(List<String> statuts, UUID commercial, UUID projet,
            List<UUID> trancheIds, ChampDateDossier champDate, LocalDateTime dateDebut, LocalDateTime dateFin,
            String search, ColonneDossier groupeColonne, String groupeValeur,
            Granularite granularite, Pageable pageable, Set<ColonneDossier> colonnes) {
        Specification<ComFolder> spec = specDossiers(statuts, commercial, projet, trancheIds,
                champDate, dateDebut, dateFin, search);
        if (groupeColonne != null) {
            // Dépliage d'un groupe : la même expression de clé que /groupes, donc une valeur
            // réémise telle quelle par le front retombe exactement sur les lignes comptées.
            spec = spec.and((root, query, cb) -> GroupementService.predicatDeGroupe(
                    root, cb, groupeColonne.chemin(), granularite, groupeValeur));
        }
        return folderRepository.findAll(spec.and(perimetreService.filtreDossiers()), pageable)
                .map(f -> toListItem(f, colonnes));
    }

    /**
     * Replie {@code GET /api/dossiers} en groupes — voir
     * {@code instructions/FEATURE-groupement-listes.md}.
     *
     * <p>Réutilise {@link #specDossiers} <strong>à l'identique</strong>, périmètre commercial
     * compris : c'est ce qui garantit que la somme des effectifs des groupes retombe sur le
     * {@code totalElements} de la liste non groupée.
     */
    public Page<GroupeDto> grouper(List<String> statuts, UUID commercial, UUID projet,
            List<UUID> trancheIds, ChampDateDossier champDate, LocalDateTime dateDebut, LocalDateTime dateFin,
            String search, ColonneDossier groupeBy, Granularite granularite,
            Set<ColonneDossier> agregats, TriGroupe tri, boolean inclureSansValeur, Pageable pageable) {
        Specification<ComFolder> spec = specDossiers(statuts, commercial, projet, trancheIds,
                champDate, dateDebut, dateFin, search)
                .and(perimetreService.filtreDossiers());
        Map<String, Chemin> cheminsAgregats = new LinkedHashMap<>();
        for (ColonneDossier agregat : agregats) {
            cheminsAgregats.put(agregat.nom(), agregat.chemin());
        }
        return groupementService.grouper(ComFolder.class, spec, groupeBy.chemin(), granularite,
                cheminsAgregats, tri, inclureSansValeur, pageable);
    }

    /**
     * Les filtres de la liste des dossiers, extraits pour être partagés avec le groupement.
     *
     * <p>N'inclut délibérément <strong>pas</strong> le périmètre commercial : les deux appelants
     * l'ajoutent explicitement, ce qui rend son absence visible à la lecture plutôt que cachée
     * dans une composition.
     */
    private static Specification<ComFolder> specDossiers(List<String> statuts, UUID commercial,
            UUID projet, List<UUID> trancheIds, ChampDateDossier champDate,
            LocalDateTime dateDebut, LocalDateTime dateFin, String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (statuts != null && !statuts.isEmpty()) {
                List<String> statutsLower = statuts.stream().map(String::toLowerCase).toList();
                predicates.add(cb.lower(root.get("strStatus").get("code")).in(statutsLower));
            }
            if (commercial != null) {
                predicates.add(cb.equal(root.get("cfgCommercial").get("id"), commercial));
            }
            if (projet != null) {
                // Le projet se résout par la TRANCHE de l'article principal, jamais par
                // StkItem.CfgProjectId : ce raccourci est mort en base réelle (0 ligne sur 1068),
                // donc le filtre ne renvoyait jamais aucun dossier. Même chemin que
                // ComFolderRepository.findDashboardProjectionDansPerimetre, qui le documentait déjà.
                predicates.add(cb.equal(
                        root.get("stkItem").get("stkHierarchy").get("cfgProject").get("id"), projet));
            }
            if (trancheIds != null && !trancheIds.isEmpty()) {
                jakarta.persistence.criteria.Subquery<UUID> subquery = query.subquery(UUID.class);
                jakarta.persistence.criteria.Root<StkItem> article = subquery.from(StkItem.class);
                subquery.select(article.get("id"))
                        .where(cb.and(
                                cb.equal(article.get("comFolderId"), root.get("id")),
                                article.get("stkHierarchy").get("id").in(trancheIds)));
                // L'EXISTS seul ne suffit pas : il passe par le lien inverse StkItem.ComFolderId,
                // qui n'est renseigné que pour 328 des 358 dossiers réels. Les 30 autres ont
                // pourtant bien une tranche via leur article principal (ComFolder.StkItemId, lui
                // renseigné à 358/358) — sans cette seconde branche ils étaient invisibles au
                // filtre, silencieusement.
                //
                // Jointures explicitement LEFT : une navigation implicite produirait des INNER
                // JOIN qui, portant sur toute la requête et non sur la seule branche du OR,
                // écarteraient un dossier sans article principal même quand l'EXISTS le retient.
                jakarta.persistence.criteria.Join<ComFolder, StkItem> principal =
                        root.join("stkItem", jakarta.persistence.criteria.JoinType.LEFT);
                jakarta.persistence.criteria.Join<StkItem, CfgTranche> trancheDuPrincipal =
                        principal.join("stkHierarchy", jakarta.persistence.criteria.JoinType.LEFT);
                predicates.add(cb.or(
                        cb.exists(subquery),
                        trancheDuPrincipal.get("id").in(trancheIds)));
            }
            if (dateDebut != null || dateFin != null) {
                String champ = (champDate != null ? champDate : ChampDateDossier.RESERVATION).getChampEntite();
                jakarta.persistence.criteria.Path<LocalDateTime> colonne = root.get(champ);
                if (dateDebut != null) {
                    predicates.add(cb.greaterThanOrEqualTo(colonne, dateDebut));
                }
                if (dateFin != null) {
                    predicates.add(cb.lessThanOrEqualTo(colonne, dateFin));
                }
            }
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.toLowerCase() + "%";
                jakarta.persistence.criteria.Join<ComFolder, CfgTier> client = root.join("cfgCustomer",
                        jakarta.persistence.criteria.JoinType.LEFT);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), pattern),
                        cb.like(cb.lower(client.get("lastName")), pattern),
                        cb.like(cb.lower(client.get("firstName")), pattern),
                        cb.like(cb.lower(client.get("socialReason")), pattern)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public DossierDetailDto findById(UUID id) {
        ComFolder folder = folderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable: " + id));
        List<ArticleSummaryDto> articles = stkItemRepository.findByComFolderId(id).stream()
                .map(DossierService::toArticleSummary)
                .toList();
        return toDetail(folder, articles);
    }

    /**
     * {@code POST /api/dossiers}: creates a Dossier from a Prospection. The prospection must
     * have a linked prospect (client). Every article in {@code articleIds} must exist and be
     * DISPONIBLE ({@code StkItem.ComFolderId IS NULL}) — confirmed availability rule from
     * {@link com.tsix.immocrm.domain.StatutArticle}. {@code StkVocation} (required, NOT NULL
     * on ComFolder) is taken from the first article, since a folder has none of its own.
     *
     * {@code modePaieId} and {@code cin} are required by business decision (the underlying DB
     * columns are nullable, but a sales dossier must not be created without them). {@code cin}
     * is written onto the buyer's {@code CfgTier.IDNumber} — it doesn't overwrite an existing
     * value with a blank one, but does overwrite a differing value (the CIN captured at sale
     * time is treated as authoritative).
     *
     * When {@code prix} is provided, also auto-generates the "Schéma de financement" échéancier
     * lines (AUTO-FINANCEMENT / AVANCE / FRAIS SYNDIC — confirmed against real
     * {@code ComSettlementCategory}/{@code ComDocument} data, see {@link #genererSchemaFinancement}).
     * Skipped entirely if {@code prix} is null, since the amounts derive from it.
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
        // Validée avant toute écriture (y compris sans `prix`, où la remise serait sans effet):
        // une remise aberrante doit être refusée, pas silencieusement ignorée.
        validerRemisePercent(request.remisePercent());
        ComPaymentMethod modePaie = paymentMethodRepository.findById(request.modePaieId())
                .orElseThrow(() -> new InvalidRequestException("ComPaymentMethod introuvable: " + request.modePaieId()));

        List<StkItem> articles = new ArrayList<>();
        for (UUID articleId : request.articleIds()) {
            StkItem item = stkItemRepository.findById(articleId)
                    .orElseThrow(() -> new InvalidRequestException("Article introuvable: " + articleId));
            if (item.getComFolderId() != null) {
                throw new InvalidRequestException("Article déjà occupé (lié à un autre dossier): " + articleId);
            }
            articles.add(item);
        }

        // Périmètre commercial : créer un dossier sur un projet non affecté est refusé (403).
        // Ici l'appelant a lui-même désigné les articles, donc les projets — il n'y a rien à
        // cacher, contrairement à une lecture hors périmètre (404). Sans ce contrôle, la frontière
        // posée sur les listes et les détails serait purement décorative.
        //
        // TOUS les articles sont contrôlés, pas seulement le principal : la boucle plus bas pose
        // StkItem.ComFolderId sur chacun d'eux. Ne valider que articles.get(0) laissait passer
        // [lotDuProjetA, annexeDuProjetB] en 201 et marquait vendu un lot d'un projet que
        // l'appelant ne peut même pas lire — une écriture transverse au périmètre.
        for (StkItem article : articles) {
            perimetreService.verifierEcritureSurTranche(article.getStkHierarchy(),
                    "Création de dossier sur l'article " + article.getCode());
        }

        CfgTier client = prospection.getCfgProspect();
        client.setIDNumber(request.cin());
        tierRepository.save(client);

        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setDocDate(LocalDateTime.now());
        folder.setCode(genererCodeDossier(articles.get(0)));
        // Main article back-link (ComFolder.StkItemId) — confirmed against real data: it points
        // to the main lot when several articles are linked (annexes like TER/PARKING only carry
        // the reverse StkItem.ComFolderId, set below). Was missing entirely before this fix,
        // leaving folder.getStkItem() always null for API-created dossiers.
        folder.setStkItem(articles.get(0));
        folder.setStkVocation(articles.get(0).getStkVocation());
        folder.setCfgCustomer(client);
        folder.setCfgCommercial(prospection.getCfgCommercial());
        folder.setComProspection(prospection);
        folder.setNotes(request.notes());
        folder.setComPaymentMethod(modePaie);
        // Initial status: confirmed 100% of real dossiers (359/359 with history) start at
        // RESERVATION — no exception. Doesn't block creation if the StrStatus row is somehow
        // missing (defensive, same spirit as genererCodeDossier's self-healing).
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
            // Base du schéma = prix APRÈS remise (posé par appliquerMontants juste au-dessus), pas
            // le `prix` brut de la requête : sans remise explicite les deux sont égaux (comportement
            // historique inchangé), avec une remise saisie par le commercial l'AVANCE et
            // l'AUTO-FINANCEMENT doivent porter sur le prix réellement dû.
            genererSchemaFinancement(folder, tranche, folder.getPriceAfterDiscount());
        }

        List<ArticleSummaryDto> articleDtos = articles.stream().map(DossierService::toArticleSummary).toList();
        return toDetail(folder, articleDtos);
    }

    /**
     * Calcule et pose la ventilation HT/TTC/remise du dossier à la création — même logique que
     * {@code DevisService#calculerMontants} (l'onglet "Montants" de l'Offre : prix commercial de
     * l'article, {@code ComSalePrice(HT)}/{@code Vat}, HT dérivé du taux de TVA), mais avec deux
     * différences volontaires par rapport à l'Offre :
     * <ul>
     *   <li>Deux modes de remise, selon que {@code remisePercent} est fourni ou non :
     *     <ul>
     *       <li><b>Remise explicite</b> ({@code remisePercent != null}) — le commercial saisit la
     *           remise sur l'écran de création. {@code prixNegocie} devient alors le prix
     *           <b>avant</b> remise et remplace le prix commercial de l'article comme base (même
     *           rôle que {@code prixOverride} dans {@code DevisService#calculerMontants}), la
     *           remise est appliquée dessus : {@code priceAfterDiscount = priceBeforeDiscount ×
     *           (1 − remise/100)}. C'est ce prix après remise qui alimente {@code TotalToPay}
     *           <i>et</i> le schéma de financement (AVANCE / AUTO-FINANCEMENT).</li>
     *       <li><b>Remise dérivée</b> ({@code remisePercent == null}, comportement historique
     *           conservé pour ne rien casser côté front) — {@code prixNegocie} est le prix
     *           <b>après</b> remise, le prix avant remise vient du prix commercial de l'article,
     *           et le pourcentage de remise est déduit de l'écart entre les deux.</li>
     *     </ul>
     *   </li>
     *   <li>{@code TotalToPay} (exposé sur {@code DossierListItemDto}/{@code DossierDetailDto} et
     *       agrégé par {@code DashboardService} pour la "valeur du pipeline" — colonne réelle,
     *       jamais posée avant ce correctif) = {@code priceAfterDiscount} (TTC) + Frais Syndic
     *       ({@link SchemaFinancementCalculator}, jamais inclus dans le total d'une Offre).</li>
     * </ul>
     * {@code annexes} (articles secondaires liés au même dossier, ex. parking/cave) jouent le
     * rôle des sous-articles côté Offre — leur {@code ComSalePrice(HT)} alimente
     * {@code underItemsPrice(HT)}, et la remise explicite porte sur le total (principal + annexes),
     * répartie au prorata comme côté Offre.
     */
    // Package-private (not private) so DossierMontantsBackfill (one-off migration, src/test) can
    // reuse this exact logic to fill TotalToPay on legacy dossiers that predate this computation.
    static void appliquerMontants(ComFolder folder, StkItem principal, List<StkItem> annexes,
                                           BigDecimal prixNegocie, BigDecimal remisePercent,
                                           BigDecimal fraisSyndic) {
        boolean remiseExplicite = remisePercent != null;
        BigDecimal vatRatio = principal.getVat() != null ? principal.getVat() : BigDecimal.ZERO;

        BigDecimal itemPrice;
        BigDecimal itemPriceHT;
        if (remiseExplicite && prixNegocie != null) {
            // Prix saisi = prix avant remise de l'article principal (peut être le prix commercial
            // pré-rempli par le front, ou une valeur ajustée) — le HT est redérivé du taux de TVA.
            itemPrice = prixNegocie;
            itemPriceHT = diviserParTaux(itemPrice, vatRatio);
        } else {
            itemPrice = principal.getComSalePrice() != null ? principal.getComSalePrice()
                    : prixNegocie != null ? prixNegocie : BigDecimal.ZERO;
            itemPriceHT = principal.getComSalePriceHT() != null
                    ? principal.getComSalePriceHT() : diviserParTaux(itemPrice, vatRatio);
        }

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

        // Répartit la remise au prorata entre article principal et annexes pour la ventilation
        // "après remise" — même principe que DevisService#calculerMontants.
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

    /**
     * Une remise saisie hors de [0 ; 100] est refusée en 400 : négative elle gonflerait le prix,
     * au-delà de 100 elle donnerait un prix après remise négatif (et un schéma de financement
     * négatif derrière). Absente ⇒ mode historique, aucune validation.
     */
    private static void validerRemisePercent(BigDecimal remisePercent) {
        if (remisePercent == null) {
            return;
        }
        if (remisePercent.signum() < 0 || remisePercent.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new InvalidRequestException("remisePercent doit être compris entre 0 et 100: " + remisePercent);
        }
    }

    /** HT = TTC / (1 + taux). Renvoie TTC tel quel si le taux est nul/absent. Même formule que DevisService. */
    private static BigDecimal diviserParTaux(BigDecimal montantTTC, BigDecimal vatRatio) {
        if (vatRatio == null || vatRatio.signum() == 0) {
            return montantTTC;
        }
        return montantTTC.divide(BigDecimal.ONE.add(vatRatio), 6, RoundingMode.HALF_UP);
    }

    /**
     * Auto-generates the "Schéma de financement" on Dossier creation, persisting each line as a
     * {@code ComDocument} échéance. Each category is looked up by exact {@code Code} scoped to
     * the folder's {@code StkVocation} (confirmed: AUTO-FINANCEMENT/AVANCE/FRAIS SYNDIC all share
     * the same "VENTE" vocation in real data) — a line whose category doesn't exist for that
     * vocation is silently skipped (matches the pre-existing behaviour, never blocks creation).
     */
    private void genererSchemaFinancement(ComFolder folder, CfgTranche tranche, BigDecimal prix) {
        UUID vocationId = folder.getStkVocation().getId();
        for (SchemaFinancementCalculator.Ligne ligne : SchemaFinancementCalculator.calculer(tranche, prix)) {
            settlementCategoryRepository.findByCodeAndStkVocationId(ligne.categorieCode(), vocationId)
                    .ifPresent(categorie -> {
                        ComDocument document = new ComDocument();
                        document.setId(UUID.randomUUID());
                        document.setComFolder(folder);
                        document.setDocDate(LocalDateTime.now());
                        document.setAmount(ligne.montant());
                        document.setComSettlementCategory(categorie);
                        document.setExcludeFromPrice(categorie.getExcludeFromPrice());
                        document.setDescription(categorie.getDescription());
                        documentRepository.save(document);
                    });
        }
    }

    /**
     * {@code POST /api/dossiers/simulation-financement}: étape de prévisualisation AVANT création
     * du dossier — calcule le même "Schéma de financement" que {@link #create} générerait
     * ({@link SchemaFinancementCalculator#calculer}, la même méthode partagée), mais ne persiste rien (aucun
     * {@code ComFolder}/{@code ComDocument} créé). Le premier article de {@code articleIds} fait
     * foi pour la tranche (barème AVANCE/SYNDIC) et la vocation (catégorie d'échéance) — même
     * convention que {@link #create}. Contrairement à la création réelle, les articles n'ont pas
     * besoin d'être DISPONIBLE ici : c'est une simulation, rien n'est réservé.
     *
     * {@code remisePercent} suit exactement la même sémantique qu'à la création (voir
     * {@link #appliquerMontants}) : fourni, le schéma porte sur le prix APRÈS remise, et les
     * articles suivants comptent alors comme annexes dans le prix avant remise.
     */
    public SchemaFinancementDto simulerSchemaFinancement(SimulerSchemaFinancementRequest request) {
        if (request.articleIds() == null || request.articleIds().isEmpty()) {
            throw new InvalidRequestException("articleIds est requis et ne peut pas être vide");
        }
        Requis.champ(request.prix(), "prix est requis");
        validerRemisePercent(request.remisePercent());

        StkItem premierArticle = stkItemRepository.findById(request.articleIds().get(0))
                .orElseThrow(() -> new InvalidRequestException("Article introuvable: " + request.articleIds().get(0)));
        // Charge aussi les articles suivants (mêmes erreurs qu'à la création réelle) : ils servent
        // d'annexes dans le calcul de la remise explicite, exactement comme à la création.
        List<StkItem> annexes = new ArrayList<>();
        for (UUID articleId : request.articleIds().subList(1, request.articleIds().size())) {
            annexes.add(stkItemRepository.findById(articleId)
                    .orElseThrow(() -> new InvalidRequestException("Article introuvable: " + articleId)));
        }

        UUID vocationId = premierArticle.getStkVocation().getId();
        CfgTranche tranche = premierArticle.getStkHierarchy();

        // Le schéma se calcule sur le prix APRÈS remise. Plutôt que de dupliquer la formule, on
        // réutilise appliquerMontants sur un ComFolder jetable, jamais persisté : la simulation ne peut
        // donc pas diverger de ce que POST /api/dossiers produira pour les mêmes paramètres.
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
     * {@code PATCH /api/dossiers/{id}/statut}. Enforces {@link TransitionsValidees} — 409 via
     * {@link InvalidStatutTransitionException} on an invalid transition. A dossier with no
     * current status (new/never set) accepts any valid {@link StatutDossier} as its first one.
     */
    @Transactional
    public DossierDetailDto changerStatut(UUID id, ChangerStatutRequest request) {
        ComFolder folder = folderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable: " + id));

        Requis.champ(request.statut(), "statut est requis");
        StatutDossier cible;
        try {
            cible = StatutDossier.fromCode(request.statut());
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Statut inconnu: " + request.statut());
        }

        StatutDossier actuel = folder.getStrStatus() != null && folder.getStrStatus().getCode() != null
                ? tryFromCode(folder.getStrStatus().getCode())
                : null;

        if (actuel != null && !TransitionsValidees.estValide(actuel, cible)) {
            List<String> autorisees = TransitionsValidees.transitionsAutorisees(actuel).stream()
                    .map(StatutDossier::getCode)
                    .toList();
            throw new InvalidStatutTransitionException(
                    "Transition invalide: " + actuel.getCode() + " -> " + cible.getCode(), autorisees);
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
     * Writes {@code request.date()} onto the target status's confirmed "milestone" column (see
     * {@link com.tsix.immocrm.domain.EtapeMilestone}) and applies {@code request.note()} —
     * {@code ANNULATION} routes it to {@code CancelDescription} (the cancellation cause, per
     * real data), every other status to the generic {@code Notes}. No-op (beyond the note) for
     * statuses with no confirmed milestone column (e.g. RESERVATION, CREDIT_EN_COURS).
     */
    /**
     * Records the transition on {@code StrFolderStatusHistory} — the same table
     * {@link TransitionsValidees} was mined from — so status changes made through this API are
     * traceable the same way legacy desktop-app changes are (see
     * {@code GET /api/dossiers/{id}/historique}). {@code note} is stored as {@code Description},
     * matching real data usage (e.g. "ANNULATION AFFECTATION").
     */
    private void enregistrerHistoriqueStatut(ComFolder folder, StrStatus nouveauStatut, String note) {
        StrFolderStatusHistory historique = new StrFolderStatusHistory();
        historique.setId(UUID.randomUUID());
        historique.setDocDate(LocalDateTime.now());
        historique.setComFolder(folder);
        historique.setStrFolderStatus(nouveauStatut);
        historique.setDescription(note);
        statusHistoryRepository.save(historique);
    }

    private static void appliquerChampEtape(ComFolder folder, StatutDossier cible, ChangerStatutRequest request) {
        if (cible == StatutDossier.ANNULATION) {
            folder.setCancelDate(request.date() != null ? request.date() : LocalDateTime.now());
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
                // RESERVATION, CREDIT_EN_COURS, LIVRAISON_PROVISOIRE, DESISTEMENT: no confirmed
                // milestone column — see EtapeMilestone.
            }
        }
    }

    /**
     * {@code GET /api/dossiers/{id}/montants}: recap grouped by category, computed
     * server-side from the échéancier — never stored. {@code ComDocument.excludeFromPrice}
     * lines (e.g. FRAIS) are counted in {@code parCategorie}/{@code totalGeneral} but excluded
     * from {@code totalSoumisReglePrixTTC}.
     */
    public MontantsDossierDto montants(UUID dossierId) {
        ComFolder folder = folderRepository.findById(dossierId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable: " + dossierId));
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
     * Ligne « Prix de vente » de l'onglet Montants du logiciel de bureau.
     *
     * <p>Le montant vient de {@code ComFolder.Price} et n'est <strong>pas</strong> re-dérivé de
     * l'échéancier : les deux coïncident sur 327 des 328 dossiers actifs réels, et c'est le prix
     * stocké qui fait foi — c'est aussi ce que lit la vue légataire.
     */
    private static SyntheseMontantDto prixVente(ComFolder f, BigDecimal regleSoumisAuPrix) {
        BigDecimal montant = requireNonNullElse(f.getPrice(), BigDecimal.ZERO);
        return new SyntheseMontantDto(montant, regleSoumisAuPrix, montant.subtract(regleSoumisAuPrix));
    }

    /**
     * Ligne « Net à payer » : le prix de vente augmenté des frais exclus du prix.
     *
     * <p><strong>Sauf sur un dossier sorti</strong> ({@code ANNULATION} / {@code DESISTEMENT}), où
     * le net à payer se réduit à ce qui a déjà été encaissé et le reste tombe à zéro — un dossier
     * annulé ne reste rien devoir. Les deux règles sont vérifiées sans exception sur les 358
     * dossiers réels : 328/328 pour les actifs, 30/30 pour les sortis. Voir
     * {@code instructions/FEATURE-fiche-dossier-alignement-ecran.md} §5.
     */
    private static SyntheseMontantDto netAPayer(ComFolder f, BigDecimal fraisExclusDuPrix,
                                                BigDecimal regleTotal) {
        if (estSorti(f)) {
            return new SyntheseMontantDto(regleTotal, regleTotal, BigDecimal.ZERO);
        }
        BigDecimal montant = requireNonNullElse(f.getPrice(), BigDecimal.ZERO).add(fraisExclusDuPrix);
        return new SyntheseMontantDto(montant, regleTotal, montant.subtract(regleTotal));
    }

    /**
     * Dossier sorti du parcours. Équivaut exactement au {@code IsCancellation} de la vue légataire
     * — vérifié 30/30 (22 {@code ANNULATION} + 8 {@code DESISTEMENT}), aucune exception —, donc
     * aucune colonne supplémentaire n'est mappée pour l'obtenir.
     */
    private static boolean estSorti(ComFolder f) {
        if (f.getStrStatus() == null || f.getStrStatus().getCode() == null) {
            return false;
        }
        // fromCode lève sur un code hors référentiel : un statut inconnu ne doit pas faire tomber
        // le calcul des montants en 500. Même repli défensif que etapesAtteignables.
        try {
            return StatutDossier.fromCode(f.getStrStatus().getCode()).isSortie();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static StatutDossier tryFromCode(String code) {
        try {
            return StatutDossier.fromCode(code);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * {@code GET /api/dossiers/{id}/statut/etapes-suivantes}: for each status reachable from the
     * Dossier's current one, which {@code ComFolder} milestone column (if any) {@code PATCH
     * .../statut}'s {@code date} would be written to — lets the front-end know which date to ask
     * for, per {@link com.tsix.immocrm.domain.EtapeMilestone}.
     */
    public List<EtapeSuivanteDto> etapesSuivantes(UUID id) {
        ComFolder folder = folderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable: " + id));

        StatutDossier actuel = folder.getStrStatus() != null && folder.getStrStatus().getCode() != null
                ? tryFromCode(folder.getStrStatus().getCode())
                : null;
        if (actuel == null) {
            return List.of();
        }
        return TransitionsValidees.transitionsAutorisees(actuel).stream()
                .map(statut -> {
                    EtapeMilestone.Info info = EtapeMilestone.of(statut);
                    return new EtapeSuivanteDto(
                            statut.getCode(), info.champ(), info.libelle(), info.confiance().name());
                })
                .toList();
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
     * Mappeur de la liste des dossiers.
     *
     * <p>Le <strong>contexte patrimonial n'est traversé que s'il est demandé</strong> : la chaîne
     * article → tranche → projet → société est de trois jointures paresseuses par ligne, soit
     * soixante requêtes inutiles sur une page de vingt quand personne ne les réclame. Même règle
     * pour la prospection d'origine.
     *
     * @param colonnes sélection résolue ; {@code Set.of()} pour les appels hors liste.
     */
    public static DossierListItemDto toListItem(ComFolder f, Set<ColonneDossier> colonnes) {
        boolean patrimoine = ColonneDossier.exigeLePatrimoine(colonnes);
        CfgTranche tranche = patrimoine ? tranche(f) : null;
        CfgProject projet = patrimoine ? projet(f) : null;
        CfgCompany societe = patrimoine ? societe(f) : null;
        ComProspection prospection = ColonneDossier.exigeLaProspection(colonnes)
                ? f.getComProspection() : null;
        return new DossierListItemDto(
                f.getId(),
                f.getCode(),
                f.getDocDate(),
                f.getStrStatus() != null ? f.getStrStatus().getCode() : null,
                f.getCfgCustomer() != null ? f.getCfgCustomer().getId() : null,
                TierNomFormatter.nom(f.getCfgCustomer()),
                f.getCfgCommercial() != null ? f.getCfgCommercial().getId() : null,
                TierNomFormatter.nom(f.getCfgCommercial()),
                f.getPrice(),
                f.getTotalToPay(),
                f.getAdvanceDate(),
                f.getComProspection() != null ? f.getComProspection().getId() : null,
                f.getComProspection() != null ? f.getComProspection().getCode() : null,
                f.getPriceBeforeDiscount(),
                f.getDiscount(),
                f.getDiscountAmount(),
                f.getPriceAfterDiscount(),
                // Le "montant d'avance" de la fiche vient d'AgreementAmount (99,7 % des 358
                // dossiers), pas d'une colonne AdvanceAmount : ComFolder n'en porte pas.
                f.getAgreementAmount(),
                f.getProbabilityOfFinalisation(),
                projet != null ? projet.getId() : null,
                projet != null ? projet.getCode() : null,
                projet != null ? projet.getDescription() : null,
                tranche != null ? tranche.getId() : null,
                tranche != null ? tranche.getCode() : null,
                tranche != null ? tranche.getDescription() : null,
                societe != null ? societe.getId() : null,
                societe != null ? societe.getCode() : null,
                societe != null ? societe.getDescription() : null,
                patrimoine && f.getStkVocation() != null ? f.getStkVocation().getCode() : null,
                patrimoine && f.getStkVocation() != null ? f.getStkVocation().getDescription() : null,
                colonnes.contains(ColonneDossier.AVOCAT_ID) && f.getCfgLawyer() != null
                        ? f.getCfgLawyer().getId() : null,
                colonnes.contains(ColonneDossier.AVOCAT_NOM) && f.getCfgLawyer() != null
                        ? TierNomFormatter.nom(f.getCfgLawyer()) : null,
                colonnes.contains(ColonneDossier.MODE_PAIE) && f.getComPaymentMethod() != null
                        ? f.getComPaymentMethod().getCode() : null,
                prospection != null && prospection.getComProspectionKind() != null
                        ? prospection.getComProspectionKind().getDescription() : null,
                prospection != null && prospection.getComProspectionOrigin() != null
                        ? prospection.getComProspectionOrigin().getDescription() : null,
                prospection != null ? prospection.getDocDate() : null,
                f.getUserCreate(),
                f.getUserCreateDateTime(),
                f.getUserUpdate(),
                f.getUserUpdateDateTime()
        );
    }

    private static DossierDetailDto toDetail(ComFolder f, List<ArticleSummaryDto> articles) {
        return new DossierDetailDto(
                f.getId(),
                f.getCode(),
                f.getDocDate(),
                f.getStrStatus() != null ? f.getStrStatus().getCode() : null,
                etapesAtteignables(f),
                f.getCfgCustomer() != null ? f.getCfgCustomer().getId() : null,
                TierNomFormatter.nom(f.getCfgCustomer()),
                f.getCfgCommercial() != null ? f.getCfgCommercial().getId() : null,
                TierNomFormatter.nom(f.getCfgCommercial()),
                f.getCfgLawyer() != null ? f.getCfgLawyer().getId() : null,
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
                f.getComPaymentMethod() != null ? f.getComPaymentMethod().getCode() : null,
                f.getUnitCost(),
                f.getPriceHT(),
                f.getPriceBeforeDiscountHT(),
                f.getVATRatio(),
                f.getStatusValidityDate(),
                f.getPartPercent(),
                f.getComProspection() != null ? f.getComProspection().getId() : null,
                f.getComProspection() != null ? f.getComProspection().getCode() : null,
                // -- Contexte patrimonial. La chaîne article → tranche → projet → société est
                //    vérifiée à 358/358, mais chaque maillon est déréférencé défensivement : la
                //    vue légataire la traverse en LEFT JOIN, donc rien ne garantit en base qu'un
                //    maillon manquant soit impossible.
                projet(f) != null ? projet(f).getId() : null,
                projet(f) != null ? projet(f).getCode() : null,
                projet(f) != null ? projet(f).getDescription() : null,
                tranche(f) != null ? tranche(f).getId() : null,
                tranche(f) != null ? tranche(f).getCode() : null,
                tranche(f) != null ? tranche(f).getDescription() : null,
                societe(f) != null ? societe(f).getId() : null,
                societe(f) != null ? societe(f).getCode() : null,
                societe(f) != null ? societe(f).getDescription() : null,
                f.getStkVocation() != null ? f.getStkVocation().getId() : null,
                f.getStkVocation() != null ? f.getStkVocation().getCode() : null,
                f.getStkVocation() != null ? f.getStkVocation().getDescription() : null,
                f.getUserCreate(),
                f.getUserCreateDateTime(),
                f.getUserUpdate(),
                f.getUserUpdateDateTime(),
                f.getComProspection() != null && f.getComProspection().getComProspectionKind() != null
                        ? f.getComProspection().getComProspectionKind().getDescription() : null,
                f.getComProspection() != null && f.getComProspection().getComProspectionOrigin() != null
                        ? f.getComProspection().getComProspectionOrigin().getDescription() : null,
                f.getComProspection() != null ? f.getComProspection().getDocDate() : null
        );
    }

    /**
     * Tranche du dossier, portée par son article principal.
     *
     * <p>{@code ComFolder} n'a pas de tranche à lui : elle vient de {@code StkItem.StkHierarchyId},
     * renseigné sur les 358 articles de dossier réels.
     */
    private static CfgTranche tranche(ComFolder f) {
        return f.getStkItem() != null ? f.getStkItem().getStkHierarchy() : null;
    }

    /**
     * Projet du dossier — <strong>via la tranche, jamais via l'article</strong>.
     *
     * <p>{@code StkItem.CfgProjectId} est {@code null} sur les <strong>358</strong> articles de
     * dossier de la base réelle. Le lire directement rendrait {@code null} partout, sans erreur
     * visible : c'est le genre de mapping qui passe les tests et ne remplit jamais l'écran.
     */
    private static CfgProject projet(ComFolder f) {
        CfgTranche t = tranche(f);
        return t != null ? t.getCfgProject() : null;
    }

    /** Entité juridique sous laquelle le projet est vendu. */
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

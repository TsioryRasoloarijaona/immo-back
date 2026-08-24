package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.domain.ChampDateDossier;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.domain.TransitionsValidees;
import com.tsix.immocrm.dto.dossier.ArticleSummaryDto;
import com.tsix.immocrm.dto.dossier.CategorieMontantDto;
import com.tsix.immocrm.dto.dossier.CreerDossierRequest;
import com.tsix.immocrm.dto.dossier.DossierDetailDto;
import com.tsix.immocrm.dto.dossier.DossierListItemDto;
import com.tsix.immocrm.dto.dossier.MontantsDossierDto;
import com.tsix.immocrm.entities.adm.AdmSequence;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComPaymentMethod;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.dto.dossier.ChangerStatutRequest;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.InvalidStatutTransitionException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComPaymentMethodRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DossierService {

    /**
     * Fallback advance ratio when the tranche has no {@code AdvancePercent} configured — per
     * business decision ("l'avance c'est 1/3 par défaut"). Real tranches carry their own
     * {@code AdvancePercent} (e.g. 0.30 confirmed on tranches APF/ARS), which always wins.
     */
    private static final BigDecimal DEFAULT_ADVANCE_PERCENT =
            BigDecimal.ONE.divide(BigDecimal.valueOf(3), 6, RoundingMode.HALF_UP);

    private final ComFolderRepository folderRepository;
    private final StkItemRepository stkItemRepository;
    private final ComProspectionRepository prospectionRepository;
    private final StrStatusRepository strStatusRepository;
    private final ComDocumentRepository documentRepository;
    private final StlSettlementLineRepository settlementLineRepository;
    private final ComPaymentMethodRepository paymentMethodRepository;
    private final CfgTierRepository tierRepository;
    private final ComSettlementCategoryRepository settlementCategoryRepository;
    private final com.tsix.immocrm.repositories.str.StrFolderStatusHistoryRepository statusHistoryRepository;
    private final com.tsix.immocrm.repositories.adm.AdmSequenceRepository sequenceRepository;

    public DossierService(ComFolderRepository folderRepository, StkItemRepository stkItemRepository,
                           ComProspectionRepository prospectionRepository, StrStatusRepository strStatusRepository,
                           ComDocumentRepository documentRepository, StlSettlementLineRepository settlementLineRepository,
                           ComPaymentMethodRepository paymentMethodRepository, CfgTierRepository tierRepository,
                           ComSettlementCategoryRepository settlementCategoryRepository,
                           com.tsix.immocrm.repositories.str.StrFolderStatusHistoryRepository statusHistoryRepository,
                           com.tsix.immocrm.repositories.adm.AdmSequenceRepository sequenceRepository) {
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
     * @param projet     {@code CfgProject.id} filter, matched via the folder's primary
     *                   article's project ({@code ComFolder.stkItem.cfgProject}) — a folder's
     *                   linked articles are assumed to share one project.
     * @param trancheIds {@code CfgTranche.id} filter — matches a dossier if at least one of its
     *                   linked articles ({@code StkItem.comFolderId}) belongs to one of these
     *                   tranches ({@code StkItem.stkHierarchy}). A dossier can have several
     *                   articles (main lot + annexes) possibly spanning tranches, hence the
     *                   {@code EXISTS} subquery rather than a direct join. See
     *                   {@code instructions/filtres-avances.md} point 2.
     * @param champDate  which {@code ComFolder} milestone column {@code dateDebut}/{@code dateFin}
     *                   apply to (defaults to {@link ChampDateDossier#RESERVATION} — i.e.
     *                   {@code docDate} — if a range is given without an explicit field). Ignored
     *                   if neither {@code dateDebut} nor {@code dateFin} is given. See
     *                   {@code instructions/filtres-avances.md} point 3.
     * @param dateDebut  inclusive lower bound on {@code champDate}'s column
     * @param dateFin    inclusive upper bound on {@code champDate}'s column
     * @param search     free-text match on code
     */
    public Page<DossierListItemDto> findAll(List<String> statuts, UUID commercial, UUID projet,
            List<UUID> trancheIds, ChampDateDossier champDate, LocalDateTime dateDebut, LocalDateTime dateFin,
            String search, Pageable pageable) {
        Specification<ComFolder> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (statuts != null && !statuts.isEmpty()) {
                List<String> statutsLower = statuts.stream().map(String::toLowerCase).toList();
                predicates.add(cb.lower(root.get("strStatus").get("code")).in(statutsLower));
            }
            if (commercial != null) {
                predicates.add(cb.equal(root.get("cfgCommercial").get("id"), commercial));
            }
            if (projet != null) {
                predicates.add(cb.equal(root.get("stkItem").get("cfgProject").get("id"), projet));
            }
            if (trancheIds != null && !trancheIds.isEmpty()) {
                jakarta.persistence.criteria.Subquery<UUID> subquery = query.subquery(UUID.class);
                jakarta.persistence.criteria.Root<StkItem> article = subquery.from(StkItem.class);
                subquery.select(article.get("id"))
                        .where(cb.and(
                                cb.equal(article.get("comFolderId"), root.get("id")),
                                article.get("stkHierarchy").get("id").in(trancheIds)));
                predicates.add(cb.exists(subquery));
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
                predicates.add(cb.like(cb.lower(root.get("code")), "%" + search.toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return folderRepository.findAll(spec, pageable).map(DossierService::toListItem);
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
        if (request.prospectionId() == null) {
            throw new InvalidRequestException("prospectionId est requis");
        }
        ComProspection prospection = prospectionRepository.findById(request.prospectionId())
                .orElseThrow(() -> new InvalidRequestException("Prospection introuvable: " + request.prospectionId()));
        if (prospection.getCfgProspect() == null) {
            throw new InvalidRequestException("La prospection n'a pas de prospect lié: " + request.prospectionId());
        }
        if (request.articleIds() == null || request.articleIds().isEmpty()) {
            throw new InvalidRequestException("articleIds est requis et ne peut pas être vide");
        }
        if (request.modePaieId() == null) {
            throw new InvalidRequestException("modePaieId est requis");
        }
        if (!StringUtils.hasText(request.cin())) {
            throw new InvalidRequestException("cin est requis");
        }
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
        folder.setPrice(request.prix());
        folder.setNotes(request.notes());
        folder.setComPaymentMethod(modePaie);
        // Initial status: confirmed 100% of real dossiers (359/359 with history) start at
        // RESERVATION — no exception. Doesn't block creation if the StrStatus row is somehow
        // missing (defensive, same spirit as genererCodeDossier's self-healing).
        StrStatus statutInitial = strStatusRepository.findByStrConfigEntityIdAndCode(
                StatutDossier.CONFIG_ENTITY_ID, StatutDossier.RESERVATION.getCode());
        folder.setStrStatus(statutInitial);
        folder = folderRepository.save(folder);
        if (statutInitial != null) {
            enregistrerHistoriqueStatut(folder, statutInitial, null);
        }

        for (StkItem item : articles) {
            item.setComFolderId(folder.getId());
            stkItemRepository.save(item);
        }

        if (request.prix() != null) {
            genererSchemaFinancement(folder, articles.get(0).getStkHierarchy(), request.prix());
        }

        List<ArticleSummaryDto> articleDtos = articles.stream().map(DossierService::toArticleSummary).toList();
        return toDetail(folder, articleDtos);
    }

    /**
     * Auto-generates the 3-line "Schéma de financement" on Dossier creation — confirmed against
     * real data (Omran "Schéma de financement" tab):
     * <ul>
     *   <li>{@code AVANCE} = prix × {@code CfgTranche.AdvancePercent} (e.g. 0.30 on real
     *       tranches APF/ARS; falls back to 1/3 if the tranche has none configured)</li>
     *   <li>{@code AUTO-FINANCEMENT} = prix − avance</li>
     *   <li>{@code FRAIS SYNDIC} = {@code CfgTranche.SyndicAmount} × {@code SyndicPeriod}
     *       (confirmed: 700.00 × 24 = 16 800.00 matches a real dossier exactly) — only created
     *       if the tranche has both configured; skipped otherwise (no syndic fee applies)</li>
     * </ul>
     * Each category is looked up by exact {@code Code} scoped to the folder's
     * {@code StkVocation} (confirmed: AUTO-FINANCEMENT/AVANCE/FRAIS SYNDIC all share the same
     * "VENTE" vocation in real data, and other categories reuse the same boolean flags, so code
     * text — not the flags — is what disambiguates them). {@code ExcludeFromPrice} on the
     * generated line mirrors the category's own default (true for FRAIS SYNDIC, confirmed).
     */
    private void genererSchemaFinancement(ComFolder folder, CfgTranche tranche, BigDecimal prix) {
        UUID vocationId = folder.getStkVocation().getId();
        BigDecimal advancePercent = tranche != null && tranche.getAdvancePercent() != null
                ? tranche.getAdvancePercent()
                : DEFAULT_ADVANCE_PERCENT;

        BigDecimal avance = prix.multiply(advancePercent).setScale(2, RoundingMode.HALF_UP);
        BigDecimal autoFinancement = prix.subtract(avance);

        creerLigneSchema(folder, "AUTO-FINANCEMENT", vocationId, autoFinancement);
        creerLigneSchema(folder, "AVANCE", vocationId, avance);

        if (tranche != null && tranche.getSyndicAmount() != null && tranche.getSyndicPeriod() != null
                && tranche.getSyndicPeriod() > 0) {
            BigDecimal fraisSyndic = tranche.getSyndicAmount()
                    .multiply(BigDecimal.valueOf(tranche.getSyndicPeriod()))
                    .setScale(2, RoundingMode.HALF_UP);
            creerLigneSchema(folder, "FRAIS SYNDIC", vocationId, fraisSyndic);
        }
    }

    private void creerLigneSchema(ComFolder folder, String categorieCode, UUID vocationId, BigDecimal montant) {
        settlementCategoryRepository.findByCodeAndStkVocationId(categorieCode, vocationId).ifPresent(categorie -> {
            ComDocument document = new ComDocument();
            document.setId(UUID.randomUUID());
            document.setComFolder(folder);
            document.setDocDate(LocalDateTime.now());
            document.setAmount(montant);
            document.setComSettlementCategory(categorie);
            document.setExcludeFromPrice(categorie.getExcludeFromPrice());
            document.setDescription(categorie.getDescription());
            documentRepository.save(document);
        });
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

        if (request.statut() == null) {
            throw new InvalidRequestException("statut est requis");
        }
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
        com.tsix.immocrm.entities.str.StrFolderStatusHistory historique = new com.tsix.immocrm.entities.str.StrFolderStatusHistory();
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
        if (!folderRepository.existsById(dossierId)) {
            throw new ResourceNotFoundException("Dossier introuvable: " + dossierId);
        }
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

        for (ComDocument ligne : lignes) {
            String categorie = ligne.getComSettlementCategory() != null ? ligne.getComSettlementCategory().getCode() : "SANS_CATEGORIE";
            BigDecimal montant = ligne.getAmount() != null ? ligne.getAmount() : BigDecimal.ZERO;
            BigDecimal regle = regleParEcheance.getOrDefault(ligne.getId(), BigDecimal.ZERO);

            montantParCategorie.merge(categorie, montant, BigDecimal::add);
            nombreParCategorie.merge(categorie, 1, Integer::sum);
            regleParCategorie.merge(categorie, regle, BigDecimal::add);
            totalGeneral = totalGeneral.add(montant);
            if (!Boolean.TRUE.equals(ligne.getExcludeFromPrice())) {
                totalSoumisReglePrixTTC = totalSoumisReglePrixTTC.add(montant);
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

        return new MontantsDossierDto(parCategorie, totalGeneral, totalSoumisReglePrixTTC);
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
    public List<com.tsix.immocrm.dto.dossier.EtapeSuivanteDto> etapesSuivantes(UUID id) {
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
                    com.tsix.immocrm.domain.EtapeMilestone.Info info = com.tsix.immocrm.domain.EtapeMilestone.of(statut);
                    return new com.tsix.immocrm.dto.dossier.EtapeSuivanteDto(
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

    private static DossierListItemDto toListItem(ComFolder f) {
        return new DossierListItemDto(
                f.getId(),
                f.getCode(),
                f.getDocDate(),
                f.getStrStatus() != null ? f.getStrStatus().getCode() : null,
                f.getCfgCustomer() != null ? f.getCfgCustomer().getId() : null,
                nomTier(f.getCfgCustomer()),
                f.getCfgCommercial() != null ? f.getCfgCommercial().getId() : null,
                nomTier(f.getCfgCommercial()),
                f.getPrice(),
                f.getTotalToPay(),
                f.getAdvanceDate()
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
                nomTier(f.getCfgCustomer()),
                f.getCfgCommercial() != null ? f.getCfgCommercial().getId() : null,
                nomTier(f.getCfgCommercial()),
                f.getCfgLawyer() != null ? f.getCfgLawyer().getId() : null,
                nomTier(f.getCfgLawyer()),
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
                f.getComPaymentMethod() != null ? f.getComPaymentMethod().getCode() : null
        );
    }

    private static ArticleSummaryDto toArticleSummary(StkItem item) {
        return new ArticleSummaryDto(item.getId(), item.getCode(), item.getDescription(), item.getArea(), item.getSalePrice());
    }

    private static String nomTier(CfgTier tier) {
        if (tier == null) return null;
        if (StringUtils.hasText(tier.getSocialReason())) return tier.getSocialReason();
        String prenom = tier.getFirstName() != null ? tier.getFirstName() : "";
        String nom = tier.getLastName() != null ? tier.getLastName() : "";
        return (prenom + " " + nom).trim();
    }
}

package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.domain.TransitionsValidees;
import com.tsix.immocrm.dto.dossier.ArticleSummaryDto;
import com.tsix.immocrm.dto.dossier.CategorieMontantDto;
import com.tsix.immocrm.dto.dossier.CreerDossierRequest;
import com.tsix.immocrm.dto.dossier.DossierDetailDto;
import com.tsix.immocrm.dto.dossier.DossierListItemDto;
import com.tsix.immocrm.dto.dossier.MontantsDossierDto;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.dto.dossier.ChangerStatutRequest;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.InvalidStatutTransitionException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    public DossierService(ComFolderRepository folderRepository, StkItemRepository stkItemRepository,
                           ComProspectionRepository prospectionRepository, StrStatusRepository strStatusRepository,
                           ComDocumentRepository documentRepository, StlSettlementLineRepository settlementLineRepository) {
        this.folderRepository = folderRepository;
        this.stkItemRepository = stkItemRepository;
        this.prospectionRepository = prospectionRepository;
        this.documentRepository = documentRepository;
        this.strStatusRepository = strStatusRepository;
        this.settlementLineRepository = settlementLineRepository;
    }

    /**
     * @param statut     {@code StrStatus.code} filter
     * @param commercial {@code CfgTier.id} (cfgCommercial) filter
     * @param projet     {@code CfgProject.id} filter, matched via the folder's primary
     *                   article's project ({@code ComFolder.stkItem.cfgProject}) — a folder's
     *                   linked articles are assumed to share one project.
     * @param search     free-text match on code
     */
    public List<DossierListItemDto> findAll(String statut, UUID commercial, UUID projet, String search) {
        Specification<ComFolder> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(statut)) {
                predicates.add(cb.equal(cb.lower(root.get("strStatus").get("code")), statut.toLowerCase()));
            }
            if (commercial != null) {
                predicates.add(cb.equal(root.get("cfgCommercial").get("id"), commercial));
            }
            if (projet != null) {
                predicates.add(cb.equal(root.get("stkItem").get("cfgProject").get("id"), projet));
            }
            if (StringUtils.hasText(search)) {
                predicates.add(cb.like(cb.lower(root.get("code")), "%" + search.toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return folderRepository.findAll(spec).stream().map(DossierService::toListItem).toList();
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

        List<StkItem> articles = new ArrayList<>();
        for (UUID articleId : request.articleIds()) {
            StkItem item = stkItemRepository.findById(articleId)
                    .orElseThrow(() -> new InvalidRequestException("Article introuvable: " + articleId));
            if (item.getComFolderId() != null) {
                throw new InvalidRequestException("Article déjà occupé (lié à un autre dossier): " + articleId);
            }
            articles.add(item);
        }

        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setStkVocation(articles.get(0).getStkVocation());
        folder.setCfgCustomer(prospection.getCfgProspect());
        folder.setCfgCommercial(prospection.getCfgCommercial());
        folder.setComProspection(prospection);
        folder.setPrice(request.prix());
        folder.setNotes(request.notes());
        folder = folderRepository.save(folder);

        for (StkItem item : articles) {
            item.setComFolderId(folder.getId());
            stkItemRepository.save(item);
        }

        List<ArticleSummaryDto> articleDtos = articles.stream().map(DossierService::toArticleSummary).toList();
        return toDetail(folder, articleDtos);
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
        if (request.note() != null) {
            folder.setNotes(request.note());
        }

        List<ArticleSummaryDto> articles = stkItemRepository.findByComFolderId(id).stream()
                .map(DossierService::toArticleSummary)
                .toList();
        return toDetail(folderRepository.save(folder), articles);
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
            String categorie = ligne.getStlSettlementType() != null ? ligne.getStlSettlementType().getCode() : "SANS_CATEGORIE";
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
                f.getTotalToPay()
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

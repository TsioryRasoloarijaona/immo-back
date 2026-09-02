package com.tsix.immocrm.services.prospect;

import com.tsix.immocrm.dto.dossier.DossierListItemDto;
import com.tsix.immocrm.dto.prospect.ContactProspectDto;
import com.tsix.immocrm.dto.prospect.CreerContactProspectRequest;
import com.tsix.immocrm.dto.prospect.HistoriqueDto;
import com.tsix.immocrm.dto.prospect.ReglementProspectDto;
import com.tsix.immocrm.dto.prospection.ProspectionListItemDto;
import com.tsix.immocrm.entities.cfg.CfgRelationType;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierContact;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.str.StrHistory;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.cfg.CfgRelationTypeRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierContactRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import com.tsix.immocrm.repositories.str.StrHistoryRepository;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.utils.TierNomFormatter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

/**
 * GAP-06 — sub-resources of the prospect fiche: linked prospections, dossiers, règlements
 * (all dossiers combined), historique, and contacts (conjoint/notaire/apporteur...).
 */
@Service
@Transactional(readOnly = true)
public class ProspectSubResourcesService {

    private final CfgTierRepository tierRepository;
    private final ComProspectionRepository prospectionRepository;
    private final ComFolderRepository folderRepository;
    private final StlSettlementRepository settlementRepository;
    private final StlSettlementLineRepository settlementLineRepository;
    private final StrHistoryRepository historyRepository;
    private final CfgTierContactRepository tierContactRepository;
    private final CfgRelationTypeRepository relationTypeRepository;
    private final PerimetreCommercialService perimetreService;

    public ProspectSubResourcesService(CfgTierRepository tierRepository,
                                        ComProspectionRepository prospectionRepository,
                                        ComFolderRepository folderRepository,
                                        StlSettlementRepository settlementRepository,
                                        StlSettlementLineRepository settlementLineRepository,
                                        StrHistoryRepository historyRepository,
                                        CfgTierContactRepository tierContactRepository,
                                        CfgRelationTypeRepository relationTypeRepository,
                                        PerimetreCommercialService perimetreService) {
        this.perimetreService = perimetreService;
        this.tierRepository = tierRepository;
        this.prospectionRepository = prospectionRepository;
        this.folderRepository = folderRepository;
        this.settlementRepository = settlementRepository;
        this.settlementLineRepository = settlementLineRepository;
        this.historyRepository = historyRepository;
        this.tierContactRepository = tierContactRepository;
        this.relationTypeRepository = relationTypeRepository;
    }

    private void requireProspect(UUID id) {
        if (!tierRepository.existsById(id)) {
            throw new ResourceNotFoundException("Prospect introuvable: " + id);
        }
    }

    /**
     * {@code GET /api/prospects/{id}/prospections}, et — via {@code HotlineService} — le bloc
     * "prospections" de {@code GET /api/hotline/recherche}.
     *
     * <p><b>Périmètre commercial (§9bis.2)</b> : ces trois sous-ressources renvoient exactement les
     * mêmes DTO que {@code GET /api/prospections}, {@code GET /api/dossiers} et
     * {@code GET /api/encaissements}, mais sous un préfixe d'URL que {@code PerimetreInterceptor}
     * ne couvre pas — le tier, lui, reste hors périmètre. Sans filtre ici, il suffisait d'énumérer
     * {@code GET /api/prospects} (non filtré, et qui doit le rester) pour reconstituer le pipeline
     * de tous les projets, sans même deviner un id : la frontière posée sur les listes principales
     * était contournable de bout en bout.
     *
     * <p>Le filtre masque les <b>lignes</b> hors périmètre ; il ne masque ni le prospect, ni le
     * client, ni la fiche hotline eux-mêmes.
     */
    public Page<ProspectionListItemDto> prospections(UUID prospectId, Pageable pageable) {
        requireProspect(prospectId);
        Specification<ComProspection> spec = Specification
                .<ComProspection>where((root, query, cb) -> cb.equal(root.get("cfgProspect").get("id"), prospectId))
                .and(perimetreService.filtreProspections());
        return prospectionRepository.findAll(spec, pageable)
                .map(ProspectSubResourcesService::toProspectionListItem);
    }

    /**
     * {@code GET /api/prospects/{id}/dossiers} <b>et</b> {@code GET /api/clients/{id}/dossiers}
     * (même service, voir {@code ClientController}), plus le bloc "dossiers" de la hotline.
     * Filtré par le périmètre commercial — voir {@link #prospections}.
     */
    public Page<DossierListItemDto> dossiers(UUID prospectId, Pageable pageable) {
        requireProspect(prospectId);
        Specification<ComFolder> spec = Specification
                .<ComFolder>where((root, query, cb) -> cb.equal(root.get("cfgCustomer").get("id"), prospectId))
                .and(perimetreService.filtreDossiers());
        return folderRepository.findAll(spec, pageable)
                .map(ProspectSubResourcesService::toDossierListItem);
    }

    /**
     * {@code GET /api/prospects/{id}/reglements} — tous dossiers confondus. Filtré par le périmètre
     * commercial (voir {@link #prospections}) : un règlement n'est visible que s'il porte au moins
     * une ligne rattachée à un dossier d'un projet du périmètre, ou si aucune de ses lignes ne
     * résout de projet.
     *
     * <p>Le tri {@code docDate} décroissant, auparavant porté par le nom de la méthode de
     * repository, est désormais appliqué explicitement lorsque l'appelant n'en fournit pas —
     * passer à un {@code Specification} fait perdre le {@code OrderBy} dérivé.
     */
    public Page<ReglementProspectDto> reglements(UUID prospectId, Pageable pageable) {
        requireProspect(prospectId);
        Specification<StlSettlement> spec = Specification
                .<StlSettlement>where((root, query, cb) -> cb.equal(root.get("tier").get("id"), prospectId))
                .and(perimetreService.filtreEncaissements());
        return settlementRepository.findAll(spec, parDefautPlusRecentDabord(pageable))
                .map(this::toReglementDto);
    }

    /** Conserve le tri historique (docDate DESC) quand l'appelant ne demande pas de tri explicite. */
    private static Pageable parDefautPlusRecentDabord(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "docDate"));
    }

    public Page<HistoriqueDto> historique(UUID prospectId, Pageable pageable) {
        requireProspect(prospectId);
        return historyRepository.findByObjectIdOrderByDocDateDesc(prospectId, pageable)
                .map(ProspectSubResourcesService::toHistoriqueDto);
    }

    public List<ContactProspectDto> contacts(UUID prospectId) {
        requireProspect(prospectId);
        return tierContactRepository.findByCfgTierIdOrderByUserCreateDateTimeDesc(prospectId).stream()
                .map(ProspectSubResourcesService::toContactDto).toList();
    }

    @Transactional
    public ContactProspectDto ajouterContact(UUID prospectId, CreerContactProspectRequest request) {
        CfgTier prospect = tierRepository.findById(prospectId)
                .orElseThrow(() -> new ResourceNotFoundException("Prospect introuvable: " + prospectId));
        if (request.contactProspectId() == null || request.relationTypeId() == null) {
            throw new InvalidRequestException("contactProspectId et relationTypeId sont requis");
        }
        CfgTier contact = tierRepository.findById(request.contactProspectId())
                .orElseThrow(() -> new InvalidRequestException("Contact (CfgTier) introuvable: " + request.contactProspectId()));
        CfgRelationType relationType = relationTypeRepository.findById(request.relationTypeId())
                .orElseThrow(() -> new InvalidRequestException("CfgRelationType introuvable: " + request.relationTypeId()));

        CfgTierContact link = new CfgTierContact();
        link.setId(UUID.randomUUID());
        link.setCfgTier(prospect);
        link.setCfgContact(contact);
        link.setCfgRelationType(relationType);
        return toContactDto(tierContactRepository.save(link));
    }

    @Transactional
    public void retirerContact(UUID prospectId, UUID contactId) {
        requireProspect(prospectId);
        tierContactRepository.deleteByIdAndCfgTierId(contactId, prospectId);
    }

    private ReglementProspectDto toReglementDto(StlSettlement s) {
        UUID dossierId = null;
        String dossierCode = null;
        List<Object[]> rows = settlementLineRepository.findDossierBySettlementId(s.getId());
        if (!rows.isEmpty()) {
            dossierId = (UUID) rows.get(0)[0];
            dossierCode = (String) rows.get(0)[1];
        }
        return new ReglementProspectDto(
                s.getId(),
                s.getCode(),
                s.getDocDate(),
                s.getSettlementDate(),
                s.getAmount(),
                s.getStlSettlementType() != null ? s.getStlSettlementType().getCode() : null,
                dossierId,
                dossierCode
        );
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

    private static ContactProspectDto toContactDto(CfgTierContact link) {
        return new ContactProspectDto(
                link.getId(),
                link.getCfgContact() != null ? link.getCfgContact().getId() : null,
                TierNomFormatter.nom(link.getCfgContact()),
                link.getCfgContact() != null ? link.getCfgContact().getPhone() : null,
                link.getCfgRelationType() != null ? link.getCfgRelationType().getId() : null,
                link.getCfgRelationType() != null ? link.getCfgRelationType().getDescription() : null
        );
    }

    private static ProspectionListItemDto toProspectionListItem(ComProspection p) {
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
                p.getStkHierarchy() != null ? p.getStkHierarchy().getDescription() : null
        );
    }

    private static DossierListItemDto toDossierListItem(ComFolder f) {
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
                f.getComProspection() != null ? f.getComProspection().getCode() : null
        );
    }
}

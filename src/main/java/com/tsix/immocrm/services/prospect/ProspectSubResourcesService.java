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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    public ProspectSubResourcesService(CfgTierRepository tierRepository,
                                        ComProspectionRepository prospectionRepository,
                                        ComFolderRepository folderRepository,
                                        StlSettlementRepository settlementRepository,
                                        StlSettlementLineRepository settlementLineRepository,
                                        StrHistoryRepository historyRepository,
                                        CfgTierContactRepository tierContactRepository,
                                        CfgRelationTypeRepository relationTypeRepository) {
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

    public Page<ProspectionListItemDto> prospections(UUID prospectId, Pageable pageable) {
        requireProspect(prospectId);
        return prospectionRepository.findByCfgProspectId(prospectId, pageable)
                .map(ProspectSubResourcesService::toProspectionListItem);
    }

    public Page<DossierListItemDto> dossiers(UUID prospectId, Pageable pageable) {
        requireProspect(prospectId);
        return folderRepository.findByCfgCustomerId(prospectId, pageable)
                .map(ProspectSubResourcesService::toDossierListItem);
    }

    public Page<ReglementProspectDto> reglements(UUID prospectId, Pageable pageable) {
        requireProspect(prospectId);
        return settlementRepository.findByTierIdOrderByDocDateDesc(prospectId, pageable)
                .map(this::toReglementDto);
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
                nomTier(link.getCfgContact()),
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
                nomTier(p.getCfgProspect()),
                p.getCfgCommercial() != null ? p.getCfgCommercial().getId() : null,
                nomTier(p.getCfgCommercial()),
                p.getComProspectionOrigin() != null ? p.getComProspectionOrigin().getCode() : null,
                p.getComProspectionKind() != null ? p.getComProspectionKind().getCode() : null,
                p.getIsClosed(),
                p.getPrice(),
                p.getArea()
        );
    }

    private static DossierListItemDto toDossierListItem(ComFolder f) {
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

    private static String nomTier(CfgTier tier) {
        if (tier == null) return null;
        if (StringUtils.hasText(tier.getSocialReason())) return tier.getSocialReason();
        String prenom = tier.getFirstName() != null ? tier.getFirstName() : "";
        String nom = tier.getLastName() != null ? tier.getLastName() : "";
        return (prenom + " " + nom).trim();
    }
}

package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.dto.dossier.CreerEcheanceRequest;
import com.tsix.immocrm.dto.dossier.CreerReglementRequest;
import com.tsix.immocrm.dto.dossier.EcheanceDto;
import com.tsix.immocrm.dto.dossier.ModifierEcheanceRequest;
import com.tsix.immocrm.dto.dossier.ReglementDto;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.entities.stl.StlSettlementType;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementTypeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Échéancier (payment schedule) sub-resource of a Dossier, backed by {@code ComDocument}. */
@Service
@Transactional(readOnly = true)
public class EcheancierService {

    private final ComFolderRepository folderRepository;
    private final ComDocumentRepository documentRepository;
    private final StlSettlementTypeRepository settlementTypeRepository;
    private final StlSettlementRepository settlementRepository;
    private final StlSettlementLineRepository settlementLineRepository;
    private final CfgTierRepository tierRepository;
    private final ComSettlementCategoryRepository settlementCategoryRepository;

    public EcheancierService(ComFolderRepository folderRepository, ComDocumentRepository documentRepository,
                              StlSettlementTypeRepository settlementTypeRepository,
                              StlSettlementRepository settlementRepository,
                              StlSettlementLineRepository settlementLineRepository,
                              CfgTierRepository tierRepository,
                              ComSettlementCategoryRepository settlementCategoryRepository) {
        this.folderRepository = folderRepository;
        this.documentRepository = documentRepository;
        this.settlementTypeRepository = settlementTypeRepository;
        this.settlementRepository = settlementRepository;
        this.settlementLineRepository = settlementLineRepository;
        this.tierRepository = tierRepository;
        this.settlementCategoryRepository = settlementCategoryRepository;
    }

    private ComDocument findEcheanceOf(UUID dossierId, UUID echeanceId) {
        return documentRepository.findById(echeanceId)
                .filter(d -> d.getComFolder() != null && dossierId.equals(d.getComFolder().getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Échéance introuvable pour ce dossier: " + echeanceId));
    }

    /** {@code POST /api/dossiers/{id}/echeancier/{echeanceId}/reglements}. */
    @Transactional
    public ReglementDto addReglement(UUID dossierId, UUID echeanceId, CreerReglementRequest request) {
        if (!folderRepository.existsById(dossierId)) {
            throw new ResourceNotFoundException("Dossier introuvable: " + dossierId);
        }
        ComDocument echeance = findEcheanceOf(dossierId, echeanceId);

        if (request.montant() == null) {
            throw new InvalidRequestException("montant est requis");
        }

        StlSettlement settlement = new StlSettlement();
        settlement.setId(UUID.randomUUID());
        settlement.setDocDate(LocalDateTime.now());
        settlement.setSettlementDate(request.dateReglement());
        settlement.setAmount(request.montant());
        settlement.setNotes(request.notes());
        if (request.typeId() != null) {
            StlSettlementType type = settlementTypeRepository.findById(request.typeId())
                    .orElseThrow(() -> new InvalidRequestException("StlSettlementType introuvable: " + request.typeId()));
            settlement.setStlSettlementType(type);
        }
        if (request.banqueId() != null) {
            CfgTier banque = tierRepository.findById(request.banqueId())
                    .orElseThrow(() -> new InvalidRequestException("Banque (CfgTier) introuvable: " + request.banqueId()));
            settlement.setCfgBank(banque);
        }
        settlement = settlementRepository.save(settlement);

        StlSettlementLine line = new StlSettlementLine();
        line.setId(UUID.randomUUID());
        line.setStlSettlement(settlement);
        line.setComDocument(echeance);
        line.setAmount(request.montant());
        settlementLineRepository.save(line);

        return toReglementDto(settlement, echeanceId);
    }

    private static ReglementDto toReglementDto(StlSettlement s, UUID echeanceId) {
        return new ReglementDto(
                s.getId(),
                echeanceId,
                s.getDocDate(),
                s.getSettlementDate(),
                s.getAmount(),
                s.getStlSettlementType() != null ? s.getStlSettlementType().getCode() : null,
                s.getNotes()
        );
    }

    public Page<EcheanceDto> findEcheances(UUID dossierId, Pageable pageable) {
        if (!folderRepository.existsById(dossierId)) {
            throw new ResourceNotFoundException("Dossier introuvable: " + dossierId);
        }
        return documentRepository.findByComFolderIdOrderByDocDateDesc(dossierId, pageable).map(this::toDto);
    }

    @Transactional
    public EcheanceDto addEcheance(UUID dossierId, CreerEcheanceRequest request) {
        ComFolder folder = folderRepository.findById(dossierId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable: " + dossierId));

        ComDocument document = new ComDocument();
        document.setId(UUID.randomUUID());
        document.setComFolder(folder);
        applyFields(document, request.dateEcheance(), request.montant(), request.description(),
                request.categorieId(), request.exclureDuPrix(), request.notes());

        return toDto(documentRepository.save(document));
    }

    private EcheanceDto toDto(ComDocument d) {
        return buildDto(d, settlementLineRepository.sumAmountByComDocumentId(d.getId()));
    }

    @Transactional
    public EcheanceDto updateEcheance(UUID dossierId, UUID echeanceId, ModifierEcheanceRequest request) {
        if (!folderRepository.existsById(dossierId)) {
            throw new ResourceNotFoundException("Dossier introuvable: " + dossierId);
        }
        ComDocument document = documentRepository.findById(echeanceId)
                .filter(d -> d.getComFolder() != null && dossierId.equals(d.getComFolder().getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Échéance introuvable pour ce dossier: " + echeanceId));

        applyFields(document, request.dateEcheance(), request.montant(), request.description(),
                request.categorieId(), request.exclureDuPrix(), request.notes());

        return toDto(documentRepository.save(document));
    }

    private void applyFields(ComDocument document, java.time.LocalDateTime dateEcheance, java.math.BigDecimal montant,
                              String description, UUID categorieId, Boolean exclureDuPrix, String notes) {
        if (dateEcheance != null) document.setDocDate(dateEcheance);
        if (montant != null) document.setAmount(montant);
        if (description != null) document.setDescription(description);
        if (exclureDuPrix != null) document.setExcludeFromPrice(exclureDuPrix);
        if (notes != null) document.setNotes(notes);
        if (categorieId != null) {
            ComSettlementCategory categorie = settlementCategoryRepository.findById(categorieId)
                    .orElseThrow(() -> new InvalidRequestException("ComSettlementCategory introuvable: " + categorieId));
            document.setComSettlementCategory(categorie);
        }
    }

    /**
     * Builds the DTO given an already-known règlement total, so callers computing totals in
     * bulk (e.g. {@link DossierService#montants}) don't issue one query per échéance.
     */
    static EcheanceDto buildDto(ComDocument d, java.math.BigDecimal montantRegle) {
        java.math.BigDecimal montant = d.getAmount() != null ? d.getAmount() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal regle = montantRegle != null ? montantRegle : java.math.BigDecimal.ZERO;
        java.math.BigDecimal reste = montant.subtract(regle);

        String statutCalcule;
        if (reste.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            statutCalcule = "PAYE";
        } else if (regle.compareTo(java.math.BigDecimal.ZERO) > 0) {
            statutCalcule = "PARTIEL";
        } else if (d.getDocDate() != null && d.getDocDate().isBefore(LocalDateTime.now())) {
            statutCalcule = "EN_RETARD";
        } else {
            statutCalcule = "A_VENIR";
        }

        return new EcheanceDto(
                d.getId(),
                d.getCode(),
                d.getDocDate(),
                d.getAmount(),
                d.getAmountHT(),
                d.getAmountTTC(),
                d.getVATRatio(),
                d.getDescription(),
                d.getComSettlementCategory() != null ? d.getComSettlementCategory().getId() : null,
                d.getComSettlementCategory() != null ? d.getComSettlementCategory().getCode() : null,
                d.getExcludeFromPrice(),
                d.getNotes(),
                regle,
                reste,
                statutCalcule
        );
    }
}

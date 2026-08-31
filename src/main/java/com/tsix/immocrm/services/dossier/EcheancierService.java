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
import org.springframework.util.StringUtils;

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

        StlSettlementType type = null;
        if (request.typeId() != null) {
            type = settlementTypeRepository.findById(request.typeId())
                    .orElseThrow(() -> new InvalidRequestException("StlSettlementType introuvable: " + request.typeId()));
        }
        // IsReferenceMandatory varie par type (confirmé sur données réelles : vrai pour
        // CHÈQUE/CHEQUE NOTAIRE/SYNDIC/BANQUE/CERTIFIÉ/EFFET, référence renseignée sur >99% des
        // lignes réelles de ces types) — enforcé ici, contrairement à IsBankMandatory qui reste
        // purement indicatif côté front (voir TypeReglementDto.banqueObligatoire).
        if (type != null && Boolean.TRUE.equals(type.getIsReferenceMandatory())
                && !StringUtils.hasText(request.reference())) {
            throw new InvalidRequestException("reference est requise pour le type de règlement " + type.getCode());
        }

        StlSettlement settlement = new StlSettlement();
        settlement.setId(UUID.randomUUID());
        settlement.setDocDate(LocalDateTime.now());
        // EcheanceDate = "date d'échéance" du règlement, renseignée sur la quasi-totalité des
        // règlements réels (voir CreerReglementRequest) — défaut à maintenant si absente, pour ne
        // pas laisser cette colonne quasi-toujours-peuplée vide sur les règlements créés via l'API.
        settlement.setEcheanceDate(request.dateEcheance() != null ? request.dateEcheance() : LocalDateTime.now());
        // SettlementDate = "date de règlement effectif" (ChampDateEncaissement.DATE_REGLEMENT) —
        // renseignée a posteriori, confirmée quasi jamais connue à la saisie (14 lignes/~3260 en
        // données réelles) : aucun défaut, laissée telle quelle (null si absente).
        settlement.setSettlementDate(request.dateReglement());
        settlement.setAmount(request.montant());
        settlement.setCode(request.reference());
        settlement.setNotes(request.notes());
        if (type != null) {
            settlement.setStlSettlementType(type);
        }
        if (request.banqueId() != null) {
            CfgTier banque = tierRepository.findById(request.banqueId())
                    .orElseThrow(() -> new InvalidRequestException("Banque (CfgTier) introuvable: " + request.banqueId()));
            settlement.setCfgBank(banque);
        }
        // TierId ("réglé par") — déjà exposé en lecture (ReglementDto.payeurId/payeurNom, résolu
        // via StlSettlement.getTier()) mais jamais posable à la création ; confirmé quasi toujours
        // renseigné en données réelles (3232/3240 lignes, 99,75%).
        if (request.payeurId() != null) {
            CfgTier payeur = tierRepository.findById(request.payeurId())
                    .orElseThrow(() -> new InvalidRequestException("Payeur (CfgTier) introuvable: " + request.payeurId()));
            settlement.setTier(payeur);
        }
        settlement = settlementRepository.save(settlement);

        StlSettlementLine line = new StlSettlementLine();
        line.setId(UUID.randomUUID());
        line.setStlSettlement(settlement);
        line.setComDocument(echeance);
        line.setAmount(request.montant());
        line = settlementLineRepository.save(line);

        return toReglementDto(line, echeanceId);
    }

    /**
     * {@code GET /api/dossiers/{id}/echeancier/{echeanceId}/reglements}: historique complet des
     * règlements d'une échéance — contrepartie en lecture du {@code POST} ci-dessus, plus récent
     * d'abord. Chaque entrée porte le détail complet (type, banque, réglé par, référence) plutôt
     * que juste le montant, pour afficher un historique exploitable côté front sans appel
     * supplémentaire par règlement.
     */
    public List<ReglementDto> findReglements(UUID dossierId, UUID echeanceId) {
        if (!folderRepository.existsById(dossierId)) {
            throw new ResourceNotFoundException("Dossier introuvable: " + dossierId);
        }
        findEcheanceOf(dossierId, echeanceId);
        return settlementLineRepository.findByComDocumentIdOrderByStlSettlement_DocDateDesc(echeanceId).stream()
                .map(line -> toReglementDto(line, echeanceId))
                .toList();
    }

    /**
     * {@code line} carries both the {@code StlSettlement} (règlement) and the specific
     * {@code StlSettlementLine} tying it to this échéance. {@code ReglementDto.id} must be the
     * {@code StlSettlement.PKey} — it's what {@code GET .../documents/recu} expects as
     * {@code reglementId} — never the line's own id, which is a different table/PK entirely
     * (was the cause of "Règlement introuvable" 404s on an id copied straight from this DTO).
     * Likewise {@code montant} must be this line's share ({@code line.getAmount()}), not the
     * settlement's total ({@code s.getAmount()}) — a règlement split across several échéances
     * (e.g. frais dossier + frais syndic) would otherwise show the full total on every line.
     */
    private static ReglementDto toReglementDto(StlSettlementLine line, UUID echeanceId) {
        StlSettlement s = line.getStlSettlement();
        StlSettlementType type = s.getStlSettlementType();
        CfgTier banque = s.getCfgBank();
        CfgTier payeur = s.getTier();
        return new ReglementDto(
                s.getId(),
                echeanceId,
                s.getCode(),
                s.getDocDate(),
                s.getEcheanceDate(),
                s.getSettlementDate(),
                line.getAmount(),
                type != null ? type.getId() : null,
                type != null ? type.getCode() : null,
                type != null ? type.getDescription() : null,
                banque != null ? banque.getId() : null,
                banque != null ? nomTier(banque) : null,
                payeur != null ? payeur.getId() : null,
                payeur != null ? nomTier(payeur) : null,
                s.getNotes()
        );
    }

    /** Même logique que {@code FicheBancaireService}/{@code FicheTropPercuService} — préfère la raison sociale/description, sinon prénom+nom. */
    private static String nomTier(CfgTier tier) {
        return com.tsix.immocrm.utils.TierNomFormatter.nom(tier);
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

package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.domain.StatutEcheance;
import com.tsix.immocrm.domain.StatutReglement;
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
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.Requis;
import com.tsix.immocrm.utils.TierNomFormatter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNullElse;

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
    private final UtilisateurAffiche utilisateurAffiche;

    public EcheancierService(ComFolderRepository folderRepository, ComDocumentRepository documentRepository,
                              StlSettlementTypeRepository settlementTypeRepository,
                              StlSettlementRepository settlementRepository,
                              StlSettlementLineRepository settlementLineRepository,
                              CfgTierRepository tierRepository,
                              ComSettlementCategoryRepository settlementCategoryRepository,
                              UtilisateurAffiche utilisateurAffiche) {
        this.folderRepository = folderRepository;
        this.documentRepository = documentRepository;
        this.settlementTypeRepository = settlementTypeRepository;
        this.settlementRepository = settlementRepository;
        this.settlementLineRepository = settlementLineRepository;
        this.tierRepository = tierRepository;
        this.settlementCategoryRepository = settlementCategoryRepository;
        this.utilisateurAffiche = utilisateurAffiche;
    }

    private ComDocument findEcheanceOf(UUID dossierId, UUID echeanceId) {
        return documentRepository.findById(echeanceId)
                .filter(d -> d.getComFolder() != null && dossierId.equals(d.getComFolder().getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Échéance introuvable pour ce dossier: " + echeanceId));
    }

    /**
     * {@code POST /api/dossiers/{id}/echeancier/{echeanceId}/reglements}.
     *
     * <p><b>Saisie complète obligatoire.</b> Un règlement incomplet produisait une « Autorisation
     * d'encaissement » trouée (Mode de versement et Versement effectué par vides) ; montant, date
     * d'échéance, type et payeur sont donc requis, la banque et la référence dès que le type les
     * impose.
     *
     * <p>Écrit aussi trois colonnes que l'API laissait vides jusqu'ici :
     * {@code Status = IN_WORK} (les 3 218 lignes legacy saisies portent cette valeur, les nôtres
     * portaient {@code NULL}), {@code IsCredit = true} (encaissement client — détermine aussi la
     * {@code Category} des pièces jointes du règlement), et {@code UserCreate}/
     * {@code UserCreateDateTime}, dont l'absence reléguait les règlements créés par l'API en bas
     * de l'historique du dossier ({@code DossierHistoriqueService} trie là-dessus, nulls en
     * dernier).
     */
    @Transactional
    public ReglementDto addReglement(UUID dossierId, UUID echeanceId, CreerReglementRequest request,
                                      AuthenticatedUser principal) {
        if (!folderRepository.existsById(dossierId)) {
            throw new ResourceNotFoundException("Dossier introuvable: " + dossierId);
        }
        ComDocument echeance = findEcheanceOf(dossierId, echeanceId);

        // dateReglement (SettlementDate) n'appartient qu'au passage à POINTED : sur 3 244 règlements
        // réels elle n'est renseignée que sur les 14 lignes pointées, sur aucune des 3 218 en cours.
        // L'accepter ici créerait la première ligne IN_WORK datée de l'historique de cette base.
        if (request.dateReglement() != null) {
            throw new InvalidRequestException("dateReglement ne peut pas être fournie à la création : "
                    + "elle se renseigne au passage à POINTED, via PATCH /api/dossiers/" + dossierId
                    + "/reglements/{reglementId}/statut");
        }
        // Aucune pièce jointe ne peut exister avant que le règlement existe, donc aucun règlement
        // ne peut naître POINTED (la preuve de paiement serait impossible à fournir).
        if (StringUtils.hasText(request.statut())
                && !StatutReglement.INITIAL.name().equalsIgnoreCase(request.statut().trim())) {
            throw new InvalidRequestException("Un règlement est toujours créé en "
                    + StatutReglement.INITIAL + " : pour le pointer, déposez d'abord la preuve de "
                    + "paiement puis appelez PATCH .../reglements/{reglementId}/statut");
        }

        BigDecimal montant = Requis.champ(request.montant(), "montant est requis");
        if (montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("montant doit être strictement positif");
        }
        LocalDateTime dateEcheance = Requis.champ(request.dateEcheance(), "dateEcheance est requise");
        UUID typeId = Requis.champ(request.typeId(), "typeId est requis");
        UUID payeurId = Requis.champ(request.payeurId(), "payeurId est requis");

        StlSettlementType type = settlementTypeRepository.findById(typeId)
                .orElseThrow(() -> new InvalidRequestException("StlSettlementType introuvable: " + typeId));

        // IsReferenceMandatory varie par type (confirmé sur données réelles : vrai pour
        // CHÈQUE/CHEQUE NOTAIRE/SYNDIC/BANQUE/CERTIFIÉ/EFFET, référence renseignée sur >99% des
        // lignes réelles de ces types).
        if (Boolean.TRUE.equals(type.getIsReferenceMandatory()) && !StringUtils.hasText(request.reference())) {
            throw new InvalidRequestException("reference est requise pour le type de règlement " + type.getCode());
        }
        // IsBankMandatory était jusqu'ici purement indicatif : annoncé au front via
        // TypeReglementDto.banqueObligatoire mais jamais appliqué côté serveur, si bien qu'un
        // chèque sans banque passait en 201. Le flag est désormais enforcé.
        if (Boolean.TRUE.equals(type.getIsBankMandatory()) && request.banqueId() == null) {
            throw new InvalidRequestException("banqueId est requise pour le type de règlement " + type.getCode());
        }

        // Reste à payer de l'échéance — même calcul que buildDto (montant de l'échéance moins la
        // somme déjà réglée). Sans ce contrôle un sur-paiement passait silencieusement et donnait
        // un reste négatif affiché avec le statut calculé "PAYE".
        BigDecimal montantEcheance = requireNonNullElse(echeance.getAmount(), BigDecimal.ZERO);
        BigDecimal dejaRegle = requireNonNullElse(
                settlementLineRepository.sumAmountByComDocumentId(echeanceId), BigDecimal.ZERO);
        BigDecimal reste = montantEcheance.subtract(dejaRegle);
        if (montant.compareTo(reste) > 0) {
            throw new InvalidRequestException("montant (" + montant + ") dépasse le reste à payer "
                    + "de cette échéance (" + reste + ")");
        }

        StlSettlement settlement = new StlSettlement();
        settlement.setId(UUID.randomUUID());
        settlement.setDocDate(LocalDateTime.now());
        settlement.setEcheanceDate(dateEcheance);
        settlement.setSettlementDate(null);
        settlement.setAmount(montant);
        settlement.setCode(request.reference());
        settlement.setNotes(request.notes());
        settlement.setStatus(StatutReglement.INITIAL.name());
        settlement.setIsCredit(Boolean.TRUE);
        settlement.setUserCreate(utilisateurAffiche.nom(principal));
        settlement.setUserCreateDateTime(LocalDateTime.now());
        settlement.setStlSettlementType(type);
        if (request.banqueId() != null) {
            CfgTier banque = tierRepository.findById(request.banqueId())
                    .orElseThrow(() -> new InvalidRequestException("Banque (CfgTier) introuvable: " + request.banqueId()));
            settlement.setCfgBank(banque);
        }
        // TierId ("réglé par") — alimente le bloc « Versement effectué par » de l'autorisation
        // d'encaissement ; confirmé renseigné sur 3232/3240 lignes réelles (99,75%).
        CfgTier payeur = tierRepository.findById(payeurId)
                .orElseThrow(() -> new InvalidRequestException("Payeur (CfgTier) introuvable: " + payeurId));
        settlement.setTier(payeur);
        settlement = settlementRepository.save(settlement);

        StlSettlementLine line = new StlSettlementLine();
        line.setId(UUID.randomUUID());
        line.setStlSettlement(settlement);
        line.setComDocument(echeance);
        line.setAmount(montant);
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
    static ReglementDto toReglementDto(StlSettlementLine line, UUID echeanceId) {
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
                Champs.de(type, StlSettlementType::getId),
                Champs.de(type, StlSettlementType::getCode),
                Champs.de(type, StlSettlementType::getDescription),
                Champs.de(banque, CfgTier::getId),
                Champs.de(banque, TierNomFormatter::nom),
                Champs.de(payeur, CfgTier::getId),
                Champs.de(payeur, TierNomFormatter::nom),
                s.getNotes(),
                s.getStatus()
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
        ComDocument document = findEcheanceOf(dossierId, echeanceId);

        applyFields(document, request.dateEcheance(), request.montant(), request.description(),
                request.categorieId(), request.exclureDuPrix(), request.notes());

        return toDto(documentRepository.save(document));
    }

    private void applyFields(ComDocument document, LocalDateTime dateEcheance, BigDecimal montant,
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
    static EcheanceDto buildDto(ComDocument d, BigDecimal montantRegle) {
        BigDecimal montant = requireNonNullElse(d.getAmount(), BigDecimal.ZERO);
        BigDecimal regle = requireNonNullElse(montantRegle, BigDecimal.ZERO);
        BigDecimal reste = montant.subtract(regle);
        StatutEcheance statutCalcule =
                StatutEcheance.calculer(montant, regle, d.getDocDate(), LocalDateTime.now());

        return new EcheanceDto(
                d.getId(),
                d.getCode(),
                d.getDocDate(),
                d.getAmount(),
                d.getAmountHT(),
                d.getAmountTTC(),
                d.getVATRatio(),
                d.getDescription(),
                Champs.de(d.getComSettlementCategory(), ComSettlementCategory::getId),
                Champs.de(d.getComSettlementCategory(), ComSettlementCategory::getCode),
                d.getExcludeFromPrice(),
                d.getNotes(),
                regle,
                reste,
                statutCalcule.name()
        );
    }
}

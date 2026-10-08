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
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNullElse;

/** Échéancier d'un dossier ({@code ComDocument}) et ses règlements. */
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
    private final Clock clock;

    public EcheancierService(ComFolderRepository folderRepository, ComDocumentRepository documentRepository,
                              StlSettlementTypeRepository settlementTypeRepository,
                              StlSettlementRepository settlementRepository,
                              StlSettlementLineRepository settlementLineRepository,
                              CfgTierRepository tierRepository,
                              ComSettlementCategoryRepository settlementCategoryRepository,
                              UtilisateurAffiche utilisateurAffiche,
                              Clock clock) {
        this.clock = clock;
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
     * <b>Saisie complète</b>, sinon l'autorisation d'encaissement sort trouée. {@code IsCredit = true}
     * détermine aussi la {@code Category} des pièces jointes ; {@code UserCreateDateTime} sert le tri
     * de l'historique. Voir {@code FEATURE-reglement-statut-preuve.md} §4.
     */
    @Transactional
    public ReglementDto addReglement(UUID dossierId, UUID echeanceId, CreerReglementRequest request,
                                      AuthenticatedUser principal) {
        if (!folderRepository.existsById(dossierId)) {
            throw new ResourceNotFoundException("Dossier introuvable: " + dossierId);
        }
        ComDocument echeance = findEcheanceOf(dossierId, echeanceId);

        // dateReglement (SettlementDate) n'appartient qu'au passage à POINTED : aucune ligne IN_WORK
        // réelle n'est datée (FEATURE-reglement-statut-preuve.md §2).
        if (request.dateReglement() != null) {
            throw new InvalidRequestException("dateReglement ne peut pas être fournie à la création : "
                    + "elle se renseigne au passage à POINTED, via PATCH /api/dossiers/" + dossierId
                    + "/reglements/{reglementId}/statut");
        }
        // Jamais créé POINTED : la preuve de paiement ne peut pas exister avant le règlement.
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

        // IsReferenceMandatory varie par type (chèques, banque, certifié, effet).
        if (Boolean.TRUE.equals(type.getIsReferenceMandatory()) && !StringUtils.hasText(request.reference())) {
            throw new InvalidRequestException("reference est requise pour le type de règlement " + type.getCode());
        }
        if (Boolean.TRUE.equals(type.getIsBankMandatory()) && request.banqueId() == null) {
            throw new InvalidRequestException("banqueId est requise pour le type de règlement " + type.getCode());
        }

        // Reste = montant de l'échéance − déjà réglé, même calcul que buildDto ; un sur-paiement
        // donnerait un reste négatif affiché « PAYE ».
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
        settlement.setDocDate(LocalDateTime.now(clock));
        settlement.setEcheanceDate(dateEcheance);
        settlement.setSettlementDate(null);
        settlement.setAmount(montant);
        settlement.setCode(request.reference());
        settlement.setNotes(request.notes());
        settlement.setStatus(StatutReglement.INITIAL.name());
        settlement.setIsCredit(Boolean.TRUE);
        settlement.setUserCreate(utilisateurAffiche.nom(principal));
        settlement.setUserCreateDateTime(LocalDateTime.now(clock));
        settlement.setStlSettlementType(type);
        if (request.banqueId() != null) {
            CfgTier banque = tierRepository.findById(request.banqueId())
                    .orElseThrow(() -> new InvalidRequestException("Banque (CfgTier) introuvable: " + request.banqueId()));
            settlement.setCfgBank(banque);
        }
        // TierId (« réglé par ») alimente « Versement effectué par » de l'autorisation d'encaissement.
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
     * {@code id} = {@code StlSettlement.PKey} (attendu par {@code .../documents/recu}), jamais l'id de
     * la ligne. {@code montant} = la part de cette ligne, pas le total d'un règlement réparti sur
     * plusieurs échéances.
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
        // UserCreate signe l'événement ECHEANCE de l'historique (FIX-tracabilite-historique-hotline.md §1).
        document.setUserCreate(utilisateurAffiche.courant().nom());
        document.setUserCreateDateTime(LocalDateTime.now(clock));
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

    private static String videEnNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /** Total réglé fourni par l'appelant, pour ne pas payer une requête par échéance. */
    EcheanceDto buildDto(ComDocument d, BigDecimal montantRegle) {
        BigDecimal montant = requireNonNullElse(d.getAmount(), BigDecimal.ZERO);
        BigDecimal regle = requireNonNullElse(montantRegle, BigDecimal.ZERO);
        BigDecimal reste = montant.subtract(regle);
        StatutEcheance statutCalcule =
                StatutEcheance.calculer(montant, regle, d.getDocDate(), LocalDateTime.now(clock));

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
                statutCalcule.name(),
                // QuittanceCode n'est jamais NULL mais souvent "" : null pour que le client masque.
                videEnNull(d.getQuittanceCode()),
                d.getUserCreateDateTime()
        );
    }
}

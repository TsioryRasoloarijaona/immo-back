package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.domain.FormatOmran;
import com.tsix.immocrm.domain.PayeurReglement;
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
import com.tsix.immocrm.services.referentiel.ReferencesOmran;
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

    private static final String DOSSIER_INTROUVABLE = "Dossier introuvable: ";
    private static final String CATEGORIE_REQUISE = "categorieId est requise : une échéance sans catégorie est invisible d'OMRAN";
    /** Valeurs constantes d'OMRAN sur tout encaissement de vente ({@code Settlement.cs:146-150}). */
    private static final String CATEGORIE_REGLEMENT = "None";
    private static final String NON_DEFINIE = "Non définie";

    private final ComFolderRepository folderRepository;
    private final ComDocumentRepository documentRepository;
    private final StlSettlementTypeRepository settlementTypeRepository;
    private final StlSettlementRepository settlementRepository;
    private final StlSettlementLineRepository settlementLineRepository;
    private final CfgTierRepository tierRepository;
    private final ComSettlementCategoryRepository settlementCategoryRepository;
    private final UtilisateurAffiche utilisateurAffiche;
    private final ReferencesOmran references;
    private final LigneEcheancierOmran ligneEcheancier;
    private final Clock clock;

    public EcheancierService(ComFolderRepository folderRepository, ComDocumentRepository documentRepository,
                              StlSettlementTypeRepository settlementTypeRepository,
                              StlSettlementRepository settlementRepository,
                              StlSettlementLineRepository settlementLineRepository,
                              CfgTierRepository tierRepository,
                              ComSettlementCategoryRepository settlementCategoryRepository,
                              UtilisateurAffiche utilisateurAffiche,
                              ReferencesOmran references,
                              LigneEcheancierOmran ligneEcheancier,
                              Clock clock) {
        this.clock = clock;
        this.references = references;
        this.ligneEcheancier = ligneEcheancier;
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
            throw new ResourceNotFoundException(DOSSIER_INTROUVABLE + dossierId);
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

        CfgTier banque = null;
        if (request.banqueId() != null) {
            banque = tierRepository.findById(request.banqueId())
                    .orElseThrow(() -> new InvalidRequestException("Banque (CfgTier) introuvable: " + request.banqueId()));
        }
        CfgTier payeur = tierRepository.findById(payeurId)
                .orElseThrow(() -> new InvalidRequestException("Payeur (CfgTier) introuvable: " + payeurId));

        StlSettlement settlement = nouveauReglement(echeance.getComFolder(), montant, request.reference(),
                utilisateurAffiche.nom(principal));
        settlement.setEcheanceDate(dateEcheance);
        settlement.setNotes(requireNonNullElse(request.notes(), ""));
        settlement.setStlSettlementType(type);
        settlement.setCfgBank(banque);
        // TierId = client du dossier (OMRAN) ; le payeur saisi va dans TierPayerId, que lisent le reçu
        // et l'autorisation d'encaissement (PayeurReglement).
        settlement.setTierPayer(payeur);
        if (settlement.getTier() == null) {
            settlement.setTier(payeur);
        }
        settlement.setDescription(descriptionReglement(type, banque, settlement.getCode(), settlement.getDocDate()));
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
     * Encaissement IN_WORK avec toutes les colonnes qu'OMRAN écrit à l'insertion
     * ({@code FEATURE-interop-omran-ecritures.md} §5) : sans module, entité, devise ou société,
     * {@code StlSettlementView} l'ignore.
     */
    private StlSettlement nouveauReglement(ComFolder folder, BigDecimal montant, String reference, String auteur) {
        LocalDateTime maintenant = LocalDateTime.now(clock);
        UUID devise = references.deviseParDefaut();
        StlSettlement settlement = new StlSettlement();
        settlement.setId(UUID.randomUUID());
        settlement.setDocDate(LigneEcheancierOmran.minuit(maintenant));
        settlement.setSettlementDate(null);
        settlement.setAmount(montant);
        settlement.setAmountBase(montant);
        settlement.setAmountBaseConverted(montant);
        settlement.setConvertValue(BigDecimal.ONE);
        settlement.setConvertUnit(1);
        settlement.setAmountRS(BigDecimal.ZERO);
        settlement.setRsRatio(BigDecimal.ZERO);
        settlement.setAmountStamp(BigDecimal.ZERO);
        settlement.setRSAmountSettled(Boolean.FALSE);
        settlement.setIsAccounted(Boolean.FALSE);
        settlement.setIsDcoumentDeliveredToCenter(Boolean.FALSE);
        settlement.setCode(requireNonNullElse(reference, ""));
        settlement.setStatus(StatutReglement.INITIAL.name());
        settlement.setIsCredit(Boolean.TRUE);
        settlement.setStrEntityId(references.entiteReglement(true));
        settlement.setAdmModuleId(references.moduleVente());
        settlement.setStlCurrencyId(devise);
        settlement.setStlCurrencyAffectationId(devise);
        settlement.setCategory(CATEGORIE_REGLEMENT);
        settlement.setItemNumber(NON_DEFINIE);
        settlement.setItemDescription(NON_DEFINIE);
        settlement.setCfgCompany(LigneEcheancierOmran.societe(folder));
        settlement.setTier(folder.getCfgCustomer());
        settlement.setSlipCode("");
        settlement.setRSSheetCode("");
        settlement.setReceiptCode("");
        settlement.setPropositionSheetCode("");
        settlement.setUserCreate(auteur);
        settlement.setUserCreateDateTime(maintenant);
        settlement.setUserUpdate(auteur);
        settlement.setUserUpdateDateTime(maintenant);
        return settlement;
    }

    /** {@code "<type> [<code banque>] [N° <référence>] du dd/MM/yyyy"}, getter de {@code Yes.Stl.Commun/Settlement.cs:795}. */
    static String descriptionReglement(StlSettlementType type, CfgTier banque, String reference, LocalDateTime date) {
        StringBuilder texte = new StringBuilder(requireNonNullElse(Champs.de(type, StlSettlementType::getDescription), ""));
        if (banque != null) {
            texte.append(' ').append(requireNonNullElse(banque.getCode(), ""));
        }
        if (reference != null && !reference.isEmpty()) {
            texte.append(" N° ").append(reference);
        }
        return texte.append(" du ").append(FormatOmran.dateCourte(date)).toString();
    }

    public List<ReglementDto> findReglements(UUID dossierId, UUID echeanceId) {
        if (!folderRepository.existsById(dossierId)) {
            throw new ResourceNotFoundException(DOSSIER_INTROUVABLE + dossierId);
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
        CfgTier payeur = PayeurReglement.de(s);
        return new ReglementDto(
                s.getId(),
                echeanceId,
                Champs.videEnNull(s.getCode()),
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
            throw new ResourceNotFoundException(DOSSIER_INTROUVABLE + dossierId);
        }
        return documentRepository.findByComFolderIdOrderByDocDateDescIdAsc(dossierId, pageable).map(this::toDto);
    }

    @Transactional
    public EcheanceDto addEcheance(UUID dossierId, CreerEcheanceRequest request) {
        ComFolder folder = folderRepository.findById(dossierId)
                .orElseThrow(() -> new ResourceNotFoundException(DOSSIER_INTROUVABLE + dossierId));
        Requis.champ(request.categorieId(), CATEGORIE_REQUISE);

        ComDocument document = new ComDocument();
        document.setId(UUID.randomUUID());
        document.setComFolder(folder);
        document.setDocDate(LocalDateTime.now(clock));
        applyFields(document, request.dateEcheance(), request.montant(), request.description(),
                request.categorieId(), request.exclureDuPrix(), request.notes());
        if (document.getDescription() == null) {
            document.setDescription(document.getComSettlementCategory().getDescription());
        }
        // UserCreate signe l'événement ECHEANCE de l'historique (FIX-tracabilite-historique-hotline.md §1).
        ligneEcheancier.completerCreation(document);

        return toDto(documentRepository.save(document));
    }

    private EcheanceDto toDto(ComDocument d) {
        return buildDto(d, settlementLineRepository.sumAmountByComDocumentId(d.getId()));
    }

    @Transactional
    public EcheanceDto updateEcheance(UUID dossierId, UUID echeanceId, ModifierEcheanceRequest request) {
        if (!folderRepository.existsById(dossierId)) {
            throw new ResourceNotFoundException(DOSSIER_INTROUVABLE + dossierId);
        }
        ComDocument document = findEcheanceOf(dossierId, echeanceId);

        applyFields(document, request.dateEcheance(), request.montant(), request.description(),
                request.categorieId(), request.exclureDuPrix(), request.notes());
        // Une ligne ancienne sans catégorie ne reste pas sans : la modification l'exige.
        Requis.champ(document.getComSettlementCategory(), CATEGORIE_REQUISE);
        ligneEcheancier.completerModification(document);

        return toDto(documentRepository.save(document));
    }

    private void applyFields(ComDocument document, LocalDateTime dateEcheance, BigDecimal montant,
                              String description, UUID categorieId, Boolean exclureDuPrix, String notes) {
        if (dateEcheance != null) document.setDocDate(dateEcheance);
        if (montant != null) document.setAmount(montant);
        if (description != null) document.setDescription(description);
        if (notes != null) document.setNotes(notes);
        if (categorieId != null) {
            ComSettlementCategory categorie = settlementCategoryRepository.findById(categorieId)
                    .orElseThrow(() -> new InvalidRequestException("ComSettlementCategory introuvable: " + categorieId));
            document.setComSettlementCategory(categorie);
            // Comme OMRAN (ComDocument.cs:1146) : la catégorie impose son ExcludeFromPrice à chaque changement.
            document.setExcludeFromPrice(Boolean.TRUE.equals(categorie.getExcludeFromPrice()));
        }
        if (exclureDuPrix != null) document.setExcludeFromPrice(exclureDuPrix);
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
                // Code et référence : "" en base (convention OMRAN), null dans l'API.
                Champs.videEnNull(d.getCode()),
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
                Champs.videEnNull(d.getQuittanceCode()),
                d.getUserCreateDateTime()
        );
    }
}

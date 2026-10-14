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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNullElse;

/** Échéancier d'un dossier ({@code ComDocument}) et ses règlements. */
@Service
@Transactional(readOnly = true)
public class EcheancierService {

    private static final String DOSSIER_INTROUVABLE = "Dossier introuvable: ";
    static final String CATEGORIE_REQUISE = "categorieId est requis : une échéance a toujours une catégorie";

    private final ComFolderRepository folderRepository;
    private final ComDocumentRepository documentRepository;
    private final StlSettlementTypeRepository settlementTypeRepository;
    private final StlSettlementRepository settlementRepository;
    private final StlSettlementLineRepository settlementLineRepository;
    private final CfgTierRepository tierRepository;
    private final ComSettlementCategoryRepository settlementCategoryRepository;
    private final UtilisateurAffiche utilisateurAffiche;
    private final LigneEcheancierOmran ligneOmran;
    private final ReglementOmran reglementOmran;
    private final Clock clock;
    private final DefautsEcheance defauts;
    private final ReequilibrageSchema reequilibrage;
    private final TotauxDossierEcheancier totaux;
    private final PenaliteRetardService penalites;

    public EcheancierService(ComFolderRepository folderRepository, ComDocumentRepository documentRepository,
                              StlSettlementTypeRepository settlementTypeRepository,
                              StlSettlementRepository settlementRepository,
                              StlSettlementLineRepository settlementLineRepository,
                              CfgTierRepository tierRepository,
                              ComSettlementCategoryRepository settlementCategoryRepository,
                              UtilisateurAffiche utilisateurAffiche,
                              LigneEcheancierOmran ligneOmran, ReglementOmran reglementOmran,
                              Clock clock,
                              EcrituresSchema ecrituresSchema) {
        this.defauts = ecrituresSchema.defauts();
        this.reequilibrage = ecrituresSchema.reequilibrage();
        this.totaux = ecrituresSchema.totaux();
        this.penalites = ecrituresSchema.penalites();
        this.clock = clock;
        this.ligneOmran = ligneOmran;
        this.reglementOmran = reglementOmran;
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
        ComFolder folder = folderRepository.findById(dossierId)
                .orElseThrow(() -> new ResourceNotFoundException(DOSSIER_INTROUVABLE + dossierId));
        ComDocument echeance = findEcheanceOf(dossierId, echeanceId);
        validerEtatInitial(request, dossierId);

        BigDecimal montant = Requis.champ(request.montant(), "montant est requis");
        if (montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("montant doit être strictement positif");
        }
        LocalDateTime dateEcheance = Requis.champ(request.dateEcheance(), "dateEcheance est requise");
        UUID typeId = Requis.champ(request.typeId(), "typeId est requis");
        if (request.payeurId() == null && folder.getCfgCustomer() == null) {
            throw new InvalidRequestException("payeurId est requis : le dossier n'a pas de client");
        }

        StlSettlementType type = settlementTypeRepository.findById(typeId)
                .orElseThrow(() -> new InvalidRequestException("StlSettlementType introuvable: " + typeId));
        validerSaisieDuType(type, request);
        validerReste(echeance, montant);
        // Avant l'enregistrement : la pénalité se calcule sur le reste d'avant ce règlement (FEATURE-impayes.md P3).
        penalites.genererPenalite(echeance);

        StlSettlement settlement = new StlSettlement();
        settlement.setId(UUID.randomUUID());
        settlement.setDocDate(LocalDate.now(clock).atStartOfDay());
        settlement.setEcheanceDate(dateEcheance);
        settlement.setSettlementDate(null);
        settlement.setAmount(montant);
        settlement.setCode(requireNonNullElse(request.reference(), ""));
        settlement.setNotes(requireNonNullElse(request.notes(), ""));
        settlement.setStatus(StatutReglement.INITIAL.name());
        settlement.setIsCredit(Boolean.TRUE);
        String auteur = utilisateurAffiche.nom(principal);
        LocalDateTime maintenant = LocalDateTime.now(clock);
        settlement.setUserCreate(auteur);
        settlement.setUserCreateDateTime(maintenant);
        settlement.setUserUpdate(auteur);
        settlement.setUserUpdateDateTime(maintenant);
        settlement.setStlSettlementType(type);
        if (request.banqueId() != null) {
            settlement.setCfgBank(tierRepository.findById(request.banqueId())
                    .orElseThrow(() -> new InvalidRequestException("Banque (CfgTier) introuvable: " + request.banqueId())));
        }
        // Comme OMRAN : TierId = client du dossier, TierPayerId = qui a payé (à défaut le client).
        CfgTier payeur = payeurSaisi(request.payeurId(), folder);
        settlement.setTier(folder.getCfgCustomer() != null ? folder.getCfgCustomer() : payeur);
        settlement.setTierPayer(payeur);
        reglementOmran.completer(settlement, folder);
        settlement = settlementRepository.save(settlement);

        StlSettlementLine line = new StlSettlementLine();
        line.setId(UUID.randomUUID());
        line.setStlSettlement(settlement);
        line.setComDocument(echeance);
        line.setAmount(montant);
        line = settlementLineRepository.save(line);

        return toReglementDto(line, echeanceId);
    }

    /** Un règlement naît IN_WORK, sans date de règlement : elle n'appartient qu'au passage à POINTED. */
    private static void validerEtatInitial(CreerReglementRequest request, UUID dossierId) {
        // FEATURE-reglement-statut-preuve.md §2 : aucune ligne IN_WORK réelle n'est datée.
        if (request.dateReglement() != null) {
            throw new InvalidRequestException("dateReglement ne peut pas être fournie à la création : "
                    + "elle se renseigne au passage à POINTED, via PATCH /api/dossiers/" + dossierId
                    + "/reglements/{reglementId}/statut");
        }
        if (StringUtils.hasText(request.statut())
                && !StatutReglement.INITIAL.name().equalsIgnoreCase(request.statut().trim())) {
            throw new InvalidRequestException("Un règlement est toujours créé en "
                    + StatutReglement.INITIAL + " : pour le pointer, déposez d'abord la preuve de "
                    + "paiement puis appelez PATCH .../reglements/{reglementId}/statut");
        }
    }

    /** IsReferenceMandatory / IsBankMandatory varient par type (chèques, banque, certifié, effet). */
    private static void validerSaisieDuType(StlSettlementType type, CreerReglementRequest request) {
        if (Boolean.TRUE.equals(type.getIsReferenceMandatory()) && !StringUtils.hasText(request.reference())) {
            throw new InvalidRequestException("reference est requise pour le type de règlement " + type.getCode());
        }
        if (Boolean.TRUE.equals(type.getIsBankMandatory()) && request.banqueId() == null) {
            throw new InvalidRequestException("banqueId est requise pour le type de règlement " + type.getCode());
        }
    }

    /** Reste = montant − déjà réglé (signé), même calcul que {@link #buildDto} ; pas de sur-paiement. */
    private void validerReste(ComDocument echeance, BigDecimal montant) {
        BigDecimal montantEcheance = requireNonNullElse(echeance.getAmount(), BigDecimal.ZERO);
        BigDecimal dejaRegle = requireNonNullElse(
                settlementLineRepository.sumAmountByComDocumentId(echeance.getId()), BigDecimal.ZERO);
        BigDecimal reste = montantEcheance.subtract(dejaRegle);
        if (montant.compareTo(reste) > 0) {
            throw new InvalidRequestException("montant (" + montant + ") dépasse le reste à payer "
                    + "de cette échéance (" + reste + ")");
        }
    }

    private CfgTier payeurSaisi(UUID payeurId, ComFolder folder) {
        if (payeurId == null) {
            return folder.getCfgCustomer();
        }
        return tierRepository.findById(payeurId)
                .orElseThrow(() -> new InvalidRequestException("Payeur (CfgTier) introuvable: " + payeurId));
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
            throw new ResourceNotFoundException(DOSSIER_INTROUVABLE + dossierId);
        }
        return documentRepository.findByComFolderIdOrderByDocDateDesc(dossierId, pageable).map(this::toDto);
    }

    /**
     * Catégorie obligatoire : OMRAN n'écrit jamais d'échéance sans elle (société, TVA, exclusion du prix). Montant et
     * date absents prennent les défauts de la catégorie ; l'écart du schéma est ensuite reporté (FEATURE-vente-echeancier.md §1).
     */
    @Transactional
    public EcheanceDto addEcheance(UUID dossierId, CreerEcheanceRequest request) {
        ComFolder folder = folderRepository.findById(dossierId)
                .orElseThrow(() -> new ResourceNotFoundException(DOSSIER_INTROUVABLE + dossierId));
        Requis.champ(request.categorieId(), CATEGORIE_REQUISE);

        ComDocument document = new ComDocument();
        document.setId(UUID.randomUUID());
        document.setComFolder(folder);
        // UserCreate signe l'événement ECHEANCE de l'historique (FIX-tracabilite-historique-hotline.md §1).
        ligneOmran.signerCreation(document);
        applyFields(document, new ChampsEcheance(request.dateEcheance(), request.montant(), request.description(),
                request.categorieId(), request.exclureDuPrix(), request.notes()));
        ComSettlementCategory categorie = document.getComSettlementCategory();
        if (document.getDescription() == null) {
            document.setDescription(categorie.getDescription());
        }
        if (document.getAmount() == null) {
            document.setAmount(defauts.montant(categorie, folder).orElseThrow(
                    () -> new InvalidRequestException("montant est requis : la catégorie n'a pas de montant par défaut")));
        }
        if (document.getDocDate() == null) {
            document.setDocDate(defauts.date(categorie, folder));
        }
        defauts.verifierDate(document, folder, document.getAmount());
        ligneOmran.completer(document, folder);

        ComDocument enregistree = documentRepository.save(document);
        reequilibrage.reporter(folder, enregistree);
        totaux.rafraichir(folder);
        return toDtoApresEcriture(enregistree, folder);
    }

    private EcheanceDto toDto(ComDocument d) {
        return buildDto(d, settlementLineRepository.sumAmountByComDocumentId(d.getId()));
    }

    @Transactional
    public EcheanceDto updateEcheance(UUID dossierId, UUID echeanceId, ModifierEcheanceRequest request) {
        ComFolder folder = folderRepository.findById(dossierId)
                .orElseThrow(() -> new ResourceNotFoundException(DOSSIER_INTROUVABLE + dossierId));
        ComDocument document = findEcheanceOf(dossierId, echeanceId);
        UUID categorieAvant = Champs.de(document.getComSettlementCategory(), ComSettlementCategory::getId);

        applyFields(document, new ChampsEcheance(request.dateEcheance(), request.montant(), request.description(),
                request.categorieId(), request.exclureDuPrix(), request.notes()));
        if (document.getComSettlementCategory() == null) {
            throw new InvalidRequestException(CATEGORIE_REQUISE);
        }
        if (!document.getComSettlementCategory().getId().equals(categorieAvant)) {
            // SetDefaluts : la remise ancien client suit la nouvelle catégorie.
            document.setDiscountRatio(null);
        }
        BigDecimal regle = requireNonNullElse(settlementLineRepository.sumAmountByComDocumentId(echeanceId),
                BigDecimal.ZERO);
        defauts.verifierDate(document, folder, requireNonNullElse(document.getAmount(), BigDecimal.ZERO).subtract(regle));
        ligneOmran.completer(document, folder);
        ligneOmran.signerModification(document);

        ComDocument enregistree = documentRepository.save(document);
        reequilibrage.reporter(folder, enregistree);
        totaux.rafraichir(folder);
        return toDtoApresEcriture(enregistree, folder);
    }

    private record ChampsEcheance(LocalDateTime dateEcheance, BigDecimal montant, String description,
                                  UUID categorieId, Boolean exclureDuPrix, String notes) {
    }

    private void applyFields(ComDocument document, ChampsEcheance champs) {
        if (champs.dateEcheance() != null) document.setDocDate(champs.dateEcheance());
        if (champs.montant() != null) document.setAmount(champs.montant());
        if (champs.description() != null) document.setDescription(champs.description());
        if (champs.exclureDuPrix() != null) document.setExcludeFromPrice(champs.exclureDuPrix());
        if (champs.notes() != null) document.setNotes(champs.notes());
        if (champs.categorieId() != null) {
            ComSettlementCategory categorie = settlementCategoryRepository.findById(champs.categorieId())
                    .orElseThrow(() -> new InvalidRequestException(
                            "ComSettlementCategory introuvable: " + champs.categorieId()));
            document.setComSettlementCategory(categorie);
        }
    }

    private static String videEnNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /** Après une écriture : la ligne et l'écart du schéma qu'elle laisse (OMRAN ne force pas l'équilibre). */
    private EcheanceDto toDtoApresEcriture(ComDocument d, ComFolder folder) {
        BigDecimal ecart = ecartSchema(folder);
        EcheanceDto dto = buildDto(d, settlementLineRepository.sumAmountByComDocumentId(d.getId()));
        return avecEcart(dto, ecart);
    }

    BigDecimal ecartSchema(ComFolder folder) {
        documentRepository.flush();
        return SchemaEcheancier.ecart(folder.getPriceAfterDiscount(),
                documentRepository.findByComFolderIdOrderByDocDateAsc(folder.getId()));
    }

    static EcheanceDto avecEcart(EcheanceDto d, BigDecimal ecart) {
        return new EcheanceDto(d.id(), d.code(), d.dateEcheance(), d.montant(), d.montantHT(), d.montantTTC(),
                d.tauxTVA(), d.description(), d.categorieId(), d.categorie(), d.exclureDuPrix(), d.notes(),
                d.montantRegle(), d.reste(), d.statutCalcule(), d.decompte(), d.creeLe(), ecart);
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
                d.getUserCreateDateTime(),
                null
        );
    }
}

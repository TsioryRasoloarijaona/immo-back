package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.domain.MontantRestant;
import com.tsix.immocrm.domain.StatutEcheance;
import com.tsix.immocrm.dto.dossier.AffectationReglementRequest;
import com.tsix.immocrm.dto.dossier.CreerEcheanceRequest;
import com.tsix.immocrm.dto.dossier.CreerReglementRequest;
import com.tsix.immocrm.dto.dossier.EcheanceDto;
import com.tsix.immocrm.dto.dossier.ModifierEcheanceRequest;
import com.tsix.immocrm.dto.dossier.ReglementDto;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.entities.stl.StlSettlementType;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.Requis;
import com.tsix.immocrm.utils.TierNomFormatter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    static final String CATEGORIE_REQUISE = "categorieId est requis : une échéance a toujours une catégorie";

    private final ComFolderRepository folderRepository;
    private final ComDocumentRepository documentRepository;
    private final StlSettlementLineRepository settlementLineRepository;
    private final ComSettlementCategoryRepository settlementCategoryRepository;
    private final LigneEcheancierOmran ligneOmran;
    private final EcritureReglement ecritureReglement;
    private final Clock clock;
    private final DefautsEcheance defauts;
    private final VerificationSchemaEcriture verification;
    private final ReequilibrageSchema reequilibrage;
    private final TotauxDossierEcheancier totaux;
    private final AlertesImpayes alertes;

    public EcheancierService(ComFolderRepository folderRepository, ComDocumentRepository documentRepository,
                              StlSettlementLineRepository settlementLineRepository,
                              ComSettlementCategoryRepository settlementCategoryRepository,
                              LigneEcheancierOmran ligneOmran, EcritureReglement ecritureReglement,
                              Clock clock,
                              EcrituresSchema ecrituresSchema) {
        this.defauts = ecrituresSchema.defauts();
        this.verification = ecrituresSchema.verification();
        this.reequilibrage = ecrituresSchema.reequilibrage();
        this.totaux = ecrituresSchema.totaux();
        this.alertes = ecrituresSchema.alertes();
        this.clock = clock;
        this.ligneOmran = ligneOmran;
        this.ecritureReglement = ecritureReglement;
        this.folderRepository = folderRepository;
        this.documentRepository = documentRepository;
        this.settlementLineRepository = settlementLineRepository;
        this.settlementCategoryRepository = settlementCategoryRepository;
    }

    private ComDocument findEcheanceOf(UUID dossierId, UUID echeanceId) {
        return documentRepository.findById(echeanceId)
                .filter(d -> d.getComFolder() != null && dossierId.equals(d.getComFolder().getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Échéance introuvable pour ce dossier: " + echeanceId));
    }

    /**
     * Une seule affectation {@code {echeanceId, montant}} du règlement multi-échéances : mêmes contrôles,
     * même écriture (FEATURE-reglement-multi-echeances.md §2.1). Échéance hors dossier = 404 ici, l'id venant
     * du chemin.
     */
    @Transactional
    public ReglementDto addReglement(UUID dossierId, UUID echeanceId, CreerReglementRequest request,
                                      AuthenticatedUser principal) {
        ComFolder folder = folderRepository.findById(dossierId)
                .orElseThrow(() -> new ResourceNotFoundException(DOSSIER_INTROUVABLE + dossierId));
        findEcheanceOf(dossierId, echeanceId);
        List<AffectationReglementRequest> uneSeule =
                List.of(new AffectationReglementRequest(echeanceId, request.montant()));
        StlSettlementLine line = ecritureReglement.creer(folder, request, uneSeule, principal).lignes().getFirst();
        return toReglementDto(line, echeanceId);
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
                s.getStatus(),
                Champs.de(s.getCfgCompany(), CfgCompany::getId)
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
        verification.avantAjout(folder, document);
        ligneOmran.completer(document, folder);

        ComDocument enregistree = documentRepository.save(document);
        reequilibrage.reporter(folder, enregistree);
        alertes.synchroniser(enregistree);
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
        verification.avantModification(folder, document,
                request.montant() != null ? request.montant() : document.getAmount(),
                request.categorieId() != null ? categorie(request.categorieId()) : document.getComSettlementCategory());

        applyFields(document, new ChampsEcheance(request.dateEcheance(), request.montant(), request.description(),
                request.categorieId(), request.exclureDuPrix(), request.notes()));
        if (document.getComSettlementCategory() == null) {
            throw new InvalidRequestException(CATEGORIE_REQUISE);
        }
        if (!document.getComSettlementCategory().getId().equals(categorieAvant)) {
            reprendreDefautsCategorie(document, request.exclureDuPrix());
        }
        BigDecimal regle = requireNonNullElse(settlementLineRepository.sumAmountByComDocumentId(echeanceId),
                BigDecimal.ZERO);
        defauts.verifierDate(document, folder, MontantRestant.echeance(document.getAmount(), regle));
        ligneOmran.completer(document, folder);
        ligneOmran.signerModification(document);

        ComDocument enregistree = documentRepository.save(document);
        reequilibrage.reporter(folder, enregistree);
        alertes.synchroniser(enregistree);
        totaux.rafraichir(folder);
        return toDtoApresEcriture(enregistree, folder);
    }

    /**
     * {@code SetDefaluts} au changement de catégorie : remise et exclusion du prix suivent la nouvelle catégorie (la TVA
     * est reprise par {@code completer}). Montant et date ne sont jamais recalculés (FEATURE-echeancier-modification.md B1).
     */
    private static void reprendreDefautsCategorie(ComDocument document, Boolean exclureDuPrixDemande) {
        document.setDiscountRatio(null);
        if (exclureDuPrixDemande == null) {
            document.setExcludeFromPrice(Boolean.TRUE.equals(document.getComSettlementCategory().getExcludeFromPrice()));
        }
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
            document.setComSettlementCategory(categorie(champs.categorieId()));
        }
    }

    private ComSettlementCategory categorie(UUID id) {
        return settlementCategoryRepository.findById(id)
                .orElseThrow(() -> new InvalidRequestException("ComSettlementCategory introuvable: " + id));
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
        BigDecimal reste = MontantRestant.echeance(montant, regle);
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

package com.tsix.immocrm.services.desistement;

import com.tsix.immocrm.domain.CalculDesistement;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.dto.desistement.DesistementDto;
import com.tsix.immocrm.dto.desistement.RembourserDesistementRequest;
import com.tsix.immocrm.dto.desistement.ValiderDesistementRequest;
import com.tsix.immocrm.dto.dossier.AffectationReglementRequest;
import com.tsix.immocrm.dto.dossier.ChangerStatutRequest;
import com.tsix.immocrm.entities.adm.AdmParamsGuid;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSaleWithdrawalCancellation;
import com.tsix.immocrm.entities.com.ComSaleWithdrawalReason;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.services.dossier.AffectationsReglement;
import com.tsix.immocrm.services.dossier.DossierService;
import com.tsix.immocrm.services.dossier.EcritureReglement;
import com.tsix.immocrm.services.dossier.LigneEcheancierOmran;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.Requis;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * R6 validation ({@code btnValidate_Click} + {@code Sale.cs:CancelFolder}) et R8 remboursement. Tout contrôle passe
 * avant la première écriture ; la sortie du dossier reste celle de {@code PATCH /api/dossiers/{id}/statut}.
 */
@Service
@Transactional(readOnly = true)
public class ValidationDesistement {

    static final String PARAMETRE_FRAIS = "DefaultCancellationDocumentType";
    static final String CODE_REMBOURSEMENT = "REMBOURSEMENT POUR DÉSISTEMENT";
    private static final int LONGUEUR_CAUSE = 255;

    private final DesistementService service;
    private final DesistementService.Depots depots;
    private final MontantsDesistement montants;
    private final DossierService dossiers;
    private final LigneEcheancierOmran ligneOmran;
    private final EcritureReglement ecriture;
    private final AffectationsReglement affectations;

    public ValidationDesistement(DesistementService service, DesistementService.Depots depots,
                                 MontantsDesistement montants, DossierService dossiers, LigneEcheancierOmran ligneOmran,
                                 EcritureReglement ecriture, AffectationsReglement affectations) {
        this.service = service;
        this.depots = depots;
        this.montants = montants;
        this.dossiers = dossiers;
        this.ligneOmran = ligneOmran;
        this.ecriture = ecriture;
        this.affectations = affectations;
    }

    @Transactional
    public DesistementDto valider(UUID id, ValiderDesistementRequest request) {
        ComSaleWithdrawalCancellation d = service.chargerVisible(id);
        if (d.estValide()) {
            throw new ConflictException(DesistementService.DEJA_VALIDE);
        }
        LocalDateTime dateValidation = Requis.champ(request.dateValidation(), "dateValidation est requise");
        ComFolder folder = d.getComFolder();
        CalculDesistement.Entree e = montants.lire(folder, d.getDiscount())
                .avecPenaliteStockee(d.getTransfertExpenseAmountAfterDiscount());
        BigDecimal penalite = CalculDesistement.penaliteApresRemise(e);
        BigDecimal aRembourser = CalculDesistement.aRembourser(e);
        if (penalite.signum() != 0 && request.dateLegalisation() == null) {
            throw new InvalidRequestException("dateLegalisation est requise : la pénalité après remise n'est pas nulle");
        }
        Optional<ComSettlementCategory> frais = penalite.signum() != 0 ? Optional.of(categorieFrais()) : Optional.empty();
        Optional<ComSettlementCategory> remboursement =
                aRembourser.signum() > 0 ? Optional.of(categorieRemboursement()) : Optional.empty();

        dossiers.changerStatut(folder.getId(),
                new ChangerStatutRequest(StatutDossier.DESISTEMENT.getCode(), null, cause(d), null));
        // Sale.cs:CancelFolder : la quittance des frais est le N° du désistement, pas un nouveau numéro.
        frais.ifPresent(c -> piece(d, folder, c, penalite, dateValidation, d.getCode()));
        remboursement.ifPresent(c -> piece(d, folder, c, aRembourser, dateValidation, null));
        MontantsDesistement.appliquer(d, e);
        d.setValidityDate(dateValidation);
        if (request.dateLegalisation() != null) {
            DesistementService.legaliser(d, request.dateLegalisation());
        }
        service.signer(d);
        return service.versDto(depots.desistements().save(d));
    }

    /** R8 : décaissement sur la pièce de résiliation, plafonné par le même reste que {@code resteARembourser}. */
    @Transactional
    public DesistementDto rembourser(UUID id, RembourserDesistementRequest request, AuthenticatedUser principal) {
        ComSaleWithdrawalCancellation d = service.chargerVisible(id);
        if (!d.estValide()) {
            throw new ConflictException("Désistement non validé : rien à rembourser.");
        }
        ComDocument piece = depots.pieces()
                .findFirstByComSaleWithdrawalCancellationIdAndComSettlementCategoryIsSaleResiliationTrue(id)
                .orElseThrow(() -> new ConflictException("Ce désistement n'a rien à rembourser."));
        BigDecimal montant = Requis.champ(request.montant(), "montant est requis");
        BigDecimal reste = affectations.resteDu(piece, true);
        if (montant.compareTo(reste) > 0) {
            throw new ConflictException("montant (" + montant.toPlainString() + ") dépasse le reste à rembourser ("
                    + reste.toPlainString() + ")");
        }
        ecriture.creer(d.getComFolder(), request.entete(),
                List.of(new AffectationReglementRequest(piece.getId(), montant)), principal, true);
        return service.versDto(d);
    }

    /** {@code CancelFolder} : « motif : observations » (à défaut le N° du désistement), dans la longueur de l'historique. */
    private static String cause(ComSaleWithdrawalCancellation d) {
        String texte = Stream.of(Champs.de(d.getMotif(), ComSaleWithdrawalReason::getDescription), d.getNotes())
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(" : "));
        if (texte.isEmpty()) {
            texte = "Désistement " + d.getCode();
        }
        return texte.length() > LONGUEUR_CAUSE ? texte.substring(0, LONGUEUR_CAUSE) : texte;
    }

    private void piece(ComSaleWithdrawalCancellation d, ComFolder folder, ComSettlementCategory categorie,
                       BigDecimal montant, LocalDateTime date, String quittance) {
        ComDocument generee = new ComDocument();
        generee.setQuittanceCode(quittance);
        generee.setId(UUID.randomUUID());
        generee.setComFolder(folder);
        generee.setDocDate(date);
        ligneOmran.signerCreation(generee);
        generee.setAmount(montant);
        generee.setComSettlementCategory(categorie);
        generee.setDescription(categorie.getDescription());
        generee.setComSaleWithdrawalCancellationId(d.getId());
        ligneOmran.completer(generee, folder);
        depots.pieces().save(generee);
    }

    private ComSettlementCategory categorieFrais() {
        return depots.parametres().findById(PARAMETRE_FRAIS).map(AdmParamsGuid::getCodeValue)
                .flatMap(depots.categories()::findById)
                .orElseThrow(() -> new ConflictException("Catégorie des frais de désistement introuvable : paramètre "
                        + PARAMETRE_FRAIS + " absent ou invalide."));
    }

    private ComSettlementCategory categorieRemboursement() {
        return depots.categories().findFirstByCodeAndIsSaleResiliationTrueAndIsCreditFalse(CODE_REMBOURSEMENT)
                .orElseThrow(() -> new ConflictException("Catégorie « " + CODE_REMBOURSEMENT + " » introuvable."));
    }
}

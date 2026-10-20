package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.domain.StatutReglement;
import com.tsix.immocrm.dto.dossier.PieceJointeDto;
import com.tsix.immocrm.entities.app.AppFileObject;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.services.piecejointe.PieceJointeSupport;
import com.tsix.immocrm.services.piecejointe.PorteurDepot;
import com.tsix.immocrm.services.piecejointe.SaisiePieceJointe;
import com.tsix.immocrm.storage.PorteurPieceJointe;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Preuves de paiement d'un règlement ({@code CategoryId = StlSettlement.PKey}), celles qui autorisent
 * le passage à {@code POINTED} — voir {@code FEATURE-reglement-statut-preuve.md} §2 et §5.
 */
@Service
@Transactional(readOnly = true)
public class ReglementPieceJointeService {

    // Category legacy entièrement déterminée par IsCredit ; cosmétique, la relecture filtre sur CategoryId.
    private static final String CATEGORY_CREDIT = "Yes.Stl.Credit.Settlement";
    private static final String CATEGORY_DEBIT = "Yes.Stl.Debit.Settlement";

    private final ComFolderRepository folderRepository;
    private final StlSettlementRepository settlementRepository;
    private final StlSettlementLineRepository settlementLineRepository;
    private final PieceJointeSupport support;

    public ReglementPieceJointeService(ComFolderRepository folderRepository,
                                       StlSettlementRepository settlementRepository,
                                       StlSettlementLineRepository settlementLineRepository,
                                       PieceJointeSupport support) {
        this.folderRepository = folderRepository;
        this.settlementRepository = settlementRepository;
        this.settlementLineRepository = settlementLineRepository;
        this.support = support;
    }

    /** Un règlement d'un autre dossier donne 404, pas 403, pour ne rien révéler de son existence. */
    private StlSettlement reglementDuDossier(UUID dossierId, UUID reglementId) {
        if (!folderRepository.existsById(dossierId)) {
            throw new ResourceNotFoundException("Dossier introuvable: " + dossierId);
        }
        StlSettlement settlement = settlementRepository.findById(reglementId)
                .orElseThrow(() -> new ResourceNotFoundException("Règlement introuvable: " + reglementId));
        if (settlementLineRepository.findBySettlementIdAndDossierId(reglementId, dossierId).isEmpty()) {
            throw new ResourceNotFoundException("Ce règlement n'est pas rattaché à ce dossier: " + reglementId);
        }
        return settlement;
    }

    public List<PieceJointeDto> findByReglement(UUID dossierId, UUID reglementId) {
        reglementDuDossier(dossierId, reglementId);
        return support.lister(reglementId);
    }

    public URI urlFichier(UUID dossierId, UUID reglementId, UUID pieceId) {
        reglementDuDossier(dossierId, reglementId);
        return support.urlTelechargement(reglementId, pieceId, "ce règlement");
    }

    /** {@code typeId} facultatif : aucune pièce de règlement réelle n'est typée. */
    @Transactional
    public PieceJointeDto create(UUID dossierId, UUID reglementId, MultipartFile fichier, String description,
                                  UUID typeId, AuthenticatedUser principal) {
        StlSettlement settlement = reglementDuDossier(dossierId, reglementId);
        String category = Boolean.FALSE.equals(settlement.getIsCredit()) ? CATEGORY_DEBIT : CATEGORY_CREDIT;
        return support.deposer(new PorteurDepot(reglementId, PorteurPieceJointe.REGLEMENT, settlement.getCode(), category,
                null), fichier, new SaisiePieceJointe(description, typeId, null), principal);
    }

    /**
     * 409 sur la <b>dernière</b> preuve d'un règlement {@code POINTED} seulement
     * ({@code FEATURE-suppression-pieces-jointes.md} §6). La pièce est résolue avant la règle :
     * sinon un {@code pieceId} inconnu donnerait 409 au lieu de 404.
     */
    @Transactional
    public void supprimer(UUID dossierId, UUID reglementId, UUID pieceId, AuthenticatedUser principal) {
        StlSettlement settlement = reglementDuDossier(dossierId, reglementId);
        AppFileObject objet = support.exigerFichier(reglementId, pieceId, "ce règlement");

        boolean pointe = StatutReglement.lireCode(settlement.getStatus())
                .filter(statut -> statut == StatutReglement.POINTED)
                .isPresent();
        if (pointe && support.nombreDeFichiers(reglementId) <= 1) {
            throw new ConflictException("Ce règlement est pointé : sa dernière preuve de paiement "
                    + "ne peut pas être supprimée. Déposez d'abord une autre pièce jointe, ou "
                    + "laissez celle-ci en place.");
        }

        support.marquerSupprime(objet, settlement.getCode(), principal);
    }
}

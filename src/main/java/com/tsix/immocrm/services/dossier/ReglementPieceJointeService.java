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
import com.tsix.immocrm.storage.PorteurPieceJointe;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * {@code /api/dossiers/{id}/reglements/{reglementId}/pieces-jointes[/...]} — les preuves de
 * paiement d'un règlement ({@code AdmAttachedFile}, {@code CategoryId = StlSettlement.PKey}).
 *
 * <p>Ce rattachement n'est pas une invention : <b>452 règlements réels portent déjà 460 fichiers</b>
 * (PDF 332, images 128, ~1,5 Mo en moyenne), déposés par l'application desktop — chèques scannés,
 * avis de virement, reçus. Les libellés le disent explicitement ("CHQ ELGHAZI ALI 38k ARS0641
 * GHRIZA 14-07-2026", "AVC VIR OUADIF ART133 100K 03-04-2026").
 *
 * <p>Ces pièces sont ce qui autorise le passage du règlement à {@code POINTED} — voir
 * {@link ReglementStatutService} et {@code FEATURE-reglement-statut-preuve.md} §2 (invariant 2).
 */
@Service
@Transactional(readOnly = true)
public class ReglementPieceJointeService {

    /**
     * Convention legacy du {@code Category} pour un encaissement client. La valeur est
     * <b>entièrement déterminée par {@code IsCredit}</b> en données réelles : les 368 fichiers
     * portés par un règlement {@code IsCredit = 1} valent tous "Yes.Stl.Credit.Settlement", les 92
     * portés par un {@code IsCredit = 0} valent tous "Yes.Stl.Debit.Settlement" — zéro croisement.
     * Comme pour le Dossier, c'est cosmétique : la relecture ne filtre que sur {@code CategoryId}.
     */
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

    /**
     * Vérifie que le règlement existe <b>et</b> qu'il est rattaché à ce dossier — même garde que
     * {@code AutorisationVersementService#collect} : un règlement d'un autre dossier donne 404, pas
     * 403, pour ne rien révéler de son existence.
     */
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

    /** URL présignée de téléchargement — mêmes gardes que {@link #findByReglement}. */
    public URI urlFichier(UUID dossierId, UUID reglementId, UUID pieceId) {
        reglementDuDossier(dossierId, reglementId);
        return support.urlTelechargement(reglementId, pieceId, "ce règlement");
    }

    /**
     * Dépose une preuve de paiement sur le règlement. {@code AdmAttachedFileTypeId} reste
     * volontairement facultatif et vaut {@code null} par défaut : les 460 pièces jointes de
     * règlement réelles n'en portent <b>aucune</b> — inutile d'imposer un typage que le métier
     * n'utilise pas.
     */
    @Transactional
    public PieceJointeDto create(UUID dossierId, UUID reglementId, MultipartFile fichier, String description,
                                  UUID typeId, AuthenticatedUser principal) {
        StlSettlement settlement = reglementDuDossier(dossierId, reglementId);
        String category = Boolean.FALSE.equals(settlement.getIsCredit()) ? CATEGORY_DEBIT : CATEGORY_CREDIT;
        return support.deposer(reglementId, PorteurPieceJointe.REGLEMENT, category, fichier, description,
                typeId, principal);
    }

    /**
     * Retire une preuve de paiement — suppression logique, mêmes gardes 404 que
     * {@link #findByReglement}.
     *
     * <p><b>La dernière preuve d'un règlement {@code POINTED} est protégée (409).</b> C'est la
     * pièce jointe qui a autorisé le pointage ({@link ReglementStatutService}) ; la retirer
     * laisserait un règlement pointé sans justificatif, trou d'audit indétectable après coup. La
     * garde ne mord que sur ce cas précis : une pièce parmi plusieurs, ou n'importe quelle pièce
     * d'un règlement {@code IN_WORK} ou {@code CANCELED}, se supprime normalement.
     *
     * <p>Le javadoc de {@link ReglementStatutService} déconseillait d'étendre la règle « pointé ⇒
     * preuve » à la suppression. L'exception est assumée et documentée dans
     * {@code FEATURE-suppression-pieces-jointes.md} §6 : l'argument d'origine portait sur le
     * risque rétroactif — rendre illisibles les 12 règlements pointés legacy sans pièce jointe —
     * et ce risque n'existe pas ici, puisque rien n'est vérifié en lecture. Mesuré sur
     * {@code yesRemsProd} : sur 17 règlements {@code POINTED}, <b>3</b> portent exactement une
     * pièce jointe, et ce sont les seules lignes de toute la base où ce 409 pourrait se
     * déclencher.
     *
     * <p>L'ordre des contrôles compte : la pièce est résolue <b>avant</b> l'évaluation de la
     * règle, sans quoi un {@code pieceId} inconnu sur un règlement pointé renverrait 409 au lieu
     * de 404 — et révélerait au passage l'état du règlement.
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

        support.marquerSupprime(objet, principal);
    }
}

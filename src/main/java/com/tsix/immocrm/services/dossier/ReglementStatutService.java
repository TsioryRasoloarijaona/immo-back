package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.domain.StatutReglement;
import com.tsix.immocrm.dto.dossier.ModifierStatutReglementRequest;
import com.tsix.immocrm.dto.dossier.ReglementDto;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.InvalidStatutTransitionException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import com.tsix.immocrm.services.piecejointe.PieceJointeSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * {@code PATCH /api/dossiers/{id}/reglements/{reglementId}/statut} — cycle de vie d'un règlement
 * ({@code StlSettlement.Status}).
 *
 * <p><b>La règle « pointé ⇒ preuve de paiement » est une règle métier nouvelle</b>, pas un
 * comportement legacy restauré, et il faut le savoir avant de la modifier. Les données réelles ne
 * peuvent ni la confirmer ni l'infirmer : le pointage s'est arrêté en 2022-2023 (14 lignes
 * {@code POINTED} en tout, la dernière modifiée le 31/03/2023) alors que le dépôt de preuves n'a
 * démarré que le 18/01/2022 et n'a fait que croître depuis. Sur la fenêtre où les deux pratiques
 * coexistent : <b>3 règlements pointés, dont 2 avec pièce jointe et 1 sans</b>. n=3.
 * À l'inverse, 451 règlements récents portent une preuve <i>sans</i> être pointés — les
 * utilisateurs joignent déjà le justificatif, ils ne pointent simplement plus.
 *
 * <p>Conséquence directe sur l'implémentation : la règle est vérifiée <b>au moment de la
 * transition uniquement</b>. Jamais en lecture, jamais rétroactivement — 12 des 14 règlements
 * {@code POINTED} existants n'ont aucune pièce jointe et doivent rester lisibles, et l'application
 * desktop continue d'écrire dans {@code yesRems} sans rien contrôler. Ne pas « renforcer » cette
 * règle en la déplaçant vers la lecture : ce serait casser des dossiers réels au nom d'un
 * invariant que la base n'a jamais respecté.
 *
 * <p><b>Une exception, décidée et mesurée</b> : {@code ReglementPieceJointeService#supprimer}
 * refuse (409) de supprimer la <i>dernière</i> pièce jointe d'un règlement déjà {@code POINTED}.
 * Cette phrase déconseillait auparavant aussi ce cas ; l'argument qui la fondait est le risque
 * <i>rétroactif</i>, et il ne s'y applique pas — rien n'est vérifié en lecture, aucune ligne
 * existante ne devient illisible, et la garde ne s'évalue qu'au moment d'une suppression demandée
 * par l'utilisateur. Sur {@code yesRemsProd} : 17 règlements {@code POINTED}, dont 3 portent
 * exactement une pièce jointe — les 3 seules lignes de la base où ce 409 peut se déclencher. Voir
 * {@code FEATURE-suppression-pieces-jointes.md} §6.
 */
@Service
@Transactional(readOnly = true)
public class ReglementStatutService {

    private final ComFolderRepository folderRepository;
    private final StlSettlementRepository settlementRepository;
    private final StlSettlementLineRepository settlementLineRepository;
    private final PieceJointeSupport pieceJointeSupport;

    public ReglementStatutService(ComFolderRepository folderRepository,
                                  StlSettlementRepository settlementRepository,
                                  StlSettlementLineRepository settlementLineRepository,
                                  PieceJointeSupport pieceJointeSupport) {
        this.folderRepository = folderRepository;
        this.settlementRepository = settlementRepository;
        this.settlementLineRepository = settlementLineRepository;
        this.pieceJointeSupport = pieceJointeSupport;
    }

    @Transactional
    public ReglementDto modifierStatut(UUID dossierId, UUID reglementId, ModifierStatutReglementRequest request) {
        if (!folderRepository.existsById(dossierId)) {
            throw new ResourceNotFoundException("Dossier introuvable: " + dossierId);
        }
        StlSettlement settlement = settlementRepository.findById(reglementId)
                .orElseThrow(() -> new ResourceNotFoundException("Règlement introuvable: " + reglementId));
        List<StlSettlementLine> lignes = settlementLineRepository.findBySettlementIdAndDossierId(reglementId, dossierId);
        if (lignes.isEmpty()) {
            throw new ResourceNotFoundException("Ce règlement n'est pas rattaché à ce dossier: " + reglementId);
        }

        if (request == null || !StringUtils.hasText(request.statut())) {
            throw new InvalidRequestException("statut est requis");
        }
        StatutReglement cible;
        try {
            cible = StatutReglement.depuisCode(request.statut());
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(e.getMessage());
        }

        // Une valeur legacy absente ou inconnue est traitée comme IN_WORK : c'est le statut des
        // 3 218 lignes réelles saisies, et les 9 lignes à Status NULL n'ont jamais été pointées.
        StatutReglement actuel = StatutReglement.lireCode(settlement.getStatus())
                .orElse(StatutReglement.IN_WORK);

        if (!StatutReglement.estValide(actuel, cible)) {
            throw new InvalidStatutTransitionException(
                    "Transition de statut invalide pour ce règlement: " + actuel + " → " + cible,
                    StatutReglement.transitionsAutorisees(actuel).stream()
                            .map(Enum::name)
                            .sorted()
                            .toList());
        }

        if (cible == StatutReglement.POINTED) {
            if (!pieceJointeSupport.auMoinsUnFichier(reglementId)) {
                throw new ConflictException("Une preuve de paiement (pièce jointe) est requise pour "
                        + "pointer ce règlement. Déposez-la via POST /api/dossiers/" + dossierId
                        + "/reglements/" + reglementId + "/pieces-jointes, puis réessayez.");
            }
            if (request.dateReglement() == null) {
                throw new InvalidRequestException("dateReglement est requise pour passer un règlement à POINTED");
            }
            settlement.setSettlementDate(request.dateReglement());
        }

        settlement.setStatus(cible.name());
        settlement = settlementRepository.save(settlement);

        // Le DTO porte la part affectée à *une* échéance : on renvoie la ligne de ce dossier. Un
        // règlement peut en couvrir plusieurs (cas réel, règlements éclatés) — on prend la
        // première par ordre d'id pour rester déterministe.
        StlSettlementLine ligne = lignes.stream()
                .min(Comparator.comparing(StlSettlementLine::getId))
                .orElseThrow();
        return EcheancierService.toReglementDto(ligne,
                ligne.getComDocument() != null ? ligne.getComDocument().getId() : null);
    }
}

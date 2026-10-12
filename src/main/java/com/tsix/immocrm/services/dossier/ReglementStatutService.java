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
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.services.piecejointe.PieceJointeSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * « Pointé ⇒ preuve de paiement » est une règle <b>nouvelle</b>, vérifiée à la transition seulement,
 * jamais en lecture : des règlements pointés legacy n'ont aucune pièce. Voir
 * {@code FEATURE-reglement-statut-preuve.md} §2 (et l'exception de {@code FEATURE-suppression-pieces-jointes.md} §6).
 */
@Service
@Transactional(readOnly = true)
public class ReglementStatutService {

    private final ComFolderRepository folderRepository;
    private final StlSettlementRepository settlementRepository;
    private final StlSettlementLineRepository settlementLineRepository;
    private final PieceJointeSupport pieceJointeSupport;
    private final UtilisateurAffiche utilisateurAffiche;
    private final Clock clock;

    public ReglementStatutService(ComFolderRepository folderRepository,
                                  StlSettlementRepository settlementRepository,
                                  StlSettlementLineRepository settlementLineRepository,
                                  PieceJointeSupport pieceJointeSupport,
                                  UtilisateurAffiche utilisateurAffiche,
                                  Clock clock) {
        this.utilisateurAffiche = utilisateurAffiche;
        this.clock = clock;
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

        // Absente ou inconnue ⇒ IN_WORK : les lignes legacy à Status NULL n'ont jamais été pointées.
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
        // GEN0016 : toute modification du règlement est signée.
        settlement.setUserUpdate(utilisateurAffiche.courant().nom());
        settlement.setUserUpdateDateTime(LocalDateTime.now(clock));
        settlement = settlementRepository.save(settlement);

        // Le DTO porte la part d'une seule échéance ; un règlement éclaté en couvre plusieurs :
        // la première par id, pour rester déterministe.
        StlSettlementLine ligne = lignes.stream()
                .min(Comparator.comparing(StlSettlementLine::getId))
                .orElseThrow();
        return EcheancierService.toReglementDto(ligne,
                ligne.getComDocument() != null ? ligne.getComDocument().getId() : null);
    }
}

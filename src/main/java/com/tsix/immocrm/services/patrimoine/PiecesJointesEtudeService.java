package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.dto.dossier.PieceJointeDto;
import com.tsix.immocrm.entities.stk.StkFeasibilityStudy;
import com.tsix.immocrm.repositories.stk.StkFeasibilityStudyRepository;
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

/** Pièces jointes d'une étude (FEATURE-patrimoine-faisabilite-workflow.md §4.3) : étude visible, écriture si non bloquée. */
@Service
@Transactional(readOnly = true)
public class PiecesJointesEtudeService {

    private final StkFeasibilityStudyRepository etudes;
    private final PieceJointeSupport support;

    public PiecesJointesEtudeService(StkFeasibilityStudyRepository etudes, PieceJointeSupport support) {
        this.etudes = etudes;
        this.support = support;
    }

    public List<PieceJointeDto> lister(UUID etudeId) {
        return support.lister(etudes.exigerVisible(etudeId).getId());
    }

    public URI urlFichier(UUID etudeId, UUID pieceId) {
        return support.urlTelechargement(etudes.exigerVisible(etudeId).getId(), pieceId, PorteurPieceJointe.ETUDE.libelle());
    }

    @Transactional
    public PieceJointeDto ajouterPieceJointe(UUID etudeId, MultipartFile fichier, SaisiePieceJointe saisie,
                                             AuthenticatedUser principal) {
        return support.deposer(porteur(FicheEtude.modifiable(etudes, etudeId)), fichier, saisie, principal);
    }

    @Transactional
    public void supprimerPieceJointe(UUID etudeId, UUID pieceId, AuthenticatedUser principal) {
        support.supprimer(porteur(FicheEtude.modifiable(etudes, etudeId)), pieceId, principal);
    }

    private static PorteurDepot porteur(StkFeasibilityStudy e) {
        PorteurPieceJointe type = PorteurPieceJointe.ETUDE;
        return new PorteurDepot(e.getId(), type, e.getCode(), type.categoryParDefaut(), e.getDescription());
    }
}

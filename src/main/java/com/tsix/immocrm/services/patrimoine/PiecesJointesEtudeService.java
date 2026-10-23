package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.dto.dossier.PieceJointeDto;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.services.piecejointe.SaisiePieceJointe;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PiecesJointesEtudeService {

    public List<PieceJointeDto> lister(UUID etudeId) {
        throw new UnsupportedOperationException();
    }

    public URI urlFichier(UUID etudeId, UUID pieceId) {
        throw new UnsupportedOperationException();
    }

    @Transactional
    public PieceJointeDto ajouterPieceJointe(UUID etudeId, MultipartFile fichier, SaisiePieceJointe saisie,
                                             AuthenticatedUser principal) {
        throw new UnsupportedOperationException();
    }

    @Transactional
    public void supprimerPieceJointe(UUID etudeId, UUID pieceId, AuthenticatedUser principal) {
        throw new UnsupportedOperationException();
    }
}

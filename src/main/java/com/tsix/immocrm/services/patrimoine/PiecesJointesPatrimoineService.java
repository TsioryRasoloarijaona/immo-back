package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.dto.dossier.PieceJointeDto;
import com.tsix.immocrm.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PiecesJointesPatrimoineService {

    /** Champs saisis avec un dépôt, tous facultatifs. */
    public record Saisie(String description, UUID typeId, String groupe) {
    }

    public List<PieceJointeDto> lister(String porteur, UUID id) {
        throw new UnsupportedOperationException();
    }

    public URI urlFichier(String porteur, UUID id, UUID pieceId) {
        throw new UnsupportedOperationException();
    }

    @Transactional
    public PieceJointeDto ajouterPieceJointe(String porteur, UUID id, MultipartFile fichier, Saisie saisie,
                                             AuthenticatedUser principal) {
        throw new UnsupportedOperationException();
    }

    @Transactional
    public void supprimerPieceJointe(String porteur, UUID id, UUID pieceId, AuthenticatedUser principal) {
        throw new UnsupportedOperationException();
    }

    public List<String> groupes() {
        throw new UnsupportedOperationException();
    }
}

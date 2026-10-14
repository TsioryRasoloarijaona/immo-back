package com.tsix.immocrm.services.dossier.workflow;

import com.tsix.immocrm.repositories.str.StrPieceObligatoireRepository;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pièces à joindre d'une transition ou d'un statut ({@code StrWorkFlow/StrStatus.VerifyForWorkflow}) : une pièce
 * manque si le dossier n'a aucun fichier de ce type ; une pièce « juste à cocher » se contente d'une ligne cochée
 * sans fichier ({@code IsCheckedButNotAttached}), les autres exigent un vrai fichier.
 */
@Component
public class PiecesObligatoires {

    private final StrPieceObligatoireRepository pieceRepository;

    public PiecesObligatoires(StrPieceObligatoireRepository pieceRepository) {
        this.pieceRepository = pieceRepository;
    }

    String manquantesDeTransition(UUID dossier, UUID transition) {
        return manquantes(dossier, pieceRepository.deTransition(transition));
    }

    String manquantesDuStatut(UUID dossier, UUID statut) {
        return manquantes(dossier, pieceRepository.deStatut(statut));
    }

    private String manquantes(UUID dossier, List<Object[]> requises) {
        if (requises.isEmpty()) {
            return "";
        }
        // Type → true si au moins un vrai fichier, false si seulement des lignes cochées sans fichier.
        Map<UUID, Boolean> jointes = new HashMap<>();
        for (Object[] piece : pieceRepository.piecesDuPorteur(dossier)) {
            UUID type = uuid(piece[0]);
            boolean fichier = !Boolean.TRUE.equals(piece[1]);
            jointes.merge(type, fichier, Boolean::logicalOr);
        }
        StringBuilder texte = new StringBuilder();
        for (Object[] requise : requises) {
            UUID type = (UUID) requise[0];
            String libelle = requise[1] != null ? (String) requise[1] : String.valueOf(type);
            boolean justeACocher = Boolean.TRUE.equals(requise[2]);
            Boolean jointe = jointes.get(type);
            if (jointe == null) {
                texte.append("Le document de type : ").append(libelle).append(" est obligatoire !")
                        .append(ChampsObligatoires.FIN_DE_LIGNE);
            } else if (!justeACocher && !jointe) {
                texte.append("Le document de type : ").append(libelle).append(" doit obligatoirement être joint !")
                        .append(ChampsObligatoires.FIN_DE_LIGNE);
            }
        }
        return texte.toString();
    }

    /** Le pilote SQL Server rend un {@code uniqueidentifier} natif en texte. */
    private static UUID uuid(Object valeur) {
        return valeur instanceof UUID id ? id : UUID.fromString(String.valueOf(valeur));
    }
}

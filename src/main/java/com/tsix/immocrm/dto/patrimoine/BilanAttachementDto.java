package com.tsix.immocrm.dto.patrimoine;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Résultat de l'attachement en masse pour un fichier.")
public record BilanAttachementDto(
        @Schema(description = "Nom d'origine du fichier envoyé.")
        String nomFichier,
        Resultat resultat,
        @Schema(description = "Motif lisible d'un refus ou d'un code introuvable ; null sinon.")
        String message,
        @Schema(description = "Pièce créée ou remplacée (AdmAttachedFile.PKey) ; null si rien n'a été écrit.")
        UUID pieceId
) {

    @Schema(description = "ATTACHE : nouvelle pièce ; REMPLACE : pièce du même type écrasée ; "
            + "CODE_INTROUVABLE : aucun article ; REFUSE : fichier rejeté.")
    public enum Resultat {
        ATTACHE, REMPLACE, CODE_INTROUVABLE, REFUSE
    }
}

package com.tsix.immocrm.services.piecejointe;

import com.tsix.immocrm.storage.PorteurPieceJointe;

import java.util.UUID;

/**
 * Porteur vérifié par l'appelant : {@code code} = ObjectLabel d'audit, {@code category} et {@code categoryDescription}
 * écrites sur la pièce (la relecture ne filtre que sur {@code CategoryId}).
 */
public record PorteurDepot(UUID id, PorteurPieceJointe type, String code, String category, String categoryDescription) {

    /** Pour une suppression : ni {@code Category} ni sa description ne sont réécrites. */
    public static PorteurDepot pourSuppression(UUID id, PorteurPieceJointe type, String code) {
        return new PorteurDepot(id, type, code, null, null);
    }
}

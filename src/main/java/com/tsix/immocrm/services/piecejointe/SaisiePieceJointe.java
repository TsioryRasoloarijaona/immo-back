package com.tsix.immocrm.services.piecejointe;

import java.util.UUID;

/** Champs saisis avec un dépôt, tous facultatifs ; {@code description} absente = nom du fichier. */
public record SaisiePieceJointe(String description, UUID typeId, String groupe) {
}

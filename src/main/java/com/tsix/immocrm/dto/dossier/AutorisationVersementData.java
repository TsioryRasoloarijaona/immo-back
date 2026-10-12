package com.tsix.immocrm.dto.dossier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * « Autorisation d'Encaissement » d'un règlement. Scopée au {@code StlSettlement}, pas à l'échéance comme
 * le SQL du modèle légataire : compte, mode de versement et payeur ne sont renseignés que sur le règlement.
 * {@code payeur} ({@code TierPayerId}, à défaut {@code TierId}) peut différer des {@code clients} (acquéreurs).
 */
public record AutorisationVersementData(
        String dossierCode,
        String documentCode,
        String operationLibelle,
        String articleCode,
        BigDecimal prixVenteTTC,
        String modeVersement,
        List<ClientAutorisation> clients,
        BigDecimal montantAVerser,
        LocalDateTime dateEmission,
        String rib,
        String banqueNom,
        Integer delaiRetourHeures,
        String responsableCommercialTitre,
        String responsableCommercialNom,
        String entrepriseNom,
        Payeur payeur
) {

    /** Imprimé « {titre} {nom} N° {type pièce}: {numéro} ». */
    public record ClientAutorisation(String titre, String nom, String pieceIdentite) {
    }

    /** Nom et prénom séparés : le modèle les imprime dans deux cases. */
    public record Payeur(String nom, String prenom, String pieceIdentite) {
    }
}

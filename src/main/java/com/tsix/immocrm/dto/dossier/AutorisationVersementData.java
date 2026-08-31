package com.tsix.immocrm.dto.dossier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data behind the "Autorisation d'Encaissement" document (see {@code AutorisationVersementPdfRenderer}
 * for the actual PDF rendering) — modeled on the legacy Stimulsoft template
 * {@code instructions/templates/autorisation_virement/ComSaleDocumentAutorisationVersement.yes}.
 * Scoped to one règlement ({@code StlSettlement}, {@code reglementId}) within a dossier — same
 * per-item scope as the "Reçu de règlement" ({@link RecuReglementData}), not the whole dossier.
 * <p>
 * <b>Corrected after initial delivery</b>: first version was scoped to one échéance
 * ({@code ComDocument}) instead, following the legacy template's SQL literally (its {@code MainID}
 * parameter does bind to {@code ComDocument.PKey}). That turned out wrong in practice — the fields
 * that make this document useful (compte destinataire, mode de versement, tiers payeur) are
 * confirmed on real data to live almost exclusively on {@code StlSettlement}, not {@code ComDocument}:
 * {@code StlSettlement.StlAccountId} 76%/{@code StlSettlementTypeId} 99.9%/{@code TierId} 99.7%
 * populated, versus {@code ComDocument}'s equivalents (2/958, 1/958) — a document scoped to the
 * échéance was blank on nearly every real record. Rescoped to the règlement accordingly.
 * <p>
 * {@code operationLibelle}/{@code articleCode}/{@code prixVenteTTC}/{@code montantAVerser} each
 * appear twice in the original template (once in the top banner, once in the "Cadre réservé à la
 * banque" box at the bottom) — kept as single fields here, the renderer prints them in both spots.
 * <p>
 * {@code clients} feeds both "Nom et prénom des clients" (top) and "Nom et Prénom du client"
 * (bottom box) — same {@code Customers} recordset in the legacy template (primary
 * {@code ComFolder.CfgCustomerId} + {@code ComFolderCustomer} co-acquéreurs). {@code payeur} is a
 * distinct person: the tiers who actually paid ({@code StlSettlement.TierId}, "réglé par", same
 * field already used by {@code ReglementDto.payeurId}) — shown only in "Versement effectué par" —
 * can differ from {@code clients} (e.g. a third party paying on the client's behalf), and is null
 * when that column isn't set.
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

    /** One entry of {@code clients} — "{titre} {nom} N° {type pièce}: {numéro}". */
    public record ClientAutorisation(String titre, String nom, String pieceIdentite) {
    }

    /** "Versement effectué par" — {@code StlSettlement.TierId}, kept as separate first/last name (legacy layout prints them in separate boxes). */
    public record Payeur(String nom, String prenom, String pieceIdentite) {
    }
}

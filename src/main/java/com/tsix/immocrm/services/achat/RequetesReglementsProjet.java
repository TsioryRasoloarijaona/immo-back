package com.tsix.immocrm.services.achat;

/** SQL natif des affectations de règlement sur les factures d'un projet (FEATURE-achat-depenses-projet.md §4.2). */
final class RequetesReglementsProjet {

    // Seul sens retenu : PrjWorkingOut.BuyDocumentId (BuyDocument.PrjWorkingOutId = avances) ; TOP 1 = jamais de doublon.
    static final String DECOMPTE = "OUTER APPLY (SELECT TOP 1 wo.PKey AS DecompteId, wo.Code AS DecompteCode, "
            + "wo.Sorting AS DecompteNumero FROM PrjWorkingOut wo WHERE wo.BuyDocumentId = d.PKey "
            + "ORDER BY wo.DocDate, wo.Code) dc ";

    private RequetesReglementsProjet() {
    }
}

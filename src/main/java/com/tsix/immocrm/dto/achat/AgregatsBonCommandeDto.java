package com.tsix.immocrm.dto.achat;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema
public record AgregatsBonCommandeDto(
        @Schema(description = "Montant facturé HT : Σ HT des factures liées (BuyOrderId).") BigDecimal factureHT,
        @Schema(description = "Montant facturé TVA : facturé TTC − facturé HT.") BigDecimal factureTVA,
        @Schema(description = "Montant facturé TTC : Σ TTC des factures liées.") BigDecimal factureTTC,
        @Schema(description = "Montant facturé net à payer : Σ AmountToPay des factures liées.") BigDecimal factureNetAPayer,
        @Schema(description = "Montant livré HT : Σ HT des bons de livraison liés.") BigDecimal livreHT,
        @Schema(description = "Montant livré TTC : Σ TTC des bons de livraison liés.") BigDecimal livreTTC,
        @Schema(description = "Montant livré net à payer : Σ AmountToPay des bons de livraison liés.")
        BigDecimal livreNetAPayer,
        @Schema(description = "Reste à livrer HT : HT du bon de commande − livré HT.") BigDecimal resteALivrerHT,
        @Schema(description = "Reste à livrer TTC : TTC du bon de commande − livré TTC.") BigDecimal resteALivrerTTC,
        @Schema(description = "Reste à livrer net à payer : à payer du bon de commande − livré net à payer.")
        BigDecimal resteALivrerNetAPayer,
        @Schema(description = "Avance : Σ AmountToPay des pièces liées de référence ORDER_ADVANCE.") BigDecimal avance,
        @Schema(description = "Avance prévue (OrderAdvanceRatio), fraction 0–1 (0,2 = 20 %) ; null dans les totaux.")
        BigDecimal avanceTauxPrevu,
        @Schema(description = "Pourcentage effectif d'avance : avance / à payer du BC, fraction ; 0 si à payer nul ; null dans les totaux.")
        BigDecimal avanceTauxEffectif,
        @Schema(description = "Avance amortie : Σ AdvanceAmount des factures liées.") BigDecimal avanceAmortie,
        @Schema(description = "Avance réglée : Σ des règlements non annulés des pièces d'avance.") BigDecimal avanceRegle,
        @Schema(description = "Avance reste : avance − avance réglée.") BigDecimal avanceReste,
        @Schema(description = "Avance reste à amortir : avance − avance amortie.") BigDecimal avanceResteAAmortir,
        @Schema(description = "Retenue de garantie retenue : Σ WarrantyAmount des factures liées.") BigDecimal garantieRetenue,
        @Schema(description = "Retenue de garantie restituée : Σ HT des pièces liées de référence ORDER_WARRANTY.")
        BigDecimal garantieRestituee,
        @Schema(description = "Retenue de garantie reste : retenue − restituée.") BigDecimal garantieReste,
        @Schema(description = "Montant réglé : réglé des factures liées + avance réglée (règlements annulés exclus).")
        BigDecimal regle,
        @Schema(description = "Reste à payer sur facturé : facturé net à payer − réglé ; signé.") BigDecimal resteAPayerSurFacture,
        @Schema(description = "Reste à payer total : à payer du bon de commande − réglé ; signé.") BigDecimal resteAPayerTotal,
        @Schema(description = "Codes des bons de livraison liés, triés, séparés par « , » ; null sans BL ou dans les totaux.")
        String bonsLivraisonCodes,
        @Schema(description = "Codes des factures liées et de celles des BL liés, séparés par « , » ; null sans facture.")
        String facturesCodes,
        @Schema(description = "Devis d'origine (BuyQuoteId) : sa référence, sinon son code ; null sans devis.")
        String devisCode) {
}

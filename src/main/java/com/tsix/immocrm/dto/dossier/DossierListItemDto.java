package com.tsix.immocrm.dto.dossier;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.tsix.immocrm.domain.ColonneDossier;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Seules les clés demandées par {@code ?colonnes=} sont sérialisées ({@link ColonneDossier}, liste
 * blanche) ; sans le paramètre, les colonnes par défaut.
 */
@JsonFilter(ColonneDossier.FILTRE)
public record DossierListItemDto(
        UUID id,
        String code,
        LocalDateTime dateDocument,
        String statut,
        UUID clientId,
        String clientNom,
        UUID commercialId,
        String commercialNom,
        BigDecimal prix,
        BigDecimal totalAPayer,
        /** Jalon « Concrétisation » ({@code ComFolder.advanceDate}) ; {@code dateDocument} tient lieu de date de
         * réservation (voir {@code ChampDateDossier} et instructions/filtres-avances.md, point 3). */
        LocalDateTime dateConcretisation,
        @Schema(description = "Identifiant de la prospection à l'origine du dossier (ComFolder.ComProspectionId).")
        UUID prospectionId,
        String prospectionCode,

        // Hors sélection par défaut, toutes les colonnes qui suivent.

        @Schema(description = "Prix avant remise.")
        BigDecimal prixAvantRemise,

        @Schema(description = "Taux de remise en pourcentage (5 = 5 %), jamais un ratio 0–1.")
        BigDecimal remise,

        @Schema(description = "Montant de la remise, hors taxes (`ComFolder.DiscountAmount`).")
        BigDecimal remiseMontant,

        @Schema(description = "Prix après remise.")
        BigDecimal prixApresRemise,

        @Schema(description = "Montant de l'avance. Renseigné sur 41,6 % des 358 dossiers réels.")
        BigDecimal montantAvance,

        @Schema(description = "Probabilité de finalisation. 100 %.")
        BigDecimal probabiliteFinalisation,

        UUID projetId,

        @Schema(description = "Code du projet. 100 %.", example = "CITEB")
        String projetCode,

        @Schema(description = "Libellé du projet. 100 %.")
        String projetLibelle,

        @Schema(description = "Identifiant de la tranche. 100 %.")
        UUID trancheId,

        @Schema(description = "Code de la tranche. 100 %.")
        String trancheCode,

        @Schema(description = "Libellé de la tranche. 100 %.")
        String trancheLibelle,

        @Schema(description = "Identifiant de la société / entité juridique. 100 %.")
        UUID societeId,

        @Schema(description = "Code de la société. 100 %.", example = "ADMOD")
        String societeCode,

        @Schema(description = "Libellé de la société. 100 %.")
        String societeLibelle,

        @Schema(description = "Code de la vocation. 100 %.")
        String vocationCode,

        @Schema(description = "Libellé de la vocation. 100 %.")
        String vocationLibelle,

        @Schema(description = "Identifiant du notaire. 19,3 %.")
        UUID avocatId,

        @Schema(description = "Nom du notaire. 19,3 %.")
        String avocatNom,

        @Schema(description = "Mode de paiement.")
        String modePaie,

        @Schema(description = "Étape de la prospection d'origine. `null` sur les 3 dossiers (sur "
                + "358) sans prospection.")
        String etapeProspection,

        @Schema(description = "Origine de la prospection. 99,2 %.", example = "FACEBOOK")
        String origineProspection,

        @Schema(description = "Date de la prospection d'origine. 99,2 %.")
        LocalDateTime dateProspection,

        @Schema(description = "Auteur de la création. 100 % des 358 dossiers réels.")
        String creePar,

        @Schema(description = "Date de création. 100 %.")
        LocalDateTime creeLe,

        @Schema(description = "Auteur de la dernière modification. 100 %.")
        String modifiePar,

        @Schema(description = "Date de dernière modification. 100 %.")
        LocalDateTime modifieLe,

        // Article principal : demander une de ces colonnes coûte jusqu'à cinq jointures paresseuses par ligne.
        @Schema(description = "Id de l'article principal (StkItem.PKey). 100 % des 358 dossiers.")
        UUID articleId,

        @Schema(example = "A-203")
        String articleCode,

        @Schema(description = "Libellé de l'article principal.", example = "Appartement 203")
        String articleLibelle,

        @Schema(description = "Numéro de titre foncier de l'article.")
        String articleTitreFoncier,

        @Schema(description = "Bâtiment de l'article (StkBuilding). Renseigné sur 1 063 des 1 068 "
                + "articles réels.", example = "Bloc B")
        String bloc,

        @Schema(description = "Étage de l'article (StkFloor). 1 063 / 1 068.", example = "2e étage")
        String etage,

        @Schema(description = "Code catégorie de l'article.", example = "APPARTEMENT")
        String categorieArticle,

        @Schema(description = "Code type de l'article.", example = "2 CHAMBRES + SALON")
        String typeArticle,

        @Schema(description = "Code du type de présentation de l'article.")
        String typePresentation,

        @Schema(description = "Id du responsable de vente (ComFolder.CfgSaleManagerId).")
        UUID responsableVenteId,

        @Schema(example = "Hamza ATCHANY")
        String responsableVenteNom,

        @Schema(description = "Date du compromis / approbation crédit (ComFolder.CreditApprovalDate).")
        LocalDateTime dateCompromis,

        @Schema(description = "Date de livraison réelle (ComFolder.DeliveryDateReal). 63 / 358.")
        LocalDateTime dateLivraisonReelle,

        @Schema(example = "47")
        Integer delaiConcretisation,

        @Schema(description = "Jours entre la concrétisation et l'envoi au notaire. Calculé côté serveur.",
                example = "112")
        Integer delaiNotaire,

        @Schema(example = "365")
        Integer delaiLivraison,

        @Schema(description = "Date d'envoi au notaire / avocat (ComFolder.AgreementDate).")
        LocalDateTime dateEnvoiNotaire,

        // Parité grille OMRAN (FEATURE-vente-liste.md §1).

        @Schema(description = "Vrai sur une ligne de lot libre (`lignes=DOSSIERS_ET_LIBRES` ou `LIBRES`) : "
                + "`id` est alors l'article.")
        Boolean libre,

        @Schema(description = "Client marqué ancien client (ComFolder.IsOldCustomer).")
        Boolean ancienClient,

        @Schema(description = "Prénom du client principal (CfgTier.FirstName).")
        String clientPrenom,

        @Schema(description = "Nom du client principal (CfgTier.LastName).")
        String clientNomFamille,

        @Schema(description = "Mobile du client principal, sans indicatif (CfgTier.Mobile).")
        String clientMobile,

        @Schema(description = "Indicatif du mobile du client (CfgTier.MobileIndicatif).", example = "212")
        String clientMobileIndicatif,

        @Schema(description = "Numéro de pièce d'identité du client (CfgTier.IDNumber).")
        String clientCin,

        @Schema(description = "Mobile formaté « (indicatif) numéro », calculé côté serveur comme OMRAN.")
        String clientMobileComplet,

        @Schema(description = "Téléphone formaté « (indicatif) numéro », calculé côté serveur comme OMRAN.")
        String clientTelephone,

        @Schema(description = "Co-acquéreurs (ComFolderCustomer), libellés séparés par des virgules.")
        String autresClients,

        @Schema(description = "Promesse de vente (ComFolder.ReservationDate).")
        LocalDateTime datePromesseVente,

        @Schema(description = "Promesse de vente côté client (ComFolder.ReservationDateCustomer).")
        LocalDateTime datePromesseVenteClient,

        @Schema(description = "Enregistrement de la réservation (ComFolder.ReservationRegistryDate).")
        LocalDateTime dateEnregistrementReservation,

        @Schema(description = "Fiche bancaire (ComFolder.BankRecordDate).")
        LocalDateTime dateFicheBancaire,

        @Schema(description = "Fiche intervention (ComFolder.InterventionDate).")
        LocalDateTime dateFicheIntervention,

        @Schema(description = "Fiche assurance (ComFolder.InsuranceDate).")
        LocalDateTime dateFicheAssurance,

        @Schema(description = "Permis d'habiter de la tranche de l'article principal (CfgTranche.OccupancyPermitsDate).")
        LocalDateTime datePermisHabiter,

        @Schema(description = "Attestation de consignation / octroi crédit (ComFolder.CreditPossessionDate).")
        LocalDateTime dateOctroiCredit,

        @Schema(description = "Minute / enregistrement contrat (ComFolder.AgreementSavingDate).")
        LocalDateTime dateMinute,

        @Schema(description = "Minute côté client (ComFolder.AgreementSavingDateCustomer).")
        LocalDateTime dateMinuteClient,

        @Schema(description = "Minute / signature contrat avocat (ComFolder.AgreementSavingDateLawyer).")
        LocalDateTime dateMinuteAvocat,

        @Schema(description = "Remise du contrat pour légalisation (ComFolder.AgreementSavingDateForLegalization).")
        LocalDateTime dateRemiseLegalisationContrat,

        @Schema(description = "Légalisation du contrat (ComFolder.AgreementSavingDateLegalization).")
        LocalDateTime dateLegalisationContrat,

        @Schema(description = "Dépôt pour enregistrement du contrat (ComFolder.AgreementSavingDateForRegistry).")
        LocalDateTime dateDepotEnregistrementContrat,

        @Schema(description = "Remise des clés prévisionnelle, celle de la tranche (CfgTranche.DeliveryDate).")
        LocalDateTime dateRemiseClesPrevue,

        @Schema(description = "Remise des clés côté client (ComFolder.DeliveryDateRealCustomer).")
        LocalDateTime dateRemiseClesClient,

        @Schema(description = "Enregistrement de la remise des clés (ComFolder.DeliveryRegistryDate).")
        LocalDateTime dateEnregistrementRemiseCles,

        @Schema(description = "Expédition / certificat de propriété (ComFolder.OwnershipCertificateDate).")
        LocalDateTime dateExpedition,

        @Schema(description = "Enregistrement du contrat (ComFolder.AgreementRegistryDate).")
        LocalDateTime dateEnregistrementContrat,

        @Schema(description = "Relance 1 du titre foncier (ComFolder.Revival1).")
        LocalDateTime dateRelance1,

        @Schema(description = "Échéance de la relance 1 (ComFolder.Revival1ValidityDate).")
        LocalDateTime echeanceRelance1,

        @Schema(description = "Relance 2 du titre foncier (ComFolder.Revival2).")
        LocalDateTime dateRelance2,

        @Schema(description = "Échéance de la relance 2 (ComFolder.Revival2ValidityDate).")
        LocalDateTime echeanceRelance2,

        @Schema(description = "Dépôt à la CPF (ComFolder.FinalTitleDateDeposit).")
        LocalDateTime dateDepotCpf,

        @Schema(description = "Retrait du dossier client de la CPF (ComFolder.FinalTitleDateWithdrawal).")
        LocalDateTime dateRetraitCpf,

        @Schema(description = "Remise du certificat de propriété au client (ComFolder.FinalTitleDate).")
        LocalDateTime dateRemiseCertificat,

        @Schema(description = "Numéro du titre foncier définitif (ComFolder.FinalTitleNumber).")
        String numeroTitre,

        @Schema(description = "Fin de validité du statut courant (ComFolder.StatusValidityDate).")
        LocalDateTime dateValiditeStatut,

        @Schema(description = "Dernière modification de la remise (ComFolder.DiscountChangeDate).")
        LocalDateTime dateModificationRemise,

        @Schema(description = "Envoi au notaire prévisionnel (ComFolder.AgreementDatePrevisionel).")
        LocalDateTime dateEnvoiNotairePrevue,

        @Schema(description = "Date d'envoi au notaire confirmée (ComFolder.IsConfirmedAgreementDate).")
        Boolean envoiNotaireConfirme,

        @Schema(description = "Visite technique / réception (ComFolder.ReceptionDate).")
        LocalDateTime dateVisiteTechnique,

        @Schema(description = "Envoi du dossier à MEILLEURTAUX (ComFolder.LoanCompanySendDate).")
        LocalDateTime dateEnvoiMeilleurTaux,

        @Schema(description = "Décision de MEILLEURTAUX (ComFolder.LoanCompanyDecisionDate).")
        LocalDateTime dateDecisionMeilleurTaux,

        @Schema(description = "Libellé de la décision MEILLEURTAUX (ComLoanCompanyDecision.Description).")
        String decisionMeilleurTaux,

        @Schema(description = "Acte de précision (ComFolder.PrecisionActDate).")
        LocalDateTime dateActePrecision,

        @Schema(description = "Acte de précision côté client (ComFolder.PrecisionActDateCustomer).")
        LocalDateTime dateActePrecisionClient,

        @Schema(description = "Enregistrement de l'acte de précision (ComFolder.PrecisionActRegistryDate).")
        LocalDateTime dateEnregistrementActePrecision,

        @Schema(description = "Remise pour légalisation de l'acte de précision (PrecisionActDateForLegalization).")
        LocalDateTime dateRemiseLegalisationActePrecision,

        @Schema(description = "Dépôt pour enregistrement de l'acte de précision (PrecisionActDateForRegistry).")
        LocalDateTime dateDepotEnregistrementActePrecision,

        @Schema(description = "Impression du contrat de réservation / promesse (ComFolder.ContractPrintDate).")
        LocalDateTime dateImpressionContrat,

        @Schema(description = "Second gestionnaire du dossier (ComFolder.CfgSaleManager2Id).")
        String gestionnaire2Nom,

        @Schema(description = "Banque du crédit ou du leasing (ComFolder.CreditBankId).")
        String banqueCredit,

        @Schema(description = "Support de la source de contact de la prospection d'origine.")
        String supportSource,

        @Schema(description = "Action commerciale de la prospection d'origine (ComAction).")
        String actionCommerciale,

        @Schema(description = "Superficie en m² (ComFolder.Area ; StkItem.Area sur un lot libre).")
        BigDecimal superficie,

        @Schema(description = "Superficie globale en m² (ComFolder.GlobalArea).")
        BigDecimal superficieGlobale,

        @Schema(description = "Codes des articles du dossier (ComFolderItem), distincts, séparés par des virgules.")
        String sousArticlesCodes,

        @Schema(description = "Libellés des articles du dossier (ComFolderItem), distincts, séparés par des virgules.")
        String sousArticlesLibelles,

        @Schema(description = "Prix du m² HT avant remise (ComFolder.UnitCostHT).")
        BigDecimal prixM2HtAvantRemise,

        @Schema(description = "Prix de l'article HT avant remise (ComFolder.ItemPriceHT).")
        BigDecimal prixArticleHtAvantRemise,

        @Schema(description = "Prix des sous-articles HT avant remise (ComFolder.UnderItemsPriceHT).")
        BigDecimal prixSousArticlesHtAvantRemise,

        @Schema(description = "Prix de vente HT avant remise (ComFolder.PriceBeforeDiscountHT).")
        BigDecimal prixAvantRemiseHt,

        @Schema(description = "Prix du m² TTC avant remise (ComFolder.UnitCost).")
        BigDecimal prixM2AvantRemise,

        @Schema(description = "Prix de l'article TTC avant remise (ComFolder.ItemPrice).")
        BigDecimal prixArticleAvantRemise,

        @Schema(description = "Prix des sous-articles TTC avant remise (ComFolder.UnderItemsPrice).")
        BigDecimal prixSousArticlesAvantRemise,

        @Schema(description = "Taux de TVA en fraction (0.2 = 20 %) (ComFolder.VATRatio).", example = "0.2")
        BigDecimal tva,

        @Schema(description = "Prix de vente du m² HT après remise : prix article HT ÷ superficie, calculé.")
        BigDecimal prixM2Ht,

        @Schema(description = "Prix de vente de l'article HT après remise (ComFolder.ItemPriceAfterHT).")
        BigDecimal prixArticleHt,

        @Schema(description = "Prix de vente des sous-articles HT après remise (ComFolder.UnderItemsPriceAfterHT).")
        BigDecimal prixSousArticlesHt,

        @Schema(description = "Prix de vente HT (ComFolder.PriceHT).")
        BigDecimal prixHt,

        @Schema(description = "Prix de vente du m² TTC : prix article TTC ÷ superficie (1 si nulle), comme OMRAN.")
        BigDecimal prixM2Ttc,

        @Schema(description = "Prix de vente de l'article TTC après remise (ComFolder.ItemPriceAfter).")
        BigDecimal prixArticleTtc,

        @Schema(description = "Prix de vente des sous-articles TTC après remise (ComFolder.UnderItemsPriceAfter).")
        BigDecimal prixSousArticlesTtc,

        @Schema(description = "Prix après remise ÷ (superficie + superficie des annexes), TTC. Calculé.")
        BigDecimal prixVenteM2Ttc,

        @Schema(description = "Prix après remise ÷ (superficie + superficie des annexes) ÷ (1 + TVA). Calculé.")
        BigDecimal prixVenteM2Ht,

        @Schema(description = "Modifications au statut ENCOURS (ComModification).")
        Integer nombreEtatsModification,

        @Schema(description = "Cadeaux attribués au dossier (ComFolderGift).")
        Integer nombreCadeaux,

        @Schema(description = "Le client choisira son cadeau plus tard (ComFolder.IsWithGiftChoiceLater).")
        Boolean cadeauPlusTard,

        @Schema(description = "Date de la dernière communication non système de la prospection d'origine.")
        LocalDateTime derniereCommunicationDate,

        @Schema(description = "Résultat (sous-catégorie) de la dernière communication.")
        String derniereCommunicationResultat,

        @Schema(description = "Message de la dernière communication, à défaut son résultat.")
        String derniereCommunicationMessage,

        @Schema(description = "Date de la dernière alerte active de la prospection d'origine ; `null` = pas d'icône.")
        LocalDateTime alerte,

        @Schema(description = "Vrai si une instruction non lue de la prospection vise l'utilisateur connecté.")
        Boolean instructionNonLue,

        @Schema(description = "Actions du dossier (réservation, livraison, annulation) datées d'aujourd'hui ou avant.")
        Integer nombreActions,

        @Schema(description = "Jours entre l'envoi au notaire et l'octroi du crédit. Calculé.")
        Integer delaiCredit,

        @Schema(description = "Jours entre l'octroi du crédit et la remise des clés. Calculé.")
        Integer delaiCreditRemiseCles,

        @Schema(description = "Jours entre l'envoi et la décision MEILLEURTAUX ; 0 si l'une manque, comme OMRAN.")
        Integer delaiMeilleurTaux
) {
}

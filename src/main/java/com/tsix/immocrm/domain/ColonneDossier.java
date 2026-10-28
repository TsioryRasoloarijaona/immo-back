package com.tsix.immocrm.domain;

import java.util.List;
import java.util.Set;

/**
 * Catalogue des colonnes de {@code GET /api/dossiers} : liste blanche de {@code ?colonnes=}.
 * Exclusions (articles, ventilation financière, notes) : FEATURE-selecteur-colonnes-prospect-dossier.md §3.2.
 */
public enum ColonneDossier implements Colonne {

    // -- Colonnes historiques : la sélection par défaut, à ne pas modifier.
    ID("id", "Identifiant", Groupe.IDENTITE, TypeColonne.TEXTE, true,
            Chemin.groupable("id")),
    CODE("code", "Code", Groupe.IDENTITE, TypeColonne.TEXTE, true,
            Chemin.groupable("code")),
    DATE_DOCUMENT("dateDocument", "Date Réservation", Groupe.IDENTITE, TypeColonne.DATE, true,
            Chemin.date("docDate")),
    STATUT("statut", "Statut", Groupe.IDENTITE, TypeColonne.TEXTE, true,
            Chemin.groupable("strStatus.code")),
    CLIENT_ID("clientId", "Id client", Groupe.ACTEURS, TypeColonne.TEXTE, true,
            Chemin.groupable("cfgCustomer.id")),
    CLIENT_NOM("clientNom", "Client", Groupe.ACTEURS, TypeColonne.TEXTE, true,
            Chemin.tier("cfgCustomer")),
    COMMERCIAL_ID("commercialId", "Id commercial", Groupe.ACTEURS, TypeColonne.TEXTE, true,
            Chemin.groupable("cfgCommercial.id")),
    COMMERCIAL_NOM("commercialNom", "Commercial", Groupe.ACTEURS, TypeColonne.TEXTE, true,
            Chemin.tier("cfgCommercial")),
    PRIX("prix", "Prix de vente", Groupe.FINANCIER, TypeColonne.MONTANT, true,
            Chemin.groupableEtAgregeable("price")),
    TOTAL_A_PAYER("totalAPayer", "Total à payer", Groupe.FINANCIER, TypeColonne.MONTANT, true,
            Chemin.groupableEtAgregeable("totalToPay")),
    DATE_CONCRETISATION("dateConcretisation", "Concrétisation", Groupe.IDENTITE, TypeColonne.DATE, true,
            Chemin.date("advanceDate")),
    PROSPECTION_ID("prospectionId", "Id prospection", Groupe.ORIGINE, TypeColonne.TEXTE, true,
            Chemin.groupable("comProspection.id")),
    PROSPECTION_CODE("prospectionCode", "Prospection", Groupe.ORIGINE, TypeColonne.TEXTE, true,
            Chemin.groupable("comProspection.code")),

    // -- Financier, déjà servi par la fiche. Scalaires de ComFolder : coût nul.
    // `REMISE` est en pourcentage 0–100, `PROBABILITE_FINALISATION` en fraction (1.00 = 100 %) :
    // sommables quand même, c'est au client de choisir ce qu'il additionne.
    PRIX_AVANT_REMISE("prixAvantRemise", "Prix avant remise", Groupe.FINANCIER, TypeColonne.MONTANT, false,
            Chemin.groupableEtAgregeable("priceBeforeDiscount")),
    REMISE("remise", "Remise (%)", Groupe.FINANCIER, TypeColonne.NOMBRE, false,
            Chemin.groupableEtAgregeable("remisePourcentage")),
    REMISE_MONTANT("remiseMontant", "Remise HT", Groupe.FINANCIER, TypeColonne.MONTANT, false,
            Chemin.groupableEtAgregeable("discountAmount")),
    PRIX_APRES_REMISE("prixApresRemise", "Prix après remise", Groupe.FINANCIER, TypeColonne.MONTANT, false,
            Chemin.groupableEtAgregeable("priceAfterDiscount")),
    // Lit ComFolder.AgreementAmount (« montant d'accord »), pas l'avance : celle-ci est la colonne AVANCE.
    MONTANT_AVANCE("montantAvance", "Montant d'accord", Groupe.FINANCIER, TypeColonne.MONTANT, false,
            Chemin.groupableEtAgregeable("agreementAmount")),
    PROBABILITE_FINALISATION("probabiliteFinalisation", "Probabilité", Groupe.FINANCIER, TypeColonne.NOMBRE, false,
            Chemin.groupableEtAgregeable("probabilityOfFinalisation")),

    // -- Contexte patrimonial, via `stkItem` (article principal) qui est NULLABLE : le groupement
    //    doit le joindre en LEFT explicite, sinon INNER JOIN implicite et groupes perdus.
    PROJET_ID("projetId", "Id projet", Groupe.PATRIMOINE, TypeColonne.TEXTE, false,
            Chemin.groupable("stkItem.stkHierarchy.cfgProject.id")),
    PROJET_CODE("projetCode", "Code projet", Groupe.PATRIMOINE, TypeColonne.TEXTE, false,
            Chemin.groupable("stkItem.stkHierarchy.cfgProject.code")),
    PROJET_LIBELLE("projetLibelle", "Projet", Groupe.PATRIMOINE, TypeColonne.TEXTE, false,
            Chemin.groupable("stkItem.stkHierarchy.cfgProject.description")),
    TRANCHE_ID("trancheId", "Id tranche", Groupe.PATRIMOINE, TypeColonne.TEXTE, false,
            Chemin.groupable("stkItem.stkHierarchy.id")),
    TRANCHE_CODE("trancheCode", "Code tranche", Groupe.PATRIMOINE, TypeColonne.TEXTE, false,
            Chemin.groupable("stkItem.stkHierarchy.code")),
    TRANCHE_LIBELLE("trancheLibelle", "Tranche", Groupe.PATRIMOINE, TypeColonne.TEXTE, false,
            Chemin.groupable("stkItem.stkHierarchy.description")),
    SOCIETE_ID("societeId", "Id société", Groupe.PATRIMOINE, TypeColonne.TEXTE, false,
            Chemin.groupable("stkItem.stkHierarchy.cfgProject.cfgCompany.id")),
    SOCIETE_CODE("societeCode", "Société", Groupe.PATRIMOINE, TypeColonne.TEXTE, false,
            Chemin.groupable("stkItem.stkHierarchy.cfgProject.cfgCompany.code")),
    SOCIETE_LIBELLE("societeLibelle", "Libellé société", Groupe.PATRIMOINE, TypeColonne.TEXTE, false,
            Chemin.groupable("stkItem.stkHierarchy.cfgProject.cfgCompany.description")),
    VOCATION_CODE("vocationCode", "Vocation", Groupe.PATRIMOINE, TypeColonne.TEXTE, false,
            Chemin.groupable("stkVocation.code")),
    VOCATION_LIBELLE("vocationLibelle", "Libellé vocation", Groupe.PATRIMOINE, TypeColonne.TEXTE, false,
            Chemin.groupable("stkVocation.description")),

    // -- Acteurs.
    AVOCAT_ID("avocatId", "Id notaire", Groupe.ACTEURS, TypeColonne.TEXTE, false,
            Chemin.groupable("cfgLawyer.id")),
    AVOCAT_NOM("avocatNom", "Notaire", Groupe.ACTEURS, TypeColonne.TEXTE, false,
            Chemin.tier("cfgLawyer")),
    MODE_PAIE("modePaie", "Mode de paiement", Groupe.FINANCIER, TypeColonne.TEXTE, false,
            Chemin.groupable("comPaymentMethod.code")),

    // -- Origine, depuis la prospection. `etapeProspection` lit ComProspectionKind, comme le mappeur de liste.
    ETAPE_PROSPECTION("etapeProspection", "Étape prospection", Groupe.ORIGINE, TypeColonne.TEXTE, false,
            Chemin.groupable("comProspection.comProspectionKind.description")),
    ORIGINE_PROSPECTION("origineProspection", "Source de contact", Groupe.ORIGINE, TypeColonne.TEXTE, false,
            Chemin.groupable("comProspection.comProspectionOrigin.description")),
    DATE_PROSPECTION("dateProspection", "Date prospection", Groupe.ORIGINE, TypeColonne.DATE, false,
            Chemin.date("comProspection.docDate")),

    // -- Piste d'audit.
    CREE_PAR("creePar", "Créé par", Groupe.AUDIT, TypeColonne.TEXTE, false,
            Chemin.groupable("userCreate")),
    CREE_LE("creeLe", "Créé le", Groupe.AUDIT, TypeColonne.DATE, false,
            Chemin.date("userCreateDateTime")),
    MODIFIE_PAR("modifiePar", "Modifié par", Groupe.AUDIT, TypeColonne.TEXTE, false,
            Chemin.groupable("userUpdate")),
    MODIFIE_LE("modifieLe", "Modifié le", Groupe.AUDIT, TypeColonne.DATE, false,
            Chemin.date("userUpdateDateTime")),

    // -- Article principal : l'écran légataire trie par `ArticleCode, H1Code, H2Code, H3Code`
    //    (H1/H2/H3 = tranche, projet, société). Voir FEATURE-colonnes-dossier-ecran-legataire.md §1.
    ARTICLE_ID("articleId", "Id article", Groupe.ARTICLE, TypeColonne.TEXTE, false,
            Chemin.groupable("stkItem.id")),
    /** La clé de tri par défaut de l'écran légataire. */
    ARTICLE_CODE("articleCode", "Article", Groupe.ARTICLE, TypeColonne.TEXTE, false,
            Chemin.groupable("stkItem.code")),
    ARTICLE_LIBELLE("articleLibelle", "Description article", Groupe.ARTICLE, TypeColonne.TEXTE, false,
            Chemin.groupable("stkItem.description")),
    ARTICLE_TITRE_FONCIER("articleTitreFoncier", "Titre foncier", Groupe.ARTICLE, TypeColonne.TEXTE, false,
            Chemin.groupable("stkItem.titleNumber")),
    BLOC("bloc", "Bloc", Groupe.ARTICLE, TypeColonne.TEXTE, false,
            Chemin.groupable("stkItem.stkBuilding.description")),
    ETAGE("etage", "Étage", Groupe.ARTICLE, TypeColonne.TEXTE, false,
            Chemin.groupable("stkItem.stkFloor.description")),
    CATEGORIE_ARTICLE("categorieArticle", "Catégorie", Groupe.ARTICLE, TypeColonne.TEXTE, false,
            Chemin.groupable("stkItem.stkItemCategory.code")),
    TYPE_ARTICLE("typeArticle", "Type", Groupe.ARTICLE, TypeColonne.TEXTE, false,
            Chemin.groupable("stkItem.stkItemType.code")),
    TYPE_PRESENTATION("typePresentation", "Présentation", Groupe.ARTICLE, TypeColonne.TEXTE, false,
            Chemin.groupable("stkItem.stkPresentationType.code")),

    // -- Responsable de vente, distinct du commercial. `CfgSaleManager2Id` non repris (vide).
    RESPONSABLE_VENTE_ID("responsableVenteId", "Id responsable de vente", Groupe.ACTEURS, TypeColonne.TEXTE, false,
            Chemin.groupable("cfgSaleManager.id")),
    RESPONSABLE_VENTE_NOM("responsableVenteNom", "Responsable de vente", Groupe.ACTEURS, TypeColonne.TEXTE, false,
            Chemin.tier("cfgSaleManager")),

    // -- Jalons et délais. Les délais sont des @Formula (FEATURE-colonnes-dossier-ecran-legataire.md §3) ;
    //    les deux dates en sont les entrées, sans elles un nombre de jours est invérifiable à l'écran.
    // OMRAN : le compromis est CreditApprovalDate ; AgreementDate = « Date Envoi Notaire / Avocat ».
    DATE_COMPROMIS("dateCompromis", "Date Compromis / Approbation Crédit", Groupe.IDENTITE, TypeColonne.DATE, false,
            Chemin.date("creditApprovalDate")),
    DATE_ENVOI_NOTAIRE("dateEnvoiNotaire", "Envoi notaire / avocat", Groupe.IDENTITE, TypeColonne.DATE, false,
            Chemin.date("agreementDate")),
    DATE_LIVRAISON_REELLE("dateLivraisonReelle", "Date Remise de Clés", Groupe.IDENTITE, TypeColonne.DATE, false,
            Chemin.date("deliveryDateReal")),
    /**
     * Non groupable : sa formule contient une sous-requête corrélée, que SQL Server refuse dans un
     * GROUP BY (SQLGrammarException en 500). Les deux autres délais n'en ont pas.
     */
    DELAI_CONCRETISATION("delaiConcretisation", "Délais Prospection-Concrétisation", Groupe.DELAIS, TypeColonne.NOMBRE,
            false, Chemin.agregeable("delaiConcretisation")),
    DELAI_NOTAIRE("delaiNotaire", "Délais Concrétisation-Envoi Notaire", Groupe.DELAIS, TypeColonne.NOMBRE, false,
            Chemin.groupableEtAgregeable("delaiNotaire")),
    DELAI_LIVRAISON("delaiLivraison", "Délais Envoi au Notaire-Remise des Clés", Groupe.DELAIS, TypeColonne.NOMBRE,
            false, Chemin.groupableEtAgregeable("delaiLivraison")),
    // Calculés en Java (chemin null) comme le DATEDIFF de ComFolderView ; une borne nulle rend null.
    DELAI_NOTAIRE_CREDIT("delaiNotaireCredit", "Délais Envoi au Notaire-Crédit Accordé", Groupe.DELAIS,
            TypeColonne.NOMBRE, false, null),
    DELAI_CREDIT_CLES("delaiCreditCles", "Délais Crédit Accordé - Remise des Clés", Groupe.DELAIS, TypeColonne.NOMBRE,
            false, null),

    // -- Parité avec la grille OMRAN (SaleMonitoringList), voir FEATURE-vente-liste.md §1.
    //    Chemin null = valeur calculée (Java ou requête native par page) : ni groupable ni agrégeable.
    LIBRE("libre", "Lot libre", Groupe.IDENTITE, TypeColonne.BOOLEEN, false, null),

    ANCIEN_CLIENT("ancienClient", "Ancien client", Groupe.CLIENT, TypeColonne.BOOLEEN, false,
            Chemin.groupable("isOldCustomer")),
    CLIENT_PRENOM("clientPrenom", "Prénom client", Groupe.CLIENT, TypeColonne.TEXTE, false,
            Chemin.groupable("cfgCustomer.firstName")),
    CLIENT_NOM_FAMILLE("clientNomFamille", "Nom client", Groupe.CLIENT, TypeColonne.TEXTE, false,
            Chemin.groupable("cfgCustomer.lastName")),
    CLIENT_MOBILE("clientMobile", "Mobile client", Groupe.CLIENT, TypeColonne.TEXTE, false,
            Chemin.groupable("cfgCustomer.mobile")),
    CLIENT_MOBILE_INDICATIF("clientMobileIndicatif", "Indicatif mobile client", Groupe.CLIENT, TypeColonne.TEXTE,
            false, Chemin.groupable("cfgCustomer.mobileIndicatif")),
    CLIENT_CIN("clientCin", "Pièce d'identité N°", Groupe.CLIENT, TypeColonne.TEXTE, false,
            Chemin.groupable("cfgCustomer.iDNumber")),
    CLIENT_MOBILE_COMPLET("clientMobileComplet", "Mobile", Groupe.CLIENT, TypeColonne.TEXTE, false, null),
    CLIENT_TELEPHONE("clientTelephone", "Tél.", Groupe.CLIENT, TypeColonne.TEXTE, false, null),
    AUTRES_CLIENTS("autresClients", "Autre clients", Groupe.CLIENT, TypeColonne.TEXTE, false, null),
    CLIENT_EMAIL("clientEmail", "E-mail client", Groupe.CLIENT, TypeColonne.TEXTE, false,
            Chemin.groupable("cfgCustomer.eMail")),
    // Groupé sur la seule première ligne d'adresse ; la valeur affichée y joint Address2 et Address3.
    CLIENT_ADRESSE("clientAdresse", "Adresse client", Groupe.CLIENT, TypeColonne.TEXTE, false,
            Chemin.groupable("cfgCustomer.address")),
    CLIENT_VILLE("clientVille", "Ville client", Groupe.CLIENT, TypeColonne.TEXTE, false, null),
    CLIENT_PAYS("clientPays", "Pays client", Groupe.CLIENT, TypeColonne.TEXTE, false,
            Chemin.groupable("cfgCustomer.admCountry.description")),

    // Toutes les dates de la grille OMRAN, même peu remplies : voir FEATURE-dossiers-colonnes-omran.md §2.1.
    DATE_FICHE_BANCAIRE("dateFicheBancaire", "Fiche bancaire", Groupe.DATES, TypeColonne.DATE, false,
            Chemin.date("bankRecordDate")),
    DATE_PERMIS_HABITER("datePermisHabiter", "Permis d'habiter", Groupe.DATES, TypeColonne.DATE, false,
            Chemin.date("stkItem.stkHierarchy.occupancyPermitsDate")),
    DATE_MINUTE("dateMinute", "Date minute / Signature contrat", Groupe.DATES, TypeColonne.DATE, false,
            Chemin.date("agreementSavingDate")),
    DATE_REMISE_CLES_PREVUE("dateRemiseClesPrevue", "Remise de clés prévisionnelle", Groupe.DATES,
            TypeColonne.DATE, false, Chemin.date("stkItem.stkHierarchy.deliveryDate")),
    DATE_ENREGISTREMENT_CONTRAT("dateEnregistrementContrat", "Enregistrement du contrat", Groupe.DATES,
            TypeColonne.DATE, false, Chemin.date("agreementRegistryDate")),
    DATE_VALIDITE_STATUT("dateValiditeStatut", "Validité du statut", Groupe.DATES, TypeColonne.DATE, false,
            Chemin.date("statusValidityDate")),
    DATE_MODIFICATION_REMISE("dateModificationRemise", "Modification de la remise", Groupe.DATES,
            TypeColonne.DATE, false, Chemin.date("discountChangeDate")),
    DATE_ENVOI_NOTAIRE_PREVUE("dateEnvoiNotairePrevue", "Envoi notaire / avocat prévisionnel", Groupe.DATES,
            TypeColonne.DATE, false, Chemin.date("agreementDatePrevisionel")),
    ENVOI_NOTAIRE_CONFIRME("envoiNotaireConfirme", "Envoi notaire confirmé", Groupe.DATES, TypeColonne.BOOLEEN,
            false, Chemin.groupable("isConfirmedAgreementDate")),
    DATE_DECISION_MEILLEUR_TAUX("dateDecisionMeilleurTaux", "Date Décision MEILLEURTAUX", Groupe.DATES,
            TypeColonne.DATE, false, Chemin.date("loanCompanyDecisionDate")),
    DATE_IMPRESSION_CONTRAT("dateImpressionContrat", "Impression contrat rés./prom.", Groupe.DATES,
            TypeColonne.DATE, false, Chemin.date("contractPrintDate")),
    DATE_REMISE_CONTRAT_LEGALISATION("dateRemiseContratLegalisation", "Date remise contrat pour légalisation",
            Groupe.DATES, TypeColonne.DATE, false, Chemin.date("agreementSavingDateForLegalization")),
    DATE_LEGALISATION_CONTRAT("dateLegalisationContrat", "Date légalisation contrat", Groupe.DATES,
            TypeColonne.DATE, false, Chemin.date("agreementSavingDateLegalization")),
    DATE_REMISE_CERTIFICAT_PROPRIETE("dateRemiseCertificatPropriete",
            "Date remise du certificat de propriété au client", Groupe.DATES, TypeColonne.DATE, false,
            Chemin.date("finalTitleDate")),
    NUMERO_TITRE("numeroTitre", "N° titre", Groupe.DATES, TypeColonne.TEXTE, false,
            Chemin.groupable("finalTitleNumber")),
    DATE_PROMESSE_VENTE("datePromesseVente", "Date Promesse de Vente", Groupe.DATES, TypeColonne.DATE, false,
            Chemin.date("reservationDate")),
    DATE_FICHE_INTERVENTION("dateFicheIntervention", "Date Fiche Intervention", Groupe.DATES, TypeColonne.DATE,
            false, Chemin.date("interventionDate")),
    DATE_OCTROI_CREDIT("dateOctroiCredit", "Date Attestation de Consignation / Octroi Crédit", Groupe.DATES,
            TypeColonne.DATE, false, Chemin.date("creditPossessionDate")),
    DATE_VISITE_TECHNIQUE("dateVisiteTechnique", "Date Visite Technique / Réception", Groupe.DATES,
            TypeColonne.DATE, false, Chemin.date("receptionDate")),
    DATE_EXPEDITION("dateExpedition", "Date Expédition", Groupe.DATES, TypeColonne.DATE, false,
            Chemin.date("ownershipCertificateDate")),
    ECHEANCE_RELANCE_1("echeanceRelance1", "Échéance de la relance 1", Groupe.DATES, TypeColonne.DATE, false,
            Chemin.date("revival1ValidityDate")),
    ECHEANCE_RELANCE_2("echeanceRelance2", "Échéance de la relance 2", Groupe.DATES, TypeColonne.DATE, false,
            Chemin.date("revival2ValidityDate")),
    DATE_ENVOI_MEILLEUR_TAUX("dateEnvoiMeilleurTaux", "Date Envoi MEILLEURTAUX", Groupe.DATES, TypeColonne.DATE,
            false, Chemin.date("loanCompanySendDate")),

    BANQUE_CREDIT("banqueCredit", "Banque crédit / leasing", Groupe.ACTEURS, TypeColonne.TEXTE, false,
            Chemin.tier("creditBank")),
    SUPPORT_SOURCE("supportSource", "Support source de contact", Groupe.ORIGINE, TypeColonne.TEXTE, false,
            Chemin.groupable("comProspection.comProspectionOriginSupport.description")),
    ACTION_COMMERCIALE("actionCommerciale", "Action commerciale", Groupe.ORIGINE, TypeColonne.TEXTE, false,
            Chemin.groupable("comProspection.comAction.description")),

    SUPERFICIE("superficie", "Sup.(m²)", Groupe.ARTICLE, TypeColonne.NOMBRE, false,
            Chemin.groupableEtAgregeable("area")),
    SUPERFICIE_GLOBALE("superficieGlobale", "Sup.Globale (m²)", Groupe.ARTICLE, TypeColonne.NOMBRE, false,
            Chemin.groupableEtAgregeable("globalArea")),
    SOUS_ARTICLES_CODES("sousArticlesCodes", "Sous-articles (codes)", Groupe.ARTICLE, TypeColonne.TEXTE, false,
            null),
    SOUS_ARTICLES_LIBELLES("sousArticlesLibelles", "Sous-articles", Groupe.ARTICLE, TypeColonne.TEXTE, false,
            null),

    PRIX_M2_HT_AVANT_REMISE("prixM2HtAvantRemise", "Prix du m² HT avant remise", Groupe.PRIX, TypeColonne.MONTANT,
            false, Chemin.groupableEtAgregeable("unitCostHT")),
    PRIX_ARTICLE_HT_AVANT_REMISE("prixArticleHtAvantRemise", "Prix article HT avant remise", Groupe.PRIX,
            TypeColonne.MONTANT, false, Chemin.groupableEtAgregeable("itemPriceHT")),
    PRIX_SOUS_ARTICLES_HT_AVANT_REMISE("prixSousArticlesHtAvantRemise", "Prix sous-articles HT avant remise",
            Groupe.PRIX, TypeColonne.MONTANT, false, Chemin.groupableEtAgregeable("underItemsPriceHT")),
    PRIX_AVANT_REMISE_HT("prixAvantRemiseHt", "Prix de vente HT avant remise", Groupe.PRIX, TypeColonne.MONTANT,
            false, Chemin.groupableEtAgregeable("priceBeforeDiscountHT")),
    PRIX_M2_AVANT_REMISE("prixM2AvantRemise", "Prix du m² TTC avant remise", Groupe.PRIX, TypeColonne.MONTANT,
            false, Chemin.groupableEtAgregeable("unitCost")),
    PRIX_ARTICLE_AVANT_REMISE("prixArticleAvantRemise", "Prix article TTC avant remise", Groupe.PRIX,
            TypeColonne.MONTANT, false, Chemin.groupableEtAgregeable("itemPrice")),
    PRIX_SOUS_ARTICLES_AVANT_REMISE("prixSousArticlesAvantRemise", "Prix sous-articles TTC avant remise",
            Groupe.PRIX, TypeColonne.MONTANT, false, Chemin.groupableEtAgregeable("underItemsPrice")),
    TVA("tva", "TVA", Groupe.PRIX, TypeColonne.NOMBRE, false, Chemin.groupable("vATRatio")),
    // Comme ComFolderView : ces trois « prix de vente HT » relisent UnitCostHT / ItemPriceHT / UnderItemsPriceHT.
    PRIX_M2_HT("prixM2Ht", "Prix de vente du m² HT", Groupe.PRIX, TypeColonne.MONTANT, false,
            Chemin.groupableEtAgregeable("unitCostHT")),
    PRIX_ARTICLE_HT("prixArticleHt", "Prix de vente article HT", Groupe.PRIX, TypeColonne.MONTANT, false,
            Chemin.groupableEtAgregeable("itemPriceHT")),
    PRIX_SOUS_ARTICLES_HT("prixSousArticlesHt", "Prix de vente sous-articles HT", Groupe.PRIX, TypeColonne.MONTANT,
            false, Chemin.groupableEtAgregeable("underItemsPriceHT")),
    PRIX_HT("prixHt", "Prix de vente HT", Groupe.PRIX, TypeColonne.MONTANT, false,
            Chemin.groupableEtAgregeable("priceHT")),
    PRIX_M2_TTC("prixM2Ttc", "Prix de vente du m² TTC", Groupe.PRIX, TypeColonne.MONTANT, false, null),
    PRIX_ARTICLE_TTC("prixArticleTtc", "Prix de vente article TTC", Groupe.PRIX, TypeColonne.MONTANT, false,
            Chemin.groupableEtAgregeable("itemPriceAfter")),
    PRIX_SOUS_ARTICLES_TTC("prixSousArticlesTtc", "Prix de vente sous-articles TTC", Groupe.PRIX,
            TypeColonne.MONTANT, false, Chemin.groupableEtAgregeable("underItemsPriceAfter")),
    PRIX_VENTE_M2_TTC("prixVenteM2Ttc", "Prix de vente m² TTC (avec sous-articles)", Groupe.PRIX,
            TypeColonne.MONTANT, false, null),
    PRIX_VENTE_M2_HT("prixVenteM2Ht", "Prix de vente m² HT (avec sous-articles)", Groupe.PRIX, TypeColonne.MONTANT,
            false, null),

    NOMBRE_ETATS_MODIFICATION("nombreEtatsModification", "Nombre d'états de modification", Groupe.SUIVI,
            TypeColonne.NOMBRE, false, null),
    NOMBRE_CADEAUX("nombreCadeaux", "Nombre de cadeaux", Groupe.SUIVI, TypeColonne.NOMBRE, false, null),
    CADEAU_PLUS_TARD("cadeauPlusTard", "Client veut choisir le cadeau plus tard", Groupe.SUIVI,
            TypeColonne.BOOLEEN, false, Chemin.groupable("isWithGiftChoiceLater")),
    DERNIERE_COMMUNICATION_DATE("derniereCommunicationDate", "Date dernière communication", Groupe.SUIVI,
            TypeColonne.DATE, false, null),
    DERNIERE_COMMUNICATION_RESULTAT("derniereCommunicationResultat", "Dernier résultat de communication",
            Groupe.SUIVI, TypeColonne.TEXTE, false, null),
    DERNIERE_COMMUNICATION_MESSAGE("derniereCommunicationMessage", "Dernier message de communication",
            Groupe.SUIVI, TypeColonne.TEXTE, false, null),
    ALERTE("alerte", "Alerte", Groupe.SUIVI, TypeColonne.DATE, false, null),
    INSTRUCTION_NON_LUE("instructionNonLue", "Instruction non lue", Groupe.SUIVI, TypeColonne.BOOLEEN, false, null),
    NOMBRE_ACTIONS("nombreActions", "Nombre d'actions", Groupe.SUIVI, TypeColonne.NOMBRE, false, null),

    DELAI_MEILLEUR_TAUX("delaiMeilleurTaux", "Délai MEILLEURTAUX (j)", Groupe.DELAIS, TypeColonne.NOMBRE, false,
            null),
    // ComLoanCompanyDecision.Description, lu par page avec les colonnes calculées (pas d'entité).
    DECISION_MEILLEUR_TAUX("decisionMeilleurTaux", "Décision MEILLEURTAUX", Groupe.DELAIS, TypeColonne.TEXTE, false,
            null),

    // -- Montants par nature de la grille OMRAN (ComSaleFolderView), lus par MontantsParNatureDossier.
    //    Chemin null : calculés par page, ni groupables ni agrégeables ; leur Σ passe par /totaux.
    PRIX_CONTRACTUEL("prixContractuel", "Prix contractuel", Groupe.FINANCIER, TypeColonne.MONTANT, false,
            Chemin.groupableEtAgregeable("agreementPrice")),
    PRIX_CONTRACTUEL_REGLE("prixContractuelRegle", "Prix contractuel réglé", Groupe.FINANCIER, TypeColonne.MONTANT,
            false, null),
    PRIX_CONTRACTUEL_RESTE("prixContractuelReste", "Prix contractuel reste", Groupe.FINANCIER, TypeColonne.MONTANT,
            false, null),
    PRIX_REGLE("prixRegle", "Prix de vente réglé", Groupe.FINANCIER, TypeColonne.MONTANT, false, null),
    PRIX_RESTE("prixReste", "Prix de vente reste", Groupe.FINANCIER, TypeColonne.MONTANT, false, null),
    NET_A_PAYER("netAPayer", "Net à payer", Groupe.FINANCIER, TypeColonne.MONTANT, false, null),
    NET_A_PAYER_REGLE("netAPayerRegle", "Net à payer réglé", Groupe.FINANCIER, TypeColonne.MONTANT, false, null),
    NET_A_PAYER_RESTE("netAPayerReste", "Net à payer reste", Groupe.FINANCIER, TypeColonne.MONTANT, false, null),
    TAUX_AVANCE("tauxAvance", "Taux avance de base (%)", Groupe.FINANCIER, TypeColonne.NOMBRE, false, null),
    TAUX_AVANCE_REEL("tauxAvanceReel", "Taux avance réel (%)", Groupe.FINANCIER, TypeColonne.NOMBRE, false, null),
    AVANCE("avance", "Avance", Groupe.FINANCIER, TypeColonne.MONTANT, false, null),
    AVANCE_REGLEE("avanceReglee", "Avance réglée", Groupe.FINANCIER, TypeColonne.MONTANT, false, null),
    AVANCE_RESTE("avanceReste", "Avance reste", Groupe.FINANCIER, TypeColonne.MONTANT, false, null),
    AUTOFINANCEMENT("autofinancement", "Autofinancement", Groupe.FINANCIER, TypeColonne.MONTANT, false, null),
    AUTOFINANCEMENT_REGLE("autofinancementRegle", "Autofinancement réglé", Groupe.FINANCIER, TypeColonne.MONTANT,
            false, null),
    AUTOFINANCEMENT_RESTE("autofinancementReste", "Autofinancement reste", Groupe.FINANCIER, TypeColonne.MONTANT,
            false, null),
    CREDIT("credit", "Crédit", Groupe.FINANCIER, TypeColonne.MONTANT, false, null),
    CREDIT_REGLE("creditRegle", "Crédit réglé", Groupe.FINANCIER, TypeColonne.MONTANT, false, null),
    CREDIT_RESTE("creditReste", "Crédit reste", Groupe.FINANCIER, TypeColonne.MONTANT, false, null),
    FRAIS("frais", "Frais", Groupe.FINANCIER, TypeColonne.MONTANT, false, null),
    FRAIS_REGLES("fraisRegles", "Frais réglés", Groupe.FINANCIER, TypeColonne.MONTANT, false, null),
    FRAIS_RESTE("fraisReste", "Frais reste", Groupe.FINANCIER, TypeColonne.MONTANT, false, null),
    FRAIS_SYNDIC("fraisSyndic", "Frais syndic", Groupe.FINANCIER, TypeColonne.MONTANT, false, null),
    FRAIS_SYNDIC_REGLES("fraisSyndicRegles", "Frais syndic réglés", Groupe.FINANCIER, TypeColonne.MONTANT, false,
            null),
    FRAIS_SYNDIC_RESTE("fraisSyndicReste", "Reste syndic", Groupe.FINANCIER, TypeColonne.MONTANT, false, null),
    MODIFICATIONS("modifications", "Modifications", Groupe.FINANCIER, TypeColonne.MONTANT, false, null),
    MODIFICATIONS_REGLEES("modificationsReglees", "Modifications réglées", Groupe.FINANCIER, TypeColonne.MONTANT,
            false, null),
    MODIFICATIONS_RESTE("modificationsReste", "Modifications reste", Groupe.FINANCIER, TypeColonne.MONTANT, false,
            null),
    PENALITES("penalites", "Pénalités", Groupe.FINANCIER, TypeColonne.MONTANT, false, null),
    PENALITES_REGLES("penalitesRegles", "Pénalités réglées", Groupe.FINANCIER, TypeColonne.MONTANT, false, null),
    PENALITES_RESTE("penalitesReste", "Pénalités reste", Groupe.FINANCIER, TypeColonne.MONTANT, false, null),
    TROP_PERCU("tropPercu", "Trop perçu", Groupe.FINANCIER, TypeColonne.MONTANT, false, null),
    TROP_PERCU_REGLE("tropPercuRegle", "Trop perçu réglé", Groupe.FINANCIER, TypeColonne.MONTANT, false, null),
    TROP_PERCU_RESTE("tropPercuReste", "Trop perçu reste", Groupe.FINANCIER, TypeColonne.MONTANT, false, null);

    /** {@code ARTICLE} ≠ {@code PATRIMOINE} : {@link #exigeLePatrimoine} ferait payer trois jointures pour un code d'article. */
    public enum Groupe { IDENTITE, CLIENT, ACTEURS, FINANCIER, PRIX, PATRIMOINE, ARTICLE, DATES, DELAIS, ORIGINE,
        SUIVI, AUDIT }

    /** Type d'affichage, pour que le front aligne et formate sans table de correspondance. */
    public enum TypeColonne { TEXTE, NOMBRE, MONTANT, DATE, BOOLEEN }

    /** Identifiant du filtre Jackson qui porte la sélection de colonnes des dossiers. */
    public static final String FILTRE = "colonnesDossier";

    private final String nom;
    private final String libelle;
    private final Groupe groupe;
    private final TypeColonne type;
    private final boolean parDefaut;
    private final Chemin chemin;

    ColonneDossier(String nom, String libelle, Groupe groupe, TypeColonne type, boolean parDefaut,
                   Chemin chemin) {
        this.nom = nom;
        this.libelle = libelle;
        this.groupe = groupe;
        this.type = type;
        this.parDefaut = parDefaut;
        this.chemin = chemin;
    }

    @Override
    public String nom() {
        return nom;
    }

    @Override
    public String libelle() {
        return libelle;
    }

    @Override
    public TypeExport typeExport() {
        return switch (type) {
            case TEXTE -> TypeExport.TEXTE;
            case NOMBRE -> TypeExport.NOMBRE;
            case MONTANT -> TypeExport.MONTANT;
            case DATE -> TypeExport.DATE;
            case BOOLEEN -> TypeExport.BOOLEEN;
        };
    }

    public Groupe groupe() {
        return groupe;
    }

    public TypeColonne type() {
        return type;
    }

    @Override
    public boolean parDefaut() {
        return parDefaut;
    }

    @Override
    public Chemin chemin() {
        return chemin;
    }

    public static List<ColonneDossier> groupables() {
        return Colonnes.groupables(ColonneDossier.class);
    }

    public static List<ColonneDossier> agregeables() {
        return Colonnes.agregeables(ColonneDossier.class);
    }

    /** @throws ColonneInconnueException si le nom n'est pas une colonne groupable de ce catalogue. */
    public static ColonneDossier resoudreGroupement(String demandee) {
        return Colonnes.resoudreGroupement(ColonneDossier.class, demandee);
    }

    /** Forme {@code colonne} (somme) ou {@code colonne:FONCTION}. */
    public static List<AgregatDemande> resoudreAgregats(List<String> demandees) {
        return Colonnes.resoudreAgregats(ColonneDossier.class, demandees);
    }

    public static Set<ColonneDossier> parDefautSet() {
        return Colonnes.parDefaut(ColonneDossier.class);
    }

    public static Set<String> nomsParDefaut() {
        return Colonnes.nomsParDefaut(ColonneDossier.class);
    }

    public static List<String> nomsValides() {
        return Colonnes.nomsValides(ColonneDossier.class);
    }

    /** @throws ColonneInconnueException si un nom ne figure pas au catalogue. */
    public static Set<ColonneDossier> resoudre(List<String> demandees) {
        return Colonnes.resoudre(ColonneDossier.class, demandees, ID);
    }

    /** Sans ce test, la liste par défaut paierait trois jointures paresseuses par ligne. */
    public static boolean exigeLePatrimoine(Set<ColonneDossier> selection) {
        return selection.stream().anyMatch(c -> c.groupe == Groupe.PATRIMOINE);
    }

    /** Vrai si la sélection oblige à déréférencer la prospection d'origine. */
    public static boolean exigeLaProspection(Set<ColonneDossier> selection) {
        return selection.contains(ETAPE_PROSPECTION)
                || selection.contains(ORIGINE_PROSPECTION)
                || selection.contains(DATE_PROSPECTION)
                || selection.contains(SUPPORT_SOURCE)
                || selection.contains(ACTION_COMMERCIALE);
    }

    /** L'article et ses référentiels : jusqu'à cinq jointures paresseuses par ligne. Superficies : ComFolder. */
    public static boolean exigeLArticle(Set<ColonneDossier> selection) {
        return selection.stream().anyMatch(c -> c.groupe == Groupe.ARTICLE
                && c.chemin != null && c.chemin.cheminCle().startsWith("stkItem."));
    }
}

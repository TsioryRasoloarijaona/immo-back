# Référence — Aide en ligne OMRAN Estate (help.omranestate.com)

_Extraction intégrale du site d'aide du logiciel légataire, réalisée le 26/09/2026. 249 pages sur 334. Source : https://help.omranestate.com/ (HelpNDoc, éditeur YASMINE Engineering Systems)._


## Comment lire ce fichier

- **Une section par page du site**, dans l'ordre de la table des matières du site, avec le fil
  d'Ariane (`Module › Sous-module`) et le nom du fichier HTML d'origine (`PROSPECTIONS.html`).
  L'ancre de chaque page vaut `page-<nom-du-fichier-en-minuscules>` — les liens internes de
  l'aide sont réécrits vers ces ancres.
- **Le texte est restitué tel quel**, sans résumé ni reformulation : paragraphes, listes, tableaux,
  remarques. Les captures d'écran ne sont pas transcrites ; elles sont signalées par
  `[image : NomDuFichier.png]` à l'endroit où elles apparaissent.
- Les pages d'écran suivent presque toutes le même plan, à retrouver par les titres `### Liste`,
  `### Fiche`, `### Champs`, `### Actions` :
  - `Liste` — les colonnes du masque de liste. Colonne **V** = visible par défaut (`X`).
  - `Fiche` / `Champs` — les champs du masque d'édition. **U** = unique, **O** = obligatoire.
    La colonne `OBSERVATIONS` porte les règles de gestion (calculs, valeurs par défaut,
    listes déroulantes et leur page de configuration, effets de bord).
  - `Actions` — les boutons du masque et ce que chacun déclenche.
  - `Remarque n :` — règles de gestion transverses de l'écran. **Ce sont les phrases à
    reproduire** quand on réimplémente une fonctionnalité.
- Les règles générales à tout le logiciel (`GEN0001`…) sont sur la page « Règles de gestion
  générale » ; les pages d'écran y renvoient par identifiant.
- Un tableau imbriqué dans une cellule apparaît avec ses barres échappées (`\|`) : c'est une
  limite du Markdown, pas une perte de contenu.

## Ce que cette référence ne remplace pas

Le site décrit le **comportement attendu** du logiciel de bureau. Le comportement **réel**
se lit dans la base (`yesRemsProd` en local, source 115) et dans la trace d'audit
(`C:\backup\*.sqlaudit`) — voir `CLAUDE.md`. Quand les deux divergent, la trace fait foi ;
la divergence se note dans la spec `FEATURE-*.md`.


## Table des matières

- [Bienvenue dans OMRAN Estate](#page-index) `index.html`
  - [Démarrer avec OMRAN Estate](#page-demarreravecomranestate) `DemarreravecOMRANEstate.html`
- [Introduction](#page-introduction) `Introduction.html`
  - [A propos de OMRAN Estate](#page-aproposdeomranestate) `AproposdeOMRANEstate.html`
  - [Configuration du système](#page-configurationdusysteme) `Configurationdusysteme.html`
  - [Obtenir de l'aide](#page-obtenirdelaide1) `Obtenirdelaide1.html`
  - [Éditions d'OMRAN Estate](#page-editionsdomranestate) `EditionsdOMRANEstate.html`
  - [Contrat de Licence Utilisateur Final YASMINE ENGINEERING SYSTEMS OMRAN Estate](#page-contratdelicenceutilisateurfinal) `ContratdeLicenceUtilisateurFinal.html`
  - [Quoi de neuf dans OMRAN Estate](#page-quoideneufdansomranestate) `QuoideneufdansOMRANEstate.html`
- [Vue d'ensemble de l'interface utilisateur](#page-vuedensembledelinterfaceutilisat) `Vuedensembledelinterfaceutilisat.html`
  - [Authentification](#page-authentification) `Authentification.html`
  - [Page principale du logiciel](#page-pageprincipaledulogiciel) `Pageprincipaledulogiciel.html`
  - [Les masques de liste](#page-lesmasquesdeliste) `Lesmasquesdeliste.html`
  - [Les filtres génériques](#page-lesfiltresgeneriques) `Lesfiltresgeneriques.html`
  - [Les résumés génériques](#page-lesresumesgeneriques) `Lesresumesgeneriques.html`
  - [Les groupements](#page-lesgroupements) `Lesgroupements.html`
  - [Les sélecteurs de champs](#page-lesselecteursdechamps) `Lesselecteursdechamps.html`
  - [Les masques d’édition](#page-lesmasquesdedition) `Lesmasquesdedition.html`
  - [Les attachements de fichiers](#page-lesattachementsdefichiers) `Lesattachementsdefichiers.html`
  - [Les notes](#page-lesnotes) `Lesnotes.html`
  - [Règles de gestion générale](#page-reglesdegestiongenerale) `Reglesdegestiongenerale.html`
- [Guides de Démarrage Rapide](#page-guidesdedemarragerapide) `GuidesdeDemarrageRapide.html`
  - [Module Projet](#page-moduleprojet1) `ModuleProjet1.html`
  - [Module Stock](#page-modulestock1) `ModuleStock1.html`
  - [Module vente](#page-modulevente1) `Modulevente1.html`
  - [Étude de faisabilité](#page-etudedefaisabilite) `Etudedefaisabilite.html`
- [ACCUEIL](#page-accueil) `ACCUEIL.html`
- [Module Projet](#page-moduleprojet) `ModuleProjet.html`
  - [LOTS](#page-lots) `LOTS.html`
    - [PREVISIONS](#page-previsions-prj) `PREVISIONS_PRJ.html`
    - [BORDEREAU ESTIMATIF](#page-bordereauestimatif) `BORDEREAUESTIMATIF.html`
    - [APPEL D'OFFRE](#page-appeldoffre) `APPELDOFFRE.html`
    - [OFFRES DES ENTREPRISES](#page-offresdesentreprises) `OFFRESDESENTREPRISES.html`
    - [BORDEREAU DEFINTIF](#page-bordereaudefintif) `BORDEREAUDEFINTIF.html`
    - [DECOMPOSITION](#page-decomposition) `DECOMPOSITION.html`
    - [AVANCES](#page-avances) `AVANCES.html`
    - [DECOMPTE](#page-decompte) `DECOMPTE.html`
    - [AVENANTS](#page-avenants) `AVENANTS.html`
    - [RETENUES DE GARANTIE](#page-retenuesdegarantie) `RETENUESDEGARANTIE.html`
    - [AVOIRS](#page-avoirs-prj) `AVOIRS_PRJ.html`
  - [LOT MULTI FOURNISSEUR](#page-lotsmulti) `LOTSMULTI.html`
    - [PREVISIONS](#page-previsions-multi) `PREVISIONS_Multi.html`
    - [BORDEREAU ESTIMATIF](#page-bordereauestimatif-multi) `BORDEREAUESTIMATIF_Multi.html`
    - [OFFRES DES ENTREPRISES](#page-offresdesentreprises-multi) `OFFRESDESENTREPRISES_Multi.html`
  - [DECOMPTES](#page-decomptes) `DECOMPTES.html`
  - [REUNIONS DE CHANTIER](#page-reunionsdechantier) `REUNIONSDECHANTIER.html`
  - [MODIFICATIONS](#page-modifications) `MODIFICATIONS.html`
  - [PREVISIONS](#page-previsions) `PREVISIONS.html`
  - [GANTT](#page-gantt) `GANTT.html`
  - [TABLEAU DE BORDS](#page-tableaudebords-prj) `TABLEAUDEBORDS_PRJ.html`
    - [BILANS](#page-bilans) `BILANS.html`
    - [TRESORERIE](#page-tresorerie) `TRESORERIE.html`
    - [SUIVI DES LOTS](#page-suivideslots) `SUIVIDESLOTS.html`
    - [RAPPORTS](#page-rapports-prj) `RAPPORTS_PRJ.html`
      - [ETAT DES MARCHES](#page-etatdesmarches) `ETATDESMARCHES.html`
  - [CONFIGURATION](#page-configuration-prj) `CONFIGURATION_PRJ.html`
    - [TRANCHES](#page-tranches-prj) `TRANCHES_PRJ.html`
    - [NIVELLEMENTS](#page-nivellements) `NIVELLEMENTS.html`
    - [PROJETS](#page-projets) `PROJETS.html`
    - [STATUTS DES LOTS](#page-statutsdeslots) `STATUTSDESLOTS.html`
    - [TRANSITIONS](#page-prj-transitions) `PRJ_TRANSITIONS.html`
    - [TYPES LOT](#page-typeslot) `TYPESLOT.html`
    - [STATUTS DES RESERVES](#page-statutsdesreserves) `STATUTSDESRESERVES.html`
    - [PLAGES DES COULEURS](#page-plagesdescouleurs) `PLAGESDESCOULEURS.html`
    - [MODELES DE SIMULATION](#page-modelesdesimulation) `MODELESDESIMULATION.html`
    - [TYPES RECEPTION](#page-typesreception) `TYPESRECEPTION.html`
    - [NATURES RECEPTION](#page-naturesreception) `NATURESRECEPTION.html`
    - [FOURNISSEURS](#page-fournisseurs-prj) `FOURNISSEURS_PRJ.html`
    - [CONTACTS](#page-contacts-prj) `CONTACTS_PRJ.html`
- [Module Achat](#page-moduleachat) `ModuleAchat.html`
  - [CONSULTATIONS](#page-consultations) `CONSULTATIONS.html`
    - [BORDEREAU ESTIMATIF](#page-bordereauestimatif-c) `BORDEREAUESTIMATIF_C.html`
    - [APPEL D'OFFRE](#page-appeldoffre-c) `APPELDOFFRE_C.html`
    - [OFFRES DES ENTREPRISES](#page-offresdesentreprises1) `OFFRESDESENTREPRISES1.html`
    - [BORDEREAU DEFINITIF](#page-bordereaudefintif-c) `BORDEREAUDEFINTIF_c.html`
  - [DEVIS](#page-devis) `DEVIS.html`
  - [BONS DE COMMANDE](#page-bonsdecommande) `BONSDECOMMANDE.html`
  - [BONS DE LIVRAISON](#page-bonsdelivraison) `BONSDELIVRAISON.html`
  - [FACTURES](#page-factures) `FACTURES.html`
  - [DOCUMENTS A REGLER](#page-documentsaregler) `DOCUMENTSAREGLER.html`
  - [AVOIRS](#page-avoirs) `AVOIRS.html`
  - [REGLEMENTS](#page-reglements) `REGLEMENTS.html`
  - [LISTE DES IMPAYES](#page-listedesimpayes) `LISTEDESIMPAYES.html`
  - [DOCUMENTS](#page-documents) `DOCUMENTS.html`
  - [CREDITS](#page-credits) `CREDITS.html`
    - [GESTION DES PRETS](#page-gestiondesprets) `GESTIONDESPRETS.html`
    - [ECHEANCES CREDITS](#page-echeancescredits) `ECHEANCESCREDITS.html`
  - [TABLEAUX DE BORDS](#page-tableauxdebords-buy) `TABLEAUXDEBORDS_BUY.html`
    - [SOLDE FOURNISSEURS](#page-soldefournisseurs) `SOLDEFOURNISSEURS.html`
    - [ETAT DE LA TVA DEDUCTIBLE](#page-etatdelatvadeductible) `ETATDELATVADEDUCTIBLE.html`
    - [ETAT DE LA RS](#page-etatdelars) `ETATDELARS.html`
    - [RAPPORTS](#page-rapports-buy) `RAPPORTS_BUY.html`
      - [SUIVI DES DEPENSES PAR MOIS](#page-buytdbactivitybymoth) `BuyTdbActivityByMoth.html`
      - [SUIVI DES RETENUES](#page-buytdbmarketsretainedamounts) `BuyTdbMarketsRetainedAmounts.html`
      - [ECHEANCIER DE REMBOURSSEMENT DES CREDITS](#page-buytdbloanschedulebyyear) `BuyTdbLoanScheduleByYear.html`
  - [CONFIGURATION](#page-configuration-buy) `CONFIGURATION_BUY.html`
    - [STATUTS](#page-statuts-buy) `STATUTS_BUY.html`
    - [TRANSITIONS](#page-transitions-buy) `TRANSITIONS_BUY.html`
    - [DOSSIERS DE GROUPEMENT](#page-dossiersdegroupement) `DOSSIERSDEGROUPEMENT.html`
    - [NOMENCLATURE](#page-nomenclature) `NOMENCLATURE.html`
    - [TYPES DE CREDIT](#page-typesdecredit) `TYPESDECREDIT.html`
    - [FOURNISSEURS](#page-fournisseurs-buy) `FOURNISSEURS_BUY.html`
- [Module Vente](#page-modulevente) `ModuleVente.html`
  - [PREPROSPECTIONS](#page-preprospections) `PREPROSPECTIONS.html`
  - [PREVISIONS](#page-previsions-com) `PREVISIONS_COM.html`
  - [HOT LINE](#page-hotline) `HOTLINE.html`
  - [PROSPECT EXPRESS](#page-prospectexpress) `PROSPECTEXPRESS.html`
  - [PROSPECTS](#page-prospects) `PROSPECTS.html`
  - [PROSPECTIONS](#page-prospections) `PROSPECTIONS.html`
  - [CLIENTS](#page-clients) `CLIENTS.html`
  - [PLANS](#page-plans) `PLANS.html`
  - [DOSSIERS DE VENTE](#page-dossiersdevente) `DOSSIERSDEVENTE.html`
  - [TRANSFERTS PRODUIT](#page-transfertsproduit) `TRANSFERTSPRODUIT.html`
  - [TRANSFERTS CLIENT](#page-transfertsclient) `TRANSFERTSCLIENT.html`
  - [DESISTEMENTS](#page-desistements) `DESISTEMENTS.html`
  - [IMPAYES](#page-impayes) `IMPAYES.html`
  - [REGLEMENTS](#page-reglements-com) `REGLEMENTS_COM.html`
  - [PAIEMENTS](#page-paiements) `PAIEMENTS.html`
  - [LISTE A FAIRE](#page-listeafaire) `LISTEAFAIRE.html`
  - [COMMISSIONS](#page-commissions) `COMMISSIONS.html`
  - [TABLEAUX DE BORDS](#page-tableauxdebords-com) `TABLEAUXDEBORDS_COM.html`
    - [SUIVI DES PREVISIONS](#page-suividesprevisions) `SUIVIDESPREVISIONS.html`
    - [SUIVI DES COMMERCIAUX](#page-suividescommerciaux) `SUIVIDESCOMMERCIAUX.html`
    - [SUIVI DES ACTIONS](#page-suividesactions) `SUIVIDESACTIONS.html`
    - [SUIVI DES DOSSIERS DE VENTE](#page-suividesdossiersdevente) `SUIVIDESDOSSIERSDEVENTE.html`
    - [RAPPORTS](#page-rapports-com) `RAPPORTS_COM.html`
      - [LISTE DOSSIERS](#page-comtdbfolderwithclientinfos) `ComTdbFolderWithClientInfos.html`
      - [SUVI HEBDOMADAIRE](#page-comtdbweeklymonitoring) `ComTdbWeeklyMonitoring.html`
      - [COMPARAISON DES VENTES](#page-comtdbsalecomparaison) `ComTdbSaleComparaison.html`
      - [SUVI HEBDOMADAIRE DES PREVISIONS](#page-comtdbsuivihebdoprevision) `ComTdbSuiviHebdoPrevision.html`
      - [ACTIVITE COMMERCIALE PAR MOIS ET PAR ANNEE](#page-comtdbactiviteparmois) `ComTdbActiviteParMois.html`
      - [ACTIVITE COMMERCIALE PAR PROJET ET PAR ANNEE](#page-comtdbactiviteparprojet) `ComTdbActiviteParProjet.html`
      - [RYTHME DES VENTES](#page-comtdbsalesrythme) `ComTdbSalesRythme.html`
      - [RYTHME DE CONCRETISATIONS/LIVRAIONS/ENCAISSEMENTS](#page-comtdbsalesrythmeconcretisation) `ComTdbSalesRythmeConcretisation.html`
      - [RYTHME DE CONCRETISATIONS/LIVRAIONS/ENCAISSEMENTS(COMPARAISON)](#page-comtdbsalesrythmelivraison) `ComTdbSalesRythmeLivraison.html`
      - [SUIVI CHIFFRE D'AFFAIRES](#page-suivichiffredaffaires) `SUIVICHIFFREDAFFAIRES.html`
      - [DETAIL CHIFFRE D'AFFAIRES](#page-comtdbdetailschiffreaffaire) `ComTdbDetailsChiffreAffaire.html`
      - [SITUATION GLOBALE DES VENTES](#page-comtdbsalessituationglobal) `ComTdbSalesSituationGlobal.html`
      - [SUIVI GLOBALE DES VENTES](#page-comtdbsalesmonitoringglobal) `ComTdbSalesMonitoringGlobal.html`
      - [CONCRETISATION PAR RAPPORT AUX LIBRE](#page-comtdbconcretisationandfree) `ComTdbConcretisationAndFree.html`
      - [CONCRETISATION ET ENCAISSEMENT](#page-comtdbconcretandsetlement) `ComTdbConcretAndSetlement.html`
      - [CONCRETISATION PAR COMMERCIAL](#page-comtdbconcretpercommercial) `ComTdbConcretPerCommercial.html`
      - [SUIVI DES PREVISIONS DE VENTE](#page-comtdbprevisionstate) `ComTdbPrevisionState.html`
      - [TAUX DE TRANSFORMATION PAR COMMERCIAL](#page-comtdbcommercialtransformation) `ComTdbCommercialTransformation.html`
      - [REPARTION DES CLIENTS ](#page-comtdbrepartitionclient) `ComTdbRepartitionClient.html`
      - [ETAT DES VENTE](#page-comtdbsalesstate) `ComTdbSalesState.html`
      - [SUIVI DES CADEAUX](#page-comtdbgift) `ComTdbGift.html`
      - [SUIVI DES COMMERCIAUX](#page-comtdbcommercialstate) `ComTdbCommercialState.html`
      - [SUIVI DES PROSPECTIONS](#page-comtdbprospectionstate) `ComTdbProspectionState.html`
      - [PROSPECTS PAR PERIODE](#page-comtdbprospectspercommercial) `ComTdbProspectsPerCommercial.html`
      - [TAUX DE VENTE PAR RAPPORT AU STOCK](#page-comtdbsalestockratio) `ComTdbSaleStockRatio.html`
      - [TAUX DE TRONSFORMATION PAR NATURE D’ACTIONS](#page-comtdbcommercialtransformationby) `ComTdbCommercialTransformationBy.html`
      - [TAUX DE CONCRETISATION PAR ANNEE/TRANCHE](#page-comsaletauxconcretisationparanne) `ComSaleTauxConcretisationParAnne.html`
      - [SUIVI NOTAIRES CRÉDITS](#page-comsaledashboardsuivinotairescre) `ComSaleDashboardSuiviNotairesCre.html`
      - [ANALYSE DELAIS DE LIVRAISONS](#page-comsaledashboarddeliverydelay) `ComSaleDashboardDeliveryDelay.html`
    - [SUIVI DES AFFECTATIONS](#page-suividesaffectations) `SUIVIDESAFFECTATIONS.html`
  - [CONFIGURATION](#page-configuration-com) `CONFIGURATION_COM.html`
    - [PROSPECTION](#page-prospection) `PROSPECTION.html`
      - [SOURCE DE CONTACT](#page-sourcedecontact) `SOURCEDECONTACT.html`
      - [SUPPORTS SOURCE DE CONTACT](#page-supportssourcedecontact) `SUPPORTSSOURCEDECONTACT.html`
      - [NATURES PROSPECTIONS](#page-naturesprospections) `NATURESPROSPECTIONS.html`
      - [ACTIONS COMMERCIALES](#page-actionscommerciales) `ACTIONSCOMMERCIALES.html`
      - [RAISON D'ARCHIVAGE DES PROSPECTIONS](#page-raisondarchivagedesprospections) `RAISONDARCHIVAGEDESPROSPECTIONS.html`
    - [VENTE](#page-vente) `VENTE.html`
      - [STATUTS](#page-statuts-com) `STATUTS_COM.html`
      - [TRESORERIE](#page-tresorerie-com) `TRESORERIE_COM.html`
      - [MOTIFS DE CHANGEMENT](#page-motifsdechangement) `MOTIFSDECHANGEMENT.html`
      - [CADEAUX](#page-cadeaux) `CADEAUX.html`
      - [TRANSITIONS](#page-transitions-com) `TRANSITIONS_COM.html`
      - [CATEGORIES DES ACTIONS](#page-categoriesdesactions) `CATEGORIESDESACTIONS.html`
      - [SOUS-CATEGORIES DES ACTIONS](#page-souscategoriesdesactions) `SOUSCATEGORIESDESACTIONS.html`
      - [TYPES DES ACTIONS](#page-typesdesactions) `TYPESDESACTIONS.html`
      - [DECISIONS MEILLEURTAUX](#page-decisionsmeilleurtaux) `DECISIONSMEILLEURTAUX.html`
      - [RAISONS DE CADEAUX](#page-raisonsdecadeaux) `RAISONSDECADEAUX.html`
- [Module Service Après Vente](#page-moduleserviceapresvente) `ModuleServiceApresVente.html`
  - [DOSSIERS](#page-dossiers-sav) `DOSSIERS_SAV.html`
  - [RECLAMATIONS](#page-reclamations) `RECLAMATIONS.html`
  - [PLANNING](#page-planning) `PLANNING.html`
  - [TABLEAUX DE BORDS](#page-tableauxdebords-sav) `TABLEAUXDEBORDS_SAV.html`
    - [RAPPORTS](#page-rapports-sav) `RAPPORTS_SAV.html`
      - [SUIVI DES RECLAMATIONS](#page-savtdbclaimstate) `SavTdbClaimState.html`
      - [ETAT GLOBAL DES INTERVENTIONS](#page-savtdbinterventionstate) `SavTdbInterventionState.html`
  - [CONFIGURATION](#page-configuration-sav) `CONFIGURATION_SAV.html`
    - [CATEGORIES DE RECLAMATION](#page-categoriesdereclamation) `CATEGORIESDERECLAMATION.html`
    - [STATUTS](#page-statuts-sav) `STATUTS_SAV.html`
    - [TRANSITIONS](#page-transitions-sav) `TRANSITIONS_SAV.html`
    - [ORIGINES](#page-origines) `ORIGINES.html`
    - [CONTENANCES](#page-contenances-sav) `CONTENANCES_SAV.html`
- [Module Location](#page-modulelocation) `ModuleLocation.html`
  - [CLIENTS](#page-clientsrnt) `CLIENTSRNT.html`
  - [PROSPECTS](#page-prospectsrnt) `PROSPECTSRNT.html`
  - [CONTACTS](#page-contacts) `CONTACTS.html`
  - [PROSPECTIONS](#page-rnt-prospections) `RNT_PROSPECTIONS.html`
  - [DOSSIERS DE LOCATION](#page-rnt-folder) `RNT_FOLDER.html`
  - [DOSSIERS DE SYNDIC](#page-syn-stock) `SYN_STOCK.html`
  - [APPEL D'ECHEANCES](#page-appeldeloyers) `APPELDELOYERS.html`
  - [FACTURES PROFORMA](#page-facture-proforma-location) `FACTURE_PROFORMA_LOCATION.html`
  - [FACTURES](#page-facture-location) `FACTURE_LOCATION.html`
  - [AVOIRS](#page-avoir-location) `AVOIR_LOCATION.html`
  - [REGLEMENTS](#page-reglements-rnt) `REGLEMENTS_RNT.html`
  - [LISTE DES IMPAYEES](#page-rnt-impayes) `RNT_IMPAYES.html`
  - [TABLEAUX DE BORDS](#page-tableauxdebords) `TABLEAUXDEBORDS.html`
    - [SOLDES LOCATAIRES](#page-soldeslocataires) `SOLDESLOCATAIRES.html`
    - [SUIVI DES DOSSIERS DE LOCATIONS](#page-suividesdossiersdelocations) `SUIVIDESDOSSIERSDELOCATIONS.html`
    - [SUIVI DES DOSSIERS DE SYNDIC](#page-suividesdossiersdesyndic) `SUIVIDESDOSSIERSDESYNDIC.html`
    - [RAPPORTS](#page-rapports) `RAPPORTS.html`
    - [SUIVI DES AFFECTATIONS](#page-suividesaffectations1) `SUIVIDESAFFECTATIONS1.html`
  - [CONFIGURATION](#page-configuration) `CONFIGURATION.html`
    - [PROSPECTIONS](#page-prospections1) `PROSPECTIONS1.html`
      - [NATURES](#page-rnt-naturesprospections) `RNT_NATURESPROSPECTIONS.html`
      - [SOURCES DE CONTACT](#page-rnt-sourcedecontact) `RNT_SOURCEDECONTACT.html`
      - [SUPPORTS SOURCE DE CONTACT](#page-rnt-supportssourcedecontact) `RNT_SUPPORTSSOURCEDECONTACT.html`
      - [ACTIONS COMMERCIALES](#page-rnt-actionscommerciales) `RNT_ACTIONSCOMMERCIALES.html`
    - [LOCATION](#page-location) `LOCATION.html`
      - [STATUTS](#page-statuts-rnt) `STATUTS_RNT.html`
      - [TRANSITIONS](#page-transitions-rnt) `TRANSITIONS_RNT.html`
      - [TRESORERIE](#page-tresorerie-rnt) `TRESORERIE_RNT.html`
    - [SYNDIC](#page-syndic) `SYNDIC.html`
      - [STATUT](#page-statuts-rnt-syn) `STATUTS_RNT_SYN.html`
      - [TRANSITIONS](#page-transitions-rnt-syn) `TRANSITIONS_RNT_SYN.html`
      - [TARIFICATION DU SYNDIC](#page-syn-pricing) `SYN_PRICING.html`
- [Module Trésorerie](#page-moduletresorerie) `ModuleTresorerie.html`
  - [ETAT DES FLUX](#page-etatdesflux) `ETATDESFLUX.html`
  - [AFFECTATION ET POINTAGE](#page-affectationetpointage) `AFFECTATIONETPOINTAGE.html`
  - [RECETTE](#page-recette) `RECETTE.html`
  - [DEPENSES](#page-depenses) `DEPENSES.html`
  - [TRANSFERTS INTERNES](#page-transfertsinternes) `TRANSFERTSINTERNES.html`
  - [PREVISIONS](#page-previsions-stl) `PREVISIONS_STL.html`
  - [RAPPROCHEMENTS BANCAIRE](#page-rapprochementsbancaire) `RAPPROCHEMENTSBANCAIRE.html`
  - [TABLEAU DE BORDS](#page-tableaudebords-stl) `TABLEAUDEBORDS_STL.html`
    - [ TRESORERIE GENERALE](#page-tresoreriegenerale) `TRESORERIEGENERALE.html`
    - [TRESORERIE ANNUELLE](#page-tresorerieannuelle) `TRESORERIEANNUELLE.html`
    - [BILAN DES PROJETS](#page-bilandesprojets) `BILANDESPROJETS.html`
    - [RETENUE A LA SOURCE](#page-retenuealasource) `RETENUEALASOURCE.html`
  - [CONFIGURATION](#page-configuration-stl) `CONFIGURATION_STL.html`
    - [NATURES COMPTE](#page-naturescompte) `NATURESCOMPTE.html`
    - [TYPES PIECE](#page-typespiece) `TYPESPIECE.html`
    - [COMPTES](#page-comptes) `COMPTES.html`
    - [CATEGORIES DES RECETTES](#page-categoriesdesrecettes) `CATEGORIESDESRECETTES.html`
    - [CATEGORIE DES DEPENSES](#page-categoriedesdepenses) `CATEGORIEDESDEPENSES.html`
    - [RELATION AVEC AUTRE MODULES](#page-relationavecautremodules) `RELATIONAVECAUTREMODULES.html`
- [Module Stock](#page-modulestock) `ModuleStock.html`
  - [ENTITES JURIDIQUE](#page-entitesjuridique) `ENTITESJURIDIQUE.html`
  - [PROSPECTIONS TERRAIN](#page-prospectionsterrain) `PROSPECTIONSTERRAIN.html`
  - [TERRAINS](#page-terrains) `TERRAINS.html`
  - [ETUDES DE FAISABILITE](#page-cfg-etudesdefaisabilite) `CFG_ETUDESDEFAISABILITE.html`
  - [PROGRAMMES](#page-cfg-programmes) `CFG_PROGRAMMES.html`
  - [TRANCHES](#page-cfg-tranches) `Cfg_TRANCHES.html`
  - [ARTICLES](#page-articles) `ARTICLES.html`
  - [SOUS ARTICLES](#page-sousarticles) `SOUSARTICLES.html` _(non téléchargée)_
  - [LOTISSEMENTS](#page-lotissements) `LOTISSEMENTS.html` _(non téléchargée)_
  - [TABLEAUX DE BORDS](#page-tableauxdebords-stk) `TABLEAUXDEBORDS_STK.html` _(non téléchargée)_
    - [RAPPORT](#page-stk-reports) `STK_REPORTS.html` _(non téléchargée)_
      - [VALORISATION DU STOCK](#page-stktdbvalorisation) `StkTdbValorisation.html` _(non téléchargée)_
      - [MOUVEMENTS DU STOCK](#page-stktdbmotion) `StkTdbMotion.html` _(non téléchargée)_
      - [COMPARATIF PRIX AU M²](#page-stktdbcomparatifaumettrecarre) `StkTdbComparatifAuMettreCarre.html` _(non téléchargée)_
  - [CONFIGURATION](#page-configuration1) `CONFIGURATION1.html` _(non téléchargée)_
    - [PROSPECTIONS DE TERRAINS](#page-prospectionsdeterrains) `PROSPECTIONSDETERRAINS.html` _(non téléchargée)_
      - [VOCATION](#page-lnd-vocation) `LND_VOCATION.html` _(non téléchargée)_
      - [NIVEAU](#page-lnd-niveau) `LND_NIVEAU.html` _(non téléchargée)_
      - [STATUTS](#page-lnd-statuts) `LND_STATUTS.html` _(non téléchargée)_
      - [TRANSITIONS](#page-lnd-transitions) `LND_TRANSITIONS.html` _(non téléchargée)_
      - [UNITES DE BASE](#page-stk-unitesdebase) `STK_UNITESDEBASE.html` _(non téléchargée)_
      - [STANDINGS](#page-stk-standings) `STK_STANDINGS.html` _(non téléchargée)_
      - [FONCTIONS SPECIFIQUES](#page-fonctionsspecifiques) `FONCTIONSSPECIFIQUES.html` _(non téléchargée)_
    - [ARTICLES](#page-articles-cfg) `ARTICLES_CFG.html` _(non téléchargée)_
      - [CATEGORIES](#page-stk-categories-item) `STK_CATEGORIES_ITEM.html` _(non téléchargée)_
      - [CATEGORIES DE CHARGE](#page-stk-categoriesdecharge) `STK_CATEGORIESDECHARGE.html` _(non téléchargée)_
      - [CONTENANCES](#page-stk-contenances) `STK_CONTENANCES.html` _(non téléchargée)_
      - [TYPES](#page-stk-types-item) `STK_TYPES_ITEM.html` _(non téléchargée)_
      - [TYPES DE PRESENTATION](#page-stk-typesdepresentation) `STK_TYPESDEPRESENTATION.html` _(non téléchargée)_
      - [ORIENTATIONS](#page-stk-orientations) `STK_ORIENTATIONS.html` _(non téléchargée)_
      - [IMMEUBLES](#page-stk-immeubles) `STK_IMMEUBLES.html` _(non téléchargée)_
      - [ETAGES](#page-stk-etages) `STK_ETAGES.html` _(non téléchargée)_
      - [TYPES DE FRAIS](#page-typesdefrais) `TYPESDEFRAIS.html` _(non téléchargée)_
      - [VOCATIONS](#page-stk-vocations-item) `STK_VOCATIONS_ITEM.html` _(non téléchargée)_
      - [NATURES](#page-stk-natures) `STK_NATURES.html` _(non téléchargée)_
      - [FACADES](#page-facades) `FACADES.html` _(non téléchargée)_
      - [VUES](#page-vues) `VUES.html` _(non téléchargée)_
    - [GROUPE D'ACTIONNAIRES](#page-groupedactionnaires) `GROUPEDACTIONNAIRES.html` _(non téléchargée)_
- [Module Administration](#page-moduleadministration) `ModuleAdministration.html` _(non téléchargée)_
  - [DROITS](#page-droits) `DROITS.html` _(non téléchargée)_
    - [ROLES](#page-roles) `ROLES.html` _(non téléchargée)_
    - [UTILISATEURS](#page-utilisateurs) `UTILISATEURS.html` _(non téléchargée)_
    - [HISTORIQUE DES ACTIONS](#page-historiquedesactions) `HISTORIQUEDESACTIONS.html` _(non téléchargée)_
    - [FICHES VERROUILLEES](#page-fichesverrouillees) `FICHESVERROUILLEES.html` _(non téléchargée)_
    - [HISTORIQUE DES CONVERSATIONS](#page-historiquedesconversations) `HISTORIQUEDESCONVERSATIONS.html` _(non téléchargée)_
  - [TIERS](#page-tiers) `TIERS.html` _(non téléchargée)_
    - [FOURNISSEURS](#page-fournisseurs-adm) `FOURNISSEURS_ADM.html` _(non téléchargée)_
    - [BANQUES](#page-banques) `BANQUES.html` _(non téléchargée)_
    - [ORGANISMES](#page-organismes) `ORGANISMES.html` _(non téléchargée)_
    - [PERSONNELS](#page-personnels) `PERSONNELS.html` _(non téléchargée)_
    - [CONTACTS](#page-contacts-adm) `CONTACTS_ADM.html` _(non téléchargée)_
  - [CONFIGURATION](#page-configuration-adm) `CONFIGURATION_ADM.html` _(non téléchargée)_
    - [CHAMPS SPECIFIQUES](#page-champsspecifiques) `CHAMPSSPECIFIQUES.html` _(non téléchargée)_
    - [LOCALISATIONS](#page-localisations) `LOCALISATIONS.html` _(non téléchargée)_
    - [PARAMETRES SYSTEME](#page-parametressysteme) `PARAMETRESSYSTEME.html` _(non téléchargée)_
    - [RAPPORTS](#page-rapports-adm) `RAPPORTS_ADM.html` _(non téléchargée)_
    - [MAIL MODELS](#page-mailmodels) `MAILMODELS.html` _(non téléchargée)_
    - [SEQUENCES](#page-sequences) `SEQUENCES.html` _(non téléchargée)_
    - [IMAGES](#page-images) `IMAGES.html` _(non téléchargée)_
    - [PAYS](#page-pays) `PAYS.html` _(non téléchargée)_
    - [VILLES](#page-villes) `VILLES.html` _(non téléchargée)_
    - [ACTIONNAIRES](#page-actionnaires) `ACTIONNAIRES.html` _(non téléchargée)_
    - [TYPES DES ATTACHEMENTS](#page-typesdesattachements) `TYPESDESATTACHEMENTS.html` _(non téléchargée)_
    - [ENTITES FONCTIONNELLES](#page-entitesfonctionnelles) `ENTITESFONCTIONNELLES.html` _(non téléchargée)_
    - [STATUTS DES RELANCES](#page-statutsdesrelances) `STATUTSDESRELANCES.html` _(non téléchargée)_
    - [TIERS](#page-tiers-cfg) `TIERS_CFG.html` _(non téléchargée)_
      - [SEXES](#page-sexes) `SEXES.html` _(non téléchargée)_
      - [TITRES](#page-titres) `TITRES.html` _(non téléchargée)_
      - [FORMES SOCIALE](#page-formessociale) `FORMESSOCIALE.html` _(non téléchargée)_
      - [TYPES](#page-types) `TYPES.html` _(non téléchargée)_
      - [NATURES](#page-natures) `NATURES.html` _(non téléchargée)_
      - [ASSUJETISSEMENTS](#page-assujetissements) `ASSUJETISSEMENTS.html` _(non téléchargée)_
      - [SITUATIONS FAMILIALE](#page-situationsfamiliale) `SITUATIONSFAMILIALE.html` _(non téléchargée)_
      - [TYPES RESIDENCE](#page-typesresidence) `TYPESRESIDENCE.html` _(non téléchargée)_
      - [REGIMES MATRIMONIAL](#page-regimesmatrimonial) `REGIMESMATRIMONIAL.html` _(non téléchargée)_
      - [TYPES PIECE IDENTITE](#page-typespieceidentite) `TYPESPIECEIDENTITE.html` _(non téléchargée)_
      - [NATIONALITES](#page-nationalites) `NATIONALITES.html` _(non téléchargée)_
      - [PROFESSIONS](#page-professions) `PROFESSIONS.html` _(non téléchargée)_
      - [SALAIRES](#page-salaires) `SALAIRES.html` _(non téléchargée)_
      - [TYPES DE RELATION](#page-typesderelation) `TYPESDERELATION.html` _(non téléchargée)_
  - [TABLEAUX DE BORDS](#page-tableauxdebords-adm) `TABLEAUXDEBORDS_ADM.html` _(non téléchargée)_
- [Module Comptabilité](#page-modulecomptabilite) `ModuleComptabilite.html` _(non téléchargée)_
  - [PARAMETRAGES](#page-parametrages) `PARAMETRAGES.html` _(non téléchargée)_
  - [DOCUMENTS A COMPTABILISER](#page-documentsacomptabiliser) `DOCUMENTSACOMPTABILISER.html` _(non téléchargée)_
  - [ECRITURES](#page-ecritures) `ECRITURES.html` _(non téléchargée)_
  - [CONFIGURATIONS](#page-configurations) `CONFIGURATIONS.html` _(non téléchargée)_
    - [PLAN COMPTABLE TYPES](#page-plancomptabletypes) `PLANCOMPTABLETYPES.html` _(non téléchargée)_
    - [PLAN COMPTABLE](#page-plancomptable) `PLANCOMPTABLE.html` _(non téléchargée)_
    - [COMPTES PAR DEFAUT](#page-comptespardefaut) `COMPTESPARDEFAUT.html` _(non téléchargée)_
    - [JOURNAUX TYPES](#page-journauxtypes) `JOURNAUXTYPES.html` _(non téléchargée)_
    - [JOURNAUX](#page-journaux) `JOURNAUX.html` _(non téléchargée)_
- [Module Notification](#page-modulenotification) `ModuleNotification.html` _(non téléchargée)_

## Index des mots-clés du site

_L'index alphabétique du site : chaque entrée renvoie aux pages qui la traitent._

- **GEN0023** — [Règles de gestion générale](#page-reglesdegestiongenerale)
- **Module** — [COMPARAISON DES VENTES](#page-comtdbsalecomparaison), [SUVI HEBDOMADAIRE](#page-comtdbweeklymonitoring), [SUVI HEBDOMADAIRE DES PREVISIONS](#page-comtdbsuivihebdoprevision), [ACTIVITE COMMERCIALE PAR MOIS ET PAR ANNEE](#page-comtdbactiviteparmois), [ACTIVITE COMMERCIALE PAR PROJET ET PAR ANNEE](#page-comtdbactiviteparprojet), [RYTHME DES VENTES](#page-comtdbsalesrythme), [RYTHME DE CONCRETISATIONS/LIVRAIONS/ENCAISSEMENTS](#page-comtdbsalesrythmeconcretisation), [.html](#page-), [SUIVI CHIFFRE D'AFFAIRES](#page-suivichiffredaffaires), [DETAIL CHIFFRE D'AFFAIRES](#page-comtdbdetailschiffreaffaire), [SITUATION GLOBALE DES VENTES](#page-comtdbsalessituationglobal), [SUIVI GLOBALE DES VENTES](#page-comtdbsalesmonitoringglobal)
- **Tableaux de bords** — [COMPARAISON DES VENTES](#page-comtdbsalecomparaison), [SUVI HEBDOMADAIRE](#page-comtdbweeklymonitoring), [SUVI HEBDOMADAIRE DES PREVISIONS](#page-comtdbsuivihebdoprevision), [ACTIVITE COMMERCIALE PAR MOIS ET PAR ANNEE](#page-comtdbactiviteparmois), [ACTIVITE COMMERCIALE PAR PROJET ET PAR ANNEE](#page-comtdbactiviteparprojet), [RYTHME DES VENTES](#page-comtdbsalesrythme), [RYTHME DE CONCRETISATIONS/LIVRAIONS/ENCAISSEMENTS](#page-comtdbsalesrythmeconcretisation), [.html](#page-), [SUIVI CHIFFRE D'AFFAIRES](#page-suivichiffredaffaires), [DETAIL CHIFFRE D'AFFAIRES](#page-comtdbdetailschiffreaffaire), [SITUATION GLOBALE DES VENTES](#page-comtdbsalessituationglobal), [SUIVI GLOBALE DES VENTES](#page-comtdbsalesmonitoringglobal)
- **Vente** — [SUVI HEBDOMADAIRE](#page-comtdbweeklymonitoring), [COMPARAISON DES VENTES](#page-comtdbsalecomparaison), [SUVI HEBDOMADAIRE DES PREVISIONS](#page-comtdbsuivihebdoprevision), [ACTIVITE COMMERCIALE PAR MOIS ET PAR ANNEE](#page-comtdbactiviteparmois), [ACTIVITE COMMERCIALE PAR PROJET ET PAR ANNEE](#page-comtdbactiviteparprojet), [RYTHME DES VENTES](#page-comtdbsalesrythme), [RYTHME DE CONCRETISATIONS/LIVRAIONS/ENCAISSEMENTS](#page-comtdbsalesrythmeconcretisation), [.html](#page-), [SUIVI CHIFFRE D'AFFAIRES](#page-suivichiffredaffaires), [DETAIL CHIFFRE D'AFFAIRES](#page-comtdbdetailschiffreaffaire), [SITUATION GLOBALE DES VENTES](#page-comtdbsalessituationglobal), [SUIVI GLOBALE DES VENTES](#page-comtdbsalesmonitoringglobal)

---

# Pages


## Bienvenue dans OMRAN Estate <a id="page-index"></a>
_Page : `index.html`_

[image : NouvelElement35.png]
Le logiciel OMRAN Estate est une solution globale et intégrée qui couvre l'ensemble des aspects de la gestion de la promotion immobilière. Depuis l'étude d'une opportunité de terrain jusqu'à la réalisation, le suivi et la commercialisation d’un projet. C’est une solution modulaire, ergonomique et intuitive avec une prise en main très facile et une implémentation rapide et efficace.

- Cette solution intègre une gestion électronique des documents. Documents attachés en un click : brochures, plans, photos, promesse de vente, contrat de vente, acte …
- Une solution qui colle à vos besoins, qui fonctionne comme vous et qui va profondément dans le sens de votre flux de travail réel. Elle assure la disponibilité de l’information en temps réel pour une meilleure aide à la prise de décision.
- La modélisation et l’intégration des normes et des règles de gestion permettent de s’assurer du respect des procédures par l’ensemble des intervenants. Ainsi OMRAN Estate couvre non seulement la fonction d’un Système d’information décisionnel, mais sert aussi d’outil de communication et de contrôle.
Avertissement : ce logiciel est protégé par les droits d’auteur et les traités internationaux. Toutes reproduction non autorisé de ce logiciel, ou d’une partie de celui-ci, entraîne de sévères sanctions civiles et pénales et sera poursuivi par toutes les mesures possibles en vertu de la loi.
Le logiciel OMRAN Estate est la propriété de YASMINE ENGINEERING SYSTEMS, société à responsabilité limitée (S.A.R.L.) au capital de 50 000 Dinars tunisien, immatriculée au registre du commerce du Tribunal de Première Instance de l’Ariana, sous le n° B0355702006, immatriculée sous le N° 000MA918388/K.

### Editeur
[image : NouvelElement39.png]
YASMINE ENGINEERING SYSTEMS
BP 103 - Pôle Technologique El Ghazela 2088 Ariana – Tunisie
R.C. B0355702006
M.F. 000MA918388/K
Tél : +216 71 859 283
Fax : +216 71 859 278
contact@yasminees.com
[www.yasminees.com](http://www.yasminees.com)
Créé avec HelpNDoc Personal Edition: [Créer de la documentation iPhone facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites/generation-de-site-web-iphone)

### Démarrer avec OMRAN Estate <a id="page-demarreravecomranestate"></a>
_Page : `DemarreravecOMRANEstate.html` — Bienvenue dans OMRAN Estate_

Nouveautés de OMRAN Estate

- Consultez la section [Introduction](#page-introduction) pour en savoir plus sur OMRAN Estate, sur ses différentes éditions et sur la configuration système nécessaire.
- Consultez la section [Vue d'ensemble de l'interface utilisateur](#page-vuedensembledelinterfaceutilisat) pour comprendre l'organisation et l'interaction avec l'interface du logiciel.
- Suivez les [Guides de Démarrage Rapide](#page-guidesdedemarragerapide) pour vous familiariser avec les processus de création et de production de vos projets.
Utilisateur enregistré pour les anciennes versions de OMRAN Estate

- Consultez la section [Quoi de neuf dans OMRAN Estate](#page-quoideneufdansomranestate) pour un aperçu rapide concernant les changements majeurs.
- Parcourez les [Guides de Démarrage Rapide](#page-guidesdedemarragerapide) pour vous familiariser avec la nouvelle version.
Créé avec HelpNDoc Personal Edition: [Générateur de documentation d'aide HTML gratuit](https://www.helpndoc.com/fr)

## Introduction <a id="page-introduction"></a>
_Page : `Introduction.html`_

- [A propos de](#page-aproposdeomranestate)
- [Configuration du système](#page-configurationdusysteme)
- [Obtenir de l'aide](#page-obtenirdelaide1)
- [Éditions d'](#page-editionsdomranestate)
- [Contrat de Licence Utilisateur Final](#page-contratdelicenceutilisateurfinal)[ContratdeLicenceUtilisateurFinal.html](#page-contratdelicenceutilisateurfinal)
- [Quoi de neuf dans](#page-quoideneufdansomranestate)
Créé avec HelpNDoc Personal Edition: [Générer des livres électroniques EPub facilement](https://www.helpndoc.com/fr/creer-des-livres-numeriques-epub)

### A propos de OMRAN Estate <a id="page-aproposdeomranestate"></a>
_Page : `AproposdeOMRANEstate.html` — Introduction_

OMRAN Estate est un outil facile à utiliser, mais cependant puissant et intuitif, pour créer et gérer vos projets immobiliers.
OMRAN Estate propose une interface claire et efficace pour manipuler et synthétiser vos données, toutes les informations métier sont stockées dans une source unique et sans se soucier du fonctionnement des rouages internes de leur génération. Il suffit que chaque utilisateur se concentre sur son métier et la solution s'occupe de l'aspect d’intégration et de synthèse.
Oubliez les interfaces utilisateur pléthoriques et les outils incompréhensibles. OMRAN Estate a été conçu pour proposer les fonctions avancées dans leur forme la plus simple.
Le logiciel OMRAN Estate est une solution modulaire avec centralisation des données et des traitements, les modules constituants le système ont été conçus sur la base d’une séparation fonctionnelle :
[image : NouvelElement37.png]
Créé avec HelpNDoc Personal Edition: [Produire des livres électroniques facilement](https://www.helpndoc.com/fr/creer-des-livres-numeriques-epub)

### Configuration du système <a id="page-configurationdusysteme"></a>
_Page : `Configurationdusysteme.html` — Introduction_

La configuration système conseillé pour OMRAN Estate comprend:

### Pour un poste client :
Processeur : Double cœur ou plus
RAM : 4 Go ou plus
Espace disque libre : 10 Go ou plus
Système d'exploitation Windows : Vista SP2, 7 SP1, 8, 8.1, 10 (Valable pour x64 et x86)

### Pour le serveur (avec 1 à 10 utilisateurs) :
Processeur : Double cœur ou plus (de préférence un processeur i3, i5 ou i7 de nouvelle génération).
RAM : 4 Go ou plus
Espace disque libre : 50 Go ou plus et prévoir un espace de stockage externe pour les sauvegardes de la base de données
Système d'exploitation Windows : 10 x64 ou encore toute version de Windows Server supérieure à 2008 R2
SQL Server 2012 Express (version gratuite) ou SQL Server 2012 Standard (version avec licence).

### Pour le serveur (avec 10 utilisateurs ou plus) :
Processeur : i5 ou i7 de nouvelle génération.
RAM : 8 Go ou plus
Espace disque libre : 100 Go ou plus et prévoir un espace de stockage externe pour les sauvegardes de la base de données
Système d'exploitation Windows : 10 x64 ou encore toute version de Windows Server supérieure à 2008 R2
SQL Server 2012 Express (version gratuite) ou SQL Server 2012 Standard (version avec licence).

### Réseaux :
Prévoir une connection réseau de bonne qualité entre le serveur et les postes clients avec de préférence un réseau cablé. Si un besoin se présente de se connecter à distance via internet, prévoir une bonne connection internet pour un temp de réponse optimal.
Créé avec HelpNDoc Personal Edition: [Produire facilement des livres électroniques Kindle](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creer-ebooks-pour-kindle-amazon)

### Obtenir de l'aide <a id="page-obtenirdelaide1"></a>
_Page : `Obtenirdelaide1.html` — Introduction_

Ce fichier d'aide est accessible en ligne . Vous pouvez obtenir la dernière version d'aide à l'adresse [http://help.omranestate.com](http://help.omranestate.com)
Accès
Le fichier d'aide en ligne fait partie de l'installation de HelpNDoc.
Pour lancer l'aide en ligne, appuyez sur la touche F1 ou cliquez sur le bouton "Aide" en haut à gauche des fenêtres de la solution OMRAN Estate.
[image : NouvelElement38.png]
Créé avec HelpNDoc Personal Edition: [Générateur de documentation d'aide HTML gratuit](https://www.helpndoc.com/fr)

### Éditions d'OMRAN Estate <a id="page-editionsdomranestate"></a>
_Page : `EditionsdOMRANEstate.html` — Introduction_

Trois éditions d'OMRAN Estate sont disponibles en fonction de vos besoins:

- Edition desktop sous licence entièrement fonctionnelle en locale ;
- Edition en ligne sous licence qui vous permet de travailler en délocalisé;
- Edition corporate sous licence qui intègre les deux variantes;
Créé avec HelpNDoc Personal Edition: [Créer des sites web d'aide facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

### Contrat de Licence Utilisateur Final YASMINE ENGINEERING SYSTEMS OMRAN Estate <a id="page-contratdelicenceutilisateurfinal"></a>
_Page : `ContratdeLicenceUtilisateurFinal.html` — Introduction_

### Conditions générales d'utilisation
L'utilisation de ce logiciel implique votre acceptation des conditions générales et mentions d'avertissement. Vous vous engagez à ne pas utiliser ce logiciel à des fins illicites ou interdites par les lois ou par les mentions légales et contractuelles précisées ci-dessous. Ces mentions ne sont pas listées de manière exhaustive; de ce fait, les droits de YASMINE ENGINEERING SYSTEMS ne se trouvent pas limités à ces dispositions.
Il appartient à l'utilisateur de ce logiciel de prendre toutes les mesures appropriées de façon à protéger ses propres données et/ou logiciels de la contamination par d'éventuels virus circulant sur le réseau Internet. De manière générale, YASMINE ENGINEERING SYSTEMS décline toute responsabilité à un éventuel dommage survenu pendant la consultation du présent site.

### Avertissements
YASMINE ENGINEERING SYSTEMS s'efforce de mettre à la disposition des utilisateurs de ce logiciel, des informations fiables et mises à jour, toutefois d'éventuelles erreurs ou omissions peuvent apparaître. Dans ce cas, nous remercions les utilisateurs de nous en faire part en nous contactant à l'adresse suivante support@yasminees.com

### Données personnelles
Les données personnelles que les utilisateurs peuvent être amenées à fournir se font sur une base volontaire par le biais de la messagerie électronique ou formulaires et sont exclusivement destinées à YASMINE ENGINEERING SYSTEMS, à des fins administratives. Si vous souhaitez une modification ou une suppression de ces informations, adressez votre demande par courriel à support@yasminees.com
Les données personnelles recueillies ne seront jamais transmises à des tiers et ne feront jamais l’objet d’une commercialisation. En particulier, nous ne commercialisons aucun fichier d’adresses ou de courriers.

### Propriété intellectuelle, droits d’auteur, copyright
La structure générale ainsi que les logiciels, textes, images, sons…et tous les autres éléments composant le logiciel sont propriété exclusive de YASMINE ENGINEERING SYSTEMS.
L'ensemble de ce logiciel relève de la législation tunisienne et internationale sur le droit d'auteur et la propriété intellectuelle. Tous les droits de reproduction sont réservés, y compris pour les documents téléchargeables et les représentations iconographiques ou photographiques. Toute reproduction, divulgation, distribution, représentation, traduction, diffusion, modification, transcription, partielle ou totale, de ce site pour un usage autre que celui privé, est interdite sans I’ autorisation préalable, expresse et écrite de YASMINE ENGINEERING SYSTEMS.
Les marques de l’exploitant du logiciel et de ses partenaires, ainsi que les logos figurant sur le logiciel sont des marques semi-figuratives ou non et sont déposées. Toute reproduction totale ou partielle de ces marques ou de ces logos effectués à partir des éléments du logiciel sans l’autorisation expresse de l’exploitant du logiciel est donc prohibée.
Vous pouvez faire la demande de cette autorisation, en précisant le(s) contenu(s), marque(s), logo(s) que vous souhaitez reproduire et le support de reproduction envisagé, par courriel à support@yasminees.com. Il en est de même des bases de données qui sont protégées par les dispositions du Code de la propriété intellectuelle (CPI). Toute contravention à ces dispositions législatives est constitutive du délit de contrefaçon, sanctionné et engage, à ce titre, la responsabilité civile et pénale de son auteur.

### Sécurité
L’exploitant prend à sa charge les mesures physiques, techniques et organisationnelles appropriées pour assurer la sécurité et la confidentialité de ses données à caractère personnel hébergées sur ce site, en vue notamment de les protéger contre toute perte, destruction accidentelle, altération, et accès non autorisés.

### Support aux utilisateurs
Nous assurons la formation (sur site ou à distance) de tous les utilisateurs et nous mettons à leur disposition un manuel d'utilisation, mis à jour à chaque nouvelle version. En outre, nos équipes assurent le traitement de toutes les demandes formulées par les utilisateurs via l'adresse support@yasminees.com et ce, 24h/24 et 7j/7.

### Contributions
Ce logiciel est en évolution permanente, d'une part pour suivre l'évolution des technologies de l'information et de la communication et d'autre part pour répondre aux demandes et besoins des utilisateurs. Les améliorations fonctionnelles apportées, en accord avec les contributions des utilisateurs, leurs sont préalablement présentées.

### Hyperliens
Ce logiciel donne accès à d'autres sites via des liens hypertextes. N’étant pas le gestionnaire de ces sites, YASMINE ENGINEERING SYSTEMS ne pourra en aucun cas être tenu pour responsable du contenu des sites ainsi accessibles, ou des éventuelles collectes et transmission de données personnelles, installation de cookies ou tout autre précédé tendant aux mêmes fins, effectués par ces sites.
Tout site public ou privé est autorisé à établir un lien vers le site www.yasminees.com, sous réserve d'informer YASMINE ENGINEERING SYSTEMS par courrier électronique au support@yasminees.com et de respecter les conditions suivantes :
• Ne pas imbriquer les pages du site du YASMINE ENGINEERING SYSTEMS à I’ intérieur des pages d'un autre site, en particulier par voie de cadres (ou « frames, iframes »), d’inlining ou de tout autre procédé de nature similaire
• Ne pas utiliser la technique du lien profond effectué vers des fichiers téléchargeables ou exécutables.
• Ne pas faire de lien vers une image, photographie ou icône en I’ excluant de sa page initiale.
Toute utilisation à des fins publicitaires ou commerciales est exclue. Cette autorisation ne s'applique pas à des sites Internet diffusant des informations à caractère polémique, pornographique, xénophobe ou pouvant, dans une plus large mesure, porter atteinte à la sensibilité du plus grand nombre.

### Modification de la notice légale
YASMINE ENGINEERING SYSTEMS se réserve le droit de modifier la présente notice à tout moment. L'utilisateur s'engage donc à la consulter régulièrement.

### Droit applicable en cas de litige
Le droit applicable est le droit tunisien.

### Attribution de juridiction
Pour tout litige, il est fait attribution exclusive de juridiction aux tribunaux compétents de l’Ariana (Tunisie).

### ENTRE LES SOUSSIGNES :
Yasmine Engineering Systems, société à responsabilité limitée (s.a.r.l) au capital de 50 000 Dinars, immatriculée au R.C du Tribunal de Première Instance de l’Ariana, sous le n° B 0355702006, M.F N° 000 M A 918388/ K, dont le siège social est au BP 103 - Pôle Technologique El Ghazela 2088 Ariana - Tunisie, représentée par son gérant, monsieur Mehdi MAHJOUB et ci-après désignée, le « FOURNISSEUR »,

### D’UNE PART

### ET
………………………………………………, société à ……………………………. (………) au capital de ………. Dinars, immatriculée au R.C du Tribunal de Première Instance …………….. sous le n° ………….., M.F N° ………………………………………, dont le siège social est au ..…………………………………………………………………………………………………………………, représentée par son ……………… ………………… ………….. ………………… ; ci-après désignée « LICENCIÉ »,

### D’AUTRE PART

### IL EST D’ABORD EXPOSE
Que le FOURNISSEUR déclare, sous son entière responsabilité, avoir mis au point un logiciel dénommé « OMRAN Estate », permettant la gestion intégrée de l’activité de promotion immobilière.
Que le LICENCIÉ déclare vouloir bénéficier de l’exploitation non exclusive du dit logiciel et de sa technologie suivant les conditions définies au présent contrat.

### CECI EXPOSÉ, IL A ÉTÉ CONVENU ET ARRÊTÉ CE QUI SUIT
ARTICLE 1 : DÉFINITIONS. Utilisés dans le présent contrat, les termes suivants auront le sens défini ci-après :

### Licence Serveur :
La licence serveur donne le droit au LICENCIÉ d’installer la base de données « YesREMS » et tous les objets serveurs nécessaires au bon fonctionnement du logiciel « OMRAN Estate » sur un serveur ou une machine dédiée lui appartenant ou dont il assure le contrôle, il a en outre le droit de le paramétrer, de le faire fonctionner et de donner accès aux licences utilisateurs aux informations qu'il aura collectées et classées dans cette base de données.

### Licence Utilisateur :
La licence utilisateur représente un nom utilisateur et un mot de passe permettant au LICENCIÉ d'accéder à la base de données « YesREMS » installée sur le serveur à travers l'interface du logiciel « OMRAN Estate » pour consulter, modifier ou créer des données dans celle-ci. Dans le cadre de ce contrat, le LICENCIÉ aura en prés paramètre Nombre N utilisateurs pouvant s'identifier avec des noms utilisateurs et des mots de passe distincts. Le nombre de licences utilisateur implique une restriction au niveau du nombre de postes sur lesquels sera installé le logiciel « OMRAN Estate ». Plus concrètement, le LICENCIÉ ne pourra installer le logiciel « OMRAN Estate » que sur Nombre N postes.

### ARTICLE 2 : CONCESSION DE LICENCE.
Le FOURNISSEUR concède, de façon définitive irrévocable et permanente, au LICENCIÉ une licence serveur et Nombre N licences utilisateur d’utilisation non exclusive de l’outil « OMRAN Estate » par lesquelles le LICENCIÉ acquiert le droit d’installer « OMRAN Estate » sur un serveur lui appartenant ou dont il assure le contrôle, de le paramétrer, de le faire fonctionner et de donner accès à ses utilisateurs licenciés aux informations qu’il aura collectées et classées.

### ARTICLE 3 : TRANSFERT DE TECHNOLOGIE.
Le FOURNISSEUR transférera au « Licencié », à partir de l’entrée en vigueur du présent contrat, toute la technologie nécessaire et utile pour lui permettre d’exploiter le logiciel cédé, dans les meilleures conditions.

### ARTICLE 4 : REMUNERATION.
En rémunération des droits et licences concédés, le LICENCIÉ paiera au FOURNISSEUR la somme forfaitaire totale de Montant en toute lettre Hors Taxes (Montant) à la date de signature du présent contrat. Toutes les charges annexes seront facturées en sus.

### ARTICLE 5 : PROPRIÉTÉ ET DROITS INTELLECTUELS.
Le FOURNISSEUR conserve la propriété exclusive des droits et licences concédées dont seule l’exploitation et la technologie d’utilisation sont transférés au « Licencié ». Cette exclusivité couvre aussi la documentation destinée au LICENCIÉ ainsi que tout code ou élément du logiciel.
Le LICENCIÉ accepte, par conséquent, et reconnaît que le présent contrat ne lui donne pas la propriété intellectuelle du logiciel cédé ou de l’un de ses éléments. Le FOURNISSEUR lui octroie seulement le droit d’exploitation du logiciel et de sa technologie dans le cadre du présent contrat. Il est donc interdit au LICENCIÉ de proposer, sous quelque forme et de quelque manière que ce soit, des copies ou reproductions des éléments concédés à des tiers.
Il est également interdit au LICENCIÉ de diffuser les éléments mis à sa disposition, y compris le logiciel, par la vente, la location, le prêt, la distribution, la mise à disposition via un réseau ou toute autre forme de mise à disposition à des tiers, et ce, à quelque fin, commerciale ou non, que ce soit, sauf dérogation écrite expresse du FOURNISSEUR. Le LICENCIÉ s'interdit formellement de communiquer à tout tiers même à titre gratuit, tout ou partie d'éléments faisant partie du logiciel.

### ARTICLE 6 : ENTRÉE EN VIGUEUR.
Le présent contrat entrera en vigueur à la date de sa signature.

### ARTICLE 7 : GARANTIES.
Le FOURNISSEUR garantit qu’il a la propriété et la jouissance des droits concédés et qu’il a la pleine capacité de les concéder et de conclure le présent contrat. En tout état de cause, le FOURNISSEUR garantit expressément que l’outil « OMRAN Estate » n’a pas été réalisé en contravention des droits d’un tiers, et notamment qu’il ne comprend aucun élément sur lequel un tiers pourrait faire valoir un droit d’auteur ou tout autre droit.
Le FOURNISSEUR garantit le LICENCIÉ contre tout recours qui serait intenté par un prétendu titulaire des droits de propriété intellectuelle sur l’outil « OMRAN Estate ». Il garantit la parfaite fiabilité du logiciel et des autres éléments concédés.
Il est, cependant, précisé que le FOURNISSEUR ne saura, en aucun cas, tenu d’indemniser d’éventuels dommages de quelque nature que ce soit, résultant :

- des dérangements causés aux logiciels émanant d’un problème d’infrastructure technique chez le LICENCIÉ (problème de machine, prise réseau, Switch, hub …) ;
- d’anomalies de fonctionnement de « OMRAN Estate » occasionnées par son utilisation non appropriée par le LICENCIÉ ou toute intervention sur « OMRAN Estate » par un tiers non mandaté par le FOURNISSEUR;
- de la connexion Télécom choisie par le LICENCIÉ et du non fonctionnement de cette connexion ;
- de tout fait dont le FOURNISSEUR aura démontré qu’il est dû à la force majeure.

### ARTICLE 8 : OBLIGATIONS DU FOURNISSEUR.
Dans le cadre de l’application du présent contrat, le FOURNISSEUR s’engage à :

- Procéder à la première installation de « OMRAN Estate » sur le serveur du LICENCIÉ ainsi que les postes utilisateurs ;
- assurer la maintenance du logiciel pendant les 12 premiers mois du présent contrat ;
- Installer toutes les mises à jour du logiciel pendant les 12 premiers mois du présent contrat ;
Toutes les prestations ci-dessus décrites, à l’art. 8 sont comprises dans le prix stipulé à l’art. 4.

### ARTICLE 9 : OBLIGATIONS DU LICENCIÉ.
Dans le cadre de l’application du présent contrat, le LICENCIÉ s’engage à :

- respecter les notices d’utilisation de « OMRAN Estate » ;
- utiliser le logiciel dans des conditions conformes à son usage, signalées par le FOURNISSEUR ;
- informer le FOURNISSEUR de toute anomalie lors de l’installation ou du fonctionnement du logiciel qu’il pourrait constater ;
- ne pas modifier le logiciel ni l’intégrer dans quelque programme informatique que ce soit ;
- veiller à disposer d’une connexion internet opérationnelle et suffisamment puissante pour permettre au FOURNISSEUR d’intervenir à distance.
Le LICENCIÉ reconnaît et accepte qu’il n'est, notamment et de manière non exhaustive, pas autorisé à :

- reconstituer la logique du logiciel, à le décompiler, à le désassembler ou à le modifier, notamment et de manière non exhaustive, en vue de produire un logiciel similaire ou concurrent ;
- accéder aux codes source du logiciel ;
- à dissocier les composants du logiciel pour les utiliser à d'autres fins que celles définies par le FOURNISSEUR ;
- à accéder aux données gérées par le logiciel autrement que par les interfaces explicitement mises à sa disposition ;
- à prêter, à donner en location, à communiquer, à copier, à reproduire, à commercialiser ou à transférer le logiciel ou toute information confidentielle à un tiers ni à permettre à tout tiers d'utiliser ou de prendre connaissance du logiciel ou de toute information confidentielle pour quelque raison que ce soit.
Tous les coûts de la connexion télécommunication entre le LICENCIÉ et tout tiers restent entièrement à la charge du LICENCIÉ.

### ARTICLE 10 : REPRODUCTION - AUTORISATIONS – INTERDICTIONS.
Les informations ou contenus auxquels « OMRAN Estate » peut donner accès peuvent être protégés par la réglementation et les traités internationaux en matière de droits d’auteur ou de propriété intellectuelle. Le LICENCIÉ garantit que l’utilisation qu’il fera de l’outil « OMRAN Estate » respectera ces règlementations et traités.

### ARTICLE 11 : PENALITES.
Tout manquement par l’une ou l’autre partie de ses obligations présentement souscrites, entraînera la résolution de plein droit du présent contrat, 30 jours après une mise en demeure pour remédier au manquement, restée infructueuse, à moins que la partie victime du manquement préfère maintenir le contrat et demander des réparations.

### ARTICLE 12 : CONFIDENTIALITE.
Chacune des parties s’engage à mettre en œuvre les moyens appropriés pour garder confidentiels les documents et informations fournis par l’autre partie dans l’exécution du présent contrat.

### ARTICLE 13 : INTUITU PERSONAE.
Le présent contrat est conclu intuitu personae, autrement dit en considération de la personne du cocontractant. Il en résulte notamment que le FOURNISSEUR ne pourra, sans l’accord express et écrit du LICENCIÉ, céder le présent contrat, sous quelque forme et de quelque manière que ce soit à un tiers. Il demeura toujours responsable, personnellement, de l’exécution du présent contrat.

### ARTICLE 14 : NOTIFICATIONS.
Sauf mention contraire, toute notification nécessaire ou autorisée dans le cadre du présent contrat devra se faire par écrit, par fax ou par e-mail ou remis en mains propres, aux adresses des parties reprises en début de contrat.

### ARTICLE 15 : DROITS DE TIMBRE ET D’ENREGISTREMENT.
Les frais de timbres et d’enregistrement du présent contrat sont à la charge du LICENCIÉ.

### ARTICLE 16 : LOI APPLICABLE – ATTRIBUTION DE COMPÉTENCE.
Le présent contrat est régi par le droit tunisien. Toute difficulté d’interprétation ou d’exécution du présent contrat sera de la compétence exclusive des tribunaux de Tunis I.
Créé avec HelpNDoc Personal Edition: [Produire des livres EPub gratuitement](https://www.helpndoc.com/fr/creer-des-livres-numeriques-epub)

### Quoi de neuf dans OMRAN Estate <a id="page-quoideneufdansomranestate"></a>
_Page : `QuoideneufdansOMRANEstate.html` — Introduction_

4.4.10 (Released 2019-04-22)
===================================
- 0001423: [Location] ARRONDISSEMENT TTC SUITE A L'AUGMENTATION.
4.4.9 (Released 2019-04-18)
==================================
- 0001409: [Achat] POSSIBILITE D'AFFECTER UNE FACTURE A UN BON DE COMMANDE.
- 0001410: [Location] LETTRE D'AUGMENTATION.
- 0001411: [Location] RAFRAÎCHIR LISTE DOSSIERS LOCATION ET SYNDIC.
- 0001414: [Projet] MODIFICATION LIBELLE Retenue : Retenue non remboursable.
- 0001416: [Projet] LOT MULTI FOURNISSEUR : FORMATAGE.
- 0001418: [Projet] PROJET ET DECOMPTE CREE LE, MODIFIE LE.
- 0001406: [Location] BLOCAGE DES REGLEMENT DANS LE CAS DE PLUSIEURS FACTURES.
- 0001407: [Vente] VERSION WEB AFFECTATION DATE IMPRESSION.
4.4.8 (Released 2019-03-29)
==================================
- 0001386: [Location] LISTE DES DOSSIERS DE LOCATION.
- 0001387: [Location] PAIEMENT LOCATION.
4.4.6 (Released 2019-03-13)
==================================
- 0001403: [Trésorerie] TRANSFERT BORDEREAU.
- 0001402: [Trésorerie] ETAT DES FLUX : DOUBLE CLICK TRANSFERT CAISSE.
4.4.5 (Released 2019-03-05)
==================================
- 0001382: [Achat] MODULE ACHAT : GENERATION DOCUMENT.
- 0001390: [Stock] FICHE TRANCHE DROIT SUR PRÉVISION DE BASE.
- 0001395: [Achat] BON DE LIVRAISON LIGNES.
- 0001399: [GRH] CORRIGER LIBELLE MODULE.
- 0001383: [Projet] PROBLEME D'EDITION DES FICHES DECOMPOSITION.
- 0001398: [Stock] STOCK IMPORT.
- 0001400: [Projet] PROJET PREVISION.
- 0001401: [Vente] DOSSIER DE VENTE : FILTRAGE RAPPORT PAR STATUT.
- 0001378: [Vente] VENTE : Actions.
- 0001380: [Projet] FICHE DECOMPTE : Pourcentage affiché en millier colonnes avant et après.
- 0001385: [WEB] LISTE DES PROSPECTION : Bug lors de la sélection des commerciaux.
- 0001392: [Stock] STOCK SUPERFICIE NETTE.
- 0001379: [Achat] ACHAT : AFFECTATION ENTITE FONCTIONNELLE.
- 0001389: [Vente] PROSPECTION SUPPORT SOURCE DE CONTACT.
4.4.4 (Not Yet Released)
===============================
- 0001377: [Trésorerie] FICHE LIAISON : Droit sur les dépots.
- 0001376: [Vente] DOSSIER DE VENTE : droit sur Modification des montants de vente.
- 0001343: [WEB] MODULE VENTE: IL FAUT CACHER L'ECRAN "COMMISSION".
- 0001374: [Rapports] Correctifs dans la fiche contrat de réservation d'EL FALL.
4.4.3 (Released 2019-02-07)
==================================
- 0001370: [Administration] Modifier les titres des fenêtres des règlements pour savoir le module en question.
4.3.30 (Not Yet Released)
================================
- 0001354: [Stock] ETUDE DE FAISABILITE : BILAN PAR TRANCHE.
- 0001371: [Stock] Marge bénéficiaire brut en HT et non en TTC = C.A HT - Charge HT.
- 0001372: [Projet] Tableau d'ouverture des plis : Budget estimé dans certains cas vide alors qu'il ne le doit pas.
- 0001373: [Vente] Correctifs dans les rapports ELFALL de contrat de réservation et demande transfert client.
- 0001364: [Stock] ETUDE DE FAISABILITE : BOLCAGE-DEBLOCAGE.
- 0001368: [Achat] MODULE ACHAT :SUPPRESSION D'UNE DOCUMENT A RÉGLER D'UNE COMMISSION.
- 0001353: [Location] GESTION DES PAIEMENTS LOCATION.
4.3.29 (Not Yet Released)
================================
- 0001369: [Achat] Impression d'une fiche après enregistrement et erreur d'enregistrement.
4.3.28 (Released 2018-11-23)
===================================
- 0001367: [Projet] MODULE PROJET: IL FAUT AJOUTER UNE COLONNE ECART.
- 0001357: [Stock] Définition de la superficie nette.
- 0001356: [Stock] StkItem : Flag Titré / Superficie titrée.
- 0001358: [Stock] Tranche : Ajouter le délai de rétractation.
- 0001362: [Vente] IMPRESSION FICHES TRANSFERT CLIENT PRODUIT PROJET.
- 0001359: [Vente] Dossier de vente : Ajout de date d'impression du contrat de réservation / promesse de vente.
- 0001363: [Vente] FICHE DESISTEMENT.
- 0001360: [Vente] IMPRESSION CONTRAT DE RESERVATION.
- 0001361: [Vente] IMPRESSION AUTORISATION DE VERSEMENT.
- 0001355: [Achat] GESTION BON DE COMMANDE : LISTE ET FICHE.
4.3.27 (Released 2018-11-01)
===================================
- 0001349: [Administration] AJOUT DES PREFIX ET DES SUFFIX DANS LES SEQUENCES.
- 0001347: [Vente] DOSSIER DE VENTE: AJOUT DE DATES (LOURD + WEB).
- 0001348: [Location] MODULE LOCATION: PROBLÈME DE LIBÉRATION DU DOSSIER DE SYNDIC.
- 0001350: [Achat] MODULE ACHAT: AJOUT DES COLONNES DANS LA LISTE DE BON DE COMMANDE.
- 0001351: [Achat] MODULE ACHAT: VÉRIFICATION A FAIRE AVANT DE GÉNÉRER UN B.L OU UNE FACTURE.
- 0001352: [Achat] MODULE ACHAT: CLÔTURE BON DE COMMANDE.
4.3.25 (Released 2018-09-12)
===================================
- 0001337: [Vente] Génération des commisions en double lorsqu'on sélectionne plusieures tranches.
- 0001340: [Achat] CONSULTATION RESILIATION.
- 0001338: [Achat] GROUPEMENT DES LIGNES PAR PROJET.
- 0001339: [Achat] SUPPRESSION DEVIS CONSULTATION.
- 0001341: [Vente] SUPERFICIE GLOBALE.
- 0001342: [Achat] CREDIT SUPPRESSION DEBLOCAGE.
- 0001344: [Achat] MODULE ACHAT:CORRECTION DU COPIAGE DE LA CONSULTATION.
- 0001345: [Achat] MODULE ACHAT:AFFICHER LES LOTS DANS LA LISTE DES CONSULTATION.
- 0001320: [Location] MODULE LOCATION: AJOUT D'UN OU PLUSIEURS ONGLETS POUR AFFICHER LES DÉTAILS QUI CONCERNENT UNE ÉCHÉANCE.
4.3.24 (Released 2018-08-30)
===================================
- 0001334: [Stock] Ajouter un bouton de suppression des plans dans la fiche tranche "Puger les plans".
- 0001332: [Vente] GESTION DES TRANSITIONS DES DOSSIERS DE VENTE A COMPTANT OU A CREDIT.
- 0001336: [Vente] MODULE VENTE: GESTION DES DROITS DES PROSPECTS ET CLIENT.
4.3.23 (Released 2018-08-26)
===================================
- 0001331: [Vente] SEGMENTATION.
- 0001333: [Vente] SEGMENTATION : ZOOM.
- 0001327: [Stock] GESTION DES DROITS MODULE STOCK.
- 0001328: [Trésorerie] SUPPRESSION CATEGORIE.
- 0001326: [Vente] MODULE VENTE: LE SYSTEME OUVRE TOUJOURS LA FICHE PROSPECT EXPRESS AVEC LE DROIT DE MODIFICATION.
- 0001321: [Location] MODULE LOCATION: AFFECTATION DE LA DATE DE JOUR A LA GÉNÉRATION D'UNE FACTURE, FACTURE PRO-FORMA ET AVOIRS (ich).
- 0001316: [Stock] MODULE STOCK: ARTICLE DUPLIQUE LORSQU'IL EST AFFECTE COMME SOUS ARTICLE DANS UN DOSSIER DE LOCATION.
- 0001323: [Location] MODULE LOCATION: GESTION DES DROITS.
- 0001322: [Vente] MODULE VENTE: GESTION DES DROITS (LOURD+WEB).
- 0001325: [Vente] MODULE VENTE: ETAT D'ARTICLE ERRONÉ.
- 0001317: [Location] MODULE LOCATION: AFFICHAGE DU CONTENU DE CERTAINE COLONNE DANS LA LISTE DE RÈGLEMENTS.
- 0001324: [Achat] MODULE ACHAT: GESTION DES DROITS.
- 0001329: [Achat] HABILITATIONS SUR LES ENTITES.
- 0001330: [Achat] GÉNÉRATION DOCUMENT A DOCUMENT.
4.3.22 (Released 2018-06-27)
===================================
- 0001309: [Location] MODULE LOCATION: MONTANT ERRONÉ LORSQU'ON GÉNÈRE UN AVOIR.
- 0001315: [Location] MODULE LOCATION: BUG A LA GÉNÉRATION DES FACTURES POUR LES LIGNES DE SYNDIC QUI ONT UN AVOIR.
4.3.20 (Released 2018-06-19)
===================================
- 0001304: [Location] IMPRESSION PLUSIEURS FACTURES.
- 0001308: [Location] MODULE LOCATION: AJOUT D'UN ECRAN "SUIVI DES AFFECTATIONS".
- 0001306: [Location] AJOUTER LA POSSIBILITE DE FACTURER SANS PASSER PAR LA Proforma.
- 0001312: [Stock] MODULE VENTE: LE SYSTEME AFFICHE LES PLANS D'ARTICLES DONT LA VOCATION N'EST PAS VENTE (LOURD+WEB).
- 0001313: [Vente] MODULE VENTE: PLAN: BUG A L'ENREGISTREMENT D'UN PROSPECT EXPRESS (LOURD).
- 0001307: [Location] MODULE LOCATION: AJOUT DES ONGLETS "RÉGLEMENTENT, FACTURES ET AVOIRS" DANS LE DOSSIER DE LOCATION ET DE SYNDIC.
- 0001305: [Projet] DECOMPTE FACTURE FODEC.
4.3.15 (Released 2018-02-23)
===========================================
- 0001303: [Stock] MODULE STOCK: AFFICHAGE DE PRIX DE VENTE TTC APRES L'IMPORT.
- 0001302: [Vente] MODULE VENTE: POSSIBILITÉ DE MODIFIER DES CHAMPS DANS LA FICHE DÉSISTEMENT APRES VALIDATION (LOURD+WEB).
- 0001299: [Vente] MODULE VENTE: IL FAUT AJOUTER LES CHAMPS DE FRAIS DANS LA FICHE DÉSISTEMENT (IMPRESSION).
4.3.14 (Released 2018-02-08)
===========================================
- 0001301: [Vente] MODULE VENTE: BUG PROSPECTION (LOURD + WEB).
4.3.13 (Released 2018-01-31)
===========================================
- 0001297: [Vente] MODULE VENTE: RÉINITIALISATION DE DOSSIER DE VENTE (LOURD+WEB).
- 0001298: [Projet] MODULE PROJET: BUG LORSQU'ON AJOUTE UN LOT MULTI FOURNISSEUR.
4.3.12 (Released 2018-01-25)
===========================================
- 0001295: [Projet] MODULE PROJET: LE SYSTEME N'AFFICHE PAS LES MONTANTS REELS DANS LE TBD TRESORERIE.
- 0001294: [Projet] MODULE PROJET: IL FAUT AJOUTER LA POSSIBILITÉ D'EXPORTER LA LISTE SUIVI DES LOTS (QUI FAIT PARTIE DES TABLEAU DE BORD).
- 0001296: [Stock] MODULE STOCK: DANS LA LISTE DES ARTICLES, MONTANT DE PRIX DE VENTE HT ERRONÉ.
4.3.10 (Released 2018-01-05)
===========================================
- 0001290: [Projet] AJOUT POSSIBILITÉ D'IMPUTER UNE FACTURE à UN COMPTE PRORATA.
- 0001216: [Stock] MODULE STOCK: AFFICHE DE LA LIGNE PROSPECTION ET LE DOSSIER DE LOCATION DANS LES FICHES ARTICLE ET LOTISSEMENT (LOURD+WEB).
- 0001293: [Location] MODULE LOCATION: APPLICATION DE TVA LORSQU'ON FAIT UNE RECONDUCTION (DOSSIER DE LOCATION + DOSSIER DE SYNDIC).
- 0001291: [Vente] Création d'un système de pré-prospection.
- 0001292: [Location] MODULE LOCATION: AJOUT DE LA POSSIBILITÉ D'AFFICHER LES PROSPECT QUI NE SONT PAS AFFECTES DANS UNE TRANCHE.
4.3.9 (Released 2018-01-04)
==========================================
- 0001288: [Achat] ACHAT TDB SOLDE FOURNISSEUR.
4.3.8 (Released 2017-12-20)
==========================================
- 0001283: [Projet] AJOUT TYPE DE REUNION.
- 0001287: [Achat] GESTION DES CREDITS : Echéances crédit.
- 0001284: [Vente] GESTION DES AUTRES FRAIS PAR TRANCHE.
- 0001285: [Administration] AJOUTER UN FILTRE DANS NOTRE CHECKLIST.
- 0001286: [Trésorerie] PROBLÈME AU NIVEAU DE L'AFFECTATION ET POINTAGE.
- 0001282: [Vente] Affectation automatique du notaire dans l'attestation de consignation.
- 0001279: [Vente] Bug lors de la recherche dans la liste des trasferts produit.
- 0001281: [Stock] Ajout de dates dans le terrain et la tranche.
4.3.6 (Released 2017-11-30)
==========================================
- 0001277: [Location] MODULE LOCATION: BUG A LA GÉNÉRATION DE L'ECHEANCIER.
- 0001278: [Location] MODULE LOCATION: BUG DE CALCUL DE MONTANT APRES AVOIR FAIRE UN AVENANT ENSUITE UNE RECONDUCTION.
- 0001275: [Location] MODULE LOCATION: IL FAUT AJOUTER LA POSSIBILITÉ D'AJOUTER UN AVENANT.
- 0001272: [Location] MODULE LOCATION: BUG AU NIVEAU DE CALCUL DES ÉCHÉANCE APRES RECONDUCTION.
- 0001270: [Location] MODULE LOCATION: APPLICATION DU MONTANT DE BASE POUR RCONDUCTON.
4.3.4 (Released 2017-11-23)
==========================================
- 0001276: [Projet] MODULE PROJET: BUG A L'AJOUT D'UNE AVANCE SUR DÉCOMPTE A PARTIR D'UN DÉCOMPTE.
4.3.3 (Released 2017-11-13)
==========================================
- 0001271: [Location] MODULE LOCATION: AJOUTER LA POSSIBILITÉ DE SUPPRIMER DES RECONDUCTIONS.
- 0001274: [Projet] MODULE PROJET: BUG LORSQU'ON AJOUTE UNE AVANCE SUR MARCHE OU UNE AVANCE SUR DECOMPTE.
- 0001269: [Achat] Ajout du champ Catégorie dans le bon de commande (tout les documents aussi).
- 0001273: [Administration] MODULE ADMINISTRATION: PARAMÉTRAGE DES MAILS AU NIVEAU DES TRANSITIONS DANS LES ENTITÉS FONCTIONNELLE.
4.3.2 (Released 2017-11-03)
==========================================
- 0001267: [Achat] MODULE ACHAT: IL FAUT AJOUTER LA POSSIBILITÉ DE PERSONNALISER LE WORKFLOW DE L'AVANCE SUR BON DE COMMANDE.
4.3.1 (Released 2017-11-02)
==========================================
- 0001264: [Trésorerie] MODULE TRÉSORERIE: MODIFICATIONS A FAIRE DANS L'ECRAN "AFFECTATIONS ET POINTAGES".
- 0001266: [Location] MODULE LOCATION: AJOUT DE LA POSSIBILITÉ DE FAIRE DES AVOIRS.
- 0001172: [Trésorerie] MODULE TRÉSORERIE: ETAT DES FLUX, AJOUT D'UNE ACTION POUR OUVRIR LA FICHE QUI CORRESPONDANT UNE LIGNE.
- 0001265: [Location] MODULE LOCATION: GESTION DE SYNDIC.
4.3.0 (Released 2017-10-24)
==========================================
- 0001235: [Achat] B.L. n'est pas marqué comme réglé bien que la facture est réglée.
- 0001159: [Vente] Gestionnaire.
- 0001203: [Administration] CORRECTIFS A FAIRE AU NIVEAU DES ATTACHEMENTS (LOURD+WEB).
- 0001222: [Projet] La présentation des états de la retenue de garantie.
- 0001228: [Achat] Saisie de prix forfaitaire dans le B.C. ou autre document.
- 0001225: [Achat] Augmenter la taille du champ de description dans les lignes des documents.
- 0001232: [Stock] Ajouter dans l'article le champ "le quota terrain groupe".
- 0001238: [Achat] Visibilité des B.C. par utilisateur.
- 0001141: [Vente] MODULE VENTE: BUG DANS LE FILTRE DE RECHERCHE DES NOUVEAUX ARTICLES DANS LA FICHE TRANSFERT PRODUIT.
- 0001158: [Vente] Renommer le champs dans la liste des dossiers de vente.
- 0001166: [Vente] Dans la prospection ordonner le combo Source de contact par ordre alphabétque.
- 0001010: [Vente] MODULE VENTE: AJOUT D'UN CHAMP NOTAIRE DANS LA FICHE DE RÈGLEMENT (LOURD+WEB).
- 0001251: [Vente] MODULE VENTE: AFFICHAGE DES TRANCHES DANS LE SÉLECTEUR DES ARTICLES POUR LE DÉSISTEMENT TRANSFERTS PRODUIT ET CLIENT (WEB+LOURD).
- 0001252: [Administration] IL FAUT AJOUTER UNE TABLE POUR SAUVEGARDER L'HISTORIQUE DES INSTALLATIONS.
- 0001257: [Location] MODULE LOCATION: BUG A L'IMPRESSION D'UN RECU.
- 0001236: [Vente] MODULE VENTE: CALCUL DE MONTANT REMISE (WEB).
- 0001088: [Location] MODULE LOCATION; AJOUT D'UNE IMPRESSION POUR LES FACTURES.
- 0001260: [Achat] AJOUT DES CHAMPS DANS LA TABLE "StlSettlement ".
- 0001253: [Trésorerie] MODULE TRÉSORERIE: MODIFICATION DANS LA LISTE DES RETENUES A LA SOURCE.
- 0001259: [Vente] MODULE VENTE: DES CORRECTIfS A FAIRE DANS LE CALCULE DES MONTANTS AFFICHES SOUS L'ONGLET "AUTRE MONTANT" (WEB+LOURD).
- 0001262: [Achat] BUG DANS LE RÈGLEMENT, LE SYSTEME N'AFFECTE PAS LE TIERS PAYEUR.
- 0001261: [Achat] MODULE ACHAT: MODIFICATIONS A FAIRE DANS LES ECRANS ( DEVIS, B.C, B.L, FACTURE, DOCUMENTS A RÉGLER ).
- 0001254: [Trésorerie] AJOUT DE DEUX FICHES "CERTIFICAT DE RETENUE A LA SOURCE" ET "ORDRE DE PAIEMENT".
- 0001263: [Projet] MODULE PROJET: CORRECTION DE LA CATÉGORIE PAR DÉFAUT A L'AJOUT D'UNE LIGNE D'APPROVISIONNEMENT.
- 0001204: [Vente] MODULE VENTE: AJOUT DES COLONNES DANS LA LISTES DE DOSSIERS DE VENTE ET LA LISTE DE SUIVI DES DOSSIERS DE VENTE (WEB+LOURD).
- 0001255: [Trésorerie] Révision de la liste de l'écran affectation et pointage.
- 0001183: [Administration] MODULE ADMINISTRATION: AJOUT DANS LA LISTE DES DROITS DES LOTS LES HABILITATIONS DE VISIBILITÉ DES BOUTONS DE LOT.
- 0001192: [Vente] MODULE VENTE: AFFECTATION D'UNE PROSPECTION SUR UN ARTICLE DONT LE STATUT "RÉSERVATION" (LOURD+WEB).
- 0001205: [Stock] Ajouter les champs.
- 0001210: [Stock] champ avec trop de 0 derrière la virgule..
- 0000966: [Vente] MODULE VENTE: INCOHÉRENCE DES DOCUMENTS ATTACHÉES APRES AVOIR FAIRE UN TRANSFERT CLIENT.
4.2.14 (Released 2017-10-06)
===========================================
- 0001247: [Vente] Problème de libération d'un dossier de vente dans la liste à faire.
4.2.13 (Released 2017-08-30)
===========================================
- 0001195: [Vente] Ajouter dans le plan un filtre par type d'article.
- 0001242: [Vente] MODULE VENTE: AJOUT D'UN BOUTON "COPIER" DANS L'ECRAN MODALITÉ DE PAIEMENT (LOURD+WEB).
- 0001245: [Achat] MODULE ACHAT: AJOUT DES COLONNES DANS LISTE DE GESTION DES PRÊTS.
- 0001241: [Trésorerie] MODULE TRÉSORERIE: IL FAUT AJOUTER LA POSSIBILITÉ DE CHOISIR UN ÉLÉMENT VIDE DANS LE FILTRE DES COMPTE.
- 0001246: [Achat] MODULE ACHAT: IL FAUT AJOUTER UN ECRAN DANS LEQUEL ON AFFICHE LES "ÉCHÉANCE CRÉDIT".
- 0001244: [Vente] MODULE VENTE: AMÉLIORATION DES DONNÉES AFFICHÉES DANS LE TOOLTIP DANS L'ECRAN PLAN (LOURD+WEB).
4.2.12 (Released 2017-08-18)
===========================================
- 0001243: [Vente] MODULE VENTE: AJOUT DE LA POSSIBILITÉ DE FAIRE UNE PROSPECTION SUR UN ARTICLE EN VENTE (WEB+LOURD).
- 0001190: [Administration] AJOUT D'UN CHAMP "TIERS PAYEUR" DANS TOUS LES PIÈCES DE RÈGLEMENT ET DE PAIEMENT DU SYSTÈME (LOURD+WEB).
- 0001197: [Achat] Revoir les colonnes 'Entité fonctionnelle' dans la liste des factures..
4.2.11 (Released 2017-08-16)
===========================================
- 0001009: [Stock] MODULE STOCK: MODIFICATION DE MESSAGE DE VÉRIFICATION DES PÉRIODES LORD DE PARAMÉTRAGE DES COMMISSIONS (LOURD+WEB).
4.2.10 (Released 2017-08-16)
===========================================
- 0001230: [Achat] MODULE ACHAT: BUG LORS DE RÈGLEMENT D'UNE ÉCHÉANCE DE REMBOURSEMENT.
- 0001227: [Projet] Calcul de la date d'échéances des retenues de garantie.
- 0001199: [Stock] MODULE STOCK: BOGUE LORSQU'ON AFFECTE UN SOUS ARTICLE A UN ARTICLE.
- 0001194: [Vente] Cas des plans avec duplex.
4.2.9 (Released 2017-07-18)
==========================================
- 0001207: [Projet] MODULE PROJET: AJOUT D'UN TABLEAU DE BORD "SUIVI DES DÉPENSES PAR MARCHE".
- 0001221: [Achat] AJOUT D'UN FLAG "AVOIR FINANCIER/ DÉBATTEMENT" DANS LA FICHE AVOIR.
- 0001223: [Vente] MODULE VENTE: AJOUT D'UN ECRAN MODALITÉ DE PAIEMENT DANS LE DOSSIER DE VENTE (LOURD+WEB).
- 0001211: [Vente] MODULE VENTE: AJOUT D'UNE COLONNE " DESCRIPTION ARTICLE " DANS LA LISTE DE DOSSIERS DE VENTE (WEB+LOURD).
- 0001142: [Projet] MODULE PROJET: FILTRE DES TABLEAUX DE BORD "ETAT DES RETENUES DE GARANTIE" ET "ETAT DES COMPTES PRORATA".
- 0001187: [Vente] MODULE VENTE: CALCUL AUTOMATIQUE DU MONTANT DE LA LIGNE SCHÉMA DE FINANCEMENT (WEB+LOURD).
- 0001108: [Administration] Bugs au niveau de l'attachement des fichiers (client lourd mais web à vérifier aussi).
- 0001237: [Location] MODULE LOCATION: CORRECTION DE CALCUL DES MONTANTS HT.
- 0001206: [Projet] Paramétrage réception dans les lots.
- 0001208: [Projet] MODULE PROJET: MODIFICATION DANS LE TABLEAU DE BORD "SUIVI DE DÉPENSES PAR MARCHE".
4.2.8 (Released 2017-07-05)
==========================================
- 0000924: [Vente] Contrôle en boucle suite au choix d'un statut dans un dossier de vente.
- 0000783: [Projet] Lots : Avancement - cas FAZ T1 15/184.
- 0001070: [Achat] MODULE ACHAT: CORRECTIFS A FAIRE DANS LA PROCESSUS DE GESTION DES AVANCES.
4.2.7 (Released 2017-07-04)
==========================================
- 0001179: [Vente] AJOUTER LA COLONNE DESCRIPTION DANS LE SÉLECTEUR D'ARTICLES DES PROSPECTIONS ET DANS LA LISTE D'AJOUT DES SOUS ARTICLE.
- 0001200: [WEB] MODULE STOCK: AFFICHAGE DES CATÉGORIES ET DES TYPES DANS LA FICHE DES LOTISSEMENTS.
- 0001130: [Projet] Ajouter dans la liste des lots une colonne "Montant TTC avec travaux supplémentaires".
- 0001185: [Administration] MODULE ADMINISTRATION: AFFICHAGE AUTOMATIQUE DES ATTACHEMENTS POUR LE PROSPECT ET LE DOSSIER DE VENTE (LOURD+WEB).
- 0001180: [Vente] MODULE VENTE: BUG LORS DE L'ENREGISTREMENT DANS PROSPECT EXPRESS SI LE TYPE PAR DÉFAUT DE PROSPECT EST PM.
- 0001201: [Vente] Différents points au niveau du stock et module vente pour MFCPOLE.
- 0001214: [Administration] DANS L'ECRAN D'ACCUEIL, ON DOIT AJOUTER UNE COLONNE TIERS (LOURD + WEB).
- 0001213: [Stock] MODULE STOCK: DANS LA FICHE LOTISSEMENT, LE SYSTÈME N'OUVRE PAS LA PROSPECTION ET LE DOSSIER DE VENTE CORRESPONDANT (LOURD).
- 0001217: [Stock] MODULE STOCK: AFFICHAGE DES STATUTS DANS LA LISTE DES ARTICLES ET DES LOTISSEMENTS (WEB).
- 0001212: [Stock] MODULE STOCK: INCOHÉRENCE DES MONTANTS ENTRE LA FICHE DE LOT ET LE DOSSIER DE VENTE (LOURD + WEB).
- 0001218: [Stock] MODULE STOCK: VERIFICATION DU PRIX DE VENTE HT POUR L'ARTICLE "35130018 UIb18".
- 0001097: [Vente] Ajouter une date "Date de remise au siège" dans la pièce de règlement.
4.2.6 (Released 2017-05-26)
==========================================
- 0001178: [Stock] MODULE STOCK: AJOUT D'UN CHAMP DE RECHERCHE MULTI-CRITÈRES DANS LA LISTE DES ARTICLES, SOUS ARTICLES ET LOTISSEMENT (LOURD+WEB).
- 0001188: [Stock] MODULE STOCK : AJOUT DES CHAMPS "MONTANT HT","TVA"et "MONTANT TTC" (LOURD+WEB).
- 0000936: [Administration] MODULE ADMINISTRATION: CORRECTIFS A FAIRE DANS LE MASQUE "UTILISATEUR".
- 0001182: [Projet] MODULE PROJET: MODIFICATIONS SUR LA LISTE DES ACTIONS QUI S'AFFICHE LORSQU'ON FAIT UN CLIC DROIT.
4.2.4 (Released 2017-04-14)
==========================================
- 0001226: [SAV] Bug lors de la saisie d'une 2eme réclamation.
4.2.3 (Released 2017-04-10)
==========================================
- 0001175: [Projet] MODULE PROJET: BUG LORSQU'ON OUVRE L'ECRAN "PREVISIONS".
- 0001176: [Achat] MODULE ACHAT: BUG LORSQU'ON GÉNÈRE UNE FACTURE SUR UN AVENANT.
- 0001184: [Vente] MODULE VENTE: MODIFICATION DE LIBELLE DU CHAMP "MINUTE/ENREGISTREMENT DE CONTRAT" (WEB+LOURD).
4.2.2 (Released 2017-03-28)
==========================================
- 0001173: [Trésorerie] MODULE TRÉSORERIE: BUG DANS LE CALCUL DE SOLDE FIN DE PÉRIODE.
- 0001169: [Projet] Ajouter dans l'édition du BL les informations sur le total commandé et le total livré.
4.2.1 (Released 2017-03-20)
==========================================
- 0001161: [Stock] Mettre à jour interface article.
4.2.0 (Released 2017-03-15)
==========================================
- 0001149: [Administration] Ajouter la notion de "Groupe d'actionnaires".
- 0001147: [Stock] Ajouter dans l'entité juridique un bouton pour déloquer tout les champs.
4.1.9 (Released 2017-03-03)
==========================================
- 0001134: [Vente] MODULE VENTE: FUSIONNER LES RAPPORT, RYTHME DE CONCRÉTISATION ET LIVRAISON, ET AJOUT DE DEUX RAPPORT RYTHME D'ENC , DECAISSEMEN.
- 0001139: [Projet] MODULE PROJET: BUG LORSQU'ON CLIC SUR IMPRIMER DANS L'ECRAN "BILAN".
- 0001131: [Projet] Gestion des prévisions de remboursement du compte prorata.
- 0001143: [Achat] Bloquer la génération d'une facture sur la base de bons de livraisons de différents fournisseurs.
- 0000906: [Stock] MODULE STOCK: CORRECTIFS A FAIRE (WEB).
- 0001148: [Stock] Ajouter un champ nommé "Date liquidation" dans l'entité juridique avec une case à côcher.
- 0001150: [Stock] Ajouter un champ en pourcentage nommé "Part maison" dans l'entité juridique.
- 0001146: [Stock] Ajouter un champ nommé "Nombre de parts (actions)" dans l'entité juridique.
- 0001145: [Stock] Contôle sur le nombre de répartitions dans l'entité juridique.
- 0001135: [Projet] MODULE PROJET: AJOUT DU CHAMP "FODEC" DANS L'ECRAN DE RECHERCHE D'UNE LIGNE DE BORDEREAU.
- 0001153: [Vente] Ajouter dans la liste des clients une colonne avec le nombre de dossiers.
- 0000917: [Projet] DEPOUILLEMENT gestion des dates.
- 0001068: [Vente] La date au niveau du règlement.
- 0000898: [Administration] Révision des libellés dans les paramètres système.
- 0001091: [Vente] Afficher l’historique des actions sur une pièce de règlement.
- 0001110: [Vente] Unicité de la référence du chèque par banque sans prendre en considération le tier.
- 0001064: [Vente] Réviser le message "Titre foncier obligatoire" dans le dossier de vente.
- 0001071: [Administration] MODULE ADMINISTRATION: MODIFICATIONS A FAIRE DANS L'ECRAN DES ENTITÉS FONCTIONNELLES.
- 0001111: [Projet] MODULE PROJET: BUG AU NIVEAU DE CALCUL DES AVANCES SUR DECOMPTE LORSQU'ON LES SUPPRIME.
- 0001124: [Projet] Bug dans les prévisions du lot multifournisseurs.
- 0001144: [Achat] MODULE ACHAT: MODIFICATIONS DANS L'ECRAN SOLDE FOURNISSEUR.
- 0001140: [Projet] MODULE PROJET: BUG LORSQU'ON CLIC SUR IMPRIMER DANS L'ECRAN "TRÉSORIÈRE TRIMESTRIELLE".
- 0001105: [Projet] MODULE PROJET: BUG DANS LA GÉNÉRATION D'UN BON DE COMMANDE A PARTIR D'UN DEVIS.
- 0001118: [Projet] MODULE LOCATION: AJOUT DE LA POSSIBILITÉ DE SUPPRIMER PLUSIEURS LIGNES DE L'ECHEANCIER DE DOSSIER DE LOCATION.
4.1.8 (Released 2017-02-08)
==========================================
- 0000741: [Projet] Ajouter des détails pour les travaux supplémentaires dans la fiche décompte.
- 0001117: [Location] MODULE LOCATION: BUG D'AFFICHAGE DES ECHEANCES DANS L'ECRAN APPEL DES LOYERS.
- 0001075: [Vente] Réaliser le rapport "ÉTAT SUIVI NOTAIRES CRÉDITS" et "ANALYSE DÉLAIS DE LIVRAISONS".
- 0001033: [Projet] Ajouter une "Pénalité sur R.G." dans la fiche de remboursement de R.G..
- 0000958: [Projet] Ajout d'une vision des règlements dans la fiche imprimable du décompte.
- 0000459: [Projet] Améliorer le rendu de l'impression lors de la comparaison des offres.
- 0001123: [Location] MODULE LOCATION: CALCUL EXONÉRATION.
- 0001122: [Location] MODULE LOCATION: AJOUT DANS CHAMP "MONTANT TIMBRE FISCAL" DANS L'ECRAN DE MODIFICATION DES RATIOS.
- 0001121: [Location] MODULE LOCATION: AJOUT D'UN FLAG "AVEC TIMBRE FISCAL" DANS LA TRÉSORERIE DE MODULE LOCATION.
- 0001115: [Location] MODULE LOCATION: AJOUT D'UN FLAG "FACTURABLE" DANS LA CONFIGURATION DE TRÉSORERIE.
- 0001116: [Location] MODULE LOCATION: AJOUT D'UN BOUTON DE MODIFICATION DES TAUX PAR DOSSIER DE LOCATION.
- 0001028: [Stock] Avoir la possibilité de remettre le terrain ou la prospection de terrain a vide dans l'E.F..
- 0001104: [Projet] MODULE PROJET: AJOUT DE LA POSSIBILITÉ DE DÉSÉLECTIONNER UN FOURNISSEUR DANS LE LOT MULTI FOURNISSEUR.
4.1.7 (Released 2017-02-01)
==========================================
- 0001095: [Projet] Ajouter un troisième niveau dans la décomposition du lot.
- 0001106: [Location] MODULE LOCATION: GESTION DE LA FACTURATION DES CLIENTS EXONÉRÉS.
- 0000786: [Projet] Lots : Réviser et corriger le prévisions (génération et tableaux de bord).
- 0001098: [Achat] MODULE ACHAT: AJOUT D'UN FILTRE SUR LES STATUTS DANS LA LISTE DES BONS DE COMMANDE.
- 0001003: [Location] MODULE LOCATION: MODIFICATIONS A FAIRE DANS LE DOSSIER DE LOCATION.
- 0000556: [Projet] Mettre à jours les éditions des PVs conformément aux modèles proposés par ELFALL (voir svn).
4.1.6 (Released 2017-01-27)
==========================================
- 0001119: [Projet] Divers points en relation avec les prévisions dans le module projet.
- 0001107: [Rapports] Ajouter un rapport : Remise de pièces bancaires.
- 0001109: [Projet] Ajout d'une case à cocher nommée Bloquer de la restitution de la retenue de garantie.
- 0001077: [Projet] Ajouter le reste à déduire de l'avance sur décompte dans la fiche imprimable du décompte.
4.1.5 (Released 2017-01-12)
==========================================
- 0001054: [Projet] MODULE PROJET: AJOUT DE LA POSSIBILITÉ.
- 0001076: [Projet] Renommer le champ "A déduire" dans le décompte.
- 0001060: [Projet] MODULE PROJET: BOGUE AU NIVEAU DES FACTURE APRES AVOIR RECALCULE UN DECOMPTE.
- 0001103: [Location] MODULE LOCATION: Bug dans le filtre de recherche de la liste "Stock".
- 0001100: [Location] MODULE LOCATION: Verification du schéma de financement d'article.
- 0001102: [Location] MODULE LOCATION: Vérification du taux de TVA appliquée lorsqu'on ajoute une échéance dans les schéma de financement.
- 0001099: [Location] Modifier les filtre de l'appel de loyer.
- 0001092: [Projet] Gestion d'un droit d'accès lors de la sélection ou dé-sélection d'un offre d'entreprise.
- 0001061: [Projet] MODULE PROJET: AFFICHAGE RÉPÉTITIF DU MESSAGE " La facture associée va être mis à jours ".
- 0001051: [Projet] MODULE PROJET: MONTANT D'AVANCE SUR DÉCOMPTE PERSISTE DANS L'ECRAN DES DÉCOMPTES MÊME APRES AVOIR SUPPRIME CETTE AVANCE.
- 0001083: [Projet] MODULE PROJET: CORRECTION DES COLONNES DE DÉPOUILLEMENT D'UN LOT MULTI FOURNISSEURS.
- 0000956: [Projet] Ajout de la notion de "Remboursements" dans le lot (Avoirs).
- 0001021: [Projet] Ajouter une fiche avoir imprimable.
- 0001020: [Projet] Ajouter une fiche avenant imprimable.
- 0001084: [Projet] Documenter la règle pour la moyenne pondérée dans les offres des lots.
- 0001096: [Projet] Les deux ventilations au niveau de la fiche projet.
- 0001032: [Projet] Ajout d'une édition "Etat des retenues de garantie".
- 0001101: [Location] MODULE LOCATION: Modifications dans la fiche article du dossier de location.
4.1.4 (Released 2016-12-30)
==========================================
- 0001055: [Achat] Comment faire pour gérer les dépenses qui sont générer à partir de module vente et location?.
- 0001059: [Projet] MODULE PROJET: IL FAUT AJOUTER LA POSSIBILITÉ DE GÉNÉRER UNE FACTURE A PARTIR DE PLUSIEURS BON DE LIVRAISONS.
- 0001090: [Location] MODULE LOCATION: Ajout d'un nœud rapport dans l'hiérarchie de module location.
- 0001085: [Location] MODULE LOCATION: AJOUT D'UNE COLONNE "STATUT" DANS LA LISTE DES ARTICLES DE DOSSIER DE LOCTAION.
- 0001094: [Achat] MODULE ACHAT: BOGUE LORSQU'ON SAISIE UN TAUX DE RETENUE A LA SOURCE DIFFÉRENT DE 0.
- 0001086: [Location] MODULE LOCATION: AJOUT DES COLONNES "NET A PAYER", "RÉGLÉ" ET "RESTE".
- 0001072: [Projet] MODULE PROJET: AJOUT D'UN ONGLET "AVOIRS" DANS LE LOT MULTI FOURNISSEUR.
- 0001089: [Location] MODULE LOCATION: modification de la description qui s'affiche dans la liste des échéanciers.
- 0001087: [Location] MODULE LOCATION: BOGUE AU NIVEAU D'APPEL DES LOYERS LORSQU'ON RÉSILIE UN CONTRAT.
- 0001048: [Vente] MODULE VENTE: AMÉLIORATION DE RECHERCHE DANS LES ECRANS "PROSPECTIONS","PROSPECT", "CLIENT" et "DOSSIER DE VENTE" (LOURD + WEB).
- 0001058: [Achat] MODULE ACHAT: BOGUE A LA RAFRAÎCHISSEMENT DE LA LISTE DES BONS DE COMMANDE.
- 0001066: [Vente] Boutons rafraîchir et enregistrer et fermer dans le dossier de vente ouvert à partir de l'accueil.
- 0001082: [Vente] Filtrer les listes (combos et similaires) des commerciaux avec un case à cocher.
- 0001081: [Vente] Exiger les données du client lors d'un transfert.
4.1.3 (Released 2016-12-26)
==========================================
- 0001025: [Stock] Prendre le code et la description du terrain pour l'E.F. lors de son affectation.
- 0000988: [Vente] MODULE VENTE: AJOUT D'UN RAPPORT D'ANALYSE DES ACTIONS COMMERCIALES.
- 0001022: [Projet] Associer une avance sur décompte à un décompte unique.
- 0001034: [Vente] Ajout de la fiche engagement imprimable dans le désistement.
- 0001035: [Projet] BUG DANS L'ECRAN DES AVANCES DANS LA FICHE LOT.
- 0000999: [Rapports] Ajouter un histogramme dans le rapport TAUX DE CONCRETISATION PAR ANNEE / TRANCHE.
- 0001030: [Stock] Gestion des statuts dans l'E.F..
- 0001041: [Location] MODULE LOCATION: CALCUL DE RETENUE A LA SOURCE.
- 0001044: [WEB] AJOUT DES BOUTONS "RECHERCHER" DANS LA VERSION WEB.
- 0001043: [Administration] AUTHENTIFICATION: LE SYSTÈME NE DOIT PLUS FAIRE LE CONTRÔLE SI LE CHAMP LOGIN EST SAISI EN MAJUSCULE OU MINUSCULE (LOURD+WEB).
- 0001052: [Stock] MODULE STOCK: LE SYSTÈME TRUNC LE CONTENUE DE LA COLONNE " CONTENANCE " LORSQUE LA CHAÎNE EST LONGUE DANS LE FICHIER EXCEL.
- 0001056: [Vente] MODULE VENTE: AJOUT DES CHAMPS DANS LA FICHE "DÉSISTEMENT" (LOURD+WEB).
- 0001039: [Vente] MODULE VENTE: ENVOI DES MAILS.
- 0001053: [Stock] MODULE STOCK: IL FAUT RENDRE LE CHAMP " NOMBRE DE CAUTION " IMPORTABLE ET EXPORTABLE.
- 0001016: [Trésorerie] MODULE TRÉSORERIE: LE SYSTÈME GÉNÈRE UNE EXCEPTION LORSQU'ON OUVRE L'ECRAN "TRÉSORERIE ANNUELLE".
- 0001040: [Vente] MODULE VENTE: AFFECTATION DES GESTIONNAIRES (LOURD + WEB).
- 0001036: [Achat] MODULE ACHAT: BUG LORS DU PASSAGE DU STATUT D'UN DOCUMENT A RÉGLER LIE PAR L’ENTITÉ FONCTIONNELLE "DOCUMENT A RÉGLER".
- 0001024: [Projet] Dans la fiche imprimable du B.C. ajouter Total HT par article et éliminer PU TTC.
- 0001026: [Stock] Lorsqu'on affecte un terrain à l'E.F. le prix n'est pas affecté.
- 0001029: [Stock] Ajouter une date dans l'E.F..
- 0001019: [Projet] Ajout des fournisseurs dans la liste des lots pour le lot multifournisseurs.
4.1.2 (Released 2016-12-12)
==========================================
- 0001005: [Administration] AJOUTER LA POSSIBILITÉ D’ÉDITER LE GROUPEMENT, LE TYPE.... D'UN FICHIER JOINT (LOURD + WEB).
- 0001006: [Vente] MODULE VENTE: MODIFICATION DU TEXTE AFFICHE DANS L'OBJET D'UN MAIL (LOURD+WEB).
- 0001011: [Vente] MODULE VENTE: MODIFICATION DE TAILLE DES COLONNES DE LA LISTE "SCHÉMA DE FINANCEMENT" (LOURD).
- 0001001: [Administration] Ajouter un "Dossier de groupement" dans les fichiers attachés.
- 0001012: [Vente] MODULE VENTE: AJOUT D'UN CONTRÔLE SUR LA PIÈCE IDENTITÉ (DU NOUVEAU CLIENT) LORS D'UN TRANSFERT CLIENT (LOURD +WEB).
4.1.1 (Released 2016-12-05)
==========================================
- 0000932: [Rapports] Dans le rapport (ENC, CONC, CH) PAR MOIS 2eme page prendre les frais de désistement directement à partir du désistement.
- 0000974: [Stock] MODULE STOCK: IL FAUT AJOUTER UN BOUTON "TRANSFORMER EN TERRAIN" DANS LA FICHE PROSPECTION.
- 0000975: [Stock] MODULE STOCK: AJOUT D'UN NATURE "LOT DE TERRAIN".
- 0000976: [Stock] MODULE STOCK: AJOUTER LA POSSIBILITE DE CHOISIR UN TERRAIN DANS L'ETUDE DE FAISABILITE.
- 0000978: [Stock] MODULE STOCK: LE BOUTON "IMPORTER D'UN PROJET RÉEL" NE FONCTIONNE PLUS.
- 0000979: [Stock] MODULE STOCK: IL FAUT REVISER LA TABULATION DANS L'ONGLET DELAIS DE L'ETUDE DE FAISABILITE.
- 0000980: [Stock] MODULE STOCK: RÉVISION DE LA LISTE DES UNITÉS DE BASES.
- 0000985: [Stock] MODULE STOCK: IL FAUT MENTIONNER QUE LES CHAMPS DES MONTANTS SONT EN TTC SOUS L'ONGLET TERRAIN DE L'ETUDE DE FAISABILITE.
- 0000986: [Stock] MODULE STOCK: IL FAUT AJOUTER LES OPTIONS "AGRANDIR" ET "RÉDUIRE" DANS LE MASQUE DE L'ETUDE DE FAISABILITÉ.
- 0000989: [Vente] MODULE VENTE: AJOUT D'UN CHAMP COUT TOTAL ET D'UN FLAG DANS LA FICHE ACTION COMMERCIALE.
- 0000892: [Projet] Ajout d'un onglet "Avenant" dans le décompte.
- 0000961: [Vente] MODULE VENTE: MODIFICATIONS A FAIRE AU NIVEAU DES COMMISSIONS (LOURD).
- 0000968: [Stock] MODULE STOCK: DES MODIFICATIONS AU NIVEAU DE BILAN DE L'ETUDE DE FAISABILITE.
- 0000981: [Stock] MODULE STOCK: IL FAUT VERIFIER LES VALEURS IMPORTÉES A PARTIR D'UN AUTRE PROJET.
- 0000982: [Stock] MODULE STOCK: RAFRAICHISSEMENT DES DONNEES DANS L'ETUDE DE FAISABILITE.
- 0000983: [Stock] MODULE STOCK: MISE A JOUR DES CHAMPS DE L'ONGLET DÉLAIS DE L'ETUDE DE FAISABILITÉ.
- 0000990: [Stock] MODULE STOCK: AFFICHAGE DES TYPES DANS LA FICHE PROSPECTION TERRAINS.
- 0000991: [Projet] MODULE PROJET: VERROUILLAGE DES CHAMPS DU DÉCOMPTE ET AVENANT APRES AVOIR GÉNÉRER UNE FACTURE.
- 0000992: [Vente] MODULE VENTE: AJOUT D'UN FILTRE DANS LA LISTE DE CHOIX DES SOUS ARTICLES A AFFECTER A UN DOSSIER DE VENTE(LOURD+WEB).
- 0000993: [SAV] MODULE S.A.V: AJOUT D'UN ECRAN DE CONFIGURATION DES CONTENANCES DANS LE MODULE SAV (LOURD + WEB).
- 0000994: [Vente] MODULE VENTE: MODIFICATION AU NIVEAU D'AFFICHAGE DES DONNÉES DANS LE TABLEAU DE BORD "SUIVI HEBDOMADAIRE".
- 0001000: [Stock] Ajout du champ ICE (Identifiant Commun de l'Entreprise) dans l'entité juridique et les tiers de type PM.
- 0001002: [Stock] MODULE STOCK: IL FAUT METTRE A JOUR LA LISTE DE L'ECRAN "PROSPECTION TERRAIN".
4.1.0 (Released 2016-11-21)
==========================================
- 0000926: [Vente] Renommer des champs dans la liste des dossiers de vente.
- 0000909: [Vente] Action d'ouverture de fiche article à partir de la liste des dossiers de vente.
- 0000919: [Vente] MODULE VENTE: APPLICATION DE SEUIL DE VERSEMENT POUR LA CONCRETISATION.
- 0000918: [Stock] MODULE STOCK: MODIFICATION DE LIBELLE ET AJOUT D'UN FLAG ( LOURD + WEB ).
- 0000920: [Vente] MODULE VENTE: BOGUE VÉRIFICATION DE SEUIL DE VERSEMENT LORS DE L'ENREGISTREMENT D'UN DOCUMENT "FRAIS DE SYNDIC" (LOURD).
- 0000798: [Vente] GESTION DES COMMISSIONS DE VENTE.
- 0000905: [Vente] Ajout de la mention Frais de syndic dans l'attestation de paiement.
- 0000865: [Achat] MODULE ACHAT: AJOUT DE COLONNE FODEC ET CALCUL DE PRIX UNITAIRE TTC AVEC FODEC.
- 0000908: [Stock] MODULE STOCK: MASQUE D'AFFECTATION DES SOUS ARTICLES (CLIENT LOURD).
- 0000910: [Vente] MODULE VENTE: AJOUT D'UN CHAMP "GESTIONNAIRE" (LOURD+WEB).
- 0000911: [Stock] Ajouter dans la tranche plancher couvert sous-sol, commerce, habitation.
- 0000914: [Stock] Convertir le nombre d'étages dans la tranche en un champ à sélections multiples.
- 0000915: [Stock] Problème de blocage d'un sous article par un dossier annulé.
- 0000916: [Rapports] Ajouter un coefficient de prévision dans le rapport ComTdbDetailsChiffreAffaire et le rapport ComTdbConcretAndSetlement.
- 0000930: [Vente] MODULE VENTE: BLOCAGE DU SYSTÈME.
- 0000934: [Achat] GESTION DES CREDITS.
- 0000921: [Stock] Problème au niveau des valeurs des champs de type montant du tableau des prévisions de base de la tranche.
- 0000923: [Projet] APPEL D'OFFRE ET DEPOUILLEMENT : Saisie des fournisseurs.
- 0000922: [WEB] MODULE VENTE: LISTE DÉROULANTE DES ACTIONS COMMERCIALES.
- 0000929: [Projet] COPIE MARCHE.
- 0000724: [Vente] Ajouter une action de type instruction.
- 0000931: [Rapports] Ajout du choix multiple des projets dans le rapport SUIVI HEBDOMADAIRE du module vente.
- 0000933: [Administration] MODULE ADMINISTRATION: AJOUT DE LA COLONNE "DESCRIPTION" DANS LA LISTE DE TRANSITION DU MASQUE "ENTITÉ FONCTIONNELLE".
- 0000935: [Stock] COPIE TRANCHE.
- 0000938: [WEB] MODULE ADMINISTRATION: MISE A JOUR DES FICHES "BANQUES" et "ORGANISMES".
- 0000939: [Stock] Gestion du Plancher couvert dans la Tranche.
- 0000940: [WEB] MODULE VENTE: AJOUT DU CHAMP "Promesse de vente" (WEB).
- 0000943: [Projet] GESTION DES BORDEREAUX.
- 0000944: [Stock] MODULE STOCK: CORRECTIFS A FAIRE LORSQU'ON COPIE LES DONNÉES D'UNE AUTRE TRANCHE (LOURD+WEB).
- 0000866: [Achat] MODULE ACHAT: AJOUT DE DEUX CHAMPS CODE COMPTABLE ET COPMTE COLLECTIF.
- 0000912: [Projet] Bug de calcul du montant de la facture dans le cas où on a une avance sur décompte.
- 0000913: [Projet] Bug de blocage du dernier décompte (flag est le dernier décompte saisie).
- 0000937: [Administration] PROBLÈME D'ENVOI DES MAILS (CLIENT LOURD).
- 0000946: [Vente] MODULE VENTE: AJOUT D'UN CHAMP "PIÈCE REMISE AU SIÈGE" (LOURD+WEB).
- 0000948: [Vente] MODULE VENTE: DES MODIFICATIONS A FAIRE LORS DE LA GÉNÉRATION DES COMMISSIONS (LOURD+WEB).
- 0000949: [Vente] MODULE VENTE: DANS LA GRILLE DES COMMISSIONS, ON NE PEUT PAS FAIRE LES OPÉRATIONS DE GROUPEMENT,CALCUL....
- 0000950: [Vente] MODULE VENTE: AJOUT D'UN DOCUMENT A IMPRIMER POUR LES COMMISSIONS.
- 0000952: [Stock] MODULE STOCK: AJOUT DE L'ACTION COMMERCIAL DANS LE PARAMÉTRAGE DES COMMISSIONS (LOURD+WEB).
- 0000957: [Projet] Fiche imprimable décompte : Renommer "Remboursements du compte prorata" en ....
- 0000960: [WEB] MODULE VENTE: PROBLÈME DE VALIDATION DES TRANSFERTS ET DE DESISTEMENT POUR LES UTILISATEUR QUI N'ONT PAS LE DROIT "VALIDATION".
- 0000965: [Vente] MODULE VENTE: IL FAUT RENDRE LE CHAMP ENTITÉ JURIDIQUE DANS LA FICHE DOCUMENT ET PIÈCE DE RÈGLEMENT ÉDITABLE PAR UN DROIT.
- 0000967: [Stock] MODULE STOCK: AJOUT D'UN ECRAN DE PARAMÉTRAGE DES FONCTIONS SPECIFIQUES.
- 0000969: [Stock] MODULE STOCK-->UNITÉS DE BASES: ON DOIT AFFICHER QUE LES CATÉGORIE DONT LA NATURE "BÂTIMENT" OU "TERRAIN".
- 0000970: [Stock] MODULE STOCK: IL FAUT AJOUTER UN CHAMP "NOTAIRE" DANS LA FICHE "PROSPECTION TERRAIN" ET LA FICHE "TERRAIN".
- 0000971: [Projet] Erreur dans la liste des règlements dans la fiche du décompte.
- 0000972: [Projet] MODULE PROJET: CORRECTIF A FAIRE DANS LA FICHE DÉCOMPTE.
- 0000757: [Stock] Harmoniser la fiche terrain avec la prospection.
- 0000941: [Rapports] Ajout d'un rapport "Formulaires de Barid Al-Maghrib".
- 0000942: [WEB] MODULE STOCK: AJOUT DES PRÉVISIONS DE BASES.
- 0000947: [Stock] MODULE STOCK: BUG AU NIVEAU DU CONTRÔLE UNICITÉ DE PARAMÉTRAGE DES COMMISSIONS (LOURD).
- 0000951: [WEB] MODULE VENTE: IL FAUT METTRE A JOUR L'ECRAN "PARAMÈTRE SYSTÈME" DANS LE WEB.
- 0000954: [Stock] MODULE STOCK: AJOUT D'UN CONTROLE POUR SAVOIR S'IL Y A UN CHEVAUCHEMENT DANS LE PAMATETRAGE DES COMMISSIONS.
- 0000959: [Vente] MODULE VENTE: CORRECTIF A FAIRE AU NIVEAU D'AFFECTATION DU GESTIONNAIRE (LOURD+WEB).
- 0000962: [Stock] MODULE STOCK: IL FAUT AFFICHER TOUS LES ACTIONS COMMERCIALES DANS LA LISTE DÉROULANTE "ACTION COMMERCIALE" (LOURD+WEB).
4.0.10 (Released 2016-09-12)
===========================================
- 0000769: [Administration] Ne pas ouvrir la même fiche plusieurs fois même si elle n'est modal.
- 0000868: [Vente] MODULE VENTE: BLOCAGE LORSQU'ON RAFRAÎCHIT LA FICHE "PROSPECT EXPRESS".
- 0000849: [Stock] Bug lors de l'enregistrement du bilan d'une étude de faisabilité si la relation des catégorie n'est pas paramétrée.
- 0000850: [Trésorerie] Ajout d'une catégorie dans le paramétrage "Relation avec les autres modules".
- 0000727: [Vente] Mise en page de l’attestation de paiement.
- 0000844: [Vente] MODULE VENTE: AJOUT D'UNE COLONNE "COMMERCIAL" DANS LA LISTE "SUIVI DES DOSSIERS DE VENTE".
- 0000845: [Stock] MODULE STOCK: IMPORT DES PLANS DES SOUS ARTICLES.
- 0000854: [Vente] MODULE VENTE: CONFIGURATION DES CADEAUX + MODIFICATION DE LA DISPOSITION DES CHAMPS (WEB + LOURD).
- 0000855: [Vente] MODULE VENTE: AJOUT D'UN RAISON DE L'ALERTE DANS LA LISTE DES PROSPECTIONS (LOURD + WEB).
- 0000856: [Vente] MODULE VENTE: MODIFICATIONS A FAIRE DANS LE MASQUE DOSSIER DE VENTE --> CADEAUX.
- 0000857: [Vente] MODULE VENTE: HABILITATION DE CONSULTATION DE L’HISTORIQUE DE LA MESSAGERIE INSTANTANÉE.
- 0000858: [Projet] MODULE PROJET: BOGUE AU NIVEAU DE LA DÉCOMPOSITION (DES LOTS).
- 0000862: [Stock] MODULE STOCK: LES CHAMPS HISTORIQUE DANS LA LISTE DES PROSPECTIONS TERRAINS.
- 0000863: [Administration] TRANSITION DES WORKFLOWS.
- 0000864: [Vente] Mettre à jour le schémas de financement dans le cas de la suppression.
- 0000870: [Stock] MODULE STOCK: IMPORT DES PLANS ETAGE.
- 0000871: [Administration] BOGUE LORSQU'ON FERME LE SYSTEME.
- 0000872: [Stock] MODULE VENTE: PLAN ÉTAGE DES SOUS ARTICLES (LOURD +WEB).
- 0000874: [Achat] MODULE ACHAT: IMPORT DES FOURNISSEURS.
- 0000875: [Stock] MODULE STOCK: BOGUE LORSQU'ON FERME UNE PROSPECTION TERRAIN APRES AVOIR EFFECTUER DES MODIFICATIONS.
- 0000536: [Vente] AJOUTER DANS LA LISTE DES ACTIONS S'IL S'AGIT D'UNE PREMIÈRE VISITE OU PLUS (LOURD+WEB).
- 0000580: [Stock] Taille des colonnes dans le bilan de l'étude de faisabilité.
- 0000838: [Vente] MODULE VENTE: FICHE IMPRIMABLE POUR LES PRÉVISIONS DE VENTE.
- 0000859: [Vente] MODULE VENTE: AJOUT D'UN RACCOURCI POUR AFFICHER LE(S) DOSSIERS DU CLIENT SELECTIONNE.
- 0000860: [Vente] MODULE VENTE: PROBLÈME DE GROUPEMENT PAR " DATE ACTION ".
- 0000861: [Projet] MODULE PROJET: BOGUE DE VENTILATION AU NIVEAU DE DÉCOMPOSITION (DES LOTS).
- 0000876: [Vente] MODULE VENTE: LA DATE LIVRAISON DOIT ETRE OBLIGATOIRE SI ON COCHE LIVRE (LOURD+WEB).
- 0000877: [Vente] MODULE VENTE: DANS LA LISTE DES DOSSIER DE VENTE, IL FAUT AJOUTER LA COLONNE "COMMUNICATION" (LOURD+WEB).
- 0000878: [Administration] MESSAGERIE INSTANTANÉE (LOURD + WEB).
- 0000879: [Vente] MODULE VENTE: AJOUT DE LA COLONNE "RAISON" DANS LE RAPPORT DE SUIVI DES CADEAUX.
- 0000881: [Stock] MODULE STOCK: PARAMÉTRAGE DE NOMBRE ET LE DÉLAI MINIMUM DES VISITES.
- 0000887: [Vente] Corriger le libellé de la colonne "Répartition" dans la fiche de de transfert client web.
- 0000889: [Vente] Le parrain doit être obligatoire si la source de contact est parrainage dans la prospection.
- 0000582: [Stock] Rapatrier des données d'une ancienne étude de faisabilité ou d'un projet vers une nouvelle étude.
- 0000611: [Stock] Export de toutes les données de l'étude de faisabilité en Excel.
- 0000764: [Stock] Vérification du champ Délégation dans la prospection de terrain.
- 0000843: [Vente] MODULE VENTE: MIS A JOURS DE l'EDITION "DEMANDE".
- 0000851: [Stock] Modifier la zone de saisie du terrain dans le terrain, prospection et E.F..
- 0000880: [Vente] MODULE VENTE:APPLICATION DES ACTIONS SUR LES PROSPECTIONS.
- 0000884: [Vente] MODULE VENTE: ON NE DOIT PAS AFFICHER LE MAIL DU SERVICE SAV POUR LA TRANCHE FAL NOOR.
- 0000891: [Vente] Import des prospections / Prospections saisies en déconnecté.
- 0000900: [Vente] VERIFICATION UNICITE DES TRANSFERT ET DESISTEMENT.
- 0000904: [Administration] TAILLE DES CHAMPS Notes.
- 0000758: [Stock] Ajouter l'édition : Comparatif prix au m2.
- 0000852: [Stock] Modifier la manière avec laquelle on fait la saisie des dates dans l'E.F..
- 0000867: [Administration] MODULE VENTE: LISTE DES PROPOSITIONS LORS D'AJOUT DES TIERS.
- 0000873: [Vente] MODULE VENTE: BOGUE LORSQU'ON FAIT UNE OPÉRATION DE TRANSFERT OU DÉSISTEMENT (WEB+LOURD).
- 0000899: [Vente] DESISTEMENT : CHOIX DES DOSSIERS A DESISTER.
- 0000901: [Vente] MISE A JOUR DU SCHEMA DE FINANCEMENT APRES ACTION.
- 0000902: [Vente] MISE A JOUR DES VALEURS DES FRAIS LORS DES TRANSFERTS TRANCHE.
- 0000799: [Administration] HABILITATION PAR TRANCHE.
- 0000893: [Vente] Ajustement de la gestion des cadeaux.
4.0.9 (Released 2016-06-15)
==========================================
- 0000888: [Rapports] Ajouter une ligne "Total facturé" dans le rapport (ENC, CONC, CH) PAR MOIS.
- 0000826: [Vente] MODULE VENTE: AJOUT DE DEUX CHAMPS DANS LE MASQUE DE PRÉVISIONS DE VENTE.
- 0000825: [Vente] MODULE VENTE: AJOUT D'UN FILTRE DATE ( LOURD + WEB ).
- 0000828: [Vente] MODULE VENTE: TDB RYTHME DE VENTE: AFFICHAGE DES LIBELLES DES MOIS.
4.0.8 (Pas encore publiée)
=========================================
- 0000834: [Projet] MODULE PROJET: AJOUT DES COLONNES "Écart Forfaitaire/ Marché HT" ET "Écart Forfaitaire/ Marché TTC".
- 0000827: [Vente] MODULE VENTE: AJOUT D'UNE MINIATURE POUR LES RAPPORTS.
- 0000833: [WEB] MODULE ADMINISTRATION: VERROUILLAGE DE SESSION WEB.
- 0000820: [Stock] MODULE STOCK: IMPORT DES SOUS ARTICLES.
- 0000831: [Vente] MODULE VENTE: ALERTE AUTOMATIQUE POUR LES NATURES (WEB+LOURD).
- 0000823: [SAV] MODULE S.A.V: PARAMÉTRAGE DU DÉLAI MINIMAL POUR LA PROGRAMMTION D' UNE VISITE.
- 0000824: [Vente] MODULE VENTE: DROIT DE MODIFICATION DU COMMERCIAL.
- 0000832: [Vente] MODULE VENTE: AJOUT DES COLONNES TAUX D'AVANCE RÉEL ET TAUX DE CONCRÉTISATION REEL.
- 0000822: [Vente] MODULE VENTE: AJOUT DE LA POSSIBILITE D'EXPORTER LES PREVISIONS.
- 0000818: [Location] MODULE LOCATION: UNICITÉ DES TRANSITIONS + SUPPRESSION DES TRANSITIONS.
- 0000830: [Vente] MODULE VENTE: MODIFICATIONS A FAIRE AU NIVEAU DES PROSPECTION (LOURD+WEB).
- 0000814: [Projet] MODULE PROJET: CORRECTIFS DANS L'EDITION DE COMPARAISON DE OFFRES AVEC ET SANS ESTIMATION.
- 0000829: [Vente] MODULE VENTE: Gestion des règlements pour autrui.
- 0000821: [Vente] MODULE VENTE ET MODULE S.A.V: CORRECTIFS A FAIRE DANS LE CONTENU DU MAIL.
- 0000809: [Vente] MODULE VENTE: AJOUTER LE CALENDRIER DES VISITES TECHNIQUES DANS LE CLIENT LOURD.
- 0000819: [Vente] MODULE VENTE: VÉRIFICATION DE L’EXISTENCE DU RESPONSABLE S.A.V AVANT L'OUVERTURE DU CALENDRIER (WEB+LOURD).
- 0000750: [Vente] Dans le message de l'action, les retours en HTML.
- 0000817: [Location] MODULE LOCATION: MODIFICATIONS A FAIRE DANS LE DOSSIER DE LOCATION.
- 0000810: [Vente] MODULE VENTE: AJOUTER LA POSSIBILITÉ DE GÉRER LES PLANS A PARTIR DU CLIENT LOURD.
- 0000816: [Vente] MODULE VENTE: MISE A JOUR DU SCHÉMA DE FINANCEMENT (LOURD + WEB).
- 0000733: [Vente] Courbes inversées dans le rapport suivi des prévisions.
- 0000815: [WEB] MODULE S.A.V: CONTRÔLE SUR LA DATE RÉCLAMATION ET LA DATE INTERVENTION.
- 0000812: [Vente] MASQUE DES ACTIONS (MODULE VENTE ET MODULE SAV): BOGUE AU NIVEAU DE LA GESTION DES ALERTES (WEB + LOURD).
- 0000784: [SAV] RECLAMATION : envoi mail au fournisseur.
- 0000813: [Vente] MODULE VENTE: RÉAJUSTEMENT DES SUPERFICIES DES SOUS ARTICLES.
- 0000728: [Vente] Gestion des relances des prospections.
- 0000794: [Vente] PROSPECTION: RELANCE DES PROSPECTIONS AVEC NATURE "TRÈS INTÉRESSE" ET "MOYENNEMENT INTÉRESSE"(LOURD + WEB).
- 0000782: [Projet] Lots : Gérer un historique des status.
- 0000681: [SAV] FILTRE DE RECHERCHE DES ARTICLES DANS LE MASQUE "DOSSIER DE RECLAMATION" (WEB).
- 0000811: [Vente] MODULE VENTE: CORRECTIF A FAIRE POUR LE DROIT D'ADMINISTRATION DES PROSPECTIONS (WEB +LOURD).
- 0000789: [Vente] DOSSIER DE VENTE: AJOUT D'UNE COLONNE "DELAI MEILLEURTAUX" DANS LA LISTE DE DOSSIERS DE VENTES (WEB+LOURD).
4.0.7 (Publiée 2016-04-18)
=========================================
- 0000723: [Achat] Actions commerciales et ventilation par projets.
- 0000775: [Achat] Importer les factures via un fichier exel.
- 0000780: [Stock] Ajouter un bouton "Réajustement des superficies" dans la tranche.
- 0000781: [WEB] CORRECTIF A FAIRE POUR LA PROGRAMMATION DES VISITES TECHNIQUES.
- 0000793: [Achat] RAPPORTS : Suivi des retenues.
- 0000805: [Administration] MODULE ADMINISTRATION: AFFICHAGE DU PROFIL DANS LA LISTE DES UTILISATEUR WEB.
- 0000806: [WEB] CORRECTIFS A FAIRE AU NIVEAU DU PLANNING DES VISITES TECHNIQUES ET AU PLANNING DU RESPONSABLE S.A.V.
- 0000785: [Projet] Lots : Prévisions en négatif à corriger.
- 0000568: [Vente] Liste des prospections archivée et annulée - mauvais compte.
- 0000731: [Vente] Création d'une prospection pour un prospect ayant déjà une autre dans le même projet.
- 0000771: [Vente] Réviser le flag archivé avec la date d'archivage.
- 0000801: [Vente] REGLEMENTS : Filtre pièce non comptabilisée.
- 0000803: [Vente] DOSSIER DE VENTE : Fiche bancaire.
- 0000804: [Vente] MODULE VENTE: CORRECTIFS A FAIRE AU NIVEAU DE RÉAJUSTEMENT DES SUPERFICIES.
- 0000725: [Vente] Ajouter un sélecteur calendrier pour tout les champs date (client lourd).
- 0000730: [Vente] Archivage automatique des prospections.
- 0000807: [Vente] MODULE VENTE: AJOUT DE LA LISTE DÉROULANTE DES STATUTS DANS LA CONFIGURATION DES " SOUS-CATÉGORIE DES ACTIONS " (LOURD).
- 0000802: [Vente] RAPPORTS : Correctifs dans le rapport chiffre d'affaire.
- 0000550: [Achat] Gestion de la ventilation par projet des documents.
4.0.6 (Publiée 2016-04-01)
=========================================
- 0000598: [Stock] Avoir le taux d'application de la R.G. à partir d'un projet existant (étude de faisabilité).
- 0000732: [Vente] Calcul du stock dans l'état TAUX DE VENTE PAR RAPPORT AU STOCK.
- 0000735: [Vente] Détails de la répartition des clients pour les < 5 et ajouter par nationalité.
- 0000745: [Vente] Ajouter les colonnes MEILLEURTAUX dans la liste des dossiers de vente.
- 0000746: [WEB] L'AJOUT DES CHAMPS " MEILLEUR TAUX" ET PARAMÉTRAGE DES CHAMPS OBLIGATOIRES AU NIVEAU DES TRANSITIONS (WEB: MODULE VENTE).
- 0000756: [Stock] Bouton terrain dans l'E.F..
- 0000712: [Vente] Saisie du champ frais après remise alors qu'il n'y a pas de frais dans un transfert.
- 0000743: [Vente] Bloquer l'enregistrement des fiches tires qui ont la même CIN.
- 0000759: [Stock] Effet de l'association d'un terrain à l'E.F..
- 0000760: [Stock] Supprimer les 4 champs HT et TVA dans l'E.F..
- 0000761: [Stock] dans l'E.F. calcul du nombre d'articles.
- 0000762: [Stock] Méthode d'import du taux de ret. de garantie dans l'E.F..
- 0000763: [Stock] Révision de l'onglet Exécution de l'E.F : champs Coût d'exécution (HT, TVA, TTC).
- 0000765: [Stock] Suppression d'une prospection de terrain.
- 0000772: [Projet] Ajouter dans le statut des lots un flag "Etat marché effectif".
- 0000774: [Vente] Ajouter la colonne (date visite technique) dans la liste des dossiers de vente.
- 0000561: [Trésorerie] TRESORERIE ANNUELLE : DÉPLACEMENT DE PRÉVISIONS DE DÉPENSES DU MODULE "TRÉSORERIE" VERS LE MODULE "ACHAT".
- 0000722: [Trésorerie] TRESORERIE ANNUELLE : CORRECTIONS DES PREVISIONS.
- 0000747: [WEB] MODULE VENTE: AJOUT DES COLONNES MEILLEUR TAUX DANS LA LISTE DE DOSSIERS DE VENTE.
- 0000751: [Vente] Reçu de paiement - cas on on paye avec la même pièce deux dossiers.
- 0000752: [Projet] Format Ret. De grarantie dans la liste des lots.
- 0000753: [Vente] Pouvoir modifier le client dans le dossier de vente avec une habilitation.
- 0000754: [Vente] Blocage de la fiche client.
- 0000773: [Vente] Ajouter la colonne article dans (suivi des actions).
- 0000776: [Vente] LISTE DES ACTIONS : Affectation de l'article pour les impayés.
- 0000690: [Vente] AFFECTATION DE LA DATE REMISE DE CLÉS PRÉVU DANS LE DOSSIER DE VENTE.
- 0000692: [Vente] L'AJOUT D'UN SCHÉMA DE FINANCEMENT CONTRACTUEL (LOURD + WEB).
- 0000721: [SAV] Correctifs et révisions à faire avec la programmation des visites techniques.
- 0000738: [Projet] Modifier la fiche RETENUE DE GARANTIE et Ajouter la fiche AVANCE SUR MARCHE.
- 0000740: [Projet] Ajouter un onglet "Suivi des réceptions" dans le lot.
- 0000748: [Vente] Suivi des des Commerciaux - Appels entrants erroné.
- 0000755: [Vente] Génération des alertes et le flag "Sans Alerte".
- 0000766: [Stock] Copie erronée du bilan d'une E.F..
- 0000770: [Vente] Révision de la date de dernière communication dans la liste des dossiers de vente.
- 0000777: [Stock] BOGUE DANS LE BILAN DE L'ETUDE DE FAISABILITE.
- 0000779: [Stock] HISTORIQUE PROSPECTION TERRAIN.
- 0000717: [Vente] AFFECTATION DES SOUS ARTICLES LORS D'UN TRANSFERT TRANCHE (LOURD+WEB).
- 0000719: [Administration] L'AJOUT D'UN MESSAGE D'INFORMATION AVANT DE PROCÉDER A LA TRANSFORMATION DU PERSONNEL EN CONTACT".
- 0000718: [WEB] TRANSFERT PRODUIT: AFFECTATION DES SOUS ARTICLES.
- 0000720: [WEB] AFFICHAGE DU PLANNING POUR LE RESPONSABLE S.A.V.
- 0000634: [Stock] BOGUE LORSQU'ON AJOUTE UNE NOUVELLE UNITE DE BASE DANS L'ETUDE DE FAISABILITE.
- 0000663: [Vente] GESTIONS DES DOCUMENTS A JOINDRE.
- 0000696: [Administration] GESTION DES FICHIERS ATTACHÉS POUR LES RÈGLEMENTS (LOURD + WEB ).
- 0000700: [Administration] PARAMÉTRAGE DES FICHIER ATTACHES OBLIGATOIRES (LOURD+WEB).
- 0000701: [Vente] AFFECTATION DES ENTITÉS JURIDIQUE LORS D'UN TRANSFERT TRANCHE (DES ENTITÉS DIFFÉRENTES) (LOURD+WEB).
- 0000734: [Vente] Le prix de vente dans la situation global des ventes.
- 0000736: [Vente] Ajouter la colonne ville dans la liste des prospects et des clients.
- 0000737: [Projet] Projet-Configuration-Projet Champ Projet de référence.
- 0000739: [Projet] Le système doit obliger l'utilisateur à enregistrer la fiche décompte avant d'imprimer la fiche.
- 0000744: [Vente] Ajouter des images pour les status.
- 0000726: [Vente] Ajouter le bordeaux de remise dans les impression du dossier de vente.
4.0.5 (Publiée 2016-03-04)
=========================================
- 0000656: [Achat] MODULE ACHAT : ventilation des projets.
- 0000694: [Vente] CORRECTION DU CHAMP MOBILE DANS LA FICHE RÉSERVATION.
- 0000704: [Achat] GESTION DES PRÊTS: CHAMP "TYPE" EST OBLIGATOIRE.
- 0000628: [Stock] L'ECRAN BILAN DE L'ETUDE DE FAISABILITE.
- 0000683: [Stock] MODIFICATION DE LIBELLE DANS LE MASQUE DE CONFIGURATION DES STATUTS (LOURD+WEB).
- 0000684: [Stock] GENEARTION D'UN DOCUMENT A REGLER A PARTIR DU TERRAIN.
- 0000685: [Administration] EXCEPTION LORS DE LA MODIFICATION DES PARAMÈTRES D'AUTHENTIFICATION.
- 0000695: [Vente] CORRECTION DE LA COLONNE "VILLE" DANS LA LISTE DES PROSPECTIONS ( LOURD+WEB ).
- 0000702: [Achat] GESTION DES PRÊTS: SUPPRESSION D'UNE LIGNE A PARTIR DE LA LISTE DES DÉBLOCAGES ET LA LISTE DES RECETTES.
- 0000703: [Achat] GESTION DES PRÊTS: SUPPRESSION D'UNE LIGNE A PARTIR DE LA LISTE DES ÉCHÉANCIERS ET DE LA LISTE DES DÉPENSES.
- 0000715: [Achat] L'AJOUT D'UN FLAG "FINANCEMENT FOURNISSEUR" DANS LA CONFIGURATION DES TYPES DE CREDITS.
- 0000686: [Achat] AJOUT D'UN FILTRE ENTITÉ JURIDIQUE DANS LE TABLEAU DE BORD "SOLDES FOURNISSEURS".
- 0000687: [Achat] SOLDE FOURNISSEUR: MODIFICATION DES COULEURS DES CRÉDITS ET DES DÉBITS ET LA DU TEXTE AFFICHER DANS LA COLONNE DESCRIPTION.
- 0000691: [Achat] GESTION DES PRETS.
- 0000708: [Trésorerie] Ajouter une édition avec les StlCategories.
- 0000710: [Vente] MODULE VENTE: EXCEPTION A L'OUVERTURE DU RAPPORT DE PRÉVISION DE VENTE.
- 0000713: [Trésorerie] GÉNÉRATION DES COMMISSIONS DANS LE TABLEAU DE BORD "BILAN DES PROJETS".
- 0000714: [Trésorerie] L'AJOUT D'UN AXE POUR LES CUMULES DANS LA GRAPHE DE TRÉSORERIE ANNUELLE.
- 0000716: [Vente] EXCEPTION LORSQU'ON RAFRAÎCHIT LE TABLEAU DE BORD " SUIVI DES COMMERCIAUX ".
- 0000527: [Vente] MISE EN PAGE DE L'ATTESTATION DE PAIEMENT.
- 0000639: [Vente] EXCEPTION LORSQU'ON SAISIE UNE APOSTROPHE DANS LA DESCRIPTION D'UN TIERS.
- 0000697: [Vente] SUIVI DES ACTIONS: L'AJOUT D'UNE COLONNE "ETAT ACTUEL".
- 0000706: [Projet] Permettre le choix d'un fournisseur même non affecté au projet en cours dans les intervenants du PV.
- 0000682: [SAV] MODIFICATION DE LIBELLE DANS LE MASQUE DE CONFIGURATION DES STATUTS (LOURD+WEB).
- 0000707: [Projet] Problème de calcul dans un décompte avec décomposition.
- 0000638: [Achat] GESTION DES PRÊTS.
- 0000705: [WEB] DOSSIER DE VENTE: SAUVGARDE DE SUJET ET DE DESCRIPTION DU CALENDRIER DES VISITES TECHNIQUES.
- 0000711: [SAV] Création d'un état global des interventions.
- 0000689: [Stock] Suppression d'une étude de faisabilité.
4.0.4 (Publiée 2016-02-16)
=========================================
- 0000569: [Projet] Date de création et utilisateur dans le PV de réunion.
- 0000571: [Projet] Ajouter dans la ligne de PVs de réunion un flag "Nous mêmes".
- 0000609: [Stock] Gestion du coût du terrain dans l'étude de faisabilité.
- 0000645: [Stock] Ajouter un flag "Traiter au début du projet" dans une ligne du bilan.
- 0000650: [Stock] Ajouter la "Fonction spécifique" d'une ligne d'un bilan de l'étude de faisabilité.
- 0000651: [Stock] Ajouter "Fonction spécifique" dans l'unité de base.
- 0000654: [Vente] Impression duplicata pour l'attestation de paiement.
- 0000410: [Administration] AMELIORATION DES LA GESTION DES FICHIERS ATTACHES.
- 0000572: [Projet] Icone et texte pour les lots multifournisseurs dans la liste des lots.
- 0000597: [Stock] Champs de la liste des études de faisabilité.
- 0000604: [Stock] Afficher les bons libellés avec TTC et HT et ajouter les différents montants TTC et HT.
- 0000605: [Stock] Vérifier le coût global de l'étude de faisabilité.
- 0000607: [Stock] Ajouter le poids en % dans le bilan et TTC et HT.
- 0000608: [Stock] Ajouter un % dans la tranche de l'étude de faisabilité.
- 0000617: [Stock] Bilan de l'étude de faisabilité : Calcul des parents en fonctions des catégories filles.
- 0000629: [Stock] FORMAT DU CHAMP POURCENTAGE DANS L'ONGLET "TERRAIN" DE L'ETUDE DE FAISABILITE.
- 0000641: [Stock] L'AJOUT D'UNE COLONNE "PIVOT" DANS LE DOCUMENT EXCEL EXPORTE.
- 0000642: [SAV] IL FAUT REMETTRE L'HISTORIQUE DE RÉCLAMATION (WEB+LOURD).
- 0000643: [Administration] Bug lors de l'enregistrement d'un utilisateur.
- 0000644: [Stock] Permettre la saisie des superficies dans l'onglet terrain.
- 0000646: [Stock] Dans le bilan, calculer le poids des recettes par rapport à la catégorie recette racine.
- 0000664: [Stock] RÉPARTITION DES MONTANTS TTC ET MONTANT HT PAR LORSQU'ON A PLUS QU'UNE SEULE TRANCHE.
- 0000647: [Stock] Coloriage des recettes et des dépenses dans le bilan.
- 0000648: [Stock] Afficher les lignes des recettes dans le tableau de l'onglet Données.
- 0000649: [Stock] Réviser les tabulation dans la fiche de l'étude de faisabilité et les fiches associées.
- 0000652: [Stock] Ajouter "Equipements et voies d'aménagement" dans l'onglet terrain de l'étude de faisabilité.
- 0000653: [Stock] Impression des relances 1 à partir de la fiche tranche.
- 0000655: [Administration] CHAMP DESCRIPTION DETAILLEE NON OBLIGATOIRE.
- 0000659: [Projet] MODULE PROJET : DIAGRAMME GANTT.
- 0000661: [Vente] TRANSFÉRER LES RÈGLES DE GESTION DE TITRE FONCIER VERS LA DATE DÉBUT FINALISATION (WEB+LOURD).
- 0000662: [Vente] L'AJOUT DU SOUS MODULE "DOCUMENTS A JOINDRE" DANS LE MODULE VENTE (WEB+LOURD).
- 0000665: [Vente] AFFICHAGE DE L'HISTORIQUE APRES AVOIR EFFECTUE UN TRANSFERT CLIENT (LOURD+WEB).
- 0000666: [Stock] AJOUT D'UNE LIBELLE "VALIDITÉ" ET MODIFICATION DU FORMAT DU CHAMP DATE.
- 0000667: [Stock] Corriger le comportement de la date se trouvant à côté du statut de la prospection terrain.
- 0000668: [Stock] Ajouter "Equipements et voies d'aménagement" dans la prospection d'un terrain.
- 0000669: [Stock] Prendre "Equipements et voies d'aménagement" de la prospection terrain vers l'étude de faisabilité.
- 0000670: [Stock] Ajouter (j) ou (jours) au libellé Délais moyen de remboursement dans l'étude de faisabilité.
- 0000671: [Stock] Après enregistrement d'une étude de faisabilité, rafraîchir la liste des E.F.s.
- 0000672: [Stock] Avoir le m2 vendable égal à la somme des m2 vendables (App, Commerces, ...).
- 0000673: [Stock] Dans le bilan de l'E.F., saisie catégorie, changer le libellé Total TTC.
- 0000674: [Stock] Enregistrement du bilan de l'E.F..
- 0000675: [Stock] Ajouter une TVA pour les intérêt dans l'E.F..
- 0000676: [Stock] Traduire les descriptions de l'onglet données.
- 0000677: [Stock] Calcul du TTC pour une catégorie recette associée à une catégorie du stock dans le bilan de l'E.F..
- 0000679: [Administration] L'AJOUT D'UN PARAMÈTRE GLOBAL POUR LE DÉLAI DE NOTIFICATION DES ALERTES.
- 0000680: [Vente] GÉNÉRATION DES ALERTES BASÉES SUR LA DATE ÉCHÉANCE DE DOSSIER DE VENTE.
4.0.3 (Publiée 2016-02-05)
=========================================
- 0000658: [Projet] MODULE PROJET : Prévision des retenue de garantie.
- 0000591: [Projet] Edition du bordereau définitif - Pouvoir travailler avec du Drag and Drop.
- 0000625: [SAV] Ajouter une contrainte d'un seul dossier de réclamations par article.
- 0000560: [Administration] MODIFICATION DE L’ARCHITECTURE DE OMRAN ESTATE.
- 0000596: [Stock] Avoir un terrain dans l'étude de faisabilité et mettre l'entité juridique en option.
- 0000606: [Stock] Affichage de la marge bénéficiaire et I.S..
- 0000610: [Stock] Ajout des ratios dans l'étude de faisabilité.
- 0000618: [Projet] AFFECTATION DES CATÉGORIES AU NIVEAU DU BORDEREAUX DEFINITF.
- 0000619: [Stock] FORMAT DES CHAMPS "POURCENTAGE" DANS L'ETUDE DE FAISABILITÉ.
- 0000623: [Administration] Dans la liste des utilisateur cacher le champ Mot de passe.
- 0000624: [SAV] Dans la réclamation rendre la liste des emplacements paramétrable.
- 0000626: [SAV] Afficher dans la des réclamation une "Date de traitement" et une "Date de prise en charge".
- 0000627: [SAV] Corriger la date de réclamation affichée dans la liste et ajouter la date du dossier de vente.
- 0000632: [SAV] GESTION DES TRANSITIONS.
- 0000630: [WEB] AJOUT DE SOUS MODULE CONFIGURATION DANS LE MODULE S.A.V DU CLIENT LÉGER.
- 0000631: [WEB] PARAMÉTRAGE DE LA VISIBILITÉ DES TRANCHES.
- 0000633: [WEB] VÉRIFICATION DU CONTRÔLE DES CHAMPS OBLIGATOIRES DANS LA FICHE RÉCLAMATION (WEB).
- 0000635: [Achat] COPIE ET GENERATION DES DOCUMENTS.
- 0000636: [Trésorerie] AFFECTATION ET POINTAGE : Problème recherche multi-critère.
- 0000637: [Achat] REGLEMENTS : Liste des affectations des documents.
- 0000640: [Achat] AJOUT D'UN ELEMENT VIDE DANS LA LISTE DE CHOIX D'UN DOSSIER DE GROUPEMENT.
4.0.2 (Publiée 2016-01-29)
=========================================
- 0000594: [Trésorerie] RÈGLE DE COLORATION ET DE VALIDATION DES LIGNES DANS L'ECRAN AFFECTATION ET POINTAGE.
- 0000587: [WEB] AJOUT DE SOUS MODULE CONFIGURATION DANS LE MODULE VENTE DU CLIENT LÉGER.
- 0000576: [Trésorerie] Lors du pointage ajouter un bouton pour supprimer le filtre des lignes.
- 0000584: [Vente] CONTRÔLE DE SAISI DES TRANSITIONS (LOURD+WEB).
- 0000585: [Vente] SUPPRESSION D'UNE TRANSITION (LOURD+WEB).
- 0000590: [Administration] Afficher le nom de la base de données à côté du nom du serveur dans le client lourd.
- 0000595: [Stock] Onglet général dans l'étude de faisabilité.
- 0000586: [Vente] AJOUT D'UN FLAG "C'EST ANCIEN CLIENT" DANS LA PROSPECTION (WEB+LOURD).
- 0000588: [WEB] GÉNÉRATION DU SCHÉMA DE FINANCEMENT LORSQUE LES VALEURS PAR DÉFAUTS DE TRÉSORERIE SONT VIDES.
- 0000570: [Projet] Affichage de la date de la prochaine réunion vide dans la liste des PVs.
- 0000589: [Vente] FILTRE "SANS RELANCE" DANS LA PROSPECTION (LOURD+WEB).
- 0000574: [Vente] Processus de mise à jour de la date d'octroi des titre fonciers.
- 0000578: [Stock] La catégorie n'est pas obligatoire dans le paramétrage de l'unité de l'étude de faisabilité.
- 0000579: [Stock] La tranche combo n'est pas obligatoire dans la sous fenêtre Tranche de l'étude de faisabilité.
- 0000581: [Stock] Largeur des colonnes dans l'onglet Unité de base / Tranche de l'étude de faisabilité.
- 0000600: [Stock] Gestion des statuts pour la prospection des terrains.
- 0000602: [Stock] OUVERTURE EN MODE EDITION D'UNE TRANCHE DANS L'ETUDE DE FAISABILITE.
- 0000549: [Vente] Avoir une visibilité sur l'historique d'une pièce de règlement (Attestation de consignation, Chèque).
- 0000592: [Trésorerie] LA DATE DE RÈGLEMENT DANS L'ECRAN D'AFFECTATION ET POINTAGE.
- 0000593: [Trésorerie] VÉRIFICATION DE L' AFFECTATION DANS L'ECRAN AFFECTATION ET POINTAGE.
- 0000601: [Stock] Ajouter un tier nommé "Le courtier" dans la prospection de terrain.
- 0000603: [Stock] Ajouter un lien GMaps dans la prospection de terrain.
- 0000612: [Vente] Fiche réservation : Ajouter "Date et" à "Signature du client".
- 0000613: [SAV] Pouvoir saisir des réclamations par projet / immeuble / étage.
- 0000614: [WEB] Droits sur les transitions dans la version web.
- 0000615: [Stock] Réviser la désignation "Immeuble" et "Bloc" entre la liste en paramétrage et la fiche article.
- 0000620: [SAV] Cacher l'onglet historique de la réclamation et le remettre dans la prochaine version.
- 0000622: [WEB] GÉNÉRATION DES DATES DANS LE DOSSIER DE VENTE DANS LE WEB.
- 0000621: [SAV] Modification de la date de réclamation (éditable).
- 0000566: [WEB] Indicateur d'archivage dans la fiche de prospection web.
- 0000567: [Vente] Correction du message affiché pour une prospection déjà archivée.
4.0.1 (Publiée 2016-01-19)
=========================================
- 0000539: [Projet] MODIFICATION DES LIBELLES DES LIGNES DU BDP DEFINITIF AVEC MODALITES DE PAIEMENT.
- 0000508: [Projet] Déplacer les boutons Modifier et Ajouter ligne du bordereau vers l'écran de recherche des lignes.
- 0000575: [Trésorerie] Avoir la fiche de pointage en non modal pour pouvoir travailler sur autre chose en parallèle.
4.0.0 (Publiée 2016-01-13)
=========================================
- 0000554: [Stock] Pour un terrain la tranche n'est pas enregistrée (bug).
- 0000555: [Projet] Ajouter un champ "Effectif" des intervenants dans le PV des réunions.
- 0000557: [Projet] Gestion des droits d'accès pour le tiers et les docmuents du module achat au niveau du module projet.
- 0000559: [Vente] TABLEAU DE BORD: ACTIVITÉ COMMERCIALE PAR MOIS.
- 0000440: [Achat] LISTE DES FACTURES ET DES DOCUMENTS A REGLER.
- 0000532: [Vente] Problème d'affectation et de réaffectation des sous articles pour un article.
- 0000544: [WEB] ICONE AFFICHÉE DANS LE MODULE STOCK (LOURD) APRES AVOIR FAIRE UN DÉSISTEMENT A PARTIR DU WEB.
- 0000545: [WEB] LIBÉRATION DES ARTICLES ET DES SOUS ARTICLES LORS D'UNE OPÉRATION DE TRANSFERT PRODUIT.
- 0000546: [WEB] SUPPRESSION DES RÈGLEMENT ET DES PAIEMENTS.
- 0000548: [Vente] Ne pas obliger l'utilisateur à saisir la date de légalisation s'il n'y a pas de frais.
- 0000551: [Stock] AUTONOMIE DES CHAMPS DE PREVISONS DE BASE.
- 0000552: [WEB] L'AJOUT DE MODULE S.A.V DANS LE CLIENT LÉGER.
- 0000553: [Vente] LA MODIFICATION DES FRAIS APRES REMISE DANS LE MASQUE DE TRANSFERT PRODUIT.
- 0000558: [Vente] LA RÉOUVERTURE D'UN DOSSIER ANNULEE POUR UN MÊME PROSPECT POUR LE MÊME ARTICLE.
- 0000563: [Trésorerie] AMÉLIORATION DU SOUS MODULE "AFFECTATION ET POINTAGE".
- 0000564: [Trésorerie] AMÉLIORATION DU SOUS MODULE "CLÔTURE DE L'EXERCICE" (RENOMME EN "RAPPROCHEMENTS BANCAIRE" ).
3.3.2 (Publiée 2015-12-11)
=========================================
3.3.1 (Publiée 2015-11-21)
=========================================
3.3.0 (Publiée 2015-11-05)
=========================================
3.2.8 (Publiée 2015-09-14)
=========================================
3.2.7 (Publiée 2015-09-14)
=========================================
3.2.6 (Publiée 2015-08-28)
=========================================
3.2.5 (Publiée 2015-08-13)
=========================================
3.2.4 (Publiée 2015-08-07)
=========================================
3.2.3 (Publiée 2015-08-03)
=========================================
3.2.2 (Publiée 2015-07-28)
=========================================
3.2.0 (Publiée 2015-07-06)
=========================================
3.1.6 (Publiée 2015-06-12)
=========================================
3.1.5 (Publiée 2015-06-08)
=========================================
3.1.4 (Publiée 2015-05-22)
=========================================
3.1.3 (Publiée 2015-05-08)
=========================================
3.1.2 (Publiée 2015-04-17)
=========================================
3.1.0 (Publiée 2015-03-20)
=========================================
3.0.0 (Publiée 2015-02-13)
=========================================
2.4.31.0 (Publiée 2015-01-23)
============================================
2.4.30.0 (Publiée 2015-01-09)
============================================
2.4.29.0 (Publiée 2014-12-30)
============================================
2.4.27.0 (Publiée 2014-12-18)
============================================
2.4.26.0 (Publiée 2014-12-11)
============================================
2.4.25.0 (Publiée 2014-12-08)
============================================
2.4.24.0 (Publiée 2014-11-17)
============================================
2.4.23.0 (Publiée 2014-11-04)
============================================
2.4.22.0 (Publiée 2014-10-27)
============================================
2.4.21.0 (Publiée 2014-10-13)
============================================
2.4.20.0 (Publiée 2014-10-10)
============================================
2.4.19.0 (Publiée 2014-09-29)
============================================
2.4.18.0 (Publiée 2014-08-08)
============================================
2.4.17.0 (Publiée 2014-08-01)
============================================
2.4.16.0 (Publiée 2014-07-15)
============================================
2.4.14.0 (Publiée 2014-06-12)
============================================
2.4.13.0 (Publiée 2014-06-02)
============================================
2.4.12.0 (Publiée 2014-05-20)
============================================
2.4.11.0 (Publiée 2014-05-09)
============================================
2.4.10.0 (Publiée 2014-05-05)
============================================
2.4.9.0 (Publiée 2014-04-28)
===========================================
2.4.8.0 (Publiée 2014-04-22)
===========================================
2.4.7.0 (Publiée 2014-04-15)
===========================================
2.3.6.0 (Publiée 2012-05-24)
===========================================
2.3.2.0 (Publiée 2012-01-17)
===========================================
2.3.1.0 (Publiée 2011-12-19)
===========================================
2.2.9.0 (Publiée 2011-10-27)
===========================================
V 2.2.6.0 (Publiée 2011-09-14)
=============================================
Créé avec HelpNDoc Personal Edition: [Générer des livres électroniques EPub facilement](https://www.helpndoc.com/fr/creer-des-livres-numeriques-epub)

## Vue d'ensemble de l'interface utilisateur <a id="page-vuedensembledelinterfaceutilisat"></a>
_Page : `Vuedensembledelinterfaceutilisat.html`_

- [Authentification](#page-authentification)
- [Page principale du logiciel](#page-pageprincipaledulogiciel)
- [Les masques de liste](#page-lesmasquesdeliste)
- [Les filtres génériques](#page-lesfiltresgeneriques)
- [Les résumés génériques](#page-lesresumesgeneriques)
- [Les groupements](#page-lesgroupements)
- [Les sélecteurs de champs](#page-lesselecteursdechamps)
- [Les masques d’édition](#page-lesmasquesdedition)
- [Les attachements de fichiers](#page-lesattachementsdefichiers)
- [Les notes](#page-lesnotes)
Créé avec HelpNDoc Personal Edition: [Générateur de documentation d'aide HTML gratuit](https://www.helpndoc.com/fr)

### Authentification <a id="page-authentification"></a>
_Page : `Authentification.html` — Vue d'ensemble de l'interface utilisateur_

Pour accéder au logiciel, double cliquez sur l’icône OMRAN Estate de votre bureau. Vous accéderez à un écran d’authentification. Une fois le «Nom utilisateur» et le «Mot de passe» saisis, cliquez sur ‘Valider’.
[image : NouvelElement7.png]
Une fois l’opération d’authentification effectuée et l’utilisateur retrouvé, l’aspect de gestion des droits d’accès est appliqué. La gestion par rôle : les rôles utilisateur nous permettent de savoir à quels masques et à quels actions il a accès.
Pour une application installée récemment, un utilisateur « admin » avec le mot de passe « admin » serait actif. Il faut que l’administrateur change le mot de passe de ce dernier afin d’assurer une bonne sécurité des données.
Une fois authentifié en cliquant sur le bouton « Valider » vous serez redirigé vers le masque principal de l’application.
Créé avec HelpNDoc Personal Edition: [Créer des livres électroniques facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

### Page principale du logiciel <a id="page-pageprincipaledulogiciel"></a>
_Page : `Pageprincipaledulogiciel.html` — Vue d'ensemble de l'interface utilisateur_

Le logiciel OMRAN Estate est une solution modulaire qui est basée sur un aspect fonctionnel des tâches à accomplir. L’organisation générale de la solution va dans un sens chronologique du métier du promoteur immobilier. L’accès aux données et à leur traitement se fait d’une façon intuitive et ergonomique.
Une fois authentifié, vous accédez à la page principale du logiciel, avec une présentation de la liste des modules et de leurs constituants, cette page est constituée de trois zones :
[image : NouvelElement6.png]

### 1 - Entête :
L’entête affiche dans sa partie droite un message indiquant l’utilisateur connecté et le serveur.
La partie gauche présente les actions possibles dans le masque en cours.

### 2 - Menu :
Le menu permet la navigation entre les fonctionnalités de l’application.
Le menu affiché dépend des droits de l’utilisateur connecté.

### 3 - Zone de travail :
La zone de travail permet de visualiser les différentes entités (Clients, Rapports, etc.).
D’une façon générale, une recherche est proposée à l’utilisateur avant d’accéder à une fiche. Cette dernière est constituée de :

- Un bandeau contenant les filtres de recherche.
- Une liste présentant le résultat de la recherche.
- Un double-clic sur une ligne sélectionnée pour ouvrir la fiche correspondante.

### 4 - Les actions :
Cette page principale regroupe le plus part des actions génériques que vous pourrez exécuter lors du traitement de vos données. Cette page s’adaptera automatiquement aux différents types de pages qui seront affichées à l’intérieur de celle-ci.
[image : NouvelElement8.png]

- 1 – Lien vers la page «A propos».
- 2 – Lien vers le support online de la solution OMRAN Estate.
- 3 – Lien vers l’aide de la solution OMRAN Estate.
- 4 – Pour quitter l’application.
- 5 – Liste des masques de traitement ouverts que vous retrouvez dans la zone 8.
- 6 – Pour fermer toutes les pages.
- 7 – Récapitulatif des actions pouvant être effectuées sur un masque ouvert et actif.
- 8 – Liste des onglets ouverts, chaque onglet correspond à un masque de traitement ouvert.
Créé avec HelpNDoc Personal Edition: [Créer des fichiers d'aide Qt Help multi-plateformes](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creez-des-fichiers-daide-pour-le-framework-qt-help)

### Les masques de liste <a id="page-lesmasquesdeliste"></a>
_Page : `Lesmasquesdeliste.html` — Vue d'ensemble de l'interface utilisateur_

Dans la plus par des cas les masques de traitement du logiciel OMRAN Estate sont présentés sous forme de liste avec des actions sur celle-ci. L’accès au détail d’un enregistrement de la liste passe par un masque spécifique à la fonctionnalité traitée.
[image : NouvelElement9.png]
Dans toutes les listes de la solution pour afficher les actions pouvant être effectués sur celle-ci, il vous suffit de cliquer sur le bouton droit de votre souris pour afficher son menu contextuel. Vous remarquerez que la liste des actions présentes (zone 1) sont représentées dans la mini barre d’outils affichée au haut et à gauche de votre page principale.

- 1 – Les actions :

Il est important de savoir que les actions d’ajout, d’édition ou de suppression des enregistrements sont gérées en droit et que un double click du bouton gauche de votre sourie sur un enregistrement de la liste provoque l’édition de celui-ci.

- 2 – Les entêtes des colonnes permettent d’effectuer les opérations suivantes :

- 3 – S’il existe un entonnoir dans l’entête de votre colonne, c’est que vous pouvez appliquer des filtres génériques sur votre liste en se basant sur les données de cette colonne.
- 4 – S’il existe le signe de la sommation dans l’entête de votre colonne, c’est que vous pouvez appliquer des résumés génériques sur les données de cette colonne (Somme, moyenne, min, …).
- 5 – En appuyant sur le bouton se situant à l’extrémité haute et gauche de votre liste vous accédez au sélecteur de champs de votre liste, qui vous permet d’afficher ou de masquer des colonnes.
- 6 – Pour fermer toutes les pages.
- 7 – Récapitulatif des actions pouvant être effectuées sur un masque ouvert et actif.
- 8 – Liste des onglets ouverts, chaque onglet correspond à un masque de traitement ouvert
Créé avec HelpNDoc Personal Edition: [Créer des documentations web iPhone](https://www.helpndoc.com/fr/tour-des-fonctionnalites/generation-de-site-web-iphone)

### Les filtres génériques <a id="page-lesfiltresgeneriques"></a>
_Page : `Lesfiltresgeneriques.html` — Vue d'ensemble de l'interface utilisateur_

Tous les masques de liste du système présentent des possibilités de filtrage très évoluées, pour permettre une manipulation et un accès rapide aux données. Chaque colonne de la liste et en se basant sur le type de ses données va présenter des critères spécifiques.
[image : NouvelElement5.png]
Créé avec HelpNDoc Personal Edition: [Environnement de création d'aide complet](https://www.helpndoc.com/fr/outils-creation-aide)

### Les résumés génériques <a id="page-lesresumesgeneriques"></a>
_Page : `Lesresumesgeneriques.html` — Vue d'ensemble de l'interface utilisateur_

Tous les masques de liste du système présentent des possibilités d’appliquer des résumés très évolués sur les données de leurs colonnes.
[image : NouvelElement4.png]
Créé avec HelpNDoc Personal Edition: [Créer des fichiers d'aide Qt Help multi-plateformes](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creez-des-fichiers-daide-pour-le-framework-qt-help)

### Les groupements <a id="page-lesgroupements"></a>
_Page : `Lesgroupements.html` — Vue d'ensemble de l'interface utilisateur_

Pour grouper une liste sur la base des données d’une colonne, il suffit d’appuyer avec le bouton gauche de votre sourie sur l’entête de la colonne, de maintenir le bouton enfoncé et de déplacer cette entête de colonne vers la zone grise en dessus de votre liste. Vous pouvez faire des groupements avec plusieurs niveaux en faisant glisser plusieurs entêtes de colonne vers la zone de regroupement.
[image : NouvelElement3.png]
Créé avec HelpNDoc Personal Edition: [Outil de création d'aide complet](https://www.helpndoc.com/fr/outils-creation-aide)

### Les sélecteurs de champs <a id="page-lesselecteursdechamps"></a>
_Page : `Lesselecteursdechamps.html` — Vue d'ensemble de l'interface utilisateur_

Pour tous les masques de liste, les sélecteurs de champs vous permettent de visualiser ou de masquer les colonnes à votre guise. Pour cela il suffit de cocher (pour visualiser) ou de décocher (pour masquer) les colonnes choisies.
[image : NouvelElement2.png]
Créé avec HelpNDoc Personal Edition: [Écrire des livres électronique Kindle](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creer-ebooks-pour-kindle-amazon)

### Les masques d’édition <a id="page-lesmasquesdedition"></a>
_Page : `Lesmasquesdedition.html` — Vue d'ensemble de l'interface utilisateur_

Les masques d’édition de données peuvent varier selon l’aspect fonctionnel traité. Dans la pluparts des cas ils présentent des actions similaires de persistance des données (validation pour l’enregistrement dans la base de données).
Ils résultent généralement des actions d’ajout et d’édition des masques de liste, par exemple l’action de double clic sur un enregistrement dans un masque de liste génère un masque d’édition pour pouvoir modifier les données de cet enregistrement. D’autres actions sont intégrées dans ces masques, comme la possibilité d’attacher des fichiers à un enregistrement ou de l’annoter.
Il est important de noter que les actions de persistances sont gérées en droit.
[image : NouvelElement10.png]

- 1 – Enregistrer les données (raccourci Ctrl S).
- 2 – Enregistrer les données et créer un nouvel enregistrement.
- 3 – Enregistrer les données et fermer le masque d’édition (raccourci Ctrl Shift S).
- 4 – Réinitialiser les données de l’enregistrement à partir de la base de données.
- 5 – Attacher des fichiers.
- 6 – Actions.
- 7 – Fermer le masque d’édition.
- 8 – Titre du masque d’édition (Lorsque le titre est suivi par la lettre «*» implique que vous avez effectué des modifications sur l’enregistrement édité, mais que vous n’avez pas encore enregistré ces modifications.
Lorsque un masque d’édition est ouvert est qu’il contient les données d’un enregistrement qui a été modifié, toutes autres actions entrainera l’affichage d’un message vous demandant si vous voulez enregistrer les modifications effectuées.
[image : NouvelElement11.png]

- 1 – Avant de procéder à l’action enregistrer les données.
- 2 – Procéder à l’action sans enregistrer les données.
- 3 – Annuler de procéder à l’action et retourner.
Créé avec HelpNDoc Personal Edition: [Produire des aides en ligne pour les applications Qt](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creez-des-fichiers-daide-pour-le-framework-qt-help)

### Les attachements de fichiers <a id="page-lesattachementsdefichiers"></a>
_Page : `Lesattachementsdefichiers.html` — Vue d'ensemble de l'interface utilisateur_

Les masques d’attachement présentent un atout fonctionnel et ergonomique très important, puisque ils permettent l’accès direct aux documents scénarisés en temps réel et sans avoir à gérer des documents physiques. Ce processus engendre un gain de temps énorme et une réactivité accrue.
Tous les fichiers attachés du système sont stockés directement dans la base de données.
[image : NouvelElement12.png]

- 1 – Liste des fichiers attachés avec leurs types, leurs descriptions et leurs extensions.
- 2 – Ajouter un fichier attaché : en cliquant sur ce bouton, vous aurez votre explorateur Windows pour sélectionner le fichier physiquement. Une fois le fiché choisi, vous pouvez choisir son type dans la liste déroulante ou écrire directement son type.
- 3 – En sélectionnant une ligne dans votre liste et en cliquant sur le bouton 3 ou en double cliquant sur la ligne le système lancera le fichier attaché automatiquement, s’il s’agit d’un fichier PDF il lancera Adobe Acrobat Reader, s’il s’agit d’un fichier Word il lancera Microsoft Word automatiquement...
- 4 – Supprimer le fichier attaché sélectionné.
- 5 pour fermer le masque d’attachement de fichier.
Créé avec HelpNDoc Personal Edition: [Créer des documentations web iPhone](https://www.helpndoc.com/fr/tour-des-fonctionnalites/generation-de-site-web-iphone)

### Les notes <a id="page-lesnotes"></a>
_Page : `Lesnotes.html` — Vue d'ensemble de l'interface utilisateur_

Les masques d’annotations présentent un atout fonctionnel et ergonomique très important, puisque ils permettent de stocker tous les messages ou les discutions en interne ou en externe concernant un enregistrement, cela facilite énormément les phases de passation et de remplacement des utilisateurs du système.
[image : NouvelElement13.png]

- 1 – Liste des notes attachés avec leurs dates, l’utilisateur qui les a introduits et leur texte.
- 2 – Ajouter une action.
- 3 – En sélectionnant une ligne dans votre liste et en cliquant sur le bouton 3 ou en double cliquant sur la ligne vous pourrez l’éditer.
- 4 – Supprimer l’action sélectionnée.
Créé avec HelpNDoc Personal Edition: [Créer des livres électroniques EPub facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

### Règles de gestion générale <a id="page-reglesdegestiongenerale"></a>
_Page : `Reglesdegestiongenerale.html` — Vue d'ensemble de l'interface utilisateur_

| IDENTIFIANT | DESCRIPTION |
|---|---|
| GEN0001 | Enregistrer<br> Lors de l’enregistrement d’une fiche le système doit vérifier que tous les champs obligatoires ont une valeur. S’il existe au moins un champ obligatoire sans valeur, le système affiche le message bloquant suivant :<br>Fiche : « Titre de la fiche »<br> La valeur du champ [« Nom du champ 1 »] est obligatoire !!<br> La valeur du champ [« Nom du champ 2 »] est obligatoire !!<br> …<br> La valeur du champ [« Nom du champ n »] est obligatoire !!<br>[image : NouvelElement16.png] |
| GEN0002 | Enregistrer et fermer :<br> Appliquer les règles de GEN0001 puis ferme la fiche. |
| GEN0003 | Rafraîchir<br> Lors de rafraîchissement le système<br>- Rafraîchir la liste des éléments<br>- Réinitialise les champs |
| GEN0004 | Fichiers attachés<br> Les masques d’attachement présentent un atout fonctionnel et ergonomique très important, puisque ils permettent l’accès direct aux documents scénarisés en temps réel et sans avoir à gérer des documents physiques. Ce processus engendre un gain de temps énorme et une réactivité accrue.<br> Tous les fichiers attachés du système sont stockés directement dans la base de données.<br>[image : NouvelElement492.png]<br>- 1 – Liste des fichiers attachés avec leurs types, leurs descriptions et leurs extensions.<br>- 2 – Ajouter un fichier attaché : en cliquant sur ce bouton, vous aurez votre explorateur Windows pour sélectionner le fichier physiquement. Une fois le fiché choisi, vous pouvez choisir son type dans la liste déroulante ou écrire directement son type.<br>- 3 – En sélectionnant une ligne dans votre liste et en cliquant sur le bouton 3 ou en double cliquant sur la ligne le système lancera le fichier attaché automatiquement, s’il s’agit d’un fichier PDF il lancera Adobe Acrobat Reader, s’il s’agit d’un fichier Word il lancera Microsoft Word automatiquement...<br>- 4 – Supprimer le fichier attaché sélectionné.<br>- 5 pour fermer le masque d’attachement de fichier. |
| GEN0005 | Actions<br> Les masques d’annotations présentent un atout fonctionnel et ergonomique très important, puisque ils permettent de stocker tous les messages ou les discutions ou les alertes en interne ou en externe concernant un enregistrement, cela facilite énormément les phases de passation et de remplacement des utilisateurs du système.<br>[image : NouvelElement20.png]<br>\| [image : NouvelElement17.png] \| Liste des notes, des actions, des alertes… \| \|---\|---\| \| [image : NouvelElement18.png] \| Liste déroulante des résultats \| \| [image : NouvelElement19.png] \| Liste déroulante des actions \| <br>1. La liste déroulante des résultats est chargée en fonction d’écran parant.<br>2. Si action(3) d’action est « NOTE »<br> 2.1. Le choix d’un élément de la liste déroulante d’résultat(2) devient facultatif.<br> 2.2. La saisie d’un message devient obligatoire.<br>1. Si action(3) d’action est différent de « NOTE »<br> 3.1. Le choix d’un élément de la liste déroulante d’actions devient obligatoire.<br> 3.2. La saisie d’un message devient facultative.<br> Remarque1 : Si le résultat choisi de la liste déroulante est paramétrée « avec alerte », les champs «Date alerte » et « Message» deviennent obligatoires.<br> Remarque2 : Lorsqu’on ajoute une date alerte pour n’importe quel élément des listes déroulantes résultat(2) et actions(3), le système génère une alerte.<br>1. Pour désactiver une alerte :<br> 4.1. On ajoute une nouvelle alerte dans le même écran parant.<br> 4.2. Lorsqu’on passe d’écran « Prospection » vers l’écran « Dossier de vente », toutes les alertes qui ont été créé pour la prospection seront désactivées.<br> 4.3. Lorsqu’on créé un dossier de vente, le système génère automatiquement des alertes pour les impayés qui sont à échéance.<br> 4.4. Lorsqu’on ajoute une nouvelle alerte ; les alertes qui concernent les documents impayés seront toujours actives.<br> 4.5. Lorsqu’on passe d’un statut à un autre, dans le dossier de vente, tous les alertes qui sont attachées au statut précédent seront désactivées sauf les alertes des documents impayés.<br>1. Pour les transferts (Client, Produit) et désistement :<br> 5.1. Lorsqu’on valide le transfert ou le désistement, le système désactive tous les alertes qui concernent les opérations cité ci-dessus. |
| GEN0006 | Lors de la fermeture d’une nouvelle fiche ou d’une fiche avec des modifications, le système doit afficher le message de confirmation suivant :<br>Fiche : « Titre de la fiche »<br>[image : NouvelElement21.png]<br>« Enregistrer les modifications »<br> Si « Oui »<br>- Le système procède à l’enregistrement et ferme la fiche<br> Si « Non »<br>- Le système ferme la fiche sans enregistrement<br> Si « Annuler »<br>- Le système ne procède à l’enregistrement et ne ferme pas la fiche. |
| GEN0007 | Champs spécifiques :<br>[image : NouvelElement22.png]<br>Le masque « Champs spécifiques » permet aux utilisateurs du système (qui ont le droit) d’ajouter, éditer et supprimer des champs pour chaque sous modules selon leurs besoins<br> La liste de ce masque visualise :<br>- Le groupe du champ spécifique de sous module parant<br>- Le libellé du champ spécifique de sous module parant<br>- La valeur d’entrer du champ spécifique créé. |
| GEN0008 | Ajouter :<br> Lors de l’ajout d’un élément, le système affiche le masque qui lui correspond.<br> NB :<br>- Les libellés des champs obligatoires en rouge<br>- Le font des champs en lecture seule en jaune<br>- Le font des listes déroulantes en lecture seule en gris<br> Ensuite, pour valider l’opération, il faut appliquer les règles de GEN0001 ou GEN0002 |
| GEN0009 | Éditer :<br> Lors de l’édition d’un élément, le système affiche le masque qui lui correspond avec les valeurs qui sont saisis |
| GEN0010 | Supprimer :<br> Lors de la suppression d’un élément, le système affiche un message « Voulez-vous vraiment supprimer cet enregistrement ? »<br> Si « Oui »<br>- Le système procède à la suppression<br> Si « Non »<br>- Le système annule la suppression |
| GEN0011 | Imprimer :<br> Le système affiche la liste à imprimer, en mode « aperçu avant l’impression » ensuite, il donne le choix à l’utilisateur de sélectionner le nombre de copie, et il lance l’impression en cliquant sur le bouton « imprimer »<br>[image : NouvelElement23.png] |
| GEN0012 | Exporter :<br> Lors de l’exportation, le système affiche l’écran du choix de l’emplacement de l’enregistrement (Enregistrer sous) et ainsi, le type de fichier à enregistrer (PDF ou XLS)<br>[image : NouvelElement24.png] |
| GEN0013 | Cliquer :<br> Lorsque l’utilisateur fait une clique sur une ligne de liste, le système sélectionne l’élément. |
| GEN0014 | Double cliquer GEN0009 : |
| GEN0015 | Sélecteur de champs :<br> Pour tous les masques de liste, les sélecteurs de champs vous permettent de visualiser ou de masquer les colonnes à votre guise. Pour cela il suffit de cocher (pour visualiser) ou de décocher (pour masquer) les colonnes choisies.<br> [image : NouvelElement25.png] |
| GEN0016 | Champs historiques :<br> Les champs historiques représentent la trace d’un acteur sur le système en indiquant dans une liste qui correspond un masque :<br>- La date de création<br>- La date de modification<br>- L’acteur qui a procédé à la création<br>- L’acteur qui a procédé à la modification |
| GEN0017 | Annuler et fermer :<br> Le système annule les modifications et ferme le masque ouvert. |
| GEN0018 | Importer :<br> Lors du l’import d’un fichier le système affiche la liste des éléments ensuite l’utilisateur affecte le document correspondant à un élément en cliquant sur « Charger le fichier »<br>[image : NouvelElement26.png] |
| GEN0019 | Copier :<br> Après avoir appliquée la règle GEN0013, le système affiche le masque de l’élément sélectionné avec les valeurs pré-affecter pour les tous les champs (Sauf les champs qui doivent être unique seront affiché vide) |
| GEN0020 | Audit :<br>[image : NouvelElement27.png]<br>L’audit permet aux utilisateurs du système (qui ont le droit) de visualiser un historique détaillé d’écran courant en visualisant les champs suivants :<br>- La date d’action.<br>- L’utilisateur qui a effectué l’action.<br>- Le type d’action.<br>- Le champ modifié.<br>- L’opération en visualisant l’ancienne valeur et la nouvelle valeur. |
| GEN0021 | Sélecteur :<br>[image : NouvelElement29.png]<br>Ce masque aide à la recherche des tiers « Clients », « Prospects », « Personnels », « Contacts », « Fournisseurs », « Organismes » et « Banques » selon deux critères :<br>1. Le type de tiers (Les cases à cocher)<br>2. La valeur sélectionnée de la liste déroulante.<br>[image : NouvelElement28.png]<br>Ce masque permet d’effectuer des recherches selon les critères suivantes, Tranche, Code, Client, Catégorie, Type, Présentation, Type, Superficie. |
| GEN0022 | Générer à partir de |
| GEN0023 | Créer liste à faire<br> [image : NouvelElement488.png]<br>Ce masque permet de donner un code pour la liste à faire crée<br> Remarques:<br>- Le code doit être unique<br>- Les code saisi des différents listes choisis seront affichés dans liste déroulante [Liste à faire](#page-listeafaire) |
Créé avec HelpNDoc Personal Edition: [Créer des sites web d'aide facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

## Guides de Démarrage Rapide <a id="page-guidesdedemarragerapide"></a>
_Page : `GuidesdeDemarrageRapide.html`_

Créé avec HelpNDoc Personal Edition: [Générateur d'aides Web gratuit](https://www.helpndoc.com/fr)

### Module Projet <a id="page-moduleprojet1"></a>
_Page : `ModuleProjet1.html` — Guides de Démarrage Rapide_

1 – créer le projet YES, YASMINE
[image : NouvelElement307.png]
2 – créer le premier lot Menuiserie Aluminium
[image : NouvelElement306.png]
Avec une estimation forfaitaire de 900 000 DT
Trésorerie annuelle : -900 000 DT
3- créer le bordereau de prix estimatif
Importer le bordereau estimatifsuivant :

| O.D.T. | Code | Description | Unité | Quantité | P.U. HT | TVA | Groupement 1 | Groupement 2 | Groupement 3 |
|---|---|---|---|---|---|---|---|---|---|
| 1 | 1.1 | Type PEA (1,80*2,20) entrée immeuble | m² | 100 | 460 | 0,18 | 1 PORTES et PORTES FENETRES |  |  |
| 2 | 1.2 | Type PA1 (1,80*2,20) Chambre | m² | 100 | 350 | 0,18 | 1 PORTES et PORTES FENETRES |  |  |
| 3 | 1.3 | Type PA2 (1,60*2,20) Salon | m² | 100 | 505,73 | 0,18 | 1 PORTES et PORTES FENETRES |  |  |
| 4 | 1.4 | Type PA3 (1,30*2,20) Salon | m² | 100 | 330,5 | 0,18 | 1 PORTES et PORTES FENETRES |  |  |
| 5 | 1.5 | Type PA3 (1,30*2) Salon | m² | 100 | 503 | 0,18 | 1 PORTES et PORTES FENETRES |  |  |
| 6 | 1.6 | Type PA3 (1,30*2,40) Salon | m² | 100 | 230,5 | 0,18 | 1 PORTES et PORTES FENETRES |  |  |
| 7 | 2.1 | Type FA0 (1,30*1,20) Loge gardien/ Séchoir | m² | 100 | 230,56 | 0,18 | 2 FENETRES EN Aluminium |  |  |
| 8 | 2.2 | Type FA1 (1,30*1,20) Chambre | m² | 100 | 502 | 0,18 | 2 FENETRES EN Aluminium |  |  |
| 9 | 2.3 | Type FA2 (0,4*1,20) Couloir | m² | 100 | 460,69 | 0,18 | 2 FENETRES EN Aluminium |  |  |
| 10 | 2.4 | Type FA3 (1,55*1,20) Séchoir | m² | 100 | 506 | 0,18 | 2 FENETRES EN Aluminium |  |  |
| 11 | 2.5 | Type FA2 (1*1,20) Couloir | m² | 100 | 504 | 0,18 | 2 FENETRES EN Aluminium |  |  |
| 12 | 2.6 | Type FA3 (1*1) Séchoir | m² | 100 | 200 | 0,18 | 2 FENETRES EN Aluminium |  |  |
| 13 | 3.1 | PECV 1 ( 3,00*3,00) banque et commerce | m² | 100 | 503 | 0,18 | 3 ENSEMBLE PORTE ET BAIE VITREE EN ALUMINIUM |  |  |
| 14 | 3.2 | PECV 2 (4,20*3,00) Banque et commerce | m² | 100 | 504 | 0,18 | 3 ENSEMBLE PORTE ET BAIE VITREE EN ALUMINIUM |  |  |
| 15 | 3.3 | PECV 3 (440*300) Banque et commerce | m² | 100 | 150,87 | 0,18 | 3 ENSEMBLE PORTE ET BAIE VITREE EN ALUMINIUM |  |  |
| 16 | 3.4 | ECV 1 ( 2,20*3,00) : deux chassis fixes de ( 1,10*3,00) | m² | 100 | 500 | 0,18 | 3 ENSEMBLE PORTE ET BAIE VITREE EN ALUMINIUM |  |  |
| 17 | 3.5 | ECV 2 ( 2,80*3,00): 1 chassis fixe | m² | 100 | 510 | 0,18 | 3 ENSEMBLE PORTE ET BAIE VITREE EN ALUMINIUM |  |  |
| 18 | 3.6 | ECV 3 (4,20*3,00) : 4 Chassis fixes de ( 1,15*3,00) chacun | m² | 100 | 350 | 0,18 | 3 ENSEMBLE PORTE ET BAIE VITREE EN ALUMINIUM |  |  |
| 19 | 3.7 | ECV 4 ( 2,80*1,20) : 3 chassis fixes de (0,70*1,20) chacun | m² | 100 | 250 | 0,18 | 3 ENSEMBLE PORTE ET BAIE VITREE EN ALUMINIUM |  |  |
| 20 | 3.8 | ECV 5 ( 5,00*3,00) : 5 chassis fixes de 100mm chacun | m² | 100 | 500 | 0,18 | 3 ENSEMBLE PORTE ET BAIE VITREE EN ALUMINIUM |  |  |
Pour une estimation confidentielle de 950 000 DT
Trésorerie annuelle : -950 000 DT
4 – Finaliser la configuration du lot :
[image : NouvelElement305.png]

| [image : NouvelElement304.png] | Date de lancement de l'appel d'offre:<br> - 01/10/2015<br> Date de retrait de dossier de consultation:<br> - 05/10/2015 08:00<br> Date échéance de remise des offres:<br> - 31/10/2015 17:00<br> Date ouverture des plis<br> - 02/11/2015 10:00 |
|---|---|

| [image : NouvelElement303.png] | Réception provisoire: 90<br> Réception définitive: 365 |
|---|---|
Corriger les prévisions
Par défaut le système affecte un avancement des travaux linéaire sur la période du lot :
[image : NouvelElement302.png]
Modifier les prévisions du lot selon des valeurs plus réalistes
[image : NouvelElement301.png]
Après la modification nous aurons l'allure suivante :
[image : NouvelElement354.png]
Pour une estimation confidentielle de 950 000 DT
Trésorerie annuelle : -912 285 DT
5 – Créer 4 fiche fournisseurs :
FOURNISSEUR 1, FOURNISSEUR 2, FOURNISSEUR 3, FOURNISSEUR 4
[image : NouvelElement300.png]
4 – Passer le lot au statut Suivant " Appel d'offres "
Ajouter les 4 fournisseurs dans la présélection de l’appel d’offre :
Leurs envoyer la lettre de consultation le 02/10/2015

| [image : NouvelElement299.png] | Date d'envoi de la lettre de consultation<br> - 02/10/2015 |
|---|---|
5- les trois premiers fournisseurs ont répondu à l’appel d’offre aux dates suivantes :
[image : NouvelElement298.png]

| Fournisseurs | Date d'envoi de la lettre de consultation | date de retrait dossier de consultation | date dépôt de l'offre |
|---|---|---|---|
| Fournisseur 1 | 02/10/2015 | 05/10/2015 | 31/10/2015 |
| Fournisseur 2 | 02/10/2015 | 06/10/2015 | 30/10/2015 |
| Fournisseur 3 | 02/10/2015 | 06/10/2015 | 31/10/2015 |
| Fournisseur 4 | 02/10/2015 | 20/10/2015 |  |
5 – Passer le lot au statut Suivant " Ouverture des plis/ dépouillement "
Créer le 1er dépouillement
[image : NouvelElement297.png]
Vous remarquerez que le fournisseur 4 n’est plus visible dans le dépouillement parce qu’il n’a pas déposé son offre.
6 – Ajouter les devis de chaque fournisseur.
[image : NouvelElement296.png]
Devis 1 fournisseur 1
Devis 1 fournisseur 2
Devis 1 fournisseur 3
Après comparaison des offres, nous avons décidé d’écarter le FOURNISSEUR 3 et de passer à un deuxième dépouillement après négociation avec les deux autres fournisseurs.
[image : NouvelElement295.png]
7 – 2ème dépouillement
[image : NouvelElement294.png]
Vous remarquerez que le fournisseur 3 n’est plus visible dans le dépouillement parce qu’il a été écarté.
8- Ajouter les devis de chaque fournisseur.
Le fournisseur 1 a appliqué une remise de 3%
Le fournisseur 2 a appliqué une remise de 1%
Devis 2 fournisseur 1
Devis 2 fournisseur 2
[image : NouvelElement293.png]
Après comparaison des offres, nous avons décidé de sélectionner l’offre du FOURNISSEUR 1.
Le système affecte le FOURNISSEUR 1 au marché, il créer le bordereau définitif pour une valeur TTC de :
1 000 000 DT
Trésorerie annuelle : -960 300 DT
9- Une fois le fournisseur sélectionné : il faut lui envoyer la lettre d’adjudication et renseigner la date
10 – passer au statut suivant " Marché " et renseigner la date de signature des contrats et la date de lancement des travaux
[image : NouvelElement292.png]
Attention : il faut vérifier que la date début prévisionnelle est égale à la date de lancement des travaux.

| [image : NouvelElement291.png] | Date adjudication<br> - 16/11/2015<br> Date signature du marché<br> - 25/11/2015<br> Date d'ordre de service<br> - 01/12/2015<br> Date de lancement des travaux<br> - 01/01/2016 |
|---|---|
11 – validation de l’avance sur marché
[image : NouvelElement290.png]
Régler le montant de l'avance avec un chèque à la date du 28/12/2015
12 – passage au statut suivant " Exécution " :
Créer le décompte N°1 à la date du 30/03/2016 avec un avancement de 10% et générer sa facture et prévoir de la régler par deux traites

- Traite 1 : 50 000 au 25/06/2016
- Traite 2 : 31 180.004 au 25/08/2016
[image : NouvelElement289.png]
Après l’ajout et la validation du décompte N° 1, il faut réajuster les prévisions :

| [image : NouvelElement287.png] | [image : NouvelElement288.png] |
|---|---|
13 – En avril le fournisseur demande une avance sur décompte de 50 000 DT réglé le 31/05/2016 par un chèque :
[image : NouvelElement286.png]
14- Créer le décompte N°2 à la date du 30/05/2016 avec un avancement de 25% et générer sa facture et prévoir de la régler par chèque
Vous remarquez que le système déduit le montant de l’avance sur décompte

- chèque : Prévoir de le régler au 31/05/2016
[image : NouvelElement285.png]

- Après l’ajout et la validation du décompte N° 2, il faut réajuster les prévisions :

| [image : NouvelElement283.png] | [image : NouvelElement284.png] |
|---|---|
15- Créer le décompte N°3 à la date du 30/08/2016 avec un avancement de 40% et générer sa facture et prévoir de la régler par chèque au 15/09/2016
[image : NouvelElement282.png]

- Après l’ajout et la validation du décompte N° 3, il faut réajuster les prévisions :

| [image : NouvelElement280.png] | [image : NouvelElement281.png] |
|---|---|
16- Créer le décompte N°4 à la date du 31/10/2016 avec un avancement de 80% et générer sa facture et prévoir de la régler par chèque au 15/11/2016
[image : NouvelElement279.png]

- Après l’ajout et la validation du décompte N° 4, il faut réajuster les prévisions :

| [image : NouvelElement277.png] | [image : NouvelElement278.png] |
|---|---|
17- Créer le décompte N°5 à la date du 31/12/2016 avec un avancement de 100% et générer sa facture et prévoir de la régler par deux traites

- Traite 3 : 100 000 au 15/02/2017
- Traite 2 : 62 360 au 15/04/2017
[image : NouvelElement276.png]
18 – passage au statut suivant "Réception provisoire" :
[image : NouvelElement275.png]
19- Réception provisoire
[image : NouvelElement274.png]
Paiement de la totalité de retenu de granite lors de la réception provisoire par un chèque au 15/04/2017
Créé avec HelpNDoc Personal Edition: [Créer des documents d'aide PDF facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

### Module Stock <a id="page-modulestock1"></a>
_Page : `ModuleStock1.html` — Guides de Démarrage Rapide_

1- Créer l’entité juridique « Yasmine Engineering Systems »
[image : NouvelElement361.png]
2- Créer le programme « Yasmine Engineering Systems »
[image : NouvelElement360.png]
3- Ajouter la tranche suivante:
[image : NouvelElement359.png]
4- Importer le stock des articles et des sous articles à partir de la fiche Excel stock des articles
[image : NouvelElement400.png]
Import du stock:
1- Importer les prévisions à partir de fichier Excel Yasmine tranche 1 prévision de base
[image : NouvelElement366.png]
Créé avec HelpNDoc Personal Edition: [Générateur de documentation d'aide HTML gratuit](https://www.helpndoc.com/fr)

### Module vente <a id="page-modulevente1"></a>
_Page : `Modulevente1.html` — Guides de Démarrage Rapide_

1- Créer 4 nouveaux prospects
[image : NouvelElement439.png]

| Titre | Sexe | Nom | Prénom | Pays de résidence | Mobile |
|---|---|---|---|---|---|
| Mr | M | 1 | Prospect | Tunisie | 11 111 111 |
| Mr | M | 2 | Prospect | Tunisie | 22 222 222 |
| Mr | M | 3 | Prospect | Tunisie | 33 333 333 |
| Mr | M | 4 | Prospect | Tunisie | 44 444 444 |
2- Créer la prospection suivante:
[image : NouvelElement441.png]
3- Créer un dossier de vente en appuyant sur le bouton "Créer/ ouvrir dossier"
[image : NouvelElement443.png]
4- Payer la totalité de la pré-réservation avec une pièce de règlement de type espèce le 16/06/2016
[image : NouvelElement444.png]
5- Payer l'avance avec 2 chèques
- Chèque numéro 0001, échéance 16/07/2016, banque AA
- Chèque numéro 0002, échéance 16/08/2016, banque AA
6- Ajouter la date "Concrétisation/ Promesse" 30/07/2016 et modifier le statut de dossier en "CONCRETISATION"
7- Ajouter le parking YEST1000 et ajuster le schéma de financement en ajoutant un versement complémentaire du montant 10 000 DT le 16/06/2016
8- Ajouter un nouveau acquéreur "prospect 2" et modifier la part de chaqu'un comme suit:
- Prospect 1 : 60%
- Prospect 2 : 40%
TRANSFERTS CLIENT
9- Ajouter un nouveau demande de transfert client

| [image : NouvelElement445.png] | - Date légalisation: 20/06/2016<br> - Date validation: 21/06/2016<br> - Ancien client (Part): 60%<br> - Nouveau client (Part): 100% |
|---|---|
Après la validation, le système :
- Modifie le client de prospect 1 à prospect 2
- Génère une ligne de frais de transfert client du montant de 15 000 DT le 21/06/2016
[image : NouvelElement446.png]
TRANSFERTS PRODUIT
10- Ajouter un nouveau demande de transferts produit
[image : NouvelElement447.png]
Après la validation, le système:
- Met à jour le montant de dossier de vente
- Met à jour la liste des sous articles
- Génère une ligne de frais de transfert produit du montant de 5 000 DT le 22/06/2016
DESISTEMENT
11- Ajouter un nouveau demande de désistement
[image : NouvelElement448.png]
Après la validation, le système
- Modifie le statut de dossier en "Désistement"
- Ajoute une ligne de frais de désistement
- Ajoute un ligne de remboursement si le montant réglé est supérieur à au montant de frais de désistement
- Le système libère l'article est sous articles
Créé avec HelpNDoc Personal Edition: [Créer des fichiers d'aide pour la plateforme Qt Help](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creez-des-fichiers-daide-pour-le-framework-qt-help)

### Étude de faisabilité <a id="page-etudedefaisabilite"></a>
_Page : `Etudedefaisabilite.html` — Guides de Démarrage Rapide_

### Création d’une prospection terrain
Dans le module Stock � Prospection terrain

- Ajouter une nouvelle « Prospection terrain » remplit par les données cité dans le tableau ci-dessous

| Libellé | Valeur |
|---|---|
| Code | Terrain code |
| Date | - |
| Description | Terrain description |
| Catégorie | Très intéressant |
| Niveau | R+5 |
| Type | Plat |
| Vocation | Zone touristique |
| Pays | Tunisie |
| Ville | Nabeul |
| Statut | En cours |
| Date validité de statut | - |
| Courtier | - |
| GMap | - |
| Adresse | Adresse |
| Propriétaires | Propriétaires |
| Notes | Notes |
| Archivée | - |
| Général |  |
| Superficie terrain (m²) | 362.00 |
| Voies d’aménagement / Équipement |  |
| Surface terrain nette (m²) | Superficie terrain (m²) - Voies d’aménagement / Équipement |
| Emprise au sol Bâtiments | 100% |
| Superficie Emprise au sol Bâtiments | = Pourcentage d’Emprise au sol Bâtiments * Surface terrain nette (m²)<br> = 362.00 |
| Plancher couvert | 627.071% |
| Superficie de Plancher couvert | = Pourcentage de Plancher couvert * Surface terrain nette<br> =2270 |
| Coefficient d’équipement et voies d’aménagement | 10% |
| Superficie d’équipement et voies d’aménagement | = Pourcentage d’équipement et voies d’aménagement * Superficie<br> = 36.2 |
| Arrêté d’alignement (m) | 0 |
| Hauteur max autorisée (m) | 0 |
| Prix du m² | 43 000.00 |
| Prix | = Superficie * Prix du m²<br> = 15 566 000.00 |
| Prix proposé | 16 000 000.00 |

### Création des unités de bases:
Dans le module Stock � Configuration � Prospection de terrain � Unités de base

- Ajouter les unités de base paramétrée comme elle indique la capture ci-dessous :
[image : NouvelElement506.png]

### Ajout des relations avec les autres modules
Dans le module Trésorerie � Configuration � Relation avec les autres modules

- Paramétrer la partie « Étude de faisabilité » (Encadré en vert) comme elle indique la capture ci-dessous
[image : NouvelElement507.png]

### Préparation de la hiérarchie des catégories de dépenses et de recettes
Dans le module Trésorerie � Configuration � Catégorie de dépenses,

- Paramétrer la catégorie de dépenses comme suit
Travaux Travaux
[image : NouvelElement508.png]
Viabilisation
[image : NouvelElement509.png]
Étude Honoraires
[image : NouvelElement510.png]

### Branchement régit
[image : NouvelElement511.png]

### Frais de gestion divers
[image : NouvelElement512.png]
Impôt
[image : NouvelElement513.png]
Création de l’étude de faisabilité
Dans le module Stock � Étude de faisabilité

- Ajouter une nouvelle étude de faisabilité remplit par les données citées dans le tableau ci-dessous

| Libellé | Valeur |
|---|---|
| Code | EtudeCode |
| Description | EtudeDescription |
| Prospection | TerrainDescription |
Après avoir choisir la prospection terrain qu’on a créé, le système sous l’onglet « Terrain » doit afficher les valeurs suivantes

| Terrain |  |
|---|---|
| Superficie du terrain (m²) | 362.00 |
| Coefficient d’occupation au sol | 100% |
| Superficie d’occupation au sol (m²) | = (Superficie du terrain (m²) * Coefficient d’occupation au sol) / 100<br> = 362.00 |
| Coefficient d’utilisation foncier | 627.07% |
| Superficie couverte totale | = (Superficie d’occupation au sol * Coefficient d’utilisation foncier)/100<br> = 2270.00 |
| Coefficient de superficie vendable |  |
| Superficie vendable | = (Superficie utilisation foncier * Coefficient de superficie vendable)/100<br> = 1530 |
| Coefficient équipements et voies d’aménagement | 10% |
| Surface équipement et voies d’aménagement | = (Coefficient équipements et voies d’aménagement * Superficie du terrain (m²))/100<br> = 36.2 |
| Superficie non bâtie | En lecture seule,<br> Superficie du terrain (m²) - Superficie d’occupation au sol (m²)<br> =0.00 |
| Prix d’achat du m² | 43 000.00 DH |
| Prix du terrain | En lecture seule,<br> Superficie du terrain (m²) * Prix d’achat du m²<br> = 15 566 000.00 DH |
| Coefficient frais d’enregistrement | 3.50% (Par défaut 0.00%) |
| Frais d’enregistrement | En lecture seule,<br> (Prix du terrain * Coefficient frais d’enregistrement)/100<br> = 544 810.00 DH |
| Coefficient d’autre frais | 0.5% (Par défaut 0.00%) |
| Autre frais | En lecture seule,<br> (Prix du terrain * Coefficient d’autre frais)/100<br> = 77 830.00 DH |
| Prix global | En lecture seule,<br> Prix du terrain + Frais d’enregistrement + Autre frais<br> = 16 188 640.00DH |

### Ajout d’un financement
Sous l’onglet « financement », ajouter un crédit avec les données citées ci-dessous

| Libellé | Valeurs |
|---|---|
| Description | Crédit |
| Montant | 1 000 000.00DH |
| Date | 17/02/2016 |
| Taux de charge | 5% |
| Période de grâce | 4 |
| Périodicité en mois | 4 |
| Nombre de remboursements | 4 |
| Montant à régler | En lecture seule,<br> 1 050 000.00DH |
Ensuite cliquer sur « Générer »,
Le système doit afficher le résultat suivant :
[image : NouvelElement514.png]

### Ajout des unités de bases
Sous l’onglet « Unités de bases », ajouter les unités et les quantités suivantes :

| Unité | Quantité |
|---|---|
| M² terrain brut | En lecture seule,<br> 362.00 |
| M² terrain net | En lecture seule,<br> 362.00 |
| M² plancher couvert | En lecture seule,<br> 2270.00 |
| M² vendable | En lecture seule,<br> 1530.00 |
| M² vendable appartement | 1208.00 |
| M² vendable commerce | 322.00 |
| M²sous-sol | 530.00 |
| Immeuble | 1.00 |
| Appartement | 16.00 |
| Commerce | 15.00 |
| Unité | 1.00 |
| Année | 4.00 |
Ajout des tranches
Sous l’onglet « Tranche », ajouter une tranche avec les données suivantes :

| Libellé | Valeurs |
|---|---|
| O.D.T | 1 |
| Code | Tranche 1 |
| Date lancement des travaux | - |
| Date fin des travaux | - |
| Date octroi des titres foncier | - |
| Tranche | - |
| Date permis d’habiter | - |
| Date début de commercialisation | - |
| Date fin des concrétisations | - |
| Date début des finalisations | - |
| Date de commercialisation | - |
| Poids | - |

### Bilan
En cliquant sur le bouton « Bilan » le système doit générer et affecter les valeurs suivantes :

|  | Catégorie | Ratio | Observation | Montant HT | Montant TTC |
|---|---|---|---|---|---|
| Foncier | Foncier | - | - | 15 566 000.00DH | 15 566 000.00DH |

|  | Catégorie | Ratio | Observation | Montant HT | Montant TTC |
|---|---|---|---|---|---|
| Frais de gestion et divers | Frais divers | - | - | 77 830.00DH | 77 830.00DH |

|  | Catégorie | Ratio | Observation | Montant HT | Montant TTC |
|---|---|---|---|---|---|
| Impôt | Frais d’enregistrement terrain | - | - | 544 810.00DH | 544 810.00DH |

|  | Catégorie | Ratio | Observation | Montant HT | Montant TTC |
|---|---|---|---|---|---|
| Prêt | Prêt | - | - | 50 000.00DH | 55 000.00DH |
[image : NouvelElement515.png]
Ajout des lignes du bilan
On clique sur le bouton « Bilan » et :

- On sélectionne les catégories inscrites dans la colonne « Description » du tableau ci-dessous
- On ajoute les ratios des catégories sélectionnées

|  | Description | Ratio | Observation | Montant HT (DH) |
|---|---|---|---|---|
| Travaux | Gros œuvre | 3066.07 | M² plancher couvert | 6 959 978.90 |
| Ascenseur | 200000 | Immeuble | 200000 |  |
| Piscine | 350000 | Unité | 350000 |  |
| Sous-sol | 1700 | M² sous-sol | 901 000 |  |

|  | Description | Ratio | Observation | Montant HT (DH) |
|---|---|---|---|---|
| Viabilisation | Terrassement généraux | 120 | M² terrain net | 43 440 |
| Assainissement et voirie | 100 | M² terrain net | 36 200 |  |
| Eau potable | 22 | M² terrain net | 7 964 |  |
| Electrification et éclairage | 64 | M² terrain net | 23 168 |  |
| Desserte téléphonique | 15 | M² terrain net | 5 430 |  |
| Aménagement extérieurs et Abords... | 200 | M² terrain net | 72 400 |  |

|  | Description | Ratio | Observation | Montant HT (DH) |
|---|---|---|---|---|
| Étude et honoraires | Honoraires architecte | 2.5% | Travaux | 210 274.47 |
| Architecte décorateur | 1 | Unité | 100 000 |  |
| Géomètre topographe | 2000 | Appartement | 32 000 |  |
| Géomètre travaux | 2000 | Appartement | 32 000 |  |
| BET VRD | 3.5% | Viabilisation | 6 601.07 |  |
| BET Technique | 27 | M² plancher couvert | 61 290 |  |
| Bureau de contrôle | 0.40% | Travaux | 33 643.92 |  |
| Laboratoire | 0.20% | Travaux | 16 821.96 |  |

|  | Description | Ratio | Observation | Montant HT (DH) |
|---|---|---|---|---|
| BR | Branchements régies | 1 | Unité | 500 000 |

|  | Description | Ratio | Observation | Montant HT (DH) |
|---|---|---|---|---|
| Frais gestion et divers | Frais publicité et salons | 2.5% | Recette | 861 462.45 |
| Frais appartement témoin | 1 | Unité | 150 000 |  |
| Aménagement extérieur et …. | 1 | Unité | 100 000 |  |
| Coordination travaux | 2.5% | Travaux | 210 274.47 |  |
| Coordination commercial | 1% | Recette | 344 584.98 |  |
| Coordination administrative et …… | 50000 | Année | 200 000 |  |
| Commission sur vente….. | 1.5% | De 25% Recette | 127 496.44 |  |

|  | Description | Ratio | Observation | Montant HT (DH) |
|---|---|---|---|---|
| Impôt | Conservation foncier et achat de terrain | 1% | Foncier | 155 660 |
| CF éclatement des titres | 157.84 | M² vendable | 241 495.20 |  |
| Taxe d’autorisation de construire | 25 | Plancher couvert | 56 750 |  |
| Protection civile | 1 | Unité | 6810.00 |  |
| Taxe agence urbaine | 1 | Unité | 160 672.00 |  |
| Autorisation de lotir | 1 | Unité | 335 625.00 |  |
Recette :

|  | Description | Ratio | Observation | Montant HT (DH) |
|---|---|---|---|---|
| Appartement | Appartement | 22520 | M² vendable appartement | 27 204 160 |

|  | Description | Ratio | Observation | Montant HT (DH) |
|---|---|---|---|---|
| Commerce | Commerce | 22529 | M² vendable commerce | 7 254 338.00 |
[image : NouvelElement516.png]
Après avoir saisir les données du tableau précédent le système dans l’onglet général doit afficher les valeurs suivantes :
[image : NouvelElement517.png]
Créé avec HelpNDoc Personal Edition: [Créer des documentations web iPhone](https://www.helpndoc.com/fr/tour-des-fonctionnalites/generation-de-site-web-iphone)

## ACCUEIL <a id="page-accueil"></a>
_Page : `ACCUEIL.html`_

Module vente
[image : NouvelElement548.png]
Cet écran visualise la liste des alertes liées aux prospections et aux dossiers de vente.

### Champs

| LIBELLE | V | OBSERVATIONS |
|---|---|---|
| Code | X | Le code est composé de deux niveaux :<br>- Niveau 1 : Programme<br>- Niveau 2 : Tranche<br> Remarque : Si on n’a pas une alerte à échéance de type Prospection, Réservation, Livraison et Impayées, la hiérarchie [programme](#page-cfg-programmes), [tranche](#page-cfg-tranches) ne sera pas affichée dans le champ code. |
| Prospection | X | Le nombre d’alertes à échéance pour les [prospections](#page-prospections). |
| Réservation | X | Le nombre d’alertes à échéance pour les réservations.<br> �Les alertes de réservation sont les alertes dont le statut de dossier de vente = (égale) exactement le [statut](#page-statuts-com) « Réservation ». |
| Livraison | X | Le nombre d’alertes à échéance pour les livraisons.<br> �Les alertes de livraison sont les alertes dont le statut de dossier de vente >= (Supérieur ou égale) le [statut](#page-statuts-com) « Concrétisation ». |
| Cadeaux |  | Le nombre de cadeaux affecté aux dossiers de vente groupé par tranche |
| Cadeaux à commander |  | Nombre de cadeaux à commander des dossiers de vente dont le [statut](#page-statuts-com) est paramétré "Avec commande des cadeaux" |
| Impayées | X | Le nombre d’alertes à échéance pour les impayées.<br> �Les alertes des impayées sont les alertes du schéma de financement lors qu’un document est à échéance et il n’est pas encore payé. |
| Remarque : On cliquant sur une cellule du tableau, le système affiche la liste de tous les dossiers (ou prospections) qui appartiennent au tranche qui ont des alertes à échéance.<br> La liste visualise :<br>- La date échéance<br>- La [catégorie](#page-souscategoriesdesactions)<br>- Le message |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Rafraîchir | GEN0003 |
Module achat
[image : NouvelElement568.png]
Cet écran visualise la liste des documents à traiter et à refaire

### Champs

| LIBELLE | V | OBSERVATIONS |
|---|---|---|
| Statut | X | La description du [statut](#page-statuts-buy) |
| Document à traiter | X | Le nombre de documents à traiter groupés par statut<br> Remarque:<br>- Le système comptabilise les documents dont les statuts de type nouveaux ou intermédiaire.<br>- Le système considère un document traiter si son statut est de type "Terminer avec sucés" ou "Terminer avec échec". |
| Document à refaire | X | Le nombre de documents à refaire groupés par statut<br> Remarque: Le système comptabilise les documents qui ont passé par le même statut plus qu'une fois.<br> Exemple : Une facture qui a passé deux fois par le statut "Traitement en cours". |
| Remarque: On cliquant sur le nombre de documents à traiter ou le nombre de document à refaire, le système affiche la liste des documents qui sont dans le statut correspondant.<br> La liste visualise :<br>- La date<br>- Le code<br>- La description<br>- L’échéance<br>- L' [entité fonctionnelle](#page-entitesfonctionnelles)<br>- Le [fournisseur](#page-fournisseurs-adm)<br>- La note |  |  |
Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Rafraîchir | GEN0003 |
Créé avec HelpNDoc Personal Edition: [Éditeur de documentation CHM facile](https://www.helpndoc.com/fr)

## Module Projet <a id="page-moduleprojet"></a>
_Page : `ModuleProjet.html`_

Dans la solution OMRAN Estate la gestion des projets se concrétise par le groupement de plusieurs lots de différents types dans une seule entité (Projet) sur laquelle le système pourra assurer le suivi et le calcul de :
L’avancement des travaux :

- Le taux d’avancement forfaitaire
- Le taux d’avancement prévisionnel
- Le taux d’avancement réel
La trésorerie:

- La trésorerie forfaitaire
- La trésorerie prévisionnelle
- La trésorerie réelle
Ce module permet aux utilisateurs de faire le suivi de tous les éléments d’un ouvrage en l’associant à des lots en utilisant les types suivants :

- Lot avec marché :
- Lot avec marché basé sur la décomposition : En fonction du bordereau de prix définitif transformé en éléments de décompositions affecté à des entités de nivellement
- Lot d’approvisionnement : En fonction du bordereau de prix estimatif avec la possibilité de faire des achats avec plusieurs fournisseurs.
Créé avec HelpNDoc Personal Edition: [Créer des fichiers d'aide pour la plateforme Qt Help](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creez-des-fichiers-daide-pour-le-framework-qt-help)

### LOTS <a id="page-lots"></a>
_Page : `LOTS.html` — Module Projet_

[image : NouvelElement845.bmp]

### Liste

| LIBELLE | V | OBSERVATIONS |  |  |
|---|---|---|---|---|
| Assurance DEC Cote-part |  | Montant d'assurance décennale |  |  |
| Assurance DEC Montant |  | Marché * Taux de retenue d'assurance décennal + Travaux supplémentaire * Taux de retenue d'assurance décennal + Dépassement * Taux de retenue d'assurance décennal + Avenant * Taux de retenue d'assurance décennal (Le taux définit dans l'avenant) |  |  |
| Assurance DEC Reste à retenir |  | Assurance DEC Montant - Assurance DEC Retenue |  |  |
| Assurance DEC Retenue |  | Σ Montants d'assurance décennal facturés |  |  |
| Assurance DEC Taux |  | Taux d'assurance décennal |  |  |
| Assurance TRC Cote-part |  | Montant d'assurance tout risque chantier |  |  |
| Assurance TRC Montant |  | Montant Avenant * Taux Assurance TRC + Marché * Taux Assurance TRC + Travaux supplémentaire * Taux Assurance TRC + Dépassement * Taux Assurance TRC |  |  |
| Assurance TRC Reste à retenir |  | Assurance TRC Montant - Assurance TRC Retenue |  |  |
| Assurance TRC Retenue |  |  |  |  |
| Assurance TRC Taux |  | Taux d'assurance tout risque chantier |  |  |
| Autre description détaillée |  |  |  |  |
| Autre Inclure dans les prévisions de paiement |  |  |  |  |
| Autre Notes |  |  |  |  |
| Avance |  | Si c'est un lot uni<br>- Marché TTC * Taux d'avance<br> Si c'est un lot multi-fournisseur:<br>- Σ des avances sur bon de commande |  |  |
| Avance Amortie |  | Σ des montants des avances amorties dans les décomptes |  |  |
| Avance Amortie Reste |  | Avance Réglée - Avance Amortie |  |  |
| Avance Réglée |  | Σ des avances réglées |  |  |
| Avance Reste |  | Avance - Avance réglée |  |  |
| Avance Taux |  | Taux d'avance |  |  |
| Avancement |  | Si un lot uni:<br>- Σ des décomptes HT Facturés (sans prendre en compte les dépassements et les travaux supplémentaires) / Bordereau des prix définitif<br> Si un lot Multi-fournisseur:<br>- Σ (Facture HT avec bon de commande - Avoir HT avec bon de commande - Dépassement HT) / Marché HT |  |  |
| Avancement prévisionnel |  |  |  |  |
| Avoirs HT |  | Σ des montants des avoirs HT |  |  |
| Avoirs Régulation |  | Σ des montants de régulation des avoirs |  |  |
| Avoirs TTC |  | Σ des montants des avoirs TTC |  |  |
| Avoirs TVA |  | Σ des montants des avoirs TVA |  |  |
| Bons de commande HT |  | Σ des montants des bons de commande HT |  |  |
| Bons de commande TTC |  | Σ des montants des bons de commande TTC |  |  |
| Bons de commande TVA |  | Σ des montants des bons de commande TVA |  |  |
| Bons de livraison HT |  | Σ des montants des bons de livraisons HT |  |  |
| Bons de livraison TTC |  | Σ des montants des bons de livraisons TTC |  |  |
| Bon de livraison TVA |  | Σ des montants des bons de livraisons TVA |  |  |
| Catégorie |  |  |  |  |
| Clôturé |  |  |  |  |
| Code |  |  |  |  |
| Coût Prév. Appro. HT |  | Si le lot est résilié ou clôturé<br> Σ des montants des Factures Approvisionnement HT<br> Sinon<br> Σ des montants des Décomptes Approvisionnement HT |  |  |
| Coût Prév. Appro.TTC |  | Si le lot est résilié ou clôturé<br> Σ des montants des Factures Approvisionnement TTC<br> Sinon<br> Σ des montants des Décomptes Approvisionnement TTC |  |  |
| Coût Prév. Appro.TVA |  | Si le lot est résilié ou clôturé<br> Σ des montants des Factures Approvisionnement TVA<br> Sinon<br> Σ des montants des Décomptes Approvisionnement TVA |  |  |
| Coût Prév. ASS. DEC |  | Si le lot est résilié ou annulé<br> Σ des montants d’assurance décennal facturée<br> sinon<br> Montant Marché TTC * Taux assurance décennal + Montant Dépassement TTC * Taux assurance décennal + Montant Travaux supplémentaire TTC * Taux assurance décennal + Montant Avenant TTC * Taux assurance décennal. |  |  |
| Coût Prév. ASS. TRC |  | Si le lot est résilié ou annulé<br> Σ des montants d’assurance décanale facturée<br> sinon<br> Montant Marché TTC * Taux assurance tout risque chantier + Montant Dépassement TTC * Taux assurance tout risque chantier + Montant Travaux supplémentaire TTC * Taux assurance tout risque chantier + Montant Avenant TTC * Taux assurance tout risque chantier. |  |  |
| Coût Prév. Avenant HT |  | Σ Montant Avenant HT |  |  |
| Coût Prév. Avenant TTC |  | Σ Montant Avenant TTC |  |  |
| Coût Prév. Avenant TVA |  | Σ Montant Avenant TVA |  |  |
| Coût Prév. Bordereau HT |  | Si un lot<br> Si il y a un bordereau définitif<br> Σ Montant HT des lignes du bordereau HT<br> Sinon<br> Σ Montant HT des lignes du bordereau estimatif<br> Si un lot multi-fournisseur<br> Σ Montant HT des bons de commandes |  |  |
| Coût Prév. Bordereau TTC |  | Si un lot<br> Si il y a un bordereau définitif<br> Σ Montant HT des lignes du bordereau définitif<br> Sinon<br> Σ Montant TTC des lignes du bordereau estimatif<br> Si un lot multi-fournisseur<br> Σ Montant TTC des bons de commandes |  |  |
| Coût Prév. Bordereau TVA |  | Si un lot<br> Si il y a un bordereau définitif<br> Σ Montant TVA des lignes du bordereau<br> Sinon<br> Σ Montant TVA des lignes du bordereau estimatif<br> Si un lot multi-fournisseur<br> Σ Montant TVA des bons de commandes |  |  |
| Coût Prév. CP |  | Si le lot est résilié ou annulé<br> Σ des montants des comptes prorata facturée<br> sinon<br> Montant Marché TTC * Taux compte prorata + Montant Dépassement TTC * Taux compte prorata + Montant Travaux supplémentaire TTC * Taux compte prorata + Montant Avenant TTC * Taux compte prorata |  |  |
| Coût Prév Dépass. HT |  | Montant dépassement HT |  |  |
| Coût Prév Dépass. TTC |  | Montant dépassement TTC |  |  |
| Coût Prév Dépass.TVA |  | Montant dépassement TVA |  |  |
| Coût Prév. Fluct. HT |  | Montant Fluctuation HT (à partir des décomptes) |  |  |
| Coût Prév. Fluct. TTC |  | Montant Fluctuation TTC (à partir des décomptes) |  |  |
| Coût Prév. Fluct. TVA |  | Montant Fluctuation TVA |  |  |
| Coût Prév. HT |  | Marché ajusté HT + Fluctuation + Travaux supplémentaire HT + Dépassement HT + Avenant HT + Approvisionnement HT |  |  |
| Coût Prév. Net à payer |  | Coût TTC + Remboursement Compte prorata + Coût régulation + Approvisionnement - Coût RNR |  |  |
| Coût Prév. Réglé |  | Σ des montants de règlements du projet en question |  |  |
| Coût Prév. Régulation |  | Σ des montants de régulation + Σ des montants des timbres fiscaux - Σ des montants régulation des avoirs - Σ des montants des timbres fiscaux des avoirs<br> Remarque : Le système ne comptabilise pas les montantes régulations et les montants des timbres fiscaux pour les documents à régler de retenue de garanti |  |  |
| Coût Prév. Remboursement |  |  |  |  |
| Coût Prév. Reste |  |  |  |  |
| Coût Prév. Retenue forfaitaire |  | Si le lot est résilié ou clôturé <br>∑ des montants des retenues forfaitaire facturées<br> Sinon<br> ∑ des montants des retenues forfaitaire à partir des décomptes. |  |  |
| Coût Prév. RNR |  | Bordereau de prix final TTC * Taux retenue compte prorata + Bordereau de prix final TTC * Taux retenue assurance décennal + Bordereau de prix final TTC * Taux retenue assurance tous risques chantier + Montant avenant TTC * Taux retenue compte prorata + Montant avenant TTC * Taux retenue assurance décennal + Montant avenant TTC * Taux retenue assurance tous risques chantier + Retenue forfaitaire + Pénalité retenue de garantie + Dépassement TTC * Taux retenue compte prorata +Dépassement TTC * Taux retenue assurance décennal + Dépassement TTC * Taux retenue assurance tous risques chantier + (Si la case (Sous l'onglet "Options") "Appliquer la retenue sur les travaux supplémentaires" est cochée, le système additionne : Travaux supplémentaire TTC * Taux retenue compte prorata + Travaux supplémentaire TTC * Taux retenue assurance décennal + Travaux supplémentaire TTC * Taux retenue assurance tous risques chantier) ( Si la case (Sous l'onglet "Options") "Bloquer la restitution de la retenue de garantie" est cochée le système soustrait - (Retenue de garantie décompté + Avenant TTC * Taux Retenue de garantie - Retenue de garantie restituée ) ) |  |  |
| Coût Prév. Total HT |  | Si lot est clôturé ou résilié<br> ∑ des montants des factures HT<br> Sinon<br> Marché ajusté HT + Dépassement HT + Travaux supplémentaire HT + Fluctuation HT |  |  |
| Coût Prév. Total TTC |  | Si lot est clôturé ou résilié<br> ∑ des montants des factures TTC<br> Sinon<br> Marché ajusté TTC + Dépassement TTC + Travaux supplémentaire TTC + Fluctuation TTC |  |  |
| Coût Prév. Total TVA |  | Si lot est clôturé ou résilié<br> ∑ des montants des factures TVA<br> Sinon<br> Marché ajusté TVA + Dépassement TVA + Travaux supplémentaire TVA + Fluctuation TVA |  |  |
| Coût Prév. Trav. Supp. HT |  | Si lot est clôturé ou résilié<br> ∑ des montants des travaux supplémentaires HT à partir des factures<br> Sinon<br> ∑ des montants des travaux supplémentaires HT à partir des décomptes |  |  |
| Coût Prév. Trav. Supp. TTC |  | Si lot est clôturé ou résilié<br> ∑ des montants des travaux supplémentaires TTC à partir des factures<br> Sinon<br> ∑ des montants des travaux supplémentaires TTC à partir des décomptes |  |  |
| Coût Prév. Trav. Supp. TVA |  | Si lot est clôturé ou résilié<br> ∑ des montants des travaux supplémentaires TVA à partir des factures<br> Sinon<br> ∑ des montants des travaux supplémentaires TVA à partir des décomptes |  |  |
| Coût Prév. Total HT |  | Si lot est clôturé ou résilié<br> ∑ des montants des factures HT - ∑ des montants des avoirs HT<br> Sinon<br> Si il y a un bordereau définitif<br> Montant bordereau définitif HT + Montant dépassement HT + Montant travaux supplémentaire HT + Montant fluctuation HT + Montant avenant HT<br> Sinon<br> Montant marché estimatif HT |  |  |
| Coût Prév. Total TTC |  | Si lot est clôturé ou résilié<br> ∑ des montants des factures TTC - ∑ des montants des avoirs TTC<br> Sinon<br> Si il y a un bordereau définitif<br> Montant bordereau définitif TTC + Montant dépassement TTC + Montant travaux supplémentaire TTC + Montant fluctuation TTC + Montant avenant TTC<br> Sinon<br> Montant marché estimatif TTC |  |  |
| Coût Prév. Total TVA |  | Si lot est clôturé ou résilié<br> ∑ des montants des factures TVA - ∑ des montants des avoirs TVA<br> Sinon<br> Si il y a un bordereau définitif<br> Montant bordereau définitif TVA + Montant dépassement TVA + Montant travaux supplémentaire TVA + Montant fluctuation TVA + Montant avenant TVA<br> Sinon<br> Montant marché estimatif TVA |  |  |
| Coût réel Appro. HT |  | ∑ des montants des approvisionnements HT à partir des factures - ∑ des montants des approvisionnements HT à partir des avoirs |  |  |
| Coût réel Appro. TTC |  | ∑ des montants des approvisionnements TTC à partir des factures - ∑ des montants des approvisionnements TTC à partir des avoirs |  |  |
| Coût réel Appro. TVA |  | ∑ des montants des approvisionnements TVA à partir des factures - ∑ des montants des approvisionnements TVA à partir des avoirs |  |  |
| Coût réel Avenant HT |  | ∑ des montants des avenants HT facturé |  |  |
| Coût réel Avenant TTC |  | ∑ des montants des avenants TTC facturé |  |  |
| Coût réel Avenant TVA |  | ∑ des montants des avenants TVA facturé |  |  |
| Coût réel bordereau HT |  | Montants des factures HT- Montant des travaux supplémentaire HT facturé – Montant de dépassement HT facturé - ∑ des montants des avoirs HT |  |  |
| Coût réel bordereau TTC |  | Montants des factures TTC - Montant des travaux supplémentaire TTC facturé – Montant de dépassement TTC facturé - ∑ des montants des avoirs TTC |  |  |
| Coût réel bordereau TVA |  | Montants des factures TVA - Montant des travaux supplémentaire TVA facturé – Montant de dépassement TVA facturé - ∑ des montants des avoirs TVA |  |  |
| Coût réel dépassement HT |  | ∑ des montants des dépassement HT facturé |  |  |
| Coût réel dépassement TTC |  | ∑ des montants des dépassement TTC facturé |  |  |
| Coût réel dépassement TVA |  | ∑ des montants des dépassement TVA facturé |  |  |
| Coût réel Fluctuations HT |  | ∑ des montants de fluctuation HT facturé |  |  |
| Coût réel Fluctuations TTC |  | ∑ des montants de fluctuation TTC facturé |  |  |
| Coût réel Fluctuations TVA |  | ∑ des montants de fluctuation TVA facturé |  |  |
| Coût réel Net à payer |  | Montant facture TTC + Montant remboursement facturé + Montant régulation – Montant Retenue non remboursable |  |  |
| Coût réel Réglé |  | Σ des règlements du projet en question |  |  |
| Coût réel Régulation |  |  |  |  |
| Coût réel Remboursement |  |  |  |  |
| Coût réel Reste |  | Coût Net à payer - Coût Reste |  |  |
| Coût réel RNR |  | Σ des montants de compte prorata facturé + Σ des montants de retenues d'assurance décennale facturé + Σ des montants d'assurance tous risque chantier facturé + Σ des montants des retenues forfaitaire facturé + Σ des montants des pénalités de retenues de garantie facturé + (Si le fournisseur est le gestionnaire de compte prorata ) + Σ des montants net à payer des factures qui sont paramétré "imputé sur le compte prorata" - Σ des montants net à payer des avoirs.<br>Remarque: Si le lot est résilié, le système calcule le montant de retenue non remboursable de la manière suivante:<br> Σ des montants de compte prorata facturé + Σ des montants de retenues d'assurance décennale facturé + Σ des montants d'assurance tout risque chantier facturé + Σ des montants des retenues forfaitaire facturé + Σ des montants des pénalités de retenues de garantie facturé + (Si le fournisseur est le gestionnaire de compte prorata ) + Σ des montants net à payer des factures qui sont paramétré "imputé sur le compte prorata" - Σ des montants net à payer des avoirs + Reste à amortir (de l'avance sur marché) + Reste à amortir (de l'avance sur décompte) + Reste à amortir de la retenue de garantie (si l'option "Bloquer la restitution de la retenue de garantie") |  |  |
| Coût réel Trav. Supp. HT |  | ∑ des montants HT des travaux supplémentaire facturé |  |  |
| Coût réel Trav. Supp. TTC |  | ∑ des montants TTC des travaux supplémentaire facturé |  |  |
| Coût réel Trav. Supp. TVA |  | ∑ des montants TVA des travaux supplémentaire facturé |  |  |
| CP Gestionnaire |  |  |  |  |
| CP montant |  | Marché * Taux de retenue de compte prorata + Travaux supplémentaire * Taux de retenue de compte prorata + Dépassement * Taux de retenue de compte prorata + Avenant * Taux de retenue de compte prorata. |  |  |
| CP pénalité |  |  |  |  |
| CP Remboursé |  | Montant remboursement compte prorata à partir des décomptes - CP Factures Imputée. |  |  |
| CP Retenue |  |  |  |  |
| CP Retenue dans le projet |  | Σ Montants de compte prorata facturés |  |  |
| CP Taux |  | Taux de retenue de compte prorata |  |  |
| Date adjudication |  |  |  |  |
| Date d’achèvement des travaux |  |  |  |  |
| Date de lancement de l’appel d’offre |  |  |  |  |
| Date de lancement des travaux |  |  |  |  |
| Date de Recollement |  |  |  |  |
| Date de report de l’appel d’offre |  |  |  |  |
| Date de retrait du dossier de consultation |  |  |  |  |
| Date début |  |  |  |  |
| Date début forfaitaire |  |  |  |  |
| Date d’ordre de service |  |  |  |  |
| Date du report du dépouillement |  |  |  |  |
| Date échéance de remise des offres |  |  |  |  |
| Date fin |  |  |  |  |
| Date fin forfaitaire |  |  |  |  |
| Date ouverture des plis |  |  |  |  |
| Date Réception définitive |  |  |  |  |
| Date signature du marché |  |  |  |  |
| Décompte Approvisionnement HT |  | Σ des montants HT des approvisionnements à partir des décomptes |  |  |
| Décompte Approvisionnement TTC |  | Σ des montants TTC des approvisionnements à partir des décomptes |  |  |
| Décompte Approvisionnement TVA |  | Σ des montants TTC des approvisionnements à partir des décomptes |  |  |
| Décompte Avenant HT |  | Σ des montants HT des avenants |  |  |
| Décompte Avenant TTC |  | Σ des montants TTC des avenants |  |  |
| Décompte Avenant TVA |  | Σ des montants TVA des avenants |  |  |
| Décomptes Coût HT |  | Σ des montants HT des décomptes + Σ des montants HT des avenants |  |  |
| Décomptes Coût TTC |  | Σ des montants TTC des décomptes + Σ des montants TTC des avenants |  |  |
| Décomptes Coût TVA |  | Σ des montants TVA des décomptes + Σ des montants TVA des avenants |  |  |
| Décomptes Dépassement HT |  | Σ des montants HT à partir des décomptes - Σ des montants HT à partir de bordereau de prix définitif. |  |  |
| Décomptes Dépassement TTC |  | Σ des montants TTC à partir des décomptes - Σ des montants TTC à partir de bordereau de prix définitif. |  |  |
| Décomptes Dépassement TVA |  | Σ des montants TVA à partir des décomptes - Σ des montants TVA à partir de bordereau de prix définitif. |  |  |
| Décomptes Fluctuation HT |  | Σ des montants HT des fluctuations à partir des décomptes |  |  |
| Décomptes Fluctuation TTC |  | Σ des montants TTC des fluctuations à partir des décomptes |  |  |
| Décomptes Fluctuation TVA |  | Σ des montants TVA des fluctuations à partir des décomptes |  |  |
| Décomptes Net à payer |  | Σ des montants Net à payer des décomptes |  |  |
| Décomptes Réglé |  | Σ des montants des règlements des décomptes |  |  |
| Décomptes Remboursement |  | Σ des montants de remboursement des compte prorata |  |  |
| Décomptes Reste |  | Décomptes Net à payer - Décomptes Réglé |  |  |
| Décompte RNR |  | Montant retenue non remboursable décompté + Montant retenue non remboursable avenant + ∑ des montants TTC des factures imputées sur le compte prorata + ∑ des montant pénalité de retenue de garantie +( si le lot est résilié ou clôturé∑ des montants des pénalité des avances )<br> Remarque :<br>Retenue non remboursable décompté = Montant décompte TTC * Taux retenue compte prorata + Montant décompte TTC * Taux retenue assurance décennal + Montant décompte TTC * Taux retenue assurance tous risques chantier<br>Retenue non remboursable avenant = Montant avenant TTC * Taux retenue compte prorata + Montant avenant TTC * Taux retenue assurance décennal + Montant avenant TTC * Taux retenue assurance tous risques chantier<br>pénalité des avances = Montant Avance sur marché + Avance sur décompte – Amortissement avance – Amortissement avance sur décompte |  |  |
| Décompte Total HT |  | ∑ des montant HT des décomptes + ∑ des montant HT des approvisionnements +∑ des montant HT des travaux supplémentaires +∑ des montant HT de fluctuation |  |  |
| Décompte Total TTC |  | ∑ des montant TTC des décomptes + ∑ des montant TTC des approvisionnements +∑ des montant TTC des travaux supplémentaires +∑ des montant TTC de fluctuation |  |  |
| Décompte Total TVA |  | ∑ des montant TVA des décomptes + ∑ des montant TVA des approvisionnements +∑ des montant TVA des travaux supplémentaires +∑ des montant TVA de fluctuation |  |  |
| Décompte Travaux Sup. HT |  | ∑ des montant HT des travaux supplémentaire décompté |  |  |
| Décompte Travaux Sup TTC |  | ∑ des montant TTC des travaux supplémentaire décompté |  |  |
| Décompte Travaux Sup TVA |  | ∑ des montant TVA des travaux supplémentaire décompté |  |  |
| Décompte TTC |  | ∑ des montant TTC des décomptes - ∑ des montant TTC des dépassements |  |  |
| Décomptes TVA |  | ∑ des montant TVA des décomptes - ∑ des montant TVA des dépassements |  |  |
| Description |  |  |  |  |
| Durée de garantie (mois) |  |  |  |  |
| Durée forfaitaire (j) |  |  |  |  |
| Durée (j) |  |  |  |  |
| Écart Coût et Est. Confidentielle HT |  | Coût HT - Montant estimation confidentielle HT |  |  |
| Écart Coût et Est. Confidentielle TTC |  | Coût TTC - Montant estimation confidentielle TTC |  |  |
| Écart Coût et Est. Forfaitaire HT |  | Coût TTC - Montant estimation Forfaitaire HT |  |  |
| Écart Coût et Est. Forfaitaire TTC |  | Coût TTC - Montant estimation Forfaitaire TTC |  |  |
| Écart Coût et Marché ajusté HT |  | Coût TTC - Montant Marché ajusté HT |  |  |
| Écart Coût et Marché ajusté TTC |  | Coût TTC - Montant Marché ajusté TTC |  |  |
| Écart Coût et Marché HT |  | Coût TTC - Montant Marché HT |  |  |
| Écart Coût et Marché TTC |  | Coût TTC - Montant Marché TTC |  |  |
| Écart Est. Confidentielle et forfaitaire HT |  | Écart estimation confidentielle HT - Confidentielle et forfaitaire HT |  |  |
| Écart Est. Confidentielle et forfaitaire TTC |  | Écart estimation confidentielle TTC - Confidentielle et forfaitaire TTC |  |  |
| Écart Marché Ajusté et Est. Confidentielle HT |  | Écart Marché Ajusté HT - Est. Confidentielle HT |  |  |
| Écart Marché Ajusté et Est. Confidentielle TTC |  | Écart Marché Ajusté TTC - Est. Confidentielle TTC |  |  |
| Écart Marché Ajusté et Est. forfaitaire HT |  | Écart Marché Ajusté HT - Est. forfaitaire HT |  |  |
| Écart Marché Ajusté et Est. forfaitaire TTC |  | Écart Marché Ajusté TTC -Est. forfaitaire TTC |  |  |
| Écart Marché Ajusté et Marché HT |  | Écart Marché Ajusté HT - Marché HT |  |  |
| Écart Marché Ajusté et Marché TTC |  | Écart Marché Ajusté TTC - Marché TTC |  |  |
| Écart Marché et Est. Confidentielle HT |  | Écart Marché HT - Est. Confidentielle HT |  |  |
| Écart Marché et Est. Confidentielle TTC |  | Écart Marché TTC - Est. Confidentielle TTC |  |  |
| Écart Marché et Est. Forfaitaire HT |  | Écart Marché HT - Est. Forfaitaire HT |  |  |
| Écart Marché et Est. Forfaitaire TTC |  | Écart Marché TTC - Est. Forfaitaire TTC |  | Ecart Marché HT - Est. Forfaitaire HT |
| Entité fonctionnelle |  |  |  |  |
| Entité juridique |  |  |  |  |
| Est. Confidentielle HT |  | ∑ des montants HT des lignes du bordereau estimatif |  |  |
| Est. Confidentielle Net à payer |  | ∑ des montants TTC des lignes du bordereau estimatif - ∑ des montants des retenues non remboursable<br> Remarque :<br> Retenues non remboursable = Montant retenue de compte prorata + Montant retenue d'assurance tous risque chantier + Montant retenue assurance décennale |  |  |
| Est. Confidentielle TTC |  | ∑ des montants TTC des lignes du bordereau estimatif |  |  |
| Est. Confidentielle TVA |  | ∑ des montants TVA des lignes du bordereau estimatif |  |  |
| Est. Forfaitaire HT |  |  |  |  |
| Est. Forfaitaire Net à payer |  | Montants TTC estimation. Forfaitaire - ∑ des montants des retenues non remboursable (par rapport l’estimation forfaitaire)<br> Remarque :<br> Retenues non remboursable = Montant retenue de compte prorata + Montant retenue d'assurance tous risque chantier + Montant retenue assurance décennale (par rapport l’estimation forfaitaire) |  |  |
| Est. Forfaitaire TTC |  | Montant estimation forfaitaire TTC saisi par l’utilisateur |  |  |
| Est. Forfaitaire TVA |  | Montant estimation forfaitaire TVA |  |  |
| Factures Approvisionnement HT |  | ∑ des montants HT des approvisionnements facturés |  |  |
| Factures Approvisionnement TTC |  | ∑ des montants TTC des approvisionnements facturés |  |  |
| Factures Approvisionnement TVA |  | ∑ des montants TVA des approvisionnements facturés |  |  |
| Factures Avenant HT |  | ∑ des montants HT des Avenant facturés |  |  |
| Factures Avenant TTC |  | ∑ des montants TTC des Avenant facturés |  |  |
| Factures Avenant TVA |  | ∑ des montants TVA des Avenant facturés |  |  |
| Factures Coût HT |  | ∑ des montants HT des factures + ∑ des montants HT des Avenant facturés |  |  |
| Factures Coût TTC |  | ∑ des montants TTC des factures + ∑ des montants TTC des Avenant facturés |  |  |
| Factures Coût TVA |  | ∑ des montants TVA des factures + ∑ des montants TVA des Avenant facturés |  |  |
| Factures Dépassement HT |  | ∑ des montants HT des dépassement facturés |  |  |
| Factures Dépassement TTC |  | ∑ des montants TTC des dépassement facturés |  |  |
| Factures Dépassement TVA |  | ∑ des montants TVA des dépassement facturés |  |  |
| Factures Fluctuation HT |  | ∑ des montants HT des fluctuations facturés |  |  |
| Factures Fluctuation TTC |  | ∑ des montants TTC des fluctuations facturés |  |  |
| Factures Fluctuation TVA |  | ∑ des montants TVA des fluctuations facturés |  |  |
| Facture HT |  |  |  |  |
| Factures Net à payer |  | ∑ des montants des factures net à payer |  |  |
| Factures réglé |  | ∑ des montants des réglementes des factures |  |  |
| Factures Régulation |  | ∑ des montants régulation des factures + ∑ des montants régulation des Avenant facturés |  |  |
| Factures Remboursement |  | En lecture seule,<br> Si le lot est clôturé (ou annulé ou résilié)<br> Remboursement = Σ des montants de remboursement de retenue de compte prorata facturé.<br> Si Montant marché = 0<br> Remboursement = Montant de retenue de compte prorata (par rapport l'estimation confidentielle)<br> Si Montant marché > 0<br> Remboursement = Montant de retenue de compte prorata (par rapport le marché ajusté) - Σ des montants des factures qui sont imputé sur le compte prorata. |  |  |
| Factures Reste |  |  |  |  |
| Facture RNR |  |  |  |  |
| Facture Total HT |  | ∑ des montants HT des factures + ∑ des montants HT des approvisionnements facturés + ∑ des montants HT des fluctuations facturés |  |  |
| Factures Total TTC |  | ∑ des montants TTC des factures + ∑ des montants TTC des approvisionnements facturés + ∑ des montants TTC des fluctuations facturés |  |  |
| Factures Total TVA |  | ∑ des montants TVA des factures + ∑ des montants TVA des approvisionnements facturés + ∑ des montants TVA des fluctuations facturés |  |  |
| Factures Travaux Sup. HT |  | Si un lot multi-fournisseur<br> ∑ des montants des factures HT qui n’ont pas des bons de commande<br> Si un lot marché<br> ∑ des montants HT des travaux supplémentaires facturé |  |  |
| Factures Travaux Sup. TTC |  | Si un lot multi-fournisseur<br> ∑ des montants des factures TTC qui n’ont pas des bons de commande<br> Si un lot marché<br> ∑ des montants TTC des travaux supplémentaires facturé |  |  |
| Factures Travaux Sup. TVA |  | Si un lot multi-fournisseur<br> ∑ des montants des factures TVA qui n’ont pas des bons de commande<br> Si un lot marché<br> ∑ des montants TVA des travaux supplémentaires facturé |  |  |
| Factures TTC |  | ∑ des montants TTC des factures - ∑ des montants TTC des dépassements - ∑ des montants TTC des travaux supplémentaires |  |  |
| Factures TVA |  | ∑ des montants TVA des factures - ∑ des montants TVA des dépassements - ∑ des montants TVA des travaux supplémentaires |  |  |
| Fournisseur |  |  |  |  |
| Fournisseur code |  |  |  |  |
| Marché Ajusté HT |  | Montant marché HT + Montant Modification HT |  |  |
| Marché Ajusté Net à payer |  | Marché Ajusté TTC – Retenue non remboursable |  |  |
| Marché Ajusté TTC |  | Montant marché TTC + Montant Modification TTC |  |  |
| Marché Ajusté TVA |  | Montant marché TVA + Montant Modification TVA |  |  |
| Marché HT |  | ∑ des montants HT des lignes du bordereau définitif |  |  |
| Marché Net à payer |  | Marché TTC – Montant RNR (par rapport le montant marché) |  |  |
| Marché TTC |  | ∑ des montants TTC des lignes du bordereau définitif |  |  |
| Marché TVA |  | ∑ des montants TVA des lignes du bordereau définitif |  |  |
| Message d’alerte |  |  |  |  |
| Modification HT |  |  |  |  |
| Modification TTC |  |  |  |  |
| Modification TVA |  |  |  |  |
| Multi-Fournisseurs |  |  |  |  |
| Options Appliquer la remise sur les travaux supplémentaires |  |  |  |  |
| Options Appliquer la retenue sur les travaux supplémentaires |  |  |  |  |
| Options avec décomposition |  |  |  |  |
| Options Bloquer la restitution de la retenue de garantie |  |  |  |  |
| Options Possibilité de saisir l’avancement (sinon le coefficient) par ligne de décompte |  |  |  |  |
| Options Saisie des décomptes basée sur les quantités exécutées (sinon sur le cumul) |  |  |  |  |
| Options Taux de remboursement des Appro. |  |  |  |  |
| Options Total des points pour la décomposition |  |  |  |  |
| Options TVA |  |  |  |  |
| Options Variation dans la masse |  |  |  |  |
| Prévision HT |  | Montant HT estimation forfaitaire, sinon montant HT estimation confidentielle sinon montant HT marché sinon montant HT marché ajusté |  |  |
| Prévision Net à payer |  | Prévision TTC – Montant RNR (Montant RNT calculé par rapport le montant prévision TTC) |  |  |
| Prévision TTC |  | Montant TTC estimation forfaitaire, sinon montant TTC estimation confidentielle sinon montant TTC marché sinon montant TTC marché ajusté |  |  |
| Prévision TVA |  | Montant TVA estimation forfaitaire, sinon montant TVA estimation confidentielle sinon montant TVA marché sinon montant TVA marché ajusté |  |  |
| Projet |  |  |  |  |
| R.G. Montant |  | Montant Marché TTC * Taux de retenue de garantie |  |  |
| R.G. Pénalité |  |  |  |  |
| R.G. Restituée |  | ∑ Montant des règlements des retenues de garanties |  |  |
| R.G. Restituée Reste |  | R.G. Montant - R.G. Restituée - R.G. Pénalité |  |  |
| R.G. Retenue |  | ∑ Montant des retenues de garanties facturées + ∑ Montant des retenues de garanties des avenants facturés |  |  |
| R.G. Retenue avenant |  | ∑ Montant des retenues de garanties des avenants facturés |  |  |
| R.G. Taux |  |  |  |  |
| Résiliation Causes |  |  |  |  |
| Résiliation Date |  |  |  |  |
| Statut |  |  |  |  |
| Tranches |  |  |  |  |
| Type |  |  |  |  |
| Type de paiement |  |  |  |  |
| Type de paiement sur (jours) |  |  |  |  |

### Champs

| PROPRIETE | U | O | OBSERVATIONS |
|---|---|---|---|
| Code | X | X | En lecture seule et généré automatiquement par le système |
| Description |  | X |  |
| Projet |  | X | Liste déroulante des [projets](#page-projets) |
| Catégorie |  | X | Sélecteur des [catégories de dépense](#page-categoriedesdepenses) |
| Type |  | X | Liste déroulantes des [types de lot](#page-typeslot) |
| Statut |  | X | Liste déroulante des statuts de lot |
| Type de paiement |  | X | Liste déroulante des [Types de pièce](#page-typespiece) |
| Sur (j) |  | X | Nombre de jours |
| Fournisseur |  |  | En lecture seule, il est affecté automatiquement lors de la sélection d’une offre dans le dépouillement. |
| DUREE FORFAITAIRE |  |  |  |
| Date début |  | X | Modifiable selon l’habilitation attribuée à un rôle |
| Durée (j) |  | X | En jour, Modifiable selon l’habilitation attribuée à un rôle |
| Date fin |  | X | Date début + durée, Modifiable selon l’habilitation attribuée à un rôle. |
| DUREE PREVISIONNELLE |  |  |  |
| Date début |  | X |  |
| Durée (j) |  | X | En jours |
| Date fin |  | X | Date début + durée |
| Durée de garantie en mois |  |  |  |
| Avancement décompté |  |  | En lecture seule |
| Avancement facturé |  |  | En lecture seule |
| Général |  |  |  |
| GENERALE : HT |  |  |  |
| Est. forfaitaire |  |  | Modifiable selon les habilitations |
| Est. confidentielle |  |  | Σ des montants HT des lignes à partir du bordereau estimatif |
| Marché |  |  | Σ des montants HT des lignes à partir du bordereau estimatif |
| Modification BDPF |  |  | Σ des montants HT des lignes du bordereau définitif - Σ des montants HT des lignes de devis du fournisseur sélectionne |
| Marché ajusté |  |  | Marché HT + Modification BDPF HT |
| GENERALE : TTC |  |  |  |
| Est. forfaitaire |  |  | Modifiable selon les habilitations |
| Est. confidentielle |  |  | Σ des montants TTC des lignes à partir du bordereau estimatif |
| Marché |  |  | Σ des montants TTC des lignes à partir du bordereau définitif |
| Modification BDPF |  |  | Σ des montants TTC des lignes du bordereau définitif - Σ des montants TTC des lignes de devis du fournisseur sélectionne |
| Marché ajusté |  |  | Marché TTC + Modification BDPF TTC |
| GENERAL : COÛT PREVISIONNEL HT |  |  |  |
| Bordereau |  |  | Σ des montants HT des lignes du bordereau définitif (sinon Σ des montants HT des lignes bordereau estimatif sinon estimation forfaitaire) |
| Approvisionnement |  |  | En lecture seule,<br> Σ des montants HT des approvisionnements |
| Dépassements |  |  | En lecture seule,<br> Σ des montants HT des dépassements |
| Travaux supp. |  |  | En lecture seule,<br> Σ des montants HT des travaux supplémentaire |
| Fluctuations |  |  | En lecture seule,<br> Σ des montants HT des fluctuations |
| Avenants |  |  | En lecture seule,<br> Σ des montants HT des avenants |
| Coût Total |  |  | En lecture seule,<br> Si lot est clôturé ou résilié<br> ∑ des montants des factures HT<br> Sinon<br> Montants marché ajusté HT + Montant travaux supplémentaire HT décompté + Montant dépassement HT décompté + Montant fluctuation HT décompté + Montant approvisionnement HT décompté + Montant avenant HT |
| GENERAL : COÛT PREVISIONNEL TTC |  |  |  |
| Bordereau |  |  | Σ des montants TTC des lignes du bordereau définitif (sinon Σ des montants TTC des lignes bordereau estimatif sinon estimation forfaitaire) |
| Approvisionnement |  |  | En lecture seule,<br> Σ des montants TTC des approvisionnements |
| Dépassements |  |  | En lecture seule,<br> Cumul des montant HT de l'article en question dans les décompte - Montant HT d'article en question (dans le bordereau définitif ) |
| Travaux supp |  |  | En lecture seule,<br> Σ des montants TTC des travaux supplémentaire |
| Fluctuations |  |  | En lecture seule,<br> Σ des montants TTC des fluctuations |
| Avenants |  |  | En lecture seule,<br> Σ des montants TTC des avenants |
| Coût Total |  |  | En lecture seule,<br> Si lot est clôturé ou résilié<br> ∑ des montants des factures TTC - ∑ des montants avoirs TTC<br> Sinon<br> Montants marché ajusté TTC + Montant travaux supplémentaire TTC décompté + Montant dépassement TTC décompté + Montant fluctuation TTC décompté + Montant approvisionnement TTC décompté + Montant avenant TTC |
| Retenue |  |  | En lecture seule,<br> Bordereau de prix final TTC * Taux retenue compte prorata + Bordereau de prix final TTC * Taux retenue assurance décennal + Bordereau de prix final TTC * Taux retenue assurance tous risques chantier + Montant avenant TTC * Taux retenue compte prorata + Montant avenant TTC * Taux retenue assurance décennal + Montant avenant TTC * Taux retenue assurance tous risques chantier + Retenue forfaitaire + Pénalité retenue de garantie + Dépassement TTC * Taux retenue compte prorata +Dépassement TTC * Taux retenue assurance décennal + Dépassement TTC * Taux retenue assurance tous risques chantier + (Si la case (Sous l'onglet "Options") "Appliquer la retenue sur les travaux supplémentaires" est cochée, le système additionne : Travaux supplémentaire TTC * Taux retenue compte prorata + Travaux supplémentaire TTC * Taux retenue assurance décennal + Travaux supplémentaire TTC * Taux retenue assurance tous risques chantier) ( Si la case (Sous l'onglet "Options") "Bloquer la restitution de la retenue de garantie" est cochée le système soustrait - (Retenue de garantie décompté + Avenant TTC * Taux Retenue de garantie - Retenue de garantie restituée ) ) |
| Remboursement |  |  | En lecture seule, |
| Régulation |  |  | En lecture seule,<br> Σ des montants de régulation + Σ des montants des timbres fiscaux - Σ des montants régulation des avoirs - Σ des montants des timbres fiscaux des avoirs<br> Remarque : Le système ne comptabilise pas les montantes régulations et les montants des timbres fiscaux pour les documents à régler de retenue de garanti |
| Net à payer |  |  | En lecture seule,<br> Coût TTC + Remboursement Compte prorata + Coût régulation + Approvisionnement - Coût RNR |
| Réglé |  |  | En lecture seule,<br> Σ des montants de règlements |
| Reste |  |  | En lecture seule,<br> Net à payer - Réglé |
| GENERAL : COÛT REEL HT |  |  |  |
| Bordereau |  |  | Montants des factures HT- Montant des travaux supplémentaire HT facturé – Montant de dépassement HT facturé - ∑ des montants des avoirs HT |
| Approvisionnement |  |  | ∑ des montants des approvisionnements HT à partir des factures - ∑ des montants des approvisionnements HT à partir des avoirs |
| Dépassements |  |  | ∑ des montants des dépassement HT facturé |
| Travaux supp. |  |  | ∑ des montants HT des travaux supplémentaire facturé |
| Fluctuations |  |  | ∑ des montants de fluctuation HT facturé |
| Avenants |  |  | ∑ des montants des avenants HT facturé |
| Coût Total |  |  | Si le lot est résilié ou clôturé<br> Σ des montants HT de factures - Σ des montants HT de avoirs<br> Sinon<br> Montants marché ajusté TTC + Montant travaux supplémentaire TTC facturé + Montant dépassement TTC facturé + Montant fluctuation TTC facturé + Montant approvisionnement TTC facturé + Montant avenant TTC facturé |
| GENERAL : COÛT REEL TTC |  |  |  |
| Bordereau |  |  | Montants des factures TTC- Montant des travaux supplémentaire TTC facturé – Montant de dépassement TTC facturé - ∑ des montants des avoirs TTC |
| Approvisionnement |  |  | ∑ des montants des approvisionnements TTC à partir des factures - ∑ des montants des approvisionnements TTC à partir des avoirs |
| Dépassements |  |  | En lecture seule,<br> Cumul des montant TTC des articles en question dans les décompte - Montant TTC des article en question (dans le bordereau définitif ) |
| Travaux supp. |  |  | ∑ des montants TTC des travaux supplémentaire facturé |
| Fluctuations |  |  | ∑ des montants de fluctuation TTC facturé |
| Avenants |  |  | ∑ des montants des avenants TTC facturé |
| Coût Total |  |  | Si le lot est résilié ou clôturé<br> Σ des montants TTC de factures - Σ des montants TTC de avoirs<br> Sinon<br> Montants marché ajusté TTC + Montant travaux supplémentaire TTC facturé + Montant dépassement TTC facturé + Montant fluctuation TTC facturé + Montant approvisionnement TTC facturé + Montant avenant TTC facturé |
| Retenue |  |  | Σ des montants de compte prorata facturé + Σ des montants de retenues d'assurance décennale facturé + Σ des montants d'assurance tous risque chantier facturé + Σ des montants des retenues forfaitaire facturé + Σ des montants des pénalités de retenues de garantie facturé + (Si le fournisseur est le gestionnaire de compte prorata ) + Σ des montants net à payer des factures qui sont paramétré "imputé sur le compte prorata" - Σ des montants net à payer des avoirs.<br>Remarque: Si le lot est résilié, le système calcule le montant de retenue non remboursable de la manière suivante:<br> Σ des montants de compte prorata facturé + Σ des montants de retenues d'assurance décennale facturé + Σ des montants d'assurance tout risque chantier facturé + Σ des montants des retenues forfaitaire facturé + Σ des montants des pénalités de retenues de garantie facturé + (Si le fournisseur est le gestionnaire de compte prorata ) + Σ des montants net à payer des factures qui sont paramétré "imputé sur le compte prorata" - Σ des montants net à payer des avoirs + Reste à amortir (de l'avance sur marché) + Reste à amortir (de l'avance sur décompte) + Reste à amortir de la retenue de garantie (si l'option "Bloquer la restitution de la retenue de garantie") |
| Remboursement |  |  | Si le lot est résilié ou clôturé<br> Σ des montants de remboursement de compte prorata facturé<br> sinon<br> Montant de de remboursement de compte prorata TTC - Σ des montants net à payer des factures qui sont imputées sur le compte prorata. |
| Régulation |  |  | Σ des montants de régulation + Σ des montants des timbres fiscaux - Σ des montants régulation des avoirs - Σ des montants des timbres fiscaux des avoirs<br>Remarque : Le système ne comptabilise pas les montantes régulations et les montants des timbres fiscaux pour les documents à régler de retenue de garanti |
| Net à payer |  |  | Coût Total TTC + Remboursement Compte prorata + Coût régulation + Approvisionnement - Coût RNR |
| Réglé |  |  | Σ des montants de règlements des factures et des documents à régler du projet en question. |
| Reste |  |  | Coût Net à payer - Coût Réglé |
| SUIVI: DECOMPTES |  |  |  |
| Montant HT |  |  | En lecture seule,<br> Σ des montants HT des décomptes |
| Montant TVA |  |  | En lecture seule,<br> Σ des montants TVA des décomptes |
| Montant TTC |  |  | En lecture seule,<br> Σ des montants TTC des décomptes |
| Appro.(Approvisionnement) TTC |  |  | En lecture seule,<br> Σ des montants TTC des approvisionnements décomptes |
| Dépassement TTC |  |  | En lecture seule,<br> Cumul des montant TTC de l'article en question dans les décompte - Montant TTC d'article en question (dans le bordereau définitif ) |
| Travaux supp.(Supplémentaires) TTC |  |  | En lecture seule,<br> Σ des montants TTC des travaux supplémentaires à partir des décomptes. |
| Fluctuation TTC |  |  | En lecture seule,<br> Σ des montants TTC des fluctuations à partir des décomptes |
| Total TTC |  |  | En lecture seule,<br> Montant TTC décompté + Appro. TTC décompté + Dépassement TTC décompté + Travaux supp. TTC décompté + Fluctuation TTC décompté. |
| Avenants TTC |  |  | En lecture seule,<br> Σ des montants TTC des avenants |
| Coût TTC |  |  | En lecture seule,<br> Montant TTC décompté + Appro. TTC décompté + Dépassement TTC décompté + Travaux supp. TTC décompté + Fluctuation TTC décompté + Avenants TTC |
| Retenues |  |  | En lecture seule, |
| Remboursement |  |  | En lecture seule,<br> Σ des montants des retenues non remboursables décomptés + Σ des montants des retenues non remboursables des avenants + Σ des montants des factures imputées sur le compte prorata + Σ des montants des pénalités de retenue de garantie + Blocage RG + Pénalité avance (si lot est résilié) |
| SUIVI: FACTURES |  |  |  |
| Montant HT |  |  | En lecture seule,<br> Σ des montants HT des décomptes qui ont des factures |
| Montant TVA |  |  | En lecture seule,<br> Σ des montants TVA des décomptes qui ont des factures |
| Montant TTC |  |  | En lecture seule,<br> Σ des montants TTC des décomptes qui ont des factures |
| Appro. TTC |  |  | En lecture seule,<br> Σ des montants TTC des approvisionnements qui ont des factures |
| Dépassement TTC |  |  | En lecture seule,<br> Σ des montants TTC des Dépassement qui sont facturés |
| Travaux supp. TTC |  |  | En lecture seule,<br> Σ des montants TTC des Travaux supplémentaires qui sont facturés |
| Fluctuations TTC |  |  | En lecture seule,<br> Σ des montants TTC des Fluctuations qui sont facturés |
| Total TTC |  |  | En lecture seule,<br> Montant TTC + Dépassement TTC + Travaux supplémentaires TTC + Fluctuations TTC |
| Avenants TTC |  |  | En lecture seule,<br> Σ des montants TTC des Avenants qui sont facturés |
| Coût TTC |  |  | En lecture seule,<br> Total TTC (facturé) + Avenants TTC (facturé) |
| Retenues |  |  | En lecture seule,<br> Montant compte prorata facturé + Montant compte d'assurance décennale facturé + Montant d'assurance tous risques chantier facturé + Montant compte prorata avenant facturé + Montant compte d'assurance décennale avenant facturé + Montant d'assurance tous risques chantier avenant facturé + Montant pénalité retenue de garantie facturé + (Si le lot est clôturé ou résilié ou annulé Montant avance sur marché réglée + Montant avance sur décompte réglée - Montant d'avance sur marché - Montant d'avance sur décompte) + (Si l'option "bloqué la restitution de retenue de garantie est coché" + Montant de retenue de garantie facturée + Montant de retenue de garantie des avenants facturé - Montant retenue de garantie validée ) |
| Remboursement |  |  | En lecture seule,<br> Σ des montants de remboursement de retenue de compte prorata facturé. |
| Régulation |  |  | En lecture seule,<br> Σ des montants régulation des décomptes facturés du projet en question + Σ des montants timbres fiscaux des décomptes facturés du projet en question + Σ des montants régulation des avenants facturés du projet en question + Σ des montants timbres fiscaux des avenants facturés du projet en question. |
| SUIVI: AVOIRS |  |  |  |
| Montant HT |  |  | En lecture seule,<br> Σ des montants des avoirs HT sur facture décompte + Σ des montants des avoirs HT sur factures avenants |
| Montant TVA |  |  | En lecture seule,<br> Σ des montants des avoirs TVA sur facture décompte + Σ des montants des avoirs TVA sur factures avenants. |
| Montant TTC |  |  | En lecture seule,<br> Σ des montants des avoirs TTC sur facture décompte + Σ des montants des avoirs TTC sur factures avenants. |
| Régulation |  |  | En lecture seule,<br> Σ des montants régulation des avoirs + Σ des montants des timbres fiscaux des avoirs. |
| SUIVI: AVANCE SUR MARCHE |  |  |  |
| Taux |  |  |  |
| Forfaitaire |  |  | En lecture seule,<br> Taux * Montant marché forfaitaire TTC |
| Confidentielle |  |  | En lecture seule,<br> Taux * Montant marché Confidentielle TTC |
| Marché |  |  | En lecture seule,<br> Taux * Montant marché TTC |
| Validée |  |  | En lecture seule,<br> Σ des montants des documents d'avance sur marché |
| Réglée |  |  | En lecture seule,<br> Σ des montants des règlements des documents d'avance sur marché. |
| Amortie |  |  | En lecture seule,<br> Σ des montants des amortissements d'avances sur marché des décomptes qui sont facturés. |
| Reste à amortir |  |  | En lecture seule,<br> Réglée - Amortie |
| SUIVI: AVANCE SUR DECOMPTE |  |  |  |
| Validée |  |  | En lecture seule,<br> Σ des montants TTC des documents à régler des avances sur décompte. |
| Réglée |  |  | En lecture seule,<br> Σ des montants réglés des avances sur décompte. |
| Amortie |  |  | En lecture seule,<br> Σ des montants des amortissements des avances sur décompte facturées. |
| Reste à amortir |  |  | En lecture seule,<br> Réglé - Amortie. |
| SUIVI: RETENUE DE GARANTIE |  |  |  |
| Taux |  |  |  |
| Forfaitaire |  |  | En lecture seule,<br> Taux de retenue de garantie * Montant marché TTC forfaitaire |
| Confidentielle |  |  | En lecture seule,<br> Taux de retenue de garantie * Montant marché TTC confidentielle |
| Marché ajusté |  |  | En lecture seule,<br> Taux de retenue de garantie * Montant marché ajusté TTC |
| Avenants |  |  | En lecture seule<br> Taux de retenue de garantie (Défini dans l'avenant) * Montant avenant TTC. |
| Factures |  |  | En lecture seule,<br> Σ des montants de retenues de garantie facturés. |
| Retenue |  |  | En lecture seule,<br> Σ des montants de retenues de garantie facturés + Σ des montants de retenues de garantie des avenants facturés. |
| Validée |  |  | En lecture seule,<br> Σ des montants des documents à régler pour la restitution de la retenue de garantie. |
| Restituée |  |  | En lecture seule,<br> Σ des montants de règlements des documents à régler pour la restitution de la retenue de garantie. |
| Reste à restituer |  |  | En lecture seule,<br> Validée - Pénalité Retenue de garantie - Restituée |
| Gestionnaire du compte prorata |  |  | En lecture seule,<br> La case est cochée si le lot en question est paramétré "Gestionnaire du compte prorata". |
| A rembourser |  |  | En lecture seule,<br> Σ des montants des retenues de compte prorata facturés. |
| Remboursement |  |  | En lecture seule,<br> Σ des montants des remboursements de retenues de compte prorata facturés. |
| RETENUES: COMPTE PRORATA |  |  |  |
| Taux |  |  |  |
| Forfaitaire |  |  | En lecture seule,<br> Taux de retenue de compte prorata * Marché forfaitaire |
| Confidentielle |  |  | En lecture seule,<br> Taux de retenue de compte prorata * Marché confidentielle |
| Marché ajusté |  |  | En lecture seule,<br> Taux de retenue de compte prorata * Marché ajusté |
| RETENUES: ASSURANCE DECENNALE |  |  |  |
| Taux |  |  | Taux de retenue d'assurance décennale |
| Cote-part |  |  | Dans ce champ, on peut saisir manuellement le montant d'assurance décennale |
| Forfaitaire |  |  | En lecture seule,<br> Taux d'assurance décennale * Marché forfaitaire |
| Confidentielle |  |  | En lecture seule,<br> Taux d'assurance décennale * Marché confidentielle |
| Marché ajusté |  |  | En lecture seule,<br> Taux d'assurance décennale * Marché ajusté |
| RETENUES: ASSURANCE TRC |  |  |  |
| Taux |  |  | Taux de retenue d'assurance TRC (Tous risque chantier) |
| Cote-part |  |  | Dans ce champ, on peut saisir manuellement le montant d'assurance TRC (Tous risque chantier) |
| Forfaitaire |  |  | En lecture seule,<br> Taux d'assurance TRC * Marché forfaitaire |
| Confidentielle |  |  | En lecture seule,<br> Taux d'assurance TRC * Marché confidentielle |
| Marché ajusté |  |  | En lecture seule,<br> Taux d'assurance TRC * Marché ajusté |
| RETENUES: DECOMPTE |  |  |  |
| Compte prorata |  |  | En lecture seule,<br> Σ des montants retenues de compte prorata décompté. |
| Avenant |  |  | En lecture seule,<br> Σ des montants retenues de compte prorata des avenants. |
| Total |  |  | En lecture seule,<br> Montant compte prorata (Décompté) + Montant avenant. |
| Assurance décanale |  |  | En lecture seule,<br> Σ des montants assurance décanale décompté. |
| Avenant |  |  | En lecture seule,<br> Σ des montants assurance décanale des avenants. |
| Total |  |  | En lecture seule,<br> Assurance décanale + Avenant. |
| Assurance TRC |  |  | En lecture seule,<br> Σ des montants assurance TRC décompté. |
| Avenant |  |  | En lecture seule,<br> Σ des montants assurance TRC des avenants. |
| Total |  |  | En lecture seule,<br> Assurance TRC + avenant. |
| Retenue forfaitaire |  |  | En lecture seule,<br> Σ des montants retenues forfaitaires décomptés |
| Pénalité compte prorata |  |  | En lecture seule, |
| Pénalité RG |  |  | En lecture seule,<br> Σ des montants pénalité retenue de garantie |
| Blocage RG |  |  | En lecture seule,<br> Si la case "Bloqué la restitution de la retenue de garantie est cochée" Blocage RG =Montant retenue de garantie décompté + Montant avenant RG - Montant retenue de garantie pénalité - Montant retenue de garantie restituée<br> Sinon<br> Blocage RG =0 |
| Pénalité avance (Résiliation) |  |  | En lecture seule,<br> Si lot dans un statut de résiliation<br> Pénalité avance (Résiliation) = Montant avance réglée + Montant avance sur décompte réglée - Montant amortissement avance décomptés - montant amortissement avance sur décompte décomptés<br> Sinon<br> Pénalité avance (Résiliation) = 0 |
| Retenues |  |  | En lecture seule,<br> Montant retenue compte prorata décompté + Montant retenue assurance décennale décompté + Montant retenue tous risques chantier décompté + Montant retenue forfaitaire décompté + Montant retenue compte prorata avenant + Montant retenue assurance décennale avenant + Montant retenue tous risques chantier avenant + (si le lot en question est un gestionnaire de compte prorata) Montant des factures TTC imputé sur le compte prorata + Montant pénalité retenue de garantie + (Si l'option "Bloquée la restitution de la retenue de garantie" est cochée Montant retenue de garantie décompté + Montant retenue de garantie avenant - Montant pénalité retenue de garantie - Montant retenue de garantie restituée ) + (Si le lot dans un statut de résiliation Montant avance sur marché réglée + Montant avance sur décompté réglée - Montant total avance sur marché - Montant total avance sur décompte ) |
| RETENUES: FACTURE |  |  |  |
| Compte prorata |  |  | En lecture seule,<br> Σ des montants retenues de compte prorata des décomptes facturé. |
| Avenant |  |  | En lecture seule,<br> Σ des montants retenues de compte prorata des avenant facturé. |
| Total |  |  | En lecture seule,<br> Compte prorata (facturé) + Avenant (facturé) |
| Assurance décanale |  |  | En lecture seule,<br> Σ des montants retenues assurance décanale des décomptes facturé. |
| Avenant |  |  | En lecture seule,<br> Σ des montants retenues assurance décanale des avenant facturé. |
| Total |  |  | En lecture seule,<br> Assurance décanale (facturé) + Avenant (facturé) |
| Assurance TRC |  |  | En lecture seule,<br> Σ des montants retenues assurance décanale des décomptes facturé. |
| Avenant |  |  | En lecture seule,<br> Σ des montants retenues assurance TRC des avenant facturé. |
| Total |  |  | En lecture seule,<br> Assurance TRC (facturé) + Avenant (facturé) |
| Retenue forfaitaire |  |  | En lecture seule,<br> Σ des montants des retenues forfaitaires des décomptes |
| Pénalité compte prorata |  |  | En lecture seule,<br> Si c'est un lot gestionnaire du compte prorata<br> Σ des montants des factures qui sont imputées sur le compte prorata |
| Pénalité RG |  |  | En lecture seule,<br> Σ des montants des pénalités de retenues de garantie |
| Blocage RG |  |  | En lecture seule,<br> Montant retenue de garantie Décomptes + Montant retenue de garantie avenant - Montant pénalité retenue de garantie - Montant retenue de garantie restituée |
| Pénalité avance (Résiliation) |  |  | En lecture seule,<br> Montant avance sur marché réglée + Montant avance sur décompte réglée - Montant amortissement avance sur marché - Montant amortissement avance sur décompte |
| Retenues |  |  | En lecture seule,<br> Montant retenue compte prorata facturé + Montant retenue assurance décennale facturé + Montant retenue tous risques chantier facturé + Montant retenue forfaitaire facturé + Montant retenue compte prorata avenant facturé + Montant retenue assurance décennale avenant facturé + Montant retenue tous risques chantier avenant facturé + (si le lot en question est un gestionnaire de compte prorata) Montant des factures TTC imputé sur le compte prorata + Montant pénalité retenue de garantie + (Si l'option "Bloquée la restitution de la retenue de garantie" est cochée Montant retenue de garantie décompté + Montant retenue de garantie avenant facturé - Montant pénalité retenue de garantie facturé - Montant retenue de garantie restituée ) + (Si le lot dans un statut de résiliation Montant avance sur marché réglée + Montant avance sur décompté réglée - Montant total avance sur marché - Montant total avance sur décompte ) |
| DATES |  |  |  |
| Date de lancement de l’appel d’offres |  |  |  |
| Date de retrait du dossier de consultation |  |  |  |
| Date échéance de remise des offres |  |  |  |
| Date de report de l’appel d’offres |  |  |  |
| Date ouverture des plis |  |  |  |
| Date du rapport de dépouillement |  |  |  |
| Date adjudication |  |  |  |
| Date signature du marché |  |  |  |
| Date d’ordre de service |  |  |  |
| Date de lancement des travaux |  |  |  |
| Date d’achèvement des travaux |  |  |  |
| Date de recollement |  |  |  |
| OPTIONS |  |  |  |
| Variation dans la masse |  |  |  |
| Taux de remboursement des appros (approvisionnement) |  |  |  |
| Total des points pour la décomposition |  |  |  |
| TVA |  |  | Le système par défaut affecte le TVA renseigné dans les paramètres système et il l'utilise à la création des lignes du bordereau.<br>Remarque: Si l'utilisateur modifie le TVA (sous l'onglet "Options"), le système utilise ce dernier par défaut pour le lot en question. |
| Avec Décomposition |  |  |  |
| Appliquer la remise sur les travaux supplémentaires |  |  |  |
| Appliquer la retenue sur les travaux supplémentaires |  |  | Si la case est coché,<br> Le système applique les taux; de retenue de compte prorata , taux d'assurance décennale, taux d'assurance tous risque chantier et le taux de retenue de garantie par rapport au montant du travaux supplémentaires |
| Bloquer la restitution de la retenue de garantie |  |  | Si la case est coché,<br> 1- On n'aura plus la possibilité d'ajouter une restitution de la retenue de garantie.<br>2- La retenue de garantie sera visible comme étant restituée à 100% dans le lot et les impressions |
| Saisie des décomptes basée sur les quantités exécutées (sinon sur le cumul) |  |  |  |
| Possibilité de saisir l’avancement (sinon le coefficient) par ligne de décompte |  |  |  |
| RESILIATION |  |  |  |
| Date |  |  | Date résiliation |
| Cause |  |  |  |
| AUTRES |  |  |  |
| Description détaillée |  |  |  |
| HISTORIQUE |  |  |  |
| Liste (Historique) |  |  | La liste visualise l’historique du lot en indiquant :<br>- L’utilisateur<br>- La date<br>- L’état |
| Remarque : La modification de l’estimation des montants de marché, de date début, durée de travaux et la date fin forfaitaires varie selon les habilitations accordées à un [rôle](#page-roles). |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Copier | Lors de la copie d'un lot le système vérifie si c'est un marché ou un marché multi-fournisseurs :<br>- Si c'est un marché multi-fournisseurs, le système :<br> - Copie le bordereau estimatif du marché source<br>- Si c'est un marché le système:<br> - Transfère les fournisseurs de l'appel d'offre et de dépouillements sans les dates et les notes.<br> - Si le marché source possède un bordereau de prix définitif, le système:<br> - Affecte le montant HT définitif, le montant TVA définitif et le montant TTC définitif dans montant HT forfaitaire, le montant TVA forfaitaire et le montant TTC forfaitaire du nouveau marché.<br> - Copie le bordereau définitif du marché source dans le bordereau estimatif du nouveau marché.<br> - Si le marché possède un bordereau de prix estimatif, le système<br> - Affecte le montant HT estimatif, le montant TVA estimatif et le montant TTC estimatif dans montant HT forfaitaire, le montant TVA forfaitaire et le montant TTC forfaitaire du nouveau marché.<br> - Copie le bordereau estimatif du marché source, dans le bordereau estimatif du nouveau marché.<br> - Si non le système copie seulement les estimations forfaitaires du marché source dans le nouveau marché |
| Cliquer | [GEN0013](#page-reglesdegestiongenerale) |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Imprimer | GEN0011 |
| Prévision | Il faut que l’option « Prévision » soit activée dans le masque de configuration des [statuts](#page-statutsdeslots). |
| Bordereau estimatif | Il faut que l’option « Bordereau estimatif » soit activée dans le masque de configuration des [statuts](#page-statutsdeslots). |
| Appel d’offre | Il faut que l’option « Appel d’offre » soit activée dans le masque de configuration des [statuts](#page-statutsdeslots). |
| Offres des entreprises | Il faut que l’option « Offre des entreprises » soit activée dans le masque de configuration des [statuts](#page-statutsdeslots). |
| Bordereau définitif | Il faut que l’option « Bordereau définitif » soit activée dans le masque de configuration des [statuts](#page-statutsdeslots). |
| Décomposition | Il faut que l’option « Décomposition » soit activée dans le masque de configuration des [statuts](#page-statutsdeslots). |
| Avances | Il faut que l’option « Avances» soit activée dans le masque de configuration des [statuts](#page-statutsdeslots). |
| Avance sur décomptes | Il faut que l’option « Avance sur décomptes » soit activée dans le masque de configuration des [statuts](#page-statutsdeslots). |
| Décomptes | Il faut que l’option « Décomptes » soit activée dans le masque de configuration des [statuts](#page-statutsdeslots). |
| Retenues de garantie | Il faut que l’option « Retenues de garantie » soit activée dans le masque de configuration des [statuts](#page-statutsdeslots). |
Créé avec HelpNDoc Personal Edition: [Outils facile d'utilisation pour créer des aides HTML et des sites web](https://www.helpndoc.com/fr/outils-creation-aide)

#### PREVISIONS <a id="page-previsions-prj"></a>
_Page : `PREVISIONS_PRJ.html` — Module Projet › LOTS_

| Prévision sans avance sur marché et sans retenue de garantie |
|---|
| [image : NouvelElement799.png]<br>Série en vert<br> Correspond les prévisions du montant forfaitaire et l’avancement forfaitaire<br>Série en bleu<br> Correspond les prévisions confidentielles et l’avancement confidentielle<br>Courbe en vert<br> Correspond au courbe d'avancement on se basant sur le montant forfaitaire<br>Courbe en bleu<br> Correspond au courbe d'avancement on se basant sur le montant confidentiel<br>- Le système a automatiquement réparti les montants totaux en portions égaux sur toute la durée du lot tout |
| On peut modifier les prévisions forfaitaire et / ou confidentielles en cliquant sur le bouton « Modifier les prévisions »<br>[image : NouvelElement800.png]<br>Le résultat obtenu sera comme suit :<br>[image : NouvelElement801.png]<br>Le système change l’allure de la courbe d’avancement en fonction des pourcentages saisi (Pareil pour les séries des montants) |
| Prévision pour le lot de gestionnaire de compte prorata |
| Si le lot en question est paramétré "Gestionnaire de compte prorata", le système affiche deux montants de prévision de remboursement de compte prorata<br> - 1er remboursement: Affiché au milieu du projet, avec un montant qui correspond à la moitié des montants de retenues de compte prorata (de tous les lots de projet en question).<br> - 2éme remboursement: Affiché à la fin du marché, avec un montant qui correspond à la moitié des montants de retenues de compte prorata (de tous les lots de projet en question).<br>[image : NouvelElement823.png] |
| Prévision avec avance sur marché et sans retenue de garantie |
| Le système affiche l’avance dans la date début prévisionnelle comme elle indique la figure ci-dessous<br>[image : NouvelElement802.png]<br>Remarque : Si le champ « Date signature du marché » est renseigné, le système affiche le montant d’avance dans ce date. |
| Prévision avec avance sur marché et avec retenue de garantie |
| Si on n’a pas un échéancier de remboursement de retenue de garantie le système affiche le montant de remboursement dans la date fin du lot + Durée de garantie en (mois) comme elle indique la figure ci-dessous<br>[image : NouvelElement803.png]<br>Si on a un échéancier de remboursement de retenue de garantie comme suit :<br>[image : NouvelElement804.png]<br>Le système affiche les montants de remboursement de retenue de garantie dans les dates échéances en fonctions des pourcentages mentionnés<br>[image : NouvelElement805.png]<br>Remarque: Si on saisie une durée de paiement (Sur(j))[image : NouvelElement822.png], le système additionne le nombre de jours mentionnée à la date échéance calculée dans l'écran "échéance de remboursement" |
| Prévision avec décomptes |
| [image : NouvelElement807.png]<br>Lorsqu’on ajoute un décompte le système :<br>- Affiche une courbe rouge qui correspond à l’avancement réel<br> Dans la liste des mouvements (lorsqu’on fait un double clic sur la série en bleu) le système:<br>- Modifie la description de décompte prévisionnel N°1 en Décompte provisoire N°…<br>- Dans la colonne « Date » il affiche la date décompte<br> Remarque : Lorsqu’on génère une facture dans la liste de mouvements le système :<br>- Modifie la description de Décompte provisoire N°… en Décompte N°…<br>- Dans la colonne « Date » il affiche la date échéance de la facture correspondante<br>[image : NouvelElement808.png]<br>Lorsqu’on effectue des règlements des factures de décompte:<br>- Le système supprime la série en bleu qui correspond le décompte en question (en effet, les séries bleus seront confondus avec les séries rouges )<br>- Le système affiche une série en rouge qui correspond le montant du règlement<br>- Le système réajuste les montants des séries en bleu restantes selon les pourcentages mentionnés dans l'écran d'ajustement des prévisions<br>[image : NouvelElement809.png] |
Créé avec HelpNDoc Personal Edition: [Générateur de documentations PDF gratuit](https://www.helpndoc.com/fr)

#### BORDEREAU ESTIMATIF <a id="page-bordereauestimatif"></a>
_Page : `BORDEREAUESTIMATIF.html` — Module Projet › LOTS_

| Bordereau estimatif |
|---|
| [image : NouvelElement818.png] |
| Cet écran donne la possibilité aux utilisateurs de gérer les bordereaux estimatifs en permettant d'effectuer les actions expliquées dans le tableau ci dessous<br>Liste (Groupes)<br>\| LIBELLE \| V \| OBSERVATIONS \| \|---\|---\|---\| \| Catégorie \| \| \| \| O.D.T \| X \| \| \| Code \| X \| \| \| Crée le \| \| \| \| Crée par \| \| \| \| Description \| X \| \| \| Unité \| X \| \| \| Quantité \| X \| \| \| P.U HT \| X \| (PU HT après remise * Pourcentage FODEC) + (PU HT après remise) \| \| FODEC \| X \| Pourcentage FODEC \| \| TVA \| X \| Pourcentage TVA \| \| P.U TTC \| X \| PU HT (avec FODEC) * Pourcentage TVA + (PU HT (avec FODEC)) \| \| TOTAL HT \| X \| P.U HT * Quantité \| \| TOTAL TTC \| X \| P.U TTC * Quantité \| \| Modifié le \| \| \| \| Modifié par \| \| \| \| Notes \| X \| \| <br>Liste (Approvisionnement)<br>\| LIBELLE \| V \| OBSERVATIONS \| \|---\|---\|---\| \| O.D.T \| X \| \| \| Code \| X \| \| \| Description \| X \| \| \| Unité \| X \| \| \| P.U HT \| X \| \| \| FODEC \| X \| \| \| TVA \| X \| \| \| P.U TTC \| X \| \| <br>Actions<br>\| LIBELLE \| OBSERVATIONS \| \|---\|---\| \| Ajouter groupe \| Ajouter un nœud racine dans la hiérarchie du bordereau \| \| Ajouter ligne \| Ajouter une ligne dans le bordereau \| \| Supprimer \| Supprimer une ligne du bordereau \| \| Régénérer les codes \| Le système réorganise la codification des lignes du bordereau \| \| Importer/Exporter \| Importer: GEN0011<br> Exporter:GEN0012 \| \| Développer \| Le système afficher toute lahiérarchie du bordereau \| \| Réduire \| Le système réduit toute la hiérarchie du bordereau \| |
| [image : NouvelElement819.jpg]<br>Après avoir créé un bordereau estimatif, l'estimation confidentielle sera modifiée comme elle le montre la figure au-dessus<br> Remarque : L’estimation confidentielle = ∑ des prix des articles du bordereau estimatif. |
Créé avec HelpNDoc Personal Edition: [Générateur de documentations PDF gratuit](https://www.helpndoc.com/fr)

#### APPEL D'OFFRE <a id="page-appeldoffre"></a>
_Page : `APPELDOFFRE.html` — Module Projet › LOTS_

| Appel d’offre |
|---|
| [image : NouvelElement811.jpg] |
| Le bouton « Appel d’offre » permet de gérer les fournisseurs présélectionnés pour le lot, et les détails qui concernent leur dépôt d’offre :<br>- Le fournisseur<br>- La date d’envoi de la lettre de consultation<br>- La date du retrait dossier de consultation<br>- La date dépôt de l’offre<br>- La note<br>On peut aussi effectuer les opérations suivantes:<br>- Ajouter un fournisseur<br>- Supprimer un fournisseur<br>Remarque: Si le statut du marché est paramétré sans appel d'offre, le système affiche la liste des fournisseurs en lecture seule. |
Créé avec HelpNDoc Personal Edition: [Générateur d'aides Web gratuit](https://www.helpndoc.com/fr)

#### OFFRES DES ENTREPRISES <a id="page-offresdesentreprises"></a>
_Page : `OFFRESDESENTREPRISES.html` — Module Projet › LOTS_

| Offre des entreprises |
|---|
| [image : NouvelElement816.jpg]<br>Fiche<br>\| LIBELLE \| U \| O \| OBSERVATIONS \| \|---\|---\|---\|---\| \| O.D.T \| \| \| En lecture seule \| \| Code \| \| X \| \| \| Date de lancement de l'appel d'offre \| \| \| \| \| Date de retrait du dossier de consultation \| \| \| \| \| Date échéance de remise des offres \| \| \| \| \| Date de report de l'appel d'offres \| \| \| \| \| Date ouverture des plis \| \| \| \| \| Date du report de dépouillement \| \| \| \| \| Présélection \| \| \| \| \| Liste des présélections \| \| \| [image : NouvelElement416.png]<br>La liste visualise les données qui concernent les présélections en indiquant:<br>- Le fournisseur<br>- La date de la lettre de consultation<br>- La date du retrait de consultation<br>- La date dépôt de l'offre...<br>Aussi, dans cette liste on peut effectuer les opérations suivantes<br>- Ajouter un fournisseur<br>- Ajouter un [devis](#page-devis) pour un fournisseur<br>- Supprimer un fournisseur<br>- Écarter un fournisseur \| \| Offres \| \| \| \| \| Liste des offres \| \| \| [image : NouvelElement417.png]<br>La liste visualise les offres en indiquant:<br>- La date<br>- Le code<br>- Le fournisseur<br>- L’échéance<br>- Le montant HT<br>- Le montant TVA<br>- Le montant TTC...<br>Aussi, dans cette liste on peut effectuer les opérations suivantes:<br>- Afficher la liste des fournisseurs qui ont déposé leur offre<br>- Afficher la liste des fournisseurs qui n’ont pas été écarté<br>- Supprimer devis<br>- Éditer devis<br>- Sélectionner le fournisseur<br>- Dé-sélectionner le fournisseur \| \| Modalité de paiement \| \| \| \| \| Liste des modalités de paiement \| \| \| La liste visualise les modalités de paiement en indiquant<br>- L'O.D.T<br>- Le code<br>- Le pourcentage<br>Aussi, dans cette liste on peut effectuer les opérations suivantes:<br>- Ajouter une modalité de paiement<br>- Supprimer une modalité de paiement \| \| Conclusion \| \| \| \| <br>Actions<br>\| LIBELLE \| OBSERVATIONS \| \|---\|---\| \| Ajouter \| GEN0008 \| \| Éditer \| GEN0009 \| \| Supprimer \| GEN0010 \| \| Exporter \| GEN0012 \| \| Rafraîchir \| GEN0003 \| \| Enregistrer \| GEN0001 \| \| Enregistrer et fermer \| GEN0002 \| \| Fichiers attachés \| GEN0004 \| \| Actions \| GEN0005 \| \| Annuler et fermer \| GEN0006 \| \| Champs spécifique \| GEN0007 \| \| Audit \| GEN0020 \| \| Imprimer \| GEN0011 \| \| Bordereau des prix \| Le système affiche le [bordereau estimatif](#page-bordereauestimatif) en lecture seule \| |
| Remarque:<br> 1 – S'il n’y a pas un ancien dépouillement, le système :<br>- Copie le bordereau estimatif dans le bordereau de dépouillement<br>- Copie la liste des fournisseurs présélectionnés dans l’appel d’offre et qui ont une date échéance de remise des offres.<br> 2 – S'il y a un ancien dépouillement, le système :<br>- Copie le bordereau du dépouillement précédent dans le bordereau de dépouillement actuel<br>- Copie la liste des fournisseurs présélectionnés dans le dépouillement précédent et qui n’ont pas été écartés. |
Créé avec HelpNDoc Personal Edition: [Créer des sites web d'aide facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

#### BORDEREAU DEFINTIF <a id="page-bordereaudefintif"></a>
_Page : `BORDEREAUDEFINTIF.html` — Module Projet › LOTS_

| Bordereau définitif |
|---|
| [image : NouvelElement556.png] |
| Remarque1 : Le devis du fournisseur retenu lors du dépouillement représente le « bordereau définitif »<br> Remarque2 : Les champs " Montant HT ", " Montant TVA ", " Montant TTC ", " Retenu non remboursable " et " Net à payer " du marché sont remplis à partir du bordereau définitif.<br> Remarque3 : Si on ne sélectionne pas un fournisseur, le système garde toujours la fiche "Bordereau définitif" vide. |
| [image : NouvelElement821.png]<br>- Cadre vert : Montants d’estimation forfaitaire.<br>- Cadre bleu : Montants d’estimation confidentielle<br>- Cadre rouge : Montants du marché |
Créé avec HelpNDoc Personal Edition: [Création d'aide CHM, PDF, DOC et HTML d'une même source](https://www.helpndoc.com/fr/outils-creation-aide)

#### DECOMPOSITION <a id="page-decomposition"></a>
_Page : `DECOMPOSITION.html` — Module Projet › LOTS_

| Décomposition |
|---|
| Élaboration<br>[image : NouvelElement747.png]<br>1- Par défaut, le système affiche les lignes du bordereau définitif sous l'onglet "Élaboration"<br> 2- Dans la partie encadrée en bleu, on ajoute les entités de décomposition<br>[image : NouvelElement741.png]<br>Champs<br>\| LIBELLE \| U \| O \| OBSERVATIONS \| \|---\|---\|---\|---\| \| Sorting \| \| \| En lecture seule, auto généré \| \| Code \| \| \| \| \| Description \| \| \| \| \| Unité \| \| \| Liste déroulante des unités (en fonction de nivellement paramétré dans le [projet](#page-projets)) \| \| Quantité \| \| \| \| \| Pourcentage \| \| \| Si la case "Est une tâche" est cochée, le système rend le champ pourcentage modifiable \| \| Est une tâche \| \| \| \| <br>Actions<br>\| LIBELLE \| OBSERVATIONS \| \|---\|---\| \| Éditer \| GEN0009 \| \| Imprimer \| GEN0011 \| \| Exporter \| GEN0012 \| \| Cliquer \| GEN0013 \| \| Double cliquer \| GEN0014 \| \| Annuler et fermer \| GEN0017 \| \| Actions \| GEN0005 \| \| Fichiers attachés \| GEN0004 \| \| Champs spécifiques \| GEN0007 \| <br>3- On affecte des lignes du bordereau à une entité de décomposition en cliquant sur le bouton encadré en vert<br> Remarque: Si on sélectionne un nœud et on clique sur le bouton encadré en rouge, le système annule l'affectation des ligne du bordereau à cet nœud. |
| Calcul des points<br>[image : NouvelElement745.png]<br>\| LIBELLE \| V \| OBSERVATIONS \| \|---\|---\|---\| \| Description \| X \| Description de l'entité de décomposition \| \| Unité \| X \| \| \| Pourcentage \| X \| Remarque1: Le système affiche les pourcentages si le nœud a des enfants<br> Remarque2: La somme des pourcentages doit être égale à 100% \| \| Quantité de base \| X \| A partir de l'entité de décomposition \| \| Total en point \| X \| A partir du paramétrage de lot sous l'onglet "Options" \| \| Montant total \| X \| Montant total = Montant du marché * Pourcentage par unité de nivellement \| |
| Unité de décomposition<br>[image : NouvelElement743.png]<br>Le système affiche une liste des case à cocher pour les niveaux de décomposition paramétré dans l'écran [projet](#page-projets) |
| Ventilation des quantités<br>[image : NouvelElement744.png]<br>\| LIBELLE \| V \| OBSERVATIONS \| \|---\|---\|---\| \| Description \| X \| Description de l'entité de décomposition \| \| Quantité de base \| X \| A partir des entités de décomposition \| \| Niveau 1, 2, 3 ... \| X \| L'utilisateur saisie les quantités par unité de décomposition<br> Remarque: La somme des quantités par niveau de décomposition doit être égal au quantité de base qui lui correspond. \| |
| Décompte<br>[image : NouvelElement748.png]<br>Dans la fiche décompte, sous l'onglet "Décomposition" l'utilisateur saisie l’avancement des travaux en renseignant le pourcentage d'avancement (ou les quantités) par rapport les quantités de base pour chaque entité de décomposition.<br>Remarque1: Le système calcul automatiquement le montant des lignes de décompte en fonction des pourcentages d'avancement (ou les quantités) saisi sous l'onglet "Décomposition".<br>Remarque2: Lorsqu'on suit l'avancement d'un lot on utilisant la décomposition, le système rend les lignes de décompte en lecture seule. |
Créé avec HelpNDoc Personal Edition: [Générateur de documentation d'aide HTML gratuit](https://www.helpndoc.com/fr)

#### AVANCES <a id="page-avances"></a>
_Page : `AVANCES.html` — Module Projet › LOTS_

| Avances |
|---|
| [image : NouvelElement810.png] |
| Le bouton « Avances» permet d’afficher le masque de gestion des avances sur marché et des avances sur décomptes.<br> Avances sur marché:<br>- Afficher la liste des avances<br>- Ajouter une avance sur marché<br>- Éditer une avance<br>- Supprimer une avance.<br>Avances sur décompte :<br>- Afficher la liste des avances<br>- Ajouter une avance sur décompte<br>- Éditer une avance<br>- Supprimer une avance<br>Exemple : Si on a deux avances sur décomptes de montant total 500000 DT répartis comme suit :<br>- Montant de la 1ère avance sur décompte : 200000 DT<br>- Montant de la 2éme avance sur décompte : 300000 DT<br>Lors qu’on atteint la date du 1er décompte le système automatiquement retient le montant total de ces deux avances (500000 DT)<br> N.B : On peut modifier le montant de retenue sur décompte dans le masque « Décompte» |
| Remarque : Le montant d’avance sur marché est calculé en fonction de montant du marché<br> Montant de marché * Pourcentage de l’avance |
Créé avec HelpNDoc Personal Edition: [Sites web iPhone faciles](https://www.helpndoc.com/fr/tour-des-fonctionnalites/generation-de-site-web-iphone)

#### DECOMPTE <a id="page-decompte"></a>
_Page : `DECOMPTE.html` — Module Projet › LOTS_

| Décompte |
|---|
| [image : NouvelElement425.jpg] |
| Le bouton « Décompte » permet de gérer les décomptes<br>- Afficher les décomptes<br>- Ajouter un décompte<br>- Éditer un décompte<br>- Supprimer un décompte (la suppression n’est possible que pour le décompte le plus récent)<br>- Générer une facture<br>- Générer un avoir (La génération est possible que pour les décomptes facturés) |
| [image : NouvelElement426.png] |
| La figure représente la fiche « décompte » |
| Remarque :<br>- Sous l’onglet « Avances et retenues » l’utilisateur paramètre le taux de retenu (en pourcentage).<br>- Lors d’un décompte le système calcule le montant d’avance à retenir en fonction du cumul actuel.<br> Formule de calcul : ((Cumul actuel *Taux)/ 100)-(Somme des retenues précédentes) |
| Remarque :<br>- L’utilisateur peut modifier le taux d’ « avance sur marché » par rapport le total de cumul du décompte en cours, sinon le système utilise le taux prédéfinit. |
| Remarque :<br>- Dans le masque de « décompte », le système calcule le total des avances sur décompte dans le champ « Avances sur décompte » |
| Remarque :<br>- L’utilisateur peut modifier le montant d’avance sur décompte à retenir en saisissant le montant dans le champ « A déduire ». |
| [image : NouvelElement817.jpg] |
| Après avoir créé un décompte, on peut générer une facture. |
Créé avec HelpNDoc Personal Edition: [Sites web iPhone faciles](https://www.helpndoc.com/fr/tour-des-fonctionnalites/generation-de-site-web-iphone)

#### AVENANTS <a id="page-avenants"></a>
_Page : `AVENANTS.html` — Module Projet › LOTS_

| AVENANTS |
|---|
| [image : NouvelElement623.png] |
| Le bouton « Avenants » permet de gérer les avenants<br>- Afficher les avenants<br>- Ajouter un avenant<br>- Éditer un avenant<br>- Supprimer un avenants (la suppression n’est possible que pour l’avenants le plus récent et qui n’a pas une facture)<br>- Générer une facture (Lorsque on génère une facture sur un avenant, le système rend tous les champs de la fiche en lecture seule) |
| [image : NouvelElement624.png]<br>\| LIBELLE \| O \| U \| Observation \| \|---\|---\|---\|---\| \| Totaux \| \| \| \| \| Code \| \| \| Auto généré, en lecture seule \| \| N° \| \| \| Auto généré, en lecture seule \| \| Lot \| \| \| Description du lot \| \| Date \| \| \| Par défaut date de jour \| \| Réf. Interne \| \| \| \| \| Tiers \| \| \| La description du fournisseur de lot \| \| Total HT \| \| \| En lecture seule = ∑ des total HT des lignes \| \| Total TVA \| \| \| En lecture seule = ∑ des total TVA des lignes \| \| Total TTC \| \| \| En lecture seule = ∑ des total TTC des lignes \| \| Retenue de garantie \| \| \| Par défaut le système prend le pourcentage défini dans la fiche lot,<br> = Montant TTC * Pourcentage retenue de garantie<br> Remarque :<br>- Si la case est cochée, le système rend le montant «Retenue de garantie» saisissable manuellement.<br>- Le pourcentage appliqué sera calculé en fonction du montant saisi. \| \| Retenue d’assurance décennale \| \| \| Par défaut le système prend le pourcentage défini dans la fiche lot,<br> = Montant TTC * Pourcentage Retenue d’assurance décennale<br> Remarque :<br>- Si la case est cochée, le système rend le montant «Retenue d’assurance décennale » saisissable manuellement<br>- Le pourcentage appliqué sera calculé en fonction du montant saisi. \| \| Retenue d’assurance tout risque chantier \| \| \| Par défaut le système prend le pourcentage défini dans la fiche lot,<br> = Montant TTC * Retenue d’assurance tout risque chantier<br> Remarque :<br>- Si la case est cochée, le système rend le montant «Retenue d’assurance tout risque chantier» saisissable manuellement.<br>- Le pourcentage appliqué sera calculé en fonction du montant saisi \| \| Retenue de compte prorata \| \| \| Par défaut le système prend le pourcentage défini dans la fiche lot,<br> = Montant TTC * Retenue de compte prorata<br> Remarque :<br>- Si la case est cochée, le système rend le montant «Retenue de compte prorata» saisissable manuellement.<br>- Le pourcentage appliqué sera calculé en fonction du montant saisi \| \| Net à payer \| \| \| En lecture seule<br> = Montant TTC – (Retenue de garantie + Retenue d’assurance décennale + Retenue d’assurance tout risque chantier + Retenue de compte prorata) \| \| Lignes \| \| \| \| \| Liste des lignes d’avenant \| \| \| La liste visualise les lignes d’un avenant :<br>- O.D.T<br>- Code<br>- Description<br>- Quantité<br>- Prix unitaire hors taxe<br>- Remise<br>- FODEC…<br>- TVA<br>- Prix unitaire TTC : (PU HT (avec FODEC) * Pourcentage TVA) + PU HT (avec FODEC)<br> Remarque: PU HT (avec FODEC) = (PU HT après remise * Pourcentage FODEC) + (PU HT après remise)<br>Encore, à partir de cette liste, on peut effectuer les opérations suivantes :<br>- Ajouter<br>- Ajouter à partir d’un BDP<br>- Supprimer<br>- Exporter \| |
| [image : NouvelElement815.jpg] |
| Après avoir créé un avenant, on peut générer une facture |
Créé avec HelpNDoc Personal Edition: [Créer de la documentation iPhone facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites/generation-de-site-web-iphone)

#### RETENUES DE GARANTIE <a id="page-retenuesdegarantie"></a>
_Page : `RETENUESDEGARANTIE.html` — Module Projet › LOTS_

| Retenues de garanties |
|---|
| [image : NouvelElement812.png]<br>Le bouton « retenues de garanties » permet aux utilisateurs de personnaliser le remboursement des garanties on donnant la main sur la gestion des :<br>1. Échéances de remboursement<br>[image : NouvelElement814.png]<br>- Lister les échéances de remboursements<br>- Ajouter une échéance de remboursement<br>- Éditer une échéance de remboursement<br>- Supprimer une échéance de remboursement<br>Remarque: Lorsqu'on saisie un délais (en jours), le système additionne le nombre de jours à la date fin du lot<br>1. Réception<br>[image : NouvelElement813.png]<br>\| LIBELLE \| U \| O \| OBSERVATIONS \| \|---\|---\|---\|---\| \| Code \| X \| \| En lecture seule \| \| Date réception \| \| X \| \| \| Nature \| \| X \| En lecture seule \| \| Type \| \| X \| \| \| Lieux \| \| \| \| \| Demandé par le fournisseur \| \| \| \| \| Participants \| \| \| \| \| Liste des participants \| \| \| La liste visualise<br>- L’ordre de tri<br>- La de<br> Aussi dans cette liste on peut effectuer les opérations suivantes :<br>- Ajouter employer<br>- Ajouter contact fournisseur<br>- Supprimer \| \| Liste des réserves \| \| \| La liste visualise<br>- L’ordre de tri<br>- La description<br>- Fixé<br> Aussi dans cette liste on peut effectuer les opérations suivantes<br>- Ajouter une réserve<br>- Supprimer une réserve \| <br>1. Remboursement<br>- Lister les remboursements<br>- Ajouter un remboursement de garantie<br>- Éditer un remboursement de garantie<br>- Supprimer un remboursement de garantie |
| Remarque1 : Le montant de garanti est calculé en fonction du montant de marché<br> Montant de marché * Pourcentage de garanti<br>Remarque2: La "Pénalité sur retenue de garantie" ne sera pas prise en considération lors des calculs des cumuls de retenue de garantie |
Créé avec HelpNDoc Personal Edition: [Générateur d'aides CHM gratuit](https://www.helpndoc.com/fr)

#### AVOIRS <a id="page-avoirs-prj"></a>
_Page : `AVOIRS_PRJ.html` — Module Projet › LOTS_

| AVOIRS |
|---|
| [image : NouvelElement626.png]<br>La liste visualise la liste des avoirs générés à partir des décomptes qui sont facturés en indiquant :<br>- La date<br>- Le code d’avenant<br>- Le fournisseur<br>- Le statut<br>- Le montant HT<br>- Le montant TVA<br>- Le montant TTC<br>- Le net à payer<br>- Le montant réglé<br>- Le reste à régler<br>- Le code de la facture source<br>Dans cette liste on peut aussi effectuer les opérations suivantes :<br>- Éditer un remboursement<br>- Supprimer un remboursement<br>- Exporter<br>Remarque: Le système affiche la liste des avoirs qui sont générés à partir de la liste des décomptes |
Créé avec HelpNDoc Personal Edition: [Générateur gratuit de livres électroniques et documentation](https://www.helpndoc.com/fr)

### LOT MULTI FOURNISSEUR <a id="page-lotsmulti"></a>
_Page : `LOTSMULTI.html` — Module Projet_

[image : NouvelElement846.bmp]

### Liste

| LIBELLE | V | OBSERVATIONS |  |  |
|---|---|---|---|---|
| Assurance DEC Cote-part |  |  |  |  |
| Assurance DEC Montant |  |  |  |  |
| Assurance DEC Reste à retenir |  |  |  |  |
| Assurance DEC Retenue |  |  |  |  |
| Assurance DEC Taux |  |  |  |  |
| Assurance TRC Cote-part |  |  |  |  |
| Assurance TRC Montant |  |  |  |  |
| Assurance TRC Reste à retenir |  |  |  |  |
| Assurance TRC Retenue |  |  |  |  |
| Assurance TRC Taux |  |  |  |  |
| Autre description détaillée |  |  |  |  |
| Autre Inclure dans les prévisions de paiement |  |  |  |  |
| Autre Notes |  |  |  |  |
| Avance |  | Σ des montants des avances sur bon de commande |  |  |
| Avance Amortie |  | Σ des montants des avances sur bon de commande amortie dans les factures |  |  |
| Avance Amortie Reste |  | Avance Réglée - Avance Amortie |  |  |
| Avance Réglée |  | Σ des avances réglées |  |  |
| Avance Reste |  | Avance - Avance réglée |  |  |
| Avance Taux |  |  |  |  |
| Avancement |  | Σ (Facture HT avec bon de commande - Avoir HT avec bon de commande - Dépassement HT) / Marché HT |  |  |
| Avancement prévisionnel |  |  |  |  |
| Avoirs HT |  | Σ des montants des avoirs HT |  |  |
| Avoirs Régulation |  | Σ des montants de régulation des avoirs |  |  |
| Avoirs TTC |  | Σ des montants des avoirs TTC |  |  |
| Avoirs TVA |  | Σ des montants des avoirs TVA |  |  |
| Bons de commande HT |  | Σ des montants des bons de commande HT |  |  |
| Bons de commande TTC |  | Σ des montants des bons de commande TTC |  |  |
| Bons de commande TVA |  | Σ des montants des bons de commande TVA |  |  |
| Bons de livraison HT |  | Σ des montants des bons de livraisons HT |  |  |
| Bons de livraison TTC |  | Σ des montants des bons de livraisons TTC |  |  |
| Bon de livraison TVA |  | Σ des montants des bons de livraisons TVA |  |  |
| Catégorie |  | La description de la catégorie du lot |  |  |
| Clôturé |  | Pour indiquer si lot est clôturé ou non |  |  |
| Code |  | Code lot |  |  |
| Coût Prév. Appro. HT |  |  |  |  |
| Coût Prév. Appro.TTC |  |  |  |  |
| Coût Prév. Appro.TVA |  |  |  |  |
| Coût Prév. ASS. DEC |  |  |  |  |
| Coût Prév. ASS. TRC |  |  |  |  |
| Coût Prév. Avenant HT |  |  |  |  |
| Coût Prév. Avenant TTC |  |  |  |  |
| Coût Prév. Avenant TVA |  |  |  |  |
| Coût Prév. Bordereau HT |  | ∑ des montants HT des lignes du bordereau estimatif<br> Sinon<br> ∑ des montants HT des bons de commande |  |  |
| Coût Prév. Bordereau TTC |  | ∑ des montants TTC des lignes du bordereau estimatif<br> Sinon<br> ∑ des montants TTC des bons de commande |  |  |
| Coût Prév. Bordereau TVA |  | ∑ des montants TVA des lignes du bordereau estimatif<br> Sinon<br> ∑ des montants TVA des bons de commande |  |  |
| Coût Prév. CP |  |  |  |  |
| Coût Prév Dépass. HT |  | Montant dépassement HT |  |  |
| Coût Prév Dépass. TTC |  | Montant dépassement TTC |  |  |
| Coût Prév Dépass.TVA |  | Montant dépassement TVA |  |  |
| Coût Prév. Fluct. HT |  |  |  |  |
| Coût Prév. Fluct. TTC |  |  |  |  |
| Coût Prév. Fluct. TVA |  |  |  |  |
| Coût Prév. HT |  | Si Marché ajusté HT + Achat sans bon de commande HT + Dépassement HT > Montant HT bordereau estimatif<br>- Coût Prév. HT= Si Marché ajusté HT + Achat sans bon de commande HT + Dépassement HT<br> Sinon<br>- Coût Prév. HT = Montant HT bordereau estimatif |  |  |
| Coût Prév. Net à payer |  | Coût Prév. HT + Montant régulation |  |  |
| Coût Prév. Réglé |  | Σ des montants de règlements du projet en question |  |  |
| Coût Prév. Régulation |  | Si le lot est terminé<br> Σ des montants de régulation + Σ des montants des timbres fiscaux - Σ des montants régulation des avoirs - Σ des montants des timbres fiscaux des avoirs<br> Sinon<br> Σ des montants de régulation + Σ des montants des timbres |  |  |
| Coût Prév. Remboursement |  |  |  |  |
| Coût Prév. Reste |  | Coût Prév. Net à payer – Montant règlement |  |  |
| Coût Prév. Retenue forfaitaire |  |  |  |  |
| Coût Prév. RNR |  |  |  |  |
| Coût Prév. Total HT |  | Si lot est clôturé ou résilié<br> ∑ des montants des factures HT<br> Sinon<br> Marché ajusté HT + Dépassement HT + Achat sans bon de commande HT – Montant Avoir HT |  |  |
| Coût Prév. Total TTC |  | Si lot est clôturé ou résilié<br> ∑ des montants des factures TTC<br> Sinon<br> Marché ajusté TTC + Dépassement TTC + Achat sans bon de commande TTC – Montant Avoir TTC |  |  |
| Coût Prév. Total TVA |  | Si lot est clôturé ou résilié<br> ∑ des montants des factures TVA<br> Sinon<br> Marché ajusté TVA + Dépassement TVA + Achat sans bon de commande TVA – Montant Avoir TVA |  |  |
| Coût Prév. Trav. Supp. HT |  | ∑ des montants HT des factures sans bon de commande - ∑ des montants HT des avoirs sans bon de commande |  |  |
| Coût Prév. Trav. Supp. TTC |  | ∑ des montants TTC des factures sans bon de commande - ∑ des montants TTC des avoirs sans bon de commande |  |  |
| Coût Prév. Trav. Supp. TVA |  | ∑ des montants TVA des factures sans bon de commande - ∑ des montants TVA des avoirs sans bon de commande |  |  |
| Coût Prév. Total HT |  | Si lot est clôturé ou résilié<br> ∑ des montants des factures HT - ∑ des montants des avoirs HT<br> Sinon<br> Si Σ des montants HT des bons de commande + Montant dépassement HT + Σ des montants HT des Achats sans bon de commande > = Estimation confidentielle HT<br> Bordereau HT = Σ des montants HT des bons de commande + Montant dépassement HT + Σ des montants HT des Achats sans bon de commande<br> sinon<br> Bordereau HT = Estimation confidentielle HT |  |  |
| Coût Prév. Total TTC |  | Si lot est clôturé ou résilié<br> ∑ des montants des factures TTC - ∑ des montants des avoirs TTC<br> Sinon<br> Si Σ des montants TTC des bons de commande + Montant dépassement TTC + Σ des montants TTC des Achats sans bon de commande > = Estimation confidentielle TTC<br> Bordereau TTC = Σ des montants TTC des bons de commande + Montant dépassement TTC + Σ des montants TTC des Achats sans bon de commande<br> sinon<br> Bordereau TTC = Estimation confidentielle TTC |  |  |
| Coût Prév. Total TVA |  | Si lot est clôturé ou résilié<br> ∑ des montants des factures TVA - ∑ des montants des avoirs TVA<br> Sinon<br> Si Σ des montants TVA des bons de commande + Montant dépassement TVA + Σ des montants TVA des Achats sans bon de commande > = Estimation confidentielle TVA<br> Bordereau TVA = Σ des montants TVA des bons de commande + Montant dépassement TVA + Σ des montants TVA des Achats sans bon de commande<br> sinon<br> Bordereau TVA = Estimation confidentielle TVA |  |  |
| Coût réel Appro. HT |  |  |  |  |
| Coût réel Appro. TTC |  |  |  |  |
| Coût réel Appro. TVA |  |  |  |  |
| Coût réel Avenant HT |  |  |  |  |
| Coût réel Avenant TTC |  |  |  |  |
| Coût réel Avenant TVA |  |  |  |  |
| Coût réel bordereau HT |  | Montants des factures HT- Montant HT des factures sans bon de commande – Montant HT de dépassement - ∑ des montants des avoirs HT |  |  |
| Coût réel bordereau TTC |  | Montants des factures TTC - Montant TTC des factures sans bon de commande – Montant TTC de dépassement - ∑ des montants des avoirs TTC |  |  |
| Coût réel bordereau TVA |  | Montants des factures TVA - Montant TVA des factures sans bon de commande – Montant TVA de dépassement - ∑ des montants des avoirs TVA |  |  |
| Coût réel dépassement HT |  | ∑ des montants des dépassement HT facturé |  |  |
| Coût réel dépassement TTC |  | ∑ des montants des dépassement TTC facturé |  |  |
| Coût réel dépassement TVA |  | ∑ des montants des dépassement TVA facturé |  |  |
| Coût réel Fluctuations HT |  |  |  |  |
| Coût réel Fluctuations TTC |  |  |  |  |
| Coût réel Fluctuations TVA |  |  |  |  |
| Coût réel Net à payer |  | Montant facture TTC + Montant remboursement facturé + Montant régulation |  |  |
| Coût réel Réglé |  | Σ des règlements du projet en question |  |  |
| Coût réel Régulation |  |  |  |  |
| Coût réel Remboursement |  |  |  |  |
| Coût réel Reste |  | Coût Net à payer - Coût Reste |  |  |
| Coût réel RNR |  |  |  |  |
| Coût réel Trav. Supp. HT |  | ∑ des montants HT des factures sans bon de commande |  |  |
| Coût réel Trav. Supp. TTC |  | ∑ des montants TTC des factures sans bon de commande |  |  |
| Coût réel Trav. Supp. TVA |  | ∑ des montants TVA des factures sans bon de commande |  |  |
| CP Gestionnaire |  |  |  |  |
| CP montant |  |  |  |  |
| CP pénalité |  |  |  |  |
| CP Remboursé |  |  |  |  |
| CP Retenue |  |  |  |  |
| CP Retenue dans le projet |  |  |  |  |
| CP Taux |  |  |  |  |
| Date adjudication |  |  |  |  |
| Date d’achèvement des travaux |  |  |  |  |
| Date de lancement de l’appel d’offre |  |  |  |  |
| Date de lancement des travaux |  |  |  |  |
| Date de Recollement |  |  |  |  |
| Date de report de l’appel d’offre |  |  |  |  |
| Date de retrait du dossier de consultation |  |  |  |  |
| Date début |  |  |  |  |
| Date début forfaitaire |  |  |  |  |
| Date d’ordre de service |  |  |  |  |
| Date du report du dépouillement |  |  |  |  |
| Date échéance de remise des offres |  |  |  |  |
| Date fin |  |  |  |  |
| Date fin forfaitaire |  |  |  |  |
| Date ouverture des plis |  |  |  |  |
| Date Réception définitive |  |  |  |  |
| Date signature du marché |  |  |  |  |
| Décompte Approvisionnement HT |  |  |  |  |
| Décompte Approvisionnement TTC |  |  |  |  |
| Décompte Approvisionnement TVA |  |  |  |  |
| Décompte Avenant HT |  |  |  |  |
| Décompte Avenant TTC |  |  |  |  |
| Décompte Avenant TVA |  |  |  |  |
| Décomptes Coût HT |  |  |  |  |
| Décomptes Coût TTC |  |  |  |  |
| Décomptes Coût TVA |  |  |  |  |
| Décomptes Dépassement HT |  |  |  |  |
| Décomptes Dépassement TTC |  |  |  |  |
| Décomptes Dépassement TVA |  |  |  |  |
| Décomptes Fluctuation HT |  |  |  |  |
| Décomptes Fluctuation TTC |  |  |  |  |
| Décomptes Fluctuation TVA |  |  |  |  |
| Décomptes Net à payer |  |  |  |  |
| Décomptes Réglé |  |  |  |  |
| Décomptes Remboursement |  |  |  |  |
| Décomptes Reste |  |  |  |  |
| Décompte RNR |  |  |  |  |
| Décompte Total HT |  |  |  |  |
| Décompte Total TTC |  |  |  |  |
| Décompte Total TVA |  |  |  |  |
| Décompte Travaux Sup. HT |  |  |  |  |
| Décompte Travaux Sup TTC |  |  |  |  |
| Décompte Travaux Sup TVA |  |  |  |  |
| Décompte TTC |  |  |  |  |
| Décomptes TVA |  |  |  |  |
| Description |  |  |  |  |
| Durée de garantie (mois) |  |  |  |  |
| Durée forfaitaire (j) |  |  |  |  |
| Durée (j) |  |  |  |  |
| Écart Coût et Est. Confidentielle HT |  | Coût HT - Montant estimation confidentielle HT |  |  |
| Écart Coût et Est. Confidentielle TTC |  | Coût TTC - Montant estimation confidentielle TTC |  |  |
| Écart Coût et Est. Forfaitaire HT |  | Coût TTC - Montant estimation Forfaitaire HT |  |  |
| Écart Coût et Est. Forfaitaire TTC |  | Coût TTC - Montant estimation Forfaitaire TTC |  |  |
| Écart Coût et Marché ajusté HT |  | Coût TTC - Montant Marché ajusté HT |  |  |
| Écart Coût et Marché ajusté TTC |  | Coût TTC - Montant Marché ajusté TTC |  |  |
| Écart Coût et Marché HT |  | Coût TTC - Montant Marché HT |  |  |
| Écart Coût et Marché TTC |  | Coût TTC - Montant Marché TTC |  |  |
| Écart Est. Confidentielle et forfaitaire HT |  | Écart estimation confidentielle HT - Confidentielle et forfaitaire HT |  |  |
| Écart Est. Confidentielle et forfaitaire TTC |  | Écart estimation confidentielle TTC - Confidentielle et forfaitaire TTC |  |  |
| Écart Marché Ajusté et Est. Confidentielle HT |  | Écart Marché Ajusté HT - Est. Confidentielle HT |  |  |
| Écart Marché Ajusté et Est. Confidentielle TTC |  | Écart Marché Ajusté TTC - Est. Confidentielle TTC |  |  |
| Écart Marché Ajusté et Est. forfaitaire HT |  | Écart Marché Ajusté HT - Est. forfaitaire HT |  |  |
| Écart Marché Ajusté et Est. forfaitaire TTC |  | Écart Marché Ajusté TTC -Est. forfaitaire TTC |  |  |
| Écart Marché Ajusté et Marché HT |  | Écart Marché Ajusté HT - Marché HT |  |  |
| Écart Marché Ajusté et Marché TTC |  | Écart Marché Ajusté TTC - Marché TTC |  |  |
| Écart Marché et Est. Confidentielle HT |  | Écart Marché HT - Est. Confidentielle HT |  |  |
| Écart Marché et Est. Confidentielle TTC |  | Écart Marché TTC - Est. Confidentielle TTC |  |  |
| Écart Marché et Est. Forfaitaire HT |  | Écart Marché HT - Est. Forfaitaire HT |  |  |
| Écart Marché et Est. Forfaitaire TTC |  | Écart Marché TTC - Est. Forfaitaire TTC |  | Ecart Marché HT - Est. Forfaitaire HT |
| Entité fonctionnelle |  |  |  |  |
| Entité juridique |  |  |  |  |
| Est. Confidentielle HT |  | ∑ des montants HT des lignes du bordereau estimatif |  |  |
| Est. Confidentielle Net à payer |  | ∑ des montants TTC des lignes du bordereau estimatif - ∑ des montants de régulation. |  |  |
| Est. Confidentielle TTC |  | ∑ des montants TTC des lignes du bordereau estimatif |  |  |
| Est. Confidentielle TVA |  | ∑ des montants TVA des lignes du bordereau estimatif |  |  |
| Est. Forfaitaire HT |  |  |  |  |
| Est. Forfaitaire Net à payer |  | Montants TTC estimation Forfaitaire - ∑ des montants de régulation. |  |  |
| Est. Forfaitaire TTC |  | Montant estimation forfaitaire TTC saisi par l’utilisateur |  |  |
| Est. Forfaitaire TVA |  | Montant estimation forfaitaire TVA |  |  |
| Factures Approvisionnement HT |  |  |  |  |
| Factures Approvisionnement TTC |  |  |  |  |
| Factures Approvisionnement TVA |  |  |  |  |
| Factures Avenant HT |  |  |  |  |
| Factures Avenant TTC |  |  |  |  |
| Factures Avenant TVA |  |  |  |  |
| Factures Coût HT |  | ∑ des montants HT des factures |  |  |
| Factures Coût TTC |  | ∑ des montants TTC des factures |  |  |
| Factures Coût TVA |  | ∑ des montants TVA des factures |  |  |
| Factures Dépassement HT |  | Σ des montants des factures HT avec bon de commande - Σ des avoirs HT avec bon de commande |  |  |
| Factures Dépassement TTC |  | Σ des montants des factures TTC avec bon de commande - Σ des avoirs TTC avec bon de commande |  |  |
| Factures Dépassement TVA |  | Σ des montants des factures TVA avec bon de commande - Σ des avoirs TVA avec bon de commande |  |  |
| Factures Fluctuation HT |  |  |  |  |
| Factures Fluctuation TTC |  |  |  |  |
| Factures Fluctuation TVA |  |  |  |  |
| Facture HT |  |  |  |  |
| Factures Net à payer |  | ∑ des montants des factures nettes à payer |  |  |
| Factures réglé |  | ∑ des montants des règlements des factures |  |  |
| Factures Régulation |  | ∑ des montants régulation des factures + ∑ des montants régulation des Avenant facturés |  |  |
| Factures Remboursement |  |  |  |  |
| Factures Reste |  |  |  |  |
| Facture RNR |  |  |  |  |
| Facture Total HT |  | ∑ des montants HT des factures |  |  |
| Factures Total TTC |  | ∑ des montants TTC des factures |  |  |
| Factures Total TVA |  | ∑ des montants TVA |  |  |
| Factures Travaux Sup. HT |  | ∑ des montants des factures HT qui n’ont pas des bons de commande |  |  |
| Factures Travaux Sup. TTC |  | ∑ des montants des factures TTC qui n’ont pas des bons de commande |  |  |
| Factures Travaux Sup. TVA |  | ∑ des montants des factures TVA qui n’ont pas des bons de commande |  |  |
| Factures TTC |  | ∑ des montants TTC des factures - ∑ des montants TTC des dépassements - ∑ des montants TTC achats sans bon de commande |  |  |
| Factures TVA |  | ∑ des montants TVA des factures - ∑ des montants TVA des dépassements - ∑ des montants TTC achats sans bon de commande |  |  |
| Fournisseur |  |  |  |  |
| Fournisseur code |  |  |  |  |
| Marché Ajusté HT |  | ∑ Montant HT des bons de commande |  |  |
| Marché Ajusté Net à payer |  |  |  |  |
| Marché Ajusté TTC |  | ∑ Montant TTC des bons de commande |  |  |
| Marché Ajusté TVA |  | ∑ Montant TTC des bons de commande |  |  |
| Marché HT |  | ∑ Montant HT des bons de commande |  |  |
| Marché Net à payer |  | Marché TTC – Montant régulation |  |  |
| Marché TTC |  | ∑ Montant TTC des bons de commande |  |  |
| Marché TVA |  | ∑ Montant TVA des bons de commande |  |  |
| Message d’alerte |  |  |  |  |
| Modification HT |  |  |  |  |
| Modification TTC |  |  |  |  |
| Modification TVA |  |  |  |  |
| Multi-Fournisseurs |  |  |  |  |
| Options Appliquer la remise sur les travaux supplémentaires |  |  |  |  |
| Options Appliquer la retenue sur les travaux supplémentaires |  |  |  |  |
| Options avec décomposition |  |  |  |  |
| Options Bloquer la restitution de la retenue de garantie |  |  |  |  |
| Options Possibilité de saisir l’avancement (sinon le coefficient) par ligne de décompte |  |  |  |  |
| Options Saisie des décomptes basée sur les quantités exécutées (sinon sur le cumul) |  |  |  |  |
| Options Taux de remboursement des Appro. |  |  |  |  |
| Options Total des points pour la décomposition |  |  |  |  |
| Options TVA |  |  |  |  |
| Options Variation dans la masse |  |  |  |  |
| Prévision HT |  | Montant HT estimation forfaitaire, sinon montant HT estimation confidentielle sinon montant HT marché sinon montant HT marché ajusté |  |  |
| Prévision Net à payer |  | Prévision TTC – Montant RNR (Montant RNT calculé par rapport le montant prévision TTC) |  |  |
| Prévision TTC |  | Montant TTC estimation forfaitaire, sinon montant TTC estimation confidentielle sinon montant TTC marché sinon montant TTC marché ajusté |  |  |
| Prévision TVA |  | Montant TVA estimation forfaitaire, sinon montant TVA estimation confidentielle sinon montant TVA marché sinon montant TVA marché ajusté |  |  |
| Projet |  |  |  |  |
| R.G. Montant |  |  |  |  |
| R.G. Pénalité |  |  |  |  |
| R.G. Restituée |  |  |  |  |
| R.G. Restituée Reste |  |  |  |  |
| R.G. Retenue |  |  |  |  |
| R.G. Retenue avenant |  |  |  |  |
| R.G. Taux |  |  |  |  |
| Résiliation Causes |  |  |  |  |
| Résiliation Date |  |  |  |  |
| Statut |  |  |  |  |
| Tranches |  |  |  |  |
| Type |  |  |  |  |
| Type de paiement |  |  |  |  |
| Type de paiement sur (jours) |  |  |  |  |

### Champs

| LIBELLE | U | O | OBSERVATIONS |  |
|---|---|---|---|---|
| Code | X |  | En lecture seule |  |
| Description |  | X |  |  |
| Projet |  | X | En lecture seule |  |
| Catégorie |  | X |  |  |
| Type |  | X |  |  |
| Type de paiement |  | X |  |  |
| Sur (j) |  | X |  |  |
| Avancement |  |  |  |  |
| Clôturé |  |  | Si la case est cochée, le système modifie l’avancement à 100% |  |
| DUREE FORFAITAIRE |  |  |  |  |
| Date début |  | X |  |  |
| Durée (j) |  | X |  |  |
| Date fin |  | X |  |  |
| DUREE PREVISIONNELLE |  |  |  |  |
| Date début |  | X |  |  |
| Durée (j) |  | X |  |  |
| Date fin |  | X |  |  |
| GENERAL HT |  |  |  |  |
| Est. forfaitaire |  |  | Saisie manuellement, selon les habilitations accordées |  |
| Est. confidentielle |  |  | En lecture seule,<br> Σ des montants HT des lignes à partir de bordereau estimatif |  |
| Commande |  |  | En lecture seule,<br> Σ des montants HT des bons de commande |  |
| Dépassements |  |  | En lecture seule,<br> Σ des montants des factures HT avec bon de commande - Σ des avoirs HT avec bon de commande |  |
| Achats sans B.C |  |  | En lecture seule,<br> Σ des montants des factures HT sans bon de commande |  |
| Livraisons |  |  | En lecture seule,<br> Σ des montants HT des bons de livraison |  |
| Factures |  |  | En lecture seule,<br> Σ des montants HT des factures |  |
| Avoirs |  |  | En lecture seule,<br> Σ des montants HT des avoirs |  |
| GENERAL TTC |  |  |  |  |
| Est. forfaitaire |  |  | Est. forfaitaire HT * Taux TVA |  |
| Est. confidentielle |  |  | En lecture seule,<br> Σ des montants TTC des lignes à partir de bordereau estimatif |  |
| Commande |  |  | En lecture seule,<br> Σ des montants TTC des bons de commande |  |
| Dépassements |  |  | En lecture seule,<br> Σ des montants des factures TTC avec bon de commande - Σ des avoirs TTC avec bon de commande |  |
| Achats sans B.C |  |  | En lecture seule,<br> Σ des montants des factures TTC sans bon de commande |  |
| Livraisons |  |  | En lecture seule,<br> Σ des montants TTC des bons de livraison |  |
| Factures |  |  | En lecture seule,<br> Σ des montants TTC des factures |  |
| Avoirs |  |  | En lecture seule,<br> Σ des montants TTC des avoirs |  |
| Coût prévisionnel HT |  |  |  |  |
| Bordereau |  |  | En lecture seule,<br> Si Σ des montants HT des bons de commande + Montant dépassement HT + Σ des montants HT des Achats sans bon de commande > = Estimation confidentielle HT<br> Bordereau HT = Σ des montants HT des bons de commande + Montant dépassement HT + Σ des montants HT des Achats sans bon de commande<br> sinon<br> Bordereau HT = Estimation confidentielle HT |  |
| Coût prévisionnel TTC |  |  |  |  |
| Bordereau |  |  | En lecture seule,<br> Si Σ des montants TTC des bons de commande + Montant dépassement TTC + Σ des montants TTC des Achats sans bon de commande > = Estimation confidentielle TTC<br> Bordereau TTC = Σ des montants TTC des bons de commande + Montant dépassement TTC + Σ des montants TTC des Achats sans bon de commande<br> sinon<br> Bordereau TTC = Estimation confidentielle TTC |  |
| Ret. non rem (Retenue non remboursable) |  |  |  |  |
| Remboursement |  |  |  |  |
| Régulation |  |  | En lecture seule,<br> Σ des montants Régul. des factures+ Σ des montants des timbres fiscaux des factures +Σ des montants Régul. des documents à régler + Σ des montants des timbres fiscaux des documents à régler - Σ des montants Régul. des avoirs+ Σ des montants des timbres fiscaux des avoirs. |  |
| Net à payer |  |  | En lecture seule,<br> Bordereau TTC + Montant remboursement facturé + Montant régulation. |  |
| Reste |  |  | Net à payer - Réglé |  |
| Coût réel HT |  |  |  |  |
| Bordereau |  |  | En lecture seule,<br> Σ des montants HT des factures avec bons de commande + Σ des montants HT des factures sans bons de commande - Σ des montants HT des avoirs (pour les factures avec bons de commande et sans bons de commande) |  |
| Coût réel TTC |  |  |  |  |
| Bordereau |  |  | En lecture seule,<br> Σ des montants TTC des factures avec bons de commande + Σ des montants TTC des factures sans bons de commande - Σ des montants TTC des avoirs (pour les factures avec bons de commande et sans bons de commande) |  |
| Ret. non rem (retenue non remboursable) |  |  |  |  |
| Remboursement |  |  |  |  |
| Régulation |  |  | En lecture seule,<br> Σ des montants Régul. des factures+ Σ des montants des timbres fiscaux des factures +Σ des montants Régul. des documents à régler + Σ des montants des timbres fiscaux des documents à régler - Σ des montants Régul. des avoirs+ Σ des montants des timbres fiscaux des avoirs. |  |
| Net à payer |  |  | En lecture seule,<br> Montant facture TTC + Montant remboursement facturé + Montant régulation |  |
| Réglé |  |  | En lecture seule,<br> Σ des montants des règlements des factures + Σ des montants des règlements des documents à régler. |  |
| Reste |  |  | En lecture seule,<br> Net à payer - Réglé. |  |
| Avances |  |  |  |  |
| Marché |  |  | En lecture seule,<br> Σ des montants des avances sur bons de commande (à partir des bons de commande) |  |
| Validée |  |  | En lecture seule,<br> Σ des montants des avances sur bons de commande (qui ont des documents à régler ) |  |
| Réglée |  |  | En lecture seule,<br> Σ des montants de règlements des avances sur bon de commande |  |
| Amortie |  |  | En lecture seule,<br> Σ des montants de avances à partir des factures |  |
| Reste à amortir |  |  | En lecture seule,<br> Réglée - Amortie |  |
| DEVIS |  |  |  |  |
| Liste des devis |  |  | La liste visualise les devis en indiquant :<br>- La date<br>- Le code<br>- Le fournisseur<br>- Le montant HT…<br> Aussi, dans cette liste on peut effectuer les opérations suivantes :<br>- Ajouter un devis<br>- Supprimer un devis<br>- Éditer un devis<br>- Générer un bon de commande<br>- Générer un bon de livraison<br>- Générer une facture |  |
| BON DE COMMANDE |  |  |  |  |
| Liste des bons de commandes |  |  | La liste visualise les bons de commandes en indiquant :<br>- La date<br>- Le code<br>- Le fournisseur<br>- Le montant HT…<br> Aussi, dans cette liste on peut effectuer les opérations suivantes :<br>- Ajouter un bon de commande<br>- Éditer un bon de commande<br>- Supprimer un bon de commande<br>- Générer un bon de livraison<br>- Générer une facture |  |
| BON DE LIVRAISON |  |  |  |  |
| Liste des bons de livraisons |  |  | La liste visualise les bons de livraisons en indiquant :<br>- La date<br>- Le code<br>- Le fournisseur<br>- Le montant HT…<br> Aussi, dans cette liste on peut effectuer les opérations suivantes :<br>- Ajouter un bon de livraison<br>- Éditer un bon de livraison<br>- Supprimer un bon de livraison<br>- Générer une facture |  |
| FACTURE |  |  |  |  |
| Liste des factures |  |  | La liste visualise les bons de livraisons en indiquant :<br>- La date<br>- Le code<br>- Le fournisseur<br>- Le [statut](#page-statuts-buy)<br>- Le montant HT…<br> Aussi, dans cette liste on peut effectuer les opérations suivantes :<br>- Ajouter une facture<br>- Supprimer une facture<br>- Éditer une facture |  |
| AVOIRS |  |  |  |  |
| Liste des avoirs |  |  | La liste visualise les avoirs en indiquant:<br>- La date<br>- Le code<br>- Le fournisseur<br>- Le [statut](#page-statuts-buy)<br>- Le montant HT…<br> Aussi, dans cette liste on peut effectuer les opérations suivantes :<br>- Ajouter un avoir<br>- Supprimer un avoir<br>- Éditer un avoir |  |
| OPTIONS |  |  |  |  |
| Inclure dans les prévisions de paiement |  |  |  |  |
| TVA |  |  |  |  |
| AUTRES |  |  |  |  |
| Description détaillée |  |  |  |  |
| Notes |  |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter lot multi-fournisseurs | GEN0008 |
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Copier | GEN0019 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Imprimer | GEN0011 |
| Prévision | Consulter les [prévisions](#page-previsions-multi) et l’avancement réel |
| Bordereaux estimatif | Gestion des bordereaux estimatifs |
| Offre des entreprises | Gestion des offres entreprises |
| Avoirs | Gestion des avoirs |
Créé avec HelpNDoc Personal Edition: [Créer des documents d'aide HTML facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

#### PREVISIONS <a id="page-previsions-multi"></a>
_Page : `PREVISIONS_Multi.html` — Module Projet › LOT MULTI FOURNISSEUR_

| Prévision sans bon de commande |
|---|
| [image : NouvelElement825.bmp]<br>Série en vert<br> Correspond les prévisions du montant forfaitaire et l’avancement forfaitaire<br> Série en bleu<br> Correspond les prévisions confidentielles et l’avancement confidentielle<br>- Le système a automatiquement réparti les montants totaux en portions égaux sur toute la durée du lot tout<br> On peut modifier les prévisions forfaitaire et / ou confidentielles en cliquant sur le bouton « Modifier les prévisions »<br> Courbe en vert<br> Correspond au courbe d'avancement on se basant sur le montant forfaitaire |
| Modification des prévisions |
| On peut modifier les prévisions en cliquant sur le bouton « Modifier les prévision » comme elle indique la capture ci-dessous<br>[image : NouvelElement826.bmp]<br>Le résultat obtenu sera comme suit :<br>[image : NouvelElement827.bmp]<br>Le système change l’altitude des séries (Les séries en blues de l'estimation confidentielle) en fonction des pourcentages saisi |
| Prévision avec bons de commande |
| Lorsqu’on ajoute deux bons de commande avec les montants et les dates respectives :<br>- Bon de commande N°1 : Montant 50000, Le 09/03/2018<br>- Bon de commande N°2 : Montant 50000, Le 09/05/2018<br> Le système affiche deux séries en bleues (confidentielle) dans les dates des bons des commandes saisies comme elle indique la capture ci-dessous<br>[image : NouvelElement828.bmp] |
| Prévision avec bon de commande et avance sur bon de commande |
| Lorsqu’on ajoute une avance sur bon de commande, le système :<br>- Affiche une série bleue dans la date échéance de document à régler d’avance<br>- Soustrait le montant d’avance sur bon de commande de la série qui correspond la bon de commande source.<br>[image : NouvelElement829.bmp] |
| Prévision avec bons de commande et facture |
| Lorsqu’on ajoute une facture, le système :<br>- Affiche une série bleue dans la date échéance de la facture<br>- L’altitude de la facture correspond au montant net à payer de la facture<br>- Soustrait le montant net à payer de la facture de la série bleue qui correspond le bon de commande source de ce dernier.<br> Dans cet exemple, la date échéance de la facture est 09/06/2018<br> [image : NouvelElement830.bmp] |
| Prévision avec bons de commande facture et avoirs |
| Lorsqu’on génère un avoir, le système soustrait son montant de la série qui lui correspond (La série ou elle se trouve la facture source)<br>[image : NouvelElement831.bmp]<br>Le montant prévisionnel encadré en bleu = Montant Facture (Net à Payer) – Montant Avoir (Net à Payer) |
| Prévision avec bons de commande, facture, avoirs et règlement |
| Lorsqu’on effectue un règlement, le système :<br>- Affiche le montant réglé dans une série en rouge dans la date échéance du règlement<br>- Garde le reste à régler de la facture en bleu dans la date échéance de la facture (si le reste à régler = 0, le système supprime la barre entière de la prévision facture)<br>[image : NouvelElement832.bmp] |
Créé avec HelpNDoc Personal Edition: [Créer des fichiers d'aide Qt Help multi-plateformes](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creez-des-fichiers-daide-pour-le-framework-qt-help)

#### BORDEREAU ESTIMATIF <a id="page-bordereauestimatif-multi"></a>
_Page : `BORDEREAUESTIMATIF_Multi.html` — Module Projet › LOT MULTI FOURNISSEUR_

[image : NouvelElement837.jpg]

### Liste

| LIBELLE | V | OBSERVATIONS |
|---|---|---|
| Catégorie |  |  |
| O.D.T | X |  |
| Code | X |  |
| Crée le |  |  |
| Crée par |  |  |
| Description | X |  |
| Unité | X |  |
| Quantité | X |  |
| P.U HT | X | (PU HT après remise * Pourcentage FODEC) + (PU HT après remise) |
| FODEC | X | Pourcentage FODEC |
| TVA | X | Pourcentage TVA |
| P.U TTC | X | PU HT (avec FODEC) * Pourcentage TVA + (PU HT (avec FODEC)) |
| TOTAL HT | X | P.U HT * Quantité |
| TOTAL TTC | X | P.U TTC * Quantité |
| Modifié le |  |  |
| Modifié par |  |  |
| Notes | X |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter groupe | Ajouter un nœud racine dans la hiérarchie du bordereau |
| Ajouter ligne | Ajouter une ligne dans le bordereau |
| Supprimer | Supprimer une ligne du bordereau |
| Régénérer les codes | Le système réorganise la codification des lignes du bordereau |
| Importer/Exporter | Importer: GEN0011<br> Exporter:GEN0012 |
| Développer | Le système afficher toute lahiérarchie du bordereau |
| Réduire | Le système réduit toute la hiérarchie du bordereau |
Créé avec HelpNDoc Personal Edition: [Création d'aide CHM, PDF, DOC et HTML d'une même source](https://www.helpndoc.com/fr/outils-creation-aide)

#### OFFRES DES ENTREPRISES <a id="page-offresdesentreprises-multi"></a>
_Page : `OFFRESDESENTREPRISES_Multi.html` — Module Projet › LOT MULTI FOURNISSEUR_

[image : NouvelElement834.jpg]

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| O.D.T |  |  | En lecture seule |
| Code |  | X |  |
| Fournisseur |  |  | En lecture seule,<br> Dans ce champ, le système affiche le fournisseur sélectionné |
| Présélection |  |  |  |
| Liste des présélections |  |  | [image : NouvelElement835.jpg]<br>La liste visualise les données des fournisseurs qui ont répondu à la consultation dans les délais en indiquant:<br>- Le fournisseur<br>- La date de la lettre de consultation<br>- La date du retrait de consultation<br>- La date dépôt de l'offre...<br>Aussi, dans cette liste on peut effectuer les opérations suivantes<br>- Ajouter un fournisseur<br>- Ajouter un [devis](#page-devis) pour un fournisseur<br>- Supprimer un fournisseur<br>- Écarter un fournisseur |
| Devis |  |  |  |
| Liste des devis |  |  | [image : NouvelElement836.jpg]<br>La liste visualise les devis en indiquant:<br>- La date<br>- Le code<br>- Le fournisseur<br>- Le montant HT<br>- Le montant TVA<br>- Le montant TTC...<br>Aussi, dans cette liste on peut effectuer les opérations suivantes:<br>- Supprimer devis<br>- Éditer devis<br>- Sélectionner fournisseur<br>- Dé-sélectionner fournisseur<br>- Générer bon de commande<br>- Générer bon de livraison<br>- Générer facture |
| Commande |  |  |  |
| Liste des bons de commande |  |  | La liste visualise les bons de commande en indiquant:<br>- La date<br>- Le code<br>- Le fournisseur<br>- Le montant HT<br>- Le montant TVA<br>- Le montant TTC...<br>Aussi, dans cette liste on peut effectuer les opérations suivantes:<br>- Éditer<br>- Supprimer<br>- Générer bon de livraison<br>- Générer facture |
| Bon de livraison |  |  |  |
| Liste des bons de livraison |  |  | La liste visualise les bons de livraison en indiquant:<br>- La date<br>- Le code<br>- Le fournisseur<br>- Le montant HT<br>- Le montant TVA<br>- Le montant TTC...<br>Aussi, dans cette liste on peut effectuer les opérations suivantes:<br>- Éditer<br>- Supprimer<br>- Générer facture |
| Factures |  |  |  |
| Liste des factures |  |  | La liste visualise les factures en indiquant:<br>- La date<br>- Le code<br>- Le fournisseur<br>- Le montant HT<br>- Le montant TVA<br>- Le montant TTC...<br>Aussi, dans cette liste on peut effectuer les opérations suivantes:<br>- Éditer<br>- Supprimer<br>- Générer avoir |
| Avoirs |  |  |  |
| Liste des avoirs |  |  | La liste visualise les avoirs en indiquant:<br>- La date<br>- Le code<br>- Le fournisseur<br>- Le montant HT<br>- Le montant TVA<br>- Le montant TTC...<br>Aussi, dans cette liste on peut effectuer les opérations suivantes:<br>- Éditer<br>- Supprimer |
Créé avec HelpNDoc Personal Edition: [Générateur de documentation complet](https://www.helpndoc.com/fr)

### DECOMPTES <a id="page-decomptes"></a>
_Page : `DECOMPTES.html` — Module Projet_

[image : NouvelElement594.png]

### Liste

| LIBELLE | V | OBSERVATIONS |
|---|---|---|
| Amortissement avance | X |  |
| Amortissement avance taux |  |  |
| Avancement | X | Cumul actuel TTC / Montant marché TTC |
| Avec décomposition |  |  |
| Code | X |  |
| Code facture | X |  |
| Date | X |  |
| Description |  |  |
| Est le dernier décompte saisi |  |  |
| Fournisseur | X |  |
| Lot | X |  |
| Montant HT | X | Montant décompte hors taxe sans tenir compte les retenues et l’amortissement d’avance |
| Montant TTC | X | Montant HT * taux de TVA + Montant HT |
| Montant TVA | X | Montant HT * taux de TVA |
| Net à payer facture | X |  |
| Net à payer | X | Total TTC - ∑ (retenue + Amortissement d’avance + Montant d’avance sur décompte) |
| Net à payer après retenue à la source | X | Net à payer -(Net à payer * taux de retenue de garantie) |
| Notes | X |  |
| O.D.T (Ordre de tri) |  |  |
| Projet | X |  |
| Référence interne |  | Référence interne de décompte |
| Règle facture | X | Le montant affecté à une facture |
| Remboursement du compte prorata | X | Total TTC * Taux de remboursement du compte prorata |
| Remboursement fluctuations | X |  |
| Remboursement approvisionnement | X |  |
| Reste à régler facture | X | Net à payer – Montant réglé |
| Retenu à la source | X | Montant de la retenue à la source |
| Retenue assurance décennale |  | Montant TTC * taux de retenue d’assurance décennale |
| Retenue assurance décennale taux |  |  |
| Retenue assurance tout risque chantier |  | Montant TTC * Taux retenue assurance tout risque chantier |
| Retenue assurance tout risque chantier taux |  |  |
| Retenue de compte prorata | X | Montant TTC * Taux retenue de compte prorata |
| Retenue de compte prorata taux |  |  |
| Retenue de garantie | X |  |
| Retenue de garantie taux |  |  |
| Retenue avance sur décompte | X | Montant déduit de l’avance sur décompte en TTC |
| Retenue forfaitaire | X | ∑ de retenues forfaitaire de tous les décomptes d’un marché en TTC |
| Travaux supplémentaire | X | ∑ des montants des travaux supplémentaire en TTC |

### Fiche

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Code | X |  | En lecture seule |
| N° Décompte |  | X | En lecture seule |
| Lot |  |  | En lecture seule |
| Date |  |  | En lecture seule |
| Note |  |  | En lecture seule |
| Référence interne |  |  | En lecture seule |
| Tiers |  |  | En lecture seule |
| Avancement |  |  | En lecture seule |
| Total TTC |  |  | En lecture seule |
| Cumul actuel |  |  | En lecture seule |
| Avance |  |  | En lecture seule |
| Retenue de garantie |  |  | En lecture seule |
| Total retenue remboursable |  |  | En lecture seule |
| Retenue d’assurance décennale |  |  | En lecture seule |
| Retenue de compte prorata |  |  | En lecture seule |
| Total retenue non remboursable |  |  | En lecture seule |
| Cumul actuel après retenue |  |  | En lecture seule |
| Cumul précédent retenue |  |  | En lecture seule |
| Travaux du mois |  |  | En lecture seule |
| Travaux supplémentaires |  |  | En lecture seule |
| Taux de remboursement approvisionnement (T) |  |  | En lecture seule |
| Ancienne valeur approvisionnement (A) |  |  | En lecture seule,<br> Nouvelle valeur approvisionnement du décompte précédent.<br> (Voir l'exemple au dessous) |
| Nouvelle valeur approvisionnement (B) |  |  | En lecture seule,<br> Σ des montants des lignes d'approvisionnement du décompte en question.<br> (Voir l'exemple au dessous) |
| Remboursement approvisionnement (C) |  |  | En lecture seule,<br> = ((B)*(T)) - ∑ montants de remboursement des Approvisionnement. précédents<br> (Voir l'exemple au dessous) |
| Avances sur décompte |  |  | En lecture seule |
| A déduire |  |  | En lecture seule |
| Retenues forfaitaires |  |  | En lecture seule |
| Remboursement de compte prorata |  |  | Remarque1: Ce champ sera modifiable par le lot qui est désigné comme un lot "Gestionnaire du compte prorata".<br>Remarque2: Le système bloque le remboursement du montant au gestionnaire du compte prorata lorsqu'il atteint 100% du montant déjà retenu. |
| Ancienne valeur fluctuation |  |  | En lecture seule |
| Nouvelle valeur fluctuations |  |  | En lecture seule |
| Remboursement fluctuation |  |  | En lecture seule |
| Net à Payer |  |  | En lecture seule |
| Retenu à la source |  |  | En lecture seule |
| Net à payer après retenu à la source |  |  | En lecture seule |
| Dernier décompte |  |  | En lecture seule |
| Lignes |  |  |  |
| Liste des lignes de décompte |  |  | Le système visualise la liste des lignes de décompte en indiquant:<br>- L'ordre de tri<br>- Le code<br>- La description<br>- L'unité<br>- La quantité totale<br>- La quantité antérieure<br>- La quantité décompté<br>- Le Coefficient<br>- La quantité exécutée<br>- La quantité cumulée...<br>Aussi, dans cette liste on peut effectuer les opérations suivantes:<br>- Ajouter une ligne<br>- Supprimer une ligne<br>- Exporter<br>Remarque: Lorsqu'on choisie l'action ajouter une ligne, le système affiche l'écran suivant:<br>[image : NouvelElement550.png]<br>Lorsqu'on saisi le code de la ligne, le système charge les données associées à l'article choisi. |
| Approvisionnement |  |  |  |
| Liste d'approvisionnement |  |  | Le système visualise la liste des approvisionnement en indiquant:<br>- L'Ordre de tri<br>- Le code<br>- La description<br>- L'unité<br>- La quantité<br>- Le prix unitaire hors taxe<br>- FODEC...<br>Remarque: Lorsqu'on clique sur "Recharger les lignes", le système charge tous les lignes d'approvisionnement. |
| Travaux supplémentaires |  |  |  |
| Liste des travaux supplémentaire |  |  | Le système visualise la liste des travaux supplémentaire en indiquant:<br>- L'ordre de tri<br>- Le code<br>- La description<br>- L'unité<br>- Les quantités exécutées...<br>Aussi, dans cette liste on peut effectuer les opérations suivantes:<br>- Ajouter<br>- Ajouter une ligne basée sur BDP<br>- Supprimer |
| Avenant |  |  |  |
| Liste des avenants |  |  | Le système visualise la liste des avenants en indiquant:<br>- L'ordre de tri<br>- Le code<br>- La description<br>- L'unité<br>- Les quantités exécutées...<br>Aussi, dans cette liste on peut effectuer les opérations suivantes:<br>- Ajouter<br>- Ajouter une ligne basée sur BDP<br>- Supprimer |
| Retenues forfaitaires |  |  |  |
| Liste des retenues forfaitaires |  |  | Le système visualise la liste des retenues forfaitaires en indiquant:<br>- L'ordre de tri<br>- La description<br>- Le montant<br>Aussi, dans cette liste on peut effectuer les opérations suivantes:<br>- Ajouter<br>- Supprimer |

### Exemple calcule des approvisionnements

|  | (A) | (B) | (C) | (T) |
|---|---|---|---|---|
| Décomptes | Ancien valeur Approvisionnement | Nouvelle valeur Approvisionnement | Remboursement valeur Approvisionnement | Taux de remboursement |
| Décompte 1 | 0 | 100 000 | 80 000 | 80% |
| Décompte 2 | 100 000 | 300 000 | 160 000 | 80% |
| Décompte 3 | 300 000 | 500 000 | 260 000 | 100% |
| Décompte 4 | 500 000 | 250 000 | -300 000 | 80% |
| Décompte 5 | 250 000 | 0 | -200 000 | 80% |
| Formules | = Nouvelle valeur Approvisionnement de décompte précédent | = ∑ des lignes d’approvisionnement | = ((B)*(T))- ∑ montants de remboursement des Approvisionnement précédents |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Éditer | GEN0009 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Annuler et fermer | GEN0017 |
| Actions | GEN0005 |
| Fichiers attachés | GEN0004 |
| Champs spécifiques | GEN0007 |
| Audit | GEN0020 |
| Recharger les lignes jusque’à la date de ce décompte | Lorsque’on clique sur le bouton « Recharger les lignes jusque’à la date de ce décompte », le système recharge les lignes du décompte précédent (Description, Unité, Quantité total, Quantité antérieur, Quantité décomptée, Quantité cumulée, Avancement…) |
| Définir en tant que dernier décompte | Lorsque’on clique sur le bouton « Définir en tant que dernier décompte », le système charge la quantité restante à décompter et il calcule le montant du décompte |
| Recalculer décompte | Le système vérifie si la facture générée sur le décompte est réglée ou non<br>- Si la facture est réglée le système affiche un message "Vous ne pouvez pas modifier un décompte avec une facture réglée"<br>- Sinon, le système rend le décompte modifiable (Les lignes de décompte ...) et il met à jour<br>1. Le pourcentage d'avance<br>2. Le pourcentage de retenue de granite<br>3. Le pourcentage de retenue d'assurance décennale<br>4. Le pourcentage de retenue d’assurance tout risque chantier<br>5. Le pourcentage de retenue de compte prorata<br> Après avoir finir les modifications, en cliquant sur "Enregistrer et fermer" le système affiche un message "Le facture associé va être mise à jour"<br>- Si on clic "oui", le système met à jour le montant de la facture<br>- Sinon, le système annule les modifications |
Créé avec HelpNDoc Personal Edition: [Créer des sites web d'aide facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

### REUNIONS DE CHANTIER <a id="page-reunionsdechantier"></a>
_Page : `REUNIONSDECHANTIER.html` — Module Projet_

[image : NouvelElement369.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| O.D.T |  |  | En lecture seule, auto généré |
| Référence P.V |  |  | En lecture seule, auto généré |
| Projet |  |  | En lecture seule |
| Surveillant |  |  | Liste déroulante de personnels |
| Date |  |  | Par défaut la date de jour |
| Date de la prochaine réunion |  |  |  |
| Note |  |  |  |
| Maître d’ouvrage |  |  |  |
| Liste de maitres d’ouvrage |  |  | La liste visualise les maîtres d’ouvrage en indiquant :<br>- L’O.D.T<br>- Le contact<br>- Présent<br>- A notifier<br> Aussi, dans cette liste on peut :<br>- Ajouter un maître d’ouvrage<br>- Supprimer un maître d’ouvrage |
| Intervenant |  |  |  |
| Liste des intervenants |  |  | La liste visualise les intervenants en indiquant :<br>- O.D.T<br>- Effectif<br>- Lot<br>- Fournisseur<br>- Note<br> Aussi, dans cette liste on peut faire les opérations suivantes :<br>- Ajouter<br>- Éditer<br>- Supprimer<br>- Ajouter une décision pour un fournisseur |
| Décisions |  |  |  |
| Liste des décisions |  |  | La liste visualise les décisions en indiquant :<br>- O.D.T<br>- Le lot<br>- Le fournisseur<br>- La description<br>- L’observation<br>- L’objectif<br>- La note<br>- Le statut de la décision (Fermé ou non)<br>- GEN0016<br> Aussi, dans cette liste on peut faire les opérations suivantes :<br>- Ajouter<br>- Éditer<br>- Supprimer |
| Avancements |  |  |  |
| Liste des avancements |  |  | La liste visualise l’avancement en indiquant<br>- La désignation.<br>- Les unités de [nivellement](#page-projets)[PROJETS.html](#page-projets)[définit dans le projet.](#page-projets) |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Imprimer | GEN0011 |
Créé avec HelpNDoc Personal Edition: [Sites web iPhone faciles](https://www.helpndoc.com/fr/tour-des-fonctionnalites/generation-de-site-web-iphone)

### MODIFICATIONS <a id="page-modifications"></a>
_Page : `MODIFICATIONS.html` — Module Projet_

[image : NouvelElement370.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Description |  |  |  |
| Statut |  |  | Liste déroulante des statuts, elle peut prendre une des valeurs suivantes :<br>- En cours<br>- Refusée par le service technique<br>- Refusé par le client<br>- Acceptée<br>- Réalisée |
| Date de la demande |  |  |  |
| Date de la validation technique |  |  |  |
| Date de la validation client |  |  |  |
| Coût de la modification |  |  |  |
| Montant de la modification |  |  |  |
| Notes |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Valider (Enregistrer) | GEN0001 |
| Annuler (Annuler et fermer) | GEN0006 |
| Fichiers attachés | GEN0004 |
| Cliquer | GEN0013 |
Remarque: Lorsqu'on ajoute une demande de modification à partir d'un dossier de vente, le système l'affiche dans la liste des modifications automatiquement
Créé avec HelpNDoc Personal Edition: [Créer des sites web d'aide facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

### PREVISIONS <a id="page-previsions"></a>
_Page : `PREVISIONS.html` — Module Projet_

[image : NouvelElement864.bmp]
Remarque: Montant prévisionnel net à payer = Montant estimation forfaitaire, sinon Montant estimation confidentielle, sinon montant marché, sinon montant marché ajusté .

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Total |  |  |  |
| Prévision |  |  | Σ des montants prévisionnels net à payer des lots |
| Prévision ajusté |  |  | Σ des montants des coûts Prévisionnels net à payer |
| Due à ce jour |  |  |  |
| Prévision |  |  | Σ des montants prévisionnels net à payer * Taux d'avancements des prévisions forfaitaire (définit dans l'écran "[Modifier les prévisions](#page-previsions-prj)") par lot par mois. |
| Prévision ajusté |  |  | Σ des montants des coûts Prévisionnels net à payer * Taux d'avancement des prévisions (définit dans l'écran "[Modifier les prévisions](#page-previsions-prj)") par lot par mois. |
| Réel |  |  | Σ des montants net à payer des décomptes facturés à ce jour |
| Reste |  |  |  |
| Prévision |  |  | Prévision total - Prévision due à ce jour |
| Prévision ajusté |  |  | Prévision ajusté total - Prévision ajusté due à ce jour |
| Réel |  |  | Prévision net à payer - Réel |
| Avancement |  |  |  |
| Prévision |  |  | Σ Poids * Taux d'avancement" (Forfaitaire par lot)<br> Poids = Montant prévisionnel HT (du marché ) / Σ des montant prévisionnel HT (de tout les marchés)<br>Exemple:<br> Supposons on a 3 marchés M1, M2 et M3 avec les montants prévisionnel HT et les avancements respectives 1 000 000, 200 000, 50 000, 80%, 100%, 20% (Avancement mentionné)<br>Poids M1 = 1 000 000 / 1 250 000 = 0.8<br> Poids M2 = 200 000 / 1 250 000 = 0.16<br> Poids M3 = 50 000 / 1 250 000 = 0.04<br>Avancement = 0.8 * 0.8 + 0.16 * 1 + 0.04* 0.2 = 80.2 % |
| Prévision ajusté |  |  | Σ Poids * Taux d'avancement ( Prévisionnel par lot)<br> Poids = Montant marché prévisionnel HT / Σ des montants marchés coût prévisionnels HT |
| Avancement réel |  |  | Σ Montant des coûts réel HT / Σ Montants des coûts prévisionnels HT |
| Graphes |  |  |  |
| Les séries vertes |  |  | Représente le montant prévisionnel par mois du projet |
| Les séries bleus |  |  | Représente le montant prévisionnel ajusté par mois du projet |
| Les séries rouges |  |  | Représente le montant réel par mois du projet |
| La courbe verte |  |  | Représente l’avancement prévisionnel par projet |
| La courbe bleu |  |  | Représente l’avancement prévisionnel ajusté par projet |
| la courbe rouge |  |  | Représente l’avancement réel par projet |
| Lots |  |  |  |
| Liste des lots |  |  | Le système affiche la liste des lots qui sont paramétrés visibles dans les prévisions du projet en indiquant:<br>- Le code<br>- La description<br>- ...<br>- Prévision HT<br>- Prévision TVA<br>- Prévision TTC<br>- Prévision net à payer..<br>Remarque: Lorsqu'on fait un double clic, le système affiche l'écran de prévision du lot en question. |
| Prévisions |  |  |  |
| Liste des prévisions |  |  | Le système affiche la liste des prévisions (prévision forfaitaire) qui sont réalisées dans l'écran "[Modifier prévisions](#page-previsions-prj)" pour les lots du projet en question en indiquant<br>- La date<br>- La description du lot<br>- Forfaitaire (le montant forfaitaire)<br>- ... |
| Prévisions ajustées |  |  |  |
| Liste des prévisions ajustées |  |  | Le système affiche la liste des prévisions ajustées (prévision) qui sont réalisées dans l'écran "[Modifier prévisions](#page-previsions-prj)" pour les lots du projet en question en indiquant<br>- La date<br>- La description du lot<br>- Le montant réel<br>- Le montant prévisionnel |
| Avancement prévisionnel |  |  |  |
| Liste des avancements prévisionnel |  |  | Le système affiche la liste des avancements prévisionnels par lot en indiquant:<br>- La date<br>- Le lot<br>- L'avancement forfaitaire<br>- L'avancement prévisionnel<br>- L'avancement réel<br>- La description |
| Avancement ajusté |  |  |  |
| Liste des avancements ajusté |  |  | Le système affiche la liste des avancements ajusté en indiquant:<br>- La date<br>- Le lot<br>- L’avancement prévisionnel<br>- L'avancement forfaitaire<br>- L'avancement réel<br>- La description |
| Avancement réel |  |  |  |
| Liste des avancements réel |  |  | Le système affiche la liste des avancements ajusté en indiquant:<br>- La date<br>- Le lot<br>- L'avancement forfaitaire<br>- L’avancement prévisionnel<br>- L'avancement réel<br>- La description |
Créé avec HelpNDoc Personal Edition: [Générateur d'aide complet](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

### GANTT <a id="page-gantt"></a>
_Page : `GANTT.html` — Module Projet_

[image : NouvelElement436.png]

### UNDER CONSTRUCTION
Créé avec HelpNDoc Personal Edition: [Créer des livres électroniques facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

### TABLEAU DE BORDS <a id="page-tableaudebords-prj"></a>
_Page : `TABLEAUDEBORDS_PRJ.html` — Module Projet_

Créé avec HelpNDoc Personal Edition: [Écrire des livres électronique Kindle](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creer-ebooks-pour-kindle-amazon)

#### BILANS <a id="page-bilans"></a>
_Page : `BILANS.html` — Module Projet › TABLEAU DE BORDS_

[image : NouvelElement881.bmp]
- La liste visualise le bilan de projets sélectionnés en indiquant les champs
- Les montants de l'estimation forfaitaire, estimation confidentielle... sont calculés par catégorie
- En utilisant les cases à cocher, le système donne la possibilité aux utilisateurs de personnaliser les colonnes à afficher.

### Liste

| LIBELLE | V | OBSERVATIONS |
|---|---|---|
| Description |  | Hiérarchie des catégories de dépense |
| Étude de faisabilité HT |  | ∑ des montants HT à partir de bilan de l'étude de faisabilité par catégorie<br> Remarque: Si la catégorie du bilan est paramétré visible dans le module projet , le système affiche la catégorie et le montant HT de ce dernier. |
| Étude de faisabilité TTC |  | ∑ des montants TTC à partir de bilan de l'étude de faisabilité par catégorie<br> Remarque: Si la catégorie du bilan est paramétré visible dans le module projet , le système affiche la catégorie et le montant TTC de ce dernier. |
| Estimation confidentielle HT |  | ∑ des montants HT des estimations confidentielle par catégorie |
| Estimation confidentielle TTC |  | ∑ des montants HT des estimations confidentielle par catégorie |
| Marché HT |  | ∑ des montants HT des marchés par catégorie |
| Marché TTC |  | ∑ des montants TTC des marchés par catégorie |
| Marché ajusté HT |  | Montant Marché HT + Montant modification bordereau définitif HT |
| Marché ajusté TTC |  | Montant Marché TTC + Montant modification bordereau définitif TTC |
| Prévision HT |  | Montant forfaitaire HT sinon Montant confidentielle HT, sinon Montant marché sinon Montant Marché ajusté HT |
| Coût prévisionnel HT |  | Marché ajusté HT + dépassement HT + Travaux supplémentaire HT + Montant fluctuation HT par catégorie. |
| Coût réel HT |  | ∑ des montants HT des factures + ∑ des montants HT des avenants facturés – (∑ des montants HT des avoirs + Montant Approvisionnement (Avoir) + Montant fluctuation (Avoir) par catégorie. |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Imprimer | GEN0011 |
| Cliquer | GEN0013 |
| Exporter | GEN0012 |
Créé avec HelpNDoc Personal Edition: [Créer des livres électroniques EPub facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

#### TRESORERIE <a id="page-tresorerie"></a>
_Page : `TRESORERIE.html` — Module Projet › TABLEAU DE BORDS_

[image : NouvelElement882.bmp]
Cet écran affiche les encaissements et les décaissements par projet pars année en respectant les règles mentionnées dans le tableau ci dessous
Remarque 1: Sous l'onglet "Projet" le système affiche les données groupées par 2 catégories Encaissement et Décaissement selon hiérarchies suivantes:
- Encaissement --> Projets --> Fournisseurs
- Décaissement --> Projets --> Fournisseurs
Remarque 2: Sous l'onglet "Fournisseur" le système affiche les données groupées par 2 catégories Encaissement et Décaissement selon hiérarchies suivantes
- Encaissement --> Fournisseurs
- Décaissement --> Fournisseurs
Remarque 3: Lorsqu'on fait un double clic, le système affiche la liste des mouvements du mois en question
[image : NouvelElement883.bmp]

| Prévisions | Engagements | Règlements |
|---|---|---|
| Le système copie les prévisions associées aux lots dans la trésorerie annuelle en appliquant la règle suivante :<br>- Si la date de prévision <= au mois en cours : le système rapatrie les prévisions dans le mois en cours<br>- Si la date de prévision > mois en cours : Le système affiche les prévisions dans leurs dates | - Le système rapatrie les engagements (Factures et document à régler) selon leurs dates échéance<br>- Si la date échéance < Mois en cours :<br>- Le système rapatrie les montants reste à régler des factures (ou des documents à régler) dans le mois courant<br>- Si la date échéance >= Mois en cours :<br> Le système affiche les montants reste à régler des factures (ou des documents à régler) dans leurs dates échéance respectives.<br>Remarque: On respectant les mêmes règles ci dessus, le système affiche les données qui concerne les Avoirs dans la partie encaissement. | - Le système converti le document en une pièce de règlement avec le montant réglé et il garde le reste à payer comme un engagement<br>Sans retenue à la source<br>- Montant R.S = 0, Le système affiche un mouvement prévisionnel qui correspond au montant net à payer à la date échéance<br>Avec retenue à la source<br>- Si on a un montant R.S > 0, le système affiche le net à payer à la date échéance, et il ajoute une dépense avec le montant R.S au 28 du mois d’après (la date de dépense de R.S est calculer par rapport la date de pièce)<br>Pointage<br>- Si règlement en cours<br> Le système garde toujours le mouvement prévisionnel<br>- Si règlement pointé<br> Le système affiche un mouvement réel dans la date de pointage avec le montant réglé (ou payé) (Si la date est inférieur ou égale le mois en cours)<br>Remarque: On respectant les mêmes règles ci dessus, le système affiche les données qui concerne les Avoirs dans la partie encaissement. |
Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Exporter | GEN0012 |
Créé avec HelpNDoc Personal Edition: [Avantages d'un outil de création d'aide](https://www.helpauthoringsoftware.com/fr)

#### SUIVI DES LOTS <a id="page-suivideslots"></a>
_Page : `SUIVIDESLOTS.html` — Module Projet › TABLEAU DE BORDS_

[image : NouvelElement704.png]

| LIBELLE | OBSERVATION |
|---|---|
| Estimation forfaitaire HT | Estimation forfaitaire HT à partir de lot |
| Estimation forfaitaire TVA | Estimation forfaitaire TVA à partir de lot |
| Estimation forfaitaire TTC | Estimation forfaitaire TTC à partir de lot |
| Estimation confidentielle HT | Estimation confidentielle HT à partir de lot |
| Estimation confidentielle TVA | Estimation confidentielle TVA à partir de lot |
| Estimation confidentielle TTC | Estimation confidentielle TTC à partir de lot |
| Marché HT | Marché HT à partir de lot |
| Marché TVA | Marché TVA à partir de lot |
| Marché TTC | Marché TTC à partir de lot |
| Marché ajusté HT | Pour un lot: Marché HT final + Travaux supplémentaire HT (facturé) + Dépassement HT(*) + Fluctuation (facturé) + Avenant HT – Avoirs HT<br> Pour un lot multifournisseur: Σ des montants HT des bons de commande + Σ des montants HT des factures qui n'ont pas un bon de commande source - Σ des montants HT des avoirs |
| Marché ajusté TVA | Pour un lot: Montant TVA marché final + Travaux supplémentaire TVA (facturé) + Dépassement TVA (*)+ Fluctuation (facturé) + Avenant TVA – Avoirs TVA<br> Pour un lot multifournisseur: Σ des montants TVA des bons de commande + Σ des montants TVA des factures qui n'ont pas un bon de commande source - Σ des montants HT des avoirs |
| Marché ajusté TTC | Pour un lot: Montant marché TTC final + Montant Travaux supplémentaire TTC (facturé) + Montant Fluctuation (facturé) + Dépassement TTC (*) + Montant Avenant TTC – Montant Avoirs TTC<br> Pour un lot multifournisseur: Σ des montants TTC des bons de commande + Σ des montants TTC des factures qui n'ont pas un bon de commande source - Σ des montants HT des avoirs |
| Facturé HT | ∑ Montant HT des factures |
| Facturé TVA | ∑ Montant TVA des factures |
| Facturé TTC | ∑ Montant TTC des factures |
| Retenue non remboursable | Retenue d’assurance TRC + Retenue d’assurance décennal + Retenue de compte prorata + Retenue forfaitaires |
| Réglé |  |
| Reste | Marché ajusté - retenue non remboursable - Montant réglé<br> Remarque: Si on trouve un montant reste négatif, il faut vérifier:<br>- L'amortissement d'avance<br>- Le remboursement de retenue de garantie |
(*): Dépassement: ∑ des montants des quantités dépassées des linges de décomptes
Créé avec HelpNDoc Personal Edition: [Générateur de documentation et EPub gratuit](https://www.helpndoc.com/fr)

#### RAPPORTS <a id="page-rapports-prj"></a>
_Page : `RAPPORTS_PRJ.html` — Module Projet › TABLEAU DE BORDS_

[image : NouvelElement436.png]

### UNDER CONSTRUCTION
Créé avec HelpNDoc Personal Edition: [Générateur de documentation iPhone gratuit](https://www.helpndoc.com/fr/tour-des-fonctionnalites/generation-de-site-web-iphone)

##### ETAT DES MARCHES <a id="page-etatdesmarches"></a>
_Page : `ETATDESMARCHES.html` — Module Projet › TABLEAU DE BORDS › RAPPORTS_

[image : NouvelElement436.png]

### UNDER CONSTRUCTION
Créé avec HelpNDoc Personal Edition: [Créer des sites web d'aide facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

### CONFIGURATION <a id="page-configuration-prj"></a>
_Page : `CONFIGURATION_PRJ.html` — Module Projet_

Créé avec HelpNDoc Personal Edition: [Générateur de documentation iPhone gratuit](https://www.helpndoc.com/fr/tour-des-fonctionnalites/generation-de-site-web-iphone)

#### TRANCHES <a id="page-tranches-prj"></a>
_Page : `TRANCHES_PRJ.html` — Module Projet › CONFIGURATION_

[image : NouvelElement357.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| [Programme](#page-cfg-programmes) |  | X | La description du programme affecté à la tranche en lecture seule |
| Code |  | X | En lecture seule |
| Description |  | X | En lecture seule |
| Délais |  |  |  |
| Date lancement des travaux |  |  | La date lancement de travaux doit être supérieure à la date fin de travaux. |
| Date fin des travaux |  |  |  |
| Remarque : Cette fiche permet aux utilisateurs du module projet d’éditer seulement les dates de lancement et de fin des travaux. |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Éditer | GEN0009 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Annuler et fermer | GEN0006 |
Créé avec HelpNDoc Personal Edition: [Générateur de documentation complet](https://www.helpndoc.com/fr)

#### NIVELLEMENTS <a id="page-nivellements"></a>
_Page : `NIVELLEMENTS.html` — Module Projet › CONFIGURATION_

[image : NouvelElement356.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Code | X | X | Libellé du nivellement |
| Niveaux de nivellement |  |  | Le niveau de nivellement représente les niveaux hiérarchiques dans la gestion des lots avec décomposition pour suivre l’avancement d’un lot. |

### Niveau de nivellement

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| O.D.T | X | X | Ordre de tri du niveau de nivellement et qui définit la profondeur dans la hiérarchie :<br> Exemple :<br> Immeuble (ODT=1)<br> � Niveau (ODT=2)<br> � Ensemble (ODT=3)<br> En lecture seule, généré par le système |
| Code | X | X |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Ajouter un niveau | Pour ajouter un niveau il suffit de saisir le libellé dans la ligne en bas de la grille et d’appuyer sur « Entrer », le système affecte automatiquement l’ordre de tri de la nouvelle ligne. |
| Éditer un niveau | Pour éditer un niveau il suffit de cliquer sur le libellé de la ligne et de le modifier |
| Supprimer un niveau | Il faut sélectionner une ligne et appuyer sur le bouton « Supprimer » |
Créé avec HelpNDoc Personal Edition: [Créer des documents d'aide CHM facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

#### PROJETS <a id="page-projets"></a>
_Page : `PROJETS.html` — Module Projet › CONFIGURATION_

[image : NouvelElement685.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Code | X | X |  |
| Description |  | X |  |
| [Entité juridique](#page-entitesjuridique) |  | X | Liste des[ENTITESJURIDIQUE.html](#page-entitesjuridique)[entités juridique](#page-entitesjuridique) configurées dans le [module stock](#page-modulestock) en lecture seule |
| Inclure dans la liste ventilation des charges indirectes |  |  | Lors de l’ajout ou la modification d’une facture ou d’un document à régler, la liste de ventilation sur les projets n’affiche que les projets ayant la case à cocher « Inclure dans la liste de ventilation des charges indirectes » cochée. |
| Ventilation par défaut pour le documents d'achat |  |  | Par défaut 0%,<br> Ce champ permet de paramétrer une ventilation par défaut qui sera utilisée dans un document (facture par exemple) qui est saisi sans association directe à un lot. |
| Projet référence |  |  | Liste déroulante des projets référence qu’appartient à une entité juridique |
| Nivellement |  |  | Liste des nivellements pré-paramétrés.<br> Le bouton Nivellement n’est activé qu’après avoir choisi un élément de la liste déroulante. Il permet de définir les niveaux hiérarchiques pour la gestion des lots avec décomposition.<br> Exemple :<br> Immeuble 1<br>- Niveau 1<br>- Niveau 2<br> Immeuble 2<br>- Niveau 1<br>- Niveau 2<br> Immeuble …<br>- Niveau 1<br>- Niveau 2<br>- Niveau 3<br>- Niveau … |
| Gestionnaire de compte prorata |  |  | Sélecteur du [lot,](#page-lots)<br> Le lot choisi sera désigné comme un lot du gestionnaire de compte prorata |
| Ventilation par tranche |  |  | La liste visualise les taux des ventilation par tranche en affichant<br>- Le taux de la ventilation<br>- La description de la tranche<br> Aussi, dans cette liste on peu effectuer les opérations suivantes:<br>- Ajouter une ventilation par tranche<br>- Supprimer une ventilation par tranche<br>==> Cette onglet nous permet d'affecter les charges du projet aux tranches de l’entité juridique sélectionnée, avec une clé de répartition.<br> Pourcentage du budget associé à une ou plusieurs tranches. |
| Critères d'avancement |  |  | La liste visualise les critères d'avancement en affichant<br>- L'ordre de tri<br>- La description<br>- La catégorie<br>Aussi, dans cette liste on peu effectuer les opérations suivantes:<br>- Ajouter<br>- Supprimer<br>- Importer depuis d'un autre projet |
| Notes |  |  |  |
| Autres |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Ajouter | GEN0008 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Impression | GEN0011 |
Créé avec HelpNDoc Personal Edition: [Générateur de documentation et EPub facile](https://www.helpndoc.com/fr)

#### STATUTS DES LOTS <a id="page-statutsdeslots"></a>
_Page : `STATUTSDESLOTS.html` — Module Projet › CONFIGURATION_

[image : NouvelElement350.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| O.D.T |  | X | Ordre de tri d’un lot, cet ordre traduit l’avancement chronologique d’un lot dans le flux de travail. |
| Code | X | X |  |
| Description |  | X |  |
| Type |  | X | C’est une liste déroulante avec les valeurs suivantes :<br> Nouveau<br> Intermédiaire<br> Terminé avec sucée<br> Terminé avec échec<br> Annulation |
| Durée de vie |  |  |  |
| Couleur |  |  |  |
| Image |  |  |  |
| Est un marché effectif |  |  | Sous l’onglet « détails » du masque « Les Statuts des lots », les cases à cocher représentent les options activées dans le masque « [Lots](#page-lots) » qui correspondent un statut sélectionné de la liste.<br> Exemple :<br>- Pour le statut « Estimation », les options qui sont activées dans le masque lot sont : Prévisions et Bordereau estimatif. |
| Est une annulation |  |  |  |
| Avec Prévisions |  |  |  |
| Avec Bordereau estimatif |  |  |  |
| Avec Appel d’offre |  |  |  |
| Avec offres des entreprises |  |  |  |
| Avec décomposition |  |  |  |
| Avec avance |  |  |  |
| Avec décomptes |  |  |  |
| Avec retenues de garantie |  |  |  |
| CHAMPS OBLIGATOIRES |  |  |  |
| Cause de résiliation |  |  | Dans la section « Champs obligatoire » du masque « Les statuts des lots », les cases à cocher représentent les champs obligatoires dans le masque «[Lots](#page-lots)» qui correspondent un statut sélectionnée de la liste.<br> Exemple :<br>- Pour le statut « Appel d’offre », les champs obligatoires sélectionnés sont : Date de lancement de l’appel d’offre et Date échéance de remise des offres. |
| Fournisseur |  |  |  |
| Date de lancement de l’appel d’offre |  |  |  |
| Date échéance de remise des offres |  |  |  |
| Date de reports de l’appel d’offres |  |  |  |
| Date ouverture des plis |  |  |  |
| Date du report du dépouillement |  |  |  |
| Date adjudication |  |  |  |
| Date signature du marché |  |  |  |
| Date d’ordre de service |  |  |  |
| Date de lancement des travaux |  |  |  |
| Date d’achèvement des travaux |  |  |  |
| Date de recollement |  |  |  |
| Date de résiliation |  |  |  |
| GEN0015, GEN0016 |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Imprimer | GEN0011 |
| Importer | GEN0018 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Enregistrer | GEN0001 pour la ligne sélectionnée |
| Supprimer | GEN0010 pour la ligne sélectionnée |
Créé avec HelpNDoc Personal Edition: [Qu'est-ce qu'un outil de création d'aide ?](https://www.helpauthoringsoftware.com/fr)

#### TRANSITIONS <a id="page-prj-transitions"></a>
_Page : `PRJ_TRANSITIONS.html` — Module Projet › CONFIGURATION_

[image : NouvelElement349.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Ordre de tri |  | X | Ordre de tri d’une transition |
| DE |  | X | Liste déroulante de [Statut](#page-statutsdeslots) de départ |
| A |  | X | Liste déroulante de [Statut](#page-statutsdeslots) d’arriver |
| Liste des rôles |  |  | Dans le masque « Transition » les cases à cocher représentent les [rôles](#page-roles) des acteurs qui peuvent modifier le statut d’un « [Lot](#page-lots) ».<br> Exemple :<br>- Les rôles des acteurs qui ont la main de modifier la transition du statut de départ « Estimation » vers le statut d’arrivé « Appel d’offre » d’un « [Lot](#page-lots) » sont :<br>- Administrateur<br>- Assistante technique<br>- Directeur technique |
|  |  |  |  |
|  |  |  |  |
|  |  |  |  |
|  |  |  |  |
|  |  |  |  |
|  |  |  |  |
|  |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Éditer | GEN0009 |
| Ajouter | GEN0008 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Supprimer | GEN0010 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichier attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0017 |
| Champs spécifiques | GEN0007 |
Créé avec HelpNDoc Personal Edition: [Créer des documents d'aide HTML facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

#### TYPES LOT <a id="page-typeslot"></a>
_Page : `TYPESLOT.html` — Module Projet › CONFIGURATION_

[image : NouvelElement347.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Code | X | X |  |
| Description |  | X |  |
| GEN0015, GEN0016 |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Éditer | GEN0009 |
| Ajouter | GEN0008 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Supprimer | GEN0010 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichier attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0017 |
| Champs spécifiques | GEN0007 |
Créé avec HelpNDoc Personal Edition: [Créer des documents d'aide PDF facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

#### STATUTS DES RESERVES <a id="page-statutsdesreserves"></a>
_Page : `STATUTSDESRESERVES.html` — Module Projet › CONFIGURATION_

[image : NouvelElement346.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| O.D.T |  | X |  |
| Code | X | X |  |
| Description |  | X |  |
| Est fermé |  |  |  |
| Couleur |  |  | Liste déroulante de couleurs<br>- Couleurs personnalisé<br>- Couleurs web<br>- Couleur système |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Enregistrer | GEN0001 pour la ligne sélectionnée |
| Supprimer | GEN0010 pour la ligne sélectionnée |
Créé avec HelpNDoc Personal Edition: [Générer facilement des livres électroniques Kindle](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creer-ebooks-pour-kindle-amazon)

#### PLAGES DES COULEURS <a id="page-plagesdescouleurs"></a>
_Page : `PLAGESDESCOULEURS.html` — Module Projet › CONFIGURATION_

[image : NouvelElement437.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| O.D.T |  | X |  |
| Description |  | X |  |
| Début |  | X |  |
| Fin |  |  |  |
| Couleur |  |  | Liste déroulante de couleurs<br>- Couleurs personnalisé<br>- Couleurs web<br>- Couleur système |
Remarque: Les couleurs choisies seront appliquées dans l'impression de la fiche des [réunions de chantier](#page-reunionsdechantier)

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Enregistrer | GEN0001 pour la ligne sélectionnée |
| Supprimer | GEN0010 pour la ligne sélectionnée |
Créé avec HelpNDoc Personal Edition: [Création d'aide CHM, PDF, DOC et HTML d'une même source](https://www.helpndoc.com/fr/outils-creation-aide)

#### MODELES DE SIMULATION <a id="page-modelesdesimulation"></a>
_Page : `MODELESDESIMULATION.html` — Module Projet › CONFIGURATION_

[image : NouvelElement345.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Code | X | X |  |
| Description |  | X |  |
| Taux d’avancement du temps |  |  |  |
| Taux d’avancement des travaux |  |  |  |
| Taux des paiements |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Éditer | GEN0009 |
| Ajouter | GEN0008 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Supprimer | GEN0010 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichier attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0017 |
| Champs spécifiques | GEN0007 |
Créé avec HelpNDoc Personal Edition: [Environnement de création d'aide complet](https://www.helpndoc.com/fr/outils-creation-aide)

#### TYPES RECEPTION <a id="page-typesreception"></a>
_Page : `TYPESRECEPTION.html` — Module Projet › CONFIGURATION_

[image : NouvelElement344.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| O.D.T |  | X |  |
| Code |  | X |  |
| Description |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Enregistrer | GEN0001 pour la ligne sélectionnée |
| Supprimer | GEN0010 pour la ligne sélectionnée |
Créé avec HelpNDoc Personal Edition: [Produire des aides en ligne pour les applications Qt](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creez-des-fichiers-daide-pour-le-framework-qt-help)

#### NATURES RECEPTION <a id="page-naturesreception"></a>
_Page : `NATURESRECEPTION.html` — Module Projet › CONFIGURATION_

[image : NouvelElement343.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| ODT |  | X |  |
| Code |  | X |  |
| Description |  |  |  |
| Est une réception définitive |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Enregistrer | GEN0001 pour la ligne sélectionnée |
| Supprimer | GEN0010 pour la ligne sélectionnée |
Créé avec HelpNDoc Personal Edition: [Créer des aides HTML, DOC, PDF et des manuels depuis une même source](https://www.helpndoc.com/fr/outils-creation-aide)

#### FOURNISSEURS <a id="page-fournisseurs-prj"></a>
_Page : `FOURNISSEURS_PRJ.html` — Module Projet › CONFIGURATION_

[image : NouvelElement368.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Code | X | X | Code auto généré en lecture seule |
| Afficher comme |  | X | En lecture seule |
| Forme juridique |  | X | Liste déroulante des formes juridique |
| Personnel |  |  | Liste déroulante de [personnels](#page-personnels) |
| Nature |  |  | Liste déroulante de nature |
| Général (PH) |  |  |  |
| Titre |  | X | Liste déroulante de titres |
| Sexe |  | X | Liste déroulante de sexe |
| Nom |  | X |  |
| Prénom |  | X |  |
| Pays de résidence |  |  | Liste déroulante des pays |
| Mobile |  | X |  |
| E-mail |  | X |  |
| Date de naissance |  |  |  |
| Lieu |  |  |  |
| Situation familiale |  |  | Liste déroulante des situations familiale |
| Enfant |  |  |  |
| Pièce d’identité |  |  | Liste déroulante de type de pièce d’identité |
| N° |  |  |  |
| Délivrée le |  |  |  |
| Lieu |  |  |  |
| Valide jusqu’au |  |  |  |
| Lieu |  |  |  |
| Fils de |  |  |  |
| Et de |  |  |  |
| Téléphone |  |  |  |
| Adresse |  |  |  |
| Pays |  |  | Liste déroulante des pays |
| Code postal |  |  |  |
| Ville |  |  | Liste déroulante de villes |
| Nationalité |  |  | Liste déroulante de nationalités |
| Résidence |  |  | Liste déroulante de résidences |
| Profession |  |  | Liste déroulante de professions |
| Salaire |  |  | Liste déroulante des fourchettes salariales |
| Employeur |  |  |  |
| Ancienneté |  |  |  |
| Régime matrimonial |  |  | Liste déroulante de régimes matrimoniaux |
| Général (PM, SARL, SURAL, SA, OG, ONG) |  |  |  |
| Raison sociale |  | X |  |
| M.F |  |  |  |
| R.C |  |  |  |
| C.D |  |  |  |
| Date de création |  |  |  |
| Lieu |  |  |  |
| Activité |  |  |  |
| Capital |  |  |  |
| Représentant légal |  |  | Sélecteur de tiers |
| Qualité |  |  |  |
| Pays de résidence |  |  | Liste déroulante de pays de résidences |
| Mobile |  | X |  |
| Téléphone 1 |  |  |  |
| Email |  | X |  |
| Adresse |  |  |  |
| Pays |  |  | Liste déroulante de pays |
| Code postal |  |  |  |
| Ville |  |  | Liste déroulante de villes |
| Nationalité |  |  | Liste déroulante de nationalités |
| Résidence |  |  | Liste déroulante de résidences |
| Autres |  |  |  |
| Assujettissement |  |  | Liste déroulante d’assujettissements |
| Banque |  |  | Liste déroulante de banques |
| Agence |  |  |  |
| RIB |  |  |  |
| IBAN |  |  |  |
| Code comptable |  |  |  |
| Compte collectif |  |  |  |
| Téléphone 2 |  |  |  |
| Téléphone 3 |  |  |  |
| Fax |  |  |  |
| Site web |  |  |  |
| Contacts |  |  |  |
| Liste de contacts |  |  | La liste visualise les contacts en indiquant :<br>- Le contact<br>- Le type de relation<br> Sur cette liste on peut effectuer les opérations suivantes :<br>- Ajouter un contact<br>- Éditer un contact<br>- Supprimer un contact |
| Notes |  |  |  |
| Champ de saisi des notes |  |  |  |
| Remarque : La combinaison des champs « Description » + « mobile » ou « Description » + « E-mail » doivent êtres uniques. |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichier attaché | GEN0004 |
| Note | GEN0005 |
| Annuler et fermer | GEN0017 |
| Champs spécifiques | GEN0007 |
Créé avec HelpNDoc Personal Edition: [Générateur complet de livres électroniques ePub](https://www.helpndoc.com/fr/creer-des-livres-numeriques-epub)

#### CONTACTS <a id="page-contacts-prj"></a>
_Page : `CONTACTS_PRJ.html` — Module Projet › CONFIGURATION_

[image : NouvelElement367.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Code | X | X | Code auto généré en lecture seule |
| Afficher comme |  | X | En lecture seule |
| Forme juridique |  | X | Liste déroulante des formes juridique |
| Personnel |  |  | Liste déroulante de [personnels](#page-personnels) |
| Nature |  |  | Liste déroulante de nature |
| Général (PH) |  |  |  |
| Titre |  | X | Liste déroulante de titres |
| Sexe |  | X | Liste déroulante de sexe |
| Nom |  | X |  |
| Prénom |  | X |  |
| Pays de résidence |  |  | Liste déroulante des pays |
| Mobile |  | X |  |
| E-mail |  | X |  |
| Date de naissance |  |  |  |
| Lieu |  |  |  |
| Situation familiale |  |  | Liste déroulante des situations familiale |
| Enfant |  |  |  |
| Pièce d’identité |  |  | Liste déroulante de type de pièce d’identité |
| N° |  |  |  |
| Délivrée le |  |  |  |
| Lieu |  |  |  |
| Valide jusqu’au |  |  |  |
| Lieu |  |  |  |
| Fils de |  |  |  |
| Et de |  |  |  |
| Téléphone |  |  |  |
| Adresse |  |  |  |
| Pays |  |  | Liste déroulante des pays |
| Code postal |  |  |  |
| Ville |  |  | Liste déroulante de villes |
| Nationalité |  |  | Liste déroulante de nationalités |
| Résidence |  |  | Liste déroulante de résidences |
| Profession |  |  | Liste déroulante de professions |
| Salaire |  |  | Liste déroulante des fourchettes salariales |
| Employeur |  |  |  |
| Ancienneté |  |  |  |
| Régime matrimonial |  |  | Liste déroulante de régimes matrimoniaux |
| Général (PM, SARL, SURAL, SA, OG, ONG) |  |  |  |
| Raison sociale |  | X |  |
| M.F |  |  |  |
| R.C |  |  |  |
| C.D |  |  |  |
| Date de création |  |  |  |
| Lieu |  |  |  |
| Activité |  |  |  |
| Capital |  |  |  |
| Représentant légal |  |  | Sélecteur de tiers |
| Qualité |  |  |  |
| Pays de résidence |  |  | Liste déroulante de pays de résidences |
| Mobile |  | X |  |
| Téléphone 1 |  |  |  |
| Email |  | X |  |
| Adresse |  |  |  |
| Pays |  |  | Liste déroulante de pays |
| Code postal |  |  |  |
| Ville |  |  | Liste déroulante de villes |
| Nationalité |  |  | Liste déroulante de nationalités |
| Résidence |  |  | Liste déroulante de résidences |
| Autres |  |  |  |
| Assujettissement |  |  | Liste déroulante d’assujettissements |
| Banque |  |  | Liste déroulante de banques |
| Agence |  |  |  |
| RIB |  |  |  |
| IBAN |  |  |  |
| Code comptable |  |  |  |
| Compte collectif |  |  |  |
| Téléphone 2 |  |  |  |
| Téléphone 3 |  |  |  |
| Fax |  |  |  |
| Site web |  |  |  |
| Contacts |  |  |  |
| Liste de contacts |  |  | La liste visualise les contacts en indiquant :<br>- Le contact<br>- Le type de relation<br> Sur cette liste on peut effectuer les opérations suivantes :<br>- Ajouter un contact<br>- Éditer un contact<br>- Supprimer un contact |
| Notes |  |  |  |
| Champ de saisi des notes |  |  |  |
| Remarque : La combinaison des champs « Description » + « mobile » ou « Description » + « E-mail » doivent êtres uniques. |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichier attaché | GEN0004 |
| Note | GEN0005 |
| Annuler et fermer | GEN0017 |
| Champs spécifiques | GEN0007 |
Créé avec HelpNDoc Personal Edition: [Créer des livres électroniques facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

## Module Achat <a id="page-moduleachat"></a>
_Page : `ModuleAchat.html`_

Créé avec HelpNDoc Personal Edition: [Produire des livres EPub gratuitement](https://www.helpndoc.com/fr/creer-des-livres-numeriques-epub)

### CONSULTATIONS <a id="page-consultations"></a>
_Page : `CONSULTATIONS.html` — Module Achat_

[image : NouvelElement849.bmp]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Code | X | X | Auto-généré en lecture seule |
| Description |  | X |  |
| Projet |  | X | Liste déroulante des projets |
| Catégorie |  | X | Sélecteur de catégorie |
| Type |  | X | Liste déroulante des types |
| Statut |  | X | Sélecteur de statuts |
| Fournisseur |  |  | En lecture seule |
| Général |  |  |  |
| Général: Estimation forfaitaire |  |  |  |
| Est. forfaitaire HT |  |  | En lecture seule, |
| Est. forfaitaire TTC |  |  | En lecture seule, |
| Général: Estimation confidentielle |  |  |  |
| Est. confidentielle HT |  |  | En lecture seule,<br> Σ des montants des lignes HT du bordereau estimatif |
| Est. confidentielle TTC |  |  | En lecture seule,<br> Σ des montants des lignes TTC du bordereau estimatif |
| Général: Marché |  |  |  |
| Marché HT |  |  | En lecture seule,<br> Σ des montants des lignes TTC du bordereau |
| Marché TTC |  |  | En lecture seule,<br> Σ des montants des lignes TTC du bordereau estimatif |
| Général: Modification |  |  |  |
| Modifications HT |  |  | En lecture seule,<br> Σ des montants HT des lignes du bordereau définitif - Σ des montants HT des lignes de devis du fournisseur sélectionne |
| Modifications TTC |  |  | En lecture seule,<br> Σ des montants HT des lignes du bordereau définitif - Σ des montants HT des lignes de devis du fournisseur sélectionne |
| Général: Marché ajusté |  |  |  |
| Marché ajusté HT |  |  | En lecture seule,<br> Marché HT + Modification HT |
| Marché ajusté TTC |  |  | En lecture seule,<br> Marché TTC + Modification TTC |
| Général: Bons de commande |  |  |  |
| Bons de commande HT |  |  | En lecture seule,<br> Σ des montant HT des bons de commande |
| Bons de commande TTC |  |  | En lecture seule,<br> Σ des montant TTC des bons de commande |
| Général: Bons de livraison |  |  |  |
| Bons de livraison HT |  |  | En lecture seule,<br> Σ des montant HT des bons de livraison |
| Bons de livraison TTC |  |  | En lecture seule,<br> Σ des montant TTC des bons de livraison |
| Général: Factures |  |  |  |
| Factures HT |  |  | En lecture seule,<br> Σ des montant HT des factures |
| Factures TTC |  |  | En lecture seule,<br> Σ des montant TTC des factures |
| Général: Avoirs |  |  |  |
| Avoirs HT |  |  | En lecture seule,<br> Σ des montant HT des avoirs |
| Avoirs TTC |  |  | En lecture seule,<br> Σ des montant TTC des avoirs |
| Bon de commande |  |  |  |
| Liste des bons de commande |  |  | La liste visualise les bons de commandes en indiquant :<br>- La date<br>- Le code<br>- Le fournisseur<br>- Le montant HT…<br>Aussi, dans cette liste on peut effectuer les opérations suivantes :<br>- Ajouter un bon de commande<br>- Éditer un bon de commande<br>- Supprimer un bon de commande<br>- Générer un bon de livraison<br>- Générer une facture<br> Remarque:<br>- Si l'utilisateur connecté a le droit d'ajouter des bons de livraison, le système affiche l'action "Générer bon de livraison"<br>- Si l'utilisateur connecté a le droit d'ajouter des factures, le système affiche l'action "Générer facture" |
| Bons de livraison |  |  |  |
| Liste des bons de livraison |  |  | La liste visualise les bons de livraison en indiquant :<br>- La date<br>- Le code<br>- Le fournisseur<br>- Le montant HT…<br>Aussi, dans cette liste on peut effectuer les opérations suivantes :<br>- Ajouter un bon de livraison<br>- Éditer un bon de livraison<br>- Supprimer un bon de livraison<br>- Générer une facture<br> Remarque:<br>- Si l'utilisateur connecté a le droit d'ajouter des factures, le système affiche l'action "Générer facture" |
| Factures |  |  |  |
| Liste des factures |  |  | La liste visualise les factures en indiquant :<br>- La date<br>- Le code<br>- Le fournisseur<br>- Le montant HT…<br>Aussi, dans cette liste on peut effectuer les opérations suivantes :<br>- Ajouter une facture<br>- Éditer une facture<br>- Supprimer une facture<br>- Générer un avoir<br> Remarque:<br>- Si l'utilisateur connecté a le droit d'ajouter des avoirs, le système affiche l'action "Générer avoir" |
| Avoirs |  |  |  |
| Liste des avoirs |  |  | La liste visualise les avoirs en indiquant :<br>- La date<br>- Le code<br>- Le fournisseur<br>- Le montant HT…<br>Aussi, dans cette liste on peut effectuer les opérations suivantes :<br>- Ajouter un avoir<br>- Éditer un avoir<br>- Supprimer avoir |
| Dates |  |  |  |
| Date de lancement de l'appel d'offre |  |  |  |
| Date de retrait du dossier de consultation |  |  |  |
| Date échéance de remise des offres |  |  |  |
| Date de report de l'appel d'offres |  |  |  |
| Date ouverture des plis |  |  |  |
| Date du report de dépouillement |  |  |  |
| Date adjudication |  |  |  |
| Date signature du marché |  |  |  |
| Date d'ordre de service |  |  |  |
| Date de lancement des travaux |  |  |  |
| Date d’achèvement des travaux |  |  |  |
| Date de recollement |  |  |  |
| Résiliation |  |  |  |
| Date |  |  |  |
| Cause |  |  |  |
| Autres |  |  |  |
| Notes |  |  |  |
| Description détaillée |  |  |  |
| TVA |  |  |  |
| Texte lettre |  |  |  |
|  |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Copier | GEN0019 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Imprimer | GEN0011 |
| Bordereaux estimatif | Gestion des bordereaux estimatifs |
| Appel d'offre | Gestion des appels d'offres |
| Offre des entreprises | Gestion des offres entreprises |
| Bordereau définitif | Gestion des bordereaux définitifs |
Créé avec HelpNDoc Personal Edition: [Générer des livres électroniques EPub facilement](https://www.helpndoc.com/fr/creer-des-livres-numeriques-epub)

#### BORDEREAU ESTIMATIF <a id="page-bordereauestimatif-c"></a>
_Page : `BORDEREAUESTIMATIF_C.html` — Module Achat › CONSULTATIONS_

[image : NouvelElement851.bmp]

### Liste

| LIBELLE | V | OBSERVATIONS |
|---|---|---|
| Catégorie |  |  |
| O.D.T | X |  |
| Code | X |  |
| Crée le |  |  |
| Crée par |  |  |
| Description | X |  |
| Unité | X |  |
| Quantité | X |  |
| P.U HT | X | (PU HT après remise * Pourcentage FODEC) + (PU HT après remise) |
| FODEC | X | Pourcentage FODEC |
| TVA | X | Pourcentage TVA |
| P.U TTC | X | PU HT (avec FODEC) * Pourcentage TVA + (PU HT (avec FODEC)) |
| TOTAL HT | X | P.U HT * Quantité |
| TOTAL TTC | X | P.U TTC * Quantité |
| Modifié le |  |  |
| Modifié par |  |  |
| Notes | X |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter groupe | Ajouter un nœud racine dans la hiérarchie du bordereau |
| Ajouter ligne | Ajouter une ligne dans le bordereau |
| Supprimer | Supprimer une ligne du bordereau |
| Régénérer les codes | Le système réorganise la codification des lignes du bordereau |
| Importer/Exporter | Importer: GEN0011<br> Exporter:GEN0012 |
| Développer | Le système afficher toute lahiérarchie du bordereau |
| Réduire | Le système réduit toute la hiérarchie du bordereau |
Créé avec HelpNDoc Personal Edition: [Créer des fichiers d'aide pour la plateforme Qt Help](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creez-des-fichiers-daide-pour-le-framework-qt-help)

#### APPEL D'OFFRE <a id="page-appeldoffre-c"></a>
_Page : `APPELDOFFRE_C.html` — Module Achat › CONSULTATIONS_

[image : NouvelElement853.bmp]
Le bouton « Appel d’offre » permet de gérer les fournisseurs présélectionnés pour le lot, et les détails qui concernent leur dépôt d’offre :

- Le fournisseur

- La date d’envoi de la lettre de consultation
- La date du retrait dossier de consultation
- La date dépôt de l’offre
- La note
On peut aussi effectuer les opérations suivantes:

- Ajouter un fournisseur
- Supprimer un fournisseur
Créé avec HelpNDoc Personal Edition: [Générateur de documentation et EPub facile](https://www.helpndoc.com/fr)

#### OFFRES DES ENTREPRISES <a id="page-offresdesentreprises1"></a>
_Page : `OFFRESDESENTREPRISES1.html` — Module Achat › CONSULTATIONS_

[image : NouvelElement854.bmp]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| O.D.T |  |  | En lecture seule |
| Code |  | X |  |
| Fournisseur |  |  | En lecture seule,<br> Dans ce champ, le système affiche le fournisseur sélectionné |
| Présélection |  |  |  |
| Liste des présélections |  |  | [image : NouvelElement835.jpg]<br>La liste visualise les données des fournisseurs qui ont répondu à la consultation dans les délais en indiquant:<br>- Le fournisseur<br>- La date de la lettre de consultation<br>- La date du retrait de consultation<br>- La date dépôt de l'offre...<br>Aussi, dans cette liste on peut effectuer les opérations suivantes<br>- Ajouter un fournisseur<br>- Ajouter [devis](#page-devis) pour un fournisseur<br>- Supprimer un fournisseur<br>- Écarter un fournisseur |
| Offres |  |  |  |
| Liste des offres |  |  | La liste visualise les offres fournisseurs en indiquant:<br>- La date<br>- Le code<br>- Le fournisseur<br>- Le montant HT<br>- Le montant TVA<br>- Le montant TTC...<br>Aussi, dans cette liste on peut effectuer les opérations suivantes:<br>- Supprimer devis<br>- Éditer devis<br>- Sélectionner fournisseur<br>- Dé-sélectionner fournisseur |
| Conclusion |  |  |  |
|  |  |  |  |
| Texte lettre |  |  |  |
|  |  |  |  |
Créé avec HelpNDoc Personal Edition: [Générateur de documentations PDF gratuit](https://www.helpndoc.com/fr)

#### BORDEREAU DEFINITIF <a id="page-bordereaudefintif-c"></a>
_Page : `BORDEREAUDEFINTIF_c.html` — Module Achat › CONSULTATIONS_

[image : NouvelElement855.bmp]
On se basant sur l'offre du fournisseur sélectionné, le système remplit le bordereau des prix définitifs
Créé avec HelpNDoc Personal Edition: [Créer des fichiers d'aide pour la plateforme Qt Help](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creez-des-fichiers-daide-pour-le-framework-qt-help)

### DEVIS <a id="page-devis"></a>
_Page : `DEVIS.html` — Module Achat_

[image : NouvelElement768.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Totaux |  |  |  |
| Code | X | X | Auto-généré en lecture seule |
| Date |  |  |  |
| Échéance |  |  |  |
| Entité juridique |  | X | Liste déroulante des[ENTITESJURIDIQUE.html](#page-entitesjuridique)[entités juridique](#page-entitesjuridique) |
| Module |  | X | Liste déroulante de modules |
| Fournisseurs |  | X | Sélecteur des [fournisseurs](#page-fournisseurs-adm) |
| Personne à contacter |  |  |  |
| Type |  |  | Liste déroulante des [types](#page-typeslot) |
| Réf. Interne (Référence interne) |  |  |  |
| Référence bureau d'ordre |  |  |  |
| Catégorie |  |  |  |
| Projet |  |  |  |
| Lot |  |  |  |
| Dossier de groupement |  |  | Sélecteur des [dossiers de groupement](#page-dossiersdegroupement) |
| Validation par |  |  | Sélecteur de [personnel](#page-personnels) |
| Ce devis a été sélectionné |  |  |  |
| Note |  |  |  |
| Note 2 |  |  |  |
| Total HT |  |  |  |
| Total TVA |  |  |  |
| Total TTC |  |  |  |
| Montant régul |  |  |  |
| Timbre fiscal |  |  |  |
| Taux RS |  |  |  |
| Montant RS |  |  |  |
| Net à payer |  |  |  |
| Lignes |  |  |  |
| Liste des lignes |  |  | La liste visualise les lignes d’un devis :<br>- O.D.T<br>- Code<br>- Description<br>- Quantité<br>- Prix unitaire hors taxe<br>- Remise<br>- FODEC..<br>- TVA<br>- Prix unitaire TTC : (PU HT (avec FODEC) * Pourcentage TVA) + PU HT (avec FODEC)<br>Remarque: PU HT (avec FODEC) = (PU HT après remise * Pourcentage FODEC) + (PU HT après remise)<br>Encore, à partir de cette liste, on peut effectuer les opérations suivantes :<br>- Ajouter<br>- Ajouter à partir d’un lot<br>- Supprimer<br>- Tout sélectionner<br>- Exporter<br>- Importer |
| Ventilations |  |  |  |
| Liste de ventilations des Projets |  |  | La liste visualise la ventilation par projet en indiquant:<br>- Le taux de ventilation<br>- Le projet<br>==> Si on clic sur le bouton "Appliquer la ventilation par défaut", le système répartie la ventilation selon le paramétrage par défaut effectué dans l'écran " [Projet](#page-projets) ".<br>==>Si on clic sur le bouton "Projet", le système affiche la liste des [projets](#page-projets)<br>Remarque 1: Si on choisit un projet à partir de la liste déroulante (Sous l'onglet "Totaux"), le système applique une ventilation de 100% sur ce dernier.<br> Remarque 2: La somme des taux de ventilation doit être égale à 100% |
| Liste de ventilations des catégories |  |  | La liste visualise la ventilation pour chaque [catégorie](#page-categoriedesdepenses) choisi en indiquant :<br>- Le taux<br>- La catégorie<br>Si la facture à des lignes<br>- Le système rend la liste en lecture seule et il applique la ventilation en fonction du montant total de document la somme des montants totaux TTC des lignes par catégorie<br> Sinon<br>- Le système garde la liste vide, et il donne la possibilité d'ajouter et d'appliquer des taux de ventilation manuellement.<br>Remarque: La somme des taux de ventilation doit être égale à 100%. |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009<br> Remarque: Si l'utilisateur connecté:<br>- N'a pas le droit d'ajout/modification des devis (Module achat)<br>- A le droit d'ajout/modification des devis à partir d'un lot Multi-fournisseurs<br>==> Dans le module achat --> Devis: Le système ouvre les devis en provenance de module projet avec le droit de modification et le reste en lecture seule. |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Audit | GEN0020 |
| Générer à partir de | GEN0022 |
Créé avec HelpNDoc Personal Edition: [Générateur complet de livres électroniques ePub](https://www.helpndoc.com/fr/creer-des-livres-numeriques-epub)

### BONS DE COMMANDE <a id="page-bonsdecommande"></a>
_Page : `BONSDECOMMANDE.html` — Module Achat_

[image : NouvelElement769.png]

### Liste

| LIBELLE | V | OBSERVATIONS |
|---|---|---|
| A clôturer |  | Si le montant HT de bon de commande >= Montant HT des factures, le système affiche une alerte si le statut de bon de commande n'est pas clôturé. |
| Avance |  | Σ des documents à régler des avances sur bon de commande |
| Avance amortie |  | Σ des montants des avances à amortir |
| Avance ratio |  | Pourcentage d'avance |
| Avance réglé |  | Σ des montants des avances réglées |
| Avance reste |  | Avance cumulé - Avance réglé |
| Avance reste à amortir |  | Avance - Avance amortie |
| Bons de livraison |  |  |
| Catégories |  |  |
| Code |  |  |
| Consultation |  |  |
| Crée le |  |  |
| Crée par |  |  |
| Date |  |  |
| Date alerte |  |  |
| Dépouillement |  |  |
| Description |  |  |
| Devis |  |  |
| Dossier de group. |  |  |
| Entité fonctionnelle |  |  |
| Entité fonctionnelle bon de commande |  |  |
| Entité juridique |  |  |
| Factures |  |  |
| Fournisseurs |  |  |
| Lot |  |  |
| Modifié le |  |  |
| Modifié par |  |  |
| Montant Facturé HT |  | Σ des montants HT des factures (à partir du bon de commande en question) |
| Montant facturé Net à payer |  | Σ des montants Net à payer des factures (à partir du bon de commande en question) |
| Montant facturé TTC |  | Σ des montants TTC des factures (à partir du bon de commande en question) |
| Montant HT |  | Σ des montants HT des lignes du bon de commande |
| Montant livré HT |  | Σ des montants HT des bons de livraison (à partir du bon de commande en question) |
| Montant livré Net à payer |  | Σ des montants Net à payer des bons de livraison (à partir du bon de commande en question) |
| Montant livré TTC |  | Σ des montants TTC des bons de livraison (à partir du bon de commande en question) |
| Montant réglé |  | Σ des montants des règlements des avances sur bon de commande + Σ des montants des règlements des factures |
| Montant TTC |  | Σ des montants TTC des lignes du bon de commande |
| Montant TVA |  | Σ des montants TVA des lignes du bon de commande |
| Net à payer |  | Montant TTC + Montant timbre fiscal + Montant régule |
| Notes |  |  |
| Notes 2 |  |  |
| Pourcentage effectif d'avance |  |  |
| Projet |  |  |
| Réf. Interne |  |  |
| Référence bureau d'ordre |  |  |
| Reste à livrer HT |  | Montant commandé HT - Σ Montant des bons de livraison HT |
| Reste à livrer Net à payer |  | Montant commandé Net à payer - Σ Montant des bons de livraison Net à payer |
| Reste à livrer TTC |  | Montant commandé TTC - Σ Montant des bons de livraison TTC |
| Reste à payer sur facturé |  | Montant facturé - Montant réglé<br> Remarque: Si on trouve un montant négative, c'est dû aux montants d'avances sur bon de commande réglé. |
| Reste à payer total |  | Montant Commandé - Montant réglé<br>Remarque: Si on trouve un trouve un montant reste à payer même si on a payer lé totalité des factures, c'est dû aux montants régul. et aux montants timbre fiscale. |
| Statut |  |  |
| Type |  |  |

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Totaux |  |  |  |
| Code | X | X | Auto-généré en lecture seule |
| Date |  |  |  |
| Échéance |  |  |  |
| Entité juridique |  | X | Liste déroulante des[ENTITESJURIDIQUE.html](#page-entitesjuridique)[entités juridique](#page-entitesjuridique) |
| Module |  | X | Liste déroulante de modules |
| Fournisseurs |  | X | Sélecteur des [fournisseurs](#page-fournisseurs-adm) |
| Personne à contacter |  |  |  |
| Type |  | X | Liste déroulante des [types](#page-typeslot) |
| Réf. Interne (Référence interne) |  |  |  |
| Déclencheur |  |  | Par défaut, ce champ prend le personnel affecté à l'utilisateur connecté |
| Statut |  |  | [Statuts](#page-statuts-buy) : sélecteur de statut, lorsque l’utilisateur clique sur le bouton le système cherche les transitions possible en respectant le cycle de vie et les habilitation.<br> Remarque : Une fois le statut destination choisi, avant l'affectation le système vérifie les champs obligatoires, les documents à joindre et les champs obligatoires du fournisseur configurés dans la transition. |
| Projet |  |  |  |
| Lot |  |  |  |
| Dossier de groupement |  |  | Sélecteur des [dossiers de groupement](#page-dossiersdegroupement) |
| Date de livraison souhaitée |  |  |  |
| Note |  |  |  |
| Note 2 |  |  |  |
| Total HT |  |  | Si la facture possède des lignes<br> ce champ sera en lecture seule = ∑ des total HT des lignes<br> Si non<br> il sera saisissable avec un calcul Total TTC = Valeur + Total TVA |
| Total TVA |  |  | Si la facture possède des lignes<br> ce champ sera en lecture = ∑ des total TVA des lignes<br> Si non<br> il sera saisissable avec un calcul Total TTC = Valeur + Total HT |
| Total TTC |  |  | Si la facture possède des lignes<br> ce champ sera en lecture seule = ∑ des total TTC des lignes<br> Si non<br> il sera saisissable avec un calcul Total HT = Valeur - Total TVA |
| Montant régule |  |  | Montant de régulation : Il existe des cas ou le calcul de la facture fournisseur est incompréhensible, il faut utiliser ce montant pour coller le Net à payer à la valeur exacte |
| Timbre fiscal |  |  |  |
| Net à payer |  |  | Montant TTC + Montant timbre fiscal + Montant régule |
| Avance générer |  |  | Taux d'avance<br> Montant d'avance = Total TTC * Taux d'avance |
| Avance effective |  |  | Montant avance / Montant TTC de bon de commande *100 |
| Avance cumulée |  |  | Σ des montants des avances |
| Amortie |  |  | Σ des montants des avances amorties dans les factures |
| Reste |  |  | Avance cumulée - Amortie |
| Lignes |  |  |  |
| Liste des lignes |  |  | La liste visualise les lignes d’un devis :<br>- O.D.T<br>- Code<br>- Description<br>- Quantité<br>- Prix unitaire hors taxe<br>- Remise<br>- FODEC..<br>- TVA<br>- Prix unitaire TTC : (PU HT (avec FODEC) * Pourcentage TVA) + PU HT (avec FODEC)<br> Remarque: PU HT (avec FODEC) = (PU HT après remise * Pourcentage FODEC) + (PU HT après remise)<br>Encore, à partie de cette liste, on peut effectuer les opérations suivantes :<br>- Ajouter<br>- Ajouter à partir d’un lot<br>- Supprimer<br>- Tout sélectionner<br>- Exporter<br>- Importer |
| Ventilations |  |  |  |
| Liste de ventilations des Projets |  |  | La liste visualise la ventilation par projet en indiquant:<br>- Le taux de ventilation<br>- Le projet<br>==> Si on clic sur le bouton "Appliquer la ventilation par défaut", le système répartie la ventilation selon le paramétrage par défaut effectué dans l'écran " [Projet](#page-projets) ".<br>==>Si on clic sur le bouton "Projet", le système affiche la liste des [projets](#page-projets)<br>Remarque 1: Si on choisit un projet à partir de la liste déroulante (Sous l'onglet "Totaux"), le système applique une ventilation de 100% sur ce dernier.<br> Remarque 2: La somme des taux de ventilation doit être égale à 100% |
| Liste de ventilations des catégories |  |  | La liste visualise la ventilation pour chaque [catégorie](#page-categoriedesdepenses) choisi en indiquant :<br>- Le taux<br>- La catégorie<br>Si la facture à des lignes<br>- Le système rend la liste en lecture seule et il applique la ventilation en fonction du montant total de document la somme des montants totaux TTC des lignes par catégorie<br> Sinon<br>- Le système garde la liste vide, et il donne la possibilité d'ajouter et d'appliquer des taux de ventilation manuellement.<br>Remarque: La somme des taux de ventilation doit être égale à 100%. |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009<br> Remarque: Si l'utilisateur connecté:<br>- N'a pas le droit d'ajout/modification des bons de commande (Module achat)<br>- A le droit d'ajout/modification des bons de commande à partir d'un lot Multi-fournisseurs<br>==> Dans le module achat --> Bons de commande: Le système ouvre les bons de commande en provenance de module projet avec le droit de modification et le reste en lecture seule. |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Audit | GEN0020 |
| Générer à partir de | GEN0022 |
| Avance | Lorsqu'on clic sur le bouton "Avance" le système affiche la liste des avances en indiquant:<br>- Le code<br>- Le statut<br>- La date<br>- Le net à payer<br>- Le montant régler<br>- Le montant reste<br>- La note<br>Dans cette liste, on peut effectuer les opérations suivantes:<br>- Ajouter une avance<br>- Éditer une avance<br>- Supprimer une avance<br>Remarque: Lorsqu'on ajoute une avance, le système ouvre la fiche des[documents à régler](#page-documentsaregler) et il affecte le montant d'avance automatiquement selon le taux prédéfini dans le bon de commande.<br> Montant avance = Total TTC * Taux d'avance |
Créé avec HelpNDoc Personal Edition: [Créer des fichiers d'aide pour la plateforme Qt Help](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creez-des-fichiers-daide-pour-le-framework-qt-help)

### BONS DE LIVRAISON <a id="page-bonsdelivraison"></a>
_Page : `BONSDELIVRAISON.html` — Module Achat_

[image : NouvelElement763.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Totaux |  |  |  |
| Code | X | X | Auto-généré en lecture seule |
| Date |  |  |  |
| Échéance |  |  |  |
| Entité juridique |  | X | Liste déroulante des entités juridique |
| Module |  | X | Liste déroulante de modules |
| Fournisseur |  | X | Sélecteur des [fournisseurs](#page-fournisseurs-adm) |
| Personne à contacter |  |  | Sélecteur des personnes à contacter |
| Type |  | X | Sélecteur des [types](#page-typeslot) |
| Réf. Interne (Référence interne) |  |  |  |
| Référence bureau d'ordre |  |  |  |
| Catégories |  |  | Sélecteur des [catégories](#page-categoriedesdepenses)<br> Remarque: La catégorie sélectionnée sera appliquée par défaut pour les lignes de document en cours. |
| Projet |  |  | Liste déroulante des [projets](#page-projets) |
| Lot |  |  |  |
| Dossier de groupement |  |  | Sélecteur des dossiers de groupement |
| Note |  |  |  |
| Note 2 |  |  |  |
| Total HT |  |  |  |
| Total TVA |  |  |  |
| Total TTC |  |  |  |
| Montant règlement |  |  |  |
| Timbre fiscal |  |  |  |
| Net à payer |  |  | Montant TTC + Montant timbre fiscal + Montant régule |
| Lignes |  |  |  |
| Liste des lignes |  |  | La liste visualise les lignes d’un devis :<br>- O.D.T<br>- Code<br>- Description<br>- Quantité<br>- Prix unitaire hors taxe<br>- Remise<br>- FODEC..<br>- TVA<br>- Prix unitaire TTC : (PU HT (avec FODEC) * Pourcentage TVA) + PU HT (avec FODEC)<br> Remarque: PU HT (avec FODEC) = (PU HT après remise * Pourcentage FODEC) + (PU HT après remise)<br>Encore, à partie de cette liste, on peut effectuer les opérations suivantes :<br>- Ajouter<br>- Ajouter à partir d’un lot<br>- Supprimer<br>- Tout sélectionner<br>- Exporter<br>- Importer |
| Ventilation |  |  |  |
| Liste de ventilations des Projets |  |  | La liste visualise la ventilation par projet en indiquant:<br>- Le taux de ventilation<br>- Le projet<br>==> Si on clic sur le bouton "Appliquer la ventilation par défaut", le système répartie la ventilation selon le paramétrage par défaut effectué dans l'écran " [Projet](#page-projets) ".<br>==>Si on clic sur le bouton "Projet", le système affiche la liste des [projets](#page-projets)<br>Remarque 1: Si on choisit un projet à partir de la liste déroulante (Sous l'onglet "Totaux"), le système applique une ventilation de 100% sur ce dernier.<br> Remarque 2: La somme des taux de ventilation doit être égale à 100% |
| Liste de ventilations des catégories |  |  | La liste visualise la ventilation pour chaque [catégorie](#page-categoriedesdepenses) choisi en indiquant :<br>- Le taux<br>- La catégorie<br>Si la facture à des lignes<br>- Le système rend la liste en lecture seule et il applique la ventilation en fonction du montant total de document la somme des montants totaux TTC des lignes par catégorie<br> Sinon<br>- Le système garde la liste vide, et il donne la possibilité d'ajouter et d'appliquer des taux de ventilation manuellement.<br>Remarque: La somme des taux de ventilation doit être égale à 100%. |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009<br> Remarque: Si l'utilisateur connecté:<br>- N'a pas le droit d'ajout/modification des bons de livraison (Module achat)<br>- A le droit d'ajout/modification des bons de livraison à partir d'un lot Multi-fournisseurs<br>==> Dans le module achat --> Bons de livraison: Le système ouvre les bons de livraison en provenance de module projet avec le droit de modification et le reste en lecture seule. |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Générer facture | Le système génère une facture à partir de bon de livraison<br> Remarque: Pour générer une facture à partir de plusieurs bons de livraison, il faut que:<br>- Les bons de livraison sélectionnés on le même fournisseur<br>- Les bons de livraison sélectionnés sont générés à partir du même bon de commande |
| Copier | GEN0019 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Audit | GEN0020 |
| Générer à partir de | GEN0022 |
Créé avec HelpNDoc Personal Edition: [Créer des fichiers d'aide pour la plateforme Qt Help](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creez-des-fichiers-daide-pour-le-framework-qt-help)

### FACTURES <a id="page-factures"></a>
_Page : `FACTURES.html` — Module Achat_

[image : NouvelElement764.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Totaux |  |  |  |
| Code | X | X | Auto-généré en lecture seule |
| Date |  |  | Date de la facture |
| Échéance |  |  | Échéance de règlement de la facture |
| Entité juridique |  | X | Liste déroulante des [entités juridique](#page-entitesjuridique) |
| Module |  | X | Liste déroulante de modules |
| Fournisseurs |  |  | Sélecteur des [fournisseurs](#page-fournisseurs-adm) |
| Personne à contacter |  |  | Zone saisissable avec proposition des contact du fournisseur sélectionné |
| Réf. Interne |  |  | Référence interne : N° facture du fournisseur |
| Référence bureau d'ordre |  |  |  |
| Catégories |  |  | Sélecteur de [catégorie](#page-categoriedesdepenses) par défaut |
| Déclencheur |  |  | Sélecteur des [personnels](#page-personnels)<br> Par défaut, le système affecte le personnel de l'utilisateur connecté |
| Statut |  | X | [Statuts](#page-statuts-buy) : sélecteur de statut, lorsque l’utilisateur clique sur le bouton le système cherche les transitions possible en respectant le cycle de vie et les habilitation.<br> Remarque : Une fois le statut destination choisi, avant l'affectation le système vérifie les champs obligatoires, les documents à joindre et les champs obligatoires du fournisseur configurés dans la transition. |
| Validité |  |  | Si la durée de vie du statut sélectionné est égale à zéro, le champ validité sera en lecture seule si non la date validité sera égale à date du jour + la durée de vie du statut.<br> Remarque : Si la date validité est renseignée le système génère une alerte d'échéance. |
| Projet |  |  | Liste des [projets](#page-projets) |
| Lot |  |  | Champs en lecture seule, renseigné lors de la génération d'une facture à partir d'un [lot](#page-lots) du module projet. |
| Dossier de groupement |  |  | Sélecteur des dossiers de groupement la liste du sélecteur affiche seulement les dossier de groupement non archivé. |
| Note |  |  | Zone de saisie libre |
| Note 2 |  |  |  |
| Total HT |  |  | Si la facture possède des lignes<br> ce champ sera en lecture seule = ∑ des total HT des lignes<br> Si non<br> il sera saisissable avec un calcul Total TTC = Valeur + Total TVA |
| Total TVA |  |  | Si la facture possède des lignes<br> ce champ sera en lecture = ∑ des total TVA des lignes<br> Si non<br> il sera saisissable avec un calcul Total TTC = Valeur + Total HT |
| Total TTC |  |  | Si la facture possède des lignes<br> ce champ sera en lecture seule = ∑ des total TTC des lignes<br> Si non<br> il sera saisissable avec un calcul Total HT = Valeur - Total TVA |
| Montant régul |  |  | Montant de régulation : Il existe des cas ou le calcul de la facture fournisseur est incompréhensible, il faut utiliser ce montant pour coller le Net à payer à la valeur exacte |
| Timbre fiscal |  |  | Lors de la création de la facture le système affecte la valeur par défaut du timbre fiscal configuré dans module administration |
| Net à payer |  |  | Lorsque l'utilisateur renseigne le Net à payer le système calcule l'impact sur le montant de régulation sans toucher le Total TTC.<br> Montant régul = (Net à payer - Timbre fiscal) - (Total TTC + Remb. Fluctuations + Avance sur décompte) +(Montant amortissement avance + Montant retenue de garantie + Montant compte prorata + Montant retenue d'assurance décanale + Montant de la retenue d'assurance tous risque chantier + Valeur approvisionnement) |
| Réglé |  |  | = ∑ des montants des paiement affectés à la facture |
| Reste |  |  | = Net à payer - Réglé |
| Valeur approvisionnement |  |  | En lecture seule, cette valeur est calculée dans les [décomptes](#page-decomptes) |
| Avance sur décompte |  |  | En lecture seule, cette valeur est calculée dans les [décomptes](#page-decomptes) |
| Retenues forfaitaires |  |  | En lecture seule, cette valeur est calculée dans les [décomptes](#page-decomptes) |
| Remboursement De compte prorata |  |  | En lecture seule, cette valeur est calculée dans les [décomptes](#page-decomptes) |
| Remboursement Fluctuations |  |  | En lecture seule, cette valeur est calculée dans les [décomptes](#page-decomptes) |
| Amortissement avance |  |  |  |
| Taux |  |  | En lecture seule |
| Montant |  |  | En lecture seule |
| Retenue d’assurance décennale |  |  |  |
| Taux |  |  | En lecture seule |
| Montant |  |  | En lecture seule |
| Retenue d’assurance tout risque chantier |  |  |  |
| Taux |  |  | En lecture seule |
| Montant |  |  | En lecture seule |
| Retenue de garantie |  |  |  |
| Taux |  |  | En lecture seule |
| Montant |  |  | En lecture seule |
| Retenue de compte prorata |  |  |  |
| Montant |  |  | En lecture seule |
| Taux |  |  | En lecture seule |
| Lignes |  |  |  |
| Liste des lignes |  |  | La liste visualise les lignes d’un devis :<br>- O.D.T<br>- Code<br>- Description<br>- Quantité<br>- Prix unitaire hors taxe<br>- Remise<br>- FODEC..<br>- TVA<br>- Prix unitaire TTC : (PU HT (avec FODEC) * Pourcentage TVA) + PU HT (avec FODEC)<br> Remarque: PU HT (avec FODEC) = (PU HT après remise * Pourcentage FODEC) + (PU HT après remise)<br>Encore, à partir de cette liste, on peut effectuer les opérations suivantes :<br>- Ajouter<br>- Ajouter à partir d’un lot<br>- Supprimer<br>- Tout sélectionner<br>- Exporter<br>- Importer |
| Ventilation |  |  |  |
| Liste de ventilations des Projets |  |  | La liste visualise la ventilation par projet en indiquant:<br>- Le taux de ventilation<br>- Le projet<br>==> Si on clic sur le bouton "Appliquer la ventilation par défaut", le système répartie la ventilation selon le paramétrage par défaut effectué dans l'écran " [Projet](#page-projets) ".<br>==>Si on clic sur le bouton "Projet", le système affiche la liste des [projets](#page-projets)<br>Remarque 1: Si on choisit un projet à partir de la liste déroulante (Sous l'onglet "Totaux"), le système applique une ventilation de 100% sur ce dernier.<br> Remarque 2: La somme des taux de ventilation doit être égale à 100% |
| Liste de ventilations des catégories |  |  | La liste visualise la ventilation pour chaque [catégorie](#page-categoriedesdepenses) choisi en indiquant :<br>- Le taux<br>- La catégorie<br>Si la facture à des lignes<br>- Le système rend la liste en lecture seule et il applique la ventilation en fonction du montant total de document la somme des montants totaux TTC des lignes par catégorie.<br> Sinon<br>- Le système garde la liste vide, et il donne la possibilité d'ajouter et d'appliquer des taux de ventilation manuellement.<br>Remarque: La somme des taux de ventilation doit être égale à 100% |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009<br> Remarque: Si l'utilisateur connecté:<br>- N'a pas le droit d'ajout/modification des factures (Module achat)<br>- A le droit d'ajout/modification des factures à partir d'un lot Multi-fournisseurs<br>==> Dans le module achat --> Factures: Le système ouvre les factures en provenance de module projet avec le droit de modification et le reste en lecture seule. |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Générer un avoir | Le système génère un avoir pour la facture sélectionnée |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Audit | GEN0020 |
| Générer à partir de | GEN0022 |
| Bon pour paiement | Le système affecte l'utilisateur connecté |
| Appliquer la ventilation par défaut | Le système applique [la ventilation par défaut des projets](#page-projets) avec leurs taux respectifs |
| Projet | Le système affiche la liste des [projets](#page-projets) en cours |
Créé avec HelpNDoc Personal Edition: [Créer des documents d'aide facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

### DOCUMENTS A REGLER <a id="page-documentsaregler"></a>
_Page : `DOCUMENTSAREGLER.html` — Module Achat_

[image : NouvelElement765.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Totaux |  |  |  |
| Code | X | X | Auto-généré en lecture seule |
| Date |  |  |  |
| Échéance |  |  |  |
| Entité juridique |  | X | Liste déroulante des [entités juridique](#page-entitesjuridique) |
| Module |  | X | Liste déroulante de modules |
| Personne à contacter |  |  |  |
| Type |  | X | Liste déroulante des [types](#page-typeslot) |
| Réf. Interne (Référence interne) |  |  |  |
| Statut |  |  | Liste déroulante de [Statuts](#page-statuts-buy) : sélecteur de statut, lorsque l’utilisateur clique sur le bouton le système cherche les transitions possible en respectant le cycle de vie et les habilitation.<br> Remarque : Une fois le statut destination choisi, avant l'affectation le système vérifie les champs obligatoires, les documents à joindre et les champs obligatoires du fournisseur configurés dans la transition. |
| Validité |  |  | Si la durée de vie du statut sélectionné est égale à zéro, le champ validité sera en lecture seule si non la date validité sera égale à date du jour + la durée de vie du statut.<br> Remarque : Si la date validité est renseignée le système génère une alerte d'échéance. |
| Fournisseur |  |  | Sélecteur des [fournisseurs](#page-fournisseurs-adm) |
| Projet |  |  | Liste déroulante des [projets](#page-projets) |
| Lot |  |  |  |
| Dossier de groupement |  |  | Sélecteur des [dossiers de groupement](#page-dossiersdegroupement) |
| Déclencheur |  |  | Sélecteur des [personnels](#page-personnels)<br> Par défaut, le système affecte le personnel de l'utilisateur connecté. |
| Validation par |  |  |  |
| Validé par |  |  |  |
| Note |  |  |  |
| Note 2 |  |  |  |
| Total HT |  |  | Si la facture possède des lignes<br> ce champ sera en lecture seule = ∑ des total HT des lignes<br> Si non<br> il sera saisissable avec un calcul Total TTC = Valeur + Total TVA |
| Total TVA |  |  | Si la facture possède des lignes<br> ce champ sera en lecture = ∑ des total TVA des lignes<br> Si non<br> il sera saisissable avec un calcul Total TTC = Valeur + Total HT |
| Total TTC |  |  |  |
| Montant régule |  |  | Montant de régulation : Il existe des cas ou le calcul de la facture fournisseur est incompréhensible, il faut utiliser ce montant pour coller le Net à payer à la valeur exacte |
| Timbre fiscal |  |  | Lors de la création de la facture le système affecte la valeur par défaut du timbre fiscal configuré dans module administration |
| Net à payer |  |  | Lorsque l'utilisateur renseigne le Net à payer le système calcule l'impact sur le montant de régulation sans toucher le Total TTC.<br> Montant régul = ((Net à payer - Timbre fiscal) - (Total TTC + Remb. Fluctuations + Avance sur décompte) +(Montant amortissement avance + Montant retenue de garantie + Montant compte prorata + Montant retenue d'assurance décanale + Montant de la retenue d'assurance tous risque chantier + Valeur approvisionnement) |
| Réglé |  |  | En lecture seule<br> = ∑ des montants des paiement affectés à la facture |
| Reste |  |  | En lecture seule<br> = Net à payer - Réglé |
| Lignes |  |  |  |
| Liste des lignes |  |  | La liste visualise les lignes d’un devis :<br>- O.D.T<br>- Code<br>- Description<br>- Quantité<br>- Prix unitaire hors taxe<br>- Remise<br>- FODEC..<br>- TVA<br>- Prix unitaire TTC : (PU HT (avec FODEC) * Pourcentage TVA) + PU HT (avec FODEC)<br> Remarque: PU HT (avec FODEC) = (PU HT après remise * Pourcentage FODEC) + (PU HT après remise)<br>Encore, à partie de cette liste, on peut effectuer les opérations suivantes :<br>- Ajouter<br>- Ajouter à partir d’un lot<br>- Supprimer<br>- Tout sélectionner<br>- Exporter<br>- Importer |
| Ventilation |  |  |  |
| Liste de ventilations des Projets |  |  | La liste visualise la ventilation par projet en indiquant:<br>- Le taux de ventilation<br>- Le projet<br>==> Si on clic sur le bouton "Appliquer la ventilation par défaut", le système répartie la ventilation selon le paramétrage par défaut effectué dans l'écran " [Projet](#page-projets) ".<br>==>Si on clic sur le bouton "Projet", le système affiche la liste des [projets](#page-projets)<br>Remarque 1: Si on choisit un projet à partir de la liste déroulante (Sous l'onglet "Totaux"), le système applique une ventilation de 100% sur ce dernier.<br> Remarque 2: La somme des taux de ventilation doit être égale à 100% |
| Liste de ventilation des catégories |  |  | La liste visualise la ventilation pour chaque [catégorie](#page-categoriedesdepenses) choisi en indiquant :<br>- Le taux<br>- La catégorie<br>Si la facture à des lignes<br>- Le système rend la liste en lecture seule et il applique la ventilation en fonction du montant total de document la somme des montants totaux TTC des lignes par catégorie.<br> Sinon<br>- Le système garde la liste vide, et il donne la possibilité d'ajouter et d'appliquer des taux de ventilation manuellement.<br>Remarque: La somme des taux de ventilation doit être égale à 100% |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Audit | GEN0020 |
| Générer à partir de | GEN0022 |
| Appliquer la ventilation par défaut | Le système applique [la ventilation par défaut des projets](#page-projets) avec leurs taux respectifs |
| Projet | Le système affiche la liste des [projets](#page-projets) en cours |
Créé avec HelpNDoc Personal Edition: [Générateur complet de livres électroniques Kindle](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creer-ebooks-pour-kindle-amazon)

### AVOIRS <a id="page-avoirs"></a>
_Page : `AVOIRS.html` — Module Achat_

[image : NouvelElement766.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Totaux |  |  |  |
| Code | X | X | Auto-généré en lecture seule |
| Date |  |  | Date de la facture |
| Échéance |  |  | Échéance de règlement de la facture |
| Entité juridique |  | X | Liste déroulante des [entités juridique](#page-entitesjuridique) |
| Module |  | X | Liste déroulante de modules |
| Fournisseurs |  |  | Sélecteur des [fournisseurs](#page-fournisseurs-adm) |
| Personne à contacter |  |  | Zone saisissable avec proposition des contact du fournisseur sélectionné |
| Type |  |  | Liste déroulante des [types](#page-typeslot) |
| Réf. Interne |  |  | Référence interne : N° facture du fournisseur |
| Référence bureau d'ordre |  |  |  |
| Catégories |  |  | Sélecteur de [catégorie](#page-categoriedesdepenses) par défaut |
| Déclencheur |  |  | Sélecteur des [personnels](#page-personnels)<br> Par défaut, le système affecte le personnel de l'utilisateur connecté |
| Statut |  | X | Liste déroulante de [Statuts](#page-statuts-buy) : sélecteur de statut, lorsque l’utilisateur clique sur le bouton le système cherche les transitions possible en respectant le cycle de vie et les habilitation.<br> Remarque : Une fois le statut destination choisi, avant l'affectation le système vérifie les champs obligatoires, les documents à joindre et les champs obligatoires du fournisseur configurés dans la transition. |
| Validité |  |  | Si la durée de vie du statut sélectionné est égale à zéro, le champ validité sera en lecture seule si non la date validité sera égale à date du jour + la durée de vie du statut.<br> Remarque : si la date validité est renseignée le système génère une alerte d'échéance. |
| Projet |  |  | Liste des [projets](#page-projets) |
| Lot |  |  | Champs en lecture seule, renseigné lors de la génération d'une facture à partir d'un [lot](#page-lots) du module projet |
| Dossier de groupement |  |  | Sélecteur des dossiers de groupement le liste du sélecteur affiche seulement les dossier de groupement non archivé |
| Facture source |  |  | Un lien vers la [facture](#page-factures) source |
| Note |  |  | Zone de saisie libre |
| Note 2 |  |  |  |
| Total HT |  |  | Si la facture possède des lignes<br> ce champ sera en lecture seule = ∑ des total HT des lignes<br> Si non<br> il sera saisissable avec un calcul Total TTC = Valeur + Total TVA |
| Total TVA |  |  | Si la facture possède des lignes<br> ce champ sera en lecture = ∑ des total TVA des lignes<br> Si non<br> il sera saisissable avec un calcul Total TTC = Valeur + Total HT |
| Total TTC |  |  | Si la facture possède des lignes<br> ce champ sera en lecture seule = ∑ des total TTC des lignes<br> Si non<br> il sera saisissable avec un calcul Total HT = Valeur - Total TVA |
| Montant régul |  |  | Montant de régulation : Il existe des cas ou le calcul de la facture fournisseur est incompréhensible, il faut utiliser ce montant pour coller le Net à payer à la valeur exacte |
| Timbre fiscal |  |  | Lors de la création de la facture le système affecte la valeur par défaut du timbre fiscal configuré dans module administration |
| Net à payer |  |  | Lorsque l'utilisateur renseigne le Net à payer le système calcule l'impact sur le montant de régulation sans toucher le Total TTC.<br> Montant régul = ((Net à payer - Timbre fiscal) - (Total TTC + Remb. Fluctuations + Avance sur décompte) +(Montant amortissement avance + Montant retenue de garantie + Montant compte prorata + Montant retenue d'assurance décanale + Montant de la retenue d'assurance tous risque chantier + Valeur approvisionnement) |
| Réglé |  |  | = ∑ des montants des paiement affectés à la facture |
| Reste |  |  | = Net à payer - Réglé |
| Valeur approvisionnement |  |  | En lecture seule, cette valeur est calculée dans les [décomptes](#page-decomptes) |
| Avance sur décompte |  |  | En lecture seule, cette valeur est calculée dans les [décomptes](#page-decomptes) |
| Retenues forfaitaires |  |  | En lecture seule, cette valeur est calculée dans les [décomptes](#page-decomptes) |
| Remb. De compte prorata |  |  | En lecture seule, cette valeur est calculée dans les [décomptes](#page-decomptes) |
| Remb. Fluctuations |  |  | En lecture seule, cette valeur est calculée dans les [décomptes](#page-decomptes) |
| Amortissement avance |  |  |  |
| Taux |  |  | En lecture seule |
| Montant |  |  | En lecture seule |
| Retenue d’assurance décennale |  |  |  |
| Taux |  |  | En lecture seule |
| Montant |  |  | En lecture seule |
| Retenue d’assurance tout risque chantier |  |  |  |
| Taux |  |  | En lecture seule |
| Montant |  |  | En lecture seule |
| Retenue de garantie |  |  |  |
| Taux |  |  | En lecture seule |
| Montant |  |  | En lecture seule |
| Retenue de compte prorata |  |  |  |
| Montant |  |  | En lecture seule |
| Taux |  |  | En lecture seule |
| Lignes |  |  |  |
| Liste des lignes |  |  | La liste visualise les lignes d’un devis :<br>- O.D.T<br>- Code<br>- Description<br>- Quantité<br>- Prix unitaire hors taxe<br>- Remise<br>- FODEC..<br>- TVA<br>- Prix unitaire TTC : (PU HT (avec FODEC) * Pourcentage TVA) + PU HT (avec FODEC)<br> Remarque: PU HT (avec FODEC) = (PU HT après remise * Pourcentage FODEC) + (PU HT après remise)<br>Encore, à partir de cette liste, on peut effectuer les opérations suivantes :<br>- Ajouter<br>- Ajouter à partir d’un lot<br>- Supprimer<br>- Tout sélectionner<br>- Exporter<br>- Importer |
| Ventilation |  |  |  |
| Liste de ventilations des Projets |  |  | La liste visualise la ventilation par projet en indiquant:<br>- Le taux de ventilation<br>- Le projet<br>==> Si on clic sur le bouton "Appliquer la ventilation par défaut", le système répartie la ventilation selon le paramétrage par défaut effectué dans l'écran " [Projet](#page-projets) ".<br>==>Si on clic sur le bouton "Projet", le système affiche la liste des [projets](#page-projets)<br>Remarque 1: Si on choisit un projet à partir de la liste déroulante (Sous l'onglet "Totaux"), le système applique une ventilation de 100% sur ce dernier.<br> Remarque 2: La somme des taux de ventilation doit être égale à 100% |
| Liste de ventilations des catégories |  |  | La liste visualise la ventilation pour chaque [catégorie](#page-categoriedesdepenses) choisi en indiquant :<br>- Le taux<br>- La catégorie<br>Si la facture à des lignes<br>- Le système rend la liste en lecture seule et il applique la ventilation en fonction du montant total de document la somme des montants totaux TTC des lignes par catégorie<br> Sinon<br>- Le système garde la liste vide, et il donne la possibilité d'ajouter et d'appliquer des taux de ventilation manuellement.<br>Remarque: La somme des taux de ventilation doit être égale à 100% |
Créé avec HelpNDoc Personal Edition: [Documentation Qt Help facile](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creez-des-fichiers-daide-pour-le-framework-qt-help)

### REGLEMENTS <a id="page-reglements"></a>
_Page : `REGLEMENTS.html` — Module Achat_

[image : NouvelElement774.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Entité juridique |  | X | Liste déroulante des [entités juridiques](#page-entitesjuridique) |
| Module |  | X | Liste déroulante des modules<br> Par défaut, module Achat |
| Date pièce |  | X |  |
| Date échéance |  | X |  |
| Type |  | X | Liste déroulante des [types](#page-typespiece) |
| Référence |  |  |  |
| Compte |  | X | Liste déroulante des [comptes](#page-comptes) |
| Base |  |  |  |
| Montant RS |  |  |  |
| Montant |  |  |  |
| Montant affecté |  |  | En lecture seule |
| Reste à affecter |  |  | En lecture seule |
| Tiers |  |  | Sélecteur des [fournisseurs](#page-fournisseurs-adm) |
| Tiers payeur |  |  |  |
| Tiers autrui |  |  |  |
| Notaire |  |  |  |
| Notes |  |  |  |
| Code P.P |  |  | Code proposition de paiement |
| Code C.R.A.S |  |  | Code certificat de retenue à la source |
| État |  |  | Liste déroulante des états<br> Les états possibles sont:<br> - En cours<br> - Pointé(e)<br> - Impayé(e) |
| Liste des montants affectés à régler |  |  | La liste visualise les détails des affectations en indiquant:<br>- Le code<br>- La date<br>- L'échéance<br>- La description<br>- Net à payer<br>- Réglé<br>- Reste à payer<br>- Montant affecté<br> Encore, dans cette liste, on peut affecter les factures qui ont un reste à réglé pour le même fournisseur. |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Audit | GEN0020 |
| Générer à partir de | GEN0022 |
| P.P | Lorsqu'on clic sur le bouton P.P, le système imprime la proposition de paiement et il génère le code correspondant. |
| C.R.A.S | Lorsqu'on clic sur le bouton C.R.A.S, le système imprime le certificat de retenue à la source et il génère le code correspondant. |
Créé avec HelpNDoc Personal Edition: [Créer des documents d'aide facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

### LISTE DES IMPAYES <a id="page-listedesimpayes"></a>
_Page : `LISTEDESIMPAYES.html` — Module Achat_

[image : NouvelElement776.png]

### Liste

| LIBELLE | V | OBSERVATIONS |
|---|---|---|
| Catégorie | X |  |
| Code | X |  |
| Code terrain |  |  |
| Crédit |  |  |
| Crée le |  |  |
| Crée par |  |  |
| Date |  |  |
| Date alerte |  |  |
| Date échéancier |  |  |
| Dépouillement | X |  |
| Description | X |  |
| Dossier de groupement |  |  |
| Échéance |  |  |
| Entité fonctionnelle |  |  |
| Entité fonctionnelle description longue |  |  |
| Entité juridique |  |  |
| Fournisseur |  |  |
| Frais terrain |  |  |
| Lot |  |  |
| Market |  |  |
| Market offer |  |  |
| Modifier le |  |  |
| Modifier par |  |  |
| Module |  |  |
| Montant HT |  |  |
| Montant TTC |  |  |
| Montant TVA |  |  |
| Net à payer | X |  |
| Note terrain |  |  |
| Notes |  |  |
| Notes 2 |  |  |
| Projet |  |  |
| Référence bureau d'ordre |  |  |
| Réglé |  |  |
| Reste |  |  |
| Retenues |  |  |
| Statut |  |  |
| Statut code |  |  |
| Terrain |  |  |
| Type |  |  |
| Type de crédit |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Éditer | GEN0009 |
| Imprimer | GEN0011 |
| Exporter | GEN0008 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014<br> Le système ouvre le document (Facture ou Document à régler) en question. |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Imprimer | GEN0011 |
Créé avec HelpNDoc Personal Edition: [Générateur de documentations PDF gratuit](https://www.helpndoc.com/fr)

### DOCUMENTS <a id="page-documents"></a>
_Page : `DOCUMENTS.html` — Module Achat_

[image : NouvelElement387.png]

### UNDER CONSTRUCTION
Créé avec HelpNDoc Personal Edition: [Créer des aides HTML, DOC, PDF et des manuels depuis une même source](https://www.helpndoc.com/fr/outils-creation-aide)

### CREDITS <a id="page-credits"></a>
_Page : `CREDITS.html` — Module Achat_

Créé avec HelpNDoc Personal Edition: [Générateur de documentation et EPub gratuit](https://www.helpndoc.com/fr)

#### GESTION DES PRETS <a id="page-gestiondesprets"></a>
_Page : `GESTIONDESPRETS.html` — Module Achat › CREDITS_

[image : NouvelElement698.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Prêts (Informations générales) |  |  |  |
| Entité juridique |  |  | sélecteur des [entités juridiques](#page-entitesjuridique) |
| Référence |  |  |  |
| Date |  |  | Par défaut date système |
| Description |  |  |  |
| Terrain |  |  | Liste déroulante des [terrains](#page-terrains) |
| Projet |  |  | Liste déroulante des [projets](#page-projets) |
| Type |  |  | Liste déroulante de [types](#page-typesdecredit) de crédit |
| Banque |  |  | Sélecteur des [banques](#page-banques) |
| Montant |  |  | En lecture seule<br> ∑ des montants de déblocages |
| Taux Intérêts |  |  |  |
| Perçu |  |  | En lecture seule<br> = ∑ des montants des déblocages |
| Reste à percevoir |  |  | En lecture seule<br> = Montant - Perçu |
| Taux TVA intérêts |  |  | Par défaut le système applique le TVA paramétré dans le masque "[Paramètres système](#page-parametressysteme)["](#page-parametressysteme) |
| Taux TVA commission |  |  | Par défaut le système applique le TVA paramétré dans le masque "Paramètres système" |
| Commission par défaut TTC |  |  |  |
| Total à régler |  |  | En lecture seule,<br> ∑ des montants à régler. |
| Prévision de frais |  |  |  |
| Frais |  |  | En cliquant sur le bouton [image : add_16x16.png], le système crée un [document à régler](#page-documentsaregler) pour le frais de prêt. |
| Liste des prêts |  |  | La liste visualise :<br>- La date<br>- Le montant de déblocage<br>- Le reste à recevoir = Montant de déblocage - Montant débloqué<br>- Le montant à régler<br>- Le montant réglé<br>- Reste à régler = Montant réglé - Reste à régler<br>- Note<br> Aussi, dans cette liste on peut effectuer les opérations suivantes :<br>- Ajouter<br>- Générer un mouvement réel: Le système génère un mouvement de type recette (sous l'onglet recette)<br>- Supprimer<br>- Double Cliquer : Le système affiche l’échéancier du remboursement de crédit sélectionné. |
| Recettes |  |  |  |
| Liste de recettes |  |  | Sous cet onglet, le système visualise tout les mouvements de type recette |
| Dépenses |  |  |  |
| Liste de dépenses |  |  | Sous cet onglet, le système visualise tout les mouvements de type dépense |
| Notes |  |  |  |
| [image : NouvelElement701.png] |  |  |  |
| Prêts (Information sur le déblocage de crédit) |  |  |  |
| Montant |  |  | Montant de déblocage |
| Montant à régler |  |  | ∑ des totaux TTC des lignes d'échéancier de remboursement |
| Notes |  |  |  |
| Date |  |  |  |
| Taux annuel |  |  |  |
| Taux mensuel/journalier |  |  |  |
| Taux actuariel |  |  |  |
| Périodicité en mois |  |  | Fréquence de remboursement de crédit |
| Nombre de périodes |  |  |  |
| Période de grâce |  |  |  |
| Durée de différé d’amortissement |  |  |  |
| Durée de différé total |  |  |  |
| Liste de remboursement de crédit |  |  | La liste visualise:<br>- La date de remboursement<br>- Le montant du principal<br>- L’intérêt TTC<br>- Commission TTC<br>- Total TTC<br>- Réglé<br>- Reste à régler<br>- Note<br>Remarque: Le système affiche ([image : NouvelElement702.png]) lorsqu'on génère un mouvement réel sur une ligne de remboursement de crédit.<br>Aussi, on peut effectuer les opérations suivantes :<br>- Ajouter un remboursement<br>- Supprimer un remboursement<br>- Générer un mouvement réel: Le système génère un mouvement de type dépense (sous l'onglet dépense)<br>- Importer |
| Recettes |  |  |  |
| Liste de recettes |  |  | La liste visualise les mouvements générés de type recette (Des pièces de règlement) en indiquant :<br>- La date<br>- La description<br>- Le montant<br>- La note…<br>Aussi, dans cette liste on peut effectuer les opérations suivantes :<br>- Éditer une recette<br>- Supprimer une recette |
| Dépenses |  |  |  |
| Liste des dépenses |  |  | La liste visualise les mouvements de type dépenses (Des documents à régler) en indiquant :<br>- Ligne de prêt: C'est la date de remboursement de crédit<br>- La date: C'est la date de création de document à régler<br>- La description<br>- Le net à payer<br>- Le montant réglé<br>- Le reste<br>- La note<br>Remarque: Sous cet onglet, le système ne visualise que les mouvements de type dépenses de déblocage en question.<br>Aussi, dans cette liste on peut effectuer les opérations suivantes :<br>- Éditer paiement<br>- Supprimer paiement |
| Notes |  |  |  |
| Champ de saisi des notes |  |  |  |
Remarque:
Pour payer une facture en utilisant un crédit il faut:
1- Sélectionner le fournisseurs
2- Cliquer sur l'action ajouter dans la liste des affectations de la pièce de règlement généré
3- Choisir la facture à régler à partir de la liste affichée
N.B: Le système affiche que les factures qu'ont un statut paramétré "Validée pour paiement" du fournisseur sélectionné.
[image : NouvelElement591.png]

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Audit | GEN0020 |
| Imprimer | GEN0011 |
| Générer | Le système génère l’échéancier de remboursement de crédit. |
| Générer un mouvement réel | Le système crée un mouvement réel :<br> Si le mouvement est généré à partir de la liste des crédits :<br>- Mouvement de type recette (Des pièces de règlement)<br> Si le mouvement est généré à partir de la liste des remboursements :<br>- Mouvement de type dépense (Des documents à régler) |
Créé avec HelpNDoc Personal Edition: [Éditeur complet de livres électroniques ePub](https://www.helpndoc.com/fr/creer-des-livres-numeriques-epub)

#### ECHEANCES CREDITS <a id="page-echeancescredits"></a>
_Page : `ECHEANCESCREDITS.html` — Module Achat › CREDITS_

[image : NouvelElement717.png]

### Liste

| LIBELLE | V | OBSERVATIONS |
|---|---|---|
| Commission HT |  |  |
| Commission taux TVA |  |  |
| Commission TTC | X |  |
| Date | X |  |
| Intérêts HT |  |  |
| Intérêts taux TVA |  |  |
| Intérêts TTC | X |  |
| Notes | X |  |
| Principal | X |  |
| Réglé | X |  |
| Reste à régler | X |  |
| Total HT | X |  |
| Total TTC | X |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Valider échéance | Le système génère un document à régler pour pour chaque lignes sélectionnées |
| Éditer | Le système ouvre la fiche de document à régler (si il est déjà généré) |
| Exporter | GEN0012 |
Remarque: Le système affiche tout les échéances qui ont une date inférieur ou égale à la période sélectionnée.
Créé avec HelpNDoc Personal Edition: [Créer de la documentation iPhone facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites/generation-de-site-web-iphone)

### TABLEAUX DE BORDS <a id="page-tableauxdebords-buy"></a>
_Page : `TABLEAUXDEBORDS_BUY.html` — Module Achat_

Créé avec HelpNDoc Personal Edition: [Créer facilement des fichiers Qt Help](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

#### SOLDE FOURNISSEURS <a id="page-soldefournisseurs"></a>
_Page : `SOLDEFOURNISSEURS.html` — Module Achat › TABLEAUX DE BORDS_

[image : NouvelElement481.png]
La liste visualise la liste des soldes fournisseurs selon l'entité juridique en indiquant:

- La Description du fournisseur
- Le Net à payer = Σ montants des nets à payer des factures et des documents à régler
Remarques:
- Le système ne soustrait pas le montants des avoirs net à payer.
- Le système ne comptabilise que les documents qui ont un tiers de type "Fournisseur"

- Le montant réglé = Σ des montants des règlements de type débit (en provenance de module Achat, y compris les règlements des crédits ) - Σ des montants des règlements de type crédit (en provenance de module Achat, y compris les règlements des crédits ) + Σ des montants des règlements de module trésorerie de type débit (qui ne sont pas en provenance de module Achat) - Σ des montants des règlements de module trésorerie de type crédit (qui ne sont pas en provenance de module Achat)
Remarque:
- Le système ne comptabilise que les documents qui ont un tiers de type "Fournisseur"

- Le montant reste = Net à payer - Réglé.
Remarque: Si on fait un double clique sur une ligne, le système affiche la liste détaillées des factures, documents à régler et règlements du fournisseur sélectionné
[image : NouvelElement884.bmp]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Fournisseur |  |  |  |
| Montant TTC |  |  | En lecture seule,<br> Σ des montants TTC des factures + Σ des montants TTC des documents à régler + Σ des montants des approvisionnements + Σ des montants fluctuation + Σ des montants de remboursement de compte prorata. |
| A payer |  |  | En lecture seule,<br> Σ des montants net à payer des factures + Σ des montants net à payer des documents à régler. |
| RAS |  |  |  |
| Retenues |  |  | En lecture seule,<br> A payer - Montant TTC |
| Base |  |  | En lecture seule,<br> Σ des montants Base des règlements de module achat + Σ des montants Base des règlements de module trésorerie du fournisseur en question |
| Règlement |  |  | En lecture seule,<br> Σ des montants après retenue à la source des règlements du fournisseur en question. |
| Solde |  |  | A payer - Base |
| Liste des documents |  |  | La liste visualise l'état des documents en indiquant:<br>- L'échéance<br>- Le montant TTC<br>- Le montant retenue<br>- Le montant a payer<br>- Le montant base<br>- Le montant RAS<br>- Le montant règlement<br>- La description<br>Dans cette liste, on peut soit ouvrir la fiche du document sélectionné ou bien exporter la liste. |

### Action

| LIBELLE | OBSERVATIONS |
|---|---|
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Rafraîchir | GEN0003 |
| Annuler et fermer | GEN0006 |
Créé avec HelpNDoc Personal Edition: [Créer des fichiers d'aide pour la plateforme Qt Help](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creez-des-fichiers-daide-pour-le-framework-qt-help)

#### ETAT DE LA TVA DEDUCTIBLE <a id="page-etatdelatvadeductible"></a>
_Page : `ETATDELATVADEDUCTIBLE.html` — Module Achat › TABLEAUX DE BORDS_

[image : NouvelElement483.png]
La liste visualise l’état de la TVA déductible en fonction de la période choisi en indiquant:

- Le montant HT
- Le TVA
- Le montant TVA
- La description

### Liste

| LIBELLE | V | OBSERVATIONS |
|---|---|---|
| Montant HT |  | Montants HT des lignes des factures |
| TVA |  | Taux TVA à partir des lignes des factures |
| Montant TVA |  | Montant HT * Taux TVA |
| Description |  |  |

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Période |  |  | Liste déroulante des périodes |
| Du |  |  |  |
| Au |  |  |  |
| Total TVA déductible |  |  | En lecture seule<br> Σ des montants TVA |
Créé avec HelpNDoc Personal Edition: [Générateur d'aide complet](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

#### ETAT DE LA RS <a id="page-etatdelars"></a>
_Page : `ETATDELARS.html` — Module Achat › TABLEAUX DE BORDS_

[image : NouvelElement210.png]

### UNDER CONSTRUCTION
Créé avec HelpNDoc Personal Edition: [Produire des livres Kindle gratuitement](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creer-ebooks-pour-kindle-amazon)

#### RAPPORTS <a id="page-rapports-buy"></a>
_Page : `RAPPORTS_BUY.html` — Module Achat › TABLEAUX DE BORDS_

Créé avec HelpNDoc Personal Edition: [Environnement de création d'aide complet](https://www.helpndoc.com/fr/outils-creation-aide)

##### SUIVI DES DEPENSES PAR MOIS <a id="page-buytdbactivitybymoth"></a>
_Page : `BuyTdbActivityByMoth.html` — Module Achat › TABLEAUX DE BORDS › RAPPORTS_

[image : NouvelElement183.png]

### UNDER CONSTRUCTION
Créé avec HelpNDoc Personal Edition: [Outil de création d'aide complet](https://www.helpndoc.com/fr/outils-creation-aide)

##### SUIVI DES RETENUES <a id="page-buytdbmarketsretainedamounts"></a>
_Page : `BuyTdbMarketsRetainedAmounts.html` — Module Achat › TABLEAUX DE BORDS › RAPPORTS_

[image : NouvelElement183.png]

### UNDER CONSTRUCTION
Créé avec HelpNDoc Personal Edition: [Qu'est-ce qu'un outil de création d'aide ?](https://www.helpauthoringsoftware.com/fr)

##### ECHEANCIER DE REMBOURSSEMENT DES CREDITS <a id="page-buytdbloanschedulebyyear"></a>
_Page : `BuyTdbLoanScheduleByYear.html` — Module Achat › TABLEAUX DE BORDS › RAPPORTS_

[image : NouvelElement183.png]

### UNDER CONSTRUCTION
Créé avec HelpNDoc Personal Edition: [Générateur d'aides Web gratuit](https://www.helpndoc.com/fr)

### CONFIGURATION <a id="page-configuration-buy"></a>
_Page : `CONFIGURATION_BUY.html` — Module Achat_

Créé avec HelpNDoc Personal Edition: [Générateur d'aide complet](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

#### STATUTS <a id="page-statuts-buy"></a>
_Page : `STATUTS_BUY.html` — Module Achat › CONFIGURATION_

Cette entité permet de définir les différents états d’un ordre de paiement qui peut correspondre à une facture ou un document à régler. Ces états constituent les étapes par lesquelles l’ordre de paiement passera lors de son cycle de vie.
[image : NouvelElement569.png]
[image : NouvelElement534.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| O.D.T |  | X |  |
| Code | X | X |  |
| Description |  | X |  |
| Type |  | X | Liste déroulante de types de statut.<br> La liste peut prendre une des valeurs suivantes :<br> Nouveau<br> Intermédiaire<br> Annulation<br> Terminer avec échec<br> Terminer avec succès |
| Durée de vie |  |  | Lorsque le statut possède une valeur de la durée de vie différente de 0, lorsqu’on affecte ce statut à un ordre de paiement le système génère automatiquement un rappel basé sur la date du jour plus le nombre de jour de la durée de vie. Ce rappel est visible dans la liste des ordres de paiement par l’cône ([image : NouvelElement528.png]) |
| Validé(e) pour paiement |  |  | Du moment que cette case n’est pas coché, le système ne permettra pas d’effectuer de paiement sur tous les ordres de paiement qui sont à cet état. |
| Validé(e) en comptabilité |  |  | Cette case à cocher permet d’identifier les ordres de paiement qui ont été transmis et validé en comptabilité. |
| Couleur |  |  |  |
| Image |  |  |  |
| Remarque : Les cases à cocher correspondent aux opérations que peut faire l’utilisateur lorsque l’ordre de paiement est dans un statut déterminé. |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Enregistrer | GEN0001 pour la ligne sélectionnée |
| Supprimer | GEN0010 pour la ligne sélectionnée |
| Champs spécifiques | GEN0007 pour la ligne sélectionnée |
Créé avec HelpNDoc Personal Edition: [Générateur de documentation et EPub facile](https://www.helpndoc.com/fr)

#### TRANSITIONS <a id="page-transitions-buy"></a>
_Page : `TRANSITIONS_BUY.html` — Module Achat › CONFIGURATION_

Cette entité permet de définir les différentes possibilités de passage d’un statut à un autre pour un ordre de paiement lors de son cycle de vie.
[image : NouvelElement531.png]
[image : NouvelElement535.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| O.D.T |  | X |  |
| De | X | X | Le statut de départ : Liste déroulante des [statuts](#page-statuts-buy). |
| à | X | X | Le statut d’arrivé : Liste déroulante des [statuts](#page-statuts-buy). |
| Commentaire obligatoire |  | X | Si cette option est cochée le système obligera l’utilisateur à saisir un commentaire lors du passage d’un statut à un autre. |
| Habilitations |  |  | Les cases à cocher représentent les [profiles](#page-roles) des acteurs qui peuvent modifier le statut d’un « Ordre de paiement ». |
| Champs obligatoires |  |  | Liste de champs obligatoires à renseigner lors d’une transition d’un statut à un autre |
| Documents à joindre |  |  | C’est la liste des documents à joindre pour l’ordre de paiement lorsque celui-ci se trouve dans le statut correspondant. ( [image : NouvelElement530.png] ) |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Enregistrer | GEN0001 pour la ligne sélectionnée |
| Supprimer | GEN0010 pour la ligne sélectionnée |
| Champs spécifiques | GEN0007 pour la ligne sélectionnée |
Créé avec HelpNDoc Personal Edition: [Créer facilement des fichiers Qt Help](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

#### DOSSIERS DE GROUPEMENT <a id="page-dossiersdegroupement"></a>
_Page : `DOSSIERSDEGROUPEMENT.html` — Module Achat › CONFIGURATION_

[image : NouvelElement384.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Code |  | X |  |
| Description |  | X |  |
| Archivé |  |  |  |
| Ventilations par projet |  |  |  |
| Liste des ventilations par projet |  |  | La liste visualise<br>- Le taux de ventilation<br>- Projet<br> Aussi dans cette liste on peut faire les opérations suivantes :<br>- Ajouter<br>- Supprimer |
| Note |  |  |  |
| Champs de saisi des notes |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Audit | GEN0020 |
Créé avec HelpNDoc Personal Edition: [Créer des aides HTML, DOC, PDF et des manuels depuis une même source](https://www.helpndoc.com/fr/outils-creation-aide)

#### NOMENCLATURE <a id="page-nomenclature"></a>
_Page : `NOMENCLATURE.html` — Module Achat › CONFIGURATION_

[image : NouvelElement385.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Catégorie |  | X | Sélecteur des [catégories](#page-categoriedesdepenses) |
| Code | X | X |  |
| Description |  | X |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Audit | GEN0020 |
Créé avec HelpNDoc Personal Edition: [Générateur de documentation iPhone gratuit](https://www.helpndoc.com/fr/tour-des-fonctionnalites/generation-de-site-web-iphone)

#### TYPES DE CREDIT <a id="page-typesdecredit"></a>
_Page : `TYPESDECREDIT.html` — Module Achat › CONFIGURATION_

[image : NouvelElement386.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| O.D.T |  | X |  |
| Code | X | X |  |
| Description |  | X |  |
| Financement fournisseur |  |  | Si le type de crédit est paramétré "Financement fournisseur", le système:<br>- Ne génère pas une recette dans la trésorerie |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Enregistrer | GEN0001 pour la ligne sélectionnée |
| Supprimer | GEN0010 pour la ligne sélectionnée |
Créé avec HelpNDoc Personal Edition: [Créer des aides HTML, DOC, PDF et des manuels depuis une même source](https://www.helpndoc.com/fr/outils-creation-aide)

#### FOURNISSEURS <a id="page-fournisseurs-buy"></a>
_Page : `FOURNISSEURS_BUY.html` — Module Achat › CONFIGURATION_

[image : NouvelElement312.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Code | X | X | Code auto généré en lecture seule |
| Afficher comme |  | X | En lecture seule |
| Forme juridique |  | X | Liste déroulante des [formes juridique](#page-formessociale) |
| Personnel |  |  | Liste déroulante de [personnels](#page-personnels) |
| Nature |  |  | Liste déroulante de [nature](#page-natures) |
| Général (PH) |  |  |  |
| Titre |  | X | Liste déroulante de [titres](#page-titres) |
| Sexe |  | X | Liste déroulante de [sexe](#page-sexes) |
| Nom |  | X |  |
| Prénom |  | X |  |
| Pays de résidence |  |  | Liste déroulante des [pays](#page-pays) |
| Mobile |  | X |  |
| E-mail |  | X |  |
| Date de naissance |  |  |  |
| Lieu |  |  |  |
| Situation familiale |  |  | Liste déroulante des [situations familiale](#page-situationsfamiliale) |
| Enfant |  |  |  |
| Pièce d’identité |  |  | Liste déroulante de [type de pièce d’identité](#page-typespieceidentite) |
| N° |  |  |  |
| Délivrée le |  |  |  |
| Lieu |  |  |  |
| Valide jusqu’au |  |  |  |
| Lieu |  |  |  |
| Fils de |  |  |  |
| Et de |  |  |  |
| Téléphone |  |  |  |
| Adresse |  |  |  |
| Pays |  |  | Liste déroulante des [pays](#page-pays) |
| Code postal |  |  |  |
| Ville |  |  | Liste déroulante de [villes](#page-villes) |
| Nationalité |  |  | Liste déroulante de [nationalités](#page-nationalites) |
| Résidence |  |  | Liste déroulante de [résidences](#page-typesresidence) |
| Profession |  |  | Liste déroulante de [professions](#page-professions) |
| Salaire |  |  | Liste déroulante des [fourchettes salariales](#page-salaires) |
| Employeur |  |  |  |
| Ancienneté |  |  |  |
| Régime matrimonial |  |  | Liste déroulante de [régimes matrimoniaux](#page-regimesmatrimonial) |
| Général (PM, SARL, SURAL, SA, OG, ONG) |  |  |  |
| Raison sociale |  | X |  |
| Pays de résidence |  |  | Liste déroulante de [pays de résidences](#page-pays) |
| Mobile |  | X |  |
| Email |  | X |  |
| Téléphone |  |  |  |
| M.F |  |  |  |
| R.C |  |  |  |
| C.D |  |  |  |
| Activité |  |  |  |
| Capital |  |  |  |
| Adresse |  |  |  |
| Pays |  |  | Liste déroulante de [pays](#page-pays) |
| Code postal |  |  |  |
| Ville |  |  | Liste déroulante de [villes](#page-villes) |
| Nationalité |  |  | Liste déroulante de [nationalités](#page-nationalites) |
| Résidence |  |  | Liste déroulante de [résidences](#page-typesresidence) |
| Date de création |  |  |  |
| Lieu |  |  |  |
| Qualité |  |  |  |
| Autres |  |  |  |
| Assujettissement |  |  | Liste déroulante d’[assujettissements](#page-assujetissements) |
| Banque |  |  | Liste déroulante de [banques](#page-banques) |
| Agence |  |  |  |
| RIB |  |  |  |
| IBAN |  |  |  |
| Code comptable |  |  |  |
| Compte collectif |  |  |  |
| Téléphone 2 |  |  |  |
| Téléphone 3 |  |  |  |
| Fax |  |  |  |
| Site web |  |  |  |
| Contacts |  |  |  |
| Liste de contacts |  |  | La liste visualise les contacts en indiquant :<br>- Le contact<br>- Le [type de relation](#page-typesderelation)<br> Sur cette liste on peut effectuer les opérations suivantes :<br>- Ajouter un contact<br>- Éditer un contact<br>- Supprimer un contact |
| Notes |  |  |  |
| Champ de saisi des notes |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Importer | GEN0018 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichier attaché | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0017 |
| Champs spécifiques | GEN0007 |
| Audit | GEN0020 |
Créé avec HelpNDoc Personal Edition: [Générateur complet de livres électroniques ePub](https://www.helpndoc.com/fr/creer-des-livres-numeriques-epub)

## Module Vente <a id="page-modulevente"></a>
_Page : `ModuleVente.html`_

Le module vente prend en charge la tâche de gestion des ventes en simulant une démarche qui se conforme parfaitement au processus réel en créant une fiche prospect, la prospection et le dossier de vente avec les différents statuts qu’il peut prendre.
Aussi, ce module gère les différentes opérations qu’elles peuvent être effectuées sur un dossier qui se résume en quatre points :

1. Les transferts clients : Le changement d’un propriétaire (ou des propriétaires) d’un article
2. Les transferts produits : Le changement d’un article
3. Les Modifications : La modification d’un article
4. Les désistements : La résiliation de l’achat d’un article
Encore, ce module est renfoncé par des tableaux de bord qui aident au suivi des ventes (prévisionnelles et réelles) et la performance des commerciaux.
Créé avec HelpNDoc Personal Edition: [Générateur complet de livres électroniques Kindle](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creer-ebooks-pour-kindle-amazon)

### PREPROSPECTIONS <a id="page-preprospections"></a>
_Page : `PREPROSPECTIONS.html` — Module Vente_

[image : NouvelElement788.png]
Fiche:

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Code |  |  | En lecture seule, Auto généré |
| Date |  | X | Par défaut, date de jour |
| Description temporaire |  | X |  |
| Téléphone temporaire |  |  |  |
| E mail |  |  |  |
| Note |  |  |  |
| Prospect |  |  |  |
| Tranche |  |  | Liste déroulante des [tranches](#page-cfg-tranches) |
| Article |  |  | Sélecteur des [articles](#page-articles) |
| Nature |  |  | Liste déroulante des [natures](#page-naturesprospections) |
| Type |  |  | Liste déroulante des [types](#page-typesdesactions) |
| Action commerciale |  |  | Liste déroulante des [actions commerciale](#page-actionscommerciales) |
| Source de contact |  |  | Liste déroulante des [sources de contact](#page-sourcedecontact) |
| Support |  |  |  |
| Commercial |  |  | Sélecteur des commerciaux |
| Données temporaires |  |  |  |
| Donnée 1 |  |  |  |
| Donnée 2 |  |  |  |
| Donnée 3 |  |  |  |
| Donnée 4 |  |  |  |
| Donnée 5 |  |  |  |
| Donnée 6 |  |  |  |
| Donnée 7 |  |  |  |
| Donnée 8 |  |  |  |
| Donnée 9 |  |  |  |
| Donnée 10 |  |  |  |
| Autres tranches |  |  |  |
| Liste des autres tranches |  |  | Le système affiche une liste de projet en visualisant :<br>- Le projet<br>- Les champs crée le, crée par, modifier le et modifier par<br> Aussi, dans cette liste on peut effectuer les opérations suivantes<br>- Ajouter un projet<br>- Supprimer un projet |
| Profil |  |  |  |
| Articles |  |  | [Catégorie](#page-stk-categories-item) articles |
|  |  | [Types](#page-stk-types-item) articles |  |
| Sous articles |  |  | [Catégorie](#page-stk-categories-item) sous articles |
|  |  | [Types](#page-stk-types-item) sous articles |  |
| Actions |  |  |  |
| Liste des actions |  |  | La liste visualise le nombre d’actions groupées par [type](#page-typesdesactions) |
| Autres |  |  |  |
| Superficie (m²) de |  |  |  |
| à |  |  |  |
| Budget de |  |  |  |
| à |  |  |  |
| Crédit bancaire |  |  |  |
| Fond propre |  |  |  |
| Parrain |  |  | sélecteur des [tiers](#page-prospects) |
Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Imprimer | GEN0011 |
| Éditer | GEN0009 |
| Exporter | GEN0012 |
| Supprimer | GEN0010 |
| Importer | GEN0018 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Créer liste à faire | GEN0023 |
| Créer tiers | On cliquant sur le bouton "Créer tiers", le système crée un tiers en utilisant les données renseignées dans la fiche. |
| Valider | On cliquant sur le bouton "Valider", le système fait les contrôles nécessaire et il crée une prospection pour le prospect en question. |
Créé avec HelpNDoc Personal Edition: [Créer de la documentation iPhone facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites/generation-de-site-web-iphone)

### PREVISIONS <a id="page-previsions-com"></a>
_Page : `PREVISIONS_COM.html` — Module Vente_

[image : NouvelElement738.png]
Lorsqu'on clic sur le bouton "Assistant prévisions" le système affiche l'écran suivant:
[image : NouvelElement401.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Tranche |  |  | En lecture seule |
| Avance de concrétisation |  |  | Pourcentage de concrétisation |
| Nombre d’article |  |  |  |
| Total |  |  | Nombre d’article total à partir du stock |
| Libre |  |  | Nombre d’article libre |
| Réservé |  |  | Nombre d’article dont le statut « Réservation » |
| Concrétisé |  |  | Nombre d’article dont le statut >= « Concrétisation » et < « Minute » |
| Finalisé |  |  | Nombre d’article dont le statut >= « Minute » |
| Libres + Réservations |  |  |  |
| Valorisation |  |  | ∑ de prix de vente des articles libre +∑ de prix de vente des dossiers dont le statut = « Réservation » |
| Montant concrétisation |  |  | = Taux de concrétisation * Valorisation libre + Réservation |
| Montant finalisation |  |  | = (100 - Taux de concrétisation) * Valorisation libre + Réservation |
| >= Concrétisation et < Minute |  |  |  |
| Reste à régler |  |  | = ∑ des prix de vente reste des dossiers dont le statut >= "Concrétisation" et < "Minute" |
| Valorisation |  |  | = ∑ des prix de vente des dossiers dont le statut >= "Concrétisation" et < "Minute" |
| Dates |  |  |  |
| Date début des concrétisations |  |  |  |
| Date fin des concrétisations |  |  |  |
| Date début des finalisations |  |  |  |
| Date fin des finalisations |  |  |  |
| Écart-type concrétisation |  |  |  |
| Concrétisation |  |  |  |
| Écart-type des finalisations |  |  |  |
| Finalisation |  |  |  |
| Concrétisation |  |  |  |
| Quantité à prévoir |  |  | Nombre d'article libre + Nombre d'article réservé |
| Quantité à prévoir reste |  |  | Quantité à prévoir - Quantité de concrétisation saisi dans la liste de réajustement des prévisions<br> Remarque: Si le champ affiche une valeur négative<br> ==> L'utilisateur a saisi dans la liste de réajustement des prévisions une quantité de concrétisation supérieure à la quantité prévu. |
| Montant à prévoir |  |  | (∑ des prix de vente définitifs des articles libres + ∑ des prix de vente définitifs des articles réservés ) * taux de concrétisation |
| Unité proposée |  |  | = Montant à prévoir / Quantité à prévoir |
| Unité appliquée |  |  |  |
| Finalisation |  |  |  |
| Quantité à prévoir |  |  | Nombre d'articles libres + Nombre de dossier de vente dont le statut < Minute |
| Quantité à prévoir reste |  |  | Quantité à prévoir - Quantité finalisation saisi dans la liste de réajustement des prévisions<br> Remarque: Si le champ affiche une valeur négative<br> ==> L'utilisateur a saisi dans la liste de réajustement des prévisions une quantité de finalisation supérieure à la quantité prévu. |
| Montant à prévoir |  |  | [(100% - Taux de concrétisation) * (∑ des prix de vente définitifs des articles libres + ∑ des prix de vente définitifs des articles réservés)] + ∑ reste à régler des dossiers de ventre dont le statut > réservation et < Minute |
| Unité proposée |  |  | = Montant à prévoir / Quantité à prévoir |
| Unité appliquée |  |  |  |
| Chiffre d’affaire TTC |  |  |  |
| Quantité à prévoir |  |  | Nombre d'article libre + Nombre de dossier dont le statut < Minute |
| Montant à prévoir |  |  | (∑ des prix de vente définitifs des articles libres + ∑ prix de vente des dossiers dont le statut < Minute) |
| Unité proposée |  |  | = Montant à prévoir / Quantité à prévoir |
| Unité appliquée |  |  |  |
| Prévisions |  |  |  |
| Liste des prévisions de ventes enregistrée dans la base de données |  |  | La liste visualise tous les détails de prévisions des ventes en affichant :<br>- La date<br>- La quantité concrétisation<br>- Les versements concrétisation<br>- La quantité chiffre d’affaire<br>- Versement chiffre d’affaire<br>- Versement total<br>- Note<br> Aussi, dans cette liste on peut exporter les prévisions enregistrées |
| Réajustement des prévisions |  |  |  |
| Liste de réajustement des prévisions |  |  | La liste visualise les détails de prévisions des ventes à partir du mois actuel en affichant :<br>- La date<br>- La quantité concrétisation<br>- Les versements concrétisation<br>- La quantité chiffre d’affaire<br>- Versement chiffre d’affaire<br>- Versement total<br>- Note<br>Aussi, dans cette liste on peut effectuer les opérations suivantes<br>- Supprimer les prévisions<br>- Exporter les prévisions<br>- Importer les prévisions<br>- Imprimer les prévisions<br>Remarque : On peut saisir des prévisions dans le passé que si l’utilisateur connecté à la droit « Enregistrer des prévisions dans le passé » |
| Liste des prévisions passées |  |  | La liste visualise les détails de prévisions des ventes en affichant :<br>- La date<br>- La quantité concrétisation<br>- Les versements concrétisation<br>- La quantité chiffre d’affaire<br>- Versement chiffre d’affaire<br>- Versement total<br>- Note<br> Aussi dans cette liste on peut effectuer les opérations suivantes :<br>- Ajouter des prévisions<br>- Supprimer des prévisions<br>Remarque : La liste des prévisions passées sera affichée en lecture seule si l’utilisateur connecté n’a pas le droit de modifier les prévisions |
| Analyse |  |  |  |
| Courbe de prévisions des ventes |  |  | La courbe représente :<br>- Le montant des concrétisations<br>- Le montant des finalisations |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Éditer | GEN0009 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Audit | GEN0020 |
| Générer | Lorsqu'on clique sur générer le système affiche le message suivant<br>[image : NouvelElement177.png]<br>- Si on clique sur oui : le système génère les prévisions sur la base des articles libres<br>- Si on clique sur non : Le système génère les prévisions sur la base de tous les articles dans le stock<br>- Si on clique sur annuler : Le système ferme la fenêtre |
| Affecter [image : NouvelElement176.png] | Affecter les unités proposées aux unité appliquées |
Créé avec HelpNDoc Personal Edition: [Générateur de documentations PDF gratuit](https://www.helpndoc.com/fr)

### HOT LINE <a id="page-hotline"></a>
_Page : `HOTLINE.html` — Module Vente_

[image : NouvelElement405.png]

### Recherche Tiers
Champs

| LIBELLE | V | OBSERVATIONS |
|---|---|---|
| État | X |  |
| Code | X |  |
| Affiché comme | X |  |
| Mobile | X |  |
| Téléphone | X |  |
| Nombre visites | X |  |
| Nombre ventes | X |  |
| Notes | X |  |
| GEN0016 | X |  |
Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | Par défaut la fiche est en lecture seule, lorsque on clique sur le bouton ajouter on applique la règle générale GEN0008 |
| Éditer | GEN0009 |
| Enregistrer | GEN0001 |
| Rafraîchir | GEN0003 |
| Ajouter prospection | Ajouter une prospection pour un prospect sélectionné.de la liste de recherche de tiers |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |

### Disponibilités

| LIBELLE | V | OBSERVATIONS |
|---|---|---|
| Statut | X |  |
| H1Description | X |  |
| H2Description | X |  |
| H3Description | X |  |
| Code | X |  |
| Catégorie | X |  |
| Présentation | X |  |
| Type | X |  |
| Blocs | X |  |
| Etage | X |  |
| Orientation | X |  |
| Superficie (m²) | X |  |
| Prix du m² | X |  |
| Prix | X |  |
| Avance | X |  |
| Client | X |  |
| Contenance | X |  |

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Code | X | X | Code auto généré en lecture seule |
| Afficher comme |  | X | En lecture seule |
| Forme juridique |  | X | Liste déroulante des formes juridique |
| Personnel |  |  | Liste déroulante de personnels |
| Nature |  |  | Liste déroulante de nature |
| Général (PH) |  |  |  |
| Titre |  | X | Liste déroulante de [titres](#page-titres) |
| Sexe |  | X | Liste déroulante de [sexe](#page-sexes) |
| Nom |  | X |  |
| Prénom |  | X |  |
| Pays de résidence |  |  | Liste déroulante des pays |
| Mobile |  | X |  |
| E-mail |  | X |  |
| Date de naissance |  |  |  |
| Lieu |  |  |  |
| Situation familiale |  |  | Liste déroulante des [situations familiale](#page-situationsfamiliale) |
| Enfant |  |  |  |
| Pièce d’identité |  |  | Liste déroulante de [type de pièce d’identité](#page-typespieceidentite) |
| N° |  |  |  |
| Délivrée le |  |  |  |
| Lieu |  |  |  |
| Valide jusqu’au |  |  |  |
| Lieu |  |  |  |
| Fils de |  |  |  |
| Et de |  |  |  |
| Téléphone |  |  |  |
| Adresse |  |  |  |
| Pays |  |  | Liste déroulante des [pays](#page-pays) |
| Code postal |  |  |  |
| Ville |  |  | Liste déroulante de [villes](#page-villes) |
| Nationalité |  |  | Liste déroulante de [nationalités](#page-nationalites) |
| Résidence |  |  | Liste déroulante de [résidences](#page-typesresidence) |
| Profession |  |  | Liste déroulante de [professions](#page-professions) |
| Salaire |  |  | Liste déroulante des [fourchettes salariales](#page-salaires) |
| Employeur |  |  |  |
| Ancienneté |  |  |  |
| Régime matrimonial |  |  | Liste déroulante de [régimes matrimoniaux](#page-regimesmatrimonial) |
| Général (PM, SARL, SURAL, SA, OG, ONG) |  |  |  |
| Raison sociale |  | X |  |
| Pays de résidence |  |  | Liste déroulante de [pays de résidences](#page-pays) |
| Mobile |  | X |  |
| Email |  | X |  |
| Téléphone |  |  |  |
| M.F |  |  |  |
| R.C |  |  |  |
| C.D |  |  |  |
| Activité |  |  |  |
| Capital |  |  |  |
| Adresse |  |  |  |
| Pays |  |  | Liste déroulante de [pays](#page-pays) |
| Code postal |  |  |  |
| Ville |  |  | Liste déroulante de [villes](#page-villes) |
| Nationalité |  |  | Liste déroulante de [nationalités](#page-nationalites) |
| Résidence |  |  | Liste déroulante de [résidences](#page-typesresidence) |
| Date de création |  |  |  |
| Lieu |  |  |  |
| Qualité |  |  |  |
| Autres |  |  |  |
| Assujettissement |  |  | Liste déroulante d’[assujettissements](#page-assujetissements) |
| Banque |  |  | Liste déroulante de [banques](#page-banques) |
| Agence |  |  |  |
| RIB |  |  |  |
| IBAN |  |  |  |
| Code comptable |  |  |  |
| Compte collectif |  |  |  |
| Téléphone 2 |  |  |  |
| Téléphone 3 |  |  |  |
| Fax |  |  |  |
| Site web |  |  |  |
| Contacts |  |  |  |
| Liste de contacts |  |  | La liste visualise les contacts en indiquant :<br>- Le contact<br>- Le [type de relation](#page-typesderelation)<br> Sur cette liste on peut effectuer les opérations suivantes :<br>- Ajouter un contact<br>- Éditer un contact<br>- Supprimer un contact |
| Notes |  |  |  |
| Champ de saisi des notes |  |  |  |
| Prospections |  |  |  |
| Liste de prospections |  |  | La liste visualise les prospections faites par un client en indiquant :<br>- Le code<br>- La date<br>- La tranche<br>- L’article<br>- La catégorie… |
| Dossier |  |  |  |
| Liste de dossiers |  |  | La liste visualise les dossiers de ventes en affichant :<br>- Le statut<br>- La tranche<br>- L’article<br>- A payer<br>- Réglé… |
| Règlements |  |  |  |
| Liste de règlements |  |  | La liste visualise les règlements en affichant :<br>- La description<br>- Le montant<br>- L’historique |
| Historique |  |  |  |
| Liste d’historique |  |  | La liste visualise l’historique de dossier de vente d’un client en affichant :<br>- L’utilisateur<br>- La date<br>- L’état<br>- Le N° Dossier<br>- L’article<br>- Les détails |
| Remarque : La combinaison des champs « Description » + « mobile » ou « Description » + « E-mail » doivent êtres uniques. |  |  |  |

### Liste de prospections
Champs

| LIBELLE | V | OBSERVATIONS |
|---|---|---|
| Code | X |  |
| Date | X |  |
| Tranche | X |  |
| Article | X |  |
| Catégorie | X |  |
| Présentation | X |  |
| Type de présentation | X |  |
| Source de contact | X |  |
| Date première visite | X |  |
| Date dernière visite | X |  |
| Date de concrétisation | X |  |
| Délais de concrétisation | X |  |
| GEN0016 |  |  |

### Liste de dossiers

### Champs

| LIBELLE | V | OBSERVATIONS |
|---|---|---|
| Statut | X |  |
| Tranche | X |  |
| Article | X |  |
| A payer | X |  |
| Réglé | X |  |
| Reste | X |  |
| GEN0016 |  |  |

### Liste d’opérations
Champs

| LIBELLE | V | OBSERVATIONS |
|---|---|---|
| Code | X |  |
| N° dossier | X |  |
| Détail | X |  |
| Date validation | X |  |
| Date validation | X |  |
| Date demande | X |  |
| frais | X |  |
| Remise | X |  |
| Frais après remise | X |  |
| Motif | X |  |
| GEN0016 |  |  |

### Liste de règlements
Champs

| LIBELLE | V | OBSERVATIONS |
|---|---|---|
| Description | X |  |
| Montant | X |  |
| GEN0016 | X |  |

### Liste de communications
Champs

| LIBELLE | V | OBSERVATIONS |
|---|---|---|
| Description | X |  |
| Notes | X |  |
Créé avec HelpNDoc Personal Edition: [Nouvelles et informations sur les outils de logiciels de création d'aide](https://www.helpauthoringsoftware.com/fr)

### PROSPECT EXPRESS <a id="page-prospectexpress"></a>
_Page : `PROSPECTEXPRESS.html` — Module Vente_

[image : NouvelElement84.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Afficher comme |  |  | Champ en lecture seule |
| Tire |  | X | Liste déroulante des [titres](#page-titres) |
| Nom |  | X |  |
| Prénom |  | X |  |
| Pays de résidence |  | X | Liste déroulante de [pays](#page-pays) |
| Ville |  |  | Liste déroulante des [villes](#page-villes) |
| Mobile | X | X |  |
| Date |  | X |  |
| Source de contact |  | X | Liste déroulante des [sources de contacts](#page-sourcedecontact) |
| Action commercial |  |  | Liste déroulante des [actions commerciales](#page-actionscommerciales) |
| Nature |  | X | Liste déroulante des [natures](#page-naturesprospections) |
| Type |  | X | Liste déroulante des [types](#page-typesdesactions) |
| Projet |  | X | Liste déroulante des [projets](#page-cfg-tranches) |
| Article |  |  | Sélecteur des [articles](#page-articles)<br> Le système affiche la liste des articles libres et les articles qui ont un dossier de vente dont le statut est paramétré "Peut effectuer une prospection" |
| Téléphone |  |  |  |
| Commercial |  |  | Liste déroulante des [commerciaux](#page-personnels)[PERSONNELS.html](#page-personnels)[qui sont paramétrés avec affichage dans la liste des commerciaux](#page-personnels) |
| E-mail |  | X |  |
| Notes |  | X | La note prend le [type d’action](#page-typesdesactions) choisi au niveau de la liste déroulante |
| Remarque : Selon les habilitations accordées à un rôle, dépend le droit de modification des champs « Action commercial », « Type » et « Note ».<br>- Si le rôle n’a pas le droit d’administrer les prospections, après l’enregistrement de la fiche, les champs cités ci-dessus seront en lecture seule,<br>- Si le rôle a le droit d’administrer les prospections, après l’enregistrement de la fiche, les champs cités ci-dessus seront toujours modifiables. |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Importer | GEN0018 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Audit | GEN0020 |
Créé avec HelpNDoc Personal Edition: [Créer des documents d'aide HTML facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

### PROSPECTS <a id="page-prospects"></a>
_Page : `PROSPECTS.html` — Module Vente_

[image : NouvelElement85.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Code | X | X | Code auto généré en lecture seule |
| Afficher comme |  | X | En lecture seule |
| Forme juridique |  | X | Liste déroulante des [formes juridique](#page-formessociale) |
| Personnel |  |  | Liste déroulante des [personnels](#page-personnels)[PERSONNELS.html](#page-personnels)[qui sont paramétrés avec affichage dans la liste des commerciaux](#page-personnels) |
| Nature |  |  | Liste déroulante de [nature](#page-natures) |
| Général (PH) |  |  |  |
| Titre |  | X | Liste déroulante de [titres](#page-titres) |
| Sexe |  | X | Liste déroulante de [sexe](#page-sexes) |
| Nom |  | X |  |
| Prénom |  | X |  |
| Pays de résidence |  |  | Liste déroulante des [pays](#page-pays) |
| Mobile |  | X |  |
| E-mail |  | X |  |
| Date de naissance |  |  |  |
| Lieu |  |  |  |
| Situation familiale |  |  | Liste déroulante des [situations familiale](#page-situationsfamiliale) |
| Enfant |  |  |  |
| Pièce d’identité |  |  | Liste déroulante de [type de pièce d’identité](#page-typespieceidentite) |
| N° |  |  |  |
| Délivrée le |  |  |  |
| Lieu |  |  |  |
| Valide jusqu’au |  |  |  |
| Lieu |  |  |  |
| Fils de |  |  |  |
| Et de |  |  |  |
| Téléphone |  |  |  |
| Adresse |  |  |  |
| Pays |  |  | Liste déroulante des [pays](#page-pays) |
| Code postal |  |  |  |
| Ville |  |  | Liste déroulante de [villes](#page-villes) |
| Nationalité |  |  | Liste déroulante de [nationalités](#page-nationalites) |
| Résidence |  |  | Liste déroulante de [résidences](#page-typesresidence) |
| Profession |  |  | Liste déroulante de [professions](#page-professions) |
| Salaire |  |  | Liste déroulante des [fourchettes salariales](#page-salaires) |
| Employeur |  |  |  |
| Ancienneté |  |  |  |
| Régime matrimonial |  |  | Liste déroulante de [régimes matrimoniaux](#page-regimesmatrimonial) |
| Général (PM, SARL, SURAL, SA, OG, ONG) |  |  |  |
| Raison sociale |  | X |  |
| Pays de résidence |  |  | Liste déroulante de [pays de résidences](#page-pays) |
| Mobile |  | X |  |
| Email |  | X |  |
| Téléphone |  |  |  |
| M.F |  |  |  |
| R.C |  |  |  |
| C.D |  |  |  |
| Activité |  |  |  |
| Capital |  |  |  |
| Adresse |  |  |  |
| Pays |  |  | Liste déroulante de [pays](#page-pays) |
| Code postal |  |  |  |
| Ville |  |  | Liste déroulante de [villes](#page-villes) |
| Nationalité |  |  | Liste déroulante de [nationalités](#page-nationalites) |
| Résidence |  |  | Liste déroulante de [résidences](#page-typesresidence) |
| Date de création |  |  |  |
| Lieu |  |  |  |
| Qualité |  |  |  |
| Autres |  |  |  |
| Assujettissement |  |  | Liste déroulante d’[assujettissements](#page-assujetissements) |
| Banque |  |  | Liste déroulante de [banques](#page-banques) |
| Agence |  |  |  |
| RIB |  |  |  |
| IBAN |  |  |  |
| Code comptable |  |  |  |
| Compte collectif |  |  |  |
| Téléphone 2 |  |  |  |
| Téléphone 3 |  |  |  |
| Fax |  |  |  |
| Site web |  |  |  |
| Est un ancien client |  |  | Un flag pour désigner si le prospect est un ancien client ou non |
| Contacts |  |  |  |
| Liste de contacts |  |  | La liste visualise les contacts en indiquant :<br>- Le contact<br>- Le [type de relation](#page-typesderelation)<br> Sur cette liste on peut effectuer les opérations suivantes :<br>- Ajouter un contact<br>- Éditer un contact<br>- Supprimer un contact |
| Notes |  |  |  |
| Champ de saisi des notes |  |  |  |
| Prospections |  |  |  |
| Liste de prospections |  |  | La liste visualise les prospections faites par un client en indiquant :<br>- Le code<br>- La date<br>- La tranche<br>- L’article<br>- La catégorie… |
| Dossier |  |  |  |
| Liste de dossiers |  |  | La liste visualise les dossiers de ventes en affichant :<br>- Le statut<br>- La tranche<br>- L’article<br>- A payer<br>- Réglé… |
| Règlements |  |  |  |
| Liste de règlements |  |  | La liste visualise les règlements en affichant :<br>- La description<br>- Le montant<br>- L’historique |
| Historique |  |  |  |
| Liste d’historique |  |  | La liste visualise l’historique de dossier de vente d’un client en affichant :<br>- L’utilisateur<br>- La date<br>- L’état<br>- Le N° Dossier<br>- L’article<br>- Les détails |
| Remarque : La combinaison des champs « Description » + « mobile » ou « Description » + « E-mail » doivent êtres uniques. |  |  |  |
Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Imprimer | GEN0011 |
| Éditer | GEN0009 |
| Exporter | GEN0012 |
| Supprimer | GEN0010 |
| Importer | GEN0018 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Créer liste à faire | GEN0023 |
Créé avec HelpNDoc Personal Edition: [Créer des documents d'aide PDF facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

### PROSPECTIONS <a id="page-prospections"></a>
_Page : `PROSPECTIONS.html` — Module Vente_

[image : NouvelElement525.png]

### Liste

| LIBELLE | V | OBSERVATIONS |
|---|---|---|
| Action commerciale | X |  |
| Alerte |  | Le système affiche la date de la dernière alerte active<br> Remarque: Le système affiche l’icône d'alerte et la date alerte selon la configuration effectuée dans l’écran des [paramètres système](#page-parametressysteme):<br> Si la date alerte - Délai d'alerte = Date de jour:<br> --> Le système affiche l'alerte<br> Sinon<br> --> Le système n'affiche pas l'alerte |
| Archivée |  |  |
| Article |  |  |
| Autres tranches |  |  |
| Avance |  |  |
| Budget à |  |  |
| Budget de |  |  |
| Catégorie article |  |  |
| Client |  | Le système affiche la description du client à partir du dossier de vente<br> Remarque: Pour une prospection abouti (a un dossier de vente), si le client est différent du prospect, on peut conclure qu'on a fait une opération de transfert client ou on a modifié directement le client à partir du dossier de vente. |
| Code |  |  |
| Commercial |  |  |
| Crédit |  |  |
| Crée le |  |  |
| Crée par |  |  |
| Date d’archivage | X | Le système archive une prospection et affecte la date après :<br>- La création d’un dossier de vente<br>- L’utilisateur archivage la prospection manuellement |
| Date de concrétisation | X |  |
| Date dernière action | X | Date de la dernière communication ajoutée automatiquement à partir du champ note ou la date de la dernière communication ajoutée par un utilisateur |
| Date dernière communication | X | Date de la dernière communication ajoutée par un utilisateur |
| Date dernière visite | X | Date de la dernière action de type visite |
| Date première visite | X | Date de la première action de type visite |
| Date prospection | X |  |
| Délais de concrétisation |  | Date concrétisation – Date prospection |
| Délais d’inactivité | X | Date de jour – Date de la dernière action utilisateur |
| Dernier message de communication |  | Le système affiche le message de la dernière communication effectuée par un utilisateur |
| Dernier résultat de communication |  | Le système affiche le résultat de la dernière communication effectué par un utilisateur |
| Dossier de vente | X | Statut de dossier de vente |
| E mail |  |  |
| Fond propre |  |  |
| Mobile |  |  |
| Modifié le |  |  |
| Modifié par |  |  |
| Nature |  |  |
| Nature prospect |  |  |
| Nombre de visites | X | Nombre d’actions de type visite |
| Nombre d’actions | X |  |
| Note raison d’archivage | X |  |
| Notes |  |  |
| Parrain | X |  |
| Présentation article |  |  |
| Prospect | X | Le système affiche la description du prospect à partir des prospections |
| Prospect code |  |  |
| Raison d’alerte | X | Si l’alerte est créée par un utilisateur, le système affiche « Alerte action utilisateur», si l’alerte est auto générée par le système, il affiche « Délais d’inactivité de X jours »<br> Remarque : Le nombre de jours est paramétré dans l’écran « [Natures prospections](#page-naturesprospections) » |
| Raison d’archivage | X |  |
| Source de contact | X |  |
| Superficie (m²) à |  |  |
| Superficie (m²) de |  |  |
| Support source de contact | X |  |
| Téléphone |  |  |
| Tranche | X |  |
| Type article |  |  |
| Type de prospection | X |  |
| Type tiers | X |  |
| Ville |  |  |
| Vocation |  |  |

### Fiche

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Code | X | X | Code auto généré en lecture seule |
| Date |  | X |  |
| Prospect |  | X | Sélecteur de [prospect](#page-prospects) |
| Tranche |  | X | Liste de déroulante de [tranches](#page-cfg-tranches) visible dans le module vente |
| Article |  |  | Sélecteur des [articles](#page-articles)<br> Le système affiche la liste des articles libres et les articles qui ont un dossier de vente dont le statut est paramétré "Peut effectuer une prospection" |
| Commercial |  |  | Liste déroulante de [commercial](#page-personnels)[PERSONNELS.html](#page-personnels)[qui sont paramétrés avec affichage dans la liste des commerciaux](#page-personnels) |
| Action commercial |  |  | Liste déroulantes des[actions commerciales](#page-actionscommerciales) |
| Nature |  |  | Liste déroulante de [natures](#page-naturesprospections) |
| Source de contact |  | X | Liste déroulante de [source de contact](#page-sourcedecontact) |
| Type |  |  | Liste déroulante de [type](#page-typesdesactions) |
| Support |  |  | Liste déroulante de [supports](#page-supportssourcedecontact) |
| Note |  |  | Le champ note prend le type d’action choisi dans la liste déroulante des types. |
| Archivée |  |  | En cliquant sur le bouton archiver, le système affiche un masque dans lequel on :<br>- choisit la raison d’archivage<br>- Saisit une note |
| Autre tranches |  |  |  |
| Liste des autres tranches |  |  | Le système affiche une liste de projet en visualisant :<br>- Le projet<br>- Les champs crée le, crée par, modifier le et modifier par<br> Aussi, dans cette liste on peut effectuer les opérations suivantes<br>- Ajouter un projet<br>- Supprimer un projet |
| Profile |  |  |  |
| Articles |  |  | [Catégorie](#page-stk-categories-item) articles |
|  |  | [Types](#page-stk-types-item) articles |  |
| Sous articles |  |  | [Catégorie](#page-stk-categories-item) sous articles |
|  |  | [Types](#page-stk-types-item) sous articles |  |
| Actions |  |  |  |
| Liste des actions |  |  | La liste visualise le nombre d’actions groupées par [type](#page-typesdesactions) |
| Autres |  |  |  |
| Superficie (m²) de |  |  |  |
| A |  |  |  |
| Budget de |  |  |  |
| A |  |  |  |
| Avance |  |  |  |
| Crédit bancaire |  |  |  |
| Fond propre |  |  |  |
| Parrain |  |  |  |
| Remarque 1: Selon les habilitations accordées à un rôle, dépend le droit de modification des champs « Action commercial », « Type » et « Note ».<br>- Si un rôle n’a pas le droit d’administrer les prospections, après l’enregistrement de la fiche, les champs cités ci-dessus seront en lecture seule,<br>- Si un rôle a le droit d’administrer les prospections, après l’enregistrement de la fiche, les champs cités ci-dessus seront toujours modifiables.<br>Remarque 2 : Le système contrôle l’unicité de la prospection en vérifiant les trois champs prospect, tranche et article:<br>- S’il existe une prospection avec le même prospect, la même tranche et le même article, le système affiche un message d’erreur « Il existe déjà un autre enregistrement avec les même données de base ! »<br>- S’il existe une prospection avec le même prospect, la même tranche et un article vide, le système affiche un message d’erreur « Il existe déjà un autre enregistrement avec les même données de base ! »<br>- S’il existe une prospection avec le même prospect, la même tranche et un article différent, le système affiche un message d’information « Il existe un autre enregistrement avec les données de bases suivantes :<br>- Entité : « Description entité »<br>- Tranche : « Description tranche »<br>- Article : « Description article » Voulez-vous continuez ?<br>- Si on clique sur « Oui » : Le système enregistre la prospection<br>- Si on clique sur « Non » : Le système n’enregistre pas la prospection |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Imprimer | GEN0011 |
| [Créer/ Ouvrir un dossier](#page-dossiersdevente) | En cliquant sur le bouton « Créer/ Ouvrir un dossier » le système créé un nouveau dossier de vente et il archive la prospection, sinon il ouvre le dossier correspondant.<br>Remarques: Si l'article choisi a un dossier de vente, le système affiche le message suivant:<br>[image : NouvelElement734.png] |
| Archiver | En cliquant sur bouton « Archiver » le système affecte une date d’archivage et il désactive toutes les relances associées à cette prospection. |
| Créer liste à faire | GEN0023 |
| Instruction | L'action instruction permet aux utilisateurs de notifier le commercial de la prospection en envoyant un mail et en affichant une icône dans la liste des prospections<br> Remarques:<br> - L’icône ne sera affiché que pour le commercial concerné.<br> - L'icône sera supprimer lorsque le commercial concerné ouvre l'écran des actions |
Créé avec HelpNDoc Personal Edition: [Créer facilement des fichiers Qt Help](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

### CLIENTS <a id="page-clients"></a>
_Page : `CLIENTS.html` — Module Vente_

[image : NouvelElement178.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Code | X | X | Code auto généré en lecture seule |
| Afficher comme |  | X | En lecture seule |
| Forme juridique |  | X | Liste déroulante des [formes juridique](#page-formessociale) |
| Personnel |  |  | Liste déroulante de [personnels](#page-personnels)[PERSONNELS.html](#page-personnels)[qui sont paramétrés avec affichage dans la liste des commerciaux](#page-personnels) |
| Nature |  |  | Liste déroulante de [nature](#page-natures) |
| Général (PH) |  |  |  |
| Titre |  | X | Liste déroulante de [titres](#page-titres) |
| Sexe |  | X | Liste déroulante de [sexe](#page-sexes) |
| Nom |  | X |  |
| Prénom |  | X |  |
| Pays de résidence |  |  | Liste déroulante des [pays](#page-pays) |
| Mobile |  | X |  |
| E-mail |  | X |  |
| Date de naissance |  |  |  |
| Lieu |  |  |  |
| Situation familiale |  |  | Liste déroulante des [situations familiale](#page-situationsfamiliale) |
| Enfant |  |  |  |
| Pièce d’identité |  |  | Liste déroulante de [type de pièce d’identité](#page-typespieceidentite) |
| N° |  |  |  |
| Délivrée le |  |  |  |
| Lieu |  |  |  |
| Valide jusqu’au |  |  |  |
| Lieu |  |  |  |
| Fils de |  |  |  |
| Et de |  |  |  |
| Téléphone |  |  |  |
| Adresse |  |  |  |
| Pays |  |  | Liste déroulante des [pays](#page-pays) |
| Code postal |  |  |  |
| Ville |  |  | Liste déroulante de [villes](#page-villes) |
| Nationalité |  |  | Liste déroulante de [nationalités](#page-nationalites) |
| Résidence |  |  | Liste déroulante de [résidences](#page-typesresidence) |
| Profession |  |  | Liste déroulante de [professions](#page-professions) |
| Salaire |  |  | Liste déroulante des [fourchettes salariales](#page-salaires) |
| Employeur |  |  |  |
| Ancienneté |  |  |  |
| Régime matrimonial |  |  | Liste déroulante de [régimes matrimoniaux](#page-regimesmatrimonial) |
| Général (PM, SARL, SURAL, SA, OG, ONG) |  |  |  |
| Raison sociale |  | X |  |
| Pays de résidence |  |  | Liste déroulante de [pays de résidences](#page-pays) |
| Mobile |  | X |  |
| Email |  | X |  |
| Téléphone |  |  |  |
| M.F |  |  |  |
| R.C |  |  |  |
| C.D |  |  |  |
| Activité |  |  |  |
| Capital |  |  |  |
| Adresse |  |  |  |
| Pays |  |  | Liste déroulante de [pays](#page-pays) |
| Code postal |  |  |  |
| Ville |  |  | Liste déroulante de [villes](#page-villes) |
| Nationalité |  |  | Liste déroulante de [nationalités](#page-nationalites) |
| Résidence |  |  | Liste déroulante de [résidences](#page-typesresidence) |
| Date de création |  |  |  |
| Lieu |  |  |  |
| Qualité |  |  |  |
| Autres |  |  |  |
| Assujettissement |  |  | Liste déroulante d’[assujettissements](#page-assujetissements) |
| Banque |  |  | Liste déroulante de [banques](#page-banques) |
| Agence |  |  |  |
| RIB |  |  |  |
| IBAN |  |  |  |
| Code comptable |  |  |  |
| Compte collectif |  |  |  |
| Téléphone 2 |  |  |  |
| Téléphone 3 |  |  |  |
| Fax |  |  |  |
| Site web |  |  |  |
| Contacts |  |  |  |
| Liste de contacts |  |  | La liste visualise les contacts en indiquant :<br>- Le contact<br>- Le [type de relation](#page-typesderelation)<br> Sur cette liste on peut effectuer les opérations suivantes :<br>- Ajouter un contact<br>- Éditer un contact<br>- Supprimer un contact |
| Notes |  |  |  |
| Champ de saisi des notes |  |  |  |
| Prospections |  |  |  |
| Liste de prospections |  |  | La liste visualise les prospections faites par un client en indiquant :<br>- Le code<br>- La date<br>- La tranche<br>- L’article<br>- La catégorie… |
| Dossier |  |  |  |
| Liste de dossiers |  |  | La liste visualise les dossiers de ventes en affichant :<br>- Le statut<br>- La tranche<br>- L’article<br>- A payer<br>- Réglé… |
| Règlements |  |  |  |
| Liste de règlements |  |  | La liste visualise les règlements en affichant :<br>- La description<br>- Le montant<br>- L’historique |
| Historique |  |  |  |
| Liste d’historique |  |  | La liste visualise l’historique de dossier de vente d’un client en affichant :<br>- L’utilisateur<br>- La date<br>- L’état<br>- Le N° Dossier<br>- L’article<br>- Les détails |
| Remarque : La combinaison des champs « Description » + « mobile » ou « Description » + « E-mail » doivent êtres uniques. |  |  |  |
Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Imprimer | GEN0011 |
| Éditer | GEN0009 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
Créé avec HelpNDoc Personal Edition: [Documentation Qt Help facile](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creez-des-fichiers-daide-pour-le-framework-qt-help)

### PLANS <a id="page-plans"></a>
_Page : `PLANS.html` — Module Vente_

[image : NouvelElement735.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Projet |  |  | Liste déroulante des [tranches](#page-cfg-tranches)<br> On choisissant une tranche, le système charge la liste des étages et leurs plans correspondants et les statuts des articles |
| Statut |  |  |  |
| Catégorie |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Cliquer | Si on clique sur un article libre, le système ouvre la fiche prospect express sinon le système ouvre le dossier de vente correspondant |
Créé avec HelpNDoc Personal Edition: [Produire des livres Kindle gratuitement](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creer-ebooks-pour-kindle-amazon)

### DOSSIERS DE VENTE <a id="page-dossiersdevente"></a>
_Page : `DOSSIERSDEVENTE.html` — Module Vente_

[image : NouvelElement838.bmp]

### Liste

| LIBELLE | V | OBSERVATIONS |
|---|---|---|
| Action commerciale | X | Action commerciale à partir de la [prospection](#page-prospections) |
| Alerte | X | Le système affiche la date de la dernière alerte active<br> Remarque: Le système affiche l’icône d'alerte et la date alerte selon la configuration effectuée dans l’écran des [paramètres système](#page-parametressysteme):<br> Si la date alerte - Délai d'alerte = Date de jour:<br> --> Le système affiche l'alerte<br> Sinon<br> --> Le système n'affiche pas l'alerte |
| Autofinancement |  | Somme des lignes de schéma de financement qui sont paramétrées dans la [trésorerie](#page-tresorerie-com) de module vente "Auto-financement" |
| Autofinancement réglé |  | Somme des lignes de schéma de financement qui sont paramétrées dans la [trésorerie](#page-tresorerie-com) de module vente "Auto-financement " réglé |
| Autofinancement reste |  | Autofinancement - Autofinancement réglé |
| Avance |  | Somme des lignes de schéma de financement qui sont paramétrées dans la [trésorerie](#page-tresorerie-com) de module vente "Avance" |
| Avance réglée |  | Somme des lignes de schéma de financement qui sont paramétrées dans la [trésorerie](#page-tresorerie-com) de module vente "Avance " réglé |
| Avance reste |  | Avance - Avance réglée |
| Banque Crédit / Leasing |  |  |
| Bloc |  | Description du bloc |
| Cat. |  | Catégorie Article |
| Client | X | Le système affiche le client du dossier de vente |
| Client veut choisir le Cadeau Plus Tard |  |  |
| Code | X | Code Article |
| Commercial | X | Par défaut le commercial qui a crée le dossier de vente |
| Confirmé |  |  |
| Crédit |  | Somme des lignes de schéma de financement qui sont paramétrées dans la [trésorerie](#page-tresorerie-com) de module vente "Crédit" |
| Crédit réglé |  | Somme des lignes de schéma de financement qui sont paramétrées dans la [trésorerie](#page-tresorerie-com) de module vente "Crédit " réglé |
| Crédit reste |  | Crédit - Crédit réglé |
| Crée le |  |  |
| Crée par |  |  |
| Date attestation de consignation/ octroi crédit | X |  |
| Date compromis / Approbation crédit | X |  |
| Date concrétisation | X |  |
| Date décision MEILLEUR TAUX | X |  |
| Date dernière communication |  |  |
| Date enregistrement du Contrat |  |  |
| Date d’envoi MEILLEUR TAUX | X |  |
| Date Envoi Notaire / Avocat | X |  |
| Date envoi notaire prévisionnelle |  |  |
| Date expédition | X |  |
| Date fiche Assurance |  |  |
| Date fiche Bancaire | X |  |
| Date fiche Intervention | X |  |
| Date minute / Signature Contrat | X |  |
| Date Permis d’Habiter | X |  |
| Date Promesse de vente |  |  |
| Date Prospection | X |  |
| Date Relance 1 |  |  |
| Date Relance 2 |  |  |
| Date Remise de clés | X |  |
| Date Remise de clés prévisionnelle |  |  |
| Date Réservation | X |  |
| Date Visite Technique/ Réception | X |  |
| Décision MEILLEUR TAUX | X |  |
| Délai MEILLEUR TAUX | X |  |
| Délais Concrétisation – Envoi Notaire | X | Délais en jours |
| Délais Envoi au Notaire – Crédit accordé | X | Délais en jours |
| Délais Envoi au Notaire – Remise des clés | X | Délais en jours |
| Délais Prospection – Concrétisation |  | Délais en jours |
| Délais remise des Clés –Crédit accordé | X | Délais en jours |
| Dernier message de communication |  | Dernier message d’une action ajoutée par un utilisateur<br> Remarque: Si le message est vide le système affiche dans cette colonne le dernier résultat de communication. |
| Dernier résultat de communication |  | Dernier résultat d’une action ajoutée par un utilisateur |
| Description Article |  |  |
| Entité |  | Description de l'entité juridique |
| Entité code |  | Code de l'entité juridique |
| Est Un Ancien Client |  |  |
| Étage |  | Code étage |
| Frais |  | Somme des lignes de schéma de financement qui sont paramétrées dans la [trésorerie](#page-tresorerie-com) de module vente "Frais" |
| Frais réglé |  | Somme des lignes de schéma de financement qui sont paramétrées dans la [trésorerie](#page-tresorerie-com) de module vente "Frais" réglé |
| Frais reste |  | Frais - Frais réglé |
| Gestionnaire | X |  |
| Mobile |  |  |
| Modifications |  | Somme des lignes de schéma de financement qui sont paramétrées dans la [trésorerie](#page-tresorerie-com) de module vente "Modifications" |
| Modifications réglées |  | Somme des lignes de schéma de financement qui sont paramétrées dans la [trésorerie](#page-tresorerie-com) de module vente "Modifications" réglée |
| Modifications restes |  | Modifications - Modifications réglées |
| Modifier le |  |  |
| Modifier par |  |  |
| Montant Remise |  |  |
| Net à payer | X | Prix de vente après remise + le montant des documents de type frais |
| Net à payer réglé | X | Prix de vente réglé après remise + le montant des documents de type frais |
| Net à payer reste | X | Net à payer - Net à payer réglé |
| Nombre d’actions | X |  |
| Nombre des Cadeaux |  |  |
| Notaire / Avocat |  |  |
| Pénalité |  | Somme des lignes de schéma de financement qui sont paramétrées dans la [trésorerie](#page-tresorerie-com) de module vente "Pénalité" |
| Pénalité réglée |  | Somme des lignes de schéma de financement qui sont paramétrées dans la [trésorerie](#page-tresorerie-com) de module vente "Pénalité" réglée |
| Pénalité reste |  | Pénalité - Pénalité réglée |
| Prix Article HT |  | Prix article HT avant remise * (1- taux de remise) |
| Prix Article HT avant remise |  | Prix de l'article HT remise |
| Prix Article TTC |  | Prix Article HT * Taux TVA (%) |
| Prix Article TTC avant remise |  | Prix de l'article TTC avant remise |
| Prix Contractuel |  | Somme des lignes de schéma de financement qui sont paramétrées dans la trésorerie "Auto-financement" ou "Crédit" ou "Avance" |
| Prix Contractuel Réglé |  | Somme de règlements des lignes de schéma de financement qui sont paramétrées dans la trésorerie "Auto-financement" ou "Crédit" ou "Avance" |
| Prix Contractuel Reste |  | Prix Contractuel - Prix Contractuel Réglé |
| Prix de vente | X | Prix de vente après remise<br> = Somme des lignes qui sont paramétrées "autofinancement" dans la trésorerie de module vente + Somme des lignes qui sont paramétrées "avance" dans la trésorerie de module vente + Somme des lignes qui sont paramétrées "Crédit" dans la trésorerie de module vente + Somme des lignes qui sont paramétrées "Modification" dans la trésorerie de module vente |
| Prix de vente HT |  | Prix article HT + Prix sous articles HT |
| Prix de vente HT avant remise |  | Prix article HT avant remise + Prix sous articles HT avant remise |
| Prix de vente m² HT |  | Prix total de vente HT / superficie de l'article |
| Prix de vente m² TTC |  | Prix total de vente TTC / superficie de l'article |
| Prix de vente réglé | X | Prix de vente réglé<br> = Somme des lignes qui sont paramétrées "autofinancement" réglé dans la [trésorerie](#page-tresorerie-com) de module vente + Somme des lignes qui sont paramétrées "avance" réglé dans la trésorerie de module vente + Somme des lignes qui sont paramétrées "Crédit" réglé dans la [trésorerie](#page-tresorerie-com) de module vente + Somme des lignes qui sont paramétrées "Modification" réglé dans la trésorerie de module vente |
| Prix de vente reste | X | Prix de vente - Prix de vente réglé |
| Prix de vente TTC avant remise |  | Prix articleTTC avant remise + Prix sous articles TTC avant remise |
| Prix du m² HT |  | Montant Remise = Prix du m² HT avant remise * Pourcentage Remise<br> Prix du m² HT = Prix du m² HT avant remise - Montant Remise |
| Prix du m² HT avant remise |  | Prix d'un article de 1m² HT avant remise à partir de module stock |
| Prix du m² TTC |  | Prix du m² HT * Taux TVA (%) |
| Prix du m² TTC avant remise |  | Prix d'un article de 1m² Tout Taxe Compris avant remise à partir de module stock |
| Prix Sous Articles HT |  | Prix Sous Articles HT avant remise * (1- taux de remise) |
| Prix Sous Articles HT avant remise |  | Prix de sous article HT avant remise |
| Prix Sous Articles TTC |  | Prix de sous article HT * Taux TVA (%) |
| Prix Sous Articles TTC avant remise |  | Prix de sous article TTC avant remise |
| Programme |  | Description Programme |
| Programme code |  | Code de Programme |
| Remise % |  | Montant remise / Total HT |
| Source de contact | X |  |
| Statut |  |  |
| Sup. (m²) | X | Surface en m² |
| Support source de contact | X |  |
| Taux Avance de base | X | ∑ (des documents de type avance) / Prix de vente * 100 |
| Taux Avance réel |  | ∑ (des montants réglés des documents de type avance) / (∑ des documents de type avance) * 100 |
| Taux de Concrétisation de base | X | ∑ (des documents de type "Dans le cadre de la concrétisation") / Prix de vente * 100 |
| Taux de Concrétisation réel | X | ∑ ( montants réglés des documents de type "Dans le cadre de la concrétisation" ) / ∑ (des documents de type "Dans le cadre de la concrétisation") *100 |
| Tél. |  |  |
| Titre Foncier |  |  |
| Tranche | X |  |
| Tranche code |  |  |
| Trop Perçu |  | Montant affecté - montant à payer : Le plus que le client a payé |
| Trop Perçu réglé |  |  |
| Trop Perçu reste |  |  |
| TVA |  | Taxe sur Valeur Ajoutée |
| Type |  |  |
| Type Présentation | X |  |

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| N° dossier |  |  | Numéro de dossier auto généré en lecture seule |
| Date |  |  |  |
| Client |  |  | Sélecteur de [clients](#page-clients) |
| Commercial |  |  | Liste déroulante de [personnel](#page-personnels)[s](#page-personnels)[PERSONNELS.html](#page-personnels)[qui sont paramétrés avec affichage dans la liste des commerciaux](#page-personnels) |
| Prospection |  |  | Afficher fiche « [Prospection](#page-prospections)» |
| Statut / validité |  |  | Sélecteur des [Statuts](#page-statuts-com)<br> Remarque1 : Les champs obligatoires varient selon le [statut](#page-statuts-com) du dossier de vente.<br> Remarque2 : Les champs obligatoires pour chaque [transition](#page-transitions-com) sont pré-paramétrés dans le sous module « [statut](#page-statuts-com)»<br> Remarque3 : Les [rôles](#page-roles) des acteurs qui ont les privilèges de modifier un statut sont pré-paramétrés dans le sous module « [Transition](#page-transitions-com)»<br> Remarque4 : Si la durée de vie d’un [statut](#page-statuts-com) égale à zéro, le champ validité sera en lecture seule. |
| Projet |  |  | En lecture seule |
| Article |  |  | Afficher la fiche « [Article](#page-articles)» |
| Catégorie |  |  | En lecture seule |
| Titre foncier |  |  | En lecture seule |
| Présentation |  |  | En lecture seule |
| Type |  |  | En lecture seule |
| Superficie (m²) |  |  | En lecture seule |
| Prix du m² |  |  | En lecture seule |
| Prix article |  |  | En lecture seule |
| Prix sous articles |  |  | En lecture seule |
| Total HT |  |  | En lecture seule |
| Total TTC |  |  | En lecture seule |
| Remise |  |  | En lecture seule<br> Montant remise / Total HT |
| Remise |  |  |  |
| Prix de vente HT |  |  | En lecture seule<br> Prix article HT après remise + Prix sous article HT après remise |
| Prix de vente TTC |  |  | En lecture seule,<br> Prix de vente HT * taux TVA |
| Prix contractuel |  |  | Σ des montants des lignes de schéma de financement qui ne sont pas paramétrées "Exclure ce montant du prix contractuel" |
| Montants (Montant) |  |  |  |
| Frais |  |  | En lecture seule,<br> Σ des montants des lignes du schéma de financement qui sont paramétrées "Frais" dans la trésorerie de module vente. |
| Modifications |  |  | En lecture seule,<br> Σ des montants des lignes du schéma de financement qui sont paramétrées "Modifications" dans la trésorerie de module vente. |
| Prix de vente |  |  | En lecture seule, |
| Avance |  |  | En lecture seule,<br> Σ des montants des lignes du schéma de financement qui sont paramétrées "Avance" dans la trésorerie de module vente |
| Crédit |  |  | En lecture seule,<br> Σ des montants des lignes du schéma de financement qui sont paramétrées "Crédit" dans la trésorerie de module vente |
| Autofinancement |  |  | En lecture seule,<br> Σ des montants des lignes du schéma de financement qui sont paramétrées "Autofinancement" dans la trésorerie de module vente |
| Pénalité |  |  | En lecture seule,<br> Σ des montants des lignes du schéma de financement qui sont paramétrées "Pénalité" dans la trésorerie de module vente |
| Résiliation |  |  | En lecture seule,<br> Σ des montants des lignes du schéma de financement qui sont paramétrées "Résiliation" dans la trésorerie de module vente |
| Net à payer |  |  | En lecture seule,<br> Montant prix de vente + Montant frais + Montant modification |
| Montants (Réglé) |  |  |  |
| Frais |  |  | En lecture seule,<br> Σ des montants de règlements des lignes du schéma de financement qui sont paramétrées "Frais" dans la trésorerie de module vente. |
| Modifications |  |  | En lecture seule,<br> Σ des montants de règlements des lignes du schéma de financement qui sont paramétrées "Modifications" dans la trésorerie de module vente. |
| Prix de vente |  |  | En lecture seule, |
| Avance |  |  | En lecture seule,<br> Σ des montants de règlements des lignes du schéma de financement qui sont paramétrées "Avance" dans la trésorerie de module vente. |
| Crédit |  |  | En lecture seule,<br> Σ des montants de règlements des lignes du schéma de financement qui sont paramétrées "Crédit" dans la trésorerie de module vente. |
| Autofinancement |  |  | En lecture seule,<br> Σ des montants de règlements des lignes du schéma de financement qui sont paramétrées "Autofinancement" dans la trésorerie de module vente. |
| Pénalité |  |  | En lecture seule,<br> Σ des montants de règlements des lignes du schéma de financement qui sont paramétrées "Pénalité" dans la trésorerie de module vente. |
| Résiliation |  |  | En lecture seule,<br> Σ des montants de règlements des lignes du schéma de financement qui sont paramétrées "Résiliation" dans la trésorerie de module vente. |
| Net à payer |  |  | En lecture seule,<br> Σ des montants de règlements des lignes du schéma de financement |
| Montants (Reste) |  |  |  |
| Frais |  |  | En lecture seule,<br> Montant Frais - Montant Frais réglé |
| Modifications |  |  | En lecture seule,<br> Montant Modification - Montant Modification réglé |
| Prix de vente |  |  | En lecture seule, |
| Avance |  |  | En lecture seule,<br> Montant Avance - Montant Avance réglé |
| Crédit |  |  | En lecture seule,<br> Montant Crédit - Montant Crédit réglé |
| Autofinancement |  |  | En lecture seule,<br> Montant Autofinancement - Montant Autofinancement réglé |
| Pénalité |  |  | En lecture seule,<br> Montant Pénalité - Montant Pénalité réglé |
| Résiliation |  |  | En lecture seule,<br> Montant Résiliation - Montant Résiliation réglé |
| Net à payer |  |  | En lecture seule,<br> Montant Net à payer - Montant Net à payer réglé |
| Autres Montants |  |  |  |
| Sup (m²) |  |  | En lecture seule,<br> Superficie en m² |
| TVA |  |  | Pourcentage TVA |
| Prix article m² HT |  |  | En lecture seule,<br> Prix d'un article de 1m² HT à partir de module stock |
| Prix article m² TTC |  |  | En lecture seule,<br> Prix d'un article de 1m² HT * Taux TVA (%) |
| Prix article HT |  |  | En lecture seule,<br> Prix de l'article HT à partir de module stock |
| Prix article TTC |  |  | En lecture seule,<br> Prix de l'article HT * Taux TVA (%) |
| Prix sous articles HT |  |  | En lecture seule,<br> Prix des sous articles HT |
| Prix sous articles TTC |  |  | En lecture seule,<br> Prix des sous articles HT * Taux TVA (%) |
| Total HT |  |  | En lecture seule,<br> Prix de l'article HT + Prix des sous articles HT |
| Total TTC |  |  | En lecture seule,<br> Prix de l'article TTC + Prix des sous articles TTC |
| Remise |  |  | En lecture seule,<br> Montant remise / Total HT |
| Prix de vente HT |  |  | En lecture seule,<br> Prix article HT après remise + Prix sous article HT après remise |
| Prix de vente TTC |  |  | En lecture seule,<br> Prix article TTC après remise + Prix sous article TTC après remise |
| Prix de vente m² HT |  |  | En lecture seule,<br> Prix total de vente HT / superficie de l'article |
| Prix de vente m² TTC |  |  | En lecture seule,<br> Prix total de vente TTC / superficie de l'article |
| Dates |  |  |  |
| Concrétisation |  |  |  |
| Promesse de vente |  |  |  |
| Visite technique / réception |  |  | Lorsqu'on clique sur le bouton situé à gauche du champ, le système affiche le calendrier des visites techniques |
| Envoie au notaire prévisionnel |  |  |  |
| Confirmé |  |  |  |
| Date enregistrement contrat |  |  |  |
| Envoie au notaire / avocat |  |  |  |
| Minute / Enregistrement du contrat |  |  |  |
| Expédition / Certificat de propriété |  |  |  |
| Relance 1 titre foncier |  |  |  |
| Échéance |  |  |  |
| Relance 2 titre foncier |  |  |  |
| Échéance |  |  |  |
| Fiche bancaire |  |  |  |
| Compromis / approbation crédit |  |  |  |
| Attestation de consi. |  |  |  |
| Intervention |  |  |  |
| Assurance |  |  |  |
| Remise des clés prévues |  |  |  |
| Remise des clés effectives |  |  |  |
| Permis d’habiter |  |  | En lecture seule<br> A partir de la [tranche](#page-cfg-tranches) |
| Acte de précision |  |  |  |
| Annulation / Résiliation |  |  |  |
| Annulation / résiliation |  |  |  |
| Causes |  |  |  |
| Autre |  |  |  |
| Gestionnaire |  |  | Prend le personnel de l'utilisateur qui a mis le dossier dans un statut de finalisation<br> Remarque: Les statuts de finalisation sont les statuts >= "Minute/contrat" (sans tenir compte les statuts d'annulation et de désistement) |
| TVA |  |  |  |
| Banque de crédit |  |  | Sélecteur des [banques](#page-banques) |
| Agence de crédit |  |  |  |
| Notaire / Avocat |  |  | Sélecteur de [Notaire/ Avocat](#page-organismes) |
| Note |  |  |  |
| C’est un ancien client |  |  | Case à cocher pour appliquer les remises pour les anciens clients. |
| Client veut choisir le cadeau plus tard |  |  | Case à cocher |
| Intégrer le montant des modifications dans le calcul du prix de vente |  |  | Case à cocher<br> Si la case est coché, le système ajoute le montant des modifications au prix de vente. |
| Date d’envoi MEILLEURTAUX |  |  |  |
| Décision MEILLEURTAUX |  |  |  |
| Date décision MEILLEURTAUX |  |  |  |
| Schéma de financement |  |  |  |
| Liste de schéma de financement |  |  | La liste visualise les documents en indiquant :<br>- La date<br>- La catégorie<br>- Le montant<br>- Le montant réglé<br>- Le montant reste à régler<br>- Le code facture<br>- La note<br> Aussi dans cette liste on peut effectuer les actions suivantes :<br>- Ajouter, éditer, supprimer un document<br>- Supprimer un document<br>- Ajouter un règlement<br>- Supprimer un règlement<br>Remarque: Si on ajoute (ou on augmente son montant) un document paramétré "Mettre à jour le schéma de financement"<br> --> Le système soustrait son montant de la ligne "Autofinancement" sinon de la linge "Crédit".<br> Si on supprime (on diminue son montant) un document paramétré " Mettre à jour le schéma de financement"<br> --> Le système ajoute son montant à la ligne "Autofinancement" sinon à la ligne "Crédit". |
| Remarque1 : Les lignes du schéma de financement sont auto-générées en fonction de la configuration effectuée dans le sous module [Tranches](#page-cfg-tranches) (les délais, les seuils de versement, les règles de gestion1, les règles de gestion2 et les remises pour les anciens clients) et en fonction de la configuration de la trésorerie. |  |  |  |
| Remarque2 : Le montant de la concrétisation est calculé de la manière suivante :<br> Si c’est un nouveau client<br>- Le système applique le taux définit dans le seuil de versement de la tranche, sinon le taux définit dans le statut.<br>- A la création des lignes du schéma de financement le système utilise le taux définit dans les règles de gestion1 de la [tranche](#page-cfg-tranches)<br> Si c’est un ancien client<br>- Le système applique le taux définit dans le remise pour les anciens clients de la [tranche](#page-cfg-tranches).<br>Remarque3: Si le "Seuil de versement lors de la concrétisation" > Seuil de concrétisation (paramétré sous l'onglet seuil de versement de la tranche ) ou le seuil de concrétisation du statut, le système utilise le seuil définit dans la tranche(sous l'onglet "Seuil de versements) sinon le seuil définit dans la configuration de statut de concrétisation. |  |  |  |
| Sous articles |  |  |  |
| Liste de sous articles |  |  | La liste visualise les sous articles affectés à un article en indiquant :<br>- Le code<br>- La catégorie<br>- Le type<br>- Superficie (m²)<br>- Affecté par défaut …<br> Aussi dans cette liste on peut effectuer les actions suivantes :<br>- Ajouter un sous article<br>- Supprimer un article<br> Remarque: Lorsqu'on ajoute ou on supprime un sous article, le système ne modifie pas le montant remise, mais il modifie le taux de remise. |
| Acquéreurs |  |  |  |
| Liste des acquéreurs |  |  | La liste visualise les acquéreurs d’un article en indiquant :<br>- Le tiers<br>- La répartition (en pourcentage)<br> Aussi, dans cette liste on peut effectuer les actions suivantes :<br>- Ajouter un acquéreur<br>- Éditer la fiche d’un acquéreur<br>- Supprimer un acquéreur |
| Documents à joindre |  |  |  |
| Liste des documents à joindre |  |  | La liste visualise les types des fichiers attachés<br> Dans cette liste, on peut faire les actions suivantes<br>- Ajouter un document<br>- Supprimer un document |
| Modifications |  |  |  |
| Liste de modifications |  |  | La liste visualise les modifications apportées à un article en indiquant :<br>- L’état<br>- La date de mande<br>- La description …<br> Aussi dans cette liste on peut effectuer les opérations suivantes :<br>- Ajouter une demande de modification<br>- Éditer une demande de modification<br>- Supprimer une demande de modification<br>Remarque: Lorsque l'utilisateur ajoute une nouvelle demande de modification, le système l'ajoute automatiquement dans l'écran "[Modification](#page-modifications)" de module projet. |
| Cadeaux |  |  |  |
| Liste de cadeaux |  |  | La liste visualise :<br>- Les cadeaux<br>- Les prix<br>- Les états (livré ou non livré)<br>- La disponibilité<br>- Les [raisons](#page-raisonsdecadeaux)<br>- La date de bon d'achat<br>- La date livraison<br>- La note<br> Aussi dans cette liste on peut effectuer les opérations suivantes (selon les habilitations) ;<br>- Ajouter un cadeau<br>- Supprimer un cadeau<br> Remarque: Si la case livré est coché, le champ date livraison devient obligatoire. |
| Actions |  |  |  |
| Liste des actions |  |  | La liste visualise le nombre d’actions groupées par [type](#page-typesdesactions) |
| Dépenses |  |  |  |
| Liste des dépenses |  |  | La liste visualise les dépenses de vente en indiquant:<br>- La date<br>- Le tiers<br>- Le code<br>- Le montant TTC<br>- Le montant réglé<br>- Le montant reste<br>Aussi dans cette liste on peut effectuer les opérations suivantes:<br>- Ajouter une dépense<br>- Supprimer une dépense<br>- Éditer une dépense<br>Remarque: Lorsqu'on ajoute une dépense, le système ajoute automatiquement un [document à régler](#page-documentsaregler). |
| Historique |  |  |  |
| Liste(Historique) |  |  | La liste visualise l’historique du dossier de vente en indiquant :<br>- L’utilisateur<br>- La date<br>- L’état … |

### Calendrier
[image : NouvelElement454.png]
A partir de cet calendrier, on peut planifier les visites techniques

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Titre |  |  | En lecture seule |
| De |  |  | Date et heure début de la visite technique |
| à |  |  | Date et heure fin de la visite technique |
| Note |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | [<br>](#page-parametressysteme) |
| Annuler | GEN0006 |
| Enregistrer | A l'enregistrement, le système vérifie le délai minimum pour la programmation d'une visite technique et le nombre maximum de visite par jour, si le saisi est validé le système enregistre la visite, il envoie un mail au responsable de service après vente paramétré dans la tranche et il met en copie le directeur technique du programme, sinon;<br>- Si le délai minimum pour la programmation d'une visite technique n'est pas respecté, le système affiche un message d'erreur " Vous devez programmer la visite technique XX heures à l'avance".<br>- Si le nombre maximum de visite par jour est supérieur au nombre de visite paramétré, le système affiche un message d'erreur " Vous ne devez pas programmer plus que XX visites technique par jour ". |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Éditer | GEN0009 |
| Imprimer | GEN0011 |
| Exporter | GEN0008 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014<br>Remarque: Si l'article n'a pas un dossier de vente, le système ouvre sa fiche si l'utilisateur a le droit "Articles" coché, sinon il affiche le message "Cet article n'a pas un dossier de vente" |
| Enregistrer / Enregistrer et fermer | Enregistrer (GEN0001) / Enregistrer et fermer (GEN0002)<br>Remarque1:<br> En cliquant sur "Enregistrer" ou "Enregistrer et fermer" le système vérifie si l’acquéreur d'article a d'autres prospections non archivées<br> Si oui,<br>- Le système affiche la liste des prospections non archivées (comme elle indique la capture ci dessous)<br>[image : NouvelElement596.png]<br>- Si l'utilisateur coche la (les) prospection(s) et il clic sur "Valider", le système (les) l'archive(s) et il continue l'opération initiale.<br> - Si l'utilisateur clic sur "Annuler",le système n'archive pas la (les) prospection(s), il ferme la fenêtre et il continue l’opération initiale.<br> Si non,<br>- Le système n'affiche pas la fenêtre<br>Enregistrer (GEN0001) / Enregistrer et fermer (GEN0002)<br>Remarque2:<br> En cliquant sur "Enregistrer" ou "Enregistrer et fermer", le système vérifie s'il y a des documents à attacher paramétrés dans le [statut](#page-statuts-com)[STATUTS_COM.html](#page-statuts-com)actuel de dossier de vente<br> Si oui<br>- Le système affiche la liste des types des documents à joindre (comme elle indique la capture suivante)<br>[image : NouvelElement597.png]<br> - Si l'utilisateur coche des documents à joindre et il clic "Valider", le système considère les fichier sélectionnés comme attachés et il continue l'opération.<br> - Si l'utilisateur clic sur "Annuler", le système annule l'opération d'attachement des fichiers et il continue l'opération initial.<br>==>N.B: Si un document de la liste est paramétré avec titre (dans la configuration des [statuts](#page-statuts-com)) et le champ "Date octroi des titres foncier" est renseigné dans la [tranche](#page-cfg-tranches), le système rend l'attachement de ce dernier obligatoire.<br> Si le document avec titre est coché<br>- Le système continue l'opération en cours<br> Sinon<br>- Le système affiche le message suivant:<br>[image : NouvelElement598.png]<br>- Si l'utilisateur clic "Attacher fichier", le système applique la règle GEN0004<br>Enregistrer et fermer (GEN0002)<br>Remarque3: Le système vérifie le seuil de versement s'il est validé selon le paramétrage effectué et si la transition est paramétrée "avec proposition du passage automatique":<br> Si oui,<br>- Le système affiche; "Le dossier de vente doit passer au statut [Description statut ] (Cliquer sur OK pour modifier le statut ou Annuler pour continuer)<br> - Si on clic OK: Le système fait les vérifications selon le paramétrage effectué dans le [statut](#page-statuts-com), si il est validés, il modifie le statut, il affecte la date du règlement le plus récent dans le champ date (La date qui est paramétrée associé au [statut](#page-statuts-com) destination ) et il continue l'enregistrement en appliquant la règle GEN002, sinon il affiche le message d'erreur correspondant.<br> Remarque: Si la date est rempli manuellement par l'utilisateur, le système ne modifie pas ce dernier.<br> - Si on clic Annuler: Le système continue l'enregistrement en appliquant la règle GEN002(Sans modification du statut)<br>Sinon<br>- Le système n'affiche pas le message et il continue à l'opération en cours normalement. |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Imprimer | GEN0011 |
| Réinitialiser | Le système recharge les données de l’[article](#page-articles) et des [sous articles](#page-sousarticles) à partir du [module stock](#page-modulestock).<br>Pour l’article le système recharge:<br> - La superficie<br> - Le taux TVA<br> - Le prix unitaire HT<br>Pour les sous articles, le système recharge:<br> - L'Adresse<br> - Toujours affecté<br> - La superficie définitive<br> - La superficie provisoire<br> - Le code<br> - La contenance<br> - La description<br> - La catégorie<br> - L'orientation<br> - L'immeuble<br> - L'étage<br> - La catégorie<br> - Le type<br> - Le TVA<br> - Le prix avant remise HT |
| Compteur | Afficher le masque « compteur» |
| Ajouter | GEN0008 |
| Supprimer | GEN0010 |
| Ajouter règlement | Régler une partie ou la totalité d’un montant du schéma de financement.<br> Remarque: Si on fait le règlement d'un document de type frais, le système génère un code facture. |
| Ajouter paiement | Rembourser une partie ou la totalité d’un montant pour le client. |
| Créer liste à faire | GEN0023 |
| Instruction | L'action instruction permet aux utilisateurs de notifier le commercial du dossier de vente en envoyant un mail et en affichant une icône dans la liste des dossiers de vente.<br> Remarques:<br> - L’icône ne sera affichée que pour le commercial concerné.<br> - L'icône sera supprimer lorsque le commercial concerné ouvre l'écran des actions. |
| Modalité de paiement | Lorsqu'on clic sur " Modalité de paiement ", le système affiche le schéma de financement initialement créé.<br> Remarque:<br> - La création du schéma de financement est manuel.<br> - On peut copier le schéma de financement en cliquant sur le bouton " Copier schéma de financement ". |
| Créer/Ouvrir dossier de syndic | Lorsqu'on clic sur le bouton "Créer/Ouvrir dossier de syndic", le système Crée (ou il ouvre) un dossier de syndic pour l'article de dossier de vente en question. |

### Réajuster les superficies
[image : NouvelElement521.png]

### Liste
Le système, par défaut affiche la liste des articles des tranches choisi en indiquant:

| LIBELLE | V | OBSERVATIONS |
|---|---|---|
| Le code de dossier de vente | X | [<br>](#page-prospections) |
| Le code article | X |  |
| Le client | X |  |
| L'ancienne superficie(m²) | X |  |
| La nouvelle superficie(m²) | X |  |
| La différence entre les superficies (m²) | X | La nouvelle superficie(m²) - L'ancienne superficie(m²) |
| Le prix | X | L'ancien prix d’article |
| Le nouveau prix | X |  |
| La différence de prix | X | Nouveau prix d’article - L'ancien prix d’article |
| La notes | X |  |
| L'action | X |  |

### Action

| LIBELLE | OBSERVATIONS |
|---|---|
| Vérifier les données | Le système charge la liste des articles (ou des sous articles) dont on a fait des modifications de superficies et il remplit les détailles des colonnes cité ci dessus.<br> Remarque: Le système n'effectue des modifications que lorsqu'on change la superficie. |
| Valider | Le système met à jours les schémas de financement des dossiers de ventes qui sont cochés<br> Remarque: Le système modifie (en ajoutant ou en soustractant la différence de prix) le montant de document de type auto financement, sinon le document de type crédit. et il ajoute une note" Document modifié lors du réajustement des superficies par Utilisateur système : La description de l'utilisateur * la Date et l'heure de l'opération " dans la ligne du schéma de financement en question. |
Créé avec HelpNDoc Personal Edition: [Générateur d'aide complet](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

### TRANSFERTS PRODUIT <a id="page-transfertsproduit"></a>
_Page : `TRANSFERTSPRODUIT.html` — Module Vente_

[image : NouvelElement110.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| N° Demande |  |  | En lecture seule, auto-généré |
| Date demande |  | X | Par défaut date de jour |
| Motif |  | X | Liste déroulante de [motifs](#page-motifsdechangement) |
| Date légalisation |  | X |  |
| Date validation |  | X | Avant de procéder à la validation de l’opération, il faut saisir la « Date de validation » |
| Ancien produit |  |  |  |
| Tranche |  | X | En lecture seule |
| Code |  | X | C’est le code de l’[article](#page-articles) |
| Catégorie |  |  | En lecture seule |
| Type |  |  | En lecture seule |
| Net à payer |  |  | En lecture seule |
| Prix article |  |  | En lecture seule |
| Prix sous article |  |  | En lecture seule |
| Total |  |  | En lecture seule |
| Montant remise |  |  | En lecture seule |
| Prix de vente |  |  | En lecture seule |
| N° Dossier |  |  | En lecture seule |
| Client |  |  | En lecture seule |
| C’est un ancien client |  |  | En lecture seule |
| Observations |  |  | En lecture seule |
| Nouveau produit |  |  |  |
| Tranche |  | X | En lecture seule |
| Code |  | X | C’est le code de l’article |
| Catégorie |  |  | En lecture seule |
| Type |  |  | En lecture seule |
| Prix de vente TTC |  |  | En lecture seule |
| Réglé |  |  | En lecture seule |
| Frais |  |  | En lecture seule |
| Remise |  |  | En lecture seule |
| Frais après remise |  |  | En lecture seule |
| Reste |  |  | En lecture seule |
| Anciens sous articles |  |  |  |
| Liste d’anciens sous articles |  |  |  |
| Nouveau sous articles |  |  |  |
| Liste de nouveaux sous articles |  |  |  |

### Liste d’anciens sous articles
Champs

| LIBELLE | V | OBSERVATIONS |
|---|---|---|
| Code | X |  |
| Catégorie | X |  |
| Type | X |  |
| Superficie (m²) | X |  |
| Prix avant remise | X |  |
| Remise | X |  |
| Prix | X |  |
| Affecté par défaut | X |  |
| Notes |  |  |

### Liste de nouveaux sous articles
Champs

| LIBELLE | V | OBSERVATIONS |
|---|---|---|
| Code | X |  |
| Catégorie | X |  |
| Type | X |  |
| Superficie (m²) | X |  |
| Prix avant remise | X |  |
| Remise | X |  |
| Prix | X |  |
| Affecté par défaut | X |  |
| Notes |  |  |
Remarque 1:
Lorsqu'on effectue un transfert entre deux tranche différentes, le système met à jours les documents de type frais:

- On modifiant le(s) montant(s) des frais, si les montants entre les deux tranches sont différents
- On ajoutant des nouveaux frais, s’ils ne sont pas paramétrés dans l’ancienne tranche
- On supprimant des frais, s’ils ne sont pas paramétrés dans la nouvelle tranche( Le système garde la ligne du document mais il affecte un montant 0.00).
Remarque 2:
Lorsqu'on effectue un transfert entités (deux entités juridique différentes) le système :

- Affecte le nouveau article et leurs sous articles dans le dossier de vente
- Met à jours l’entité juridique de tous les documents du schéma de financement en affectant la nouvelle entité sauf la ligne du transfert produit (le système garde l’entité source)
- Le système garde l’entité juridique de toutes les pièces de règlements
- Tous les sous articles de l’ancien dossier ne seront pas affectés au nouveau dossier et ils seront libérés
Remarque 3:
Le système affiche que les articles dont le statut de dossier de vente est paramétré "Peut effectuer un transfert produit".

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0008 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Imprimer | GEN0011 |
| Valider | Valider le transfert produit : Cette action est gérée dans les habilitations<br>- Tous les champs seront en lecture seule.<br>- Tous les boutons seront désactivés.<br>- Le système affiche le dossier de vente en mettant à jour le prix d’article et il ajoute les frais de transfert produit. |
Créé avec HelpNDoc Personal Edition: [Outils facile d'utilisation pour créer des aides HTML et des sites web](https://www.helpndoc.com/fr/outils-creation-aide)

### TRANSFERTS CLIENT <a id="page-transfertsclient"></a>
_Page : `TRANSFERTSCLIENT.html` — Module Vente_

[image : NouvelElement792.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| N° Demande |  |  | En lecture seule |
| Date demande |  | X |  |
| Motif |  | X | Liste déroulante de [motifs](#page-motifsdechangement) |
| Date légalisation |  | X |  |
| Date validation |  | X | Avant de procéder à la validation de l’opération, il faut saisir la « Date de validation » |
| Net à payer |  |  | En lecture seule |
| Réglé |  |  | Montant réglé en lecture seule |
| Reste |  |  | Montant restant en lecture seule |
| Frais |  |  | Montant des frais en lecture seule |
| Remise |  |  |  |
| Frais après remise |  |  |  |
| Ancien client |  |  |  |
| Part |  |  | En lecture seule |
| N° Dossier |  |  | En lecture seule |
| Tranche |  | X | En lecture seule |
| Article |  | X | En lecture seule |
| Catégorie |  |  | En lecture seule |
| Type |  |  | En lecture seule |
| C’est un ancien client |  |  | En lecture seule |
| Observations |  |  |  |
| Nouveau client |  | X | Sélecteur des [tiers](#page-clients) |
| Part |  |  |  |
Remarque:
Le système affiche que les articles dont le statut de dossier de vente est paramétré "Peut effectuer un transfert client".

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0008 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001<br> A l'enregistrement du demande de transfert client, le système vérifie les champs "Pièce d'identité " et "Numéro" si sont renseignés ou non<br>- Si oui: le système continue l'enregistrement.<br>- Sinon: Le système affiche un message d'erreur. |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Imprimer | GEN0011 |
| Valider | Valider le transfert client : Cette action est gérée dans les habilitations<br>- Tous les champs seront en lecture seule<br>- Tous les boutons seront désactivés.<br>- Le système affiche le dossier de vente en mettant à jour le client et les acquéreurs et ajoute les frais de transfert client. |
Créé avec HelpNDoc Personal Edition: [Outil de création d'aide complet](https://www.helpndoc.com/fr/outils-creation-aide)

### DESISTEMENTS <a id="page-desistements"></a>
_Page : `DESISTEMENTS.html` — Module Vente_

[image : NouvelElement708.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| N° Dossier |  |  | En lecture seule |
| Client |  |  | En lecture seule |
| Projet |  | X | En lecture seule |
| Article |  | X | En lecture seule |
| Catégorie |  |  | En lecture seule |
| Type |  |  | En lecture seule |
| C’est un ancien client |  |  | En lecture seule |
| N° Demande |  |  | En lecture seule |
| Date demande |  | X | En lecture seule |
| Motif |  | X | Liste déroulante de [motifs](#page-motifsdechangement) |
| Date légalisation |  |  |  |
| Date validation |  | X | Avant de procéder à la validation de l’opération, il faut saisir la « Date de validation » |
| Net à payer |  |  | En lecture seule |
| Réglé |  |  | En lecture seule<br> = Σ des échéances qui sont paramétrés "Avances" dans la trésorerie + Σ des échéances qui sont paramétrés "Crédit" dans la trésorerie + Σ des échéances qui sont paramétrés "Autofinancement" dans la trésorerie. |
| Reste |  |  | En lecture seule |
| Pénalité |  |  | Pourcentage de la pénalité de désistement<br> Remarque: Si la case "Pénalité de désistement basée sur le prix de vente sinon sur les encaissements" est cochée, le système calcule le montant de la pénalité de la manière suivante; Σ des encaissements * Pourcentage de la pénalité de désistement, sinon; Prix de vente * Pourcentage de la pénalité de désistement. |
| Remise |  |  | Taux de remise |
| Pénalité après remise |  |  | ∑ des restes à régler des frais qui sont paramétrés "Doit être réglé après désistement". |
| A rembourser |  |  | En lecture seule,<br> = Σ des versements - Pénalité après remise |
| Observations |  |  |  |
| Type pièce de règlement |  |  | Liste déroulante des [types pièce de règlement](#page-typespiece) |
| Référence |  |  | Référence de règlement |
| Banque |  |  | Liste déroulante des [banque](#page-banques) |
Remarques:
- Le système affiche que les articles dont le statut de dossier de vente est paramétré " Peut effectuer un désistement ".
- On peut modifier les champs "Type pièce de règlement","Référence" et "Banque" après la validation de désistement selon les droits accordés à l'utilisateur connecté.

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0008 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Imprimer | GEN0011 |
| Valider | Valider le désistement : Cette action est gérée dans les habilitations<br>- Tous les champs seront en lecture seule<br>- Tous les boutons seront désactivés.<br>- Le système affiche le dossier de vente en mettant à jour le statut et ajoute les frais de désistement et le montant à rembourser.<br>- Le système modifie le « Client » en « Prospect » après l’enregistrement du dossier de vente. |
Créé avec HelpNDoc Personal Edition: [Générateur de documentation complet](https://www.helpndoc.com/fr)

### IMPAYES <a id="page-impayes"></a>
_Page : `IMPAYES.html` — Module Vente_

[image : NouvelElement475.png]
La liste visualise la liste des documents impayés des dossiers de ventes en indiquant les champs cité ci dessous
Liste

| LIBELLE | V | OBSERVATIONS |
|---|---|---|
| Etat | X |  |
| N° Dossier | X |  |
| Projet | X |  |
| Article | X |  |
| Échéance | X |  |
| Catégorie | X |  |
| Montant | X |  |
| Reste | X |  |
| Client | X |  |
| Commercial | X |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Éditer | GEN0009 |
| Imprimer | GEN0011 |
| Exporter | GEN0008 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Imprimer | GEN0011 |
| Réinitialiser | Le système recharge les données de l’[article](#page-articles) et des [sous articles](#page-sousarticles) à partie du [module stock](#page-modulestock). |
| Ajouter un document | GEN0008 |
| Éditer un document | GEN0009 |
| Supprimer un document | GEN0010 |
| Ajouter règlement | Régler une partie ou la totalité d’un montant du schéma de financement |
| Ajouter paiement | Rembourser une partie ou la totalité d’un montant pour le client |
Créé avec HelpNDoc Personal Edition: [Créer de la documentation iPhone facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites/generation-de-site-web-iphone)

### REGLEMENTS <a id="page-reglements-com"></a>
_Page : `REGLEMENTS_COM.html` — Module Vente_

[image : NouvelElement703.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Entité |  | X | Liste des [entités juridique](#page-entitesjuridique) |
| Date pièce |  | X |  |
| Échéance |  | X |  |
| Type |  | X | Liste de [types de pièces](#page-typespiece) |
| Référence |  |  |  |
| Banque |  |  | Liste déroulante des [banques](#page-banques) |
| Pièce remise (au siège) |  |  | Case à cocher, modifiable selon le droit " Réceptionner les pièces de règlements " |
| Date remise au siège |  |  | Par défaut en lecture seule,<br> Si la case "Pièce remise (au siège)" est cochée, le système rend le champ "Date remise au siège" modifiable. |
| Pointé (date) |  |  |  |
| Base |  |  |  |
| Montant RS |  |  |  |
| Montant |  |  |  |
| Montant affecté |  |  | En lecture seule,<br> Σ des nets à payer des lignes de l'onglet affectation |
| Reste à affecter |  |  | En lecture seule<br> Montant base - Montant affecté (Si Avec gestion de la retenue à la source est vrai)<br> Montant - Montant affecté |
| Notes |  |  |  |
| Compte |  |  | Liste déroulante des [comptes](#page-comptes) |
| Tiers |  | X | Sélecteur de [tiers](#page-tiers) |
| Tiers payeur |  |  | Sélecteur de [tiers](#page-tiers) |
| Tiers autrui |  |  | Sélecteur de [tiers](#page-tiers) |
| Notaire |  |  | Sélecteur de [tiers](#page-tiers) |
| Code reçu |  |  | En lecture seule |
| Liste des affectations |  |  | La liste visualise affectations en indiquant :<br>- Le projet<br>- L’article<br>- Le dossier<br>- La date<br>- La description<br>- Le net à payer<br>- Le montant affecté<br> Aussi, dans cette liste on peut effectuer les opérations suivantes :<br>- Ajouter un échéancier: On cliquant sur ajouter, le système affiche la liste des échéanciers qui appartiennent à l'entité du pièce de règlement du tiers sélectionné.<br>- Supprimer un échéancier<br>- Sélectionner un échéancier |
| Documents joints |  |  | La liste visualise la liste des types des documents à joindre |
Remarque: Les champs, Base, Pourcentage RS et montant RS si le paramètre "Avec gestion de la retenue à la source" (Dans l’écran [paramètres système](#page-parametressysteme) ) est vrai

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0008 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001<br> A l'enregistrement le système vérifie dans l'entité fonctionnelle de la fiche règlement s'il y a des types de documents à attacher<br>- Si oui : Le système affiche la liste des document à attacher<br>- Sinon: Le système continue l'opération d'enregistrement normalement |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Imprimer | GEN0011 |
| Reçu | Le système imprime le reçu de règlement |
Créé avec HelpNDoc Personal Edition: [Créer des sites web d'aide facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

### PAIEMENTS <a id="page-paiements"></a>
_Page : `PAIEMENTS.html` — Module Vente_

[image : NouvelElement736.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Entité |  | X | Liste des [entités juridique](#page-entitesjuridique) |
| Date pièce |  | X |  |
| Échéance |  | X |  |
| Type |  | X | Liste de[types de pièces](#page-typespiece) |
| Référence |  |  |  |
| Compte |  |  | Liste déroulante des [comptes](#page-comptes) |
| Pièce remise (au siège) |  |  | Case à cocher, modifiable selon le droit " Réceptionner les pièces de règlements " |
| Date remise au siège |  |  | Par défaut en lecture seule,<br> Si la case "Pièce remise (au siège)" est cochée, le système rend le champ "Date remise au siège" modifiable. |
| Base |  |  |  |
| Montant RS |  |  |  |
| Montant |  |  |  |
| Montant affecté |  |  | En lecture seule<br> Σ des nets à payer des lignes de l'onglet affectation |
| Reste à affecter |  |  | En lecture seule<br> Montant base - Montant affecté (Si Avec gestion de la retenue à la source est vrai)<br> Montant - Montant affecté |
| Notes |  |  |  |
| Tiers |  | X | Sélecteur de [tiers](#page-tiers) |
| Tiers payeur |  |  | Sélecteur de [tiers](#page-tiers) |
| Tiers autrui |  |  | Sélecteur de [tiers](#page-tiers) |
| Notaire |  |  | Sélecteur de [tiers](#page-tiers) |
| Code réçu |  |  | En lecture seule |
| Liste des affectations |  |  | La liste visualise affectations en indiquant :<br>- Le projet<br>- L’article<br>- Le dossier<br>- La date<br>- La description<br>- Le net à payer<br>- Le montant affecté<br>Aussi, dans cette liste on peut effectuer les opérations suivantes :<br>- Ajouter un échéancier<br>- Supprimer un échéancier<br>- Sélectionner un échéancier |
Remarque: Les champs Base, Pourcentage RS et montant RS si le paramètre "Avec gestion de la retenue à la source" (Dans l’écran [paramètres système](#page-parametressysteme) ) est vrai

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0008 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Imprimer | GEN0011 |
Créé avec HelpNDoc Personal Edition: [Générer des livres électroniques EPub facilement](https://www.helpndoc.com/fr/creer-des-livres-numeriques-epub)

### LISTE A FAIRE <a id="page-listeafaire"></a>
_Page : `LISTEAFAIRE.html` — Module Vente_

[image : NouvelElement487.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Liste de travail à faire |  |  | La liste visualise les listes en fonction de leurs sources<br>- Prospects<br>- Prospections<br>- Dossiers de vente |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Changer statut | [Le système affecte le statut à la ligne choisi](#page-statutsdesrelances) |
| Actions | Le système donne la possibilité d'ajouter une action sur un ou plusieurs Prospect, Prospections ou Dossiers de vente |
| Imprimer | GEN0011 |
| Exporter | GEN0008 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
Créé avec HelpNDoc Personal Edition: [Générateur gratuit de livres électroniques et documentation](https://www.helpndoc.com/fr)

### COMMISSIONS <a id="page-commissions"></a>
_Page : `COMMISSIONS.html` — Module Vente_

[image : NouvelElement660.png]

### Liste

| LIBELLE | V | OBSERVATIONS |
|---|---|---|
| Commercial/Gestionnaire | X | [<br>](#page-prospections) |
| Tranche | X |  |
| Catégorie | X |  |
| Dossier de vente | X |  |
| Type | X |  |
| Date | X |  |
| Montant | X |  |

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Tranches |  |  | Sélecteur des tranches<br> Avant de procéder au calcul des commissions, l'utilisateur doit choisir au moins une tranche sinon le système affiche le message d'erreur suivant:<br>[image : NouvelElement607.png] |
| Date de |  | X | Date début de la période de calcul des commissions |
| A |  | X | Date fin de la période de calcul des commissions |
| Commission concrétisation |  |  | Si la case est coché, le système affiche les commissions de concrétisation |
| Finalisation 1 |  |  | Si la case est coché, le système affiche les commissions de finalisation 1 |
| Finalisation 2 |  |  | Si la case est coché, le système affiche les commissions de finalisation 2 |
| Désistement |  |  | Si la case est coché, le système affiche les désistements |
| Commercial/Gestionnaire |  |  | Sélecteur des [commerciaux/gestionnaires](#page-personnels)[qui ont des dossiers de vente](#page-personnels) |
| Dossier de vente |  |  | Champ de recherche des commissions par dossier de vente |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0008 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Imprimer | GEN0011 |
| Générer | - Le système affiche tous les dossiers de ventes des tranches sélectionnées qui ont été concrétisés et/ou finalisés(selon le choix d'utilisateur) dans la période choisi et il applique la commission pour chacun selon le paramétrage effectué sous l'onglet "Commissions vendeur" de la [tranche](#page-cfg-tranches)<br>Exemple:<br> - Si on fait un paramétrage comme suit:<br> \| \| Paramétrage 1: Tranche 1 \| Paramétrage 2:Tranche 1 \| Paramétrage 3 : Tranche 2 \| Paramétrage 4: Tranche 2 \| \|---\|---\|---\|---\|---\| \| Date début \| 01/01/2015 \| - \| 01/01/2015 \| - \| \| Date fin \| 31/12/2015 \| - \| 31/12/2015 \| - \| \| Commission vente \| 100 \| 50 \| 300 \| 120 \| \| Commission vente (Prestataire) \| 200 \| 70 \| 400 \| 220 \| \| Commission finalisation 1 \| 50 \| 20 \| 100 \| 60 \| \| Commission finalisation 1 (Prestataire) \| 80 \| 30 \| 180 \| 80 \| \| Commission finalisation 2 \| 100 \| 40 \| 250 \| 120 \| \| Commission finalisation 1 (Prestataire) \| 170 \| 50 \| 270 \| 200 \| \| Catégorie article \| Appartement \| \| Commerce \| - \| \| Action commerciale \| Action commerciale 1 \| \| - \| Action commerciale 4 \| \| Commercial \| Prénom 1 \| Prénom 2 \| - \| - \| <br>- Dans l'écran de calcul des commissions, on choisit<br> - La date début : 01/01/2015<br> - La date fin : 31/12/2015<br> Le système va appliquer les commissions comme suit:<br>- Tout les commerciaux affectés au dossiers qui appartiennent à la tranche 1 dont le commercial = "Prénom 1", action commerciale = "Action commerciale 1" et la catégorie d'article = "Appartement" vont avoir:<br> - Une commission de vente = 100 (s'il y a des dates de concrétisation dans la période)<br> - Une commission de vente (Prestataire) = 200 (s'il y a des dates de concrétisation (La concrétisation est effectuée par un prestataire) dans la période)<br> - Une commission de finalisation 1 = 50 (s'il y a une dates associée du statut de finalisation1 dans la période)<br> - Une commission de finalisation 2 = 100 (s'il y a une dates associée du statut de finalisation2 dans la période)<br> - Une commission de finalisation 1(Prestataire) = 80 (s'il y a une dates associée du statut de finalisation1 (La finalisation est effectuée par un prestataire) dans la période)<br> - Une commission de finalisation 2(Prestataire) = 170 (s'il y a une dates associée du statut de finalisation2 (La finalisation est effectuée par un prestataire) dans la période)<br>==> Les dossiers qui ont, le commercial/gestionnaire différent de "Prénom 1" et/ou l'action commerciale différente de "Action commerciale 1" et/ou la catégorie d'article différente de "Appartement" n'auront pas une commission (selon le paramétrage 1 effectué dans le tableau ci dessus).<br>- Tout les commerciaux affectés au dossiers qui appartiennent à la tranche 1 dont le commercial = "Prénom 2", action commerciale = "Peu importe" et la catégorie d'article = "Peu importe" vont avoir:<br> - Une commission de vente = 50 (s'il y a des dates de concrétisation dans la période)<br> - Une commission de vente (Prestataire) = 70 (s'il y a des dates de concrétisation (La concrétisation est effectuée par un prestataire) dans la période)<br> - Une commission de finalisation 1 = 20 (s'il y a une dates associée du statut de finalisation1 dans la période)<br> - Une commission de finalisation 2 = 40 (s'il y a une dates associée du statut de finalisation2 dans la période)<br> - Une commission de finalisation 1(Prestataire) = 30 (s'il y a une dates associée du statut de finalisation1 (La finalisation est effectuée par un prestataire) dans la période)<br> - Une commission de finalisation 2(Prestataire) = 50 (s'il y a une dates associée du statut de finalisation2 (La finalisation est effectuée par un prestataire) dans la période)<br>==> Les dossier qui ont, le commercial/gestionnaire différent de "Prénom 2" n'auront pas une commission (selon le paramétrage 2 effectué dans le tableau ci dessus).<br>===> Le résultat de calcul est l'union des résultats des deux paramétrages.<br>- Tout les commerciaux affectés au dossiers qui appartiennent à la tranche 2 dont la catégorie d'article = "Commerce" vont avoir:<br> - Une commission de vente = 300 (s'il y a des dates de concrétisation dans la période)<br> - Une commission de vente (Prestataire) = 400 (s'il y a des dates de concrétisation (La concrétisation est effectuée par un prestataire) dans la période)<br> - Une commission de finalisation1 = 100 (s'il y a une dates associée du statut de finalisation1 dans la période)<br> - Une commission de finalisation2 = 250 (s'il y a une dates associée du statut de finalisation2 dans la période)<br> - Une commission de finalisation1(Prestataire) = 180 (s'il y a une dates associée du statut de finalisation1 (La finalisation est effectuée par un prestataire) dans la période)<br> - Une commission de finalisation2(Prestataire) = 270 (s'il y a une dates associée du statut de finalisation2 (La finalisation est effectuée par un prestataire) dans la période)<br>==> Les dossiers qui ont, une catégorie d'article différente de "Commerce" n'auront pas une commission. (selon le paramétrage 3 effectué dans le tableau ci dessus)<br>- Tout les commerciaux affectés au dossiers qui appartiennent à la tranche 2 dont l'action commerciale = "Action commerciale 4" vont avoir:<br> - Une commission de vente = 120 (s'il y a des dates de concrétisation dans la période)<br> - Une commission de vente (Prestataire) = 220 (s'il y a des dates de concrétisation (La concrétisation est effectuée par un prestataire) dans la période)<br> - Une commission de finalisation1 = 60 (s'il y a une dates associée du statut de finalisation1 dans la période)<br> - Une commission de finalisation1 = 120 (s'il y a une dates associée du statut de finalisation2 dans la période)<br> - Une commission de finalisation1(Prestataire) = 80 (s'il y a une dates associée du statut de finalisation1 (La finalisation est effectuée par un prestataire) dans la période)<br> - Une commission de finalisation1(Prestataire) = 200 (s'il y a une dates associée du statut de finalisation2 (La finalisation est effectuée par un prestataire) dans la période)<br>==> Les dossiers qui ont, une action commercial différente de "Action commerciale 4" n'auront pas une commission. (selon le paramétrage 4 effectué dans le tableau ci dessus)<br>===> Le résultat de calcul est l'union des résultats des deux paramétrages.<br>Remarque: Le système applique la commission du paramétrage 4 même pour les dossier qui ont des articles de catégorie = "Commerce" et leurs action commerciale = "Action commerciale 4" car ce dernier est plus prioritaire que la catégorie.<br>En effet, l'ordre de priorité d'application des commissions est le suivant:<br>- Un paramétrage qui a un Commercial/gestionnaire, Action commerciale et Catégorie est plus prioritaire qu'un paramétrage qui a un Commercial/gestionnaire et Action commerciale, est plus prioritaire qu'un paramétrage qui a Commercial/gestionnaire et Catégorie, est plus prioritaire qu'un paramétrage qui a Commercial/gestionnaire, est plus prioritaire qu'un paramétrage qui a une Action commerciale et Catégorie, est plus prioritaire qu'un paramétrage qui a une Action commerciale, est plus prioritaire qu'un paramétrage qui a une Catégorie, est plus prioritaire qu'un paramétrage qui n'a ni Commercial/gestionnaire ni Action commerciale ni Catégorie.<br>Remarque: Si on paramètre tous les commissions à 0, le système n'affiche aucune ligne. |
| Modifier montant | Lorsqu'on clic "Modifier montant", le système affiche l'écran suivant<br>[image : NouvelElement661.png]<br>\| LIBELLE \| U \| O \| OBSERVATIONS \| \|---\|---\|---\|---\| \| Commercial/ Gestionnaire \| \| \| En lecture seule,<br> Le système affiche la description du « Commercial/ Gestionnaire \| \| Type \| \| \| En lecture seule,<br> Le système affiche le type de commission, il peut être :<br>- Commission vente<br>- Finalisation 1<br>- Finalisation 2 \| \| Article \| \| \| En lecture seule,<br> Le système affiche le code article. \| \| Tranche \| \| \| En lecture seule,<br> Le système affiche la description de la tranche. \| \| Montant \| \| \| Le système par défaut affiche le montant de commission (selon le paramétrage) et il donne la possibilité de modifier ce dernier. \| |
| Générer document à payer | Cette action permet la génération d'un document à régler.<br> Remarque: On ne peut pas générer un document à payer pour deux commerciaux différents.<br> ==> La génération des documents se fait par commercial. |
| Ouvrir document à payer | Si la ligne sélectionnée à un document à payer généré, le système ouvre la fiche, sinon il affiche un message "Cette ligne n'a pas encore un document à payer". |
| Supprimer | Le système supprime la ligne de commission sélectionnée. |
Créé avec HelpNDoc Personal Edition: [Environnement de création d'aide complet](https://www.helpndoc.com/fr/outils-creation-aide)

### TABLEAUX DE BORDS <a id="page-tableauxdebords-com"></a>
_Page : `TABLEAUXDEBORDS_COM.html` — Module Vente_

Créé avec HelpNDoc Personal Edition: [Générateur de documentation d'aide HTML gratuit](https://www.helpndoc.com/fr)

#### SUIVI DES PREVISIONS <a id="page-suividesprevisions"></a>
_Page : `SUIVIDESPREVISIONS.html` — Module Vente › TABLEAUX DE BORDS_

[image : NouvelElement142.png]

### UNDER CONSTRUCTION
Créé avec HelpNDoc Personal Edition: [Produire des aides en ligne pour les applications Qt](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creez-des-fichiers-daide-pour-le-framework-qt-help)

#### SUIVI DES COMMERCIAUX <a id="page-suividescommerciaux"></a>
_Page : `SUIVIDESCOMMERCIAUX.html` — Module Vente › TABLEAUX DE BORDS_

[image : NouvelElement143.png]

### UNDER CONSTRUCTION
Créé avec HelpNDoc Personal Edition: [Générateur complet de livres électroniques ePub](https://www.helpndoc.com/fr/creer-des-livres-numeriques-epub)

#### SUIVI DES ACTIONS <a id="page-suividesactions"></a>
_Page : `SUIVIDESACTIONS.html` — Module Vente › TABLEAUX DE BORDS_

[image : NouvelElement518.png]

### Liste

| LIBELLE | V | OBSERVATIONS |
|---|---|---|
| Utilisateur | X | L'utilisateur qui a créé l'action<br> Remarque: L'action peut être créé par un utilisateur ou par le système. |
| Action | X | Type action (Note, téléphone entrant, Mail entrant...) |
| rang | X | Rang de type d'action pour chaque tiers |
| Date action | X |  |
| Date alerte | X |  |
| Alerte active | X |  |
| Catégorie | X | Catégorie action (Prospect, Prospection, Dossier de vente, Transfert..) |
| Commercial | X |  |
| Résultat | X |  |
| Message | X |  |
| Impayé | X |  |
| Tiers | X |  |
| Prospection | X |  |
| N° Dossier | X |  |
| État | X |  |
| Transfert produit | X |  |
| Transfert client | X |  |
| Entité | X |  |
| Désistement | X |  |
| Programme | X |  |
| Article | X |  |
| Tranche | X |  |
| Personnel | X |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Éditer | GEN0009 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014Le système ouvre l'écran correspondant (Prospect, Prospection, Dossier de vente...) |
Créé avec HelpNDoc Personal Edition: [Qu'est-ce qu'un outil de création d'aide ?](https://www.helpauthoringsoftware.com/fr)

#### SUIVI DES DOSSIERS DE VENTE <a id="page-suividesdossiersdevente"></a>
_Page : `SUIVIDESDOSSIERSDEVENTE.html` — Module Vente › TABLEAUX DE BORDS_

[image : NouvelElement653.png]

### Liste

| LIBELLE | V | OBSERVATIONS |
|---|---|---|
| Action commerciale | X | Action commerciale à partir de la [prospection](#page-prospections) |
| Alerte | X | Le système affiche la date de la dernière alerte active<br> Remarque: Le système affiche l’icône d'alerte et la date alerte selon la configuration effectuée dans l’écran des [paramètres système](#page-parametressysteme):<br> Si la date alerte - Délai d'alerte = Date de jour:<br> --> Le système affiche l'alerte<br> Sinon<br> --> Le système n'affiche pas l'alerte |
| Autofinancement |  | Somme des lignes de schéma de financement qui sont paramétrées dans la [trésorerie](#page-tresorerie-com) de module vente "Auto-financement" |
| Autofinancement réglé |  | Somme des lignes de schéma de financement qui sont paramétrées dans la [trésorerie](#page-tresorerie-com) de module vente "Auto-financement " réglé |
| Autofinancement reste |  | Autofinancement - Autofinancement réglé |
| Avance |  | Somme des lignes de schéma de financement qui sont paramétrées dans la [trésorerie](#page-tresorerie-com) de module vente "Avance" |
| Avance réglée |  | Somme des lignes de schéma de financement qui sont paramétrées dans la [trésorerie](#page-tresorerie-com) de module vente "Avance " réglé |
| Avance reste |  | Avance - Avance réglée |
| Banque Crédit / Leasing |  |  |
| Bloc |  | Description du bloc |
| Cat. |  | Catégorie Article |
| Client | X | Le système affiche le client du dossier de vente |
| Client veut choisir le Cadeau Plus Tard |  |  |
| Code | X | Code Article |
| Commercial | X | Par défaut le commercial qui a crée le dossier de vente |
| Confirmé |  |  |
| Crédit |  | Somme des lignes de schéma de financement qui sont paramétrées dans la [trésorerie](#page-tresorerie-com) de module vente "Crédit" |
| Crédit réglé |  | Somme des lignes de schéma de financement qui sont paramétrées dans la [trésorerie](#page-tresorerie-com) de module vente "Crédit " réglé |
| Crédit reste |  | Crédit - Crédit réglé |
| Crée le |  |  |
| Crée par |  |  |
| Date attestation de consignation/ octroi crédit | X |  |
| Date compromis / Approbation crédit | X |  |
| Date concrétisation | X |  |
| Date décision MEILLEUR TAUX | X |  |
| Date dernière communication |  |  |
| Date enregistrement du Contrat |  |  |
| Date d’envoi MEILLEUR TAUX | X |  |
| Date Envoi Notaire / Avocat | X |  |
| Date envoi notaire prévisionnelle |  |  |
| Date expédition | X |  |
| Date fiche Assurance |  |  |
| Date fiche Bancaire | X |  |
| Date fiche Intervention | X |  |
| Date minute / Signature Contrat | X |  |
| Date Permis d’Habiter | X |  |
| Date Promesse de vente |  |  |
| Date Prospection | X |  |
| Date Relance 1 |  |  |
| Date Relance 2 |  |  |
| Date Remise de clés | X |  |
| Date Remise de clés prévisionnelle |  |  |
| Date Réservation | X |  |
| Date Visite Technique/ Réception | X |  |
| Décision MEILLEUR TAUX | X |  |
| Délai MEILLEUR TAUX | X |  |
| Délais Concrétisation – Envoi Notaire | X | Délais en jours |
| Délais Envoi au Notaire – Crédit accordé | X | Délais en jours |
| Délais Envoi au Notaire – Remise des clés | X | Délais en jours |
| Délais Prospection – Concrétisation |  | Délais en jours |
| Délais remise des Clés –Crédit accordé | X | Délais en jours |
| Dernier message de communication |  | Dernier message d’une action ajoutée par un utilisateur<br> Remarque: Si le message est vide le système affiche dans cette colonne le dernier résultat de communication. |
| Dernier résultat de communication |  | Dernier résultat d’une action ajoutée par un utilisateur |
| Description Article |  |  |
| Entité |  | Description de l'entité juridique |
| Entité code |  | Code de l'entité juridique |
| Est Un Ancien Client |  |  |
| Étage |  | Code étage |
| Frais |  | Somme des lignes de schéma de financement qui sont paramétrées dans la [trésorerie](#page-tresorerie-com) de module vente "Frais" |
| Frais réglé |  | Somme des lignes de schéma de financement qui sont paramétrées dans la [trésorerie](#page-tresorerie-com) de module vente "Frais" réglé |
| Frais reste |  | Frais - Frais réglé |
| Gestionnaire | X |  |
| Mobile |  |  |
| Modifications |  | Somme des lignes de schéma de financement qui sont paramétrées dans la [trésorerie](#page-tresorerie-com) de module vente "Modifications" |
| Modifications réglées |  | Somme des lignes de schéma de financement qui sont paramétrées dans la [trésorerie](#page-tresorerie-com) de module vente "Modifications" réglée |
| Modifications restes |  | Modifications - Modifications réglées |
| Modifier le |  |  |
| Modifier par |  |  |
| Montant Remise |  |  |
| Net à payer | X | Prix de vente après remise + le montant des documents de type frais |
| Net à payer réglé | X | Prix de vente réglé après remise + le montant des documents de type frais |
| Net à payer reste | X | Net à payer - Net à payer réglé |
| Nombre d’actions | X |  |
| Nombre des Cadeaux |  |  |
| Notaire / Avocat |  |  |
| Pénalité |  | Somme des lignes de schéma de financement qui sont paramétrées dans la [trésorerie](#page-tresorerie-com) de module vente "Pénalité" |
| Pénalité réglée |  | Somme des lignes de schéma de financement qui sont paramétrées dans la [trésorerie](#page-tresorerie-com) de module vente "Pénalité" réglée |
| Pénalité reste |  | Pénalité - Pénalité réglée |
| Prix Article HT |  | Prix article HT avant remise * (1- taux de remise) |
| Prix Article HT avant remise |  | Prix de l'article HT remise |
| Prix Article TTC |  | Prix Article HT * Taux TVA (%) |
| Prix Article TTC avant remise |  | Prix de l'article TTC avant remise |
| Prix Contractuel |  | Somme des lignes de schéma de financement qui sont paramétrées dans la trésorerie "Auto-financement" ou "Crédit" ou "Avance" |
| Prix Contractuel Réglé |  | Somme de règlements des lignes de schéma de financement qui sont paramétrées dans la trésorerie "Auto-financement" ou "Crédit" ou "Avance" |
| Prix Contractuel Reste |  | Prix Contractuel - Prix Contractuel Réglé |
| Prix de vente | X | Prix de vente après remise<br> = Somme des lignes qui sont paramétrées "autofinancement" dans la trésorerie de module vente + Somme des lignes qui sont paramétrées "avance" dans la trésorerie de module vente + Somme des lignes qui sont paramétrées "Crédit" dans la trésorerie de module vente + Somme des lignes qui sont paramétrées "Modification" dans la trésorerie de module vente |
| Prix de vente HT |  | Prix article HT + Prix sous articles HT |
| Prix de vente HT avant remise |  | Prix article HT avant remise + Prix sous articles HT avant remise |
| Prix de vente m² HT |  | Prix total de vente HT / superficie de l'article |
| Prix de vente m² TTC |  | Prix total de vente TTC / superficie de l'article |
| Prix de vente réglé | X | Prix de vente réglé<br> = Somme des lignes qui sont paramétrées "autofinancement" réglé dans la [trésorerie](#page-tresorerie-com) de module vente + Somme des lignes qui sont paramétrées "avance" réglé dans la trésorerie de module vente + Somme des lignes qui sont paramétrées "Crédit" réglé dans la [trésorerie](#page-tresorerie-com) de module vente + Somme des lignes qui sont paramétrées "Modification" réglé dans la trésorerie de module vente |
| Prix de vente reste | X | Prix de vente - Prix de vente réglé |
| Prix de vente TTC avant remise |  | Prix articleTTC avant remise + Prix sous articles TTC avant remise |
| Prix du m² HT |  | Montant Remise = Prix du m² HT avant remise * Pourcentage Remise<br> Prix du m² HT = Prix du m² HT avant remise - Montant Remise |
| Prix du m² HT avant remise |  | Prix d'un article de 1m² HT avant remise à partir de module stock |
| Prix du m² TTC |  | Prix du m² HT * Taux TVA (%) |
| Prix du m² TTC avant remise |  | Prix d'un article de 1m² Tout Taxe Compris avant remise à partir de module stock |
| Prix Sous Articles HT |  | Prix Sous Articles HT avant remise * (1- taux de remise) |
| Prix Sous Articles HT avant remise |  | Prix de sous article HT avant remise |
| Prix Sous Articles TTC |  | Prix de sous article HT * Taux TVA (%) |
| Prix Sous Articles TTC avant remise |  | Prix de sous article TTC avant remise |
| Programme |  | Description Programme |
| Programme code |  | Code de Programme |
| Remise % |  | Montant remise / Total HT |
| Source de contact | X |  |
| Statut |  |  |
| Sup. (m²) | X | Surface en m² |
| Support source de contact | X |  |
| Taux Avance de base | X | ∑ (des documents de type avance) / Prix de vente * 100 |
| Taux Avance réel |  | ∑ (des montants réglés des documents de type avance) / (∑ des documents de type avance) * 100 |
| Taux de Concrétisation de base | X | ∑ (des documents de type "Dans le cadre de la concrétisation") / Prix de vente * 100 |
| Taux de Concrétisation réel | X | ∑ ( montants réglés des documents de type "Dans le cadre de la concrétisation" ) / ∑ (des documents de type "Dans le cadre de la concrétisation") *100 |
| Tél. |  |  |
| Titre Foncier |  |  |
| Tranche | X |  |
| Tranche code |  |  |
| Trop Perçu |  | Montant affecté - montant à payer : Le plus que le client a payé |
| Trop Perçu réglé |  |  |
| Trop Perçu reste |  |  |
| TVA |  | Taxe sur Valeur Ajoutée |
| Type |  |  |
| Type Présentation | X |  |

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| N° dossier |  |  | Numéro de dossier auto généré en lecture seule |
| Date |  |  |  |
| Client |  |  | Sélecteur de clients |
| Commercial |  |  | Liste déroulante de personnels |
| Prospection |  |  | Afficher fiche « Prospection» |
| Statut / validité |  |  | Liste déroulante de Statuts<br> Remarque1 : Les champs obligatoires varient selon le statut du dossier de vente.<br> Remarque2 : Les champs obligatoires pour chaque transition sont pré-paramétrés dans le sous module « statut»<br> Remarque3 : Les rôles des acteurs qui ont les privilèges de modifier un statut sont pré-paramétrés dans le sous module « Transition»<br> Remarque4 : Si la durée de vie d’un statut égale à zéro, le champ validité sera en lecture seule. |
| Projet |  |  | En lecture seule |
| Article |  |  | Afficher la fiche « Article» |
| Catégorie |  |  | En lecture seule |
| Titre foncier |  |  | En lecture seule |
| Présentation |  |  | En lecture seule |
| Type |  |  | En lecture seule |
| Superficie (m²) |  |  | En lecture seule |
| Prix du m² |  |  | En lecture seule |
| Prix article |  |  | En lecture seule |
| Prix sous articles |  |  | En lecture seule |
| Total |  |  | En lecture seule |
| Remise |  |  | En lecture seule |
| Montant remise |  |  |  |
| Prix de vente |  |  |  |
| Prix contractuel |  |  |  |
| Montants (montant) |  |  |  |
| Frais |  |  | En lecture seule |
| Modifications |  |  | En lecture seule |
| Prix de vente |  |  | En lecture seule |
| Avance |  |  | En lecture seule |
| Crédit |  |  | En lecture seule |
| Autofinancement |  |  | En lecture seule |
| Pénalité |  |  | En lecture seule |
| Résiliation |  |  | En lecture seule |
| Net à payer |  |  | En lecture seule |
| Montants (réglé) |  |  |  |
| Frais |  |  | En lecture seule |
| Modifications |  |  | En lecture seule |
| Prix de vente |  |  | En lecture seule |
| Avance |  |  | En lecture seule |
| Crédit |  |  | En lecture seule |
| Autofinancement |  |  | En lecture seule |
| Pénalité |  |  | En lecture seule |
| Résiliation |  |  | En lecture seule |
| Net à payer |  |  | En lecture seule |
| Montants (Reste) |  |  |  |
| Frais |  |  | En lecture seule |
| Modifications |  |  | En lecture seule |
| Prix de vente |  |  | En lecture seule |
| Avance |  |  | En lecture seule |
| Crédit |  |  | En lecture seule |
| Autofinancement |  |  | En lecture seule |
| Pénalité |  |  | En lecture seule |
| Résiliation |  |  | En lecture seule |
| Net à payer |  |  | En lecture seule |
| Dates |  |  |  |
| Concrétisation / Promesse de vente |  |  |  |
| Visite technique / réception |  |  |  |
| Envoie au notaire prévisionnel |  |  |  |
| Confirmé |  |  | Cochée si la date d’envoi au notaire prévisionnelle est confirmée avec le client |
| Envoie au notaire / avocat |  |  |  |
| Minute / Enregistrement du contrat |  |  |  |
| Expédition / Certificat de propriété |  |  |  |
| Relance 1 titre foncier |  |  |  |
| Échéance |  |  |  |
| Relance 2 titre foncier |  |  |  |
| Échéance |  |  |  |
| Fiche bancaire |  |  |  |
| Compromis / approbation crédit |  |  |  |
| Attestation de consi. |  |  |  |
| Intervention |  |  |  |
| Assurance |  |  |  |
| Remise des clés prévues |  |  |  |
| Remise des clés effectives |  |  |  |
| Permis d’habiter |  |  | En lecture seule |
| Annulation / Résiliation |  |  |  |
| Annulation / résiliation |  |  |  |
| Causes |  |  |  |
| Autre |  |  |  |
| TVA |  |  |  |
| Banque de crédit |  |  | Sélecteur des banques |
| Agence de crédit |  |  |  |
| Notaire / Avocat |  |  | Sélecteur de Notaire/ Avocat |
| Note |  |  |  |
| C’est un ancien client |  |  | Case à cocher pour appliquer les remises pour les anciens clients. |
| Intégrer le montant des modifications dans le calcul du prix de vente |  |  |  |
| Date d’envoi MEILLEURTAUX |  |  |  |
| Décision MEILLEURTAUX |  |  |  |
| Date décision MEILLEURTAUX |  |  |  |
| Schéma de financement |  |  |  |
| Liste de schéma de financement |  |  | La liste visualise la liste des documents en indiquant :<br>- La date<br>- La catégorie<br>- Le montant<br>- Le montant réglé<br>- Le montant reste à régler<br>- Le code facture<br>- La note<br> Aussi dans cette liste on peut effectuer les actions suivantes :<br>- Ajouter, éditer, supprimer un document<br>- Supprimer un document<br>- Ajouter un règlement<br>- Supprimer un règlement |
| Remarque1 : Les lignes du schéma de financement sont auto-générées en fonction de la configuration effectuée dans le sous module Tranches (les délais, les seuils de versement, les règles de gestion1, les règles de gestion2 et les remises pour les anciens clients) et en fonction de la configuration de la trésorerie. |  |  |  |
| Remarque2 : Le montant de la concrétisation est calculé de la manière suivante :<br> Si c’est un nouveau client<br>- Le système applique le taux définit dans le seuil de versement de la tranche, sinon le taux définit dans le statut, sinon le taux définit dans les règles de gestion1 de la tranche.<br>- A la création des lignes du schéma de financement le système utilise le taux définit dans les règles de gestion1 de la tranche<br> Si c’est un ancien client<br>- Le système applique le taux définit dans le remise pour les anciens clients de la tranche. |  |  |  |
| Sous articles |  |  |  |
| Liste de sous articles |  |  | La liste visualise les sous articles affectés à un article en indiquant :<br>- Le code<br>- La catégorie<br>- Le type<br>- Superficie (m²)<br>- Affecté par défaut …<br> Aussi dans cette liste on peut effectuer les actions suivantes :<br>- Ajouter un sous article<br>- Supprimer un article |
| Acquéreurs |  |  |  |
| Liste des acquéreurs |  |  | La liste visualise les acquéreurs d’un article en indiquant :<br>- Le tiers<br>- La répartition (en pourcentage)<br> Aussi, dans cette liste on peut effectuer les actions suivantes :<br>- Ajouter un acquéreur<br>- Éditer la fiche d’un acquéreur<br>- Supprimer un acquéreur |
| Documents à joindre |  |  |  |
| Liste des documents à joindre |  |  | La liste visualise les types des fichiers attachés<br> Dans cette liste, on peut faire les actions suivantes<br>- Ajouter un document<br>- Supprimer un document |
| Modifications |  |  |  |
| Liste de modifications |  |  | La liste visualise les modifications apportées à un article en indiquant :<br>- L’état<br>- La date de mande<br>- La description …<br> Aussi dans cette liste on peut effectuer les opérations suivantes :<br>- Ajouter une demande de modification<br>- Éditer une demande de modification<br>- Supprimer une demande de modification |
| Cadeaux |  |  |  |
| Liste de cadeaux |  |  | La liste visualise :<br>- Les cadeaux<br>- Les prix<br>- Les états (livré ou non livré)<br>- La disponibilité<br>- Les raisons<br>- La date livraison<br>- La note<br> Aussi dans cette liste on peut effectuer les opérations suivantes ;<br>- Ajouter un cadeau<br>- Supprimer un cadeau |
| Actions |  |  |  |
| Liste des actions |  |  | La liste visualise le nombre d’actions groupées par type |
| Historique |  |  |  |
| Liste(Historique) |  |  | La liste visualise l’historique du dossier de vente en indiquant :<br>- L’utilisateur<br>- La date<br>- L’état … |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Éditer | GEN0009 |
| Imprimer | GEN0011 |
| Exporter | GEN0008 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Imprimer | GEN0011 |
| Réinitialiser | Le système recharge les données de l’article et des sous articles à partie du module stock. |
| Compteur | Afficher le masque « compteur» |
| Ajouter un document | GEN0008 |
| Éditer un document | GEN0009 |
| Supprimer un document | GEN0010 |
| Ajouter règlement | Régler une partie ou la totalité d’un montant du schéma de financement |
| Ajouter paiement | Rembourser une partie ou la totalité d’un montant pour le client |
Créé avec HelpNDoc Personal Edition: [Créer des fichiers d'aide pour la plateforme Qt Help](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creez-des-fichiers-daide-pour-le-framework-qt-help)

#### RAPPORTS <a id="page-rapports-com"></a>
_Page : `RAPPORTS_COM.html` — Module Vente › TABLEAUX DE BORDS_

Créé avec HelpNDoc Personal Edition: [Créer facilement des fichiers Qt Help](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

##### LISTE DOSSIERS <a id="page-comtdbfolderwithclientinfos"></a>
_Page : `ComTdbFolderWithClientInfos.html` — Module Vente › TABLEAUX DE BORDS › RAPPORTS_

| Dossier | Article | Client Principal | Téléphone | Portable | e-mail | Adresse | Ville | Pays | Statut | Frais syndic | Reste syndic | Concrétisation | Minute | Remise de clés | N°Titre foncier |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Code dossier de vente | Code article | Description client principal | Numéro de téléphone client principal | Numéro de portable client principal | E-mail client principal | Adresse client principal | Ville client principal | Pays client principal | Statut dossier de vente | Frais de syndic | Reste à régler du frais de syndic | Date concrétisation | Date Minute/Contrat | Date remise de clés | N°Titre foncier de l’article |
Remarque : L’utilisateur peut choisir la tranche dont il souhaite afficher sa liste des dossiers de vente.
Créé avec HelpNDoc Personal Edition: [Documentation Qt Help facile](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creez-des-fichiers-daide-pour-le-framework-qt-help)

##### SUVI HEBDOMADAIRE <a id="page-comtdbweeklymonitoring"></a>
_Page : `ComTdbWeeklyMonitoring.html` — Module Vente › TABLEAUX DE BORDS › RAPPORTS_

### SUIVI HEBDOMADAIRE

### CONCRETISATIONS

| Tranche | Produit | Client | Nature | Date concrétisation | Commercial | Prix Vente TTC | Remise | Avance réglée | Superficie (m²) | Superficie sous articles | Prix/ m² TTC |
|---|---|---|---|---|---|---|---|---|---|---|---|
| Liste des dossiers qui ont une date « Concrétisation» non nulle dont le statut >= « Concrétisation» et date « Concrétisation» date dans la période. |  |  |  |  |  |  |  |  |  |  |  |

### DESISTEMENTS

| Tranche | Produit | Client | Nature | Date désistement | Prix Vente TTC | Remise | A rembourser | Détails |
|---|---|---|---|---|---|---|---|---|
| Liste des dossiers dont le statut = « Désistement » et la date « Désistement » dans la période. |  |  |  |  |  |  |  |  |

### VERSEMENTS

| Programmes | Tranches | Montant |
|---|---|---|
| ∑ des encaissements - ∑ des décaissements<br> Remarque : Les encaissements et les décaissements qui ne sont pas des frais et qui non sont pas paramétré comme un règlement non comptabilisé. |  |  |
| Total |  |  |

### CONTRATS DE VENTE

| Programmes | Tranches | Quantité |
|---|---|---|
| Liste de dossiers qui ont une date « Minute/Contrat » non nulle dont le statut >= « Minute/Contrat » et la date« Minute/Contrat » dans la période. |  |  |
| Total |  |  |

### LIVRAISONS

| Programmes | Tranches | Quantité |
|---|---|---|
| Liste de dossiers qui ont une date « Livraison/Remise des clés » non nulle dont le statut >= « Livraison/Remise des clés » et la date « Livraison/Remise des clés » dans la période |  |  |
| Total |  |  |
Créé avec HelpNDoc Personal Edition: [Avantages d'un outil de création d'aide](https://www.helpauthoringsoftware.com/fr)

##### COMPARAISON DES VENTES <a id="page-comtdbsalecomparaison"></a>
_Page : `ComTdbSaleComparaison.html` — Module Vente › TABLEAUX DE BORDS › RAPPORTS_

[image : NouvelElement225.png]
[image : NouvelElement224.png]

|  | Encaissement | Chiffre d’affaire TTC |  |  |  |  |  |
|---|---|---|---|---|---|---|---|
| Année | Mois | Prévisions de base | Prévisions de vente | Réel (*) | Prévisions de base | Prévisions de vente | Réel (*) |
| Années | Mois | = Total versement à partir des prévisions de base de la tranche de module Stock | = Total versement à partir des prévisions de module Vente | = ∑ des affectations | = Chiffre d’affaire TTC à partir des prévisions de base de la tranche de module Stock | = Chiffre d’affaire TTC à partir des prévisions de module Vente | ∑ des prix de vente des dossiers qui ont un statut >= « Minute », non annulés et non désistés et qui ont une date minute non nulle |
Remarque : L’utilisateur peut choisir le projet dont il souhaite afficher la comparaison des ventes par année et par mois
(*) Sans tenir compte des frais et des pièces non comptabilisées
Créé avec HelpNDoc Personal Edition: [Créer de la documentation iPhone facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites/generation-de-site-web-iphone)

##### SUVI HEBDOMADAIRE DES PREVISIONS <a id="page-comtdbsuivihebdoprevision"></a>
_Page : `ComTdbSuiviHebdoPrevision.html` — Module Vente › TABLEAUX DE BORDS › RAPPORTS_

| Programme | Analyse de la semaine du J au J+7 | Analyse mois (Cumul réalisation / Visites –Au ) |  |  |  |  |  |  |  |  |  |  |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Tranche | Stock à la date J | Cumul<br> (Lancement de projet) | Prévision de la semaine | Réalisation | Écart | Nombre de visite | Total versements de la semaine | Prévision du mois | Réalisation du mois | Écart | Nombre de visite | Total versements du mois |
|  | Stock à la date J | Nombre d’articles concrétisé à la date J | Prévision du mois / 4 | Stock à la date J – Stock à la date J+7 | Réalisation de la semaine – Prévision de la semaine | Nombre de prospections de type visite de la semaine | ∑Encaissement - ∑Décaissement de la semaine (sans tenir compte les frais.) | Prévision du mois | Stock à la date du début – Stock à la date Fin du mois | Réalisation du mois – Prévision du mois | Nombre de prospections de type visite du mois | ∑Encaissement - ∑Décaissement du mois (sans tenir compte les frais.) |
|  | %Réalisation | = (Nombre de réalisation de la semaine/ nombre de visite de la semaine)*100 |  | % Réalisation | = (Nombre de réalisation du mois/ nombre de visite du mois)*100 |  |  |  |  |  |  |  |
| TOTAL GENERAL |  |  |  |  |  |  |  |  |  |  |  |  |
(*) Sans tenir compte des frais et des pièces non comptabilisées
Remarque :

- L’utilisateur peut choisir la date (J) de suivi hebdomadaire des prévisions (Par défaut c’est la date début de la semaine en cours).
- Le stock à la date j et le cumul lancement de projet sont comptabilisés lorsque la date de commercialisation est inférieure ou égale à la date J choisi par l’utilisateur
- Stock à une date : ∑ des entrées - ∑ des sorties
- Une sortie du stock : Un dossier concrétisé ou un transfert produit (basée sur la date « Concrétisation/Promesse » ou transfert)
- Une entrée du stock : Un dossier résilié ou annulé ou un transfert produit (basée sur la date annulation ou transfert)
Exemple :
Si on a choisi de faire le suivi hebdomadaire à la date J pour un stock total de 150 articles
Si la date J < La date de commercialisation :

- Le stock à la date J=0
- Cumul de lancement projet =0
Si la date J >= La date de commercialisation et on a fait 50 « Concrétisation/Promesse »

- Le stock à la date J=100 (nombre d’article total – nombre de « Concrétisation /promesse »)
- Cumul de lancement projet = 50
Créé avec HelpNDoc Personal Edition: [Environnement de création d'aide complet](https://www.helpndoc.com/fr/outils-creation-aide)

##### ACTIVITE COMMERCIALE PAR MOIS ET PAR ANNEE <a id="page-comtdbactiviteparmois"></a>
_Page : `ComTdbActiviteParMois.html` — Module Vente › TABLEAUX DE BORDS › RAPPORTS_

L’utilisateur peut choisir l’année dont il souhaite afficher les détails d’activités commerciales par mois, par défaut le système affiche les données de l’année en cours.
L’utilisateur peut choisir une ou plusieurs tranches (Si aucune tranche n’est choisie, le système affiche les données de toutes les tranches)
(*) Remarque : Sans tenir compte des frais et des pièces non comptabilisées
ACTIVITE COMMERCIAL PAR MOIS

|  | C.A. Montant TTC | Encaissement Montant<br> Concrétisation et C.A. | Concrétisation quantité | Finalisation quantité | Concrétisation Montant TTC |  |  |  |  |  |
|---|---|---|---|---|---|---|---|---|---|---|
| Mois | Prévisionnel | Réel | Prévisionnel | Réel (*) | Prévisionnel | Réel | Prévisionnel | Réel | Prévisionnel | Réel (*) |
| Janvier | = Chiffre d’affaire TTC à partir des prévisions de module vente | = ∑ des prix de vente des dossiers dont le statut >= Minute non annulés et non résiliés et dont la date minute non nulle | = Versement Total à partir des prévisions de module vente | =∑ des affectations d’encaissement - ∑ des affectations de décaissement | = Quantité Concrétisation à partir des prévisions de module vente | = Nombre de dossier de vente dont le statut >= « Concrétisation» non annulés et non résiliés et dont la date concrétisation non nulle | = Quantité Finalisation à partir des prévisions de module vente | = Nombre de dossier de vente dont le statut >= « Remise des clés» et dont la date de remise des clés non nulle | = Versement concrétisation<br> à partir des prévisions de module vente | =∑ des affectations d’encaissement - ∑ des affectations de décaissement<br>dont le statut >= « Concrétisation» non annulés et non résiliés et dont la date concrétisation non nulle<br>Avec affectation au document de type avance et dans le cadre de la concrétisation des dossiers (Pré réservation et avance) |
| Février |  |  |  |  |  |  |  |  |  |  |
| Mars |  |  |  |  |  |  |  |  |  |  |
| Avril |  |  |  |  |  |  |  |  |  |  |
| Mai |  |  |  |  |  |  |  |  |  |  |
| Juin |  |  |  |  |  |  |  |  |  |  |
| Juillet |  |  |  |  |  |  |  |  |  |  |
| Août |  |  |  |  |  |  |  |  |  |  |
| Septembre |  |  |  |  |  |  |  |  |  |  |
| Octobre |  |  |  |  |  |  |  |  |  |  |
| Novembre |  |  |  |  |  |  |  |  |  |  |
| Décembre |  |  |  |  |  |  |  |  |  |  |
| TOTAL |  | A0 |  | B0 |  | C0 |  | D0 |  | Z0 |
| DONNEES DE BASE | A1 : Valorisation définitive du stock |  | B1 = A1 |  | C1 : Le nombre total d’article mis à la vente |  | D1 = C1 |  |  |  |
| FACTURE ANNEE-1 | A2 : ∑ des prix de vente des dossiers dont le statut >= Minute non annulés et non résiliés et dont la date minute non nulle dans l’année-1 | A2/A1 % | (*) B2 : ∑ des affectations d’encaissement - ∑ des affectations de décaissement date = Année-1 | B2/B1 % | C2 : Nombre de dossier dont le statut >= « Concrétisation» non annulés et non résiliés et dont la date concrétisation non nulle et dans l’année-1 |  | D2 : Nombre de dossier dont le statut >= « Remise des clés» et dont la date Remise des clés non nulle et dans l’année-1 |  |  |  |
| CUMUL DEJA FACTURE | A3 : ∑ des prix de vente des dossiers dont le statut >= Minute non annulés et non résiliés et dont la date minute non nulle et < année | A3/A1 % | (*) B3 : ∑ des affectations d’encaissement - ∑ des affectations de décaissement date < Année | B3/B1 % | C3 : Nombre de dossier dont le statut >= « Concrétisation» non annulés et non résiliés et dont la date concrétisation non nulle et < année |  | D3 : Nombre de dossier dont le statut >= « Remise des clés» dont la date remise des clés non nulle et < année |  |  |  |
| TOTAL FACTURE | A4 : A0 + A3 | A4/A1% | B4: B0 + B3 | B4/B1% | C4 : C0 + C3 |  | D4 : D0 + D3 |  |  |  |
| RESTE | A1 - A4 |  | B1 - B4 |  | C1 - C4 |  | D1 - D4 |  |  |  |

| CUMUL A CE JOUR | D0 : ∑ Montant C.A prévisionnel TTC au mois courant-1 +( Cumul du mois/30 * date ) | D1 = A0 | E0:∑ Montant des encaissements prévisionnel au mois courant-1 +( Cumul du mois courant/30 * date) | E1= B0 | F0:∑Quantité Concrétisation prévisionnel au mois courant-1 +( Cumul du mois/30 * date) | F1 = C0 | G0:∑Quantités finalisation prévisionnelles au mois courant-1 + (Cumul du mois/30 * date) | G1 = D0 | H0:ΣConcrétisation Montant TTC prévisionnelles au mois courant-1 + (Cumul du mois/30 * date) | H1 = Z0 |
|---|---|---|---|---|---|---|---|---|---|---|
|  |  | D1/D0 *100 |  | E1/E0 *100 |  | F1/F0 *100 |  | G1/G0 *100 |  | H1/H0 *100 |
Date: Exemple si on est le 20-09-2016, le système va multiplier le résultat de Cumul du mois/30 par 20
Remarque: Si l'utilisateur choisi l'année courante le système applique les règles qui sont mentionnées dans les colonnes de la ligne "CUMUL A CE JOUR", sinon il copie les valeurs de la ligne "TOTAL".

| Mois | Frais de dossier | Frais de transfert | Frais de désistement | Total |
|---|---|---|---|---|
| Janvier | ∑ des affectations d’encaissement de Frais de dossiers du mois(A) | ∑ des affectations d’encaissement des frais de transfert du mois (B) | ∑ des montants des documentes de type Frais de désistement du mois (C) | (A)+(B)+(C) par mois |
| Février |  |  |  |  |
| Mars |  |  |  |  |
| Avril |  |  |  |  |
| Mai |  |  |  |  |
| Juin |  |  |  |  |
| Juillet |  |  |  |  |
| Août |  |  |  |  |
| Septembre |  |  |  |  |
| Octobre |  |  |  |  |
| Novembre |  |  |  |  |
| Décembre |  |  |  |  |
| Total | Total encaissements des frais de dossiers de l’année (E) | Total encaissement des frais de transfert de l’année(F). | Total des montants des documentes de type Frais de désistement de l’année(J). | (E)+(F)+(J) de l'année |
Activité commerciale par moi pour l'année (Choisi): Chiffre d'affaires TTC des tranches choisies
[image : NouvelElement34.png]
Activité commerciale par moi pour l'année (choisi) : Encaissements des tranches choisies
[image : NouvelElement33.png]
Activité commerciale par mois pour l'année (choisi): Concrétisation quantité des tranches choisies
[image : NouvelElement32.png]
Activité commerciale par mois pour l'année (choisi): Finalisation quantité des tranches choisies
[image : NouvelElement31.png]
Activité commerciale par mois pour l'année (choisi): Montant concrétisation pour les tranches choisies
[image : NouvelElement30.png]
Créé avec HelpNDoc Personal Edition: [Éditeur complet de livres électroniques ePub](https://www.helpndoc.com/fr/creer-des-livres-numeriques-epub)

##### ACTIVITE COMMERCIALE PAR PROJET ET PAR ANNEE <a id="page-comtdbactiviteparprojet"></a>
_Page : `ComTdbActiviteParProjet.html` — Module Vente › TABLEAUX DE BORDS › RAPPORTS_

| Programme | Tranche | Concrétisation | Livraisons | Désistement | Versement | CA TTC Année |
|---|---|---|---|---|---|---|
|  |  | = Nombre de dossier de vente dont le statut >= « Concrétisation» et dont la date concrétisation non nulle | = Nombre de dossiers dont le statut est >= « Remise des clés » et qui ont une date « remise de clefs » non nulle | = Nombre de dossiers dont le statut = « Désistement » et la date désistement non nulle | =∑ des affectations d’encaissement - ∑ des affectations de décaissement sans tenir compte des frais et des règlements Non comptabilisés | =∑ montants des dossiers dont le statut >= « Minute/Contrat » non annulés et non résiliés et qui ont une date « Minute/Contrat » non nulle |
| TOTAL |  |  |  |  |  |  |
Remarque : L’utilisateur peut choisir l’année dont il souhaite afficher les activités commerciales par programme, par défaut le système affiche les données de l’année courante.
Créé avec HelpNDoc Personal Edition: [Créer des fichiers d'aide pour la plateforme Qt Help](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creez-des-fichiers-daide-pour-le-framework-qt-help)

##### RYTHME DES VENTES <a id="page-comtdbsalesrythme"></a>
_Page : `ComTdbSalesRythme.html` — Module Vente › TABLEAUX DE BORDS › RAPPORTS_

| RESERVATIONS (Qté) | Programmes, Tranches |  |
|---|---|---|
| Année | Mois | Programme |
| Tranches |  |  |
|  |  | =Nombre de dossiers dont le statut >= « Réservation» et qui ont une date « Réservation » non nulle |

| CONCRETISATIONS (Qté) | Programmes, Tranches |  |
|---|---|---|
| Année | Mois | Programme |
| Tranches |  |  |
|  |  | =Nombre de dossiers dont le statut >= « Concrétisation» et qui ont une date « Concrétisation» non nulle |

| CONTRATS DE VENTE (Qté) | Programmes, Tranches |  |
|---|---|---|
| Année | Mois | Programme |
| Tranches |  |  |
|  |  | =Nombre de dossiers dont le statut >= « Minute» et qui ont une date « Minute» non nulle |

| LIVRAISONS (Qté) | Programmes, Tranches |  |
|---|---|---|
| Année | Mois | Programme |
| Tranches |  |  |
|  |  | = Nombre de dossiers dont le statut est >= « Remise des clés » et qui ont une date « Remise de clefs » non nulle |

| DESISTEMENTS (Qté) | Programmes, Tranches |  |
|---|---|---|
| Année | Mois | Programme |
| Tranches |  |  |
|  |  | = Nombre de dossiers dont le statut est = « Désistement » et qui ont une date « Désistement/ Annulation » non nulle |

| ANNULATIONS (Qté) | Programmes, Tranches |  |
|---|---|---|
| Année | Mois | Programme |
| Tranches |  |  |
|  |  | = Nombre de dossiers dont le statut est = « Annulation » et qui ont une date « Désistement/ Annulation » non nulle |
Créé avec HelpNDoc Personal Edition: [Générateur de documentation et EPub facile](https://www.helpndoc.com/fr)

##### RYTHME DE CONCRETISATIONS/LIVRAIONS/ENCAISSEMENTS <a id="page-comtdbsalesrythmeconcretisation"></a>
_Page : `ComTdbSalesRythmeConcretisation.html` — Module Vente › TABLEAUX DE BORDS › RAPPORTS_

Rythme de concrétisations (*)

| Concrétisation | Programmes, Tranches |
|---|---|
| N°MOIS | Programme |
| Tranche |  |
| = Date de la « Concrétisation/Promesse »en cours – la date de la première « Concrétisation » du (/des) projets sélectionnés | = Nombre de « Concrétisation/Promesse » / nombre total d’articles du tranche |
(*) En tenant compte les dossiers annulés et désistés
Rythme de livraisons

| LIVRAISON (%) | Programmes, Tranches |
|---|---|
| N°MOIS | Programme |
| Tranche |  |
| = Date de la « Livraison/Remise des clés » en cours – la date de la première « Livraison/Remise des clés »du (/des) projets sélectionnés | = Nombre de « Livraison/Remise des clés »/ nombre total d’articles du tranche |
Rythme d'encaissements (*) (**)

| Encaissement (%) | Programmes, Tranches |
|---|---|
| N°MOIS | Programme |
| Tranche |  |
| = Date « Encaissement » en cours – la date du premier « Encaissement »du (/des) projets sélectionnés | = (∑Encaissements - ∑ Décaissements) par tranche) / ∑ des prix de vente des dossiers de vente+ ∑ des prix de vente des articles libres par tranche |
(*) Sans tenir compte les frais et les pièces non comptabilisées
(**) Sans tenir compte les dossiers annulés et désistés
Créé avec HelpNDoc Personal Edition: [Créer des fichiers d'aide pour la plateforme Qt Help](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creez-des-fichiers-daide-pour-le-framework-qt-help)

##### RYTHME DE CONCRETISATIONS/LIVRAIONS/ENCAISSEMENTS(COMPARAISON) <a id="page-comtdbsalesrythmelivraison"></a>
_Page : `ComTdbSalesRythmeLivraison.html` — Module Vente › TABLEAUX DE BORDS › RAPPORTS_

Rythme de concrétisations (*) (**)

| Concrétisation | Programmes, Tranches |
|---|---|
| N°MOIS | Programme |
| Tranche |  |
| = Date de la « Concrétisation/Promesse »en cours – la date de la première « Concrétisation » du (/des) projets sélectionnés | = Nombre de « Concrétisation/Promesse » / nombre total d’articles du tranche |
(*) En tenant compte les dossiers annulés et désistés
(**) Sans tenir en compte le décalage entre les dates de concrétisations des différentes tranches
Rythme de livraisons (*)

| LIVRAISON (%) | Programmes, Tranches |
|---|---|
| N°MOIS | Programme |
| Tranche |  |
| = Date de la « Livraison/Remise des clés » en cours – la date de la première « Livraison/Remise des clés »du (/des) projets sélectionnés | = Nombre de « Livraison/Remise des clés »/ nombre total d’articles du tranche |
(*) Sans tenir en compte le décalage entre les dates de livraisons des différentes tranches
Rythme d'encaissements (*) (**) (***)

| Encaissement (%) | Programmes, Tranches |
|---|---|
| N°MOIS | Programme |
| Tranche |  |
| = Date « Encaissement » en cours – la date du premier « Encaissement »du (/des) projets sélectionnés | = (∑Encaissements - ∑ Décaissements) par tranche) / ∑ des prix de vente des dossiers de vente+ ∑ des prix de vente des articles libres par tranche |
(*) Sans tenir compte les frais et les pièces non comptabilisées
(**) Sans tenir compte les dossiers annulés et désistés
(***) Sans tenir en compte le décalage entre les dates d'encaissements des différentes tranches
Créé avec HelpNDoc Personal Edition: [Environnement de création d'aide complet](https://www.helpndoc.com/fr/outils-creation-aide)

##### SUIVI CHIFFRE D'AFFAIRES <a id="page-suivichiffredaffaires"></a>
_Page : `SUIVICHIFFREDAFFAIRES.html` — Module Vente › TABLEAUX DE BORDS › RAPPORTS_

[image : NouvelElement89.png]
Prévision du chiffre d'affaire pour les dossiers avec envoi au notaire pour l'année...
Prévisionnel (Date envoi notaire + X jours): Chiffre d’affaires des dossiers non annulés et non désistés dont la date = « Envoi notaire » différent de nulle + X jours
Réel (Date minute /Contrat) : Chiffre d’affaires des dossiers non annulés et non désistés dont la date « Minute/Contrat » différent de nulle
Remarques :

- Le graphe visualise le chiffre d’affaires prévisionnel et le chiffre d’affaires réel des mois de l’année choisi par l’utilisateur, par défaut le système prend l’année en cours
- Chiffre d’affaire = ∑ des prix de vente avec remise des dossiers dont le statut >= « Minute/Contrat »
- Date « Minute/Contrat » prévisionnel = Date « Envoi notaire » + X jours
- Les règlements de type « Frais » ne sont pas calculés dans le chiffre d’affaires
- Les règlements qui sont paramétrés « Nom comptabilisé » ne sont pas calculés dans le chiffre d’affaires
- X nombre de jours est paramétré dans la tranche de module stock
[image : NouvelElement88.png]
Prévision des encaissements du reste à régler pour l'année...
Prévisionnel (Date minute + Y jours) : ∑ des règlements des dossiers non annulés et non désistés dont le statut = « Minute/contrat » et la date « Minute/Contrat » + Y jours différent de nulle
Réel (Date pièce de règlement) : = ∑ des règlements des dossiers non annulés et non désistés dont le statut = « Minute/contrat » » et la date « Minute/Contrat » différent de nulle
Remarque :

- Le graphe visualise le montant des encaissements prévisionnel et le montant des encaissements réel des mois de l’année choisi par l’utilisateur, par défaut le système prend l’année en cours
- Les règlements de type « Frais » ne sont pas calculés dans le chiffre d’affaires
- Les règlements qui sont paramétrés « Nom comptabilisé » ne sont pas calculés dans le chiffre d’affaires
- X nombre de jours est paramétré dans la tranche de module stock
[image : NouvelElement87.png]
Prévision du chiffre d'affaire pour les dossiers en concrétisation basée sur la date d’envoi au notaire prévisionnelle +60 jours pour l'année...
Confirmée : ∑ des prix de vente avec remise des dossiers non annulés et non désistés qui ont une date d’envoi notaire prévisionnel non nulle(*) confirmés
Nom confirmées : ∑ des prix de vente avec remise des dossiers non annulés et non désistés qui ont une date d’envoi notaire prévisionnel non nulle (*) non confirmé
Remarque (*) :

- Les prévisions de chiffre d’affaires sont basées sur la date d’envoi notaire prévisionnel + X jours pour les dossiers dont le statut = « Concrétisation/Promesse »
- X nombre de jours est paramétré dans la tranche de module stock.
Créé avec HelpNDoc Personal Edition: [Produire des aides en ligne pour les applications Qt](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creez-des-fichiers-daide-pour-le-framework-qt-help)

##### DETAIL CHIFFRE D'AFFAIRES <a id="page-comtdbdetailschiffreaffaire"></a>
_Page : `ComTdbDetailsChiffreAffaire.html` — Module Vente › TABLEAUX DE BORDS › RAPPORTS_

| Programme | Tranche | C.A. de base par projet TTC<br> (A) | Déjà facturé<br> (B) | Reste à facturer | C.A. Prévisionnel Année TTC (C) | C.A. Réel Année TTC (D) | Écart |
|---|---|---|---|---|---|---|---|
|  |  | =Valorisation définitive du stock | = ∑ des prix de vente des dossiers dont le statut >= Minute non annulés et non résiliés et dont la date minute non nulle < année | = (A)- (B) | =Montant de C.A Prévisionnel<br> à partir des prévisions de module vente pour l’année | = ∑ des prix de vente des dossiers dont le statut >= Minute non annulés et non résiliés et dont la date minute non nulle = année | = D-C |
| TOTAL |  |  |  |  |  |  |  |
Remarques :

- L’utilisateur peut choisir l’année et la/les tranche(s) dont il souhaite afficher les détails chiffre d’affaires; par défaut le système affiche les données de l’année courante de toutes les tranches.

- Pour Le chiffre d'affaire prévisionnel année TTC, le système applique un coefficient de prévision (par défaut 100%) de la manière suivante:
- Chiffre d'affaires prévisionnel année TTC = (Chiffre d'affaires prévisionnel année TTC * coefficient de prévision) /100
Créé avec HelpNDoc Personal Edition: [Créer de la documentation iPhone facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites/generation-de-site-web-iphone)

##### SITUATION GLOBALE DES VENTES <a id="page-comtdbsalessituationglobal"></a>
_Page : `ComTdbSalesSituationGlobal.html` — Module Vente › TABLEAUX DE BORDS › RAPPORTS_

|  | Trésorerie |  |  |  |  |  |  |  |  |  |  |  |  |  |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
|  | Nombre d’article | Valorisation stock TTC | Chiffre d’affaires TTC | Reste à Encaisser (libre) | Reliquat (Concrétisés) | Reliquat (Concrétisés) |  |  |  |  |  |  |  |  |
| Programme | Tranche | Total | Libre | Concrétisé | Vendus | Livrés | Provisoire | Libre | Concrétisé | Vendus | Versements (*) |  |  |  |
| P1 | T1 |  |  |  |  |  |  |  |  |  |  |  |  |  |
| T2 |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| TOTAL |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| TOTAL GENERAL |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| PROGRAMME | TRANCHE | =Nombre d’article dans la tranche (sans les sous article) dont la vocation est vente | = Nombre d’article libre + Nombre d’article réservé | = Nombre de dossiers non annulés et non désistés dont le statut est >= « concrétisation » et < au statut contrat (minute) et qui ont une date de concrétisation non nulle | =Nombre de dossiers non annulés et non désistés dont le statut est >= Contrat (minute) et qui ont une date minute non nulle | = Nombre de dossiers non annulés et non désistés dont le statut est >= Remise des clés et qui ont une date remise de clefs non nulle | = ∑ des montants définitifs des articles et des sous articles | = ∑ des valorisations stock définitives libre (plus les sous articles) + les dossiers dont le statut = réservation (en prenant en considération les remises) | = ∑ des dossiers non annulés et non désistés dont le statut est >= concrétisation et < Minute et qui ont une date concrétisation non nulle | =∑ des dossiers non annulé et non désistés dont le statut >= Minute et qui ont une date minute | =∑Encaissement - ∑Décaissement | =∑ des prix définitif des articles et des sous articles libres + le reste à encaisser pour les Réservation (A) | = ∑ reste à régler du prix de vente de dossier non annulés et non désistés dont le statut est >= Concrétisation qui ont une date concrétisation non nulle (B) | = (A) + (B) |
(*) Sans tenir compte des frais et des pièces non comptabilisées
Attention ! : Pour la colonne Chiffre d’affaires � Versements :

- Le montant peut ne correspond pas la ∑ des règlements - ∑ des paiements (Voir les affectations).

### 

### 
Créé avec HelpNDoc Personal Edition: [Écrire des livres électroniques ePub pour l'iPad](https://www.helpndoc.com/fr/creer-des-livres-numeriques-epub)

##### SUIVI GLOBALE DES VENTES <a id="page-comtdbsalesmonitoringglobal"></a>
_Page : `ComTdbSalesMonitoringGlobal.html` — Module Vente › TABLEAUX DE BORDS › RAPPORTS_

STOCK

|  | Articles | Sous articles (Affectés et non affectés) |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Nombre | Superficie (m²) (Définitive) | Nombre | Superficie (m²) (Définitive) |  |  |  |  |  |  |  |  |  |  |  |  |  |
| Tranches | Total<br> (A) | Concrétisé<br> (B) | Libre | Taux | Total<br> (C) | Concrétisé<br> (D) | Libre | Taux | Total<br> (E) | Concrétisé<br> (F) | Libre | Taux | Total<br> (G) | Concrétisé<br> (H) | Libre | Taux |
| Tranches | Nombre d’articles dans le stock dont la vocation est « Vente » par tranche | Nombre d’articles dont le statut >= « Concrétisation/Promesse » et qui par tranche | Nombre d’articles « Libres» + nombre d’articles « Réservés » par tranche | (B)/(A) * 100 | ∑ de superficies de tous les articles par tranche | ∑ Des superficies des articles dont le statut >= « Concrétisation /Promesse » par tranche | ∑ des superficies des articles « Libres » + ∑ des superficies des articles « Réservés » par tranche | (D)/(C) *100 | Nombre de sous articles dans le stock dont la vocation est « Vente » par tranche | Nombre de sous articles dont le statut >= « Concrétisation/Promesse » par tranche | Nombre de sous articles « Libre » + « Nombre de sous articles « Réservé » | (F)/(E) *100 | ∑ des superficies de tous les sous articles par tranche | ∑ des superficies des sous articles dont le statut >= « Concrétisation/Promesse » par tranche | ∑ des superficies des sous articles dont le statut « Libre » et « Réservation » | (H)/(G) *100 |
| Total |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
[image : NouvelElement93.png]
ETAT DES VENTES

| Nombre | État |  |  |  |  |  |  |  |  |  |
|---|---|---|---|---|---|---|---|---|---|---|
| Tranche | LIBRE | RESERVATION | CONCRETISATION | CREDIT EN COURS | CREDIT ACCORDE | ENVOI AU NOTAIRE | MINUTE | REMISE DE CLEFS | FORMALISME | Total |
|  | = Nombre d’articles dont le statut « libre » par tranche | = Nombre d’articles dont le statut « Réservation » par tranche | = Nombre d’articles dont le statut « Concrétisation » par tranche | = Nombre d’articles dont le statut « Crédit en cours » par tranche | = Nombre d’articles dont le statut « Crédit accordée » par tranche | = Nombre d’articles dont le statut « Envoi au notaire » par tranche | = Nombre d’articles dont le statut « Minute/Contrat » par tranche | = Nombre d’articles dont le statut « Remise de clefs » par tranche | = Nombre d’articles dont le statut « Formalisme » par tranche |  |
| Total |  |  |  |  |  |  |  |  |  |  |

| Pourcentage | État |  |  |  |  |  |  |  |  |  |
|---|---|---|---|---|---|---|---|---|---|---|
| Tranche | LIBRE | RESERVATION | CONCRETISATION | CREDIT EN COURS | CREDIT ACCORDE | ENVOI AU NOTAIRE | MINUTE | REMISE DE CLEFS | FORMALISME | Total |
|  | = (Nombre d’article dont le statut « libre »/ nombre d’article total d’une tranche)*100 | = (Nombre d’articles dont le statut « Réservation » par tranche / nombre d’article total d’une tranche)*100 | = (Nombre d’articles dont le statut « Concrétisation/Promesse »/ nombre d’article total d’une tranche | = (Nombre d’articles dont le statut « Crédit en cours » / nombre d’article total d’une tranche | = (Nombre d’articles dont le statut « Crédit accordée » / nombre d’article total d’une tranche | = (Nombre d’articles dont le statut « Envoi au notaire » nombre d’article total d’une tranche | = (Nombre d’articles dont le statut « Minute/Contrat » nombre d’article total d’une tranche | = (Nombre d’articles dont le statut « Remise de clefs » nombre d’article total d’une tranche | = (Nombre d’articles dont le statut « Formalisme » nombre d’article total d’une tranche |  |

|  | Prix de vente | Avance | Autofinancement | Crédit |  |  |  |  |  |  |  |  |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Tranche | Montant (A) | Réglé<br> (B) | Reste | Montant (C) | Réglé<br> (D) | Reste | Montant (E) | Réglé<br> (F) | Reste | Montant (G) | Réglé<br> (H) | Reste |
| Tranches | ∑ des prix de ventes avec remise des dossiers dont le statut > de « Libre » par tranche | ∑ de règlements des dossiers dont le statut > « Libre » par tranche | =(A) -(B) | ∑ des avances des dossiers dont le statut > « Libre » par tranche | ∑ des avances réglées des dossiers dont le statut > « Libre » par tranche | (C) – (D) | ∑ des autofinancements des dossiers dont le statut > de « Libre » par tranche | ∑ des autofinancements réglés des dossiers dont le statut > « Libre » par tranche | (E) – (F) | ∑ des crédits des dossiers dont le statut > « Libre » par tranche | ∑ des crédits réglés des dossiers dont le statut > « Libre » par tranche | (G) – (H) |
| Total |  |  |  |  |  |  |  |  |  |  |  |  |
| Recouvrement |  | (B)/(A) *100 |  |  | (D)/(C) *100 |  |  | (F) / (E) *100 |  |  | (H)/(G) *100 |  |

### AUTRE FRAIS

|  | Frais | Modifications |  |  |  |  |
|---|---|---|---|---|---|---|
| Tranches | Montant<br> (A) | Réglé<br> (B) | Reste | Montant<br> (C) | Réglé<br> (D) | Reste |
| Tranches | =∑ de montants des frais des dossiers dont le statut > « Libre » par tranche | =∑ de montants des frais réglés des dossiers dont le statut > « Libre » par tranche | = (A) – (B) | =∑ de montants des frais de modifications des dossiers dont le statut > « Libre » par tranche | =∑ de montants des frais de modifications réglés des dossiers dont le statut > « Libre » par tranche | =(C) – (D) |
| Total |  |  |  |  |  |  |
| Recouvrement |  | (B)/(A) *100 |  |  | (D)/(C) *100 |  |

|  | Remboursement pour désistement |  |  |
|---|---|---|---|
| Tranches | Montant<br> (A) | Réglé<br> (B) | Reste |
| Tranches | =∑ des montants de remboursement des dossiers dont le statut = « Désistement »par tranche | =∑ des montants de remboursement réglés des dossiers dont le statut = « Désistement »par tranche | (A)– (B) |
| Total |  |  |  |
| Recouvrement |  | (B)/(A) * 100 |  |
[image : NouvelElement244.png]
Suivi global des ventes par : catégories
Nombre d’article libres
Nombre d'article vendus
[image : NouvelElement245.png]
Suivi global des ventes par : types
Nombre d’article libres
Nombre d'article vendus
[image : NouvelElement246.png]
Suivi global des ventes par : Etages
Nombre d’article libres
Nombre d'article vendus
[image : NouvelElement247.png]
Suivi global des ventes par : Immeubles
Nombre d’article libres
Nombre d'article vendus
[image : NouvelElement248.png]
Suivi global des ventes par : type de présentation
Nombre d’article libres
Nombre d'article vendus
[image : NouvelElement249.png]
Suivi global des ventes par : orientation
Nombre d’article libres
Nombre d'article vendus
Créé avec HelpNDoc Personal Edition: [Générateur de documentations PDF gratuit](https://www.helpndoc.com/fr)

##### CONCRETISATION PAR RAPPORT AUX LIBRE <a id="page-comtdbconcretisationandfree"></a>
_Page : `ComTdbConcretisationAndFree.html` — Module Vente › TABLEAUX DE BORDS › RAPPORTS_

[image : NouvelElement94.png]
Concrétisés = Nombre d’articles dont le statut >= « Concrétisation/Promesse » non annulés et non désistés et la date concrétisation non nul d’une tranche.
Libre = Stock total (D’une tranche) – Nombre d’articles concrétisés.
Remarque : L’utilisateur peut personnaliser les tranches à visualiser.

### 

### 
Créé avec HelpNDoc Personal Edition: [Générer des livres électroniques EPub facilement](https://www.helpndoc.com/fr/creer-des-livres-numeriques-epub)

##### CONCRETISATION ET ENCAISSEMENT <a id="page-comtdbconcretandsetlement"></a>
_Page : `ComTdbConcretAndSetlement.html` — Module Vente › TABLEAUX DE BORDS › RAPPORTS_

[image : NouvelElement95.png]
Graphe de Concrétisations et Encaissements
(A) : ∑ Dossiers dont le statut >= « Concrétisation/Promesse » non annulés et non désistés et la date « Concrétisation/Promesse » < Date choisi
(B) : Nombre de concrétisation avant la date choisi à partir de prévisions de base
(C) : ((A)/(B))*100
(D): (A) /(I)
(E) : ∑ Encaissements total à partir de prévisions de base avant la date choisi.
(F)(*) : ∑ Encaissements - ∑Décaissements
(G) : ((F)/(E))*100
(H) : 1-((A)/(B))*100
(I) : Nombre de prospections de types visite
Remarques :

- Le système visualise le nombre de concrétisations (Prévisionnel et réel) et le montant des encaissements (Prévisionnel et réel) du / des programme(s) jusqu’à la date choisi par l’utilisateur ; par défaut le système prend la date de jour.
- Pour la quantité des concrétisations prévisionnelles et le montant d'encaissement prévisionnel, le système applique un coefficient de prévision (par défaut 100%) de la manière suivante:
- Quantité concrétisation prévisionnelle = (Quantité concrétisation * coefficient de prévision) / 100
- Encaissement prévisionnel = (Montant encaissement prévisionnel * coefficient de prévision) /100

- (*) sans tenir compte les frais et les pièces non comptabilisées.
Créé avec HelpNDoc Personal Edition: [Qu'est-ce qu'un outil de création d'aide ?](https://www.helpauthoringsoftware.com/fr)

##### CONCRETISATION PAR COMMERCIAL <a id="page-comtdbconcretpercommercial"></a>
_Page : `ComTdbConcretPerCommercial.html` — Module Vente › TABLEAUX DE BORDS › RAPPORTS_

CONCRETISATION PAR COMMERCIAL PAR SEMAINE

|  | S1 (Semaine 1) | S2 | S… |
|---|---|---|---|
| Commercial | Nombre de dossiers dont le statut >= « Concrétisation/Promesse » non annulés et non désistés et qui ont une date « Concrétisation/Promesse » non nul effectué par un commercial |  |  |
| Total |  |  |  |
[image : NouvelElement98.png]
GRAPHE DE CONCRETISATION PAR COMMERCIAL PAR SEMAINES
CONCRETISATION PAR COMMERCIAL PAR MOIS

|  | 1 (Mois 1) | 2… | 12 |
|---|---|---|---|
| Commercial | Nombre de dossiers dont le statut >= « Concrétisation/Promesse » non annulés et non désistés et qui ont une date « Concrétisation/Promesse » non nul effectué par un commercial |  |  |
| Total |  |  |  |
[image : NouvelElement97.png]
GRAPHE DE CONCRETISATION PAR COMMERCIAL PAR MOIS
CONCRETISATION PAR COMMERCIAL PAR AN

|  | 2013 (Année) | 2014 | … |
|---|---|---|---|
| Commercial | Nombre de dossiers dont le statut >= « Concrétisation/Promesse » non annulés et non désistés et qui ont une date « Concrétisation/Promesse » non nul effectué par un commercial |  |  |
| Total |  |  |  |
[image : NouvelElement96.png]
GRAPHE DE CONCRETISATION PAR AN
Créé avec HelpNDoc Personal Edition: [Générer facilement des livres électroniques Kindle](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creer-ebooks-pour-kindle-amazon)

##### SUIVI DES PREVISIONS DE VENTE <a id="page-comtdbprevisionstate"></a>
_Page : `ComTdbPrevisionState.html` — Module Vente › TABLEAUX DE BORDS › RAPPORTS_

Nombre de concrétisation :

| Mois | Quantité Prev. De base | Anc.Qté. Prév.<br> (A) | Qté. Prév.<br> (B) | Qté. Réelle<br> (C) | Cumul Prév<br> (D) | Cumul réel<br> (E) | Pourc. Réalisé (F) | Pourc. Cumulé (G) | Rentabilité<br> (H) |
|---|---|---|---|---|---|---|---|---|---|
| Janvier | Quantité concrétisation à partir de prévision base de la tranche de module stock | Quantité concrétisation à partir de l’historique de prévisions de module vente | Quantité concrétisation à partir de prévision de module vente | = Nombre de dossiers dont le statut >= « Concrétisation/Promesse »qui ont une date « Concrétisation/Promesse » non nulle | = Quantité prévisionnel + Cumule prévisionnel du mois précédent<br> (Initialement pour le mois janvier cumul prévisionnel = Quantité prévisionnel | = Quantité réelle + Cumule réel du mois précédent<br> (Initialement pour le mois janvier cumul réel = Quantité réel | =(C) /(B) | =(E) /(D) | (C)/ Max Cumul réel |
| FEVRIER |  |  |  |  |  |  |  |  |  |
| MARS |  |  |  |  |  |  |  |  |  |
| AVRIL |  |  |  |  |  |  |  |  |  |
| MAI |  |  |  |  |  |  |  |  |  |
| JUIN |  |  |  |  |  |  |  |  |  |
| JUILLET |  |  |  |  |  |  |  |  |  |
| AOUT |  |  |  |  |  |  |  |  |  |
| SPTEMBRE |  |  |  |  |  |  |  |  |  |
| OCTOBRE |  |  |  |  |  |  |  |  |  |
| NOVEMBRE |  |  |  |  |  |  |  |  |  |
| DECEMBRE |  |  |  |  |  |  |  |  |  |
Remarque : Tous les données sont pour les tranches sélectionnées du mois correspondant, par défaut le système affiche les données de l’année courante.
[image : NouvelElement106.png]
Montants des concrétisations

| Mois | Montant Prev. De base | Anc. Montant. Prév.<br> (A) | Montant. Prév.<br> (B) | Montant. Réelle<br> (C) | Cumul Prév<br> (D) | Cumul réel<br> (E) | Pourc. Réalisé (F) | Pourc. Cumulé (G) | Rentabilité<br> (H) |
|---|---|---|---|---|---|---|---|---|---|
| Janvier | Montants de concrétisation à partir de prévision de base | Montants de concrétisation à partir de l’historique de prévisions | Montants de concrétisation à partir des prévisions | = ∑des montants dossiers dont le statut >=« Concrétisation/Promesse » qui ont une date « Concrétisation/Promesse » non nulle | = Montant prévisionnel + Montant Cumule prévisionnel du mois précédent<br> (Initialement pour le mois janvier cumul prévisionnel = Quantité prévisionnel | = Montant réelle + Montant Cumule réel du mois précédent<br> (Initialement pour le mois janvier cumul réel = Montant réel | =(C) /(B) | =(E) /(D) | (C)/ Cumul du montant décembre |
| FEVRIER |  |  |  |  |  |  |  |  |  |
| MARS |  |  |  |  |  |  |  |  |  |
| AVRIL |  |  |  |  |  |  |  |  |  |
| MAI |  |  |  |  |  |  |  |  |  |
| JUIN |  |  |  |  |  |  |  |  |  |
| JUILLET |  |  |  |  |  |  |  |  |  |
| AOUT |  |  |  |  |  |  |  |  |  |
| SPTEMBRE |  |  |  |  |  |  |  |  |  |
| OCTOBRE |  |  |  |  |  |  |  |  |  |
| NOVEMBRE |  |  |  |  |  |  |  |  |  |
| DECEMBRE |  |  |  |  |  |  |  |  |  |
Remarque : Tous les données sont pour les tranches sélectionnées du mois correspondant, par défaut le système affiche les données de l’année courante.
[image : NouvelElement105.png]
Quantité Chiffre d’affaires

| Mois | Quantité Prev. De base | Anc.Qté. Prév.<br> (A) | Qté. Prév.<br> (B) | Qté. Réelle<br> (C) | Cumul Prév<br> (D) | Cumul réel<br> (E) | Pourc. Réalisé (F) | Pourc. Cumulé (G) | Rentabilité<br> (H) |
|---|---|---|---|---|---|---|---|---|---|
| Janvier | Quantité finalisation à partir de prévision base de la tranche de module stock | Quantité finalisation à partir de l’historique de prévisions de module vente | ∑ des finalisations à partir de prévisions de module vente | = Nombre de dossiers dont le statut >= « Minute/Contrat » et qui ont une date « Minute/Contrat » non nulle | = Quantité prévisionnel + Cumule prévisionnel du mois précédent<br> (Initialement pour le mois janvier cumul prévisionnel = Quantité prévisionnel | = Quantité réelle + Cumule réel du mois précédent<br> (Initialement pour le mois janvier cumul réel = Quantité réel | =(C) /(B) | =(E) /(D) | (C)/ Cumul décembre |
| FEVRIER |  |  |  |  |  |  |  |  |  |
| MARS |  |  |  |  |  |  |  |  |  |
| AVRIL |  |  |  |  |  |  |  |  |  |
| MAI |  |  |  |  |  |  |  |  |  |
| JUIN |  |  |  |  |  |  |  |  |  |
| JUILLET |  |  |  |  |  |  |  |  |  |
| AOUT |  |  |  |  |  |  |  |  |  |
| SPTEMBRE |  |  |  |  |  |  |  |  |  |
| OCTOBRE |  |  |  |  |  |  |  |  |  |
| NOVEMBRE |  |  |  |  |  |  |  |  |  |
| DECEMBRE |  |  |  |  |  |  |  |  |  |
Remarque : Tous les données sont pour les tranches sélectionnées du mois correspondant, par défaut le système affiche les données de l’année courante.
[image : NouvelElement104.png]
Montant chiffre d’affaires

| Mois | Montant Prev. De base | Anc. Montant. Prév.<br> (A) | Montant. Prév.<br> (B) | Montant. Réelle<br> (C) | Cumul Prév<br> (D) | Cumul réel<br> (E) | Pourc. Réalisé (F) | Pourc. Cumulé (G) | Rentabilité<br> (H) |
|---|---|---|---|---|---|---|---|---|---|
| Janvier | Montant de finalisation à partir de prévision de base de la tranche de module stock | Montant de finalisation à partir de l’historique de prévisions de module vente | Montant de finalisation à partir des prévisions de module vente | = ∑ des montants dossiers dont le statut >=« Minute/Contrat » qui ont une date « Minute / Contrat » non nulle | = Montant prévisionnel + Montant Cumule prévisionnel du mois précédent<br> (Initialement pour le mois janvier cumul prévisionnel = Quantité prévisionnel | = Montant réelle + Montant Cumule réel du mois précédent<br> (Initialement pour le mois janvier cumul réel = Montant réel | =(C) /(B) | =(E) /(D) | (C)/ Cumul du montant décembre |
| FEVRIER |  |  |  |  |  |  |  |  |  |
| MARS |  |  |  |  |  |  |  |  |  |
| AVRIL |  |  |  |  |  |  |  |  |  |
| MAI |  |  |  |  |  |  |  |  |  |
| JUIN |  |  |  |  |  |  |  |  |  |
| JUILLET |  |  |  |  |  |  |  |  |  |
| AOUT |  |  |  |  |  |  |  |  |  |
| SPTEMBRE |  |  |  |  |  |  |  |  |  |
| OCTOBRE |  |  |  |  |  |  |  |  |  |
| NOVEMBRE |  |  |  |  |  |  |  |  |  |
| DECEMBRE |  |  |  |  |  |  |  |  |  |
Remarque : Tous les données sont pour les tranches sélectionnées du mois correspondant, par défaut le système affiche les données de l’année courante.
[image : NouvelElement103.png]
Créé avec HelpNDoc Personal Edition: [Nouvelles et informations sur les outils de logiciels de création d'aide](https://www.helpauthoringsoftware.com/fr)

##### TAUX DE TRANSFORMATION PAR COMMERCIAL <a id="page-comtdbcommercialtransformation"></a>
_Page : `ComTdbCommercialTransformation.html` — Module Vente › TABLEAUX DE BORDS › RAPPORTS_

| Commercial | PROGRAMME |  |  |  |  |  |  |  |
|---|---|---|---|---|---|---|---|---|
| Prospection | Total concrétisation | Visites | Réservations | Concrétisations | Annulations | Contrats signés ou livrés | Taux de transferts |  |
|  | = Nombre des prospections par commercial dans la période choisi | = Nombre de dossier dont le statut >= « Concrétisation » et < « Minute » non annulés et non désistés qui ont une date « Concrétisation » non nulle sans tenir compte la période choisi (B) | = Nombre de prospection dont le type est « Visite » dans la période (C) | = Nombre de réservation dans la période<br> sans tenir compte les annulations et les désistements dont le statut >= « Réservation » (D) | = Nombre de dossier dont le statut >= « Concrétisation/Promesse » et < « Minute » qui ont une date « Concrétisation /Promesse » non nulle dans la période choisi (E) | = Nombre d'annulations et des désistements des dossiers qui ont une date « Concrétisation » non nulle et ils ont un commercial affecté, sans tenir compte la période choisi (F) | Nombre de dossier dont le statut >= « Minute » non annulé, non désisté<br> et ont une date « Concrétisation » non nulle dans la période (G) | (E + G)/C * 100 |
| Total |  |  |  |  |  |  |  |  |
Remarque : On peut personnaliser le choix du/ des programme(s) et la période, par défaut le système prend la date de jour.
[image : NouvelElement108.png]
Graphe de taux de transformations par commercial
La figure ci-dessus représente le taux de transformation par commercial dans la période choisi.
[image : NouvelElement107.png]
Graphe de nombre « Visite », « Concrétisation/Promesse », « Réservation » et « Annulation » par commercial
La figure ci-dessus représente le nombre de « Visites », « Concrétisation/Promesse », « Réservation » et « Annulation » effectués par commercial.
Créé avec HelpNDoc Personal Edition: [Générateur gratuit de livres électroniques et documentation](https://www.helpndoc.com/fr)

##### REPARTION DES CLIENTS  <a id="page-comtdbrepartitionclient"></a>
_Page : `ComTdbRepartitionClient.html` — Module Vente › TABLEAUX DE BORDS › RAPPORTS_

[image : NouvelElement241.png]
Nombre de clients par profession qui ont un dossier de vente effectif avec la date concrétisation dans la période choisi / Nombre total des clients
Dossier de vente effectif = Dossier dont le statut >= « Concrétisation »
[image : NouvelElement240.png]
Nombre de clients par fourchette salariale qui ont un dossier de vente effectif avec la date concrétisation dans la période choisi / Nombre total des clients
Dossier de vente effectif = Dossier dont le statut >= « Concrétisation »
[image : NouvelElement239.png]
Nombre de clients par tranche d’âge qui ont un dossier de vente effectif avec la date concrétisation dans la période choisi / Nombre total des clients
Dossier de vente effectif = Dossier dont le statut >= « Concrétisation »
[image : NouvelElement242.png]
Nombre de clients par ville qui ont un dossier de vente effectif avec la date concrétisation dans la période choisi / Nombre total des clients
Dossier de vente effectif = Dossier dont le statut >= « Concrétisation »
Remarque: Si le système affiche "Autre < 1%: X %", c'est à dire qu'il y a X % de clients qui habitent dans une ville ou le pourcentage est strictement inférieur à 1%
[image : NouvelElement238.png]
Nombre de clients par nationalité qui ont un dossier de vente effectif avec la date concrétisation dans la période choisi / Nombre total des clients
Dossier de vente effectif = Dossier dont le statut >= « Concrétisation »
Créé avec HelpNDoc Personal Edition: [Écrire des livres électroniques ePub pour l'iPad](https://www.helpndoc.com/fr/creer-des-livres-numeriques-epub)

##### ETAT DES VENTE <a id="page-comtdbsalesstate"></a>
_Page : `ComTdbSalesState.html` — Module Vente › TABLEAUX DE BORDS › RAPPORTS_

ETAT DES VENTES : CONCRETISATIONS

|  | ANNEES |  |  |  |  |  |  |
|---|---|---|---|---|---|---|---|
| Année courante - 5 |  |  |  |  |  |  |  |
| MOIS | Année-5 | Année-4 | Année-3 | Année-2 | Année-1 | Année courante | Total |
| JANVIER | Nombre de dossiers de vente dont le statut >= « Concrétisation » et la date concrétisation non nulle dans la période choisi | Total concrétisation par mois |  |  |  |  |  |
| FERVRIER |  |  |  |  |  |  |  |
| MARS |  |  |  |  |  |  |  |
| AVRIL |  |  |  |  |  |  |  |
| MAI |  |  |  |  |  |  |  |
| JUIN |  |  |  |  |  |  |  |
| JUILLET |  |  |  |  |  |  |  |
| AOUT |  |  |  |  |  |  |  |
| SEPTEMBRE |  |  |  |  |  |  |  |
| OCTOBRE |  |  |  |  |  |  |  |
| NOVEMBRE |  |  |  |  |  |  |  |
| DECEMBRE |  |  |  |  |  |  |  |
| Total | Total concrétisation par année |  |  |  |  |  |  |
[image : NouvelElement228.png]

### ETAT DES VENTES : CHIFFRE D’AFFAIRE

|  | ANNEES |  |  |  |  |  |  |
|---|---|---|---|---|---|---|---|
| Année courante - 5 |  |  |  |  |  |  |  |
| MOIS | Année-5 | Année-4 | Année-3 | Année-2 | Année-1 | Année courante | Total |
| JANVIER | ∑ des prix de ventes des dossiers dont le statut >= « Minute » et la date minute non nulle et dans la période choisi | Total Chiffre d'affaire TTC par mois |  |  |  |  |  |
| FERVRIER |  |  |  |  |  |  |  |
| MARS |  |  |  |  |  |  |  |
| AVRIL |  |  |  |  |  |  |  |
| MAI |  |  |  |  |  |  |  |
| JUIN |  |  |  |  |  |  |  |
| JUILLET |  |  |  |  |  |  |  |
| AOUT |  |  |  |  |  |  |  |
| SEPTEMBRE |  |  |  |  |  |  |  |
| OCTOBRE |  |  |  |  |  |  |  |
| NOVEMBRE |  |  |  |  |  |  |  |
| DECEMBRE |  |  |  |  |  |  |  |
| Total | Total Chiffre d'affaire TTC par année |  |  |  |  |  |  |
[image : NouvelElement229.png]

### ETAT DES VENTES : LIVRAISON

|  | ANNEES |  |  |  |  |  |  |
|---|---|---|---|---|---|---|---|
| Année courante - 5 |  |  |  |  |  |  |  |
| MOIS | Année-5 | Année-4 | Année-3 | Année-2 | Année-1 | Année courante | Total |
| JANVIER | Nombre de dossiers de vente dont le statut >= « Remise de clés » et la date Remise de clés non nulle dans la période choisi | Total livraison par mois |  |  |  |  |  |
| FERVRIER |  |  |  |  |  |  |  |
| MARS |  |  |  |  |  |  |  |
| AVRIL |  |  |  |  |  |  |  |
| MAI |  |  |  |  |  |  |  |
| JUIN |  |  |  |  |  |  |  |
| JUILLET |  |  |  |  |  |  |  |
| AOUT |  |  |  |  |  |  |  |
| SEPTEMBRE |  |  |  |  |  |  |  |
| OCTOBRE |  |  |  |  |  |  |  |
| NOVEMBRE |  |  |  |  |  |  |  |
| DECEMBRE |  |  |  |  |  |  |  |
| Total | Total livraison par année |  |  |  |  |  |  |
[image : NouvelElement230.png]
ETAT DES VENTE: ENCAISSEMENT (*)

|  | ANNEES |  |  |  |  |  |  |
|---|---|---|---|---|---|---|---|
| Année courante - 5 |  |  |  |  |  |  |  |
| MOIS | Année-5 | Année-4 | Année-3 | Année-2 | Année-1 | Année courante | Total |
| JANVIER | ∑ des montants des affectations dont la date pointage ou la date échéance dans la période choisi | Total encaissement par mois |  |  |  |  |  |
| FERVRIER |  |  |  |  |  |  |  |
| MARS |  |  |  |  |  |  |  |
| AVRIL |  |  |  |  |  |  |  |
| MAI |  |  |  |  |  |  |  |
| JUIN |  |  |  |  |  |  |  |
| JUILLET |  |  |  |  |  |  |  |
| AOUT |  |  |  |  |  |  |  |
| SEPTEMBRE |  |  |  |  |  |  |  |
| OCTOBRE |  |  |  |  |  |  |  |
| NOVEMBRE |  |  |  |  |  |  |  |
| DECEMBRE |  |  |  |  |  |  |  |
| Total | Total encaissement par année |  |  |  |  |  |  |
[image : NouvelElement231.png]
(*)Sans tenir compte les frais et les pièces non comptabilisées
ETAT DES VENTE: ANNULATION

|  | ANNEES |  |  |  |  |  |  |
|---|---|---|---|---|---|---|---|
| Année courante - 5 |  |  |  |  |  |  |  |
| MOIS | Année-5 | Année-4 | Année-3 | Année-2 | Année-1 | Année courante | Total |
| JANVIER | Nombre de dossiers dont le statut « Annulation » et la date annulation non nulle dans la période choisi | Total annulation par mois |  |  |  |  |  |
| FERVRIER |  |  |  |  |  |  |  |
| MARS |  |  |  |  |  |  |  |
| AVRIL |  |  |  |  |  |  |  |
| MAI |  |  |  |  |  |  |  |
| JUIN |  |  |  |  |  |  |  |
| JUILLET |  |  |  |  |  |  |  |
| AOUT |  |  |  |  |  |  |  |
| SEPTEMBRE |  |  |  |  |  |  |  |
| OCTOBRE |  |  |  |  |  |  |  |
| NOVEMBRE |  |  |  |  |  |  |  |
| DECEMBRE |  |  |  |  |  |  |  |
| Total | Total annulation par année |  |  |  |  |  |  |
[image : NouvelElement232.png]
ETAT DES VENTE: DESISTEMENT

|  | ANNEES |  |  |  |  |  |  |
|---|---|---|---|---|---|---|---|
| Année courante - 5 |  |  |  |  |  |  |  |
| MOIS | Année-5 | Année-4 | Année-3 | Année-2 | Année-1 | Année courante | Total |
| JANVIER | Nombre de dossiers dont le statut « Désistement » et la date annulation non nulle dans la période choisi | Total désistement par mois |  |  |  |  |  |
| FERVRIER |  |  |  |  |  |  |  |
| MARS |  |  |  |  |  |  |  |
| AVRIL |  |  |  |  |  |  |  |
| MAI |  |  |  |  |  |  |  |
| JUIN |  |  |  |  |  |  |  |
| JUILLET |  |  |  |  |  |  |  |
| AOUT |  |  |  |  |  |  |  |
| SEPTEMBRE |  |  |  |  |  |  |  |
| OCTOBRE |  |  |  |  |  |  |  |
| NOVEMBRE |  |  |  |  |  |  |  |
| DECEMBRE |  |  |  |  |  |  |  |
| Total | Total désistement par année |  |  |  |  |  |  |
[image : NouvelElement233.png]
Créé avec HelpNDoc Personal Edition: [Générateur gratuit de livres électroniques et documentation](https://www.helpndoc.com/fr)

##### SUIVI DES CADEAUX <a id="page-comtdbgift"></a>
_Page : `ComTdbGift.html` — Module Vente › TABLEAUX DE BORDS › RAPPORTS_

| Article | Statut | Client | Parrain | Cadeau | Raison | Livré |
|---|---|---|---|---|---|---|
| Tranche |  |  |  |  |  |  |
| Code article | Statut de dossier de vente | Description client | Description parrain (à partir de la prospection) | Description cadeau | Description raison | État de cadeau à partir de dossier de vente |
Créé avec HelpNDoc Personal Edition: [Générateur complet de livres électroniques Kindle](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creer-ebooks-pour-kindle-amazon)

##### SUIVI DES COMMERCIAUX <a id="page-comtdbcommercialstate"></a>
_Page : `ComTdbCommercialState.html` — Module Vente › TABLEAUX DE BORDS › RAPPORTS_

SUIVI DES COMMERCIAUX : VISITES
Remarque 1: L’utilisateur peut personnaliser l’année, les tranches et les commerciaux à visualiser

|  | Mois |  |  |  |  |  |  |  |  |  |  |  |  |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Commercial | Janvier | Février | Mars | Avril | Mai | Juin | Juillet | Août | Septembre | Octobre | Novembre | Décembre | Total |
| Description commercial | Nombre d’actions de type visites réalisées par le commercial affecté à la prospection et dont la date action est dans la période choisie. | Total par commercial |  |  |  |  |  |  |  |  |  |  |  |
|  | Total par mois |  |  |  |  |  |  |  |  |  |  |  |  |
[image : NouvelElement234.png]
SUIVI DES COMMERCIAUX : PROSPECTIONS

|  | Mois |  |  |  |  |  |  |  |  |  |  |  |  |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Commercial | Janvier | Février | Mars | Avril | Mai | Juin | Juillet | Aout | Septembre | Octobre | Novembre | Décembre | Total |
| Liste des commerciaux choisis | Nombre de prospections réalisées par le commercial affecté à la prospection et dont la date prospection est dans la période choisie. | Total par commercial |  |  |  |  |  |  |  |  |  |  |  |
|  | Total par mois |  |  |  |  |  |  |  |  |  |  |  |  |
[image : NouvelElement235.png]
SUIVI DES COMMERCIAUX : COMMUNICATIONS ENTRANTES

|  | Mois |  |  |  |  |  |  |  |  |  |  |  |  |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Commercial | Janvier | Février | Mars | Avril | Mai | Juin | Juillet | Aout | Septembre | Octobre | Novembre | Décembre | Total |
| Liste des commerciaux choisis | Nombre d’actions de type mail entrant ou téléphone entrant réalisées par un commercial dont la date action est dans la période choisie.<br> Commercial : Le personnel affecté à l'utilisateur qui a fait l'action sinon la description de l’utilisateur si aucun personnel ne lui est affecté. | Total par commercial |  |  |  |  |  |  |  |  |  |  |  |
|  | Total par mois |  |  |  |  |  |  |  |  |  |  |  |  |
[image : NouvelElement236.png]
SUIVI DES COMMERCIAUX : COMMUNICATIONS SORTANTES

|  | Mois |  |  |  |  |  |  |  |  |  |  |  |  |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Commercial | Janvier | Février | Mars | Avril | Mai | Juin | Juillet | Aout | Septembre | Octobre | Novembre | Décembre | Total |
| Liste des commerciaux choisis | Nombre d’actions de type mail sortant ou téléphone sortant réalisées par un commercial dont la date action est dans la période choisie.<br> Commercial : Le personnel affecté à l'utilisateur qui a fait l'action sinon la description de l’utilisateur si aucun personnel ne lui est affecté. | Total par commercial |  |  |  |  |  |  |  |  |  |  |  |
|  | Total par mois |  |  |  |  |  |  |  |  |  |  |  |  |
[image : NouvelElement237.png]
Créé avec HelpNDoc Personal Edition: [Générateur de documentation et EPub facile](https://www.helpndoc.com/fr)

##### SUIVI DES PROSPECTIONS <a id="page-comtdbprospectionstate"></a>
_Page : `ComTdbProspectionState.html` — Module Vente › TABLEAUX DE BORDS › RAPPORTS_

[image : NouvelElement329.png]
Nombre de prospections groupés par source de contact
Remarque :

- Si la source de contact est vide, le système l’affiche dans une portion intitulé « Non définit » avec le pourcentage correspondant.
- Si le système affiche "Autre < 2%: X %", c'est à dire qu'il y a X % de source de contact strictement inférieur à 2%
[image : NouvelElement330.png]
Nombre de prospections groupés par type
Remarque :

- Si le type est vide, le système l’affiche dans une portion intitulé « Non définit » avec le pourcentage correspondant.
- Si le système affiche "Autre < 2%: X %", c'est à dire qu'il y a X % de type prospections strictement inférieur à 2%
[image : NouvelElement332.png]
Nombre de prospections groupés par nature
Remarque :

- Si la nature est vide, le système l’affiche dans une portion intitulé « Non définit » avec le pourcentage correspondant
- Si le système affiche "Autre < 2%: X %", c'est à dire qu'il y a X % de nature strictement inférieur à 2%
[image : NouvelElement339.png]
Nombre de prospection par mois
[image : NouvelElement333.png]
Nombre d’action affecté à des prospections groupés par résultat
Remarque :

- Si le résultat est vide, le système l’affiche dans une portion intitulé « Non définit » avec le pourcentage correspondant
- Si le système affiche "Autre < 2%: X %", c'est à dire qu'il y a X % de résultat strictement inférieur à 2%
[image : NouvelElement338.png]
Nombre d’action affecté à des prospections groupés par type
Remarque :

- Si le type est vide, le système l’affiche dans une portion intitulé « Non définit » avec le pourcentage correspondant
- Si le système affiche "Autre < 2%: X %", c'est à dire qu'il y a X % de type action strictement inférieur à 2%
[image : NouvelElement336.png]
Nombre d’action groupés par utilisateur qui a fait l’action
Remarque :

- Si l’action n’a pas un utilisateur, le système l’affiche dans une portion intitulé « Non définit » avec le pourcentage correspondant
- Si le système affiche "Autre < 2%: X %", c'est à dire qu'il y a X % d’utilisateurs qui ont fond des actions strictement inférieur à 2%
[image : NouvelElement337.png]
Nombre d’action par mois pour les prospections
Créé avec HelpNDoc Personal Edition: [Outils facile d'utilisation pour créer des aides HTML et des sites web](https://www.helpndoc.com/fr/outils-creation-aide)

##### PROSPECTS PAR PERIODE <a id="page-comtdbprospectspercommercial"></a>
_Page : `ComTdbProspectsPerCommercial.html` — Module Vente › TABLEAUX DE BORDS › RAPPORTS_

### CONCRETISATION PAR COMMERCIAL PAR SEMAINE

|  | S1 (Semaine 1) | S2 | S… |
|---|---|---|---|
| Commercial | Nombre de prospect créé par commercial par semaine dans la période choisie |  |  |
| Total |  |  |  |
[image : NouvelElement572.png]

### GRAPHE DE CONCRETISATION PAR COMMERCIAL PAR SEMAINES

### CONCRETISATION PAR COMMERCIAL PAR MOIS

|  | 1 (Mois 1) | 2… | 12 |
|---|---|---|---|
| Commercial | Nombre de prospect créé par commercial par mois dans la période choisie |  |  |
| Total |  |  |  |
[image : NouvelElement571.png]

### GRAPHE DE CONCRETISATION PAR COMMERCIAL PAR MOIS

### CONCRETISATION PAR COMMERCIAL PAR AN

|  | 2013 (Année) | 2014 | … |
|---|---|---|---|
| Commercial | Nombre de prospect créé par commercial par an dans la période choisie |  |  |
| Total |  |  |  |
[image : NouvelElement570.png]

### GRAPHE DE CONCRETISATION PAR AN
Créé avec HelpNDoc Personal Edition: [Créer des sites web d'aide facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

##### TAUX DE VENTE PAR RAPPORT AU STOCK <a id="page-comtdbsalestockratio"></a>
_Page : `ComTdbSaleStockRatio.html` — Module Vente › TABLEAUX DE BORDS › RAPPORTS_

| Mois/Année | Départ Année -1 (A) | Année -1 (B) | Perf année -1 | Départ Année courante (C) | Année (D) | Perf année | Total |
|---|---|---|---|---|---|---|---|
| JANVIER | Stock clôture du mois précédent | Mouvement du mois (Concrétisation, Transfert produit, désistement, annulation après concrétisation) | (B)/(A) | Stock clôture du mois précédent | Mouvement du mois (Concrétisation, Transfert produit, désistement, annulation après concrétisation) | (D)/(C) | B+D |
| FEVRIER |  |  |  |  |  |  |  |
| MARS |  |  |  |  |  |  |  |
| AVRIL |  |  |  |  |  |  |  |
| MAI |  |  |  |  |  |  |  |
| JUIN |  |  |  |  |  |  |  |
| JUILLET |  |  |  |  |  |  |  |
| AOUT |  |  |  |  |  |  |  |
| SEPTEMBRE |  |  |  |  |  |  |  |
| OCTOBRE |  |  |  |  |  |  |  |
| NOVEMBRE |  |  |  |  |  |  |  |
| DECEMBRE |  |  |  |  |  |  |  |
| Total concrétisation (E) | ∑ (B) |  |  |  |  |  |  |
| Stock disponible de l’année (F) | Stock début de l’année |  |  |  |  |  |  |
| Taux de « Concrétisation/Promesse » du stock disponible | (E)/(F) |  |  |  |  |  |  |
[image : NouvelElement353.png]
TAUX DE VENTE PAR RAPPORT AU STOCK PAR TRANCHES

| Stock disponible initial | Année-1 | Ventes Année -1 | Année | Ventes Année |
|---|---|---|---|---|
| Tranches | Stock clôture de l’année -1 par tranche | Mouvement de l’année (Concrétisation/Promesse, Transfert produit, désistement, annulation après concrétisation) par tranche | Stock clôture de l’année précédente par tranche | Mouvement de l’année (Concrétisation/Promesse, Transfert produit, désistement, annulation après concrétisation) |
| TOTAL PRODUIT |  |  |  |  |
[image : NouvelElement352.png]
(A): Total disponible de l’année / Total concrétisation
Créé avec HelpNDoc Personal Edition: [Produire des livres EPub gratuitement](https://www.helpndoc.com/fr/creer-des-livres-numeriques-epub)

##### TAUX DE TRONSFORMATION PAR NATURE D’ACTIONS <a id="page-comtdbcommercialtransformationby"></a>
_Page : `ComTdbCommercialTransformationBy.html` — Module Vente › TABLEAUX DE BORDS › RAPPORTS_

| Commercial | PROGRAMME |  |  |  |  |  |  |  |
|---|---|---|---|---|---|---|---|---|
| Prospection | Total concrétisation | Nature action choisi | Réservations | Concrétisations | Annulations | Contrats signés ou livrés | Taux de transferts |  |
|  | = Nombre des prospections par commercial dans la période choisi | = Nombre de dossier dont le statut >= « Concrétisation » et < « Minute » non annulés et non désistés qui ont une date « Concrétisation » non nulle sans tenir compte la période choisi (B) | = Nombre de prospection selon le type choisi dans la période (C) | = Nombre de réservation dans la période<br> sans tenir compte les annulations et les désistements dont le statut >= « Réservation » (D) | = Nombre de dossier dont le statut >= « Concrétisation/Promesse » et < « Minute » qui ont une date « Concrétisation /Promesse » non nulle dans la période choisi (E) | = Nombre d'annulations et des désistements des dossiers qui ont une date « Concrétisation » non nulle et ils ont un commercial affecté, sans tenir compte la période choisi (F) | Nombre de dossier dont le statut >= « Minute » non annulé, non désisté<br> et ont une date « Concrétisation » non nulle dans la période (G) | (E + G)/C * 100 |
| Total |  |  |  |  |  |  |  |  |
Créé avec HelpNDoc Personal Edition: [Générateur de documentation d'aide HTML gratuit](https://www.helpndoc.com/fr)

##### TAUX DE CONCRETISATION PAR ANNEE/TRANCHE <a id="page-comsaletauxconcretisationparanne"></a>
_Page : `ComSaleTauxConcretisationParAnne.html` — Module Vente › TABLEAUX DE BORDS › RAPPORTS_

[image : NouvelElement621.png]
Première concrétisation : Le système affiche le mois et l’année de la première concrétisation

| Année | Nombre de produit | Taux |
|---|---|---|
| Année de la 1ère concrétisation | Nombre de dossiers de vente dont le statut >= "Concrétisation" non annulés et non désisté qui ont une date concrétisation non nulle par année | Nombre de produit/Total produit *100 |
| Année… |  |  |
| Année de la dernière concrétisation |  |  |
| Reste | Nombre de dossiers de vente dont le statut < "Concrétisation" |  |
| Total produit | Nombre d'article dans le stock dont la vocation est "vente" | ---- |
Remarque: Le système affiche les données des tranches sélectionnées.
Créé avec HelpNDoc Personal Edition: [Créer des documents d'aide CHM facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

##### SUIVI NOTAIRES CRÉDITS <a id="page-comsaledashboardsuivinotairescre"></a>
_Page : `ComSaleDashboardSuiviNotairesCre.html` — Module Vente › TABLEAUX DE BORDS › RAPPORTS_

|  | Nombre de dossiers traités | C.A. | Montant crédit | Montant crédit débloqué | Montant attestations non débloquées | Échéances attestations non débloquées | Délais de déblocage des attestations de consignations |
|---|---|---|---|---|---|---|---|
| Janvier | Nombre de dossier dont le statut >= « Envoi au notaire » non annulés et non désistés et qui ont une ligne de type « Crédit » | ∑ des prix de vente des dossiers dont le statut >= « Envoi au notaire » non annulés et non désistés et qui ont une ligne de type « Crédit » | ∑ des montant lignes de type « Crédit » des vente des dossiers dont le statut >= « Envoi au notaire » non annulés et non désistés | ∑ des montant lignes de type « Crédit » des dossiers dont le statut >= « Envoi au notaire » non annulés et non désistés et qui sont réglées par un « Chèque notaire » | ∑ des montant lignes de type « Crédit » des dossiers dont le statut >= « Envoi au notaire » non annulés et non désistés et qui sont réglées par une « Attestations de consignation » | ∑ des montants des lignes crédit avec une attestation de consignation qui sont arrivé à la date d'échéance et qui ne sont pas réglé | ∑ (Date échéance – date pièce) par mois / Nombre de dossier dont le statut >= « Envoi au notaire » non annulés et non désistés et qui ont une ligne de type « Crédit » |
| Février |  |  |  |  |  |  |  |
| Mars |  |  |  |  |  |  |  |
| Avril |  |  |  |  |  |  |  |
| Mai |  |  |  |  |  |  |  |
| Juin |  |  |  |  |  |  |  |
| Juillet |  |  |  |  |  |  |  |
| Août |  |  |  |  |  |  |  |
| Septembre |  |  |  |  |  |  |  |
| Octobre |  |  |  |  |  |  |  |
| Novembre |  |  |  |  |  |  |  |
| Décembre |  |  |  |  |  |  |  |
| Total |  |  |  |  |  |  |  |
Créé avec HelpNDoc Personal Edition: [Produire des livres électroniques facilement](https://www.helpndoc.com/fr/creer-des-livres-numeriques-epub)

##### ANALYSE DELAIS DE LIVRAISONS <a id="page-comsaledashboarddeliverydelay"></a>
_Page : `ComSaleDashboardDeliveryDelay.html` — Module Vente › TABLEAUX DE BORDS › RAPPORTS_

Au comptant

| Délai titres foncier / date envoi au notaire | ∑ Date titre foncier (à partir de la fiche article) – Date envoi au notaire / (Nombre de dossier de vente dont le statut >= « Envoi au notaire » et qui n’ont pas une ligne crédit) |
|---|---|
| Délai minute / remise de clé | ∑ Date minute – Date remise de clé / (Nombre de dossier de vente dont le statut >= « Envoi au notaire » et qui n’ont pas une ligne crédit) |
| Délai titre foncier / remise de clé | ∑ Date titre foncier (à partir de la fiche article) – Date remise de clé / (Nombre de dossier de vente dont le statut >= « Envoi au notaire » et qui n’ont pas une ligne crédit) |

| Notaire | Délai envoi au notaire / minute | Délai minute/ Formalisé |
|---|---|---|
| Description notaire | ∑ (Date envoi au notaire – Date minute) / (Nombre de dossier de vente dont le statut >= « Envoi au notaire » et qui n’ont pas une ligne crédit) (Par notaire) | ∑ (Date minute – Date expédition certificat de propriété) / (Nombre de dossier de vente dont le statut >= « Envoi au notaire » et qui n’ont pas une ligne crédit) (Par notaire) |
A crédit

| Délai titres foncier / date envoi au notaire | ∑ Date titre foncier (à partir de la fiche article) – Date envoi au notaire / (Nombre de dossier de vente dont le statut >= « Envoi au notaire » et qui ont une ligne crédit) |
|---|---|
| Délai minute / remise de clé | ∑ Date minute – Date remise de clé / (Nombre de dossier de vente dont le statut >= « Envoi au notaire » et qui ont une ligne crédit) |

| Notaire | Délai envoi notaire / Crédit en cours | Délai crédit en cours / Crédit débloqué | Délai crédit débloqué / Minute | Délai crédit en cours / Minute | Délai Minute / Formalisé | Délai titre foncier / Remise de clé |
|---|---|---|---|---|---|---|
| Description notaire | = ∑ Date envoi au notaire – Date Approbation crédit / (Nombre de dossier de vente dont le statut >= « Envoi au notaire » et qui ont une ligne crédit) par notaire | = ∑ Date Approbation crédit – Date octroi crédit / (Nombre de dossier de vente dont le statut >= « Envoi au notaire » et qui ont une ligne crédit) par notaire | = ∑ Date octroi crédit – Date minute / (Nombre de dossier de vente dont le statut >= « Envoi au notaire » et qui ont une ligne crédit) par notaire | = ∑ Date Approbation crédit – Date minute / (Nombre de dossier de vente dont le statut >= « Envoi au notaire » et qui ont une ligne crédit) par notaire | = ∑ Date minute – Date expédition/ (Nombre de dossier de vente dont le statut >= « Envoi au notaire » et qui ont une ligne crédit) par notaire | = ∑ Date titre foncier (à partir de la fiche article) – Date remise de clé / (Nombre de dossier de vente dont le statut >= « Envoi au notaire » et qui ont une ligne crédit) par notaire |
Créé avec HelpNDoc Personal Edition: [Créer des fichiers d'aide Qt Help multi-plateformes](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creez-des-fichiers-daide-pour-le-framework-qt-help)

#### SUIVI DES AFFECTATIONS <a id="page-suividesaffectations"></a>
_Page : `SUIVIDESAFFECTATIONS.html` — Module Vente › TABLEAUX DE BORDS_

[image : NouvelElement118.png]
La liste visualise les affectations effectués en fonction de l’[entité juridique](#page-entitesjuridique), le type de document, la / les[TYPESPIECE.html](#page-typespiece)[types de pièce](#page-typespiece) sélectionnées , le [statut](#page-statuts-com) de dossier et la période.
Remarque: A partir de cette liste, on peut accéder au dossier de vente qui contient le règlement sélectionné.

### Champs

| LIBELLE | V | OBSERVATIONS |
|---|---|---|
| Date | X |  |
| Catégorie | X |  |
| Entité juridique | X |  |
| Programme | X |  |
| Tranche | X |  |
| Article | X |  |
| Montant affecté | X |  |
| Dossier de vente | X |  |
| État de dossier de vente | X |  |
| Tiers | X |  |
| Pièce de règlement | X |  |
| Banque | X |  |
| Note pièce de règlement | X |  |
| Note document | X |  |
| Compte |  |  |
| Date document |  |  |
| Date échéance pièce de règlement |  |  |
| Date opération pièce de règlement |  |  |
| Date pièce de règlement |  |  |
| État pièce de règlement |  |  |
| Montant du document |  |  |
| Montant pièce de règlement |  |  |
| Référence pièce de règlement |  |  |
| Référence reçu |  |  |
| Type pièce de règlement |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Éditer | GEN0009 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
Créé avec HelpNDoc Personal Edition: [Éditeur complet de livres électroniques ePub](https://www.helpndoc.com/fr/creer-des-livres-numeriques-epub)

### CONFIGURATION <a id="page-configuration-com"></a>
_Page : `CONFIGURATION_COM.html` — Module Vente_

Créé avec HelpNDoc Personal Edition: [Générateur de documentation et EPub facile](https://www.helpndoc.com/fr)

#### PROSPECTION <a id="page-prospection"></a>
_Page : `PROSPECTION.html` — Module Vente › CONFIGURATION_

Créé avec HelpNDoc Personal Edition: [Créer des livres électroniques facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

##### SOURCE DE CONTACT <a id="page-sourcedecontact"></a>
_Page : `SOURCEDECONTACT.html` — Module Vente › CONFIGURATION › PROSPECTION_

[image : NouvelElement549.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Détails |  |  |  |
| O.D.T |  | X |  |
| Code | X | X |  |
| Parrain obligatoire |  |  | Si on choisi dans la [prospection](#page-prospections) un source de contact paramétré avec "Parrain obligatoire", le système vérifie si le champ "Parrain" est renseigné ou non<br>- Si le champ est vide: le système affiche un message " La valeur du champ [Parrain] est obligatoire !! "<br>- Sinon le système enregistre la prospection. |
| Avec cadeau |  |  | Si on choisi dans la [prospection](#page-prospections) un source de contact paramétré" Avec cadeau", le système vérifie si la tranche est paramétrée "Avec octroi d'un cadeau pour une source de contact":<br> Si oui:<br>- Le système à la création du dossier de vente créé une ligne de cadeau avec le raison source de contact<br> Sinon:<br>- Le système ne créé pas une ligne de cadeau |
| Valeurs par défaut |  |  |  |
| Source de contact |  |  | Liste déroulante des sources de contacts.<br> A la création d’une « [Prospections](#page-prospections) » ou un « [Prospects express](#page-prospectexpress)», la source de contact choisi sera affectée par défaut dans la fiche. |
| GEN0015, GEN0016 |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Enregistrer | GEN0001 pour la ligne sélectionnée |
| Supprimer | GEN0010 pour la ligne sélectionnée |
Créé avec HelpNDoc Personal Edition: [Environnement de création d'aide complet](https://www.helpndoc.com/fr/outils-creation-aide)

##### SUPPORTS SOURCE DE CONTACT <a id="page-supportssourcedecontact"></a>
_Page : `SUPPORTSSOURCEDECONTACT.html` — Module Vente › CONFIGURATION › PROSPECTION_

[image : NouvelElement120.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Code | X | X |  |
| Description |  | X |  |
| Source de contact |  |  | Liste déroulante des [sources de contactes](#page-sourcedecontact) |
| GEN0015, GEN0016 |  |  |  |
Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 pour la ligne sélectionnée |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichier attachés | GEN0004 |
| Note | GEN0005 |
| Annuler et fermer | GEN0017 |
| Supprimer | GEN0010 pour la ligne sélectionnée |
| Champs spécifiques | GEN0007 pour la ligne sélectionnée |
Créé avec HelpNDoc Personal Edition: [Créer des documents d'aide PDF facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

##### NATURES PROSPECTIONS <a id="page-naturesprospections"></a>
_Page : `NATURESPROSPECTIONS.html` — Module Vente › CONFIGURATION › PROSPECTION_

[image : NouvelElement121.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Code | X | X |  |
| Description |  | X |  |
| Projet obligatoire |  |  | Pour une nature de prospection, si la case « Projet obligatoire » est cochée, le choix d’un projet dans la fiche [prospection](#page-prospections) devient obligatoire sinon, il devient optionnel. |
| Délai d’alerte |  |  |  |
| Résultat |  |  | Liste déroulante des résultats |
| Remarque 1: Le système affecte le résultat choisi au niveau de la configuration des natures au résultat d'action et il crée automatiquement une alerte (Si délai d'alerte >0) si cette nature est affectée à la prospection.<br>Remarque 2: Si la durée paramétré >= Délai d’inactivité ; le système génère une alerte (Date dernière action + Délai d’alerte)<br> Remarque 3: Si l’utilisateur modifie la nature (nature avec alerte) de la prospection, le système génère une alerte (Date dernière action + Délai d’alerte) |  |  |  |
| GEN0015, GEN0016 |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 pour la ligne sélectionnée |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichier attachés | GEN0004 |
| Note | GEN0005 |
| Annuler et fermer | GEN0017 |
| Supprimer | GEN0010 pour la ligne sélectionnée |
| Champs spécifiques | GEN0007 pour la ligne sélectionnée |
Créé avec HelpNDoc Personal Edition: [Créer des livres électroniques facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

##### ACTIONS COMMERCIALES <a id="page-actionscommerciales"></a>
_Page : `ACTIONSCOMMERCIALES.html` — Module Vente › CONFIGURATION › PROSPECTION_

[image : NouvelElement547.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Code | X | X |  |
| Description |  | X |  |
| Date début |  |  |  |
| Date fin |  |  |  |
| Couleur |  |  |  |
| Notes |  |  |  |
| Actif (ve) |  |  | En cochant le champ « actif (ve) », le système affiche l'action dans la liste déroulante des actions commerciales du masque « [Prospection](#page-prospections)» |
|  |  |  |  |
| Raison de cadeau |  |  | Liste déroulante des [raisons de cadeaux.](#page-raisonsdecadeaux) |
| Avec cadeau pour les tranches |  |  | Liste des [tranches](#page-cfg-tranches)<br> Lorsqu'on affecte une tranche(ou plusieurs) à une action commerciale, le système:<br>- Affecte un cadeau au dossier de vente avec la raison "Action commercial" si:<br> - L'action commerciale en question est choisi dans la prospection et le prospect est un nouveau client<br> - L'action commerciale en question est choisi dans la prospection et la tranche paramétrée "avec .cumul des cadeaux" |
| GEN0015, GEN0016 |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Enregistrer | GEN0001 pour la ligne sélectionnée |
| Supprimer | GEN0010 pour la ligne sélectionnée |
| Sélectionner / Désélectionner par défaut | A la création d’une « Prospections » ou un « [Prospects express](#page-prospectexpress) », l’action commercial choisit sera affectée par défaut dans la fiche. |
| Créer un dossier de groupement | Créer un dossier pour grouper les factures et les documents à régler de l'action commercial correspondante. |
Créé avec HelpNDoc Personal Edition: [Produire des livres Kindle gratuitement](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creer-ebooks-pour-kindle-amazon)

##### RAISON D'ARCHIVAGE DES PROSPECTIONS <a id="page-raisondarchivagedesprospections"></a>
_Page : `RAISONDARCHIVAGEDESPROSPECTIONS.html` — Module Vente › CONFIGURATION › PROSPECTION_

[image : NouvelElement123.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Code | X | X |  |
| Description |  | X |  |
| GEN0015, GEN0016 |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Enregistrer | GEN0001 pour la ligne sélectionnée |
| Supprimer | GEN0010 pour la ligne sélectionnée |
Créé avec HelpNDoc Personal Edition: [Avantages d'un outil de création d'aide](https://www.helpauthoringsoftware.com/fr)

#### VENTE <a id="page-vente"></a>
_Page : `VENTE.html` — Module Vente › CONFIGURATION_

Créé avec HelpNDoc Personal Edition: [Générateur complet d'aides multi-formats](https://www.helpndoc.com/fr/outils-creation-aide)

##### STATUTS <a id="page-statuts-com"></a>
_Page : `STATUTS_COM.html` — Module Vente › CONFIGURATION › VENTE_

[image : NouvelElement727.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| O.D.T |  | X |  |
| Code | X | X |  |
| Description |  | X |  |
| Type |  |  | Liste déroulante de types de statut.<br> La liste peut prendre une des valeurs suivantes :<br>- Nouveau<br>- Intermédiaire<br>- Annulation<br>- Terminer avec échec<br>- Terminer avec succès |
| Durée de vie |  |  |  |
| Seuil de versement |  |  | Liste déroulante d’opérateurs |
| Pourcentage |  |  | Le taux en pourcentage à appliquer lorsque le dossier de vente dans un statut déterminé |
| Couleur |  |  |  |
| Vérifier le seuil de versement |  |  | Liste déroulante d’options de vérification de seuil de versement.<br> La liste peut prendre une des valeurs suivantes :<br>- Toujours : Toujours vérifier le montant<br>- Crédit : La vérification est faite que pour les crédits<br>- Au comptant : La vérification est faite que pour les montants comptant |
| Date associée |  |  | Liste déroulante des dates de la fiche "[Dossier de vente](#page-dossiersdevente)".<br> La date choisie sera une référence dans la processus de:<br>- Calcul des commissions vendeurs (Les commissions de finalisation).<br>- Affectation de date automatiquement lors du passage des statuts d'un dossier de vente. |
| Est une vente effective |  |  | Si la case n’est pas cochée le système considère que le dossier de vente n’est pas encore concrétisé. |
| Est une annulation |  |  | C’est l’annulation d’un dossier de vente sans frais et remboursement |
| Est une résiliation |  |  | C’est l’annulation d’un dossier de vente avec frais et remboursement |
| Vente au comptant |  |  | Si le client va payer le montant en comptant |
| Vente à crédit |  |  | Si le client va payer le montant à crédit |
| Peut effectuer un transfert produit |  |  | L'utilisateur peut modifier l’article si le statut de vente est paramétré " Peut effectuer un transfert produit " |
| Peut effectuer un transfert client |  |  | L'utilisateur peut modifier le client si le statut de dossier de vente est paramétré " Peut effectuer un transfert client " |
| Transforme le prospect en client |  |  | L'utilisateur peut modifier le client si le statut de dossier de vente est paramétré " Peut effectuer un transfert client " |
| Peut effectuer un désistement |  |  | L'utilisateur peut faire un désistement si le statut de dossier de vente est paramétré "Peut effectuer un désistement" |
| Peut effectuer une prospection |  |  | Le système affiche dans le sélecteur des articles dans la fiche [prospection](#page-prospections) les articles qui ont des dossiers de vente dont le statut est paramétré "Peut effectuer une prospection". |
| Appliquer les frais |  |  | Le système ajoute les frais d’une opération |
| Avec vérification du schéma de financement |  |  | Le système vérifie si la somme des documents de type avance et/ou crédit bancaire et/ou autofinancement égale au prix de vente après remise ou non<br> si oui<br>- Il enregistre le dossier de vente<br> Sinon<br>- Il affiche un message d'erreur "Votre schéma de financement ne correspond pas au montant contractuel :...." |
| Avec vérification sur les cadeaux |  |  | Si le champ "Avec vérification sur les cadeaux" est coché et le champ "Le client veut choisir plus tard" n'est pas coché le système:<br>- Rend l'ajout d'un cadeau obligatoire<br>- Rend l'affectation d'un cadeau obligatoire<br>Si le champ "Avec vérification sur les cadeaux" est coché et le champ "Le client veut choisir plus tard" est coché rend le système<br>- Rend l'ajout d'une ligne cadeau obligatoire<br>- Rend l'affectation d'un cadeau facultatif |
| Avec commande des cadeaux |  |  | Paramétré dans les statuts dont la commande des cadeaux est obligatoire |
| Avec affectation de gestionnaire |  |  | Le système vérifie si le statut est paramétré "avec affectation de gestionnaire" et le champ gestionnaire est vide<br>- Si oui: Le système affecte le personnel de l'utilisateur connecté<br>- Si non; le système ne rien fait |
| Remarque : Les cases à cocher correspondent:<br>- Aux opérations que l’utilisateur peuvent faire.<br>- Aux vérifications que le système doit faire. |  |  |  |
| GEN0015, GEN0016 |  |  |  |

### Champs obligatoires

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| [image : NouvelElement130.png]<br> Configuration des champs obligatoires |  |  |  |
| Date réservation |  |  | La liste des champs obligatoires qui correspondent à un statut |
| Date concrétisation/ promesse de vente |  |  |  |
| Date relance 1 |  |  |  |
| Date relance 2 |  |  |  |
| Date envoie au notaire / avocat |  |  |  |
| Date envoie au notaire / avocat prévisionnel |  |  |  |
| Notaire / avocat |  |  |  |
| Titre foncier |  |  |  |
| Date fiche bancaire |  |  |  |
| Date permis d’habiter |  |  |  |
| Banque du crédit |  |  |  |
| Agence du crédit |  |  |  |
| Date compromis / Approbation crédit |  |  |  |
| Date attestation de consignation / Octroi crédit |  |  |  |
| Date assurance |  |  |  |
| Date d’expédition / certificat de propriété |  |  |  |
| Date annulation / résiliation |  |  |  |
| Détail résiliation |  |  |  |
| Date remise des clés |  |  |  |
| Date fiche intervention |  |  |  |
| Date minute / Enregistrement du contrat |  |  |  |
| Date d’envoi MEILLEURTAUX |  |  |  |
| Décision MEILLEURTAUX |  |  |  |
| Date décision MEILLEURTAUX |  |  |  |

### Champs obligatoires des tiers

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| [image : NouvelElement131.png]<br> Figure des champs obligatoires des tiers |  |  |  |
| Adresse |  |  | La liste des champs obligatoires des tiers qui correspondent à un statut |
| Code postal |  |  |  |
| Téléphone indicatif |  |  |  |
| Téléphone |  |  |  |
| Téléphone 1 indicatif |  |  |  |
| Téléphone 1 |  |  |  |
| Téléphone 2 indicatif |  |  |  |
| Téléphone 2 |  |  |  |
| Fax indicatif |  |  |  |
| Fax |  |  |  |
| E mail |  |  |  |
| Site web |  |  |  |
| Agence |  |  |  |
| RIB |  |  |  |
| IBAN |  |  |  |
| Registre de commerce |  |  |  |
| Code en douane |  |  |  |
| Fils de |  |  |  |
| Et de |  |  |  |
| Employeur |  |  |  |
| Ancienneté |  |  |  |
| Date de naissance |  |  |  |
| Lieu de naissance |  |  |  |
| Nbr enfants |  |  |  |
| Capital |  |  |  |
| Qualité |  |  |  |
| Pièce d’identité Type |  |  |  |
| Pièce d’identité N° |  |  |  |
| Pièce d’identité date |  |  |  |
| Pièce d’identité Lieu |  |  |  |
| Pièce d’identité valide jusqu’au |  |  |  |
| Pièce lieu renouvellement |  |  |  |
| Comptabilité Code |  |  |  |
| Comptabilité Compte collectif |  |  |  |
| Banque |  |  |  |
| Personnel |  |  |  |
| Contact |  |  |  |
| Assujettissement |  |  |  |
| Situation familiale |  |  |  |
| Résidence |  |  |  |
| Régime matrimonial |  |  |  |
| Nature |  |  |  |
| Pays |  |  |  |
| Nationalité |  |  |  |
| Profession |  |  |  |
| Salaire |  |  |  |
| Ville |  |  |  |

### Documents à joindre

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| [image : NouvelElement133.png]<br> Configuration des documents à joindre |  |  |  |
| Liste des documents à joindre |  |  | La liste visualise :<br>- Le type de fichier attaché<br>- Un flag « Avec titre »<br>Aussi, dans cette liste on peut effectuer les opérations suivantes :<br>- Ajouter un document à joindre<br>- Supprimer un document à joindre<br>Remarque: Si un document est paramétré "Avec titre", le système rend l'attachement de ce dernier obligatoire si le champ "Date octroi des titres foncier " de la [tranche](#page-cfg-tranches) est renseigné. |

### Valeurs par défaut

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| [image : NouvelElement134.png]<br> Configuration des valeurs par défaut |  |  |  |
| Réservation |  |  | Listes déroulantes d’affectation des valeurs par défaut pour les statuts qui peut prendre un [dossier de vente](#page-dossiersdevente) |
| Concrétisation |  |  |  |
| Envoi notaire / avocat |  |  |  |
| Minute / contrat |  |  |  |
| Livraison |  |  |  |
| Désistement |  |  |  |
| Annulation |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Enregistrer | GEN0001 pour la ligne sélectionnée |
| Supprimer | GEN0010 pour la ligne sélectionnée |
| Champs spécifiques | GEN0007 pour la ligne sélectionnée |
Créé avec HelpNDoc Personal Edition: [Générateur d'aide complet](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

##### TRESORERIE <a id="page-tresorerie-com"></a>
_Page : `TRESORERIE_COM.html` — Module Vente › CONFIGURATION › VENTE_

[image : NouvelElement538.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Trésorerie |  | X | Sélecteur des [catégories de recette](#page-categoriesdesrecettes) |
| Code | X | X |  |
| Description |  | X |  |
| TVA |  |  |  |
| Crédit/Débit |  |  |  |
| Avance |  |  | Les types qu'il peut prendre un document. |
| Autofinancement |  |  |  |
| Crédit bancaire |  |  |  |
| Frais |  |  |  |
| Modification |  |  |  |
| Résiliation |  |  |  |
| Pénalité |  |  |  |
| Syndic |  |  |  |
| Pré réservation |  |  |  |
| Basée sur la date de début de finalisations |  |  | La date échéance du document configuré prend la date de début des finalisations de la tranche si l’option « Basée sur la date de début des finalisations» est coché. |
| Exclure du prix contractuel |  |  | Exclure le montant de document du prix contractuel de dossier de vente. |
| Dans le cadre de la concrétisation |  |  | Inclure le montant du document paramétré « Dans le cadre de la concrétisation » dans le montant de concrétisation. |
| Mettre à jour le schéma de financement |  |  | Si on ajoute (ou on augmente son montant) un document paramétré "Mettre à jour le schéma de financement"<br> --> Le système soustrait son montant de la ligne "Autofinancement" sinon de la linge "Crédit"<br> Si on supprime (on diminue son montant) un document paramétré " Mettre à jour le schéma de financement"<br> --> Le système ajoute son montant à la ligne "Autofinancement" sinon à la ligne "Crédit" |
| Pour autrui |  |  | Pour indiquer que le règlement de document est pour un tiers (Fournisseur) autre que l’entité juridique.<br> Le tiers est paramétré sous l'onglet "Autres" de la [tranche](#page-cfg-tranches) du module stock |
| Doit être réglé après désistement |  |  | Le système soustrait le montant des documents paramétrés " Doit être réglé après désistement " du montant réglé. |
| Valeurs par défaut |  |  |  |
| Pré réservation |  |  | Listes déroulantes des frais et des versements qui vont être prisent comme étant des valeurs par défaut<br>Remarque1 : Ces valeurs seront affectées par défaut à la création d’un schéma de financement si les montants ne sont pas nuls dans la [tranche](#page-cfg-tranches).<br> Remarque2 : Les montants (et les remises) des frais et des versements sont paramétrable dans la [tranche](#page-cfg-tranches). |
| Frais de dossier |  |  |  |
| Frais de syndic |  |  |  |
| Autre frais |  |  |  |
| Avance |  |  |  |
| Auto financement |  |  |  |
| Crédit |  |  |  |
| Transfert produit |  |  | Lors d’un [transfert produit](#page-transfertsproduit) le système ajoute un nouveau document de catégorie « Transfert produit » dans le schéma de financement si le montants n'est pas nuls dans la tranche. |
| Transfert client |  |  | Lors d’un [transfert client](#page-transfertsclient) le système ajoute un nouveau document de catégorie « Transfert client » dans le schéma de financement si le montants n'est pas nuls dans la tranche. |
| Désistement |  |  | Lors d’un [désistement](#page-desistements) le système ajoute un nouveau document de catégorie « Désistement » dans le schéma de financement si le montants n'est pas nuls dans la tranche. |
| Frais de dossier |  |  | A l’ajout d’une ligne de type frais le système affectera la catégorie sélectionnée pour la ligne générée dans le dossier de vente si le montants n'est pas nuls dans la tranche. |
| GEN0015, GEN0016 |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Enregistrer | GEN0001 pour la ligne sélectionnée |
| Supprimer | GEN0010 pour la ligne sélectionnée |
| Champs spécifiques | GEN0007 pour la ligne sélectionnée |
Créé avec HelpNDoc Personal Edition: [Écrire des livres électroniques ePub pour l'iPad](https://www.helpndoc.com/fr/creer-des-livres-numeriques-epub)

##### MOTIFS DE CHANGEMENT <a id="page-motifsdechangement"></a>
_Page : `MOTIFSDECHANGEMENT.html` — Module Vente › CONFIGURATION › VENTE_

[image : NouvelElement146.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| O.D.T |  | X |  |
| Code | X | X |  |
| Description |  | X |  |
| GEN0015, GEN0016 |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 pour la ligne sélectionnée |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichier attachés | GEN0004 |
| Note | GEN0005 |
| Annuler et fermer | GEN0017 |
| Supprimer | GEN0010 pour la ligne sélectionnée |
| Champs spécifiques | GEN0007 pour la ligne sélectionnée |
Créé avec HelpNDoc Personal Edition: [Créer facilement des fichiers Qt Help](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

##### CADEAUX <a id="page-cadeaux"></a>
_Page : `CADEAUX.html` — Module Vente › CONFIGURATION › VENTE_

[image : NouvelElement392.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| O.D.T |  | X |  |
| Code | X | X |  |
| Description |  | X |  |
| Prix |  |  |  |
| Nombre total |  |  | En lecture seule |
| Nombre livré |  |  | En lecture seule |
| Nombre en stock |  |  | En lecture seule, Nombre total – Nombre livré |
| Entrées |  |  |  |
| Date réception |  |  |  |
| Quantité |  |  |  |
| GEN0015, GEN0016 |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 pour la ligne sélectionnée |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichier attachés | GEN0004 |
| Note | GEN0005 |
| Annuler et fermer | GEN0017 |
| Supprimer | GEN0010 pour la ligne sélectionnée |
| Champs spécifiques | GEN0007 pour la ligne sélectionnée |
| Mettre à jours les prix | Lorsque’on clique sur le bouton le système affiche le message suivant :<br>[image : NouvelElement393.png]<br>- Si on clique sur oui : Le système modifie les prix de tous les cadeaux de même type qui ne sont pas livrés<br>- Si on clique sur non : Le système ferme le message et il ne met pas à jours les prix des cadeaux |
Créé avec HelpNDoc Personal Edition: [Générateur de documentation d'aide HTML gratuit](https://www.helpndoc.com/fr)

##### TRANSITIONS <a id="page-transitions-com"></a>
_Page : `TRANSITIONS_COM.html` — Module Vente › CONFIGURATION › VENTE_

[image : NouvelElement600.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| O.D.T |  | X |  |
| De |  | X | Liste déroulante de [statut](#page-statuts-com) de départ |
| A |  | X | Liste déroulante de [statut](#page-statuts-com) d’arrivé |
| Avec proposition du passage automatique |  |  | Case à cocher, pour indiquer si la transition va être exécuter automatiquement si le seuil de versement (paramétré dans la [tranche](#page-cfg-tranches) ou dans le [statut](#page-statuts-com)) est vérifié.<br>Remarque1: Si le seuil de versement est vérifié, mais l'utilisateur n'a pas mentionné la date, le système automatiquement affecte la date du dernier règlement dans le champ en question.<br>Remarque2: Si on paramètre deux transitions qui ont le même statut de départ "Avec proposition de passage automatique", le système ignore ce paramétrage et il n'affiche pas le message de suggestion du passage automatique de statut si le seuil de versement est vérifié à la fermeture de [dossier de vente.](#page-dossiersdevente) |
| Liste des rôles |  |  | Les cases à cocher représentent les [rôles](#page-roles) des acteurs qui peuvent modifier le statut d’un « Dossier de vente». |
| Liste des champs obligatoires |  |  | Liste de champs obligatoires à vérifier lors d’une transition d’un [statut](#page-statuts-com) à un autre |
| GEN0015, GEN0016 |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 pour la ligne sélectionnée |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichier attachés | GEN0004 |
| Note | GEN0005 |
| Annuler et fermer | GEN0017 |
| Supprimer | GEN0010 pour la ligne sélectionnée |
| Champs spécifiques | GEN0007 pour la ligne sélectionnée |
Créé avec HelpNDoc Personal Edition: [Éditeur complet de livres électroniques ePub](https://www.helpndoc.com/fr/creer-des-livres-numeriques-epub)

##### CATEGORIES DES ACTIONS <a id="page-categoriesdesactions"></a>
_Page : `CATEGORIESDESACTIONS.html` — Module Vente › CONFIGURATION › VENTE_

[image : NouvelElement150.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| O.D.T |  | X | En lecture seule |
| Code |  | X | En lecture seule |
| Description |  | X | En lecture seule |
| GEN0015, GEN0016 |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
Créé avec HelpNDoc Personal Edition: [Créer des fichiers d'aide pour la plateforme Qt Help](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creez-des-fichiers-daide-pour-le-framework-qt-help)

##### SOUS-CATEGORIES DES ACTIONS <a id="page-souscategoriesdesactions"></a>
_Page : `SOUSCATEGORIESDESACTIONS.html` — Module Vente › CONFIGURATION › VENTE_

[image : NouvelElement540.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Code |  | X |  |
| Description |  | X |  |
| Catégorie parente |  | X | Liste déroulante des [catégories parente](#page-categoriesdesactions) |
| Statut |  |  | Liste déroulante des statuts des visites techniques |
| Avec alerte |  |  | Le saisi de la date alerte est obligatoire dans le masque des actions si la case est coché. |
| Mettre à jour le schéma de financement sur la base de la date alerte |  |  | Si l'utilisateur effectue une action et il choisi un résultat paramétré " Mettre à jour le schéma de financement sur la base de la date alerte ", le système met à jour les dates des documents qui ont une date inférieur à la date alerte mis dans l'action et qui ont un reste à payer supérieur à 0. |
| GEN0015, GEN0016 |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Enregistrer | GEN0001 pour la ligne sélectionnée |
| Supprimer | GEN0010 pour la ligne sélectionnée |
Créé avec HelpNDoc Personal Edition: [Éditeur complet de livres électroniques ePub](https://www.helpndoc.com/fr/creer-des-livres-numeriques-epub)

##### TYPES DES ACTIONS <a id="page-typesdesactions"></a>
_Page : `TYPESDESACTIONS.html` — Module Vente › CONFIGURATION › VENTE_

[image : NouvelElement737.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Code |  | X | En lecture seule |
| Description |  | X | En lecture seule |
| Système |  |  | En lecture seule<br> Pour indiquer les types d'action par défaut pré-configurés dans le système |
| Valeurs par défaut |  |  |  |
| Type d’action |  |  | Liste déroulante de types d’actions.<br> A la création d’une « [Prospections](#page-prospections) » ou un « [Prospects express](#page-prospectexpress)», le type choisi sera affectée par défaut dans la fiche. |
| GEN0015, GEN0016 |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
Créé avec HelpNDoc Personal Edition: [Qu'est-ce qu'un outil de création d'aide ?](https://www.helpauthoringsoftware.com/fr)

##### DECISIONS MEILLEURTAUX <a id="page-decisionsmeilleurtaux"></a>
_Page : `DECISIONSMEILLEURTAUX.html` — Module Vente › CONFIGURATION › VENTE_

[image : NouvelElement153.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| O.D.T |  | X |  |
| Code | X | X |  |
| Description |  | X |  |
| GEN0015, GEN0016 |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Enregistrer | GEN0001 pour la ligne sélectionnée |
| Supprimer | GEN0010 pour la ligne sélectionnée |
Créé avec HelpNDoc Personal Edition: [Créer des sites web d'aide facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

##### RAISONS DE CADEAUX <a id="page-raisonsdecadeaux"></a>
_Page : `RAISONSDECADEAUX.html` — Module Vente › CONFIGURATION › VENTE_

[image : NouvelElement546.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Code | C | X | En lecture seule |
| Description |  | X | En lecture seule |
| GEN0015, GEN0016 |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
Créé avec HelpNDoc Personal Edition: [Générateur de documentations PDF gratuit](https://www.helpndoc.com/fr)

## Module Service Après Vente <a id="page-moduleserviceapresvente"></a>
_Page : `ModuleServiceApresVente.html`_

Créé avec HelpNDoc Personal Edition: [Générateur de documentation et EPub gratuit](https://www.helpndoc.com/fr)

### DOSSIERS <a id="page-dossiers-sav"></a>
_Page : `DOSSIERS_SAV.html` — Module Service Après Vente_

[image : NouvelElement180.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Code |  |  | En lecture seule auto généré |
| Date |  |  |  |
| Sous garantie |  |  | En lecture seule, initialement le champ est vide<br> Si la date de dossier de réclamation < Date fin de garanti<br>- Le système affiche sous garanti (En vert)<br> Sinon<br>- Le système affiche hors garanti (En rouge) |
| Tranche |  | X | En lecture seule |
| Article |  |  | Sélecteur des [articles](#page-articles) |
| Client |  |  | En lecture seule |
| Date fin de garanti |  |  | En lecture seule,<br> La date affichée dans ce champ est calculé de la manière suivante :<br> Si date fiche technique est vide<br>- Date fin de garanti = Date octroi de titre foncier (à partir de la tranche) + Durée de garanti (à partir de la tranche)<br> Si la date fiche technique est paramétré<br>- Date fin de garantie = Date octroi de titre foncier (à partir de la tranche) + Durée de garanti (à partir de la tranche)) + Date fiche technique + Durée de garanti supplémentaire (à partir de la tranche)<br> Remarque : Si la date octroi de titre foncier est vide, le système prend la date début des finalisations comme une date repère de début de service après-vente.<br>Remarque : S’il y a un décalage entre la date fin de garanti et la date fiche technique, cette période sera considérer sous garantie. |
| Origine |  | X | Liste déroulante des [origines](#page-origines) |
| Responsable SAV |  |  | Liste déroulante de [personnels](#page-personnels), par défaut le système charge le responsable SAV qui a été affecté dans le paramétrage de la [tranche](#page-cfg-tranches). |
| PV N° |  |  |  |
| Date du PV |  |  |  |
| Dans le cadre d’une réception |  |  | Si la réclamation est faite par le client, la case « Dans le cadre de réception » doit être sélectionnée. |
| Liste des réclamations |  |  | La liste visualise les détails de réclamation :<br>- La date de réclamation<br>- Le statut<br>- L’emplacement<br>- La catégorie<br>- Le type<br>- Le fournisseur…<br> Encore, dans cette liste on peut faire les actions suivantes<br>- Ajouter une réclamation<br>- Éditer une réclamation<br>- Supprimer une réclamation<br> Remarque: Si on clique sur « Ajouter » et les champs du masque de réclamation ne sont pas enregistré le système affiche un message « Vous devez enregistrer vos modifications avant cette opération. Voulez-vous continuer ? »<br> [image : NouvelElement179.png]<br>- Si on clique « oui », le système enregistre les modifications et il affiche le masque de définition des réclamations de la figure ci-dessous<br>- Si on clique « Non », le système le système ferme la fenêtre. |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Supprimer | GEN0010 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Audit | GEN0020 |
Créé avec HelpNDoc Personal Edition: [Créer des documents d'aide facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

### RECLAMATIONS <a id="page-reclamations"></a>
_Page : `RECLAMATIONS.html` — Module Service Après Vente_

[image : NouvelElement181.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Code dossier |  | X | En lecture seule auto générer |
| Date |  |  | En lecture seule |
| Tranche |  |  | En lecture seule |
| Article |  |  | En lecture seule |
| Client |  |  | En lecture seule |
| Origine |  |  | En lecture seule |
| Date fin de garanti |  |  | En lecture seule |
| PV N° |  |  | En lecture seule |
| Date du PV |  |  | En lecture seule |
| Dans le cadre de réception |  |  | En lecture seule |
| Réclamation |  |  |  |
| Code |  |  | En lecture seule |
| Date |  |  | En lecture seule |
| Emplacement |  |  | Liste multi-choix d’emplacement, paramétrable dans la configuration de module stock |
| Catégorie |  |  | El lecture seule |
| Type |  |  | Sélecteur de types |
| Délai de résolution |  |  |  |
| Statut |  |  | Liste déroulante des [statuts](#page-statuts-sav) |
| Lot |  |  | Liste déroulante des lots chargée en fonction de la [catégorie de réclamation](#page-categoriesdereclamation) |
| Fournisseur |  |  | Sélecteur des [fournisseurs](#page-fournisseurs-adm)<br> Remarque : Le fournisseur est chargé automatiquement en fonction du lot |
| Date intervention |  |  |  |
| Date clôture |  |  |  |
| Notes |  |  |  |
| Champ de saisi des notes |  |  |  |
| Historique |  |  |  |
| Historique |  |  | La liste visualise l’historique des réclamations en indiquant :<br>- L’utilisateur<br>- La date<br>- L’état<br>- Le client<br>- L’article |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Supprimer | GEN0010 |
| Exporter | GEN0012 |
| Ouvrir dossier | Ouvrir la fiche de [dossier](#page-dossiers-sav) qui contient la réclamation sélectionnée |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Audit | GEN0020 |
Créé avec HelpNDoc Personal Edition: [Environnement de création d'aide complet](https://www.helpndoc.com/fr/outils-creation-aide)

### PLANNING <a id="page-planning"></a>
_Page : `PLANNING.html` — Module Service Après Vente_

[image : NouvelElement455.png]
Dans cet calendrier, le système affiche le planning des visites techniques du responsable service après vente affecté au tranche choisi

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Titre |  |  | En lecture seule |
| De |  |  | Date et heure début de la visite technique |
| à |  |  | Date et heure fin de la visite technique |
| Note |  |  |  |

### Actions pour les indisponibilités

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Supprimer | GEN0010 |
| Annuler | GEN0006 |

### Actions pour les visites technique

| LIBELLE | OBSERVATIONS |
|---|---|
| Affecter un statut (Statut) | On choisissant un statut le système modifie la couleur<br> Remarque: Les alertes sont générées selon le [paramétrage](#page-souscategoriesdesactions) effectué<br> Remarque: Les statuts possibles sont:<br>- Réalisée<br>- Réalisée à reprogrammer<br>- Non réalisée à reprogrammer |
| Ajouter une communication | Le système permet au responsable S.A.V d'ajouter une communication à un statut |
Créé avec HelpNDoc Personal Edition: [Créer des livres électroniques EPub facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

### TABLEAUX DE BORDS <a id="page-tableauxdebords-sav"></a>
_Page : `TABLEAUXDEBORDS_SAV.html` — Module Service Après Vente_

Créé avec HelpNDoc Personal Edition: [Générateur d'aide complet](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

#### RAPPORTS <a id="page-rapports-sav"></a>
_Page : `RAPPORTS_SAV.html` — Module Service Après Vente › TABLEAUX DE BORDS_

Créé avec HelpNDoc Personal Edition: [Produire des livres électroniques facilement](https://www.helpndoc.com/fr/creer-des-livres-numeriques-epub)

##### SUIVI DES RECLAMATIONS <a id="page-savtdbclaimstate"></a>
_Page : `SavTdbClaimState.html` — Module Service Après Vente › TABLEAUX DE BORDS › RAPPORTS_

[image : NouvelElement395.png]
Nombre de réclamations groupé par catégories / Nombre total des réclamations
[image : NouvelElement396.png]
Nombre de réclamations groupé par types / Nombre total des réclamations
[image : NouvelElement397.png]
Nombre de réclamations groupé par tranche / nombre total des réclamations
[image : NouvelElement398.png]
Nombre de réclamations par mois
Créé avec HelpNDoc Personal Edition: [Environnement de création d'aide complet](https://www.helpndoc.com/fr/outils-creation-aide)

##### ETAT GLOBAL DES INTERVENTIONS <a id="page-savtdbinterventionstate"></a>
_Page : `SavTdbInterventionState.html` — Module Service Après Vente › TABLEAUX DE BORDS › RAPPORTS_

| Article | Client | État dossier | État intervention | Date | De | A |
|---|---|---|---|---|---|---|
| Tranche |  |  |  |  |  |  |
| Code article | Client | Statut de dossier de vente | Statut de l’intervention | Date d’intervention | Heure début de l’intervention | Heure fin de l’intervention |
Remarque :

- L’utilisateur peu personnalisé les tranches à visualiser
- Les interventions sont classées par tranche
Créé avec HelpNDoc Personal Edition: [Créer des documents d'aide PDF facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

### CONFIGURATION <a id="page-configuration-sav"></a>
_Page : `CONFIGURATION_SAV.html` — Module Service Après Vente_

Créé avec HelpNDoc Personal Edition: [Outils facile d'utilisation pour créer des aides HTML et des sites web](https://www.helpndoc.com/fr/outils-creation-aide)

#### CATEGORIES DE RECLAMATION <a id="page-categoriesdereclamation"></a>
_Page : `CATEGORIESDERECLAMATION.html` — Module Service Après Vente › CONFIGURATION_

[image : NouvelElement191.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Code | X | X |  |
| Description |  | X |  |
| Catégorie |  |  | Sélecteur des catégories |
| Liste des types |  |  | La liste visualise les types de réclamations en indiquant :<br>- Le Code<br>- La description<br>- Le délai de réservation<br> Encore, dans cette liste on peut<br>- Ajouter un nouveau type de réclamation<br>- Editer une réclamation<br>- Supprimer une réclamation<br> Remarque : Le nouveau type de réclamation est associé à une catégorie sélectionnée |
| GEN0015 |  |  |  |

### MASQUE DE GESTION DE TYPE DE RECLAMATION
[image : NouvelElement190.png]
Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Code |  | X |  |
| Description |  |  |  |
| Délai de traitement |  |  | Le nombre de jours nécessaire pour réparer une panne |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Audit | GEN0020 |
Créé avec HelpNDoc Personal Edition: [Générateur complet de livres électroniques ePub](https://www.helpndoc.com/fr/creer-des-livres-numeriques-epub)

#### STATUTS <a id="page-statuts-sav"></a>
_Page : `STATUTS_SAV.html` — Module Service Après Vente › CONFIGURATION_

[image : NouvelElement193.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Ordre de tri |  | X | Ordre de tri d'un statut |
| Code | X | X |  |
| Description |  | X |  |
| Type |  |  | Liste déroulante de types de statut, elle peut prendre une des valeurs suivantes :<br>- Nouveau<br>- Intermédiaire<br>- Annulation<br>- Terminer avec échec<br>- Terminer avec succès |
| Durée de vie (en jour) |  |  | Durée de vie d’un statut |
| Statut de rejet |  |  | Pour indiquer que le statut crée ou sélectionné est un statut de rejet |
| Statut de clôture |  |  | Pour indiquer que le statut crée ou sélectionné est un statut de clôture |
| Couleur |  |  |  |
| Image |  |  |  |
| Champs obligatoires |  |  |  |
| Date |  |  | Liste des champs paramétrables pour un statut |
| Date intervention |  |  |  |
| Date clôture |  |  |  |
| GEN0015, GEN0016 |  |  |  |
Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Enregistrer | GEN0001 pour la ligne sélectionnée |
| Supprimer | GEN0010 pour la ligne sélectionnée |
Créé avec HelpNDoc Personal Edition: [Écrire des livres électroniques ePub pour l'iPad](https://www.helpndoc.com/fr/creer-des-livres-numeriques-epub)

#### TRANSITIONS <a id="page-transitions-sav"></a>
_Page : `TRANSITIONS_SAV.html` — Module Service Après Vente › CONFIGURATION_

[image : NouvelElement195.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Ordre de tri |  | X | Ordre de tri d’une transition |
| De |  | X | Liste déroulante de [Statut](#page-statuts-sav) de départ |
| A |  | X | Liste déroulante de [Statut](#page-statuts-sav) d’arriver |
| Liste des rôles |  |  | Les cases à cocher représentent les [rôles](#page-roles) des acteurs qui peuvent modifier le statut d’une « [Réclamation](#page-reclamations)». |
|  |  |  |  |
|  |  |  |  |
|  |  |  |  |
|  |  |  |  |
|  |  |  |  |
|  |  |  |  |
|  |  |  |  |
| GEN0015, GEN0016 |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Audit | GEN0020 |
Créé avec HelpNDoc Personal Edition: [Créer des documents d'aide CHM facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

#### ORIGINES <a id="page-origines"></a>
_Page : `ORIGINES.html` — Module Service Après Vente › CONFIGURATION_

[image : NouvelElement194.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Détails |  |  |  |
| O.D.T |  | X |  |
| Code | X | X |  |
| Description |  | X |  |
| Valeurs par défaut |  |  |  |
| Origine par défaut |  |  | Liste déroulante des origines |
| GEN0015 |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Enregistrer | GEN0001 pour la ligne sélectionnée |
| Supprimer | GEN0010 pour la ligne sélectionnée |
Créé avec HelpNDoc Personal Edition: [Produire des livres EPub gratuitement](https://www.helpndoc.com/fr/creer-des-livres-numeriques-epub)

#### CONTENANCES <a id="page-contenances-sav"></a>
_Page : `CONTENANCES_SAV.html` — Module Service Après Vente › CONFIGURATION_

[image : NouvelElement622.png]
Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| O.D.T |  | X |  |
| Code | X | X |  |
| Description |  | X |  |
| GEN0015 |  |  |  |
Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Enregistrer | GEN0001 pour la ligne sélectionnée |
| Supprimer | GEN0010 pour la ligne sélectionnée |
Créé avec HelpNDoc Personal Edition: [Générer facilement des livres électroniques Kindle](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creer-ebooks-pour-kindle-amazon)

## Module Location <a id="page-modulelocation"></a>
_Page : `ModuleLocation.html`_

Créé avec HelpNDoc Personal Edition: [Créer facilement des fichiers Qt Help](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

### CLIENTS <a id="page-clientsrnt"></a>
_Page : `CLIENTSRNT.html` — Module Location_

[image : NouvelElement771.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Code | X | X | Code auto généré en lecture seule |
| Afficher comme |  | X | En lecture seule |
| Forme juridique |  | X | Liste déroulante des [formes juridique](#page-formessociale) |
| Personnel |  |  | Liste déroulante de [personnels](#page-personnels)[PERSONNELS.html](#page-personnels)[qui sont paramétrés avec affichage dans la liste des commerciaux](#page-personnels) |
| Nature |  |  | Liste déroulante de [nature](#page-natures) |
| Général (PH) |  |  |  |
| Titre |  | X | Liste déroulante de [titres](#page-titres) |
| Sexe |  | X | Liste déroulante de [sexe](#page-sexes) |
| Nom |  | X |  |
| Prénom |  | X |  |
| Pays de résidence |  |  | Liste déroulante des [pays](#page-pays) |
| Mobile |  | X |  |
| E-mail |  | X |  |
| Date de naissance |  |  |  |
| Lieu |  |  |  |
| Situation familiale |  |  | Liste déroulante des [situations familiale](#page-situationsfamiliale) |
| Enfant |  |  |  |
| Pièce d’identité |  |  | Liste déroulante de [type de pièce d’identité](#page-typespieceidentite) |
| N° |  |  |  |
| Délivrée le |  |  |  |
| Lieu |  |  |  |
| Valide jusqu’au |  |  |  |
| Lieu |  |  |  |
| Fils de |  |  |  |
| Et de |  |  |  |
| Téléphone |  |  |  |
| Adresse |  |  |  |
| Pays |  |  | Liste déroulante des [pays](#page-pays) |
| Code postal |  |  |  |
| Ville |  |  | Liste déroulante de [villes](#page-villes) |
| Nationalité |  |  | Liste déroulante de [nationalités](#page-nationalites) |
| Résidence |  |  | Liste déroulante de [résidences](#page-typesresidence) |
| Profession |  |  | Liste déroulante de [professions](#page-professions) |
| Salaire |  |  | Liste déroulante des [fourchettes salariales](#page-salaires) |
| Employeur |  |  |  |
| Ancienneté |  |  |  |
| Régime matrimonial |  |  | Liste déroulante de [régimes matrimoniaux](#page-regimesmatrimonial) |
| Général (PM, SARL, SURAL, SA, OG, ONG) |  |  |  |
| Raison sociale |  | X |  |
| Pays de résidence |  |  | Liste déroulante de [pays de résidences](#page-pays) |
| Mobile |  | X |  |
| Email |  | X |  |
| Téléphone |  |  |  |
| M.F |  |  |  |
| R.C |  |  |  |
| C.D |  |  |  |
| Activité |  |  |  |
| Capital |  |  |  |
| Adresse |  |  |  |
| Pays |  |  | Liste déroulante de [pays](#page-pays) |
| Code postal |  |  |  |
| Ville |  |  | Liste déroulante de [villes](#page-villes) |
| Nationalité |  |  | Liste déroulante de [nationalités](#page-nationalites) |
| Résidence |  |  | Liste déroulante de [résidences](#page-typesresidence) |
| Date de création |  |  |  |
| Lieu |  |  |  |
| Qualité |  |  |  |
| Autres |  |  |  |
| Assujettissement |  |  | Liste déroulante d’[assujettissements](#page-assujetissements) |
| Banque |  |  | Liste déroulante de [banques](#page-banques) |
| Agence |  |  |  |
| RIB |  |  |  |
| IBAN |  |  |  |
| Code comptable |  |  |  |
| Compte collectif |  |  |  |
| Téléphone 2 |  |  |  |
| Téléphone 3 |  |  |  |
| Fax |  |  |  |
| Site web |  |  |  |
| Contacts |  |  |  |
| Liste de contacts |  |  | La liste visualise les contacts en indiquant :<br>- Le contact<br>- Le [type de relation](#page-typesderelation)<br> Sur cette liste on peut effectuer les opérations suivantes :<br>- Ajouter un contact<br>- Éditer un contact<br>- Supprimer un contact |
| Notes |  |  |  |
| Champ de saisi des notes |  |  |  |
| Prospections |  |  |  |
| Liste de prospections |  |  | La liste visualise les prospections faites par un client en indiquant :<br>- Le code<br>- La date<br>- La tranche<br>- L’article<br>- La catégorie… |
| Dossier |  |  |  |
| Liste de dossiers |  |  | La liste visualise les dossiers de location en affichant :<br>- Le statut<br>- La tranche<br>- L’article<br>- A payer<br>- Réglé… |
| Règlements |  |  |  |
| Liste de règlements |  |  | La liste visualise les règlements en affichant :<br>- La description<br>- Le montant<br>- L’historique |
| Historique |  |  |  |
| Liste d’historique |  |  | La liste visualise l’historique de dossier de vente d’un client en affichant :<br>- L’utilisateur<br>- La date<br>- L’état<br>- Le N° Dossier<br>- L’article<br>- Les détails |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Imprimer | GEN0011 |
| Éditer | GEN0009 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
Créé avec HelpNDoc Personal Edition: [Générateur complet de livres électroniques ePub](https://www.helpndoc.com/fr/creer-des-livres-numeriques-epub)

### PROSPECTS <a id="page-prospectsrnt"></a>
_Page : `PROSPECTSRNT.html` — Module Location_

[image : NouvelElement772.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Code | X | X | Code auto généré en lecture seule |
| Afficher comme |  | X | En lecture seule |
| Forme juridique |  | X | Liste déroulante des [formes juridique](#page-formessociale) |
| Personnel |  |  | Liste déroulante des [personnels](#page-personnels)[PERSONNELS.html](#page-personnels)[qui sont paramétrés avec affichage dans la liste des commerciaux](#page-personnels) |
| Nature |  |  | Liste déroulante de [nature](#page-natures) |
| Général (PH) |  |  |  |
| Titre |  | X | Liste déroulante de [titres](#page-titres) |
| Sexe |  | X | Liste déroulante de [sexe](#page-sexes) |
| Nom |  | X |  |
| Prénom |  | X |  |
| Pays de résidence |  |  | Liste déroulante des [pays](#page-pays) |
| Mobile |  | X |  |
| E-mail |  | X |  |
| Date de naissance |  |  |  |
| Lieu |  |  |  |
| Situation familiale |  |  | Liste déroulante des [situations familiale](#page-situationsfamiliale) |
| Enfant |  |  |  |
| Pièce d’identité |  |  | Liste déroulante de [type de pièce d’identité](#page-typespieceidentite) |
| N° |  |  |  |
| Délivrée le |  |  |  |
| Lieu |  |  |  |
| Valide jusqu’au |  |  |  |
| Lieu |  |  |  |
| Fils de |  |  |  |
| Et de |  |  |  |
| Téléphone |  |  |  |
| Adresse |  |  |  |
| Pays |  |  | Liste déroulante des [pays](#page-pays) |
| Code postal |  |  |  |
| Ville |  |  | Liste déroulante de [villes](#page-villes) |
| Nationalité |  |  | Liste déroulante de [nationalités](#page-nationalites) |
| Résidence |  |  | Liste déroulante de [résidences](#page-typesresidence) |
| Profession |  |  | Liste déroulante de [professions](#page-professions) |
| Salaire |  |  | Liste déroulante des [fourchettes salariales](#page-salaires) |
| Employeur |  |  |  |
| Ancienneté |  |  |  |
| Régime matrimonial |  |  | Liste déroulante de [régimes matrimoniaux](#page-regimesmatrimonial) |
| Général (PM, SARL, SURAL, SA, OG, ONG) |  |  |  |
| Raison sociale |  | X |  |
| Pays de résidence |  |  | Liste déroulante de [pays de résidences](#page-pays) |
| Mobile |  | X |  |
| Email |  | X |  |
| Téléphone |  |  |  |
| M.F |  |  |  |
| R.C |  |  |  |
| C.D |  |  |  |
| Activité |  |  |  |
| Capital |  |  |  |
| Adresse |  |  |  |
| Pays |  |  | Liste déroulante de [pays](#page-pays) |
| Code postal |  |  |  |
| Ville |  |  | Liste déroulante de [villes](#page-villes) |
| Nationalité |  |  | Liste déroulante de [nationalités](#page-nationalites) |
| Résidence |  |  | Liste déroulante de [résidences](#page-typesresidence) |
| Date de création |  |  |  |
| Lieu |  |  |  |
| Qualité |  |  |  |
| Autres |  |  |  |
| Assujettissement |  |  | Liste déroulante d’[assujettissements](#page-assujetissements) |
| Banque |  |  | Liste déroulante de [banques](#page-banques) |
| Agence |  |  |  |
| RIB |  |  |  |
| IBAN |  |  |  |
| Code comptable |  |  |  |
| Compte collectif |  |  |  |
| Téléphone 2 |  |  |  |
| Téléphone 3 |  |  |  |
| Fax |  |  |  |
| Site web |  |  |  |
| Est un ancien client |  |  | Un flag pour désigner si le prospect est un ancien client ou non |
| Contacts |  |  |  |
| Liste de contacts |  |  | La liste visualise les contacts en indiquant :<br>- Le contact<br>- Le [type de relation](#page-typesderelation)<br> Sur cette liste on peut effectuer les opérations suivantes :<br>- Ajouter un contact<br>- Éditer un contact<br>- Supprimer un contact |
| Notes |  |  |  |
| Champ de saisi des notes |  |  |  |
| Dossiers de location |  |  |  |
| Liste de dossiers de location |  |  | La liste visualise les dossiers de location en affichant :<br>- Le statut<br>- La tranche<br>- L’article<br>- A payer<br>- Réglé… |
| Règlements |  |  |  |
| Liste de règlements |  |  | La liste visualise les règlements en affichant :<br>- La description<br>- Le montant<br>- L’historique |
| Remarque1 : La combinaison des champs « Description » + « mobile » ou « Description » + « E-mail » doivent êtres uniques.<br> Remarque2 : Dans le module location, le système affiche par défaut la fiche de type "Personne morale" de prospect. |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Imprimer | GEN0011 |
| Éditer | GEN0009 |
| Exporter | GEN0012 |
| Supprimer | GEN0010 |
| Importer | GEN0018 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
Créé avec HelpNDoc Personal Edition: [Créer des livres électroniques EPub facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

### CONTACTS <a id="page-contacts"></a>
_Page : `CONTACTS.html` — Module Location_

[image : NouvelElement773.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Code | X | X | Code auto généré en lecture seule |
| Afficher comme |  | X | En lecture seule |
| Forme juridique |  | X | Liste déroulante des formes juridique |
| Personnel |  |  | Liste déroulante de [personnels](#page-personnels) |
| Nature |  |  | Liste déroulante de nature |
| Général (PH) |  |  |  |
| Titre |  | X | Liste déroulante de titres |
| Sexe |  | X | Liste déroulante de sexe |
| Nom |  | X |  |
| Prénom |  | X |  |
| Pays de résidence |  |  | Liste déroulante des pays |
| Mobile |  | X |  |
| E-mail |  | X |  |
| Date de naissance |  |  |  |
| Lieu |  |  |  |
| Situation familiale |  |  | Liste déroulante des situations familiale |
| Enfant |  |  |  |
| Pièce d’identité |  |  | Liste déroulante de type de pièce d’identité |
| N° |  |  |  |
| Délivrée le |  |  |  |
| Lieu |  |  |  |
| Valide jusqu’au |  |  |  |
| Lieu |  |  |  |
| Fils de |  |  |  |
| Et de |  |  |  |
| Téléphone |  |  |  |
| Adresse |  |  |  |
| Pays |  |  | Liste déroulante des pays |
| Code postal |  |  |  |
| Ville |  |  | Liste déroulante de villes |
| Nationalité |  |  | Liste déroulante de nationalités |
| Résidence |  |  | Liste déroulante de résidences |
| Profession |  |  | Liste déroulante de professions |
| Salaire |  |  | Liste déroulante des fourchettes salariales |
| Employeur |  |  |  |
| Ancienneté |  |  |  |
| Régime matrimonial |  |  | Liste déroulante de régimes matrimoniaux |
| Général (PM, SARL, SURAL, SA, OG, ONG) |  |  |  |
| Raison sociale |  | X |  |
| M.F |  |  |  |
| R.C |  |  |  |
| C.D |  |  |  |
| Date de création |  |  |  |
| Lieu |  |  |  |
| Activité |  |  |  |
| Capital |  |  |  |
| Représentant légal |  |  | Sélecteur de tiers |
| Qualité |  |  |  |
| Pays de résidence |  |  | Liste déroulante de pays de résidences |
| Mobile |  | X |  |
| Téléphone 1 |  |  |  |
| Email |  | X |  |
| Adresse |  |  |  |
| Pays |  |  | Liste déroulante de pays |
| Code postal |  |  |  |
| Ville |  |  | Liste déroulante de villes |
| Nationalité |  |  | Liste déroulante de nationalités |
| Résidence |  |  | Liste déroulante de résidences |
| Autres |  |  |  |
| Assujettissement |  |  | Liste déroulante d’assujettissements |
| Banque |  |  | Liste déroulante de banques |
| Agence |  |  |  |
| RIB |  |  |  |
| IBAN |  |  |  |
| Code comptable |  |  |  |
| Compte collectif |  |  |  |
| Téléphone 2 |  |  |  |
| Téléphone 3 |  |  |  |
| Fax |  |  |  |
| Site web |  |  |  |
| C'est un ancien client |  |  |  |
| C'est un prestataire |  |  |  |
| Contacts |  |  |  |
| Liste de contacts |  |  | La liste visualise les contacts en indiquant :<br>- Le contact<br>- Le type de relation<br> Sur cette liste on peut effectuer les opérations suivantes :<br>- Ajouter un contact<br>- Éditer un contact<br>- Supprimer un contact |
| Notes |  |  |  |
| Champ de saisi des notes |  |  |  |
| Remarque : La combinaison des champs « Description » + « mobile » ou « Description » + « E-mail » doivent êtres uniques. |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Importer | GEN0018 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichier attaché | GEN0004 |
| Note | GEN0005 |
| Annuler et fermer | GEN0017 |
| Champs spécifiques | GEN0007 |
Créé avec HelpNDoc Personal Edition: [Générateur de documentation d'aide HTML gratuit](https://www.helpndoc.com/fr)

### PROSPECTIONS <a id="page-rnt-prospections"></a>
_Page : `RNT_PROSPECTIONS.html` — Module Location_

[image : NouvelElement677.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Code | X | X | Code auto généré en lecture seule |
| Date |  | X | Par défaut date de jour |
| Prospect |  | X | Sélecteur de [prospect](#page-prospectsrnt) |
| Tranche |  | X | Liste de déroulante de [tranches](#page-cfg-tranches) visible dans le module vente |
| Article |  |  | Sélecteur des [articles](#page-articles)<br> Remarque: Le système affiche les articles et les sous articles qui ont une vocation "Location". |
| Commercial |  |  | Liste déroulante de [commercial](#page-personnels)[PERSONNELS.html](#page-personnels)[qui sont paramétrés avec affichage dans la liste des commerciaux](#page-personnels) |
| Action commercial |  |  | Liste déroulantes des[actions commerciales](#page-rnt-actionscommerciales) |
| Nature |  |  | Liste déroulante de [natures](#page-rnt-naturesprospections) |
| Source de contact |  | X | Liste déroulante de [source de contact](#page-rnt-sourcedecontact) |
| Type |  |  | Liste déroulante de [type](#page-typesdesactions) |
| Support |  |  | Liste déroulante de [supports](#page-rnt-supportssourcedecontact) |
| Note |  |  | Le champ note prend le type d’action choisi dans la liste déroulante des types. |
| Archivée |  |  | En cliquant sur le bouton archiver, le système affiche un masque dans lequel on :<br>- choisit la raison d’archivage<br>- Saisit une note |
| Autre tranches |  |  |  |
| Liste des autres tranches |  |  | Le système affiche une liste de projet en visualisant :<br>- Le projet<br>- Les champs crée le, crée par, modifier le et modifier par<br> Aussi, dans cette liste on peut effectuer les opérations suivantes<br>- Ajouter un projet<br>- Supprimer un projet |
| Profile |  |  |  |
| Articles |  |  | [Catégorie](#page-stk-categories-item) articles |
|  |  | [Types](#page-stk-types-item) articles |  |
| Sous articles |  |  | [Catégorie](#page-stk-categories-item) sous articles |
|  |  | [Types](#page-stk-types-item) sous articles |  |
| Actions |  |  |  |
| Liste des actions |  |  | La liste visualise le nombre d’actions groupées par [type](#page-typesdesactions) |
| Autres |  |  |  |
| Superficie (m²) de |  |  |  |
| A |  |  |  |
| Budget de |  |  |  |
| A |  |  |  |
| Avance |  |  |  |
| Crédit bancaire |  |  |  |
| Fond propre |  |  |  |
| Parrain |  |  |  |
| Remarque 1: Selon les habilitations accordées à un rôle, dépend le droit de modification des champs « Action commercial », « Type » et « Note ».<br>- Si un rôle n’a pas le droit d’administrer les prospections, après l’enregistrement de la fiche, les champs cités ci-dessus seront en lecture seule,<br>- Si un rôle a le droit d’administrer les prospections, après l’enregistrement de la fiche, les champs cités ci-dessus seront toujours modifiables.<br>Remarque 2 : Le système contrôle l’unicité de la prospection en vérifiant les trois champs prospect, tranche et article:<br>- S’il existe une prospection avec le même prospect, la même tranche et le même article, le système affiche un message d’erreur « Il existe déjà un autre enregistrement avec les même données de base ! »<br>- S’il existe une prospection avec le même prospect, la même tranche et un article vide, le système affiche un message d’erreur « Il existe déjà un autre enregistrement avec les même données de base ! »<br>- S’il existe une prospection avec le même prospect, la même tranche et un article différent, le système affiche un message d’information « Il existe un autre enregistrement avec les données de bases suivantes :<br>- Entité : « Description entité »<br>- Tranche : « Description tranche »<br>- Article : « Description article » Voulez-vous continuez ?<br>- Si on clique sur « Oui » : Le système enregistre la prospection<br>- Si on clique sur « Non » : Le système n’enregistre pas la prospection |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Imprimer | GEN0011 |
| [Créer/ Ouvrir un dossier](#page-dossiersdevente) | En cliquant sur le bouton « Créer/ Ouvrir un dossier » le système créé un nouveau dossier de location et il archive la prospection, sinon il ouvre le dossier correspondant. |
| Archiver | En cliquant sur bouton « Archiver » le système affecte une date d’archivage et il désactive toutes les relances associées à cette prospection. |
Créé avec HelpNDoc Personal Edition: [Produire facilement des livres électroniques Kindle](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creer-ebooks-pour-kindle-amazon)

### DOSSIERS DE LOCATION <a id="page-rnt-folder"></a>
_Page : `RNT_FOLDER.html` — Module Location_

[image : NouvelElement840.bmp]
Champs
\| LIBELLE \| U \| O \| OBSERVATION \| \|---\|---\|---\|---\| \| Statut \| \| X \| Sélecteur des [statuts](#page-statuts-rnt) \| \| Code \| \| \| En lecture seule
 Auto généré \| \| Date \| \| \| Par défaut, la date de jour \| \| Projet \| \| \| En lecture seule,
 Le système affiche la description de la tranche dont il appartient l’article \| \| Superficie \| \| \| En lecture seule,
 Superficie d’article \| \| Catégorie \| \| \| En lecture seule
 Catégorie d’[article](#page-articles) \| \| Type \| \| \| En lecture seule
 Type d’[article](#page-articles) \| \| Commercial \| \| \| En lecture seule
 Sélecteur des [employés](#page-personnels) \| \| Client \| \| \| En lecture seule,
 Le système affiche la description du locataire \| \| Usage \| \| \| \| \| Contact \| \| \| \| \| Date début \| \| \| Par défaut date de jour \| \| Périodicité \| \| \| Liste déroulante des périodicités \| \| Nombre de période \| \| \| \| \| Date reconduction \| \| \| En lecture seule
 Date début + (nombre de période * périodicité) \| \| Type de reconduction \| \| \| On peut avoir deux types de reconduction:
- Tacite
- Sur demande \| \| Taux d'augmentation \| \| \| Le dernier loyer HT * taux d'augmentation \| \| Fréquence d'augmentation (mois) \| \| \| \| \| Date prochaine augmentation \| \| \| Le système appliquera l'augmentation à partir de la date mentionnée dans ce champ \| \| Facturation groupée par client \| \| \| Si la case " Facturation groupée par client " est cochée, le système génère une facture qui groupe tous les échéances sélectionnées \| \| Loyer et syndic groupés dans la même ligne \| \| \| Si la case " Loyer et syndic groupés dans la même ligne " est cochée, le système groupe le loyer et le syndic dans la même échéance, sinon il affiche chacun dans une échéance. \| \| Montant du syndic basé sur la tarification \| \| \| Si la case " Montant du syndic basé sur la tarification " est cochée, le système met à jour le montant de syndic à partir de l'écran "[Tarification du syndic](#page-syn-pricing)". \| \| Loyer TVA \| \| \| \| \| Loyer \| \| \| \| \| Mensuel \| \| \| \| \| HT \| \| \| \| \| TTC \| \| \| Montant loyer mensuel HT + ( TVA * Montant loyer mensuel HT ) \| \| Par période \| \| \| \| \| HT \| \| \| Montant loyer par période HT = Montant loyer mensuel * nombre de période \| \| TTC \| \| \| Montant loyer par période HT + ( TVA * Montant loyer par période HT ) \| \| Annuel \| \| \| \| \| HT \| \| \| Montant loyer annuel HT = Montant loyer mensuel HT *12 \| \| TTC \| \| \| Montant loyer annuel HT + ( TVA * Montant loyer annuel HT) \| \| Caution \| \| \| \| \| Nbr. mois \| \| \| \| \| HT \| \| \| Loyer mensuel HT * nombre de mois \| \| TVA \| \| \| \| \| TTC \| \| \| Caution HT + (TVA * Caution HT) \| \| Syndic TVA \| \| \| \| \| Syndic \| \| \| \| \| Mensuel \| \| \| \| \| HT \| \| \| \| \| TTC \| \| \| Montant syndic mensuel HT + ( TVA * Montant syndic mensuel HT ) \| \| Par période \| \| \| \| \| HT \| \| \| Montant syndic par période HT = Montant syndic mensuel * nombre de période \| \| TTC \| \| \| Montant syndic par période HT + ( TVA * Montant syndic par période HT ) \| \| Annuel \| \| \| \| \| HT \| \| \| Montant syndic annuel HT = Montant syndic mensuel HT *12 \| \| TTC \| \| \| Montant syndic annuel HT + ( TVA * Montant syndic annuel HT) \| \| Échéance \| \| \| \| \| Mensuel \| \| \| \| \| HT \| \| \| Montant loyer mensuel HT + Montant syndic mensuel HT \| \| TTC \| \| \| Montant loyer mensuel TTC + Montant syndic mensuel TTC \| \| Par période \| \| \| \| \| HT \| \| \| Montant loyer par période HT + Montant syndic par période HT \| \| TTC \| \| \| Montant loyer par période TTC + Montant syndic par période TTC \| \| Annuel \| \| \| \| \| HT \| \| \| Montant loyer annuel HT + Montant syndic annuel HT \| \| TTC \| \| \| Montant loyer annuel TTC + Montant syndic annuel TTC \| \| Échéancier \| \| \| \| \| Liste d’échéanciers \| \| \| Dans cette liste le système affiche
- L' échéance
- La catégorie
- La description
- Le montant HT
- Le taux TVA
- Le montant TTC
- Le net à payer
- Le montant réglé
- Le montant reste
- Le nombre de jours : Durée en jour de la période
- Le nombre de jours exonérés TVA:
- Le reste à facturer HT (Montant échéance HT - Montant affecté HT dans la facture - Montant avoir HT)
- La notes...
Aussi, dans cette liste on peut effectuer les opérations suivantes :
- Ajouter
- Éditer
- Supprimer
- Ajouter règlement
- Ajouter paiement \| \| Sous articles \| \| \| \| \| Liste des sous articles \| \| \| Dans cette liste, le système affiche les détails des sous articles en indiquant:
- Le code
- La description
- Affecté par défaut
- Type
- Catégorie
- Notes
Aussi, dans cette liste on peut effectuer les opérations suivantes
- Ajouter un sous article
- Supprimer un sous article \| \| Reconduction \| \| \| \| \| Liste des reconductions \| \| \| Dans cette liste, le système visualise
- Date début
- Date fin
- Note
Aussi, à partir de cette liste on peut supprimer une reconduction. \| \| Dates \| \| \| \| \| Date signature de contrat \| \| \| \| \| Notaire/Avocat \| \| \| Sélecteur du[notaire/avocat](#page-fournisseurs-adm) \| \| Date signature de contrat par le client \| \| \| \| \| Date enregistrement contrat \| \| \| \| \| Date première occupation \| \| \| \| \| Date fiche assurance \| \| \| \| \| Résiliation \| \| \| \| \| Date résiliation \| \| \| \| \| Montant résiliation \| \| \| \| \| Raisons \| \| \| \| \| Exonérations TVA \| \| \| \| \| Liste des périodes d’exonération \| \| \| [image : NouvelElement684.png]
La liste visualise les périodes d’exonérations en indiquant:
- La date début
- La date fin
Aussi, dans cette liste on peut effectuer les opérations suivantes:
- Ajouter une exonération
- Supprimer une exonération
- Éditer une exonération
Remarque: On ne doit pas avoir un chevauchement dans les périodes d'exonérations TVA ajoutées. \| \| Avenants \| \| \| \| \| Liste des avenants \| \| \| Dans cette liste le système visualise les avenants en indiquent:
- La date effective
- La date
- La validité
- La périodicité
- Le nombre de période
- Le taux d'augmentation
- La date prochaine augmentation
- La note
Aussi, dans cette liste on peut effectuer les opérations suivantes:
- Ajouter un avenant
- Éditer un avenant
- Supprimer un avenant
Remarques :
- On crée un avenant pour modifier : Date prochaine augmentation, Périodicité, Nombre de période, Taux d'augmentation, Fréquence d'augmentation, Type de reconduction
- Par défaut, le système affiche dans les champs "Date" et "Date effective" la date de la dernière échéance + 1 jour.
- La suppression n'est permise que pour les avenants non validés.
- Le système applique le paramétrage de l'avenant à partir la date effective s'il est validé.
- Si la date de la reconduction est supérieure ou égale à la date effective de l'avenant, le système applique les nouvelles clauses (Périodicité, taux d'augmentation...) de l'avenant pour les échéances en question. \| \| Notes \| \| \| \| \| Notes \| \| \| \| \| Règlements \| \| \| \| \| Liste des règlements \| \| \| Le système affiche la liste des factures en indiquant:
- État
- Date pièce
- Date échéance
- Description
- Base
- RAS
- Montant
- Référence
- Reste à affecter
- N° Reçu
- Facture
- Note \| \| Factures \| \| \| \| \| Liste des factures \| \| \| Le système affiche la liste des factures en indiquant:
- Date
- Code
- Total HT
- Total TVA
- Total TTC
- Montant Régulation
- Timbre
- Net à payer
- Réglé
- Reste
- Avoirs
- Notes \| \| Avoirs \| \| \| \| \| Liste des avoirs \| \| \| Le système affiche la liste des factures en indiquant:
- Date
- Code
- Total HT
- Total TVA
- Total TTC
- Montant Régulation
- Timbre
- Net à payer
- Réglé
- Reste
- Notes \| \| Historique \| \| \| \| \| Historique de dossier de location \| \| \| La liste visualise l’historique de dossier de location en indiquant :
- L’utilisateur
- L’état
- Le Client
- L’article \|

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Éditer | GEN0009 |
| Imprimer | GEN0011 |
| Exporter | GEN0008 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Imprimer | GEN0011 |
| Générer | Le système génère l'échéancier de location selon le paramétrage effectué<br> Exemple:<br> - Loyer HT: 1000<br> - Date début: 01/01/2018<br> - Périodicité: Mensuelle<br> - Nombre de période: 12<br>==> Le système génère 12 échéances du 01/01/2018 au 01/12/2018 avec un loyer HT 1000 comme elle indique la capture ci-dessous<br>[image : NouvelElement847.bmp]<br> Remarque: Le système désactive le bouton " Générer " lorsqu'on ajoute une reconduction. |
| Reconduire | [image : NouvelElement779.png]<br>Lorsqu'on clic sur le bouton "reconduire", le système affiche un écran dans lequel il indique:<br>- La date début<br>- La date fin<br>- Les nouvelles échéances.<br>Remarques:<br>- Le système active le bouton "reconduction" lorsque le statut de dossier est paramétré "Est une location effective"<br>- Le système applique les données affichées lorsque l'utilisateur clic sur le bouton "valider"<br>- Si on a un avenant, le système utilise les données de ce dernier pour générer les nouvelles lignes. |
| Détails | Lorsqu'on clic sur le bouton "Détails", le système affiche un résumé des montants de location et de syndic (Si on les gère dans le même dossier)<br> - Les montants par catégories (Catégorie Loyer, Catégorie Syndic)<br> - Les données actuelles<br> - Les totaux à ce jour<br> - Le Total général |
| Réinitialiser | Lorsqu'on clique sur " Réinitialiser ", le système met à jour les données qui concerne l'article et les sous articles à partir de module stock. |
| Calculer exonération | Le système recalcule les montants de TVA en fonction des périodes d'exonération<br> Remarque: Le système ne calcule pas l’exonération des échéances qui sont facturées. |
| Modifier les retenues | [image : NouvelElement848.bmp]<br>Le système permet de modifier le Loyer HT, Loyer TVA , Syndic HT et Syndic TVA des lignes de l'échéancier sélectionnées si les cases correspondantes sont cochées. |
| Créer/Ouvrir dossier de syndic | En cliquant sur le bouton "Créer/Ouvrir dossier de syndic" le système crée un[dossier de syndic](#page-syn-stock) pour le dossier de location en question, sinon il ouvre le dossier. |
Créé avec HelpNDoc Personal Edition: [Sites web iPhone faciles](https://www.helpndoc.com/fr/tour-des-fonctionnalites/generation-de-site-web-iphone)

### DOSSIERS DE SYNDIC <a id="page-syn-stock"></a>
_Page : `SYN_STOCK.html` — Module Location_

[image : NouvelElement841.bmp]

### Champs

| LIBELLE | U | O | OBSERVATION |
|---|---|---|---|
| Statut |  | X | Sélecteur des [statuts](#page-statuts-rnt) |
| Code |  |  | En lecture seule<br> Auto généré |
| Date |  |  | Par défaut, la date de jour |
| Projet |  |  | En lecture seule,<br> Le système affiche la description de la tranche dont il appartient l’article |
| Superficie |  |  | En lecture seule<br> Superficie d’article en lecture seule |
| Catégorie |  |  | En lecture seule<br> Catégorie d’[article](#page-articles) |
| Type |  |  | En lecture seule<br> Type d’[article](#page-articles) |
| Commercial |  |  | En lecture seule<br> Sélecteur des [employés](#page-personnels) |
| Client |  |  | En lecture seule,<br> Le système affiche la description du locataire |
| Usage |  |  |  |
| Contact |  |  |  |
| Périodicité |  |  | Liste déroulante des périodicités |
| Nombre de période |  |  |  |
| Taux d'augmentation |  |  | Le dernier montant HT de syndic * taux d'augmentation |
| Fréquence d'augmentation (période) |  |  |  |
| Période de grâce (période) |  |  |  |
| Type de reconduction |  |  | On peut avoir deux types de reconduction:<br>- Tacite<br>- Sur demande |
| Date début |  |  | Par défaut date de jour |
| Date fin |  |  | En lecture seule<br> Date début + (nombre de période * périodicité) |
| Avec cumul des augmentations |  |  | Si la case " Avec cumul des augmentations " est coché, le système cumule le montant des augmentations prévus (les augmentation prévu durant la période de grâce) |
| Facturation groupée par client |  |  | Si la case " Facturation groupée par client " est cochée, le système génère une facture qui groupe tous les échéances sélectionnées |
| Montant du syndic basé sur la tarification |  |  | Si la case " Montant du syndic basé sur la tarification " est cochée, le système met à jours le montant de syndic en se basant sur [les tarifications syndic](#page-syn-pricing) |
| Syndic |  |  |  |
| TVA |  |  |  |
| Mensuel |  |  |  |
| HT |  |  |  |
| TTC |  |  | Montant syndic mensuel HT + ( TVA * Montant syndic mensuel HT ) |
| Par période |  |  |  |
| HT |  |  | Montant syndic par période HT = Montant syndic mensuel * nombre de période |
| TTC |  |  | Montant syndic par période HT + ( TVA * Montant syndic par période HT ) |
| Annuel |  |  |  |
| HT |  |  | Montant syndic annuel HT = Montant syndic mensuel HT *12 |
| TTC |  |  | Montant syndic annuel HT + ( TVA * Montant syndic annuel HT) |
| Échéancier |  |  |  |
| Liste d’échéanciers |  |  | Dans cette liste le système affiche<br>- L' échéance<br>- La catégorie<br>- La description<br>- Le montant HT<br>- Le taux TVA<br>- Le montant TTC<br>- Le net à payer<br>- Le montant réglé<br>- Le montant reste<br>- Le nombre de jours : Durée en jour de la période<br>- Le nombre de jours exonérés TVA<br>- Le reste à facturer HT (Montant échéance HT - Montant affecté HT dans la facture - Montant avoir HT)<br>- La notes...<br>Aussi, dans cette liste on peut effectuer les opérations suivantes :<br>- Ajouter<br>- Éditer<br>- Supprimer<br>- Ajouter règlement<br>- Ajouter paiement |
| Reconduction |  |  |  |
| Liste des reconductions |  |  | Dans cette liste, le système visualise<br>- Date début<br>- Date fin<br>- Note<br>Remarque: Si le statut courant de dossier de syndic est paramétré "Est un statut contrat" le système active l'action "Reconduire" sinon, il la garde désactivée. |
| Dates |  |  |  |
| Date signature de contrat |  |  |  |
| Notaire/Avocat |  |  | Sélecteur du[notaire/avocat](#page-fournisseurs-adm) |
| Date signature de contrat par le client |  |  |  |
| Date enregistrement contrat |  |  |  |
| Date première occupation |  |  |  |
| Date fiche assurance |  |  |  |
| Résiliation |  |  |  |
| Date résiliation |  |  |  |
| Montant résiliation |  |  |  |
| Raisons |  |  |  |
| Exonérations TVA |  |  |  |
| Liste des périodes d’exonération |  |  | [image : NouvelElement684.png]<br>La liste visualise les périodes d’exonérations en indiquant:<br>- La date début<br>- La date fin<br>Aussi, dans cette liste on peut effectuer les opérations suivantes:<br>- Ajouter une exonération<br>- Supprimer une exonération<br>- Éditer une exonération<br>Remarque: On ne doit pas avoir un chevauchement dans les périodes d'exonérations TVA ajoutées. |
| Avenants |  |  |  |
| Liste des avenants |  |  | Dans cette liste le système visualise les avenants en indiquent:<br>- La date effective<br>- La date<br>- La validité<br>- La périodicité<br>- Le nombre de période<br>- Le taux d'augmentation<br>- La date prochaine augmentation<br>- La note<br> Aussi, dans cette liste on peut effectuer les opérations suivantes:<br>- Ajouter un avenant<br>- Modifier un avenant<br>- Supprimer un avenant<br>Remarques :<br>- On crée un avenant pour modifier : Date prochaine augmentation, Périodicité, Nombre de période, Taux d'augmentation, Fréquence d'augmentation, Type de reconduction<br>- Par défaut, le système affiche dans les champs "Date" et "Date effective" la date de la dernière échéance + 1 jour.<br>- La suppression n'est permise que pour les avenants non validés.<br>- Le système applique le paramétrage de l'avenant à partir la date effective s'il est validé.<br>- Si la date de la reconduction est supérieure ou égale à la date effective de l'avenant, le système applique les nouvelles clauses (Périodicité, taux d'augmentation...) de l'avenant pour les échéances en question. |
| Notes |  |  |  |
| Notes |  |  |  |
| Règlements |  |  |  |
| Liste des règlements |  |  | Le système affiche la liste des factures en indiquant:<br>- État<br>- Date pièce<br>- Date échéance<br>- Description<br>- Base<br>- RAS<br>- Montant<br>- Référence<br>- Reste à affecter<br>- N° Reçu<br>- Facture<br>- Note |
| Factures |  |  |  |
| Liste des factures |  |  | Le système affiche la liste des factures en indiquant:<br>- Date<br>- Code<br>- Total HT<br>- Total TVA<br>- Total TTC<br>- Montant Régulation<br>- Timbre<br>- Net à payer<br>- Réglé<br>- Reste<br>- Avoirs<br>- Notes |
| Avoirs |  |  |  |
| Liste des avoirs |  |  | Le système affiche la liste des factures en indiquant:<br>- Date<br>- Code<br>- Total HT<br>- Total TVA<br>- Total TTC<br>- Montant Régulation<br>- Timbre<br>- Net à payer<br>- Réglé<br>- Reste<br>- Avoirs<br>- Notes |
| Historique |  |  |  |
| Historique de dossier de location |  |  | La liste visualise l’historique de dossier de location en indiquant :<br>- L’utilisateur<br>- L’état<br>- Le Client<br>- L’article |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0008 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Imprimer | GEN0011 |
| Générer | Le système génère l'échéancier de syndic selon le paramétrage effectué |
| Reconduire | [image : NouvelElement842.bmp]<br>Lorsqu'on clic sur le bouton "reconduire", le système affiche un écran dans lequel il indique:<br>- La date début<br>- La date fin<br>- Les échéances de la période en question.<br>Remarques:<br>- Le système active le bouton "reconduction" lorsque le statut de dossier est paramétré "Est une location effective"<br>- Le système applique les données affichées lorsque l'utilisateur clic sur le bouton "valider"<br>- Si on a un avenant, le système utilise utilise les données de ce dernier pour générer les nouvelles lignes. |
| Détails | Lorsqu'on clic sur le bouton "Détails", le système affiche un résumé des montants de location et de syndic (Si on les gère dans le même dossier ) |
| Réinitialiser | Lorsqu'on clique sur " Réinitialiser ", le système met à jour les données qui concerne l'article et les sous articles à partir de module stock. |
| Calculer exonération | Le système recalcule les montants de TVA en fonction des périodes d'exonération |
Créé avec HelpNDoc Personal Edition: [Éditeur de documentation CHM facile](https://www.helpndoc.com/fr)

### APPEL D'ECHEANCES <a id="page-appeldeloyers"></a>
_Page : `APPELDELOYERS.html` — Module Location_

[image : NouvelElement839.bmp]

### Liste

| LIBELLE | V | OBSERVATIONS |
|---|---|---|
| Article Catégorie |  |  |
| Article Code |  |  |
| Article Description | X |  |
| Catégorie | X |  |
| Client | X |  |
| Code |  |  |
| Date fin |  |  |
| Description |  |  |
| Dossier |  |  |
| Échéance | X |  |
| Entité fonctionnelle |  |  |
| Entité juridique |  |  |
| Facturable |  |  |
| Facturation groupée par client |  |  |
| Facture | X |  |
| Loyer HT |  |  |
| Loyer TTC | X |  |
| Loyer TVA |  |  |
| Loyer TVA Montant |  |  |
| Montant HT |  |  |
| Montant TTC |  |  |
| Nbr. jours |  |  |
| Nbr. jours exonérés |  |  |
| Net à payer | X |  |
| Notes | X |  |
| Période |  |  |
| Périodicité | X |  |
| Proforma | X |  |
| Syndic HT |  |  |
| Syndic TTC | X |  |
| Syndic TVA |  |  |
| Syndic TVA Montant |  |  |
| Tranche |  |  |
| TVA | X |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Générer les factures pro-forma | Le système génère les factures pro-forma pour les lignes sélectionnées.<br>Remarque: Si la case " Facturation groupée par client " est cochée, le système génère une seule facture pro-forma pour les échéances sélectionnées, sinon, il génère une facture pro-forma pour chaque échéance. |
| Générer et imprimer les factures pro-forma | Le système génère et imprime une facture pro-forma pour les lignes sélectionnées.<br>Remarque: Si la case " Facturation groupée par client " est cochée, le système génère et imprime une seule facture pro-forma pour les échéances sélectionnées, sinon, il génère et imprime une facture pro-forma pour chaque échéance. |
| Générer les factures | Le système génère les factures pour les lignes sélectionnées.<br>Remarque: Si la case " Facturation groupée par client " est cochée, le système génère une seule facture pour les échéances sélectionnées, sinon, il génère une facture pour chaque échéance |
| Générer et imprimer les factures | Le système génère et imprime les factures pour les lignes sélectionnées.<br>Remarque: Si la case " Facturation groupée par client " est cochée, le système génère et imprime une seule facture pro-forma pour les échéances sélectionnées, sinon, il génère et imprime les factures pour chaque échéance. |
| Imprimer | GEN0011 |
| Exporter | GEN0008 |
Remarques:
Le système affiche les échéances qui appartiennent à la tranche, et qui sont paramétrées dans la [trésorerie](#page-tresorerie-rnt) de module location "Facturable".
1- Si la case "Afficher seulement les échéances du mois" est cochée, le système n'affiche pas les échéances qui n'ont pas une date dans la période choisie.
2- Si la case "Masquer les lignes facturées" est cochée, le système n'affiche pas les lignes dont les factures sont déjà générées
3- Lorsque l'utilisateur clic sur le bouton "Rafraîchir", le système vérifie si il y a des reconductions prévu; si oui, il affiche le message suivant "Vous avez des dossiers à reconduire. Voulez vous que la reconduction se fasse automatiquement ?":
--> Si on clic oui : Le système reconduit les dossiers en question et il affiche la liste des échéances
--> Si on clic non : Le système ne reconduit pas les dossiers et il n'affiche pas la liste des échéances
Créé avec HelpNDoc Personal Edition: [Outils facile d'utilisation pour créer des aides HTML et des sites web](https://www.helpndoc.com/fr/outils-creation-aide)

### FACTURES PROFORMA <a id="page-facture-proforma-location"></a>
_Page : `FACTURE_PROFORMA_LOCATION.html` — Module Location_

[image : NouvelElement783.png]
Champs

| LIBELLE | U | O | OBSERVATION |
|---|---|---|---|
| Proforma |  |  | En lecture seule, auto généré |
| Code | X |  | En lecture seule, auto généré lorsqu'on appuie sur le bouton "Valider la facture" |
| Date |  |  | Par défaut date de jour |
| Échéance |  |  | Par défaut date de jour |
| Entité juridique |  | X | Liste déroulante des entités juridique |
| Module |  |  |  |
| Tiers |  | X | Sélecteur de tiers |
| Personne à contacter |  |  | Sélecteur des contacts qui sont affectés au tiers choisi |
| Référence interne |  |  |  |
| Note |  |  |  |
| Note 1 |  |  |  |
| Facture |  |  |  |
| Total HT |  |  | ∑ des montants HT des lignes de facture |
| Total TVA |  |  | Total HT * Pourcentage TVA |
| Total TTC |  |  | Total TVA + Total HT |
| Avoir |  |  |  |
| Total HT |  |  | ∑ des montants HT des lignes d'avoir |
| Total TVA |  |  | Total HT * Pourcentage TVA |
| Total TTC |  |  | Total TVA + Total HT |
| Total |  |  |  |
| Total HT |  |  | Total facture HT - Total avoir HT |
| Total TVA |  |  | Total facture TVA - Total avoir TVA |
| Total TTC |  |  | Total facture TTC - Total avoir TVA |
| Timbre fiscal |  |  |  |
| Net à payer |  |  | = ∑ des montants des lignes TTC + Montant timbre fiscal |
| Régler |  |  | = ∑ des montants des paiements affectés à la facture |
| Reste |  |  | = Net à payer - Régler |
| Lignes |  |  |  |
| Liste des lignes de facture |  |  | La liste visualise les lignes d’un devis :<br>- O.D.T<br>- Code<br>- Description<br>- Quantité<br>- Prix unitaire hors taxe<br>- Remise<br>- TVA<br>- P.U. TTC<br>- Total HT<br>- Total TTC<br>- Catégorie<br>Aussi, dans cette lise on peut effectuer les opérations suivantes<br>- Ajouter<br>- Supprimer<br>- Tout sélectionner |
| Avoirs |  |  |  |
| Liste des lignes des avoirs |  |  | La liste visualise les lignes des avoirs en indiquant :<br>- O.D.T<br>- Code<br>- Description<br>- Quantité<br>- Prix unitaire hors taxe<br>- Remise<br>- TVA<br>- P.U. TTC<br>- Total HT<br>- Total TTC<br>- Notes<br>- Document code<br>- Document description<br>- Catégorie |
Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0008 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Imprimer | GEN0011 |
| Valider la facture | Lorsqu'on clic sur le bouton "Valider la facture", le système génère une facture et un code facture. |
Créé avec HelpNDoc Personal Edition: [Nouvelles et informations sur les outils de logiciels de création d'aide](https://www.helpauthoringsoftware.com/fr)

### FACTURES <a id="page-facture-location"></a>
_Page : `FACTURE_LOCATION.html` — Module Location_

[image : NouvelElement759.png]

### Champs

| LIBELLE | U | O | OBSERVATION |
|---|---|---|---|
| Code | X |  | En lecture seule, auto généré |
| Date |  |  | Par défaut date de jour |
| Échéance |  |  | Par défaut date de jour |
| Entité juridique |  | X | Liste déroulante des entités juridique |
| Module |  |  |  |
| Tiers |  | X | Sélecteur de tiers |
| Personne à contacter |  |  | Sélecteur des contacts qui sont affectés au tiers choisi |
| Référence interne |  |  |  |
| Note |  |  |  |
| Note 1 |  |  |  |
| Facture |  |  |  |
| Total HT |  |  | ∑ des montants HT des lignes de facture |
| Total TVA |  |  | Total HT * Pourcentage TVA |
| Total TTC |  |  | Total TVA + Total HT |
| Avoir |  |  |  |
| Total HT |  |  | ∑ des montants HT des lignes d'avoir |
| Total TVA |  |  | Total HT * Pourcentage TVA |
| Total TTC |  |  | Total TVA + Total HT |
| Total |  |  |  |
| Total HT |  |  | Total facture HT - Total avoir HT |
| Total TVA |  |  | Total facture TVA - Total avoir TVA |
| Total TTC |  |  | Total facture TTC - Total avoir TVA |
| Timbre fiscal |  |  |  |
| Net à payer |  |  | = ∑ des montants des lignes TTC + Montant timbre fiscal |
| Régler |  |  | = ∑ des montants des paiements affectés à la facture |
| Reste |  |  | = Net à payer - Régler |
| Lignes |  |  |  |
| Liste des lignes de facture |  |  | La liste visualise les lignes d’un devis :<br>- O.D.T<br>- Code<br>- Description<br>- Quantité<br>- Prix unitaire hors taxe<br>- Remise<br>- TVA<br>- P.U. TTC<br>- Total HT<br>- Total TTC<br>- Catégorie<br>Aussi, dans cette lise on peut effectuer les opérations suivantes<br>- Ajouter<br>- Supprimer<br>- Tout sélectionner |
| Avoirs |  |  |  |
| Liste des lignes des avoirs |  |  | La liste visualise les lignes des avoirs en indiquant :<br>- O.D.T<br>- Code<br>- Description<br>- Quantité<br>- Prix unitaire hors taxe<br>- Remise<br>- TVA<br>- P.U. TTC<br>- Total HT<br>- Total TTC<br>- Notes<br>- Document code<br>- Document description<br>- Catégorie |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0008 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Imprimer | GEN0011 |
| Générer un avoir | Lorsqu'on clic sur le bouton "Générer un avoir", le système crée un [avoir](#page-avoir-location) pour le document en question.<br>Remarque 1: Pour garantir le bon fonctionnement, il est fortement conseillé d'utiliser les avoirs total; c'est à dire un avoir qui annule le montant de la facture en question.<br>Pour procéder à la correction d'une facture il faut :<br>1 - Créer un avoir total (Les quantités dans les lignes égales 1)<br> 2 - Corriger les échéances en question en effectuant les ajustements nécessaires<br> 3 - Régénérer la factures en effectuant à nouveau l'appel d'échéances<br> 4 - Effectuer le règlement de la facture<br>Remarque 2: Si la facture est réglée, il faut:<br>- Refaire les étapes de 1 à 3 cité ci-dessus (dans la remarque 1)<br> - Ouvrir la fiche du règlement<br> - Supprimer l'affectation<br> - Affecter la nouvelle facture en faisant clic droit --> Ajouter --> Double clic sur la facture en question. |
Créé avec HelpNDoc Personal Edition: [Produire des livres EPub gratuitement](https://www.helpndoc.com/fr/creer-des-livres-numeriques-epub)

### AVOIRS <a id="page-avoir-location"></a>
_Page : `AVOIR_LOCATION.html` — Module Location_

[image : NouvelElement767.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Code |  | X |  |
| Date |  |  |  |
| Échéance |  |  |  |
| Entité juridique |  | X |  |
| Module |  |  | En lecture seule,<br> Par défaut "Location" |
| Tiers |  | X |  |
| Personne à contacter |  |  |  |
| Référence interne |  |  |  |
| Note |  |  |  |
| Note2 |  |  |  |
| Total HT |  |  | Σ des montants total HT |
| Total TVA |  |  | Σ des montants TVA |
| Total TTC |  |  | Σ des montant total TTC |
| Timbre fiscal |  |  |  |
| Net à payer |  |  | Total TTC + Timbre fiscal |
| Réglé |  |  |  |
| Reste |  |  | Net à payer - montant réglé |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0008 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Imprimer | GEN0011 |
Créé avec HelpNDoc Personal Edition: [Créer des fichiers d'aide pour la plateforme Qt Help](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creez-des-fichiers-daide-pour-le-framework-qt-help)

### REGLEMENTS <a id="page-reglements-rnt"></a>
_Page : `REGLEMENTS_RNT.html` — Module Location_

[image : NouvelElement714.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Entité |  | X | Liste des entités juridiques |
| Date |  | X |  |
| Échéance |  | X |  |
| Type |  | X |  |
| Référence |  |  |  |
| Banque |  |  |  |
| Pièce remise au siège (date) |  |  |  |
| Pointé (date) |  |  |  |
| Base |  |  |  |
| Montant RS |  |  |  |
| Montant |  |  |  |
| Montant affecté |  |  | En lecture seule,<br> Σ des nets à payer des lignes de l'onglet affectation |
| Reste à affecter |  |  | En lecture seule<br> Montant - Montant affecté |
| Note |  |  |  |
| Compte |  |  | Liste déroulante des [comptes](#page-comptes) |
| Tiers |  | X | Sélecteur de tiers |
| Tiers payeur |  |  | Sélecteur de tiers |
| Tiers autrui |  |  | Sélecteur de tiers |
| Notaire |  |  | Sélecteur de tiers |
| Notes |  |  |  |
| Liste des affectations |  |  | La liste visualise affectations en indiquant :<br>- Le projet<br>- L’article<br>- Le dossier<br>- La date<br>- La description<br>- Le net à payer<br>- Le montant affecté<br>- Aussi, dans cette liste on peut effectuer les opérations suivantes :<br>- Ajouter un échéancier<br>- Supprimer un échéancier<br>- Sélectionner un échéancier<br>Remarque: Lorsqu'on clic sur ajouter, le système affiche la liste des échéances et des factures qui ont des reste à régler du client en question. |
Remarque: Le système affiche les champs; Base, Montant RS si le paramètre "Avec gestion de la retenue à la source" dans les [paramètres système](#page-parametressysteme) est Vrai.

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0008 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Imprimer | GEN0011 |
| Reçu | Le système imprime le reçu de règlement |
Créé avec HelpNDoc Personal Edition: [Générateur facile de livres électroniques et documentation](https://www.helpndoc.com/fr)

### LISTE DES IMPAYEES <a id="page-rnt-impayes"></a>
_Page : `RNT_IMPAYES.html` — Module Location_

[image : NouvelElement775.png]

### Liste

| LIBELLE | V | OBSERVATIONS |
|---|---|---|
| Article | X |  |
| Article code | X |  |
| Catégorie | X |  |
| Client |  |  |
| Code |  |  |
| Crée le |  |  |
| Crée par |  |  |
| Date fin |  |  |
| Description |  |  |
| Dossier de location | X |  |
| Dossier de syndic | X |  |
| Échéance |  |  |
| Échéance HT |  |  |
| Échéance Net à payer |  |  |
| Échéance Réglé |  |  |
| Échéance Reste | X |  |
| Timbre fiscal |  |  |
| Échéance TTC |  |  |
| Échéance TVA |  |  |
| Entité |  |  |
| État | X |  |
| Facture code | X |  |
| Loyer HT |  |  |
| Loyer Ajustement |  |  |
| Loyer contractuel |  |  |
| Loyer Montant TVA |  |  |
| Loyer TTC |  |  |
| Loyer TVA |  |  |
| Modifié le |  |  |
| Modifié par |  |  |
| Module |  |  |
| Nbr. jours |  |  |
| Nbr. jours exonérés TVA |  |  |
| Notes | X |  |
| Période |  |  |
| Périodicité |  |  |
| Périodicité unité |  |  |
| Programme |  |  |
| StatusImage | X |  |
| Syndic HT |  |  |
| Syndic HT Ajustement |  |  |
| Syndic HT contractuel |  |  |
| Syndic Montant TVA |  |  |
| Syndic TTC |  |  |
| Syndic TVA |  |  |
| Tranche | X |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Éditer | GEN0009 |
| Imprimer | GEN0011 |
| Exporter | GEN0008 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014<br> Le système ouvre le dossier de location (ou Dossier de syndic) en question. |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Imprimer | GEN0011 |
| Générer les relances | Lorsqu'on clique sur le bouton "Générer les relances", le système imprime les relances des lignes sélectionnées.<br> Remarque: Le système personnalise la fiche imprimée en fonction du nombre de relance. |
Créé avec HelpNDoc Personal Edition: [Éditeur complet de livres électroniques ePub](https://www.helpndoc.com/fr/creer-des-livres-numeriques-epub)

### TABLEAUX DE BORDS <a id="page-tableauxdebords"></a>
_Page : `TABLEAUXDEBORDS.html` — Module Location_

Créé avec HelpNDoc Personal Edition: [Créer des fichiers d'aide pour la plateforme Qt Help](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creez-des-fichiers-daide-pour-le-framework-qt-help)

#### SOLDES LOCATAIRES <a id="page-soldeslocataires"></a>
_Page : `SOLDESLOCATAIRES.html` — Module Location › TABLEAUX DE BORDS_

[image : NouvelElement844.bmp]
Champs

| LIBELLE | U | O | OBSERVATION |
|---|---|---|---|
| Locataire |  |  | En lecture seule, |
| Montant TTC |  |  | En lecture seule,<br> Σ des montants TTC des échéances |
| Retenues |  |  | En lecture seule, |
| A payer |  |  | En lecture seule,<br> Σ des montants net à payer des échéances |
| Base |  |  | En lecture seule,<br> Σ des montants base des règlements des échéances |
| Solde |  |  | En lecture seule,<br> A payer - Base |
| RAS |  |  | En lecture seule,<br> Σ des montants de retenues à la sources des règlements des échéances |
| Règlement |  |  | En lecture seule,<br> Σ des montants Base - Σ des montants RAS |
| Liste des échéances à ce jour |  |  | La liste affiche la liste des échéances à ce jour en indiquant:<br>- Échéance<br>- Montant TTC<br>- Retenues<br>- A payer<br>- Base<br>- RAS<br>- Règlement<br>- Description<br>Remarque: Lorsqu'on fait un double clic sur une ligne, le système ouvre la fiche correspondante (Soit le dossier de location /syndic, soit la facture, soit le règlement ) |
Créé avec HelpNDoc Personal Edition: [Générateur d'aides Web gratuit](https://www.helpndoc.com/fr)

#### SUIVI DES DOSSIERS DE LOCATIONS <a id="page-suividesdossiersdelocations"></a>
_Page : `SUIVIDESDOSSIERSDELOCATIONS.html` — Module Location › TABLEAUX DE BORDS_

[image : NouvelElement840.bmp]
Remarque: Le système affiche la liste des dossiers de location (aussi les dossiers qui sont résiliés)

### Champs

| LIBELLE | U | O | OBSERVATION |
|---|---|---|---|
| Statut |  | X | Sélecteur des [statuts](#page-statuts-rnt) |
| Code |  |  | En lecture seule<br> Auto généré |
| Date |  |  | Par défaut, la date de jour |
| Projet |  |  | En lecture seule,<br> Le système affiche la description de la tranche dont il appartient l’article |
| Superficie |  |  | En lecture seule,<br> Superficie d’article |
| Catégorie |  |  | En lecture seule<br> Catégorie d’[article](#page-articles) |
| Type |  |  | En lecture seule<br> Type d’[article](#page-articles) |
| Commercial |  |  | En lecture seule<br> Sélecteur des [employés](#page-personnels) |
| Client |  |  | En lecture seule,<br> Le système affiche la description du locataire |
| Usage |  |  |  |
| Contact |  |  |  |
| Date début |  |  | Par défaut date de jour |
| Périodicité |  |  | Liste déroulante des périodicités |
| Nombre de période |  |  |  |
| Date reconduction |  |  | En lecture seule<br> Date début + (nombre de période * périodicité) |
| Type de reconduction |  |  | On peut avoir deux types de reconduction:<br>- Tacite<br>- Sur demande |
| Taux d'augmentation |  |  | Le dernier loyer HT * taux d'augmentation |
| Fréquence d'augmentation (mois) |  |  |  |
| Date prochaine augmentation |  |  | Le système appliquera l'augmentation à partir de la date mentionnée dans ce champ |
| Facturation groupée par client |  |  | Si la case " Facturation groupée par client " est cochée, le système génère une facture qui groupe tous les échéances sélectionnées |
| Loyer et syndic groupés dans la même ligne |  |  | Si la case " Loyer et syndic groupés dans la même ligne " est cochée, le système groupe le loyer et le syndic dans la même échéance, sinon il affiche chacun dans une échéance. |
| Montant du syndic basé sur la tarification |  |  | Si la case " Montant du syndic basé sur la tarification " est cochée, le système met à jour le montant de syndic à partir de l'écran "[Tarification du syndic](#page-syn-pricing)". |
| Loyer TVA |  |  |  |
| Loyer |  |  |  |
| Mensuel |  |  |  |
| HT |  |  |  |
| TTC |  |  | Montant loyer mensuel HT + ( TVA * Montant loyer mensuel HT ) |
| Par période |  |  |  |
| HT |  |  | Montant loyer par période HT = Montant loyer mensuel * nombre de période |
| TTC |  |  | Montant loyer par période HT + ( TVA * Montant loyer par période HT ) |
| Annuel |  |  |  |
| HT |  |  | Montant loyer annuel HT = Montant loyer mensuel HT *12 |
| TTC |  |  | Montant loyer annuel HT + ( TVA * Montant loyer annuel HT) |
| Caution |  |  |  |
| Nbr. mois |  |  |  |
| HT |  |  | Loyer mensuel HT * nombre de mois |
| TVA |  |  |  |
| TTC |  |  | Caution HT + (TVA * Caution HT) |
| Syndic TVA |  |  |  |
| Syndic |  |  |  |
| Mensuel |  |  |  |
| HT |  |  |  |
| TTC |  |  | Montant syndic mensuel HT + ( TVA * Montant syndic mensuel HT ) |
| Par période |  |  |  |
| HT |  |  | Montant syndic par période HT = Montant syndic mensuel * nombre de période |
| TTC |  |  | Montant syndic par période HT + ( TVA * Montant syndic par période HT ) |
| Annuel |  |  |  |
| HT |  |  | Montant syndic annuel HT = Montant syndic mensuel HT *12 |
| TTC |  |  | Montant syndic annuel HT + ( TVA * Montant syndic annuel HT) |
| Échéance |  |  |  |
| Mensuel |  |  |  |
| HT |  |  | Montant loyer mensuel HT + Montant syndic mensuel HT |
| TTC |  |  | Montant loyer mensuel TTC + Montant syndic mensuel TTC |
| Par période |  |  |  |
| HT |  |  | Montant loyer par période HT + Montant syndic par période HT |
| TTC |  |  | Montant loyer par période TTC + Montant syndic par période TTC |
| Annuel |  |  |  |
| HT |  |  | Montant loyer annuel HT + Montant syndic annuel HT |
| TTC |  |  | Montant loyer annuel TTC + Montant syndic annuel TTC |
| Échéancier |  |  |  |
| Liste d’échéanciers |  |  | Dans cette liste le système affiche<br>- L' échéance<br>- La catégorie<br>- La description<br>- Le montant HT<br>- Le taux TVA<br>- Le montant TTC<br>- Le net à payer<br>- Le montant réglé<br>- Le montant reste<br>- Le nombre de jours : Durée en jour de la période<br>- Le nombre de jours exonérés TVA:<br>- Le reste à facturer HT<br>- La notes...<br>Aussi, dans cette liste on peut effectuer les opérations suivantes :<br>- Ajouter<br>- Éditer<br>- Supprimer<br>- Ajouter règlement<br>- Ajouter paiement |
| Sous articles |  |  |  |
| Liste des sous articles |  |  | Dans cette liste, le système affiche les détails des sous articles en indiquant:<br>- Le code<br>- La description<br>- Affecté par défaut<br>- Type<br>- Catégorie<br>- Notes<br>Aussi, dans cette liste on peut effectuer les opérations suivantes<br>- Ajouter un sous article<br>- Supprimer un sous article |
|  |  |  |  |
| Reconduction |  |  |  |
| Liste des reconductions |  |  | Dans cette liste, le système visualise<br>- Date début<br>- Date fin<br>- Note<br>Aussi, à partir de cette liste on peut supprimer une reconduction. |
| Dates |  |  |  |
| Date signature de contrat |  |  |  |
| Notaire/Avocat |  |  | Sélecteur du[notaire/avocat](#page-fournisseurs-adm) |
| Date signature de contrat par le client |  |  |  |
| Date enregistrement contrat |  |  |  |
| Date première occupation |  |  |  |
| Date fiche assurance |  |  |  |
| Résiliation |  |  |  |
| Date résiliation |  |  |  |
| Montant résiliation |  |  |  |
| Raisons |  |  |  |
| Exonérations TVA |  |  |  |
| Liste des périodes d’exonération |  |  | [image : NouvelElement684.png]<br>La liste visualise les périodes d’exonérations en indiquant:<br>- La date début<br>- La date fin<br>Aussi, dans cette liste on peut effectuer les opérations suivantes:<br>- Ajouter une exonération<br>- Supprimer une exonération<br>- Éditer une exonération<br>Remarque: On ne doit pas avoir un chevauchement dans les périodes d'exonérations TVA ajoutées. |
| Avenants |  |  |  |
| Liste des avenants |  |  | Dans cette liste le système visualise les avenants en indiquent:<br>- La date effective<br>- La date<br>- La validité<br>- La périodicité<br>- Le nombre de période<br>- Le taux d'augmentation<br>- La date prochaine augmentation<br>- La note<br>Aussi, dans cette liste on peut effectuer les opérations suivantes:<br>- Ajouter un avenant<br>- Éditer un avenant<br>- Supprimer un avenant<br>Remarques :<br>- On crée un avenant pour modifier : Date prochaine augmentation, Périodicité, Nombre de période, Taux d'augmentation, Fréquence d'augmentation, Type de reconduction<br>- Par défaut, le système affiche dans les champs "Date" et "Date effective" la date de la dernière échéance + 1 jour.<br>- La suppression n'est permise que pour les avenants non validés.<br>- Le système applique le paramétrage de l'avenant à partir la date effective s'il est validé.<br>- Si la date de la reconduction est supérieure ou égale à la date effective de l'avenant, le système applique les nouvelles clauses (Périodicité, taux d'augmentation...) de l'avenant pour les échéances en question. |
| Notes |  |  |  |
| Notes |  |  |  |
| Règlements |  |  |  |
| Liste des règlements |  |  | Le système affiche la liste des factures en indiquant:<br>- État<br>- Date pièce<br>- Date échéance<br>- Description<br>- Base<br>- RAS<br>- Montant<br>- Référence<br>- Reste à affecter<br>- N° Reçu<br>- Facture<br>- Note |
| Factures |  |  |  |
| Liste des factures |  |  | Le système affiche la liste des factures en indiquant:<br>- Date<br>- Code<br>- Total HT<br>- Total TVA<br>- Total TTC<br>- Montant Régulation<br>- Timbre<br>- Net à payer<br>- Réglé<br>- Reste<br>- Avoirs<br>- Notes |
| Avoirs |  |  |  |
| Liste des avoirs |  |  | Le système affiche la liste des factures en indiquant:<br>- Date<br>- Code<br>- Total HT<br>- Total TVA<br>- Total TTC<br>- Montant Régulation<br>- Timbre<br>- Net à payer<br>- Réglé<br>- Reste<br>- Notes |
| Historique |  |  |  |
| Historique de dossier de location |  |  | La liste visualise l’historique de dossier de location en indiquant :<br>- L’utilisateur<br>- L’état<br>- Le Client<br>- L’article |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Éditer | GEN0009 |
| Imprimer | GEN0011 |
| Exporter | GEN0008 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Imprimer | GEN0011 |
| Générer | Le système génère l'échéancier de location selon le paramétrage effectué<br> Remarque: Le système désactive le bouton " Générer " lorsqu'on ajoute une reconduction. |
| Reconduire | [image : NouvelElement779.png]<br>Lorsqu'on clic sur le bouton "reconduire", le système affiche un écran dans lequel il indique:<br>- La date début<br>- La date fin<br>- Les nouvelles échéances.<br>Remarques:<br>- Le système active le bouton "reconduction" lorsque le statut de dossier est paramétré "Est une location effective"<br>- Le système applique les données affichées lorsque l'utilisateur clic sur le bouton "valider"<br>- Si on a un avenant, le système utilise les données de ce dernier pour générer les nouvelles lignes. |
| Détails | Lorsqu'on clic sur le bouton "Détails", le système affiche un résumé des montants de location et de syndic (Si on les gère dans le même dossier)<br> - Les montants par catégories<br> - Les données actuelles<br> - Les totaux à ce jour<br> - Le Total général |
| Réinitialiser | Lorsqu'on clique sur " Réinitialiser ", le système met à jour les données qui concerne l'article et les sous articles à partir de module stock. |
| Calculer exonération | Le système recalcule les montants de TVA en fonction des périodes d'exonération<br> Remarque: Le système ne calcule pas l’exonération des échéances qui sont facturées. |
| Modifier les retenues | [image : NouvelElement793.png]<br>Le système permet de modifier les taux de TVA et le montant du timbre fiscal des lignes de l'échéancier sélectionnées si les cases correspondantes sont cochées. |
| Créer/Ouvrir dossier de syndic | En cliquant sur le bouton "Créer/Ouvrir dossier de syndic" le système crée un[dossier de syndic](#page-syn-stock) pour le dossier de location en question. |
Créé avec HelpNDoc Personal Edition: [Écrire des livres électronique Kindle](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creer-ebooks-pour-kindle-amazon)

#### SUIVI DES DOSSIERS DE SYNDIC <a id="page-suividesdossiersdesyndic"></a>
_Page : `SUIVIDESDOSSIERSDESYNDIC.html` — Module Location › TABLEAUX DE BORDS_

[image : NouvelElement841.bmp]
Remarque: Le système affiche la liste des dossiers de syndic (aussi les dossiers qui sont résiliés)

### Champs

| LIBELLE | U | O | OBSERVATION |
|---|---|---|---|
| Statut |  | X | Sélecteur des [statuts](#page-statuts-rnt) |
| Code |  |  | En lecture seule<br> Auto généré |
| Date |  |  | Par défaut, la date de jour |
| Projet |  |  | En lecture seule,<br> Le système affiche la description de la tranche dont il appartient l’article |
| Superficie |  |  | En lecture seule<br> Superficie d’article en lecture seule |
| Catégorie |  |  | En lecture seule<br> Catégorie d’[article](#page-articles) |
| Type |  |  | En lecture seule<br> Type d’[article](#page-articles) |
| Commercial |  |  | En lecture seule<br> Sélecteur des [employés](#page-personnels) |
| Client |  |  | En lecture seule,<br> Le système affiche la description du locataire |
| Usage |  |  |  |
| Contact |  |  |  |
| Périodicité |  |  | Liste déroulante des périodicités |
| Nombre de période |  |  |  |
| Taux d'augmentation |  |  | Le dernier montant HT de syndic * taux d'augmentation |
| Fréquence d'augmentation (période) |  |  |  |
| Période de grâce (période) |  |  |  |
| Type de reconduction |  |  | On peut avoir deux types de reconduction:<br>- Tacite<br>- Sur demande |
| Date début |  |  | Par défaut date de jour |
| Date fin |  |  | En lecture seule<br> Date début + (nombre de période * périodicité) |
| Avec cumul des augmentations |  |  | Si la case " Avec cumul des augmentations " est coché, le système cumule le montant des augmentations prévus (les augmentation prévu durant la période de grâce) |
| Facturation groupée par client |  |  | Si la case " Facturation groupée par client " est cochée, le système génère une facture qui groupe tous les échéances sélectionnées |
| Montant du syndic basé sur la tarification |  |  | Si la case " Montant du syndic basé sur la tarification " est cochée, le système met à jours le montant de syndic en se basant sur [les tarifications syndic](#page-syn-pricing) |
| Syndic |  |  |  |
| TVA |  |  |  |
| Mensuel |  |  |  |
| HT |  |  |  |
| TTC |  |  | Montant syndic mensuel HT + ( TVA * Montant syndic mensuel HT ) |
| Par période |  |  |  |
| HT |  |  | Montant syndic par période HT = Montant syndic mensuel * nombre de période |
| TTC |  |  | Montant syndic par période HT + ( TVA * Montant syndic par période HT ) |
| Annuel |  |  |  |
| HT |  |  | Montant syndic annuel HT = Montant syndic mensuel HT *12 |
| TTC |  |  | Montant syndic annuel HT + ( TVA * Montant syndic annuel HT) |
| Échéancier |  |  |  |
| Liste d’échéanciers |  |  | Dans cette liste le système affiche<br>- L' échéance<br>- La catégorie<br>- La description<br>- Le montant HT<br>- Le taux TVA<br>- Le montant TTC<br>- Le net à payer<br>- Le montant réglé<br>- Le montant reste<br>- Le nombre de jours : Durée en jour de la période<br>- Le nombre de jours exonérés TVA:<br>- Le reste à facturer HT<br>- La notes...<br>Aussi, dans cette liste on peut effectuer les opérations suivantes :<br>- Ajouter<br>- Éditer<br>- Supprimer<br>- Ajouter règlement<br>- Ajouter paiement |
| Reconduction |  |  |  |
| Liste des reconductions |  |  | Dans cette liste, le système visualise<br>- Date début<br>- Date fin<br>- Note<br>Remarque: Si le statut courant de dossier de syndic est paramétré "Est un statut contrat" le système active l'action "Reconduire" sinon, il la garde désactivée. |
| Dates |  |  |  |
| Date signature de contrat |  |  |  |
| Notaire/Avocat |  |  | Sélecteur du[notaire/avocat](#page-fournisseurs-adm) |
| Date signature de contrat par le client |  |  |  |
| Date enregistrement contrat |  |  |  |
| Date première occupation |  |  |  |
| Date fiche assurance |  |  |  |
| Résiliation |  |  |  |
| Date résiliation |  |  |  |
| Montant résiliation |  |  |  |
| Raisons |  |  |  |
| Exonérations TVA |  |  |  |
| Liste des périodes d’exonération |  |  | [image : NouvelElement684.png]<br>La liste visualise les périodes d’exonérations en indiquant:<br>- La date début<br>- La date fin<br>Aussi, dans cette liste on peut effectuer les opérations suivantes:<br>- Ajouter une exonération<br>- Supprimer une exonération<br>- Éditer une exonération<br>Remarque: On ne doit pas avoir un chevauchement dans les périodes d'exonérations TVA ajoutées. |
| Avenants |  |  |  |
| Liste des avenants |  |  | Dans cette liste le système visualise les avenants en indiquent:<br>- La date effective<br>- La date<br>- La validité<br>- La périodicité<br>- Le nombre de période<br>- Le taux d'augmentation<br>- La date prochaine augmentation<br>- La note<br> Aussi, dans cette liste on peut effectuer les opérations suivantes:<br>- Ajouter un avenant<br>- Modifier un avenant<br>- Supprimer un avenant<br>Remarques :<br>- On crée un avenant pour modifier : Date prochaine augmentation, Périodicité, Nombre de période, Taux d'augmentation, Fréquence d'augmentation, Type de reconduction<br>- Par défaut, le système affiche dans les champs "Date" et "Date effective" la date de la dernière échéance + 1 jour.<br>- La suppression n'est permise que pour les avenants non validés.<br>- Le système applique le paramétrage de l'avenant à partir la date effective s'il est validé.<br>- Si la date de la reconduction est supérieure ou égale à la date effective de l'avenant, le système applique les nouvelles clauses (Périodicité, taux d'augmentation...) de l'avenant pour les échéances en question. |
| Notes |  |  |  |
| Notes |  |  |  |
| Règlements |  |  |  |
| Liste des règlements |  |  | Le système affiche la liste des factures en indiquant:<br>- État<br>- Date pièce<br>- Date échéance<br>- Description<br>- Base<br>- RAS<br>- Montant<br>- Référence<br>- Reste à affecter<br>- N° Reçu<br>- Facture<br>- Note |
| Factures |  |  |  |
| Liste des factures |  |  | Le système affiche la liste des factures en indiquant:<br>- Date<br>- Code<br>- Total HT<br>- Total TVA<br>- Total TTC<br>- Montant Régulation<br>- Timbre<br>- Net à payer<br>- Réglé<br>- Reste<br>- Avoirs<br>- Notes |
| Avoirs |  |  |  |
| Liste des avoirs |  |  | Le système affiche la liste des factures en indiquant:<br>- Date<br>- Code<br>- Total HT<br>- Total TVA<br>- Total TTC<br>- Montant Régulation<br>- Timbre<br>- Net à payer<br>- Réglé<br>- Reste<br>- Avoirs<br>- Notes |
| Historique |  |  |  |
| Historique de dossier de location |  |  | La liste visualise l’historique de dossier de location en indiquant :<br>- L’utilisateur<br>- L’état<br>- Le Client<br>- L’article |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0008 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Imprimer | GEN0011 |
| Générer | Le système génère l'échéancier de syndic selon le paramétrage effectué |
| Reconduire | [image : NouvelElement842.bmp]<br>Lorsqu'on clic sur le bouton "reconduire", le système affiche un écran dans lequel il indique:<br>- La date début<br>- La date fin<br>- Les échéances de la période en question.<br>Remarques:<br>- Le système active le bouton "reconduction" lorsque le statut de dossier est paramétré "Est une location effective"<br>- Le système applique les données affichées lorsque l'utilisateur clic sur le bouton "valider"<br>- Si on a un avenant, le système utilise les données de ce dernier pour générer les nouvelles lignes. |
| Détails | Lorsqu'on clic sur le bouton "Détails", le système affiche un résumé des montants de location et de syndic (Si on les gère dans le même dossier ) |
| Réinitialiser | Lorsqu'on clique sur " Réinitialiser ", le système met à jour les données qui concerne l'article et les sous articles à partir de module stock. |
| Calculer exonération | Le système recalcule les montants de TVA en fonction des périodes d'exonération |
Créé avec HelpNDoc Personal Edition: [Créer des aides HTML, DOC, PDF et des manuels depuis une même source](https://www.helpndoc.com/fr/outils-creation-aide)

#### RAPPORTS <a id="page-rapports"></a>
_Page : `RAPPORTS.html` — Module Location › TABLEAUX DE BORDS_

Créé avec HelpNDoc Personal Edition: [Créer de la documentation iPhone facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites/generation-de-site-web-iphone)

#### SUIVI DES AFFECTATIONS <a id="page-suividesaffectations1"></a>
_Page : `SUIVIDESAFFECTATIONS1.html` — Module Location › TABLEAUX DE BORDS_

[image : NouvelElement843.bmp]
La liste visualise les affectations effectués en fonction de l’[entité juridique](#page-entitesjuridique), le type de document, la / les[types de pièce](#page-typespiece) sélectionnées , le [statut](#page-statuts-com) de dossier et la période.
Remarque: A partir de cette liste, on peut accéder au dossier de location (ou bien dossier de syndic) qui contient le règlement sélectionné.

### Champs

| LIBELLE | V | OBSERVATIONS |
|---|---|---|
| Article | X |  |
| Banque | X |  |
| Catégorie | X |  |
| Compte |  |  |
| Crédit/Débit |  |  |
| Crée le | X |  |
| Crée par | X |  |
| Date | X |  |
| Date document |  |  |
| Date échéance pièce de règlement |  |  |
| Date opération pièce de règlement |  |  |
| Date pièce de règlement |  |  |
| Entité | X |  |
| Est un Caution |  |  |
| Est un Syndic |  |  |
| Est une Location |  |  |
| État pièce de règlement |  |  |
| Modifié le | X |  |
| Modifié par | X |  |
| Montant affecté | X |  |
| Montant pièce de règlement |  |  |
| Notes document |  |  |
| Notes pièce de règlement | X |  |
| Pièce de règlement | X |  |
| Programme | X |  |
| Réf. pièce de règlement |  |  |
| Réf. reçu |  |  |
| Tiers | X |  |
| Tranche | X |  |
| Type pièce de règlement |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Éditer | GEN0009 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
Créé avec HelpNDoc Personal Edition: [Créer des fichiers d'aide pour la plateforme Qt Help](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creez-des-fichiers-daide-pour-le-framework-qt-help)

### CONFIGURATION <a id="page-configuration"></a>
_Page : `CONFIGURATION.html` — Module Location_

Créé avec HelpNDoc Personal Edition: [Création d'aide CHM, PDF, DOC et HTML d'une même source](https://www.helpndoc.com/fr/outils-creation-aide)

#### PROSPECTIONS <a id="page-prospections1"></a>
_Page : `PROSPECTIONS1.html` — Module Location › CONFIGURATION_

Créé avec HelpNDoc Personal Edition: [Générateur d'aides CHM gratuit](https://www.helpndoc.com/fr)

##### NATURES <a id="page-rnt-naturesprospections"></a>
_Page : `RNT_NATURESPROSPECTIONS.html` — Module Location › CONFIGURATION › PROSPECTIONS_

[image : NouvelElement669.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Code | X | X |  |
| Description |  | X |  |
| Projet obligatoire |  |  | Pour une nature de prospection, si la case « Projet obligatoire » est cochée, le choix d’un projet dans la fiche [prospection](#page-rnt-prospections) devient obligatoire sinon, il devient optionnel. |
| Délai d’alerte |  |  |  |
| Résultat |  |  | Liste déroulante des résultats |
| Remarque 1: Le système affecte le résultat choisi au niveau de la configuration des natures au résultat d'action et il crée automatiquement une alerte (Si délai d'alerte >0) si cette nature est affectée à la prospection.<br> Remarque 2 : Si la durée paramétré >= Délai d’inactivité ; le système génère une alerte (Date dernière action + Délai d’alerte)<br> Remarque 3 : Si l’utilisateur modifie la nature (nature avec alerte) de la prospection, le système génère une alerte (Date dernière action + Délai d’alerte) |  |  |  |
| GEN0015, GEN0016 |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 pour la ligne sélectionnée |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichier attachés | GEN0004 |
| Note | GEN0005 |
| Annuler et fermer | GEN0017 |
| Supprimer | GEN0010 pour la ligne sélectionnée |
| Champs spécifiques | GEN0007 pour la ligne sélectionnée |
Créé avec HelpNDoc Personal Edition: [Outils facile d'utilisation pour créer des aides HTML et des sites web](https://www.helpndoc.com/fr/outils-creation-aide)

##### SOURCES DE CONTACT <a id="page-rnt-sourcedecontact"></a>
_Page : `RNT_SOURCEDECONTACT.html` — Module Location › CONFIGURATION › PROSPECTIONS_

[image : NouvelElement672.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Détails |  |  |  |
| O.D.T |  | X |  |
| Code | X | X |  |
| Parrain obligatoire |  |  | Si on choisi dans la [prospection](#page-rnt-prospections) un source de contact paramétré avec "Parrain obligatoire", le système vérifie si le champ "Parrain" est renseigné ou non<br>- Si le champ est vide: le système affiche un message " La valeur du champ [Parrain] est obligatoire !! "<br>- Sinon le système enregistre la prospection. |
| Avec cadeau |  |  | Si on choisi dans la [prospection](#page-rnt-prospections) un source de contact paramétré" Avec cadeau", le système vérifie si la tranche est paramétrée "Avec octroi d'un cadeau pour une source de contact":<br> Si oui:<br>- Le système à la création du dossier de vente créé une ligne de cadeau avec le raison source de contact<br> Sinon:<br>- Le système ne créé pas une ligne de cadeau |
| Valeurs par défaut |  |  |  |
| Source de contact |  |  | Liste déroulante des sources de contacts.<br> A la création d’une « [Prospections](#page-rnt-prospections) », la source de contact choisi sera affectée par défaut dans la fiche. |
| GEN0015, GEN0016 |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Enregistrer | GEN0001 pour la ligne sélectionnée |
| Supprimer | GEN0010 pour la ligne sélectionnée |
Créé avec HelpNDoc Personal Edition: [Qu'est-ce qu'un outil de création d'aide ?](https://www.helpauthoringsoftware.com/fr)

##### SUPPORTS SOURCE DE CONTACT <a id="page-rnt-supportssourcedecontact"></a>
_Page : `RNT_SUPPORTSSOURCEDECONTACT.html` — Module Location › CONFIGURATION › PROSPECTIONS_

[image : NouvelElement674.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Code | X | X |  |
| Description |  | X |  |
| Source de contact |  |  | Liste déroulante des [sources de contactes](#page-rnt-sourcedecontact) |
| GEN0015, GEN0016 |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 pour la ligne sélectionnée |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichier attachés | GEN0004 |
| Note | GEN0005 |
| Annuler et fermer | GEN0017 |
| Supprimer | GEN0010 pour la ligne sélectionnée |
| Champs spécifiques | GEN0007 pour la ligne sélectionnée |
Créé avec HelpNDoc Personal Edition: [Générateur complet de livres électroniques ePub](https://www.helpndoc.com/fr/creer-des-livres-numeriques-epub)

##### ACTIONS COMMERCIALES <a id="page-rnt-actionscommerciales"></a>
_Page : `RNT_ACTIONSCOMMERCIALES.html` — Module Location › CONFIGURATION › PROSPECTIONS_

[image : NouvelElement675.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Code | X | X |  |
| Description |  | X |  |
| Date début |  |  |  |
| Date fin |  |  |  |
| Couleur |  |  |  |
| Notes |  |  |  |
| Actif (ve) |  |  | En cochant le champ « actif (ve) », le système affiche l'action dans la liste déroulante des actions commerciales du masque « [Prospection](#page-rnt-prospections)» |
|  |  |  |  |
| GEN0015, GEN0016 |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 pour la ligne sélectionnée |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichier attachés | GEN0004 |
| Note | GEN0005 |
| Annuler et fermer | GEN0017 |
| Supprimer | GEN0010 pour la ligne sélectionnée |
| Champs spécifiques | GEN0007 pour la ligne sélectionnée |
Créé avec HelpNDoc Personal Edition: [Outil de création d'aide complet](https://www.helpndoc.com/fr/outils-creation-aide)

#### LOCATION <a id="page-location"></a>
_Page : `LOCATION.html` — Module Location › CONFIGURATION_

Créé avec HelpNDoc Personal Edition: [Générateur complet de livres électroniques Kindle](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creer-ebooks-pour-kindle-amazon)

##### STATUTS <a id="page-statuts-rnt"></a>
_Page : `STATUTS_RNT.html` — Module Location › CONFIGURATION › LOCATION_

[image : NouvelElement726.png]

### Champs

| LIBELLE | U | O | OBSERVATION |
|---|---|---|---|
| O.D.T |  | X |  |
| Code | X | X |  |
| Description |  | X |  |
| Type |  | X | Liste déroulante des types d’un statut, on peut choisir un de ces valeurs :<br>- Nouveau<br>- Intermédiaire<br>- Terminé avec succès<br>- Terminé avec échec |
| Durée de vie |  |  |  |
| Couleur |  |  |  |
| Est une location effective |  |  | Case à cocher pour indiquer si la location est effective |
| Est une annulation |  |  | Est un statut d'annulation |
| Est une résiliation |  |  | Est un statut de résiliation |
| Peut effectuer une prospection |  |  | Le système affiche dans le sélecteur des articles dans la fiche [prospection](#page-prospections) les articles qui ont des dossiers de vente dont le statut est paramétré " Peut effectuer une prospection ". |
| Image |  |  | Icône associé pour un statut |
|  |  |  |  |
| Champs obligatoires |  |  |  |
| [image : NouvelElement629.png] |  |  |  |
| Date envoie au notaire/ avocat |  |  |  |
| Date minute/ Enregistrement du contrat |  |  |  |
| Date effective |  |  |  |
| Date assurance |  |  |  |
| Notaire/ Avocat |  |  |  |
| Identifiant du client |  |  |  |
| Champs obligatoires des tiers |  |  |  |
| [image : NouvelElement630.png] |  |  |  |
| Adresse |  |  | La liste des champs obligatoires des tiers qui correspondent à un statut |
| Code postal |  |  |  |
| Téléphone indicatif |  |  |  |
| Téléphone |  |  |  |
| Téléphone 1 indicatif |  |  |  |
| Téléphone 1 |  |  |  |
| Téléphone 2 indicatif |  |  |  |
| Téléphone 2 |  |  |  |
| Fax indicatif |  |  |  |
| Fax |  |  |  |
| Email |  |  |  |
| Site web |  |  |  |
| Agence |  |  |  |
| RIB |  |  |  |
| IBAN |  |  |  |
| Registre de commerce |  |  |  |
| Code en douane |  |  |  |
| Fils de |  |  |  |
| Et de |  |  |  |
| Employeur |  |  |  |
| Ancienneté |  |  |  |
| Date de naissance |  |  |  |
| Lieu de naissance |  |  |  |
| Nbr enfants |  |  |  |
| Capital |  |  |  |
| Qualité |  |  |  |
| Pièce d’identité Type |  |  |  |
| Pièce d’identité N° |  |  |  |
| Pièce d’identité date |  |  |  |
| Pièce d’identité Lieu |  |  |  |
| Pièce d’identité valide jusqu’au |  |  |  |
| Pièce lieu renouvellement |  |  |  |
| Comptabilité Code |  |  |  |
| Comptabilité Compte collectif |  |  |  |
| Banque |  |  |  |
| Personnel |  |  |  |
| Contact |  |  |  |
| Assujettissement |  |  |  |
| Situation familiale |  |  |  |
| Résidence |  |  |  |
| Régime matrimonial |  |  |  |
| Nature |  |  |  |
| Pays |  |  |  |
| Nationalité |  |  |  |
| Profession |  |  |  |
| Salaire |  |  |  |
| Ville |  |  |  |
| Documents à joindre |  |  |  |
| [image : NouvelElement631.png] |  |  |  |
| Liste des documents à joindre |  |  | La liste visualise les types de fichiers attachés<br> Aussi, dans cette liste on peut effectuer les opérations suivantes :<br>- Ajouter un document<br>- Supprimer un document |
| Valeurs par défaut |  |  |  |
| [image : NouvelElement632.png] |  |  |  |
| Réservation |  |  | Listes déroulantes d’affectation des valeurs par défaut pour les statuts qui peut prendre un dossier de location. |
| Concrétisation |  |  |  |
| Désistement |  |  |  |
| Annulation |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Enregistrer | GEN0001 pour la ligne sélectionnée |
| Supprimer | GEN0010 pour la ligne sélectionnée |
Créé avec HelpNDoc Personal Edition: [Générateur de documentation iPhone gratuit](https://www.helpndoc.com/fr/tour-des-fonctionnalites/generation-de-site-web-iphone)

##### TRANSITIONS <a id="page-transitions-rnt"></a>
_Page : `TRANSITIONS_RNT.html` — Module Location › CONFIGURATION › LOCATION_

[image : NouvelElement643.png]

### Champs

| LIBELLE | U | O | OBSERVATION |
|---|---|---|---|
| O.D.T |  | X |  |
| De |  | X |  |
| à |  | X |  |
| Liste des rôles crées |  |  | les cases à cocher représentent les rôles des acteurs qui peuvent modifier le statut d’un dossier de location |
| Liste des champs obligatoires |  |  | Liste de choix des champs obligatoire à renseigner dans la fiche dossier de location |
| Remarque 1: Le statut de départ et le statut d'arrivé doivent être différents<br> Remarque 2: On ne peut pas créer deux transitions avec la même source et la même destination |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0008 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Imprimer | GEN0011 |
Créé avec HelpNDoc Personal Edition: [Créer des fichiers d'aide pour la plateforme Qt Help](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creez-des-fichiers-daide-pour-le-framework-qt-help)

##### TRESORERIE <a id="page-tresorerie-rnt"></a>
_Page : `TRESORERIE_RNT.html` — Module Location › CONFIGURATION › LOCATION_

[image : NouvelElement682.png]

### Champs

| LIBELLE | U | O | OBSERVATION |
|---|---|---|---|
| Trésorerie |  | X |  |
| Code | X | X |  |
| Description |  | X |  |
| TVA |  |  | Par défaut, prend la valeur paramétrée dans les paramètres système. |
| Crédit/Débit |  |  | Si la case est coché, le document créé est de type crédit sinon, de type débit. |
| R.S (Retenue à la source) |  |  | Ce taux sera affecté par défaut pour le documents qui a un pourcentage de retenue à la source > 0% du dossier de location. |
| Caution |  |  | Des cases à cocher pour indiquer le type de document (Caution ou Frais ou Syndic ou Loyer ou Résiliation). |
| Frais |  |  |  |
| Syndic |  |  |  |
| Loyer |  |  |  |
| Résiliation |  |  |  |
| Facturable |  |  | A l'appel des loyers, le système affiche les documents qui sont paramétrés "Facturable". |
| Avec timbre fiscal |  |  | Si cette case est cochée, le système, à la génération de l’échéancier de location, affecte le montant du timbre fiscal (Montant à partir de la configuration des paramètres système ) au documents correspondants. |
| Valeurs par défaut |  |  |  |
| [image : NouvelElement642.png] |  |  |  |
| Caution |  |  | Listes déroulantes d’affectation des valeurs par défaut aux documents créés |
| Frais de dossier |  |  |  |
| Frais de syndic |  |  |  |
| Loyer |  |  |  |
| Annulation |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Enregistrer | GEN0001 pour la ligne sélectionnée |
| Supprimer | GEN0010 pour la ligne sélectionnée |
Créé avec HelpNDoc Personal Edition: [Écrire des livres électronique Kindle](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creer-ebooks-pour-kindle-amazon)

#### SYNDIC <a id="page-syndic"></a>
_Page : `SYNDIC.html` — Module Location › CONFIGURATION_

Créé avec HelpNDoc Personal Edition: [Créer des documents d'aide facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

##### STATUT <a id="page-statuts-rnt-syn"></a>
_Page : `STATUTS_RNT_SYN.html` — Module Location › CONFIGURATION › SYNDIC_

[image : NouvelElement751.png]

### Champs

| LIBELLE | U | O | OBSERVATION |
|---|---|---|---|
| O.D.T |  | X |  |
| Code | X | X |  |
| Description |  | X |  |
| Type |  | X | Liste déroulante des types d’un statut, on peut choisir un de ces valeurs :<br>- Nouveau<br>- Intermédiaire<br>- Terminé avec succès<br>- Terminé avec échec |
| Durée de vie |  |  |  |
| Couleur |  |  |  |
| Est un statut contrat |  |  |  |
| Est une annulation |  |  | Est un statut d'annulation |
| Est une résiliation |  |  | Est un statut de résiliation |
| Peut effectuer une prospection |  |  | Le système affiche dans le sélecteur des articles dans la fiche [prospection](#page-prospections) les articles qui ont des dossiers de vente dont le statut est paramétré " Peut effectuer une prospection ". |
| Image |  |  | Icône associé pour un statut |
|  |  |  |  |
| Champs obligatoires |  |  |  |
| [image : NouvelElement629.png] |  |  |  |
| Date envoie au notaire/ avocat |  |  |  |
| Date minute/ Enregistrement du contrat |  |  |  |
| Date effective |  |  |  |
| Date assurance |  |  |  |
| Notaire/ Avocat |  |  |  |
| Identifiant du client |  |  |  |
| Champs obligatoires des tiers |  |  |  |
| [image : NouvelElement630.png] |  |  |  |
| Adresse |  |  | La liste des champs obligatoires des tiers qui correspondent à un statut |
| Code postal |  |  |  |
| Téléphone indicatif |  |  |  |
| Téléphone |  |  |  |
| Téléphone 1 indicatif |  |  |  |
| Téléphone 1 |  |  |  |
| Téléphone 2 indicatif |  |  |  |
| Téléphone 2 |  |  |  |
| Fax indicatif |  |  |  |
| Fax |  |  |  |
| Email |  |  |  |
| Site web |  |  |  |
| Agence |  |  |  |
| RIB |  |  |  |
| IBAN |  |  |  |
| Registre de commerce |  |  |  |
| Code en douane |  |  |  |
| Fils de |  |  |  |
| Et de |  |  |  |
| Employeur |  |  |  |
| Ancienneté |  |  |  |
| Date de naissance |  |  |  |
| Lieu de naissance |  |  |  |
| Nbr enfants |  |  |  |
| Capital |  |  |  |
| Qualité |  |  |  |
| Pièce d’identité Type |  |  |  |
| Pièce d’identité N° |  |  |  |
| Pièce d’identité date |  |  |  |
| Pièce d’identité Lieu |  |  |  |
| Pièce d’identité valide jusqu’au |  |  |  |
| Pièce lieu renouvellement |  |  |  |
| Comptabilité Code |  |  |  |
| Comptabilité Compte collectif |  |  |  |
| Banque |  |  |  |
| Personnel |  |  |  |
| Contact |  |  |  |
| Assujettissement |  |  |  |
| Situation familiale |  |  |  |
| Résidence |  |  |  |
| Régime matrimonial |  |  |  |
| Nature |  |  |  |
| Pays |  |  |  |
| Nationalité |  |  |  |
| Profession |  |  |  |
| Salaire |  |  |  |
| Ville |  |  |  |
| Valeurs par défaut |  |  |  |
| [image : NouvelElement754.png] |  |  |  |
| État initial |  |  | Liste déroulante d’affectation de la valeur par défaut pour le statut de dossier de syndic |
Actions
Créé avec HelpNDoc Personal Edition: [Générateur d'aides Web gratuit](https://www.helpndoc.com/fr)

##### TRANSITIONS <a id="page-transitions-rnt-syn"></a>
_Page : `TRANSITIONS_RNT_SYN.html` — Module Location › CONFIGURATION › SYNDIC_

[image : NouvelElement752.png]

### Champs

| LIBELLE | U | O | OBSERVATION |
|---|---|---|---|
| O.D.T |  | X |  |
| De |  | X |  |
| à |  | X |  |
| Liste des rôles crées |  |  | les cases à cocher représentent les rôles des acteurs qui peuvent modifier le statut d’un dossier de location |
| Liste des champs obligatoires |  |  | Liste de choix des champs obligatoire à renseigner dans la fiche dossier de syndic |
| Remarque 1: Le statut de départ et le statut d'arrivé doivent être différents<br> Remarque 2: On ne peut pas créer deux transitions avec la même source et la même destination |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0008 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Imprimer | GEN0011 |
Créé avec HelpNDoc Personal Edition: [Générateur complet de livres électroniques Kindle](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creer-ebooks-pour-kindle-amazon)

##### TARIFICATION DU SYNDIC <a id="page-syn-pricing"></a>
_Page : `SYN_PRICING.html` — Module Location › CONFIGURATION › SYNDIC_

[image : NouvelElement753.png]
Champs

| LIBELLE | U | O | OBSERVATION |
|---|---|---|---|
| Tranche |  | X | En lecture seule |
| Code | X | X |  |
| A partir de |  | X | Date début d'application du montant de syndic |
| Notes |  |  |  |
| Liste des article |  |  | La liste visualise les articles de la tranche sélectionnée en indiquant:<br>- Code<br>- Description<br>- Superficie(m²)<br>- Catégorie<br>- Type<br>- Sous-article<br>- Montant HT (Modifiable directement à partir de la liste)<br>- TVA (Modifiable directement à partir de la liste)<br>- Montant TTC (Modifiable directement à partir de la liste)<br>- Notes<br>Aussi, dans cette liste on peut effectuer les opérations suivantes:<br>- Exporter la liste des articles<br>- Importer les nouveaux montants de syndic |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008<br> Remarque: Pour ajouter une nouvelle tarification syndic, l'utilisateur doit sélectionner tout d'abord une tranche. |
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0008 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Imprimer | GEN0011 |
| Réinitialiser à partir de stock | Le système réinitialise la liste des article à partir de stock |
| Mettre à jour les dossiers | Le système met à jour les montants des échéances de syndic qui ont une date supérieure ou égale à la date définie dans la tarification syndic de la tranche en question |
Créé avec HelpNDoc Personal Edition: [Créer des documents d'aide HTML facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

## Module Trésorerie <a id="page-moduletresorerie"></a>
_Page : `ModuleTresorerie.html`_

Créé avec HelpNDoc Personal Edition: [Générateur de documentation d'aide HTML gratuit](https://www.helpndoc.com/fr)

### ETAT DES FLUX <a id="page-etatdesflux"></a>
_Page : `ETATDESFLUX.html` — Module Trésorerie_

[image : NouvelElement438.png]
La liste visualise l’état des flux en fonction des critères suivants :

- Le compte
- Le type de pièce
- La nature du mouvement (Débit, Crédit, Seulement les mouvements pointés)

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Solde début de période |  |  | En lecture seule |
| Liste des mouvements en fonction de type de mouvements sélectionnés |  |  | La liste visualise les mouvements en indiquant :<br>- La date échéance<br>- La de de règlement<br>- La catégorie<br>- Le tiers<br>- Le compte<br>- Le montant de débit<br>- Le montant de crédit<br>- L’entité juridique… |
| Nombre |  |  | Le nombre des mouvements en fonction de la nature (Débit, Crédit, Seulement les mouvements pointés) en lecture seule |
| Débit |  |  | Débit = La somme des montants des mouvements de nature « Débit » en lecture seule |
| Crédit |  |  | Crédit = La somme des montants des mouvements de nature « Crédit » et (ou) la sommes des montants des mouvements de nature « Seulement les mouvements pointés » en lecture seule |
| Solde fin de période |  |  | Solde fin de période= (Solde début de période + Crédit) – Débit en lecture seule |
| Liste des mouvements en fonction de comptes sélectionnés |  |  | La liste visualise les mouvements en en indiquant :<br>- Le code du compte<br>- Le solde à échéance<br>- La devise<br>- La solde pointé<br>- La description |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Imprimer | GEN0011 |
| Rafraîchir | GEN0003 |
| Cliquer | GEN0013 |
Créé avec HelpNDoc Personal Edition: [Écrire des livres électroniques ePub pour l'iPad](https://www.helpndoc.com/fr/creer-des-livres-numeriques-epub)

### AFFECTATION ET POINTAGE <a id="page-affectationetpointage"></a>
_Page : `AFFECTATIONETPOINTAGE.html` — Module Trésorerie_

[image : NouvelElement201.png]
La liste visualise les mouvements d’encaissement et de décaissements saisis dans tous les autres modules. Le but de ce masque est de pouvoir effectuer un rapprochement bancaire en pointant tous les mouvements qui ont réellement impacter les comptes bancaire ou caisse.

### Champs

| LIBELLE | V | OBSERVATIONS |
|---|---|---|
| Pointer | X | Si le mouvement est pointé ou non : un mouvement pointé est un mouvement qui a été réellement impacté dans vos comptes. |
| Entité juridique | X |  |
| Article | X |  |
| Tranche | X |  |
| Date règlement | X | Si le mouvement possède un compte la valeur de la date règlement pourra être renseignée si non le système l’annulera automatiquement. Cette date correspond à la date valeur de votre opération en banque |
| Compte | X | Liste déroulante des [comptes](#page-comptes) de l’[entité juridique](#page-entitesjuridique) sélectionnée dans le filtre |
| Description | X | Description du mouvement, généralement c’est le [Type pièce] + [Banque] + N° + [Référence pièce] + du [Date pièce] |
| Crédit | X | Si le mouvement est un encaissement le montant sera renseigné dans cette colonne et en vert. |
| Débit | X | Si le mouvement est un décaissement le montant sera renseigné dans cette colonne et en rouge. |
| Tiers | X | C’est la description ou la raison sociale du tiers, généralement si le mouvement est un encaissement c’est qu’il s’agit d’un client si non c’est un fournisseur. |
| Module | X | Le module qui a été à l’origine de la saisie de ce mouvement (Vente, Projet, Stock …) |
| Relation | X | Le document affecté au mouvement, facture, document à régler … |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Affecter les mouvements sélectionnés au compte | Voir ci-dessous |
| Pointer les mouvements sélectionnés | Voir ci-dessous |
| Annuler l’affectation des mouvements sélectionnés | L’annulation remet la colonne compte à vide pour tous les mouvements sélectionnés |
| Rafraîchir | GEN0003 |
| Enregistrer | GEN0001 |

| Affecter les mouvements sélectionnés au compte |
|---|
| [image : NouvelElement204.png] |
| Pour pouvoir affecter les mouvements aux comptes, il faut procéder de la manière suivante :<br> 1 – filtrer les mouvements<br> 2 – à l’aide de la touche contrôle, sélectionner les mouvements à affecter<br> 3 – choisir le compte à affecter<br> 4 – cliquer sur le bouton « Affecter les mouvements sélectionnés au compte »<br>- Le système affecte la colonne compte de tous les mouvements sélectionnés au compte sélectionné. |
| Pointer les mouvements sélectionnés |
| [image : NouvelElement205.png] |
| En cliquant sur le bouton « Pointer les mouvements sélectionnés» le système coche la colonne pointer et affecte la date échéance à la date de règlement de tous les mouvements sélectionnés. Les dates de règlements restent modifiables. Si le mouvement n’a pas de compte affecté, le système ne le pointe pas.<br>- A la fin il faut cliquer sur « Enregistrer » |
[image : NouvelElement206.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Période |  |  | Liste déroulante de choix de nombre de jours de tolérance |
| Entité juridique |  |  | La description de l’entité juridique sélectionnée |
| Compte |  |  | Le compte sélectionné de l’entité juridique |
| Pointée |  |  | Filtre sur les mouvements système pointés:<br>- Tous : Tous les mouvements pointés et non pointés<br>- Oui : Tous les mouvements pointés<br>- Non : Tous les mouvements non pointés |
| Crédit |  |  | Filtre sur le type des mouvements système à afficher :<br>- Tous : Les mouvements de type Crédit et Débit<br>- Oui : Les mouvements de type Crédit<br>- Non : Les mouvements de type Débit |
| Tiers |  |  | Filtre dans la liste des mouvements système et la liste des mouvements importés par tiers |
| Référence |  |  | Filtre dans la liste des mouvements système et la liste des mouvements importés par référence |
| Relation |  |  | Filtre dans la liste des mouvements système et la liste des mouvements importés par relation |
| Liste des mouvements système |  |  | La liste visualise la liste des mouvements système en indiquant :<br>- Pointée<br>- La date échéance<br>- La date règlement<br>- Le montant crédit<br>- Le montant débit<br>- Le tiers<br>- La référence<br>- Le type de pièce<br>- Le compte<br>- Relation |
| Liste des mouvements importés |  |  | La liste visualise les mouvements importés à partir du document Excel en indiquant :<br>- La date<br>- Le montant crédit<br>- Le montant débit<br>- Le tiers<br>- La référence<br>- Le type de pièce<br>- relation |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Rapprochement bancaire | Le système vérifie si les champs « Entité juridique » et « Compte » sont remplit :<br>- Si oui : Le système ouvre le masque du rapprochement bancaire<br>- Si non : Le système affiche un message d’erreur |
| Rafraîchir | GEN0003 |
| Charger le ficher | GEN0018 |
| Vérifier les données | La vérification se fait à la base des mouvements système et les mouvements importés<br>- Si la référence système et le montant système = la référence importé et le montant importé<br>- Le système colore les lignes correspondantes des mouvements système en vert<br>- Si la référence système = la référence importé<br>- Le système colore les lignes correspondantes des mouvements système en bleu<br>- Si le montant système = le montant importé et la date système = date importé dans la période<br>- Le système colore les lignes correspondantes des mouvements système en bleu<br>- Sinon<br>- Le système ne colore pas les lignes des mouvements système<br> Remarque : Date dans la période = la date du mouvement système +/- le nombre de jours choisi |
| Exporter les mouvements vérifiés | GEN0012 |
| Valider la sélection | Le système valide les mouvements sélectionnés manuellement, il affecte la date de règlement et il rend la ligne des mouvements système coloré en vert<br> Remarque : La date règlement = la date du mouvement de la ligne correspondante de la liste importés |
| Annuler la validation | Le système annule la validation des mouvements sélectionnés, il efface la date règlement affecté et il rend la ligne des mouvements système incolore |
| Tout afficher | Le système annule les filtres Tiers, Référence et Relation et il réaffiche les deux listes |
| Enregistrer | Le système pointe tous les mouvements système qui sont colorés en vert |
Créé avec HelpNDoc Personal Edition: [Générer facilement des livres électroniques Kindle](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creer-ebooks-pour-kindle-amazon)

### RECETTE <a id="page-recette"></a>
_Page : `RECETTE.html` — Module Trésorerie_

[image : NouvelElement208.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Module |  | X | Liste déroulante des modules |
| Entité |  | X | Liste déroulante des [entités](#page-entitesjuridique) |
| Compte |  |  | Liste déroulante des [comptes](#page-comptes) |
| Banque |  |  | Liste déroulante des [banques](#page-banques) |
| Type |  |  | Liste déroulante des [types](#page-typespiece) |
| Référence |  |  |  |
| Montant |  |  |  |
| Date |  |  |  |
| Échéance |  |  |  |
| Statut |  |  | Liste déroulante qui peut prendre un des trois valeurs suivantes :<br>- En cours<br>- Pointée<br>- Impayé |
| Date règlement |  |  |  |
| Catégorie |  |  | Sélecteur des catégories de recettes |
| Tiers |  |  | Sélecteur des [tiers](#page-tiers) en lecture seule |
| Notes |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichier attachés | GEN0004 |
| Note | GEN0005 |
| Annuler et fermer | GEN0017 |
| Champs spécifiques | GEN0007 |
| Audit | GEN0020 |
Créé avec HelpNDoc Personal Edition: [Documentation Qt Help facile](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creez-des-fichiers-daide-pour-le-framework-qt-help)

### DEPENSES <a id="page-depenses"></a>
_Page : `DEPENSES.html` — Module Trésorerie_

[image : NouvelElement326.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Société Module |  | X | Liste déroulante des modules |
| Entité |  | X | Liste déroulante des [entités](#page-entitesjuridique) |
| Compte |  |  | Liste déroulante des [comptes](#page-comptes) |
| Banque |  |  | Liste déroulante des [banques](#page-banques) |
| Type |  |  | Liste déroulante des [types de pièce](#page-typespiece) |
| Référence |  |  |  |
| Montant |  |  |  |
| Date |  |  |  |
| Échéance |  |  |  |
| Statut |  |  | Liste déroulante qui peut prendre un des trois valeurs suivantes :<br>- En cours<br>- Pointée<br>- Impayé |
| Liste déroulante de statut |  |  |  |
| Date règlement |  |  |  |
| Catégorie |  |  | Sélecteur des [catégories de dépense](#page-categoriedesdepenses) en lecture seule |
| Tiers |  |  | Sélecteur des [tiers](#page-tiers) en lecture seule |
| Notes |  |  |  |
| GEN0015 |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichier attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0017 |
| Champs spécifiques | GEN0007 |
| Audit | GEN0020 |
| Imprimer | GEN0011 |
Créé avec HelpNDoc Personal Edition: [Générateur complet d'aides multi-formats](https://www.helpndoc.com/fr/outils-creation-aide)

### TRANSFERTS INTERNES <a id="page-transfertsinternes"></a>
_Page : `TRANSFERTSINTERNES.html` — Module Trésorerie_

[image : NouvelElement324.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Entité |  |  | Liste déroulante des [entités juridique](#page-entitesjuridique) |
| Code |  |  |  |
| Date |  | X | Par défaut date de jour |
| Montant |  |  |  |
| Compte source |  | X | Liste déroulante de [comptes](#page-comptes) |
| Type de source |  | X | Les déroulante de [types de pièces](#page-typespiece) |
| Référence source |  |  |  |
| Compte destination |  | X | Liste déroulante de [comptes](#page-comptes) |
| Type destination |  | X | Liste déroulante [types de pièces](#page-typespiece) |
| Référence destination |  |  |  |
| Notes |  |  |  |
| GEN0015 |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichier attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0017 |
| Champs spécifiques | GEN0007 |
| Audit | GEN0020 |
| Imprimer | GEN0011 |
Créé avec HelpNDoc Personal Edition: [Créer des sites web d'aide facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

### PREVISIONS <a id="page-previsions-stl"></a>
_Page : `PREVISIONS_STL.html` — Module Trésorerie_

[image : NouvelElement323.png]

### Champs

| LIBELLE | V | OBSERVATIONS |
|---|---|---|
| Prévisions |  |  |
| Catégorie | X | Le système affiche la hiérarchie de la catégorie des dépenses |
| Mois | X | En cliquant sur une cellule, on peut affecter le montant à une catégorie de dépense qui correspond un mois |
| Total | X | Le montant total de prévisions de dépenses d’un an pour une catégorie |
| Ventilation |  |  |
| Ventilation | X | Pourcentage de la ventilation |
| Projet | X |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Générer à partir de l’exercice |  |
Créé avec HelpNDoc Personal Edition: [Produire des livres électroniques facilement](https://www.helpndoc.com/fr/creer-des-livres-numeriques-epub)

### RAPPROCHEMENTS BANCAIRE <a id="page-rapprochementsbancaire"></a>
_Page : `RAPPROCHEMENTSBANCAIRE.html` — Module Trésorerie_

[image : NouvelElement322.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Entité |  | X | Liste déroulante des entités juridique |
| Compte |  | X | Liste déroulante des [comptes](#page-comptes) |
| Année |  | X |  |
| Mois |  | X | Liste des numéros du mois |
| Solde calculé |  | X | En lecture seule,<br> Solde calculé = ∑ des montants des mouvements pointés jusque’à le mois précédent de la période choisi. |
| Saisie rapproché |  | X |  |
| Notes |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichier attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0017 |
| Champs spécifiques | GEN0007 |
| Audit | GEN0020 |
| Imprimer | GEN0011 |
Créé avec HelpNDoc Personal Edition: [Documentation Qt Help facile](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creez-des-fichiers-daide-pour-le-framework-qt-help)

### TABLEAU DE BORDS <a id="page-tableaudebords-stl"></a>
_Page : `TABLEAUDEBORDS_STL.html` — Module Trésorerie_

Créé avec HelpNDoc Personal Edition: [Produire facilement des livres électroniques Kindle](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creer-ebooks-pour-kindle-amazon)

####  TRESORERIE GENERALE <a id="page-tresoreriegenerale"></a>
_Page : `TRESORERIEGENERALE.html` — Module Trésorerie › TABLEAU DE BORDS_

[image : NouvelElement209.png]

### UNDER CONSTRUCTION
Créé avec HelpNDoc Personal Edition: [Produire des livres électroniques facilement](https://www.helpndoc.com/fr/creer-des-livres-numeriques-epub)

#### TRESORERIE ANNUELLE <a id="page-tresorerieannuelle"></a>
_Page : `TRESORERIEANNUELLE.html` — Module Trésorerie › TABLEAU DE BORDS_

[image : NouvelElement221.png]

| Module | Prévisions | Engagement | Règlement |
|---|---|---|---|
| Module vente | - Le système affiche tous les prévisions qui ont une date supérieure ou égale au mois courant<br>- Le système n’affiche pas les prévisions qui ont une date strictement inférieure au mois courant.<br>- Les utilisateurs doivent corriger périodiquement leurs prévisions de vente | - Le système affiche les engagements des dossiers de ventes dont le statut est paramétré « Est une vente effective »<br>- Le système rapatrie les engagements en fonction de la date de document<br>- Si la date document <= mois en cours :<br> Le système rapatrie le montant de document dans le mois courant<br>- Si la date document > mois en cours :<br> Le système affiche les engagements dans leurs dates | - Le système converti le document en une pièce de règlement avec le montant réglé et il garde le reste à payer comme un engagement<br>Sans retenue à la source<br>- Montant R.S = 0, Le système affiche un mouvement prévisionnel qui correspond au montant net à payer à la date échéance<br>Avec retenue à la source<br>- Si on a un montant R.S > 0, le système affiche le net à payer à la date échéance, et il ajoute une dépense avec le montant R.S au 28 du mois d’après (la date de dépense de R.S est calculer par rapport la date de pièce)<br>Pointage<br>- Si règlement en cours<br>- Le système garde toujours le mouvement prévisionnel<br>- Si règlement pointé<br> Le système affiche un mouvement réel dans la date de pointage avec le montant réglé (ou payé) (Si la date est inférieur ou égale le mois en cours) |
| Factures et documents à régler | - | - Le système rapatrie les engagements (Factures et document à régler) selon leurs dates échéance<br>- Si la date échéance < Mois en cours :<br>- Le système rapatrie les montants reste à régler des factures (ou des documents à régler) dans le mois courant<br>- Si la date échéance >= Mois en cours :<br>- Le système affiche les montants reste à régler des factures (ou des documents à régler) dans leurs dates échéance respectives. |  |
| Crédits conventionnels et les financements fournisseur | - | - Si c’est un crédit conventionnel :<br>- Recette : Si la date de déblocage <= au mois courant<br>- Le système affiche une recette prévisionnelle<br>- Si la date de déblocage >= mois courant<br>- Le système affiche une recette prévisionnelle<br>- Dépense : Si les dates des échéanciers < au mois courant<br>- Le système les rapatrie dans le mois courant<br>- Si la date d’échéance >= mois courant<br>- Le système affiches les prévisions des échéances dans leurs dates<br>- Si c’est un financement fournisseur<br>- Le système ne génère pas une recette<br>- Si on paye la totalité d’une facture ou d’une pièce de règlement par un crédit fournisseur le système n’affiche pas le mouvement de dépense et il supprime les prévisions de paiement de ces engagements (Facture ou document à régler)<br>- Si on paye une facture ou une pièce de règlement partiellement, le système n’affiche que le reste à payer des factures (ou des pièces de règlement) dans leurs dates échéance<br>- Dépense : Si les dates des échéanciers < au mois courant<br>- Le système les rapatrie dans le mois courant<br>- Si la date d’échéance >= mois courant<br>- Le système affiches les prévisions des échéances dans leurs dates |  |
| Module projet | Le système copie les prévisions associées aux lots dans la trésorerie annuelle en appliquant la règle suivante :<br>- Si la date de prévision <= au mois en cours : le système rapatrie les prévisions dans le mois en cours<br>- Si la date de prévision > mois en cours : Le système affiche les prévisions dans leurs dates | - Le système rapatrie les engagements (Factures et document à régler) selon leurs dates échéance<br>- Si la date échéance < Mois en cours :<br>- Le système rapatrie les montants reste à régler des factures (ou des documents à régler) dans le mois courant<br>- Si la date échéance >= Mois en cours :<br> Le système affiche les montants reste à régler des factures (ou des documents à régler) dans leurs dates échéance respectives. |  |
| Module location | - | - Le système affiche les engagements des dossiers de location dont le statut est paramétré « Est une location effective »<br>- Le système rapatrie les engagements en fonction de la date de document<br>- Si la date document <= mois en cours :<br>- Le système rapatrie le montant de document dans le mois courant<br>- Si la date document > mois en cours :<br>- Le système affiche les engagements dans leurs dates respectives. |  |
Créé avec HelpNDoc Personal Edition: [Créer des livres électroniques facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

#### BILAN DES PROJETS <a id="page-bilandesprojets"></a>
_Page : `BILANDESPROJETS.html` — Module Trésorerie › TABLEAU DE BORDS_

[image : NouvelElement211.png]

### UNDER CONSTRUCTION
Créé avec HelpNDoc Personal Edition: [Produire des livres Kindle gratuitement](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creer-ebooks-pour-kindle-amazon)

#### RETENUE A LA SOURCE <a id="page-retenuealasource"></a>
_Page : `RETENUEALASOURCE.html` — Module Trésorerie › TABLEAU DE BORDS_

[image : NouvelElement212.png]

### UNDER CONSTRUCTION
Créé avec HelpNDoc Personal Edition: [Avantages d'un outil de création d'aide](https://www.helpauthoringsoftware.com/fr)

### CONFIGURATION <a id="page-configuration-stl"></a>
_Page : `CONFIGURATION_STL.html` — Module Trésorerie_

Créé avec HelpNDoc Personal Edition: [Générateur de documentation et EPub facile](https://www.helpndoc.com/fr)

#### NATURES COMPTE <a id="page-naturescompte"></a>
_Page : `NATURESCOMPTE.html` — Module Trésorerie › CONFIGURATION_

[image : NouvelElement886.bmp]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Code | X | X |  |
| Description |  | X |  |
| Compte caisse |  |  | Si oui c’est un compte espèce |
| Notes |  |  |  |
| GEN0015 |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Enregistrer | GEN0001 pour la ligne sélectionnée |
| Supprimer | GEN0010 pour la ligne sélectionnée |
Créé avec HelpNDoc Personal Edition: [Créer des aides HTML, DOC, PDF et des manuels depuis une même source](https://www.helpndoc.com/fr/outils-creation-aide)

#### TYPES PIECE <a id="page-typespiece"></a>
_Page : `TYPESPIECE.html` — Module Trésorerie › CONFIGURATION_

[image : NouvelElement888.bmp]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Code | X | X |  |
| Description |  | X |  |
| Format |  |  | Non encore implémenté |
| Notes |  |  |  |
| Référence obligatoire |  |  | La pièce doit avoir obligatoirement une référence |
| Unicité sur la référence |  |  | La pièce doit avoir une référence unique |
| Banque obligatoire |  |  | La pièce doit avoir obligatoirement une banque |
| Unicité sur le Tier |  |  | La pièce doit être unique en combinaison de la référence et le tier<br> Si cette case est coché:<br>- Le système vérifie l'unicité par rapport les champs: Tiers + Référence + Banque<br> Sinon<br> Le système vérifie l'unicité par rapport les champs: Référence + Banque |
| Notaire / Avocat obligatoire |  |  |  |
| Non comptabilisé |  |  | Le montant de la pièce ne sera pas calculer au niveau des versements |
| Avec compte de dépôt |  |  | Si OUI lors de la création d’un règlement le compte de dépôt devient obligatoire |
| Avec compte |  |  | Non encore implémenté |
| Compte collectif Crédit |  |  | Comptabilisation |
| Compte collectif Débit |  |  | Comptabilisation |
| Natures compte |  |  | Lors de la création d’un règlement le système filtre les comptes selon les natures affectées au type sélectionné. |
| GEN0015 |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Enregistrer | GEN0001 pour la ligne sélectionnée |
| Supprimer | GEN0010 pour la ligne sélectionnée |
Créé avec HelpNDoc Personal Edition: [Environnement de création d'aide complet](https://www.helpndoc.com/fr/outils-creation-aide)

#### COMPTES <a id="page-comptes"></a>
_Page : `COMPTES.html` — Module Trésorerie › CONFIGURATION_

[image : NouvelElement215.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Détails |  |  |  |
| Entité |  | X | Liste déroulante des [entités juridiques](#page-entitesjuridique) |
| Nature |  | X | Liste déroulante des [natures de compte](#page-naturescompte) |
| Code | X | X |  |
| Description |  | X |  |
| Banque |  |  | Sélecteur des [banques](#page-banques) en lecture seule |
| Code banque |  |  |  |
| Code agence |  |  |  |
| Numéro de compte |  |  |  |
| Clé |  |  |  |
| RIB |  |  | En lecture seule |
| IBAN |  |  |  |
| Code BIC |  |  |  |
| Devise |  |  |  |
| Autres |  |  |  |
| Lieu de création |  |  |  |
| Nom et adresse |  |  |  |
| Domiciliation |  |  |  |
| Code comptable |  |  |  |
| GEN0015 |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Imprimer | GEN0011 |
| Exporter | GEN0012 |
| Cliquer | GEN0013 |
| Enregistrer | GEN0001 pour la ligne sélectionnée |
| Supprimer | GEN0010 pour la ligne sélectionnée |
Créé avec HelpNDoc Personal Edition: [Écrire des livres électroniques ePub pour l'iPad](https://www.helpndoc.com/fr/creer-des-livres-numeriques-epub)

#### CATEGORIES DES RECETTES <a id="page-categoriesdesrecettes"></a>
_Page : `CATEGORIESDESRECETTES.html` — Module Trésorerie › CONFIGURATION_

[image : NouvelElement216.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Code | X | X |  |
| Description |  | X |  |
| Corresp. Comp |  |  |  |
| Compte |  |  | Liste déroulante des [comptes](#page-comptes) |
| Sans prévision manuelle |  |  |  |

### Action

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter une catégorie | Ajouter une catégorie racine |
| Ajouter une sous-catégorie | Ajouter une sous-catégorie qui appartient à la catégorie racine |
| Supprimer | GEN0010 |
| Éditer | GEN0009 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
Créé avec HelpNDoc Personal Edition: [Créer des documents d'aide PDF facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

#### CATEGORIE DES DEPENSES <a id="page-categoriedesdepenses"></a>
_Page : `CATEGORIEDESDEPENSES.html` — Module Trésorerie › CONFIGURATION_

[image : NouvelElement217.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Code | X | X |  |
| Description |  | X |  |
| Corresp. Comp |  |  |  |
| Compte |  |  | Liste déroulante des [comptes](#page-comptes) |
| Sans prévision manuelle |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter une catégorie | Ajouter une catégorie racine |
| Ajouter une sous-catégorie | Ajouter une sous-catégorie qui appartient à la catégorie racine |
| Supprimer | GEN0010 |
| Éditer | GEN0009 |
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
Créé avec HelpNDoc Personal Edition: [Générateur de documentation Qt Help gratuit](https://www.helpndoc.com/fr)

#### RELATION AVEC AUTRE MODULES <a id="page-relationavecautremodules"></a>
_Page : `RELATIONAVECAUTREMODULES.html` — Module Trésorerie › CONFIGURATION_

[image : NouvelElement220.png]

### Champs

| Les prêts |  |  |  |
|---|---|---|---|
| LIBELLE | U | O | OBSERVATIONS |
| Remboursement capital prêt |  |  | Sélecteur de [catégorie de dépenses](#page-categoriedesdepenses) en lecture seule |
| Frais de dossier prêt |  |  | Sélecteur de [catégorie de dépenses](#page-categoriedesdepenses) en lecture seule |
| Remboursement intérêts prêt |  |  | Sélecteur de[catégorie de dépenses](#page-categoriedesdepenses) en lecture seule |
| Encaissement d’un déblocage de prêt |  |  | Sélecteur de [catégorie de recettes](#page-categoriesdesrecettes) en lecture seule |

| Étude de faisabilité |  |  |  |
|---|---|---|---|
| LIBELLE | U | O | OBSERVATIONS |
| Prix du terrain |  |  | Sélecteur de [catégorie de dépenses](#page-categoriedesdepenses) en lecture seule |
| Frais d’enregistrement du terrain |  |  | Sélecteur de [catégorie de dépenses](#page-categoriedesdepenses) en lecture seule |
| Autre frais du terrain |  |  | Sélecteur de[catégorie de dépenses](#page-categoriedesdepenses) en lecture seule |
| Intérêt sur crédit |  |  | Sélecteur de [catégorie de dépenses](#page-categoriedesdepenses) en lecture seule |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Cliquer | GEN0013 |
| Double cliquer | GEN0014 |
Créé avec HelpNDoc Personal Edition: [Sites web iPhone faciles](https://www.helpndoc.com/fr/tour-des-fonctionnalites/generation-de-site-web-iphone)

## Module Stock <a id="page-modulestock"></a>
_Page : `ModuleStock.html`_

Le module stock prend en charge tout le patrimoine immobilier de votre entreprise. Votre stock de biens est organisé d’une façon hiérarchisée sous les entités suivantes :

- Entités juridique
- Programmes immobilier
- Tranches
Chaque bien doit avoir une nature, une vocation, un type et une catégorie bien déterminée pour que le système sache exactement ou le gérer.
Vos biens sont classés en deux natures principales :

- BATIMENT : Les biens construits ou en construction
- TERRAIN : les lots de terrains destinés à la vente ou pour le développement de nouveaux projets.
Vos biens sont aussi classés en trois vocations principales :

- VENTE : Les biens destinés à la vente
- LOCATION : Les biens destinés à la location
- PROJET : Les biens destinés à l’exécution d’un projet
Les types et les catégories sont complètement paramétrables selon l’organisation de vos projets.
Un autre aspect très important étant géré dans le module stock, c’est la constitution d’une base de données concernant les prospections des terrains éventuellement susceptibles d’être acquis par votre entreprise pour des projets futures.
Créé avec HelpNDoc Personal Edition: [Produire des livres Kindle gratuitement](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creer-ebooks-pour-kindle-amazon)

### ENTITES JURIDIQUE <a id="page-entitesjuridique"></a>
_Page : `ENTITESJURIDIQUE.html` — Module Stock_

L’entité juridique correspond à une société, dans la solution OMRAN Estate vous pouvez gérer plusieurs sociétés à la fois.
(*) Remarque : toutes les données de la solution pour tous les modules seront classées sous une entité juridique.
[image : AA.png]

### Liste

| LIBELLE | V | OBSERVATIONS |
|---|---|---|
| Active |  |  |
| Adresse |  |  |
| Adresse 1 |  |  |
| Adresse 2 |  |  |
| Adresse 3 |  |  |
| Capital |  |  |
| CNSS |  |  |
| Code |  | Code Entité Juridique |
| Code postal |  | Code postal de l'entité juridique |
| Courrier fait à |  | Lieu où le le document a été imprimé |
| Courrier signataire |  |  |
| Crée le |  |  |
| Crée par |  |  |
| Date de création |  |  |
| Date liquidation |  |  |
| Description |  | Description Entité juridique |
| Description détaillée |  |  |
| Email |  |  |
| Fax |  |  |
| Forme juridique |  |  |
| Groupe actionnaire |  |  |
| Identifiant commun de l'entreprise |  |  |
| Matricule fiscale |  |  |
| Mobile |  |  |
| Modifié le |  |  |
| Modifié par |  |  |
| Nombre actionnaires |  | Nombre des actionnaires |
| Nombre de parts (actions) |  | Nombre total des parts |
| Part maison |  |  |
| Patente |  |  |
| Pays |  | Description Pays |
| Registre de commerce |  |  |
| Représentant légal |  |  |
| Responsable commercial |  |  |
| Responsable technique |  |  |
| Site web |  |  |
| Tél . |  |  |
| Ville |  | Description Ville |

### Champs

| Libellé | U | O | OBSERVATIONS |
|---|---|---|---|
| Code | X | X | Code entité juridique |
| Description |  | X | Description entité juridique |
| Matricule fiscale |  |  |  |
| Registre du commerce |  |  |  |
| Forme juridique |  |  |  |
| Groupe d'actionnaires |  |  | Liste déroulante des [groupes d'actionnaire](#page-groupedactionnaires) |
| Date de création |  |  |  |
| Capital |  |  |  |
| Nombre actionnaires |  |  |  |
| CNSS |  |  |  |
| Patente |  |  |  |
| Identifiant commun de l'entreprise |  |  |  |
| Nombre de parts (Actions) |  |  | Nombre total des parts |
| Date liquidation |  |  |  |
| Active |  |  |  |
| Part maison |  |  |  |
| Coordonnées |  |  |  |
| Adresse |  |  |  |
| Code postale |  |  |  |
| Ville |  |  |  |
| Pays |  |  | Liste déroulante des [pays](#page-pays) |
| Téléphone |  |  |  |
| Fax |  |  |  |
| Mobile |  |  |  |
| E-mail |  |  |  |
| Site web |  |  |  |
| Administration |  |  |  |
| Courrier fait à |  |  | Lors de l'impression des éditions (lettres, rapports ...) avant la date du document imprimé le système insère la valeur du "Courrier fait à = Tunis"<br> Exemple : Tunis, le 01/01/20016 |
| Représentation légal |  |  | Choix du [personnel](#page-personnels) |
| Courrier signataire |  |  | Choix du [personnel](#page-personnels) |
| Responsable technique |  |  | Choix du [personnel](#page-personnels) |
| Responsable commercial |  |  | Choix du [personnel](#page-personnels) |
| Répartition |  |  |  |
| Liste de répartition |  |  | La liste visualise les [actionnaires](#page-actionnaires) de l’entité juridique :<br>- Actionnaire<br>- Nombre Parts/Actions<br>- Crée par<br>- Crée le<br>- Modifié par<br>- Modifié le<br>Remarque: Le système affecte les actionnaires et calcule automatiquement le nombre de part de chacun en utilisant le paramétrage effectué dans l'écran "[Groupe d'actionnaires](#page-groupedactionnaires)". |
| Autres |  |  |  |
| Description détaillée |  |  |  |
[PERSONNELS.html](#page-personnels)

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Exporter | GEN0012 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Audit | GEN0020 |
| Imprimer | GEN0011 |
| Débloquer | Lorsqu'on clic sur "débloquer", le système rend la fiche entité juridique modifiable<br> Remarque: Par défaut, à l'enregistrement de la fiche, le système rend tout les champs en lecture seule. |
Créé avec HelpNDoc Personal Edition: [Créer des livres électroniques EPub facilement](https://www.helpndoc.com/fr/tour-des-fonctionnalites)

### PROSPECTIONS TERRAIN <a id="page-prospectionsterrain"></a>
_Page : `PROSPECTIONSTERRAIN.html` — Module Stock_

[image : aaaaa.png]

### Liste

| LIBELLE | V | OBSERVATIONS |
|---|---|---|
| Adresse |  |  |
| Anomalies |  |  |
| Archivée |  |  |
| Arrêté d'alignement |  |  |
| CPF Immatriculation |  | La Conservation de la Propriété Foncière |
| Catégorie |  | Description Catégorie |
| Coef equip. voi. |  |  |
| Coef Plancher vendable |  |  |
| Coût de construction du m² |  |  |
| Coût du m² non bâti |  |  |
| Crée le |  |  |
| Crée par |  |  |
| Date |  |  |
| Date enregistrement |  |  |
| Date Immatriculation |  |  |
| Description |  |  |
| Emprise au sol Bâtiment (COS) |  | Emprise au sol bâtiment = Pourcentage Emprise au sol bâtiment * Superficie (m²) |
| Hauteur max autorisée(m) |  |  |
| Intermédiaire |  |  |
| Lieu d'enregistrement |  |  |
| Modifié le |  | Date de modification |
| Modifié par |  | L'utilisateur qui a fait la modification |
| N° enregistrement |  |  |
| Nbr . étude de fais. |  |  |
| Niveau |  |  |
| Notes |  |  |
| Pays |  | Description du Pays |
| Plancher Couvert (CUF) |  | Plancher couvert= Pourcentage Plancher couvert * Emprise au sol bâtiment |
| Plancher vendable |  | Plancher vendable= Pourcentage plancher vendable * plancher couvert |
| Prix d'achat |  | Prix du m² * Surface terrain |
| Prix de vente du m² |  |  |
| Prix du m² |  |  |
| Prix proposé |  |  |
| Propriété |  |  |
| Statut |  |  |
| Superficie Emprise au sol Bâtiment (m²) |  |  |
| Superficie equip. voi. (m²) |  |  |
| Superficie Plancher couvert (m²) |  |  |
| Superficie (m²) |  |  |
| Transformé en terrain |  |  |
| Type |  | Description Type |
| Validité |  |  |
| Ville |  | Description Ville |
| Vocation |  | Vocation description |

### Champs

| PROPRIETE | U | O | OBSERVATIONS |
|---|---|---|---|
| Code | X | X | Code de prospection terrain |
| Date |  | X |  |
| Description |  | X |  |
| Catégorie |  | X | Liste déroulante de [catégories](#page-stk-categories-item)[STK_CATEGORIES_ITEM.html](#page-stk-categories-item)qui ont la nature "Terrain" |
| Niveau |  |  | Liste déroulante de [niveaux](#page-lnd-niveau) |
| Type |  | X | Liste déroulante de [types](#page-stk-types-item)[STK_TYPES_ITEM.html](#page-stk-types-item)qui ont la nature "Terrain" |
| Vocation |  |  | Liste déroulante de [vocations](#page-lnd-vocation) |
| Pays |  | X | Liste déroulante des [pays](#page-pays) |
| Ville |  | X | Liste déroulante des [villes](#page-villes) |
| Statut |  | X | Liste déroulante de [statuts](#page-lnd-statuts) |
| Date validité de statut |  |  | Date de la prospection + Durée de vie (La durée de vie est paramétrée dans le [statut](#page-lnd-statuts) ) |
| Courtier |  |  | Sélecteur de [tiers](#page-fournisseurs-adm) (par défaut tiers de type fournisseurs) |
| Notaire |  |  | Sélecteur de [tiers](#page-fournisseurs-adm) (par défaut de type fournisseurs ) |
| GMap |  |  | Lien d’accès à l’emplacement du terrain via Google Maps |
| Orientation |  |  | Liste déroulante des [orientations](#page-stk-orientations) |
| Adresse |  |  |  |
| Propriétaires |  |  |  |
| Notes |  |  |  |
| Archivée |  |  |  |
| Général |  |  |  |
| Surface terrain (m²) |  |  |  |
| Voies d’aménagement/ Équipement |  |  |  |
| Surface terrain nette (m²) |  |  | = Surface terrain – Voie d’aménagement/ Équipements |
| Emprise au sol bâtiment (COS) |  |  | = Pourcentage emprise au sol bâtiment * surface terrain nette (m²) |
| Plancher couvert (CUF) |  |  | = Pourcentage plancher couvert * Surface terrain nette (m²) |
| Plancher vendable |  |  | = Pourcentage plancher vendable * plancher couvert |
| Arrêté d’alignement (m) |  |  |  |
| Hauteur max autorisée (m) |  |  |  |
| Prix du m² |  |  |  |
| Prix |  |  | Prix du m² * Surface terrain |
| Prix proposé |  |  |  |
| Enregistrement |  |  |  |
| Date |  |  |  |
| N° |  |  |  |
| Lieu |  |  |  |
| C.P.F. Immat |  |  |  |
| Date Immat |  |  |  |
| Anomalies |  |  |  |
|  |  |  | Note anomalies |
| Historique |  |  |  |
| Liste historique |  |  | La liste visualise l’historique d’une prospection en identifiant :<br>- L’utilisateur<br>- La date<br>- L’état |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Exporter | GEN0012 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Imprimer | GEN0011 |
| Transformer en terrain | Ajouter le terrain dans la liste des « [Terrains](#page-terrains)»<br> En cliquant sur « Transformer en terrain » le système affiche un message de confirmation « Voulez-vous vraiment transformer cette prospection en terrain »<br>[image : NouvelElement43.png]<br>- Si on clique « Oui » : Le système affiche la fiche « Terrain » et il affecte les informations déjà enregistrés dans le masque « Prospection terrain »<br>- Si on clique « Non » : Le système ne fait rien |
Créé avec HelpNDoc Personal Edition: [Outil de création d'aide complet](https://www.helpndoc.com/fr/outils-creation-aide)

### TERRAINS <a id="page-terrains"></a>
_Page : `TERRAINS.html` — Module Stock_

[image : ccccc.png]

### Liste

| LIBELLE | V | OBSERVATIONS |
|---|---|---|
| Adresse |  |  |
| Anomalies |  |  |
| Arrêté d'alignement |  |  |
| Catégorie |  | Catégorie Article |
| Catégorie Code |  | Code de Catégorie de l'article |
| Catégorie de Charge |  | Catégorie charge |
| Catégorie de Charge Code |  | Code Catégorie charge |
| Code |  | Code de terrain |
| Contenance |  |  |
| CPF Immatriculation |  | La Conservation de la Propriété Foncière |
| Crée le |  |  |
| Crée par |  |  |
| Date du titre foncier |  |  |
| Date enregistrement |  |  |
| Date Immatriculation |  |  |
| Description |  |  |
| Emprise au sol Bâtiment (COS) |  | Emprise au sol bâtiment = Pourcentage Emprise au sol bâtiment * Superficie (m²) |
| Entité |  | Description de l'Entité juridique |
| Entité Code |  | Code de l'Entité juridique |
| État |  | Description État |
| Fraction Divise N° |  |  |
| Fraction Divise Terrain |  |  |
| Hauteur max autorisée(m) |  |  |
| Index compt.eau |  |  |
| Index compt.électricité |  |  |
| Index compt.gaz |  |  |
| Intitulé du Titre foncier |  |  |
| Lieu d'enregistrement |  |  |
| Modifié le |  | Date de modification |
| Modifié par |  | L'utilisateur qui a fait la modification |
| N° compt.eau |  |  |
| N° compt.électricité |  |  |
| N° compt.gaz |  |  |
| N° Dossier |  |  |
| N° enregistrement |  |  |
| N° Titre foncier |  |  |
| N° Titre foncier mère |  |  |
| Notaire |  |  |
| Notes |  |  |
| Notes du titre foncier |  |  |
| Orientation |  | Description Orientation |
| Orientation Code |  | Code de l'orientation |
| Pays |  | Description du Pays |
| Pays Code |  | Code du pays |
| Plancher Couvert (CUF) |  | Plancher couvert= Pourcentage Plancher couvert * Emprise au sol bâtiment |
| Prix Article HT |  | Prix de l'article hors taxe = Superficie (m²) * Prix de m² hors taxe |
| Prix Article Prov.HT |  | Prix provisoire de l'article hors taxe = Superficie (m²) * Prix de m² provisoire hors taxe |
| Prix Article Prov.TTC |  | Prix provisoire de l'article tout taxe compris= Superficie (m²) * Prix de m² provisoire tout taxe compris |
| Prix Article TTC |  | Prix de l'article tout taxe compris = Superficie (m²) * Prix de m² tout taxe compris |
| Prix de vente HT |  | Prix de vente = Prix article hors taxe + Prix sous articles hors taxe |
| Prix de vente Prov.HT |  | Prix de vente = Prix article provisoire hors taxe + Prix sous articles provisoire hors taxe |
| Prix de vente Prov.TTC |  | Prix de vente = Prix article provisoire tout taxe compris + Prix sous articles provisoire tout taxe compris |
| Prix de vente TTC |  | Prix de vente = Prix article tout taxe compris + Prix sous articles tout taxe compris |
| Prix du m² HT |  | Prix d'un article de 1m² hors taxe |
| Prix du m² Prov.HT |  | Prix provisoire d'un article de 1m² hors taxe |
| Prix du m² Prov.TTC |  | Prix provisoire d'un article de 1m² hors taxe * Taux TVA (%) |
| Prix du m² TTC |  | Prix d'un article de 1m² hors taxe * Taux TVA (%) |
| Prix Sous Article HT |  | Prix de sous article hors taxe |
| Prix Sous Article Prov.HT |  | Prix provisoire des sous articles hors taxe |
| Prix Sous Article Prov.TTC |  | Prix provisoire des sous articles tout taxe compris |
| Programme |  | Description Programme |
| Programme Code |  | Code du programme |
| Propriété dite |  |  |
| Ref.compt.eau |  |  |
| Ref.compt.électricité |  |  |
| Ref.compt.gaz |  |  |
| Ref.police compt.eau |  |  |
| Ref.police compt.électricité |  |  |
| Ref.police compt.gaz |  |  |
| Remise max |  |  |
| Retrait / voisin (m) |  |  |
| Superficie (m²) |  |  |
| Superficie Nette (m²) |  |  |
| Superficie Prov. (m²) |  |  |
| Superficie sous Articles (m²) |  |  |
| Tranche |  | Description de la tranche |
| Tranche Code |  | Code de la tranche |
| TVA |  | Taxe sur valeur ajoutée |
| Type |  | Description Type |
| Type Code |  | Code type |
| Ville |  | Description Ville |
| Ville Code |  | Code de la ville |
| Vocation |  | Vocation description |
| Vocation Code |  | Code vocation |
[image : NouvelElement706.png]

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Code | X | X |  |
| Description |  | X |  |
| Entité juridique |  | X | Liste déroulante d’[entités juridique](#page-entitesjuridique) |
| Tranche |  |  | Liste déroulante des [tranches](#page-cfg-tranches) |
| Catégorie |  | X | Liste déroulante de catégories qui ont la nature "Terrain" |
| Type |  | X | Liste déroulante des [types](#page-stk-types-item)[STK_TYPES_ITEM.html](#page-stk-types-item)qui ont la nature "Terrain |
| Niveau |  |  | Liste déroulante des [niveaux](#page-lnd-niveau) |
| Vocation |  |  | Liste déroulante de [vocations](#page-lnd-vocation) |
| Pays |  | X | Liste déroulante des [pays](#page-pays) |
| Ville |  | X | Liste déroulante des [villes](#page-villes) |
| Courtier |  | X | Sélecteur des [tiers](#page-fournisseurs-adm) |
| Notaire |  |  | Sélecteur des [tiers](#page-fournisseurs-adm) |
| Gmap |  |  |  |
| Nature |  | X |  |
| Vocation article |  |  | Liste déroulante des [vocations articles](#page-stk-vocations-item) |
| Orientation |  |  | Liste déroulante [d’orientation](#page-stk-orientations) |
| Cat. Charge |  |  | Liste déroulante de [catégorie de charge](#page-stk-categoriesdecharge) |
| Adresse |  |  |  |
| Propriétaires |  |  |  |
| Notes |  |  |  |
| Superficies/ Valorisation (Provisoire) |  |  |  |
| Superficie (m²) |  |  |  |
| Prix du m² HT |  |  | Prix de l'article de 1m² hors taxe |
| Prix du m² TTC |  |  | Prix de l'article de 1m² hors taxe * Taux TVA (%) |
| Prix article |  |  | Prix article provisoire = Superficie provisoire (m²) * prix provisoire de m² |
| Retrait / voisin (m) |  |  |  |
| Superficies/ Valorisation (Définitif) |  |  |  |
| Surface terrain (m²) |  |  |  |
| Voies aménagement/Équipements |  |  |  |
| Surface terrain nette (m²) |  |  | Surface terrain – Voie d’aménagement/ Équipements |
| Emprise au sol bâtiments (COS) |  |  | Pourcentage emprise au sol bâtiment * surface terrain nette (m²) |
| Plancher couvert (CUF) |  |  | Pourcentage plancher couvert * Surface terrain nette (m²) |
| Plancher vendable |  |  | Pourcentage plancher vendable * plancher couvert |
| Arrêté d'alignement (m) |  |  |  |
| Hauteur max autorisée (m) |  |  |  |
| Prix du m² HT |  |  | Prix de l'article de 1m² hors taxe |
| Prix du m² TTC |  |  | Prix de l'article de 1m² hors taxe * Taux TVA (%) |
| Prix article |  |  | Prix article définitif = Superficie définitif (m²) * prix définitif de m² |
| Avance minimum |  |  |  |
| TVA |  |  | Taxe sur Valeur Ajoutée |
| Loyer HT |  |  | Montant de location hors taxe |
| Nombre de caution |  |  |  |
| Superficies/ Valorisation (Prospection) |  |  |  |
| Prix du m² |  |  |  |
| Prix |  |  |  |
| Prix proposé |  |  |  |
| Coûts |  |  |  |
| Propriétaire |  |  | Sélecteur des [tiers](#page-fournisseurs-adm) |
| Prix du m² |  |  |  |
| Coût total |  |  |  |
| Date |  |  | Date d’acquisition du terrain |
| Prix |  |  |  |
| Coût de revient du m² |  |  |  |
| Liste des frais |  |  | La liste visualise les détails des dépenses d’un terrain<br>- Type de dépense<br>- Date<br>- Montant frais<br>- Fournisseur<br>- Notes<br>- Crée le<br>- Crée par<br>- Modifié par<br>- Modifié le<br> Lors qu’on clique « valider pour règlement » le système génère un document à régler dans le module Achat. |
| Identification foncière |  |  |  |
| Date enregistrement |  |  |  |
| N° enregistrement |  |  |  |
| Lieu enregistrement |  |  |  |
| C.P.F Immat |  |  |  |
| Date Immat |  |  |  |
| N° titre mère |  |  |  |
| N° titre |  |  |  |
| Intitulé titre |  |  |  |
| Date titre |  |  |  |
| Propriété dite |  |  |  |
| Fraction divise N° |  |  |  |
| Fraction divises terrain |  |  |  |
| Parties divises |  |  |  |
| Parties indivises |  |  |  |
| Notes |  |  |  |
| Compteur (Électricité) |  |  |  |
| Référence |  |  |  |
| N° police |  |  |  |
| N° compteur |  |  |  |
| Arrêté du compteur |  |  |  |
| Compteur (Gaz) |  |  |  |
| Référence |  |  |  |
| N° police |  |  |  |
| N° compteur |  |  |  |
| Arrêté du compteur |  |  |  |
| Compteur (Eau) |  |  |  |
| Référence |  |  |  |
| N° police |  |  |  |
| N° compteur |  |  |  |
| Arrêté du compteur |  |  |  |
| Contenances |  |  |  |
| Note contenance |  |  |  |
|  |  |  |  |
| Anomalie |  |  |  |
| Note anomalie |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Exporter | GEN0012 |
| Importer | GEN0018 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Audit | GEN0020 |
| Imprimer | GEN0011 |
Créé avec HelpNDoc Personal Edition: [Écrire des livres électroniques ePub pour l'iPad](https://www.helpndoc.com/fr/creer-des-livres-numeriques-epub)

### ETUDES DE FAISABILITE <a id="page-cfg-etudesdefaisabilite"></a>
_Page : `CFG_ETUDESDEFAISABILITE.html` — Module Stock_

[image : NouvelElement879.bmp]

### Liste

| LIBELLE | V | OBSERVATIONS |
|---|---|---|
| Charge TTC |  | Calculé à partir du bilan |
| Chiffre d'affaire TTC |  |  |
| Code |  |  |
| Crée le |  |  |
| Crée par |  |  |
| Date |  |  |
| Description |  |  |
| Entité |  |  |
| Fonds propre TTC |  |  |
| Marge bénéficiaire nette |  |  |
| Modifié le |  |  |
| Modifié par |  |  |
| Montant Concrétisation TTC |  |  |
| Montant Finalisation TTC |  |  |
| Prospection terrain |  |  |
| Retour sur coût projet |  |  |
| Retour sur fond propre |  |  |
| Retour sur le C.A |  |  |
| Taux de concrétisation |  |  |

### Champs

| LIBELLE | U | O | OBSERVATIONS |
|---|---|---|---|
| Société |  |  | Liste déroulante des entités juridique |
| Date |  |  |  |
| Code | X | X |  |
| Description |  | X |  |
| Statut |  |  |  |
| Prospection |  |  | Sélecteur des prospections terrains |
| Terrain |  |  | Sélecteur des [terrains](#page-terrains) |
|  |  |  |  |
| Général |  |  |  |
| Charge HT |  |  | Calculer à partir des lignes du bilan, en lecture seule |
| Charge TVA |  |  |  |
| Charge TTC |  |  |  |
| Chiffre d’affaire HT |  |  |  |
| Chiffre d’affaire TVA |  |  |  |
| Chiffre d’affaire TTC |  |  |  |
| Taux de concrétisation |  |  |  |
| Concrétisation HT |  |  | En lecture seule,<br> Concrétisation = Chiffre d’affaires * Taux de concrétisation |
| Concrétisation TVA |  |  |  |
| Concrétisation TTC |  |  |  |
| Finalisation HT |  |  | En lecture seule,<br> Finalisation = Chiffre d’affaire - Concrétisation |
| Finalisation TVA |  |  |  |
| Finalisation TTC |  |  |  |
| Crédit (Pourcentage) |  |  | En lecture seule |
| Crédit TTC |  |  |  |
| Fond propre (Pourcentage) |  |  | En lecture seule,<br> = Total charge - Avance client - Principale crédit (sans intérêt) |
| Fond propre TTC |  |  |  |
| Avances clients (Pourcentage) |  |  | En lecture seule,<br> = Montant concrétisation |
| Avances clients TTC |  |  |  |
| Marge bénéficiaire brute |  |  | En lecture seule,<br> Marge bénéficiaire brut = Chiffre d’affaire TTC - Charge TTC |
| I.S |  |  | En lecture seule,<br> I.S= Pourcentage I.S * Marge bénéficiaire brut TTC |
| Marge bénéficiaire nette |  |  | En lecture seule,<br> Marge bénéficiaire nette =Marge bénéficiaire brut – Montant I.S |
| Retour sur coût de projet |  |  | En lecture seule,<br> Retour sur coût de projet = Marge bénéficiaire nette / Charge (En H.T) |
| Retour sur le C.A |  |  | En lecture seule,<br> Retour sur le C.A = Marge bénéficiaire nette / Chiffre d’affaires (En H.T) |
| Retour sur fond propre |  |  | En lecture seule,<br> Retour sur fond propre = Marge bénéficiaire nette / Fond propre (En H.T) |
| Terrain |  |  |  |
| Terrain |  |  | Description terrain |
| Surface terrain (m²) |  |  |  |
| Voies d’aménagement/ équipements |  |  |  |
| Superficie terrain nette (m²) |  |  | = Surface terrain (m²) - Voies d’aménagement/ équipements |
| Emprise au sol bâtiments |  |  | = Superficie terrain nette (m²) * pourcentage emprise au sol bâtiments |
| Plancher couvert |  |  | = Superficie terrain nette (m²) * pourcentage Plancher couvert |
| Plancher vendable |  |  | = Plancher couvert (m²) * pourcentage plancher vendable |
| Prix d’achat du m² (TTC) |  |  |  |
| Coefficient frais d’enregistrement |  |  |  |
| Coefficient d’autre frais |  |  |  |
| TVA terrain |  |  | Par défaut 0% |
| Prix global du terrain (TTC) |  |  | = Prix du terrain + Frais d’enregistrement + Autre frais |
| Superficie non bâtie |  |  |  |
| Prix du terrain (TTC) |  |  |  |
| Frais d’enregistrement |  |  | = Prix du terrain * coefficient d’enregistrement |
| Autre frais |  |  | = Prix du terrain * coefficient d’autre frais |
| Exécution |  |  |  |
| Nombre d’article |  |  |  |
| Taux d’application de la retenue de garantie |  |  | (Ensemble de lots qui ont un statut paramétré " Est un marché définitif " et qui ont une retenue de garantie > 0) / (Ensemble de lots qui ont un statut paramétré "Est un marché définitif") * 100 |
| Taux de la retenue de garantie |  |  | Moyenne de retenue de granite des lots qui ont un statut paramétré " Est un marché définitif "et qui ont une retenue de garantie > 0 |
| Délai moyen de remboursement de garantie (j) |  |  | Moyenne de nombre de jours des lots qui ont un statut paramétré " Est un marché définitif "et qui ont une retenue de garantie > 0 |
| Délais |  |  |  |
| Achat terrain |  |  |  |
| Date début |  |  |  |
| Durée (mois) |  |  |  |
| Date fin |  |  |  |
| Étude et plans APD |  |  |  |
| Délai de relation |  |  |  |
| Date début |  |  |  |
| Durée (mois) |  |  |  |
| Date fin |  |  |  |
| Dépôt Autorisation de Construire |  |  |  |
| Délai de relation |  |  |  |
| Date début |  |  |  |
| Durée (mois) |  |  |  |
| Date fin |  |  |  |
| Étude béton structure + DCE |  |  |  |
| Délai de relation |  |  |  |
| Date début |  |  |  |
| Durée (mois) |  |  |  |
| Date fin |  |  |  |
| Démarrage travaux |  |  |  |
| Délai de relation |  |  |  |
| Date début |  |  |  |
| Durée (mois) |  |  |  |
| Date fin |  |  |  |
| Concrétisation |  |  |  |
| Délai de relation |  |  |  |
| Date début |  |  |  |
| Durée (mois) |  |  |  |
| Date fin |  |  |  |
| Finalisation |  |  |  |
| Délai de relation |  |  |  |
| Date début |  |  |  |
| Durée (mois) |  |  |  |
| Date fin |  |  |  |
| Financement |  |  |  |
| Taux d’intérêt TVA |  |  | Par défaut 10% |
| Liste de financement |  |  | La liste visualise la liste des crédits en indiquant :<br>- Description<br>- Date<br>- Déblocage<br>- Montant à régler<br>- Nombre de remboursements<br>- Périodicité en mois<br>- Période de grâce<br>- Taux de charge<br> Aussi, dans cette liste, on peut :<br>- Ajouter un crédit<br>- Éditer un crédit<br>- Supprimer un crédit |
| Analyse |  |  |  |
| Courbes d’analyse de l’étude de faisabilité |  |  | Les courbes visualisent les prévisions de :<br>- Montant de concrétisation<br>- Montant de finalisation<br>- Dépenses d’exécution<br>- Solde trésorerie<br>- Financement<br> en se basant sur l’étude de faisabilité effectuée<br> Aussi, on peut modifier les analyses en modifiant :<br>- L’écart-type de dépense<br>- L’écart-type de concrétisation<br>- L’écart-type de finalisation |
| Données |  |  |  |
| Liste des données |  |  | La liste visualise :<br>- La date<br>- Le montant débit<br>- Le montant crédit<br>- La description |
| Unité de base |  |  |  |
| Liste des unités de base |  |  | La liste visualise les unités de bases correspondantes à l’étude de faisabilité<br> Les unités de bases sont paramétrées dans la configuration du module stock<br> Encore, à partir de cette liste on peut effectuer les opérations suivantes :<br>- Ajouter une unité de base<br>- Supprimer une unité de base<br>- Définir la quantité d’une unité de base choisie<br>Remarque:<br> Le système vérifie les superficies des unité de base qui sont paramétrées "est une superficie" de la manière suivante:<br>- ∑ des unités de bases qui sont paramétrées "une superficie" et qui ont la même [fonction](#page-fonctionsspecifiques) spécifique (Une des fonctions spécifiques doit être paramétrée " Est un total " )<br>- Le système affiche la superficie en question sauf si la fonction spécifique de l'unité de base est paramétrée " Est un total ". |
| Tranches |  |  |  |
| Liste des tranches |  |  | La liste visualise les tranches d’un projet en identifiant :<br>- O.D.T<br>- Code<br>- Poids<br>- Tranche<br>Remarque:<br> On se basant sur le poids, le système ventile les unités de bases, les dépense et les recette (du bilan). |
| Unité de base/ Tranche |  |  |  |
| Liste des unités de base par tranche |  |  | La liste visualise :<br>- L’unité de base<br>- Quantité de base<br>- Quantité affecté à une tranche<br>- Pourcentage<br>Remarque:<br> Par défaut le système ventile les quantités des unités de base en fonction du poids, mais le système donne toujours la main de les modifier. |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Exporter | GEN0012 |
| Importer | GEN0018 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Audit | GEN0020 |
| Imprimer | GEN0011 |
| Bilant | En cliquant sur le bouton « Bilan », le système affiche un masque qui contient :<br>- La liste des unités de bases paramétré sous l’onglet " Unité de base " de l’étude de faisabilité<br>- La liste des lignes de dépense et des recettes (paramétré dans le module trésorerie), cette liste affiche :<br>- La description<br>- L’observation<br>- Le ratio = Prix unitaire<br>- Le montant HT<br>- Le montant TTC<br>- Poids<br>- Le montant tranche (HT)<br>- Le montant tranche (TTC)<br>- Les tranches |
| Export | Le système exporte dans un document Excel les données de l'étude de faisabilité. |
Exemple:
Pour créer une étude de faisabilité il faut procéder comme suit
Paramétrages
Module Stock
Création des fonctions spécifiques:
Dans le module Stock � Configuration � Prospections de terrain � fonctions spécifiques

[image : NouvelElement875.bmp]

### Création des unités de bases:
Dans le module Stock � Configuration � Prospection de terrain � Unités de base

[image : NouvelElement876.bmp]
Module Trésorerie

### Ajout des relations avec les autres modules
Dans le module Trésorerie � Configuration � Relation avec les autres modules

[image : NouvelElement877.bmp]
Étude de faisabilité
1- On ajoute une nouvelle étude de faisabilité
2- Sous l'onglet "Terrain" on ajoute les données qui concerne le terrain (ou bien on peut affecter directement un terrain ou une prospection terrain)
[image : NouvelElement869.bmp]
3- On ajoute les unités de base
[image : NouvelElement878.bmp]
Remarque:
Le système affiche la superficie en question si est seulement si la fonction spécifique paramétrée " Est un total ".
4- On affecte les tranches
[image : NouvelElement872.bmp]
5- On crée le bilan
5.1. On affecte les catégories de dépenses et de recettes on cliquant sur ajouter "Ajouter catégorie"
[image : NouvelElement874.bmp]
5.2. On ajoute les montants des dépenses et des recettes en fonction des unités de base et des montants saisi par l'utilisateur
[image : NouvelElement873.bmp]
Remarque:
Le système automatiquement dans le bilan, ajoute les dépenses qui sont liées au terrain.
[image : NouvelElement879.bmp]
Remarque:
A la fin le système affiche le résultat de l'étude de faisabilité comme elle indique la capture ci-dessus.
6- Sous l'onglet "Financement" on peut ajouter une simulation de crédit (optionnel)
[image : NouvelElement880.bmp]
Créé avec HelpNDoc Personal Edition: [Environnement de création d'aide complet](https://www.helpndoc.com/fr/outils-creation-aide)

### PROGRAMMES <a id="page-cfg-programmes"></a>
_Page : `CFG_PROGRAMMES.html` — Module Stock_

## [image : NouvelElement67.png]

### Liste

| LIBELLE | V | OBSERVATIONS |
|---|---|---|
| Adresse |  |  |
| Code |  | Code programme |
| Courrier fait à |  |  |
| Courrier signataire |  |  |
| Crée le |  |  |
| Crée par |  |  |
| Date début de commercialisation |  |  |
| Date fin de commercialisation |  |  |
| Date fin des travaux |  |  |
| Date lancement des travaux |  |  |
| Description |  | Description programme |
| Description détaillée |  |  |
| Email |  |  |
| Entité |  | Entité juridique |
| Fax |  |  |
| Mobile |  |  |
| Modifié le |  |  |
| Modifié par |  |  |
| Représentant légal |  |  |
| Représentant commercial |  |  |
| Représentant technique |  |  |
| Site web |  |  |
| Tél. |  |  |

### Champs

| PROPRIETE | U | O | OBSERVATIONS |
|---|---|---|---|
| Entité |  | X | Liste déroulante des [entités juridique](#page-entitesjuridique).<br> Remarque : En choisissant une entité juridique de la liste, le système affiche un message « Voulez-vous charger les données par défaut de l’entité sélectionnée »<br>[image : NouvelElement68.png]<br>- Si on clique « Oui » : Le système charge les données saisi dans le module [Entité juridique](#page-entitesjuridique).<br>- Si on clique « Non » : Le système ne charge pas les données du module cité ci-dessus. |
| Code | X | X |  |
| Description |  | X |  |
| Délais |  |  |  |
| Date lancement des travaux |  |  |  |
| Date fin des travaux |  |  |  |
| Date début de commercialisation |  |  |  |
| Date fin de commercialisation |  |  |  |
| Coordonnée |  |  |  |
| Adresse |  |  |  |
| Code postal |  |  |  |
| Ville |  |  |  |
| Pays |  |  | Liste déroulante des [Pays](#page-pays) |
| Téléphone |  |  |  |
| Fax |  |  |  |
| Mobile |  |  |  |
| E-mail |  |  |  |
| Site Web |  |  |  |
| Administration |  |  |  |
| Courrier fait à |  |  |  |
| Courrier signataire |  |  | Sélecteur de [personnels](#page-personnels) |
| Responsable technique |  |  | Sélecteur de [personnels](#page-personnels) |
| Responsable commercial |  |  | Sélecteur de [personnels](#page-personnels) |
| Autres |  |  |  |
| Description détaillée |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Exporter | GEN0012 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Audit | GEN0020 |
| Imprimer | GEN0011 |
Créé avec HelpNDoc Personal Edition: [Produire des livres électroniques facilement](https://www.helpndoc.com/fr/creer-des-livres-numeriques-epub)

### TRANCHES <a id="page-cfg-tranches"></a>
_Page : `Cfg_TRANCHES.html` — Module Stock_

[image : mmmm.png]

### Liste

| LIBELLE | V | OBSERVATIONS |
|---|---|---|
| Active |  |  |
| Adresse |  |  |
| Adresse SAV |  |  |
| Article légal pour la résiliation |  |  |
| Autre frais |  | Montant des autres frais |
| Avec octroi d'un cadeau pour le parrain |  | Si la tranche est paramétrée "Avec octroi d'un cadeau pour le parrain" , le système ajoute un cadeau dans le dernier dossier de vente de parrain avec la raison "Parrainage" |
| Avec octroi d'un cadeau pour les anciens clients |  | Si la tranche est paramétrée "Avec octroi d'un cadeau pour les anciens clients" et si le prospect de la prospection est un ancien client , à la création du dossier de vente, le système ajoute un cadeau avec la raison "Ancien Client" |
| Avec octroi d'un cadeau pour une source de contact |  | Si la tranche est paramétrée "Avec octroi d'un cadeau pour une source de contact" et si la source de contact de la prospection est paramétrée "Avec cadeau", le système , lors de la création de dossier de vente , ajoute un cadeau avec la raison "Source de contact" |
| Avec possibilité de cumul des cadeaux |  | Si la tranche est paramétrée "Avec possibilité de cumul des cadeaux" , le système affecte les lignes de cadeaux selon le paramétrage. |
| Code |  |  |
| Concrétisation |  | Pourcentage de la concrétisation |
| Courrier fait à |  |  |
| Courrier signataire |  |  |
| Crée le |  |  |
| Crée par |  |  |
| Date de la 1ere relance |  |  |
| Date début de commercialisation |  |  |
| Date début de finalisations |  | Date début de signatures des contrats |
| Date fin de commercialisation |  |  |
| Date fin de concrétisations |  |  |
| Date fin des travaux |  |  |
| Date lancement des travaux |  |  |
| Date octroi des T.F |  |  |
| Date permis d'habiter |  |  |
| Déduction après clôture du dossier de vente |  |  |
| Délai d'attente après deuxième notification T.F. (en jours) |  |  |
| Délai d'attente après première notification T.F. (en jours) |  |  |
| Délai de mise à disposition du solde de versements (en jours) |  |  |
| Délai entre la date d'envoi au notaire et la date minute |  |  |
| Délai entre la date minute et l'encaissement des restes à régler |  |  |
| Délai minimum pour la programmation d'une visite technique |  | Lors de la planification des visites techniques, le commercial ne peut pas planifier une visite avant le nombre de jours paramétré |
| Description |  |  |
| Description détaillée |  |  |
| Durée de service après vente (en mois) |  |  |
| Durée de service après vente supp. (en mois) |  |  |
| Écart-type concrétisation |  |  |
| Écart-type finalisation |  |  |
| Entité |  | Entité juridique |
| Frais de dossier |  |  |
| Frais de syndic |  |  |
| Modifié le |  |  |
| Modifié par |  |  |
| Nbr . étage |  |  |
| Nombre maximum de visite par jours |  | Lors de la planification des visites techniques, le commercial ne peut pas dépasser le nombre de jours paramétré |
| Pays |  | Description des Pays |
| Pénalité de désistement |  | Pénalité de désistement = Prix de vente de l'article (ou montant des encaissements * Pourcentage de la pénalité de ce désistement ) |
| Pénalité de désistement basée sur le prix de vente / encaissement |  | Montant des encaissements * Pourcentage de la pénalité de désistement<br> Si non<br> Prix de l'article * Pourcentage de la pénalité de désistement |
| Pénalité de non finalisation de la vente |  |  |
| Pénalité de transfert client |  | = Frais de transfert client lors de l'opération "Transfert Client" d'un client à un autre |
| Pénalité de transfert produit |  | = Frais de transfert produit lors de l'opération "Transfert Produit" d'un article à un autre dans la même tranche |
| Pénalité de transfert tranche |  | =Frais de transfert tranche lors de l'opération "Transfert Tranche" d'un article à un autre article qui appartient à une autre tranche |
| Période de syndic |  |  |
| Plancher couvert (m²) |  | Plancher couvert en m² = Somme des superficies des catégories saisi sous l'onglet Plancher couvert |
| Pré-réservation |  | Le montant de la pré-réservation |
| Programme |  |  |
| Remise sur autre frais |  | Remise sur autre frais = Montant des autres frais * Pourcentage de remise |
| Remise sur la pré-réservation |  | Remise de la pré-réservation = montant de la pré-réservation * Pourcentage de remise |
| Remise sur les frais de dossier |  | Remise sur les frais de dossier = Montant des frais de dossier * Pourcentage de remise |
| Remise sur les frais de syndic |  |  |
| Remise sur les pénalités de désistement |  | Remise sur les pénalités de désistement = Montant de pénalités de désistement * Pourcentage de remise |
| Remise sur les pénalités de transfert client |  | Remise sur les pénalités de transfert client = Montant de pénalités de transfert client * Pourcentage de la remise |
| Remise sur les pénalités de transfert produit |  | Remise sur les pénalités de transfert produit = Montant de pénalités de transfert produit * Pourcentage de remise |
| Remise sur les pénalités de transfert tranche |  | Remise sur les pénalités de transfert tranche = Montant des pénalités de transfert tranche * Pourcentage de remise |
| Représentant légal |  |  |
| Responsable commercial |  |  |
| Responsable S.A.V |  |  |
| Responsable technique |  |  |
| Seuil de versement lors de la concrétisation |  |  |
| Standing |  |  |
| Statut finalisation 1 |  | Permet de définir dans quel statut le système va appliquer les commissions de finalisation |
| Statut finalisation 2 |  | Permet de définir dans quel statut le système va appliquer les commissions de finalisation |
| Tiers autrui |  |  |
| Tribunal de commerce |  |  |
| Visible dans le module Achat |  | Si la case est cochée, le système affiche la tranche en question dans le module achat |
| Visible dans le module Location |  |  |
| Visible dans le module Projet |  |  |
| Visible dans le module service après vente |  |  |
| Visible dans le module Vente |  |  |

### Champs

| PROPRIETE | U | O | OBSERVATIONS |
|---|---|---|---|
| Programme |  | X | Liste déroulante des [programmes](#page-cfg-programmes)<br> Remarque : En choisissant un programme de la liste déroulante, le système affiche un message « Voulez-vous charger les données par défaut de l’entité sélectionnée »<br>[image : NouvelElement341.png]<br>- Si on clique « Oui » : Le système charge les données saisi dans le [programme](#page-cfg-programmes).<br>- Si on clique « Non » : Le système ne charge pas les données du module cité ci-dessus. |
| Code | X | X |  |
| Description |  | X |  |
| Standing |  |  | Liste déroulante des [standings](#page-stk-standings) |
| Plancher couvert m² |  |  | En lecture seule<br> Plancher couvert m² = ∑ des superficies des [catégories](#page-stk-categories-item) saisi sous l'onglet " Plancher couvert " |
| Responsable SAV |  |  | Sélecteur du [personnels](#page-personnels) |
| Visible dans le [module Vente](#page-modulevente) |  |  | Des cases à cocher qui indiquent dans quels modules la tranche sera visible |
| Visible dans le [module Location](#page-modulelocation) |  |  |  |
| Visible dans le [module Projet](#page-moduleprojet) |  |  |  |
| Visible dans le [module Achat](#page-moduleachat) |  |  |  |
| Visible dans le [module S.A.V](#page-moduleserviceapresvente) |  |  |  |
| Délais |  |  |  |
| Date lancement des travaux |  |  |  |
| Date fin des travaux |  |  |  |
| Date octroi des titres foncier |  |  |  |
| Date permis d’habiter |  |  |  |
| Date de la 1er relance |  |  |  |
| Date début de commercialisation |  |  |  |
| Date fin des concrétisations |  |  |  |
| Date début des finalisations |  |  |  |
| Date fin de commercialisation |  |  |  |
| Coordonnées |  |  |  |
| Adresse |  |  |  |
| Code postal |  |  |  |
| Ville |  |  |  |
| Pays |  |  | Liste déroulante de [Pays](#page-pays) |
| Téléphone |  |  |  |
| Fax |  |  |  |
| Mobile |  |  |  |
| E mail |  |  |  |
| Site web |  |  |  |
| Adresse S.A.V |  |  |  |
| Terrains |  |  |  |
| Liste de terrains |  |  | Affectation des terrains sur lesquelles la tranche sera construite avec un pourcentage d’occupation du terrain. |
| Étages |  |  |  |
| Liste des étages |  |  | Le système affiche la liste des [étages](#page-stk-etages) en indiquant la description de chaque étage<br> Aussi, dans cette liste on peut effectuer les opérations suivantes:<br>- Ajouter un étage<br>- Supprimer. |
| Plancher couvert |  |  |  |
| Liste des planchers couverts |  |  | Le système affiche la liste des planchers couverts en indiquant:<br>- La [catégorie](#page-stk-categories-item)<br>- La superficie (en m²)<br> Aussi, dans cette liste on peut effectuer les opérations suivantes:<br>- Ajouter: En cliquant sur cette action, les système affiche la liste de choix des catégories qui ont la [nature](#page-stk-natures) bâtiment.<br>- Supprimer. |
| Commission vendeurs |  |  |  |
| Liste des commissions vendeurs |  |  | Le système affiche la liste des [commissions](#page-commissions) vendeurs en indiquant:<br>- Date début<br>- Date fin<br>- Commission vente<br>- Commission vente (prestataire)<br>- Commission livraison<br>- Commission livraison (prestataire)<br>- Commission livraison 2<br>- Commission livraison 2 (prestataire)<br>- Catégorie article<br>- Tiers<br>- Action commerciale<br>- Crée le<br>- Crée par<br>- Modifié par<br>- Modifié le<br>Aussi, dans cette liste on peut effectuer les opérations suivantes:<br>- Ajouter: En cliquant sur cette action, le système affiche l’écran de paramétrage des commissions<br>- Supprimer<br>[image : NouvelElement658.png]<br>Remarque 1:<br> La combinaison des champs date début, date fin, catégorie des article, action commerciale et commercial doivent êtres uniques.<br>Remarque 2:<br> Pour deux ou plusieurs lignes de paramétrage identiques, on ne doit pas avoir un chevauchement des périodes.<br>==> Pour accéder à l'écran d'aide de la commission cliquer [ici](#page-commissions) |
| Seuils de versement |  |  |  |
| Liste de seuils de versement |  |  | Pour pouvoir personnaliser les seuils de versements pour les [statuts](#page-statuts-com) du [dossier de vente](#page-dossiersdevente)<br> La liste visualise :<br>- Le statut<br>- L’opérateur (>,>=)<br>- Seuil de versement (en pourcentage) |
| Règles de gestion 1 |  |  |  |
| Pré-réservation |  |  | Le montant de la pré-réservation |
| Concrétisation |  |  | Montant concrétisation = Prix article * Pourcentage de la concrétisation |
| Frais de dossier |  |  |  |
| Pénalité de désistement |  |  | Lors d’un désistement d’un dossier de vente ; le système applique une pénalité = Prix de vente de l’article (ou Montant des encaissements) *Pourcentage de la pénalité de désistement |
| Pénalité de désistement basée sur le prix de vente sinon sur les encaissements |  |  | Lors d’un désistement d’un dossier de vente, si la case est décochée, la pénalité se calcule de la manière suivante :<br> Pénalité de désistement = Montant des encaissements *Pourcentage de la pénalité de désistement<br> Sinon<br> Pénalité de désistement = Prix d’article *Pourcentage de la pénalité de désistement |
| Pénalité de transfert produit |  |  | Lors de l’opération « transfert produit » d’un article à un autre dans la même tranche, le système applique une Pénalité de transfert = Frais de transfert produit |
| Pénalité de transfert tranche |  |  | Lors de l’opération «transfert tranche » d’un article à un autre qui appartient à une autre tranche, le système applique une Pénalité de transfert = Frais de transfert tranche |
| Pénalité de transfert client |  |  | Lors de l’opération «transfert client » d’un client à un autre, le système applique une Pénalité de transfert = Frais de transfert client |
| Autre frais |  |  |  |
| Frais de syndic mensuel |  |  |  |
| Durée du syndic (en mois) |  |  |  |
| Règles de gestion 2 |  |  |  |
| Délai d’attente après première notification T.F (en jours) |  |  |  |
| Délai d’attente après deuxième notification T.F (en jours) |  |  |  |
| Pénalité de non finalisation de la vente |  |  |  |
| Déduction après clôture du dossier de vente |  |  |  |
| Délai de mise à disposition du solde des versements (en jours) |  |  |  |
| Durée du service après-vente (en mois) |  |  |  |
| Durée du service après-vente supp. (en mois) |  |  |  |
| Article légal pour la résiliation |  |  |  |
| Tribunal de commerce |  |  |  |
| Délai minimum pour la programmation d'une visite technique (j) |  |  | Lors de la planification des visites techniques, le commercial ne peut pas planifier une visite avant le nombre de jours paramétré. (La vérification se fait par rapport à la date de jour) |
| Nombre maximum de visites par jour |  |  | Lors de la planification des visites techniques, le commercial ne peut pas dépasser le nombre de jours paramétré dans ce champ |
| Avec octroi d'un cadeau pour le parrain |  |  | Si la tranche est paramétrée "Avec octroi d'un cadeau pour le parrain",et si le champ "Parrain" de la [prospection](#page-prospections) est renseigné, le système ajoute un cadeau dans le dernier dossier de vente du parrain avec le raison "Parrainage", sinon le système garde la liste des cadeaux du parrain inchangée. |
| Avec octroi d'un cadeau pour un ancien client |  |  | Si la tranche est paramétrée "Avec octroi d'un cadeau pour un ancien client" et le prospect de la prospection est un ancien client (ou la [prospection](#page-prospections) est flaguée "c'est un ancien client" ) le système, à la création du dossier de vente, ajoute un cadeau avec la raison "Ancien client". |
| Avec octroi d'un cadeau pour une source de contact |  |  | Si la tranche est paramétrée "Avec octroi d'un cadeau pour une source de contact" et la source de contact de la prospection est paramétrée "avec cadeau", le système à la création du dossier de vente ajoute un cadeau avec la raison "source de contact". |
| Avec possibilité de cumul des cadeaux |  |  | Si la tranche est paramétrée "Avec possibilité de cumul des cadeaux", le système affecte les lignes des cadeaux selon le paramétrage.<br> Sinon, le système affecte une seule ligne de cadeaux selon l'ordre des priorités suivantes:<br> P1- Si le prospect est un ancien client ; le système à la création du dossier de vente ajoute un cadeau avec la raison "Ancien client"<br>P2- Si la tranche appartient à une action commercial avec cadeau; le système à la création du dossier de vente ajoute un cadeau avec la raison "Action commerciale".<br>P3- Si la le source de contact de la prospection est paramétrée avec cadeau; le système à la création du dossier de vente ajoute un cadeau avec la raison "source de contact". |
| Remises pour les anciens clients |  |  |  |
| Remise sur la pré-réservation |  |  | Remise sur la pré-réservation= montant de la pré-réservation* Pourcentage de remise |
| Seuil de versement lors de la concrétisation |  |  | Lors de la concrétisation, le montant de versement pour les anciens clients doit être >= Pourcentage * Prix de vente<br>Remarque: Si le "Seuil de versement lors de la concrétisation" > Seuil de concrétisation (paramétré sous l'onglet seuil de versement de la tranche ) ou le seuil de concrétisation du statut, le système utilise le seuil définit dans la tranche(sous l'onglet "Seuil de versements") sinon le seuil définit dans la configuration de statut de concrétisation. |
| Active |  |  | Un flag pour indiquer si les anciens clients ont le droit d'avoir une remise ou non. |
| Remise sur les frais de dossier |  |  | Remise sur les frais de dossier = Montant du frais de dossier * Pourcentage de remise. |
| Remise sur les pénalités de désistement |  |  | Remise sur les pénalités de désistement = Montant de la pénalité sur désistement * Pourcentage de remise |
| Remise sur les pénalités de transfert produit |  |  | Remise sur les pénalités de transfert produit = Montant de la pénalité de transfert produit * Pourcentage de remise |
| Remise sur les pénalités de tranche |  |  | Remise sur les pénalités de tranche = Montant de la pénalité de transfert tranche * Pourcentage de remise |
| Remise sur les pénalités de transfert client |  |  | Remise sur les pénalités de transfert client = Montant de la pénalité de transfert client * Pourcentage de remise |
| Remise sur les frais de syndic |  |  | Remise sur les frais de syndic = Montant du frais syndic total * Pourcentage de remise |
| Remise sur autre frais |  |  | Remise sur autre frais = Montant d’autres frais * Pourcentage de remise |
| Autres |  |  |  |
| Délai entre la date d’envoi au notaire et la date minute (en jour) |  |  | Ce paramètre est utilisé dans le tableau de bord " Chiffre d'affaires " |
| Délai entre la date minute et l’encaissement des restes à régler ( en jour ) |  |  | Ce paramètre est utilisé dans le tableau de bord " Chiffre d'affaires " |
| Description détaillée |  |  |  |
| Tier autrui |  |  | Sélecteur des [fournisseurs](#page-fournisseurs-adm)<br> Le système affecte automatiquement le fournisseur choisi dans le champ "Tiers autrui" de la fiche de règlement(ou paiement), lorsqu'on règle (paye) un document paramétré dans la [trésorière](#page-tresorerie-com) "Pour autrui". |
| Statut de finalisation 1 |  |  | Listes déroulantes des statuts<br> Les deux listes déroulantes permettent de définir dans quel [statut](#page-statuts-com) le système va appliquer les commissions de finalisation. |
| Statut de finalisation 2 |  |  |  |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | GEN0008 |
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Exporter | GEN0012 |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Audit | GEN0020 |
| Imprimer | GEN0011 |
| Charger à partir d’une autre tranche | Le système affiche la liste des tranches qui sont définies, et, en effectuant un double clic (ou en sélectionnant une ligne et cliquant sur valider), il copie les données de la tranche sélectionnée à la tranche en cours.<br>[image : NouvelElement75.png] |
| Prévisions de base | Créer des prévisions de vente pour la tranche. |
| Imprimer les premières relances | On cliquant sur le bouton, le système affiche un message<br>[image : NouvelElement484.png]<br> Si on clique sur oui: le système modifie la date de la première relance dans tout les dossier de vente de la tranche sélectionnée, et il imprime les fiches relance 1.<br> Sinon: Le système ne fait rien<br>Remarque: Si le champs "Date de la 1ére relance" est vide, le système affiche un message<br> [image : NouvelElement485.png] |
| Mette à jour les schémas de financement | En cliquant sur le bouton « Mettre à jour les schémas de financement »<br> 1 - Le système parcourt toutes les lignes des schémas de financement des dossiers de vente de la tranche.<br> 2 - Le système retient les lignes dont la catégorie est basée sur la date l’octroi des titres fonciers<br> 3 - Le système affecte la nouvelle date aux dates antérieures des lignes retenues |
Prévisions de base
[image : NouvelElement585.png]

### Champs

| PROPRIETE | U | O | OBSERVATIONS |
|---|---|---|---|
| Tranche |  |  | En lecture seule |
| EN STOCK |  |  |  |
| Nombre d’article |  |  |  |
| Valorisation |  |  |  |
| PREVUE |  |  |  |
| Nombre d’article |  |  |  |
| Valorisation |  |  |  |
| CONCRETISATION |  |  |  |
| Montant |  |  | Valorisation * Taux Avance de concrétisation |
| FINALISATION |  |  |  |
| Montant |  |  | Valorisation du stock total – Montant Concrétisation |
|  |  |  |  |
| Avance de concrétisation |  |  |  |
| Date début de commercialisation |  |  |  |
| Date fin des concrétisations |  |  |  |
| Date début des finalisations |  |  |  |
| Date fin des finalisations |  |  |  |
| Écart-type |  |  |  |
| Concrétisation |  |  |  |
| Écart-type finalisation |  |  |  |
| Finalisation |  |  |  |
| PREVISIONS |  |  |  |
| Liste des prévisions de base enregistrées dans la base de données |  |  | La liste visualise tous les détails de prévisions de base en affichant (En lecture seule) :<br>- La date<br>- La quantité concrétisation<br>- Les versements concrétisation<br>- La quantité chiffre d’affaires<br>- Versement chiffre d’affaires<br>- Versement total<br>- Notes<br> Aussi, dans cette liste on peut exporter les prévisions enregistrées |
| Réajustement des prévisions |  |  |  |
| Liste des prévisions |  |  | La liste visualise les prévisions par mois en indiquant :<br>- La date<br>- La quantité concrétisation : Doit être égale au nombre d’articles de la tranche<br>- Versement concrétisation : Montant concrétisation / Nombre d’articles* Nombre d’articles à concrétiser du mois correspondant<br>- Quantité finalisation : Doit être égale au nombre d’article de la tranche<br>- Versement finalisation : Montant finalisation / Nombre d’articles * Nombre d’articles à finaliser du mois correspondant<br>- Versement total : Versement concrétisation + Versement finalisation<br>- Chiffre d’affaires TTC<br>- Note |
| Analyse |  |  |  |
| Courbes en cloche qui visualisent les versements des concrétisations et des finalisations |  |  |  |
Créé avec HelpNDoc Personal Edition: [Produire des livres EPub gratuitement](https://www.helpndoc.com/fr/creer-des-livres-numeriques-epub)

### ARTICLES <a id="page-articles"></a>
_Page : `ARTICLES.html` — Module Stock_

[image : 11.png]

### Liste

| LIBELLE | V | OBSERVATIONS |
|---|---|---|
| Adresse |  |  |
| Anomalies |  |  |
| Avance |  | Somme des lignes de schéma de financement qui sont paramétrées dans la [trésorerie](#page-tresorerie-com) de module vente "Avance" |
| Catégorie |  | Catégorie Article |
| Catégorie Code |  | Code de Catégorie de l'article |
| Catégorie de Charge |  | Catégorie charge |
| Catégorie de Charge Code |  | Code Catégorie charge |
| Code |  |  |
| Contenance |  |  |
| CPF Immatriculation |  | La Conservation de la Propriété Foncière |
| Crée le |  |  |
| Crée par |  |  |
| Date du titre foncier |  |  |
| Date enregistrement |  |  |
| Date Immatriculation |  |  |
| Description |  |  |
| Entité |  | Description de l'Entité juridique |
| Entité Code |  | Code de l'Entité juridique |
| Étage |  | Description Etage |
| Étage Code |  | Code de l'étage |
| État |  | Description état |
| Fraction Divise N° |  |  |
| Fraction Divise Terrain |  |  |
| Immeuble |  | Description immeuble |
| Immeuble Code |  | Code de l'immeuble |
| Index compt.eau |  |  |
| Index compt.électricité |  |  |
| Index compt.gaz |  |  |
| Intitulé du Titre foncier |  |  |
| Lieu d'enregistrement |  |  |
| Loyer mensuel HT |  | Montant d'un mois de location hors taxe |
| Loyer mensuel TTC |  | Montant d'un mois de location hors taxe * Taux TVA (%) |
| Modifié le |  | Date de modification |
| Modifié par |  | L'utilisateur qui a fait la modification |
| N° compt.eau |  |  |
| N° compt.électricité |  |  |
| N° compt.gaz |  |  |
| N° Dossier |  |  |
| N° enregistrement |  |  |
| N° Titre foncier |  |  |
| N° Titre foncier mère |  |  |
| Nombre de caution |  |  |
| Notes |  |  |
| Notes du titre foncier |  |  |
| Orientation |  | Description Orientation |
| Orientation Code |  | Code de l'orientation |
| Parties divise |  |  |
| Parties indivise |  |  |
| Prix Article HT |  | Prix de l'article hors taxe |
| Prix Article Prov.HT |  | Prix provisoire de l'article hors taxe |
| Prix Article Prov.TTC |  | Prix provisoire de l'article tout taxe compris |
| Prix Article TTC |  | Prix de l'article tout taxe compris |
| Prix de Ventes HT |  | Prix de vente = Prix article hors taxe + Prix sous articles hors taxe |
| Prix de Ventes Prov.HT |  | Prix de vente = Prix article provisoire hors taxe + Prix sous articles provisoire hors taxe |
| Prix de Ventes Prov.TTC |  | Prix de vente = Prix article provisoire tout taxe compris + Prix sous articles provisoire tout taxe compris |
| Prix de Ventes TTC |  | Prix de vente = Prix article tout taxe compris + Prix sous articles tout taxe compris |
| Prix du m² HT |  | Prix d'un article de 1m² hors taxe |
| Prix du m² Prov.HT |  | Prix provisoire d'un article de 1m² hors taxe |
| Prix du m² Prov.TTC |  | Prix provisoire d'un article de 1m² hors taxe * Taux TVA (%) |
| Prix du m² TTC |  | Prix d'un article de 1m² hors taxe * Taux TVA (%) |
| Prix Sous Article HT |  | Prix de sous article hors taxe |
| Prix Sous Article Prov.HT |  | Prix provisoire de sous article hors taxe |
| Prix Sous Article Prov.TTC |  | Prix provisoire de sous article tout taxe compris |
| Prix Sous Article TTC |  | Prix de sous article tout taxe compris |
| Programme |  |  |
| Programme Code |  |  |
| Propriété dite |  |  |
| Ref.compt.eau |  |  |
| Ref.compt.électricité |  |  |
| Ref.compt.gaz |  |  |
| Ref.police compt.eau |  |  |
| Ref.police compt.électricité |  |  |
| Ref.police compt.gaz |  |  |
| Remise max |  |  |
| Superficie (m²) |  |  |
| Superficie Nette (m²) |  |  |
| Superficie Prov. (m²) |  |  |
| Superficie Sous Article (m²) |  |  |
| Tranche |  | Description de la tranche |
| Tranche Code |  | Code de la tranche |
| TVA |  | Taxe sur valeur ajoutée |
| Type |  | Description Type |
| Type Code |  | Code type |
| Type Présentation |  | Type de présentation description |
| Type Présentation Code |  | Code Type de présentation |
| Vocation |  | Vocation description |
| Vocation Code |  | Code vocation |
[image : NouvelElement705.png]

### Champs

| PROPRIETE | U | O | OBSERVATIONS |
|---|---|---|---|
| Tranche |  | X | Liste déroulante des [tranches](#page-cfg-tranches) |
| Nature |  | X | En lecture seule |
| Catégorie |  | X | Liste déroulante de [catégories](#page-stk-categories-item) qui ont la nature "Bâtiment" et qui ne sont pas paramétrées "Pour les sous articles" |
| Vocation |  | X | Liste déroulante de [vocations](#page-stk-vocations-item) |
| Type |  | X | Liste déroulante de [types](#page-stk-types-item) |
| Code | X | X |  |
| Présentation |  |  | Liste déroulante de [présentations](#page-stk-typesdepresentation) |
| Description |  | X |  |
| Immeuble |  |  | Liste déroulante de [Immeuble](#page-stk-immeubles) |
| Orientation |  |  | Liste déroulante d’[orientations](#page-stk-orientations) |
| Étage |  |  | Liste déroulante d’[étages](#page-stk-etages) |
| Catégorie de charge |  |  | Liste déroulante de [catégorie de charge](#page-stk-categoriesdecharge) |
| Adresse |  |  |  |
| Notes |  |  |  |

| Superficies/ valorisation |  |  |  |
|---|---|---|---|
| Avance minimum |  |  |  |
| TVA |  |  | Taxe sur valeur ajoutée |
| Superficie nette (m²) |  |  | La surface nette en m² |
| Loyer HT |  |  | Montant de location hors taxe |
| Nombre de caution |  |  |  |
| Superficies/ valorisation (Provisoire) |  |  |  |
| Superficie (m²) |  |  | La superficie en m² |
| Prix du m² HT |  |  | Prix provisoire de 1m² hors taxe |
| Prix du m² TTC |  |  | Prix de 1m² provisoire toute taxe comprise |
| Prix article |  |  | Prix article = Superficie provisoire (m²) * Prix provisoire de m² |
| Prix sous articles |  |  | En lecture seule |
| Prix de vente |  |  | Prix de vente = Prix article provisoire + Prix sous articles provisoire (Le champ est en lecture seule) |
| Superficies/ valorisation(Définitive) |  |  |  |
| Superficie (m²) |  |  | La superficie en m² |
| Prix du m² HT |  |  | Prix définitif de 1m² hors taxe |
| Prix du m² TTC |  |  | Prix définitif de 1m² toute taxe comprise |
| Prix article |  |  | Prix article = Superficie définitive (m²) * Prix définitif de m² |
| Prix sous articles |  |  | En lecture seule |
| Prix de vente |  |  | Prix de vente = Prix article définitif + Prix sous articles définitif (Le champ est en lecture seule) |
| Remise Max |  |  | Le pourcentage de remise maximal qu’il peut attendre un rôle qui n’a pas le droit de modifier le prix de vente d’un article<br> Remarque :<br>- Si la remise est paramétrée à 0% le champ «Montant Remise » dans le [dossier de vente](#page-dossiersdevente) sera en lecture seule.<br>- Les rôles qui ont le droit de modifier le prix de vente peuvent dépasser le pourcentage de remise paramétré dans la fiche article. |
| Sous articles |  |  |  |
| Liste sous articles |  |  | La liste visualise les [sous articles](#page-sousarticles) affectés à l’article ; encore, à partir de cette liste on peut effectuer les opérations suivantes :<br>- Ajouter un sous article<br>- Modifier un sous article<br>- Supprimer un sous article<br>- Affecter un sous article<br>- Annuler l'affectation d'un sous article<br>Remarque: On peut annuler l'affectation que des sous articles libres |
| Identification foncière |  |  |  |
| Date enregistrement |  |  |  |
| N° enregistrement |  |  |  |
| Lieu enregistrement |  |  |  |
| C.P.F Immat |  |  | La Conservation de la Propriété Foncière |
| Date Immat |  |  |  |
| N° titre mère |  |  |  |
| N° titre |  |  |  |
| Intitulé titre |  |  |  |
| Date titre |  |  |  |
| Propriété dite |  |  |  |
| Fraction Divise N° |  |  |  |
| Fraction divises terrain |  |  |  |
| Parties divises |  |  |  |
| Parties indivises |  |  |  |
| Notes |  |  |  |
| Compteurs (Électricité) |  |  |  |
| Référence |  |  |  |
| N° police |  |  |  |
| N° compteur |  |  |  |
| Arrêté du compteur |  |  |  |
| Compteurs (Gaz) |  |  |  |
| Référence |  |  |  |
| N° police |  |  |  |
| N° compteur |  |  |  |
| Arrêté du compteur |  |  |  |
| Compteurs (Eau) |  |  |  |
| Référence |  |  |  |
| N° police |  |  |  |
| N° compteur |  |  |  |
| Arrêté du compteur |  |  |  |
| Contenances |  |  |  |
| Description d’article |  |  | Champs texte pour saisir la description de la contenance d’un article |
| Anomalie |  |  |  |
| Description d’anomalie |  |  | Champs texte pour saisir la description d’anomalie d’un article |
| Prospections |  |  |  |
| Liste de prospections |  |  | Liste des prospections qui ont été faite sur l’article en indiquant :<br>- Le statut de dossier<br>- La date<br>- Le prospect…<br> Aussi dans cette liste on peut faire l’action double clique pour ouvrir la fiche [prospection](#page-prospections) |
| Dossiers |  |  |  |
| Liste de dossiers |  |  | Liste des dossiers qui ont été créé pour l’article en indiquant :<br>- Le statut<br>- Le client<br>- Le montant à payer<br>- Reste à payer..<br> Aussi, dans cette liste on peut faire l’action double clique pour ouvrir le [dossier de vente](#page-dossiersdevente) du client |
| Historique |  |  |  |
| Historique |  |  | La liste visualise l’historique associée à un article en indiquant :<br>- L’utilisateur<br>- La date<br>- L’état<br>- N° Dossier<br>- Le Client<br>- Les détails |

### Actions

| LIBELLE | OBSERVATIONS |
|---|---|
| Ajouter | [GEN0008](#page-lesmasquesdeliste) |
| Éditer | GEN0009 |
| Supprimer | GEN0010 |
| Exporter | GEN0012 |
| Importer | GEN0018 |
| Importer plans | Importer le plan étages de la tranche choisi |
| Enregistrer | GEN0001 |
| Enregistrer et fermer | GEN0002 |
| Rafraîchir | GEN0003 |
| Fichiers attachés | GEN0004 |
| Actions | GEN0005 |
| Annuler et fermer | GEN0006 |
| Champs spécifique | GEN0007 |
| Audit | GEN0020 |
| Imprimer | GEN0011 |
Créé avec HelpNDoc Personal Edition: [Générateur complet de livres électroniques Kindle](https://www.helpndoc.com/fr/tour-des-fonctionnalites/creer-ebooks-pour-kindle-amazon)

### SOUS ARTICLES <a id="page-sousarticles"></a>
_Page : `SOUSARTICLES.html` — Module Stock_

_(page non téléchargée)_


### LOTISSEMENTS <a id="page-lotissements"></a>
_Page : `LOTISSEMENTS.html` — Module Stock_

_(page non téléchargée)_


### TABLEAUX DE BORDS <a id="page-tableauxdebords-stk"></a>
_Page : `TABLEAUXDEBORDS_STK.html` — Module Stock_

_(page non téléchargée)_


#### RAPPORT <a id="page-stk-reports"></a>
_Page : `STK_REPORTS.html` — Module Stock › TABLEAUX DE BORDS_

_(page non téléchargée)_


##### VALORISATION DU STOCK <a id="page-stktdbvalorisation"></a>
_Page : `StkTdbValorisation.html` — Module Stock › TABLEAUX DE BORDS › RAPPORT_

_(page non téléchargée)_


##### MOUVEMENTS DU STOCK <a id="page-stktdbmotion"></a>
_Page : `StkTdbMotion.html` — Module Stock › TABLEAUX DE BORDS › RAPPORT_

_(page non téléchargée)_


##### COMPARATIF PRIX AU M² <a id="page-stktdbcomparatifaumettrecarre"></a>
_Page : `StkTdbComparatifAuMettreCarre.html` — Module Stock › TABLEAUX DE BORDS › RAPPORT_

_(page non téléchargée)_


### CONFIGURATION <a id="page-configuration1"></a>
_Page : `CONFIGURATION1.html` — Module Stock_

_(page non téléchargée)_


#### PROSPECTIONS DE TERRAINS <a id="page-prospectionsdeterrains"></a>
_Page : `PROSPECTIONSDETERRAINS.html` — Module Stock › CONFIGURATION_

_(page non téléchargée)_


##### VOCATION <a id="page-lnd-vocation"></a>
_Page : `LND_VOCATION.html` — Module Stock › CONFIGURATION › PROSPECTIONS DE TERRAINS_

_(page non téléchargée)_


##### NIVEAU <a id="page-lnd-niveau"></a>
_Page : `LND_NIVEAU.html` — Module Stock › CONFIGURATION › PROSPECTIONS DE TERRAINS_

_(page non téléchargée)_


##### STATUTS <a id="page-lnd-statuts"></a>
_Page : `LND_STATUTS.html` — Module Stock › CONFIGURATION › PROSPECTIONS DE TERRAINS_

_(page non téléchargée)_


##### TRANSITIONS <a id="page-lnd-transitions"></a>
_Page : `LND_TRANSITIONS.html` — Module Stock › CONFIGURATION › PROSPECTIONS DE TERRAINS_

_(page non téléchargée)_


##### UNITES DE BASE <a id="page-stk-unitesdebase"></a>
_Page : `STK_UNITESDEBASE.html` — Module Stock › CONFIGURATION › PROSPECTIONS DE TERRAINS_

_(page non téléchargée)_


##### STANDINGS <a id="page-stk-standings"></a>
_Page : `STK_STANDINGS.html` — Module Stock › CONFIGURATION › PROSPECTIONS DE TERRAINS_

_(page non téléchargée)_


##### FONCTIONS SPECIFIQUES <a id="page-fonctionsspecifiques"></a>
_Page : `FONCTIONSSPECIFIQUES.html` — Module Stock › CONFIGURATION › PROSPECTIONS DE TERRAINS_

_(page non téléchargée)_


#### ARTICLES <a id="page-articles-cfg"></a>
_Page : `ARTICLES_CFG.html` — Module Stock › CONFIGURATION_

_(page non téléchargée)_


##### CATEGORIES <a id="page-stk-categories-item"></a>
_Page : `STK_CATEGORIES_ITEM.html` — Module Stock › CONFIGURATION › ARTICLES_

_(page non téléchargée)_


##### CATEGORIES DE CHARGE <a id="page-stk-categoriesdecharge"></a>
_Page : `STK_CATEGORIESDECHARGE.html` — Module Stock › CONFIGURATION › ARTICLES_

_(page non téléchargée)_


##### CONTENANCES <a id="page-stk-contenances"></a>
_Page : `STK_CONTENANCES.html` — Module Stock › CONFIGURATION › ARTICLES_

_(page non téléchargée)_


##### TYPES <a id="page-stk-types-item"></a>
_Page : `STK_TYPES_ITEM.html` — Module Stock › CONFIGURATION › ARTICLES_

_(page non téléchargée)_


##### TYPES DE PRESENTATION <a id="page-stk-typesdepresentation"></a>
_Page : `STK_TYPESDEPRESENTATION.html` — Module Stock › CONFIGURATION › ARTICLES_

_(page non téléchargée)_


##### ORIENTATIONS <a id="page-stk-orientations"></a>
_Page : `STK_ORIENTATIONS.html` — Module Stock › CONFIGURATION › ARTICLES_

_(page non téléchargée)_


##### IMMEUBLES <a id="page-stk-immeubles"></a>
_Page : `STK_IMMEUBLES.html` — Module Stock › CONFIGURATION › ARTICLES_

_(page non téléchargée)_


##### ETAGES <a id="page-stk-etages"></a>
_Page : `STK_ETAGES.html` — Module Stock › CONFIGURATION › ARTICLES_

_(page non téléchargée)_


##### TYPES DE FRAIS <a id="page-typesdefrais"></a>
_Page : `TYPESDEFRAIS.html` — Module Stock › CONFIGURATION › ARTICLES_

_(page non téléchargée)_


##### VOCATIONS <a id="page-stk-vocations-item"></a>
_Page : `STK_VOCATIONS_ITEM.html` — Module Stock › CONFIGURATION › ARTICLES_

_(page non téléchargée)_


##### NATURES <a id="page-stk-natures"></a>
_Page : `STK_NATURES.html` — Module Stock › CONFIGURATION › ARTICLES_

_(page non téléchargée)_


##### FACADES <a id="page-facades"></a>
_Page : `FACADES.html` — Module Stock › CONFIGURATION › ARTICLES_

_(page non téléchargée)_


##### VUES <a id="page-vues"></a>
_Page : `VUES.html` — Module Stock › CONFIGURATION › ARTICLES_

_(page non téléchargée)_


#### GROUPE D'ACTIONNAIRES <a id="page-groupedactionnaires"></a>
_Page : `GROUPEDACTIONNAIRES.html` — Module Stock › CONFIGURATION_

_(page non téléchargée)_


## Module Administration <a id="page-moduleadministration"></a>
_Page : `ModuleAdministration.html`_

_(page non téléchargée)_


### DROITS <a id="page-droits"></a>
_Page : `DROITS.html` — Module Administration_

_(page non téléchargée)_


#### ROLES <a id="page-roles"></a>
_Page : `ROLES.html` — Module Administration › DROITS_

_(page non téléchargée)_


#### UTILISATEURS <a id="page-utilisateurs"></a>
_Page : `UTILISATEURS.html` — Module Administration › DROITS_

_(page non téléchargée)_


#### HISTORIQUE DES ACTIONS <a id="page-historiquedesactions"></a>
_Page : `HISTORIQUEDESACTIONS.html` — Module Administration › DROITS_

_(page non téléchargée)_


#### FICHES VERROUILLEES <a id="page-fichesverrouillees"></a>
_Page : `FICHESVERROUILLEES.html` — Module Administration › DROITS_

_(page non téléchargée)_


#### HISTORIQUE DES CONVERSATIONS <a id="page-historiquedesconversations"></a>
_Page : `HISTORIQUEDESCONVERSATIONS.html` — Module Administration › DROITS_

_(page non téléchargée)_


### TIERS <a id="page-tiers"></a>
_Page : `TIERS.html` — Module Administration_

_(page non téléchargée)_


#### FOURNISSEURS <a id="page-fournisseurs-adm"></a>
_Page : `FOURNISSEURS_ADM.html` — Module Administration › TIERS_

_(page non téléchargée)_


#### BANQUES <a id="page-banques"></a>
_Page : `BANQUES.html` — Module Administration › TIERS_

_(page non téléchargée)_


#### ORGANISMES <a id="page-organismes"></a>
_Page : `ORGANISMES.html` — Module Administration › TIERS_

_(page non téléchargée)_


#### PERSONNELS <a id="page-personnels"></a>
_Page : `PERSONNELS.html` — Module Administration › TIERS_

_(page non téléchargée)_


#### CONTACTS <a id="page-contacts-adm"></a>
_Page : `CONTACTS_ADM.html` — Module Administration › TIERS_

_(page non téléchargée)_


### CONFIGURATION <a id="page-configuration-adm"></a>
_Page : `CONFIGURATION_ADM.html` — Module Administration_

_(page non téléchargée)_


#### CHAMPS SPECIFIQUES <a id="page-champsspecifiques"></a>
_Page : `CHAMPSSPECIFIQUES.html` — Module Administration › CONFIGURATION_

_(page non téléchargée)_


#### LOCALISATIONS <a id="page-localisations"></a>
_Page : `LOCALISATIONS.html` — Module Administration › CONFIGURATION_

_(page non téléchargée)_


#### PARAMETRES SYSTEME <a id="page-parametressysteme"></a>
_Page : `PARAMETRESSYSTEME.html` — Module Administration › CONFIGURATION_

_(page non téléchargée)_


#### RAPPORTS <a id="page-rapports-adm"></a>
_Page : `RAPPORTS_ADM.html` — Module Administration › CONFIGURATION_

_(page non téléchargée)_


#### MAIL MODELS <a id="page-mailmodels"></a>
_Page : `MAILMODELS.html` — Module Administration › CONFIGURATION_

_(page non téléchargée)_


#### SEQUENCES <a id="page-sequences"></a>
_Page : `SEQUENCES.html` — Module Administration › CONFIGURATION_

_(page non téléchargée)_


#### IMAGES <a id="page-images"></a>
_Page : `IMAGES.html` — Module Administration › CONFIGURATION_

_(page non téléchargée)_


#### PAYS <a id="page-pays"></a>
_Page : `PAYS.html` — Module Administration › CONFIGURATION_

_(page non téléchargée)_


#### VILLES <a id="page-villes"></a>
_Page : `VILLES.html` — Module Administration › CONFIGURATION_

_(page non téléchargée)_


#### ACTIONNAIRES <a id="page-actionnaires"></a>
_Page : `ACTIONNAIRES.html` — Module Administration › CONFIGURATION_

_(page non téléchargée)_


#### TYPES DES ATTACHEMENTS <a id="page-typesdesattachements"></a>
_Page : `TYPESDESATTACHEMENTS.html` — Module Administration › CONFIGURATION_

_(page non téléchargée)_


#### ENTITES FONCTIONNELLES <a id="page-entitesfonctionnelles"></a>
_Page : `ENTITESFONCTIONNELLES.html` — Module Administration › CONFIGURATION_

_(page non téléchargée)_


#### STATUTS DES RELANCES <a id="page-statutsdesrelances"></a>
_Page : `STATUTSDESRELANCES.html` — Module Administration › CONFIGURATION_

_(page non téléchargée)_


#### TIERS <a id="page-tiers-cfg"></a>
_Page : `TIERS_CFG.html` — Module Administration › CONFIGURATION_

_(page non téléchargée)_


##### SEXES <a id="page-sexes"></a>
_Page : `SEXES.html` — Module Administration › CONFIGURATION › TIERS_

_(page non téléchargée)_


##### TITRES <a id="page-titres"></a>
_Page : `TITRES.html` — Module Administration › CONFIGURATION › TIERS_

_(page non téléchargée)_


##### FORMES SOCIALE <a id="page-formessociale"></a>
_Page : `FORMESSOCIALE.html` — Module Administration › CONFIGURATION › TIERS_

_(page non téléchargée)_


##### TYPES <a id="page-types"></a>
_Page : `TYPES.html` — Module Administration › CONFIGURATION › TIERS_

_(page non téléchargée)_


##### NATURES <a id="page-natures"></a>
_Page : `NATURES.html` — Module Administration › CONFIGURATION › TIERS_

_(page non téléchargée)_


##### ASSUJETISSEMENTS <a id="page-assujetissements"></a>
_Page : `ASSUJETISSEMENTS.html` — Module Administration › CONFIGURATION › TIERS_

_(page non téléchargée)_


##### SITUATIONS FAMILIALE <a id="page-situationsfamiliale"></a>
_Page : `SITUATIONSFAMILIALE.html` — Module Administration › CONFIGURATION › TIERS_

_(page non téléchargée)_


##### TYPES RESIDENCE <a id="page-typesresidence"></a>
_Page : `TYPESRESIDENCE.html` — Module Administration › CONFIGURATION › TIERS_

_(page non téléchargée)_


##### REGIMES MATRIMONIAL <a id="page-regimesmatrimonial"></a>
_Page : `REGIMESMATRIMONIAL.html` — Module Administration › CONFIGURATION › TIERS_

_(page non téléchargée)_


##### TYPES PIECE IDENTITE <a id="page-typespieceidentite"></a>
_Page : `TYPESPIECEIDENTITE.html` — Module Administration › CONFIGURATION › TIERS_

_(page non téléchargée)_


##### NATIONALITES <a id="page-nationalites"></a>
_Page : `NATIONALITES.html` — Module Administration › CONFIGURATION › TIERS_

_(page non téléchargée)_


##### PROFESSIONS <a id="page-professions"></a>
_Page : `PROFESSIONS.html` — Module Administration › CONFIGURATION › TIERS_

_(page non téléchargée)_


##### SALAIRES <a id="page-salaires"></a>
_Page : `SALAIRES.html` — Module Administration › CONFIGURATION › TIERS_

_(page non téléchargée)_


##### TYPES DE RELATION <a id="page-typesderelation"></a>
_Page : `TYPESDERELATION.html` — Module Administration › CONFIGURATION › TIERS_

_(page non téléchargée)_


### TABLEAUX DE BORDS <a id="page-tableauxdebords-adm"></a>
_Page : `TABLEAUXDEBORDS_ADM.html` — Module Administration_

_(page non téléchargée)_


## Module Comptabilité <a id="page-modulecomptabilite"></a>
_Page : `ModuleComptabilite.html`_

_(page non téléchargée)_


### PARAMETRAGES <a id="page-parametrages"></a>
_Page : `PARAMETRAGES.html` — Module Comptabilité_

_(page non téléchargée)_


### DOCUMENTS A COMPTABILISER <a id="page-documentsacomptabiliser"></a>
_Page : `DOCUMENTSACOMPTABILISER.html` — Module Comptabilité_

_(page non téléchargée)_


### ECRITURES <a id="page-ecritures"></a>
_Page : `ECRITURES.html` — Module Comptabilité_

_(page non téléchargée)_


### CONFIGURATIONS <a id="page-configurations"></a>
_Page : `CONFIGURATIONS.html` — Module Comptabilité_

_(page non téléchargée)_


#### PLAN COMPTABLE TYPES <a id="page-plancomptabletypes"></a>
_Page : `PLANCOMPTABLETYPES.html` — Module Comptabilité › CONFIGURATIONS_

_(page non téléchargée)_


#### PLAN COMPTABLE <a id="page-plancomptable"></a>
_Page : `PLANCOMPTABLE.html` — Module Comptabilité › CONFIGURATIONS_

_(page non téléchargée)_


#### COMPTES PAR DEFAUT <a id="page-comptespardefaut"></a>
_Page : `COMPTESPARDEFAUT.html` — Module Comptabilité › CONFIGURATIONS_

_(page non téléchargée)_


#### JOURNAUX TYPES <a id="page-journauxtypes"></a>
_Page : `JOURNAUXTYPES.html` — Module Comptabilité › CONFIGURATIONS_

_(page non téléchargée)_


#### JOURNAUX <a id="page-journaux"></a>
_Page : `JOURNAUX.html` — Module Comptabilité › CONFIGURATIONS_

_(page non téléchargée)_


## Module Notification <a id="page-modulenotification"></a>
_Page : `ModuleNotification.html`_

_(page non téléchargée)_


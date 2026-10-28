# FormulairesBaridController

## formulaires

Impression du rapport n° 35 « Formulaires de Barid Al-Maghrib » (FEATURE-vente-formulaire-barid.md) : le modèle de
document `DOSSIER_VENTE` / `FORMULAIRE_BARID` rempli avec la liste `destinataires` de la section du hub. Un seul PDF, **une page par destinataire**, dans l'ordre de la section `destinataires` du résultat du hub avec les mêmes
filtres (nom du destinataire tous dossiers confondus, puis dossier, puis acquéreur avant co-acquéreurs) : le nombre de
pages est le nombre de lignes de la section. Un destinataire = l'acquéreur principal ou un co-acquéreur d'un dossier ni
annulé ni désisté, d'une tranche visible de l'appelant (société active, périmètre commercial, en vente).

Mêmes droits et même visibilité que le hub : rôle libre (`WithReportRestriction = 0`) ou rôle lié au rapport dans
`AdmReportDataRole`, relus à chaque appel. Un `trancheId` hors des tranches visibles est ignoré ; si aucun `trancheId`
donné n'est visible, aucune page (404). Un `statutId` donné exclut les dossiers sans statut. Ordre des contrôles :
module (404), rapport (404), droit (403), paramètres (400), modèle (404), plafond (400), aucun destinataire (404).
Aucune clé d'API n'a accès à cette route (403 `HORS_PORTEE`).

Réponse : `application/pdf` affiché en ligne, fichier `formulaires-barid_<aaaaMMjj_HHmmss>.pdf`,
`Cache-Control: private, no-store` (noms et adresses de clients) ; le contenu n'est jamais journalisé.

### reponse 400

Paramètre inconnu, `trancheId` ou `statutId` non UUID, `statutId` hors des options du descripteur, ou plus de 600 pages :
« Affinez les filtres : n pages (maximum 600). »

### reponse 404

Module autre que `vente`, code autre que `ComSaleFormulaireBaridMaroc` ou rapport absent de `AdmReportData`, modèle
`FORMULAIRE_BARID` absent ou inactif (« Le modèle FORMULAIRE_BARID est absent ou inactif »), ou aucune page à imprimer
(« Aucun destinataire »).

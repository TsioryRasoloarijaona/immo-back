# RapportsController

Hub « Rapports » d'un module (Vente › Tableau de bord › Rapports d'OMRAN Estate). Un rapport est listé s'il est implémenté
par le serveur, présent dans `AdmReportData` pour le module, et visible par l'appelant. Voir
`instructions/FEATURE-tdb-vente-rapports-socle.md`.

**Droits** : un compte est libre si l'un de ses rôles (`AdmUserRole`, relu à chaque appel, jamais le jeton) a
`AdmRole.WithReportRestriction = 0` ; libre, il voit tous les rapports ; sinon il ne voit que ceux liés à l'un de ses rôles
dans `AdmReportDataRole`. Sans rôle, rien. Un rapport absent de la liste répond 403 sur ses autres routes. Ordre des
contrôles : module (404), rapport (404), droit (403), paramètres (400). Aucune clé d'API n'a accès à ces routes.

**Données** : un rapport ne lit que les tranches visibles de l'appelant — société active, périmètre commercial, et en vente
pour le module `vente`. Un `trancheId` hors de ces tranches est ignoré sans erreur ; si aucun `trancheId` donné n'est
visible, le résultat est vide (jamais « toutes les tranches »).

## descripteur

Les options d'un paramètre `TRANCHES` sont les tranches visibles de l'appelant, triées par libellé ; celles d'un
paramètre `COMMERCIAUX` les commerciaux de sa portée (tous pour un rôle exempté, son équipe pour un chef, lui seul sinon).
Les options d'un `CHOIX` peuvent être lues en base à chaque appel.

## resultat

**Paramètres de requête** : ceux du descripteur, par leur `nom` ; un paramètre `multiple` se répète
(`trancheId=…&trancheId=…`). Absent : `valeurParDefaut`. Dates `aaaa-MM-jj`.

**Totaux** : `totaux` et `sousTotaux` sont calculés ici, en décimal exact, selon la `fonctionTotal` de chaque colonne ;
le rapport peut imposer une valeur (ex. un taux global), qui l'emporte. Section sans ligne : `totaux` = null. Le client
n'a rien à recalculer.

### reponse 400

Paramètre inconnu (le message liste les noms acceptés), obligatoire manquant, date mal formée, `au` avant `du`, année hors
[1900 ; 2200], entier hors de ses bornes `min`/`max`, choix hors options, booléen autre que true/false, identifiant non
UUID, valeur répétée sur un paramètre non `multiple`, ou section de plus de 20 000 lignes (affinez les filtres).

## exporter

Classeur `.xlsx` du même résultat, fichier `<code>_<aaaaMMjj_HHmmss>.xlsx` : une feuille par section (nom = titre sans
`[]:*?/\`, 31 caractères au plus, dédoublonné), en-tête en gras figé, colonnes reçues dans l'ordre, dernière ligne
« Total » en gras quand la section a des totaux. Mêmes paramètres que `/resultat`.

### param format

Seule valeur acceptée : `xlsx` (défaut).

### reponse 400

`format` autre que `xlsx`, ou un paramètre refusé comme sur `/resultat`.
